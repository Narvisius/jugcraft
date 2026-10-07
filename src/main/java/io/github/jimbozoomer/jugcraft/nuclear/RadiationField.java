package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

/**
 * Radiation from far away (round 14: "the more there is in one spot, the further it spreads"). Close to a player
 * (Radiation.rateAt, within {@link #NEAR}) every radioactive block and container is counted one by one; beyond that,
 * each chunk section holding radioactive blocks is summed into one source at its centre, and each chunk's containers
 * are listed with what they hold. A source counts as far as it can still be measured over background ({@link #FLOOR}),
 * up to {@link #FAR}: one uranium block is lost in the background within a few dozen blocks, a thousand of them
 * still read a couple of hundred blocks away. Air thins radiation too ({@link #AIR}).
 * <p>A pile shields itself: a block inside it is behind the blocks round it, so each section's sum is kept for each
 * of the six directions, every block counted by how much of the pile lies between it and the outside that way.
 * <p>The summaries are refreshed a few chunks a tick round the players (every chunk in reach every few seconds),
 * without ever loading a chunk.
 */
public final class RadiationField {
	/** Within this (blocks, rounded out to whole sections) blocks and containers are counted one by one. */
	public static final int NEAR = 16;
	/** Nothing further than this is counted. */
	public static final double FAR = 192;
	/** mSv/h: a source that would read less than this (a fifteenth of background) is not counted. */
	public static final double FLOOR = 1e-5;
	/** Metres of air that thin gamma radiation by a factor of e (about 130 m for a megaelectronvolt gamma). */
	public static final double AIR = 130;
	/** Chunks refreshed a tick. */
	static final int PER_TICK = 8;
	private static final Map<ServerLevel, Field> FIELDS = new WeakHashMap<>();

	private RadiationField() {
	}

	/** One chunk section's radioactive blocks as one source: strength-weighted centre, total, and per direction. */
	public record SectionSum(long key, Vec3 centre, double total, double[] toward) {
		/** The strength seen from a direction (unit vector from the centre to the eye), allowing for the pile's self-shielding. */
		double seenFrom(double vx, double vy, double vz) {
			double ax = Math.abs(vx), ay = Math.abs(vy), az = Math.abs(vz);
			double sum = ax + ay + az;
			if (sum < 1e-9) {
				return total;
			}
			return (ax * toward[vx >= 0 ? 0 : 1] + ay * toward[vy >= 0 ? 2 : 3] + az * toward[vz >= 0 ? 4 : 5]) / sum;
		}
	}

	/** A container holding something radioactive (strength after its own shielding). */
	public record Stored(BlockPos pos, double strength) {
	}

	static final class Field {
		final Map<Long, SectionSum> sections = new HashMap<>();
		final Map<Long, List<Stored>> stored = new HashMap<>();
		final ArrayDeque<Long> queue = new ArrayDeque<>();
		Set<Long> wanted = new HashSet<>();
	}

	static Field field(ServerLevel level) {
		synchronized (FIELDS) {
			return FIELDS.computeIfAbsent(level, l -> new Field());
		}
	}

	/** The section sums in a level (tests, Radiation.rateAt). */
	public static Iterable<SectionSum> sections(ServerLevel level) {
		return List.copyOf(field(level).sections.values());
	}

	public static Iterable<Stored> stored(ServerLevel level) {
		List<Stored> out = new ArrayList<>();
		field(level).stored.values().forEach(out::addAll);
		return out;
	}

