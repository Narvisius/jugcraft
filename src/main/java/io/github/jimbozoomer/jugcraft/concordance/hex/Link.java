package io.github.jimbozoomer.jugcraft.concordance.hex;

import java.util.UUID;

/**
 * A sympathetic link (roadmap step 22), held in a taglock: who made it, to whom, what the target is ({@code player} or
 * {@code creature}), the dimension it was made in, when, and its strength then. Strength fades by one every
 * {@value #DECAY_TICKS} ticks; at none the link has expired. A link is only ever made by touch, so its target was
 * valid then; every use revalidates it ({@link Hexes}).
 */
public record Link(UUID linker, UUID target, String kind, String dimension, long created, int strength) {
	/** A fresh link's strength. */
	public static final int FRESH = 100;
	/** One strength lost every this many ticks: a fresh link lasts twenty minutes. */
	public static final int DECAY_TICKS = 240;

	public int strength(long now) {
		long faded = Math.max(0L, now - created) / DECAY_TICKS;
		return (int) Math.max(0L, strength - faded);
	}

	public boolean expired(long now) {
		return strength(now) <= 0;
	}

	public boolean player() {
		return "player".equals(kind);
	}
}
