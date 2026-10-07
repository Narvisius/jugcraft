package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.jimbozoomer.jugcraft.client.SeasonColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Seasonal grass and foliage: each biome sample the level blends into a block's tint goes through
 * {@link SeasonColors#adjust}, so seasonal biomes blend smoothly into their neighbours. The level caches the result.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelSeasonMixin {
	@WrapOperation(method = "calculateBlockTint(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/ColorResolver;)I",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ColorResolver;getColor(Lnet/minecraft/world/level/biome/Biome;DD)I"))
	private int jugcraft$seasonalTint(ColorResolver resolver, Biome biome, double x, double z, Operation<Integer> original) {
		int vanilla = original.call(resolver, biome, x, z);
		return SeasonColors.adjust(resolver, biome, x, z, vanilla);
	}
}
