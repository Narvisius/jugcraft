package io.github.jimbozoomer.jugcraft.diagonal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A vanilla wall while it joins a wall diagonally (docs/features/diagonal-connections.md). The vanilla wall itself is
 * left exactly as it is, with its 324 states; it becomes this block only while it has a diagonal, and goes back to the
 * vanilla wall when its last diagonal goes ({@link DiagonalWalls}).
 *
 * <p>Its sides are joined or not, and one property, {@link #TALL}, raises them all, straight and diagonal, to meet the
 * block above: a post, four sides, four diagonals, tall and waterlogged make 2,048 states, where the same properties on
 * the vanilla wall would make 5,184. Players see the vanilla
 * wall: it drops it, picks as it, is named as it and is in {@code #minecraft:walls}, so walls, fence gates, bars, tools
 * and mobs treat it as one. Its updates are the vanilla wall's own: it hands each to the wall it stands for, which works
 * out the sides, the tall sides and the post and turns back into this block if it still joins diagonally.
 */
public class DiagonalWallBlock extends Block implements SimpleWaterloggedBlock {
	public static final BooleanProperty UP = WallBlock.UP;
	public static final BooleanProperty WATERLOGGED = WallBlock.WATERLOGGED;
	/** Every joined side and diagonal arm rises to the top of the block, to meet the block above (DiagonalWalls.settle). */
	public static final BooleanProperty TALL = BooleanProperty.create("tall");
	/** Its straight sides: the same properties fences and bars use, so the diagonal rule reads them alike. */
	public static final Map<Direction, BooleanProperty> SIDES = CrossCollisionBlock.PROPERTY_BY_DIRECTION;
	private static final List<Direction> HORIZONTAL = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

	/** Outlines and collisions for every combination of post, sides, diagonals and height, shared by every diagonal wall. */
	private static final AtomicReferenceArray<VoxelShape> OUTLINES = new AtomicReferenceArray<>(1024);
	private static final AtomicReferenceArray<VoxelShape> COLLISIONS = new AtomicReferenceArray<>(1024);

	private final WallBlock wall;

	public DiagonalWallBlock(WallBlock wall, Properties properties) {
		super(properties);
		this.wall = wall;
		BlockState state = stateDefinition.any().setValue(UP, true).setValue(TALL, false).setValue(WATERLOGGED, false);
		for (Direction side : HORIZONTAL) {
			state = state.setValue(SIDES.get(side), false);
		}
		registerDefaultState(DiagonalConnections.withoutDiagonals(state));
	}

	/** The vanilla wall this block stands for. */
	public WallBlock wall() {
		return wall;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(UP, SIDES.get(Direction.NORTH), SIDES.get(Direction.EAST), SIDES.get(Direction.SOUTH), SIDES.get(Direction.WEST),
				TALL, WATERLOGGED, DiagonalConnections.NORTH_EAST, DiagonalConnections.SOUTH_EAST, DiagonalConnections.SOUTH_WEST,
				DiagonalConnections.NORTH_WEST);
	}

	/** The vanilla wall's item: picking or listing this block gives the wall. */
	@Override
	public Item asItem() {
		return wall.asItem();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return wall.getStateForPlacement(context);
	}

	/**
	 * The vanilla wall works out the update (its sides, tall sides and post) and turns back into this block if the wall
	 * still joins diagonally. If it no longer does, it is the vanilla wall again, and its tall sides are worked out from
	 * what is above it, as the vanilla wall does when the block above changes.
	 */
	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
		BlockState updated = DiagonalWalls.toWall(state).updateShape(level, ticks, pos, direction, neighbourPos, neighbour, random);
		if (updated.getBlock() == wall && direction != Direction.UP) {
			BlockPos above = pos.above();
			updated = updated.updateShape(level, ticks, pos, Direction.UP, above, level.getBlockState(above), random);
		}
		return updated;
	}

	@Override
	protected void updateIndirectNeighbourShapes(BlockState state, LevelAccessor level, BlockPos pos, int flags, int recursionLeft) {
		DiagonalConnections.updateDiagonalNeighbours(level, pos, flags, recursionLeft);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state, OUTLINES, state.getValue(TALL) ? 16 : 14, 16);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state, COLLISIONS, 24, 24);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return !state.getValue(WATERLOGGED);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState rotated = state;
		for (Direction side : HORIZONTAL) {
			rotated = rotated.setValue(SIDES.get(rotation.rotate(side)), state.getValue(SIDES.get(side)));
		}
		return DiagonalConnections.rotate(rotated, state, rotation);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState mirrored = state;
		for (Direction side : HORIZONTAL) {
			mirrored = mirrored.setValue(SIDES.get(mirror.mirror(side)), state.getValue(SIDES.get(side)));
		}
		return DiagonalConnections.mirror(mirrored, state, mirror);
	}

	/**
	 * The shape for the state's post, sides and diagonals, as vanilla's walls are drawn: an 8-pixel post, 6-pixel sides
	 * to the edge and diagonal arms as wide, `sideHeight` pixels high. Worked out once for each of the 1,024 combinations
	 * (with the height), for every diagonal wall.
	 */
	private static VoxelShape shape(BlockState state, AtomicReferenceArray<VoxelShape> cache, double sideHeight, double postHeight) {
		int index = state.getValue(UP) ? 1 : 0;
		for (int i = 0; i < HORIZONTAL.size(); i++) {
			index |= state.getValue(SIDES.get(HORIZONTAL.get(i))) ? 2 << i : 0;
		}
		index |= DiagonalConnections.mask(state) << 5;
		index |= state.getValue(TALL) ? 1 << 9 : 0;
		VoxelShape shape = cache.get(index);
		if (shape == null) {
			shape = (index & 1) != 0 ? Block.column(8, 0, postHeight) : Shapes.empty();
			Map<Direction, VoxelShape> sides = Shapes.rotateHorizontal(Block.boxZ(6, 0, sideHeight, 0, 11));
			for (int i = 0; i < HORIZONTAL.size(); i++) {
				if ((index & 2 << i) != 0) {
					shape = Shapes.or(shape, sides.get(HORIZONTAL.get(i)));
				}
			}
			shape = Shapes.or(shape, DiagonalConnections.Arms.of(3 / 16.0, 0, sideHeight / 16, index >> 5 & 0b1111)).optimize();
			cache.set(index, shape);
		}
		return shape;
	}
}
