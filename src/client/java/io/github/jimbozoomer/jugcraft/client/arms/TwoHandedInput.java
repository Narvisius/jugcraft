package io.github.jimbozoomer.jugcraft.client.arms;

import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
import io.github.jimbozoomer.jugcraft.weapons.TwoHandedSwingPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The client's half of two-handed swings (Arms III, batch 46; the server's is weapons/TwoHanded). A click with a
 * two-handed arm, at a foe or at the air, does not hit at once: it asks the server for a swing and swings the arm here
 * at once, so the motion starts without waiting for the server; the server lands the blow. A click at a block still
 * mines it, and one at something that is not alive (a boat, an item frame) still hits it, as vanilla's. A click within
 * QUEUE_TICKS of the end of a swing waits for it; earlier clicks are let go. With something that blocks in the off
 * hand the server refuses and says why.
 */
public final class TwoHandedInput {
	private static boolean queued;

	private TwoHandedInput() {
	}

	public static void register() {
		ClientPreAttackCallback.EVENT.register(TwoHandedInput::attack);
		ClientTickEvents.END_CLIENT_TICK.register(TwoHandedInput::tick);
	}

	/** Whether this attack-key press is a two-handed swing's (and so taken from vanilla). */
	private static boolean attack(Minecraft client, LocalPlayer player, int clicks) {
		ItemStack stack = player.getMainHandItem();
		if (TwoHanded.heavy(stack) == null || player.isSpectator()) {
			return false;
		}
		HitResult hit = client.hitResult;
		if (hit != null && hit.getType() == HitResult.Type.BLOCK
				|| hit instanceof EntityHitResult entity && !(entity.getEntity() instanceof LivingEntity)) {
			return false;
		}
		if (clicks == 0) {
			return false;
		}
		if (player.isUsingItem()) {
			return true;
		}
		if (TwoHanded.offHandBusy(player)) {
			// The server refuses it and says why on the action bar.
			ClientPlayNetworking.send(TwoHandedSwingPayload.INSTANCE);
			return true;
		}
		if (player.isSwinging()) {
			float left = (1.0F - player.getSwingAnimation(0.0F)) * animation(stack).duration();
			if (left <= JugcraftArms.QUEUE_TICKS) {
				queued = true;
			}
			return true;
		}
		swing(player, stack);
		return true;
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (!queued || player == null || player.isSwinging()) {
			return;
		}
		queued = false;
		ItemStack stack = player.getMainHandItem();
		if (TwoHanded.heavy(stack) != null && !player.isUsingItem() && !TwoHanded.offHandBusy(player)) {
			swing(player, stack);
		}
	}

	/** Asks the server for the swing first (it reads the attack charge as it was), then swings here. */
	private static void swing(LocalPlayer player, ItemStack stack) {
		ClientPlayNetworking.send(TwoHandedSwingPayload.INSTANCE);
		player.swing(InteractionHand.MAIN_HAND, animation(stack), false);
		player.resetAttackStrengthTicker();
	}

	private static SwingAnimation animation(ItemStack stack) {
		return stack.getOrDefault(DataComponents.ATTACK_ANIMATION, SwingAnimation.DEFAULT);
	}
}
