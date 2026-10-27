package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * The landscape collection's wildlife (docs/features/landscape-wildlife.md): the menagerie's creatures come to the
 * landscape biomes by the habitats tools/landscape_habitats.py gives them, as they come to vanilla's and Jugcraft's own
 * biomes by vanilla's tags. Each case sets a biome over the test with /fillbiome and reads the creatures that may spawn
 * there: the biome's natural mob spawns, which Menagerie adds to.
 */
public class LandscapeWildlifeGameTests {
	/** A biome, menagerie species that live there and ones that do not. */
	record Case(String biome, List<String> present, List<String> absent) {}

	static final List<Case> CASES = List.of(
			// Woods: forest creatures, and the landscape collection's own cave creatures beneath; not swamp or savanna ones.
			new Case("jugcraft:landscape_maple_woods", List.of("wildlife_deer", "landscape_gob"), List.of("wildlife_alligator", "wildlife_lion")),
			// A wood with no vanilla creatures of its own: the menagerie's are its first.
			new Case("jugcraft:landscape_sakura_grove", List.of("wildlife_deer"), List.of("wildlife_alligator")),
			new Case("jugcraft:landscape_bayou", List.of("wildlife_alligator"), List.of("wildlife_deer")),
			// A flowered field: open country's and the meadow's.
			new Case("jugcraft:landscape_lavender_field", List.of("wildlife_rat", "wildlife_butterfly"), List.of("wildlife_deer")),
			// Arid land by its ground: painted clay, dry grass, open sand.
			new Case("jugcraft:landscape_white_mesa", List.of("wildlife_vulture"), List.of("wildlife_deer", "wildlife_lion")),
			new Case("jugcraft:landscape_lush_savanna", List.of("wildlife_lion"), List.of("wildlife_vulture", "wildlife_desert_scorpion")),
			new Case("jugcraft:landscape_desert_canyon", List.of("wildlife_desert_scorpion"), List.of("wildlife_lion")),
			new Case("jugcraft:landscape_tropics", List.of("wildlife_tiger"), List.of("wildlife_deer")),
			// The deep ocean is ocean too; nothing of the land or the caves comes there.
			new Case("jugcraft:landscape_deep_warm_ocean", List.of("wildlife_whale", "wildlife_ray"), List.of("wildlife_deer", "landscape_gob")),
			// Snowy land by its cold (below 0.15), besides its kind.
			new Case("jugcraft:landscape_wintry_forest", List.of("wildlife_deer", "wildlife_mammoth"), List.of("wildlife_lion")),
			// A volcano holds only the climate's creatures: the caves'.
			new Case("jugcraft:landscape_volcano", List.of("landscape_gob"), List.of("wildlife_deer", "wildlife_mammoth")),
			// The landscape Nether and End take those dimensions' creatures.
			new Case("jugcraft:landscape_erupting_inferno", List.of("mount_netherbat"), List.of("wildlife_deer", "landscape_gob")),
			new Case("jugcraft:landscape_end_wilds", List.of("beast_endergrade"), List.of("wildlife_deer", "mount_netherbat")),
			// Vanilla's biomes keep theirs.
			new Case("minecraft:forest", List.of("wildlife_deer"), List.of("wildlife_alligator")));

	/** Each case's biome holds its creatures and not the others. */
	@GameTest
	public void landscapeBiomesHoldTheirWildlife(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos spot = helper.absolutePos(new BlockPos(4, 2, 4));
		for (Case test : CASES) {
			fill(helper, test.biome());
			helper.assertTrue(level.getBiome(spot).is(ResourceKey.create(Registries.BIOME, Identifier.parse(test.biome()))),
					"fillbiome did not make the test " + test.biome() + ": " + level.getBiome(spot).getRegisteredName());
			for (String species : test.present()) {
				helper.assertTrue(spawns(level, spot, species), species + " does not live in " + test.biome());
			}
			for (String species : test.absent()) {
				helper.assertFalse(spawns(level, spot, species), species + " lives in " + test.biome());
			}
			// For the record: how many of the creatures that may spawn here are not the menagerie's (the biome's own).
			var settings = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, spot);
			int menagerie = 0, other = 0;
			for (MobCategory category : MobCategory.values()) {
				for (var entry : settings.getMobsToSpawn(category).unwrap()) {
					if (Menagerie.TYPES.containsValue(entry.value().type())) {
						menagerie++;
					} else {
						other++;
					}
				}
			}
			Jugcraft.LOGGER.info("[landscape wildlife] {}: {} menagerie spawn entries, {} others", test.biome(), menagerie, other);
		}
		helper.succeed();
	}

	/** Whether {@code species} is among the creatures that may spawn at {@code pos}, in any category. */
	private static boolean spawns(ServerLevel level, BlockPos pos, String species) {
		EntityType<?> type = Menagerie.TYPES.get(species);
		if (type == null) {
			throw new IllegalArgumentException("No menagerie species " + species);
		}
		var spawns = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, pos);
		for (MobCategory category : MobCategory.values()) {
			if (spawns.getMobsToSpawn(category).unwrap().stream().anyMatch(entry -> entry.value().type() == type)) {
				return true;
			}
		}
		return false;
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
