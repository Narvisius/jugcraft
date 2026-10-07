package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.season.SeasonState;
import io.github.jimbozoomer.jugcraft.season.SeasonalBiome;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Season flags on each biome, and winter snow: while it is snowing (opt-in, see SeasonState), rain in biomes tagged
 * {@code #jugcraft:has_winter_snow} falls as snow, as it does in vanilla's snowy biomes. The biome's temperature is
 * untouched, so world generation, ice and vanilla's own snow are unchanged; Jugcraft's seasonal snow lies and melts
 * by itself (season/SeasonalSnow).
 */
@Mixin(Biome.class)
public abstract class BiomeSeasonMixin implements SeasonalBiome {
	@Unique
	private volatile boolean jugcraft$seasons;
	@Unique
	private volatile boolean jugcraft$winterSnow;

	@Override
	public boolean jugcraft$hasSeasons() {
		return jugcraft$seasons;
	}

	@Override
	public boolean jugcraft$hasWinterSnow() {
		return jugcraft$winterSnow;
	}

	@Override
	public void jugcraft$setSeasonFlags(boolean seasons, boolean winterSnow) {
		jugcraft$seasons = seasons;
		jugcraft$winterSnow = winterSnow;
	}

	@Inject(method = "getPrecipitationAt", at = @At("RETURN"), cancellable = true)
	private void jugcraft$winterSnow(CallbackInfoReturnable<Biome.Precipitation> callback) {
		if (callback.getReturnValue() == Biome.Precipitation.RAIN && jugcraft$winterSnow && SeasonState.snowing()) {
			callback.setReturnValue(Biome.Precipitation.SNOW);
		}
	}
}
