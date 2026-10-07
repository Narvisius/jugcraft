package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A flat decoration hung on the side of a solid block, like a wall torch: the Ornamental Corn Bundle on a
 * door frame. {@link #FACING} is the way it faces, away from the wall; it drops when the wall goes.
 */
public class WallDecorationBlock extends HorizontalDirectionalBlock {
	private final VoxelShape[] shapes = new VoxelShape[4];

	/** {@code depth}: how far it stands out from the wall; {@code low} and {@code high}: its extent, in pixels. */
	public WallDecorationBlock(Properties properties, double depth, double low, double high) {
		super(properties);
		shapes[Direction.SOUTH.get2DDataValue()] = Block.box(low, low, 0.0, high, high, depth);
		shapes[Direction.NORTH.get2DDataValue()] = Block.box(low, low, 16.0 - depth, high, high, 16.0);
		shapes[Direction.EAST.get2DDataValue()] = Block.box(0.0, low, low, depth, high, high);
		shapes[Direction.WEST.get2DDataValue()] = Block.box(16.0 - depth, low, low, 16.0, high, high);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes[state.getValue(FACING).get2DDataValue()];
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockPos wall = pos.relative(facing.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	/** Hangs on the clicked wall, or the nearest wall the player looks towards. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		for (Direction direction : context.getNearestLookingDirections()) {
			if (direction.getAxis().isHorizontal()) {
				BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
				if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
					return state;
				}
			}
		}
		return null;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
