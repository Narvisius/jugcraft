package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.LaserDesignatorClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The scroll wheel through the LTD-7's sight (drop pod squads, sheet 5): zoom, or with crouch the squad picked
 * ({@link LaserDesignatorClient#scroll}), not the hotbar. Fabric API has no scroll event. Optional: if the method were
 * renamed the game would still load, the sight stuck at 1x.
 */
@Mixin(MouseHandler.class)
public abstract class DesignatorScrollMixin {
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
	private void jugcraft$designatorScroll(long window, double xOffset, double yOffset, CallbackInfo info) {
		if (LaserDesignatorClient.scroll(yOffset)) {
			info.cancel();
		}
	}
}
