package io.github.jimbozoomer.jugcraft.world;

import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.Locale;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Alpine Spawn: a large, cool alpine meadow on mountain plateaus, where new worlds start. The biome is data
 * (data/jugcraft/worldgen, generated from tools/alpine.py); this class places it and moves the world spawn into it.
 *
 * <p>Placement, in the Overworld climate table through {@code mixin/OverworldBiomeBuilderMixin}: every vanilla meadow
 * becomes Alpine Spawn, and so do the cool plateau's forest and taiga, which border the cool meadows in vanilla's
 * plateau table ({@link #takesPlateau}). Forest and taiga elsewhere stay as they are.
 *
 * <p>The start: on a new world's first start (game time 0) the server moves the world spawn to the alpine village
 * nearest the origin, looking up to {@link #VILLAGE_CELLS} cells of the alpine village grid away. Alpine villages
 * generate only in Alpine Spawn and are common (their own tight grid, tools/alpine.py). Only when there is none does
 * the world start in the nearest Alpine Spawn within {@link #SEARCH_RADIUS} blocks of the origin.
 * {@code alpine_spawn.enabled=false} stops new generation; {@code alpine_spawn.start=off} keeps vanilla's spawn.
 */
public final class AlpineSpawn {
	public static final String FEATURE = "alpine_spawn";
	public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Jugcraft.id("alpine_spawn"));
	/**
	 * The plateau table's cells Alpine Spawn takes besides meadows: the cool row (temperature index 1) at humidity
	 * indexes 2 to 3, vanilla's forest and taiga there (their weird variants are meadows). Keep in sync with
	 * PLATEAU in tools/alpine.py.
	 */
	public static final int PLATEAU_TEMPERATURE = 1;
	public static final int PLATEAU_HUMIDITY_MIN = 2;
	public static final int PLATEAU_HUMIDITY_MAX = 3;
	/** The alpine villages (structure tag), which only generate in Alpine Spawn. */
	public static final TagKey<Structure> VILLAGES = TagKey.create(Registries.STRUCTURE, Jugcraft.id("alpine_villages"));
	public static final int SEARCH_RADIUS = 6400;
	public static final int SEARCH_STEP = 64;
	/** How far to look for an alpine village to start at, in cells of their grid (16 chunks): as far as the biome search. */
	public static final int VILLAGE_CELLS = 25;
	/** The start must have Alpine Spawn this far away on all four sides, so the spawn's scatter stays in the biome. */
	public static final int MARGIN = 48;
	/** How far from the biome's nearest edge to look for such a start. */
	public static final int INSIDE_SEARCH = 512;
	/**
	 * A village's locate position is a corner of its start chunk, which may lie just outside the biome when the village
	 * stands at its edge: the start moves to the nearest point within this many blocks that has Alpine Spawn under it
	 * and {@link #VILLAGE_MARGIN} blocks away on all four sides (or, failing that, under it at least).
	 */
	public static final int VILLAGE_REACH = 96;
	public static final int VILLAGE_MARGIN = 16;

	private AlpineSpawn() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(AlpineSpawn::moveWorldSpawn);
	}

	/** Wraps the Overworld biome builder's output so meadows come out as Alpine Spawn (unless switched off). */
	public static Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> wrap(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes) {
		if (!JugcraftConfig.isFeatureEnabled(FEATURE)) {
			return biomes;
		}
		return entry -> biomes.accept(replaces(entry) ? Pair.of(entry.getFirst(), BIOME) : entry);
	}

	/** Whether a climate entry is a meadow, which Alpine Spawn takes over. */
	public static boolean replaces(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry) {
		return entry.getSecond().equals(Biomes.MEADOW);
	}

	/**
	 * Whether Alpine Spawn takes the plateau table's cell at these temperature and humidity indexes (0 to 4, as vanilla's
	 * builder numbers them): the cool plateau's forest and taiga (unless switched off).
	 */
	public static boolean takesPlateau(int temperature, int humidity) {
		return JugcraftConfig.isFeatureEnabled(FEATURE) && temperature == PLATEAU_TEMPERATURE
				&& humidity >= PLATEAU_HUMIDITY_MIN && humidity <= PLATEAU_HUMIDITY_MAX;
	}

	private static void moveWorldSpawn(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (io.github.jimbozoomer.jugcraft.world.design.WorldDesigner.applySpawn(level)) return;
		if (!JugcraftConfig.isFeatureEnabled(FEATURE) || !JugcraftConfig.textOption("alpine_spawn.start").trim().equalsIgnoreCase("on")
				|| level.getGameTime() != 0L || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource)) {
			return;
		}
		BlockPos start = findStart(level);
		if (start == null) {
			Jugcraft.LOGGER.info("Alpine Spawn: none within {} blocks of the origin; the world spawn stays vanilla's", SEARCH_RADIUS);
			return;
		}
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
				String.format(Locale.ROOT, "setworldspawn %d %d %d", start.getX(), start.getY(), start.getZ()));
		Jugcraft.LOGGER.info("Alpine Spawn: the world starts at {} {} {}", start.getX(), start.getY(), start.getZ());
	}

	/**
	 * Where a new world should start: on the surface at the alpine village nearest the origin, or in the nearest Alpine
	 * Spawn if there is no alpine village in reach; null if there is no Alpine Spawn within {@link #SEARCH_RADIUS} blocks.
	 */
	public static BlockPos findStart(ServerLevel level) {
		BlockPos origin = new BlockPos(0, 96, 0);
		BlockPos target;
		BlockPos village = level.findNearestMapStructure(VILLAGES, origin, VILLAGE_CELLS, false);
		if (village != null) {
			// Biomes are three-dimensional: read them at the village's ground, not at the locate position's height.
			level.getChunk(village.getX() >> 4, village.getZ() >> 4);
			BlockPos ground = village.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, village.getX(), village.getZ()));
			target = around(level, ground, VILLAGE_MARGIN, VILLAGE_REACH, 8);
			if (target == null) {
				target = around(level, ground, 0, VILLAGE_REACH, 8);
			}
			if (target == null) {
				target = ground;
			}
			Jugcraft.LOGGER.info("Alpine Spawn: starting at the alpine village at {} {} ({} {})", village.getX(), village.getZ(),
					target.getX(), target.getZ());
		} else {
			Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(BIOME), origin, SEARCH_RADIUS, SEARCH_STEP, 64);
			if (found == null) {
				return null;
			}
			target = around(level, found.getFirst(), MARGIN, INSIDE_SEARCH, 16);
			if (target == null) {
				target = found.getFirst();
			}
			Jugcraft.LOGGER.info("Alpine Spawn: no alpine village in reach; starting in the biome at {} {}", target.getX(), target.getZ());
		}
		level.getChunk(target.getX() >> 4, target.getZ() >> 4);
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
		return new BlockPos(target.getX(), y, target.getZ());
	}

	/**
	 * The nearest point to {@code center} (within {@code reach} blocks, in rings {@code step} apart) that has Alpine
	 * Spawn under it and {@code margin} blocks away on all four sides; null if there is none. Biomes are read from the
	 * generator, so no chunk is generated.
	 */
	private static BlockPos around(ServerLevel level, BlockPos center, int margin, int reach, int step) {
		for (int radius = 0; radius <= reach; radius += step) {
			for (int dx = -radius; dx <= radius; dx += step) {
				for (int dz = -radius; dz <= radius; dz += step) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}
					BlockPos pos = center.offset(dx, 0, dz);
					if (alpine(level, pos) && (margin == 0 || alpine(level, pos.east(margin)) && alpine(level, pos.west(margin))
							&& alpine(level, pos.north(margin)) && alpine(level, pos.south(margin)))) {
						return pos;
					}
				}
			}
		}
		return null;
	}

	private static boolean alpine(ServerLevel level, BlockPos pos) {
		return level.getBiome(pos).is(BIOME);
	}
}
