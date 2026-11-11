package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.Sight;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block entities drawn off their own chunk section are otherwise drawn every frame wherever you look: client/Sight.java
 * skips ours while out of sight (once they are in range), and notes which ones this frame should measure.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class SightExtractMixin {
	@Shadow
	public abstract <E extends BlockEntity, S extends BlockEntityRenderState> BlockEntityRenderer<E, S> getRenderer(E blockEntity);

	@Inject(method = "tryExtractRenderState", cancellable = true, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;createRenderState()Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"))
	private <E extends BlockEntity, S extends BlockEntityRenderState> void jugcraft$sight(E blockEntity, float partialTick,
			ModelFeatureRenderer.CrumblingOverlay crumbling, boolean global, CallbackInfoReturnable<S> info) {
		if (global && Sight.skip(blockEntity, getRenderer(blockEntity))) {
			info.setReturnValue(null);
		}
	}

	@Inject(method = "tryExtractRenderState", at = @At("RETURN"))
	private <E extends BlockEntity, S extends BlockEntityRenderState> void jugcraft$sightMeasure(E blockEntity, float partialTick,
			ModelFeatureRenderer.CrumblingOverlay crumbling, boolean global, CallbackInfoReturnable<S> info) {
		Sight.extracted(info.getReturnValue());
	}
}
