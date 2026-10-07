package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Leaves of a deciduous tree (the larch, maple and aspen) that follow the seasons. They follow the server's season
 * day ({@link JugcraftSeasons#today()}) on their tree's {@link Schedule}: green, then their autumn colour
 * ({@link Foliage#GOLD}; the maple's models show reds, oranges and golds), then bare twigs until spring. Each block
 * turns up to {@link #JITTER} days early or late, fixed by its position, so a crown turns gradually and neighbouring
 * trees differ. Leaves start in today's look: generated trees through {@link SeasonalLeavesDecorator}, leaves placed by
 * a player or grown from a sapling through {@link #onPlace}. After that they catch up on their random ticks; one that
 * changes also brings the leaves it touches up to date (up to {@link #SPREAD} of them, in loaded chunks), so a crown
 * turns together. With seasons off they stay green. Only the look changes: otherwise these are vanilla leaves (natural ones decay away from logs; they drop
 * saplings and sticks).
 */
public class SeasonalLeavesBlock extends TintedParticleLeavesBlock {
	public static final EnumProperty<Foliage> SEASON = EnumProperty.create("season", Foliage.class);
	/** Keep in sync with JITTER and SPREAD in tools/agriculture.py. */
	public static final int JITTER = 7;
	/** At most this many touching leaves catch up with one that changed on its random tick. */
	public static final int SPREAD = 128;

	/** The leaves' look in each season. */
	public enum Foliage implements StringRepresentable {
		GREEN, GOLD, BARE;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * When a tree's leaves change, in season days (northern calendar): green from {@code greenFrom}, autumn colour
	 * from {@code goldFrom}, bare from {@code bareFrom} until {@code greenFrom} comes round again.
	 */
	public record Schedule(int greenFrom, int goldFrom, int bareFrom) {
		/** The leaves at {@code pos} on season day {@code day} (0: seasons off, always green). */
		public Foliage on(int day, BlockPos pos) {
			if (day <= 0) {
				return Foliage.GREEN;
			}
			int shifted = Math.floorMod(day - 1 + jitter(pos), SeasonCalendar.DAYS) + 1;
			if (shifted >= bareFrom || shifted < greenFrom) {
				return Foliage.BARE;
			}
			return shifted >= goldFrom ? Foliage.GOLD : Foliage.GREEN;
		}
	}

	private final Schedule schedule;

	public SeasonalLeavesBlock(Schedule schedule, Properties properties) {
		super(0.01F, properties);
		this.schedule = schedule;
		registerDefaultState(defaultBlockState().setValue(SEASON, Foliage.GREEN));
	}

	public Schedule schedule() {
		return schedule;
	}

	/** How many days early (negative) or late this position turns: -JITTER to JITTER, always the same. */
	public static int jitter(BlockPos pos) {
		int hash = pos.getX() * 73428767 ^ pos.getY() * 912931 ^ pos.getZ() * 42317861;
		hash ^= hash >>> 15;
		return Math.floorMod(hash, 2 * JITTER + 1) - JITTER;
	}

	/** Every seasonal leaf ticks, placed ones too, so all of them follow the season. */
	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.randomTick(state, level, pos, random);
		BlockState now = level.getBlockState(pos);
		int day = JugcraftSeasons.today();
		if (now.is(this) && follow(now, level, pos, day)) {
			catchUp(level, pos, day);
		}
	}

	/** Leaves placed or grown from a sapling start in today's state rather than waiting for a random tick. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide() && !oldState.is(this)) {
			follow(state, level, pos, JugcraftSeasons.today());
		}
	}

	/** Brings the loaded leaves connected to {@code start} up to date for {@code day}, at most {@link #SPREAD} of them. */
	private void catchUp(ServerLevel level, BlockPos start, int day) {
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			for (Direction direction : Direction.values()) {
				if (seen.size() >= SPREAD) {
					return;
				}
				BlockPos next = pos.relative(direction);
				if (!seen.contains(next) && level.isLoaded(next)) {
					BlockState state = level.getBlockState(next);
					if (state.is(this)) {
						seen.add(next);
						follow(state, level, next, day);
						queue.add(next);
					}
				}
			}
		}
	}

	/** Sets the leaves at {@code pos} to their look on {@code day}; true if that changed them. */
	private boolean follow(BlockState state, Level level, BlockPos pos, int day) {
		Foliage foliage = schedule.on(day, pos);
		if (state.getValue(SEASON) == foliage) {
			return false;
		}
		level.setBlock(pos, state.setValue(SEASON, foliage), Block.UPDATE_CLIENTS);
		return true;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SEASON);
	}
}
