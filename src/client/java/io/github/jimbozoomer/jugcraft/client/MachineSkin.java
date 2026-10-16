package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * What the machine screens draw over their look's background (docs/features/machine-screens.md): slots, the energy
 * bar, the progress arrow, the status lamp, the pixel icons and the themed buttons, in the colours of the machine's
 * {@link MachineScreenThemes.Theme look}. Shared by {@link MachineScreen} and {@link FormMachineScreen}, which lay out
 * the same parts: the title strip with its lamp, the machine pane, the status pane, the button row and the help box.
 */
public final class MachineSkin {
	/** The title strip: the status lamp, then the machine's name. */
	public static final int LAMP_X = 4;
	public static final int LAMP_Y = 3;
	public static final int TITLE_X = 12;
	public static final int TITLE_Y = 2;
	/** The status pane's text area. */
	public static final int STATUS_X = 183;
	public static final int STATUS_Y = 15;
	public static final int STATUS_WIDTH = 78;
	public static final int STATUS_BOTTOM = 74;
	/** The row of icon buttons along the top of the help box. */
	public static final int BUTTON_X = 182;
	public static final int BUTTON_Y = 82;
	public static final int BUTTON_SIZE = 18;
	public static final int BUTTON_STEP = 21;
	/** The help box's text area: under the buttons, or from the top of the box on a screen without them. */
	public static final int HELP_X = 182;
	public static final int HELP_Y = 104;
	public static final int HELP_TOP = 83;
	public static final int HELP_WIDTH = 81;
	public static final int HELP_BOTTOM = 162;

	private MachineSkin() {
	}

	/** Eight-by-eight pixel icons for the buttons, the slot hints and the status pane. */
	public enum Icon {
		REDSTONE("...##...", "..####..", "...##...", "...##...", "...##...", "...##...", "..####..", ".######."),
		EJECT("...#....", "..###...", ".#.#.#..", "...#....", "#..#...#", "#......#", "#......#", "########"),
		SIDES("..####..", ".#...##.", "######.#", "#....#.#", "#....#.#", "#....#.#", "#....##.", "######.."),
		PAUSE("........", ".##..##.", ".##..##.", ".##..##.", ".##..##.", ".##..##.", ".##..##.", "........"),
		PLAY("........", ".#......", ".###....", ".#####..", ".######.", ".#####..", ".###....", ".#......"),
		CANCEL("........", ".#....#.", "..#..#..", "...##...", "...##...", "..#..#..", ".#....#.", "........"),
		VENT("..#..#..", ".#..#...", "..#..#..", ".#..#...", "........", "########", "#......#", "########"),
		BACK("........", "...#....", "..#.....", ".######.", "..#.....", "...#....", "........", "........"),
		BOLT("....##..", "...##...", "..##....", ".######.", "....##..", "...##...", "..##....", ".#......"),
		UPGRADE("...##...", "..####..", ".##..##.", "#......#", "...##...", "..####..", ".##..##.", "#......#"),
		FLAME("...#....", "..##....", "..###...", ".####.#.", ".######.", "########", ".######.", "..####.."),
		RECIPES("########", "#......#", "#.####.#", "#......#", "#.####.#", "#......#", "#.###..#", "########"),
		DRAIN("...#....", "...#....", "..###...", "..###...", ".#####..", ".#####..", ".#####..", "..###..."),
		ROCKET("...##...", "..####..", "..#..#..", "..####..", "..####..", ".######.", ".##..##.", ".#....#."),
		PLAN("#######.", "#.....#.", "#.###.#.", "#.....#.", "#.##..#.", "#.....##", "#......#", "########");

		private final String[] rows;

		Icon(String... rows) {
			this.rows = rows;
		}
	}

	/** An icon with its top-left corner at (x, y). */
	public static void icon(GuiGraphicsExtractor graphics, int x, int y, Icon icon, int color) {
		for (int row = 0; row < 8; row++) {
			String line = icon.rows[row];
			int run = -1;
			for (int col = 0; col <= 8; col++) {
				boolean on = col < 8 && line.charAt(col) == '#';
				if (on && run < 0) {
					run = col;
				} else if (!on && run >= 0) {
					graphics.fill(x + run, y + row, x + col, y + row + 1, color);
					run = -1;
				}
			}
		}
	}

