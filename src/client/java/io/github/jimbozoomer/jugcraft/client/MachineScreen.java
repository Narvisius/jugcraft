package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Screen for every machine: energy bar on the left, slots and progress in the middle. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/gui/machine.png");
	private static final int BAR_X = 10;
	private static final int BAR_Y = 17;
	private static final int BAR_WIDTH = 12;
	private static final int BAR_HEIGHT = 52;

	private static final int DARK = 0xFF373737;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int ENERGY = 0xFFE0B020;
	private static final int PROGRESS = 0xFF60A0E0;
	private static final int FLAME = 0xFFE06020;
	private static final int TEXT = 0xFF404040;

	public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
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

		// Energy bar, filled from the bottom.
		graphics.fill(x + BAR_X - 1, y + BAR_Y - 1, x + BAR_X + BAR_WIDTH + 1, y + BAR_Y + BAR_HEIGHT + 1, DARK);
		long capacity = Math.max(1, menu.capacity());
		int filled = (int) (menu.energy() * BAR_HEIGHT / capacity);
		graphics.fill(x + BAR_X, y + BAR_Y + BAR_HEIGHT - filled, x + BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, ENERGY);

		MachineKind kind = menu.kind();
		if (kind == MachineKind.COAL_GENERATOR) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + MachineMenu.SLOT_Y);
			int maxBurn = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_BURN));
			int flame = menu.data(MachineBlockEntity.DATA_BURN) * 14 / maxBurn;
			graphics.fill(x + 57, y + 20 + 14 - flame, x + 71, y + 34, FLAME);
		} else if (kind.isProcessor()) {
			slotFrame(graphics, x + MachineMenu.INPUT_X, y + MachineMenu.SLOT_Y);
			slotFrame(graphics, x + MachineMenu.OUTPUT_X, y + MachineMenu.SLOT_Y);
			int maxProgress = Math.max(1, menu.data(MachineBlockEntity.DATA_MAX_PROGRESS));
			int arrow = menu.data(MachineBlockEntity.DATA_PROGRESS) * 24 / maxProgress;
			graphics.fill(x + 80, y + 41, x + 104, y + 45, DARK);
			graphics.fill(x + 80, y + 41, x + 80 + arrow, y + 45, PROGRESS);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		String energy = menu.energy() + " / " + menu.capacity() + " JE";
		graphics.text(font, energy, 28, 60, TEXT);
		if (menu.kind() == MachineKind.ARC_FURNACE) {
			String key = menu.data(MachineBlockEntity.DATA_FORMED) == 1
					? "container.jugcraft.arc_furnace.formed" : "container.jugcraft.arc_furnace.incomplete";
			graphics.text(font, Component.translatable(key).getString(), 28, 18, TEXT);
		}
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
