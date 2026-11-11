package io.github.jimbozoomer.jugcraft.client;

import java.util.ArrayList;
import java.util.List;

/**
 * A ticker rail: a line of text running along a path of straight pieces and arcs, scrolling on as a ticker does. Justin
 * asked for it on 22 October 2026 as a system to put on things ("make a system so you can do stuff like that ... in
 * place for you to apply to multiple things"); it is not something players build. On it run the status ring round the
 * hologram's rotunda (HoloMapRenderer: smooth lettering on a dark band, the text repeating end to end) and the name
 * round a Drone Display Case's light hood (DroneDisplayCaseRenderer: an amber LED dot band, the name in four evenly
 * spaced spots).
 *
 * <p>The path lies level, at the height of the band's middle, and is read from one side: each piece from the right-hand
 * side of the way it runs (a straight piece from its start to its end; an arc clockwise as seen from above is read from
 * inside, anticlockwise from outside). So text reads on from piece to piece round a loop, turning its corners, as a
 * reader in front of each piece reads it, left to right; the text moves right to left.
 *
 * <ul>
 * <li>{@link Style#FONT}: one character a slot ({@code pitch} apart), drawn by the sink in the game's font
 * ({@link Sink#glyph}), each on a piece of dark band ({@link #BAND} quads) set a little behind it. A slot is left out
 * where it would turn a corner between two pieces.</li>
 * <li>{@link Style#DOTS}: a 5 x 7 LED dot matrix (the classic character-display shapes, tools/dotfont.py), columns
 * {@code pitch} apart (each piece a whole number of them), {@code rows} rows high with the glyph in the middle seven;
 * the lit dots are {@link #DOT} quads standing {@code raise} in front of the band (the unlit band itself is the thing's
 * own, e.g. a dot-panel texture in its mesh, unless a band colour is given). It steps a whole column at a time.</li>
 * </ul>
 * The content is either the text over and over end to end ({@code copies} 0) or {@code copies} copies of it spaced
 * evenly along the path. A gate can leave out the parts of the rail that are not there (the ring shows only where its
 * slab edge stands).
 */
public final class TickerRail {
	/** How a rail draws its text. */
	public enum Style {
		FONT, DOTS
	}

	/** Quad kinds a sink is given: a piece of the dark band, a lit dot. */
	public static final int BAND = 0, DOT = 1;
	/** The middle dot that separates the parts of a ticker's text: the font style draws it in the separator colour. */
	public static final char SEPARATOR = '·';

	/** Where the rail's quads and glyphs go (in the frame the path is given in). */
	public interface Sink {
		/**
		 * A flat quad, corners a, b, c, d in order round it (top-left, top-right, bottom-right, bottom-left as its reader
		 * sees them), facing the reader along (nx, 0, nz).
		 */
		void quad(int kind, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
				float nx, float nz, int argb);

		/** One character in the font style: its middle at (x, y, z), turned {@code yaw} (0 reads from +z), {@code px} blocks a font pixel. */
		default void glyph(float x, float y, float z, float yaw, float px, char ch, int argb) {
		}
	}

	/** Whether the rail is there at (x, z) on its path. */
	public interface Gate {
		boolean shows(double x, double z);
	}

	// ------------------------------------------------------------------ the path

	/** A piece of the path: a straight line, or an arc round (cx, cz) of radius r from angle a0 turning by sweep. */
	private record Piece(boolean arc, double x0, double z0, double x1, double z1, double cx, double cz, double r, double a0, double sweep,
			double length) {
		/** The point s along it: x, z and the reader's side (nx, nz). */
		void at(double s, double[] out) {
			if (!arc) {
				double t = s / length, dx = (x1 - x0) / length, dz = (z1 - z0) / length;
				out[0] = x0 + (x1 - x0) * t;
				out[1] = z0 + (z1 - z0) * t;
				out[2] = -dz;
				out[3] = dx;
			} else {
				double sign = Math.signum(sweep), a = a0 + sign * s / r;
				// angles clockwise from north, as the tower's renderers measure them: (sin a, -cos a) points out
				out[0] = cx + Math.sin(a) * r;
				out[1] = cz - Math.cos(a) * r;
				double tx = sign * Math.cos(a), tz = sign * Math.sin(a);
				out[2] = -tz;
				out[3] = tx;
			}
		}
	}

	private final Piece[] pieces;
	private final double[] starts;
	private final double length;
	private final boolean closed;
	private final double y;
	private final Style style;
	private final double pitch, half, back, raise, glyphPx, speed;
	private final int rows, copies, textColor, separatorColor, bandColor;
	/** Per piece, its first column (DOTS); then the total. */
	private final int[] columnStarts;
	private final int columns;

