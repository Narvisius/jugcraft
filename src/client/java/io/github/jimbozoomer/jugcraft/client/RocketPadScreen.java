package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.rocketry.RocketPadBlockEntity;
import io.github.jimbozoomer.jugcraft.rocketry.RocketPadMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The rocket pad (batch 39), in the industrial look (docs/features/machine-screens.md): the 3x3 cargo, an arrow to the
 * rocket and flight plan slots (faint hints while empty, help when hovered), the Launch key, and the last launch's
 * result in a chip by the title, with the status lamp lit green after a launch and red when one failed.
 */
public class RocketPadScreen extends AbstractContainerScreen<RocketPadMenu> {
	private static final int CONTAINER_SLOTS = RocketPadBlockEntity.CARGO + 2;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("rocket_pad");

	public RocketPadScreen(RocketPadMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 12;
		titleLabelY = 2;
		inventoryLabelY = 10_000;
		ScreenKit.TextButton launch = addRenderableWidget(new ScreenKit.TextButton(leftPos + RocketPadMenu.ROCKET_X - 6,
				topPos + RocketPadMenu.ROCKET_Y + 24, 52, 16, theme, Component.translatable("screen.jugcraft.rocket_pad.launch"), b -> launch()));
		launch.setTooltip(Tooltip.create(Component.translatable("screen.jugcraft.rocket_pad.launch_help")));
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
		Slot slot = hoveredSlot;
		if (slot != null && !slot.hasItem() && slot.index < CONTAINER_SLOTS) {
			String role = slot.index == RocketPadBlockEntity.ROCKET ? "rocket" : slot.index == RocketPadBlockEntity.PLAN ? "plan" : "cargo";
			graphics.setTooltipForNextFrame(font, Component.translatable("screen.jugcraft.rocket_pad.slot." + role), mouseX, mouseY);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		ScreenKit.body(graphics, theme, x, y, imageWidth, 78);
		ScreenKit.pane(graphics, theme, x + 3, y + 11, imageWidth - 6, 66);
		ScreenKit.tray(graphics, theme, x, y + 79, imageWidth, imageHeight - 79);
		ScreenKit.inventory(graphics, theme, x + 8, y + 84);
		RocketPadBlockEntity.Result result = menu.result();
		MachineSkin.lamp(graphics, x + 4, y + 3, result == RocketPadBlockEntity.Result.LAUNCHED ? theme.ok()
				: result == RocketPadBlockEntity.Result.NONE ? theme.dim() : theme.warn());
		for (int slot = 0; slot < RocketPadBlockEntity.CARGO; slot++) {
			MachineSkin.slot(graphics, theme, x + RocketPadMenu.CARGO_X + (slot % 3) * 18, y + RocketPadMenu.CARGO_Y + (slot / 3) * 18);
		}
		// The cargo goes into the rocket: an arrow, lit once a rocket is loaded, faint until then.
		boolean loaded = RocketPadBlockEntity.ROCKET < menu.slots.size() && menu.slots.get(RocketPadBlockEntity.ROCKET).hasItem();
		int arrowX = x + RocketPadMenu.CARGO_X + 58;
		int arrowY = y + RocketPadMenu.ROCKET_Y + 8;
		for (int column = 0; column < 24; column++) {
			int half = column < 18 ? 1 : 5 - (column - 18);
			graphics.fill(arrowX + column, arrowY - half, arrowX + column + 1, arrowY + half + 1, loaded ? theme.progress() : theme.dim());
		}
		slot(graphics, RocketPadMenu.ROCKET_X, RocketPadMenu.ROCKET_Y, RocketPadBlockEntity.ROCKET, MachineSkin.Icon.ROCKET);
		slot(graphics, RocketPadMenu.PLAN_X, RocketPadMenu.PLAN_Y, RocketPadBlockEntity.PLAN, MachineSkin.Icon.PLAN);
	}

	/** A slot's frame, with a faint hint of what goes there while it is empty. */
	private void slot(GuiGraphicsExtractor graphics, int slotX, int slotY, int index, MachineSkin.Icon hint) {
		MachineSkin.slot(graphics, theme, leftPos + slotX, topPos + slotY);
		if (index < menu.slots.size() && !menu.slots.get(index).hasItem()) {
			MachineSkin.hint(graphics, theme, leftPos + slotX, topPos + slotY, hint);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title, titleLabelX, titleLabelY, theme.label(), false);
		RocketPadBlockEntity.Result result = menu.result();
		if (result != RocketPadBlockEntity.Result.NONE) {
			ScreenKit.chip(graphics, font, theme, imageWidth - 3, 0, result.message().getString(),
					result == RocketPadBlockEntity.Result.LAUNCHED ? theme.ok() : theme.warn());
		}
	}
}
