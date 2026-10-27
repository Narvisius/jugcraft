package io.github.jimbozoomer.jugcraft.mixin;

import io.github.jimbozoomer.jugcraft.world.RetroTraderJam;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link RetroTraderJam} when a trade completes and when trading stops, so a Retro Trader can break into his
 * stomp-clap after the screen closes. Fabric API has no villager trade event.
 */
@Mixin(AbstractVillager.class)
public abstract class RetroTraderJamMixin {
	@Inject(method = "notifyTrade", at = @At("TAIL"))
	private void jugcraft$traded(MerchantOffer offer, CallbackInfo info) {
		RetroTraderJam.traded((AbstractVillager) (Object) this);
	}

	@Inject(method = "stopTrading", at = @At("TAIL"))
	private void jugcraft$stoppedTrading(CallbackInfo info) {
		RetroTraderJam.stoppedTrading((AbstractVillager) (Object) this);
	}
}
