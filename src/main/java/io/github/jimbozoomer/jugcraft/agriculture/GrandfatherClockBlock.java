package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * The Grandfather Clock (Halloween decorations batch 19, the Poltergeist's Dinner Party): a carved longcase clock two
 * blocks tall. Its hands show the time of day on the overworld clock, the painted moon in its arched dial shows the
 * moon's phase, and its pendulum swings behind the glass (all drawn by the client, client/GrandfatherClockRenderer.java).
 * Every hour ({@value #HOUR_TICKS} ticks) it strikes the hour, one chime a second, and gives a redstone pulse of
 * {@value #PULSE_TICKS} ticks; a comparator reads the hour, 1 to 12. At midnight a pale face looks out through the
 * glass while it strikes twelve.
 */
public class GrandfatherClockBlock extends TallDecorationBlock implements EntityBlock {
	public static final int HOUR_TICKS = 1000;
	public static final int PULSE_TICKS = 4;
	public static final int STRIKE_GAP = 20;
	public static final int FACE_TICKS = 200;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public GrandfatherClockBlock(Properties properties) {
		super(properties, Block.box(2.0, 0.0, 3.0, 14.0, 16.0, 13.0), Block.box(1.5, 0.0, 2.5, 14.5, 16.0, 13.5));
		registerDefaultState(defaultBlockState().setValue(POWERED, false));
	}

	/** Which hour of the world {@code dayTime} is in, counted from the start. */
	public static long hourIndex(long dayTime) {
		return Math.floorDiv(dayTime, HOUR_TICKS);
	}

	/** The hour the clock shows at {@code dayTime}, 1 to 12 (time 0 is six in the morning, 6000 noon, 18000 midnight). */
	public static int clockHour(long dayTime) {
		int hour = (int) Math.floorMod(hourIndex(dayTime) + 6, 12L);
		return hour == 0 ? 12 : hour;
	}

	/** The minutes past the hour at {@code dayTime}, 0 to 59. */
	public static int minutes(long dayTime) {
		return (int) (Math.floorMod(dayTime, (long) HOUR_TICKS) * 60 / HOUR_TICKS);
	}

	/** The moon's phase on the night of {@code dayTime}: 0 full, 4 new. */
	public static int moonPhase(long dayTime) {
		return (int) Math.floorMod(Math.floorDiv(dayTime, 24000L), 8L);
	}

	/** Whether the pale face is at the glass at {@code dayTime}: the first {@value #FACE_TICKS} ticks after midnight. */
	public static boolean midnight(long dayTime) {
		long time = Math.floorMod(dayTime, 24000L);
		return time >= 18000 && time < 18000 + FACE_TICKS;
	}

	static void pulse(ServerLevel level, BlockPos lower) {
		BlockState state = level.getBlockState(lower);
		if (state.getBlock() instanceof GrandfatherClockBlock) {
			setBoth(level, lower, state.setValue(POWERED, true));
			level.scheduleTick(lower, state.getBlock(), PULSE_TICKS);
			level.updateNeighbourForOutputSignal(lower, state.getBlock());
		}
	}

	/** The hour's pulse ends. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(POWERED)) {
			setBoth(level, pos, state.setValue(POWERED, false));
		}
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(POWERED) ? 15 : 0;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return clockHour(level.getOverworldClockTime());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new GrandfatherClockBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER || type != JugcraftAgriculture.GRANDFATHER_CLOCK_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((GrandfatherClockBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(POWERED);
	}
}
