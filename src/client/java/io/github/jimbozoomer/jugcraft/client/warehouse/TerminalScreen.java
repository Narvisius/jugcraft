package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.BuildingPayload;
import io.github.jimbozoomer.jugcraft.warehouse.CaseLoad;
import io.github.jimbozoomer.jugcraft.warehouse.Order;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalMenu;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Warehouse Control Terminal's screen (the mockup warehouse_terminal_screen): WAREHOUSE MANAGEMENT. STOCK: every item
 * stored (search, quantity, cases, pallets, places), the one picked in detail with where it is; FLEET: each drone, what
 * it is doing, where to, its battery; ORDERS (part W2): everyone's orders and the streams' refills, the one picked with its
 * progress (CANCEL, SEND TO PIPES), Ready Storage's slots and the Output Port; STOCK's REQUEST box orders the item picked.
 * BUILDING (part W3): the pre-made warehouse's floors, its lifts and stairs, ADD A FLOOR (what the next floor needs, built
 * with your items or the party's, its progress once staked). Part W5: LAYOUT, the live floor map (the drones as they
 * move, the racks' fill or counts, a drone's buttons) and CONTROL (the job priorities, housekeeping, power and its
 * circuits, ACCESS, PAUSE ALL DRONES); RUSH on ORDERS. The pages come from the server every second while it is open.
 */
