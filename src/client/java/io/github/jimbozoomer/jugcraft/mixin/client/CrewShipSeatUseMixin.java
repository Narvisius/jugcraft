package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.CrewShipSeatUse;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A right-click on a landed drop ship's seat sits in it ({@link CrewShipSeatUse}): the seats are only drawn, so the game
 * itself would send nothing (with an empty hand, nothing at all). Asked first at the start of the game's right-click.
 * Optional: if the method were renamed the game would still load, the seats just not sat in.
 */
@Mixin(Minecraft.class)
public abstract class CrewShipSeatUseMixin {
	@Shadow
	private int rightClickDelay;

	@Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 0)
	private void jugcraft$shipSeat(CallbackInfo info) {
		if (CrewShipSeatUse.tryUse((Minecraft) (Object) this)) {
			rightClickDelay = 4;
			info.cancel();
		}
	}
}
