package io.github.jimbozoomer.jugcraft.client;

import static io.github.jimbozoomer.jugcraft.client.ExchangeScreen.*;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * How a casino game's screen is dressed: the frame's colours, the header and bottom plaques, the title colour and an
 * animated decoration that fits the machine (chasing bulbs on a carnival wheel, neon on the arcade, turning gears on the
 * clockwork slots, lava cracks on the Nether Furnace, rising bubbles on the Sunken Treasure, and so on).
 */
public record CasinoTheme(int face, int light, int dark, int accent, int plaque, int plaqueDark, int plaqueLight, int title,
		int bulbOn, int bulbOff, Style style) {
	public enum Style { FILIGREE, BULBS, NEON, GEARS, LAVA, BUBBLES, PIXELS, CHROME, STARS, CARNIVAL, WAVES, SCALES }

	/** The Exchange's own walnut and brass. */
	public static final CasinoTheme WALNUT = new CasinoTheme(ExchangeScreen.WALNUT, WALNUT_LIGHT, WALNUT_DARK, BRASS, PLAQUE,
			PLAQUE_DARK, PLAQUE_LIGHT, GOLD, GOLD, GOLD_DIM, Style.FILIGREE);
	/** Card and dice tables: a burgundy rail and gold, like a casino's table pit. */
	public static final CasinoTheme TABLE = new CasinoTheme(0xFF4A1418, 0xFF6E2630, 0xFF22080A, GOLD, 0xFF1C0A0C, 0xFF0C0405,
			0xFF3A1418, GOLD, 0xFFFFE07A, 0xFF6A4A2A, Style.FILIGREE);
	/** Baccarat: emerald and gold, the high-limit room. */
	public static final CasinoTheme EMERALD = new CasinoTheme(0xFF0E3A26, 0xFF1E5A3C, 0xFF061A10, GOLD, 0xFF08180F, 0xFF020A06,
			0xFF1A3A26, GOLD, 0xFFFFE07A, 0xFF4A5A2A, Style.FILIGREE);
	public static final CasinoTheme ARCADE = new CasinoTheme(0xFF0E1028, 0xFF2A2E5A, 0xFF04040E, 0xFF5AE0FF, 0xFF06061A,
			0xFF000004, 0xFF1A1A3A, 0xFF5AE0FF, 0xFFFF5AE0, 0xFF5AE0FF, Style.NEON);
	public static final CasinoTheme PLINKO = new CasinoTheme(0xFF1C1A3A, 0xFF34306A, 0xFF0A0A1E, GOLD, 0xFF100E24, 0xFF060510,
			0xFF2A2650, GOLD, 0xFFFFE07A, 0xFF4A3A6A, Style.BULBS);
	public static final CasinoTheme WHEEL = new CasinoTheme(0xFFB3261E, 0xFFE04A3A, 0xFF5A0E0A, 0xFFF4EEDC, 0xFF2A0A08,
			0xFF140402, 0xFF4A1410, 0xFFFFE07A, 0xFFFFE07A, 0xFF7A3A1A, Style.CARNIVAL);
	public static final CasinoTheme PUSHER = new CasinoTheme(0xFF5A1A6A, 0xFF8A3A9A, 0xFF2A0A34, GOLD, 0xFF1E0A24, 0xFF0C0410,
			0xFF3A1A44, 0xFFFFC0FF, 0xFFFFE070, 0xFF9A5AAA, Style.BULBS);
	/** The Derby Table: turf green with white rails and gold. */
	public static final CasinoTheme TURF = new CasinoTheme(0xFF1E4A22, 0xFF2E6A30, 0xFF0C2410, 0xFFF4EEDC, 0xFF0C1C0E, 0xFF040C06,
			0xFF1E3A22, GOLD, 0xFFFFE07A, 0xFF4A6A3A, Style.BULBS);
	public static final CasinoTheme LOTTERY = new CasinoTheme(0xFF141448, 0xFF2A2A7A, 0xFF06061E, GOLD, 0xFF0A0A2A, 0xFF040414,
			0xFF1E1E5A, 0xFFFFE070, 0xFFFFF4C0, 0xFF3A3A8A, Style.STARS);

	/** A slot machine's own colours, with its decoration. */
	public static CasinoTheme slot(int body, int bodyLight, int bodyDark, int trim, int bulbOn, int bulbOff, Style style) {
		return new CasinoTheme(body, bodyLight, bodyDark, trim, darker(bodyDark), 0xFF050505, bodyDark, bulbOn, bulbOn, bulbOff,
				style);
	}

	private static int darker(int c) {
		int r = ((c >> 16) & 0xFF) * 6 / 10;
		int g = ((c >> 8) & 0xFF) * 6 / 10;
		int b = (c & 0xFF) * 6 / 10;
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	/** The frame round the screen (left, top, right, bottom are the panel's edges), then its decoration. */
	public void drawFrame(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int ticks) {
		g.fill(left - 3, top - 3, right + 3, bottom + 3, dark);
		g.fill(left - 2, top - 2, right + 2, bottom + 2, accent);
		g.fill(left - 1, top - 1, right + 1, bottom + 1, dark);
		raised(g, left, top, right, bottom, face, light, dark);
		decorate(g, left, top, right, bottom, ticks);
	}

	private void decorate(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int ticks) {
		switch (style) {
			case FILIGREE -> {
				filigree(g, left - 2, top - 2, 1, 1);
				filigree(g, right + 1, top - 2, -1, 1);
				filigree(g, left - 2, bottom + 1, 1, -1);
				filigree(g, right + 1, bottom + 1, -1, -1);
			}
			case BULBS, CARNIVAL -> {
				if (style == Style.CARNIVAL) {
					// Candy stripes down the sides.
					for (int y = top + 2; y < bottom - 2; y += 4) {
						int c = ((y - top) / 4) % 2 == 0 ? accent : face;
						g.fill(left, y, left + 3, y + 2, c);
						g.fill(right - 3, y, right, y + 2, c);
					}
				}
				bulbs(g, left, top, right, bottom, ticks);
			}
			case NEON -> {
				// Two tubes, cyan and magenta, flickering now and then.
				boolean flicker = (ticks % 97) < 2;
				int a = flicker ? bulbOff : accent;
				int b = flicker ? accent : bulbOn;
				g.fill(left + 1, top + 1, right - 1, top + 2, a);
				g.fill(left + 1, bottom - 2, right - 1, bottom - 1, a);
				g.fill(left + 1, top + 1, left + 2, bottom - 1, b);
				g.fill(right - 2, top + 1, right - 1, bottom - 1, b);
			}
			case GEARS -> {
				gear(g, left - 1, top - 1, ticks, false);
				gear(g, right - 8, top - 1, ticks, true);
				gear(g, left - 1, bottom - 8, ticks, true);
				gear(g, right - 8, bottom - 8, ticks, false);
			}
			case LAVA -> {
				// Glowing cracks along the frame that brighten and dim.
				for (int i = 0; i < 40; i++) {
					int along = (i * 37) % Math.max(1, right - left);
					int glow = ((ticks + i * 7) / 6) % 3;
					int c = glow == 0 ? 0xFFFFC040 : glow == 1 ? accent : 0xFF8A2A10;
					g.fill(left + along, top + (i % 3), left + along + 2, top + (i % 3) + 1, c);
					g.fill(right - along - 2, bottom - 1 - (i % 3), right - along, bottom - (i % 3), c);
				}
			}
			case BUBBLES -> {
				// Bubbles rising up both sides.
				int height = Math.max(1, bottom - top - 8);
				for (int i = 0; i < 6; i++) {
					int y = bottom - 4 - Math.floorMod(ticks + i * height / 6, height);
					int x = (i % 2 == 0) ? left + 1 : right - 3;
					g.fill(x, y, x + 2, y + 2, 0xAAD8F0E8);
					g.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
				}
			}
			case PIXELS -> {
				// Blocky pixels in the frame, a few lit.
				for (int x = left; x < right - 2; x += 3) {
					int c = ((x - left) / 3 + ticks / 10) % 5 == 0 ? bulbOn : ((x - left) / 3) % 2 == 0 ? light : face;
					g.fill(x, top, x + 2, top + 2, c);
					g.fill(x, bottom - 2, x + 2, bottom, c);
				}
			}
			case CHROME -> {
				g.fill(left, top + 1, right, top + 2, 0xFFF4F6F8);
				g.fill(left, bottom - 2, right, bottom - 1, accent);
				rivet(g, left + 1, top + 1);
				rivet(g, right - 3, top + 1);
				rivet(g, left + 1, bottom - 3);
				rivet(g, right - 3, bottom - 3);
			}
			case STARS -> {
				for (int i = 0; i < 24; i++) {
					boolean twinkle = ((ticks / 4) + i * 3) % 7 == 0;
					int x = left + (i * 53) % Math.max(1, right - left);
					int y = i % 2 == 0 ? top + 1 : bottom - 2;
					g.fill(x, y, x + 1, y + 1, twinkle ? 0xFFFFFFFF : bulbOn);
					if (twinkle) {
						g.fill(x - 1, y, x + 2, y + 1, 0x88FFFFFF);
					}
				}
			}
			case WAVES -> {
				for (int x = left; x < right; x++) {
					int dy = (int) Math.round(Math.sin((x + ticks * 0.6) / 4.0));
					g.fill(x, top + 1 + dy, x + 1, top + 2 + dy, accent);
					g.fill(x, bottom - 2 - dy, x + 1, bottom - 1 - dy, accent);
				}
			}
			case SCALES -> {
				for (int x = left; x < right - 3; x += 4) {
					for (int k = 0; k < 2; k++) {
						int y = k == 0 ? top : bottom - 3;
						boolean shine = ((x - left) / 4 + ticks / 5) % 9 == 0;
						g.fill(x, y + 1, x + 3, y + 2, shine ? 0xFFFFF4C0 : accent);
						g.fill(x + 1, y + 2, x + 2, y + 3, accent);
					}
				}
				filigree(g, left - 2, top - 2, 1, 1);
				filigree(g, right + 1, top - 2, -1, 1);
			}
		}
	}

	/** Chasing bulbs round the outside of the frame. */
	private void bulbs(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int ticks) {
		int k = 0;
		for (int x = left + 2; x < right - 2; x += 6, k++) {
			int c = (k + ticks / 4) % 3 == 0 ? bulbOn : bulbOff;
			g.fill(x, top - 2, x + 2, top, c);
			g.fill(x, bottom, x + 2, bottom + 2, c);
		}
		for (int y = top + 4; y < bottom - 4; y += 6, k++) {
			int c = (k + ticks / 4) % 3 == 0 ? bulbOn : bulbOff;
			g.fill(left - 2, y, left, y + 2, c);
			g.fill(right, y, right + 2, y + 2, c);
		}
	}

	/** A small brass gear, 9 pixels across, turning one way or the other. */
	private void gear(GuiGraphicsExtractor g, int x, int y, int ticks, boolean reverse) {
		boolean step = ((ticks / 6) % 2 == 0) ^ reverse;
		g.fill(x + 2, y + 2, x + 7, y + 7, accent);
		g.fill(x + 3, y + 3, x + 6, y + 6, dark);
		g.fill(x + 4, y + 4, x + 5, y + 5, accent);
		if (step) {
			g.fill(x + 4, y, x + 5, y + 2, accent);
			g.fill(x + 4, y + 7, x + 5, y + 9, accent);
			g.fill(x, y + 4, x + 2, y + 5, accent);
			g.fill(x + 7, y + 4, x + 9, y + 5, accent);
		} else {
			g.fill(x + 1, y + 1, x + 3, y + 3, accent);
			g.fill(x + 6, y + 6, x + 8, y + 8, accent);
			g.fill(x + 6, y + 1, x + 8, y + 3, accent);
			g.fill(x + 1, y + 6, x + 3, y + 8, accent);
		}
	}
}
