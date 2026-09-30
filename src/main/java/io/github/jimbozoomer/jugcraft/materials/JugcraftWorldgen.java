package io.github.jimbozoomer.jugcraft.materials;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Adds data-driven ore features to Overworld biomes. Only newly generated chunks
 * are affected; there is no retrogeneration.
 */
public final class JugcraftWorldgen {
	public static final ResourceKey<PlacedFeature> ORE_TIN =
			ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("ore_tin"));

	private JugcraftWorldgen() {
	}

	public static void register() {
		if (!JugcraftConfig.tinEnabled()) {
			Jugcraft.LOGGER.info("Tin worldgen disabled by config");
			return;
		}
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES, ORE_TIN);
	}
}
