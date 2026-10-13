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
	private static final int WIDTH = 316;
	private static final int HEIGHT = 234;
	// The tower palette (the names stay: CYAN is the accent, CYAN_DIM the labels).
	private static final int FRAME = 0xFF3A2422;
	private static final int GLASS = 0xF0141012;
	private static final int PANEL = 0xFF1E1A1C;
	private static final int CYAN = 0xFFE05040;
	private static final int CYAN_DIM = 0xFFC28E84;
	private static final int WHITE = 0xFFECE6E6;
	private static final int AMBER = 0xFFFFC04A;
	private static final int RED = 0xFFFF6A5A;
	private static final String[] TABS = {"OVERVIEW", "FLEET", "JOBS", "POWER"};

	private final BlockPos pos;
	private final BlockPos accessPos;
	private int tab;
	private int left;
	private int top;
	private Button modeButton;
	private Button towerButton;
	private int jobScroll;
	private static final int JOB_ROWS = 6;

	public DroneTerminalScreen(BlockPos pos) {
		this(pos, pos);
	}

	public DroneTerminalScreen(BlockPos pos, BlockPos accessPos) {
		super(Component.translatable("block.jugcraft.drone_depot_terminal"));
		this.pos = pos;
		this.accessPos = accessPos;
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
			addRenderableWidget(Button.builder(Component.literal(TABS[i]), button -> { tab = index; jobScroll = 0; })
					.bounds(left + 6 + i * 76, top + 22, 74, 16).build());
		}
		modeButton = addRenderableWidget(Button.builder(Component.literal("MODE"), button -> ClientPlayNetworking.send(new TerminalModePayload(pos, accessPos)))
				.bounds(left + WIDTH - 86, top + 4, 80, 14).build());
		towerButton = addRenderableWidget(Button.builder(Component.translatable("screen.jugcraft.terminal.tower"), button -> {
			DepotView view = view();
			if (view != null && view.towerPos != null) {
				Minecraft.getInstance().gui.setScreen(new TowerScreen(view.towerPos, accessPos));
			}
		}).bounds(left + WIDTH - 196, top + 4, 106, 14).build());
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
		graphics.fill(left + 6 + tab * 76, top + 38, left + 80 + tab * 76, top + 40, CYAN);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		DepotView view = view();
		int titleWidth = view != null && view.towerPos != null ? towerButton.getX() - left - 12 : WIDTH - 102;
		graphics.text(font, fit("DRONE CONTROL", titleWidth), left + 8, top + 7, CYAN, false);
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
			case 2 -> jobs(graphics, view, x, y, mouseX, mouseY);
			default -> power(graphics, view, x, y);
		}
	}

	private double now() {
		var level = Minecraft.getInstance().level;
		return level == null ? 0 : level.getGameTime();
	}

	private void overview(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		int fleet = v.docks.size() + v.flights.size();
		line(g, "TOTAL", fleet + " / " + v.maxDrones + " DRONES", WHITE, x, y);
		line(g, "ACTIVE", v.active() + " ON JOBS  |  " + v.airborne(now()) + " AIRBORNE", WHITE, x, y + 14);
		line(g, "PADS", v.padCount() + " PADS / BAYS  |  " + Math.max(0, v.maxDrones - fleet) + " FREE SLOTS", WHITE, x, y + 28);
		bar(g, x, y + 42, fleet, v.maxDrones, CYAN);
		long charge = v.capacity <= 0 ? 0 : v.energy * 100 / v.capacity;
		line(g, "BATTERY", charge + "%  |  " + v.energy + " JE", v.lowPower ? RED : WHITE, x, y + 56);
		bar(g, x, y + 70, v.energy, v.capacity, v.lowPower ? RED : CYAN);
		line(g, "POWER LOAD", v.loadPercent() + "% OF INPUT  |  " + v.draw() + " JE/t", v.loadPercent() > 100 ? AMBER : WHITE, x, y + 84);
		boolean ready = !v.pickups.isEmpty() && v.packager && v.missing.isEmpty();
		String supply = v.pickups.isEmpty() ? "NO PICKUP PAD" : !v.packager ? "NO CARGO PACKAGER"
				: !v.missing.isEmpty() ? "LACKING MATERIALS - SEE JOBS" : "SUPPLY READY";
		line(g, "SUPPLY", supply, ready ? CYAN : AMBER, x, y + 102);
		line(g, "WORK", v.openTargets + " OPEN  |  " + v.covered + " UNDER COVER", WHITE, x, y + 116);
		line(g, "TOWER", v.towerTier > 0 ? "TIER " + v.towerTier + " - CONTROL / UPGRADE ABOVE" : "STANDALONE DEPOT", WHITE, x, y + 132);
		if (v.lowPower) say(g, "LOW POWER - FLIGHTS SLOWED", x, y + 150, RED);
	}

	private void fleet(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		Map<Integer, int[]> byTier = new TreeMap<>();
		v.docks.forEach(d -> byTier.computeIfAbsent(d.tier(), k -> new int[2])[0]++);
		v.flights.forEach(f -> {
			boolean airborneNow = f.ticksAt(now(), v.syncTime) < f.path().totalTicks() - io.github.jimbozoomer.jugcraft.drone.FlightScheduler.RECHARGE_TICKS;
			byTier.computeIfAbsent(f.tier(), k -> new int[2])[airborneNow ? 1 : 0]++;
		});
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

	private void jobs(GuiGraphicsExtractor g, DepotView v, int x, int y, int mouseX, int mouseY) {
		line(g, "OPEN", v.openTargets + " POSITIONS / " + v.covered + " UNDER COVER", WHITE, x, y);
		say(g, "MISSING MATERIALS IN CARGO PACKAGER", x, y + 16, CYAN_DIM);
		var entries = new java.util.ArrayList<>(v.missing.entrySet());
		jobScroll = Math.max(0, Math.min(jobScroll, Math.max(0, entries.size() - JOB_ROWS)));
		if (entries.isEmpty()) say(g, "NOTHING MISSING - SUPPLY OK", x, y + 36, WHITE);
		for (int row = 0; row < JOB_ROWS && row + jobScroll < entries.size(); row++) {
			var entry = entries.get(row + jobScroll);
			var id = net.minecraft.resources.Identifier.tryParse(entry.getKey());
			var item = id == null ? net.minecraft.world.item.Items.AIR : net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
			var stack = new net.minecraft.world.item.ItemStack(item);
			int ry = y + 32 + row * 19;
			g.item(stack, x, ry);
			String name = stack.isEmpty() ? entry.getKey() : stack.getHoverName().getString();
			say(g, entry.getValue() + " x " + name, x + 22, ry + 4, AMBER);
			if (mouseX >= x && mouseX < contentRight() && mouseY >= ry && mouseY < ry + 18) {
				g.setTooltipForNextFrame(font, Component.literal(name + " [" + entry.getKey() + "] - missing " + entry.getValue()), mouseX, mouseY);
			}
		}
		if (entries.size() > JOB_ROWS) say(g, "SCROLL " + (jobScroll + 1) + "-" + Math.min(entries.size(), jobScroll + JOB_ROWS) + "/" + entries.size(), x, top + HEIGHT - 18, CYAN_DIM);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double dx, double dy) {
		DepotView v = view();
		if (tab == 2 && v != null && x >= left + 6 && x < left + WIDTH - 6 && y >= top + 76 && y < top + HEIGHT - 6) {
			jobScroll = Math.max(0, Math.min(Math.max(0, v.missing.size() - JOB_ROWS), jobScroll - (int) Math.signum(dy)));
			return true;
		}
		return super.mouseScrolled(x, y, dx, dy);
	}

	private void power(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		line(g, "STORED", v.energy + " / " + v.capacity + " JE", WHITE, x, y);
		bar(g, x, y + 12, v.energy, v.capacity, v.lowPower ? RED : CYAN);
		line(g, "DRAW NOW", v.draw() + " JE/t", WHITE, x, y + 26);
		line(g, "INPUT LOAD", v.loadPercent() + "% OF " + DroneTerminalBlockEntity.ENERGY_INPUT + " JE/t INPUT", WHITE, x, y + 38);
		line(g, "WORKING", v.workingDraw + " JE/t (all drone upkeep)", WHITE, x, y + 52);
		line(g, "STANDBY", v.standbyDraw + " JE/t", WHITE, x, y + 66);
		if (v.lowPower) {
			paragraph(g, "⚠ LOW POWER: FLIGHTS SLOWED, NO NEW DEPARTURES", x, y + 84, RED);
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
