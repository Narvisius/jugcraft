package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.FilmCapture;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The world drawn for the frame (post effects too), before the screen's overlays: the film camera's frame (client/FilmCapture).
 * Not required: if another mod moves the call, the film records nothing (its report says so) but the game plays on. */
@Mixin(GameRenderer.class)
public abstract class FilmCaptureMixin {
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;applyPostEffects()V", shift = At.Shift.AFTER), require = 0)
	private void jugcraft$filmFrame(CallbackInfo info) {
		FilmCapture.frame();
	}
}
