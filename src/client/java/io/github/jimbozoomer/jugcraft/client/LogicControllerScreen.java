package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.control.Channels;
import io.github.jimbozoomer.jugcraft.control.LogicControllerBlockEntity;
import io.github.jimbozoomer.jugcraft.control.LogicControllerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/**
 * The logic controller (batch 36), in the advanced look (docs/features/machine-screens.md): eight rules, one a row
 * ("[n] IF <sensor channel> <below/above> N% THEN <target channel> ON/OFF") under a heading row, the rules that are on
 * lit; along the bottom every channel's swatch, its reading and whether this controller has it on (hover a swatch for
 * its name); and a help strip. Each part of a rule is a key with a tooltip; the server keeps the rules
 * ({@link LogicControllerMenu}). The status lamp is lit while the controller has any channel on.
 */
public class LogicControllerScreen extends AbstractContainerScreen<LogicControllerMenu> implements ScreenKit.Fitted {
	/** The channel colours, as drawn on the sensor and relay lamps (tools/control_electronics.py CHANNEL_COLORS). */
	public static final int[] COLORS = {0xF0F0F0, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
			0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};
	private static final int ROW = 18;
	private static final int TOP = 29;
	private static final int STRIP = 176;
	/** Each key of a rule: its left edge and width (the rule number, sensor, compare, -, +, target, on/off). */
	private static final int[][] KEYS = {{8, 20}, {48, 60}, {110, 20}, {134, 14}, {182, 14}, {222, 48}, {272, 22}};
	private static final String[] TIPS = {"enable", "sensor", "compare", "down", "up", "target", "action"};
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("logic_controller");
	private final ScreenKit.TextButton[][] buttons = new ScreenKit.TextButton[LogicControllerBlockEntity.RULES][];

