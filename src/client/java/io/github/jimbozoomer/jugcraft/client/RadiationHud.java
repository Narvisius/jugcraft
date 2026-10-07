package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import io.github.jimbozoomer.jugcraft.nuclear.Radiation;
import io.github.jimbozoomer.jugcraft.nuclear.RadiationState;
import java.util.Locale;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The dose readout (top-right corner) and the Geiger counter's clicking (client/GeigerSound.java). Both show only while the player holds the
 * counter or wears the suit's hood or coverall (the suit has a dosimeter badge). Everything here only displays what
 * the server worked out and synced in {@link JugcraftNuclear#RADIATION}; nothing is decided on the client.
 */
public final class RadiationHud {
	public static final Identifier ID = Jugcraft.id("radiation_hud");
	public static final Identifier TREFOIL = Jugcraft.id("textures/gui/radiation_trefoil.png");
	static final int W = 172, H = 66;

	private RadiationHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ID, RadiationHud::draw);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			RadiationMeter.tick(client);
			if (client.player != null && !client.isPaused()) {
				click(client.player);
			}
		});
	}

	static boolean holdingCounter(LocalPlayer player) {
		return player.getMainHandItem().is(JugcraftNuclear.GEIGER_COUNTER) || player.getOffhandItem().is(JugcraftNuclear.GEIGER_COUNTER);
	}

	static boolean shown(LocalPlayer player) {
		return holdingCounter(player) || Radiation.hazmatPieces(player) > 0
				&& (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(JugcraftNuclear.HAZMAT_ITEMS[0])
						|| player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(JugcraftNuclear.HAZMAT_ITEMS[1]));
	}

	/** Natural background (mSv/h): what the readout shows with nothing radioactive about. */
	public static final double BACKGROUND = 0.00015;

	/**
	 * Counts a second for a dose rate (mSv/h): a tick or two a few seconds apart on background, a brisk patter by
	 * uranium ore, a steady rattle at a few mSv/h, a roar near spent fuel, topping out where the tube saturates. (A
	 * real counter is far more sensitive than this; the scale is squeezed so the whole range from ore to a reactor
	 * can be told apart by ear.)
	 */
	public static double countsPerSecond(double rate) {
		return Math.min(600, 0.4 + 60 * Math.pow(Math.max(0, rate), 0.7));
	}

	/** The counter's live sound while it is in hand (client/GeigerSound.java). */
	private static GeigerSound playing;

	private static void click(LocalPlayer player) {
		GeigerSound.countsPerSecond = countsPerSecond(Radiation.state(player).rate());
		if (!holdingCounter(player)) {
			return;
		}
		var sounds = Minecraft.getInstance().getSoundManager();
		if (playing == null || !sounds.isActive(playing)) {
			playing = new GeigerSound();
			sounds.play(playing);
		}
	}

	/**
	 * The readout (round 14): the rate and its band (SAFE / CAUTION / LETHAL), a live gauge whose needle swings
	 * across a log scale from background to 100 Sv/h (RadiationMeter, the same as the Geiger counter's dial), and the
	 * dose taken so far on a thin bar under it.
	 */
	private static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !shown(player)) {
			return;
		}
		RadiationState state = Radiation.state(player);
		Font font = mc.font;
		int x = g.guiWidth() - W - 6, y = 6;
		double shownRate = RadiationMeter.reading();
		RadiationMeter.Zone zone = RadiationMeter.zone(RadiationMeter.rate());
		g.fill(x, y, x + W, y + H, 0xD20A1016);
		g.outline(x, y, W, H, zone.colour);
		g.blit(RenderPipelines.GUI_TEXTURED, TREFOIL, x + 4, y + 4, 0, 0, 20, 20, 22, 22, 22, 22);
		g.text(font, rate(shownRate - BACKGROUND), x + 28, y + 5, 0xFFFFC43C, true);
		String word = zone.name();
		g.text(font, word, x + W - 6 - font.width(word), y + 5, zone.colour, true);
		int pieces = Radiation.hazmatPieces(player);
		g.text(font, pieces > 0 ? "SUIT " + Math.round(pieces * 22.5) + "%" : "NO SUIT", x + 28, y + 16, 0xFF9AB0BC, false);
		// The gauge: the three bands on a log scale, decade ticks, and the needle.
		int bx0 = x + 8, bx1 = x + W - 8, gy = y + 32;
		int span = bx1 - bx0;
		int safe = bx0 + (int) Math.round(span * RadiationMeter.position(RadiationMeter.SAFE));
		int lethal = bx0 + (int) Math.round(span * RadiationMeter.position(RadiationMeter.LETHAL));
		g.fill(bx0, gy, safe, gy + 7, 0xFF3C9A50);
		g.fill(safe, gy, lethal, gy + 7, 0xFFD89A1C);
		g.fill(lethal, gy, bx1, gy + 7, 0xFFC8362A);
		g.fill(bx0, gy + 5, bx1, gy + 7, 0x40000000);
		for (int dec = -4; dec <= 5; dec++) {
			int tx = bx0 + (int) Math.round(span * RadiationMeter.position(Math.pow(10, dec)));
			g.fill(tx, gy - 3, tx + 1, gy, 0xFFBEC8D0);
		}
		g.pose().pushMatrix();
		g.pose().scale(0.75F, 0.75F);
		for (double[] mark : new double[][] {{1e-3, 0}, {1, 1}, {1e3, 2}}) {
			String text = new String[] {"1 \u00B5Sv", "1 mSv", "1 Sv"}[(int) mark[1]];
			int tx = bx0 + (int) Math.round(span * RadiationMeter.position(mark[0]));
			g.text(font, text, Math.round((tx - font.width(text) * 0.375F) / 0.75F), Math.round((gy + 10) / 0.75F), 0xFFBEC8D0, false);
		}
		g.pose().popMatrix();
		int nx = bx0 + (int) Math.round(span * RadiationMeter.needle(delta.getGameTimeDeltaPartialTick(false)));
		g.fill(nx - 1, gy - 4, nx + 1, gy + 9, 0xFFFFFFFF);
		g.fill(nx - 2, gy - 6, nx + 2, gy - 4, 0xFFFFFFFF);
		// The dose so far.
		int dy = y + 53;
		String doseText = "DOSE " + dose(state.dose());
		g.text(font, doseText, x + 8, dy, 0xFFBEE2EC, false);
		int dx0 = x + 14 + font.width(doseText), dx1 = bx1;
		int dspan = Math.max(1, dx1 - dx0);
		for (int k = 0; k < dspan; k++) {
			double t = k / (double) dspan;
			int colour = t < 1 / 3.0 ? 0xFF5ADC78 : t < 2 / 3.0 ? 0xFFFFBA3C : 0xFFF04034;
			g.fill(dx0 + k, dy + 2, dx0 + k + 1, dy + 6, colour);
		}
		int mark = dx0 + (int) Math.round(dspan * barPosition(state.dose()));
		g.fill(mark - 1, dy, mark + 1, dy + 8, 0xFFFFFFFF);
	}

	/** Where a dose sits on the bar: the first third up to sickness, the middle third to severe, the last to lethal. */
	static double barPosition(double dose) {
		if (dose < Radiation.SICKNESS) {
			return dose / Radiation.SICKNESS / 3;
		}
		if (dose < Radiation.SEVERE) {
			return 1 / 3.0 + (dose - Radiation.SICKNESS) / (Radiation.SEVERE - Radiation.SICKNESS) / 3;
		}
		return Math.min(1, 2 / 3.0 + (dose - Radiation.SEVERE) / (Radiation.LETHAL - Radiation.SEVERE) / 3);
	}

	/** The rate as a counter shows it: microsieverts an hour below one millisievert, background included. */
	static String rate(double rate) {
		rate += BACKGROUND;
		if (rate < 1) {
			double micro = rate * 1000;
			return (micro < 10 ? String.format(Locale.ROOT, "%.2f", micro) : String.format(Locale.ROOT, "%.0f", micro)) + " \u00B5Sv/h";
		}
		return (rate < 100 ? String.format(Locale.ROOT, "%.2f", rate) : String.format(Locale.ROOT, "%,.0f", rate)) + " mSv/h";
	}

	static String dose(double dose) {
		return (dose < 10 ? String.format(Locale.ROOT, "%.1f", dose) : String.format(Locale.ROOT, "%,.0f", dose)) + " mSv";
	}
}
