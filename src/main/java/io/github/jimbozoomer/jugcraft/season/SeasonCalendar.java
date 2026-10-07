package io.github.jimbozoomer.jugcraft.season;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The server's season, from its own date (never a client's clock). The season is a "season day" from 1 to 365 on the
 * northern calendar: 1 is the first of January, 293 the 20th of October. The southern hemisphere is half a year
 * ahead (its first of January is northern day 183). Day 0 means seasons are off and colours stay vanilla.
 *
 * <p>Events are date windows on the same clock and the same zone: the Harvest Feast (Thanksgiving) and December.
 * They follow the calendar date, not the hemisphere: the Harvest Feast is in November in Sydney too.
 */
public final class SeasonCalendar {
	public static final int DAYS = 365;
	/** Half a year, for the southern hemisphere. */
	public static final int SOUTH_OFFSET = 182;
	/** Winter snow falls and lies from season day 335 (1 December) to day 59 (28 February); it melts after. */
	public static final int SNOW_FROM = 335;
	public static final int SNOW_UNTIL = 59;
	public static final int MAX_SNOW_DEPTH = 8;

	/** How the season is chosen: from the date, fixed to one season (for testing and off-season worlds) or off. */
	public enum Mode {
		AUTO(-1), SPRING(105), SUMMER(196), AUTUMN(293), WINTER(15), OFF(0);

		/** The season day a fixed mode shows (mid-season), or -1 for {@link #AUTO}. */
		public final int day;

		Mode(int day) {
			this.day = day;
		}

		public static Mode parse(String text) {
			return valueOf(text.trim().toUpperCase(Locale.ROOT));
		}
	}

	/** When the Harvest Feast is: the American Thanksgiving, the Canadian one, or not at all. */
	public enum Feast {
		US, CANADA, OFF;

		public static Feast parse(String text) {
			return valueOf(text.trim().toUpperCase(Locale.ROOT));
		}
	}

	/** Events on the season clock. Each is a window of calendar dates (both ends included). */
	public enum Event {
		HARVEST_FEAST("Harvest Feast"), DECEMBER("December");

		public final String display;

		Event(String display) {
			this.display = display;
		}
	}

	/** A window of dates, both ends included; it may run over New Year (12-01..01-06). */
	public record Window(MonthDay start, MonthDay end) {
		public static Window parse(String text) {
			String[] parts = text.trim().split("\\.\\.");
			if (parts.length != 2) {
				throw new IllegalArgumentException(text);
			}
			return new Window(MonthDay.parse("--" + parts[0].trim()), MonthDay.parse("--" + parts[1].trim()));
		}

		public boolean contains(LocalDate date) {
			MonthDay day = MonthDay.from(date);
			return start.isAfter(end) ? !day.isBefore(start) || !day.isAfter(end) : !day.isBefore(start) && !day.isAfter(end);
		}

		@Override
		public String toString() {
			return String.format(Locale.ROOT, "%02d-%02d..%02d-%02d", start.getMonthValue(), start.getDayOfMonth(),
					end.getMonthValue(), end.getDayOfMonth());
		}
	}

