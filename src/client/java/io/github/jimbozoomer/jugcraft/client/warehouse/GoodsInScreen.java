package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.nuclear.TscRoom;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsInMenu;
import io.github.jimbozoomer.jugcraft.warehouse.GoodsInPayload;
import io.github.jimbozoomer.jugcraft.warehouse.IntakeBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.TerminalPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The GOODS IN screen (the mockup warehouse_ui_goods_in_v2): the intake's 54-slot drop-off bay, what the case packer is
 * packing and how far, the intake's state (the packer, cases on the conveyor, pallets being built, pallet places), each
 * pipe inlet's flow, INSERT ALL / INSERT MATCHING / TAKE ALL BACK, the player's inventory and the last pallets put away.
 */
public class GoodsInScreen extends AbstractContainerScreen<GoodsInMenu> {
	private static final String[] BUTTONS = {"INSERT ALL", "INSERT MATCHING", "TAKE ALL BACK"};

	public GoodsInScreen(GoodsInMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, GoodsInMenu.WIDTH, GoodsInMenu.HEIGHT);
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
		GoodsInPayload s = WarehouseClient.goodsIn();
		boolean panel = menu.panel != null && !menu.panel.equals(menu.intake);
		WhGui.window(g, font, x, y, x + imageWidth, y + imageHeight, "GOODS IN", panel ? "DROP-OFF HATCH" : "INTAKE CASE PACKER");
		int right = x + imageWidth - 9;
		if (s != null) {
			int free = s.placesTotal() - s.placesUsed();
			right = WhGui.chip(g, font, right, y + 4, free + " PALLET PLACES FREE", WhGui.CYAN) - 4;
			WhGui.chip(g, font, right, y + 4, s.networked() ? "INTAKE RUNNING" : "NO TERMINAL", s.networked() ? WhGui.GREEN : WhGui.AMBER);
		}

		// The bay.
		int bx0 = x + 8, by0 = y + 26, bx1 = x + 178, by1 = y + 178;
		int used = 0;
		for (int i = 0; i < IntakeBlockEntity.BAY; i++) {
			if (menu.getSlot(i).hasItem()) {
				used++;
			}
		}
		WhGui.box(g, font, bx0, by0, bx1, by1, "DROP-OFF BAY", "54 slots");
		for (int r = 0; r < 6; r++) {
			for (int c = 0; c < 9; c++) {
				WhGui.slotWell(g, x + GoodsInMenu.BAY_X + c * 18, y + GoodsInMenu.BAY_Y + r * 18);
			}
		}
		int py = by0 + 125;
		if (s != null && !s.packing().isEmpty()) {
			g.text(font, "PACKING", bx0 + 5, py, WhGui.AMBER, false);
			String what = WhGui.fit(font, s.packing().getHoverName().getString(), 92) + " -> case";
			g.text(font, what, bx0 + 50, py, WhGui.WHITE, false);
			WhGui.bar(g, bx0 + 5, py + 10, bx1 - 5, py + 15, s.progress(), WhGui.AMBER);
		} else {
			g.text(font, "PACKER IDLE", bx0 + 5, py, WhGui.CYAN, false);
			WhGui.bar(g, bx0 + 5, py + 10, bx1 - 5, py + 15, 0, WhGui.AMBER);
		}
		g.text(font, used + " / 54 slots used", bx0 + 5, py + 18, WhGui.GREY, false);

