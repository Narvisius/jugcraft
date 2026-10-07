package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Giant Bone Hand (the churchyard's ornaments): a giant's skeletal hand two blocks tall, reaching out of a heap of
 * grave earth with its fingers spread and clawing. Give either half a redstone signal and it clenches into a fist with
 * a crack of bones, opening again when the signal goes.
 */
public class GiantBoneHandBlock extends TallDecorationBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public GiantBoneHandBlock(Properties properties, VoxelShape lower, VoxelShape upper) {
		super(properties, lower, upper);
		registerDefaultState(defaultBlockState().setValue(POWERED, false));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level instanceof ServerLevel server) {
			BlockPos other = pos.relative(state.getValue(HALF).getDirectionToOther());
			boolean powered = level.hasNeighborSignal(pos) || level.hasNeighborSignal(other);
			if (powered != state.getValue(POWERED)) {
				setBoth(level, pos, state.setValue(POWERED, powered));
				BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : other;
				server.playSound(null, lower.above(), powered ? SoundEvents.SKELETON_HURT : SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F,
						powered ? 0.6F : 0.8F);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POWERED);
	}
}
