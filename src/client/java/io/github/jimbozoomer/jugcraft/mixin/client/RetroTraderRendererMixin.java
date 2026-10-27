package io.github.jimbozoomer.jugcraft.mixin.client;

import io.github.jimbozoomer.jugcraft.client.RetroTraderClient;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Works out a Retro Trader's face and stomp-clap pose when his render state is extracted. */
@Mixin(VillagerRenderer.class)
public abstract class RetroTraderRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/npc/villager/Villager;Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;F)V",
			at = @At("TAIL"))
	private void jugcraft$extractRetroTrader(Villager villager, VillagerRenderState state, float partialTick, CallbackInfo info) {
		RetroTraderClient.extract(villager, state, partialTick);
	}
}
