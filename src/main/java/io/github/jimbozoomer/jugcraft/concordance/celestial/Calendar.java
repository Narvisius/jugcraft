package io.github.jimbozoomer.jugcraft.concordance.celestial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * The celestial calendar (roadmap step 15): pure arithmetic on the world's clock (the overworld's day time, the clock
 * the sun and moon follow), so every client and every save agrees on what is in the sky. Day {@code d} is
 * {@code floor(time / 24000)}; vanilla's moon is full on days {@code d mod 8 == 0}. A pattern's {@code k}-th occurrence
 * is the window on day {@code offset + k * period}; {@link #occurrence} numbers them, and that number is the event's
 * identity for {@link io.github.jimbozoomer.jugcraft.concordance.resource.AstralLedger}.
 */
public final class Calendar {
	public static final int DAY = 24000;
	/** How long a recalled attunement lasts (a stored alternative to waiting: shorter than the real thing). */
	public static final int RECALL_TICKS = 2400;
	/** The most days a forecast looks ahead. */
	public static final int MAX_FORECAST_DAYS = 32;
	/** The longest one attunement lasts, however long its pattern is up: eight minutes, vanilla's longest potion. */
	public static final int MAX_ATTUNEMENT_TICKS = 9600;

	private Calendar() {
	}

	public static long day(long time) {
		return Math.floorDiv(time, DAY);
	}

	public static int tickOfDay(long time) {
		return (int) Math.floorMod(time, DAY);
	}

	/** Vanilla's moon phase, 0 (full) to 7. */
	public static int moonPhase(long time) {
		return (int) Math.floorMod(day(time), 8L);
	}

	/** Whether the pattern's day is today (whatever the hour). */
	public static boolean today(Pattern pattern, long time) {
		return Math.floorMod(day(time) - pattern.offset(), (long) pattern.period()) == 0;
	}

	/** Whether the pattern is up now (its day and its hours). Weather is the caller's: see {@link Pattern#clearSky}. */
	public static boolean active(Pattern pattern, long time) {
		int tick = tickOfDay(time);
		return today(pattern, time) && tick >= pattern.from() && tick < pattern.to();
	}

	/** Which occurrence of the pattern the day of {@code time} belongs to (the latest on or before it). */
	public static long occurrence(Pattern pattern, long time) {
		return Math.floorDiv(day(time) - pattern.offset(), (long) pattern.period());
	}

	/** One window of a pattern: its occurrence number and the world times it opens and closes. */
	public record Window(Pattern pattern, long occurrence, long start, long end) {
		public boolean open(long time) {
			return time >= start && time < end;
		}
	}

	public static Window window(Pattern pattern, long occurrence) {
		long day = pattern.offset() + occurrence * pattern.period();
		return new Window(pattern, occurrence, day * DAY + pattern.from(), day * DAY + pattern.to());
	}

	/** The pattern's window that is open at {@code time}, or the next to open after it. */
	public static Window next(Pattern pattern, long time) {
		Window window = window(pattern, occurrence(pattern, time));
		return window.end() > time ? window : window(pattern, window.occurrence() + 1);
	}

	/**
	 * Every window that is open at {@code time} or opens within {@code days} days of it (at most
	 * {@value #MAX_FORECAST_DAYS}), by when it opens and then by pattern: a forecast. Bounded by the patterns' periods.
	 */
	public static List<Window> forecast(Collection<Pattern> patterns, long time, int days) {
		long horizon = time + (long) Math.clamp(days, 0, MAX_FORECAST_DAYS) * DAY;
		List<Window> out = new ArrayList<>();
		for (Pattern pattern : patterns) {
			for (Window window = next(pattern, time); window.start() < horizon; window = window(pattern, window.occurrence() + 1)) {
				out.add(window);
			}
		}
		out.sort(Comparator.comparingLong(Window::start).thenComparing(window -> window.pattern().id()));
		return out;
	}

	/**
	 * The fewest game ticks between two claims of the same pattern: half its period, so a player who sleeps through
	 * nights still meets every occurrence, while moving the clock forward cannot make one pay sooner than the world runs.
	 */
	public static long minGap(Pattern pattern) {
		return (long) pattern.period() * DAY / 2;
	}
}
