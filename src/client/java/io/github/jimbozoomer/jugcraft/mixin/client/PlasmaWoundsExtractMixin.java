package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.PlasmaWoundsClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Puts a mob's plasma wounds (client/PlasmaWoundsClient) on its render state, for every living mob. */
@Mixin(LivingEntityRenderer.class)
public abstract class PlasmaWoundsExtractMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void jugcraft$plasmaWounds(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo info) {
		PlasmaWoundsClient.extract(entity, state);
	}
}
