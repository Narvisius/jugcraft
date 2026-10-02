package io.github.jimbozoomer.jugcraft.fluid;

import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The tank gauge (batch 20): a sight-glass panel hung on the side of anything that holds fluid (a tinplate or glass
 * tank and the tanks joined to it, a steel tank, gas holder, flow battery or machine). Its glass shows how full the
 * block behind it is in eighths, read twice a second; right-click it to read the fluid and amount, and a comparator
 * reads it like the tank. It holds nothing and changes nothing.
 */
public class TankGaugeBlock extends Block {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 8);
	/** Ticks between readings. */
	public static final int INTERVAL = 10;
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Block.box(3, 1, 14, 13, 15, 16),
			Direction.SOUTH, Block.box(3, 1, 0, 13, 15, 2),
			Direction.WEST, Block.box(14, 1, 3, 16, 15, 13),
			Direction.EAST, Block.box(0, 1, 3, 2, 15, 13));

	public TankGaugeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEVEL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LEVEL);
	}

	/** Faces the player, on the block it was placed against (or behind it, placed on top of something). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** The fluid storage the gauge is hung on, or null. */
	public static Storage<FluidVariant> watched(Level level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(FACING);
		return FluidStorage.SIDED.find(level, pos.relative(facing.getOpposite()), facing);
	}

	/** Amount and capacity (droplets) and the first fluid found in a storage, summed over all its tanks. */
	public record Reading(FluidVariant fluid, long amount, long capacity) {
		public static final Reading NONE = new Reading(FluidVariant.blank(), 0, 0);

		/** Eighths full: 0 only when empty, 8 only when full. */
		public int eighths() {
			if (amount <= 0 || capacity <= 0) {
				return 0;
			}
			if (amount >= capacity) {
				return 8;
			}
			return (int) Math.max(1, Math.min(7, amount * 8 / capacity));
		}
	}

	public static Reading read(Storage<FluidVariant> storage) {
		if (storage == null) {
			return Reading.NONE;
		}
		FluidVariant fluid = FluidVariant.blank();
		long amount = 0;
		long capacity = 0;
		for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
			if (fluid.isBlank()) {
				fluid = view.getResource();
			}
		}
		for (StorageView<FluidVariant> view : storage) {
			amount += view.getAmount();
			capacity += view.getCapacity();
		}
		return new Reading(fluid, amount, capacity);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int eighths = read(watched(level, pos, state)).eighths();
		if (state.getValue(LEVEL) != eighths) {
			level.setBlock(pos, state.setValue(LEVEL, eighths), Block.UPDATE_ALL);
		}
		level.scheduleTick(pos, this, INTERVAL);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			Reading reading = read(watched(level, pos, state));
			player.sendOverlayMessage(FluidTankBlock.describe(reading.fluid(), reading.amount(), reading.capacity()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		Reading reading = read(watched(level, pos, state));
		return reading.capacity() <= 0 || reading.amount() <= 0 ? 0
				: (int) (1 + reading.amount() * 14 / reading.capacity());
	}
}
