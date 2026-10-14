package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A relay room cabinet (round 26, #10: RelayRoom): one block wide and two high, labelled (its variant). Right-clicking
 * it says its name and what it reads; sneaking and right-clicking it with an empty hand steps it to its next label
 * (so cabinets put down by hand can be told apart, as the plants' are).
 */
public class RelayCabinetBlock extends LivePanelBlock {
	public RelayCabinetBlock(Properties properties, int labels) {
		super(properties, 1, 2, labels);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		int kind = RelayRoom.kind(state);
		Direction facing = state.getValue(FACING);
		BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
		int variant = LivePanelBlockEntity.variant(state);
		IntegerProperty v = variantProperty();
		if (player.isShiftKeyDown() && v != null) {
			// The next label, on every part of the cabinet.
			int next = (variant + 1) % variants;
			for (int p = 0; p < width * rows; p++) {
				BlockPos at = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(at);
				if (s.getBlock() == this) {
					level.setBlock(at, s.setValue(v, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", RelayRoom.label(kind, next)));
			return InteractionResult.SUCCESS_SERVER;
		}
		RelayRoom.Reactor reactor = level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? panel.reactor() : RelayRoom.Reactor.NONE;
		player.sendOverlayMessage(Component.translatable(kind != RelayRoom.RELAY && reactor.status() == 0 ? "message.jugcraft.nuclear.relay_no_reactor"
				: "message.jugcraft.nuclear.relay_cabinet", RelayRoom.describe(kind, variant, reactor)));
		return InteractionResult.SUCCESS_SERVER;
	}
}