	/**
	 * The operator's settings (config/jugcraft.properties, see JugcraftConfig.TEXT_OPTIONS), plus a date fixed by
	 * {@code /jugcraft season date} for previews ({@code null}: today's date). {@code december} is null when off.
	 */
	public record Settings(Mode mode, boolean southern, ZoneId zone, boolean snow, int snowDepth, Feast feast, int feastDays,
			Window december, MonthDay fixedDate) {
		public static final Settings DEFAULT = new Settings(Mode.AUTO, false, ZoneId.of("UTC"), false, 2, Feast.US, 4,
				new Window(MonthDay.of(Month.DECEMBER, 1), MonthDay.of(Month.JANUARY, 6)), null);

		/** The settings in config/jugcraft.properties; an unreadable value keeps its default and logs a warning. */
		public static Settings fromConfig() {
			Mode mode = parse("seasons.mode", DEFAULT.mode, Mode::parse, "auto, spring, summer, autumn, winter or off");
			String hemisphere = JugcraftConfig.textOption("seasons.hemisphere").trim().toLowerCase(Locale.ROOT);
			boolean southern = DEFAULT.southern;
			if (hemisphere.equals("south") || hemisphere.equals("north")) {
				southern = hemisphere.equals("south");
			} else {
				Jugcraft.LOGGER.warn("seasons.hemisphere \"{}\" is not north or south; using north", hemisphere);
			}
			ZoneId zone = parse("seasons.timezone", DEFAULT.zone, text -> ZoneId.of(text.trim()), "a time zone such as UTC or Europe/London");
			boolean snow = parse("seasons.snow", DEFAULT.snow, SeasonCalendar::onOff, "on or off");
			int depth = parse("seasons.snow_depth", DEFAULT.snowDepth, text -> range(text, 1, MAX_SNOW_DEPTH), "a number of layers from 1 to 8");
			Feast feast = parse("harvest_feast", DEFAULT.feast, Feast::parse, "us, canada or off");
			int days = parse("harvest_feast.days", DEFAULT.feastDays, text -> range(text, 1, 7), "a number of days from 1 to 7");
			Window december = parse("december", DEFAULT.december,
					text -> text.trim().equalsIgnoreCase("off") ? null : Window.parse(text), "MM-DD..MM-DD or off");
			return new Settings(mode, southern, zone, snow, depth, feast, days, december, null);
		}

		public Settings withMode(Mode newMode) {
			return new Settings(newMode, southern, zone, snow, snowDepth, feast, feastDays, december, null);
		}

		/**
		 * A fixed preview date (null: back to today's date). While it is set the season follows that date whatever the
		 * mode; ending the preview goes back to the mode it had.
		 */
		public Settings withFixedDate(MonthDay date) {
			return new Settings(mode, southern, zone, snow, snowDepth, feast, feastDays, december, date);
		}

		public Settings withSnow(boolean newSnow) {
			return new Settings(mode, southern, zone, newSnow, snowDepth, feast, feastDays, december, fixedDate);
		}

		/** The date the season and events follow: {@code today}, or the fixed preview date in today's year. */
		public LocalDate effectiveDate(LocalDate today) {
			if (fixedDate == null) {
				return today;
			}
			// 29 February in a year without one falls back to the 28th.
			return fixedDate.isValidYear(today.getYear()) ? fixedDate.atYear(today.getYear()) : LocalDate.of(today.getYear(), 2, 28);
		}

		/** The season day on {@code today} (in this zone): the mode's fixed day, or the date's day in this hemisphere. */
		public int dayOn(LocalDate today) {
			return mode == Mode.AUTO || fixedDate != null ? seasonDay(effectiveDate(today), southern) : mode.day;
		}

		/**
		 * Whether winter snow falls and lies on {@code today}: the option is on and it is winter, by the calendar month
		 * when following the date (December to February in the north, June to August in the south) or the fixed mode.
		 */
		public boolean snowOn(LocalDate today) {
			if (!snow) {
				return false;
			}
			if (mode == Mode.AUTO || fixedDate != null) {
				return isSnowMonth(effectiveDate(today).getMonth(), southern);
			}
			return isSnowSeason(mode.day);
		}

		/** The events running on {@code today}, in {@link Event} order. */
		public List<Event> eventsOn(LocalDate today) {
			LocalDate date = effectiveDate(today);
			List<Event> events = new ArrayList<>();
			if (feast != Feast.OFF && (feastWindowContains(date.getYear(), date) || feastWindowContains(date.getYear() - 1, date))) {
				events.add(Event.HARVEST_FEAST);
			}
			if (december != null && december.contains(date)) {
				events.add(Event.DECEMBER);
			}
			return events;
		}

		/** The first and last day of the Harvest Feast in {@code year}, or null when it is off. */
		public LocalDate[] feastWindow(int year) {
			if (feast == Feast.OFF) {
				return null;
			}
			LocalDate day = harvestFeastDay(feast, year);
			// The American feast starts on Thanksgiving Thursday and runs over the weekend; the Canadian one ends on
			// Thanksgiving Monday, after its weekend.
			return feast == Feast.US ? new LocalDate[] {day, day.plusDays(feastDays - 1)}
					: new LocalDate[] {day.minusDays(feastDays - 1), day};
		}

		private boolean feastWindowContains(int year, LocalDate date) {
			LocalDate[] window = feastWindow(year);
			return window != null && !date.isBefore(window[0]) && !date.isAfter(window[1]);
		}
	}

	private SeasonCalendar() {
	}

	/**
	 * The season day of a date, 1 to 365. The 29th of February shares the 28th's day, so every year has 365; in the
	 * southern hemisphere the day is half a year on.
	 */
	public static int seasonDay(LocalDate date, boolean southern) {
		int day = date.getDayOfYear();
		if (date.isLeapYear() && day > 59) {
			day--;
		}
		return southern ? (day - 1 + SOUTH_OFFSET) % DAYS + 1 : day;
	}

	/** Winter's months: December to February in the north, June to August in the south. */
	public static boolean isSnowMonth(Month month, boolean southern) {
		return southern ? month == Month.JUNE || month == Month.JULY || month == Month.AUGUST
				: month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY;
	}

	/** Whether a season day is in the snow season (1 December to 28 February in the north). */
	public static boolean isSnowSeason(int day) {
		return day >= SNOW_FROM || day >= 1 && day <= SNOW_UNTIL;
	}

	/** Thanksgiving: the fourth Thursday of November (US) or the second Monday of October (Canada). */
	public static LocalDate harvestFeastDay(Feast feast, int year) {
		return switch (feast) {
			case US -> LocalDate.of(year, Month.NOVEMBER, 1).with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY));
			case CANADA -> LocalDate.of(year, Month.OCTOBER, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.MONDAY));
			case OFF -> throw new IllegalArgumentException("The Harvest Feast is off");
		};
	}

	/** The season's name for a season day (0: off). */
	public static String seasonName(int day) {
		if (day <= 0) {
			return "off";
		}
		if (day >= 335 || day < 60) {
			return "winter";
		}
		return day < 152 ? "spring" : day < 244 ? "summer" : "autumn";
	}

	private static boolean onOff(String text) {
		return switch (text.trim().toLowerCase(Locale.ROOT)) {
			case "on", "true" -> true;
			case "off", "false" -> false;
			default -> throw new IllegalArgumentException(text);
		};
	}

	private static int range(String text, int min, int max) {
		int value = Integer.parseInt(text.trim());
		if (value < min || value > max) {
			throw new IllegalArgumentException(text);
		}
		return value;
	}

	private interface Parser<T> {
		T parse(String text);
	}

	private static <T> T parse(String key, T fallback, Parser<T> parser, String expected) {
		String text = JugcraftConfig.textOption(key);
		try {
			return parser.parse(text);
		} catch (IllegalArgumentException | DateTimeException e) {
			Jugcraft.LOGGER.warn("{} \"{}\" is not {}; using the default", key, text, expected);
			return fallback;
		}
	}
}
