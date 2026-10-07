package io.github.jimbozoomer.jugcraft.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Jugcraft regions (docs/features/biome-regions.md, tools/biomes.py): the Overworld is divided into irregular cells
 * about {@code biomes.region_size} blocks across, and {@code biomes.region_share} of them are Jugcraft regions. Each
 * Jugcraft region uses one of {@link #LAYOUTS} layouts: vanilla's climate table with that layout's {@link #rules()}
 * applied (a vanilla biome, in the given climate bands and weirdness half, becomes a Jugcraft biome; entries are cut at
 * the band edges first, {@link #split}). The rest stay vanilla. Terrain does not depend on biomes, so a region's border is a change of biome only.
 *
 * <p>How: {@code mixin/OverworldBiomeBuilderMixin} records the layouts as the builder fills vanilla's table
 * ({@link #recorder}), and lists every Jugcraft biome in vanilla's table at {@link #UNREACHABLE}, a climate no place
 * has, so world generation knows its features and structures. {@code mixin/MultiNoiseBiomeSourceMixin} then answers
 * biome lookups in Jugcraft regions from their layout ({@link Source}). Which cells are Jugcraft regions, and which
 * layout each uses, comes from the world seed (read as the Overworld loads, before any chunk generates), so a seed
 * always makes the same world. The rules are data ({@code /jugcraft/region_rules.json}, generated from
 * tools/biomes.py). {@code biomes.enabled=false} turns regions off for new chunks; the biomes stay registered.
 */
public final class JugcraftRegions {
	public static final String FEATURE = "biomes";
	/** Defaults of the region options. Keep in sync with REGIONS in tools/biomes.py. */
	public static final int SIZE = 1024;
	public static final double SHARE = 0.5;
	/** How many layouts Jugcraft regions come in, equally often. Keep in sync with LAYOUTS in tools/biomes.py. */
	public static final int LAYOUTS = 4;
	/** Vanilla's climate band edges (OverworldBiomeBuilder). Keep in sync with tools/biomes.py. */
	private static final float[] TEMPERATURE_BANDS = {-0.45F, -0.15F, 0.2F, 0.55F};
	private static final float[] HUMIDITY_BANDS = {-0.35F, -0.1F, 0.1F, 0.3F};
	/** Where the Jugcraft biomes sit in vanilla's table: further from any place's climate than vanilla's own biomes. */
	public static final Climate.ParameterPoint UNREACHABLE = Climate.parameters(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
	private static final String RULES_FILE = "/jugcraft/region_rules.json";

	/**
	 * In the layouts of the {@code layouts} bit set, entries of {@code replaces} in these temperature and humidity bands,
	 * and in this weirdness half (-1: negative, 1: positive, 0: either), become {@code biome}.
	 */
	public record Rule(int layouts, ResourceKey<Biome> replaces, int minTemperature, int maxTemperature, int minHumidity,
			int maxHumidity, int weirdness, ResourceKey<Biome> biome) {
		public boolean matches(int layout, Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry) {
			if ((layouts & 1 << layout) == 0 || !entry.getSecond().equals(replaces)) {
				return false;
			}
			Climate.ParameterPoint point = entry.getFirst();
			int temperature = temperatureBand(point);
			int humidity = humidityBand(point);
			return temperature >= minTemperature && temperature <= maxTemperature && humidity >= minHumidity && humidity <= maxHumidity
					&& (weirdness == 0 || weirdness == half(point.weirdness()));
		}
	}

	private static final List<Rule> RULES = loadRules();

	private static volatile List<List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>> layouts;
	private static volatile long seedPrint = mix(0L);
	private static volatile int cellQuarts = SIZE / 4;
	private static volatile int share = (int) (SHARE * 65536);

	private JugcraftRegions() {
	}

	/** The rules, from the generated {@value #RULES_FILE}; none (and an error logged) if it cannot be read. */
	private static List<Rule> loadRules() {
		List<Rule> rules = new ArrayList<>();
		try (InputStream stream = JugcraftRegions.class.getResourceAsStream(RULES_FILE)) {
			if (stream == null) {
				throw new IOException("missing");
			}
			JsonArray array = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
			for (JsonElement element : array) {
				JsonObject rule = element.getAsJsonObject();
				int mask = 0;
				for (JsonElement layout : rule.getAsJsonArray("layouts")) {
					mask |= 1 << layout.getAsInt();
				}
				JsonArray temperature = rule.getAsJsonArray("temperature");
				JsonArray humidity = rule.getAsJsonArray("humidity");
				rules.add(new Rule(mask, biomeKey(rule.get("replaces").getAsString()), temperature.get(0).getAsInt(),
						temperature.get(1).getAsInt(), humidity.get(0).getAsInt(), humidity.get(1).getAsInt(),
						rule.get("weirdness").getAsInt(), biomeKey(rule.get("biome").getAsString())));
			}
		} catch (IOException | RuntimeException e) {
			Jugcraft.LOGGER.error("Could not read the Jugcraft region rules {}; Jugcraft regions stay vanilla", RULES_FILE, e);
			return List.of();
		}
		return List.copyOf(rules);
	}

	private static ResourceKey<Biome> biomeKey(String id) {
		return ResourceKey.create(Registries.BIOME, Identifier.parse(id));
	}

	/** The rules, in order (the first that matches an entry decides it). */
	public static List<Rule> rules() {
		return RULES;
	}

	/**
	 * Reads the region options (after the config has loaded), and the world seed as each Overworld loads (before the
	 * server places the start or generates any chunk).
	 */
	public static void register() {
		ServerLevelEvents.LOAD.register((server, level) -> {
			if (level.dimension() == Level.OVERWORLD) {
				seedPrint = fingerprint(level.getSeed());
			}
		});
		int size = SIZE;
		double fraction = SHARE;
		try {
			size = Math.max(256, Math.min(8192, Integer.parseInt(JugcraftConfig.textOption("biomes.region_size").trim())));
		} catch (NumberFormatException e) {
			Jugcraft.LOGGER.warn("biomes.region_size is not a number of blocks; using {}", SIZE);
		}
		try {
			fraction = Math.max(0.0, Math.min(1.0, Double.parseDouble(JugcraftConfig.textOption("biomes.region_share").trim())));
		} catch (NumberFormatException e) {
			Jugcraft.LOGGER.warn("biomes.region_share is not a fraction from 0 to 1; using {}", SHARE);
		}
		cellQuarts = size / 4;
		share = (int) Math.round(fraction * 65536);
	}

	public static boolean enabled() {
		return JugcraftConfig.isFeatureEnabled(FEATURE);
	}

	/** The Jugcraft biomes the rules place, in rule order. */
	public static Set<ResourceKey<Biome>> biomes() {
		Set<ResourceKey<Biome>> out = new LinkedHashSet<>();
		for (Rule rule : RULES) {
			out.add(rule.biome());
		}
		return out;
	}

	/** The band (0 to 4) where the middle of a climate parameter's range falls. */
	private static int band(Climate.Parameter parameter, float[] edges) {
		long middle = (parameter.min() + parameter.max()) / 2;
		int band = 0;
		for (float edge : edges) {
			if (middle >= Climate.quantizeCoord(edge)) {
				band++;
			}
		}
		return band;
	}

	/** The temperature band (0, coldest, to 4) of a climate entry. */
	public static int temperatureBand(Climate.ParameterPoint point) {
		return band(point.temperature(), TEMPERATURE_BANDS);
	}

	/** The humidity band (0, driest, to 4) of a climate entry. */
	public static int humidityBand(Climate.ParameterPoint point) {
		return band(point.humidity(), HUMIDITY_BANDS);
	}

	/** The weirdness half where the middle of the range falls: -1 below zero, 1 from zero up. */
	public static int half(Climate.Parameter parameter) {
		return (parameter.min() + parameter.max()) / 2 < 0 ? -1 : 1;
	}

	/**
	 * A vanilla table entry cut along the temperature and humidity band edges and at zero weirdness, so that each piece
	 * lies in one band of each and one weirdness half, and rules match exact climates (vanilla's entries often span
	 * several bands: a swamp entry covers cool and temperate). The pieces cover what the entry covered.
	 */
	public static List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> split(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry) {
		Climate.ParameterPoint point = entry.getFirst();
		List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> out = new ArrayList<>();
		for (Climate.Parameter temperature : cut(point.temperature(), TEMPERATURE_BANDS)) {
			for (Climate.Parameter humidity : cut(point.humidity(), HUMIDITY_BANDS)) {
				for (Climate.Parameter weirdness : cut(point.weirdness(), WEIRDNESS_HALVES)) {
					out.add(Pair.of(new Climate.ParameterPoint(temperature, humidity, point.continentalness(), point.erosion(), point.depth(),
							weirdness, point.offset()), entry.getSecond()));
				}
			}
		}
		return out;
	}

	private static final float[] WEIRDNESS_HALVES = {0.0F};

	private static List<Climate.Parameter> cut(Climate.Parameter parameter, float[] edges) {
		List<Climate.Parameter> out = new ArrayList<>();
		long start = parameter.min();
		for (float edge : edges) {
			long at = Climate.quantizeCoord(edge);
			if (at > start && at < parameter.max()) {
				out.add(new Climate.Parameter(start, at));
				start = at;
			}
		}
		out.add(new Climate.Parameter(start, parameter.max()));
		return out;
	}

	/** The biome a vanilla table entry (or a piece of one, {@link #split}) has in a layout. */
	public static ResourceKey<Biome> regional(int layout, Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry) {
		for (Rule rule : RULES) {
			if (rule.matches(layout, entry)) {
				return rule.biome();
			}
		}
		return entry.getSecond();
	}

	/** The layouts last recorded from the Overworld biome builder, by index (null: none yet, or regions are off). */
	public static List<List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>> layouts() {
		return layouts;
	}

	/** A consumer for the Overworld biome builder that passes entries on and records the Jugcraft layout; null if off. */
	public static Recorder recorder(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> vanilla) {
		return enabled() ? new Recorder(vanilla) : null;
	}

	/** Records the layouts while vanilla's table is built. */
	public static final class Recorder implements Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> {
		private final Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> vanilla;
		private final List<List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>> recorded = new ArrayList<>();

		private Recorder(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> vanilla) {
			this.vanilla = vanilla;
			for (int layout = 0; layout < LAYOUTS; layout++) {
				recorded.add(new ArrayList<>());
			}
		}

		@Override
		public void accept(Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry) {
			vanilla.accept(entry);
			for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> piece : split(entry)) {
				for (int layout = 0; layout < LAYOUTS; layout++) {
					recorded.get(layout).add(Pair.of(piece.getFirst(), regional(layout, piece)));
				}
			}
		}

		/** Lists the Jugcraft biomes in vanilla's table where no climate reaches them, and publishes the layouts. */
		public void finish() {
			for (ResourceKey<Biome> biome : biomes()) {
				vanilla.accept(Pair.of(UNREACHABLE, biome));
			}
			layouts = recorded.stream().map(List::copyOf).toList();
		}
	}

	/**
	 * The layouts with this biome source's own holders; null if it is not an Overworld with the Jugcraft biomes (every
	 * biome the rules place).
	 */
	public static List<Climate.ParameterList<Holder<Biome>>> layoutsFor(Set<Holder<Biome>> possible) {
		List<List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>> tables = layouts;
		if (tables == null) {
			return null;
		}
		Map<ResourceKey<Biome>, Holder<Biome>> byKey = new HashMap<>();
		for (Holder<Biome> holder : possible) {
			holder.unwrapKey().ifPresent(key -> byKey.put(key, holder));
		}
		if (!byKey.keySet().containsAll(biomes())) {
			return null;
		}
		List<Climate.ParameterList<Holder<Biome>>> out = new ArrayList<>();
		for (List<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> table : tables) {
			List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = new ArrayList<>(table.size());
			for (Pair<Climate.ParameterPoint, ResourceKey<Biome>> entry : table) {
				Holder<Biome> holder = byKey.get(entry.getSecond());
				if (holder == null) {
					return null;
				}
				values.add(Pair.of(entry.getFirst(), holder));
			}
			out.add(new Climate.ParameterList<>(values));
		}
		return List.copyOf(out);
	}

	/** The number regions are drawn from for a world seed. */
	public static long fingerprint(long seed) {
		return mix(seed ^ 0x5DEECE66DL);
	}

	/**
	 * The layout of the Jugcraft region at quart coordinates (block / 4), for a seed's {@link #fingerprint}; -1 where
	 * the place is in a vanilla region.
	 */
	public static int regionOf(long fingerprint, int quartX, int quartZ) {
		int cell = cellQuarts;
		int cx = Math.floorDiv(quartX, cell);
		int cz = Math.floorDiv(quartZ, cell);
		long best = Long.MAX_VALUE;
		long chosen = 0;
		// The nearest of the jittered cell centres around: irregular cells (a Voronoi diagram).
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				long hash = mix(fingerprint ^ mix((cx + dx) * 0x9E3779B97F4A7C15L ^ (cz + dz) * 0xC2B2AE3D27D4EB4FL));
				long px = (long) (cx + dx) * cell + ((hash & 0xFFFF) * cell >>> 16);
				long pz = (long) (cz + dz) * cell + (((hash >>> 16) & 0xFFFF) * cell >>> 16);
				long distance = (quartX - px) * (quartX - px) + (quartZ - pz) * (quartZ - pz);
				if (distance < best) {
					best = distance;
					chosen = hash;
				}
			}
		}
		if (((chosen >>> 32) & 0xFFFF) >= share) {
			return -1;
		}
		return (int) (((chosen >>> 48) & 0xFFFF) * LAYOUTS >>> 16);
	}

	/** Whether the place at quart coordinates is in a Jugcraft region (of any layout). */
	public static boolean isJugcraft(long fingerprint, int quartX, int quartZ) {
		return regionOf(fingerprint, quartX, quartZ) >= 0;
	}

	private static long mix(long value) {
		value ^= value >>> 33;
		value *= 0xFF51AFD7ED558CCDL;
		value ^= value >>> 33;
		value *= 0xC4CEB9FE1A85EC53L;
		return value ^ value >>> 33;
	}

	/** One biome source's region state: its layouts, built once (held by the mixin). */
	public static final class Source {
		private volatile List<Climate.ParameterList<Holder<Biome>>> regional;
		private volatile boolean checked;

		/**
		 * The biome for a climate {@code target} at quart coordinates {@code x}, {@code z} if they lie in a Jugcraft region
		 * of this source; null to let vanilla answer.
		 */
		public Holder<Biome> biome(BiomeSource source, int x, int z, Climate.TargetPoint target) {
			if (!enabled()) {
				return null;
			}
			int layout = regionOf(seedPrint, x, z);
			if (layout < 0) {
				return null;
			}
			if (!checked) {
				regional = layoutsFor(source.possibleBiomes());
				checked = true;
			}
			List<Climate.ParameterList<Holder<Biome>>> lists = regional;
			return lists == null ? null : lists.get(layout).findValue(target);
		}
	}
}
