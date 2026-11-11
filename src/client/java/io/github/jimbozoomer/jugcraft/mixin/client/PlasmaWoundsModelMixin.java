package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.client.PlasmaWoundsClient;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a model is drawn from its submission (ModelFeatureRenderer): the plasma wounds' holes its render state carries,
 * and the model (which part each of its boxes is in), for ModelPart.Cube (PlasmaWoundsCubeMixin); cleared once it's
 * drawn. Any model drawn for the mob gets them: its own, its clothes and outer layers, its armour.
 */
@Mixin(ModelFeatureRenderer.class)
public abstract class PlasmaWoundsModelMixin {
	@WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
	private void jugcraft$woundsBegin(Model<?> model, Object state, Operation<Void> original) {
		PlasmaWoundsClient.begin(state, model);
		original.call(model, state);
	}

	/** What the model is drawn with (its render type and texture mapping): what plasma cuts off it is drawn the same way (PlasmaWoundsDebris). */
	@Inject(method = "prepareModel", at = @At("HEAD"))
	private void jugcraft$woundsWith(ModelFeatureRenderer.Submit<?> submit, CallbackInfo info) {
		PlasmaWoundsClient.with(submit.renderType(), submit.uvMapping());
	}

	@WrapOperation(method = "prepareModel", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
	private void jugcraft$woundsDraw(Model<?> model, PoseStack pose, VertexConsumer out, int light, int overlay, int color, Operation<Void> original) {
		try {
			original.call(model, pose, out, light, overlay, color);
		} finally {
			PlasmaWoundsClient.end();
		}
	}
}
