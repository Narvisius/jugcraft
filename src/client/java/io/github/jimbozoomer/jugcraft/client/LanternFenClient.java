package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;

/**
 * The Lantern Fen's mist on the client (docs/features/lantern-fen.md): while the player is in the fen, puffs of the Fog
 * Machine's ground fog ({@link JugcraftAgriculture#FOG}) gather low over its open water round them, {@value #TRIES} tried a
 * tick within {@value #RADIUS} blocks, each drifting a little for four to six seconds. Drawn on this client only, from the
 * blocks it already has; nothing here decides anything.
 */
final class LanternFenClient {
	/** Puffs tried each tick, how far round the player (blocks), and how far up and down from their feet to look for water. */
	private static final int TRIES = 3;
	private static final int RADIUS = 24;
	private static final int ABOVE = 2;
	private static final int BELOW = 4;
	private static final double DRIFT = 0.01;

	private LanternFenClient() {
	}

	static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(LanternFenClient::tick);
	}

	private static void tick(Minecraft client) {
		ClientLevel level = client.level;
		LocalPlayer player = client.player;
		if (level == null || player == null || client.isPaused() || !level.dimension().equals(Lair.LANTERN_FEN.dimension)) {
			return;
		}
		RandomSource random = level.getRandom();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < TRIES; i++) {
			int x = player.getBlockX() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
			int z = player.getBlockZ() + random.nextInt(RADIUS * 2 + 1) - RADIUS;
			// The water's top near the player's height: water with no water over it.
			for (int y = player.getBlockY() + ABOVE; y >= player.getBlockY() - BELOW; y--) {
				pos.set(x, y, z);
				if (level.getFluidState(pos).is(FluidTags.WATER)) {
					if (level.getFluidState(pos.above()).isEmpty()) {
						level.addParticle(JugcraftAgriculture.FOG, x + random.nextDouble(), y + 1.05, z + random.nextDouble(),
								(random.nextDouble() - 0.5) * DRIFT, 0.0, (random.nextDouble() - 0.5) * DRIFT);
					}
					break;
				}
			}
		}
	}
}
