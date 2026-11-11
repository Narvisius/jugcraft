package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.SignalBoxBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.SignalBoxMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/**
 * Part W5: the Warehouse Signal Box's screen (the mockup gui_w5_screens): OUTPUT or INPUT, its rule, the item (using
 * the box with an item sets it) and the number for the stock rules, the signal it gives or gets now.
 */
public class SignalBoxScreen extends AbstractContainerScreen<SignalBoxMenu> {
	private static final int W = 300, H = 196;
	private static final int[] STEPS = {-100, -10, -1, 1, 10, 100};

	public SignalBoxScreen(SignalBoxMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, H);
	}

	private @Nullable SignalBoxBlockEntity box() {
		return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.box) instanceof SignalBoxBlockEntity b ? b : null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		extractTooltip(g, mouseX, mouseY);
	}

	private boolean stockRule(SignalBoxBlockEntity b) {
		return b.mode() == SignalBoxBlockEntity.OUTPUT && b.rule() <= SignalBoxBlockEntity.STOCK_LEVEL;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int x = leftPos, y = topPos;
		SignalBoxBlockEntity b = box();
		WhGui.window(g, font, x, y, x + W, y + H, "SIGNAL BOX", "warehouse I/O  -  one rule a box");
		if (b == null) {
			return;
		}
		WhGui.chip(g, font, x + W - 9, y + 4, b.linked() ? "WAREHOUSE LINKED" : "NO WAREHOUSE", b.linked() ? WhGui.GREEN : WhGui.AMBER);
		int bx0 = x + 8, bx1 = x + W - 8;
		WhGui.box(g, font, bx0, y + 26, bx1, y + 112, "RULE", WhGui.fit(font, b.describe(), 160));
		String[] modes = {"OUTPUT", "INPUT"};
		for (int k = 0; k < 2; k++) {
			int mx = bx0 + 6 + k * 68;
			WhGui.button(g, font, mx, y + 40, mx + 64, y + 52, modes[k], b.mode() == k, WhGui.in(mouseX, mouseY, mx, y + 40, mx + 64, y + 52), true);
		}
		String[] rules = b.mode() == SignalBoxBlockEntity.INPUT ? SignalBoxBlockEntity.IN_RULES : SignalBoxBlockEntity.OUT_RULES;
		for (int k = 0; k < rules.length; k++) {
			int rx = bx0 + 6 + (k % 4) * 70, ry = y + 58 + (k / 4) * 15;
			WhGui.button(g, font, rx, ry, rx + 67, ry + 13, WhGui.fit(font, rules[k], 64), b.rule() == k, WhGui.in(mouseX, mouseY, rx, ry, rx + 67, ry + 13), true);
		}
		g.text(font, b.mode() == SignalBoxBlockEntity.INPUT ? "While the box gets redstone, the warehouse does this." : "The box gives this as redstone, from every side.",
				bx0 + 6, y + 98, WhGui.GREY, false);
		// The item and the number (the stock rules).
		WhGui.box(g, font, bx0, y + 116, bx1, y + 160, "ITEM", stockRule(b) ? "use the box with an item to set it" : "");
		boolean stock = stockRule(b);
		WhGui.slotWell(g, bx0 + 8, y + 130);
		if (stock && !b.item().isEmpty()) {
			g.item(b.item(), bx0 + 8, y + 130);
			g.text(font, WhGui.fit(font, b.item().getHoverName().getString(), 80), bx0 + 30, y + 129, WhGui.WHITE, false);
			g.text(font, String.format(Locale.ROOT, "stored: %,d", b.value()), bx0 + 30, y + 140, WhGui.GREY, false);
			WhGui.button(g, font, bx0 + 30, y + 150 - 1, bx0 + 72, y + 158, "CLEAR", false, WhGui.in(mouseX, mouseY, bx0 + 30, y + 149, bx0 + 72, y + 158), true);
		} else {
			g.text(font, stock ? "no item yet" : "-", bx0 + 30, y + 134, WhGui.DIM, false);
		}
		int nx = bx0 + 122;
		g.text(font, b.rule() == SignalBoxBlockEntity.STOCK_LEVEL ? "FULL AT" : b.rule() == SignalBoxBlockEntity.STOCK_ABOVE ? "ABOVE" : "BELOW", nx, y + 128,
				stock ? WhGui.GREY : WhGui.DIM, false);
		g.fill(nx + 46, y + 126, nx + 158, y + 138, WhGui.WELL);
		g.outline(nx + 46, y + 126, 112, 12, WhGui.CYAN_DIM);
		String n = String.format(Locale.ROOT, "%,d", b.threshold());
		g.text(font, n, nx + 102 - font.width(n) / 2, y + 128, stock ? WhGui.WHITE : WhGui.DIM, false);
		for (int k = 0; k < STEPS.length; k++) {
			int sx = nx + k * 27;
			String s = (STEPS[k] > 0 ? "+" : "") + STEPS[k];
			WhGui.button(g, font, sx, y + 142, sx + 25, y + 154, s, false, WhGui.in(mouseX, mouseY, sx, y + 142, sx + 25, y + 154), stock);
		}
		// The signal now.
		boolean lit = b.mode() == SignalBoxBlockEntity.OUTPUT ? b.output() > 0 : b.input();
		g.fill(bx0, y + 164, bx1, y + H - 8, lit ? 0xFF12281A : 0xFF0C141E);
		g.outline(bx0, y + 164, bx1 - bx0, H - 8 - 164, lit ? WhGui.GREEN : WhGui.LINE);
		String sig = b.mode() == SignalBoxBlockEntity.OUTPUT ? (lit ? "SIGNAL ON  -  " + b.output() + "  (from every side)" : "SIGNAL OFF")
				: (lit ? "GETTING REDSTONE  -  " + SignalBoxBlockEntity.IN_RULES[Math.max(0, Math.min(SignalBoxBlockEntity.IN_RULES.length - 1, b.rule()))]
				: "NO REDSTONE IN");
		g.text(font, sig, bx0 + 8, y + 172, lit ? WhGui.GREEN : WhGui.GREY, false);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		SignalBoxBlockEntity b = box();
		if (b == null) {
			return super.mouseClicked(event, doubleClick);
		}
		int x = leftPos, y = topPos, bx0 = x + 8;
		for (int k = 0; k < 2; k++) {
			int bx = bx0 + 6 + k * 68;
			if (WhGui.in(mx, my, bx, y + 40, bx + 64, y + 52)) {
				press(k == 0 ? SignalBoxMenu.OUTPUT : SignalBoxMenu.INPUT);
				return true;
			}
		}
		String[] rules = b.mode() == SignalBoxBlockEntity.INPUT ? SignalBoxBlockEntity.IN_RULES : SignalBoxBlockEntity.OUT_RULES;
		for (int k = 0; k < rules.length; k++) {
			int rx = bx0 + 6 + (k % 4) * 70, ry = y + 58 + (k / 4) * 15;
			if (WhGui.in(mx, my, rx, ry, rx + 67, ry + 13)) {
				press(SignalBoxMenu.RULE + k);
				return true;
			}
		}
		if (stockRule(b)) {
			if (!b.item().isEmpty() && WhGui.in(mx, my, bx0 + 30, y + 149, bx0 + 72, y + 158)) {
				press(SignalBoxMenu.CLEAR);
				return true;
			}
			int nx = bx0 + 122;
			for (int k = 0; k < STEPS.length; k++) {
				int sx = nx + k * 27;
				if (WhGui.in(mx, my, sx, y + 142, sx + 25, y + 154)) {
					long next = Math.max(0, Math.min(1_000_000_000L, b.threshold() + STEPS[k] * (minecraft != null && minecraft.hasShiftDown() ? 64L : 1L)));
					press(SignalBoxMenu.NUMBER + (int) next);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
