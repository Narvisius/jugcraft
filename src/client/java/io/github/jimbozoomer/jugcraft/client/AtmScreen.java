package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.client.ExchangeScreen.Plate;
import io.github.jimbozoomer.jugcraft.town.AtmMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Jug Teller, in the trade look (docs/features/trade-look.md): the players online to send to (a page of names at a
 * time, the chosen one in brass), the teller's slate display with the player's Jugs, the recipient and the amount, keys
 * to add or take 1, 10, 100 or 1,000, and a brass Send. The amount and choice are kept by the server ({@link AtmMenu}).
 */
public class AtmScreen extends AbstractContainerScreen<AtmMenu> implements ScreenKit.Fitted {
	private static final int NAMES_PER_PAGE = 6;
	private int page;
	/** The choice the name keys were laid out for: they are laid out again when the server's choice changes. */
	private int shownChoice = -1;

	public AtmScreen(AtmMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 240, 196);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 10;
		titleLabelY = 8;
		inventoryLabelY = 10_000;
		rebuild();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (menu.chosen() != shownChoice) {
			rebuild();
		}
	}

	private void rebuild() {
		clearWidgets();
		shownChoice = menu.chosen();
		List<String> names = menu.opening.names();
		int pages = Math.max(1, (names.size() + NAMES_PER_PAGE - 1) / NAMES_PER_PAGE);
		page = Math.min(page, pages - 1);
		for (int i = 0; i < NAMES_PER_PAGE; i++) {
			int index = page * NAMES_PER_PAGE + i;
			if (index >= names.size()) {
				break;
			}
			int id = index;
			addRenderableWidget(new Plate(leftPos + 10, topPos + 50 + i * 18, 104, 16, names.get(index),
					index == shownChoice ? Plate.Kind.BRASS : Plate.Kind.WOOD, b -> click(id)));
		}
		if (pages > 1) {
			addRenderableWidget(new Plate(leftPos + 10, topPos + 162, 20, 14, "<", Plate.Kind.WOOD, b -> {
				page = (page + pages - 1) % pages;
				rebuild();
			}));
			addRenderableWidget(new Plate(leftPos + 94, topPos + 162, 20, 14, ">", Plate.Kind.WOOD, b -> {
				page = (page + 1) % pages;
				rebuild();
			}));
		}
		String[] steps = {"1", "10", "100", "1K"};
		for (int i = 0; i < 4; i++) {
			int add = AtmMenu.ADD + i;
			int sub = AtmMenu.SUBTRACT + i;
			addRenderableWidget(new Plate(leftPos + 124 + i * 27, topPos + 92, 26, 16, "+" + steps[i], Plate.Kind.WOOD, b -> click(add)));
			addRenderableWidget(new Plate(leftPos + 124 + i * 27, topPos + 110, 26, 16, "-" + steps[i], Plate.Kind.WOOD, b -> click(sub)));
		}
		addRenderableWidget(new Plate(leftPos + 124, topPos + 130, 52, 16, Component.translatable("screen.jugcraft.atm.clear").getString(),
				Plate.Kind.WOOD, b -> click(AtmMenu.CLEAR)));
		addRenderableWidget(new Plate(leftPos + 178, topPos + 130, 52, 16, Component.translatable("screen.jugcraft.atm.all").getString(),
				Plate.Kind.WOOD, b -> click(AtmMenu.ALL)));
		addRenderableWidget(new Plate(leftPos + 124, topPos + 156, 106, 20, Component.translatable("screen.jugcraft.atm.send").getString(),
				Plate.Kind.BRASS, b -> click(AtmMenu.SEND)));
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
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		TradeLook.frame(graphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
		TradeLook.rule(graphics, leftPos + 6, topPos + 19, leftPos + imageWidth - 6);
		// The names on their ledger, and the teller's display.
		TradeLook.tray(graphics, leftPos + 8, topPos + 48, leftPos + 116, topPos + 158);
		TradeLook.tray(graphics, leftPos + 122, topPos + 22, leftPos + imageWidth - 8, topPos + 86);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, TradeLook.GOLD, false);
		graphics.text(font, Component.translatable("screen.jugcraft.atm.to").getString(), 10, 37, TradeLook.TAN, false);
		if (menu.opening.names().isEmpty()) {
			int y = 54;
			for (var line : font.split(Component.translatable("screen.jugcraft.atm.nobody"), 100)) {
				graphics.text(font, line, 12, y, TradeLook.DIM, false);
				y += 10;
			}
		}
		graphics.text(font, Component.translatable("screen.jugcraft.jugs", String.format("%,d", menu.balance())).getString(), 128, 30, TradeLook.GOLD, false);
		int chosen = menu.chosen();
		String to = chosen >= 0 && chosen < menu.opening.names().size() ? menu.opening.names().get(chosen) : "-";
		graphics.text(font, ScreenKit.fit(font, Component.translatable("screen.jugcraft.atm.recipient", to).getString(), imageWidth - 140), 128, 46,
				TradeLook.CREAM, false);
		graphics.text(font, Component.translatable("screen.jugcraft.atm.amount", String.format("%,d", menu.amount())).getString(), 128, 62,
				menu.amount() > 0 ? TradeLook.GAIN : TradeLook.CREAM, false);
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(leftPos - 3, topPos - 3, imageWidth + 6, imageHeight + 6);
	}
}
