package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.FarmStandBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FarmStandMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Farm Stand, in the workshop look (docs/features/bench-screens.md): its six crates in a row on the glass, each with
 * its chalked price in Jugs on a slate and a key under it, the player's Jugs, and their inventory in the tray. Whose
 * stand it is shows in a chip by the title. A buyer's keys buy one item from a crate. The owner's pick a crate to price
 * (the picked crate's key is lit and its crate framed), and two rows of keys move its price down or up by 1, 10, 100 or
 * 1,000. Every key is a menu button; the server checks it and does the work ({@link FarmStandMenu}).
 */
public class FarmStandScreen extends AbstractContainerScreen<FarmStandMenu> implements ScreenKit.Fitted {
	private static final int SLATE = 0xFF2E3436;
	private static final int CHALK = 0xFFEDEBE0;
	private static final int BUTTON_Y = 50;
	private static final int STEPS_Y = 67;
	/** The line with the player's Jugs (and, for the owner, the picked crate's price), at the foot of the glass. */
	private static final int INFO_Y = 96;
	private static final String[] STEP_LABELS = {"1", "10", "100", "1K"};
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("farm_stand");
	private int picked;

	public FarmStandScreen(FarmStandMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, FarmStandMenu.INVENTORY_Y + 82);
	}

	/** The casing and its glass end here, just above the inventory; the tray starts under them. */
	private static int bodyHeight() {
		return FarmStandMenu.INVENTORY_Y - 5;
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 12;
		titleLabelY = 2;
		inventoryLabelY = 10_000;
		rebuild();
	}

	private void rebuild() {
		clearWidgets();
		boolean owner = menu.opening.owner();
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			int id = crate;
			int x = leftPos + FarmStandMenu.CRATE_X + crate * FarmStandMenu.CRATE_STEP - 4;
			if (owner) {
				ScreenKit.TextButton pick = addRenderableWidget(new ScreenKit.TextButton(x, topPos + BUTTON_Y, 24, 14, theme,
						Component.translatable("gui.jugcraft.farm_stand.pick"), b -> {
							picked = id;
							rebuild();
						}));
				pick.active = crate != picked;
				pick.setSelected(crate == picked);
				pick.setTooltip(Tooltip.create(Component.translatable("gui.jugcraft.farm_stand.pick_help")));
			} else {
				addRenderableWidget(new ScreenKit.TextButton(x, topPos + BUTTON_Y, 24, 14, theme, Component.translatable("gui.jugcraft.farm_stand.buy"),
						b -> click(id)));
			}
		}
		if (!owner) {
			return;
		}
		for (int step = 0; step < FarmStandMenu.PRICE_STEPS.length; step++) {
			int down = FarmStandMenu.priceButton(picked, step, false);
			int up = FarmStandMenu.priceButton(picked, step, true);
			addRenderableWidget(new ScreenKit.TextButton(leftPos + 8 + step * 40, topPos + STEPS_Y, 39, 13, theme,
					Component.literal("-" + STEP_LABELS[step]), b -> click(down)));
			addRenderableWidget(new ScreenKit.TextButton(leftPos + 8 + step * 40, topPos + STEPS_Y + 14, 39, 13, theme,
					Component.literal("+" + STEP_LABELS[step]), b -> click(up)));
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
		int body = bodyHeight();
		ScreenKit.body(graphics, theme, x, y, imageWidth, body);
		ScreenKit.pane(graphics, theme, x + 3, y + 13, imageWidth - 6, body - 15);
		ScreenKit.tray(graphics, theme, x, y + body + 1, imageWidth, imageHeight - body - 1);
		ScreenKit.inventory(graphics, theme, x + 8, y + FarmStandMenu.INVENTORY_Y);
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			int sx = x + FarmStandMenu.CRATE_X + crate * FarmStandMenu.CRATE_STEP;
			if (menu.opening.owner() && crate == picked) {
				graphics.outline(sx - 3, y + FarmStandMenu.CRATE_Y - 3, 22, 22, theme.progress());
			}
			MachineSkin.slot(graphics, theme, sx, y + FarmStandMenu.CRATE_Y);
			// The slate the price is chalked on.
			graphics.fill(sx - 4, y + 40, sx + 20, y + 49, SLATE);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		String whose = Component.translatable("gui.jugcraft.farm_stand.owner", menu.opening.ownerName()).getString();
		ScreenKit.chip(graphics, font, theme, imageWidth - 3, 1, ScreenKit.fit(font, whose, 84), theme.text());
		int titleRoom = imageWidth - 12 - Math.min(font.width(whose), 84) - 16;
		graphics.text(font, ScreenKit.fit(font, title.getString(), titleRoom), titleLabelX, titleLabelY, theme.label(), false);
		for (int crate = 0; crate < FarmStandBlockEntity.CRATES; crate++) {
			String price = shortPrice(menu.price(crate));
			int centre = FarmStandMenu.CRATE_X + crate * FarmStandMenu.CRATE_STEP + 8;
			graphics.text(font, price, centre - font.width(price) / 2, 41, CHALK, false);
		}
		String jugs = Component.translatable("gui.jugcraft.farm_stand.balance", String.format("%,d", menu.balance())).getString();
		graphics.text(font, jugs, 9, INFO_Y, theme.text(), false);
		if (menu.opening.owner()) {
			String pricing = Component.translatable("gui.jugcraft.farm_stand.pricing", picked + 1,
					String.format("%,d", menu.price(picked))).getString();
			pricing = ScreenKit.fit(font, pricing, imageWidth - 22 - font.width(jugs));
			graphics.text(font, pricing, imageWidth - 9 - font.width(pricing), INFO_Y, theme.progress(), false);
		} else {
			MachineSkin.wrap(graphics, font, Component.translatable("gui.jugcraft.farm_stand.hint"), 9, STEPS_Y + 2, imageWidth - 18,
					INFO_Y - 2, theme.dim());
		}
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight);
	}
}
