package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.client.arms.FlailHeads;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arms motion (batch 43): once vanilla has read an armed entity's hands and swing into its render state, the player's
 * arms pose for this frame is worked out and kept on the state ({@link ArmsMotion#extract}). This is the one place a
 * renderer gives the entity, the state and the partial tick together; Fabric's render state data carries the pose to
 * the model and the item layer. A flail's swinging head is resolved here too ({@link FlailHeads#extract}).
 */
@Mixin(ArmedEntityRenderState.class)
public abstract class ArmsRenderStateMixin {
	@Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
	private static void jugcraft$extractArmsPose(LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver resolver,
			float partialTick, CallbackInfo info) {
		ArmsMotion.extract(entity, state, partialTick);
		FlailHeads.extract(entity, state, resolver, partialTick);
	}
}
