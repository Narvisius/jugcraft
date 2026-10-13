package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.guns.GunView;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Guns (slice 7, scopes): aiming through a scope narrows the view, so the mouse turns the player as much more slowly
 * ({@link GunView#turnScale}), as vanilla slows it for a spyglass. Fabric API has no event for the mouse's turn: this
 * scales the movement the mouse has gathered just before vanilla turns the player by it (it is cleared after, so nothing
 * carries over). Optional: if the method were renamed the game would still load, at the mouse's own speed.
 */
@Mixin(MouseHandler.class)
public abstract class GunMouseMixin {
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), require = 0)
	private void jugcraft$scopeTurn(double elapsed, CallbackInfo info) {
		double scale = GunView.turnScale();
		accumulatedDX *= scale;
		accumulatedDY *= scale;
	}
}