	private TickerRail(Builder b) {
		pieces = b.pieces.toArray(Piece[]::new);
		starts = new double[pieces.length + 1];
		for (int i = 0; i < pieces.length; i++) {
			starts[i + 1] = starts[i] + pieces[i].length();
		}
		length = starts[pieces.length];
		closed = b.closed;
		y = b.y;
		style = b.style;
		pitch = b.pitch;
		half = b.half;
		back = b.back;
		raise = b.raise;
		glyphPx = b.glyphPx;
		speed = b.speed;
		rows = b.rows;
		copies = b.copies;
		textColor = b.textColor;
		separatorColor = b.separatorColor;
		bandColor = b.bandColor;
		columnStarts = new int[pieces.length + 1];
		for (int i = 0; i < pieces.length; i++) {
			columnStarts[i + 1] = columnStarts[i] + Math.max(1, (int) Math.round(pieces[i].length() / pitch));
		}
		columns = columnStarts[pieces.length];
	}

	/** A rail whose band's middle is at height {@code y}. */
	public static Builder at(double y) {
		return new Builder(y);
	}

	/** The path's length (blocks). */
	public double length() {
		return length;
	}

	/** The dot columns along the whole path (DOTS). */
	public int columns() {
		return columns;
	}

	private int pieceAt(double s) {
		for (int i = 0; i < pieces.length - 1; i++) {
			if (s < starts[i + 1]) {
				return i;
			}
		}
		return pieces.length - 1;
	}

	/** The point {@code s} along the path: x, z, and the reader's side (nx, nz). */
	public void point(double s, double[] out) {
		int i = pieceAt(s);
		pieces[i].at(Math.min(Math.max(s - starts[i], 0), pieces[i].length()), out);
	}

	// ------------------------------------------------------------------ drawing

	/** Draws {@code text} as it stands at {@code now} (ticks), where {@code gate} (or everywhere, if null) lets it. */
	public void draw(String text, double now, Gate gate, Sink sink) {
		if (text.isEmpty() || length <= 0) {
			return;
		}
		if (style == Style.FONT) {
			font(text, now, gate, sink);
		} else {
			dots(text, now, gate, sink);
		}
	}

	private void font(String text, double now, Gate gate, Sink sink) {
		int n = text.length();
		double u = now * speed;
		double[] a = new double[4], b = new double[4], m = new double[4];
		if (copies <= 0) {
			long first = (long) Math.floor(u / pitch);
			double shift = u - first * pitch;
			int slots = (int) (length / pitch);
			for (int k = 0; k <= slots; k++) {
				double s = k * pitch - shift;
				if (s >= 0 && s <= length - pitch) {
					slot(s, text.charAt((int) Math.floorMod(first + k, (long) n)), gate, sink, a, b, m);
				}
			}
		} else {
			double period = length / copies;
			for (int c = 0; c < copies; c++) {
				for (int j = 0; j < n; j++) {
					double s = c * period + j * pitch - u;
					s = closed ? s - length * Math.floor(s / length) : s;
					if (s >= 0 && s <= length - pitch) {
						slot(s, text.charAt(j), gate, sink, a, b, m);
					}
				}
			}
		}
	}

	/** One character's slot from {@code s}: its piece of band and its glyph (none where it would turn a corner). */
	private void slot(double s, char ch, Gate gate, Sink sink, double[] a, double[] b, double[] m) {
		if (pieceAt(s) != pieceAt(s + pitch * 0.999)) {
			return;
		}
		point(s + pitch / 2, m);
		if (gate != null && !gate.shows(m[0], m[1])) {
			return;
		}
		point(s, a);
		point(s + pitch, b);
		if (bandColor != 0) {
			float ax = (float) (a[0] - a[2] * back), az = (float) (a[1] - a[3] * back);
			float bx = (float) (b[0] - b[2] * back), bz = (float) (b[1] - b[3] * back);
			float yb = (float) (y - half), yt = (float) (y + half);
			sink.quad(BAND, ax, yt, az, bx, yt, bz, bx, yb, bz, ax, yb, az, (float) m[2], (float) m[3], bandColor);
		}
		if (ch != ' ') {
			sink.glyph((float) m[0], (float) y, (float) m[1], (float) Math.atan2(m[2], m[3]), (float) glyphPx, ch,
					ch == SEPARATOR ? separatorColor : textColor);
		}
	}

