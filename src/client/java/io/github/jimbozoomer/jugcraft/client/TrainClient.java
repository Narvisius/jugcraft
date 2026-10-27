package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.train.Locomotive;
import io.github.jimbozoomer.jugcraft.train.TrainInputPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends the locomotive driver's keys to the server (docs/features/trains.md): forwards and back work the throttle,
 * jump brakes. Sent when they change, and every few ticks anyway so the server knows the driver is still there.
 */
public final class TrainClient {
	private static final int RESEND = 5;
	private static TrainInputPayload last = new TrainInputPayload(0, false);
	private static int sinceSent;

	private TrainClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(TrainClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused() || !(player.getVehicle() instanceof Locomotive locomotive)
				|| locomotive.getFirstPassenger() != player) {
			return;
		}
		TrainInputPayload input = client.gui.screen() != null ? new TrainInputPayload(0, false)
				: new TrainInputPayload((client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0),
						client.options.keyJump.isDown());
		if (!input.equals(last) || ++sinceSent >= RESEND) {
			ClientPlayNetworking.send(input);
			last = input;
			sinceSent = 0;
		}
	}
}
