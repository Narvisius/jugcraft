package io.github.jimbozoomer.jugcraft.lair.yeti;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Yeti Mitten (docs/features/yeti-king.md): held in the offhand, its wearer never freezes. Powder snow, the Yeti
 * King's Frost Breath and his blizzard leave them warm: whatever frost they gather is gone each tick, so they are never
 * slowed by it nor hurt. Work is bounded: one look at each player's offhand every {@value #CHECK_TICKS} tick.
 */
public final class YetiMitten {
	public static final int CHECK_TICKS = 1;

	private YetiMitten() {
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(YetiMitten::tick);
	}

	/** Whether {@code player} holds a Yeti Mitten in their offhand. */
	public static boolean warm(ServerPlayer player) {
		return player.getOffhandItem().is(JugcraftYeti.YETI_MITTEN);
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % CHECK_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.getTicksFrozen() > 0 && warm(player)) {
				player.setTicksFrozen(0);
			}
		}
	}
}
