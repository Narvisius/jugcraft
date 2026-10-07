package io.github.jimbozoomer.jugcraft.control;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A data cable (batch 36): optical fibre in a plastic sheath. It joins sensors, relays and logic controllers into one
 * control network ({@link ControlNetwork}); it carries no power and no items, and has no block entity.
 */
public class DataCableBlock extends PipeBlock implements DataConnectable {
	public DataCableBlock(Properties properties) {
		super(4.0F, properties);
		BlockState state = stateDefinition.any();
		for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			BlockState neighbor = context.getLevel().getBlockState(context.getClickedPos().relative(entry.getKey()));
			state = state.setValue(entry.getValue(), connectsTo(neighbor));
		}
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighborState));
	}

	private static boolean connectsTo(BlockState neighbor) {
		return neighbor.getBlock() instanceof DataConnectable;
	}
}
