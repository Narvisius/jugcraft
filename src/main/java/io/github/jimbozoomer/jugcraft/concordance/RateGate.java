package io.github.jimbozoomer.jugcraft.concordance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player limits on how often a Concordance action can run (writing and reading notes, pouring into a sconce), so
 * holding a key or a modified client sending requests as fast as it can does no more work than a player using it
 * normally. Server thread only; forgotten when the player leaves or the server stops.
 */
public final class RateGate {
	private static final Map<UUID, Map<String, Integer>> LAST = new HashMap<>();

	private RateGate() {
	}

	static void register() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST.remove(handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LAST.clear());
	}

	/** Whether {@code player} may do {@code action} now, at most once per {@code ticks}; records it if so. */
	public static boolean allow(ServerPlayer player, String action, int ticks) {
		int now = player.level().getServer().getTickCount();
		Map<String, Integer> actions = LAST.computeIfAbsent(player.getUUID(), unused -> new HashMap<>());
		Integer last = actions.get(action);
		if (last != null && now >= last && now - last < ticks) {
			return false;
		}
		actions.put(action, now);
		return true;
	}

	/** Forgets a player's limits (tests run several actions in a row). */
	public static void forget(UUID player) {
		LAST.remove(player);
	}
}
