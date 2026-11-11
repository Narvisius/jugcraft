package io.github.jimbozoomer.jugcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.Sight;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** On the frames client/Sight.java measures a block entity, it is drawn through a collector that notes where its parts land. */
@Mixin(LevelRenderer.class)
public abstract class SightSubmitMixin {
	@WrapOperation(method = "submitBlockEntities", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
	private void jugcraft$sightMeasure(BlockEntityRenderDispatcher dispatcher, BlockEntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
		original.call(dispatcher, state, pose, Sight.wrap(state, pose, collector), camera);
	}
}
