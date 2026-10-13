package io.github.jimbozoomer.jugcraft.concordance.crimson;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * How a piece of living equipment develops (roadmap step 16): through three kinds of deed, each with many subjects:
 * slaying (the kind of hostile creature), enduring (the kind of harm survived while wielding it) and nourishing (Vitae
 * fed to it). A deed pays by how often its subject has already come up in the current day of game time
 * ({@link #DIMINISHING}: 4, 2, 1, 1, then nothing), each kind of deed pays at most {@value #DAILY_CAP} points a day,
 * and only {@value #MAX_SUBJECTS} subjects are remembered a day, so repeating one trivial act (a mob farm, a cactus,
 * feeding it again and again) stops paying at once. Each stage needs several kinds of deed ({@link #stage}), so no
 * single kind of use grows it, and development, once earned, is never lost.
 * <p>
 * Vigor is the separate store the equipment runs on: nourishing fills it, and its powers spend it. Without vigor its
 * stage is still its own; only its powers wait. Immutable: each change returns the growth after it.
 */
public record Growth(Map<String, Integer> points, long window, Map<String, Integer> today, int vigor) {
	public static final long WINDOW_TICKS = 24000L;
	public static final int MAX_SUBJECTS = 32;
	public static final int DAILY_CAP = 24;
	public static final List<Integer> DIMINISHING = List.of(4, 2, 1, 1);
	public static final int MAX_VIGOR = 64;
	public static final int VIGOR_PER_VITAE = 4;
	public static final int MAX_STAGE = 3;
	public static final Growth NEW = new Growth(Map.of(), Long.MIN_VALUE, Map.of(), 0);

	/** The kinds of deed. */
	public enum Deed {
		SLAY("slay"), ENDURE("endure"), NOURISH("nourish");

		public final String id;

		Deed(String id) {
			this.id = id;
		}
	}

	/** Each stage's least total points, and the least points each kind of deed counted for it must have. */
	private static final int[] TOTAL = {0, 20, 60, 150};
	private static final int[] EACH = {0, 5, 10, 30};
	/** How many kinds of deed must reach {@link #EACH} for each stage. */
	private static final int[] KINDS = {0, 2, 3, 3};

	public Growth {
		points = Collections.unmodifiableMap(new TreeMap<>(points));
		today = Collections.unmodifiableMap(new TreeMap<>(today));
		vigor = Math.clamp(vigor, 0, MAX_VIGOR);
	}

	public int points(Deed deed) {
		return points.getOrDefault(deed.id, 0);
	}

	public int total() {
		int sum = 0;
		for (Deed deed : Deed.values()) {
			sum += points(deed);
		}
		return sum;
	}

	/** 0 (dormant) to {@value #MAX_STAGE} (flourishing): the highest stage whose total and spread it has reached. */
	public int stage() {
		int stage = 0;
		for (int next = 1; next <= MAX_STAGE; next++) {
			int kinds = 0;
			for (Deed deed : Deed.values()) {
				if (points(deed) >= EACH[next]) {
					kinds++;
				}
			}
			if (total() < TOTAL[next] || kinds < KINDS[next]) {
				break;
			}
			stage = next;
		}
		return stage;
	}

	/** A deed's result: the growth after it and the points it paid (0 when it paid nothing). */
	public record Result(Growth growth, int gained) {
	}

	/** Points this kind of deed has paid in the day (game-time window) {@code day}. */
	private int paidToday(Deed deed, long day) {
		return day == window ? today.getOrDefault(deed.id + "|*", 0) : 0;
	}

	/** Records one deed with {@code subject} at game time {@code gameTime}. */
	public Result record(Deed deed, String subject, long gameTime) {
		long day = Math.floorDiv(gameTime, WINDOW_TICKS);
		Map<String, Integer> counts = new TreeMap<>(day == window ? today : Map.of());
		String key = deed.id + "|" + subject;
		int seen = counts.getOrDefault(key, 0);
		if (!counts.containsKey(key) && counts.size() >= MAX_SUBJECTS) {
			return new Result(new Growth(points, day, counts, vigor), 0);
		}
		counts.put(key, seen + 1);
		int gain = seen < DIMINISHING.size() ? DIMINISHING.get(seen) : 0;
		gain = Math.max(0, Math.min(gain, DAILY_CAP - paidToday(deed, day)));
		Map<String, Integer> earned = new TreeMap<>(points);
		if (gain > 0) {
			counts.merge(deed.id + "|*", gain, Integer::sum);
			earned.merge(deed.id, gain, Integer::sum);
		}
		return new Result(new Growth(earned, day, counts, vigor), gain);
	}

	/**
	 * Feeds it {@code vitae} Vitae at {@code gameTime}: vigor for each (up to {@value #MAX_VIGOR}), and one nourishing
	 * deed, which pays as any other.
	 */
	public Result nourish(int vitae, long gameTime) {
		Result fed = record(Deed.NOURISH, "vitae", gameTime);
		Growth after = fed.growth();
		return new Result(new Growth(after.points(), after.window(), after.today(), after.vigor() + vitae * VIGOR_PER_VITAE), fed.gained());
	}

	/** How much Vitae it can take now before its vigor is full (none spilt). */
	public int hunger() {
		return (MAX_VIGOR - vigor) / VIGOR_PER_VITAE;
	}

	/** Spends {@code amount} vigor; null when it has too little. */
	public Growth spend(int amount) {
		if (amount > vigor) {
			return null;
		}
		return new Growth(points, window, today, vigor - amount);
	}
}
