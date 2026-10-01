package io.github.jimbozoomer.jugcraft.client.blueprint;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The red-and-graphite look shared by the Drone Tower and Blueprint screens. */
final class SciFi {
	static final int FRAME = 0xFF3A2422;
	static final int GLASS = 0xF0141012;
	static final int PANEL = 0xFF1E1A1C;
	static final int ACCENT = 0xFFE05040;
	static final int DIM = 0xFF96463C;
	static final int WHITE = 0xFFECE6E6;
	static final int AMBER = 0xFFFFC04A;
	static final int GOOD = 0xFF8CDC8C;
	static final int SELECTED = 0xFF461C18;

	private SciFi() {
	}

	static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, FRAME);
		g.fill(x0, y0, x1, y1, GLASS);
		for (int[] c : new int[][] {{x0, y0}, {x1 - 10, y0}, {x0, y1 - 2}, {x1 - 10, y1 - 2}}) {
			g.fill(c[0], c[1], c[0] + 10, c[1] + 2, ACCENT);
		}
	}

	static String fit(Font font, String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))) + "...";
	}
}
