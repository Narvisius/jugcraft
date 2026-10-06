package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nuclear.ConsoleActionPayload;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import io.github.jimbozoomer.jugcraft.nuclear.EquipmentKind;
import io.github.jimbozoomer.jugcraft.nuclear.ReactorCore;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The Reactor Control Console's screen (the approved sci-fi design): the core map with its rod clusters and each
 * assembly's heat, the rod slider, the plant readouts, ten minutes of core temperature with the alarm and SCRAM lines,
 * the annunciator windows, the protection switch, the SCRAM button, the fuel line and REFUEL. Everything shown comes
 * from the linked vessel's synced block entity; every button is a {@link ConsoleActionPayload} the server checks.
 * The screen is laid out at 504 x 348 and scaled to fit.
 */
public class ReactorConsoleScreen extends Screen {
	static final int W = 504, H = 348;
	static final Identifier SCRAM_TEXTURE = Jugcraft.id("textures/gui/reactor_scram.png");
	static final int BG = 0xF00E1820, PANEL = 0xF0132029, EDGE = 0xFF2B5566, ACCENT = 0xFF4FC8E8, TEXT = 0xFFBCD4DC, DIM = 0xFF6F8A94,
			GREEN = 0xFF5ADC78, AMBER = 0xFFF0B030, RED = 0xFFF04034, WHITE = 0xFFFFFFFF;
	/** The annunciator windows: alarm bit and label key (in this order on the panel). */
	static final int[][] ANNUNCIATORS = {{ReactorCore.A_HIGH_TEMP}, {ReactorCore.A_SCRAM}, {ReactorCore.A_LOW_WATER}, {ReactorCore.A_LOOPS},
			{ReactorCore.A_NO_POWER_OUT}, {ReactorCore.A_PROTECTION_OFF}, {ReactorCore.A_FUEL_SPENT}, {ReactorCore.A_FUEL_DAMAGED},
			{ReactorCore.A_NOT_FORMED}, {ReactorCore.A_REFUEL}, {ReactorCore.A_NO_FUEL}, {0}};
	static final String[] ANNUNCIATOR_KEYS = {"high_temp", "scram", "low_water", "loops", "no_power_out", "protection_off",
			"fuel_spent", "fuel_damaged", "not_formed", "refuel", "no_fuel", "rad_monitor"};

	private final BlockPos console;
	private float scale = 1;
	private int left, top;
	private boolean dragging;
	private int dragValue = -1;

	public ReactorConsoleScreen(BlockPos console) {
		super(Component.translatable("screen.jugcraft.reactor.title"));
		this.console = console;
	}

	@Override
	protected void init() {
		scale = Math.min(1F, Math.min((width - 8) / (float) W, (height - 8) / (float) H));
		left = Math.round((width / scale - W) / 2);
		top = Math.round((height / scale - H) / 2);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private EquipmentBlockEntity consoleEntity() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.getBlockEntity(console) instanceof EquipmentBlockEntity e ? e : null;
	}

	private EquipmentBlockEntity vessel() {
		Minecraft mc = Minecraft.getInstance();
		EquipmentBlockEntity c = consoleEntity();
		if (c == null || c.linked == null || mc.level == null) return null;
		return mc.level.getBlockEntity(c.linked) instanceof EquipmentBlockEntity v && v.kind().reactor() ? v : null;
	}

	private void send(int action, int value) {
		ClientPlayNetworking.send(new ConsoleActionPayload(console, action, value));
	}

	private static String t(String key, Object... args) {
		return Component.translatable("screen.jugcraft.reactor." + key, args).getString();
	}

