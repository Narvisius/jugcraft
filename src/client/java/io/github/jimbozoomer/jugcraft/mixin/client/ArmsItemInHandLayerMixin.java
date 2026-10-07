package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.client.arms.FlailHeads;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arms motion (batch 43): the held arm turns in the hand with the pose (the wrist: {@link ArmsMotion#wrist}), and a
 * thrusting arm's swing is the pose's rather than vanilla's spear thrust of the item, which would add to it. A flail's
 * chain and ball are drawn where the held item is ({@link FlailHeads#submitThirdPerson}), from the model's root kept as
 * the arm starts ({@link FlailHeads#root}).
 */
@Mixin(ItemInHandLayer.class)
public abstract class ArmsItemInHandLayerMixin {
	private static final String SUBMIT_ARM = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;"
			+ "Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;"
			+ "Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V";

	@Inject(method = SUBMIT_ARM, at = @At("HEAD"))
	private void jugcraft$flailRoot(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
			PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo info) {
		FlailHeads.root(state, poseStack);
	}

	@Inject(method = SUBMIT_ARM, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void jugcraft$turnWrist(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
			PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo info) {
		ArmsMotion.wrist(state, arm, poseStack);
		FlailHeads.submitThirdPerson(state, stack, arm, poseStack, collector, light);
	}

	@WrapOperation(method = SUBMIT_ARM, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/effects/SpearAnimations;thirdPersonAttackItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
	private void jugcraft$thrust(ArmedEntityRenderState state, PoseStack poseStack, Operation<Void> original) {
		if (!ArmsMotion.overridesArms(state)) {
			original.call(state, poseStack);
		}
	}
}
