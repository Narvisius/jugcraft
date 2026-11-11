package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Advanced Crafting Table's screen (take 2: just the grid): the 6 x 6 grid, the arrow (lit while the grid makes
 * something), the result in its frame with its name under it, and the inventory, on a slate panel in the machines'
 * style with cyan trim (textures/gui/advanced_crafting_table.png, tools/adv_crafting_table.py gui()).
 */
public class AdvancedCraftingScreen extends AbstractContainerScreen<AdvancedCraftingMenu> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/gui/advanced_crafting_table.png");
	private static final int WIDTH = 236, HEIGHT = 238;
	private static final int ARROW_X = 134, ARROW_Y = 72;
	private static final int FRAME_X = 168, FRAME_Y = 61, FRAME = 26;
	private static final int TITLE = 0xFFE2F0F6, CYAN = 0xFF46C8E6, GREY = 0xFF82949C;

	public AdvancedCraftingScreen(AdvancedCraftingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 8;
		titleLabelY = 6;
		inventoryLabelX = 8;
		inventoryLabelY = 142;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		extractTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos, y = topPos;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, WIDTH, HEIGHT, 256, 256);
		if (!result().isEmpty()) {
			// the arrow lit and the result's frame in cyan while the grid makes something
			graphics.fill(x + ARROW_X, y + ARROW_Y, x + ARROW_X + 18, y + ARROW_Y + 3, CYAN);
			for (int k = 0; k < 5; k++) {
				graphics.fill(x + ARROW_X + 18 + k, y + ARROW_Y - 4 + k, x + ARROW_X + 19 + k, y + ARROW_Y + 7 - k, CYAN);
			}
			graphics.fill(x + FRAME_X, y + FRAME_Y, x + FRAME_X + FRAME, y + FRAME_Y + 1, CYAN);
			graphics.fill(x + FRAME_X, y + FRAME_Y + FRAME - 1, x + FRAME_X + FRAME, y + FRAME_Y + FRAME, CYAN);
			graphics.fill(x + FRAME_X, y + FRAME_Y, x + FRAME_X + 1, y + FRAME_Y + FRAME, CYAN);
			graphics.fill(x + FRAME_X + FRAME - 1, y + FRAME_Y, x + FRAME_X + FRAME, y + FRAME_Y + FRAME, CYAN);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, TITLE, false);
		graphics.text(font, "AF-6", WIDTH - 8 - font.width("AF-6"), titleLabelY, CYAN, false);
		graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, GREY, false);
		ItemStack made = result();
		if (!made.isEmpty()) {
			List<FormattedCharSequence> lines = font.split(made.getHoverName(), 96);
			int middle = FRAME_X + FRAME / 2;
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				graphics.text(font, lines.get(i), middle - font.width(lines.get(i)) / 2, FRAME_Y + FRAME + 5 + i * 10, TITLE, false);
			}
		}
	}

	private ItemStack result() {
		return menu.getSlot(AdvancedCraftingMenu.RESULT).getItem();
	}
}