		// The intake.
		int rx0 = x + 186, ry0 = y + 26, rx1 = x + 344, ry1 = y + 134;
		WhGui.box(g, font, rx0, ry0, rx1, ry1, "INTAKE", "case packer");
		if (s != null) {
			boolean packing = !s.packing().isEmpty();
			row(g, rx0, rx1, ry0 + 15, "Case packer", packing ? "PACKING" : "IDLE", packing ? WhGui.GREEN : WhGui.CYAN);
			row(g, rx0, rx1, ry0 + 26, "Cases on the conveyor", s.conveyor() + " / " + IntakeBlockEntity.CONVEYOR,
					s.conveyor() >= IntakeBlockEntity.CONVEYOR ? WhGui.AMBER : WhGui.WHITE);
			row(g, rx0, rx1, ry0 + 37, "Pallets being built", String.valueOf(s.building()), WhGui.WHITE);
			g.text(font, "Pallet places", rx0 + 5, ry0 + 48, WhGui.GREY, false);
			WhGui.bar(g, rx0 + 70, ry0 + 49, rx1 - 46, ry0 + 55, s.placesTotal() == 0 ? 0 : s.placesUsed() / (double) s.placesTotal(), WhGui.CYAN);
			String places = s.placesUsed() + "/" + s.placesTotal();
			g.text(font, places, rx1 - 5 - font.width(places), ry0 + 48, WhGui.WHITE, false);
		}
		g.fill(rx0 + 5, ry0 + 60, rx1 - 5, ry0 + 61, WhGui.LINE);
		g.text(font, "PIPE INLETS", rx0 + 5, ry0 + 64, WhGui.CYAN, false);
		if (!panel) {
			g.text(font, "(on a Goods In panel)", rx0 + 5, ry0 + 76, WhGui.DIM, false);
		} else if (s != null) {
			for (int i = 0; i < s.inlets().size(); i++) {
				GoodsInPayload.Inlet in = s.inlets().get(i);
				int yy = ry0 + 75 + i * 10;
				int colour = in.backed() ? WhGui.AMBER : in.rate() > 0 ? WhGui.GREEN : in.piped() ? WhGui.CYAN : WhGui.DIM;
				g.fill(rx0 + 6, yy + 2, rx0 + 10, yy + 6, colour);
				g.text(font, "IN" + (i + 1), rx0 + 14, yy, WhGui.WHITE, false);
				if (!in.last().isEmpty()) {
					WhGui.smallItem(g, in.last(), rx0 + 34, yy);
				}
				String name = !in.piped() ? "no pipe" : in.last().isEmpty() ? "waiting" : WhGui.fit(font, in.last().getHoverName().getString(), 70);
				g.text(font, name, rx0 + 45, yy, in.piped() ? WhGui.WHITE : WhGui.DIM, false);
				String rate = in.backed() ? "BACKED UP" : in.rate() > 0 ? in.rate() + "/s" : "";
				g.text(font, rate, rx1 - 5 - font.width(rate), yy, colour, false);
			}
		}

		// The buttons under it.
		int bw = (rx1 - rx0 - 6) / 3;
		for (int k = 0; k < 3; k++) {
			int b0 = rx0 + k * (bw + 3);
			WhGui.button(g, font, b0, ry1 + 5, b0 + bw, ry1 + 17, BUTTONS[k], false, WhGui.in(mouseX, mouseY, b0, ry1 + 5, b0 + bw, ry1 + 17), true);
		}

		// The player's inventory and the put-away log.
		g.text(font, "INVENTORY", x + 12, y + GoodsInMenu.INV_Y - 11, WhGui.CYAN, false);
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				WhGui.slotWell(g, x + GoodsInMenu.INV_X + c * 18, y + GoodsInMenu.INV_Y + r * 18);
			}
		}
		for (int c = 0; c < 9; c++) {
			WhGui.slotWell(g, x + GoodsInMenu.INV_X + c * 18, y + GoodsInMenu.HOTBAR_Y);
		}
		int lx0 = x + 186, ly0 = y + 157, lx1 = x + 344, ly1 = y + 272;
		WhGui.box(g, font, lx0, ly0, lx1, ly1, "PUT AWAY", "last 8");
		if (s != null && minecraft != null && minecraft.level != null) {
			long gameNow = minecraft.level.getGameTime();
			for (int i = 0; i < s.log().size() && i < 8; i++) {
				TerminalPayload.Logged l = s.log().get(i);
				int yy = ly0 + 14 + i * 11;
				g.text(font, TscRoom.clock(s.time() - (gameNow - l.time())), lx0 + 5, yy, WhGui.GREY, false);
				WhGui.smallItem(g, l.item(), lx0 + 33, yy);
				String what = WhGui.fit(font, WhGui.what(l.item(), l.count()), 104);
				g.text(font, what, lx0 + 44, yy, WhGui.WHITE, false);
				g.text(font, l.place(), lx1 - 5 - font.width(l.place()), yy, WhGui.CYAN, false);
			}
			if (s.log().isEmpty()) {
				g.text(font, s.networked() ? "Nothing stored yet" : "No terminal: nothing is put away", lx0 + 5, ly0 + 16, WhGui.DIM, false);
			}
		}
	}

	private void row(GuiGraphicsExtractor g, int x0, int x1, int y, String label, String value, int colour) {
		g.text(font, label, x0 + 5, y, WhGui.GREY, false);
		g.text(font, value, x1 - 5 - font.width(value), y, colour, false);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int rx0 = leftPos + 186, rx1 = leftPos + 344, ry1 = topPos + 134;
		int bw = (rx1 - rx0 - 6) / 3;
		for (int k = 0; k < 3; k++) {
			int b0 = rx0 + k * (bw + 3);
			if (WhGui.in(event.x(), event.y(), b0, ry1 + 5, b0 + bw, ry1 + 17) && minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, k);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
