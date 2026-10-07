package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jimbozoomer.jugcraft.biome.JugcraftRegions;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Jugcraft regions: in a Jugcraft region of an Overworld with the Jugcraft biomes, biome lookups answer from the
 * Jugcraft layout ({@link JugcraftRegions}); everywhere else vanilla answers. In 26.3 a multi-noise source answers
 * through resolvers ({@code createResolver} and {@code createResolverForChunk}) whose lambdas turn a place (quart x,
 * y, z) into a climate and look it up; this wraps that lookup, where the place is known. Every biome lookup of world
 * generation, structures, spawning and /locate goes through one of the two.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
	@Unique
	private JugcraftRegions.Source jugcraft$regionState = new JugcraftRegions.Source();

	@WrapOperation(method = "/^lambda\\$createResolver\\$.*/", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;"),
			remap = false)
	private Holder<Biome> jugcraft$regions(MultiNoiseBiomeSource source, Climate.TargetPoint target, Operation<Holder<Biome>> original,
			@Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 2) int z) {
		Holder<Biome> biome = jugcraft$regionState.biome(source, x, z, target);
		return biome != null ? biome : original.call(source, target);
	}

	@WrapOperation(method = "/^lambda\\$createResolverForChunk\\$.*/", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;"),
			remap = false)
	private Holder<Biome> jugcraft$regionsForChunk(MultiNoiseBiomeSource source, Climate.TargetPoint target,
			Operation<Holder<Biome>> original, @Local(argsOnly = true, ordinal = 3) int x, @Local(argsOnly = true, ordinal = 5) int z) {
		Holder<Biome> biome = jugcraft$regionState.biome(source, x, z, target);
		return biome != null ? biome : original.call(source, target);
	}
}
