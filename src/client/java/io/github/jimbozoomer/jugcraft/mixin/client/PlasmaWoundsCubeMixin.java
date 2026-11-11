package io.github.jimbozoomer.jugcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.jimbozoomer.jugcraft.client.PlasmaWoundsClient;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A model box with a plasma wound through it is drawn cut (client/PlasmaWoundsClient.cut) instead of whole. */
@Mixin(ModelPart.Cube.class)
public abstract class PlasmaWoundsCubeMixin {
	@Inject(method = "compile", at = @At("HEAD"), cancellable = true)
	private void jugcraft$plasmaWound(PoseStack.Pose pose, VertexConsumer out, int light, int overlay, int color, CallbackInfo info) {
		if (PlasmaWoundsClient.cut((ModelPart.Cube) (Object) this, pose, out, light, overlay, color)) {
			info.cancel();
		}
	}
}
