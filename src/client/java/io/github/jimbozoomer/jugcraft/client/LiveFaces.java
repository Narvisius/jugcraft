package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.tower.RoomKit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;

/**
 * The room kits' live faces other than pages (Tri-Pillar work list item 22 B2; client/RoomKitDisplayRenderer draws
 * them): each kind's picture worked out from the time and the piece, in blocks across and down the face, as the baked
 * picture under it has it (tools/training_kit.py seven_seg and live_clock, tools/wash_kit.py filler_screen and
 * scrub_panel). Nothing here touches the game but the font's widths, so the harness draws them off the game too.
 */
final class LiveFaces {
	private LiveFaces() {
	}

	/**
	 * A live face's picture as last drawn: a page (its DeskScreen, in screen units), or the other kinds' shapes and text
	 * in blocks across and down the face (DeskScreen's quads: four corners, colour, layer). {@code key} is what it was
	 * drawn for (the minute and the colon, the count...): it is drawn again only when that changes.
	 */
	static final class Picture {
		DeskScreen screen;
		float[] quads = new float[DeskScreen.STRIDE * 64];
		int size;
		final List<DeskScreen.Text> texts = new ArrayList<>();
		long key = Long.MIN_VALUE;
		int detail = -1;

		void clear() {
			size = 0;
			texts.clear();
		}

		int count() {
			return size / DeskScreen.STRIDE;
		}

		/** A quad facing the viewer (y down: round anticlockwise as they see it), on {@code layer}. */
		void put(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int colour, int layer) {
			if (size + DeskScreen.STRIDE > quads.length) {
				quads = Arrays.copyOf(quads, quads.length * 2);
			}
			float area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
			float[] q = area <= 0 ? new float[] {ax, ay, bx, by, cx, cy, dx, dy} : new float[] {ax, ay, dx, dy, cx, cy, bx, by};
			System.arraycopy(q, 0, quads, size, 8);
			quads[size + 8] = Float.intBitsToFloat(colour);
			quads[size + 9] = layer;
			size += DeskScreen.STRIDE;
		}

		void rect(float x0, float y0, float x1, float y1, int colour, int layer) {
			put(x0, y0, x0, y1, x1, y1, x1, y0, colour, layer);
		}

		/** A dot (an octagon) of radius r round (x, y). */
		void dot(float x, float y, float r, int colour, int layer) {
			float a = r * 0.414F;
			put(x - r, y - a, x - r, y + a, x + r, y + a, x + r, y - a, colour, layer);
			put(x - a, y - r, x - r, y - a, x + r, y - a, x + a, y - r, colour, layer);
			put(x - r, y + a, x - a, y + r, x + a, y + r, x + r, y + a, colour, layer);
		}

		/** A line of text centred on (x, y): its capitals {@code cap} high, or narrower to fit {@code maxWidth}. */
		void text(Font font, float x, float y, String s, int colour, float cap, float maxWidth) {
			float scale = Math.min(cap / 7, maxWidth / Math.max(1, font.width(s) - 1));
			texts.add(new DeskScreen.Text(x, y - 3.5F * scale, s, colour, scale, 1));
		}
	}

	/** The hour and minute of the tower's day ({@code dayTime} the overworld's clock, 0 at 06:00), plus {@code hours}. */
	static int[] time(long dayTime, int hours) {
		long t = Math.floorMod(dayTime + 6000 + 1000L * hours, 24000L);
		return new int[] {(int) (t / 1000), (int) (t % 1000 * 60 / 1000)};
	}

	/** Which segments each figure lights (a to g), and where each segment runs (x0, half-heights y0, x1, y1). */
	private static final String[] LIT = {"abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgedc", "abc", "abcdefg", "abcdfg"};
	private static final int[][] SEGMENTS = {{0, 0, 1, 0}, {1, 0, 1, 1}, {1, 1, 1, 2}, {0, 2, 1, 2}, {0, 1, 0, 2}, {0, 0, 0, 1}, {0, 1, 1, 1}};

