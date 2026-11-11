package io.github.jimbozoomer.jugcraft.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * TOWER TV, the channel the Lounge TV shows (Justin's walk through of 24 October 2026, notes 16 and 20; take 1 approved
 * 25 October): the tower's own live news, drawn the way the desks' pages are (flat shapes and the game's font, {@link
 * DeskScreen}) from what the tower's terminal sends the desks ({@link DeskData}). Across the top the channel's name, LIVE,
 * the day and the clock; the weather now over the land and the tower (sun or the moon in its phase and the stars, rain
 * falling, a storm's lightning, the beacon blinking at night); the drones in the air, the three due soonest with what
 * each is doing, its home and when it is due; the tier being built; and the news ticker: the desks' own event log and
 * what the build is short of, running along a character at a time.
 *
 * <p>Laid out as the mockup (tools/mockups: r22b/tv/tv_channel.py) at 256 units across, drawn here at the desks' 246
 * ({@link #U}); {@code height}: how far down the glass reaches in screen units (a 16:9 TV's glass is 139 down).
 */
final class TowerTv {
	private TowerTv() {
	}

	/** The mockup's units in the screen's. */
	static final double U = DeskScreen.W / 256.0;
	static final int NAVY = DeskScreen.rgb(12, 24, 52), BAR = DeskScreen.rgb(6, 12, 30), PANEL = DeskScreen.rgb(20, 38, 78),
			ROW = DeskScreen.rgb(28, 50, 96), WHITE = DeskScreen.rgb(240, 242, 246), GREY = DeskScreen.rgb(150, 164, 190),
			YELLOW = DeskScreen.rgb(255, 206, 64), RED = DeskScreen.rgb(214, 38, 44), GREEN = DeskScreen.rgb(90, 210, 120),
			INK = DeskScreen.rgb(16, 22, 40), TRACK = DeskScreen.rgb(30, 46, 84);
	/** How many ticks each character of the ticker stays before it moves along one. */
	static final int TICKER_TICKS = 4;
	static final String GAP = "   |   ";

	// ------------------------------------------------------------------ the mockup's units

	private static void rect(DeskScreen s, double x, double y, double w, double h, int colour) {
		s.rect(x * U, y * U, w * U, h * U, colour);
	}

	private static void text(DeskScreen s, double x, double y, String t, int colour, double scale, String align) {
		s.text(x * U, y * U, t, colour, scale * U, align);
	}

	private static void text(DeskScreen s, double x, double y, String t, int colour, double scale) {
		text(s, x, y, t, colour, scale, "left");
	}

	private static double width(DeskScreen s, String t, double scale) {
		return s.width(t, scale * U) / U;
	}

	private static void dot(DeskScreen s, double x, double y, double r, int colour) {
		s.dot(x * U, y * U, r * U, colour);
	}

	private static void line(DeskScreen s, double ax, double ay, double bx, double by, int colour, double w) {
		s.line(ax * U, ay * U, bx * U, by * U, colour, w * U);
	}

	// ------------------------------------------------------------------ the page

	/** The channel, or (no tower) the TV's own no-signal card. */
	static void draw(DeskScreen s, @Nullable DeskData d, double now, double height) {
		rect(s, 0, 0, 256, height / U, NAVY);
		if (d == null) {
			noSignal(s, height / U);
			return;
		}
		long ticks = (long) now;
		// the top bar
		rect(s, 0, 0, 256, 15, BAR);
		rect(s, 4, 3, width(s, "TOWER TV", 1) + 6, 9, RED);
		text(s, 7, 4, "TOWER TV", WHITE, 1);
		dot(s, 68, 7, 2, RED);
		text(s, 74, 4, "LIVE", WHITE, 1);
		text(s, 251, 4, "DAY " + d.dayNumber + "   " + d.clock, WHITE, 1, "right");
		weather(s, d, ticks);
		air(s, d);
		build(s, d);
		ticker(s, d, ticks);
	}

	/**
	 * By moon phase (0 full, 4 new): how far the shadow disc stands off the moon's middle, in its radius, leaving three
	 * quarters (1.25) or a quarter (0.40) of the disc lit (the quarter moons, 2 and 6, are shaded by a half disc).
	 */
	static final double[] SHADOW = {0, 1.25, 0, 0.40, 0, 0.40, 0, 1.25};

	/** Night: from the sun's setting to its rising (the moon up, the stars out). */
	static boolean night(long dayTime) {
		return dayTime >= 12542 && dayTime < 23460;
	}

	private static void weather(DeskScreen s, DeskData d, long ticks) {
		rect(s, 4, 18, 122, 87, PANEL);
		text(s, 8, 21, "WEATHER NOW", YELLOW, 1);
		boolean night = night(d.dayTime), storm = d.thundering, rain = d.raining || storm;
		double x0 = 8, y0 = 31, x1 = 121, y1 = 76;
		int sky = night ? DeskScreen.rgb(16, 22, 44) : storm ? DeskScreen.rgb(58, 66, 80) : rain ? DeskScreen.rgb(84, 98, 116)
				: DeskScreen.rgb(92, 150, 214);
		rect(s, x0, y0, x1 - x0, y1 - y0, sky);
		if (night && !rain) {
			// the moon in its phase (DeskData.MOON: 0 full ... 4 new), and the stars
			double mx = x0 + 22, my = y0 + 16, r = 8;
			int phase = Math.floorMod(d.moon, 8);
			if (phase != 4) {
				dot(s, mx, my, r, DeskScreen.rgb(236, 236, 220));
				if (phase == 2 || phase == 6) {
					// a quarter moon: half of it in shadow, the line between straight (waning: lit on the left)
					double a0 = phase == 2 ? -90 : 90;
					s.arc(mx * U, my * U, (r + 0.4) * U, a0, a0 + 180, sky, (r + 0.4) * U);
				} else if (phase != 0) {
					// gibbous or crescent: a disc of sky as far off as leaves the phase's share lit (three quarters or a
					// quarter), from the right while it wanes, from the left while it waxes
					double off = r * SHADOW[phase];
					dot(s, mx + (phase < 4 ? 1 : -1) * off, my, r, sky);
				}
			}
			for (int k = 0; k < 14; k++) {
				double sx = x0 + 6 + (k * 37) % (x1 - x0 - 12), sy = y0 + 4 + (k * 23) % 26;
				if (Math.hypot(sx - mx, sy - my) > r + 2) {
					rect(s, sx, sy, 1, 1, DeskScreen.rgb(220, 220, 240));
				}
			}
		} else if (!rain) {
			dot(s, x0 + 24, y0 + 18, 10, DeskScreen.rgb(255, 214, 80));
		}
		// the land along its foot, the tower on it (its beacon blinking at night)
		int land = night ? DeskScreen.rgb(14, 24, 18) : DeskScreen.rgb(44, 70, 40), tower = night ? DeskScreen.rgb(26, 30, 40) : DeskScreen.rgb(60, 66, 80);
		rect(s, x0, y1 - 10, x1 - x0, 10, land);
		double tx = x0 + (x1 - x0) * 0.68;
		for (double[] wh : new double[][] {{12, 18}, {8, 26}, {4, 31}}) {
			rect(s, tx - wh[0] / 2, y1 - 10 - wh[1], wh[0], wh[1], tower);
		}
		if (night && ticks / 20 % 2 == 0) {
			rect(s, tx - 1, y1 - 43, 2, 2, DeskScreen.rgb(255, 80, 60));
		}
		if (rain) {
			int cloud = storm ? DeskScreen.rgb(120, 126, 140) : DeskScreen.rgb(190, 198, 210);
			double[][] clouds = {{24, 14, 9}, {38, 10, 11}, {54, 15, 8}, {78, 12, 10}, {92, 16, 7}};
			for (double[] c : clouds) {
				dot(s, x0 + c[0] - c[2] * 0.45, y0 + c[1], c[2] * 0.7, cloud);
				dot(s, x0 + c[0] + c[2] * 0.45, y0 + c[1], c[2] * 0.7, cloud);
			}
			// the rain falling
			for (int k = 0; k < 26; k++) {
				double rx = x0 + 8 + (k * 29) % (x1 - x0 - 16), ry = y0 + 24 + Math.floorMod(k * 17 + ticks, 22);
				line(s, rx, ry, rx - 2, ry + 5, DeskScreen.rgb(170, 200, 240), 0.6);
			}
			if (storm && Math.floorMod(ticks, 90) < 4) {
				// a bolt from the clouds now and then
				line(s, x0 + 46, y0 + 20, x0 + 42, y0 + 30, DeskScreen.rgb(255, 250, 200), 1.2);
				line(s, x0 + 42, y0 + 30, x0 + 48, y0 + 30, DeskScreen.rgb(255, 250, 200), 1.2);
				line(s, x0 + 48, y0 + 30, x0 + 43, y0 + 41, DeskScreen.rgb(255, 250, 200), 1.2);
			}
		}
		text(s, 8, 80, storm ? "STORM" : rain ? "RAIN" : "CLEAR", WHITE, 2);
		text(s, 8, 96, night ? "SUNRISE 06:00" : "SUNSET 18:00", GREY, 1);
	}

	/** A flight's home as the column has room for it: its bay ("B3"), or PAD. */
	static String where(String home) {
		return home.startsWith("HANGAR ") ? home.substring(7) : "PAD";
	}

	/** Seconds as the TV shows them: 0:42, 9:59, then minutes (14M). */
	static String eta(int seconds) {
		seconds = Math.max(0, seconds);
		return seconds < 600 ? seconds / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds % 60) : seconds / 60 + "M";
	}

	private static void air(DeskScreen s, DeskData d) {
		rect(s, 130, 18, 122, 87, PANEL);
		text(s, 134, 21, "IN THE AIR", YELLOW, 1);
		List<DeskData.Flight> up = new ArrayList<>();
		for (DeskData.Flight f : d.flights) {
			if (f.airborne()) {
				up.add(f);
			}
		}
		up.sort(Comparator.comparingInt(DeskData.Flight::eta));
		String n = String.valueOf(up.size());
		text(s, 134, 32, n, WHITE, 3);
		double nx = 134 + width(s, n, 3) + 5;
		text(s, nx, 32, up.size() == 1 ? "DRONE" : "DRONES", WHITE, 1);
		text(s, nx, 42, "FLYING", GREY, 1);
		if (up.isEmpty()) {
			text(s, 137, 60, "ALL DRONES HOME", GREEN, 1);
			return;
		}
		for (int k = 0; k < Math.min(3, up.size()); k++) {
			DeskData.Flight f = up.get(k);
			double y = 60 + k * 14;
			rect(s, 134, y - 2, 113, 11, ROW);
			text(s, 137, y, "T" + f.tier(), WHITE, 1);
			text(s, 152, y, f.state(), f.returning() ? GREEN : YELLOW, 1);
			text(s, 203, y, where(f.home()), GREY, 1);
			text(s, 245, y, eta(f.eta()), WHITE, 1, "right");
		}
	}

	private static void build(DeskScreen s, DeskData d) {
		if (d.building > 0) {
			text(s, 4, 110, "TIER " + d.building + " BUILD", WHITE, 1);
			text(s, 80, 110, Math.round(Math.max(0, d.progress) * 100) + "%", YELLOW, 1);
			rect(s, 104, 110, 147, 7, TRACK);
			rect(s, 104, 110, 147 * Math.max(0, Math.min(1, d.progress)), 7, YELLOW);
		} else {
			text(s, 4, 110, d.tier >= 9 ? "THE TOWER IS COMPLETE" : d.tier + " OF 9 TIERS BUILT", WHITE, 1);
			rect(s, 160, 110, 91, 7, TRACK);
			rect(s, 160, 110, 91 * Math.min(1, d.tier / 9.0), 7, GREEN);
		}
	}

	/** The ticker's words: the desks' event log (newest first), then what the build is short of. */
	static String news(DeskData d) {
		StringBuilder b = new StringBuilder();
		int n = 0;
		for (DeskData.Event e : d.rec.events) {
			if (n++ == 6) {
				break;
			}
			b.append(e.time()).append(' ').append(e.text().replaceAll("\\s+", " ")).append(GAP);
		}
		for (int i = 0; i < Math.min(2, d.missing.size()); i++) {
			DeskData.Item m = d.missing.get(i);
			b.append("SHORT: ").append(m.count()).append(' ').append(m.name()).append(GAP);
		}
		if (b.length() == 0) {
			b.append("ALL QUIET AT THE TOWER").append(GAP);
		}
		return b.toString();
	}

	private static void ticker(DeskScreen s, DeskData d, long ticks) {
		rect(s, 0, 124, 256, 20, WHITE);
		rect(s, 0, 124, 37, 20, RED);
		text(s, 18, 131, "NEWS", WHITE, 1, "center");
		String words = news(d);
		int start = (int) Math.floorMod(ticks / TICKER_TICKS, (long) words.length());
		String run = words.substring(start) + words + words;
		// as many characters as fit the band (the game's font: each its own width)
		double room = 256 - 41 - 3;
		int end = 0;
		while (end < run.length() && width(s, run.substring(0, end + 1), 1) <= room) {
			end++;
		}
		text(s, 41, 131, run.substring(0, end), INK, 1);
	}

	private static void noSignal(DeskScreen s, double h) {
		double w = width(s, "TOWER TV", 2) + 12;
		rect(s, 128 - w / 2, h / 2 - 26, w, 20, RED);
		text(s, 128, h / 2 - 22, "TOWER TV", WHITE, 2, "center");
		text(s, 128, h / 2 + 4, "NO SIGNAL", WHITE, 1.4, "center");
		text(s, 128, h / 2 + 20, "THE TV SHOWS ITS TOWER'S CHANNEL", GREY, 1, "center");
	}
}
