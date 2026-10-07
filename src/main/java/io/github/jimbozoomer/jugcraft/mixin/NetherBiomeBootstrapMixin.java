package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.biome.BiomeBootstrapScope;
import java.util.function.Function;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A vanilla viewer bootstrap cannot resolve data-pack biomes added by Fabric's Nether API. */
@Mixin(targets = "net.fabricmc.fabric.impl.biome.NetherBiomeData", remap = false)
public abstract class NetherBiomeBootstrapMixin {
	@Inject(method = "withModdedBiomeEntries", at = @At("HEAD"), cancellable = true, remap = false)
	private static <T> void jugcraft$vanillaPreview(Climate.ParameterList<T> vanilla,
			Function<ResourceKey<Biome>, T> biomeLookup, CallbackInfoReturnable<Climate.ParameterList<T>> cir) {
		if (BiomeBootstrapScope.isVanillaOnly()) cir.setReturnValue(vanilla);
	}
}
