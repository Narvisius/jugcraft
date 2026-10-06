package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Furniture that joins up when stacked (server racks, relay cabinets, lockers, pallet racking): a stack of them reads
 * as one tall unit, with the top cap only on the highest block and the plinth only on the lowest.
 */
public class StackingFurnitureBlock extends FurnitureBlock {
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

	public StackingFurnitureBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
		registerDefaultState(defaultBlockState().setValue(UP, false).setValue(DOWN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(UP, DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return joined(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		if (direction.getAxis() == Direction.Axis.Y) {
			return joined(state, level, pos);
		}
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
	}

	private BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
		return state.setValue(UP, level.getBlockState(pos.above()).is(this)).setValue(DOWN, level.getBlockState(pos.below()).is(this));
	}
}
