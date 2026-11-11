package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Steel Girder, joined (work list item 12, approved by Justin on 23 October 2026): a wide-flange section, and a run
 * of girders along one axis reads as one member. Where the run carries on past an end ({@link #JOIN_LO} below / west /
 * north of it, {@link #JOIN_HI} the other way: the next block along its axis is a girder on the same axis) the section
 * runs straight on; where it ends it ends on a plate: a column on a base plate with anchor bolts, under a cap plate; a
 * beam on bolted end plates (tools/joined_girders.py). Collision and neighbour occlusion follow the section and plates.
 */
public final class TowerGirderBlock extends RotatedPillarBlock {
	public static final BooleanProperty JOIN_LO = BooleanProperty.create("join_lo");
	public static final BooleanProperty JOIN_HI = BooleanProperty.create("join_hi");
	/** The section in a column's frame (pixels; tools/joined_girders.py): flanges square to z, the web square to x. */
	private static final double Z0 = 1, Z1 = 15, X0 = 2.4, X1 = 13.6, FLANGE = 1.6, W0 = 7.45, W1 = 8.55, PLATE = 1;
	private static final double[] BASE = {0, 16, 0, 16}, CAP = {1.9, 14.1, 0.65, 15.35}, END = {1.8, 14.2, 0.5, 15.5};
	/** By axis (y, x, z) and plates (none, low, high, both). */
	private static final VoxelShape[][] SHAPES = new VoxelShape[3][4];

	public TowerGirderBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(defaultBlockState().setValue(JOIN_LO, false).setValue(JOIN_HI, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(JOIN_LO, JOIN_HI);
	}

	/** Whether {@code state} is a girder on {@code axis}. */
	private static boolean girder(BlockState state, Direction.Axis axis) {
		return state.getBlock() instanceof TowerGirderBlock && state.getValue(AXIS) == axis;
	}

	/** {@code state} with its joins worked out from its neighbours at {@code pos}. */
	public static BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
		Direction.Axis axis = state.getValue(AXIS);
		Direction hi = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		return state.setValue(JOIN_LO, girder(level.getBlockState(pos.relative(hi.getOpposite())), axis))
				.setValue(JOIN_HI, girder(level.getBlockState(pos.relative(hi)), axis));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : joined(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		Direction.Axis axis = state.getValue(AXIS);
		if (direction.getAxis() != axis) {
			return state;
		}
		boolean join = girder(neighbor, axis);
		return state.setValue(direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? JOIN_HI : JOIN_LO, join);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction.Axis axis = state.getValue(AXIS);
		int a = axis == Direction.Axis.Y ? 0 : axis == Direction.Axis.X ? 1 : 2;
		int p = (state.getValue(JOIN_LO) ? 0 : 1) | (state.getValue(JOIN_HI) ? 0 : 2);
		VoxelShape cached = SHAPES[a][p];
		if (cached == null) {
			cached = make(a, (p & 1) != 0, (p & 2) != 0);
			SHAPES[a][p] = cached;
		}
		return cached;
	}

	/** The section and its plates in a column's frame, turned to the axis as tools/joined_girders.py turns its models. */
	private static VoxelShape make(int axis, boolean lo, boolean hi) {
		double y0 = lo ? PLATE : 0, y1 = hi ? 16 - PLATE : 16;
		double[][] boxes = {{X0, y0, Z0, X1, y1, Z0 + FLANGE}, {X0, y0, Z1 - FLANGE, X1, y1, Z1}, {W0, y0, Z0 + FLANGE, W1, y1, Z1 - FLANGE},
				null, null};
		double[] low = axis == 0 ? BASE : END, high = axis == 0 ? CAP : END;
		if (lo) {
			boxes[3] = new double[] {low[0], 0, low[2], low[1], PLATE, low[3]};
		}
		if (hi) {
			boxes[4] = new double[] {high[0], 16 - PLATE, high[2], high[1], 16, high[3]};
		}
		VoxelShape shape = Shapes.empty();
		for (double[] b : boxes) {
			if (b == null) {
				continue;
			}
			// a column's (x, y, z) -> along z: (x, z, y); along x: (y, z, x)
			double[] t = switch (axis) {
				case 1 -> new double[] {b[1], b[2], b[0], b[4], b[5], b[3]};
				case 2 -> new double[] {b[0], b[2], b[1], b[3], b[5], b[4]};
				default -> b;
			};
			shape = Shapes.or(shape, Block.box(t[0], t[1], t[2], t[3], t[4], t[5]));
		}
		return shape.optimize();
	}
}
