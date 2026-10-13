package io.github.jimbozoomer.jugcraft.concordance.dream;

import org.jspecify.annotations.Nullable;

/**
 * The rules of a dream expedition (roadmap step 22, docs/features/arcane-concordance-hexes.md). A dreamer enters with
 * nothing: everything they carry is held in one escrow on them (the server's {@code DreamExpedition}), with their
 * experience, place and game mode, and given back exactly when the dream ends, however it ends. What a dream brings back
 * is decided here: the dreamglass caught, at most {@value #MAX_CAUGHT}, halved if the dream was broken; so is how many
 * wisps have gathered, when a dream must end, and which experience the dreamer wakes with. Pure.
 */
public final class DreamRules {
	/** A dream lasts at most three minutes. */
	public static final int MAX_TICKS = 3_600;
	/** How far the dream self may stray from the sleeping body. */
	public static final int RADIUS = 24;
	/** The wisps that gather round a new dream. */
	public static final int WISPS = 3;
	/** One more wisp gathers this often while a dream lasts, until {@value #MAX_CAUGHT} have. */
	public static final int WISP_TICKS = 600;
	/** The most dreamglass one dream brings back. */
	public static final int MAX_CAUGHT = 8;
	/** What entering a dream costs, in Focus. */
	public static final int FOCUS = 10;

	/** How a dream ended. */
	public enum End {
		/** The dreamer chose to wake (or the censer was used again). */
		WOKE("woke"),
		/** Its time ran out. */
		TIMEOUT("timeout"),
		/** The dream self strayed too far from the body. */
		STRAYED("strayed"),
		/** The dreamer was hurt. */
		HURT("hurt"),
		/** The dreamer died. */
		DIED("died"),
		/** The dreamer left the server mid-dream. */
		DISCONNECTED("disconnected"),
		/** The dream was found open when the dreamer joined (after a crash). */
		RECOVERED("recovered");

		public final String id;

		End(String id) {
			this.id = id;
		}
	}

	private DreamRules() {
	}

	/**
	 * The dreamglass a dream brings back: what was caught, at most {@link #MAX_CAUGHT}; halved (rounded down) if the
	 * dream was broken by straying or harm; none if the dreamer died; all of it when the server, not the dreamer, ended it.
	 */
	public static int reward(int caught, End end) {
		int kept = Math.clamp(caught, 0, MAX_CAUGHT);
		return switch (end) {
			case WOKE, TIMEOUT, DISCONNECTED, RECOVERED -> kept;
			case STRAYED, HURT -> kept / 2;
			case DIED -> 0;
		};
	}

	/**
	 * Whether a dream must end now, and how: its time over, or the dreamer gone from the body's dimension or more than
	 * {@value #RADIUS} blocks from it; null while it goes on.
	 */
	public static @Nullable End check(long now, long until, boolean sameDimension, double distanceFromBody) {
		if (now >= until) {
			return End.TIMEOUT;
		}
		return !sameDimension || distanceFromBody > RADIUS ? End.STRAYED : null;
	}

	/** How many wisps have gathered {@code elapsed} ticks into a dream: three at once, one more every 30 s, eight at most. */
	public static int gathered(long elapsed) {
		return (int) Math.min(MAX_CAUGHT, WISPS + Math.max(0L, elapsed) / WISP_TICKS);
	}

	/**
	 * Whether the dreamer wakes with the experience they have now rather than what they entered with: only when they have
	 * less (spent in the dream, on an enchantment or a repair, which are real and stay spent). Otherwise they wake with what
	 * they entered with, and whatever they gained in the dream is gone. So a dream never adds experience, and never gives
	 * back what was spent.
	 */
	public static boolean spent(int level, float progress, int enteredLevel, float enteredProgress) {
		return level < enteredLevel || level == enteredLevel && progress < enteredProgress;
	}
}
