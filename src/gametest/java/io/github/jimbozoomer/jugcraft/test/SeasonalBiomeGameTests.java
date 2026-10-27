package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Squirrels;
import io.github.jimbozoomer.jugcraft.agriculture.Turkeys;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import io.github.jimbozoomer.jugcraft.agriculture.Wisps;
import io.github.jimbozoomer.jugcraft.lair.HagStoneRite;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonalBiome;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * The seasonal biome audit (docs/features/seasonal-biome-audit.md): the landscape collection's temperate biomes turn
 * with the seasons as Jugcraft's own do, its mild ones without winter snow; its frozen, tropical, arid, magic, coastal,
 * cave and other-dimension biomes, the caverns and the lairs' own biomes do not; across the whole biome registry every
 * seasonal biome rains and is neither frozen nor tropical. The werewolves, squirrels, turkeys and wisps find the
 * landscape collection's woods, open country, snowy land and swamps; the fall's wild patches, the graveyard flowers and
 * the Hag Stone reach them too, through the same kinds.
 */
public class SeasonalBiomeGameTests {
	/** Temperate landscape biomes: seasons and winter snow. */
	static final List<String> SEASONAL = List.of("landscape_seasonal_forest", "landscape_maple_woods", "landscape_pumpkin_patch",
			"landscape_birch_taiga", "landscape_wetland", "landscape_prairie", "landscape_temperate_highlands");
	/** Mild landscape biomes: seasons, no winter snow. */
	static final List<String> MILD = List.of("landscape_mediterranean_forest", "landscape_lavender_field", "landscape_redwood_forest");
	/** Neither: one of each other kind, the caverns and the lairs' and pocket dimensions' biomes. */
	static final List<String> NONE = List.of("landscape_wintry_forest", "landscape_tropics", "landscape_desert_canyon",
			"landscape_skylands_autumn", "landscape_mystic_grove", "landscape_ominous_woods", "landscape_dune_beach",
			"landscape_warm_river", "landscape_white_cliffs", "landscape_volcano", "cave/landscape_deep_caves",
			"landscape_erupting_inferno", "landscape_end_wilds", "cavern_candy_cavity", "hollow_acre", "moonlit_ring", "lantern_fen",
			"nouveau_jar");

