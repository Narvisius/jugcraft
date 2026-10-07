package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Black Cat Figure: a glazed black cat, sitting tall, that is a little too lifelike. Its tail swishes slowly
 * ({@link #swish}) and at night its eyes glow green. Run past it (a sprinting player within {@value #REACH} blocks)
 * and it arches its back and hisses at you ({@link #HISSING}, for {@value #HISS_TICKS} ticks; then it won't again for
 * {@value #COOLDOWN_TICKS}). Use it and it purrs. {@link BlackCatBlockEntity} watches for runners on the server; the
 * tail and eyes are drawn by the client (client/BlackCatRenderer.java).
 */
public class BlackCatBlock extends BaseEntityBlock {
	public static final double REACH = 3.0;
	public static final int HISS_TICKS = 30;
	public static final int COOLDOWN_TICKS = 60;
	public static final int SWISH_PERIOD = 50;
	public static final float SWISH_DEGREES = 25.0F;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty HISSING = BooleanProperty.create("hissing");
	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 3.0, 12.0, 12.0, 13.0);

	public BlackCatBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HISSING, false));
	}

	/** How far the tail of the cat at {@code pos} has swung (degrees) at {@code time}; still while it hisses. */
	public static float swish(BlockPos pos, boolean hissing, float time) {
		if (hissing) {
			return 0.0F;
		}
		float phase = (pos.hashCode() & 0xFF) / 256.0F;
		return SWISH_DEGREES * Mth.sin((time / SWISH_PERIOD + phase) * Mth.TWO_PI);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BlackCatBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.BLACK_CAT_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((BlackCatBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** It purrs. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && !state.getValue(HISSING)) {
			level.playSound(null, pos, SoundEvents.FOX_SNIFF, SoundSource.BLOCKS, 0.8F, 0.5F);
		}
		return InteractionResult.SUCCESS;
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
		builder.add(FACING, HISSING);
	}
}
