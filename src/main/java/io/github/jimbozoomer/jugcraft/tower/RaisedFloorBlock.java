package io.github.jimbozoomer.jugcraft.tower;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Raised Floor (work list item 6, with the Tower Control Console): a carpeted access floor three pixels high. Where
 * it steps down to the floor beside it (nothing that carries the raised floor on, and no wall), that side shows an
 * aluminium nosing with an amber step light under it. The console's floor band and a Console Chair set on a raised
 * floor carry it on; a Console Chair placed on a raised floor takes its place.
 */
public final class RaisedFloorBlock extends Block {
	/** Whether the floor steps down on that side (shows the nosing and the step light there). */
	public static final BooleanProperty NORTH = BooleanProperty.create("edge_north"), EAST = BooleanProperty.create("edge_east"),
			SOUTH = BooleanProperty.create("edge_south"), WEST = BooleanProperty.create("edge_west");
	public static final Map<Direction, BooleanProperty> EDGES = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH,
			Direction.WEST, WEST);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);

	public RaisedFloorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST);
	}

	/** Blocks that carry the raised floor on: more raised floor, a chair on it, the console's floor band. */
	public static boolean carriesFloor(BlockState state) {
		return state.getBlock() instanceof RaisedFloorBlock || state.getBlock() instanceof TowerControlConsoleBlock
				|| state.getBlock() instanceof ConsoleChairBlock && state.getValue(ConsoleChairBlock.PLATFORM);
	}

	/** Whether the raised floor at {@code pos} steps down on its {@code side}. */
	public static boolean stepsDown(BlockGetter level, BlockPos pos, Direction side) {
		BlockPos next = pos.relative(side);
		BlockState there = level.getBlockState(next);
		return !carriesFloor(there) && !there.isFaceSturdy(level, next, side.getOpposite());
	}

	public BlockState withEdges(BlockGetter level, BlockPos pos, BlockState state) {
		for (Map.Entry<Direction, BooleanProperty> e : EDGES.entrySet()) {
			state = state.setValue(e.getValue(), stepsDown(level, pos, e.getKey()));
		}
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withEdges(context.getLevel(), context.getClickedPos(), defaultBlockState());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BooleanProperty edge = EDGES.get(direction);
		return edge == null ? state : state.setValue(edge, stepsDown(level, pos, direction));
	}

	/** A Console Chair placed on the floor takes the floor's cell (and carries the floor on). */
	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return context.getItemInHand().is(JugcraftTower.BLOCKS.get("console_chair").asItem()) || super.canBeReplaced(state, context);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		BlockState out = state;
		for (Map.Entry<Direction, BooleanProperty> e : EDGES.entrySet()) {
			out = out.setValue(EDGES.get(rotation.rotate(e.getKey())), state.getValue(e.getValue()));
		}
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		BlockState out = state;
		for (Map.Entry<Direction, BooleanProperty> e : EDGES.entrySet()) {
			out = out.setValue(EDGES.get(mirror.mirror(e.getKey())), state.getValue(e.getValue()));
		}
		return out;
	}
}
