package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.PlatformLayout;
import io.github.jimbozoomer.jugcraft.drone.TerminalModePayload;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Drone Depot Terminal screen, in the Drone Tower's look: graphite glass, dull red readouts, amber
 * warnings. Tabs: OVERVIEW (platform, pickup, fleet, power at a glance), FLEET (drones per tier, docked
 * and airborne), JOBS (open build positions and missing materials) and POWER. The PERSONAL / PARTY button
 * switches who the depot works for (owner only; the server checks). Everything shown comes from the
 * terminal's live {@link DepotView}.
 */
public class DroneTerminalScreen extends Screen {
	private static final int WIDTH = 260;
	private static final int HEIGHT = 170;
	// The tower palette (the names stay: CYAN is the accent, CYAN_DIM the labels).
	private static final int FRAME = 0xFF3A2422;
	private static final int GLASS = 0xF0141012;
	private static final int PANEL = 0xFF1E1A1C;
	private static final int CYAN = 0xFFE05040;
	private static final int CYAN_DIM = 0xFF96463C;
	private static final int WHITE = 0xFFECE6E6;
	private static final int AMBER = 0xFFFFC04A;
	private static final int RED = 0xFFFF6A5A;
	private static final String[] TABS = {"OVERVIEW", "FLEET", "JOBS", "POWER"};

	private final BlockPos pos;
	private int tab;
	private int left;
	private int top;
	private Button modeButton;
	private Button towerButton;

	public DroneTerminalScreen(BlockPos pos) {
		super(Component.translatable("block.jugcraft.drone_depot_terminal"));
		this.pos = pos;
	}