	/**
	 * A seven-segment clock (tools/training_kit.py seven_seg): its four figures the tower's time of day, plus the zone's
	 * hours, the unlit segments dim as on a real LED clock; its colon blinks each second ({@code gameTime} in ticks).
	 */
	static void clock(RoomKit.Live f, long dayTime, long gameTime, Picture p, float w, float h) {
		int[] hm = time(dayTime, f.integer("hours", 0));
		boolean colon = Math.floorMod(gameTime, 20) < 10;
		long key = (hm[0] * 100L + hm[1]) * 2 + (colon ? 1 : 0);
		if (p.key == key && p.size > 0) {
			return;
		}
		p.key = key;
		p.clear();
		int on = f.colour("on", 0xFFFF3A24), off = f.colour("off", 0xFF2C0E0C);
		p.rect(0, 0, w, h, f.colour("bg", 0xFF0A0606), 0);
		float[] digits = f.floats("digits");
		if (digits.length < 16) {
			return;
		}
		// the segments' thickness (given as a fraction of the face's width)
		float th = f.params().has("thick") ? f.params().get("thick").getAsFloat() * w : 0.18F * digits[2] * w;
		int[] figures = {hm[0] / 10, hm[0] % 10, hm[1] / 10, hm[1] % 10};
		for (int n = 0; n < 4; n++) {
			float x = digits[n * 4] * w, y = digits[n * 4 + 1] * h, dw = digits[n * 4 + 2] * w, dh = digits[n * 4 + 3] * h;
			String lit = LIT[figures[n]];
			for (int s = 0; s < 7; s++) {
				int[] seg = SEGMENTS[s];
				float x0 = x + seg[0] * dw, y0 = y + seg[1] * dh / 2, x1 = x + seg[2] * dw, y1 = y + seg[3] * dh / 2;
				int colour = lit.indexOf((char) ('a' + s)) >= 0 ? on : off;
				if (seg[0] == seg[2]) {
					// a vertical segment: a long hexagon, pointed at both ends (two quads)
					p.put(x0, y0 + th * 0.6F, x0 + th / 2, y0 + th * 1.1F, x0 + th / 2, y1 - th * 1.1F, x0, y1 - th * 0.6F, colour, 1);
					p.put(x0, y0 + th * 0.6F, x0, y1 - th * 0.6F, x0 - th / 2, y1 - th * 1.1F, x0 - th / 2, y0 + th * 1.1F, colour, 1);
				} else {
					p.put(x0 + th * 0.6F, y0, x0 + th * 1.1F, y0 - th / 2, x1 - th * 1.1F, y0 - th / 2, x1 - th * 0.6F, y0, colour, 1);
					p.put(x0 + th * 0.6F, y0, x1 - th * 0.6F, y0, x1 - th * 1.1F, y0 + th / 2, x0 + th * 1.1F, y0 + th / 2, colour, 1);
				}
			}
		}
		float[] c = f.floats("colon");
		if (c.length >= 4) {
			float cx = (c[0] + c[1]) / 2 * w, r = (c[1] - c[0]) / 2 * w;
			p.dot(cx, c[2] * h, r, colon ? on : off, 1);
			p.dot(cx, c[3] * h, r, colon ? on : off, 1);
		}
	}

	/** The bottle filler's counter: its baked count plus the bottles it has filled. */
	static void counter(Font font, RoomKit.Live f, int filled, Picture p, float w, float h) {
		int digits = Math.max(1, Math.min(9, f.integer("digits", 6)));
		long value = Math.min((long) Math.pow(10, digits) - 1, (long) f.integer("base", 0) + filled);
		if (p.key == value && p.size > 0) {
			return;
		}
		p.key = value;
		p.clear();
		p.rect(0, 0, w, h, f.colour("bg", 0xFF121816), 0);
		p.text(font, w / 2, h / 2, String.format(Locale.ROOT, "%0" + digits + "d", value), f.colour("fg", 0xFF96FFAA), h * 0.62F, w * 0.92F);
	}

	/** When the floor scrubber is on charge each day (hours of the tower's day), from a fifth full to full. */
	static final double CHARGE_FROM = 5, CHARGE_TO = 11;

	/**
	 * The floor scrubber's battery gauge: on charge in its closet after the night's cleaning, from a fifth full at 05:00
	 * to full at 11:00 (the bars filling, the next one blinking), then CHARGED till the next night.
	 */
	static void charge(Font font, RoomKit.Live f, long dayTime, long gameTime, Picture p, float w, float h) {
		double hour = Math.floorMod(dayTime + 6000, 24000L) / 1000.0;
		boolean charging = hour >= CHARGE_FROM && hour < CHARGE_TO;
		double full = charging ? 0.2 + 0.8 * (hour - CHARGE_FROM) / (CHARGE_TO - CHARGE_FROM) : 1;
		float[] bars = f.floats("bars");
		int count = bars.length / 4, lit = Math.min(count, (int) Math.floor(full * count + 1e-6));
		boolean blink = charging && Math.floorMod(gameTime, 20) < 10;
		long key = (charging ? 1000 : 0) + lit * 2L + (blink ? 1 : 0);
		if (p.key == key && p.size > 0) {
			return;
		}
		p.key = key;
		p.clear();
		int on = f.colour("on", 0xFF5ADC6E), off = f.colour("off", 0xFF3C4640);
		p.rect(0, 0, w, h, f.colour("bg", 0xFF282A2E), 0);
		for (int b = 0; b < count; b++) {
			boolean shown = b < lit || b == lit && blink;
			float x = bars[b * 4] * w, y = bars[b * 4 + 1] * h;
			p.rect(x, y, x + bars[b * 4 + 2] * w, y + bars[b * 4 + 3] * h, shown ? on : off, 1);
		}
		float[] t = f.floats("text");
		if (t.length >= 3) {
			float cx = t[0] * w;
			p.text(font, cx, t[1] * h, charging ? "CHARGING" : "CHARGED", f.colour("fg", 0xFF96E6A0), t[2] * h * 0.75F,
					2 * Math.min(cx, w - cx) * 0.9F);
		}
	}

