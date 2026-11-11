package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsOutBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsOutMenu;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsOutPayload;
import io.github.jimbozoomer.jugcraft.warehouse.Order;
import io.github.jimbozoomer.jugcraft.warehouse.PortBuffer;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseActionPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Goods Out panel's screens (part W2; the mockups warehouse_ui_goods_out_window_v2, _kiosk and _output_port): READY
 * STORAGE at the pickup window (Ready Storage's slots: take only, MINE or ALL, a search, TAKE ALL; your orders; the
 * window's tray), REQUEST at the kiosk (the stock, an amount, the window or pipes, PLACE ORDER; the line, where you can
 * cancel your own), and the OUTPUT PORT (its mode, its buffer, the streams, the orders going to pipes). The tabs switch
 * between them (the server opens the other page).
 */
public class GoodsOutScreen extends AbstractContainerScreen<GoodsOutMenu> {
	private static final String[] TABS = {"READY STORAGE", "REQUEST", "OUTPUT PORT"};
	private static final String[] SUB = {"PICKUP WINDOW", "ORDER KIOSK", "OUTPUT PORT"};
	private static final String[] MODES = {"OFF", "ORDERS ONLY", "ORDERS + STREAMS"};
	private static final int ROW = 14, ROWS = 13;
	private EditBox search, amount;
	private int scroll, selected = -1, dest = Order.WINDOW;

