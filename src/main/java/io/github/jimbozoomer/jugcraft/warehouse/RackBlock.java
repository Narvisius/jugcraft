package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One pallet place of selective pallet racking (FACING: the way a drone in the aisle looks into it; its front, with
 * the beams' labels, faces the aisle). Places side by side and stacked join into a rack: LEFT and RIGHT put the blue
 * uprights at a bay's ends (bays of two places, along the run), LEVEL is ground (its pallet on the floor), first (the
 * beams over the ground place, both labels) or upper, TOP caps the uprights. LOAD is the pallet standing on it, by its
 * look (PalletLoad.look: 0 none), part of the block's own model so a full warehouse costs nothing to draw; what is on
 * it is in its block entity.
 */
public class RackBlock extends BaseEntityBlock implements WarehouseBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final EnumProperty<RackLevel> LEVEL = EnumProperty.create("level", RackLevel.class);
	public static final IntegerProperty LOAD = IntegerProperty.create("load", 0, 7 * PalletLoad.MAX_CASES + 4 * Drums.PER_PALLET);
	/** Part W3: an in-rack sprinkler line fitted to the place (it works with the building's sprinkler system). */
	public static final BooleanProperty SPRINKLER = BooleanProperty.create("sprinkler");

	/** Where a place stands in its column. */
	public enum RackLevel implements StringRepresentable {
		GROUND("ground"), FIRST("first"), UPPER("upper");

		private final String name;

		RackLevel(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public RackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, true).setValue(RIGHT, true)
				.setValue(TOP, true).setValue(LEVEL, RackLevel.GROUND).setValue(LOAD, 0).setValue(SPRINKLER, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEFT, RIGHT, TOP, LEVEL, LOAD, SPRINKLER);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RackBlockEntity(pos, state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return joined(defaultBlockState().setValue(FACING, context.getHorizontalDirection()), context.getLevel(), context.getClickedPos());
	}

	/** The state with its joins to the places round it worked out. */
	static BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		Direction right = facing.getClockWise();
		boolean leftRack = same(level.getBlockState(pos.relative(right.getOpposite())), facing);
		boolean rightRack = same(level.getBlockState(pos.relative(right)), facing);
		// Bays of two along the run, by where the place lies (so each place can tell from its neighbours alone).
		int along = right.getAxis().choose(pos.getX(), pos.getY(), pos.getZ()) * right.getAxisDirection().getStep();
		BlockState below = level.getBlockState(pos.below());
		RackLevel lv = !same(below, facing) ? RackLevel.GROUND : below.getValue(LEVEL) == RackLevel.GROUND ? RackLevel.FIRST : RackLevel.UPPER;
		return state.setValue(LEFT, !leftRack || Math.floorMod(along, 2) == 0).setValue(RIGHT, !rightRack)
				.setValue(LEVEL, lv).setValue(TOP, !same(level.getBlockState(pos.above()), facing));
	}

	private static boolean same(BlockState state, Direction facing) {
		return state.getBlock() instanceof RackBlock && state.getValue(FACING) == facing;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return joined(state, level, pos);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			Warehouses.changed(level, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Warehouses.changed(level, pos);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		VoxelShape frame = WarehouseLayout.get().rack(state.getValue(LEFT), state.getValue(RIGHT), state.getValue(LEVEL).getSerializedName(),
				state.getValue(TOP), facing);
		if (state.getValue(LOAD) == 0) {
			return frame;
		}
		double y0 = state.getValue(LEVEL) == RackLevel.GROUND ? 0 : 1.6;
		return Shapes.or(frame, Block.box(1, y0, 1, 15, Math.min(16, y0 + 13), 15));
	}

	/** Part W5: a comparator by a place reads how full it is (0 empty, 15 a full pallet). */
	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		if (!(level.getBlockEntity(pos) instanceof RackBlockEntity rack) || rack.load() == null || rack.load().isEmpty()) {
			return 0;
		}
		return Math.min(15, 1 + 14 * rack.load().size() / PalletLoad.MAX_CASES);
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	/** Using a place shows what is on its pallet (players never take from the racks: everything goes through the terminal). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RackBlockEntity rack) {
			String label = rack.label().isEmpty() ? "?" : rack.label();
			player.sendOverlayMessage(rack.load() == null ? Component.translatable("message.jugcraft.warehouse.empty_place", label)
					: Component.translatable("message.jugcraft.warehouse.pallet_contents", label, rack.load().describe()));
		}
		return InteractionResult.SUCCESS;
	}
}
