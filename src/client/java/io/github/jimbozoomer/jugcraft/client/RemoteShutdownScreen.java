package io.github.jimbozoomer.jugcraft.client;

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

/**
 * The remote shutdown panel's screen (round 24; Justin: "the shutdown panel must actually work", linked to the nearest
 * reactor by itself): the reactor it found and its state (power, rods, core temperature, output, tripped or not), where
 * control is, the transfer switch that takes the reactor's controls from the control room (or hands them back), and
 * the reactor trip. What it shows is the panel's own copy of its reactor's state (synced with the panel, so it works
 * however far away the reactor is); every button is a {@link ConsoleActionPayload} the server checks. In the reactor
 * console's advanced look (docs/features/machine-screens.md), with a status lamp and chip by the title; laid out at
 * 360 x 220 and scaled to fit.
 */
public class RemoteShutdownScreen extends Screen {
	static final int W = 360, H = 220;
	static final int EDGE = ReactorConsoleScreen.EDGE, TEXT = ReactorConsoleScreen.TEXT, DIM = ReactorConsoleScreen.DIM,
			GREEN = ReactorConsoleScreen.GREEN, AMBER = ReactorConsoleScreen.AMBER, RED = ReactorConsoleScreen.RED;
	/** Buttons, relative to the screen's corner: the transfer switch, the reactor trip, the owner's switch. */
	static final int[] TRANSFER = {196, 92, 154, 28}, TRIP = {300, 140, 48, 48}, MODE = {196, 196, 96, 14};

	private final BlockPos panel;
	private final MachineScreenThemes.Theme theme = MachineScreenThemes.screen("remote_shutdown");
	private float scale = 1;
	private int left, top;

	public RemoteShutdownScreen(BlockPos panel) {
		super(Component.translatable("screen.jugcraft.remote_shutdown.title"));
		this.panel = panel;
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

	private EquipmentBlockEntity panelEntity() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && mc.level.getBlockEntity(panel) instanceof EquipmentBlockEntity e
				&& e.kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL ? e : null;
	}

	private void send(int action, int value) {
		ClientPlayNetworking.send(new ConsoleActionPayload(panel, action, value));
	}

	private static String t(String key, Object... args) {
		return Component.translatable("screen.jugcraft.remote_shutdown." + key, args).getString();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		g.pose().pushMatrix();
		g.pose().scale(scale, scale);
		draw(g, Math.round(mouseX / scale), Math.round(mouseY / scale));
		g.pose().popMatrix();
	}

	private void draw(GuiGraphicsExtractor g, int mx, int my) {
		int x0 = left, y0 = top;
		ScreenKit.body(g, theme, x0, y0, W, H);
		EquipmentBlockEntity p = panelEntity();
		if (p == null || p.linked == null) {
			ScreenKit.title(g, font, theme, x0 + 4, y0 + 5, Component.literal(t("title")), AMBER, W - 16);
			g.text(font, t("no_reactor", EquipmentBlockEntity.PANEL_RANGE), x0 + 12, y0 + 40, AMBER, false);
			return;
		}
		ReactorCore core = p.core;
		String status;
		int statusColour;
		if (!p.panelOnline) {
			status = t("status.no_signal");
			statusColour = AMBER;
		} else if (core.scrammed) {
			status = t("status.tripped");
			statusColour = RED;
		} else if (core.fission > 0.005) {
			status = t("status.operating");
			statusColour = GREEN;
		} else {
			status = t("status.shut_down");
			statusColour = DIM;
		}
		ScreenKit.title(g, font, theme, x0 + 4, y0 + 5, Component.literal(t("title")), statusColour, W - font.width(status) - 32);
		ScreenKit.chip(g, font, theme, x0 + W - 8, y0 + 4, status, statusColour);
		reactor(g, p, core, x0 + 8, y0 + 28);
		control(g, core, x0 + 188, y0 + 28, mx, my);
		trip(g, core, x0 + 188, y0 + 132);
		boolean party = p.mode() == UseMode.PARTY;
		button(g, MODE, t(party ? "mode_party" : "mode_personal"), TEXT, over(MODE, mx, my), true);
	}

