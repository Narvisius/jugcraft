package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tools.RocketPackItem;
import io.github.jimbozoomer.jugcraft.tools.RocketThrustPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * Fires the rocket pack while jump is held in the air: lifts the player (their movement is the
 * client's), puffs smoke and flame from the nozzles, and tells the server, which pays the JE.
 */
public final class RocketPackClient {
	private RocketPackClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(RocketPackClient::tick);
	}

	private static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.level == null || client.isPaused() || !client.options.keyJump.isDown()
				|| player.onGround() || player.getAbilities().flying || !RocketPackItem.canThrust(player)) {
			return;
		}
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, Math.min(motion.y + RocketPackItem.THRUST, RocketPackItem.MAX_CLIMB), motion.z);
		player.resetFallDistance();
		// Nozzles: on the back, left and right of the spine.
		double yaw = Math.toRadians(player.yBodyRot);
		double backX = Math.sin(yaw) * 0.3;
		double backZ = -Math.cos(yaw) * 0.3;
		for (int side = -1; side <= 1; side += 2) {
			double x = player.getX() + backX + Math.cos(yaw) * 0.18 * side;
			double z = player.getZ() + backZ + Math.sin(yaw) * 0.18 * side;
			double y = player.getY() + 0.7;
			client.level.addParticle(ParticleTypes.FLAME, x, y, z, 0, -0.25, 0);
			client.level.addParticle(ParticleTypes.SMOKE, x, y - 0.2, z, 0, -0.15, 0);
		}
		ClientPlayNetworking.send(new RocketThrustPayload());
	}
}
