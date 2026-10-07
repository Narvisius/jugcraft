package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Gargoyle Sentinel (Halloween decorations batch 18): a gargoyle crouched on a pedestal, wings folded, that watches
 * for hostile mobs. Every {@value #CHECK_TICKS} ticks it finds the nearest within {@value #RANGE} blocks
 * ({@link GargoyleSentinelBlockEntity}); its head turns to follow it and its eyes glow brighter as it comes (drawn by the
 * client), and it gives a redstone signal ({@link #POWER}) from 1 at {@value #RANGE} blocks to 15 at {@value #NEAR} or
 * closer, to every side. Players and peaceful mobs are ignored.
 */
public class GargoyleSentinelBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final double RANGE = 16.0;
	public static final double NEAR = 2.0;
	public static final int CHECK_TICKS = 10;
	public static final float TURN_SPEED = 6.0F;
	public static final float MAX_TURN = 80.0F;
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

	public GargoyleSentinelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWER, 0));
	}

	/** The signal for a hostile mob {@code distance} blocks away: 15 at {@value #NEAR} or less, 1 at {@value #RANGE}, 0 beyond. */
	public static int signal(double distance) {
		if (distance > RANGE) {
			return 0;
		}
		if (distance <= NEAR) {
			return 15;
		}
		return Math.max(1, 15 - (int) Math.ceil((distance - NEAR) * 14.0 / (RANGE - NEAR)));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
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
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GargoyleSentinelBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.GARGOYLE_SENTINEL_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((GargoyleSentinelBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWER);
	}
}
