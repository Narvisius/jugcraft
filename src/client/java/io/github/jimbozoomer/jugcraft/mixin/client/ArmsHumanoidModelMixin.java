package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.client.arms.FlailHeads;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arms motion (batch 43): after vanilla has posed a humanoid model (walk, crouch, its own swing), the arms pose is laid
 * over it ({@link ArmsMotion#apply}). The player model calls this last, and armor models are posed the same way from
 * the same state, so worn armor follows. A state without a pose (anything not a player holding an arm) is left as is.
 * The body as finally posed is what a held flail's ball keeps clear of ({@link FlailHeads#pose}).
 */
@Mixin(HumanoidModel.class)
public abstract class ArmsHumanoidModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
	private void jugcraft$applyArmsPose(HumanoidRenderState state, CallbackInfo info) {
		ArmsMotion.apply((HumanoidModel<?>) (Object) this, state);
		FlailHeads.pose((HumanoidModel<?>) (Object) this, state);
	}
}
