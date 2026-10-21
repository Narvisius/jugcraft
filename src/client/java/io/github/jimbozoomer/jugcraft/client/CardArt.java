package io.github.jimbozoomer.jugcraft.client;

/**
 * Playing cards drawn as pixel art, the same at every table: rank and suit in the top-left corner and again in the
 * bottom-right, the real pip layout on number cards, one big suit on an ace, a framed portrait on jacks, queens
 * and kings, and a red lattice back. Everything is built from filled rectangles, so it scales cleanly from the small
 * cards at a poker seat to the big ones in video poker.
 *
 * <p>Cards are numbered as {@code PokerHands} numbers them: rank {@code card % 13} (0 is a two, 12 an ace) and suit
 * {@code card / 13} (clubs, diamonds, hearts, spades). -2 is a face-down card and -1 no card at all.
 */
public final class CardArt {
	/** Where the pixels go: a filled rectangle from (x0, y0) to (x1, y1), exclusive, in ARGB. */
	public interface Pixels {
		void fill(int x0, int y0, int x1, int y1, int argb);
	}

	public static final int FACE = 0xFFF6F0E2;
	public static final int EDGE = 0xFF1A1410;
	public static final int RED = 0xFFC8302A;
	public static final int BLACK = 0xFF1C1C22;
	private static final int BACK_RED = 0xFFA3262A;
	private static final int BACK_DARK = 0xFF6E1418;
	private static final int BACK_GOLD = 0xFFE0B048;
	private static final int GOLD = 0xFFE0B040;
	private static final int GOLD_DARK = 0xFFA87A22;
	private static final int SKIN = 0xFFF2C9A0;
	private static final int HAIR = 0xFF6A3A1A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int PORTRAIT_BG = 0xFFF4E4B4;