	// ------------------------------------------------------------------ drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		int mx = Math.round(mouseX / scale), my = Math.round(mouseY / scale);
		draw(g, mx, my);
		g.pose().popMatrix();
	}

	private void draw(GuiGraphicsExtractor g, int mx, int my) {
		int x0 = left, y0 = top;
		g.fill(x0, y0, x0 + W, y0 + H, BG);
		g.fill(x0, y0 + 20, x0 + W, y0 + 21, ACCENT);
		g.text(font, t("title"), x0 + 8, y0 + 6, ACCENT, false);
		EquipmentBlockEntity v = vessel();
		EquipmentBlockEntity c = consoleEntity();
		if (v == null || c == null) {
			g.text(font, c != null && c.linked != null ? t("out_of_range", c.linked.toShortString()) : t("unlinked"), x0 + 8, y0 + 40, AMBER, false);
			return;
		}
		ReactorCore core = v.core;
		String header = switch (core.design.id()) {
			case "epr" -> t("header_epr");
			case "smr" -> t("header_smr");
			default -> t("header", v.loops);
		};
		g.text(font, header, x0 + W / 2 - font.width(header) / 2, y0 + 6, DIM, false);
		String status;
		int statusColour;
		if ((core.alarms & ReactorCore.A_NOT_FORMED) != 0) {
			status = t("status.incomplete");
			statusColour = AMBER;
		} else if (core.scrammed) {
			status = t("status.scram");
			statusColour = RED;
		} else if (core.refuelOpen) {
			status = t("status.refuelling");
			statusColour = AMBER;
		} else if (core.fission > 0.005) {
			status = t("status.operating");
			statusColour = GREEN;
		} else {
			status = t("status.shut_down");
			statusColour = DIM;
		}
		g.text(font, status, x0 + W - 8 - font.width(status), y0 + 6, statusColour, false);

		coreMap(g, core, x0 + 6, y0 + 26);
		rods(g, core, x0 + 160, y0 + 26, mx, my);
		plant(g, v, core, x0 + 246, y0 + 26);
		trend(g, core, x0 + 246, y0 + 118);
		annunciators(g, core, x0 + 6, y0 + 198);
		protection(g, core, c, x0 + 352, y0 + 198, mx, my);
		fuel(g, core, x0 + 6, y0 + 300, mx, my);
	}

	private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
		g.fill(x, y, x + w, y + h, PANEL);
		g.outline(x, y, w, h, EDGE);
		g.fill(x + w - 8, y, x + w, y + 1, ACCENT);
		g.text(font, title, x + 6, y + 5, ACCENT, false);
		g.fill(x + 6, y + 15, x + w - 6, y + 16, EDGE);
	}

	private void coreMap(GuiGraphicsExtractor g, ReactorCore core, int x, int y) {
		panel(g, x, y, 150, 168, t("core_map"));
		int[] coreRows = core.design.rows();
		int rows = coreRows.length;
		int wide = 0;
		for (int n : coreRows) {
			wide = Math.max(wide, n);
		}
		// The map fills the same square whatever the core: 8 px cells for the standard core, smaller for the EPR's.
		int cell = Math.max(4, Math.min(16, 120 / Math.max(rows, wide)));
		int ox = x + 75 - (wide * cell) / 2, oy = y + 22 + (120 - rows * cell) / 2;
		double midRow = (rows - 1) / 2.0, midCol = (wide - 1) / 2.0;
		int index = 0;
		for (int r = 0; r < rows; r++) {
			int n = coreRows[r];
			int start = (wide - n) / 2;
			for (int k = 0; k < n; k++) {
				int col = start + k;
				int px = ox + col * cell, py = oy + r * cell;
				int colour;
				if (index >= core.fuel.size()) {
					colour = 0xFF1A2830;
				} else if (core.fuelDamaged) {
					colour = 0xFF5A2A20;
				} else {
					double burnup = core.fuel.get(index);
					if (burnup >= 1) {
						colour = 0xFF34404A;
					} else {
						// Heat: hotter towards the middle and with more power.
						double dr = (r - midRow) / Math.max(1, midRow), dc = (col - midCol) / Math.max(1, midCol);
						double heat = core.fission * (1.15 - 0.45 * (dr * dr + dc * dc)) * (1 - burnup * 0.3);
						colour = heatColour(heat);
					}
				}
				g.fill(px, py, px + cell - 1, py + cell - 1, colour);
				if (r % 2 == 1 && col % 2 == 1 && (r + col) % 4 == 2 && index < core.fuel.size()) {
					// A rod cluster: white when out, dark when in.
					int rodColour = core.rods > 99 ? 0xFF101418 : core.rods < 1 ? 0xFFE8EEF0 : 0xFF6C7880;
					g.fill(px + 2, py + 2, px + cell - 3, py + cell - 3, rodColour);
				}
				index++;
			}
		}
		g.text(font, t("core_legend", core.fuel.size(), core.design.size()), x + 6, y + 156, DIM, false);
	}

	static int heatColour(double heat) {
		heat = Math.max(0, Math.min(1.2, heat));
		int r, gr, b;
		if (heat < 0.5) {
			double t = heat / 0.5;
			r = (int) (90 + 90 * t);
			gr = (int) (130 + 60 * t);
			b = (int) (140 - 60 * t);
		} else {
			double t = Math.min(1, (heat - 0.5) / 0.6);
			r = (int) (180 + 60 * t);
			gr = (int) (190 - 110 * t);
			b = (int) (80 - 40 * t);
		}
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	static final int SLIDER_TOP = 50, SLIDER_HEIGHT = 112;

	private void rods(GuiGraphicsExtractor g, ReactorCore core, int x, int y, int mx, int my) {
		panel(g, x, y, 80, 168, t("rods"));
		int sx = x + 34, sy = y + 22, sh = 120;
		for (int k = 0; k <= 10; k++) {
			int ty = sy + sh * k / 10;
			g.fill(sx - 12, ty, sx - (k % 5 == 0 ? 2 : 6), ty + 1, DIM);
			if (k % 5 == 0) {
				String label = Integer.toString(k * 10);
				g.text(font, label, sx - 14 - font.width(label), ty - 4, DIM, false);
			}
		}
		g.fill(sx, sy, sx + 22, sy + sh, 0xFF0A1218);
		g.outline(sx, sy, 22, sh, EDGE);
		int rodsY = sy + (int) Math.round(sh * core.rods / 100);
		g.fill(sx + 2, sy + 1, sx + 20, rodsY, 0xFF5C6E78);
		int target = dragging && dragValue >= 0 ? dragValue : (int) Math.round(core.rodTarget);
		int handle = sy + sh * target / 100;
		g.fill(sx - 3, handle - 3, sx + 25, handle + 3, ACCENT);
		g.outline(sx - 3, handle - 3, 28, 6, WHITE);
		String in = t("rods_in", Math.round(core.rods));
		g.text(font, in, x + 40 - font.width(in) / 2, y + 150, TEXT, false);
	}

	private boolean onSlider(double mx, double my) {
		int sx = left + 160 + 34, sy = top + 26 + 22;
		return mx >= sx - 6 && mx <= sx + 28 && my >= sy - 4 && my <= sy + 124;
	}

	private int sliderValue(double my) {
		int sy = top + 26 + 22;
		return (int) Math.max(0, Math.min(100, Math.round((my - sy) * 100 / 120)));
	}

	private void plant(GuiGraphicsExtractor g, EquipmentBlockEntity v, ReactorCore core, int x, int y) {
		panel(g, x, y, 252, 88, t("plant"));
		String[][] rows = {
				{t("thermal"), String.format(Locale.ROOT, "%.0f %%", core.heat() * 100)},
				{t("electric"), String.format(Locale.ROOT, "%,d JE/t", core.electric)},
				{t("core_temp"), String.format(Locale.ROOT, "%.0f °C", core.temperature)},
				{t("coolant"), String.format(Locale.ROOT, "%.0f °C / %s", Math.max(20, core.temperature - 15 * core.heat()),
						(v.missing.contains("pressurizer") ? "0.1" : "15.5") + " MPa")},
				{t("steam"), t("steam_value", v.loops, Math.round(core.removed * 100))},
				{t("cooling_water"), String.format(Locale.ROOT, "%,d / %,d mB/t", Math.round(core.need * core.waterShare), core.need)},
		};
		int[] colours = {GREEN, GREEN, core.temperature >= ReactorCore.ALARM_TEMP ? RED : TEXT, TEXT, TEXT,
				core.waterShare < 0.95 && core.heat() > 0.01 ? AMBER : TEXT};
		for (int i = 0; i < rows.length; i++) {
			int ry = y + 20 + i * 11;
			g.text(font, rows[i][0], x + 8, ry, DIM, false);
			g.text(font, rows[i][1], x + 244 - font.width(rows[i][1]), ry, colours[i], false);
		}
	}

	private void trend(GuiGraphicsExtractor g, ReactorCore core, int x, int y) {
		panel(g, x, y, 252, 74, t("trend"));
		int gx = x + 8, gy = y + 20, gw = 236, gh = 48;
		double lo = 100, hi = 420;
		int scramY = gy + gh - (int) ((ReactorCore.SCRAM_TEMP - lo) / (hi - lo) * gh);
		int alarmY = gy + gh - (int) ((ReactorCore.ALARM_TEMP - lo) / (hi - lo) * gh);
		g.fill(gx, scramY, gx + gw, scramY + 1, RED);
		g.fill(gx, alarmY, gx + gw, alarmY + 1, AMBER);
		String scramLabel = t("scram_line", ReactorCore.SCRAM_TEMP);
		g.text(font, scramLabel, gx + gw - font.width(scramLabel), scramY - 9, RED, false);
		for (int i = 0; i < gw; i++) {
			int sample = (core.trendHead + (int) ((long) i * ReactorCore.TREND / gw)) % ReactorCore.TREND;
			int temp = core.trend[sample];
			if (temp <= 0) continue;
			int py = gy + gh - (int) Math.max(0, Math.min(gh, (temp - lo) / (hi - lo) * gh));
			g.fill(gx + i, py, gx + i + 1, py + 2, ACCENT);
		}
	}

	private void annunciators(GuiGraphicsExtractor g, ReactorCore core, int x, int y) {
		panel(g, x, y, 340, 98, t("annunciators"));
		boolean flash = (System.currentTimeMillis() / 500) % 2 == 0;
		for (int i = 0; i < ANNUNCIATORS.length; i++) {
			int col = i % 4, row = i / 4;
			int bx = x + 8 + col * 82, by = y + 22 + row * 24;
			boolean lit = ANNUNCIATORS[i][0] != 0 ? (core.alarms & ANNUNCIATORS[i][0]) != 0 : core.refuelOpen;
			boolean bad = lit && i != 9;
			int fill = lit ? (bad && flash ? 0xFF6A4810 : 0xFF4A3410) : 0xFF16262E;
			g.fill(bx, by, bx + 78, by + 20, fill);
			g.outline(bx, by, 78, 20, lit ? AMBER : EDGE);
			String label = t("ann." + ANNUNCIATOR_KEYS[i]);
			int colour = lit ? AMBER : DIM;
			g.text(font, fit(label, 74), bx + 39 - font.width(fit(label, 74)) / 2, by + 6, colour, false);
		}
	}

	private String fit(String s, int width) {
		if (font.width(s) <= width) return s;
		while (s.length() > 1 && font.width(s + ".") > width) s = s.substring(0, s.length() - 1);
		return s + ".";
	}

	// Buttons, in screen coordinates relative to left/top.
	static final int[] PROTECT_ON = {358, 236, 40, 18}, PROTECT_OFF = {398, 236, 40, 18}, SCRAM = {448, 222, 48, 48},
			RESET = {358, 262, 80, 16}, MODE = {358, 282, 80, 12}, REFUEL = {420, 306, 76, 20};

	private boolean over(int[] b, double mx, double my) {
		return mx >= left + b[0] && mx < left + b[0] + b[2] && my >= top + b[1] && my < top + b[1] + b[3];
	}

	private void button(GuiGraphicsExtractor g, int[] b, String label, int colour, boolean hot, boolean active) {
		int x = left + b[0], y = top + b[1];
		g.fill(x, y, x + b[2], y + b[3], active ? (hot ? 0xFF22404C : 0xFF16303A) : 0xFF141C20);
		g.outline(x, y, b[2], b[3], active ? colour : EDGE);
		g.text(font, label, x + b[2] / 2 - font.width(label) / 2, y + b[3] / 2 - 4, active ? colour : DIM, false);
	}

	private void protection(GuiGraphicsExtractor g, ReactorCore core, EquipmentBlockEntity c, int x, int y, int mx, int my) {
		panel(g, x, y, 146, 98, t("protection"));
		g.text(font, t("rps"), x + 6, y + 22, DIM, false);
		int[] on = PROTECT_ON, off = PROTECT_OFF;
		g.fill(left + on[0], top + on[1], left + on[0] + on[2], top + on[1] + on[3], core.protection ? 0xFF1E6040 : 0xFF141C20);
		g.fill(left + off[0], top + off[1], left + off[0] + off[2], top + off[1] + off[3], core.protection ? 0xFF141C20 : 0xFF6A2018);
		g.outline(left + on[0], top + on[1], on[2] + off[2], on[3], EDGE);
		g.text(font, t("on"), left + on[0] + 20 - font.width(t("on")) / 2, top + on[1] + 5, core.protection ? GREEN : DIM, false);
		g.text(font, t("off"), left + off[0] + 20 - font.width(t("off")) / 2, top + off[1] + 5, core.protection ? DIM : RED, false);
		g.blit(RenderPipelines.GUI_TEXTURED, SCRAM_TEXTURE, left + SCRAM[0], top + SCRAM[1], 0, 0, 48, 48, 48, 48, 48, 48);
		button(g, RESET, t("reset"), AMBER, over(RESET, mx, my), core.scrammed);
		boolean party = c.mode() == UseMode.PARTY;
		button(g, MODE, t(party ? "mode_party" : "mode_personal"), TEXT, over(MODE, mx, my), true);
	}

	private void fuel(GuiGraphicsExtractor g, ReactorCore core, int x, int y, int mx, int my) {
		panel(g, x, y, 492, 42, t("fuel"));
		int spent = core.spentCount();
		double burn = 0;
		for (double b : core.fuel) burn += Math.min(1, b);
		double avg = core.fuel.isEmpty() ? 0 : burn / core.fuel.size();
		String line = t("fuel_line", core.fuel.size(), core.fuel.size() - spent, spent, Math.round(avg * 100));
		g.text(font, line, x + 50, y + 5, TEXT, false);
		int bx = x + 8, by = y + 22, bw = 396;
		g.fill(bx, by, bx + bw, by + 10, 0xFF0A1218);
		g.outline(bx, by, bw, 10, EDGE);
		g.fill(bx + 1, by + 1, bx + 1 + (int) Math.round((bw - 2) * avg), by + 9, AMBER);
		boolean canRefuel = core.refuelOpen || (core.shutDown() && core.temperature < 120 && core.rodTarget >= 100);
		button(g, REFUEL, t(core.refuelOpen ? "refuel_close" : "refuel"), ACCENT, over(REFUEL, mx, my), canRefuel);
	}

	// ------------------------------------------------------------------ input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x() / scale, my = event.y() / scale;
		EquipmentBlockEntity v = vessel();
		if (event.button() == 0 && v != null) {
			if (onSlider(mx, my)) {
				dragging = true;
				dragValue = sliderValue(my);
				return true;
			}
			if (over(SCRAM, mx, my)) {
				send(ConsoleActionPayload.SCRAM, 0);
				return true;
			}
			if (over(PROTECT_ON, mx, my) && !v.core.protection || over(PROTECT_OFF, mx, my) && v.core.protection) {
				send(ConsoleActionPayload.PROTECTION, 0);
				return true;
			}
			if (over(RESET, mx, my)) {
				send(ConsoleActionPayload.RESET, 0);
				return true;
			}
			if (over(MODE, mx, my)) {
				send(ConsoleActionPayload.MODE, 0);
				return true;
			}
			if (over(REFUEL, mx, my)) {
				send(ConsoleActionPayload.REFUEL, 0);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			dragValue = sliderValue(event.y() / scale);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging) {
			dragging = false;
			if (dragValue >= 0) {
				send(ConsoleActionPayload.SET_RODS, dragValue);
			}
			dragValue = -1;
			return true;
		}
		return super.mouseReleased(event);
	}
}
