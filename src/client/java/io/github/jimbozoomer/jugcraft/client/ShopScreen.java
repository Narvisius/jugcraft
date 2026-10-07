package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.town.ShopMenu;
import io.github.jimbozoomer.jugcraft.town.TownShops;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A town shop: a wooden counter board with the shop's offers on parchment, a row each (the item, its name and how
 * many, the price in Jugs, and a button), the player's Jugs at the top, tabs for buying and (where the shop buys
 * goods) selling, and page buttons. Every button is a menu button; the server does the trade ({@link ShopMenu}).
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
	static final int WOOD = 0xFF5A3A22;
	static final int WOOD_DARK = 0xFF3A2414;
	static final int PARCHMENT = 0xFFE8D9B0;
	static final int PARCHMENT_DARK = 0xFFD2BF8E;
	static final int INK = 0xFF3A2A1A;
	static final int GOLD = 0xFFE0B040;
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
		int pages = Math.max(1, (offers.size() + ROWS - 1) / ROWS);
		page = Math.min(page, pages - 1);
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.shop.buy"), b -> {
			selling = false;
			page = 0;
			rebuild();
		}).bounds(leftPos + 10, topPos + 20, 50, 14).build()).active = selling;
		if (!menu.purchases().isEmpty()) {
			addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.shop.sell"), b -> {
				selling = true;
				page = 0;
				rebuild();
			}).bounds(leftPos + 62, topPos + 20, 50, 14).build()).active = !selling;
		}
		for (int row = 0; row < ROWS; row++) {
			int index = page * ROWS + row;
			if (index >= offers.size()) {
				break;
			}
			int id = selling ? ShopMenu.SELL_BASE + index : index;
			addRenderableWidget(Button.builder(Component.translatable(selling ? "screen.jugcraft.shop.sell_one" : "screen.jugcraft.shop.buy_one"),
					b -> click(id)).bounds(leftPos + imageWidth - 58, topPos + LIST_Y + row * ROW_HEIGHT + 2, 48, 16).build());
		}
		if (pages > 1) {
			addRenderableWidget(Button.builder(Component.literal("<"), b -> {
				page = (page + pages - 1) % pages;
				rebuild();
			}).bounds(leftPos + imageWidth - 58, topPos + imageHeight - 20, 20, 14).build());
			addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				page = (page + 1) % pages;
				rebuild();
			}).bounds(leftPos + imageWidth - 30, topPos + imageHeight - 20, 20, 14).build());
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
			TownShops.Offer offer = offers.get(index);
			ItemStack stack = offer.stack();
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
		panel(graphics, x, y, imageWidth, imageHeight);
		for (int row = 0; row < ROWS; row++) {
			int ry = y + LIST_Y + row * ROW_HEIGHT;
			graphics.fill(x + LIST_X, ry, x + imageWidth - 10, ry + ROW_HEIGHT - 1, row % 2 == 0 ? PARCHMENT : PARCHMENT_DARK);
		}
	}

	/** A wooden board with a parchment face and brass corners. */
	static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
		graphics.fill(x, y, x + w, y + h, WOOD_DARK);
		graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, WOOD);
		graphics.fill(x + 6, y + 6, x + w - 6, y + h - 6, PARCHMENT);
		for (int[] c : new int[][] {{x + 1, y + 1}, {x + w - 5, y + 1}, {x + 1, y + h - 5}, {x + w - 5, y + h - 5}}) {
			graphics.fill(c[0], c[1], c[0] + 4, c[1] + 4, GOLD);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, INK, false);
		String jugs = Component.translatable("screen.jugcraft.jugs", String.format("%,d", menu.balance())).getString();
		graphics.text(font, jugs, imageWidth - 10 - font.width(jugs), titleLabelY, INK, false);
		List<TownShops.Offer> offers = offers();
		if (offers.isEmpty()) {
			graphics.text(font, Component.translatable("screen.jugcraft.shop.empty").getString(), LIST_X + 4, LIST_Y + 6, INK, false);
		}
		for (int row = 0; row < ROWS; row++) {
			int index = page * ROWS + row;
			if (index >= offers.size()) {
				break;
			}
			TownShops.Offer offer = offers.get(index);
			ItemStack stack = offer.stack();
			int y = LIST_Y + row * ROW_HEIGHT + 6;
			String name = offer.count() + " " + stack.getHoverName().getString();
			if (font.width(name) > 120) {
				name = font.plainSubstrByWidth(name, 116) + "...";
			}
			graphics.text(font, name, LIST_X + 24, y, INK, false);
			String price = Component.translatable("screen.jugcraft.price", offer.price()).getString();
			graphics.text(font, price, imageWidth - 64 - font.width(price), y, INK, false);
		}
	}
}