public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
	private static final int W = 420, H = 262, ROW = 18;
	private static final String[] TABS = {"STOCK", "FLEET", "ORDERS", "LAYOUT", "CONTROL", "BUILDING"};
	private static final int LIVE_TABS = 6, BUILDING_TAB = 5;
	private int tab, scroll, selected = -1, dest = Order.WINDOW, orderPick = -1, floorScroll;
	/** Part W4: STOCK's filter: ALL, ITEMS, LIQUIDS. */
	private int filter;
	private static final String[] FILTERS = {"ALL", "ITEMS", "LIQUIDS"};
	private boolean allOrders, partyItems;
	private EditBox search, amount;

	public TerminalScreen(TerminalMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, W, H);
	}

	@Override
	protected void init() {
		super.init();
		search = addRenderableWidget(new EditBox(font, leftPos + 14, topPos + 45, 112, 12, Component.literal("search")));
		search.setHint(Component.literal("search items...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
		search.setBordered(false);
		search.setResponder(s -> {
			scroll = 0;
			selected = -1;
		});
		amount = addRenderableWidget(new EditBox(font, leftPos + 278, topPos + H - 92, 56, 10, Component.literal("amount")));
		amount.setValue("64");
		amount.setMaxLength(7);
		amount.setBordered(false);
	}

	private long parse() {
		try {
			return amount.getValue().replaceAll("[^0-9]", "").isEmpty() ? 0 : Long.parseLong(amount.getValue().replaceAll("[^0-9]", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private void send(int action, int arg, ItemStack item, long n) {
		ClientPlayNetworking.send(new WarehouseActionPayload(menu.terminal, action, item, n, arg, 0, ""));
	}

	private List<TerminalPayload.OrderLine> orders() {
		TerminalPayload d = data();
		List<TerminalPayload.OrderLine> out = new ArrayList<>();
		if (d != null) {
			for (TerminalPayload.OrderLine o : d.orders()) {
				if (allOrders || o.state() < Order.DONE) {
					out.add(o);
				}
			}
		}
		return out;
	}

	private TerminalPayload data() {
		return WarehouseClient.snapshot(menu.terminal);
	}

	private List<TerminalPayload.Item> items() {
		TerminalPayload d = data();
		List<TerminalPayload.Item> out = new ArrayList<>();
		if (d == null) {
			return out;
		}
		String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		for (TerminalPayload.Item i : d.items()) {
			boolean liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(i.item());
			if (filter == 1 && liquid || filter == 2 && !liquid) {
				continue;
			}
			if (q.isEmpty() || WhGui.name(i.item()).toLowerCase(Locale.ROOT).contains(q)
					|| i.places().stream().anyMatch(p -> p.toLowerCase(Locale.ROOT).contains(q))) {
				out.add(i);
			}
		}
		return out;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		search.visible = tab == 0;
		amount.visible = tab == 0 && selected >= 0 && selected < items().size();
		extractBackground(g, mouseX, mouseY, delta);
		super.extractRenderState(g, mouseX, mouseY, delta);
		extractTooltip(g, mouseX, mouseY);
		if (tab == 0) {
			List<TerminalPayload.Item> items = items();
			int x0 = leftPos + 10, y0 = topPos + 76;
			for (int k = 0; k < visibleRows() && scroll + k < items.size(); k++) {
				int y = y0 + k * ROW;
				if (WhGui.in(mouseX, mouseY, x0, y, x0 + 18, y + 18)) {
					ItemStack hover = items.get(scroll + k).item();
					if (io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(hover)) {
						g.setTooltipForNextFrame(font, Component.literal(WhGui.name(hover)), mouseX, mouseY);
					} else {
						g.setTooltipForNextFrame(font, hover, mouseX, mouseY);
					}
				}
			}
		}
	}

	private int visibleRows() {
		return 8;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		int x = leftPos, y = topPos;
		TerminalPayload d = data();
		WhGui.window(g, font, x, y, x + W, y + H, "WAREHOUSE MANAGEMENT", "WH-1");
		int right = x + W - 9;
		if (d != null) {
			long ready = d.fleet().stream().filter(f -> !f.job().equals("CHARGING")).count();
			right = WhGui.chip(g, font, right, y + 4, String.format(Locale.ROOT, "PLACES %d%%",
					d.placesTotal() == 0 ? 0 : Math.round(100.0 * d.placesUsed() / d.placesTotal())), WhGui.CYAN) - 4;
			if (!d.out().liquidIn().isEmpty()) {
				right = WhGui.chip(g, font, right, y + 4, WhGui.fit(font, "LIQUIDS IN: " + d.out().liquidIn().toUpperCase(Locale.ROOT), 110), WhGui.GREEN) - 4;
			}
			BuildingPayload b = WarehouseClient.building(menu.terminal);
			if (b != null && b.state() == BuildingPayload.ADDING) {
				// (In place of the intake's chip while a floor is being added.)
				right = WhGui.chip(g, font, right, y + 4, "FLOOR " + (b.floors() + 1) + " " + percent(b.done(), b.total()) + "%", WhGui.AMBER) - 4;
			} else {
				right = WhGui.chip(g, font, right, y + 4, "INTAKE " + d.casesWaiting() + " CASES", d.casesWaiting() > 0 ? WhGui.AMBER : WhGui.CYAN) - 4;
			}
			WhGui.chip(g, font, right, y + 4, "DRONES " + ready + "/" + d.fleet().size(), WhGui.GREEN);
		}
		for (int k = 0; k < TABS.length; k++) {
			int tx = x + 8 + k * 64;
			boolean enabled = k < LIVE_TABS;
			WhGui.button(g, font, tx, y + 24, tx + 60, y + 36, TABS[k], tab == k, WhGui.in(mouseX, mouseY, tx, y + 24, tx + 60, y + 36), enabled);
		}
		if (d == null) {
			g.text(font, "CONNECTING...", x + 12, y + 50, WhGui.GREY, false);
			return;
		}
		if (!d.primary()) {
			g.text(font, "Another terminal runs this warehouse.", x + 12, y + 50, WhGui.AMBER, false);
		}
		if (tab == 0) {
			stock(g, d, mouseX, mouseY);
		} else if (tab == 1) {
			fleet(g, d);
		} else if (tab == 2) {
			orders(g, d, mouseX, mouseY);
		} else if (tab == 3) {
			layout(g, d, mouseX, mouseY);
		} else if (tab == 4) {
			control(g, d, mouseX, mouseY);
		} else {
			building(g, mouseX, mouseY);
		}
		// The bottom: pallet places (on BUILDING, what the building was built from).
		int by = y + H - 22;
		g.fill(x + 8, by, x + W - 8, by + 16, WhGui.HEADER);
		g.outline(x + 8, by, W - 16, 16, 0xFF1E3244);
		BuildingPayload bp = WarehouseClient.building(menu.terminal);
		if (tab == BUILDING_TAB) {
			g.text(font, "BUILT FROM", x + 14, by + 4, WhGui.GREY, false);
			String from = bp == null ? "" : bp.kind().isEmpty() ? "built by hand (not a pre-made warehouse)"
					: bp.builtFrom() + "  -  facing " + bp.facing().toLowerCase(Locale.ROOT);
			g.text(font, WhGui.fit(font, from, W - 90), x + 72, by + 4, WhGui.WHITE, false);
			return;
		}
		g.text(font, "PALLET PLACES", x + 14, by + 4, WhGui.GREY, false);
		WhGui.bar(g, x + 92, by + 5, x + 252, by + 11, d.placesTotal() == 0 ? 0 : d.placesUsed() / (double) d.placesTotal(), WhGui.CYAN);
		g.text(font, d.placesUsed() + " / " + d.placesTotal() + " in use", x + 258, by + 4, WhGui.WHITE, false);
		String stored = String.format(Locale.ROOT, "%,d STORED", d.stored());
		g.text(font, stored, x + W - 14 - font.width(stored), by + 4, WhGui.CYAN, false);
		if (d.truncated()) {
			g.text(font, "Too big: not all of it was found", x + 12, y + H - 32, WhGui.AMBER, false);
		}
	}

	private void stock(GuiGraphicsExtractor g, TerminalPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		int lx0 = x + 8, ly0 = y + 40, lx1 = x + 262, ly1 = y + H - 28;
		WhGui.box(g, font, lx0, ly0, lx1, ly1, "", "");
		g.fill(x + 12, y + 43, x + 130, y + 57, WhGui.WELL);
		g.outline(x + 12, y + 43, 118, 14, WhGui.CYAN_DIM);
		// Part W4: ALL, ITEMS, LIQUIDS.
		for (int k = 0; k < FILTERS.length; k++) {
			int bx = x + 134 + k * 42;
			WhGui.button(g, font, bx, y + 44, bx + 40, y + 56, FILTERS[k], filter == k, WhGui.in(mouseX, mouseY, bx, y + 44, bx + 40, y + 56), true);
		}
		int[] cols = {32, 140, 186, 222};
		String[] heads = filter == 2 ? new String[] {"LIQUID", "AMOUNT", "DRUMS", "PALLETS"} : new String[] {"ITEM", "QUANTITY", "CASES", "PALLETS"};
		for (int k = 0; k < cols.length; k++) {
			g.text(font, heads[k], x + 8 + cols[k] - (k == 0 ? 22 : 0), y + 63, WhGui.GREY, false);
		}
		g.fill(lx0 + 4, y + 73, lx1 - 4, y + 74, WhGui.LINE);
		List<TerminalPayload.Item> items = items();
		int y0 = y + 76;
		for (int k = 0; k < visibleRows() && scroll + k < items.size(); k++) {
			TerminalPayload.Item it = items.get(scroll + k);
			int ry = y0 + k * ROW;
			boolean sel = scroll + k == selected;
			if (sel || WhGui.in(mouseX, mouseY, lx0 + 2, ry, lx1 - 8, ry + ROW)) {
				g.fill(lx0 + 2, ry - 1, lx1 - 8, ry + ROW - 1, sel ? WhGui.SELECT : 0x40162C3E);
				if (sel) {
					g.outline(lx0 + 2, ry - 1, lx1 - lx0 - 10, ROW, WhGui.CYAN);
				}
			}
			g.item(WhGui.icon(it.item()), x + 12, ry);
			g.text(font, WhGui.fit(font, WhGui.name(it.item()), 104), x + 32, ry + 5, WhGui.WHITE, false);
			g.text(font, WhGui.amount(it.item(), it.count()), x + 8 + cols[1], ry + 5, WhGui.WHITE, false);
			g.text(font, String.valueOf(it.cases()), x + 8 + cols[2], ry + 5, WhGui.GREY, false);
			g.text(font, String.valueOf(it.pallets()), x + 8 + cols[3], ry + 5, WhGui.GREY, false);
		}
		if (items.isEmpty()) {
			String none = d.items().isEmpty() ? "Nothing stored yet. Pipe or drop items into Goods In."
					: filter == 2 ? "No liquids stored. Pipe them into a Liquids In panel." : "Nothing matches.";
			g.text(font, WhGui.fit(font, none, lx1 - lx0 - 12), x + 14, y0 + 4, WhGui.DIM, false);
		}
		// The scroll bar.
		int total = items.size();
		if (total > visibleRows()) {
			int sx = lx1 - 7, top = y0, h = visibleRows() * ROW;
			g.fill(sx, top, sx + 4, top + h, WhGui.WELL);
			int th = Math.max(10, h * visibleRows() / total), ty = top + (h - th) * scroll / Math.max(1, total - visibleRows());
			g.fill(sx, ty, sx + 4, ty + th, WhGui.CYAN_DIM);
		}
		String count = String.format(Locale.ROOT, "%d kinds  -  %,d items", d.types(), d.stored())
				+ (d.out().liquids() > 0 ? "  -  " + io.github.jimbozoomer.jugcraft.warehouse.Drums.buckets(d.out().liquids()) : "");
		g.text(font, count, (lx0 + lx1) / 2 - font.width(count) / 2, ly1 - 11, WhGui.GREY, false);

		// The item (or liquid) picked.
		int rx0 = x + 270, ry0 = y + 40, rx1 = x + W - 8, ry1 = y + H - 28;
		TerminalPayload.Item it = selected >= 0 && selected < items.size() ? items.get(selected) : null;
		WhGui.box(g, font, rx0, ry0, rx1, ry1, it == null ? "ITEM" : WhGui.fit(font, WhGui.name(it.item()).toUpperCase(Locale.ROOT), 120), "");
		if (it == null) {
			g.text(font, "Pick an item on the left.", rx0 + 6, ry0 + 18, WhGui.DIM, false);
			return;
		}
		net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.liquid(it.item());
		g.item(WhGui.icon(it.item()), rx0 + 6, ry0 + 16);
		g.text(font, WhGui.amount(it.item(), it.count()), rx0 + 28, ry0 + 16, WhGui.WHITE, false);
		if (liquid != null) {
			io.github.jimbozoomer.jugcraft.warehouse.Drums.Kind kind = io.github.jimbozoomer.jugcraft.warehouse.Drums.kindFor(liquid);
			String drum = kind == null ? "" : switch (kind) {
				case STEEL -> "steel drums";
				case RED -> "flammables drums";
				case POLY -> "HDPE drums";
				case DEWAR -> "dewars";
			};
			g.text(font, WhGui.fit(font, it.cases() + " " + drum + "  " + it.pallets() + " pal.", rx1 - rx0 - 40), rx0 + 28, ry0 + 26, WhGui.GREY, false);
			io.github.jimbozoomer.jugcraft.nuclear.TankProducts.Product product = io.github.jimbozoomer.jugcraft.nuclear.TankProducts.of(liquid);
			if (product != null && product.diamond()) {
				WhGui.diamond(g, font, rx1 - 16, ry0 + 22, 10, product.health(), product.fire(), product.instability(), product.special());
				if (!product.un().isEmpty()) {
					g.text(font, "UN" + product.un(), rx1 - 6 - font.width("UN" + product.un()), ry0 + 36, WhGui.GREY, false);
				}
			}
		} else {
			g.text(font, it.cases() + " cases  " + it.pallets() + " pallets", rx0 + 28, ry0 + 26, WhGui.GREY, false);
		}
		g.text(font, "WHERE IT IS", rx0 + 6, ry0 + 42, WhGui.CYAN, false);
		int yy = ry0 + 54;
		for (String place : it.places()) {
			if (yy > ry1 - 98) {
				g.text(font, "...", rx0 + 6, yy, WhGui.GREY, false);
				break;
			}
			g.fill(rx0 + 5, yy - 2, rx1 - 5, yy + 10, 0xFF0C141E);
			g.text(font, place, rx0 + 9, yy, WhGui.WHITE, false);
			yy += 13;
		}
		// The request: an amount (buckets, for a liquid), quick amounts, where to, the order.
		g.text(font, "REQUEST", rx0 + 6, ry1 - 84, WhGui.CYAN, false);
		g.fill(rx0 + 6, ry1 - 74, rx0 + 70, ry1 - 61, WhGui.WELL);
		g.outline(rx0 + 6, ry1 - 74, 64, 13, WhGui.CYAN_DIM);
		if (liquid != null) {
			g.text(font, "B", rx0 + 73, ry1 - 71, WhGui.GREY, false);
		}
		String[] quick = liquid != null ? new String[] {"+1 B", "+DRUM", "PAL", "ALL"} : new String[] {"+STK", "CASE", "PAL", "ALL"};
		for (int k = 0; k < 4; k++) {
			int bx = rx0 + 6 + k * 33;
			WhGui.button(g, font, bx, ry1 - 57, bx + 31, ry1 - 45, quick[k], false, WhGui.in(mouseX, mouseY, bx, ry1 - 57, bx + 31, ry1 - 45), true);
		}
		int mid = (rx0 + rx1) / 2;
		WhGui.button(g, font, rx0 + 6, ry1 - 41, mid - 1, ry1 - 29, liquid != null ? "FILL POINT" : "WINDOW", dest == Order.WINDOW,
				WhGui.in(mouseX, mouseY, rx0 + 6, ry1 - 41, mid - 1, ry1 - 29), true);
		WhGui.button(g, font, mid + 1, ry1 - 41, rx1 - 6, ry1 - 29, "PIPES", dest == Order.PIPES, WhGui.in(mouseX, mouseY, mid + 1, ry1 - 41, rx1 - 6, ry1 - 29), true);
		boolean can = parse() > 0;
		WhGui.button(g, font, rx0 + 6, ry1 - 25, rx1 - 6, ry1 - 9, "PLACE ORDER", can, WhGui.in(mouseX, mouseY, rx0 + 6, ry1 - 25, rx1 - 6, ry1 - 9), can);
		String note = liquid != null ? (d.out().emptiers() == 0 ? "No emptying station" : "") : d.out().hatches() == 0 ? "No Ready Storage" : "";
		if (!note.isEmpty()) {
			g.text(font, WhGui.fit(font, note, rx1 - rx0 - 88), rx0 + 82, ry1 - 71, WhGui.AMBER, false);
		}
	}

	private void orders(GuiGraphicsExtractor g, TerminalPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		int lx0 = x + 8, ly0 = y + 40, lx1 = x + 262, ly1 = y + H - 28;
		WhGui.box(g, font, lx0, ly0, lx1, ly1, "", "");
		WhGui.button(g, font, lx0 + 4, ly0 + 4, lx0 + 40, ly0 + 15, "OPEN", !allOrders, WhGui.in(mouseX, mouseY, lx0 + 4, ly0 + 4, lx0 + 40, ly0 + 15), true);
		WhGui.button(g, font, lx0 + 42, ly0 + 4, lx0 + 70, ly0 + 15, "ALL", allOrders, WhGui.in(mouseX, mouseY, lx0 + 42, ly0 + 4, lx0 + 70, ly0 + 15), true);
		String[] heads = {"ORDER", "FOR", "ITEM", "AMOUNT", "STATUS"};
		int[] cols = {4, 40, 86, 162, 206};
		for (int k = 0; k < heads.length; k++) {
			g.text(font, heads[k], lx0 + cols[k], ly0 + 20, WhGui.GREY, false);
		}
		g.fill(lx0 + 4, ly0 + 30, lx1 - 4, ly0 + 31, WhGui.LINE);
		List<TerminalPayload.OrderLine> list = orders();
		int yy = ly0 + 34;
		for (int i = 0; i < list.size() && yy < ly1 - 12; i++) {
			TerminalPayload.OrderLine o = list.get(i);
			boolean sel = i == orderPick;
			if (sel || WhGui.in(mouseX, mouseY, lx0 + 2, yy - 2, lx1 - 2, yy + 11)) {
				g.fill(lx0 + 2, yy - 2, lx1 - 2, yy + 11, sel ? WhGui.SELECT : 0x40162C3E);
				if (sel) {
					g.outline(lx0 + 2, yy - 2, lx1 - lx0 - 4, 13, WhGui.CYAN);
				}
			}
			g.text(font, o.stream() > 0 ? "S-" + o.stream() : "#" + o.id(), lx0 + cols[0], yy, o.rush() ? WhGui.AMBER : o.stream() > 0 ? WhGui.CYAN : WhGui.WHITE,
					false);
			g.text(font, WhGui.fit(font, o.who(), 44), lx0 + cols[1], yy, WhGui.WHITE, false);
			WhGui.smallItem(g, WhGui.icon(o.item()), lx0 + cols[2], yy);
			g.text(font, WhGui.fit(font, WhGui.name(o.item()), 62), lx0 + cols[2] + 11, yy, WhGui.WHITE, false);
			g.text(font, WhGui.amount(o.item(), o.amount()), lx0 + cols[3], yy, WhGui.WHITE, false);
			int colour = o.state() == Order.READY ? WhGui.GREEN : o.state() == Order.QUEUED ? WhGui.GREY : o.state() >= Order.DONE ? WhGui.DIM
					: o.state() == Order.SENT ? WhGui.CYAN : WhGui.AMBER;
			g.text(font, Order.STATES[o.state()], lx0 + cols[4], yy, colour, false);
			yy += 14;
		}
		if (list.isEmpty()) {
			g.text(font, "No orders. Pick an item on STOCK and place one,", lx0 + 6, ly0 + 36, WhGui.DIM, false);
			g.text(font, "or order at a Goods Out panel's kiosk.", lx0 + 6, ly0 + 47, WhGui.DIM, false);
		}
		// The order picked.
		int rx0 = x + 270, ry0 = y + 40, rx1 = x + W - 8, ry1 = y + H - 28;
		TerminalPayload.OrderLine o = orderPick >= 0 && orderPick < list.size() ? list.get(orderPick) : null;
		WhGui.box(g, font, rx0, ry0, rx1, ry1, o == null ? "ORDER" : "ORDER #" + o.id(), "");
		TerminalPayload.Outbound out = d.out();
		if (o == null) {
			g.text(font, "Pick an order on the left.", rx0 + 6, ry0 + 18, WhGui.DIM, false);
		} else {
			boolean liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(o.item());
			g.item(WhGui.icon(o.item()), rx0 + 6, ry0 + 16);
			g.text(font, WhGui.fit(font, WhGui.name(o.item()), 100), rx0 + 26, ry0 + 15, WhGui.WHITE, false);
			g.text(font, (liquid ? "" : "x") + WhGui.amount(o.item(), o.amount()) + "  for " + o.who(), rx0 + 26, ry0 + 25, WhGui.GREY, false);
			g.text(font, liquid ? (o.dest() == Order.PIPES ? "to PIPES (Liquids Out's OUT flange)" : "to the fill point (Liquids Out)")
					: o.dest() == Order.PIPES ? "to PIPES (the Output Port)" : "to the pickup window", rx0 + 6, ry0 + 38, WhGui.GREY, false);
			String[] steps = liquid ? new String[] {"Placed", "Drums fetched", "At the emptying station", o.dest() == Order.PIPES ? "Out of the OUT flange"
					: "At the fill point"} : new String[] {"Placed", "Pallet fetched", "At the dispatch hatch", o.dest() == Order.PIPES ? "In the port's buffer" : "In Ready Storage"};
			int sy = ry0 + 52;
			for (int k = 0; k < steps.length; k++) {
				long t = k < o.times().size() ? o.times().get(k) : 0;
				boolean done = t > 0, now = !done && (k == 0 || k > 0 && k - 1 < o.times().size() && o.times().get(k - 1) > 0) && o.state() <= Order.PICKING;
				int c = done ? WhGui.GREEN : now ? WhGui.AMBER : WhGui.DIM;
				g.fill(rx0 + 7, sy + 1, rx0 + 13, sy + 7, c);
				g.text(font, steps[k], rx0 + 18, sy, done || now ? WhGui.WHITE : WhGui.GREY, false);
				sy += 12;
			}
			g.text(font, WhGui.fit(font, o.now(), rx1 - rx0 - 12), rx0 + 6, sy + 2, WhGui.CYAN, false);
			WhGui.bar(g, rx0 + 6, sy + 13, rx1 - 6, sy + 18, o.progress(), WhGui.CYAN);
			boolean open = o.state() <= Order.PICKING;
			int mid = (rx0 + rx1) / 2;
			WhGui.button(g, font, rx0 + 6, sy + 24, mid - 1, sy + 36, "TO PIPES", false, WhGui.in(mouseX, mouseY, rx0 + 6, sy + 24, mid - 1, sy + 36),
					(open || liquid && o.state() == Order.READY) && o.dest() == Order.WINDOW);
			WhGui.button(g, font, mid + 1, sy + 24, rx1 - 6, sy + 36, "CANCEL", false, WhGui.in(mouseX, mouseY, mid + 1, sy + 24, rx1 - 6, sy + 36), open);
			// Part W5: RUSH: ahead of every other job (again: back in line).
			WhGui.button(g, font, rx0 + 6, sy + 38, rx1 - 6, sy + 50, o.rush() ? "RUSHED (BACK IN LINE)" : "RUSH", o.rush(),
					WhGui.in(mouseX, mouseY, rx0 + 6, sy + 38, rx1 - 6, sy + 50), open);
		}
		g.text(font, "READY STORAGE", rx0 + 6, ry1 - 40, WhGui.CYAN, false);
		WhGui.bar(g, rx0 + 6, ry1 - 30, rx1 - 6, ry1 - 25, out.poolSlots() == 0 ? 0 : out.poolUsed() / (double) out.poolSlots(), WhGui.CYAN);
		g.text(font, out.poolUsed() + " / " + out.poolSlots() + " slots  -  hatch " + out.busy() + "%", rx0 + 6, ry1 - 20, WhGui.GREY, false);
		String[] modes = {"PORT OFF", "PORT: ORDERS", "PORT: + STREAMS"};
		g.text(font, modes[Math.max(0, Math.min(2, out.portMode()))] + "  (" + out.streams() + ")", rx0 + 6, ry1 - 10, WhGui.GREY, false);
	}

	private static int percent(int done, int total) {
		return total == 0 ? 0 : (int) Math.floor(100.0 * done / total);
	}

	/** The BUILDING page's floors, as many as fit (the top first; the wheel scrolls). */
	private int floorRows() {
		return 6;
	}

	private void building(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		BuildingPayload b = WarehouseClient.building(menu.terminal);
		int lx0 = x + 8, ly0 = y + 40, lx1 = x + 262, ly1 = y + H - 28;
		if (b == null) {
			WhGui.box(g, font, lx0, ly0, lx1, ly1, "BUILDING", "");
			g.text(font, "CONNECTING...", lx0 + 6, ly0 + 18, WhGui.GREY, false);
			return;
		}
		boolean premade = !b.kind().isEmpty();
		WhGui.box(g, font, lx0, ly0, lx1, ly1, premade ? WhGui.fit(font, (b.name() + "  -  " + b.width() + " x " + b.depth() + "  -  floor to floor "
				+ b.storey()).toUpperCase(Locale.ROOT), lx1 - lx0 - 10) : "BUILDING", "");
		// The floors, the top first: a staked one dashed over the roof line.
		boolean adding = b.state() == BuildingPayload.ADDING;
		List<BuildingPayload.FloorLine> lines = new ArrayList<>(b.lines());
		java.util.Collections.reverse(lines);
		int rowH = 22, fx0 = lx0 + 6, fx1 = lx1 - 44, top = ly0 + 26;
		int rows = floorRows() - (adding ? 1 : 0);
		floorScroll = Math.max(0, Math.min(floorScroll, Math.max(0, lines.size() - rows)));
		int yy = top;
		if (premade) {
			g.text(font, "ROOF", fx0, ly0 + 15, WhGui.DIM, false);
			g.fill(fx0 + 24, ly0 + 18, fx1, ly0 + 20, 0xFF3A4652);
		}
		if (adding && floorScroll == 0) {
			int c = WhGui.AMBER;
			for (int k = fx0; k < fx1; k += 6) {
				g.fill(k, yy, Math.min(fx1, k + 3), yy + 1, WhGui.CYAN);
				g.fill(k, yy + rowH - 3, Math.min(fx1, k + 3), yy + rowH - 2, WhGui.CYAN);
			}
			g.text(font, "FLOOR " + (b.floors() + 1) + "  -  BLUEPRINT STAKED", fx0 + 4, yy + 3, WhGui.CYAN, false);
			g.text(font, String.format(Locale.ROOT, "%,d of %,d blocks", b.done(), b.total()), fx0 + 4, yy + 12, WhGui.GREY, false);
			WhGui.bar(g, fx1 - 64, yy + 12, fx1 - 4, yy + 17, b.total() == 0 ? 0 : b.done() / (double) b.total(), c);
			yy += rowH;
		}
		for (int k = floorScroll; k < lines.size() && k < floorScroll + rows; k++) {
			BuildingPayload.FloorLine l = lines.get(k);
			g.fill(fx0, yy, fx1, yy + rowH - 2, 0xFF0F1A26);
			g.outline(fx0, yy, fx1 - fx0, rowH - 2, 0xFF1E3244);
			g.text(font, "FLOOR " + l.n(), fx0 + 4, yy + 3, WhGui.WHITE, false);
			g.text(font, WhGui.fit(font, l.what(), fx1 - fx0 - 58), fx0 + 52, yy + 3, WhGui.GREY, false);
			WhGui.bar(g, fx0 + 4, yy + 13, fx1 - 62, yy + 18, l.total() == 0 ? 0 : l.used() / (double) l.total(), WhGui.CYAN);
			String places = l.used() + " / " + l.total();
			g.text(font, places, fx1 - 4 - font.width(places), yy + 12, WhGui.WHITE, false);
			yy += rowH;
		}
		if (lines.size() > rows) {
			g.text(font, floorScroll > 0 ? "^ more" : "", fx1 - 30, ly0 + 15, WhGui.DIM, false);
			if (floorScroll + rows < lines.size()) {
				g.text(font, "v " + (lines.size() - rows - floorScroll) + " more (scroll)", fx0, yy + 1, WhGui.DIM, false);
			}
		}
		// The lift shafts and the stair tower, the floors' height.
		int sx = fx1 + 6, sh0 = top, sh1 = Math.max(top + rowH, Math.min(ly1 - 22, top + rowH * (Math.min(lines.size(), rows) + (adding && floorScroll == 0 ? 1 : 0))));
		g.fill(sx, sh0, sx + 14, sh1, WhGui.WELL);
		g.outline(sx, sh0, 14, sh1 - sh0, WhGui.AMBER);
		g.fill(sx + 3, sh1 - 12, sx + 11, sh1 - 6, WhGui.AMBER);
		g.text(font, "LIFT", sx - 1, sh1 + 2, WhGui.AMBER, false);
		if (b.lifts() > 1) {
			g.text(font, "x" + b.lifts(), sx + 2, sh1 + 11, WhGui.AMBER, false);
		}
		int tx = sx + 18;
		g.outline(tx, sh0, 14, sh1 - sh0, WhGui.GREY);
		for (int k = sh0; k + rowH / 2 <= sh1; k += rowH / 2) {
			int from = (k - sh0) / (rowH / 2) % 2 == 0 ? tx : tx + 14, to = from == tx ? tx + 14 : tx;
			for (int t = 0; t < 11; t++) {
				int px = from + (to - from) * t / 11, py = k + t;
				g.fill(px, py, px + 1, py + 1, WhGui.GREY);
			}
		}
		g.text(font, "STAIRS", tx - 6, sh1 + 2, WhGui.GREY, false);
		if (premade) {
			String height = "Height used: y " + b.y0() + " to " + b.y1() + "  -  room for " + b.room() + " more floors";
			g.text(font, WhGui.fit(font, height, lx1 - lx0 - 12), lx0 + 6, ly1 - 11, WhGui.DIM, false);
		} else {
			g.text(font, "Floors " + b.floors() + "  -  lifts " + b.lifts(), lx0 + 6, ly1 - 11, WhGui.DIM, false);
		}

		// ADD A FLOOR.
		int rx0 = x + 270, ry0 = y + 40, rx1 = x + W - 8, ry1 = y + H - 28;
		WhGui.box(g, font, rx0, ry0, rx1, ry1, "ADD A FLOOR", "");
		if (!premade) {
			String[] lines2 = {"This warehouse was built by", "hand: add floors by hand.", "", "The pre-made Small, Medium", "and Large Warehouses grow", "floor by floor from here."};
			for (int k = 0; k < lines2.length; k++) {
				g.text(font, lines2[k], rx0 + 6, ry0 + 16 + k * 10, WhGui.GREY, false);
			}
			return;
		}
		int next = b.floors() + 1;
		String intro = switch (b.state()) {
			case BuildingPayload.ADDING -> "Floor " + next + " is being built.";
			case BuildingPayload.INCOMPLETE -> "Floor " + next + " was left half built.";
			case BuildingPayload.FULL -> "No room for another floor.";
			default -> "Floor " + next + " goes on the roof.";
		};
		g.text(font, WhGui.fit(font, intro, rx1 - rx0 - 12), rx0 + 6, ry0 + 15, WhGui.WHITE, false);
		g.text(font, "IT NEEDS", rx0 + 6, ry0 + 28, WhGui.CYAN, false);
		int ny = ry0 + 39;
		int shown = 0;
		for (BuildingPayload.Need n : b.needs()) {
			if (ny > ry1 - 72) {
				break;
			}
			WhGui.smallItem(g, n.item(), rx0 + 6, ny);
			g.text(font, WhGui.fit(font, n.item().getHoverName().getString(), rx1 - rx0 - 52), rx0 + 17, ny, WhGui.GREY, false);
			String count = String.format(Locale.ROOT, "%,d", n.count());
			g.text(font, count, rx1 - 6 - font.width(count), ny, WhGui.WHITE, false);
			ny += 10;
			shown++;
		}
		if (shown == 0) {
			g.text(font, b.state() == BuildingPayload.ADDING ? "Everything is in place." : "-", rx0 + 6, ny, WhGui.DIM, false);
		}
		// BUILD WITH: your own items or the party's (the stake's mode).
		boolean party = adding ? b.party() : partyItems;
		int mid = (rx0 + rx1) / 2;
		g.text(font, "BUILD WITH", rx0 + 6, ry1 - 64, WhGui.CYAN, false);
		WhGui.button(g, font, rx0 + 6, ry1 - 54, mid - 1, ry1 - 42, "MY ITEMS", !party, WhGui.in(mouseX, mouseY, rx0 + 6, ry1 - 54, mid - 1, ry1 - 42), true);
		WhGui.button(g, font, mid + 1, ry1 - 54, rx1 - 6, ry1 - 42, "PARTY ITEMS", party, WhGui.in(mouseX, mouseY, mid + 1, ry1 - 54, rx1 - 6, ry1 - 42), true);
		String main = switch (b.state()) {
			case BuildingPayload.ADDING -> "FLOOR " + next + " BUILDING " + percent(b.done(), b.total()) + "%";
			case BuildingPayload.INCOMPLETE -> "FINISH FLOOR " + next;
			case BuildingPayload.FULL -> "AT THE BUILD LIMIT";
			default -> "ADD FLOOR " + next;
		};
		boolean can = b.state() == BuildingPayload.READY || b.state() == BuildingPayload.INCOMPLETE;
		WhGui.button(g, font, rx0 + 6, ry1 - 38, rx1 - 6, ry1 - 22, main, can, can && WhGui.in(mouseX, mouseY, rx0 + 6, ry1 - 38, rx1 - 6, ry1 - 22), can || adding);
		if (adding) {
			boolean hover = WhGui.in(mouseX, mouseY, rx0 + 6, ry1 - 18, rx1 - 6, ry1 - 6);
			g.fill(rx0 + 6, ry1 - 18, rx1 - 6, ry1 - 6, 0xFF1A0C0C);
			g.outline(rx0 + 6, ry1 - 18, rx1 - rx0 - 12, 12, hover ? WhGui.RED : 0xFF6A2A26);
			String c = "CANCEL (REMOVE STAKE)";
			g.text(font, c, mid - font.width(c) / 2, ry1 - 16, WhGui.RED, false);
		} else {
			g.text(font, WhGui.fit(font, "Drone Towers in range build it.", rx1 - rx0 - 12), rx0 + 6, ry1 - 15, WhGui.DIM, false);
		}
	}

	private boolean buildingClick(double mx, double my) {
		BuildingPayload b = WarehouseClient.building(menu.terminal);
		if (b == null || b.kind().isEmpty()) {
			return false;
		}
		int rx0 = leftPos + 270, rx1 = leftPos + W - 8, ry1 = topPos + H - 28, mid = (rx0 + rx1) / 2;
		boolean adding = b.state() == BuildingPayload.ADDING;
		if (WhGui.in(mx, my, rx0 + 6, ry1 - 54, mid - 1, ry1 - 42) || WhGui.in(mx, my, mid + 1, ry1 - 54, rx1 - 6, ry1 - 42)) {
			partyItems = mx >= mid;
			if (adding) {
				send(WarehouseActionPayload.FLOOR_MODE, partyItems ? 1 : 0, ItemStack.EMPTY, 0);
			}
			return true;
		}
		if (WhGui.in(mx, my, rx0 + 6, ry1 - 38, rx1 - 6, ry1 - 22) && (b.state() == BuildingPayload.READY || b.state() == BuildingPayload.INCOMPLETE)) {
			send(WarehouseActionPayload.ADD_FLOOR, partyItems ? 1 : 0, ItemStack.EMPTY, 0);
			return true;
		}
		if (adding && WhGui.in(mx, my, rx0 + 6, ry1 - 18, rx1 - 6, ry1 - 6)) {
			send(WarehouseActionPayload.CANCEL_FLOOR, 0, ItemStack.EMPTY, 0);
			return true;
		}
		return false;
	}

	private void fleet(GuiGraphicsExtractor g, TerminalPayload d) {
		int x = leftPos, y = topPos;
		int x0 = x + 8, y0 = y + 40, x1 = x + W - 8, y1 = y + H - 28;
		WhGui.box(g, font, x0, y0, x1, y1, "FLEET", d.fleet().size() + " drones");
		boolean floors = d.fleet().stream().anyMatch(l -> l.floor() > 1);
		String[] heads = {"DRONE", "KIND", "DOING", "TO", "BATTERY"};
		int[] cols = {6, 54, 130, 232, 300};
		if (floors) {
			// Part W3: each floor's drones together, the floor before each.
			g.text(font, "FL", x0 + 40, y0 + 16, WhGui.GREY, false);
		}
		for (int k = 0; k < heads.length; k++) {
			g.text(font, heads[k], x0 + cols[k], y0 + 16, WhGui.GREY, false);
		}
		g.fill(x0 + 4, y0 + 26, x1 - 4, y0 + 27, WhGui.LINE);
		int yy = y0 + 31;
		List<TerminalPayload.DroneLine> list = new ArrayList<>(d.fleet());
		list.sort(java.util.Comparator.comparingInt(TerminalPayload.DroneLine::floor));
		for (TerminalPayload.DroneLine l : list) {
			if (yy > y1 - 12) {
				break;
			}
			if (floors) {
				g.text(font, String.valueOf(l.floor()), x0 + 42, yy, WhGui.CYAN, false);
			}
			int colour = l.job().equals("IDLE") || l.job().equals("READY") ? WhGui.CYAN : l.job().equals("CHARGING") ? WhGui.AMBER : WhGui.GREEN;
			String job = l.job();
			if (l.flag(io.github.jimbozoomer.jugcraft.warehouse.Drone.OUT_OF_SERVICE)) {
				job = "OUT OF SERVICE";
				colour = WhGui.RED;
			} else if (l.flag(io.github.jimbozoomer.jugcraft.warehouse.Drone.PAUSED)) {
				job = "PAUSED";
				colour = WhGui.AMBER;
			}
			g.text(font, l.name(), x0 + cols[0], yy, WhGui.WHITE, false);
			g.text(font, l.kind() == 0 ? "Stacker" : "Pallet", x0 + cols[1], yy, WhGui.GREY, false);
			g.text(font, WhGui.fit(font, job, cols[3] - cols[2] - 4), x0 + cols[2], yy, colour, false);
			g.text(font, l.where(), x0 + cols[3], yy, WhGui.CYAN, false);
			WhGui.bar(g, x0 + cols[4], yy + 1, x0 + cols[4] + 60, yy + 7, l.battery(), l.battery() < 0.2F ? WhGui.RED : WhGui.GREEN);
			String pct = Math.round(l.battery() * 100) + "%";
			g.text(font, pct, x0 + cols[4] + 66, yy, WhGui.WHITE, false);
			yy += 14;
		}
		if (d.fleet().isEmpty()) {
			g.text(font, "No drones. Use a Stacker Drone or Pallet Drone on a charging dock.", x0 + 6, y0 + 34, WhGui.DIM, false);
		}
	}

	// ------------------------------------------------------------------ part W5: LAYOUT (the live floor map) and CONTROL

	/** LAYOUT: the floor shown, the drone picked (its id, or -1), the map's colours (FILL or COUNTED). */
	private int mapFloor, droneSel = -1;
	private boolean showCounted;
	private static final int MAP_X1 = 300;

	private static final int C_LANE = 0xFF1C2632, C_WALK = 0xFF1E4232, C_CROSS = 0xFF4A3C1C, C_DOCK = 0xFF263848, C_PIECE = 0xFF24425E,
			C_FULL = 0xFF2878C8, C_PART = 0xFFC89632, C_EMPTY = 0xFF26323E, C_SOME = 0xFF1E5A8C, C_LOCATE = 0xFF3C8CFF;

	private io.github.jimbozoomer.jugcraft.warehouse.@org.jspecify.annotations.Nullable TerminalBlockEntity terminalEntity() {
		return minecraft != null && minecraft.level != null
				&& minecraft.level.getBlockEntity(menu.terminal) instanceof io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity t ? t : null;
	}

	/** Small words (half size), for the map's labels. */
	private void tiny(GuiGraphicsExtractor g, String text, int x, int y, int colour) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.5F, 0.5F);
		g.text(font, text, 0, 0, colour, false);
		g.pose().popMatrix();
	}

	private static String ago(long now, long t) {
		if (t <= 0) {
			return "never";
		}
		long m = Math.max(0, now - t) / 1200;
		return m < 1 ? "just now" : m < 60 ? m + " min ago" : m / 60 + " h ago";
	}

	/** The map's geometry for a floor: the cell size and where its corner is drawn. */
	private int[] mapGeometry(io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Floor f) {
		int mx0 = leftPos + 12, my0 = topPos + 58, mw = MAP_X1 - 8 - 12, mh = H - 28 - 14 - 58;
		int cell = Math.max(2, Math.min(10, Math.min(mw / Math.max(1, f.w()), mh / Math.max(1, f.d()))));
		int ox = mx0 + Math.max(0, (mw - f.w() * cell) / 2), oy = my0 + Math.max(0, (mh - f.d() * cell) / 2);
		return new int[] {cell, ox, oy, mx0, my0, mx0 + mw, my0 + mh};
	}

	private void layout(GuiGraphicsExtractor g, TerminalPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		int lx0 = x + 8, ly0 = y + 40, lx1 = x + MAP_X1, ly1 = y + H - 28;
		io.github.jimbozoomer.jugcraft.warehouse.MapPayload map = WarehouseClient.map(menu.terminal);
		WhGui.box(g, font, lx0, ly0, lx1, ly1, "", "");
		if (map == null || map.floors().isEmpty()) {
			g.text(font, "CONNECTING...", lx0 + 6, ly0 + 6, WhGui.GREY, false);
			layoutSide(g, d, mouseX, mouseY);
			return;
		}
		mapFloor = Math.max(0, Math.min(map.floors().size() - 1, mapFloor));
		for (int k = 0; k < map.floors().size() && k < 6; k++) {
			int bx = lx0 + 4 + k * 36;
			WhGui.button(g, font, bx, ly0 + 4, bx + 34, ly0 + 15, "FL " + (k + 1), k == mapFloor, WhGui.in(mouseX, mouseY, bx, ly0 + 4, bx + 34, ly0 + 15), true);
		}
		WhGui.button(g, font, lx1 - 92, ly0 + 4, lx1 - 50, ly0 + 15, "FILL", !showCounted, WhGui.in(mouseX, mouseY, lx1 - 92, ly0 + 4, lx1 - 50, ly0 + 15), true);
		WhGui.button(g, font, lx1 - 48, ly0 + 4, lx1 - 4, ly0 + 15, "COUNT", showCounted, WhGui.in(mouseX, mouseY, lx1 - 48, ly0 + 4, lx1 - 4, ly0 + 15), true);
		io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Floor f = map.floors().get(mapFloor);
		int[] geo = mapGeometry(f);
		int cell = geo[0], ox = geo[1], oy = geo[2];
		long now = minecraft != null && minecraft.level != null ? minecraft.level.getGameTime() : 0;
		java.util.Map<Long, io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Column> columns = new java.util.HashMap<>();
		for (io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Column c : f.racks()) {
			columns.put(net.minecraft.core.BlockPos.asLong(c.x(), 0, c.z()), c);
		}
		g.enableScissor(geo[3], geo[4], geo[5], geo[6]);
		for (int cz = 0; cz < f.d(); cz++) {
			for (int cx = 0; cx < f.w(); cx++) {
				byte code = f.cells()[cz * f.w() + cx];
				if (code == io.github.jimbozoomer.jugcraft.warehouse.MapPayload.NONE) {
					continue;
				}
				int colour = switch (code) {
					case io.github.jimbozoomer.jugcraft.warehouse.MapPayload.WALKWAY -> C_WALK;
					case io.github.jimbozoomer.jugcraft.warehouse.MapPayload.CROSSING -> C_CROSS;
					case io.github.jimbozoomer.jugcraft.warehouse.MapPayload.DOCK -> C_DOCK;
					case io.github.jimbozoomer.jugcraft.warehouse.MapPayload.PIECE -> C_PIECE;
					case io.github.jimbozoomer.jugcraft.warehouse.MapPayload.RACK -> rackColour(columns.get(net.minecraft.core.BlockPos.asLong(f.x0() + cx, 0,
							f.z0() + cz)), now);
					default -> C_LANE;
				};
				int px = ox + cx * cell, py = oy + cz * cell;
				g.fill(px, py, px + cell - (cell > 3 ? 1 : 0), py + cell - (cell > 3 ? 1 : 0), colour);
			}
		}
		// The pieces' footprints and names.
		for (io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Piece p : f.pieces()) {
			int px0 = ox + (p.x0() - f.x0()) * cell, pz0 = oy + (p.z0() - f.z0()) * cell;
			int pw = (p.x1() - p.x0() + 1) * cell, ph = (p.z1() - p.z0() + 1) * cell;
			g.outline(px0, pz0, pw, ph, 0xFF3C78A8);
			if (font.width(p.name()) / 2 <= pw - 2) {
				tiny(g, p.name(), px0 + pw / 2 - font.width(p.name()) / 4, pz0 + ph / 2 - 2, WhGui.WHITE);
			}
		}
		// The drones, where they are now; the one picked with what is left of its move.
		io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity te = terminalEntity();
		float part = minecraft == null ? 0 : minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		if (te != null) {
			for (io.github.jimbozoomer.jugcraft.warehouse.Drone.View v : te.clientFleet()) {
				io.github.jimbozoomer.jugcraft.warehouse.Leg leg = v.leg();
				if (leg.y() != f.y()) {
					continue;
				}
				if (v.id() == droneSel) {
					for (double t = now + part; t < leg.end(); t += 4) {
						io.github.jimbozoomer.jugcraft.warehouse.Leg.Key k = leg.at(t);
						int px = ox + Math.round((k.x() - f.x0()) * cell), py = oy + Math.round((k.z() - f.z0()) * cell);
						g.fill(px, py, px + 1, py + 1, WhGui.WHITE);
					}
				}
				io.github.jimbozoomer.jugcraft.warehouse.Leg.Key k = leg.at(now + part);
				int s = Math.max(3, cell - 2), px = ox + Math.round((k.x() - f.x0()) * cell) - s / 2, py = oy + Math.round((k.z() - f.z0()) * cell) - s / 2;
				boolean pallet = v.droneKind() == io.github.jimbozoomer.jugcraft.warehouse.DroneKind.PALLET;
				int colour = (v.flags() & io.github.jimbozoomer.jugcraft.warehouse.Drone.OUT_OF_SERVICE) != 0 ? WhGui.RED
						: (v.flags() & io.github.jimbozoomer.jugcraft.warehouse.Drone.PAUSED) != 0 ? WhGui.GREY : pallet ? WhGui.AMBER : WhGui.GREEN;
				if (now < v.locateUntil() && (now / 5) % 2 == 0) {
					colour = C_LOCATE;
				}
				g.fill(px, py, px + s, py + s, colour);
				if (v.id() == droneSel) {
					g.outline(px - 1, py - 1, s + 2, s + 2, WhGui.WHITE);
					String name = String.format(Locale.ROOT, "%s-%02d", v.droneKind().prefix, v.id());
					tiny(g, name, px + s + 2, py - 1, WhGui.WHITE);
				}
			}
		}
		g.disableScissor();
		// The legend.
		int gx = lx0 + 4, gy = ly1 - 10;
		Object[][] legend = showCounted ? new Object[][] {{"today", WhGui.GREEN}, {"older", C_PART}, {"never", C_EMPTY}, {"pallet drone", WhGui.AMBER},
				{"stacker", WhGui.GREEN}} : new Object[][] {{"full", C_FULL}, {"part", C_PART}, {"empty", C_EMPTY}, {"lane", C_LANE}, {"walkway", C_WALK},
				{"pallet drone", WhGui.AMBER}, {"stacker", WhGui.GREEN}};
		for (Object[] l : legend) {
			g.fill(gx, gy + 1, gx + 5, gy + 6, (Integer) l[1]);
			tiny(g, (String) l[0], gx + 7, gy + 2, WhGui.GREY);
			gx += 12 + font.width((String) l[0]) / 2;
		}
		// The place under the pointer.
		int cx = (int) Math.floor((mouseX - ox) / (double) cell), cz = (int) Math.floor((mouseY - oy) / (double) cell);
		if (WhGui.in(mouseX, mouseY, geo[3], geo[4], geo[5], geo[6]) && cx >= 0 && cz >= 0 && cx < f.w() && cz < f.d()) {
			io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Column c = columns.get(net.minecraft.core.BlockPos.asLong(f.x0() + cx, 0, f.z0() + cz));
			if (c != null) {
				String a = c.label() + "  -  " + c.used() + " / " + c.levels() + " in use";
				String b = (c.parts() > 0 ? c.parts() + " part pallet" + (c.parts() == 1 ? "" : "s") + "  -  " : "") + "counted " + ago(now, c.counted());
				int w = Math.max(font.width(a), font.width(b)) + 8;
				int bx = Math.min(mouseX + 8, lx1 - w - 2), by = Math.max(ly0 + 18, mouseY - 26);
				g.fill(bx, by, bx + w, by + 22, 0xF00C141E);
				g.outline(bx, by, w, 22, WhGui.CYAN);
				g.text(font, a, bx + 4, by + 3, WhGui.WHITE, false);
				g.text(font, b, bx + 4, by + 12, WhGui.GREY, false);
			}
		}
		layoutSide(g, d, mouseX, mouseY);
	}

	private int rackColour(io.github.jimbozoomer.jugcraft.warehouse.MapPayload.@org.jspecify.annotations.Nullable Column c, long now) {
		if (c == null) {
			return C_EMPTY;
		}
		if (showCounted) {
			return c.counted() <= 0 ? C_EMPTY : now - c.counted() < 24000 ? 0xFF3C9A5A : C_PART;
		}
		if (c.used() == 0) {
			return C_EMPTY;
		}
		if (c.parts() > 0) {
			return C_PART;
		}
		return c.used() >= c.levels() ? C_FULL : C_SOME;
	}

	/** LAYOUT's right side: the drone picked (its buttons), housekeeping. */
	private void layoutSide(GuiGraphicsExtractor g, TerminalPayload d, int mouseX, int mouseY) {
		int rx0 = leftPos + MAP_X1 + 4, ry0 = topPos + 40, rx1 = leftPos + W - 8, ry1 = topPos + 150;
		TerminalPayload.DroneLine l = d.fleet().stream().filter(x -> x.id() == droneSel).findFirst().orElse(null);
		WhGui.box(g, font, rx0, ry0, rx1, ry1, l == null ? "DRONE" : l.name(), "");
		if (l == null) {
			g.text(font, "Click a drone", rx0 + 6, ry0 + 16, WhGui.DIM, false);
			g.text(font, "on the map.", rx0 + 6, ry0 + 26, WhGui.DIM, false);
		} else {
			boolean paused = l.flag(io.github.jimbozoomer.jugcraft.warehouse.Drone.PAUSED), oos = l.flag(io.github.jimbozoomer.jugcraft.warehouse.Drone.OUT_OF_SERVICE);
			g.text(font, (l.kind() == 0 ? "STACKER" : "PALLET DRONE") + "  FL " + l.floor(), rx0 + 6, ry0 + 14, WhGui.GREY, false);
			g.text(font, WhGui.fit(font, oos ? "OUT OF SERVICE" : paused ? "PAUSED" : l.job(), rx1 - rx0 - 12), rx0 + 6, ry0 + 24,
					oos ? WhGui.RED : paused ? WhGui.AMBER : WhGui.GREEN, false);
			g.text(font, WhGui.fit(font, l.where(), rx1 - rx0 - 12), rx0 + 6, ry0 + 34, WhGui.CYAN, false);
			WhGui.bar(g, rx0 + 6, ry0 + 45, rx1 - 30, ry0 + 50, l.battery(), l.battery() < 0.2F ? WhGui.RED : WhGui.GREEN);
			g.text(font, Math.round(l.battery() * 100) + "%", rx1 - 27, ry0 + 44, WhGui.WHITE, false);
			String[] b = {"LOCATE", paused ? "GO ON" : "PAUSE", "SEND HOME", oos ? "IN SERVICE" : "OUT OF SERVICE"};
			for (int k = 0; k < 4; k++) {
				int by = ry0 + 55 + k * 13;
				WhGui.button(g, font, rx0 + 6, by, rx1 - 6, by + 12, b[k], false, WhGui.in(mouseX, mouseY, rx0 + 6, by, rx1 - 6, by + 12), true);
			}
		}
		io.github.jimbozoomer.jugcraft.warehouse.ControlPayload c = WarehouseClient.control(menu.terminal);
		int hy0 = topPos + 152, hy1 = topPos + H - 28;
		WhGui.box(g, font, rx0, hy0, rx1, hy1, "HOUSEKEEPING", "");
		if (c == null) {
			return;
		}
		int yy = hy0 + 14;
		for (String line : wrap(c.house().isEmpty() ? "Nothing now." : c.house(), rx1 - rx0 - 12, 2)) {
			g.text(font, line, rx0 + 6, yy, c.house().isEmpty() ? WhGui.DIM : WhGui.AMBER, false);
			yy += 10;
		}
		for (String line : wrap(c.next(), rx1 - rx0 - 12, 2)) {
			g.text(font, line, rx0 + 6, yy, WhGui.GREY, false);
			yy += 10;
		}
		g.text(font, "COUNTED TODAY", rx0 + 6, hy1 - 22, WhGui.GREY, false);
		g.text(font, c.counted() + " / " + c.places(), rx0 + 6, hy1 - 12, WhGui.WHITE, false);
	}

	/** Words broken into lines that fit, at most n. */
	private List<String> wrap(String text, int width, int n) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String w : text.split(" ")) {
			String next = line.isEmpty() ? w : line + " " + w;
			if (font.width(next) > width && !line.isEmpty()) {
				out.add(line.toString());
				line = new StringBuilder(w);
				if (out.size() >= n) {
					return out;
				}
			} else {
				line = new StringBuilder(next);
			}
		}
		if (!line.isEmpty() && out.size() < n) {
			out.add(WhGui.fit(font, line.toString(), width));
		}
		return out;
	}

	private boolean layoutClick(double mx, double my) {
		int lx0 = leftPos + 8, ly0 = topPos + 40, lx1 = leftPos + MAP_X1;
		io.github.jimbozoomer.jugcraft.warehouse.MapPayload map = WarehouseClient.map(menu.terminal);
		if (map != null) {
			for (int k = 0; k < map.floors().size() && k < 6; k++) {
				int bx = lx0 + 4 + k * 36;
				if (WhGui.in(mx, my, bx, ly0 + 4, bx + 34, ly0 + 15)) {
					mapFloor = k;
					return true;
				}
			}
		}
		if (WhGui.in(mx, my, lx1 - 92, ly0 + 4, lx1 - 50, ly0 + 15) || WhGui.in(mx, my, lx1 - 48, ly0 + 4, lx1 - 4, ly0 + 15)) {
			showCounted = mx >= lx1 - 48;
			return true;
		}
		// A drone's button.
		int rx0 = leftPos + MAP_X1 + 4, rx1 = leftPos + W - 8, ry0 = topPos + 40;
		if (droneSel >= 0) {
			int[] action = {3, 0, 1, 2};
			for (int k = 0; k < 4; k++) {
				int by = ry0 + 55 + k * 13;
				if (WhGui.in(mx, my, rx0 + 6, by, rx1 - 6, by + 12)) {
					ClientPlayNetworking.send(new WarehouseActionPayload(menu.terminal, WarehouseActionPayload.DRONE, ItemStack.EMPTY, action[k], droneSel, 0, ""));
					return true;
				}
			}
		}
		// A drone on the map (the nearest one under the pointer).
		io.github.jimbozoomer.jugcraft.warehouse.TerminalBlockEntity te = terminalEntity();
		if (map == null || map.floors().isEmpty() || te == null || minecraft == null || minecraft.level == null) {
			return false;
		}
		io.github.jimbozoomer.jugcraft.warehouse.MapPayload.Floor f = map.floors().get(Math.max(0, Math.min(map.floors().size() - 1, mapFloor)));
		int[] geo = mapGeometry(f);
		if (!WhGui.in(mx, my, geo[3], geo[4], geo[5], geo[6])) {
			return false;
		}
		long now = minecraft.level.getGameTime();
		double best = Math.max(6, geo[0]) * Math.max(6, geo[0]);
		int pick = -1;
		for (io.github.jimbozoomer.jugcraft.warehouse.Drone.View v : te.clientFleet()) {
			if (v.leg().y() != f.y()) {
				continue;
			}
			io.github.jimbozoomer.jugcraft.warehouse.Leg.Key k = v.leg().at(now);
			double px = geo[1] + (k.x() - f.x0()) * geo[0], py = geo[2] + (k.z() - f.z0()) * geo[0];
			double dd = (px - mx) * (px - mx) + (py - my) * (py - my);
			if (dd < best) {
				best = dd;
				pick = v.id();
			}
		}
		droneSel = pick;
		return true;
	}

	private void control(GuiGraphicsExtractor g, TerminalPayload d, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		io.github.jimbozoomer.jugcraft.warehouse.ControlPayload c = WarehouseClient.control(menu.terminal);
		int lx0 = x + 8, lx1 = x + 212, rx0 = x + 216, rx1 = x + W - 8;
		if (c == null) {
			WhGui.box(g, font, lx0, y + 40, rx1, y + H - 28, "CONTROL", "");
			g.text(font, "CONNECTING...", lx0 + 6, y + 56, WhGui.GREY, false);
			return;
		}
		boolean may = c.mayChange();
		// Job priorities.
		int py0 = y + 40, py1 = y + 132;
		WhGui.box(g, font, lx0, py0, lx1, py1, "JOB PRIORITIES", "first to last");
		for (int k = 0; k < c.priorities().size(); k++) {
			int group = c.priorities().get(k), ry = py0 + 14 + k * 16;
			g.fill(lx0 + 4, ry, lx1 - 4, ry + 14, k == 0 ? WhGui.SELECT : 0xFF0C141E);
			g.outline(lx0 + 4, ry, lx1 - lx0 - 8, 14, k == 0 ? WhGui.CYAN_DIM : 0xFF1E3244);
			g.text(font, String.valueOf(k + 1), lx0 + 8, ry + 3, WhGui.CYAN, false);
			g.text(font, io.github.jimbozoomer.jugcraft.warehouse.ControlPayload.GROUPS[group], lx0 + 18, ry + 1, WhGui.WHITE, false);
			tiny(g, WhGui.fit(font, io.github.jimbozoomer.jugcraft.warehouse.ControlPayload.GROUP_WHAT[group], 2 * (lx1 - lx0 - 60)), lx0 + 18, ry + 9, WhGui.GREY);
			WhGui.button(g, font, lx1 - 34, ry + 2, lx1 - 21, ry + 12, "^", false, WhGui.in(mouseX, mouseY, lx1 - 34, ry + 2, lx1 - 21, ry + 12), may && k > 0);
			WhGui.button(g, font, lx1 - 19, ry + 2, lx1 - 6, ry + 12, "v", false, WhGui.in(mouseX, mouseY, lx1 - 19, ry + 2, lx1 - 6, ry + 12),
					may && k < c.priorities().size() - 1);
		}
		tiny(g, "Always first: lift transfers, pallets back off hatches and stations.", lx0 + 6, py1 - 13, WhGui.GREY);
		tiny(g, "A RUSH order (ORDERS page) goes ahead of everything.", lx0 + 6, py1 - 7, WhGui.AMBER);
		// Housekeeping.
		int hy0 = y + 134, hy1 = y + H - 28;
		WhGui.box(g, font, lx0, hy0, lx1, hy1, "HOUSEKEEPING", "when idle");
		g.text(font, "CONSOLIDATE", lx0 + 6, hy0 + 15, WhGui.GREY, false);
		String[] cm = {"OFF", "WHEN IDLE"};
		for (int k = 0; k < 2; k++) {
			int bx = lx0 + 76 + k * 56;
			WhGui.button(g, font, bx, hy0 + 13, bx + 54, hy0 + 24, cm[k], c.consolidate() == k, WhGui.in(mouseX, mouseY, bx, hy0 + 13, bx + 54, hy0 + 24), may);
		}
		g.text(font, "CYCLE COUNT", lx0 + 6, hy0 + 30, WhGui.GREY, false);
		String[] cc = {"OFF", "DAILY", "NONSTOP"};
		for (int k = 0; k < 3; k++) {
			int bx = lx0 + 76 + k * 42;
			WhGui.button(g, font, bx, hy0 + 28, bx + 40, hy0 + 39, cc[k], c.count() == k, WhGui.in(mouseX, mouseY, bx, hy0 + 28, bx + 40, hy0 + 39), may);
		}
		g.text(font, "COUNTED", lx0 + 6, hy0 + 45, WhGui.GREY, false);
		WhGui.bar(g, lx0 + 52, hy0 + 46, lx1 - 50, hy0 + 51, c.places() == 0 ? 0 : c.counted() / (double) c.places(), WhGui.GREEN);
		String counted = c.counted() + " / " + c.places();
		g.text(font, counted, lx1 - 6 - font.width(counted), hy0 + 45, WhGui.WHITE, false);
		int yy = hy0 + 57;
		for (String line : wrap(c.house().isEmpty() ? "Nothing to do now." : c.house(), lx1 - lx0 - 12, 2)) {
			g.text(font, line, lx0 + 6, yy, c.house().isEmpty() ? WhGui.DIM : WhGui.AMBER, false);
			yy += 10;
		}
		for (int k = 0; k < c.adjustments().size() && yy < hy1 - 8; k++) {
			tiny(g, WhGui.fit(font, c.adjustments().get(k), 2 * (lx1 - lx0 - 12)), lx0 + 6, yy + 1, WhGui.AMBER);
			yy += 6;
		}
		// Power.
		int qy0 = y + 40, qy1 = y + 156;
		WhGui.box(g, font, rx0, qy0, rx1, qy1, "POWER", c.unlimited() ? "unlimited (demo)" : c.boards() == 0 ? "" : c.boards() + " switchboard"
				+ (c.boards() == 1 ? "" : "s"));
		if (!c.unlimited() && c.boards() == 0) {
			g.text(font, "NO SWITCHBOARD", rx0 + 6, qy0 + 14, WhGui.RED, false);
			tiny(g, "No power: place a Warehouse Switchboard and bring a cable to it.", rx0 + 6, qy0 + 25, WhGui.GREY);
		} else {
			g.text(font, String.format(Locale.ROOT, "IN %,d  DRAW %,d JE/t", c.supply(), c.draw()), rx0 + 6, qy0 + 14, c.low() ? WhGui.AMBER : WhGui.CYAN, false);
			if (!c.unlimited()) {
				WhGui.bar(g, rx0 + 6, qy0 + 25, rx1 - 50, qy0 + 30, c.capacity() == 0 ? 0 : c.stored() / (double) c.capacity(), c.low() ? WhGui.AMBER : WhGui.GREEN);
				String pct = (c.capacity() == 0 ? 0 : Math.round(100.0 * c.stored() / c.capacity())) + "% STORED";
				tiny(g, pct, rx1 - 46, qy0 + 26, WhGui.WHITE);
			}
			if (c.mainOff()) {
				g.text(font, "MAIN SWITCH OFF", rx1 - 6 - font.width("MAIN SWITCH OFF"), qy0 + 14, WhGui.RED, false);
			}
		}
		io.github.jimbozoomer.jugcraft.warehouse.WarehousePower.Circuit[] circuits = io.github.jimbozoomer.jugcraft.warehouse.WarehousePower.Circuit.values();
		for (int k = 0; k < circuits.length; k++) {
			int ry = qy0 + 34 + k * 13;
			boolean on = c.circuitOn(k), shed = c.circuitShed(k);
			int lamp = !on ? WhGui.DIM : shed ? WhGui.AMBER : WhGui.GREEN;
			g.fill(rx0 + 6, ry + 3, rx0 + 11, ry + 8, lamp);
			g.text(font, WhGui.fit(font, circuits[k].label, 84), rx0 + 14, ry + 2, WhGui.WHITE, false);
			String load = c.load(k) + " JE/t";
			g.text(font, load, rx1 - 42 - font.width(load), ry + 2, WhGui.GREY, false);
			WhGui.button(g, font, rx1 - 38, ry + 1, rx1 - 6, ry + 12, on ? (shed ? "SHED" : "ON") : "OFF", on, WhGui.in(mouseX, mouseY, rx1 - 38, ry + 1,
					rx1 - 6, ry + 12), may);
		}
		// Access.
		int ay0 = y + 158, ay1 = y + 196;
		WhGui.box(g, font, rx0, ay0, rx1, ay1, "ACCESS", WhGui.fit(font, c.owner().isEmpty() ? "nobody's yet" : "owner " + c.owner(), 120));
		WhGui.button(g, font, rx0 + 6, ay0 + 14, rx0 + 70, ay0 + 26, "PERSONAL", !c.party(), WhGui.in(mouseX, mouseY, rx0 + 6, ay0 + 14, rx0 + 70, ay0 + 26),
				c.mayAccess());
		WhGui.button(g, font, rx0 + 72, ay0 + 14, rx0 + 136, ay0 + 26, "PARTY", c.party(), WhGui.in(mouseX, mouseY, rx0 + 72, ay0 + 14, rx0 + 136, ay0 + 26),
				c.mayAccess());
		tiny(g, c.party() ? "Your party uses it too; only the owner changes ACCESS." : "Only the owner uses its panels, terminal and boxes.", rx0 + 6,
				ay0 + 29, WhGui.GREY);
		// The fleet.
		int fy0 = y + 198, fy1 = y + H - 28;
		WhGui.box(g, font, rx0, fy0, rx1, fy1, "FLEET", c.signals().isEmpty() ? "" : c.signals().size() + " signal" + (c.signals().size() == 1 ? "" : "s"));
		WhGui.button(g, font, rx0 + 6, fy0 + 13, rx0 + 110, fy0 + 30, c.paused() ? "RESUME ALL DRONES" : "PAUSE ALL DRONES", c.paused(),
				WhGui.in(mouseX, mouseY, rx0 + 6, fy0 + 13, rx0 + 110, fy0 + 30), may);
		List<String> sig = c.signals().isEmpty() ? List.of("They finish their move", "and stop.") : c.signals();
		for (int k = 0; k < sig.size() && k < 3; k++) {
			tiny(g, WhGui.fit(font, sig.get(k), 2 * (rx1 - rx0 - 118)), rx0 + 114, fy0 + 14 + k * 6, c.signals().isEmpty() ? WhGui.GREY : WhGui.AMBER);
		}
	}

	private boolean controlClick(double mx, double my) {
		io.github.jimbozoomer.jugcraft.warehouse.ControlPayload c = WarehouseClient.control(menu.terminal);
		if (c == null) {
			return false;
		}
		int x = leftPos, y = topPos, lx0 = x + 8, lx1 = x + 212, rx0 = x + 216, rx1 = x + W - 8;
		int py0 = y + 40;
		for (int k = 0; k < c.priorities().size(); k++) {
			int ry = py0 + 14 + k * 16, group = c.priorities().get(k);
			if (WhGui.in(mx, my, lx1 - 34, ry + 2, lx1 - 21, ry + 12) && k > 0) {
				send(WarehouseActionPayload.PRIORITY, group, ItemStack.EMPTY, -1);
				return true;
			}
			if (WhGui.in(mx, my, lx1 - 19, ry + 2, lx1 - 6, ry + 12) && k < c.priorities().size() - 1) {
				send(WarehouseActionPayload.PRIORITY, group, ItemStack.EMPTY, 1);
				return true;
			}
		}
		int hy0 = y + 134;
		for (int k = 0; k < 2; k++) {
			int bx = lx0 + 76 + k * 56;
			if (WhGui.in(mx, my, bx, hy0 + 13, bx + 54, hy0 + 24)) {
				send(WarehouseActionPayload.CONSOL_MODE, k, ItemStack.EMPTY, 0);
				return true;
			}
		}
		for (int k = 0; k < 3; k++) {
			int bx = lx0 + 76 + k * 42;
			if (WhGui.in(mx, my, bx, hy0 + 28, bx + 40, hy0 + 39)) {
				send(WarehouseActionPayload.COUNT_MODE, k, ItemStack.EMPTY, 0);
				return true;
			}
		}
		int qy0 = y + 40;
		for (int k = 0; k < io.github.jimbozoomer.jugcraft.warehouse.WarehousePower.Circuit.values().length; k++) {
			int ry = qy0 + 34 + k * 13;
			if (WhGui.in(mx, my, rx1 - 38, ry + 1, rx1 - 6, ry + 12)) {
				send(WarehouseActionPayload.CIRCUIT, k, ItemStack.EMPTY, 0);
				return true;
			}
		}
		int ay0 = y + 158;
		if (WhGui.in(mx, my, rx0 + 6, ay0 + 14, rx0 + 70, ay0 + 26) || WhGui.in(mx, my, rx0 + 72, ay0 + 14, rx0 + 136, ay0 + 26)) {
			send(WarehouseActionPayload.ACCESS, mx >= rx0 + 72 ? 1 : 0, ItemStack.EMPTY, 0);
			return true;
		}
		int fy0 = y + 198;
		if (WhGui.in(mx, my, rx0 + 6, fy0 + 13, rx0 + 110, fy0 + 30)) {
			send(WarehouseActionPayload.PAUSE_ALL, 0, ItemStack.EMPTY, 0);
			return true;
		}
		return false;
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		for (int k = 0; k < LIVE_TABS; k++) {
			int tx = leftPos + 8 + k * 64;
			if (WhGui.in(mx, my, tx, topPos + 24, tx + 60, topPos + 36)) {
				tab = k;
				return true;
			}
		}
		if (tab == 0) {
			for (int k = 0; k < FILTERS.length; k++) {
				int bx = leftPos + 134 + k * 42;
				if (WhGui.in(mx, my, bx, topPos + 44, bx + 40, topPos + 56)) {
					filter = k;
					scroll = 0;
					selected = -1;
					return true;
				}
			}
		}
		if (tab == 0 && requestClick(mx, my)) {
			return true;
		}
		if (tab == 2 && ordersClick(mx, my)) {
			return true;
		}
		if (tab == 3 && layoutClick(mx, my)) {
			return true;
		}
		if (tab == 4 && controlClick(mx, my)) {
			return true;
		}
		if (tab == BUILDING_TAB && buildingClick(mx, my)) {
			return true;
		}
		if (tab == 0) {
			int x0 = leftPos + 10, y0 = topPos + 76;
			for (int k = 0; k < visibleRows(); k++) {
				if (WhGui.in(mx, my, x0, y0 + k * ROW, leftPos + 254, y0 + (k + 1) * ROW) && scroll + k < items().size()) {
					selected = scroll + k;
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private boolean requestClick(double mx, double my) {
		List<TerminalPayload.Item> items = items();
		if (selected < 0 || selected >= items.size()) {
			return false;
		}
		TerminalPayload.Item it = items.get(selected);
		int rx0 = leftPos + 270, rx1 = leftPos + W - 8, ry1 = topPos + H - 28, mid = (rx0 + rx1) / 2;
		boolean liquid = io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(it.item());
		long stack = Math.max(1, it.item().getMaxStackSize()), caseSize = CaseLoad.capacity(it.item());
		// (A liquid's amounts in buckets.)
		long drum = io.github.jimbozoomer.jugcraft.warehouse.Drums.CAPACITY / 1000;
		long[] quick = liquid ? new long[] {parse() + 1, parse() + drum, drum * io.github.jimbozoomer.jugcraft.warehouse.Drums.PER_PALLET,
				Math.max(1, it.count() / 1000)} : new long[] {parse() + stack, caseSize, caseSize * 8, it.count()};
		for (int k = 0; k < 4; k++) {
			int bx = rx0 + 6 + k * 33;
			if (WhGui.in(mx, my, bx, ry1 - 57, bx + 31, ry1 - 45)) {
				amount.setValue(String.valueOf(Math.min(9_999_999, quick[k])));
				return true;
			}
		}
		if (WhGui.in(mx, my, rx0 + 6, ry1 - 41, mid - 1, ry1 - 29)) {
			dest = Order.WINDOW;
			return true;
		}
		if (WhGui.in(mx, my, mid + 1, ry1 - 41, rx1 - 6, ry1 - 29)) {
			dest = Order.PIPES;
			return true;
		}
		if (WhGui.in(mx, my, rx0 + 6, ry1 - 25, rx1 - 6, ry1 - 9) && parse() > 0) {
			send(WarehouseActionPayload.ORDER, dest, it.item(), liquid ? parse() * 1000 : parse());
			return true;
		}
		return false;
	}

	private boolean ordersClick(double mx, double my) {
		int lx0 = leftPos + 8, ly0 = topPos + 40, lx1 = leftPos + 262, ly1 = topPos + H - 28;
		if (WhGui.in(mx, my, lx0 + 4, ly0 + 4, lx0 + 40, ly0 + 15)) {
			allOrders = false;
			orderPick = -1;
			return true;
		}
		if (WhGui.in(mx, my, lx0 + 42, ly0 + 4, lx0 + 70, ly0 + 15)) {
			allOrders = true;
			orderPick = -1;
			return true;
		}
		List<TerminalPayload.OrderLine> list = orders();
		int yy = ly0 + 34;
		for (int i = 0; i < list.size() && yy < ly1 - 12; i++) {
			if (WhGui.in(mx, my, lx0 + 2, yy - 2, lx1 - 2, yy + 11)) {
				orderPick = i;
				return true;
			}
			yy += 14;
		}
		TerminalPayload.OrderLine o = orderPick >= 0 && orderPick < list.size() ? list.get(orderPick) : null;
		if (o != null && o.state() == Order.READY && o.dest() == Order.WINDOW && io.github.jimbozoomer.jugcraft.warehouse.Drums.isLiquid(o.item())) {
			int rx0 = leftPos + 270, rx1 = leftPos + W - 8, mid = (rx0 + rx1) / 2, sy = topPos + 40 + 52 + 48;
			if (WhGui.in(mx, my, rx0 + 6, sy + 24, mid - 1, sy + 36)) {
				send(WarehouseActionPayload.LIQUID_TO_PIPES, o.id(), ItemStack.EMPTY, 0);
				return true;
			}
		}
		if (o != null && o.state() <= Order.PICKING) {
			int rx0 = leftPos + 270, rx1 = leftPos + W - 8, mid = (rx0 + rx1) / 2, sy = topPos + 40 + 52 + 48;
			if (WhGui.in(mx, my, rx0 + 6, sy + 24, mid - 1, sy + 36) && o.dest() == Order.WINDOW) {
				send(WarehouseActionPayload.TO_PIPES, o.id(), ItemStack.EMPTY, 0);
				return true;
			}
			if (WhGui.in(mx, my, mid + 1, sy + 24, rx1 - 6, sy + 36)) {
				send(WarehouseActionPayload.CANCEL, o.id(), ItemStack.EMPTY, 0);
				return true;
			}
			if (WhGui.in(mx, my, rx0 + 6, sy + 38, rx1 - 6, sy + 50)) {
				send(WarehouseActionPayload.RUSH, o.id(), ItemStack.EMPTY, 0);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
		if (tab == 0) {
			int max = Math.max(0, items().size() - visibleRows());
			scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
			return true;
		}
		if (tab == BUILDING_TAB) {
			floorScroll = Math.max(0, floorScroll - (int) Math.signum(dy));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, dx, dy);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		// Typing in the search box mustn't close the screen (the inventory key).
		if ((search != null && search.isFocused() || amount != null && amount.isFocused()) && minecraft != null
				&& minecraft.options.keyInventory.matches(event)) {
			return true;
		}
		return super.keyPressed(event);
	}
}
