package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.ControlPayload;
import io.github.jimbozoomer.jugcraft.warehouse.SwitchboardBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.SwitchboardMenu;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePower;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/**
 * Part W5: the Warehouse Switchboard's screen (the mockup gui_w5_screens): INCOMER (what comes in by cable, what the
 * warehouse draws, what is stored, the main breaker and MAIN OFF) and OUTGOING (the six circuits: click one to switch it;
 * its lamp green on, amber shed, dark off; what it draws now).
 */
public class SwitchboardScreen extends AbstractContainerScreen<SwitchboardMenu> {
	private static final int W = 300, H = 222;

	public SwitchboardScreen(SwitchboardMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, H);
	}

	private @Nullable SwitchboardBlockEntity board() {
		return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.board) instanceof SwitchboardBlockEntity sb ? sb : null;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		extractTooltip(g, mouseX, mouseY);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int x = leftPos, y = topPos;
		SwitchboardBlockEntity sb = board();
		ControlPayload c = sb == null ? null : WarehouseClient.control(sb.terminalPos());
		WhGui.window(g, font, x, y, x + W, y + H, "SWITCHBOARD", "main distribution board");
		if (sb == null) {
			return;
		}
		WhGui.chip(g, font, x + W - 9, y + 4, sb.linked() ? "WAREHOUSE LINKED" : "NO WAREHOUSE", sb.linked() ? WhGui.GREEN : WhGui.AMBER);
		// The incomer.
		int bx0 = x + 8, bx1 = x + W - 8, iy0 = y + 26, iy1 = y + 92;
		WhGui.box(g, font, bx0, iy0, bx1, iy1, "INCOMER", "service entrance");
		long capacity = c == null ? WarehousePower.CAPACITY : Math.max(WarehousePower.CAPACITY, c.capacity());
		String[][] rows = {{"Cable in", String.format(Locale.ROOT, "%,d JE/t", sb.supply())}, {"Draw", String.format(Locale.ROOT, "%,d JE/t", sb.draw())},
				{"Stored", sb.storedPct() + " %  of " + String.format(Locale.ROOT, "%,d JE", WarehousePower.CAPACITY)},
				{"Main breaker", sb.mainOn() ? "CLOSED" : "OPEN"}};
		for (int k = 0; k < rows.length; k++) {
			int ry = iy0 + 14 + k * 10;
			g.text(font, rows[k][0], bx0 + 6, ry, WhGui.GREY, false);
			int colour = k == 0 ? WhGui.CYAN : k == 3 ? (sb.mainOn() ? WhGui.GREEN : WhGui.RED) : k == 2 ? WhGui.GREEN : WhGui.WHITE;
			g.text(font, rows[k][1], bx0 + 150 - font.width(rows[k][1]), ry, colour, false);
		}
		WhGui.bar(g, bx0 + 160, iy0 + 35, bx1 - 6, iy0 + 40, sb.storedPct() / 100.0, sb.storedPct() < 10 ? WhGui.AMBER : WhGui.GREEN);
		boolean hover = WhGui.in(mouseX, mouseY, bx0 + 160, iy0 + 46, bx1 - 6, iy0 + 60);
		g.fill(bx0 + 160, iy0 + 46, bx1 - 6, iy0 + 60, sb.mainOn() ? 0xFF1A0C0C : 0xFF0C1A10);
		g.outline(bx0 + 160, iy0 + 46, bx1 - 6 - bx0 - 160, 14, hover ? (sb.mainOn() ? WhGui.RED : WhGui.GREEN) : 0xFF3A2A26);
		String main = sb.mainOn() ? "MAIN OFF (STOP ALL)" : "MAIN ON";
		g.text(font, main, (bx0 + 160 + bx1 - 6) / 2 - font.width(main) / 2, iy0 + 49, sb.mainOn() ? WhGui.RED : WhGui.GREEN, false);
		if (capacity > WarehousePower.CAPACITY && c != null) {
			g.text(font, c.boards() + " boards", bx0 + 160, iy0 + 14, WhGui.GREY, false);
		}
		// The outgoing circuits.
		int oy0 = y + 96, oy1 = y + H - 8;
		WhGui.box(g, font, bx0, oy0, bx1, oy1, "OUTGOING", "click to switch a circuit");
		WarehousePower.Circuit[] circuits = WarehousePower.Circuit.values();
		for (int k = 0; k < circuits.length; k++) {
			int ry = oy0 + 14 + k * 17;
			boolean on = (sb.circuits() >> k & 1) != 0, shed = (sb.shed() >> k & 1) != 0;
			boolean over = WhGui.in(mouseX, mouseY, bx0 + 4, ry, bx1 - 4, ry + 15);
			g.fill(bx0 + 4, ry, bx1 - 4, ry + 15, over ? WhGui.SELECT : 0xFF0C141E);
			g.outline(bx0 + 4, ry, bx1 - bx0 - 8, 15, over ? WhGui.CYAN : 0xFF1E3244);
			// The breaker's toggle: up on, down off.
			g.fill(bx0 + 8, ry + 2, bx0 + 16, ry + 13, 0xFF1E2226);
			g.outline(bx0 + 8, ry + 2, 8, 11, WhGui.GREY);
			g.fill(bx0 + 10, on ? ry + 3 : ry + 8, bx0 + 14, on ? ry + 7 : ry + 12, WhGui.WHITE);
			g.text(font, circuits[k].label, bx0 + 22, ry + 1, WhGui.WHITE, false);
			tinyText(g, circuits[k].what, bx0 + 22, ry + 10, WhGui.GREY);
			String load = (c == null ? 0 : c.load(k)) + " JE/t";
			g.text(font, load, bx1 - 70 - font.width(load), ry + 4, WhGui.WHITE, false);
			int lamp = !on ? 0xFF2A3038 : shed ? WhGui.AMBER : WhGui.GREEN;
			g.fill(bx1 - 62, ry + 4, bx1 - 55, ry + 11, lamp);
			String st = !on ? "OFF" : shed ? "SHED" : "ON";
			g.text(font, st, bx1 - 8 - font.width(st), ry + 4, !on ? WhGui.DIM : shed ? WhGui.AMBER : WhGui.GREEN, false);
		}
	}

	private void tinyText(GuiGraphicsExtractor g, String text, int x, int y, int colour) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.5F, 0.5F);
		g.text(font, text, 0, 0, colour, false);
		g.pose().popMatrix();
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
		int x = leftPos, y = topPos, bx0 = x + 8, bx1 = x + W - 8;
		if (WhGui.in(mx, my, bx0 + 160, y + 26 + 46, bx1 - 6, y + 26 + 60)) {
			press(SwitchboardMenu.MAIN);
			return true;
		}
		for (int k = 0; k < WarehousePower.Circuit.values().length; k++) {
			int ry = y + 96 + 14 + k * 17;
			if (WhGui.in(mx, my, bx0 + 4, ry, bx1 - 4, ry + 15)) {
				press(k);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
