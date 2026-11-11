package io.github.jimbozoomer.jugcraft.client.warehouse;

import io.github.jimbozoomer.jugcraft.warehouse.Drums;
import io.github.jimbozoomer.jugcraft.warehouse.FillerBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsInBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsInMenu;
import io.github.jimbozoomer.jugcraft.warehouse.LiquidsPayload;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The LIQUIDS IN screen (part W4; the mockup warehouse_W4_screens): each inlet (what comes in and how fast, waiting its
 * turn, no pipe), the TIP BUCKETS slots with TIP ALL and TAKE BACK, the Drum Filling Station behind the panel (its
 * buffer tank, the drum it fills, its empty drums by kind, its pallets), the player's inventory.
 */
public class LiquidsInScreen extends AbstractContainerScreen<LiquidsInMenu> {
	private static final String[] STATES = {"WAITING", "PALLET OUT", "FILLING", "STRAPPING", "PALLET READY", "STOWING EMPTIES", "PALLET BACK"};

	public LiquidsInScreen(LiquidsInMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, LiquidsInMenu.WIDTH, LiquidsInMenu.HEIGHT);
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
		LiquidsPayload s = WarehouseClient.liquids();
		if (s != null && !s.in()) {
			s = null;
		}
		WhGui.window(g, font, x, y, x + imageWidth, y + imageHeight, "LIQUIDS IN", "the panel  -  feeds the Drum Filling Station behind it");
		if (s != null) {
			WhGui.chip(g, font, x + imageWidth - 9, y + 4, s.linked() ? "FILLING STATION" : "NO FILLING STATION", s.linked() ? WhGui.GREEN : WhGui.AMBER);
		}
		// The inlets.
		int lx0 = x + 8, lx1 = x + 178;
		WhGui.box(g, font, lx0, y + 26, lx1, y + 124, "INLETS", "pipes on the panel's face");
		for (int i = 0; i < LiquidsInBlockEntity.INLETS; i++) {
			int ry = y + 42 + i * 16;
			LiquidsPayload.Inlet in = s == null || i >= s.inlets().size() ? null : s.inlets().get(i);
			int dot = in == null ? WhGui.DIM : in.rate() > 0 ? WhGui.GREEN : in.waits() ? WhGui.AMBER : in.piped() ? WhGui.CYAN_DIM : WhGui.DIM;
			g.fill(lx0 + 6, ry + 1, lx0 + 12, ry + 7, dot);
			g.text(font, i == LiquidsInBlockEntity.TANKER ? "TANKER" : "LIQ " + (i + 1), lx0 + 16, ry, WhGui.WHITE, false);
			String what = in == null ? "" : in.piped() || in.rate() > 0 ? (in.liquid().isBlank() ? "waiting" : Drums.name(in.liquid()).getString())
					: i == LiquidsInBlockEntity.TANKER ? "not coupled" : "no pipe";
			g.text(font, WhGui.fit(font, what, 70), lx0 + 60, ry, in != null && in.rate() > 0 ? WhGui.WHITE : WhGui.GREY, false);
			String rate = in == null ? "" : in.rate() > 0 ? Drums.buckets(in.rate()) + "/s" : in.waits() ? "WAITS" : "";
			g.text(font, rate, lx1 - 6 - font.width(rate), ry, in != null && in.waits() && in.rate() == 0 ? WhGui.AMBER : WhGui.GREEN, false);
		}
		// The funnel.
		WhGui.box(g, font, lx0, y + 128, lx1, y + 170, "TIP BUCKETS", "into the funnel");
		for (int c = 0; c < LiquidsInBlockEntity.SLOTS; c++) {
			WhGui.slotWell(g, x + LiquidsInMenu.TIP_X + c * 18, y + LiquidsInMenu.TIP_Y);
		}
		// The filling station.
		int rx0 = x + 186, rx1 = x + 344;
		WhGui.box(g, font, rx0, y + 26, rx1, y + 146, "DRUM FILLING STATION", "DF-1");
		LiquidsPayload.Station st = s == null ? LiquidsPayload.Station.NONE : s.station();
		g.text(font, "Buffer tank", rx0 + 6, y + 42, WhGui.GREY, false);
		WhGui.bar(g, rx0 + 6, y + 53, rx1 - 6, y + 59, st.tank() / (double) FillerBlockEntity.TANK, WhGui.AMBER);
		String tank = Drums.buckets(st.tank()) + " / " + Drums.buckets(FillerBlockEntity.TANK) + (st.liquid().isBlank() ? ""
				: "  " + Drums.name(st.liquid()).getString().toUpperCase(Locale.ROOT));
		g.text(font, WhGui.fit(font, tank, rx1 - rx0 - 12), rx0 + 6, y + 62, WhGui.WHITE, false);
		String now = st.drum() > 0 ? "filling drum " + st.drum() + " of 4  " + Drums.buckets(st.filled()) + " / " + Drums.buckets(Drums.CAPACITY)
				: STATES[Math.max(0, Math.min(STATES.length - 1, st.state()))].toLowerCase(Locale.ROOT);
		g.text(font, "Now", rx0 + 6, y + 76, WhGui.GREY, false);
		g.text(font, WhGui.fit(font, now, rx1 - rx0 - 36), rx0 + 30, y + 76, st.drum() > 0 ? WhGui.AMBER : WhGui.WHITE, false);
		g.text(font, "Empty drums", rx0 + 6, y + 90, WhGui.GREY, false);
		String[] kinds = {"STEEL", "RED", "HDPE", "DEWAR"};
		int[] colours = {0xFF5A8CDC, 0xFFDC463C, 0xFFE2DECC, 0xFFC8CCD2};
		int cw = (rx1 - rx0 - 12 - 9) / 4;
		for (int k = 0; k < 4; k++) {
			int cx = rx0 + 6 + k * (cw + 3);
			g.fill(cx, y + 100, cx + cw, y + 112, WhGui.WELL);
			g.outline(cx, y + 100, cw, 12, colours[k]);
			String t = kinds[k] + " " + (k < st.drums().size() ? st.drums().get(k) : 0);
			g.text(font, t, cx + cw / 2 - font.width(t) / 2, y + 102, colours[k], false);
		}
		g.text(font, "Pallets  " + st.pallets() + "     free places  " + st.free(), rx0 + 6, y + 118, WhGui.GREY, false);
		if (!st.note().isEmpty()) {
			g.text(font, WhGui.fit(font, st.note(), rx1 - rx0 - 12), rx0 + 6, y + 131, WhGui.AMBER, false);
		}
		// TIP ALL, TAKE BACK.
		int mid = (rx0 + rx1) / 2;
		WhGui.button(g, font, rx0, y + 150, mid - 2, y + 166, "TIP ALL", true, WhGui.in(mouseX, mouseY, rx0, y + 150, mid - 2, y + 166), true);
		WhGui.button(g, font, mid + 2, y + 150, rx1, y + 166, "TAKE BACK", false, WhGui.in(mouseX, mouseY, mid + 2, y + 150, rx1, y + 166), true);
		// The player's inventory.
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				WhGui.slotWell(g, x + LiquidsInMenu.INV_X + c * 18, y + LiquidsInMenu.INV_Y + r * 18);
			}
		}
		for (int c = 0; c < 9; c++) {
			WhGui.slotWell(g, x + LiquidsInMenu.INV_X + c * 18, y + LiquidsInMenu.HOTBAR_Y);
		}
		g.text(font, "Gases and lava are turned away.", rx0, y + 176, WhGui.DIM, false);
		g.text(font, "Load empty drums and pallets", rx0, y + 190, WhGui.DIM, false);
		g.text(font, "by using them on the station.", rx0, y + 200, WhGui.DIM, false);
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int rx0 = leftPos + 186, rx1 = leftPos + 344, mid = (rx0 + rx1) / 2, y = topPos;
		if (minecraft != null && minecraft.gameMode != null) {
			if (WhGui.in(event.x(), event.y(), rx0, y + 150, mid - 2, y + 166)) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LiquidsInMenu.TIP_ALL);
				return true;
			}
			if (WhGui.in(event.x(), event.y(), mid + 2, y + 150, rx1, y + 166)) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, LiquidsInMenu.TAKE_BACK);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