	// ------------------------------------------------------------------ readouts and traces

	/** A slow wander between -1 and 1 (three sines, their phases from {@code seed}), {@code period} seconds the slowest. */
	static double wander(double seconds, double period, int seed) {
		double a = (seed * 0.6180339887) % 1, b = (seed * 0.4142135623) % 1, c = (seed * 0.7320508075) % 1;
		double t = seconds / Math.max(0.1, period) * 2 * Math.PI;
		return 0.6 * Math.sin(t + a * 6.283) + 0.3 * Math.sin(2.7 * t + b * 6.283) + 0.1 * Math.sin(7.3 * t + c * 6.283);
	}

	/**
	 * What a readout shows now ({@code dayTime} the overworld's clock, {@code gameTime} ticks, {@code seed} the piece's):
	 * <ul>
	 * <li>time: the tower's time of day, HH:MM (plus "hours"); time12: h:MM, as an appliance's clock;
	 * <li>value: "base" wandering by "spread" over "period" seconds, plus "rate" a second over a "cycle" of seconds
	 * (a battery running down, a print getting on), within "min" and "max", to "dp" places, between "pre" and "unit";
	 * <li>dtime: a desk phone's, the day of the week and the time (THU 14:32);
 * <li>timer: the seconds into a "period" (or left of it, "down"), as MM:SS or ("fmt" hms) HH:MM:SS.
	 * </ul>
	 */
	static String reading(RoomKit.Live f, long dayTime, long gameTime, int seed) {
		String src = f.params().has("src") ? f.params().get("src").getAsString() : "time";
		double seconds = gameTime / 20.0;
		switch (src) {
			case "time", "time12", "dtime" -> {
				int[] hm = time(dayTime, f.integer("hours", 0));
				if (src.equals("time12")) {
					int h12 = hm[0] % 12 == 0 ? 12 : hm[0] % 12;
					return String.format(Locale.ROOT, "%d:%02d", h12, hm[1]);
				}
				String t = String.format(Locale.ROOT, "%02d:%02d", hm[0], hm[1]);
				// a desk phone's: the day of the week (the tower's days, from its first Monday) and the time
				return src.equals("dtime") ? DAYS[(int) Math.floorMod(Math.floorDiv(dayTime + 6000, 24000L), 7L)] + " " + t : t;
			}
			case "timer" -> {
				long period = Math.max(1, (long) number(f, "period", 600));
				long into = Math.floorMod((long) seconds + seed * 7L, period);
				long s = f.params().has("down") && f.params().get("down").getAsBoolean() ? period - into : into;
				return "hms".equals(text(f, "fmt")) ? String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
						: String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
			}
			default -> {
				double v = value(f, seconds, seed);
				int dp = f.integer("dp", 0);
				String n = String.format(Locale.ROOT, "%." + dp + "f", v);
				int pad = f.integer("pad", 0);
				if (pad > 0 && n.length() < pad) {
					n = "0".repeat(pad - n.length()) + n;
				}
				return text(f, "pre") + n + text(f, "unit");
			}
		}
	}

	private static final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

	/** A value readout's figure now (see {@link #reading}). */
	static double value(RoomKit.Live f, double seconds, int seed) {
		double cycle = number(f, "cycle", 0);
		double v = number(f, "base", 0) + number(f, "spread", 0) * wander(seconds, number(f, "period", 20), seed)
				+ number(f, "rate", 0) * (cycle > 0 ? Math.floorMod((long) seconds + seed * 13L, (long) cycle) : 0);
		return Math.max(number(f, "min", -1e9), Math.min(number(f, "max", 1e9), v));
	}

