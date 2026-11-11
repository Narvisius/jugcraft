package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsOutMenu;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsPayload;
import io.github.jimbozoomer.jugcraft.warehouse.Order;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseActionPayload;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The LIQUIDS OUT screen (part W4; the mockup warehouse_W4_screens): your liquid orders (with how far along), the fill
 * point (the holding tank, what is ready for you, FILL MY BUCKETS, SEND TO PIPES), the OUT flange's mode and the liquid
 * streams (a liquid sent out all the time at a rate, keeping a reserve; HELD at the reserve).
 */
public class LiquidsOutScreen extends AbstractContainerScreen<LiquidsOutMenu> {
	private static final int W = 352, H = 232;
	private static final String[] MODES = {"OFF", "ORDERS", "ORDERS + STREAMS"};
	/** The liquid picked to start a stream with (its place in the stored liquids). */
	private int pick;

	public LiquidsOutScreen(LiquidsOutMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, H);
	}

	private LiquidsPayload data() {
		LiquidsPayload s = WarehouseClient.liquids();
		return s == null || s.in() ? null : s;
	}

	private void send(int action, int arg, ItemStack item, long amount, long extra) {
		ClientPlayNetworking.send(new WarehouseActionPayload(menu.panel, action, item, amount, arg, extra, ""));
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
		LiquidsPayload d = data();
		WhGui.window(g, font, x, y, x + W, y + H, "LIQUIDS OUT", "the fill point's screen");
		if (d != null) {
			WhGui.chip(g, font, x + W - 9, y + 4, d.linked() ? "EMPTYING STATION" : "NO EMPTYING STATION", d.linked() ? WhGui.GREEN : WhGui.AMBER);
		}
		// Your orders.
		int lx0 = x + 8, lx1 = x + 178;
		WhGui.box(g, font, lx0, y + 26, lx1, y + 170, "YOUR LIQUID ORDERS", "");
		int yy = y + 40;
		int shown = 0;
		if (d != null) {
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (o.state() >= Order.DONE || yy > y + 148) {
					continue;
				}
				g.item(WhGui.icon(o.item()), lx0 + 5, yy);
				g.text(font, WhGui.fit(font, "#" + o.id() + "  " + WhGui.name(o.item()), 92), lx0 + 24, yy, WhGui.WHITE, false);
				g.text(font, Drums.buckets(o.amount()), lx0 + 24, yy + 10, WhGui.GREY, false);
				String st = o.state() == Order.PICKING ? "PUMPING" : Order.STATES[o.state()];
				int colour = o.state() == Order.READY ? WhGui.GREEN : o.state() == Order.QUEUED ? WhGui.GREY : o.state() == Order.SENT ? WhGui.CYAN : WhGui.AMBER;
				g.text(font, st, lx1 - 6 - font.width(st), yy, colour, false);
				WhGui.bar(g, lx1 - 70, yy + 12, lx1 - 6, yy + 16, o.progress(), colour);
				yy += 22;
				shown++;
			}
		}
		if (shown == 0) {
			g.text(font, "No liquid orders.", lx0 + 6, y + 42, WhGui.DIM, false);
		}
		g.text(font, "Order liquids at the warehouse", lx0 + 6, y + 176, WhGui.DIM, false);
		g.text(font, "terminal: STOCK, LIQUIDS, REQUEST.", lx0 + 6, y + 186, WhGui.DIM, false);
		g.text(font, "Or hold an empty bucket under the", lx0 + 6, y + 200, WhGui.DIM, false);
		g.text(font, "nozzle and use it.", lx0 + 6, y + 210, WhGui.DIM, false);
		// The fill point.
		int rx0 = x + 186, rx1 = x + 344;
		WhGui.box(g, font, rx0, y + 26, rx1, y + 106, "FILL POINT", "");
		g.text(font, "Holding tank", rx0 + 6, y + 40, WhGui.GREY, false);
		long tank = d == null ? 0 : d.tank(), cap = d == null || d.tankCap() == 0 ? 1 : d.tankCap();
		WhGui.bar(g, rx0 + 6, y + 51, rx1 - 6, y + 57, tank / (double) cap, WhGui.CYAN);
		String t = Drums.buckets(tank) + " / " + Drums.buckets(cap) + (d == null || d.tankLiquid().isBlank() ? ""
				: "  " + Drums.name(d.tankLiquid()).getString().toUpperCase(Locale.ROOT));
		g.text(font, WhGui.fit(font, t, rx1 - rx0 - 12), rx0 + 6, y + 60, WhGui.WHITE, false);
		long mine = d == null ? 0 : d.mine();
		g.text(font, "Ready for you", rx0 + 6, y + 73, WhGui.GREY, false);
		g.text(font, mine > 0 ? Drums.buckets(mine) : "-", rx1 - 6 - font.width(mine > 0 ? Drums.buckets(mine) : "-"), y + 73, mine > 0 ? WhGui.GREEN : WhGui.DIM,
				false);
		int mid = (rx0 + rx1) / 2;
		WhGui.button(g, font, rx0 + 6, y + 86, mid - 2, y + 100, "FILL MY BUCKETS", mine > 0, WhGui.in(mouseX, mouseY, rx0 + 6, y + 86, mid - 2, y + 100), mine > 0);
		WhGui.button(g, font, mid + 2, y + 86, rx1 - 6, y + 100, "SEND TO PIPES", false, WhGui.in(mouseX, mouseY, mid + 2, y + 86, rx1 - 6, y + 100), mine > 0);
		// The OUT flange.
		WhGui.box(g, font, rx0, y + 110, rx1, y + 226, "OUT FLANGE", d != null && d.flow() > 0 ? Drums.buckets(d.flow()) + "/s" : "to pipes");
		int mode = d == null ? 0 : d.mode();
		int bw = (rx1 - rx0 - 12 - 6) / 3;
		for (int k = 0; k < 3; k++) {
			int bx = rx0 + 6 + k * (bw + 3);
			WhGui.button(g, font, bx, y + 124, bx + bw, y + 136, WhGui.fit(font, MODES[k], bw - 4), mode == k, WhGui.in(mouseX, mouseY, bx, y + 124, bx + bw, y + 136), true);
		}
		g.text(font, "STREAMS", rx0 + 6, y + 142, WhGui.CYAN, false);
		g.text(font, "B/s", rx0 + 80, y + 142, WhGui.GREY, false);
		g.text(font, "KEEP", rx0 + 112, y + 142, WhGui.GREY, false);
		yy = y + 154;
		int n = d == null ? 0 : d.streams().size();
		for (int k = 0; k < n; k++) {
			LiquidsPayload.LiquidStream st = d.streams().get(k);
			g.text(font, WhGui.fit(font, WhGui.name(st.liquid()), 56), rx0 + 6, yy, WhGui.WHITE, false);
			stepper(g, mouseX, mouseY, rx0 + 66, yy, String.format(Locale.ROOT, "%.1f", st.rate() / 1000.0));
			stepper(g, mouseX, mouseY, rx0 + 104, yy, Drums.buckets(st.reserve()));
			String state = mode != 2 ? "PAUSED" : st.held() ? "HELD" : "ON";
			g.text(font, "x", rx1 - 12, yy, WhGui.in(mouseX, mouseY, rx1 - 14, yy - 1, rx1 - 5, yy + 8) ? WhGui.RED : WhGui.DIM, false);
			g.text(font, state, rx1 - 16 - font.width(state), yy, mode != 2 || st.held() ? WhGui.AMBER : WhGui.GREEN, false);
			yy += 12;
		}
		if (n < TerminalBlockEntity.MAX_STREAMS && d != null && !d.stocked().isEmpty()) {
			LiquidsPayload.Stocked s = d.stocked().get(Math.floorMod(pick, d.stocked().size()));
			g.text(font, "<", rx0 + 6, yy, WhGui.GREY, false);
			g.text(font, WhGui.fit(font, WhGui.name(s.liquid()), 70), rx0 + 16, yy, WhGui.WHITE, false);
			g.text(font, ">", rx0 + 90, yy, WhGui.GREY, false);
			WhGui.button(g, font, rx1 - 50, yy - 2, rx1 - 6, yy + 9, "+ ADD", false, WhGui.in(mouseX, mouseY, rx1 - 50, yy - 2, rx1 - 6, yy + 9), true);
		}
	}

	private void stepper(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, String value) {
		g.text(font, "-", x, y, WhGui.in(mouseX, mouseY, x - 2, y - 1, x + 6, y + 8) ? WhGui.WHITE : WhGui.GREY, false);
		g.text(font, value, x + 8, y, WhGui.CYAN, false);
		int px = x + 10 + font.width(value);
		g.text(font, "+", px, y, WhGui.in(mouseX, mouseY, px - 2, y - 1, px + 7, y + 8) ? WhGui.WHITE : WhGui.GREY, false);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int x = leftPos, y = topPos, rx0 = x + 186, rx1 = x + 344, mid = (rx0 + rx1) / 2;
		LiquidsPayload d = data();
		if (WhGui.in(mx, my, rx0 + 6, y + 86, mid - 2, y + 100) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LiquidsOutMenu.FILL_ALL);
			return true;
		}
		if (WhGui.in(mx, my, mid + 2, y + 86, rx1 - 6, y + 100) && d != null) {
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (o.state() == Order.READY && o.dest() == Order.WINDOW) {
					send(WarehouseActionPayload.LIQUID_TO_PIPES, o.id(), ItemStack.EMPTY, 0, 0);
				}
			}
			return true;
		}
		int bw = (rx1 - rx0 - 12 - 6) / 3;
		for (int k = 0; k < 3; k++) {
			int bx = rx0 + 6 + k * (bw + 3);
			if (WhGui.in(mx, my, bx, y + 124, bx + bw, y + 136)) {
				send(WarehouseActionPayload.LIQUID_MODE, k, ItemStack.EMPTY, 0, 0);
				return true;
			}
		}
		boolean shift = minecraft != null && minecraft.hasShiftDown();
		int yy = y + 154;
		int n = d == null ? 0 : d.streams().size();
		for (int k = 0; k < n; k++) {
			LiquidsPayload.LiquidStream st = d.streams().get(k);
			String rate = String.format(Locale.ROOT, "%.1f", st.rate() / 1000.0), keep = Drums.buckets(st.reserve());
			int rp = rx0 + 66 + 10 + font.width(rate), kp = rx0 + 104 + 10 + font.width(keep);
			int rs = shift ? 1000 : 100;
			long ks = shift ? 80_000 : 20_000;
			if (WhGui.in(mx, my, rx0 + 64, yy - 1, rx0 + 72, yy + 8)) {
				send(WarehouseActionPayload.LSTREAM_SET, st.index(), ItemStack.EMPTY, st.rate() - rs, st.reserve());
				return true;
			}
			if (WhGui.in(mx, my, rp - 2, yy - 1, rp + 7, yy + 8)) {
				send(WarehouseActionPayload.LSTREAM_SET, st.index(), ItemStack.EMPTY, st.rate() + rs, st.reserve());
				return true;
			}
			if (WhGui.in(mx, my, rx0 + 102, yy - 1, rx0 + 110, yy + 8)) {
				send(WarehouseActionPayload.LSTREAM_SET, st.index(), ItemStack.EMPTY, st.rate(), Math.max(0, st.reserve() - ks));
				return true;
			}
			if (WhGui.in(mx, my, kp - 2, yy - 1, kp + 7, yy + 8)) {
				send(WarehouseActionPayload.LSTREAM_SET, st.index(), ItemStack.EMPTY, st.rate(), st.reserve() + ks);
				return true;
			}
			if (WhGui.in(mx, my, rx1 - 14, yy - 1, rx1 - 5, yy + 8)) {
				send(WarehouseActionPayload.LSTREAM_REMOVE, st.index(), ItemStack.EMPTY, 0, 0);
				return true;
			}
			yy += 12;
		}
		if (n < TerminalBlockEntity.MAX_STREAMS && d != null && !d.stocked().isEmpty()) {
			if (WhGui.in(mx, my, rx0 + 4, yy - 1, rx0 + 12, yy + 8)) {
				pick--;
				return true;
			}
			if (WhGui.in(mx, my, rx0 + 88, yy - 1, rx0 + 96, yy + 8)) {
				pick++;
				return true;
			}
			if (WhGui.in(mx, my, rx1 - 50, yy - 2, rx1 - 6, yy + 9)) {
				LiquidsPayload.Stocked s = d.stocked().get(Math.floorMod(pick, d.stocked().size()));
				send(WarehouseActionPayload.STREAM_ADD, 0, s.liquid(), 1000, 0);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
