package io.github.jimbozoomer.jugcraft.concordance.crimson;

/**
 * Offering exhaustion (roadmap step 16): what giving Vitae leaves behind, apart from the health it took. It rises with
 * each offering, lowers what the next one yields ({@link Offerings#efficiency}) and, at {@value #MAX}, refuses offerings
 * altogether. It falls by one point every {@value #RECOVERY_TICKS} game ticks and by nothing else: no food, potion,
 * spell or regeneration touches it, so healing to full never resets the offering loop. Worked out from the game time
 * when read ({@link #current}), so nothing ticks per player.
 */
public record Exhaustion(int points, long stamp) {
	public static final int MAX = 12;
	public static final int RECOVERY_TICKS = 2400;
	public static final Exhaustion NONE = new Exhaustion(0, 0L);

	public Exhaustion {
		points = Math.clamp(points, 0, MAX);
	}

	/** The points left at game time {@code now}. */
	public int current(long now) {
		if (points == 0) {
			return 0;
		}
		long recovered = Math.max(0L, now - stamp) / RECOVERY_TICKS;
		return (int) Math.max(0L, points - recovered);
	}

	/** Game ticks until it is clear of every point (0 when it already is). */
	public long ticksToClear(long now) {
		int left = current(now);
		if (left == 0) {
			return 0L;
		}
		long progress = Math.max(0L, now - stamp) % RECOVERY_TICKS;
		return (long) left * RECOVERY_TICKS - progress;
	}

	/**
	 * The exhaustion after {@code amount} more points at {@code now} (up to {@link #MAX}), keeping the time already spent
	 * recovering the next point, so offering never makes recovery start over.
	 */
	public Exhaustion add(long now, int amount) {
		int left = current(now);
		long progress = left > 0 ? Math.max(0L, now - stamp) % RECOVERY_TICKS : 0L;
		return new Exhaustion(left + amount, now - progress);
	}
}