	private static double number(RoomKit.Live f, String name, double fallback) {
		return f.params().has(name) ? f.params().get(name).getAsDouble() : fallback;
	}

	private static String text(RoomKit.Live f, String name) {
		return f.params().has(name) ? f.params().get(name).getAsString() : "";
	}

	/** How often a readout can change (ticks): a clock each minute's start is caught within a second; values twice a second. */
	static long readingKey(RoomKit.Live f, long gameTime) {
		String src = f.params().has("src") ? f.params().get("src").getAsString() : "time";
		return Math.floorDiv(gameTime, src.equals("value") ? 10 : 20);
	}

	/**
	 * A readout (an appliance's clock, an instrument's figure, a phone's time): a line of text in the face, on its own
	 * background ("bg"; none, and it is drawn straight over the picture), aligned as "align" (0 left, 1 centred, 2
	 * right), its capitals "cap" of the face's height, or narrower to fit.
	 */
	static void text(Font font, RoomKit.Live f, long dayTime, long gameTime, int seed, Picture p, float w, float h) {
		long key = readingKey(f, gameTime) * 1440 + time(dayTime, 0)[0] * 60L + time(dayTime, 0)[1];
		if (p.key == key && (p.size > 0 || !p.texts.isEmpty())) {
			return;
		}
		p.key = key;
		p.clear();
		if (f.params().has("bg")) {
			p.rect(0, 0, w, h, f.colour("bg", 0xFF000000), 0);
		}
		String s = reading(f, dayTime, gameTime, seed);
		int align = f.integer("align", 1);
		float cap = (float) number(f, "cap", 0.7) * h;
		float scale = Math.min(cap / 7, w * 0.98F / Math.max(1, font.width(s) - 1));
		float x = align == 0 ? 0 : align == 2 ? w : w / 2;
		p.texts.add(new DeskScreen.Text(x, h / 2 - 3.5F * scale, s, f.colour("fg", 0xFFFFFFFF), scale, align));
	}

	/**
	 * A gauge's bar (a print's progress, a motor's throttle): filled as far as a value readout's figure goes between
	 * "min" and "max", in "fg" on "bg", from the left (or, "vertical", up from the bottom).
	 */
	static void bar(RoomKit.Live f, long gameTime, int seed, Picture p, float w, float h) {
		double lo = number(f, "min", 0), hi = number(f, "max", 100);
		double v = value(f, gameTime / 20.0, seed);
		float k = (float) Math.max(0, Math.min(1, (v - lo) / Math.max(1e-6, hi - lo)));
		long key = Math.round(k * 400);
		if (p.key == key && p.size > 0) {
			return;
		}
		p.key = key;
		p.clear();
		p.rect(0, 0, w, h, f.colour("bg", 0xFF202020), 0);
		if (k > 0) {
			if (f.params().has("vertical") && f.params().get("vertical").getAsBoolean()) {
				p.rect(0, h * (1 - k), w, h, f.colour("fg", 0xFF40A0FF), 1);
			} else {
				p.rect(0, 0, w * k, h, f.colour("fg", 0xFF40A0FF), 1);
			}
		}
	}

	/**
	 * A job's progress (a 3D printer's, a plotter's): "label" and the per cent ("text": its left, middle and capitals'
	 * height, fractions of the face) over a bar ("bar": x0, y0, x1, y1) outlined in "off" and filled in "on", on "bg";
	 * the per cent a value readout's figure (it climbs over the job's "cycle", then starts the next).
	 */
	static void progress(Font font, RoomKit.Live f, long gameTime, int seed, Picture p, float w, float h) {
		int pct = (int) Math.max(0, Math.min(100, Math.floor(value(f, gameTime / 20.0, seed))));
		if (p.key == pct && p.size > 0) {
			return;
		}
		p.key = pct;
		p.clear();
		p.rect(0, 0, w, h, f.colour("bg", 0xFF0E1622), 0);
		float[] t = f.floats("text"), b = f.floats("bar");
		if (b.length >= 4) {
			float x0 = b[0] * w, y0 = b[1] * h, x1 = b[2] * w, y1 = b[3] * h, e = Math.max(h * 0.012F, (y1 - y0) * 0.08F);
			int off = f.colour("off", 0xFF5078A0);
			p.rect(x0, y0, x1, y0 + e, off, 1);
			p.rect(x0, y1 - e, x1, y1, off, 1);
			p.rect(x0, y0, x0 + e, y1, off, 1);
			p.rect(x1 - e, y0, x1, y1, off, 1);
			if (pct > 0) {
				p.rect(x0, y0, x0 + (x1 - x0) * pct / 100F, y1, f.colour("on", 0xFF50AAF0), 2);
			}
		}
		if (t.length >= 3) {
			String s = text(f, "label") + pct + "%";
			float x = t[0] * w, cap = t[2] * h;
			float scale = Math.min(cap / 7, (w - x - w * 0.02F) / Math.max(1, font.width(s) - 1));
			p.texts.add(new DeskScreen.Text(x, t[1] * h - 3.5F * scale, s, f.colour("fg", 0xFFB4DCFF), scale, 0));
		}
	}