	private void dots(String text, double now, Gate gate, Sink sink) {
		int[] cols = columns(text);
		if (cols.length == 0) {
			return;
		}
		long step = (long) Math.floor(now * speed);
		int[] lit = new int[columns];
		if (copies <= 0) {
			for (int i = 0; i < columns; i++) {
				lit[i] = cols[(int) Math.floorMod(i + step, (long) cols.length)];
			}
		} else {
			for (int c = 0; c < copies; c++) {
				long start = Math.round((double) c * columns / copies) - step;
				for (int k = 0; k < cols.length; k++) {
					long i = start + k;
					if (closed) {
						i = Math.floorMod(i, (long) columns);
					} else if (i < 0 || i >= columns) {
						continue;
					}
					lit[(int) i] |= cols[k];
				}
			}
		}
		double pitchY = 2 * half / rows;
		int margin = (rows - 7) / 2;
		double top = y + half;
		double[] a = new double[4], b = new double[4], m = new double[4];
		for (int p = 0; p < pieces.length; p++) {
			Piece piece = pieces[p];
			int n = columnStarts[p + 1] - columnStarts[p];
			double w = piece.length() / n;
			if (bandColor != 0) {
				// (an arc's band in steps, a column each)
				int steps = piece.arc() ? n : 1;
				for (int q = 0; q < steps; q++) {
					piece.at(piece.length() * q / steps, a);
					piece.at(piece.length() * (q + 1) / steps, b);
					sink.quad(BAND, (float) a[0], (float) (y + half), (float) a[1], (float) b[0], (float) (y + half), (float) b[1], (float) b[0],
							(float) (y - half), (float) b[1], (float) a[0], (float) (y - half), (float) a[1], (float) a[2], (float) a[3], bandColor);
				}
			}
			for (int q = 0; q < n; q++) {
				int bits = lit[columnStarts[p] + q];
				if (bits == 0) {
					continue;
				}
				if (gate != null) {
					piece.at((q + 0.5) * w, m);
					if (!gate.shows(m[0], m[1])) {
						continue;
					}
				}
				piece.at((q + 0.18) * w, a);
				piece.at((q + 0.82) * w, b);
				float ax = (float) (a[0] + a[2] * raise), az = (float) (a[1] + a[3] * raise);
				float bx = (float) (b[0] + b[2] * raise), bz = (float) (b[1] + b[3] * raise);
				float nx = (float) ((a[2] + b[2]) / 2), nz = (float) ((a[3] + b[3]) / 2);
				for (int r = 0; r < 7; r++) {
					if ((bits >> r & 1) == 0) {
						continue;
					}
					float yt = (float) (top - (r + margin) * pitchY - pitchY * 0.18), yb = (float) (top - (r + margin + 1) * pitchY + pitchY * 0.18);
					sink.quad(DOT, ax, yt, az, bx, yt, bz, bx, yb, bz, ax, yb, az, nx, nz, textColor);
				}
			}
		}
	}

	// ------------------------------------------------------------------ the dot font

	/** Per character, its five columns (bit 0 the top row); from tools/dotfont.py. */
	private static final int[][] DOTS = new int[0x100][];

	private static void glyph(char ch, int... columns) {
		DOTS[ch] = columns;
	}

	static {
		glyph('A', 126, 9, 9, 9, 126);
		glyph('B', 127, 73, 73, 73, 54);
		glyph('C', 62, 65, 65, 65, 34);
		glyph('D', 127, 65, 65, 65, 62);
		glyph('E', 127, 73, 73, 73, 65);
		glyph('F', 127, 9, 9, 9, 1);
		glyph('G', 62, 65, 73, 73, 122);
		glyph('H', 127, 8, 8, 8, 127);
		glyph('I', 0, 65, 127, 65, 0);
		glyph('J', 32, 64, 65, 63, 1);
		glyph('K', 127, 8, 20, 34, 65);
		glyph('L', 127, 64, 64, 64, 64);
		glyph('M', 127, 2, 12, 2, 127);
		glyph('N', 127, 4, 8, 16, 127);
		glyph('O', 62, 65, 65, 65, 62);
		glyph('P', 127, 9, 9, 9, 6);
		glyph('Q', 62, 65, 81, 33, 94);
		glyph('R', 127, 9, 25, 41, 70);
		glyph('S', 70, 73, 73, 73, 49);
		glyph('T', 1, 1, 127, 1, 1);
		glyph('U', 63, 64, 64, 64, 63);
		glyph('V', 31, 32, 64, 32, 31);
		glyph('W', 63, 64, 56, 64, 63);
		glyph('X', 99, 20, 8, 20, 99);
		glyph('Y', 3, 4, 120, 4, 3);
		glyph('Z', 97, 81, 73, 69, 67);
		glyph('0', 62, 81, 73, 69, 62);
		glyph('1', 0, 66, 127, 64, 0);
		glyph('2', 66, 97, 81, 73, 70);
		glyph('3', 33, 65, 69, 75, 49);
		glyph('4', 24, 20, 18, 127, 16);
		glyph('5', 39, 69, 69, 69, 57);
		glyph('6', 60, 74, 73, 73, 48);
		glyph('7', 1, 113, 9, 5, 3);
		glyph('8', 54, 73, 73, 73, 54);
		glyph('9', 6, 73, 73, 41, 30);
		glyph(' ', 0, 0, 0, 0, 0);
		glyph('-', 8, 8, 8, 8, 8);
		glyph('\u00B7', 0, 0, 8, 0, 0);
		glyph('.', 0, 96, 96, 0, 0);
		glyph(',', 0, 80, 48, 0, 0);
		glyph(':', 0, 54, 54, 0, 0);
		glyph('/', 32, 16, 8, 4, 2);
		glyph('%', 35, 19, 8, 100, 98);
		glyph('+', 8, 8, 62, 8, 8);
		glyph('#', 20, 127, 20, 127, 20);
		glyph('>', 65, 34, 20, 8, 0);
		glyph('<', 0, 8, 20, 34, 65);
		glyph('!', 0, 0, 95, 0, 0);
		glyph('?', 2, 1, 81, 9, 6);
		glyph('\'', 0, 4, 3, 0, 0);
		glyph('(', 0, 28, 34, 65, 0);
		glyph(')', 0, 65, 34, 28, 0);
	}

