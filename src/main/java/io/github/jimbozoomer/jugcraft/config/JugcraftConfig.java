package io.github.jimbozoomer.jugcraft.config;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Server-side feature switches and options, read once at startup from config/jugcraft.properties
 * as {@code <feature>.enabled} and {@link #TEXT_OPTIONS}. A switch disables acquisition (worldgen and recipes);
 * it never unregisters content, so saved blocks and items survive.
 */
public final class JugcraftConfig {
	/** Every feature switch. Keep in sync with FEATURES in tools/materials.py. */
	public static final List<String> FEATURES = List.of(
			"tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium", "titanium", "thallite", "aluminum",
			"salt", "phosphate", "lithium", "rare_earths", "sulfur", "silicon", "crude_oil", "machines",
			"deposits", "explosives", "agriculture", "parties", "drones",
			"pixel_hollows", "retro_trader", "alpine_spawn", "biomes", "town", "diagonal_connections", "raiders");

	/**
	 * Other server options, with their defaults. {@code carving.free_draw}: players may carve any face into a
	 * pumpkin; false allows only the starter faces (for servers that want no free drawing).
	 */
	public static final Map<String, Boolean> OPTIONS = Map.of("carving.free_draw", true);

	/**
	 * Text options, with their defaults.
	 * <ul>
	 * <li>The Halloween event (trick-or-treating) runs from {@code halloween.start} to {@code halloween.end} (month-day,
	 * both included) in {@code halloween.timezone} on the server's clock; {@code halloween.mode} is {@code auto} (follow
	 * the dates), {@code on} or {@code off} (for testing and off-season worlds); {@code halloween.harvest_moon} is the
	 * day (month-day) of the Harvest Moon, whose nights make crops grow faster. Treats already given are kept whatever
	 * the setting.</li>
	 * <li>Seasons (see {@code season/SeasonCalendar.Settings}): the season follows the server's date in
	 * {@code seasons.timezone} for {@code seasons.hemisphere} ({@code north} or {@code south}); {@code seasons.mode} is
	 * {@code auto} (follow the date), {@code spring}, {@code summer}, {@code autumn}, {@code winter} (always that
	 * season) or {@code off} (vanilla colours). {@code seasons.snow} ({@code off} or {@code on}) lets winter lay snow,
	 * up to {@code seasons.snow_depth} layers, that melts in spring. Events on the same clock: the Harvest Feast
	 * ({@code harvest_feast}: {@code us}, {@code canada} or {@code off}, lasting {@code harvest_feast.days}) and
	 * December ({@code december}: {@code MM-DD..MM-DD} or {@code off}).</li>
	 * <li>{@code alpine_spawn.start} ({@code on} or {@code off}): new worlds start in the Alpine Spawn biome.</li>
	 * <li>Jugcraft regions (see {@code biome/JugcraftRegions}): {@code biomes.region_size} (blocks across, 256 to 8192)
	 * and {@code biomes.region_share} (the fraction of regions with the biomes branch's biomes, 0 to 1).</li>
	 * <li>Party limits (see {@code party/JugcraftParties}): {@code parties.max_size} members (2 to 64),
	 * {@code parties.invite_minutes} before an invite expires (1 to 60) and {@code parties.invites_per_minute}
	 * each player may send (1 to 60).</li>
	 * <li>{@code town.protection} ({@code on} or {@code off}) keeps the walled town as it was built (town/TownProtection).</li>
	 * <li>Raids (see {@code raiders/RaiderRaids}): {@code raiders.raids} ({@code on} or {@code off}); a player is raided only
	 * after {@code raiders.grace_days} days of play, and the world at most once every {@code raiders.interval_days} days;
	 * {@code raiders.walkers} and {@code raiders.blimps} ({@code on} or {@code off}) let walkers and blimps join them.</li>
	 * </ul>
	 */
	public static final Map<String, String> TEXT_OPTIONS = Map.ofEntries(
			Map.entry("halloween.start", "10-20"), Map.entry("halloween.end", "11-03"), Map.entry("halloween.timezone", "UTC"),
			Map.entry("halloween.mode", "auto"), Map.entry("halloween.harvest_moon", "10-31"),
			Map.entry("seasons.mode", "auto"), Map.entry("seasons.hemisphere", "north"), Map.entry("seasons.timezone", "UTC"),
			Map.entry("seasons.snow", "off"), Map.entry("seasons.snow_depth", "2"),
			Map.entry("harvest_feast", "us"), Map.entry("harvest_feast.days", "4"),
			Map.entry("december", "12-01..01-06"),
			Map.entry("parties.max_size", "8"), Map.entry("parties.invite_minutes", "5"), Map.entry("parties.invites_per_minute", "10"),
			Map.entry("alpine_spawn.start", "on"),
			Map.entry("biomes.region_size", "1024"), Map.entry("biomes.region_share", "0.5"),
			Map.entry("town.protection", "on"),
			Map.entry("raiders.raids", "on"), Map.entry("raiders.grace_days", "3"), Map.entry("raiders.interval_days", "3"),
			Map.entry("raiders.walkers", "on"), Map.entry("raiders.blimps", "on"));

	private static final String FILE_NAME = "jugcraft.properties";
	private static final Map<String, Boolean> ENABLED = new LinkedHashMap<>();
	private static final Map<String, Boolean> OPTION_VALUES = new LinkedHashMap<>(OPTIONS);
	private static final Map<String, String> TEXT_VALUES = new LinkedHashMap<>(TEXT_OPTIONS);

	private JugcraftConfig() {
	}

	public static boolean isFeatureEnabled(String feature) {
		return ENABLED.getOrDefault(feature, false);
	}

	/** A server option from {@link #OPTIONS}, as set in the file (its default before {@link #load}). */
	public static boolean option(String key) {
		Boolean value = OPTION_VALUES.get(key);
		if (value == null) {
			throw new IllegalArgumentException("No Jugcraft option " + key);
		}
		return value;
	}

	/** A text option from {@link #TEXT_OPTIONS}, as set in the file (its default before {@link #load}). */
	public static String textOption(String key) {
		String value = TEXT_VALUES.get(key);
		if (value == null) {
			throw new IllegalArgumentException("No Jugcraft option " + key);
		}
		return value;
	}

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		Properties properties = new Properties();

		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				properties.load(reader);
			} catch (IOException e) {
				Jugcraft.LOGGER.error("Could not read {}; using defaults", path, e);
			}
		}

		for (String feature : FEATURES) {
			String key = feature + ".enabled";
			boolean enabled = Boolean.parseBoolean(properties.getProperty(key, "true"));
			ENABLED.put(feature, enabled);
			properties.setProperty(key, Boolean.toString(enabled));
		}

		for (Map.Entry<String, Boolean> option : OPTIONS.entrySet()) {
			boolean value = Boolean.parseBoolean(properties.getProperty(option.getKey(), option.getValue().toString()));
			OPTION_VALUES.put(option.getKey(), value);
			properties.setProperty(option.getKey(), Boolean.toString(value));
		}

		for (Map.Entry<String, String> option : TEXT_OPTIONS.entrySet()) {
			String value = properties.getProperty(option.getKey(), option.getValue()).trim();
			TEXT_VALUES.put(option.getKey(), value);
			properties.setProperty(option.getKey(), value);
		}

		try (Writer writer = Files.newBufferedWriter(path)) {
			properties.store(writer, "Jugcraft feature switches (false stops new worldgen and recipes; existing items and blocks stay)"
					+ " and server options (carving.free_draw=false allows only the starter pumpkin faces; halloween.* sets when"
					+ " the Halloween event runs: start and end as MM-DD, a timezone, mode auto, on or off, and the Harvest Moon's day;"
					+ " seasons.*: seasonal colours follow the server's date; mode auto, spring, summer, autumn, winter or off,"
					+ " hemisphere north or south, a timezone, and opt-in winter snow; harvest_feast us, canada or off;"
					+ " december MM-DD..MM-DD or off; parties.max_size 2-64, parties.invite_minutes 1-60, parties.invites_per_minute 1-60;"
					+ " alpine_spawn.start on or off: new worlds start in the Alpine Spawn biome;"
					+ " biomes.region_size in blocks and biomes.region_share from 0 to 1: Jugcraft regions with the new biomes;"
					+ " town.protection on or off; raiders.raids on or off, raiders.grace_days and raiders.interval_days in game days,"
					+ " raiders.walkers and raiders.blimps on or off).");
		} catch (IOException e) {
			Jugcraft.LOGGER.warn("Could not write {}", path, e);
		}
	}
}
