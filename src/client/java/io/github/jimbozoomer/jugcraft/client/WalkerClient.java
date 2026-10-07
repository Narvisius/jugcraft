package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.walker.DieselWalker;
import io.github.jimbozoomer.jugcraft.walker.WalkerInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends the Diesel Walker pilot's keys to the server, which drives it: forward/back, left/right to turn, jump, use
 * to drill and attack to punch. Sent when they change, and every few ticks anyway so the server knows the pilot is
 * still there.
 */
public final class WalkerClient {
	private static final int RESEND = 5;
	private static WalkerInputPayload last = new WalkerInputPayload(0, 0, 0, 0, 0);
	private static int sinceSent;

	private WalkerClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(WalkerClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused() || !(player.getVehicle() instanceof DieselWalker walker)
				|| walker.getFirstPassenger() != player) {
			return;
		}
		WalkerInputPayload input = client.gui.screen() != null ? new WalkerInputPayload(0, 0, 0, 0, 0)
				: new WalkerInputPayload(
						(client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0),
						(client.options.keyLeft.isDown() ? 1 : 0) - (client.options.keyRight.isDown() ? 1 : 0),
						client.options.keyJump.isDown() ? 1 : 0, client.options.keyUse.isDown() ? 1 : 0,
						client.options.keyAttack.isDown() ? 1 : 0);
		if (!input.equals(last) || ++sinceSent >= RESEND) {
			ClientPlayNetworking.send(input);
			last = input;
			sinceSent = 0;
		}
	}
}
