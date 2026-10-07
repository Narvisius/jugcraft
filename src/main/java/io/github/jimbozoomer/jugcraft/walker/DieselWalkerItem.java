package io.github.jimbozoomer.jugcraft.walker;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** A walker as an item: use it on the ground to stand the walker there, facing the way you face. */
public class DieselWalkerItem extends Item {
	private final Supplier<EntityType<? extends DieselWalker>> type;
	private final String tooltip;

	public DieselWalkerItem(Properties properties) {
		this(properties, () -> JugcraftWalkers.DIESEL_WALKER, "tooltip.jugcraft.diesel_walker");
	}

	public DieselWalkerItem(Properties properties, Supplier<EntityType<? extends DieselWalker>> type, String tooltip) {
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
		DieselWalker walker = type.get().create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
		if (walker == null) {
			return InteractionResult.FAIL;
		}
		walker.snapTo(spot.x, spot.y, spot.z, player != null ? player.getYRot() : 0.0F, 0.0F);
		if (!level.noCollision(walker, walker.getBoundingBox())) {
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.walker.no_room"));
			}
			return InteractionResult.FAIL;
		}
		level.addFreshEntity(walker);
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
