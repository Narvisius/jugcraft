package io.github.jimbozoomer.jugcraft.concordance.celestial;

import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * A celestial pattern (roadmap step 15; data: {@code data/<ns>/concordance/pattern/}): an event in the sky that comes
 * round every {@link #period} days of the world's clock, on the days where {@code day mod period == offset}, between
 * {@link #from} and {@link #to} ticks of the day ({@code 0} is dawn; night runs from 13000 to 23000). Some need a clear
 * sky. While it is up, an observatory under the open sky gathers {@link #resonance} Astral Resonance from it once, and a
 * player may attune to it: spending {@link #attuneCost} resonance gives its {@link #effect} while it is up (or, by
 * recalling it with {@link #recallCost} when it is not, for {@link Calendar#RECALL_TICKS} ticks).
 * <p>
 * The world's clock decides everything: never the rendered sky, the moon a shader draws or a client's settings.
 */
public record Pattern(String id, String principle, int period, int offset, int from, int to, boolean clearSky, int resonance,
		String effect, int amplifier, int attuneCost, int recallCost, @Nullable String season) {
	/**
	 * The times of year a pattern may keep to, from the established calendars: Jugcraft's four seasons (the server's date,
	 * {@code season/SeasonCalendar}) and the Harvest Moon (the nights of the Halloween event,
	 * {@code agriculture/HarvestMoon}). When seasons are switched off, a seasonal pattern keeps to none and comes round
	 * all year; the Harvest Moon comes only with its event.
	 */
	public static final Set<String> SEASONS = Set.of("winter", "spring", "summer", "autumn", "harvest_moon");

	public Pattern {
		if (period < 1 || offset < 0 || offset >= period || from < 0 || from >= to || to > Calendar.DAY) {
			throw new IllegalArgumentException("a pattern needs period >= 1, 0 <= offset < period and 0 <= from < to <= 24000");
		}
		if (season != null && !SEASONS.contains(season)) {
			throw new IllegalArgumentException("unknown season " + season + " (one of " + new TreeSet<>(SEASONS) + ")");
		}
	}
}
