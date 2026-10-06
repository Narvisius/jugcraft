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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Furniture that joins up side by side (sofas, reception and kitchen counters, lab benches, locker-room benches): a row
 * of pieces of the same group facing the same way reads as one long piece, with end panels (sofa arms) only at the
 * two ends. LEFT and RIGHT are as seen by whoever uses it.
 */
public class RowFurnitureBlock extends FurnitureBlock {
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	private final String group;

	public RowFurnitureBlock(Properties properties, String group, double[]... boxes) {
		super(properties, boxes);
		this.group = group;
		registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LEFT, RIGHT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return joined(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		if (direction.getAxis() != Direction.Axis.Y) {
			return joined(state, level, pos);
		}
		return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbour, random);
	}

	private BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state.setValue(LEFT, joins(level.getBlockState(pos.relative(facing.getCounterClockWise())), facing))
				.setValue(RIGHT, joins(level.getBlockState(pos.relative(facing.getClockWise())), facing));
	}

	private boolean joins(BlockState other, Direction facing) {
		return other.getBlock() instanceof RowFurnitureBlock row && row.group.equals(group) && other.getValue(FACING) == facing;
	}
}
