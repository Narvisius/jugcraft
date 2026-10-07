package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Farm Stand: a wooden board with its six crates in a row, each with its chalked price in Jugs and a button under
 * it, the player's Jugs, and their inventory. A buyer's buttons buy one item from a crate. The owner's pick a crate to
 * price, and two rows of buttons move the picked crate's price down or up by 1, 10, 100 or 1,000. Every button is a
 * menu button; the server checks it and does the work ({@link FarmStandMenu}).
 */
public class FarmStandScreen extends AbstractContainerScreen<FarmStandMenu> {
	private static final int DARK = 0xFF373737;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int SLATE = 0xFF2E3436;
	private static final int CHALK = 0xFFEDEBE0;
	private static final int PICKED = 0xFFE0B040;
	private static final int BUTTON_Y = 50;
	private static final int STEPS_Y = 68;
	private static final String[] STEP_LABELS = {"1", "10", "100", "1K"};
	private int picked;

	public FarmStandScreen(FarmStandMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, FarmStandMenu.INVENTORY_Y + 82);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 10;
		titleLabelY = 8;
		inventoryLabelY = FarmStandMenu.INVENTORY_Y - 10;
		rebuild();
	}

	private void rebuild() {
		clearWidgets();
		boolean owner = menu.opening.owner();
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			int id = crate;
			int x = leftPos + FarmStandMenu.CRATE_X + crate * FarmStandMenu.CRATE_STEP - 4;
			if (owner) {
				addRenderableWidget(Button.builder(Component.translatable("gui.jugcraft.farm_stand.pick"), b -> {
					picked = id;
					rebuild();
				}).bounds(x, topPos + BUTTON_Y, 24, 14).build()).active = crate != picked;
			} else {
				addRenderableWidget(Button.builder(Component.translatable("gui.jugcraft.farm_stand.buy"), b -> click(id))
						.bounds(x, topPos + BUTTON_Y, 24, 14).build());
			}
		}
		if (!owner) {
			return;
		}
		for (int step = 0; step < FarmStandMenu.PRICE_STEPS.length; step++) {
			int down = FarmStandMenu.priceButton(picked, step, false);
			int up = FarmStandMenu.priceButton(picked, step, true);
			addRenderableWidget(Button.builder(Component.literal("-" + STEP_LABELS[step]), b -> click(down))
					.bounds(leftPos + 8 + step * 40, topPos + STEPS_Y, 39, 14).build());
			addRenderableWidget(Button.builder(Component.literal("+" + STEP_LABELS[step]), b -> click(up))
					.bounds(leftPos + 8 + step * 40, topPos + STEPS_Y + 15, 39, 14).build());
		}
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	/** A price short enough for its crate: exact below 10,000, then in thousands, millions or billions. */
	static String shortPrice(long price) {
		if (price < 10_000) {
			return Long.toString(price);
		}
		String[] units = {"K", "M", "B"};
		double value = price;
		int unit = -1;
		while (value >= 1000 && unit < units.length - 1) {
			value /= 1000;
			unit++;
		}
		return (value < 100 ? String.format("%.1f", Math.floor(value * 10) / 10) : Long.toString((long) value)) + units[unit];
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		ShopScreen.panel(graphics, x, y, imageWidth, imageHeight);
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			int sx = x + FarmStandMenu.CRATE_X + crate * FarmStandMenu.CRATE_STEP;
			if (menu.opening.owner() && crate == picked) {
				graphics.fill(sx - 3, y + FarmStandMenu.CRATE_Y - 3, sx + 19, y + FarmStandMenu.CRATE_Y + 19, PICKED);
			}
			slotFrame(graphics, sx, y + FarmStandMenu.CRATE_Y);
			// The slate the price is chalked on.
			graphics.fill(sx - 4, y + 40, sx + 20, y + 49, SLATE);
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				slotFrame(graphics, x + 8 + column * 18, y + FarmStandMenu.INVENTORY_Y + row * 18);
			}
		}
		for (int column = 0; column < 9; column++) {
			slotFrame(graphics, x + 8 + column * 18, y + FarmStandMenu.INVENTORY_Y + 58);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, ShopScreen.INK, false);
		String whose = Component.translatable("gui.jugcraft.farm_stand.owner", menu.opening.ownerName()).getString();
		graphics.text(font, whose, imageWidth - 10 - font.width(whose), titleLabelY, ShopScreen.INK, false);
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			String price = shortPrice(menu.price(crate));
			int centre = FarmStandMenu.CRATE_X + crate * FarmStandMenu.CRATE_STEP + 8;
			graphics.text(font, price, centre - font.width(price) / 2, 41, CHALK, false);
		}
		String jugs = Component.translatable("gui.jugcraft.farm_stand.balance", String.format("%,d", menu.balance())).getString();
		int jugsY = menu.opening.owner() ? STEPS_Y + 32 : STEPS_Y + 4;
		graphics.text(font, jugs, 10, jugsY, ShopScreen.INK, false);
		if (menu.opening.owner()) {
			String pricing = Component.translatable("gui.jugcraft.farm_stand.pricing", picked + 1,
					String.format("%,d", menu.price(picked))).getString();
			graphics.text(font, pricing, imageWidth - 10 - font.width(pricing), STEPS_Y + 32, ShopScreen.INK, false);
		} else {
			String hint = Component.translatable("gui.jugcraft.farm_stand.hint").getString();
			graphics.text(font, hint, 10, STEPS_Y + 18, ShopScreen.INK, false);
		}
		graphics.text(font, playerInventoryTitle.getString(), inventoryLabelX, inventoryLabelY, ShopScreen.INK, false);
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
