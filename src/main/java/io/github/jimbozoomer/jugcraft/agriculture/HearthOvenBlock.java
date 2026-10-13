package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Hearth Oven: a domed brick bread oven on a stone hearth, its arched mouth to the front and a chimney out of the
 * top ({@link HearthOvenBlockEntity}). Use it with fuel to feed the fire (coal, charcoal, coke or a log); with a raw pie
 * to put it in; with an empty hand to take the pie out. Lit, it glows from the mouth and sends sparks and smoke up the
 * chimney; comparators read how far the pie is baked.
 */
public class HearthOvenBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final int LIGHT = 13;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);

	public HearthOvenBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		// The partial-height brick shell is still an obstacle, not a passage through the oven.
		return false;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HearthOvenBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.HEARTH_OVEN_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((HearthOvenBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	/** Fuel feeds the fire; a raw pie goes in. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof HearthOvenBlockEntity oven)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		boolean pie = HearthOvenBlockEntity.rawFilling(stack) != null;
		if (!pie && !HearthOvenBlockEntity.isFuel(stack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (pie) {
			oven.putIn(stack, player);
		} else {
			oven.feed(stack, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand takes the pie out (or says how it is coming along). */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof HearthOvenBlockEntity oven)) {
			return InteractionResult.SUCCESS;
		}
		oven.takeOut(player);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		double x = pos.getX() + 0.5 + facing.getStepX() * 0.45;
		double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.45;
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 0.2, z, 0.0, 0.0, 0.0);
		}
		if (random.nextInt(4) == 0) {
			// Up the chimney, behind and to the right of the middle.
			Direction back = facing.getOpposite();
			Direction side = facing.getClockWise();
			double cx = pos.getX() + 0.5 + (back.getStepX() + side.getStepX()) * 0.125;
			double cz = pos.getZ() + 0.5 + (back.getStepZ() + side.getStepZ()) * 0.125;
			level.addParticle(ParticleTypes.SMOKE, cx, pos.getY() + 1.05, cz, 0.0, 0.04, 0.0);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof HearthOvenBlockEntity oven ? oven.signal() : 0;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
	}
}
