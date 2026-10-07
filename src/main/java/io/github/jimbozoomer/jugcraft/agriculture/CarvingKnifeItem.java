package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Carving Knife: used on the side of a pumpkin, it opens the carving screen for that side. The face
 * is checked and carved on the server ({@link PumpkinCarvings}); each finished carving costs one durability.
 * Used while sneaking on the top of a giant pumpkin 2 or 3 blocks wide, it hollows it out into a boat
 * ({@link PumpkinCarvings#hollow}); without sneaking it only says how.
 */
public class CarvingKnifeItem extends Item {
	public CarvingKnifeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Direction side = context.getClickedFace();
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (side == Direction.UP && state.getBlock() instanceof GiantPumpkinBlock && state.getValue(GiantPumpkinBlock.SIZE) >= 2) {
			if (context.getPlayer() instanceof ServerPlayer player) {
				if (!player.isSecondaryUseActive()) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.hollow.hint"));
				} else {
					PumpkinCarvings.HollowResult result = PumpkinCarvings.hollow(player, context.getClickedPos(), context.getHand());
					if (result != PumpkinCarvings.HollowResult.HOLLOWED) {
						player.sendOverlayMessage(Component.translatable("message.jugcraft.hollow." + result.name().toLowerCase()));
					}
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (side.getAxis() == Direction.Axis.Y || !PumpkinCarvings.isCarvable(state)) {
			return InteractionResult.PASS;
		}
		if (context.getPlayer() instanceof ServerPlayer player) {
			if (!player.mayUseItemAt(context.getClickedPos(), side, context.getItemInHand())) {
				return InteractionResult.FAIL;
			}
			PumpkinCarvings.open(player, context.getClickedPos(), side);
		}
		return InteractionResult.SUCCESS;
	}
}
