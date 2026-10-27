package io.github.jimbozoomer.jugcraft.concordance.worker;

/**
 * A familiar's bond with its person (roadmap step 17). It grows by time spent together ({@value #GAIN} a visit near
 * them, at most {@value #DAILY_GAIN} a day of game time, so idling beside a familiar for hours cannot max it in one day)
 * and fades by {@value #NEGLECT} for each whole day of its person's own play time (the game's play-time statistic, which
 * grows only while they are in the game) spent away from it, online elsewhere in its level or another (Concordance Chunk
 * D, D-S17): days they are logged out never count, whether or not the familiar was loaded meanwhile, and neither does
 * play while the Concordance is switched off and the familiar is loaded ({@link #restart}; one not loaded meanwhile counts
 * that play at its first look once the switch is on, D-P13). Its strength decides the support it gives: none below
 * {@value #FIRST}, a mending at {@value #FIRST}, and the mending more often at {@value #SECOND}. A familiar bonds to one
 * person; a person keeps one familiar.
 *
 * @param lastSeen   the game time of the last visit, which paces visits
 * @param seenPlayed its person's play time (ticks) up to which their absence has been counted, or {@link #UNSEEN}
 */
public record Bond(int strength, long window, int gainedToday, long lastSupport, long lastSeen, long seenPlayed) {
	public static final int MAX = 100;
	public static final int GAIN = 1;
	public static final int DAILY_GAIN = 24;
	public static final int NEGLECT = 4;
	public static final int FIRST = 20;
	public static final int SECOND = 60;
	public static final long DAY = 24000L;
	/** Its person's play time not yet counted (a new bond, or one saved before Chunk D): the next look only starts the count. */
	public static final long UNSEEN = -1L;
	public static final Bond NEW = new Bond(0, Long.MIN_VALUE, 0, Long.MIN_VALUE / 2, 0L, UNSEEN);

	public Bond {
		strength = Math.clamp(strength, 0, MAX);
		seenPlayed = Math.max(UNSEEN, seenPlayed);
	}

	/** A bond whose person's play time is not yet counted, as one saved before Chunk D reads. */
	public Bond(int strength, long window, int gainedToday, long lastSupport, long lastSeen) {
		this(strength, window, gainedToday, lastSupport, lastSeen, UNSEEN);
	}

	/** A visit near its person at game time {@code now}, who has played {@code played} ticks: it grows, within the day's limit. */
	public Bond together(long now, long played) {
		long day = Math.floorDiv(now, DAY);
		int gained = day == window ? gainedToday : 0;
		int gain = Math.max(0, Math.min(GAIN, DAILY_GAIN - gained));
		return new Bond(strength + gain, day, gained + gain, lastSupport, now, played);
	}

	/**
	 * Its person, here with {@code played} ticks of play time in all: for each whole day of that play since their absence was
	 * last counted, it fades. A first count, and a play time that went back (statistics reset), only start the count.
	 */
	public Bond neglected(long played) {
		if (seenPlayed == UNSEEN || played < seenPlayed) {
			return new Bond(strength, window, gainedToday, lastSupport, lastSeen, played);
		}
		long days = (played - seenPlayed) / DAY;
		if (days == 0) {
			return this;
		}
		return new Bond(strength - (int) Math.min(MAX, days * NEGLECT), window, gainedToday, lastSupport, lastSeen, seenPlayed + days * DAY);
	}

	/**
	 * Its person, online with {@code played} ticks of play time in all, while the Concordance is switched off: nothing fades,
	 * and their absence is counted from here, so play while it is off is never owed (D-S16, D-S17).
	 */
	public Bond restart(long played) {
		return new Bond(strength, window, gainedToday, lastSupport, lastSeen, played);
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
		return new Bond(strength, window, gainedToday, now, lastSeen, seenPlayed);
	}
}
