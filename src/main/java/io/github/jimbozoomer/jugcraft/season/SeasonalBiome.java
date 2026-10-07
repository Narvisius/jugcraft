package io.github.jimbozoomer.jugcraft.season;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/**
 * A biome's season flags, added to vanilla's Biome by {@code mixin/BiomeSeasonMixin} and set from the biome tags
 * whenever tags load (on the server, and on a client from the server's tags), so hot paths need no tag lookup.
 */
public interface SeasonalBiome {
	boolean jugcraft$hasSeasons();

	boolean jugcraft$hasWinterSnow();

	void jugcraft$setSeasonFlags(boolean seasons, boolean winterSnow);

	static SeasonalBiome of(Biome biome) {
		return (SeasonalBiome) (Object) biome;
	}

	/** Sets every biome's flags from {@code #jugcraft:has_seasons} and {@code #jugcraft:has_winter_snow}. */
	static void updateFlags(RegistryAccess registries) {
		registries.lookupOrThrow(Registries.BIOME).listElements().forEach((Holder.Reference<Biome> biome) ->
				of(biome.value()).jugcraft$setSeasonFlags(biome.is(JugcraftSeasons.HAS_SEASONS), biome.is(JugcraftSeasons.HAS_WINTER_SNOW)));
	}
}
