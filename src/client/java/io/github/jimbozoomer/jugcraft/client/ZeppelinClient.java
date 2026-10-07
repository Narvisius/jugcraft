package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.airship.Zeppelin;
import io.github.jimbozoomer.jugcraft.airship.ZeppelinInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends the zeppelin's pilot's keys to the server, which flies it: forward/back, left/right, jump to climb and sprint
 * to sink. Sent when they change, and every few ticks anyway so the server knows the pilot is still there.
 */
public final class ZeppelinClient {
	private static final int RESEND = 5;
	private static ZeppelinInputPayload last = new ZeppelinInputPayload(0, 0, 0);
	private static int sinceSent;

	private ZeppelinClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(ZeppelinClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused() || !(player.getVehicle() instanceof Zeppelin zeppelin)
				|| zeppelin.getFirstPassenger() != player) {
			return;
		}
		boolean typing = client.gui.screen() != null;
		int forward = typing ? 0 : (client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0);
		int turn = typing ? 0 : (client.options.keyLeft.isDown() ? 1 : 0) - (client.options.keyRight.isDown() ? 1 : 0);
		int vertical = typing ? 0 : (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keySprint.isDown() ? 1 : 0);
		ZeppelinInputPayload input = new ZeppelinInputPayload(forward, turn, vertical);
		if (!input.equals(last) || ++sinceSent >= RESEND) {
			ClientPlayNetworking.send(input);
			last = input;
			sinceSent = 0;
		}
	}
}
