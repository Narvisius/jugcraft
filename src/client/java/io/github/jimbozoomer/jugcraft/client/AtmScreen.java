package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.town.AtmMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Jug Teller: the player's Jugs, the players online to send to (a page of names at a time), the amount with
 * buttons to add or take 1, 10, 100 or 1,000, and Send. The amount and choice are kept by the server ({@link AtmMenu}).
 */
public class AtmScreen extends AbstractContainerScreen<AtmMenu> {
	private static final int NAMES_PER_PAGE = 6;
	private static final int SCREEN = 0xFF1E3A22;
	private static final int SCREEN_TEXT = 0xFF8CFF9C;
	private int page;

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

	private void rebuild() {
		clearWidgets();
		List<String> names = menu.opening.names();
		int pages = Math.max(1, (names.size() + NAMES_PER_PAGE - 1) / NAMES_PER_PAGE);
		page = Math.min(page, pages - 1);
		for (int i = 0; i < NAMES_PER_PAGE; i++) {
			int index = page * NAMES_PER_PAGE + i;
			if (index >= names.size()) {
				break;
			}
			int id = index;
			addRenderableWidget(Button.builder(Component.literal(names.get(index)), b -> click(id))
					.bounds(leftPos + 10, topPos + 50 + i * 18, 104, 16).build());
		}
		if (pages > 1) {
			addRenderableWidget(Button.builder(Component.literal("<"), b -> {
				page = (page + pages - 1) % pages;
				rebuild();
			}).bounds(leftPos + 10, topPos + 160, 20, 14).build());
			addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				page = (page + 1) % pages;
				rebuild();
			}).bounds(leftPos + 94, topPos + 160, 20, 14).build());
		}
		String[] steps = {"1", "10", "100", "1K"};
		for (int i = 0; i < 4; i++) {
			int add = AtmMenu.ADD + i;
			int sub = AtmMenu.SUBTRACT + i;
			addRenderableWidget(Button.builder(Component.literal("+" + steps[i]), b -> click(add))
					.bounds(leftPos + 124 + i * 27, topPos + 92, 26, 16).build());
			addRenderableWidget(Button.builder(Component.literal("-" + steps[i]), b -> click(sub))
					.bounds(leftPos + 124 + i * 27, topPos + 110, 26, 16).build());
		}
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.atm.clear"), b -> click(AtmMenu.CLEAR))
				.bounds(leftPos + 124, topPos + 130, 52, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.atm.all"), b -> click(AtmMenu.ALL))
				.bounds(leftPos + 178, topPos + 130, 52, 16).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.atm.send"), b -> click(AtmMenu.SEND))
				.bounds(leftPos + 124, topPos + 156, 106, 20).build());
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
		ShopScreen.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
		// The machine's little green screen.
		graphics.fill(leftPos + 122, topPos + 22, leftPos + imageWidth - 8, topPos + 86, 0xFF8A6A2A);
		graphics.fill(leftPos + 124, topPos + 24, leftPos + imageWidth - 10, topPos + 84, SCREEN);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, ShopScreen.INK, false);
		graphics.text(font, Component.translatable("screen.jugcraft.atm.to").getString(), 10, 38, ShopScreen.INK, false);
		if (menu.opening.names().isEmpty()) {
			graphics.text(font, Component.translatable("screen.jugcraft.atm.nobody").getString(), 10, 54, ShopScreen.INK, false);
		}
		graphics.text(font, Component.translatable("screen.jugcraft.jugs", String.format("%,d", menu.balance())).getString(), 128, 30, SCREEN_TEXT, false);
		int chosen = menu.chosen();
		String to = chosen >= 0 && chosen < menu.opening.names().size() ? menu.opening.names().get(chosen) : "-";
		graphics.text(font, Component.translatable("screen.jugcraft.atm.recipient", to).getString(), 128, 46, SCREEN_TEXT, false);
		graphics.text(font, Component.translatable("screen.jugcraft.atm.amount", String.format("%,d", menu.amount())).getString(), 128, 62, SCREEN_TEXT, false);
	}
}
