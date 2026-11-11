package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.PlasmaWoundsClient;
import io.github.jimbozoomer.jugcraft.client.PlasmaWoundsMesh;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Plasma wounds on mobs drawn some other way than from model parts (client/PlasmaWoundsMesh): their frame onto the
 * render state as it's extracted, and their renderer's collector wrapped while it draws them.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class PlasmaWoundsDispatchMixin {
	@Shadow
	public abstract <T extends Entity> EntityRenderer<? super T, ?> getRenderer(T entity);

	@Inject(method = "extractEntity", at = @At("RETURN"))
	private <E extends Entity> void jugcraft$woundsFrame(E entity, float partialTick, CallbackInfoReturnable<EntityRenderState> info) {
		EntityRenderState state = info.getReturnValue();
		if (state != null) {
			PlasmaWoundsMesh.extract(entity, state, partialTick, getRenderer(entity) instanceof LivingEntityRenderer);
		}
	}

	@WrapOperation(method = "submit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
	private void jugcraft$woundsCut(EntityRenderer<?, ?> renderer, EntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera,
			Operation<Void> original) {
		if (PlasmaWoundsClient.vanished(state)) {
			return;                                           // a plasma blow killed it and it has come apart: its pieces lie burning instead
		}
		original.call(renderer, state, pose, PlasmaWoundsMesh.wrap(state, pose, collector), camera);
	}
}
