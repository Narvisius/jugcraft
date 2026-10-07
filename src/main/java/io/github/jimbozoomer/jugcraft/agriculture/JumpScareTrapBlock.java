package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Jump-Scare Trap: a battered wooden crate. When someone walks up to its front (within {@value #REACH} blocks, not
 * sneaking; {@link JumpScareTrapBlockEntity}) or it gets a rising redstone signal (hook it to a tripwire), the lid
 * bursts open and a ghost on a spring shoots up out of it with a shriek ({@link Phase#POPPED}, for {@value #POP_TICKS}
 * ticks); then the ghost sinks back, the lid shuts and it rests {@value #RESET_TICKS} ticks before it can go off again.
 * The lid and ghost are drawn by the client (client/JumpScareTrapRenderer.java). It frightens; it does no harm.
 */
public class JumpScareTrapBlock extends BaseEntityBlock {
	public static final double REACH = 2.5;
	public static final int POP_TICKS = 40;
	public static final int RESET_TICKS = 60;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);

	/** Ready to go off, the ghost out, or shutting and resting. */
	public enum Phase implements StringRepresentable {
		READY, POPPED, RESETTING;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public JumpScareTrapBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PHASE, Phase.READY).setValue(POWERED, false));
	}

	/** Springs the trap at {@code pos} if it is ready; returns whether it did. */
	public static boolean spring(Level level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof JumpScareTrapBlock) || state.getValue(PHASE) != Phase.READY) {
			return false;
		}
		level.setBlock(pos, state.setValue(PHASE, Phase.POPPED), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.GHAST_SCREAM, SoundSource.BLOCKS, 1.0F, 1.3F);
		level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
		level.scheduleTick(pos, state.getBlock(), POP_TICKS);
		return true;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new JumpScareTrapBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(PHASE) != Phase.READY || type != JugcraftAgriculture.JUMP_SCARE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((JumpScareTrapBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	/** The ghost sinks back and the lid shuts; then it rests and is ready again. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(PHASE) == Phase.POPPED) {
			level.setBlock(pos, state.setValue(PHASE, Phase.RESETTING), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.8F, 0.7F);
			level.scheduleTick(pos, this, RESET_TICKS);
		} else if (state.getValue(PHASE) == Phase.RESETTING) {
			level.setBlock(pos, state.setValue(PHASE, Phase.READY), Block.UPDATE_ALL);
		}
	}

	/** A rising redstone signal (a tripwire) springs it. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (level.isClientSide()) {
			return;
		}
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED)) {
			BlockState changed = state.setValue(POWERED, powered);
			level.setBlock(pos, changed, Block.UPDATE_ALL);
			if (powered) {
				spring(level, pos, changed);
			}
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
		builder.add(FACING, PHASE, POWERED);
	}
}
