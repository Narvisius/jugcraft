package io.github.jimbozoomer.jugcraft.artillery;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/**
 * A big gun or balloon as an item: use it on the ground to set it down there, facing the way you face. A tower gun
 * (batch 54) is centred on the block above the one clicked and needs a solid top under its whole footprint.
 */
public class PlaceEntityItem extends Item {
	private final Supplier<EntityType<? extends Entity>> type;
	private final String tooltip;

	public PlaceEntityItem(Properties properties, Supplier<EntityType<? extends Entity>> type, String tooltip) {
		super(properties);
		this.type = type;
		this.tooltip = tooltip;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return InteractionResult.SUCCESS;
		}
		BlockPos at = context.getClickedPos().relative(context.getClickedFace());
		Vec3 spot = Vec3.atBottomCenterOf(at);
		Player player = context.getPlayer();
		Entity entity = type.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		if (entity == null) {
			return InteractionResult.FAIL;
		}
		float yaw = player != null ? player.getYRot() : 0.0F;
		entity.snapTo(spot.x, spot.y, spot.z, yaw, 0.0F);
		if (entity instanceof CrewedGun gun) {
			gun.face(yaw);
		}
		if (entity instanceof TowerGun gun && !TowerGun.supported(level, at, gun.spec().footprint())) {
			// A tower gun stands on a solid top as wide as it is: a tower's, a wall's or the ground.
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.needs_top", gun.spec().footprint()));
			}
			return InteractionResult.FAIL;
		}
		if (!level.noCollision(entity, entity.getBoundingBox())) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.artillery.no_room"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(entity);
		if (player == null || !player.getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		tooltip.accept(Component.translatable(this.tooltip).withStyle(ChatFormatting.GRAY));
	}
}
