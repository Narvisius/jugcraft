package io.github.jimbozoomer.jugcraft.control;

import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A sensor (batch 36): a small plate mounted on a tank, battery, machine or chest that reads how full it is. It gives
 * a redstone signal like a comparator's (0 empty, 1-15 by fill), and reports the exact percentage to logic controllers
 * on its channel ({@link Channels}). It reads energy if the block stores any, else fluids, else items, and checks every
 * {@link #INTERVAL} ticks.
 */
public class SensorBlock extends Block implements DataConnectable {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	public static final int INTERVAL = 20;
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.values()) {
			// Facing north the plate sits against the south face, on the block it reads.
			SHAPES.put(facing, Shapes.or(box(facing, 3, 3, 14, 13, 13, 16), box(facing, 5, 5, 11, 11, 11, 14)));
		}
	}

	public SensorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Channels.CHANNEL, DyeColor.WHITE)
				.setValue(POWER, 0));
	}

	/** A box given facing north, turned to face {@code facing}. */
	private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		return switch (facing) {
			case NORTH -> Block.box(x0, y0, z0, x1, y1, z1);
			case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			case UP -> Block.box(x0, 16 - z1, y0, x1, 16 - z0, y1);
			case DOWN -> Block.box(x0, z0, 16 - y1, x1, z1, 16 - y0);
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, Channels.CHANNEL, POWER);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** Where the block this sensor reads is. */
	public static BlockPos target(BlockPos pos, BlockState state) {
		return pos.relative(state.getValue(FACING).getOpposite());
	}

	/**
	 * How full the block the sensor is mounted on is, 0 to 100, or -1 if it holds nothing a sensor can read (no
	 * energy, fluid or item storage on the touching face).
	 */
	public static int percent(Level level, BlockPos pos, BlockState state) {
		BlockPos target = target(pos, state);
		Direction side = state.getValue(FACING);
		EnergyStorage energy = EnergyStorage.SIDED.find(level, target, side);
		if (energy != null && energy.getCapacity() > 0) {
			return (int) (energy.getAmount() * 100 / energy.getCapacity());
		}
		Storage<FluidVariant> fluids = FluidStorage.SIDED.find(level, target, side);
		if (fluids != null) {
			long amount = 0;
			long capacity = 0;
			for (StorageView<FluidVariant> view : fluids) {
				amount += view.getAmount();
				capacity += view.getCapacity();
			}
			if (capacity > 0) {
				return (int) (amount * 100 / capacity);
			}
		}
		Storage<ItemVariant> items = ItemStorage.SIDED.find(level, target, side);
		if (items != null) {
			double fill = 0;
			int slots = 0;
			for (StorageView<ItemVariant> view : items) {
				slots++;
				if (view.getCapacity() > 0) {
					fill += (double) view.getAmount() / view.getCapacity();
				}
			}
			if (slots > 0) {
				return (int) (fill * 100 / slots);
			}
		}
		return -1;
	}

	/** The redstone strength for a percentage, as a comparator gives it: 0 when empty, else 1 to 15. */
	public static int signal(int percent) {
		return percent <= 0 ? 0 : 1 + Math.min(14, percent * 14 / 100);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int power = signal(percent(level, pos, state));
		if (power != state.getValue(POWER)) {
			level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
		}
		level.scheduleTick(pos, this, INTERVAL);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWER);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return Channels.dye(stack, state, level, pos, player);
	}

	/** Right-click: shows the reading and the channel. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			int percent = percent(level, pos, state);
			player.sendOverlayMessage(percent < 0
					? Component.translatable("message.jugcraft.sensor.none", Channels.name(state.getValue(Channels.CHANNEL)))
					: Component.translatable("message.jugcraft.sensor", percent, Channels.name(state.getValue(Channels.CHANNEL))));
		}
		return InteractionResult.SUCCESS;
	}
}
