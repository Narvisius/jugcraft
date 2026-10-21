package io.github.jimbozoomer.jugcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.jimbozoomer.jugcraft.biome.JugcraftRegions;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhman;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Jugcraft regions: in a Jugcraft region of an Overworld with the Jugcraft biomes, biome lookups answer from the
 * Jugcraft layout ({@link JugcraftRegions}); everywhere else vanilla answers. In 26.3 a multi-noise source answers
 * through resolvers ({@code createResolver} and {@code createResolverForChunk}) whose lambdas turn a place (quart x,
 * y, z) into a climate and look it up; this wraps that lookup, where the place is known. Every biome lookup of world
 * generation, structures, spawning and /locate goes through one of the two. Round an Adam-ondi-Ahman valley's site
 * the answer is then the valley's ({@link AdamOndiAhman#override}).
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
	@Unique
	private JugcraftRegions.Source jugcraft$regionState = new JugcraftRegions.Source();

	/** Adam-ondi-Ahman judges the land with the climate sampler the game hands over here ({@link AdamOndiAhman#noteSampler}). */
	@Inject(method = "createResolver", at = @At("HEAD"), remap = false)
	private void jugcraft$noteSampler(Climate.Sampler sampler, CallbackInfoReturnable<BiomeResolver> cir) {
		AdamOndiAhman.noteSampler((MultiNoiseBiomeSource) (Object) this, sampler);
	}

	@Inject(method = "createResolverForChunk", at = @At("HEAD"), remap = false)
	private void jugcraft$noteSamplerForChunk(Climate.Sampler sampler, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
			CallbackInfoReturnable<BiomeResolver> cir) {
		AdamOndiAhman.noteSampler((MultiNoiseBiomeSource) (Object) this, sampler);
	}

	@WrapOperation(method = "/^lambda\\$createResolver\\$.*/", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;"),
			remap = false)
	private Holder<Biome> jugcraft$regions(MultiNoiseBiomeSource source, Climate.TargetPoint target, Operation<Holder<Biome>> original,
			@Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 2) int z) {
		Holder<Biome> biome = jugcraft$regionState.biome(source, x, z, target);
		return AdamOndiAhman.override(source, x, z, target, biome != null ? biome : original.call(source, target));
	}

	@WrapOperation(method = "/^lambda\\$createResolverForChunk\\$.*/", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;"),
			remap = false)
	private Holder<Biome> jugcraft$regionsForChunk(MultiNoiseBiomeSource source, Climate.TargetPoint target,
			Operation<Holder<Biome>> original, @Local(argsOnly = true, ordinal = 3) int x, @Local(argsOnly = true, ordinal = 5) int z) {
		Holder<Biome> biome = jugcraft$regionState.biome(source, x, z, target);
		return AdamOndiAhman.override(source, x, z, target, biome != null ? biome : original.call(source, target));
	}
}
