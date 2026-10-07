package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.artillery.ArtilleryInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends a big gun's gunner's keys to the server, which works the gun: forward/back and left/right drive a
 * self-propelled gun, attack fires. Sent when they change, and every few ticks anyway so the server knows the gunner is
 * still there. The gun's aim follows the gunner's own view, which the server already knows.
 */
public final class ArtilleryClient {
	private static final int RESEND = 5;
	private static ArtilleryInputPayload last = new ArtilleryInputPayload(0, 0, 0);
	private static int sinceSent;

	private ArtilleryClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ArtilleryClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused() || player.getVehicle() == null
				|| !ArtilleryRenderers.crewing(player.getVehicle(), player)) {
			return;
		}
		ArtilleryInputPayload input = client.gui.screen() != null ? new ArtilleryInputPayload(0, 0, 0)
				: new ArtilleryInputPayload(
						(client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0),
						(client.options.keyLeft.isDown() ? 1 : 0) - (client.options.keyRight.isDown() ? 1 : 0),
						client.options.keyAttack.isDown() ? 1 : 0);
		if (!input.equals(last) || ++sinceSent >= RESEND) {
			ClientPlayNetworking.send(input);
			last = input;
			sinceSent = 0;
		}
	}
}
