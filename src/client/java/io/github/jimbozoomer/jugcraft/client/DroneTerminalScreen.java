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
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Drone Depot Terminal screen, in the drone look (docs/features/machine-screens.md: the advanced look's hologram
 * shapes in the Drone Tower's graphite and dull red, white readouts, amber warnings). A status lamp by the title: green
 * while drones are up, amber when something is missing, red on low power. Tab keys: OVERVIEW (platform, pickup, fleet,
 * power at a glance), FLEET (drones per tier, docked and airborne), JOBS (open build positions and missing materials)
 * and POWER. The PERSONAL / PARTY key switches who the depot works for (owner only; the server checks), and TOWER opens
 * the tower's screen. Everything shown comes from the terminal's live {@link DepotView}.
 */
public class DroneTerminalScreen extends Screen implements ScreenKit.Fitted {
	private static final int WIDTH = 260;
	private static final int HEIGHT = 170;
	private static final int AMBER = 0xFFFFC04A;
	private static final int RED = 0xFFFF6A5A;
	private static final String[] TABS = {"OVERVIEW", "FLEET", "JOBS", "POWER"};

	private final BlockPos pos;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("drone_terminal");
	private final ScreenKit.TextButton[] tabs = new ScreenKit.TextButton[TABS.length];
	private int tab;
	private int left;
	private int top;
	private ScreenKit.TextButton modeButton;
	private ScreenKit.TextButton towerButton;

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
			tabs[i] = addRenderableWidget(new ScreenKit.TextButton(left + 6 + i * 62, top + 22, 60, 16, theme, Component.literal(TABS[i]),
					button -> tab = index));
		}
		modeButton = addRenderableWidget(new ScreenKit.TextButton(left + WIDTH - 86, top + 4, 80, 14, theme, Component.literal("MODE"),
				button -> ClientPlayNetworking.send(new TerminalModePayload(pos))));
		towerButton = addRenderableWidget(new ScreenKit.TextButton(left + WIDTH - 130, top + 4, 42, 14, theme,
				Component.translatable("screen.jugcraft.terminal.tower"), button -> {
					DepotView view = view();
					if (view != null && view.towerPos != null) {
						Minecraft.getInstance().gui.setScreen(new TowerScreen(view.towerPos));
					}
				}));
		towerButton.visible = false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);
		ScreenKit.body(graphics, theme, left, top, WIDTH, HEIGHT);
		ScreenKit.pane(graphics, theme, left + 6, top + 42, WIDTH - 12, HEIGHT - 48);
		// A scan line down the glass.
		long ms = System.currentTimeMillis();
		int scan = top + 44 + (int) ((ms / 30) % (HEIGHT - 52));
		graphics.fill(left + 8, scan, left + WIDTH - 8, scan + 1, (theme.dim() & 0x00FFFFFF) | 0x30000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		DepotView view = view();
		for (int i = 0; i < tabs.length; i++) {
			tabs[i].setSelected(i == tab);
		}
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		int lamp = theme.dim();
		if (view != null) {
			boolean warning = view.layoutStatus() != PlatformLayout.Status.OK || view.pickups.isEmpty() || !view.packager || !view.missing.isEmpty();
			lamp = view.lowPower ? RED : warning ? AMBER : view.flying() > 0 ? theme.ok() : theme.dim();
		}
		ScreenKit.title(graphics, font, theme, left, top + 5, Component.literal("DRONE DEPOT TERMINAL"), lamp, 132);
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
				status == PlatformLayout.Status.OK ? theme.text() : AMBER, x, y);
		line(g, "SUPPLY", v.pickups.isEmpty() ? "NO PICKUP PAD" : !v.packager ? "NO CARGO PACKAGER"
				: v.pickups.size() + (v.pickups.size() == 1 ? " PICKUP" : " PICKUPS") + " + PACKAGER READY",
				!v.pickups.isEmpty() && v.packager ? theme.text() : AMBER, x, y + 12);
		int fleet = v.docks.size() + v.flights.size();
		line(g, "FLEET", fleet + "/" + v.maxDrones + " · " + v.flying() + " UP · UP TO T" + v.maxTier
				+ (v.towerTier > 0 ? " · TOWER T" + v.towerTier : ""), theme.text(), x, y + 24);
		bar(g, x, y + 36, fleet, v.maxDrones, theme.energy());
		long percent = v.capacity <= 0 ? 0 : v.energy * 100 / v.capacity;
		line(g, "POWER", percent + "%" + (v.lowPower ? " · LOW POWER, FLIGHTS SLOWED" : ""), v.lowPower ? RED : theme.text(), x, y + 50);
		bar(g, x, y + 62, v.energy, v.capacity, v.lowPower ? RED : theme.energy());
		line(g, "JOBS", v.openTargets + " OPEN" + (v.covered > 0 ? " · " + v.covered + " UNDER COVER" : ""), theme.text(), x, y + 76);
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
		g.text(font, "TIER", x, y, theme.dim(), false);
		g.text(font, "DOCKED", docked, y, theme.dim(), false);
		g.text(font, "UP", airborne, y, theme.dim(), false);
		int row = y + 12;
		if (byTier.isEmpty()) {
			paragraph(g, "NO DRONES LINKED — USE A DRONE ON A PAD", x, row, AMBER);
		}
		for (Map.Entry<Integer, int[]> entry : byTier.entrySet()) {
			String name = DroneTier.byNumber(entry.getKey()).map(t -> "T" + t.number() + " " + t.name().replace('_', ' ')).orElse("T" + entry.getKey());
			g.text(font, fit(name, docked - x - 6), x, row, theme.text(), false);
			g.text(font, String.valueOf(entry.getValue()[0]), docked, row, theme.text(), false);
			g.text(font, String.valueOf(entry.getValue()[1]), airborne, row, entry.getValue()[1] > 0 ? theme.label() : theme.text(), false);
			row += 10;
		}
	}

	private void jobs(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		line(g, "OPEN POSITIONS", String.valueOf(v.openTargets), theme.text(), x, y);
		line(g, "UNDER COVER", v.covered + (v.covered > 0 ? " (NEED OPEN SKY)" : ""), v.covered > 0 ? AMBER : theme.text(), x, y + 12);
		say(g, "MISSING IN PACKAGER", x, y + 28, theme.dim());
		int row = y + 40;
		if (v.missing.isEmpty()) {
			say(g, "NOTHING — SUPPLY OK", x, row, theme.text());
		}
		for (Map.Entry<String, Integer> entry : v.missing.entrySet()) {
			say(g, entry.getValue() + "× " + entry.getKey().replaceFirst("^minecraft:", ""), x, row, AMBER);
			row += 11;
		}
	}

	private void power(GuiGraphicsExtractor g, DepotView v, int x, int y) {
		line(g, "STORED", v.energy + " / " + v.capacity + " JE", theme.text(), x, y);
		bar(g, x, y + 12, v.energy, v.capacity, v.lowPower ? RED : theme.energy());
		line(g, "DRAW NOW", (v.flying() > 0 ? v.workingDraw : v.standbyDraw) + " JE/t", theme.text(), x, y + 26);
		line(g, "WORKING", v.workingDraw + " JE/t (all drone upkeep)", theme.text(), x, y + 38);
		line(g, "STANDBY", v.standbyDraw + " JE/t", theme.text(), x, y + 50);
		if (v.lowPower) {
			paragraph(g, "⚠ LOW POWER: FLIGHTS SLOWED, NO NEW DEPARTURES", x, y + 66, RED);
		}
	}

	private void line(GuiGraphicsExtractor g, String label, String value, int color, int x, int y) {
		g.text(font, fit(label, 76), x, y, theme.dim(), false);
		g.text(font, fit(value, contentRight() - (x + 80)), x + 80, y, color, false);
	}

	/** Right edge of the content panel, minus a margin. */
	private int contentRight() {
		return left + WIDTH - 12;
	}

	/** {@code text} cut to fit {@code width} pixels, ending in an ellipsis if it was cut. */
	private String fit(String text, int width) {
		return ScreenKit.fit(font, text, width);
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
		ScreenKit.bar(g, theme, x, y, WIDTH - 40, 6, max <= 0 ? 0 : value / (double) max, color);
	}

	@Override
	public ScreenRectangle panelArea() {
		return new ScreenRectangle(left, top, WIDTH, HEIGHT);
	}
}
