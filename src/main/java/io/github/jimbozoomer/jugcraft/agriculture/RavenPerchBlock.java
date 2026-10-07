package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Raven on a Perch: a stuffed-looking raven on a turned wooden perch that is not as stuffed as it looks. Its head
 * turns to watch the nearest player within {@value #WATCH_RANGE} blocks (up to {@value #MAX_TURN} degrees either way);
 * now and then it ruffles its feathers ({@value #RUFFLE_TICKS} ticks in every {@value #RUFFLE_PERIOD}) and croaks.
 * Use it and it caws and flaps its wings at you. Drawn by the client (client/RavenRenderer.java).
 */
public class RavenPerchBlock extends BaseEntityBlock {
	public static final double WATCH_RANGE = 8.0;
	public static final float MAX_TURN = 90.0F;
	public static final int RUFFLE_PERIOD = 140;
	public static final int RUFFLE_TICKS = 12;
	/** The block event that makes it flap, and how long a flap lasts. */
	public static final int FLAP = 1;
	public static final int FLAP_TICKS = 16;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);

	public RavenPerchBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** Whether the raven at {@code pos} is ruffling its feathers at {@code time}. */
	public static boolean ruffling(BlockPos pos, long time) {
		return Math.floorMod(time + pos.hashCode() * 7L, RUFFLE_PERIOD) < RUFFLE_TICKS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.RAVEN_PERCH_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** It caws and flaps. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			level.playSound(null, pos, SoundEvents.PARROT_AMBIENT, SoundSource.BLOCKS, 1.0F, 0.45F);
			level.blockEvent(pos, this, FLAP, 0);
		}
		return InteractionResult.SUCCESS;
	}

	/** A flap reaches clients as a block event; the block entity records when. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		BlockEntity entity = level.getBlockEntity(pos);
		if (id == FLAP && entity instanceof DecorationBlockEntity raven) {
			raven.mark(level.getGameTime());
			return true;
		}
		return false;
	}

	/** Now and then, as it ruffles, it croaks (heard by each client near it). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(120) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, SoundEvents.PARROT_AMBIENT, SoundSource.BLOCKS, 0.5F,
					0.4F + random.nextFloat() * 0.1F, false);
		}
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
		builder.add(FACING);
	}
}
