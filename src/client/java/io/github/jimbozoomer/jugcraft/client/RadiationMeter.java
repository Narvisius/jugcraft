package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nuclear.Radiation;
import java.util.Random;
import net.minecraft.client.Minecraft;

/**
 * The analogue meter behind the Geiger counter's needle and the readout's gauge (round 14: they didn't move). It
 * reads the dose rate the server syncs, adds natural background, and moves a needle the way a real ratemeter's does:
 * <ul>
 * <li>a log scale from 0.1 µSv/h (background) to 100 Sv/h, nine decades across the dial;</li>
 * <li>bands: SAFE below 50 µSv/h (where a real plant ropes off a "radiation area"), CAUTION up to 1 Sv/h, LETHAL
 * above (a lethal dose in a few hours for real; a few minutes in game, where an hour passes in 50 seconds);</li>
 * <li>the reading wanders with counting statistics: a tube counts about 2.5 a second per µSv/h, averaged over a
 * couple of seconds, so on background the needle hunts about, and it steadies as the rate climbs;</li>
 * <li>the needle is a damped spring: it swings to a new reading in about half a second and settles with a little
 * overshoot.</li>
 * </ul>
 * tools/nuclear.py draws the dial with the same scale (DIAL_LOW/HIGH/SAFE/LETHAL there).
 */
public final class RadiationMeter {
	/** The scale (mSv/h): the dial's ends, and the band edges. */
	public static final double LOW = 1e-4, HIGH = 1e5, SAFE = 0.05, LETHAL = 1000;
	/** Counts a second per mSv/h for the tube (2.5 per µSv/h). */
	static final double SENSITIVITY = 2500;
	/** The ratemeter's averaging time (s), and the needle's spring. */
	static final double TAU = 2.0, OMEGA = 9.0, ZETA = 0.62;
	static final double DT = 0.05;
	private static final Random RANDOM = new Random();
	private static double noise;
	private static double needle, previousNeedle, velocity;
	private static double reading = RadiationHud.BACKGROUND;

	public enum Zone {
		SAFE(0xFF4CC864), CAUTION(0xFFF0B428), LETHAL(0xFFE8402E);

		public final int colour;

		Zone(int colour) {
			this.colour = colour;
		}
	}

	private RadiationMeter() {
	}

	/** Where a rate (mSv/h, background included) sits on the dial, 0 (left stop) to 1 (right stop). */
	public static double position(double rate) {
		double lo = Math.log10(LOW), hi = Math.log10(HIGH);
		return Math.max(0, Math.min(1, (Math.log10(Math.max(rate, 1e-12)) - lo) / (hi - lo)));
	}

	public static Zone zone(double rate) {
		return rate >= LETHAL ? Zone.LETHAL : rate >= SAFE ? Zone.CAUTION : Zone.SAFE;
	}

	/** The needle now (0..1 across the dial). */
	public static double needle() {
		return needle;
	}

	/** The needle between the last tick and this one (for drawing every frame). */
	public static double needle(float partialTick) {
		return previousNeedle + (needle - previousNeedle) * partialTick;
	}

	/** The rate the meter shows (mSv/h, background included, with its statistical wobble). */
	public static double reading() {
		return reading;
	}

	/** The true rate the server sent, plus background (mSv/h). */
	public static double rate() {
		Minecraft mc = Minecraft.getInstance();
		return (mc.player == null ? 0 : Radiation.state(mc.player).rate()) + RadiationHud.BACKGROUND;
	}

	/** Every client tick. */
	static void tick(Minecraft mc) {
		previousNeedle = needle;
		if (mc.player == null) {
			needle = velocity = noise = 0;
			return;
		}
		if (mc.isPaused()) {
			return;
		}
		double rate = rate();
		// Counting statistics: the relative wobble of a count over the averaging time, as slow random drift.
		double counts = Math.max(1, rate * SENSITIVITY * TAU);
		double sigma = Math.min(0.45, 1 / Math.sqrt(counts));
		double a = Math.exp(-DT / TAU);
		noise = noise * a + sigma * Math.sqrt(1 - a * a) * RANDOM.nextGaussian();
		reading = rate * Math.exp(noise);
		double target = position(reading);
		double accel = OMEGA * OMEGA * (target - needle) - 2 * ZETA * OMEGA * velocity;
		velocity += accel * DT;
		needle += velocity * DT;
		if (needle < 0 || needle > 1) {
			// Against the stop pin.
			needle = Math.max(0, Math.min(1, needle));
			velocity = -velocity * 0.2;
		}
	}
}
