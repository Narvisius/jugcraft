package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.Sight;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Each frame's world extraction starts (client/Sight.java's, client/FrameArrays.java's and client/Retained.java's per-frame bookkeeping). */
@Mixin(LevelExtractor.class)
public abstract class SightFrameMixin {
	@Inject(method = "extract", at = @At("HEAD"))
	private void jugcraft$sightFrame(DeltaTracker delta, Camera camera, float partialTick, CallbackInfo info) {
		Sight.beginFrame();
		io.github.jimbozoomer.jugcraft.client.FrameArrays.beginFrame();
		io.github.jimbozoomer.jugcraft.client.Retained.beginFrame();
	}
}
