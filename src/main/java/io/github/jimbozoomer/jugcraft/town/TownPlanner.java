package io.github.jimbozoomer.jugcraft.town;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict;
import java.util.Arrays;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.jspecify.annotations.Nullable;

/**
 * Chooses where the town stands in a new world: on the flattest dry ground {@link #MIN_DISTANCE} to
 * {@link #MAX_DISTANCE} blocks from the world's start, so it is in sight of the start without covering it (or the
 * village Alpine Spawn starts beside). It runs once, on a new world's first start (game time 0), after the world
 * spawn has been settled (a later event phase than Alpine Spawn's move), and only for noise-generated Overworlds; the
 * land is read from the generator's noise, so no chunk is generated to choose. {@code town.enabled=false} skips it.
 * Operators can place the town in any other world with {@code /jugcraft town place}.
 */
public final class TownPlanner {
	public static final int MIN_DISTANCE = 170;
	public static final int MAX_DISTANCE = 300;
	public static final int RING_STEP = 26;
	public static final int ANGLE_STEP = 15;
	/** Heights sampled per candidate: a GRID x GRID square over the walled area. */
	public static final int GRID = 7;
	/** How far apart the samples reach from the town's middle (about the wall's radius). */
	public static final int REACH = 72;
	/** A candidate with more wet samples than this (a lake or the sea) is skipped. */
	public static final int MAX_WET = 3;
	private static final Identifier AFTER_SPAWN = Jugcraft.id("town_after_spawn");

	private TownPlanner() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Event.DEFAULT_PHASE, AFTER_SPAWN);
		ServerLifecycleEvents.SERVER_STARTED.register(AFTER_SPAWN, TownPlanner::planNewWorld);
	}

	private static void planNewWorld(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (!JugcraftConfig.isFeatureEnabled(JugcraftTown.FEATURE) || level.getGameTime() != 0L || TownState.get(level).origin() != null
				|| !(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator)) {
			return;
		}
		BlockPos start = level.getRespawnData().pos();
		if (io.github.jimbozoomer.jugcraft.world.design.WorldDesigner.applyTown(level)) return;
		BlockPos origin = choose(level, start);
		if (origin == null) {
			Jugcraft.LOGGER.info("Town: no dry, open ground {}-{} blocks from the start; no town in this world", MIN_DISTANCE, MAX_DISTANCE);
			return;
		}
		place(level, origin);
	}

	/** Puts the town's corner here and builds whatever of it is already loaded. */
	public static void place(ServerLevel level, BlockPos origin) {
		TownState.get(level).place(origin, io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict.canReserve(level, origin));
		io.github.jimbozoomer.jugcraft.styx.StyxTownDistrict.reserve(level);
		TownBuilder.queueLoaded(level);
		BlockPos centre = Town.centre(origin);
		Jugcraft.LOGGER.info("Town: placed with its middle at {} {} {}", centre.getX(), centre.getY(), centre.getZ());
	}

	/** The corner (origin) of the town whose middle stands nearest (x, ground, z), aligned to chunk borders. */
	public static BlockPos originAround(int x, int ground, int z) {
		int half = TownData.get().size / 2;
		return new BlockPos(Math.floorDiv(x - half + 8, 16) * 16, ground, Math.floorDiv(z - half + 8, 16) * 16);
	}

	/** The best site, as the town's origin; null if every candidate is wet. */
	public static @Nullable BlockPos choose(ServerLevel level, BlockPos start) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState random = level.getChunkSource().randomState();
		double bestScore = Double.MAX_VALUE;
		BlockPos best = null;
		for (int r = MIN_DISTANCE; r <= MAX_DISTANCE; r += RING_STEP) {
			for (int a = 0; a < 360; a += ANGLE_STEP) {
				// The town's middle as built (its corner sits on a chunk border), which must itself be in range.
				BlockPos middle = Town.centre(originAround(start.getX() + (int) Math.round(r * Math.cos(Math.toRadians(a))), 0,
						start.getZ() + (int) Math.round(r * Math.sin(Math.toRadians(a)))));
				int cx = middle.getX();
				int cz = middle.getZ();
				double distance = Math.hypot(cx - start.getX(), cz - start.getZ());
				if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
					continue;
				}
				BlockPos candidate = originAround(cx, 0, cz);
				// The east district must not cover the spawn village. Include it when scoring terrain,
				// without generating any chunks merely to choose a location.
				BlockPos home = StyxTownDistrict.home(candidate);
				if (start.getX() >= home.getX() - 48 && start.getX() < home.getX() + StyxTownDistrict.WIDTH + 48
						&& start.getZ() >= home.getZ() - 48 && start.getZ() < home.getZ() + StyxTownDistrict.DEPTH + 48) continue;
				int[] heights = new int[GRID * GRID + 9];
				int wet = 0;
				int n = 0;
				for (int i = 0; i < GRID; i++) {
					for (int j = 0; j < GRID; j++) {
						int x = cx - REACH + 2 * REACH * i / (GRID - 1);
						int z = cz - REACH + 2 * REACH * j / (GRID - 1);
						int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
						int surface = generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random);
						if (surface > floor) {
							wet++;
						}
						heights[n++] = floor;
					}
				}
				for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
					int x = home.getX() + (StyxTownDistrict.WIDTH - 1) * i / 2;
					int z = home.getZ() + (StyxTownDistrict.DEPTH - 1) * j / 2;
					int floor = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
					if (generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, random) > floor) wet++;
					heights[n++] = floor;
				}
				if (wet > MAX_WET) {
					continue;
				}
				int[] sorted = heights.clone();
				Arrays.sort(sorted);
				int median = sorted[sorted.length / 2];
				double spread = 0;
				for (int h : heights) {
					spread += Math.abs(h - median);
				}
				spread /= heights.length;
				// Flat ground first; a little nearer is a little better.
				double score = spread + 0.02 * r + 2.0 * wet;
				if (score < bestScore) {
					bestScore = score;
					// getBaseHeight is the first free block; the town's ground is the block under it.
					best = originAround(cx, median - 1, cz);
				}
			}
		}
		return best;
	}
}
