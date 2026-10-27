package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.client.ExchangeScreen.Plate;
import io.github.jimbozoomer.jugcraft.town.ShopMenu;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A town shop, in the trade look (docs/features/trade-look.md): the shop's name and the player's Jugs on the walnut, tabs
 * for buying and (where the shop buys goods) selling, and the offers on a slate ledger, a line each (the item, its name
 * and how many, the price in Jugs, and a brass key), with page keys under it. Every key is a menu button; the server does
 * the trade ({@link ShopMenu}).
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> implements ScreenKit.Fitted {
	private static final int ROWS = 7;
	private static final int ROW_HEIGHT = 20;
	private static final int LIST_X = 10;
	private static final int LIST_Y = 40;
	private boolean selling;
	private int page;

	public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 256, 200);
	}

	private List<TownShops.Offer> offers() {
		return selling ? menu.purchases() : menu.sales();
	}

	private int pages() {
		return Math.max(1, (offers().size() + ROWS - 1) / ROWS);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 10;
		titleLabelY = 8;
		inventoryLabelY = 10_000;
		rebuild();
	}

	private void rebuild() {
		clearWidgets();
		List<TownShops.Offer> offers = offers();
		int pages = pages();
		page = Math.min(page, pages - 1);
		// The tabs: the open one joins the ledger.
		Plate buy = addRenderableWidget(new Plate(leftPos + LIST_X, topPos + 22, 50, 14, Component.translatable("screen.jugcraft.shop.buy").getString(),
				selling ? Plate.Kind.TAB : Plate.Kind.TAB_OPEN, b -> {
					selling = false;
					page = 0;
					rebuild();
				}));
		buy.active = selling;
		if (!menu.purchases().isEmpty()) {
			Plate sell = addRenderableWidget(new Plate(leftPos + LIST_X + 52, topPos + 22, 50, 14,
					Component.translatable("screen.jugcraft.shop.sell").getString(), selling ? Plate.Kind.TAB_OPEN : Plate.Kind.TAB, b -> {
						selling = true;
						page = 0;
						rebuild();
					}));
			sell.active = !selling;
		}
		for (int row = 0; row < ROWS; row++) {
			int index = page * ROWS + row;
			if (index >= offers.size()) {
				break;
			}
			int id = selling ? ShopMenu.SELL_BASE + index : index;
			addRenderableWidget(new Plate(leftPos + imageWidth - 58, topPos + LIST_Y + row * ROW_HEIGHT + 2, 44, 16,
					Component.translatable(selling ? "screen.jugcraft.shop.sell_one" : "screen.jugcraft.shop.buy_one").getString(), Plate.Kind.BRASS,
					b -> click(id)));
		}
		if (pages > 1) {
			addRenderableWidget(new Plate(leftPos + imageWidth - 58, topPos + imageHeight - 17, 20, 14, "<", Plate.Kind.WOOD, b -> {
				page = (page + pages - 1) % pages;
				rebuild();
			}));
			addRenderableWidget(new Plate(leftPos + imageWidth - 34, topPos + imageHeight - 17, 20, 14, ">", Plate.Kind.WOOD, b -> {
				page = (page + 1) % pages;
				rebuild();
			}));
		}
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		List<TownShops.Offer> offers = offers();
		for (int row = 0; row < ROWS; row++) {
			int index = page * ROWS + row;
			if (index >= offers.size()) {
				break;
			}
			ItemStack stack = offers.get(index).stack();
			int x = leftPos + LIST_X + 4;
			int y = topPos + LIST_Y + row * ROW_HEIGHT + 2;
			graphics.item(stack, x, y);
			graphics.itemDecorations(font, stack, x, y);
			if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
				graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		TradeLook.frame(graphics, x, y, x + imageWidth, y + imageHeight);
		TradeLook.rule(graphics, x + 6, y + 19, x + imageWidth - 6);
		// The ledger, its lines striped and the shown offers' lines only.
		TradeLook.tray(graphics, x + LIST_X - 2, y + LIST_Y - 2, x + imageWidth - 8, y + LIST_Y + ROWS * ROW_HEIGHT + 1);
		TradeLook.stripes(graphics, x + LIST_X, y + LIST_Y, x + imageWidth - 10, ROWS, ROW_HEIGHT);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, TradeLook.GOLD, false);
		String jugs = Component.translatable("screen.jugcraft.jugs", String.format("%,d", menu.balance())).getString();
		graphics.text(font, jugs, imageWidth - 10 - font.width(jugs), titleLabelY, TradeLook.CREAM, false);
		List<TownShops.Offer> offers = offers();
		if (offers.isEmpty()) {
			int y = LIST_Y + 6;
			for (FormattedCharSequence line : font.split(Component.translatable("screen.jugcraft.shop.empty"), imageWidth - LIST_X - 22)) {
				graphics.text(font, line, LIST_X + 4, y, TradeLook.DIM, false);
				y += 10;
			}
		}
		for (int row = 0; row < ROWS; row++) {
			int index = page * ROWS + row;
			if (index >= offers.size()) {
				break;
			}
			TownShops.Offer offer = offers.get(index);
			ItemStack stack = offer.stack();
			int y = LIST_Y + row * ROW_HEIGHT + 6;
			String price = Component.translatable("screen.jugcraft.price", offer.price()).getString();
			String name = offer.count() + " " + stack.getHoverName().getString();
			graphics.text(font, ScreenKit.fit(font, name, imageWidth - 92 - LIST_X - 24 - font.width(price)), LIST_X + 24, y, TradeLook.CREAM, false);
			graphics.text(font, price, imageWidth - 64 - font.width(price), y, TradeLook.GOLD, false);
		}
		int pages = pages();
		if (pages > 1) {
			String at = (page + 1) + " / " + pages;
			graphics.text(font, at, imageWidth - 66 - font.width(at), imageHeight - 14, TradeLook.TAN, false);
		}
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(leftPos - 3, topPos - 3, imageWidth + 6, imageHeight + 6);
	}
}
