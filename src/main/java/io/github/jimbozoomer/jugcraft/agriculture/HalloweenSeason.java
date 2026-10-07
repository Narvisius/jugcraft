package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * When the Halloween event (trick-or-treating) runs. The server operator sets it in
 * config/jugcraft.properties: {@code halloween.start} and {@code halloween.end} as month-day ({@code MM-DD},
 * both days included; a window may run over New Year), {@code halloween.timezone} (an ID such as
 * {@code UTC} or {@code America/New_York}) and {@code halloween.mode}: {@code auto} follows the dates,
 * {@code on} and {@code off} override them (for testing and off-season worlds). Only the server's clock
 * counts; clients are never asked. Ending the event only stops new treats: everything already handed out,
 * worn or built stays.
 */
public final class HalloweenSeason {
	public enum Mode {
		AUTO, ON, OFF
	}

	private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");
	public static final MonthDay DEFAULT_START = MonthDay.of(10, 20);
	public static final MonthDay DEFAULT_END = MonthDay.of(11, 3);
	/** The Harvest Moon's day: Halloween itself. */
	public static final MonthDay DEFAULT_HARVEST_MOON = MonthDay.of(10, 31);

	private static MonthDay start = DEFAULT_START;
	private static MonthDay end = DEFAULT_END;
	private static MonthDay harvestMoon = DEFAULT_HARVEST_MOON;
	private static ZoneId zone = ZoneOffset.UTC;
	private static Mode mode = Mode.AUTO;
	private static Clock clock = Clock.systemUTC();

	private HalloweenSeason() {
	}

	/** Reads the window from the config; a bad value is logged and its default kept. */
	static void load() {
		start = monthDay("halloween.start", DEFAULT_START);
		end = monthDay("halloween.end", DEFAULT_END);
		harvestMoon = monthDay("halloween.harvest_moon", DEFAULT_HARVEST_MOON);
		String zoneId = JugcraftConfig.textOption("halloween.timezone");
		try {
			zone = ZoneId.of(zoneId);
		} catch (DateTimeException e) {
			Jugcraft.LOGGER.warn("halloween.timezone={} is not a time zone; using UTC", zoneId);
			zone = ZoneOffset.UTC;
		}
		String modeName = JugcraftConfig.textOption("halloween.mode");
		try {
			mode = Mode.valueOf(modeName.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			Jugcraft.LOGGER.warn("halloween.mode={} is not auto, on or off; using auto", modeName);
			mode = Mode.AUTO;
		}
	}

	private static MonthDay monthDay(String key, MonthDay fallback) {
		String value = JugcraftConfig.textOption(key);
		try {
			return MonthDay.parse(value, MONTH_DAY);
		} catch (DateTimeParseException e) {
			Jugcraft.LOGGER.warn("{}={} is not a month-day (MM-DD); using {}", key, value, fallback.format(MONTH_DAY));
			return fallback;
		}
	}

	/** Whether trick-or-treating runs now, on the server's clock. */
	public static boolean active() {
		return switch (mode) {
			case ON -> true;
			case OFF -> false;
			case AUTO -> inWindow(MonthDay.now(clock.withZone(zone)), start, end);
		};
	}

	/**
	 * Which Halloween it is (or was last): the year the current window started, on the server's clock. A window
	 * running over New Year belongs to the year it started in; with {@code on} outside the dates, this year.
	 */
	public static int year() {
		LocalDate today = LocalDate.now(clock.withZone(zone));
		boolean wraps = start.isAfter(end);
		MonthDay day = MonthDay.from(today);
		return wraps && !day.isBefore(MonthDay.of(1, 1)) && !day.isAfter(end) ? today.getYear() - 1 : today.getYear();
	}

	/** Whether {@code day} is in the window from {@code from} to {@code to}, both included; it may wrap past New Year. */
	public static boolean inWindow(MonthDay day, MonthDay from, MonthDay to) {
		return from.isAfter(to) ? !day.isBefore(from) || !day.isAfter(to) : !day.isBefore(from) && !day.isAfter(to);
	}

	/** Today's date on the server's clock, in the operator's time zone. */
	public static MonthDay today() {
		return MonthDay.now(clock.withZone(zone));
	}

	/** The Harvest Moon's day ({@code halloween.harvest_moon}, Halloween by default). */
	public static MonthDay harvestMoon() {
		return harvestMoon;
	}

	/** Sets the Harvest Moon's day until the next restart or {@link #reset} (tests). */
	public static void setHarvestMoon(MonthDay day) {
		harvestMoon = day;
	}

	public static Mode mode() {
		return mode;
	}

	/** Overrides the mode until the next restart (tests; the config's {@code halloween.mode} is the operator's switch). */
	public static void setMode(Mode newMode) {
		mode = newMode;
	}

	/** Sets the window and the clock it is read from (tests). */
	public static void setWindow(MonthDay from, MonthDay to, ZoneId timeZone, Clock now) {
		start = from;
		end = to;
		zone = timeZone;
		clock = now;
	}

	/** The configured window back on the system clock (tests). */
	public static void reset() {
		load();
		clock = Clock.systemUTC();
	}
}
