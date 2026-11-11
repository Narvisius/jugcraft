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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A Neon Floor Line (work list item 11, made real 3D neon after Justin's walk through: tools/neon_3d.py): the floor's
 * own block with a lined channel in its top and a round glass neon tube in it, joined to the floor lines beside it
 * ({@link #NORTH} ...): straight on, round a corner (the tube bends), a tee, a cross; closed at a run's end; on its own a
 * short dash. It lets its neighbours draw their faces against it (no occlusion), so a channel's end is closed by them.
 * {@link Phased}: one of a TowerPanelBlock material (concrete, steel), its texture's quadrant by position as theirs.
 */
public class NeonFloorBlock extends Block {
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	private static final Map<Direction, BooleanProperty> JOINS = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH,
			Direction.WEST, WEST);

	public NeonFloorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST);
	}

	public static BooleanProperty join(Direction direction) {
		return JOINS.get(direction);
	}

	/** The state joined to the floor lines beside {@code pos}. */
	public static BlockState shaped(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction d : Direction.Plane.HORIZONTAL) {
			state = state.setValue(JOINS.get(d), level.getBlockState(pos.relative(d)).getBlock() instanceof NeonFloorBlock);
		}
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return shaped(TowerPanelBlock.at(defaultBlockState(), context.getClickedPos()), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return direction.getAxis().isHorizontal() ? state.setValue(JOINS.get(direction), neighborState.getBlock() instanceof NeonFloorBlock) : state;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			out = out.setValue(JOINS.get(rotation.rotate(d)), state.getValue(JOINS.get(d)));
		}
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			out = out.setValue(JOINS.get(mirror.mirror(d)), state.getValue(JOINS.get(d)));
		}
		return out;
	}

	/** A floor line of a phased material. */
	public static final class Phased extends NeonFloorBlock {
		public Phased(Properties properties) {
			super(properties);
			registerDefaultState(defaultBlockState().setValue(TowerPanelBlock.PHASE, 0));
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			super.createBlockStateDefinition(builder);
			builder.add(TowerPanelBlock.PHASE);
		}
	}
}