	public GoodsOutScreen(GoodsOutMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, GoodsOutMenu.WIDTH, GoodsOutMenu.HEIGHT);
	}

	@Override
	protected void init() {
		super.init();
		if (menu.page == GoodsOutBlockEntity.WINDOW) {
			search = addRenderableWidget(new EditBox(font, leftPos + 13, topPos + 45, 70, 10, Component.literal("search")));
			search.setResponder(s -> send(WarehouseActionPayload.SEARCH, 0, ItemStack.EMPTY, 0, 0, s));
		} else if (menu.page == GoodsOutBlockEntity.KIOSK) {
			search = addRenderableWidget(new EditBox(font, leftPos + 13, topPos + 45, 160, 10, Component.literal("search")));
			search.setResponder(s -> {
				scroll = 0;
				selected = -1;
			});
			amount = addRenderableWidget(new EditBox(font, leftPos + 193, topPos + 87, 60, 10, Component.literal("amount")));
			amount.setValue("64");
			amount.setMaxLength(7);
			amount.setBordered(false);
		}
		if (search != null) {
			search.setHint(Component.literal("search...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
			search.setBordered(false);
		}
	}

	private void send(int action, int arg, ItemStack item, long amount, long extra, String text) {
		ClientPlayNetworking.send(new WarehouseActionPayload(menu.panel, action, item, amount, arg, extra, text));
	}

	private GoodsOutPayload data() {
		GoodsOutPayload d = WarehouseClient.goodsOut();
		return d != null && d.panel().equals(menu.panel) ? d : null;
	}

	private String me() {
		return minecraft != null && minecraft.player != null ? minecraft.player.getUUID().toString() : "";
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		extractTooltip(g, mouseX, mouseY);
		if (menu.page == GoodsOutBlockEntity.KIOSK) {
			List<TerminalPayload.Item> items = items();
			for (int k = 0; k < ROWS && scroll + k < items.size(); k++) {
				int ry = topPos + 60 + k * ROW;
				if (WhGui.in(mouseX, mouseY, leftPos + 12, ry, leftPos + 26, ry + 12)) {
					g.setTooltipForNextFrame(font, items.get(scroll + k).item(), mouseX, mouseY);
				}
			}
		}
		if (menu.page == GoodsOutBlockEntity.PORT) {
			GoodsOutPayload d = data();
			if (d != null) {
				for (int k = 0; k < d.buffer().size(); k++) {
					int bx = leftPos + 14 + k * 18, by = topPos + 72;
					if (!d.buffer().get(k).isEmpty() && WhGui.in(mouseX, mouseY, bx, by, bx + 16, by + 16)) {
						g.setTooltipForNextFrame(font, d.buffer().get(k), mouseX, mouseY);
					}
				}
			}
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int x = leftPos, y = topPos;
		GoodsOutPayload d = data();
		WhGui.window(g, font, x, y, x + imageWidth, y + imageHeight, "GOODS OUT", SUB[menu.page]);
		if (d != null && d.poolSlots() > 0) {
			WhGui.chip(g, font, x + imageWidth - 9, y + 4, "READY STORAGE " + Math.round(100.0 * d.poolUsed() / d.poolSlots()) + "%", WhGui.CYAN);
		}
		for (int k = 0; k < TABS.length; k++) {
			int tx = x + 8 + k * 96;
			WhGui.button(g, font, tx, y + 24, tx + 92, y + 36, TABS[k], menu.page == k, WhGui.in(mouseX, mouseY, tx, y + 24, tx + 92, y + 36), true);
		}
		switch (menu.page) {
			case GoodsOutBlockEntity.WINDOW -> window(g, d, mouseX, mouseY);
			case GoodsOutBlockEntity.KIOSK -> kiosk(g, d, mouseX, mouseY);
			default -> port(g, d, mouseX, mouseY);
		}
		if (menu.page != GoodsOutBlockEntity.KIOSK) {
			g.text(font, "INVENTORY", x + 12, y + GoodsOutMenu.INV_Y - 11, WhGui.CYAN, false);
			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 9; c++) {
					WhGui.slotWell(g, x + GoodsOutMenu.INV_X + c * 18, y + GoodsOutMenu.INV_Y + r * 18);
				}
			}
			for (int c = 0; c < 9; c++) {
				WhGui.slotWell(g, x + GoodsOutMenu.INV_X + c * 18, y + GoodsOutMenu.HOTBAR_Y);
			}
		}
	}

	// ------------------------------------------------------------------ READY STORAGE (the pickup window)

	private void window(GuiGraphicsExtractor g, GoodsOutPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		WhGui.box(g, font, x + 8, y + 40, x + 180, y + 182, "", "");
		g.fill(x + 11, y + 43, x + 86, y + 56, WhGui.WELL);
		g.outline(x + 11, y + 43, 75, 13, WhGui.CYAN_DIM);
		boolean mine = d != null && d.mine();
		WhGui.button(g, font, x + 89, y + 43, x + 113, y + 56, "MINE", mine, WhGui.in(mouseX, mouseY, x + 89, y + 43, x + 113, y + 56), true);
		WhGui.button(g, font, x + 115, y + 43, x + 135, y + 56, "ALL", !mine, WhGui.in(mouseX, mouseY, x + 115, y + 43, x + 135, y + 56), true);
		WhGui.button(g, font, x + 137, y + 43, x + 177, y + 56, "TAKE ALL", false, WhGui.in(mouseX, mouseY, x + 137, y + 43, x + 177, y + 56), true);
		for (int r = 0; r < GoodsOutMenu.ROWS; r++) {
			for (int c = 0; c < GoodsOutMenu.COLS; c++) {
				WhGui.slotWell(g, x + GoodsOutMenu.VIEW_X + c * 18, y + GoodsOutMenu.VIEW_Y + r * 18);
			}
		}
		if (d != null) {
			String foot = d.mineStacks() + " stacks yours  -  " + d.poolUsed() + " / " + d.poolSlots() + " slots";
			g.text(font, foot, x + 12, y + 171, WhGui.GREY, false);
		}
		// Your orders.
		int rx0 = x + 186, rx1 = x + 344;
		List<TerminalPayload.OrderLine> mineOrders = new ArrayList<>();
		if (d != null) {
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (o.owner().equals(me()) && o.state() < Order.DONE && o.stream() == 0) {
					mineOrders.add(o);
				}
			}
		}
		WhGui.box(g, font, rx0, y + 40, rx1, y + 150, "YOUR ORDERS", mineOrders.size() + " open");
		int yy = y + 54;
		for (TerminalPayload.OrderLine o : mineOrders) {
			if (yy > y + 128) {
				break;
			}
			orderRow(g, o, rx0 + 4, rx1 - 4, yy, d.time());
			yy += 24;
		}
		if (mineOrders.isEmpty()) {
			g.text(font, "No orders. Order at the kiosk", rx0 + 5, y + 56, WhGui.DIM, false);
			g.text(font, "(the REQUEST tab).", rx0 + 5, y + 67, WhGui.DIM, false);
		}
		// The pickup window.
		WhGui.box(g, font, rx0, y + 156, rx1, y + 272, "PICKUP WINDOW", "");
		if (d != null) {
			row(g, rx0, rx1, y + 172, "Tray", d.trayOut() ? "OUT - open" : "IN", d.trayOut() ? WhGui.GREEN : WhGui.CYAN);
			row(g, rx0, rx1, y + 184, "Coming to the tray", d.next().isEmpty() ? "-" : d.next(), d.next().isEmpty() ? WhGui.DIM : WhGui.CYAN);
		}
		g.text(font, "Click a stack to take it, shift-click", rx0 + 5, y + 206, WhGui.DIM, false);
		g.text(font, "to send it to your inventory.", rx0 + 5, y + 217, WhGui.DIM, false);
		g.text(font, "Nothing can be put in here: bring", rx0 + 5, y + 232, WhGui.DIM, false);
		g.text(font, "items to GOODS IN instead.", rx0 + 5, y + 243, WhGui.DIM, false);
	}

	private void orderRow(GuiGraphicsExtractor g, TerminalPayload.OrderLine o, int x0, int x1, int y, long time) {
		g.fill(x0, y - 2, x1, y + 20, 0xFF0C141E);
		g.outline(x0, y - 2, x1 - x0, 22, o.state() == Order.READY ? WhGui.GREEN : 0xFF1E3244);
		WhGui.smallItem(g, o.item(), x0 + 3, y + 1);
		String what = "#" + o.id() + " " + WhGui.fit(font, o.item().getHoverName().getString(), 70) + String.format(Locale.ROOT, " x%,d", o.amount());
		g.text(font, what, x0 + 14, y, WhGui.WHITE, false);
		int colour = colour(o.state());
		String st = Order.STATES[o.state()];
		g.text(font, st, x1 - 4 - font.width(st), y, colour, false);
		g.text(font, WhGui.fit(font, o.now() + (o.dest() == Order.PIPES ? "  (pipes)" : ""), x1 - x0 - 60), x0 + 14, y + 10, WhGui.GREY, false);
		if (o.state() <= Order.PICKING) {
			WhGui.bar(g, x1 - 44, y + 11, x1 - 4, y + 16, o.progress(), colour);
		}
	}

	private static int colour(int state) {
		return switch (state) {
			case Order.READY -> WhGui.GREEN;
			case Order.QUEUED -> WhGui.GREY;
			case Order.SENT -> WhGui.CYAN;
			case Order.DONE, Order.CANCELLED -> WhGui.DIM;
			default -> WhGui.AMBER;
		};
	}

	private void row(GuiGraphicsExtractor g, int x0, int x1, int y, String label, String value, int colour) {
		g.text(font, label, x0 + 5, y, WhGui.GREY, false);
		g.text(font, value, x1 - 5 - font.width(value), y, colour, false);
	}

	// ------------------------------------------------------------------ REQUEST (the kiosk)

	private List<TerminalPayload.Item> items() {
		TerminalPayload t = WarehouseClient.snapshot(menu.terminal);
		List<TerminalPayload.Item> out = new ArrayList<>();
		if (t == null) {
			return out;
		}
		String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		for (TerminalPayload.Item i : t.items()) {
			// (Liquids are ordered at the terminal, and come out at the Liquids Out panel.)
			if (io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(i.item())) {
				continue;
			}
			if (q.isEmpty() || i.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
				out.add(i);
			}
		}
		return out;
	}

	private TerminalPayload.Item picked() {
		List<TerminalPayload.Item> items = items();
		return selected >= 0 && selected < items.size() ? items.get(selected) : null;
	}

	private void kiosk(GuiGraphicsExtractor g, GoodsOutPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		WhGui.box(g, font, x + 8, y + 40, x + 180, y + 272, "", "");
		g.fill(x + 11, y + 43, x + 176, y + 56, WhGui.WELL);
		g.outline(x + 11, y + 43, 165, 13, WhGui.CYAN_DIM);
		List<TerminalPayload.Item> items = items();
		for (int k = 0; k < ROWS && scroll + k < items.size(); k++) {
			TerminalPayload.Item it = items.get(scroll + k);
			int ry = y + 60 + k * ROW;
			boolean sel = scroll + k == selected;
			if (sel || WhGui.in(mouseX, mouseY, x + 10, ry - 1, x + 176, ry + ROW - 1)) {
				g.fill(x + 10, ry - 1, x + 176, ry + ROW - 1, sel ? WhGui.SELECT : 0x40162C3E);
				if (sel) {
					g.outline(x + 10, ry - 1, 166, ROW, WhGui.CYAN);
				}
			}
			WhGui.smallItem(g, it.item(), x + 13, ry + 2);
			g.text(font, WhGui.fit(font, it.item().getHoverName().getString(), 100), x + 25, ry + 3, WhGui.WHITE, false);
			String n = String.format(Locale.ROOT, "%,d", it.count());
			g.text(font, n, x + 172 - font.width(n), ry + 3, WhGui.WHITE, false);
		}
		TerminalPayload t = WarehouseClient.snapshot(menu.terminal);
		if (items.isEmpty()) {
			g.text(font, t == null ? "CONNECTING..." : "Nothing stored.", x + 14, y + 62, WhGui.DIM, false);
		} else if (t != null) {
			String foot = String.format(Locale.ROOT, "%d kinds  -  %,d items", t.types(), t.stored());
			g.text(font, foot, x + 12, y + 260, WhGui.GREY, false);
		}
		// The request.
		int rx0 = x + 186, rx1 = x + 344;
		WhGui.box(g, font, rx0, y + 40, rx1, y + 170, "REQUEST", "");
		TerminalPayload.Item it = picked();
		if (it == null) {
			g.text(font, "Pick an item on the left.", rx0 + 5, y + 56, WhGui.DIM, false);
		} else {
			g.item(it.item(), rx0 + 5, y + 52);
			g.text(font, WhGui.fit(font, it.item().getHoverName().getString(), 120), rx0 + 26, y + 52, WhGui.WHITE, false);
			g.text(font, String.format(Locale.ROOT, "%,d in stock", it.count()), rx0 + 26, y + 62, WhGui.GREY, false);
		}
		g.text(font, "AMOUNT", rx0 + 5, y + 75, WhGui.CYAN, false);
		g.fill(rx0 + 5, y + 85, rx0 + 70, y + 98, WhGui.WELL);
		g.outline(rx0 + 5, y + 85, 65, 13, WhGui.CYAN_DIM);
		String[] quick = {"+STACK", "CASE", "PALLET", "ALL"};
		for (int k = 0; k < 4; k++) {
			int bx = rx0 + 5 + k * 38;
			WhGui.button(g, font, bx, y + 101, bx + 36, y + 113, quick[k], false, WhGui.in(mouseX, mouseY, bx, y + 101, bx + 36, y + 113), it != null);
		}
		g.text(font, "SEND TO", rx0 + 5, y + 118, WhGui.CYAN, false);
		WhGui.button(g, font, rx0 + 5, y + 128, rx0 + 76, y + 140, "PICKUP WINDOW", dest == Order.WINDOW, WhGui.in(mouseX, mouseY, rx0 + 5, y + 128, rx0 + 76, y + 140), true);
		WhGui.button(g, font, rx0 + 80, y + 128, rx1 - 5, y + 140, "PIPES  OUT", dest == Order.PIPES, WhGui.in(mouseX, mouseY, rx0 + 80, y + 128, rx1 - 5, y + 140), true);
		boolean can = it != null && parse() > 0;
		WhGui.button(g, font, rx0 + 5, y + 146, rx1 - 5, y + 164, "PLACE ORDER", can, WhGui.in(mouseX, mouseY, rx0 + 5, y + 146, rx1 - 5, y + 164), can);
		// The line.
		WhGui.box(g, font, rx0, y + 176, rx1, y + 272, "LINE", "everyone's orders");
		int yy = y + 190;
		if (d != null) {
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (o.state() >= Order.DONE || o.stream() > 0) {
					continue;
				}
				if (yy > y + 262) {
					break;
				}
				boolean own = o.owner().equals(me());
				g.text(font, "#" + o.id(), rx0 + 5, yy, WhGui.GREY, false);
				String what = WhGui.fit(font, (own ? "you" : o.who()) + "  " + o.item().getHoverName().getString(), 78);
				g.text(font, what, rx0 + 36, yy, own ? WhGui.CYAN : WhGui.WHITE, false);
				String st = Order.STATES[o.state()];
				g.text(font, st, rx1 - (own && o.state() <= Order.PICKING ? 16 : 5) - font.width(st), yy, colour(o.state()), false);
				if (own && o.state() <= Order.PICKING) {
					boolean hover = WhGui.in(mouseX, mouseY, rx1 - 13, yy - 1, rx1 - 4, yy + 8);
					g.text(font, "x", rx1 - 10, yy, hover ? WhGui.RED : WhGui.DIM, false);
				}
				yy += 11;
			}
		}
	}

	private long parse() {
		try {
			return amount == null || amount.getValue().replaceAll("[^0-9]", "").isEmpty() ? 0 : Long.parseLong(amount.getValue().replaceAll("[^0-9]", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// ------------------------------------------------------------------ OUTPUT PORT

	private void port(GuiGraphicsExtractor g, GoodsOutPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		g.text(font, "MODE", x + 12, y + 44, WhGui.CYAN, false);
		for (int k = 0; k < 3; k++) {
			int bx = x + 50 + k * 98;
			boolean on = d != null && d.portMode() == k;
			WhGui.button(g, font, bx, y + 41, bx + 94, y + 53, MODES[k], on, WhGui.in(mouseX, mouseY, bx, y + 41, bx + 94, y + 53), true);
		}
		WhGui.box(g, font, x + 8, y + 58, x + 344, y + 92, "PORT BUFFER", "what goes into the pipe next");
		for (int k = 0; k < PortBuffer.SLOTS; k++) {
			int bx = x + 14 + k * 18, by = y + 72;
			WhGui.slotWell(g, bx, by);
			if (d != null && k < d.buffer().size() && !d.buffer().get(k).isEmpty()) {
				g.item(d.buffer().get(k), bx, by);
				g.itemDecorations(font, d.buffer().get(k), bx, by);
			}
		}
		if (d != null) {
			g.text(font, "Flow", x + 186, y + 72, WhGui.GREY, false);
			String flow = d.flow() + " items / s";
			g.text(font, flow, x + 339 - font.width(flow), y + 72, WhGui.WHITE, false);
			g.text(font, "Pipe", x + 186, y + 82, WhGui.GREY, false);
			String pipe = d.piped() ? "connected" : "none on OUT";
			g.text(font, pipe, x + 339 - font.width(pipe), y + 82, d.piped() ? WhGui.GREEN : WhGui.AMBER, false);
		}
		WhGui.box(g, font, x + 8, y + 96, x + 344, y + 182, "STREAMS", "always send these, keeping a reserve");
		g.text(font, "UP TO", x + 150, y + 108, WhGui.GREY, false);
		g.text(font, "KEEP AT LEAST", x + 216, y + 108, WhGui.GREY, false);
		int yy = y + 120;
		int n = d == null ? 0 : d.streams().size();
		for (int k = 0; k < n; k++) {
			GoodsOutPayload.StreamLine st = d.streams().get(k);
			WhGui.smallItem(g, st.item(), x + 13, yy);
			g.text(font, WhGui.fit(font, st.item().getHoverName().getString(), 110), x + 25, yy, WhGui.WHITE, false);
			stepper(g, mouseX, mouseY, x + 150, yy, st.rate() + "/s");
			stepper(g, mouseX, mouseY, x + 216, yy, String.format(Locale.ROOT, "%,d", st.reserve()));
			boolean held = st.held() || d.portMode() != 2;
			String state = d.portMode() != 2 ? "PAUSED" : st.held() ? "HELD" : "RUNNING";
			g.text(font, state, x + 318 - font.width(state), yy, held ? WhGui.AMBER : WhGui.GREEN, false);
			g.text(font, "x", x + 333, yy, WhGui.in(mouseX, mouseY, x + 330, yy - 1, x + 340, yy + 8) ? WhGui.RED : WhGui.DIM, false);
			yy += 13;
		}
		if (n < 4) {
			WhGui.slotWell(g, x + 12, yy - 1);
			g.text(font, "Click here with an item to add a stream", x + 34, yy + 3, WhGui.DIM, false);
		}
		// The orders going to pipes.
		int rx0 = x + 186, rx1 = x + 344;
		WhGui.box(g, font, rx0, y + 190, rx1, y + 272, "ORDERS TO PIPES", "");
		yy = y + 204;
		if (d != null) {
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (o.dest() != Order.PIPES || yy > y + 262) {
					continue;
				}
				g.text(font, "#" + o.id(), rx0 + 5, yy, WhGui.GREY, false);
				g.text(font, WhGui.fit(font, o.item().getHoverName().getString() + String.format(Locale.ROOT, " x%,d", o.amount()), 74), rx0 + 36, yy,
						WhGui.WHITE, false);
				String st = o.stream() > 0 && o.state() < Order.DONE ? "STREAM" : Order.STATES[o.state()];
				g.text(font, st, rx1 - 5 - font.width(st), yy, colour(o.state()), false);
				yy += 11;
			}
		}
	}

	/** A value with - and + either side of it. */
	private void stepper(GuiGraphicsExtractor g, int mouseX, int mouseY, int x, int y, String value) {
		g.text(font, "-", x, y, WhGui.in(mouseX, mouseY, x - 2, y - 1, x + 6, y + 8) ? WhGui.WHITE : WhGui.GREY, false);
		g.text(font, value, x + 10, y, WhGui.CYAN, false);
		int px = x + 14 + font.width(value);
		g.text(font, "+", px, y, WhGui.in(mouseX, mouseY, px - 2, y - 1, px + 7, y + 8) ? WhGui.WHITE : WhGui.GREY, false);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int x = leftPos, y = topPos;
		for (int k = 0; k < TABS.length; k++) {
			int tx = x + 8 + k * 96;
			if (WhGui.in(mx, my, tx, y + 24, tx + 92, y + 36)) {
				if (k != menu.page) {
					send(WarehouseActionPayload.PAGE, k, ItemStack.EMPTY, 0, 0, "");
				}
				return true;
			}
		}
		if (menu.page == GoodsOutBlockEntity.WINDOW) {
			int[][] buttons = {{x + 89, x + 113, GoodsOutMenu.SHOW_MINE}, {x + 115, x + 135, GoodsOutMenu.SHOW_ALL}, {x + 137, x + 177, GoodsOutMenu.TAKE_ALL}};
			for (int[] b : buttons) {
				if (WhGui.in(mx, my, b[0], y + 43, b[1], y + 56) && minecraft != null && minecraft.gameMode != null) {
					minecraft.gameMode.handleInventoryButtonClick(menu.containerId, b[2]);
					return true;
				}
			}
		} else if (menu.page == GoodsOutBlockEntity.KIOSK) {
			if (kioskClick(mx, my)) {
				return true;
			}
		} else if (portClick(mx, my)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean kioskClick(double mx, double my) {
		int x = leftPos, y = topPos, rx0 = x + 186, rx1 = x + 344;
		List<TerminalPayload.Item> items = items();
		for (int k = 0; k < ROWS; k++) {
			int ry = y + 60 + k * ROW;
			if (WhGui.in(mx, my, x + 10, ry - 1, x + 176, ry + ROW - 1) && scroll + k < items.size()) {
				selected = scroll + k;
				return true;
			}
		}
		TerminalPayload.Item it = picked();
		if (it != null && amount != null) {
			long stack = Math.max(1, it.item().getMaxStackSize()), caseSize = CaseLoad.capacity(it.item());
			long[] quick = {parse() + stack, caseSize, caseSize * 8, it.count()};
			for (int k = 0; k < 4; k++) {
				int bx = rx0 + 5 + k * 38;
				if (WhGui.in(mx, my, bx, y + 101, bx + 36, y + 113)) {
					amount.setValue(String.valueOf(Math.min(9_999_999, k == 0 ? quick[0] : quick[k])));
					return true;
				}
			}
		}
		if (WhGui.in(mx, my, rx0 + 5, y + 128, rx0 + 76, y + 140)) {
			dest = Order.WINDOW;
			return true;
		}
		if (WhGui.in(mx, my, rx0 + 80, y + 128, rx1 - 5, y + 140)) {
			dest = Order.PIPES;
			return true;
		}
		if (WhGui.in(mx, my, rx0 + 5, y + 146, rx1 - 5, y + 164) && it != null && parse() > 0) {
			send(WarehouseActionPayload.ORDER, dest, it.item(), parse(), 0, "");
			return true;
		}
		// Cancelling your own order in the line.
		GoodsOutPayload d = data();
		if (d != null) {
			int yy = y + 190;
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (o.state() >= Order.DONE || o.stream() > 0) {
					continue;
				}
				if (yy > y + 262) {
					break;
				}
				if (o.owner().equals(me()) && o.state() <= Order.PICKING && WhGui.in(mx, my, rx1 - 13, yy - 1, rx1 - 4, yy + 8)) {
					send(WarehouseActionPayload.CANCEL, o.id(), ItemStack.EMPTY, 0, 0, "");
					return true;
				}
				yy += 11;
			}
		}
		return false;
	}

	private boolean portClick(double mx, double my) {
		int x = leftPos, y = topPos;
		for (int k = 0; k < 3; k++) {
			int bx = x + 50 + k * 98;
			if (WhGui.in(mx, my, bx, y + 41, bx + 94, y + 53)) {
				send(WarehouseActionPayload.PORT_MODE, k, ItemStack.EMPTY, 0, 0, "");
				return true;
			}
		}
		GoodsOutPayload d = data();
		int n = d == null ? 0 : d.streams().size();
		int yy = y + 120;
		boolean shift = minecraft != null && minecraft.hasShiftDown();
		for (int k = 0; k < n; k++) {
			GoodsOutPayload.StreamLine st = d.streams().get(k);
			String rate = st.rate() + "/s", reserve = String.format(Locale.ROOT, "%,d", st.reserve());
			int rp = x + 150 + 14 + font.width(rate), sp = x + 216 + 14 + font.width(reserve);
			long step = shift ? 1024 : 64;
			if (WhGui.in(mx, my, x + 148, yy - 1, x + 156, yy + 8)) {
				send(WarehouseActionPayload.STREAM_SET, k, ItemStack.EMPTY, Math.max(1, st.rate() - 1), st.reserve(), "");
				return true;
			}
			if (WhGui.in(mx, my, rp - 2, yy - 1, rp + 7, yy + 8)) {
				send(WarehouseActionPayload.STREAM_SET, k, ItemStack.EMPTY, Math.min(64, st.rate() + 1), st.reserve(), "");
				return true;
			}
			if (WhGui.in(mx, my, x + 214, yy - 1, x + 222, yy + 8)) {
				send(WarehouseActionPayload.STREAM_SET, k, ItemStack.EMPTY, st.rate(), Math.max(0, st.reserve() - step), "");
				return true;
			}
			if (WhGui.in(mx, my, sp - 2, yy - 1, sp + 7, yy + 8)) {
				send(WarehouseActionPayload.STREAM_SET, k, ItemStack.EMPTY, st.rate(), st.reserve() + step, "");
				return true;
			}
			if (WhGui.in(mx, my, x + 330, yy - 1, x + 340, yy + 8)) {
				send(WarehouseActionPayload.STREAM_REMOVE, k, ItemStack.EMPTY, 0, 0, "");
				return true;
			}
			yy += 13;
		}
		if (n < 4 && WhGui.in(mx, my, x + 11, yy - 2, x + 29, yy + 16) && !menu.getCarried().isEmpty()) {
			send(WarehouseActionPayload.STREAM_ADD, 0, menu.getCarried().copyWithCount(1), 8, 512, "");
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
		if (menu.page == GoodsOutBlockEntity.WINDOW && minecraft != null && minecraft.gameMode != null
				&& WhGui.in(mouseX, mouseY, leftPos + 8, topPos + 58, leftPos + 180, topPos + 170)) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, dy > 0 ? GoodsOutMenu.UP : GoodsOutMenu.DOWN);
			return true;
		}
		if (menu.page == GoodsOutBlockEntity.KIOSK) {
			int max = Math.max(0, items().size() - ROWS);
			scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, dx, dy);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		// Typing in a box mustn't close the screen (the inventory key).
		if ((search != null && search.isFocused() || amount != null && amount.isFocused()) && minecraft != null
				&& minecraft.options.keyInventory.matches(event)) {
			return true;
		}
		return super.keyPressed(event);
	}
}
