package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Pumpkin Barge or Pumpkin Racer as an item: placed on water (or anywhere a boat goes) like a boat, facing
 * the way the player looks. It carries its {@link PumpkinBoatData} to the boat and back.
 */
public class PumpkinBoatItem extends Item {
	private final PumpkinBoat.Kind kind;

	public PumpkinBoatItem(PumpkinBoat.Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public PumpkinBoat.Kind kind() {
		return kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
		if (hit.getType() != HitResult.Type.BLOCK) {
			return InteractionResult.PASS;
		}
		PumpkinBoat boat = JugcraftAgriculture.boatType(kind).create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		if (boat == null) {
			return InteractionResult.FAIL;
		}
		Vec3 at = hit.getLocation();
		boat.setInitialPos(at.x, at.y, at.z);
		boat.setYRot(player.getYRot());
		boat.setItem(stack);
		if (!level.noCollision(boat, boat.getBoundingBox())) {
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide()) {
			level.addFreshEntity(boat);
			level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
			stack.consume(1, player);
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		PumpkinBoatData data = stack.getOrDefault(JugcraftAgriculture.PUMPKIN_BOAT, kind.defaultData());
		tooltip.accept(Component.translatable("item.jugcraft.pumpkin_boat.weight", data.weight()).withStyle(ChatFormatting.GRAY));
		if (data.carved()) {
			tooltip.accept(Component.translatable(data.lit() ? "item.jugcraft.pumpkin_boat.carved_lit" : "item.jugcraft.pumpkin_boat.carved")
					.withStyle(ChatFormatting.GRAY));
		}
	}
}
