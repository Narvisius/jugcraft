package io.github.jimbozoomer.jugcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.jimbozoomer.jugcraft.client.PlasmaRifleClient;
import io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The NT-7 in first person (P6): drawn by {@link PlasmaRifleClient#firstPerson} (placed from the rifle's own numbers, the
 * player's arms on it, sliding onto the sight as they aim) in place of vanilla's held item for that hand.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class PlasmaRifleHandsMixin {
	@Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;"
			+ "Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;"
			+ "FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;"
			+ "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V", at = @At("HEAD"), cancellable = true)
	private void jugcraft$plasmaRifle(PlayerRenderState player, FirstPersonHandsAndItemsRenderState hands, float partialTick, float pitch,
			InteractionHand hand, float swingProgress, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector, int light,
			CallbackInfo info) {
		if (io.github.jimbozoomer.jugcraft.client.LaserDesignatorClient.sighted()) {
			info.cancel();                                   // through the LTD-7's sight: the hands are out of view
			return;
		}
		if (stack.getItem() instanceof PlasmaRifleItem && player.avatarRenderState != null
				&& PlasmaRifleClient.firstPerson(player.avatarRenderState, hand == InteractionHand.MAIN_HAND ? hands.mainHandRenderState : hands.offHandRenderState,
						partialTick, hand, equip, poseStack, collector, light)) {
			info.cancel();
		}
	}
}
