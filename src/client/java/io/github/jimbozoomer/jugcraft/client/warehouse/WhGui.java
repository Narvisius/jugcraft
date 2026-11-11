package io.github.jimbozoomer.jugcraft.client.warehouse;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * The warehouse screens' look (the mockups, wh/guikit.py): dark glass, cyan outlines and corner brackets, amber for
 * waiting, green for done; ordinary item slots drawn as dark wells.
 */
public final class WhGui {
	public static final int GLASS = 0xF60E1620, HEADER = 0xFF0A121C, BOX = 0xEB0C131C, WELL = 0xFF080D14, EDGE = 0xFF223A4E, LINE = 0xFF283C50;
	public static final int CYAN = 0xFF5ADCF0, CYAN_DIM = 0xFF286E82, WHITE = 0xFFE6EEF6, GREY = 0xFF788CA0, DIM = 0xFF506070, AMBER = 0xFFFFB83C,
			GREEN = 0xFF64E68C, RED = 0xFFFF5046, SELECT = 0xFF162C3E;

	private WhGui() {
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

	public static void slotWell(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, WELL);
		g.outline(x - 1, y - 1, 18, 18, 0xFF243C50);
	}

	public static void bar(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, double fill, int colour) {
		g.fill(x0, y0, x1, y1, WELL);
		g.outline(x0, y0, x1 - x0, y1 - y0, CYAN_DIM);
		int w = (int) Math.round((x1 - x0 - 2) * Math.max(0, Math.min(1, fill)));
		if (w > 0) {
			g.fill(x0 + 1, y0 + 1, x0 + 1 + w, y1 - 1, colour);
		}
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

	/** An item drawn half size (a list's icon). */
	public static void smallItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.5F, 0.5F);
		g.item(stack, 0, 0);
		g.pose().popMatrix();
	}

	/**
	 * Part W4: the NFPA 704 fire diamond, its middle at (cx, cy), r its half height: health (blue, left), fire (red,
	 * top), instability (yellow, right), the special mark (white, bottom), each with its number.
	 */
	public static void diamond(GuiGraphicsExtractor g, Font font, int cx, int cy, int r, int health, int fire, int instability, String special) {
		int h = r / 2;
		for (int dy = -r; dy <= r; dy++) {
			int w = r - Math.abs(dy);
			for (int dx = -w; dx <= w; dx++) {
				int colour;
				if (Math.abs(dx) + Math.abs(dy + h) <= h) {
					colour = 0xFFD8282C;
				} else if (Math.abs(dx + h) + Math.abs(dy) <= h) {
					colour = 0xFF2C58C8;
				} else if (Math.abs(dx - h) + Math.abs(dy) <= h) {
					colour = 0xFFF0D020;
				} else if (Math.abs(dx) + Math.abs(dy - h) <= h) {
					colour = 0xFFF4F4F0;
				} else {
					colour = 0xFF141414;
				}
				g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, colour);
			}
		}
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(0.5F, 0.5F);
		String[] nums = {String.valueOf(fire), String.valueOf(health), String.valueOf(instability), special};
		int[][] at = {{0, -h * 2}, {-h * 2, 0}, {h * 2, 0}, {0, h * 2}};
		for (int k = 0; k < 4; k++) {
			if (!nums[k].isEmpty()) {
				g.text(font, nums[k], at[k][0] - font.width(nums[k]) / 2, at[k][1] - 4, k == 1 ? 0xFFFFFFFF : 0xFF101010, false);
			}
		}
		g.pose().popMatrix();
	}

	/** Part W4: a stock line's or order's name (a liquid's for a book drum), its amount ("80 B" for a liquid), both. */
	static String name(ItemStack stack) {
		return io.github.jimbozoomer.jugcraft.warehouse.Drums.name(stack).getString();
	}

	static String amount(ItemStack stack, long n) {
		return io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(stack) ? io.github.jimbozoomer.jugcraft.warehouse.Drums.buckets(n)
				: String.format(java.util.Locale.ROOT, "%,d", n);
	}

	static String what(ItemStack stack, long n) {
		return io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(stack) ? name(stack) + " " + amount(stack, n)
				: name(stack) + String.format(java.util.Locale.ROOT, " x%,d", n);
	}

	/** The icon for a stock line: a liquid's bucket (its drum when it has none), else the item. */
	static ItemStack icon(ItemStack stack) {
		net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.liquid(stack);
		if (liquid != null) {
			net.minecraft.world.item.Item bucket = liquid.getFluid().getBucket();
			return bucket != null && bucket != net.minecraft.world.item.Items.AIR ? new ItemStack(bucket) : stack;
		}
		return stack;
	}

	public static String fit(Font font, String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("."))) + ".";
	}
}
