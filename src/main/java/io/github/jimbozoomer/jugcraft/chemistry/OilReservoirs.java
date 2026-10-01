package io.github.jimbozoomer.jugcraft.chemistry;

import com.mojang.serialization.Codec;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Hidden oil fields under the Overworld. Which chunks hold oil, what kind and how much is fixed by the world seed, so
 * nothing is stored until oil is taken out; then how much each chunk has given is saved with the dimension.
 * <ul>
 * <li>{@link Kind#CONVENTIONAL}: a pool in porous rock that a pumpjack can draw on. About 1 chunk in 12.</li>
 * <li>{@link Kind#SHALE}: oil locked in tight rock, which only a fracking rig can free. About 1 chunk in 4 of the rest,
 * and larger.</li>
 * </ul>
 * Every reservoir is finite: once a chunk has given its capacity it is dry for good.
 */
public final class OilReservoirs {
	public enum Kind {
		NONE, CONVENTIONAL, SHALE
	}

	/** A chunk's reservoir: its kind, how much it held at the start and how much is left (mB). */
	public record Reservoir(Kind kind, long capacity, long remaining) {
		public boolean isDry() {
			return remaining <= 0;
		}
	}

	/** One chunk in this many holds a conventional reservoir. */
	public static final int CONVENTIONAL_ONE_IN = 12;
	/** Of the chunks without one, one in this many holds shale oil. */
	public static final int SHALE_ONE_IN = 4;
	/** Conventional reservoir size range, in buckets. */
	public static final int CONVENTIONAL_MIN_BUCKETS = 50;
	public static final int CONVENTIONAL_MAX_BUCKETS = 250;
	/** Shale reservoir size range, in buckets. */
	public static final int SHALE_MIN_BUCKETS = 200;
	public static final int SHALE_MAX_BUCKETS = 800;
	/** Mixed into the world seed so reservoirs don't line up with anything else seeded from it. */
	private static final long SALT = 0x6F696C5F6669656CL;

	private OilReservoirs() {
	}

	/** The reservoir under a chunk; NONE outside the Overworld. */
	public static Reservoir get(ServerLevel level, ChunkPos chunk) {
		Reservoir initial = initial(level, chunk);
		if (initial.kind() == Kind.NONE) {
			return initial;
		}
		long taken = data(level).taken(chunk);
		return new Reservoir(initial.kind(), initial.capacity(), Math.max(0, initial.capacity() - taken));
	}

	/**
	 * Takes up to {@code mb} from a chunk's reservoir of the given kind (a pumpjack can't draw on shale), and returns how
	 * much it got: less when the reservoir runs dry, 0 when it is dry or another kind.
	 */
	public static int extract(ServerLevel level, ChunkPos chunk, Kind kind, int mb) {
		Reservoir reservoir = get(level, chunk);
		if (reservoir.kind() != kind || mb <= 0) {
			return 0;
		}
		int got = (int) Math.min(mb, reservoir.remaining());
		if (got > 0) {
			data(level).take(chunk, got);
		}
		return got;
	}

	/**
	 * Game tests only: makes a chunk of the Overworld hold this reservoir (as generated) until the server stops, since
	 * tests can't choose which chunk their structure lands in. Never called by the mod itself.
	 */
	public static void overrideForTest(ChunkPos chunk, Kind kind, long capacity) {
		TEST_OVERRIDES.put(chunk.pack(), new Reservoir(kind, capacity, capacity));
	}

	private static final Map<Long, Reservoir> TEST_OVERRIDES = new ConcurrentHashMap<>();

	/** The reservoir as generated, before anything was taken. Deterministic from the seed and chunk. */
	static Reservoir initial(ServerLevel level, ChunkPos chunk) {
		if (level.dimension() != Level.OVERWORLD) {
			return new Reservoir(Kind.NONE, 0, 0);
		}
		Reservoir override = TEST_OVERRIDES.get(chunk.pack());
		if (override != null) {
			return override;
		}
		RandomSource random = RandomSource.create(level.getSeed() ^ SALT
				^ (chunk.x() * 341_873_128_712L) ^ (chunk.z() * 132_897_987_541L));
		if (random.nextInt(CONVENTIONAL_ONE_IN) == 0) {
			long buckets = CONVENTIONAL_MIN_BUCKETS + random.nextInt(CONVENTIONAL_MAX_BUCKETS - CONVENTIONAL_MIN_BUCKETS + 1);
			return new Reservoir(Kind.CONVENTIONAL, buckets * 1000, buckets * 1000);
		}
		if (random.nextInt(SHALE_ONE_IN) == 0) {
			long buckets = SHALE_MIN_BUCKETS + random.nextInt(SHALE_MAX_BUCKETS - SHALE_MIN_BUCKETS + 1);
			return new Reservoir(Kind.SHALE, buckets * 1000, buckets * 1000);
		}
		return new Reservoir(Kind.NONE, 0, 0);
	}

	private static Data data(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(Data.TYPE);
	}

	/** How much each chunk has given, saved with the dimension (data/jugcraft_oil_reservoirs.dat). */
	static final class Data extends SavedData {
		private static final Codec<Map<Long, Long>> MAP = Codec.unboundedMap(
				Codec.STRING.xmap(Long::parseLong, String::valueOf), Codec.LONG);
		static final Codec<Data> CODEC = MAP.xmap(Data::new, data -> data.taken);
		static final SavedDataType<Data> TYPE = new SavedDataType<>(Jugcraft.id("oil_reservoirs"), Data::new, CODEC, null);

		private final Map<Long, Long> taken;

		Data() {
			this(Map.of());
		}

		Data(Map<Long, Long> taken) {
			this.taken = new HashMap<>(taken);
		}

		long taken(ChunkPos chunk) {
			return taken.getOrDefault(chunk.pack(), 0L);
		}

		void take(ChunkPos chunk, long mb) {
			taken.merge(chunk.pack(), mb, Long::sum);
			setDirty();
		}
	}
}
