package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.guns.GunView;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guns (slice 6): the view narrows while the player aims down a gun's sights ({@link GunView#fov}). Fabric API has no
 * field of view event, and vanilla's own narrowing (a drawn bow, a spyglass) lives in this one method, so the gun's zoom
 * multiplies what it returns. It takes no arguments, so a change to the method's parameters cannot break it, and it is
 * optional: if the method were renamed the game would still load, without the zoom (the guns client game test then
 * fails, saying so).
 */
@Mixin(AbstractClientPlayer.class)
public abstract class GunFovMixin {
	@Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true, require = 0)
	private void jugcraft$aimZoom(CallbackInfoReturnable<Float> info) {
		info.setReturnValue(GunView.fov((AbstractClientPlayer) (Object) this, info.getReturnValueF()));
	}
}
