package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * The parts the other machine screens are drawn with (docs/features/machine-screens.md): the prospector, rocket pad,
 * storage tank, logic controller, reactor console, remote shutdown panel, drone depot terminal and drone tower. Each
 * takes the look of its tech level ({@link MachineScreenThemes#screen}), so they match the machine screens: the same
 * casing as a body of any size, the same panes, a status lamp by the title, a status chip, bars, a help strip and
 * buttons in the look's own keys. Shapes follow tools/gui_textures.py's _casing and _pane.
 */
public final class ScreenKit {
	/** The patterned casings, tiled over the body (tools/gui_textures.py body_tile). */
	private static final Identifier WORKSHOP_TILE = Jugcraft.id("textures/gui/screen_workshop.png");
	private static final Identifier INDUSTRIAL_TILE = Jugcraft.id("textures/gui/screen_industrial.png");
	private static final int TILE = 96;
	/** Where the title strip puts the lamp and the title, from the body's corner; panes start under it. */
	public static final int TITLE_HEIGHT = 13;

	private ScreenKit() {
	}

	// ------------------------------------------------------------------ bodies and panes

	/** The body: the look's casing over (x, y, w, h), with its edges. */
	public static void body(GuiGraphicsExtractor g, MachineScreenThemes.Theme theme, int x, int y, int w, int h) {
		int[] c = theme.frame().casing();
		switch (theme.shape()) {
			case "workshop" -> {
				tile(g, WORKSHOP_TILE, x + 1, y + 1, w - 2, h - 2);
				bevel(g, x, y, w, h, c[4], c[0]);
				g.outline(x, y, w, h, theme.frame().outline());
				// A rivet in each corner.
				for (int[] r : new int[][] {{x + 2, y + 2}, {x + w - 4, y + 2}, {x + 2, y + h - 4}, {x + w - 4, y + h - 4}}) {
					g.fill(r[0], r[1], r[0] + 2, r[1] + 2, c[0]);
					g.fill(r[0], r[1], r[0] + 1, r[1] + 1, c[4]);
				}
			}
			case "industrial" -> {
				tile(g, INDUSTRIAL_TILE, x + 1, y + 1, w - 2, h - 2);
				bevel(g, x + 1, y + 1, w - 2, h - 2, c[4], c[1]);
				g.outline(x, y, w, h, theme.frame().outline());
			}
			case "electric" -> {
				g.fill(x, y, x + w, y + h, c[2]);
				bevel(g, x + 1, y + 1, w - 2, h - 2, c[4], c[0]);
				bevel(g, x + 2, y + 2, w - 4, h - 4, c[3], c[1]);
				g.outline(x, y, w, h, theme.frame().outline());
				for (int vy = y + 14; vy < y + h - 6; vy += 5) {
					g.fill(x + w - 4, vy, x + w - 2, vy + 2, c[0]);
				}
			}
			case "lab" -> {
				g.fill(x, y, x + w, y + h, theme.frame().screen());
				grid(g, x + 1, y + 1, w - 2, h - 2, 8, theme.frame().grid());
				bevel(g, x, y, w, h, c[4], c[1]);
				bevel(g, x + 1, y + 1, w - 2, h - 2, c[3], c[2]);
				g.outline(x + 2, y + 2, w - 4, h - 4, c[0]);
			}
			default -> {
				// advanced, and the drone screens: a dark scanned panel in a frame with two corners cut.
				chamfer(g, x, y, w, h, 6, c[1], 0);
				for (int row = y + 1; row < y + h - 1; row += 3) {
					// Each line stops short of the cut corners.
					int left = x + 1 + Math.max(0, 6 - (row - y));
					int right = x + w - 1 - Math.max(0, 6 - (y + h - 1 - row));
					if (right > left) {
						g.fill(left, row, right, row + 1, c[2]);
					}
				}
				chamfer(g, x, y, w, h, 6, 0, theme.frame().rim());
				chamfer(g, x + 2, y + 2, w - 4, h - 4, 5, 0, c[4]);
			}
		}
	}

	/** A display pane over (x, y, w, h): the look's glass with its grid and rim. */
	public static void pane(GuiGraphicsExtractor g, MachineScreenThemes.Theme theme, int x, int y, int w, int h) {
		MachineScreenThemes.Frame f = theme.frame();
		int[] c = f.casing();
		switch (theme.shape()) {
			case "workshop" -> {
				g.fill(x - 1, y - 1, x + w + 1, y + h + 1, f.outline());
				g.fill(x, y, x + w, y + h, f.screen());
				grid(g, x + 1, y + 1, w - 2, h - 2, 5, f.grid());
				g.outline(x, y, w, h, f.rim());
			}
			case "industrial" -> {
				bevel(g, x - 1, y - 1, w + 2, h + 2, c[0], c[4]);
				g.fill(x, y, x + w, y + h, f.screen());
				g.outline(x, y, w, h, f.rim());
			}
			case "electric" -> {
				g.fill(x - 1, y - 1, x + w + 1, y + h + 1, c[0]);
				g.fill(x, y, x + w, y + h, f.screen());
				grid(g, x + 1, y + 1, w - 2, h - 2, 6, f.grid());
				g.outline(x, y, w, h, f.rim());
			}
			case "lab" -> {
				g.fill(x, y, x + w, y + h, f.screen());
				grid(g, x + 1, y + 1, w - 2, h - 2, 8, f.grid());
				g.outline(x, y, w, h, f.rim());
			}
			default -> chamfer(g, x, y, w, h, 4, f.screen(), f.rim());
		}
	}

	/** A pane with a heading along its top, a rule under it; returns the y under the rule. */
	public static int pane(GuiGraphicsExtractor g, Font font, MachineScreenThemes.Theme theme, int x, int y, int w, int h, String heading) {
		pane(g, theme, x, y, w, h);
		g.text(font, fit(font, heading, w - 12), x + 6, y + 4, theme.text(), false);
		g.fill(x + 5, y + 14, x + w - 5, y + 15, (theme.dim() & 0x00FFFFFF) | 0x90000000);
		return y + 18;
	}

	/** Where the player's inventory sits: a tray in the casing's darker tones. */
	public static void tray(GuiGraphicsExtractor g, MachineScreenThemes.Theme theme, int x, int y, int w, int h) {
		int[] c = theme.frame().casing();
		if (theme.outlined()) {
			if ("lab".equals(theme.shape())) {
				g.fill(x, y, x + w, y + h, theme.frame().screen());
				g.outline(x, y, w, h, theme.frame().rim());
			} else {
				chamfer(g, x, y, w, h, 5, theme.frame().screen(), theme.slotDark());
			}
			return;
		}
		g.fill(x, y, x + w, y + h, c[1]);
		bevel(g, x, y, w, h, c[3], c[0]);
		g.outline(x, y, w, h, theme.frame().outline());
	}

	/** The player's inventory slots, with the menu's usual top-left corner at (x, y) (8, 84 in a 166-high screen). */
	public static void inventory(GuiGraphicsExtractor g, MachineScreenThemes.Theme theme, int x, int y) {
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				MachineSkin.slot(g, theme, x + col * 18, y + row * 18);
			}
		}
		for (int col = 0; col < 9; col++) {
			MachineSkin.slot(g, theme, x + col * 18, y + 58);
		}
	}

	// ------------------------------------------------------------------ the title strip and readouts

	/** The status lamp and the title along the body's top, from its corner at (x, y). */
	public static void title(GuiGraphicsExtractor g, Font font, MachineScreenThemes.Theme theme, int x, int y, Component title, int lamp,
			int width) {
		MachineSkin.lamp(g, x + 4, y + 3, lamp);
		String text = fit(font, title.getString(), width - 16);
		g.text(font, text, x + 12, y + 2, theme.label(), false);
	}

	/** A small status chip with its right edge at {@code right}, in the title strip at y: the state in a word. */
	public static void chip(GuiGraphicsExtractor g, Font font, MachineScreenThemes.Theme theme, int right, int y, String text, int color) {
		int w = font.width(text) + 8;
		int x = right - w;
		if (theme.outlined()) {
			chamfer(g, x, y, w, 11, 2, theme.frame().screen(), theme.frame().rim());
		} else {
			g.fill(x, y, x + w, y + 11, theme.frame().screen());
			g.outline(x, y, w, 11, theme.frame().rim());
		}
		g.text(font, text, x + 4, y + 2, color, false);
	}

	/** A bar over (x, y, w, h) lit to {@code fraction} in segments; the unlit part a faint copy of the colour. */
	public static void bar(GuiGraphicsExtractor g, MachineScreenThemes.Theme theme, int x, int y, int w, int h, double fraction, int color) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, theme.slotDark());
		g.fill(x, y, x + w, y + h, theme.slotFace());
		int lit = (int) Math.round(Math.max(0, Math.min(1, fraction)) * w);
		int unlit = (color & 0x00FFFFFF) | 0x30000000;
		for (int sx = x + 1; sx < x + w - 1; sx += 4) {
			int end = Math.min(sx + 3, x + w - 1);
			g.fill(sx, y + 1, end, y + h - 1, sx - x < lit ? color : unlit);
		}
	}

	/** A label on the left and its value on the right of a row {@code w} wide; the label is cut short to leave room. */
	public static void row(GuiGraphicsExtractor g, Font font, MachineScreenThemes.Theme theme, int x, int y, int w, String label, String value,
			int color) {
		String shown = fit(font, value, w);
		g.text(font, fit(font, label, w - font.width(shown) - 4), x, y, theme.dim(), false);
		g.text(font, shown, x + w - font.width(shown), y, color, false);
	}

	/** A help strip over (x, y, w, h): the look's help box with its text wrapped inside. */
	public static void help(GuiGraphicsExtractor g, Font font, MachineScreenThemes.Theme theme, int x, int y, int w, int h, Component text) {
		if (theme.outlined() && !"lab".equals(theme.shape())) {
			chamfer(g, x, y, w, h, 3, theme.frame().helpBox(), theme.frame().rim());
		} else {
			g.fill(x, y, x + w, y + h, theme.frame().helpBox());
			g.outline(x, y, w, h, "workshop".equals(theme.shape()) ? theme.slotLight() : theme.frame().rim());
		}
		int line = y + (h - Math.min(h - 4, font.split(text, w - 10).size() * 9)) / 2;
		for (FormattedCharSequence part : font.split(text, w - 10)) {
			if (line + 9 > y + h - 1) {
				break;
			}
			g.text(font, part, x + 5, line, theme.help(), false);
			line += 9;
		}
	}

	// ------------------------------------------------------------------ drawing helpers

	/** A raised edge: light along the top and left, dark along the bottom and right. */
	public static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int light, int dark) {
		g.fill(x, y, x + w, y + 1, light);
		g.fill(x, y, x + 1, y + h, light);
		g.fill(x, y + h - 1, x + w, y + h, dark);
		g.fill(x + w - 1, y, x + w, y + h, dark);
	}

	/** Grid lines every {@code step} pixels over (x, y, w, h). */
	public static void grid(GuiGraphicsExtractor g, int x, int y, int w, int h, int step, int color) {
		for (int gx = x; gx < x + w; gx += step) {
			g.fill(gx, y, gx + 1, y + h, color);
		}
		for (int gy = y; gy < y + h; gy += step) {
			g.fill(x, gy, x + w, gy + 1, color);
		}
	}

	/**
	 * A hologram box with its top-left and bottom-right corners cut by {@code cut} pixels: filled with {@code fill}
	 * and edged with {@code edge} (either 0 to leave it out).
	 */
	public static void chamfer(GuiGraphicsExtractor g, int x, int y, int w, int h, int cut, int fill, int edge) {
		if (fill != 0) {
			for (int r = 0; r < cut && r < h; r++) {
				g.fill(x + cut - r, y + r, x + w, y + r + 1, fill);
			}
			if (h > 2 * cut) {
				g.fill(x, y + cut, x + w, y + h - cut, fill);
			}
			for (int r = 0; r < cut && r < h; r++) {
				int row = y + h - 1 - r;
				g.fill(x, row, x + w - (cut - r), row + 1, fill);
			}
		}
		if (edge != 0) {
			g.fill(x + cut, y, x + w, y + 1, edge);
			g.fill(x, y + cut, x + 1, y + h, edge);
			g.fill(x, y + h - 1, x + w - cut, y + h, edge);
			g.fill(x + w - 1, y, x + w, y + h - cut, edge);
			for (int i = 1; i < cut; i++) {
				g.fill(x + cut - i, y + i, x + cut - i + 1, y + i + 1, edge);
				g.fill(x + w - 1 - i, y + h - 1 - cut + i, x + w - i, y + h - cut + i, edge);
			}
		}
	}

	/** The texture (a {@value #TILE}-pixel tile) repeated over (x, y, w, h). */
	private static void tile(GuiGraphicsExtractor g, Identifier texture, int x, int y, int w, int h) {
		for (int ty = 0; ty < h; ty += TILE) {
			for (int tx = 0; tx < w; tx += TILE) {
				g.blit(RenderPipelines.GUI_TEXTURED, texture, x + tx, y + ty, 0.0F, 0.0F, Math.min(TILE, w - tx), Math.min(TILE, h - ty), TILE,
						TILE);
			}
		}
	}

	/** {@code text} cut to fit {@code width} pixels, ending in an ellipsis if it was cut. */
	public static String fit(Font font, String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
	}

	/** A colour faded towards black by {@code amount} (0 to 255). */
	static int darken(int color, int amount) {
		int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
		int gr = Math.max(0, ((color >> 8) & 0xFF) - amount);
		int b = Math.max(0, (color & 0xFF) - amount);
		return (color & 0xFF000000) | (r << 16) | (gr << 8) | b;
	}

	// ------------------------------------------------------------------ buttons

	/**
	 * A text key in the screen's look: a brass-rimmed key, a green key, a grey-blue key, a lab tab or a hologram key
	 * with two corners cut. A selected key (a tab that is open, a switch that is on) is lit; a key that cannot be
	 * pressed is dimmed. Its label keeps its own colour if it has one (a channel's name in the channel's colour).
	 */
	public static final class TextButton extends Button {
		private final MachineScreenThemes.Theme theme;
		private boolean selected;

		public TextButton(int x, int y, int w, int h, MachineScreenThemes.Theme theme, Component label, OnPress onPress) {
			super(x, y, w, h, label, onPress, Button.DEFAULT_NARRATION);
			this.theme = theme;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			int x = getX();
			int y = getY();
			int w = width;
			int h = height;
			boolean hover = active && (isHovered() || isFocused());
			int face = theme.buttonFace();
			if (selected) {
				face = MachineSkin.lighten(face, 40);
			} else if (hover) {
				face = MachineSkin.lighten(face, 24);
			} else if (!active) {
				face = darken(face, 20);
			}
			if (theme.outlined() && !"lab".equals(theme.shape())) {
				int edge = selected || hover ? theme.buttonLight() : active ? theme.buttonEdge() : theme.dim();
				chamfer(g, x, y, w, h, Math.min(3, h / 4), face, edge);
			} else {
				int rim = "lab".equals(theme.shape()) ? (hover || selected ? 0xFFF0F0F0 : 0xFF141414) : theme.buttonEdge();
				g.fill(x, y, x + w, y + h, rim);
				g.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
				g.fill(x + 1, y + 1, x + w - 1, y + 2, "lab".equals(theme.shape()) ? MachineSkin.lighten(face, 60) : theme.buttonLight());
			}
			if (selected) {
				g.fill(x + 3, y + h - 3, x + w - 3, y + h - 2, theme.ok());
			}
			int color = !active ? theme.dim() : "lab".equals(theme.shape()) ? 0xFFF0F0F0 : theme.buttonIcon();
			String text = fit(font, getMessage().getString(), w - 4);
			Component shown = text.equals(getMessage().getString()) ? getMessage() : Component.literal(text).setStyle(getMessage().getStyle());
			g.text(font, shown, x + (w - font.width(text)) / 2, y + (h - 8) / 2, color, false);
		}
	}
}
