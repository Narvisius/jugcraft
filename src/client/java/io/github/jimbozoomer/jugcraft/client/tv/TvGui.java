package io.github.jimbozoomer.jugcraft.client.tv;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The TV windows' look (TvScreen, TvFullScreen): the sci-fi screens' dark glass, cyan outlines and corner brackets,
 * amber for waiting, green for playing.
 */
public final class TvGui {
	public static final int GLASS = 0xF60E1620, HEADER = 0xFF0A121C, BOX = 0xEB0C131C, WELL = 0xFF080D14, EDGE = 0xFF223A4E, LINE = 0xFF283C50;
	public static final int CYAN = 0xFF5ADCF0, CYAN_DIM = 0xFF286E82, WHITE = 0xFFE6EEF6, GREY = 0xFF788CA0, DIM = 0xFF506070, AMBER = 0xFFFFB83C,
			GREEN = 0xFF64E68C, RED = 0xFFFF5046, SELECT = 0xFF162C3E;

	private TvGui() {
	}

	public static void brackets(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int colour, int n) {
		g.fill(x0, y0, x0 + n, y0 + 1, colour);
		g.fill(x0, y0, x0 + 1, y0 + n, colour);
		g.fill(x1 - n, y0, x1, y0 + 1, colour);
		g.fill(x1 - 1, y0, x1, y0 + n, colour);
		g.fill(x0, y1 - 1, x0 + n, y1, colour);
		g.fill(x0, y1 - n, x0 + 1, y1, colour);
		g.fill(x1 - n, y1 - 1, x1, y1, colour);
		g.fill(x1 - 1, y1 - n, x1, y1, colour);
	}

	/** The screen's window: glass, its header band with the title, scan lines, the corner brackets. */
	public static void window(GuiGraphicsExtractor g, Font font, int x0, int y0, int x1, int y1, String title, String sub) {
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xC8000000);
		g.fill(x0, y0, x1, y1, GLASS);
		g.outline(x0, y0, x1 - x0, y1 - y0, EDGE);
		g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 20, HEADER);
		g.fill(x0 + 1, y0 + 20, x1 - 1, y0 + 21, CYAN_DIM);
		for (int y = y0 + 22; y < y1 - 1; y += 2) {
			g.fill(x0 + 1, y, x1 - 1, y + 1, 0x05FFFFFF);
		}
		brackets(g, x0, y0, x1, y1, CYAN, 10);
		g.text(font, title, x0 + 9, y0 + 6, WHITE, false);
		g.text(font, sub, x0 + 13 + font.width(title), y0 + 6, GREY, false);
	}

	/** A box inside the window, its title in cyan and a note on the right. */
	public static void box(GuiGraphicsExtractor g, Font font, int x0, int y0, int x1, int y1, String title, String note) {
		g.fill(x0, y0, x1, y1, BOX);
		g.outline(x0, y0, x1 - x0, y1 - y0, 0xFF1E3244);
		brackets(g, x0, y0, x1, y1, CYAN_DIM, 4);
		if (!title.isEmpty()) {
			g.text(font, title, x0 + 5, y0 + 4, CYAN, false);
		}
		if (!note.isEmpty()) {
			g.text(font, note, x1 - 5 - font.width(note), y0 + 4, GREY, false);
		}
	}

	/** A chip in the header: outlined, its words in its colour. Returns its left edge (they are set out from the right). */
	public static int chip(GuiGraphicsExtractor g, Font font, int right, int y, String text, int colour) {
		int w = font.width(text) + 10;
		g.fill(right - w, y, right, y + 12, 0xFF101C28);
		g.outline(right - w, y, w, 12, colour);
		g.text(font, text, right - w + 5, y + 2, colour, false);
		return right - w;
	}

	/** A button: its outline lit when on or hovered, its word centred. */
	public static void button(GuiGraphicsExtractor g, Font font, int x0, int y0, int x1, int y1, String text, boolean on, boolean hover, boolean enabled) {
		g.fill(x0, y0, x1, y1, on ? SELECT : 0xFF0C141E);
		g.outline(x0, y0, x1 - x0, y1 - y0, on || hover && enabled ? CYAN : LINE);
		int colour = !enabled ? DIM : on || hover ? WHITE : GREY;
		g.text(font, text, (x0 + x1) / 2 - font.width(text) / 2, (y0 + y1) / 2 - 4, colour, false);
	}

	public static boolean in(double mx, double my, int x0, int y0, int x1, int y1) {
		return mx >= x0 && mx < x1 && my >= y0 && my < y1;
	}

	public static String fit(Font font, String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("."))) + ".";
	}
}
