package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.Broomstick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Hands the pilot's keys to the flying broomstick they ride (fall addition 22): forward and back, left and right, and
 * jump. The broom flies on this client, as a boat does, and the server checks where it goes.
 */
public final class BroomstickClient {
	private BroomstickClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(BroomstickClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || !(player.getVehicle() instanceof Broomstick broom)) {
			return;
		}
		boolean paused = client.isPaused() || client.gui.screen() != null;
		float forward = paused ? 0.0F : (client.options.keyUp.isDown() ? 1.0F : 0.0F) - (client.options.keyDown.isDown() ? 1.0F : 0.0F);
		float strafe = paused ? 0.0F : (client.options.keyRight.isDown() ? 1.0F : 0.0F) - (client.options.keyLeft.isDown() ? 1.0F : 0.0F);
		broom.steer(forward, strafe, !paused && client.options.keyJump.isDown());
	}
}