	private static Holder<Biome> biome(Registry<Biome> biomes, String path) {
		return biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Jugcraft.id(path)));
	}

	@GameTest
	public void landscapeBiomesFollowTheSeasons(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		for (String path : SEASONAL) {
			Holder<Biome> holder = biome(biomes, path);
			helper.assertTrue(holder.is(JugcraftSeasons.HAS_SEASONS) && SeasonalBiome.of(holder.value()).jugcraft$hasSeasons(),
					path + " has no seasons");
			helper.assertTrue(holder.is(JugcraftSeasons.HAS_WINTER_SNOW) && SeasonalBiome.of(holder.value()).jugcraft$hasWinterSnow(),
					path + " gets no winter snow");
		}
		for (String path : MILD) {
			Holder<Biome> holder = biome(biomes, path);
			helper.assertTrue(holder.is(JugcraftSeasons.HAS_SEASONS), path + " has no seasons");
			helper.assertTrue(!holder.is(JugcraftSeasons.HAS_WINTER_SNOW) && !SeasonalBiome.of(holder.value()).jugcraft$hasWinterSnow(),
					path + " has a mild winter but gets winter snow");
		}
		for (String path : NONE) {
			Holder<Biome> holder = biome(biomes, path);
			helper.assertTrue(!holder.is(JugcraftSeasons.HAS_SEASONS) && !SeasonalBiome.of(holder.value()).jugcraft$hasSeasons(),
					path + " has seasons");
			helper.assertTrue(!holder.is(JugcraftSeasons.HAS_WINTER_SNOW), path + " gets winter snow");
		}
		// Across the registry: a seasonal biome rains (not snow all year, not dry) and is not tropical; winter snow
		// falls only where it rains.
		BlockPos at = helper.absolutePos(BlockPos.ZERO);
		biomes.listElements().forEach(holder -> {
			String name = holder.getRegisteredName();
			boolean rains = holder.value().getPrecipitationAt(at, level.getSeaLevel()) != Biome.Precipitation.NONE;
			float temperature = holder.value().getBaseTemperature();
			if (holder.is(JugcraftSeasons.HAS_SEASONS)) {
				helper.assertTrue(rains && temperature >= 0.15F && temperature <= 0.9F,
						name + " has seasons but rain " + rains + " and temperature " + temperature);
			}
			if (holder.is(JugcraftSeasons.HAS_WINTER_SNOW)) {
				helper.assertTrue(rains, name + " gets winter snow but no precipitation");
			}
		});
		helper.succeed();
	}

	@GameTest
	public void landscapeCreaturesFindTheirHaunts(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		record Expect(TagKey<Biome> tag, String what, List<String> in, List<String> out) {
		}
		List<Expect> expected = List.of(
				new Expect(Werewolves.HAUNTS, "werewolves", List.of("landscape_maple_woods", "landscape_coniferous_forest", "landscape_ominous_woods",
						"landscape_snowy_maple_woods"), List.of("landscape_prairie", "landscape_tropics")),
				new Expect(Werewolves.SNOW_HAUNTS, "snow werewolves", List.of("landscape_wintry_forest", "landscape_snowy_shield"),
						List.of("landscape_maple_woods")),
				new Expect(Werewolves.SHADOW_HAUNTS, "shadow werewolves", List.of("landscape_ominous_woods"), List.of("landscape_woodland")),
				new Expect(Squirrels.HABITAT, "squirrels", List.of("landscape_woodland", "landscape_birch_taiga"), List.of("landscape_steppe")),
				new Expect(Turkeys.HABITAT, "turkeys", List.of("landscape_prairie", "landscape_maple_woods", "landscape_pasture"),
						List.of("landscape_wintry_lowlands", "landscape_desert_canyon")),
				new Expect(Wisps.HAUNTS, "wisps", List.of("landscape_bayou", "landscape_bog", "landscape_marsh"), List.of("landscape_field")));
		for (Expect expect : expected) {
			for (String path : expect.in()) {
				helper.assertTrue(biome(biomes, path).is(expect.tag()), "The " + expect.what() + " do not haunt " + path);
			}
			for (String path : expect.out()) {
				helper.assertTrue(!biome(biomes, path).is(expect.tag()), "The " + expect.what() + " haunt " + path);
			}
		}
		helper.assertTrue(biomes.getOrThrow(Biomes.SWAMP).is(Wisps.HAUNTS), "Wisps no longer haunt vanilla's swamp");
		helper.succeed();
	}

	/** Over a landscape swamp, under the open sky and away from corn, a wisp may appear; over landscape fields it may not. */
	@GameTest(skyAccess = true)
	public void wispsComeOutOverLandscapeSwamps(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.MUD);
			}
		}
		BlockPos spot = helper.absolutePos(new BlockPos(4, 2, 4));
		fill(helper, "jugcraft:landscape_bayou");
		helper.assertTrue(level.getBiome(spot).is(ResourceKey.create(Registries.BIOME, Jugcraft.id("landscape_bayou"))),
				"fillbiome did not make the test a bayou: " + level.getBiome(spot).getRegisteredName());
		helper.assertTrue(Wisps.canSpawnAt(level, spot), "No wisp may appear over the landscape bayou");
		fill(helper, "jugcraft:landscape_field");
		helper.assertTrue(!Wisps.canSpawnAt(level, spot), "A wisp may appear over a landscape field away from corn");
		helper.succeed();
	}

	/**
	 * The fall's wild patches and the graveyard flowers reach the landscape biomes of their kinds, and the other wild crops
	 * do not; the Hag Stone's lands hold the landscape swamps. Fabric places the patches in the Overworld's biomes, so each
	 * landscape biome is held to the vanilla biome of its kind: whenever the vanilla one has a fall patch, the landscape one
	 * has it too, and a wild crop that is not a fall patch stays out of the landscape one (the log says which case this
	 * test world showed).
	 */
	@GameTest
	public void landscapeBiomesTakeTheFallsPatches(GameTestHelper helper) {
		Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
		Object[][] pairs = {
				{Biomes.FOREST, "landscape_maple_woods", "chestnut_tree"},
				{Biomes.FOREST, "landscape_woodland", "chanterelle"},
				{Biomes.TAIGA, "landscape_siberian_taiga", "porcini"},
				{Biomes.PLAINS, "landscape_pasture", "butternut_squash"},
				{Biomes.SWAMP, "landscape_bog", "cranberry_bush"},
				{Biomes.SWAMP, "landscape_bog", "spider_lily"}};
		int placed = 0;
		for (Object[] pair : pairs) {
			@SuppressWarnings("unchecked")
			Holder<Biome> vanilla = biomes.getOrThrow((ResourceKey<Biome>) pair[0]);
			Holder<Biome> landscape = biome(biomes, (String) pair[1]);
			String patch = "patch_" + pair[2];
			if (hasPatch(vanilla, patch)) {
				placed++;
				helper.assertTrue(hasPatch(landscape, patch), pair[1] + " has no " + patch + ", though " + vanilla.getRegisteredName() + " has it");
			}
		}
		// Wild rice is no fall patch: it keeps to the conventional swamps.
		helper.assertTrue(!hasPatch(biome(biomes, "landscape_bog"), "patch_wild_rice"), "The landscape bog has wild rice, which is no fall patch");
		org.slf4j.LoggerFactory.getLogger("SeasonalBiomeGameTests").info("[landscape patches] {} of {} vanilla biomes carry their fall patch in this test world; "
				+ "each landscape counterpart did too", placed, pairs.length);
		helper.assertTrue(biome(biomes, "landscape_bog").is(HagStoneRite.LANDS) && biome(biomes, "landscape_marsh").is(HagStoneRite.LANDS),
				"The landscape swamps are not in #jugcraft:hag_stone_lands");
		helper.assertTrue(!biome(biomes, "landscape_field").is(HagStoneRite.LANDS), "A landscape field is in #jugcraft:hag_stone_lands");
		helper.assertTrue(biome(biomes, "landscape_ominous_woods").is(TagKey.create(Registries.BIOME, Jugcraft.id("landscape_kind/dark_woods"))),
				"The Ominous Woods are not in #jugcraft:landscape_kind/dark_woods");
		helper.succeed();
	}

	/** Whether {@code biome} lists {@code patch} (a Jugcraft placed feature) in its vegetation step. */
	private static boolean hasPatch(Holder<Biome> biome, String patch) {
		List<net.minecraft.core.HolderSet<PlacedFeature>> steps = biome.value().getGenerationSettings().features();
		int step = GenerationStep.Decoration.VEGETAL_DECORATION.ordinal();
		ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id(patch));
		return step < steps.size() && steps.get(step).stream().anyMatch(holder -> holder.is(key));
	}

	/** Fills the test's area, and 5 blocks round it (a biome lookup blends neighbouring cells), with {@code biome}. */
	private static void fill(GameTestHelper helper, String biome) {
		MinecraftServer server = helper.getLevel().getServer();
		BlockPos low = helper.absolutePos(new BlockPos(-5, -4, -5));
		BlockPos high = helper.absolutePos(new BlockPos(13, 8, 13));
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), String.format(Locale.ROOT,
				"fillbiome %d %d %d %d %d %d %s", low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ(), biome));
	}
}
