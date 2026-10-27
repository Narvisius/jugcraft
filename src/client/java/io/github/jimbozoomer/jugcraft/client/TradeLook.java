package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * The trade look (docs/features/trade-look.md): the Exchange's walnut and brass, for every screen where Jugs or goods
 * change hands: the Exchange and its market chest, the town shops, the Jug Teller and the personal safe and shops. A
 * walnut panel in a brass rim with gold filigree at the corners, slate trays for slots and ledgers, gold and cream
 * lettering, and brass and walnut plates for keys ({@link ExchangeScreen.Plate}).
 */
public final class TradeLook {
	/** Headings and sums. */
	public static final int GOLD = ExchangeScreen.GOLD;
	/** Text on a slate tray. */
	public static final int CREAM = ExchangeScreen.CREAM;
	/** Text on the walnut. */
	public static final int TAN = ExchangeScreen.TAN;
	/** Quieter text: hints, empty states. */
	public static final int DIM = ExchangeScreen.DIM;
	/** Money coming in, a trade that can go ahead. */
	public static final int GAIN = ExchangeScreen.GAIN;
	/** Something in the way. */
	public static final int LOSS = ExchangeScreen.LOSS;
	/** A selected ledger line's wash. */
	public static final int SELECTED = 0x55B8913F;

	private TradeLook() {
	}

	/** The walnut panel over (x0, y0)-(x1, y1), in its brass rim (3 pixels outside it), with filigree at the corners. */
	public static void frame(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, ExchangeScreen.WALNUT_DARK);
		g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, ExchangeScreen.BRASS);
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, ExchangeScreen.WALNUT_DARK);
		ExchangeScreen.raised(g, x0, y0, x1, y1, ExchangeScreen.WALNUT, ExchangeScreen.WALNUT_LIGHT, ExchangeScreen.WALNUT_DARK);
		ExchangeScreen.filigree(g, x0 - 2, y0 - 2, 1, 1);
		ExchangeScreen.filigree(g, x1 + 1, y0 - 2, -1, 1);
		ExchangeScreen.filigree(g, x0 - 2, y1 + 1, 1, -1);
		ExchangeScreen.filigree(g, x1 + 1, y1 + 1, -1, -1);
	}

	/** A sunken slate tray: a slot bay, a ledger page, a teller's display. */
	public static void tray(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		ExchangeScreen.sunken(g, x0, y0, x1, y1, ExchangeScreen.SLATE, ExchangeScreen.SLATE_DARK, ExchangeScreen.SLATE_LIGHT);
	}

	/** Ledger lines from (x0, y) to x1, {@code rows} of them {@code height} high: every other one striped. */
	public static void stripes(GuiGraphicsExtractor g, int x0, int y, int x1, int rows, int height) {
		for (int row = 1; row < rows; row += 2) {
			g.fill(x0, y + row * height, x1, y + (row + 1) * height, ExchangeScreen.SLATE_STRIPE);
		}
	}

	/** A brass rule across (x0, y)-(x1, y): between a panel's parts. */
	public static void rule(GuiGraphicsExtractor g, int x0, int y, int x1) {
		g.fill(x0, y, x1, y + 1, ExchangeScreen.BRASS_DARK);
	}

	/** A slot, its item's top-left at (x, y). */
	public static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x - 1, y - 1, x + 17, y + 17, ExchangeScreen.SLATE_DARK);
		g.fill(x, y, x + 17, y + 17, ExchangeScreen.SLATE_LIGHT);
		g.fill(x, y, x + 16, y + 16, ExchangeScreen.SLATE_STRIPE);
	}

	/** Every slot of the menu that is in use, the menu's top-left at (x, y). */
	public static void slots(GuiGraphicsExtractor g, int x, int y, AbstractContainerMenu menu) {
		for (Slot slot : menu.slots) {
			if (slot.isActive()) {
				slot(g, x + slot.x, y + slot.y);
			}
		}
	}
}
