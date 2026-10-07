package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity;
import io.github.jimbozoomer.jugcraft.rocketry.RocketPadMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The rocket pad (batch 39): 3x3 cargo, the rocket and flight plan slots, a launch button and the last launch's result. */
public class RocketPadScreen extends AbstractContainerScreen<RocketPadMenu> {
	private static final Identifier TEXTURE = Jugcraft.id("textures/gui/machine.png");
	private static final int DARK = 0xFF373737;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int GOOD = 0xFF2E7D32;
	private static final int BAD = 0xFFB02E26;

	public RocketPadScreen(RocketPadMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = (imageWidth - font.width(title)) / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.rocket_pad.launch"), b -> launch())
				.bounds(leftPos + RocketPadMenu.ROCKET_X - 1, topPos + RocketPadMenu.ROCKET_Y + 22, 44, 16).build());
	}

	private void launch() {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RocketPadMenu.LAUNCH);
		}
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
		for (int slot = 0; slot < RocketPadBlockEntity.CARGO; slot++) {
			slotFrame(graphics, x + RocketPadMenu.CARGO_X + (slot % 3) * 18, y + RocketPadMenu.CARGO_Y + (slot / 3) * 18);
		}
		slotFrame(graphics, x + RocketPadMenu.ROCKET_X, y + RocketPadMenu.ROCKET_Y);
		slotFrame(graphics, x + RocketPadMenu.PLAN_X, y + RocketPadMenu.PLAN_Y);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractLabels(graphics, mouseX, mouseY);
		RocketPadBlockEntity.Result result = menu.result();
		if (result != RocketPadBlockEntity.Result.NONE) {
			String text = result.message().getString();
			int width = font.width(text);
			graphics.text(font, text, Math.max(8, imageWidth - 8 - width), 72,
					result == RocketPadBlockEntity.Result.LAUNCHED ? GOOD : BAD);
		}
	}

	private static void slotFrame(GuiGraphicsExtractor graphics, int slotX, int slotY) {
		graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, DARK);
		graphics.fill(slotX, slotY, slotX + 17, slotY + 17, LIGHT);
		graphics.fill(slotX, slotY, slotX + 16, slotY + 16, SLOT);
	}
}
