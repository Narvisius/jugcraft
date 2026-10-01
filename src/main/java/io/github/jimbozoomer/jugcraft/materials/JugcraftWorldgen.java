package io.github.jimbozoomer.jugcraft.materials;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Adds the data-driven ore features and surface deposits (data/jugcraft/worldgen) to biomes. Only newly
 * generated chunks are affected; there is no retrogeneration.
 */
public final class JugcraftWorldgen {
	private JugcraftWorldgen() {
	}

	public static void register() {
		Predicate<BiomeSelectionContext> overworld = BiomeSelectors.foundInOverworld();

		// Metal and mineral ores: placed feature name, feature switch.
		String[][] ores = {
				{"tin", "tin"}, {"zinc", "zinc"}, {"lead", "lead"}, {"silver", "silver"},
				{"nickel", "nickel"}, {"tungsten", "tungsten"}, {"uranium", "uranium"}, {"titanium", "titanium"},
				{"salt", "salt"}, {"phosphate", "phosphate"}, {"lepidolite", "lithium"}, {"monazite", "rare_earths"},
		};
		for (String[] ore : ores) {
			add(ore[0], ore[1], overworld);
		}

		// Bauxite forms by tropical weathering; oil sand occurs in dry sandy basins.
		add("bauxite", "aluminum", BiomeSelectors.tag(ConventionalBiomeTags.IS_JUNGLE)
				.or(BiomeSelectors.tag(ConventionalBiomeTags.IS_SAVANNA))
				.or(BiomeSelectors.tag(ConventionalBiomeTags.IS_BADLANDS)));
		add("oil_sand", "crude_oil", BiomeSelectors.tag(ConventionalBiomeTags.IS_DESERT)
				.or(BiomeSelectors.tag(ConventionalBiomeTags.IS_BADLANDS)));

		// Surface resource deposits on stony hills: the windswept hills, stony peaks and stony shores, where bare rock
		// shows (docs/features/resource-deposits.md).
		Predicate<BiomeSelectionContext> stonyHills = BiomeSelectors.tag(ConventionalBiomeTags.IS_WINDSWEPT)
				.or(BiomeSelectors.includeByKey(Biomes.STONY_PEAKS))
				.or(BiomeSelectors.tag(ConventionalBiomeTags.IS_STONY_SHORES));
		// Deposit, then the feature switches it needs (all must be on).
		addDeposit(stonyHills, "coal_deposit", "deposits");
		addDeposit(stonyHills, "iron_deposit", "deposits");
		addDeposit(stonyHills, "copper_deposit", "deposits");
		addDeposit(stonyHills, "tin_deposit", "deposits", "tin");
	}

	private static void addDeposit(Predicate<BiomeSelectionContext> biomes, String deposit, String... features) {
		for (String feature : features) {
			if (!JugcraftConfig.isFeatureEnabled(feature)) {
				Jugcraft.LOGGER.info("{} worldgen disabled by config", deposit);
				return;
			}
		}
		ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id(deposit));
		BiomeModifications.addFeature(biomes, GenerationStep.Decoration.LOCAL_MODIFICATIONS, key);
	}

	private static void add(String name, String feature, Predicate<BiomeSelectionContext> biomes) {
		if (!JugcraftConfig.isFeatureEnabled(feature)) {
			Jugcraft.LOGGER.info("{} worldgen disabled by config", name);
			return;
		}
		ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("ore_" + name));
		BiomeModifications.addFeature(biomes, GenerationStep.Decoration.UNDERGROUND_ORES, key);
	}
}
