package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nuclear.JugcraftNuclear;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.lwjgl.BufferUtils;

/**
 * The Geiger counter's sound, made live (no recording): while the counter is in hand this streams audio generated a
 * few hundredths of a second at a time from the current dose rate. Counts arrive at random, a Poisson process with
 * the tube's dead time, so it never repeats and it follows the radiation as it changes. Each count is the tube's
 * discharge pulse kicking a small speaker: a resonant cone and a little case thump (tools/nuclear.py has the same
 * model and renders a preview).
 */
public class GeigerSound extends AbstractTickableSoundInstance implements FabricSoundInstance {
	static final int RATE = 44_100;
	/** Each read hands back this much audio (80 ms): the game keeps four queued, so the sound lags the rate by about a third of a second. */
	static final int CHUNK = RATE * 80 / 1000;
	static final int DEAD_TIME = 4;                 // ~0.1 ms: the tube cannot count again straight away
	static final float[][] CLICKS = new float[4][];

	/** Counts per second, set every client tick from the synced dose rate and read on the sound thread. */
	static volatile double countsPerSecond;

	static {
		for (int k = 0; k < CLICKS.length; k++) {
			CLICKS[k] = click(new Random(31L * k + 7));
		}
	}

	GeigerSound() {
		super(JugcraftNuclear.GEIGER, SoundSource.PLAYERS, RandomSource.create());
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.volume = 0.7F;
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	@Override
	public void tick() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !RadiationHud.holdingCounter(player)) {
			stop();
		}
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
		return CompletableFuture.completedFuture(new Counts());
	}

	/** One count: about 12 ms. */
	static float[] click(Random r) {
		int n = (int) (RATE * 0.012);
		double[] pulse = new double[n];
		int width = 3 + r.nextInt(2);
		for (int i = 0; i < width; i++) {
			pulse[i] = 1.0;
			pulse[width + i] = -0.55;
		}
		for (int i = 0; i < n; i++) {
			pulse[i] += r.nextGaussian() * 0.02 * Math.exp(-i / (RATE * 0.0008));
		}
		double[] cone = resonance(pulse, 3300 + (r.nextDouble() * 300 - 150), 4.5);
		double[] body = resonance(pulse, 620 + (r.nextDouble() * 80 - 40), 2.2);
		float[] out = new float[n];
		double peak = 1e-9;
		for (int i = 0; i < n; i++) {
			double v = (cone[i] + body[i] * 0.6) * Math.exp(-i / (RATE * 0.0025));
			out[i] = (float) v;
			peak = Math.max(peak, Math.abs(v));
		}
		for (int i = 0; i < n; i++) {
			out[i] /= (float) peak;
		}
		return out;
	}

	/** A two-pole resonant filter (the speaker cone). */
	private static double[] resonance(double[] x, double freq, double q) {
		double w = 2 * Math.PI * freq / RATE;
		double r = Math.exp(-w / (2 * q));
		double a1 = -2 * r * Math.cos(w), a2 = r * r;
		double[] y = new double[x.length];
		for (int n = 0; n < x.length; n++) {
			y[n] = x[n] - a1 * (n > 0 ? y[n - 1] : 0) - a2 * (n > 1 ? y[n - 2] : 0);
		}
		return y;
	}

	/** The endless stream of counts. */
	public static final class Counts implements AudioStream {
		private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
		private final Random random = new Random();
		private final float[] carry = new float[CLICKS[0].length];
		/** Samples from the start of the next chunk to the next count. */
		private double next = -1;
		private double rate = -1;
		/** Counts made so far (tests). */
		public long counted;

		@Override
		public AudioFormat getFormat() {
			return FORMAT;
		}

		/** Generates the next chunk of audio (fewer bytes than asked for, so the sound keeps up with the rate). */
		@Override
		public ByteBuffer read(int size) {
			int n = Math.max(1, Math.min(size / 2, CHUNK));
			float[] mix = mix(n, countsPerSecond);
			ByteBuffer buffer = BufferUtils.createByteBuffer(n * 2);
			for (int i = 0; i < n; i++) {
				// A soft limit, so a roaring counter saturates instead of clipping harshly.
				buffer.putShort((short) Math.round(Math.tanh(mix[i] * 0.55) * 30_000));
			}
			buffer.flip();
			return buffer;
		}

		/** n samples of counts at cps counts a second. */
		public float[] mix(int n, double cps) {
			float[] out = new float[n + carry.length];
			System.arraycopy(carry, 0, out, 0, carry.length);
			if (cps > 0) {
				double mean = RATE / cps;
				// Counting is memoryless, so when the rate changes the wait for the next count starts afresh.
				if (next < 0 || Math.abs(cps - rate) > rate * 0.05) {
					next = exponential(mean);
					rate = cps;
				}
				while (next < n) {
					int at = (int) next;
					float[] click = CLICKS[random.nextInt(CLICKS.length)];
					float gain = 0.9F + random.nextFloat() * 0.1F;
					for (int i = 0; i < click.length; i++) {
						out[at + i] += click[i] * gain;
					}
					counted++;
					next += DEAD_TIME + exponential(mean);
				}
				next -= n;
			} else {
				next = -1;
			}
			System.arraycopy(out, n, carry, 0, carry.length);
			return out;
		}

		private double exponential(double mean) {
			return -Math.log(1 - random.nextDouble()) * mean;
		}

		@Override
		public void close() {
		}
	}
}
