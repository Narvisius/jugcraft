package io.github.jimbozoomer.jugcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.walker.HowitzerWalker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The pilot's hands and held items stay inside the enclosed cockpit, too. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class HowitzerPilotHandsMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void jugcraft$hideCockpitHands(float partialTick, PoseStack pose, SubmitNodeCollector collector,
			PlayerRenderState state, FirstPersonHandsAndItemsRenderState hands, CallbackInfo info) {
		var player = Minecraft.getInstance().player;
		if (player != null && (player.getVehicle() instanceof HowitzerWalker || player.getVehicle() instanceof io.github.jimbozoomer.jugcraft.menagerie.CavernSubmarine submarine && submarine.hidesPilot()) && player.getVehicle().getFirstPassenger() == player) {
			info.cancel();
		}
	}
}