	// Suits, small (5x5) and large (7x7): clubs, diamonds, hearts, spades.
	private static final String[][] SMALL_SUITS = {
			{".XXX.", "XXXXX", "XXXXX", "..X..", ".XXX."},
			{"..X..", ".XXX.", "XXXXX", ".XXX.", "..X.."},
			{"XX.XX", "XXXXX", "XXXXX", ".XXX.", "..X.."},
			{"..X..", ".XXX.", "XXXXX", "XXXXX", ".XXX."}};
	private static final String[][] LARGE_SUITS = {
			{"..XXX..", "..XXX..", "XX.X.XX", "XXXXXXX", "XX.X.XX", "...X...", "..XXX.."},
			{"...X...", "..XXX..", ".XXXXX.", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."},
			{".XX.XX.", "XXXXXXX", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."},
			{"...X...", "..XXX..", ".XXXXX.", "XXXXXXX", "XXXXXXX", "...X...", "..XXX.."}};

	// Ranks in a 3x5 pixel font: 2..9, then T (drawn as 10), J, Q, K, A.
	private static final String[][] RANKS = {
			{"XXX", "..X", "XXX", "X..", "XXX"},
			{"XXX", "..X", ".XX", "..X", "XXX"},
			{"X.X", "X.X", "XXX", "..X", "..X"},
			{"XXX", "X..", "XXX", "..X", "XXX"},
			{"XXX", "X..", "XXX", "X.X", "XXX"},
			{"XXX", "..X", ".X.", ".X.", ".X."},
			{"XXX", "X.X", "XXX", "X.X", "XXX"},
			{"XXX", "X.X", "XXX", "..X", "XXX"},
			{"X.XXX", "X.X.X", "X.X.X", "X.X.X", "X.XXX"},
			{"..X", "..X", "..X", "X.X", "XXX"},
			{"XXX", "X.X", "X.X", "XX.", ".XX"},
			{"X.X", "X.X", "XX.", "X.X", "X.X"},
			{".X.", "X.X", "XXX", "X.X", "X.X"}};

	// Court portraits, 11x14. Y gold, S skin, K dark, C the suit's colour, W white, H hair.
	private static final String[] JACK = {
			"......WW...",
			"..CCCCCW...",
			".CCCCCCCC..",
			"YYYYYYYYYYY",
			"..HSSSSSH..",
			"..SKSSSKS..",
			"..SSSSSSS..",
			"..SSSKSSS..",
			"...SSSSS...",
			"..WWWWWWW..",
			"CCCCWYWCCCC",
			"CCCCCWCCCCC",
			"CCCCWYWCCCC",
			"CCCCCWCCCCC"};
	private static final String[] QUEEN = {
			"...Y.Y.Y...",
			"...YYYYY...",
			"..HHHHHHH..",
			".HHSSSSSHH.",
			".HSKSSSKSH.",
			".HSSSSSSSH.",
			".HSSSCSSSH.",
			".HHSSSSSHH.",
			"HHH.SSS.HHH",
			"HCCCWWWCCCH",
			"CCCCWYWCCCC",
			"CCCCCWCCCCC",
			"CCCWWYWWCCC",
			"CCCCCWCCCCC"};
	private static final String[] KING = {
			"Y.Y..Y..Y.Y",
			"YYYYYYYYYYY",
			"YCYYYCYYYCY",
			"..KSSSSSK..",
			"..SKSSSKS..",
			"..SSSSSSS..",
			"..KSSKSSK..",
			"..KKKKKKK..",
			"...KKKKK...",
			".CCCWWWCCC.",
			"CCCCYWYCCCC",
			"CCCCCWCCCCC",
			"CCCCYWYCCCC",
			"CCCCCWCCCCC"};

	// Pip positions for 2..10: {column 0-2, row in sixths 0-6}.
	private static final int[][][] PIPS = {
			{{1, 0}, {1, 6}},
			{{1, 0}, {1, 3}, {1, 6}},
			{{0, 0}, {2, 0}, {0, 6}, {2, 6}},
			{{0, 0}, {2, 0}, {1, 3}, {0, 6}, {2, 6}},
			{{0, 0}, {2, 0}, {0, 3}, {2, 3}, {0, 6}, {2, 6}},
			{{0, 0}, {2, 0}, {1, 1}, {0, 3}, {2, 3}, {0, 6}, {2, 6}},
			{{0, 0}, {2, 0}, {1, 1}, {0, 3}, {2, 3}, {1, 5}, {0, 6}, {2, 6}},
			{{0, 0}, {2, 0}, {0, 2}, {2, 2}, {1, 3}, {0, 4}, {2, 4}, {0, 6}, {2, 6}},
			{{0, 0}, {2, 0}, {1, 1}, {0, 2}, {2, 2}, {0, 4}, {2, 4}, {1, 5}, {0, 6}, {2, 6}}};

	private CardArt() {
	}

	/** Draws a card filling (x, y, w, h). */
	public static void draw(Pixels p, int x, int y, int w, int h, int card) {
		if (card == -1 || w < 6 || h < 8) {
			return;
		}
		// A dark outline with its corners cut, so the card looks rounded.
		p.fill(x + 1, y, x + w - 1, y + h, EDGE);
		p.fill(x, y + 1, x + w, y + h - 1, EDGE);
		if (card < 0) {
			back(p, x + 1, y + 1, w - 2, h - 2);
			return;
		}
		p.fill(x + 1, y + 1, x + w - 1, y + h - 1, FACE);
		int rank = card % 13;
		int suit = card / 13;
		int colour = suit == 1 || suit == 2 ? RED : BLACK;
		int scale = h >= 64 ? 2 : 1;
		// Top-left corner: rank, and the suit under it.
		int cx = x + 2 + (scale == 2 ? 1 : 0);
		int cy = y + 2 + (scale == 2 ? 1 : 0);
		glyph(p, RANKS[rank], cx, cy, scale, colour, false);
		glyph(p, SMALL_SUITS[suit], cx, cy + 6 * scale, scale, colour, false);
		int cornerW = Math.max(width(RANKS[rank]), 5) * scale + 2;
		boolean roomy = h >= 34;
		if (roomy) {
			// Bottom-right corner (kept upright: an upside-down heart reads as a spade at this size).
			int bx = x + w - 2 - (scale == 2 ? 1 : 0);
			int by = y + h - 2 - (scale == 2 ? 1 : 0);
			glyph(p, SMALL_SUITS[suit], bx - 5 * scale, by - 11 * scale, scale, colour, false);
			glyph(p, RANKS[rank], bx - width(RANKS[rank]) * scale, by - 5 * scale, scale, colour, false);
		}
		// The middle of the card, clear of the corners.
		int mx0 = x + (roomy ? cornerW + 1 : 2);
		int mx1 = x + w - (roomy ? cornerW + 1 : 2);
		int my0 = y + (roomy ? 3 : 2 + 12 * scale);
		int my1 = y + h - 3;
		if (!roomy) {
			// A small card: one suit in the lower right of the face.
			int s = 7;
			glyph(p, LARGE_SUITS[suit], x + w - 2 - s, y + h - 2 - s, 1, colour, false);
			return;
		}
		if (rank >= 9 && rank <= 11) {
			court(p, rank == 9 ? JACK : rank == 10 ? QUEEN : KING, mx0, my0, mx1, my1, colour);
		} else if (rank == 12) {
			int big = Math.max(1, Math.min((mx1 - mx0) / 7, (my1 - my0) / 7));
			big = Math.min(big, h >= 64 ? 5 : 3);
			glyph(p, LARGE_SUITS[suit], (mx0 + mx1 - 7 * big) / 2, (my0 + my1 - 7 * big) / 2, big, colour, false);
		} else {
			pips(p, rank, suit, mx0, my0, mx1, my1, colour, h);
		}
	}

	private static void pips(Pixels p, int rank, int suit, int x0, int y0, int x1, int y1, int colour, int h) {
		boolean large = h >= 64;
		String[] sprite = large ? LARGE_SUITS[suit] : SMALL_SUITS[suit];
		int size = sprite.length;
		if (h < 46) {
			// Not enough room for every pip: one suit in the middle.
			glyph(p, LARGE_SUITS[suit], (x0 + x1 - 7) / 2, (y0 + y1 - 7) / 2, 1, colour, false);
			return;
		}
		int[][] spots = PIPS[rank];
		int colGap = Math.max(size + 1, (x1 - x0 - size) / 2 - 1);
		int left = (x0 + x1) / 2 - colGap - size / 2;
		int rowSpan = y1 - y0 - size - 2;
		for (int[] spot : spots) {
			int px = left + spot[0] * colGap;
			int py = y0 + 1 + spot[1] * rowSpan / 6;
			glyph(p, sprite, px, py, 1, colour, false);
		}
	}

	private static void court(Pixels p, String[] art, int x0, int y0, int x1, int y1, int colour) {
		int scale = Math.max(1, Math.min((x1 - x0 - 2) / 11, (y1 - y0 - 2) / 14));
		int pw = 11 * scale;
		int ph = 14 * scale;
		int px = (x0 + x1 - pw) / 2;
		int py = (y0 + y1 - ph) / 2;
		// A gold frame round a pale panel, and the figure in it.
		p.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, GOLD_DARK);
		p.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, PORTRAIT_BG);
		for (int row = 0; row < art.length; row++) {
			String line = art[row];
			for (int col = 0; col < line.length(); col++) {
				int c = switch (line.charAt(col)) {
					case 'Y' -> GOLD;
					case 'S' -> SKIN;
					case 'K' -> 0xFF2A1A12;
					case 'C' -> colour;
					case 'W' -> WHITE;
					case 'H' -> HAIR;
					default -> 0;
				};
				if (c != 0) {
					p.fill(px + col * scale, py + row * scale, px + (col + 1) * scale, py + (row + 1) * scale, c);
				}
			}
		}
	}

