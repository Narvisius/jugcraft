package io.github.jimbozoomer.jugcraft.concordance.relic;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A relic's own state (roadmap step 20), saved on its item: its charge, the player it is bound to, and its last pulse. */
public record RelicState(int charge, @Nullable UUID owner, long lastPulse) {
	public static final RelicState FRESH = new RelicState(0, null, Long.MIN_VALUE / 2);

	public RelicState spent(int cost, long now) {
		return new RelicState(Math.max(0, charge - cost), owner, now);
	}

	public RelicState charged(int amount, int capacity) {
		return new RelicState(Math.min(capacity, charge + Math.max(0, amount)), owner, lastPulse);
	}

	public RelicState bound(UUID player) {
		return new RelicState(charge, player, lastPulse);
	}
}
