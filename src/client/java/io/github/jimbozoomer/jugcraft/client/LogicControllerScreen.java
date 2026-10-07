package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.control.Channels;
import io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity;
import io.github.jimbozoomer.jugcraft.control.LogicControllerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * The logic controller (batch 36): eight rules, one a row ("[n] IF <sensor channel> <below/above> N% THEN <target
 * channel> ON/OFF"), and along the bottom every channel's reading and whether this controller has it on. Each part of a
 * rule is a button; the server keeps the rules ({@link LogicControllerMenu}).
 */
public class LogicControllerScreen extends AbstractContainerScreen<LogicControllerMenu> {
	/** The channel colours, as drawn on the sensor and relay lamps (tools/control_electronics.py CHANNEL_COLORS). */
	public static final int[] COLORS = {0xF0F0F0, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
			0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};
	private static final int BEZEL = 0xFF2A3036;
	private static final int SCREEN = 0xFF0E2A2E;
	private static final int TEXT = 0xFF7FE6F0;
	private static final int DIM = 0xFF3C9AA8;
	private static final int ROW = 20;
	private static final int TOP = 24;
	private final Button[][] buttons = new Button[LogicControllerBlockEntity.RULES][];

	public LogicControllerScreen(LogicControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 300, 226);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 8;
		titleLabelY = 8;
		inventoryLabelY = 10_000;
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			int y = topPos + TOP + rule * ROW;
			int base = rule * 8;
			buttons[rule] = new Button[] {
					button(base, leftPos + 8, y, 20),
					button(base + 1, leftPos + 48, y, 60),
					button(base + 2, leftPos + 110, y, 20),
					button(base + 3, leftPos + 134, y, 14),
					button(base + 4, leftPos + 182, y, 14),
					button(base + 5, leftPos + 222, y, 48),
					button(base + 6, leftPos + 272, y, 22)};
		}
	}

	private Button button(int id, int x, int y, int width) {
		return addRenderableWidget(Button.builder(Component.empty(), b -> click(id)).bounds(x, y, width, 16).build());
	}

	private void click(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	private static Component channel(int channel) {
		return Channels.name(DyeColor.values()[channel]).copy().withColor(COLORS[channel]);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			boolean enabled = menu.rule(rule, LogicControllerBlockEntity.ENABLED) == 1;
			buttons[rule][0].setMessage(Component.literal(String.valueOf(rule + 1)).withColor(enabled ? 0x80FF90 : 0x707070));
			buttons[rule][1].setMessage(channel(menu.rule(rule, LogicControllerBlockEntity.SENSOR)));
			buttons[rule][2].setMessage(Component.literal(menu.rule(rule, LogicControllerBlockEntity.ABOVE) == 1 ? ">" : "<"));
			buttons[rule][3].setMessage(Component.literal("-"));
			buttons[rule][4].setMessage(Component.literal("+"));
			buttons[rule][5].setMessage(channel(menu.rule(rule, LogicControllerBlockEntity.TARGET)));
			buttons[rule][6].setMessage(Component.translatable(menu.rule(rule, LogicControllerBlockEntity.ACTION) == 1
					? "screen.jugcraft.logic_controller.on" : "screen.jugcraft.logic_controller.off"));
		}
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BEZEL);
		graphics.fill(leftPos + 3, topPos + 20, leftPos + imageWidth - 3, topPos + imageHeight - 3, SCREEN);
		// The channel strip: a swatch per channel, lit while this controller has it on.
		int y = topPos + TOP + LogicControllerBlockEntity.RULES * ROW + 6;
		for (int channel = 0; channel < Channels.COUNT; channel++) {
			int x = leftPos + 8 + channel * 18;
			graphics.fill(x, y, x + 14, y + 8, 0xFF000000 | COLORS[channel]);
			if (menu.isOn(channel)) {
				graphics.fill(x + 5, y + 10, x + 9, y + 13, 0xFF80FF90);
			}
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, 0xFFDDE6EA, false);
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			int y = TOP + rule * ROW + 4;
			graphics.text(font, Component.translatable("screen.jugcraft.logic_controller.if").getString(), 32, y, DIM, false);
			graphics.text(font, menu.rule(rule, LogicControllerBlockEntity.THRESHOLD) + "%", 152, y, TEXT, false);
			graphics.text(font, Component.translatable("screen.jugcraft.logic_controller.then").getString(), 200, y, DIM, false);
		}
		int y = TOP + LogicControllerBlockEntity.RULES * ROW + 20;
		for (int channel = 0; channel < Channels.COUNT; channel++) {
			int reading = menu.reading(channel);
			graphics.text(font, reading < 0 ? "-" : String.valueOf(reading), 8 + channel * 18, y, reading < 0 ? DIM : TEXT, false);
		}
	}
}