	private DepotView view() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof DroneTerminalBlockEntity terminal ? terminal.view() : null;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		for (int i = 0; i < TABS.length; i++) {
			int index = i;
			addRenderableWidget(Button.builder(Component.literal(TABS[i]), button -> tab = index)
					.bounds(left + 6 + i * 62, top + 22, 60, 16).build());
		}
		modeButton = addRenderableWidget(Button.builder(Component.literal("MODE"), button -> ClientPlayNetworking.send(new TerminalModePayload(pos)))
				.bounds(left + WIDTH - 86, top + 4, 80, 14).build());
		towerButton = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.terminal.tower"), button -> {
			DepotView view = view();
			if (view != null && view.towerPos != null) {
				Minecraft.getInstance().gui.setScreen(new TowerScreen(view.towerPos));
			}
		}).bounds(left + WIDTH - 140, top + 4, 50, 14).build());
		towerButton.visible = false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, FRAME);
		graphics.fill(left, top, left + WIDTH, top + HEIGHT, GLASS);
		graphics.fill(left + 6, top + 42, left + WIDTH - 6, top + HEIGHT - 6, PANEL);
		// Corner brackets and a scan line, for the sci-fi look.
		for (int[] c : new int[][] {{left, top}, {left + WIDTH - 10, top}, {left, top + HEIGHT - 2}, {left + WIDTH - 10, top + HEIGHT - 2}}) {
			graphics.fill(c[0], c[1], c[0] + 10, c[1] + 2, CYAN);
		}
		long ms = System.currentTimeMillis();
		int scan = top + 42 + (int) ((ms / 30) % (HEIGHT - 48));
		graphics.fill(left + 6, scan, left + WIDTH - 6, scan + 1, 0x3096463C);
		graphics.fill(left + 6 + tab * 62, top + 38, left + 66 + tab * 62, top + 40, CYAN);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.text(font, fit("▌DRONE DEPOT TERMINAL", WIDTH - 150), left + 8, top + 7, CYAN, false);
		DepotView view = view();
		if (view == null) {
			graphics.text(font, "LINKING…", left + 14, top + 50, AMBER, false);
			return;
		}
		modeButton.setMessage(Component.literal(fit("▸ " + view.mode.toUpperCase(Locale.ROOT), 72)));
		towerButton.visible = view.towerPos != null;
		int x = left + 14;
		int y = top + 50;
		switch (tab) {
			case 0 -> overview(graphics, view, x, y);
			case 1 -> fleet(graphics, view, x, y);
			case 2 -> jobs(graphics, view, x, y);
			default -> power(graphics, view, x, y);
		}
	}

	private void overview(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		PlatformLayout.Status status = v.layoutStatus();
		line(g, "PLATFORM", status == PlatformLayout.Status.OK ? v.width + "×" + v.depth + " · " + v.pads.size() + " PADS" : status.name().replace('_', ' '),
				status == PlatformLayout.Status.OK ? WHITE : AMBER, x, y);
		line(g, "SUPPLY", v.pickups.isEmpty() ? "NO PICKUP PAD" : !v.packager ? "NO CARGO PACKAGER"
				: v.pickups.size() + (v.pickups.size() == 1 ? " PICKUP" : " PICKUPS") + " + PACKAGER READY",
				!v.pickups.isEmpty() && v.packager ? WHITE : AMBER, x, y + 12);
		int fleet = v.docks.size() + v.flights.size();
		line(g, "FLEET", fleet + "/" + v.maxDrones + " · " + v.flying() + " UP · UP TO T" + v.maxTier
				+ (v.towerTier > 0 ? " · TOWER T" + v.towerTier : ""), WHITE, x, y + 24);
		bar(g, x, y + 36, fleet, v.maxDrones, CYAN);
		long percent = v.capacity <= 0 ? 0 : v.energy * 100 / v.capacity;
		line(g, "POWER", percent + "%" + (v.lowPower ? " · LOW POWER, FLIGHTS SLOWED" : ""), v.lowPower ? RED : WHITE, x, y + 50);
		bar(g, x, y + 62, v.energy, v.capacity, v.lowPower ? RED : CYAN);
		line(g, "JOBS", v.openTargets + " OPEN" + (v.covered > 0 ? " · " + v.covered + " UNDER COVER" : ""), WHITE, x, y + 76);
		if (!v.missing.isEmpty()) {
			say(g, "⚠ MATERIALS NEEDED — SEE JOBS", x, y + 90, AMBER);
		}
	}

	private void fleet(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		Map<Integer, int[]> byTier = new TreeMap<>();
		v.docks.forEach(d -> byTier.computeIfAbsent(d.tier(), k -> new int[2])[0]++);
		v.flights.forEach(f -> byTier.computeIfAbsent(f.tier(), k -> new int[2])[1]++);
		int docked = contentRight() - 90;
		int airborne = contentRight() - 44;
		g.text(font, "TIER", x, y, CYAN_DIM, false);
		g.text(font, "DOCKED", docked, y, CYAN_DIM, false);
		g.text(font, "UP", airborne, y, CYAN_DIM, false);
		int row = y + 12;
		if (byTier.isEmpty()) {
			paragraph(g, "NO DRONES LINKED — USE A DRONE ON A PAD", x, row, AMBER);
		}
		for (Map.Entry<Integer, int[]> entry : byTier.entrySet()) {
			String name = DroneTier.byNumber(entry.getKey()).map(t -> "T" + t.number() + " " + t.name().replace('_', ' ')).orElse("T" + entry.getKey());
			g.text(font, fit(name, docked - x - 6), x, row, WHITE, false);
			g.text(font, String.valueOf(entry.getValue()[0]), docked, row, WHITE, false);
			g.text(font, String.valueOf(entry.getValue()[1]), airborne, row, entry.getValue()[1] > 0 ? CYAN : WHITE, false);
			row += 11;
		}
	}

	private void jobs(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		line(g, "OPEN POSITIONS", String.valueOf(v.openTargets), WHITE, x, y);
		line(g, "UNDER COVER", v.covered + (v.covered > 0 ? " (NEED OPEN SKY)" : ""), v.covered > 0 ? AMBER : WHITE, x, y + 12);
		say(g, "MISSING IN PACKAGER", x, y + 28, CYAN_DIM);
		int row = y + 40;
		if (v.missing.isEmpty()) {
			say(g, "NOTHING — SUPPLY OK", x, row, WHITE);
		}
		for (Map.Entry<String, Integer> entry : v.missing.entrySet()) {
			say(g, entry.getValue() + "× " + entry.getKey().replaceFirst("^minecraft:", ""), x, row, AMBER);
			row += 11;
		}
	}

	private void power(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		line(g, "STORED", v.energy + " / " + v.capacity + " JE", WHITE, x, y);
		bar(g, x, y + 12, v.energy, v.capacity, v.lowPower ? RED : CYAN);
		line(g, "DRAW NOW", (v.flying() > 0 ? v.workingDraw : v.standbyDraw) + " JE/t", WHITE, x, y + 26);
		line(g, "WORKING", v.workingDraw + " JE/t (all drone upkeep)", WHITE, x, y + 38);
		line(g, "STANDBY", v.standbyDraw + " JE/t", WHITE, x, y + 50);
		if (v.lowPower) {
			paragraph(g, "⚠ LOW POWER: FLIGHTS SLOWED, NO NEW DEPARTURES", x, y + 66, RED);
		}
	}

	private void line(GuiGraphicsExtractor g, String label, String value, int color, int x, int y) {
		g.text(font, fit(label, 76), x, y, CYAN_DIM, false);
		g.text(font, fit(value, contentRight() - (x + 80)), x + 80, y, color, false);
	}

	/** Right edge of the content panel, minus a margin. */
	private int contentRight() {
		return left + WIDTH - 12;
	}

	/** {@code text} cut to fit {@code width} pixels, ending in an ellipsis if it was cut. */
	private String fit(String text, int width) {
		if (font.width(text) <= width) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
	}

	/** Text trimmed to the content panel. */
	private void say(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		g.text(font, fit(text, contentRight() - x), x, y, color, false);
	}

	/** Text wrapped to the content panel; returns the y below it. */
	private int paragraph(GuiGraphicsExtractor g, String text, int x, int y, int color) {
		for (var line : font.split(Component.literal(text), contentRight() - x)) {
			g.text(font, line, x, y, color, false);
			y += 10;
		}
		return y;
	}

	private void bar(GuiGraphicsExtractor g, int x, int y, long value, long max, int color) {
		int w = WIDTH - 40;
		g.fill(x, y, x + w, y + 6, 0xFF0C0A0B);
		int filled = max <= 0 ? 0 : (int) Math.min(w, value * w / max);
		g.fill(x, y, x + filled, y + 6, color);
		for (int i = 1; i < 20; i++) {
			g.fill(x + i * w / 20, y, x + i * w / 20 + 1, y + 6, 0xFF0C0A0B);
		}
	}
}
