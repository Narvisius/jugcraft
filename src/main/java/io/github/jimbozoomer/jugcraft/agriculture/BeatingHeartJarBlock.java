package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Beating Heart Jar (Halloween decorations batch 17): a heart on a brass stand in red fluid, with a little dial. It
 * is a redstone clock: it beats at one of {@link #TEMPOS} beats a minute (use it to change), and each beat ({@link #BEAT})
 * gives a {@value #PULSE_TICKS}-tick signal of 15 to every side but the one below. A redstone signal into its base stops
 * it. Its beats are scheduled ticks, so it does nothing between them.
 */
public class BeatingHeartJarBlock extends OddityJarBlock {
	public static final int[] TEMPOS = {60, 80, 100, 120};
	public static final int PULSE_TICKS = 2;
	public static final BooleanProperty BEAT = BooleanProperty.create("beat");
	public static final IntegerProperty TEMPO = IntegerProperty.create("tempo", 0, 3);

	public BeatingHeartJarBlock(Properties properties) {
		super(properties, Kind.HEART);
		registerDefaultState(stateDefinition.any().setValue(BEAT, false).setValue(TEMPO, 0));
	}

	/** The ticks from one beat to the next at {@code state}'s tempo. */
	public static int period(BlockState state) {
		return 1200 / TEMPOS[state.getValue(TEMPO)];
	}

	/** Whether a signal comes into its base from below, stopping it. */
	public static boolean stopped(Level level, BlockPos pos) {
		return level.hasSignal(pos.below(), Direction.DOWN);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		if (!old.is(this)) {
			level.scheduleTick(pos, this, period(state));
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(BEAT)) {
			level.setBlock(pos, state.setValue(BEAT, false), Block.UPDATE_ALL);
			level.scheduleTick(pos, this, Math.max(1, period(state) - PULSE_TICKS));
			return;
		}
		if (stopped(level, pos)) {
			return;
		}
		level.setBlock(pos, state.setValue(BEAT, true), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 0.5F, 1.4F);
		level.scheduleTick(pos, this, PULSE_TICKS);
	}

	/** A signal going from its base starts it again. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		if (!level.isClientSide() && !stopped(level, pos) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, period(state));
		}
	}

	/** Use it to change its tempo. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			BlockState next = state.setValue(TEMPO, (state.getValue(TEMPO) + 1) % TEMPOS.length);
			level.setBlock(pos, next, Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.4F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.beating_heart_jar.tempo", TEMPOS[next.getValue(TEMPO)]));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/** Each beat to every side but the one below, so it can't power what stops it. */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(BEAT) && direction != Direction.UP ? 15 : 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BEAT, TEMPO);
	}
}
