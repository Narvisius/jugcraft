package io.github.jimbozoomer.jugcraft.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * A fluid valve: a steel pipe segment with a valve body and handwheel. Open, it carries fluid like any pipe; a
 * redstone signal closes it, and then the pipe network stops there, so the pipes on either side are separate
 * networks. Its lamp shows green while open and amber while closed.
 */
public class FluidValveBlock extends FluidPipeBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public FluidValveBlock(Properties properties, long rateMb) {
		super(properties, rateMb);
		registerDefaultState(defaultBlockState().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POWERED);
	}

	/** Open unless powered. */
	@Override
	public boolean carries(BlockState state) {
		return !state.getValue(POWERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED)) {
			level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
		}
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
	}
}
