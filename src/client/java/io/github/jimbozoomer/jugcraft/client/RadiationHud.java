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
	static final int W = 150, H = 58;

	private RadiationHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ID, RadiationHud::draw);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
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

	/**
	 * Counts a second for a dose rate: a few a minute of background, then roughly in step with the rate (a real
	 * counter's count rate is proportional to the dose rate), topping out where the tube saturates.
	 */
	public static double countsPerSecond(double rate) {
		return Math.min(600, 0.15 + rate * 0.8);
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

	private static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null || !shown(player)) {
			return;
		}
		RadiationState state = Radiation.state(player);
		Font font = mc.font;
		int x = g.guiWidth() - W - 6, y = 6;
		int edge = state.rate() >= 50 ? 0xFFF04034 : state.rate() >= 5 ? 0xFFFFBA3C : 0xFFF0C828;
		g.fill(x, y, x + W, y + H, 0xD20A1016);
		g.outline(x, y, W, H, edge);
		g.blit(RenderPipelines.GUI_TEXTURED, TREFOIL, x + 4, y + 4, 0, 0, 22, 22, 22, 22, 22, 22);
		g.text(font, rate(state.rate()), x + 30, y + 6, 0xFFFFC43C, true);
		int pieces = Radiation.hazmatPieces(player);
		String line = "DOSE " + dose(state.dose()) + (pieces > 0 ? "  SUIT " + Math.round(pieces * 22.5) + "%" : "");
		g.text(font, line, x + 30, y + 18, 0xFFBEE2EC, false);
		// The dose bar: safe, sickness, lethal.
		int bx0 = x + 6, bx1 = x + W - 6, by = y + 34;
		int span = bx1 - bx0;
		for (int k = 0; k < span; k++) {
			double t = k / (double) span;
			int colour = t < 1 / 3.0 ? 0xFF5ADC78 : t < 2 / 3.0 ? 0xFFFFBA3C : 0xFFF04034;
			g.fill(bx0 + k, by, bx0 + k + 1, by + 5, colour);
		}
		int mark = bx0 + (int) Math.round(span * barPosition(state.dose()));
		g.fill(mark - 1, by - 3, mark + 2, by + 8, 0xFFFFFFFF);
		g.text(font, "SAFE", bx0, by + 9, 0xFF5ADC78, false);
		g.text(font, "LETHAL", bx1 - font.width("LETHAL"), by + 9, 0xFFF04034, false);
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

	static String rate(double rate) {
		if (rate < 0.01) {
			return "0.00 mSv/h";
		}
		return (rate < 100 ? String.format(Locale.ROOT, "%.2f", rate) : String.format(Locale.ROOT, "%,.0f", rate)) + " mSv/h";
	}

	static String dose(double dose) {
		return (dose < 10 ? String.format(Locale.ROOT, "%.1f", dose) : String.format(Locale.ROOT, "%,.0f", dose)) + " mSv";
	}
}
