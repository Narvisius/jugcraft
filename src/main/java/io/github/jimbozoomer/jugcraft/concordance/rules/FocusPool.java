package io.github.jimbozoomer.jugcraft.concordance.rules;

import org.jspecify.annotations.Nullable;

/**
 * A player's Focus: the points they hold, and the game time from which the next points are counted. Focus returns one
 * point every {@value #REGEN_TICKS} ticks up to {@value #MAX}, worked out from the game time whenever it is read, so no
 * player ticks to regenerate it and the server and client agree without a packet every tick. The game time only ever
 * moves forward (changing the time of day does not touch it), so turning the clock cannot refill anyone.
 * <p>
 * Immutable; {@link #spend} returns the new pool, or null when there is not enough, so a failed spend changes nothing.
 * Keep the two numbers equal to FOCUS_MAX and FOCUS_REGEN_TICKS in tools/concordance.py.
 */
public record FocusPool(int stored, long stamp) {
	public static final int MAX = 20;
	public static final int REGEN_TICKS = 40;
	/** The save format version. */
	public static final int SCHEMA = 1;
	/** A new practitioner starts full. */
	public static final FocusPool FULL = new FocusPool(MAX, 0L);

	public FocusPool {
		stored = Math.clamp(stored, 0, MAX);
	}

	/** The points available at game time {@code now}. */
	public int current(long now) {
		if (now <= stamp || stored >= MAX) {
			return stored;
		}
		long gained = (now - stamp) / REGEN_TICKS;
		return (int) Math.min(MAX, stored + gained);
	}

	/** Ticks until the next point returns at {@code now}, or 0 when full. */
	public int ticksToNext(long now) {
		if (current(now) >= MAX) {
			return 0;
		}
		long since = Math.max(0L, now - stamp);
		return (int) (REGEN_TICKS - since % REGEN_TICKS);
	}

	/**
	 * The pool after {@code amount} points are given back at {@code now} (up to {@link #MAX}), keeping the progress towards
	 * the next point as {@link #spend} does. Only an explicit source gives Focus this way (roadmap step 16's Crimson Surge).
	 */
	public FocusPool gain(long now, int amount) {
		int available = current(now);
		long newStamp = available >= MAX || now <= stamp ? Math.max(now, stamp) : stamp + (now - stamp) / REGEN_TICKS * REGEN_TICKS;
		return new FocusPool(available + Math.max(0, amount), newStamp);
	}

	/**
	 * The pool after spending {@code cost} at {@code now}, or null if fewer points are available. Partial progress
	 * towards the next point is kept, so spending never delays regeneration.
	 */
	public @Nullable FocusPool spend(long now, int cost) {
		int available = current(now);
		if (cost < 0 || available < cost) {
			return null;
		}
		long newStamp;
		if (available >= MAX || now <= stamp) {
			newStamp = Math.max(now, stamp);
		} else {
			long gained = (now - stamp) / REGEN_TICKS;
			newStamp = stamp + gained * REGEN_TICKS;
		}
		return new FocusPool(available - cost, newStamp);
	}
}
