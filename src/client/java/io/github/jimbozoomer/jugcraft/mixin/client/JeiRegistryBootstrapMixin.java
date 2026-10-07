package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import io.github.jimbozoomer.jugcraft.biome.BiomeBootstrapScope;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * JEI 31.8 rebuilds a vanilla world registry before its separate loot-registry bootstrap.
 * That rebuild cannot resolve data-pack-only biomes. Disable additions to the Overworld and
 * Nether presets throughout that fallback's world, loot and advancement bootstrap. Normal
 * generation stays outside the scope, which restores itself even if a preview bootstrap fails.
 */
@Pseudo
@Mixin(targets = "mezz.jei.common.util.RegistryUtil", remap = false)
public abstract class JeiRegistryBootstrapMixin {
	@WrapMethod(method = "createRegistryProvider")
	private static HolderLookup.Provider jugcraft$vanillaFallback(HolderLookup.Provider registries,
			Operation<HolderLookup.Provider> original) {
		return BiomeBootstrapScope.vanillaOnly(() -> original.call(registries));
	}
}
