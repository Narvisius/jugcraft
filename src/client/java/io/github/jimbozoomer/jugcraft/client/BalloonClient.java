package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.BalloonControlPayload;
import io.github.jimbozoomer.jugcraft.agriculture.HotAirBalloon;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Sends a hot-air balloon pilot's keys to the server (fall addition 29): jump fires the burner, back opens the vent.
 * Only when they change, and only while this player pilots a balloon; the server flies it.
 */
public final class BalloonClient {
	private static boolean sentBurner;
	private static boolean sentVent;

	private BalloonClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(BalloonClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		boolean piloting = player != null && player.getVehicle() instanceof HotAirBalloon balloon && balloon.pilot() == player;
		boolean paused = client.isPaused() || client.gui.screen() != null;
		boolean burner = piloting && !paused && client.options.keyJump.isDown();
		boolean vent = piloting && !paused && client.options.keyDown.isDown();
		if ((burner != sentBurner || vent != sentVent) && (piloting || sentBurner || sentVent) && ClientPlayNetworking.canSend(BalloonControlPayload.TYPE)) {
			ClientPlayNetworking.send(new BalloonControlPayload(burner, vent));
			sentBurner = burner;
			sentVent = vent;
		}
	}
}
