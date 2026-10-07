package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.landship.Landship;
import io.github.jimbozoomer.jugcraft.landship.LandshipInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends the Landship driver's keys to the server, which drives it: forward/back, left/right to turn, attack for the
 * cannon and use for the sponson guns. Sent when they change, and every few ticks anyway so the server knows the
 * driver is still there.
 */
public final class LandshipClient {
	private static final int RESEND = 5;
	private static LandshipInputPayload last = new LandshipInputPayload(0, 0, 0, 0);
	private static int sinceSent;

	private LandshipClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(LandshipClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused() || !(player.getVehicle() instanceof Landship landship)
				|| landship.getFirstPassenger() != player) {
			return;
		}
		LandshipInputPayload input = client.gui.screen() != null ? new LandshipInputPayload(0, 0, 0, 0)
				: new LandshipInputPayload(
						(client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0),
						(client.options.keyLeft.isDown() ? 1 : 0) - (client.options.keyRight.isDown() ? 1 : 0),
						client.options.keyAttack.isDown() ? 1 : 0, client.options.keyUse.isDown() ? 1 : 0);
		if (!input.equals(last) || ++sinceSent >= RESEND) {
			ClientPlayNetworking.send(input);
			last = input;
			sinceSent = 0;
		}
	}
}