	/** Every tick: refresh a few chunks round the players. */
	public static void tick(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.players().isEmpty()) {
				continue;
			}
			Field f = field(level);
			if (f.queue.isEmpty()) {
				// Start a new round: every loaded chunk within reach of a player.
				Set<Long> wanted = new HashSet<>();
				int r = (int) Math.ceil(FAR / 16);
				for (ServerPlayer player : level.players()) {
					ChunkPos c = player.chunkPosition();
					for (int x = c.x() - r; x <= c.x() + r; x++) {
						for (int z = c.z() - r; z <= c.z() + r; z++) {
							wanted.add(ChunkPos.pack(x, z));
						}
					}
				}
				// Forget what is out of reach now.
				f.sections.keySet().removeIf(k -> !wanted.contains(ChunkPos.pack(SectionPos.x(k), SectionPos.z(k))));
				f.stored.keySet().removeIf(k -> !wanted.contains(k));
				f.wanted = wanted;
				f.queue.addAll(wanted);
			}
			for (int i = 0; i < PER_TICK && !f.queue.isEmpty(); i++) {
				long key = f.queue.poll();
				refresh(level, f, ChunkPos.getX(key), ChunkPos.getZ(key));
			}
		}
	}

	/** Brings one chunk's summaries up to date now (also used by tests). */
	public static void refresh(ServerLevel level, int cx, int cz) {
		refresh(level, field(level), cx, cz);
	}

	/** Refreshes every loaded chunk within reach of a point at once (tests). */
	public static void refreshAround(ServerLevel level, Vec3 at, double range) {
		int r = (int) Math.ceil(range / 16);
		ChunkPos c = ChunkPos.containing(BlockPos.containing(at));
		for (int x = c.x() - r; x <= c.x() + r; x++) {
			for (int z = c.z() - r; z <= c.z() + r; z++) {
				refresh(level, x, z);
			}
		}
	}

	private static void refresh(ServerLevel level, Field f, int cx, int cz) {
		LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
		LevelChunkSection[] sections = chunk == null ? null : chunk.getSections();
		for (int sy = level.getMinSectionY(); sy <= level.getMaxSectionY(); sy++) {
			long key = SectionPos.asLong(cx, sy, cz);
			LevelChunkSection section = sections == null ? null : sections[level.getSectionIndexFromSectionY(sy)];
			SectionSum sum = section == null || section.hasOnlyAir() || !section.maybeHas(s -> RadioactiveMaterials.block(s) > 0)
					? null : summarise(level, chunk, section, cx, sy, cz, key);
			if (sum == null) {
				f.sections.remove(key);
			} else {
				f.sections.put(key, sum);
			}
		}
		long ck = ChunkPos.pack(cx, cz);
		if (chunk == null) {
			f.stored.remove(ck);
			return;
		}
		List<Stored> list = new ArrayList<>();
		for (BlockEntity entity : chunk.getBlockEntities().values()) {
			double s = held(entity);
			if (s > 0) {
				list.add(new Stored(entity.getBlockPos().immutable(), s));
			}
		}
		if (list.isEmpty()) {
			f.stored.remove(ck);
		} else {
			f.stored.put(ck, list);
		}
	}

	/** What a block entity holds, after its own shielding (0 if not a container or nothing radioactive in it). */
	public static double held(BlockEntity entity) {
		if (!(entity instanceof Container container)) {
			return 0;
		}
		double s = 0;
		for (int i = 0; i < container.getContainerSize(); i++) {
			s += Radiation.strength(container.getItem(i));
		}
		if (s <= 0) {
			return 0;
		}
		// Ordinary chests and barrels stop a little; shielded containers (lead pig, locker, waste drum) nearly all.
		return s * (entity instanceof ShieldedContainer shielded ? shielded.leakage() : 0.8);
	}

	private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private static SectionSum summarise(ServerLevel level, LevelChunk chunk, LevelChunkSection section, int cx, int sy, int cz, long key) {
		double total = 0, mx = 0, my = 0, mz = 0;
		double[] toward = new double[6];
		int x0 = cx << 4, y0 = sy << 4, z0 = cz << 4;
		for (int y = 0; y < 16; y++) {
			for (int z = 0; z < 16; z++) {
				for (int x = 0; x < 16; x++) {
					BlockState state = section.getBlockState(x, y, z);
					double s = RadioactiveMaterials.block(state);
					if (s <= 0) {
						continue;
					}
					total += s;
					mx += s * (x0 + x + 0.5);
					my += s * (y0 + y + 0.5);
					mz += s * (z0 + z + 0.5);
					// Self-shielding: how much of the pile lies between this block and the outside, each way.
					for (int d = 0; d < 6; d++) {
						double e = 1;
						for (int k = 1; k <= 6 && e > 1e-6; k++) {
							BlockState next = loaded(level, x0 + x + DIRS[d][0] * k, y0 + y + DIRS[d][1] * k, z0 + z + DIRS[d][2] * k);
							if (RadioactiveMaterials.block(next) <= 0) {
								break;
							}
							e *= Shielding.factor(next);
						}
						toward[d] += s * e;
					}
				}
			}
		}
		if (total <= 0) {
			return null;
		}
		return new SectionSum(key, new Vec3(mx / total, my / total, mz / total), total, toward);
	}

	/** A block state, or air where the chunk is not loaded (never loads one). */
	public static BlockState loaded(ServerLevel level, int x, int y, int z) {
		if (y < level.getMinY() || y > level.getMaxY()) {
			return Blocks.AIR.defaultBlockState();
		}
		LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
		return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(new BlockPos(x, y, z));
	}

	/** Reads blocks for Shielding.through without loading chunks (remembers the last chunk). */
	public static Shielding.States states(ServerLevel level) {
		return new Shielding.States() {
			private LevelChunk last;
			private int lx = Integer.MIN_VALUE, lz = Integer.MIN_VALUE;
			private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

			@Override
			public BlockState at(int x, int y, int z) {
				if (y < level.getMinY() || y > level.getMaxY()) {
					return Blocks.AIR.defaultBlockState();
				}
				int cx = x >> 4, cz = z >> 4;
				if (cx != lx || cz != lz) {
					last = level.getChunkSource().getChunkNow(cx, cz);
					lx = cx;
					lz = cz;
				}
				return last == null ? Blocks.AIR.defaultBlockState() : last.getBlockState(pos.set(x, y, z));
			}
		};
	}

	/** How much air thins radiation over a distance (blocks). */
	public static double air(double distance) {
		return Math.exp(-distance / AIR);
	}

	/** How far a source of this strength (mSv/h a block away) can still be measured. */
	public static double reach(double strength) {
		return Math.min(FAR, Math.sqrt(Math.max(0, strength) / FLOOR));
	}
}