	private void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
		ScreenKit.pane(g, font, theme, x, y, w, h, title);
	}

	/** The reactor it found and its readings. */
	private void reactor(GuiGraphicsExtractor g, EquipmentBlockEntity p, ReactorCore core, int x, int y) {
		panel(g, x, y, 172, 184, t("reactor"));
		BlockPos at = p.linked;
		double far = Math.sqrt(at.distSqr(p.getBlockPos()));
		g.text(font, t("found", at.getX(), at.getY(), at.getZ()), x + 6, y + 22, DIM, false);
		g.text(font, t("distance", Math.round(far)), x + 6, y + 32, DIM, false);
		String design = switch (core.design.id()) {
			case "epr" -> t("design_epr");
			case "smr" -> t("design_smr");
			default -> t("design_pwr");
		};
		g.text(font, design, x + 6, y + 42, DIM, false);
		int ry = y + 60;
		row(g, x, ry, t("power"), String.format(Locale.ROOT, "%.1f %%", core.fission * 100), TEXT);
		row(g, x, ry + 16, t("rods"), String.format(Locale.ROOT, "%.0f %%", core.rods), core.rods >= 99.9 ? GREEN : TEXT);
		int tempColour = core.temperature >= ReactorCore.ALARM_TEMP ? RED : core.temperature >= ReactorCore.NORMAL_TEMP + 10 ? AMBER : TEXT;
		row(g, x, ry + 32, t("temperature"), String.format(Locale.ROOT, "%.0f °C", core.temperature), tempColour);
		double output = core.electric / (double) Math.max(1, core.design.ratedJe());
		row(g, x, ry + 48, t("output"), String.format(Locale.ROOT, "%.0f %%", output * 100), TEXT);
		// The rods' travel as a bar: full when all the way in.
		int bx = x + 6, by = ry + 70, bw = 160;
		g.text(font, t("rods_bar"), bx, by, DIM, false);
		g.fill(bx, by + 10, bx + bw, by + 20, 0xFF0A1218);
		g.outline(bx, by + 10, bw, 10, EDGE);
		g.fill(bx + 1, by + 11, bx + 1 + (int) Math.round((bw - 2) * Math.max(0, Math.min(100, core.rods)) / 100), by + 19,
				core.scrammed ? RED : GREEN);
		if (core.fuelDamaged) {
			g.text(font, t("fuel_damaged"), bx, by + 26, RED, false);
		}
	}

	private void row(GuiGraphicsExtractor g, int x, int y, String label, String value, int colour) {
		g.text(font, label, x + 6, y, DIM, false);
		g.text(font, value, x + 166 - font.width(value), y, colour, false);
		g.fill(x + 6, y + 11, x + 166, y + 12, 0xFF1A2830);
	}

	/** Where control is, and the transfer switch. */
	private void control(GuiGraphicsExtractor g, ReactorCore core, int x, int y, int mx, int my) {
		panel(g, x, y, 164, 98, t("control"));
		// The two lamps over the transfer switches: white while the control room has control, amber while this panel does.
		lamp(g, x + 10, y + 24, !core.remote ? 0xFFF4F0DC : 0xFF5A5850);
		g.text(font, ScreenKit.fit(font, t("at_mcr"), 134), x + 24, y + 26, !core.remote ? TEXT : DIM, false);
		lamp(g, x + 10, y + 40, core.remote ? AMBER : 0xFF5A4420);
		// Wrapped inside the pane, above the transfer switch.
		MachineSkin.wrap(g, font, Component.literal(t("at_panel")), x + 24, y + 42, 134, top + TRANSFER[1] - 2, core.remote ? AMBER : DIM);
		boolean hot = over(TRANSFER, mx, my);
		int bx = left + TRANSFER[0], by = top + TRANSFER[1];
		g.fill(bx, by, bx + TRANSFER[2], by + TRANSFER[3], hot ? 0xFF3A3210 : 0xFF282410);
		g.outline(bx, by, TRANSFER[2], TRANSFER[3], 0xFFE8C030);
		// The key, upright with control at the control room, turned across with it here.
		int kx = bx + 14, ky = by + TRANSFER[3] / 2;
		g.fill(kx - 7, ky - 7, kx + 7, ky + 7, 0xFF6C7074);
		if (core.remote) {
			g.fill(kx - 6, ky - 2, kx + 6, ky + 2, 0xFFE8C030);
		} else {
			g.fill(kx - 2, ky - 6, kx + 2, ky + 6, 0xFFE8C030);
		}
		String label = t(core.remote ? "return" : "take");
		g.text(font, label, bx + 28 + (TRANSFER[2] - 28) / 2 - font.width(label) / 2, by + TRANSFER[3] / 2 - 4, 0xFFE8C030, false);
	}

	private void lamp(GuiGraphicsExtractor g, int x, int y, int colour) {
		g.fill(x - 1, y - 1, x + 9, y + 9, 0xFF20262A);
		g.fill(x, y, x + 8, y + 8, colour);
	}

	/** The reactor trip under its guard, and what it does. */
	private void trip(GuiGraphicsExtractor g, ReactorCore core, int x, int y) {
		panel(g, x, y, 164, 60, t("trip"));
		// Wrapped to the left of the trip button, which sits over the pane's right side.
		int width = left + TRIP[0] - 4 - (x + 6);
		int line = MachineSkin.wrap(g, font, Component.literal(t(core.scrammed ? "tripped_line" : "trip_line")), x + 6, y + 21, width, y + 58,
				core.scrammed ? RED : DIM);
		MachineSkin.wrap(g, font, Component.literal(t("trip_line2")), x + 6, line + 1, width, y + 58, DIM);
		g.blit(RenderPipelines.GUI_TEXTURED, ReactorConsoleScreen.SCRAM_TEXTURE, left + TRIP[0], top + TRIP[1], 0, 0, 48, 48, 48, 48, 48, 48);
	}

	private boolean over(int[] b, double mx, double my) {
		return mx >= left + b[0] && mx < left + b[0] + b[2] && my >= top + b[1] && my < top + b[1] + b[3];
	}

	private void button(GuiGraphicsExtractor g, int[] b, String label, int colour, boolean hot, boolean active) {
		int x = left + b[0], y = top + b[1];
		g.fill(x, y, x + b[2], y + b[3], active ? (hot ? 0xFF22404C : 0xFF16303A) : 0xFF141C20);
		g.outline(x, y, b[2], b[3], active ? colour : EDGE);
		g.text(font, label, x + b[2] / 2 - font.width(label) / 2, y + b[3] / 2 - 4, active ? colour : DIM, false);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x() / scale, my = event.y() / scale;
		EquipmentBlockEntity p = panelEntity();
		if (event.button() == 0 && p != null) {
			if (over(MODE, mx, my)) {
				send(ConsoleActionPayload.MODE, 0);
				return true;
			}
			if (p.linked != null && over(TRIP, mx, my)) {
				send(ConsoleActionPayload.SCRAM, 0);
				return true;
			}
			if (p.linked != null && over(TRANSFER, mx, my)) {
				send(ConsoleActionPayload.TRANSFER, p.core.remote ? 0 : 1);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