	/** A slot for an item at (x, y): sunken in the casing looks, outlined in the screen-like ones. */
	public static void slot(GuiGraphicsExtractor graphics, MachineScreenThemes.Theme theme, int x, int y) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, theme.slotDark());
		graphics.fill(x, y, x + 17, y + 17, theme.slotLight());
		graphics.fill(x, y, x + 16, y + 16, theme.slotFace());
	}

	/** A faint icon in an empty slot: what goes there. */
	public static void hint(GuiGraphicsExtractor graphics, MachineScreenThemes.Theme theme, int x, int y, Icon icon) {
		icon(graphics, x + 4, y + 4, icon, (theme.dim() & 0x00FFFFFF) | 0x70000000);
	}

	/** The status lamp: all's-well, idle or a warning. */
	public static void lamp(GuiGraphicsExtractor graphics, int x, int y, int color) {
		graphics.fill(x, y, x + 5, y + 5, 0xFF000000 | (color & 0x00FFFFFF) & 0xFF7F7F7F);
		graphics.fill(x + 1, y + 1, x + 4, y + 4, color);
		graphics.fill(x + 1, y + 1, x + 2, y + 2, 0xFFFFFFFF);
	}

	/** The energy bar: a column of lit segments, filled from the bottom. */
	public static void energyBar(GuiGraphicsExtractor graphics, MachineScreenThemes.Theme theme, int left, int top, int width, int height,
			long energy, long capacity) {
		graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, theme.slotDark());
		graphics.fill(left, top, left + width, top + height, theme.slotFace());
		int filled = (int) (Math.max(0, energy) * height / Math.max(1, capacity));
		int unlit = (theme.energy() & 0x00FFFFFF) | 0x30000000;
		for (int segment = 0; segment * 4 < height; segment++) {
			int bottom = top + height - segment * 4;
			graphics.fill(left + 1, bottom - 3, left + width - 1, bottom, segment * 4 < filled ? theme.energy() : unlit);
		}
	}

	/**
	 * The big charge gauge filling an otherwise empty machine pane (batteries, solar panels): a battery lying on its
	 * side, twenty cells lit from the left as it fills.
	 */
	public static void charge(GuiGraphicsExtractor graphics, MachineScreenThemes.Theme theme, int left, int top, long energy,
			long capacity) {
		int width = 122;
		int height = 26;
		graphics.fill(left - 1, top - 1, left + width + 1, top + height + 1, theme.slotDark());
		graphics.fill(left, top, left + width, top + height, theme.slotFace());
		graphics.fill(left + width + 1, top + 8, left + width + 5, top + height - 8, theme.slotDark());
		graphics.fill(left + width + 1, top + 9, left + width + 4, top + height - 9, theme.slotLight());
		int cells = 20;
		int lit = capacity <= 0 || energy <= 0 ? 0 : (int) Math.max(1, Math.min(cells, energy * cells / capacity));
		int unlit = (theme.energy() & 0x00FFFFFF) | 0x30000000;
		for (int cell = 0; cell < cells; cell++) {
			int x = left + 2 + cell * 6;
			graphics.fill(x, top + 3, x + 5, top + height - 3, cell < lit ? theme.energy() : unlit);
		}
	}

	/** The progress arrow: a shaft and a head pointing right, filled from the left as the work goes on. */
	public static void arrow(GuiGraphicsExtractor graphics, MachineScreenThemes.Theme theme, int left, int middle, float fraction) {
		int done = (int) (Math.max(0, Math.min(1, fraction)) * 24);
		for (int column = 0; column < 24; column++) {
			int half = column < 18 ? 2 : 6 - (column - 18);
			graphics.fill(left + column, middle - half, left + column + 1, middle + half, column < done ? theme.progress() : theme.slotDark());
		}
	}

	/** A one-line meter in the status pane: ten cells and a label on the right. */
	public static void meter(GuiGraphicsExtractor graphics, Font font, MachineScreenThemes.Theme theme, int y, int percent, String label) {
		int cells = 10;
		int cell = Math.max(2, (STATUS_WIDTH - font.width(label) - 4) / cells);
		int lit = Math.max(0, Math.min(cells, percent * cells / 100));
		for (int i = 0; i < cells; i++) {
			int left = STATUS_X + i * cell;
			graphics.fill(left, y + 1, left + cell - 1, y + 7, i < lit ? theme.progress() : (theme.progress() & 0x00FFFFFF) | 0x30000000);
		}
		graphics.text(font, label, STATUS_X + STATUS_WIDTH - font.width(label), y, theme.text(), false);
	}

	/** Text wrapped to a width from (x, y), stopping before {@code bottom}; returns the y under the last line drawn. */
	public static int wrap(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int bottom, int color) {
		for (FormattedCharSequence line : font.split(text, width)) {
			if (y + 9 > bottom) {
				break;
			}
			graphics.text(font, line, x, y, color, false);
			y += 9;
		}
		return y;
	}

	/** 950, 12.5k, 400k, 64M: short enough for the status pane. */
	public static String compact(long value) {
		if (value < 1_000) {
			return Long.toString(value);
		}
		if (value < 100_000) {
			long tenths = value / 100;
			return tenths % 10 == 0 ? tenths / 10 + "k" : tenths / 10 + "." + tenths % 10 + "k";
		}
		if (value < 10_000_000) {
			return value / 1_000 + "k";
		}
		return value / 1_000_000 + "M";
	}

	/** 12k and 64M without tenths, for when even the compact form is too wide. */
	public static String thousands(long value) {
		if (value < 1_000) {
			return Long.toString(value);
		}
		return value < 10_000_000 ? value / 1_000 + "k" : value / 1_000_000 + "M";
	}

	/** "12s", "3m 05s": how long the work has left. */
	public static String duration(int seconds) {
		return seconds < 60 ? seconds + "s" : seconds / 60 + "m " + String.format("%02d", seconds % 60) + "s";
	}

	/** A colour lightened towards white by {@code amount} (0 to 255). */
	static int lighten(int color, int amount) {
		int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
		int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
		int b = Math.min(255, (color & 0xFF) + amount);
		return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	/**
	 * A square icon button in the machine's look: a brass-rimmed key, a green key, a grey-blue key, a coloured tab (lab,
	 * where each control keeps its own colour) or a cut-corner hologram key. A small lit dot shows a control that is on.
	 */
	public static final class IconButton extends Button {
		private final MachineScreenThemes.Theme theme;
		/** The lab look's tab colour for this control, or 0 for the look's own button colour. */
		private final int tab;
		private Icon icon;
		private int iconColor;
		private boolean lit;

		public IconButton(int x, int y, MachineScreenThemes.Theme theme, Icon icon, int tab, Component name, OnPress onPress) {
			super(x, y, BUTTON_SIZE, BUTTON_SIZE, name, onPress, Button.DEFAULT_NARRATION);
			this.theme = theme;
			this.icon = icon;
			this.tab = tab;
		}

		public void setIcon(Icon icon) {
			this.icon = icon;
		}

		/** The icon's colour, or 0 for the look's own. */
		public void setIconColor(int color) {
			this.iconColor = color;
		}

		public void setLit(boolean lit) {
			this.lit = lit;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			int x = getX();
			int y = getY();
			int w = width;
			int h = height;
			boolean hover = isHovered() || isFocused();
			int face = "lab".equals(theme.look()) && tab != 0 ? tab : theme.buttonFace();
			if (hover) {
				face = lighten(face, 28);
			}
			if ("advanced".equals(theme.look())) {
				// A hologram key: a dark face inside a frame with its top-left and bottom-right corners cut.
				graphics.fill(x + 3, y + 1, x + w - 1, y + 3, face);
				graphics.fill(x + 2, y + 2, x + 3, y + 3, face);
				graphics.fill(x + 1, y + 3, x + w - 1, y + h - 3, face);
				graphics.fill(x + 1, y + h - 3, x + w - 3, y + h - 1, face);
				graphics.fill(x + w - 3, y + h - 3, x + w - 2, y + h - 2, face);
				int edge = theme.buttonEdge();
				graphics.fill(x + 3, y, x + w, y + 1, edge);
				graphics.fill(x, y + 3, x + 1, y + h, edge);
				graphics.fill(x, y + h - 1, x + w - 3, y + h, edge);
				graphics.fill(x + w - 1, y, x + w, y + h - 3, edge);
				graphics.fill(x + 1, y + 2, x + 2, y + 3, edge);
				graphics.fill(x + 2, y + 1, x + 3, y + 2, edge);
				graphics.fill(x + w - 2, y + h - 3, x + w - 1, y + h - 2, edge);
				graphics.fill(x + w - 3, y + h - 2, x + w - 2, y + h - 1, edge);
			} else {
				int rim = "lab".equals(theme.look()) ? (hover || lit ? 0xFFF0F0F0 : 0xFF141414) : theme.buttonEdge();
				graphics.fill(x, y, x + w, y + h, rim);
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
				graphics.fill(x + 1, y + 1, x + w - 1, y + 2, "lab".equals(theme.look()) ? lighten(face, 60) : theme.buttonLight());
			}
			int color = iconColor != 0 ? iconColor : "lab".equals(theme.look()) ? 0xFFF0F0F0 : theme.buttonIcon();
			icon(graphics, x + (w - 8) / 2, y + (h - 8) / 2 + 1, icon, color);
			if (lit) {
				graphics.fill(x + w - 4, y + 2, x + w - 2, y + 4, theme.ok());
			}
		}
	}

	/** One face in the sides view: its letter on its mode's colour. Clicking it cycles what the face does. */
	public static final class FaceButton extends Button {
		public static final int SIZE = 11;
		private final String letter;
		private int color = 0xFF9A9A9A;

		public FaceButton(int x, int y, String letter, Component name, OnPress onPress) {
			super(x, y, SIZE, SIZE, name, onPress, Button.DEFAULT_NARRATION);
			this.letter = letter;
		}

		public void setColor(int color) {
			this.color = color;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			int x = getX();
			int y = getY();
			boolean hover = isHovered() || isFocused();
			graphics.fill(x, y, x + width, y + height, hover ? 0xFFF0F0F0 : 0xFF101010);
			graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, hover ? lighten(color, 30) : color);
			graphics.text(font, letter, x + (width - font.width(letter)) / 2 + 1, y + 2, 0xFF101010, false);
		}
	}
}
