package io.github.jimbozoomer.jugcraft.concordance.worker;

/**
 * A familiar's bond with its person (roadmap step 17). It grows by time spent together ({@value #GAIN} a visit near
 * them, at most {@value #DAILY_GAIN} a day of game time, so idling beside a familiar for hours cannot max it in one day)
 * and fades by {@value #NEGLECT} for each day of game time the familiar spends unvisited while its person is online
 * elsewhere. Its strength decides the support it gives: none below {@value #FIRST}, a mending at {@value #FIRST}, and the
 * mending more often at {@value #SECOND}. A familiar bonds to one person; a person keeps one familiar.
 */
public record Bond(int strength, long window, int gainedToday, long lastSupport, long lastSeen) {
	public static final int MAX = 100;
	public static final int GAIN = 1;
	public static final int DAILY_GAIN = 24;
	public static final int NEGLECT = 4;
	public static final int FIRST = 20;
	public static final int SECOND = 60;
	public static final long DAY = 24000L;
	public static final Bond NEW = new Bond(0, Long.MIN_VALUE, 0, Long.MIN_VALUE / 2, 0L);

	public Bond {
		strength = Math.clamp(strength, 0, MAX);
	}

	/** A visit near its person at game time {@code now}: the bond grows, within the day's limit. */
	public Bond together(long now) {
		long day = Math.floorDiv(now, DAY);
		int gained = day == window ? gainedToday : 0;
		int gain = Math.max(0, Math.min(GAIN, DAILY_GAIN - gained));
		return new Bond(strength + gain, day, gained + gain, lastSupport, now);
	}

	/** Its person is online but has not been near it: for each whole day since they last were, it fades. */
	public Bond neglected(long now) {
		long days = Math.max(0L, now - lastSeen) / DAY;
		if (days == 0) {
			return this;
		}
		return new Bond(strength - (int) Math.min(MAX, days * NEGLECT), window, gainedToday, lastSupport, lastSeen + days * DAY);
	}

	/** Support level: 0 none, 1 a mending, 2 a mending twice as often. */
	public int level() {
		return strength >= SECOND ? 2 : strength >= FIRST ? 1 : 0;
	}

	/** Game ticks between two mendings at this level (0 when it gives none). */
	public long cooldown() {
		return switch (level()) {
			case 2 -> 600L;
			case 1 -> 1200L;
			default -> 0L;
		};
	}

	/** Whether it may mend its person at {@code now}. */
	public boolean mayMend(long now) {
		return level() > 0 && now - lastSupport >= cooldown();
	}

	public Bond mended(long now) {
		return new Bond(strength, window, gainedToday, now, lastSeen);
	}
}