	/** A trace's shape at phase {@code x} (0 to 1 a beat): from -1 to 1. */
	static double shape(String kind, double x, int seed) {
		x -= Math.floor(x);
		return switch (kind) {
			case "ecg" -> 0.12 * bump(x, 0.14, 0.035) - 0.12 * bump(x, 0.285, 0.008) + 1.0 * bump(x, 0.3, 0.009) - 0.28 * bump(x, 0.318, 0.01)
					+ 0.3 * bump(x, 0.56, 0.05);
			case "pleth" -> 1.6 * bump(x, 0.3, 0.09) + 0.5 * bump(x, 0.55, 0.07) - 0.6;
			case "square" -> x < 0.5 ? 0.8 : -0.8;
			case "noise" -> 0.8 * wander(x * 40, 1.3, seed);
			default -> Math.sin(x * 2 * Math.PI);
		};
	}

	private static double bump(double x, double at, double width) {
		double d = (x - at) / width;
		return Math.exp(-d * d);
	}

	/**
	 * A trace (an oscilloscope's, a patient monitor's ECG or pleth): "shape", "cycles" across the face, moving at
	 * "speed" cycles a second, "amp" of the half-height about "mid" (of the height, down), the line "thick" (of the
	 * height), "jitter" of noise on it; a monitor's ("sweep") is drawn left to right with a gap ahead of the pen, a
	 * scope's runs still (a triggered wave).
	 */
	static void wave(RoomKit.Live f, long gameTime, float partial, int seed, Picture p, float w, float h) {
		p.key = gameTime;
		p.clear();
		if (f.params().has("bg")) {
			p.rect(0, 0, w, h, f.colour("bg", 0xFF000000), 0);
		}
		if (f.params().has("grid")) {
			int grid = f.colour("grid", 0xFF203020), n = Math.max(2, f.integer("gridn", 8));
			float t = h * 0.012F;
			for (int i = 1; i < n; i++) {
				float x = w * i / n;
				p.rect(x - t / 2, 0, x + t / 2, h, grid, 1);
			}
			for (int i = 1; i < 4; i++) {
				float y = h * i / 4;
				p.rect(0, y - t / 2, w, y + t / 2, grid, 1);
			}
		}
		String kind = f.params().has("shape") ? f.params().get("shape").getAsString() : "sine";
		double cycles = number(f, "cycles", 3), speed = number(f, "speed", 1), amp = number(f, "amp", 0.7), mid = number(f, "mid", 0.5);
		double jitter = number(f, "jitter", 0);
		float thick = (float) number(f, "thick", 0.04) * h;
		boolean sweep = f.params().has("sweep") && f.params().get("sweep").getAsBoolean();
		double seconds = (gameTime + partial) / 20.0;
		int fg = f.colour("fg", 0xFF40FF60);
		int n = 72;
		double pen = sweep ? (seconds * speed / cycles) % 1 : -1;
		float px = 0, py = 0;
		for (int i = 0; i <= n; i++) {
			double u = (double) i / n;
			// a sweep's trace was drawn by the pen: behind it this beat's, ahead of it (past the gap) the last one's
			double phase = sweep ? u * cycles + Math.floor(seconds * speed / cycles) * cycles - (u > pen ? cycles : 0)
					: u * cycles + (kind.equals("noise") ? seconds * speed : 0);
			// (a live signal: a little noise on it, never still)
			double noise = jitter == 0 ? 0 : jitter * wander(seconds * 13 + u * 37, 0.9, seed + i);
			float x = (float) (u * w), y = (float) (h * mid - (shape(kind, phase, seed) + noise) * amp * h / 2);
			boolean gap = sweep && u > pen && u < pen + 0.06;
			if (i > 0 && !gap) {
				float dx = x - px, dy = y - py, len = (float) Math.sqrt(dx * dx + dy * dy), nx = -dy / len * thick / 2, ny = dx / len * thick / 2;
				p.put(px + nx, py + ny, px - nx, py - ny, x - nx, y - ny, x + nx, y + ny, fg, 2);
			}
			px = x;
			py = y;
		}
	}
}
