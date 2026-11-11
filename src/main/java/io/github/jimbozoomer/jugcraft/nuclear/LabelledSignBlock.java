package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A sign on a wall more than a block big with its label picked, not random (turbine hall part 3: the crane's travel
 * direction signs on the hall's walls, N, E, S or W, the way the pendant's buttons run it; tools/hall_fittings.py
 * crane_compass): placed with the first, and used with an empty hand it steps every part to the next.
 */
public class LabelledSignBlock extends MultiFurnitureBlock {
	private final String[] labels;

	public LabelledSignBlock(Properties properties, int width, int rows, String[] labels) {
		super(properties, width, rows, true, labels.length);
		this.labels = labels;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null || variantProperty() == null ? state : state.setValue(variantProperty(), 0);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		IntegerProperty v = variantProperty();
		if (v == null || !player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			BlockPos origin = pos.subtract(offset(facing, state.getValue(partProperty())));
			int next = (state.getValue(v) + 1) % variants;
			for (int p = 0; p < width * rows; p++) {
				BlockPos at = origin.offset(offset(facing, p));
				BlockState s = level.getBlockState(at);
				if (s.getBlock() == this) {
					level.setBlock(at, s.setValue(v, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.relay_relabel", labels[next]));
		}
		return InteractionResult.SUCCESS;
	}
}
