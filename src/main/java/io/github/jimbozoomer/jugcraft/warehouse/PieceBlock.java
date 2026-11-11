package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.placement.StructureGhost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A warehouse piece bigger than a block (WarehousePiece: the Intake Case Packer, the Palletizing Station, the Control
 * Terminal, the Goods In panel), placed from one item and broken as one, like the plant's multi-block furniture. Every
 * block is a part (PART); only the origin part holds the block entity, and using any part uses the piece.
 */
public class PieceBlock extends BaseEntityBlock implements WarehouseBlock, StructureGhost.Source {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final Map<Integer, IntegerProperty> PROPERTIES = new HashMap<>();
	private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> false);
	private static int building;
	public final WarehousePiece piece;
	private final IntegerProperty part;

	static IntegerProperty property(int count) {
		return PROPERTIES.computeIfAbsent(Math.max(2, count), n -> IntegerProperty.create("part", 0, n - 1));
	}

	private static Properties prepare(Properties properties, WarehousePiece piece) {
		building = piece.parts();
		return properties;
	}

	public PieceBlock(WarehousePiece piece, Properties properties) {
		super(prepare(properties, piece));
		this.piece = piece;
		this.part = property(piece.parts());
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(part, piece.origin()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, property(building));
	}

	public IntegerProperty partProperty() {
		return part;
	}

	public int part(BlockState state) {
		return state.getValue(part);
	}

	public boolean isOrigin(BlockState state) {
		return state.getValue(part) == piece.origin();
	}

	/** The origin block of the piece this block belongs to. */
	public BlockPos origin(BlockPos pos, BlockState state) {
		return pos.subtract(piece.offset(state.getValue(FACING), state.getValue(part)));
	}

	/** The block entity of the piece a block of it belongs to, or null (not loaded, or not this piece). */
	public static @Nullable BlockEntity entity(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof PieceBlock block)) {
			return null;
		}
		BlockPos origin = block.origin(pos, state);
		return level.isLoaded(origin) ? level.getBlockEntity(origin) : null;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (!isOrigin(state)) {
			return null;
		}
		return switch (piece) {
			case INTAKE -> new IntakeBlockEntity(pos, state);
			case STATION -> new StationBlockEntity(pos, state);
			case TERMINAL -> new TerminalBlockEntity(pos, state);
			case GOODS_IN -> new GoodsInBlockEntity(pos, state);
			case READY_STORAGE -> new ReadyStorageBlockEntity(pos, state);
			case READY_STORAGE_EXT -> null;
			case GOODS_OUT -> new GoodsOutBlockEntity(pos, state);
			case LIFT_LANDING -> new LiftBlockEntity(pos, state);
			case LIFT_SHAFT, LIFT_DRIVE -> null;
			case DRUM_FILLER -> new FillerBlockEntity(pos, state);
			case DRUM_EMPTIER -> new EmptierBlockEntity(pos, state);
			case LIQUIDS_IN -> new LiquidsInBlockEntity(pos, state);
			case LIQUIDS_OUT -> new LiquidsOutBlockEntity(pos, state);
			case SWITCHBOARD -> new SwitchboardBlockEntity(pos, state);
			case STATUS_BOARD -> new StatusBoardBlockEntity(pos, state);
			case SIGNAL_BOX -> new SignalBoxBlockEntity(pos, state);
		};
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || !isOrigin(state)) {
			return null;
		}
		return switch (piece) {
			case INTAKE -> createTickerHelper(type, JugcraftWarehouse.INTAKE_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case STATION -> createTickerHelper(type, JugcraftWarehouse.STATION_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case TERMINAL -> createTickerHelper(type, JugcraftWarehouse.TERMINAL_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case GOODS_IN -> createTickerHelper(type, JugcraftWarehouse.GOODS_IN_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case READY_STORAGE -> createTickerHelper(type, JugcraftWarehouse.READY_STORAGE_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case READY_STORAGE_EXT -> null;
			case GOODS_OUT -> createTickerHelper(type, JugcraftWarehouse.GOODS_OUT_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case LIFT_LANDING -> createTickerHelper(type, JugcraftWarehouse.LIFT_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case LIFT_SHAFT, LIFT_DRIVE -> null;
			case DRUM_FILLER -> createTickerHelper(type, JugcraftWarehouse.FILLER_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case DRUM_EMPTIER -> createTickerHelper(type, JugcraftWarehouse.EMPTIER_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case LIQUIDS_IN -> createTickerHelper(type, JugcraftWarehouse.LIQUIDS_IN_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case LIQUIDS_OUT -> createTickerHelper(type, JugcraftWarehouse.LIQUIDS_OUT_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case SWITCHBOARD -> createTickerHelper(type, JugcraftWarehouse.SWITCHBOARD_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case STATUS_BOARD -> createTickerHelper(type, JugcraftWarehouse.STATUS_BOARD_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
			case SIGNAL_BOX -> createTickerHelper(type, JugcraftWarehouse.SIGNAL_BOX_ENTITY, (l, p, s, e) -> e.serverTick((ServerLevel) l));
		};
	}

	// ---- part W5: comparators (AnalogSource pieces: any of their blocks) and the Signal Box's redstone

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return switch (piece) {
			case TERMINAL, READY_STORAGE, DRUM_FILLER, DRUM_EMPTIER, SWITCHBOARD -> true;
			default -> false;
		};
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(origin(pos, state)) instanceof AnalogSource source ? Math.max(0, Math.min(15, source.analog())) : 0;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return piece == WarehousePiece.SIGNAL_BOX;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return piece == WarehousePiece.SIGNAL_BOX && level.getBlockEntity(origin(pos, state)) instanceof SignalBoxBlockEntity box
				&& box.mode() == SignalBoxBlockEntity.OUTPUT ? box.output() : 0;
	}

	/** Part W5: a piece's comparator value changed: the comparators by any of its blocks look again. */
	static void analogChanged(Level level, BlockPos origin, BlockState state) {
		if (!(state.getBlock() instanceof PieceBlock block)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int p = 0; p < block.piece.parts(); p++) {
			level.updateNeighbourForOutputSignal(origin.offset(block.piece.offset(facing, p)), block);
		}
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos at = context.getClickedPos();
		for (int p = 0; p < piece.parts(); p++) {
			BlockPos cell = at.offset(piece.offset(facing, p));
			if (p != piece.origin() && (context.getLevel().isOutsideBuildHeight(cell) || !context.getLevel().getBlockState(cell).canBeReplaced(context))) {
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide()) {
			Direction facing = state.getValue(FACING);
			for (int p = 0; p < piece.parts(); p++) {
				if (p != piece.origin()) {
					level.setBlock(pos.offset(piece.offset(facing, p)), state.setValue(part, p), Block.UPDATE_ALL);
				}
			}
			Warehouses.changed(level, pos);
			// Part W5: a terminal is its placer's (PERSONAL till they say otherwise).
			if (piece == WarehousePiece.TERMINAL && placer instanceof Player player && level.getBlockEntity(pos) instanceof TerminalBlockEntity t
					&& t.owner() == null) {
				t.setOwner(player);
			}
			if (piece == WarehousePiece.SIGNAL_BOX && placer instanceof Player player && level.getBlockEntity(pos) instanceof SignalBoxBlockEntity box) {
				box.setOwner(player.getUUID());
			}
		}
	}

	/**
	 * A piece's origin put in on its own (a blueprint lists only the origin, so a drone places that and the piece costs
	 * one item; tools/warehouse_bp.py placed_whole): the rest of the piece comes with it, where there is room.
	 */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (piece == WarehousePiece.SWITCHBOARD) {
			io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.invalidate(level);
		}
		if (level.isClientSide() || CLEARING.get() || !isOrigin(state) || oldState.is(this)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int p = 0; p < piece.parts(); p++) {
			if (p == piece.origin()) {
				continue;
			}
			BlockPos at = pos.offset(piece.offset(facing, p));
			BlockState there = level.getBlockState(at);
			if (!level.isOutsideBuildHeight(at) && there.canBeReplaced() && !(there.is(this) && there.getValue(FACING) == facing && there.getValue(part) == p)) {
				level.setBlock(at, state.setValue(part, p), Block.UPDATE_ALL);
			}
		}
	}

	@Override
	public StructureGhost ghost(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos at = context.getClickedPos();
		BlockState state = defaultBlockState().setValue(FACING, facing);
		List<StructureGhost.Cell> cells = new ArrayList<>();
		List<BlockPos> blocked = new ArrayList<>();
		for (int p = 0; p < piece.parts(); p++) {
			BlockPos cell = at.offset(piece.offset(facing, p));
			cells.add(new StructureGhost.Cell(cell, state.setValue(part, p)));
			boolean free = p == piece.origin() ? context.canPlace() : context.getLevel().getBlockState(cell).canBeReplaced(context);
			if (!free || context.getLevel().isOutsideBuildHeight(cell)) {
				blocked.add(cell);
			}
		}
		return new StructureGhost(cells, blocked);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide()) {
			// The piece's block entity lets go of what it holds first (the intake's bay, the station's pallets).
			if (level.getBlockEntity(origin(pos, state)) instanceof PieceEntity entity) {
				entity.dropContents();
			}
			clear(level, pos, state);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	private void clear(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		BlockPos origin = origin(pos, state);
		CLEARING.set(true);
		try {
			for (int p = 0; p < piece.parts(); p++) {
				BlockPos at = origin.offset(piece.offset(facing, p));
				BlockState there = level.getBlockState(at);
				if (!at.equals(pos) && there.is(this) && there.getValue(FACING) == facing && there.getValue(part) == p) {
					level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
				}
			}
		} finally {
			CLEARING.set(false);
		}
	}

	/** However a part goes (an explosion, a command), the rest of the piece goes with it: one piece, one item. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Warehouses.changed(level, pos);
		if (piece == WarehousePiece.SWITCHBOARD) {
			io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.invalidate(level);
		}
		if (CLEARING.get() || level.getBlockState(pos).is(this)) {
			return;
		}
		clear(level, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return WarehouseLayout.get().piece(piece.id, state.getValue(FACING), state.getValue(part));
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return use(state, level, pos, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		InteractionResult result = use(state, level, pos, player, stack, hand);
		return result == InteractionResult.PASS ? InteractionResult.TRY_WITH_EMPTY_HAND : result;
	}

	private InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack, InteractionHand hand) {
		BlockPos origin = origin(pos, state);
		if (!(level.getBlockEntity(origin) instanceof PieceEntity entity)) {
			return InteractionResult.PASS;
		}
		// Part W5: a warehouse is its owner's (PERSONAL) or their party's (PARTY); the Status Board shows to anyone.
		if (!level.isClientSide() && piece != WarehousePiece.STATUS_BOARD) {
			TerminalBlockEntity t = piece == WarehousePiece.SWITCHBOARD || piece == WarehousePiece.SIGNAL_BOX ? Warehouses.terminalNear(level, origin)
					: Warehouses.terminalOf(level, origin);
			if (t == null && entity instanceof TerminalBlockEntity self) {
				t = self;
			}
			if (t != null && !t.mayUse(player)) {
				t.deny(player);
				return InteractionResult.SUCCESS;
			}
		}
		return entity.use(level, pos, player, stack, hand);
	}
}
