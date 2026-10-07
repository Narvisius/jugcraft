package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The Cooking Pot: a 3x2 ingredient grid, a progress arrow, 2x2 meal slots, and a flame that shows whether the pot has heat. */
public class CookingPotScreen extends AbstractContainerScreen<CookingPotMenu> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/gui/machine.png");
	private static final int DARK = 0xFF373737;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int PROGRESS = 0xFFD8862C;
	private static final int FLAME = 0xFFE06020;
	private static final int COLD = 0xFF6A6A6A;
	private static final int TEXT = 0xFF404040;
	private static final int ARROW_X = 88;
	private static final int ARROW_Y = 41;
	private static final int ARROW_WIDTH = 22;

	public CookingPotScreen(CookingPotMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
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
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
		for (int slot = 0; slot < CookingPotBlockEntity.INPUTS; slot++) {
			slotFrame(graphics, x + CookingPotMenu.INPUT_X + (slot % 3) * 18, y + CookingPotMenu.INPUT_Y + (slot / 3) * 18);
		}
		for (int index = 0; index < CookingPotBlockEntity.OUTPUTS; index++) {
			slotFrame(graphics, x + CookingPotMenu.RESULT_X + (index % 2) * 18, y + CookingPotMenu.RESULT_Y + (index / 2) * 18);
		}
		graphics.fill(x + ARROW_X, y + ARROW_Y, x + ARROW_X + ARROW_WIDTH, y + ARROW_Y + 4, DARK);
		graphics.fill(x + ARROW_X, y + ARROW_Y, x + ARROW_X + menu.progress(ARROW_WIDTH), y + ARROW_Y + 4, PROGRESS);
		// A small flame under the arrow: bright with heat below the pot, grey without.
		int color = menu.heated() ? FLAME : COLD;
		int fx = x + ARROW_X + ARROW_WIDTH / 2;
		int fy = y + 52;
		graphics.fill(fx - 3, fy + 4, fx + 3, fy + 8, color);
		graphics.fill(fx - 2, fy + 1, fx + 2, fy + 4, color);
		graphics.fill(fx - 1, fy - 2, fx + 1, fy + 1, color);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		if (!menu.heated()) {
			String hint = Component.translatable("container.jugcraft.cooking_pot.cold").getString();
			graphics.text(font, hint, imageWidth - 8 - font.width(hint), 72, TEXT);
		}
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
