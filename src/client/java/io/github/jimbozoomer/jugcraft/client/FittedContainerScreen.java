package io.github.jimbozoomer.jugcraft.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A container screen laid out at its own size and scaled down to fit a smaller window (the smallest GUI is 320 x 240:
 * docs/features/screen-fit-check.md), as the reactor console and the aircraft screen are. It draws its panel
 * ({@link #background}), the slots and the widgets in one scaled pass, and scales the mouse to match, so the slots and
 * keys work at any size; a slot's tooltip, drawn after the screen, goes at the real mouse. At full size nothing changes.
 */
public abstract class FittedContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements ScreenKit.Fitted {
	private float scale = 1;

	protected FittedContainerScreen(T menu, Inventory inventory, Component title, int width, int height) {
		super(menu, inventory, title, width, height);
	}

	@Override
	protected void init() {
		super.init();
		scale = Math.min(1F, Math.min((width - 8F) / imageWidth, (height - 8F) / imageHeight));
		leftPos = Math.round((width / scale - imageWidth) / 2);
		topPos = Math.round((height / scale - imageHeight) / 2);
	}

	/** The scale the screen is drawn at: 1 where it fits, less in a smaller window. */
	protected float scale() {
		return scale;
	}

	/** The panel under the slots and widgets, in the screen's own (unscaled) coordinates. */
	protected abstract void background(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);

	/** Anything drawn over the slots and widgets, in the screen's own coordinates (nothing by default). */
	protected void foreground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = Math.round(mouseX / scale);
		int y = Math.round(mouseY / scale);
		graphics.pose().pushMatrix();
		graphics.pose().scale(scale, scale);
		background(graphics, x, y, delta);
		super.extractRenderState(graphics, x, y, delta);
		foreground(graphics, x, y, delta);
		graphics.pose().popMatrix();
		// The slot's tooltip is drawn after the screen, unscaled: at the real mouse.
		extractTooltip(graphics, mouseX, mouseY);
	}

	/** The panel is drawn in {@link #extractRenderState}, in its scale; nothing is drawn here, whoever calls it. */
	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
	}

	private MouseButtonEvent scaled(MouseButtonEvent event) {
		return new MouseButtonEvent(event.x() / scale, event.y() / scale, event.buttonInfo());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return super.mouseClicked(scaled(event), doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return super.mouseReleased(scaled(event));
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		return super.mouseDragged(scaled(event), dragX / scale, dragY / scale);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return super.mouseScrolled(mouseX / scale, mouseY / scale, scrollX, scrollY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(mouseX / scale, mouseY / scale);
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(Math.round(leftPos * scale), Math.round(topPos * scale), Math.round(imageWidth * scale),
				Math.round(imageHeight * scale));
	}
}
