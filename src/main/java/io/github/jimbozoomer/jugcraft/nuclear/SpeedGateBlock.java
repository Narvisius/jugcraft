package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A sliding glass speed gate (Justin, October 8 2026, after the Grand Central fare gates): one lane, one way. The lane
 * block stands at the end you come in by ({@link #FACING} is the way through); the block ahead of it is left clear,
 * and a {@link SpeedGatePedestalBlock} stands either side of both (shared with the lane beside, when lanes stand side by
 * side). Two glass doors slide out of the pedestals along the line between the gate's two blocks and meet in the
 * middle; when someone walks up to them the way through, they slide back into their pedestals' slots and close
 * again behind them (SpeedGateBlockEntity). From the other side they stay shut.
 *
 * <p>The lane block draws its mat; its {@link #PART} values other than {@code lane} are only there so the client can
 * look up the models of the doors, the lights and the sign gantry over the lane (client/SpeedGateRenderer.java) and
 * the placement preview the whole gate. Models: tools/speed_gate.py.
 */
public class SpeedGateBlock extends HorizontalDirectionalBlock implements EntityBlock, StructureGhost.Source {
	public enum Part implements StringRepresentable {
		LANE, GHOST, DOOR_LEFT, DOOR_RIGHT, LEDS_WAIT, LEDS_GO, GANTRY, GANTRY_EAST;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	/** How far each door is (px) from the line between the gate's two blocks; see {@link #slotSign}. */
	public static final float SLOT = 2F;
	/** Each door's width (px), from its pedestal's face to the middle of the lane. */
	public static final float DOOR_WIDTH = 10F;

	/**
	 * Facing north: the mat (to pick the gate by) and, shut, the doors' barrier: round the doors where they run behind
	 * the line, in this block ({@link #ownSlot}), else as near the line as this block reaches (they are drawn 2 px into
	 * the block ahead).
	 */
	private static final Map<Direction, VoxelShape> MAT = Shapes.rotateHorizontal(Block.box(0, 0, 0, 16, 0.5, 16));
	private static final Map<Direction, VoxelShape> DOORS_OWN = Shapes.rotateHorizontal(Block.box(0, 0, SLOT - 0.8, 16, 24, SLOT + 0.8));
	private static final Map<Direction, VoxelShape> DOORS_AHEAD = Shapes.rotateHorizontal(Block.box(0, 0, 0, 16, 24, 0.8));
	private static final Map<Direction, VoxelShape> SHUT_OWN = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 0, 0, 16, 0.5, 16),
			Block.box(0, 3, SLOT - 0.8, 16, 24, SLOT + 0.8)));
	private static final Map<Direction, VoxelShape> SHUT_AHEAD = Shapes.rotateHorizontal(Shapes.or(Block.box(0, 0, 0, 16, 0.5, 16),
			Block.box(0, 3, 0, 16, 24, 0.8)));
	private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);

	public SpeedGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(PART, Part.LANE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, PART);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpeedGateBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != PlantInterior.SPEED_GATE_ENTITY) {
			return null;
		}
		return (BlockEntityTicker<T>) (BlockEntityTicker<SpeedGateBlockEntity>) (tickLevel, pos, tickState, gate) -> gate.serverTick((ServerLevel) tickLevel, tickState);
	}

	// ------------------------------------------------------------------ the gate's cells

	/** The block ahead of the lane block (the lane's second block, kept clear). */
	public static BlockPos exit(BlockPos lane, Direction facing) {
		return lane.relative(facing);
	}

	/** The four pedestal cells of a lane: left and right of its block and of the block ahead. */
	public static List<BlockPos> pedestals(BlockPos lane, Direction facing) {
		BlockPos ahead = exit(lane, facing);
		Direction left = facing.getCounterClockWise(), right = facing.getClockWise();
		return List.of(lane.relative(left), lane.relative(right), ahead.relative(left), ahead.relative(right));
	}

	/** The pedestal state for a cell of a lane's: lengthwise along the lane, and which half of it the cell is. */
	public static BlockState pedestalState(BlockPos cell, BlockPos lane, Direction facing) {
		BlockPos ahead = exit(lane, facing);
		Direction.Axis axis = facing.getAxis();
		int laneAt = axis == Direction.Axis.Z ? lane.getZ() : lane.getX();
		int aheadAt = axis == Direction.Axis.Z ? ahead.getZ() : ahead.getX();
		int at = axis == Direction.Axis.Z ? cell.getZ() : cell.getX();
		boolean higher = at == Math.max(laneAt, aheadAt);
		// Half b is drawn as the south half (axis z); turned for axis x, it is the west one (tools/speed_gate.py).
		boolean b = axis == Direction.Axis.Z ? higher : !higher;
		return PlantInterior.BLOCKS.get("speed_gate_pedestal").defaultBlockState().setValue(SpeedGatePedestalBlock.AXIS, axis)
				.setValue(SpeedGatePedestalBlock.HALF, b ? SpeedGatePedestalBlock.Half.B : SpeedGatePedestalBlock.Half.A);
	}

	/**
	 * Which side of the line this lane's doors run (+1 toward south or east, -1 toward north or west). Lanes side by
	 * side share a pedestal, so their doors must take its two slots: along a row of lanes on one line the sides
	 * alternate, starting at the row's west (or north) end with that lane's doors in its own block. So an entry lane and
	 * an exit lane side by side, as at the plants, both have theirs in their own blocks, wherever they stand.
	 */
	public static int slotSign(BlockGetter level, BlockPos lane, Direction facing) {
		Direction back = facing.getAxis() == Direction.Axis.Z ? Direction.WEST : Direction.NORTH;
		Block gate = PlantInterior.BLOCKS.get("speed_gate");
		BlockPos at = lane;
		Direction f = facing;
		int steps = 0;
		while (steps < 64) {
			// The lane sharing the pedestal on this one's west (north) side: two back, facing the same way, or two back
			// and one ahead, facing the other way.
			BlockPos same = at.relative(back, 2);
			BlockPos other = same.relative(f);
			BlockState s = level.getBlockState(same);
			BlockState o = level.getBlockState(other);
			if (s.is(gate) && s.getValue(FACING) == f) {
				at = same;
			} else if (o.is(gate) && o.getValue(FACING) == f.getOpposite()) {
				at = other;
				f = f.getOpposite();
			} else {
				break;
			}
			steps++;
		}
		int first = ownSide(f);
		return steps % 2 == 0 ? first : -first;
	}

	/** The side of the line a lane's own block is on (+1 south or east). */
	private static int ownSide(Direction facing) {
		return facing == Direction.NORTH || facing == Direction.WEST ? 1 : -1;
	}

	/**
	 * Whether this lane's doors run behind the line, in its own block, rather than 2 px into the block ahead: the
	 * north-facing models' +z (client/SpeedGateRenderer.java slides them there).
	 */
	public static boolean ownSlot(BlockGetter level, BlockPos lane, Direction facing) {
		return slotSign(level, lane, facing) == ownSide(facing);
	}

	private static boolean fits(LevelReader level, BlockPos lane, Direction facing, @Nullable BlockPlaceContext context, List<BlockPos> blocked) {
		BlockPos ahead = exit(lane, facing);
		BlockState there = level.getBlockState(ahead);
		if (!(context == null ? there.canBeReplaced() : there.canBeReplaced(context))) {
			blocked.add(ahead);
		}
		for (BlockPos cell : pedestals(lane, facing)) {
			BlockState at = level.getBlockState(cell);
			BlockState want = pedestalState(cell, lane, facing);
			boolean free = context == null ? at.canBeReplaced() : at.canBeReplaced(context);
			if (!free && !at.equals(want)) {
				blocked.add(cell);
			}
		}
		return blocked.isEmpty();
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		if (!fits(context.getLevel(), context.getClickedPos(), facing, context, new ArrayList<>())) {
			return null;
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			for (BlockPos cell : pedestals(pos, facing)) {
				if (!level.getBlockState(cell).is(PlantInterior.BLOCKS.get("speed_gate_pedestal"))) {
					level.setBlock(cell, pedestalState(cell, pos, facing), Block.UPDATE_ALL);
				}
			}
		}
	}

	/** The whole gate where its item would go (client/MbsPreview.java): the lane, its doors shut, the new pedestals. */
	@Override
	public StructureGhost ghost(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos at = context.getClickedPos();
		List<StructureGhost.Cell> cells = new ArrayList<>();
		List<BlockPos> blocked = new ArrayList<>();
		cells.add(new StructureGhost.Cell(at, defaultBlockState().setValue(FACING, facing).setValue(PART, Part.GHOST)));
		if (!context.canPlace()) {
			blocked.add(at);
		}
		fits(context.getLevel(), at, facing, context, blocked);
		for (BlockPos cell : pedestals(at, facing)) {
			if (!context.getLevel().getBlockState(cell).is(PlantInterior.BLOCKS.get("speed_gate_pedestal"))) {
				cells.add(new StructureGhost.Cell(cell, pedestalState(cell, at, facing)));
			}
		}
		return new StructureGhost(cells, blocked);
	}

	// ------------------------------------------------------------------ coming down whole

	/** Whether a pedestal still serves a lane other than {@code except} (a lane beside it on its other side). */
	static boolean pedestalServes(LevelReader level, BlockPos pedestal, @Nullable BlockPos except) {
		Block gate = PlantInterior.BLOCKS.get("speed_gate");
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (int along = -1; along <= 1; along++) {
				BlockPos lane = pedestal.relative(side);
				Direction axisDir = side.getClockWise();
				lane = lane.relative(axisDir, along);
				if (lane.equals(except)) {
					continue;
				}
				BlockState state = level.getBlockState(lane);
				if (state.is(gate) && pedestals(lane, state.getValue(FACING)).contains(pedestal)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Takes away a lane's pedestals no other lane uses. */
	private static void clearPedestals(Level level, BlockPos lane, Direction facing) {
		Block pedestal = PlantInterior.BLOCKS.get("speed_gate_pedestal");
		CLEARING.set(true);
		try {
			for (BlockPos cell : pedestals(lane, facing)) {
				if (level.getBlockState(cell).is(pedestal) && !pedestalServes(level, cell, lane)) {
					level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		} finally {
			CLEARING.set(false);
		}
	}

	static boolean clearing() {
		return CLEARING.get();
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			clearPedestals(level, pos, state.getValue(FACING));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However the lane block goes (an explosion, a command), its pedestals go with it unless another lane uses them. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (!CLEARING.get() && !level.getBlockState(pos).is(this)) {
			clearPedestals(level, pos, state.getValue(FACING));
		}
	}

	// ------------------------------------------------------------------ shape

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return state.getValue(OPEN) ? MAT.get(facing) : (ownSlot(level, pos, facing) ? SHUT_OWN : SHUT_AHEAD).get(facing);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return state.getValue(OPEN) ? Shapes.empty() : (ownSlot(level, pos, facing) ? DOORS_OWN : DOORS_AHEAD).get(facing);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}

	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
