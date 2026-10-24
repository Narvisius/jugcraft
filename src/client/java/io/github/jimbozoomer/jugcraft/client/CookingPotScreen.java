package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Cooking Pot, in the workshop look (docs/features/bench-screens.md): a 3x2 ingredient grid on the glass, a progress
 * arrow to the 2x2 meal slots, and a flame under the arrow that shows whether the pot has heat. The status lamp is lit
 * while the pot has heat (green while a helper speeds it up), and a line under the meals says what it lacks.
 */
public class CookingPotScreen extends AbstractContainerScreen<CookingPotMenu> implements ScreenKit.Fitted {
	private static final int FLAME = 0xFFE06020;
	private static final int ARROW_X = 88;
	private static final int ARROW_Y = 43;
	private static final int ARROW_WIDTH = 22;
	/** The casing and its glass end here; the inventory tray starts under them. */
	private static final int BODY_HEIGHT = 78;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("cooking_pot");

	public CookingPotScreen(CookingPotMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 12;
		titleLabelY = 2;
		inventoryLabelY = 10_000;
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
		ScreenKit.body(graphics, theme, x, y, imageWidth, BODY_HEIGHT);
		ScreenKit.pane(graphics, theme, x + 3, y + 13, imageWidth - 6, BODY_HEIGHT - 15);
		ScreenKit.tray(graphics, theme, x, y + BODY_HEIGHT + 1, imageWidth, imageHeight - BODY_HEIGHT - 1);
		ScreenKit.inventory(graphics, theme, x + 8, y + 84);
		MachineSkin.lamp(graphics, x + 4, y + 3, menu.assisted() ? theme.ok() : menu.heated() ? theme.progress() : theme.dim());
		for (int slot = 0; slot < CookingPotBlockEntity.INPUTS; slot++) {
			MachineSkin.slot(graphics, theme, x + CookingPotMenu.INPUT_X + (slot % 3) * 18, y + CookingPotMenu.INPUT_Y + (slot / 3) * 18);
		}
		for (int index = 0; index < CookingPotBlockEntity.OUTPUTS; index++) {
			MachineSkin.slot(graphics, theme, x + CookingPotMenu.RESULT_X + (index % 2) * 18, y + CookingPotMenu.RESULT_Y + (index / 2) * 18);
		}
		ScreenKit.arrow(graphics, x + ARROW_X, y + ARROW_Y, ARROW_WIDTH, menu.progress(ARROW_WIDTH),
				menu.assisted() ? theme.ok() : theme.progress());
		// A small flame under the arrow: bright with heat below the pot, faint without.
		int color = menu.heated() ? FLAME : theme.dim();
		int fx = x + ARROW_X + ARROW_WIDTH / 2;
		int fy = y + 51;
		graphics.fill(fx - 3, fy + 4, fx + 3, fy + 8, color);
		graphics.fill(fx - 2, fy + 1, fx + 2, fy + 4, color);
		graphics.fill(fx - 1, fy - 2, fx + 1, fy + 1, color);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		// What the pot lacks, or what speeds it up, under the meals.
		String line = menu.assisted() ? Component.translatable("screen.jugcraft.cooking_pot.assisted").getString()
				: menu.heated() ? "" : Component.translatable("container.jugcraft.cooking_pot.cold").getString();
		if (!line.isEmpty()) {
			graphics.text(font, line, imageWidth - 9 - font.width(line), 65, menu.assisted() ? theme.ok() : theme.warn(), false);
		}
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight);
	}
}
