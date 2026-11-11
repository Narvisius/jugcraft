package io.github.jimbozoomer.jugcraft.exterior;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A steel stair at a real angle (Justin, 2026-10-17: "make the stairs look actually real"; "two blocks wide, the rails
 * actually keep you from walking through them, and that they work like stairs"): riser 1/6 block, tread 1/4 (167 and
 * 250 mm: 33.7 degrees), pan treads with yellow nosings (tools/exterior_kit.py stair_cell). A flight rises two blocks
 * in three; one section is that, two blocks wide: eight cells, PART 0 (column 0, lower), 1 (column 1, lower), 2
 * (column 1, upper), 3 (column 2, upper) on each SIDE (left and right as you walk up), climbing toward FACING.
 *
 * <p>The section is placed and broken whole: its first cell (part 0, left) puts down the other seven when it is
 * placed (by hand, or by a builder drone from a blueprint, which lists only that cell), and breaking any cell takes
 * the section away (one item back). Each tread is a step 1/6 high under its top, so you walk up as on any stairs. On a
 * side with no wall beside it the section has a guard rail: its collision stands 1.5 blocks over that side of each cell
 * and in a {@link StairGuardBlock} over the top of each column, so the rail can't be walked through or jumped over.
 * The stringers and handrails (at 33.7 degrees, which block models can't turn to) are drawn by the part 0 cells'
 * {@link StairBlockEntity} (client/exterior/StairRenderer.java).
 */
public class SteelStairBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
	public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
	/** Each part's column (along the flight) and level (up). */
	public static final int[][] CELLS = {{0, 0}, {1, 0}, {1, 1}, {2, 1}};
	/** A tread's rise and run (px). */
	public static final double RISE = 16.0 / 6.0, RUN = 4.0;
	/** The guard rail's collision: how high over the cell, how thick at its side (px). */
	static final double GUARD_H = 24.0, GUARD_T = 1.0;
	private static final Map<Direction, VoxelShape[]> TREADS = new EnumMap<>(Direction.class);
	private static final Map<Direction, VoxelShape[]> GUARDS = new EnumMap<>(Direction.class);

	public enum Side implements StringRepresentable {
		LEFT("left"), RIGHT("right");

		private final String name;

		Side(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	static {
		for (Direction f : Direction.Plane.HORIZONTAL) {
			VoxelShape[] treads = new VoxelShape[4];
			for (int p = 0; p < 4; p++) {
				treads[p] = ExteriorShapes.turned(f, treadBoxes(p));
			}
			TREADS.put(f, treads);
			GUARDS.put(f, new VoxelShape[] {ExteriorShapes.turned(f, new double[] {0, 0, 0, GUARD_T, GUARD_H, 16}),
					ExteriorShapes.turned(f, new double[] {16 - GUARD_T, 0, 0, 16, GUARD_H, 16})});
		}
	}

	/** The cell's treads as collision boxes (px): each a step down to the riser under it, open beneath. */
	static double[][] treadBoxes(int part) {
		int c = CELLS[part][0], lv = CELLS[part][1];
		List<double[]> out = new ArrayList<>();
		for (int k = 0; k < 4; k++) {
			double top = (4 * c + k + 1) * RISE - lv * 16.0;
			if (top <= 0 || top > 16.0 + 1e-6) {
				continue;
			}
			double z0 = 16.0 - (k + 1) * RUN, z1 = 16.0 - k * RUN;
			out.add(new double[] {0, Math.max(0, top - 4.0), z0, 16, Math.min(16, top), z1});
		}
		return out.toArray(new double[0][]);
	}

	public SteelStairBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0).setValue(SIDE, Side.LEFT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART, SIDE);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? new StairBlockEntity(pos, state) : null;
	}

	// ------------------------------------------------------------------------------------------- the section
	/** The section's first cell (part 0, left) from any of its cells. */
	public static BlockPos origin(BlockPos pos, BlockState state) {
		Direction f = state.getValue(FACING);
		int[] cell = CELLS[state.getValue(PART)];
		BlockPos o = pos.relative(f, -cell[0]).below(cell[1]);
		return state.getValue(SIDE) == Side.RIGHT ? o.relative(f.getCounterClockWise()) : o;
	}

	/** A cell of the section starting at origin. */
	public static BlockPos cell(BlockPos origin, Direction facing, int part, Side side) {
		BlockPos p = origin.relative(facing, CELLS[part][0]).above(CELLS[part][1]);
		return side == Side.RIGHT ? p.relative(facing.getClockWise()) : p;
	}

	public BlockState state(Direction facing, int part, Side side) {
		return defaultBlockState().setValue(FACING, facing).setValue(PART, part).setValue(SIDE, side);
	}

	/** Whether a section climbing toward facing fits with its first cell at origin (that cell itself may be taken). */
	static boolean fits(LevelReader level, BlockPos origin, Direction facing) {
		for (int p = 0; p < 4; p++) {
			for (Side s : Side.values()) {
				BlockPos at = cell(origin, facing, p, s);
				if (!(p == 0 && s == Side.LEFT) && !level.getBlockState(at).canBeReplaced()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction f = context.getHorizontalDirection();
		if (!fits(context.getLevel(), context.getClickedPos(), f)) {
			if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
				context.getPlayer().sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.jugcraft.exterior.stair_blocked"));
			}
			return null;
		}
		return state(f, 0, Side.LEFT);
	}

	/** The first cell puts down the rest of its section and its guard rails. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (level.isClientSide() || oldState.is(this) || state.getValue(PART) != 0 || state.getValue(SIDE) != Side.LEFT) {
			return;
		}
		Direction f = state.getValue(FACING);
		for (int p = 0; p < 4; p++) {
			for (Side s : Side.values()) {
				BlockPos at = cell(pos, f, p, s);
				if (!at.equals(pos) && level.getBlockState(at).canBeReplaced()) {
					level.setBlock(at, state(f, p, s), Block.UPDATE_ALL);
				}
			}
		}
		guards(level, pos, f);
	}

	/** Over the top cell of each column on an open side (no wall beside it), the guard rail's upper collision. */
	static void guards(Level level, BlockPos origin, Direction f) {
		for (Side s : Side.values()) {
			for (int p : new int[] {0, 2, 3}) {
				BlockPos at = cell(origin, f, p, s);
				if (wallBeside(level, at, f, s)) {
					continue;
				}
				BlockPos up = at.above();
				if (level.getBlockState(up).isAir()) {
					level.setBlock(up, JugcraftExterior.STAIR_GUARD.defaultBlockState().setValue(StairGuardBlock.FACING, f)
							.setValue(StairGuardBlock.SIDE, s), Block.UPDATE_ALL);
				}
			}
		}
	}

	/** Whether the cell's outer side (its side's way out from the flight) is against a wall. */
	public static boolean wallBeside(BlockGetter level, BlockPos pos, Direction facing, Side side) {
		Direction out = side == Side.LEFT ? facing.getCounterClockWise() : facing.getClockWise();
		BlockPos n = pos.relative(out);
		BlockState there = level.getBlockState(n);
		if (there.getBlock() instanceof SteelStairBlock || there.getBlock() instanceof StairGuardBlock) {
			return false;
		}
		return there.isFaceSturdy(level, n, out.getOpposite());
	}

	/** Breaking a cell takes its whole section (and its guard rails) away; the cell broken gives the item back. */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			removeSection(level, pos, state);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		removeSection(level, pos, state);
	}

	private void removeSection(Level level, BlockPos pos, BlockState state) {
		Direction f = state.getValue(FACING);
		BlockPos origin = origin(pos, state);
		for (int p = 0; p < 4; p++) {
			for (Side s : Side.values()) {
				BlockPos at = cell(origin, f, p, s);
				BlockState there = level.getBlockState(at);
				if (!at.equals(pos) && there.is(this) && there.getValue(FACING) == f && there.getValue(PART) == p && there.getValue(SIDE) == s) {
					level.removeBlock(at, false);
				}
				if (level.getBlockState(at.above()).is(JugcraftExterior.STAIR_GUARD)) {
					level.removeBlock(at.above(), false);
				}
			}
		}
	}

	// ------------------------------------------------------------------------------------------- shapes
	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return TREADS.get(state.getValue(FACING))[state.getValue(PART)];
	}

	/** The treads, and the guard rail's collision over an open side. */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction f = state.getValue(FACING);
		VoxelShape treads = TREADS.get(f)[state.getValue(PART)];
		Side s = state.getValue(SIDE);
		if (wallBeside(level, pos, f, s)) {
			return treads;
		}
		return Shapes.or(treads, GUARDS.get(f)[s.ordinal()]);
	}

	/** Not a support for anything (and found without looking at the neighbours: two flights side by side ask each other). */
	@Override
	protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
		return TREADS.get(state.getValue(FACING))[state.getValue(PART)];
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}
}
