package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.client.arms.FlailHeads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Arms motion (batch 43), on screen: while the main hand holding an arm is drawn, the arm takes its guard and its
 * attacks' keyed strokes where vanilla places the hand ({@link ArmsMotion#firstPersonPose}), in place of vanilla's
 * swing for that hand (its whack and its spear thrust). Other items and the off hand are left alone. A flail's chain and
 * ball are drawn on screen too ({@link FlailHeads}): the view's frame is kept as the hand pass begins, the item's once the
 * hand is placed (and again after its swing), and the head is drawn as the pass returns.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ArmsFirstPersonMixin {
	private static final String SUBMIT_ARM = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;"
			+ "Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;"
			+ "FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V";

	@Inject(method = SUBMIT_ARM, at = @At("HEAD"))
	private void jugcraft$beginArm(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partialTick, float pitch,
			InteractionHand hand, float swingProgress, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector,
			int light, CallbackInfo info) {
		ArmsMotion.beginFirstPerson(Minecraft.getInstance().player, stack, hand, swingProgress, partialTick);
		FlailHeads.beginFirstPerson(stack, poseStack, collector, light, partialTick);
	}

	@Inject(method = SUBMIT_ARM, at = @At("RETURN"))
	private void jugcraft$endArm(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partialTick, float pitch,
			InteractionHand hand, float swingProgress, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector,
			int light, CallbackInfo info) {
		ArmsMotion.endFirstPerson();
		FlailHeads.endFirstPerson(poseStack);
	}

	@Inject(method = "applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", at = @At("TAIL"))
	private void jugcraft$armPose(PoseStack poseStack, HumanoidArm arm, float equip, CallbackInfo info) {
		ArmsMotion.firstPersonPose(poseStack, arm, equip);
		FlailHeads.firstPersonFrame(poseStack, arm);
	}

	@WrapOperation(method = SUBMIT_ARM, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void jugcraft$swing(FirstPersonHandsAndItemsRenderer self, float progress, PoseStack poseStack, int side, HumanoidArm arm,
			Operation<Void> original) {
		if (!ArmsMotion.firstPersonActive()) {
			original.call(self, progress, poseStack, side, arm);
		}
		FlailHeads.firstPersonFrame(poseStack, arm);
	}

	@WrapOperation(method = SUBMIT_ARM, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/model/effects/SpearAnimations;firstPersonAttack(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void jugcraft$thrust(float progress, PoseStack poseStack, int side, HumanoidArm arm, Operation<Void> original) {
		if (!ArmsMotion.firstPersonActive()) {
			original.call(progress, poseStack, side, arm);
		}
	}
}