	/** The text as dot columns: each character's five, then a dark column. */
	static int[] columns(String text) {
		int[] out = new int[text.length() * 6];
		int k = 0;
		for (int i = 0; i < text.length(); i++) {
			char ch = Character.toUpperCase(text.charAt(i));
			int[] g = ch < DOTS.length && DOTS[ch] != null ? DOTS[ch] : DOTS['?'];
			for (int c : g) {
				out[k++] = c;
			}
			out[k++] = 0;
		}
		return out;
	}

	// ------------------------------------------------------------------ building one

	public static final class Builder {
		private final List<Piece> pieces = new ArrayList<>();
		private final double y;
		private boolean closed;
		private Style style = Style.FONT;
		private double pitch = 0.42, half = 0.31, back = 0.025, raise = 0.003, glyphPx = 0.07, speed = 0.06;
		private int rows = 9, copies, textColor = 0xFFFFE6BC, separatorColor = 0xFFFFA040, bandColor;

		private Builder(double y) {
			this.y = y;
		}

		/** A straight piece from (x0, z0) to (x1, z1), read from the right-hand side of that way. */
		public Builder line(double x0, double z0, double x1, double z1) {
			pieces.add(new Piece(false, x0, z0, x1, z1, 0, 0, 0, 0, 0, Math.hypot(x1 - x0, z1 - z0)));
			return this;
		}

		/**
		 * An arc round (cx, cz), radius {@code r}, from angle {@code from} to {@code to} (radians clockwise from north):
		 * clockwise (to greater than from) it is read from inside, anticlockwise from outside.
		 */
		public Builder arc(double cx, double cz, double r, double from, double to) {
			pieces.add(new Piece(true, 0, 0, 0, 0, cx, cz, r, from, to - from, Math.abs(to - from) * r));
			return this;
		}

		/** The path is a loop (the text carries on past its end round to its start). */
		public Builder closed() {
			closed = true;
			return this;
		}

		/** Smooth lettering: a character every {@code pitch} blocks, {@code px} blocks a font pixel. */
		public Builder font(double pitch, double px) {
			style = Style.FONT;
			this.pitch = pitch;
			glyphPx = px;
			return this;
		}

		/** An LED dot matrix: dot columns about {@code pitch} apart, {@code rows} rows high (the glyph the middle seven). */
		public Builder dots(double pitch, int rows) {
			style = Style.DOTS;
			this.pitch = pitch;
			this.rows = Math.max(7, rows);
			return this;
		}

		/** The band: half its height; behind the glyphs by {@code back} (font), the dots in front of it by {@code raise}. */
		public Builder band(double half, double back, double raise) {
			this.half = half;
			this.back = back;
			this.raise = raise;
			return this;
		}

		/** Text and separator colours, and the band's (0: none drawn). */
		public Builder colours(int text, int separator, int band) {
			textColor = text;
			separatorColor = separator;
			bandColor = band;
			return this;
		}

		/** How fast it moves: blocks a tick (font), dot columns a tick (dots). */
		public Builder speed(double speed) {
			this.speed = speed;
			return this;
		}

		/** The text over and over end to end (0), or this many copies spaced evenly along the path. */
		public Builder copies(int copies) {
			this.copies = copies;
			return this;
		}

		public TickerRail build() {
			if (pieces.isEmpty()) {
				throw new IllegalStateException("a ticker rail needs a path");
			}
			return new TickerRail(this);
		}
	}
}
