package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import io.github.jimbozoomer.jugcraft.season.SeasonCommand;
import io.github.jimbozoomer.jugcraft.season.SeasonPalette;
import io.github.jimbozoomer.jugcraft.season.SeasonState;
import io.github.jimbozoomer.jugcraft.season.SeasonalBiome;
import io.github.jimbozoomer.jugcraft.season.SeasonalSnow;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Seasons: the server's calendar and events, the operator's settings and command, the palette, the biome tags and winter snow. */
public class SeasonGameTests {
	@GameTest
	public void seasonDaysFollowTheCalendar(GameTestHelper helper) {
		// 20 October is peak autumn in the north and spring (half a year on) in the south.
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2026, 10, 20), false), 293, "20 October, north");
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2026, 10, 20), true), 110, "20 October, south");
		// The year turns over cleanly in both hemispheres, and leap years still end on day 365.
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2026, 12, 31), false), 365, "31 December");
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2027, 1, 1), false), 1, "1 January");
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2027, 1, 1), true), 183, "1 January, south");
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2028, 12, 31), false), 365, "31 December, leap year");
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2028, 2, 29), false),
				SeasonCalendar.seasonDay(LocalDate.of(2028, 2, 28), false), "29 February shares the 28th");
		equal(helper, SeasonCalendar.seasonDay(LocalDate.of(2028, 3, 1), false),
				SeasonCalendar.seasonDay(LocalDate.of(2026, 3, 1), false), "1 March in a leap year");
		for (LocalDate date = LocalDate.of(2028, 1, 1); date.getYear() == 2028; date = date.plusDays(1)) {
			for (boolean southern : new boolean[] {false, true}) {
				int day = SeasonCalendar.seasonDay(date, southern);
				helper.assertTrue(day >= 1 && day <= SeasonCalendar.DAYS, "Season day " + day + " on " + date + " is outside the year");
			}
		}
		helper.succeed();
	}

	@GameTest
	public void modesAndTimeZonesSetTheDay(GameTestHelper helper) {
		LocalDate june = LocalDate.of(2026, 6, 1);
		SeasonCalendar.Settings settings = SeasonCalendar.Settings.DEFAULT.withMode(SeasonCalendar.Mode.AUTO);
		equal(helper, settings.dayOn(june), SeasonCalendar.seasonDay(june, false), "auto follows the date");
		equal(helper, settings.withMode(SeasonCalendar.Mode.AUTUMN).dayOn(june), 293, "autumn override");
		equal(helper, settings.withMode(SeasonCalendar.Mode.WINTER).dayOn(june), 15, "winter override");
		equal(helper, settings.withMode(SeasonCalendar.Mode.OFF).dayOn(june), 0, "off");
		equal(helper, SeasonCalendar.Mode.parse(" Autumn "), SeasonCalendar.Mode.AUTUMN, "mode text");
		// The server's zone decides when the day turns: 23:30 UTC on 31 December is already New Year in Auckland.
		Instant instant = Instant.parse("2026-12-31T23:30:00Z");
		equal(helper, settings.dayOn(LocalDate.ofInstant(instant, ZoneOffset.UTC)), 365, "UTC");
		equal(helper, settings.dayOn(LocalDate.ofInstant(instant, ZoneId.of("Pacific/Auckland"))), 1, "Auckland");
		helper.succeed();
	}

	@GameTest
	public void overridesLastUntilRestart(GameTestHelper helper) {
		// The test server's config file has the defaults. Overrides change the settings in memory only, and a server
		// starting (a restart, or the next world opened in the same game) begins from the file again.
		MinecraftServer server = helper.getLevel().getServer();
		SeasonCalendar.Settings fromFile = SeasonCalendar.Settings.fromConfig();
		equal(helper, fromFile, SeasonCalendar.Settings.DEFAULT, "settings in the test config");
		try {
			JugcraftSeasons.setMode(server, SeasonCalendar.Mode.WINTER);
			JugcraftSeasons.setSnow(server, true);
			JugcraftSeasons.setFixedDate(server, MonthDay.of(1, 10));
			helper.assertTrue(SeasonState.serverSnowing(), "Not snowing on a 10 January preview with snow on");
			equal(helper, SeasonCalendar.Settings.fromConfig(), fromFile, "settings in the file after overrides");
			JugcraftSeasons.serverStarted(server);
			equal(helper, JugcraftSeasons.settings(), fromFile, "settings after a server start");
			helper.assertTrue(!SeasonState.serverSnowing(), "Still snowing after a server start with seasons.snow off");
		} finally {
			JugcraftSeasons.serverStarted(server);
		}
		helper.succeed();
	}

	@GameTest
	public void paletteKeepsSummerAndTurnsAutumn(GameTestHelper helper) {
		int foliage = 0xFF77AB2F;
		int grass = 0xFF91BD59;
		equal(helper, SeasonPalette.colour(0, foliage, true, 10, 20), foliage, "off");
		equal(helper, SeasonPalette.colour(SeasonCalendar.Mode.SUMMER.day, foliage, true, 10, 20), foliage, "summer foliage");
		equal(helper, SeasonPalette.colour(SeasonCalendar.Mode.SUMMER.day, grass, false, 10, 20), grass, "summer grass");
		int autumn = SeasonPalette.colour(SeasonCalendar.Mode.AUTUMN.day, foliage, true, 10, 20);
		helper.assertTrue((autumn >>> 24) == 0xFF, "Autumn lost the tint's alpha");
		helper.assertTrue((autumn >> 16 & 0xFF) > (foliage >> 16 & 0xFF) + 40, "Autumn leaves are not redder: " + Integer.toHexString(autumn));
		helper.assertTrue(SeasonPalette.colour(SeasonCalendar.Mode.WINTER.day, grass, false, 10, 20) != grass, "Winter grass is vanilla");
		// Colours change gradually, day by day and across New Year, and the same place gets the same colour.
		for (int day = 1; day <= SeasonCalendar.DAYS; day++) {
			int next = day % SeasonCalendar.DAYS + 1;
			for (boolean isFoliage : new boolean[] {true, false}) {
				int vanilla = isFoliage ? foliage : grass;
				int a = SeasonPalette.colour(day, vanilla, isFoliage, 37, -81);
				int b = SeasonPalette.colour(next, vanilla, isFoliage, 37, -81);
				for (int shift = 0; shift <= 16; shift += 8) {
					int step = Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF));
					helper.assertTrue(step <= 4, "Colour jumps by " + step + " from day " + day + " to " + next);
				}
			}
		}
		equal(helper, SeasonPalette.colour(293, foliage, true, 5, 9), SeasonPalette.colour(293, foliage, true, 5, 9), "repeatable");
		// Autumn patches show more than one colour across a forest.
		boolean redder = false;
		boolean golder = false;
		for (int x = 0; x < 256; x += 4) {
			for (int z = 0; z < 256; z += 4) {
				int hue = SeasonPalette.autumnHue(x, z);
				redder |= (hue >> 8 & 0xFF) < 0x60;
				golder |= (hue >> 8 & 0xFF) > 0xA0;
			}
		}
		helper.assertTrue(redder && golder, "Autumn foliage has no red or no gold patches");
		helper.succeed();
	}

	@GameTest
	public void fourSeasonBiomesHaveSeasons(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		for (ResourceKey<Biome> biome : List.of(Biomes.PLAINS, Biomes.FOREST, Biomes.DARK_FOREST, Biomes.TAIGA, Biomes.MEADOW,
				Biomes.SWAMP, Biomes.DAPPLED_FOREST, Biomes.CHERRY_GROVE)) {
			helper.assertTrue(biomes.getOrThrow(biome).is(JugcraftSeasons.HAS_SEASONS), biome.identifier() + " has no seasons");
			helper.assertTrue(SeasonalBiome.of(biomes.getOrThrow(biome).value()).jugcraft$hasSeasons(), biome.identifier() + "'s flag is not set");
		}
		for (ResourceKey<Biome> biome : List.of(Biomes.DESERT, Biomes.JUNGLE, Biomes.MANGROVE_SWAMP, Biomes.SAVANNA, Biomes.BADLANDS,
				Biomes.SNOWY_PLAINS, Biomes.OCEAN, Biomes.PALE_GARDEN, Biomes.RIVER)) {
			helper.assertTrue(!biomes.getOrThrow(biome).is(JugcraftSeasons.HAS_SEASONS), biome.identifier() + " has seasons");
		}
		// Winter snow: the seasonal biomes and the pale garden, not rivers (they also run through jungles and deserts).
		helper.assertTrue(biomes.getOrThrow(Biomes.PALE_GARDEN).is(JugcraftSeasons.HAS_WINTER_SNOW), "The pale garden gets no winter snow");
		helper.assertTrue(biomes.getOrThrow(Biomes.PLAINS).is(JugcraftSeasons.HAS_WINTER_SNOW), "Plains get no winter snow");
		helper.assertTrue(!biomes.getOrThrow(Biomes.RIVER).is(JugcraftSeasons.HAS_WINTER_SNOW), "Rivers get winter snow");
		helper.assertTrue(!biomes.getOrThrow(Biomes.DESERT).is(JugcraftSeasons.HAS_WINTER_SNOW), "Deserts get winter snow");
		helper.succeed();
	}

	@GameTest
	public void eventsFollowTheCalendar(GameTestHelper helper) {
		// Thanksgiving: the fourth Thursday of November in the US, the second Monday of October in Canada.
		equal(helper, SeasonCalendar.harvestFeastDay(SeasonCalendar.Feast.US, 2026), LocalDate.of(2026, 11, 26), "US 2026");
		equal(helper, SeasonCalendar.harvestFeastDay(SeasonCalendar.Feast.US, 2027), LocalDate.of(2027, 11, 25), "US 2027");
		equal(helper, SeasonCalendar.harvestFeastDay(SeasonCalendar.Feast.CANADA, 2026), LocalDate.of(2026, 10, 12), "Canada 2026");
		SeasonCalendar.Settings us = SeasonCalendar.Settings.DEFAULT;
		SeasonCalendar.Settings canada = new SeasonCalendar.Settings(SeasonCalendar.Mode.AUTO, false, us.zone(), false, 2,
				SeasonCalendar.Feast.CANADA, 4, us.december(), null);
		SeasonCalendar.Settings off = new SeasonCalendar.Settings(SeasonCalendar.Mode.AUTO, false, us.zone(), false, 2,
				SeasonCalendar.Feast.OFF, 4, null, null);
		List<SeasonCalendar.Event> feast = List.of(SeasonCalendar.Event.HARVEST_FEAST);
		List<SeasonCalendar.Event> december = List.of(SeasonCalendar.Event.DECEMBER);
		// The American feast runs from Thursday over the weekend; the Canadian one ends on its Monday.
		equal(helper, us.eventsOn(LocalDate.of(2026, 11, 25)), List.of(), "US, the Wednesday before");
		equal(helper, us.eventsOn(LocalDate.of(2026, 11, 26)), feast, "US, Thanksgiving");
		equal(helper, us.eventsOn(LocalDate.of(2026, 11, 29)), feast, "US, the Sunday after");
		equal(helper, us.eventsOn(LocalDate.of(2026, 11, 30)), List.of(), "US, the Monday after");
		equal(helper, canada.eventsOn(LocalDate.of(2026, 10, 9)), feast, "Canada, the Friday before");
		equal(helper, canada.eventsOn(LocalDate.of(2026, 10, 13)), List.of(), "Canada, the Tuesday after");
		// December runs over New Year.
		equal(helper, us.eventsOn(LocalDate.of(2026, 12, 1)), december, "1 December");
		equal(helper, us.eventsOn(LocalDate.of(2027, 1, 6)), december, "6 January");
		equal(helper, us.eventsOn(LocalDate.of(2027, 1, 7)), List.of(), "7 January");
		equal(helper, off.eventsOn(LocalDate.of(2026, 11, 26)), List.of(), "events off");
		// Events follow the calendar in both hemispheres; the snow season follows the hemisphere.
		SeasonCalendar.Settings south = new SeasonCalendar.Settings(SeasonCalendar.Mode.AUTO, true, us.zone(), true, 2,
				SeasonCalendar.Feast.US, 4, us.december(), null);
		equal(helper, south.eventsOn(LocalDate.of(2026, 11, 26)), feast, "the feast in the south");
		helper.assertTrue(us.withSnow(true).snowOn(LocalDate.of(2027, 1, 10)), "No snow on 10 January in the north");
		helper.assertTrue(!us.withSnow(true).snowOn(LocalDate.of(2027, 3, 1)), "Snow on 1 March in the north");
		helper.assertTrue(south.snowOn(LocalDate.of(2026, 7, 10)), "No snow on 10 July in the south");
		helper.assertTrue(south.snowOn(LocalDate.of(2026, 6, 1)) && south.snowOn(LocalDate.of(2026, 8, 31)),
				"The southern snow season does not run from 1 June to 31 August");
		helper.assertTrue(!south.snowOn(LocalDate.of(2026, 5, 31)) && !south.snowOn(LocalDate.of(2026, 9, 1)),
				"Snow outside June to August in the south");
		helper.assertTrue(us.withSnow(true).snowOn(LocalDate.of(2028, 2, 29)), "No snow on 29 February in the north");
		helper.assertTrue(!us.snowOn(LocalDate.of(2027, 1, 10)), "Snow while seasons.snow is off");
		// A preview date drives the season and the events.
		SeasonCalendar.Settings preview = us.withMode(SeasonCalendar.Mode.WINTER).withFixedDate(MonthDay.of(11, 26));
		equal(helper, preview.dayOn(LocalDate.of(2026, 6, 1)), 330, "preview day");
		equal(helper, preview.eventsOn(LocalDate.of(2026, 6, 1)), feast, "preview events");
		helper.succeed();
	}

	@GameTest
	public void seasonCommandChangesTheSeason(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		try {
			commandsChangeTheSeason(helper, server);
		} finally {
			JugcraftSeasons.serverStarted(server);
		}
		helper.succeed();
	}

	private static void commandsChangeTheSeason(GameTestHelper helper, MinecraftServer server) {
		CommandSourceStack operator = server.createCommandSourceStack().withSuppressedOutput();
		server.getCommands().performPrefixedCommand(operator, "jugcraft season set winter");
		equal(helper, JugcraftSeasons.today(), 15, "after set winter");
		server.getCommands().performPrefixedCommand(operator, "jugcraft season date 11-26");
		helper.assertTrue(JugcraftSeasons.isActive(SeasonCalendar.Event.HARVEST_FEAST), "No Harvest Feast on a 26 November preview");
		equal(helper, JugcraftSeasons.today(), 330, "after date 11-26");
		server.getCommands().performPrefixedCommand(operator, "jugcraft season snow on");
		helper.assertTrue(!SeasonState.serverSnowing(), "Snowing in November");
		server.getCommands().performPrefixedCommand(operator, "jugcraft season date 01-10");
		helper.assertTrue(SeasonState.serverSnowing(), "Not snowing on 10 January with snow on");
		helper.assertTrue(SeasonCommand.describe().contains("winter"), "The description does not say winter: " + SeasonCommand.describe());
		server.getCommands().performPrefixedCommand(operator, "jugcraft season snow off");
		helper.assertTrue(!SeasonState.serverSnowing(), "Snowing with snow off");
		server.getCommands().performPrefixedCommand(operator, "jugcraft season date today");
		equal(helper, JugcraftSeasons.settings().fixedDate(), null, "after date today");
		// Ending a preview goes back to the mode held before it, not to following the date.
		equal(helper, JugcraftSeasons.settings().mode(), SeasonCalendar.Mode.WINTER, "mode after date today");
		equal(helper, JugcraftSeasons.today(), 15, "season day after date today");
	}

	@GameTest
	public void winterSnowLiesAndMeltsInSpring(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		try {
			snowLiesAndMelts(helper, server);
		} finally {
			JugcraftSeasons.serverStarted(server);
		}
		helper.succeed();
	}

	private static void snowLiesAndMelts(GameTestHelper helper, MinecraftServer server) {
		ServerLevel level = helper.getLevel();
		// A biome lookup blends the 4x4x4 biome cells around the block (vanilla's BiomeManager), reaching up to 5 blocks
		// away, so the plains reach 5 blocks past every block this test reads (x 1 to 7, y 1 to 2, z 1). With only the
		// test's own blocks filled, lookups near its edge could see the natural biome of wherever the test was placed.
		BlockPos low = helper.absolutePos(new BlockPos(-4, -4, -4));
		BlockPos high = helper.absolutePos(new BlockPos(12, 7, 6));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), String.format(Locale.ROOT,
				"fillbiome %d %d %d %d %d %d minecraft:plains", low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ()));
		for (int x = 1; x <= 7; x++) {
			for (int y = 1; y <= 2; y++) {
				BlockPos read = helper.absolutePos(new BlockPos(x, y, 1));
				helper.assertTrue(level.getBiome(read).is(Biomes.PLAINS), "fillbiome did not make " + read + " plains: "
						+ level.getBiome(read).getRegisteredName());
			}
		}
		for (int x = 0; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
		}
		helper.setBlock(new BlockPos(3, 1, 1), Blocks.FARMLAND);
		helper.setBlock(new BlockPos(5, 2, 1), Blocks.SNOW);
		helper.setBlock(new BlockPos(7, 1, 1), Blocks.WATER);
		BlockPos open = helper.absolutePos(new BlockPos(1, 2, 1));
		Biome plains = level.getBiome(open).value();

		JugcraftSeasons.setMode(server, SeasonCalendar.Mode.WINTER);
		JugcraftSeasons.setSnow(server, true);
		helper.assertTrue(plains.getPrecipitationAt(open, level.getSeaLevel()) == Biome.Precipitation.SNOW, "Winter rain does not fall as snow");
		helper.assertTrue(SeasonalSnow.snowAt(level, open, 2), "No first layer of seasonal snow");
		helper.assertTrue(SeasonalSnow.snowAt(level, open, 2), "No second layer of seasonal snow");
		helper.assertTrue(!SeasonalSnow.snowAt(level, open, 2), "A third layer past the depth of 2");
		helper.assertTrue(level.getBlockState(open).is(SeasonalSnow.BLOCK) && level.getBlockState(open).getValue(SnowLayerBlock.LAYERS) == 2,
				"Not two layers of seasonal snow: " + level.getBlockState(open));
		helper.assertTrue(!SeasonalSnow.snowAt(level, helper.absolutePos(new BlockPos(3, 2, 1)), 2), "Snow on farmland");
		helper.assertTrue(!SeasonalSnow.snowAt(level, helper.absolutePos(new BlockPos(5, 2, 1)), 2), "Seasonal snow over vanilla snow");
		helper.assertBlockPresent(Blocks.SNOW, new BlockPos(5, 2, 1));
		helper.assertTrue(!SeasonalSnow.snowAt(level, helper.absolutePos(new BlockPos(7, 2, 1)), 2), "Snow on water");
		helper.assertBlockPresent(Blocks.WATER, new BlockPos(7, 1, 1));
		// Grass under the season's snow lives through winter (vanilla kills grass under anything that shuts out its
		// light but one layer of vanilla snow): it stays grass, snowy, through random ticks.
		BlockPos lawn = helper.absolutePos(new BlockPos(6, 1, 1));
		level.setBlockAndUpdate(lawn, Blocks.GRASS_BLOCK.defaultBlockState());
		helper.assertTrue(SeasonalSnow.snowAt(level, lawn.above(), 2) && SeasonalSnow.snowAt(level, lawn.above(), 2), "No snow on the grass");
		for (int tick = 0; tick < 4; tick++) {
			level.getBlockState(lawn).randomTick(level, lawn, level.getRandom());
		}
		helper.assertTrue(level.getBlockState(lawn).is(Blocks.GRASS_BLOCK) && level.getBlockState(lawn).getValue(BlockStateProperties.SNOWY),
				"Grass under two layers of seasonal snow became " + level.getBlockState(lawn));

		// Spring: the season's snow melts a layer per random tick; vanilla snow stays.
		JugcraftSeasons.setMode(server, SeasonCalendar.Mode.SPRING);
		helper.assertTrue(plains.getPrecipitationAt(open, level.getSeaLevel()) == Biome.Precipitation.RAIN, "Spring rain still falls as snow");
		level.getBlockState(open).randomTick(level, open, level.getRandom());
		helper.assertTrue(level.getBlockState(open).is(SeasonalSnow.BLOCK) && level.getBlockState(open).getValue(SnowLayerBlock.LAYERS) == 1,
				"Not one layer left after a spring tick: " + level.getBlockState(open));
		level.getBlockState(open).randomTick(level, open, level.getRandom());
		helper.assertBlockNotPresent(SeasonalSnow.BLOCK, new BlockPos(1, 2, 1));
		BlockPos vanillaSnow = helper.absolutePos(new BlockPos(5, 2, 1));
		level.getBlockState(vanillaSnow).randomTick(level, vanillaSnow, level.getRandom());
		helper.assertBlockPresent(Blocks.SNOW, new BlockPos(5, 2, 1));
	}

	private static void equal(GameTestHelper helper, Object actual, Object expected, String what) {
		helper.assertTrue(Objects.equals(actual, expected), what + ": expected " + expected + ", got " + actual);
	}
}