	private static void back(Pixels p, int x, int y, int w, int h) {
		// White border, then a red field with a diamond lattice and a gold emblem in the middle.
		p.fill(x, y, x + w, y + h, WHITE);
		int fx0 = x + 1;
		int fy0 = y + 1;
		int fx1 = x + w - 1;
		int fy1 = y + h - 1;
		p.fill(fx0, fy0, fx1, fy1, BACK_RED);
		for (int yy = fy0; yy < fy1; yy++) {
			for (int xx = fx0; xx < fx1; xx++) {
				int i = xx - fx0;
				int j = yy - fy0;
				if ((i + j) % 4 == 0 || ((i - j) % 4 + 4) % 4 == 0) {
					p.fill(xx, yy, xx + 1, yy + 1, BACK_DARK);
				}
			}
		}
		if (w >= 14 && h >= 18) {
			int cx = x + w / 2;
			int cy = y + h / 2;
			int r = Math.max(2, Math.min(w, h) / 5);
			for (int d = -r; d <= r; d++) {
				int half = r - Math.abs(d);
				p.fill(cx - half - 1, cy + d, cx + half + 2, cy + d + 1, BACK_GOLD);
			}
			for (int d = -r + 2; d <= r - 2; d++) {
				int half = r - 2 - Math.abs(d);
				p.fill(cx - half, cy + d, cx + half + 1, cy + d + 1, BACK_RED);
			}
		}
	}

	private static int width(String[] art) {
		return art[0].length();
	}

	/** Draws a bitmap at (x, y), each pixel {@code scale} square; {@code flip} turns it upside down (for the lower corner). */
	private static void glyph(Pixels p, String[] art, int x, int y, int scale, int colour, boolean flip) {
		int rows = art.length;
		for (int row = 0; row < rows; row++) {
			String line = art[flip ? rows - 1 - row : row];
			int n = line.length();
			for (int col = 0; col < n; col++) {
				char ch = line.charAt(flip ? n - 1 - col : col);
				if (ch != '.') {
					p.fill(x + col * scale, y + row * scale, x + (col + 1) * scale, y + (row + 1) * scale, colour);
				}
			}
		}
	}
}