	public LogicControllerScreen(LogicControllerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 300, 236);
	}

	@Override
	protected void init() {
		super.init();
		titleLabelX = 12;
		titleLabelY = 2;
		inventoryLabelY = 10_000;
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			int y = topPos + TOP + rule * ROW;
			int base = rule * 8;
			buttons[rule] = new ScreenKit.TextButton[KEYS.length];
			for (int key = 0; key < KEYS.length; key++) {
				int id = base + key;
				ScreenKit.TextButton button = addRenderableWidget(new ScreenKit.TextButton(leftPos + KEYS[key][0], y, KEYS[key][1], 16, theme,
						Component.empty(), b -> click(id)));
				button.setTooltip(Tooltip.create(Component.translatable("screen.jugcraft.logic_controller.tip." + TIPS[key])));
				buttons[rule][key] = button;
			}
		}
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
		int availableRows=LogicControllerBlockEntity.RULES;
		if(minecraft!=null&&minecraft.level!=null) {
			var state=minecraft.level.getBlockState(menu.pos);
			if(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialAutomation.Gate gate)
				availableRows=Math.max(1,gate.rows/(state.getValue(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialAutomation.Gate.MODIFIER)>=2?2:1));
		}
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			for(var button:buttons[rule])button.active=rule<availableRows;
			boolean enabled = menu.rule(rule, LogicControllerBlockEntity.ENABLED) == 1;
			buttons[rule][0].setMessage(Component.literal(String.valueOf(rule + 1)));
			buttons[rule][0].setSelected(enabled);
			buttons[rule][1].setMessage(channel(menu.rule(rule, LogicControllerBlockEntity.SENSOR)));
			buttons[rule][2].setMessage(Component.literal(menu.rule(rule, LogicControllerBlockEntity.ABOVE) == 1 ? ">" : "<"));
			buttons[rule][3].setMessage(Component.literal("-"));
			buttons[rule][4].setMessage(Component.literal("+"));
			buttons[rule][5].setMessage(channel(menu.rule(rule, LogicControllerBlockEntity.TARGET)));
			boolean on = menu.rule(rule, LogicControllerBlockEntity.ACTION) == 1;
			buttons[rule][6].setMessage(Component.translatable(on ? "screen.jugcraft.logic_controller.on" : "screen.jugcraft.logic_controller.off"));
			buttons[rule][6].setSelected(on);
		}
		extractBackground(graphics, mouseX, mouseY, delta);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		// A channel swatch hovered: its name, its reading and whether this controller has it on.
		int mx = mouseX - leftPos;
		int my = mouseY - topPos;
		if (my >= STRIP + 4 && my < STRIP + 28) {
			int channel = (mx - 8) / 18;
			if (mx >= 8 && channel >= 0 && channel < Channels.COUNT && (mx - 8) % 18 < 14) {
				int reading = menu.reading(channel);
				graphics.setTooltipForNextFrame(font, channel(channel).copy().append(": " + (reading < 0 ? "-" : reading + "%")
						+ (menu.isOn(channel) ? " · " + Component.translatable("screen.jugcraft.logic_controller.on").getString() : "")),
						mouseX, mouseY);
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		int x = leftPos;
		int y = topPos;
		ScreenKit.body(graphics, theme, x, y, imageWidth, imageHeight);
		boolean anyOn = false;
		for (int channel = 0; channel < Channels.COUNT; channel++) {
			anyOn |= menu.isOn(channel);
		}
		MachineSkin.lamp(graphics, x + 4, y + 3, anyOn ? theme.ok() : theme.dim());
		ScreenKit.pane(graphics, theme, x + 4, y + 13, imageWidth - 8, STRIP - 16);
		graphics.fill(x + 8, y + 26, x + imageWidth - 8, y + 27, (theme.dim() & 0x00FFFFFF) | 0x90000000);
		// The rules that are on stand out behind their keys.
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			if (menu.rule(rule, LogicControllerBlockEntity.ENABLED) == 1) {
				int ry = y + TOP + rule * ROW;
				graphics.fill(x + 6, ry - 1, x + imageWidth - 6, ry + 17, (theme.text() & 0x00FFFFFF) | 0x14000000);
			}
		}
		// The channel strip: a swatch per channel, lit under while this controller has it on.
		ScreenKit.pane(graphics, theme, x + 4, y + STRIP, imageWidth - 8, 30);
		for (int channel = 0; channel < Channels.COUNT; channel++) {
			int sx = x + 8 + channel * 18;
			graphics.fill(sx - 1, y + STRIP + 4, sx + 15, y + STRIP + 13, theme.slotDark());
			graphics.fill(sx, y + STRIP + 5, sx + 14, y + STRIP + 12, 0xFF000000 | COLORS[channel]);
			if (menu.isOn(channel)) {
				graphics.fill(sx + 4, y + STRIP + 14, sx + 10, y + STRIP + 16, theme.ok());
			}
		}
		ScreenKit.help(graphics, font, theme, x + 4, y + STRIP + 33, imageWidth - 8, imageHeight - STRIP - 37,
				Component.translatable("screen.jugcraft.logic_controller.help"));
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, title.getString(), titleLabelX, titleLabelY, theme.label(), false);
		// The heading row, each over its column.
		heading(graphics, "rule", KEYS[0][0], KEYS[0][1]);
		heading(graphics, "sensor", KEYS[1][0], KEYS[1][1]);
		heading(graphics, "level", KEYS[3][0], KEYS[4][0] + KEYS[4][1] - KEYS[3][0]);
		heading(graphics, "target", KEYS[5][0], KEYS[5][1]);
		heading(graphics, "set", KEYS[6][0], KEYS[6][1]);
		for (int rule = 0; rule < LogicControllerBlockEntity.RULES; rule++) {
			int y = TOP + rule * ROW + 4;
			int color = menu.rule(rule, LogicControllerBlockEntity.ENABLED) == 1 ? theme.text() : theme.dim();
			graphics.text(font, Component.translatable("screen.jugcraft.logic_controller.if").getString(), 32, y, theme.dim(), false);
			String level = menu.rule(rule, LogicControllerBlockEntity.THRESHOLD) + "%";
			graphics.text(font, level, 165 - font.width(level) / 2, y, color, false);
			graphics.text(font, Component.translatable("screen.jugcraft.logic_controller.then").getString(), 199, y, theme.dim(), false);
		}
		int y = STRIP + 18;
		for (int channel = 0; channel < Channels.COUNT; channel++) {
			int reading = menu.reading(channel);
			String text = reading < 0 ? "-" : String.valueOf(reading);
			graphics.text(font, text, 8 + channel * 18 + 7 - font.width(text) / 2, y, reading < 0 ? theme.dim() : theme.text(), false);
		}
	}

	private void heading(GuiGraphicsExtractor graphics, String key, int x, int width) {
		String text = Component.translatable("screen.jugcraft.logic_controller." + key).getString();
		graphics.text(font, text, x + (width - font.width(text)) / 2, 16, theme.dim(), false);
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(leftPos, topPos, imageWidth, imageHeight);
	}
}
