package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.tv.TvFarUse;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flat screen TVs: a right-click that reaches nothing in the game's reach but meets a TV further on opens its window
 * ({@link TvFarUse}); sneaking with an empty hand, a right-click on a screen puts it full screen. Fabric API has no event for a right-click at nothing with an empty hand (the game sends nothing
 * then): this asks first at the start of the game's right-click. Optional: if the method were renamed the game would
 * still load, the TVs opening only from within reach.
 */
@Mixin(Minecraft.class)
public abstract class TvFarUseMixin {
	@Shadow
	private int rightClickDelay;

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void jugcraft$farTv(CallbackInfo info) {
		if (TvFarUse.tryFullscreen((Minecraft) (Object) this) || TvFarUse.tryOpen((Minecraft) (Object) this)) {
			rightClickDelay = 4;
			info.cancel();
		}
	}
}
