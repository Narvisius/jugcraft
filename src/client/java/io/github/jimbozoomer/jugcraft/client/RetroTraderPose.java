package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.world.RetroTraderJam;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * One frame of the Retro Trader's look, worked out when his render state is extracted: which eyes and mouth he
 * wears (frames of textures/entity/retro_trader/expressions.png, drawn by tools/retro_trader_look.py) and, during
 * his stomp-clap ({@link RetroTraderJam}), how his free arms, feet, body and head move. Pure arithmetic on the
 * routine's time, his age and his entity id, so every client draws the same jam.
 *
 * <p>Angles are radians and offsets model pixels, in the villager model's space (y down, his front towards -z).
 * Arm rotations are applied like a model part's (z, then y, then x), about each shoulder.
 */
public final class RetroTraderPose {
	// Eye frames, in the sheet's order (EYES in tools/retro_trader_look.py).
	public static final int EYES_OPEN = 0;
	public static final int EYES_HALF = 1;
	public static final int EYES_CLOSED = 2;
	public static final int EYES_LOOK_LEFT = 3;
	public static final int EYES_LOOK_RIGHT = 4;
	public static final int EYES_HAPPY = 5;
	public static final int EYES_WIDE = 6;
	public static final int EYES_SQUEEZE = 7;
	public static final int EYES_FROWN = 8;
	// Mouth frames (MOUTHS in tools/retro_trader_look.py).
	public static final int MOUTH_NEUTRAL = 0;
	public static final int MOUTH_SMILE = 1;
	public static final int MOUTH_GRIN = 2;
	public static final int MOUTH_TALK = 3;
	public static final int MOUTH_SHOUT = 4;
	public static final int MOUTH_OOH = 5;
	public static final int MOUTH_FROWN = 6;

	/** Shoulder pivots of the free arms (x, y, z); the right arm is on -x. */
	public static final float SHOULDER_X = 6.0F;
	public static final float SHOULDER_Y = 2.0F;
	public static final float SHOULDER_Z = -0.5F;
	/** Arm poses {xRot, yRot, zRot} for the right arm; the left arm mirrors yRot and zRot. */
	static final float[] FOLDED = {-0.8F, -0.75F, 0.0F};
	static final float[] READY = {-1.15F, -0.22F, 0.0F};
	static final float[] CLAP = {-1.3F, -0.58F, 0.0F};
	static final float[] TA_DA = {-0.35F, 0.0F, 2.0F};
	/** How far his nose sits above a villager's, in pixels (so it ends above his mouth instead of below his chin). */
	public static final float NOSE_RAISE = 2.0F;
	/** How high a stomping foot rises, in pixels. */
	static final float LIFT = 3.2F;

	/** What he is doing when he is not jamming. */
	public enum Mood {
		IDLE, NEAR, TALKING, UNHAPPY
	}

	/** Render-state access (VillagerRenderState gains this through a mixin). */
	public interface Holder {
		@Nullable RetroTraderPose jugcraft$retroTrader();

		void jugcraft$setRetroTrader(@Nullable RetroTraderPose pose);
	}

	public int eyes = EYES_OPEN;
	public int mouth = MOUTH_NEUTRAL;
	/** Whether he is mid-routine (the model mixin moves his feet, body and head). */
	public boolean jamming;
	/** Whether his free arms are drawn instead of the villager's folded arms. */
	public boolean freeArms;
	public final float[] rightArm = new float[3];
	public final float[] leftArm = new float[3];
	public float rightLift;
	public float leftLift;
	/** How far his body and head dip on an impact (pixels, down). */
	public float bounce;
	/** Extra head pitch (a nod on the beat) and roll (a sway with the groove). */
	public float nod;
	public float sway;

	/**
	 * Works out the frame. {@code jam} is ticks into the stomp-clap (with the partial tick), or negative when he is not
	 * jamming; {@code age} his age in ticks; {@code seed} his entity id (so traders blink and glance out of step).
	 */
	public RetroTraderPose update(float jam, float age, int seed, Mood mood) {
		jamming = jam >= 0 && jam < RetroTraderJam.LENGTH;
		freeArms = jamming;
		rightLift = leftLift = bounce = nod = sway = 0;
		if (jamming) {
			jam(jam);
		} else {
			idleFace(age, seed, mood);
		}
		return this;
	}

	// ------------------------------------------------------------------ the stomp-clap

	private void jam(float t) {
		float[] arm = arms(t);
		set(rightArm, leftArm, arm);
		for (int beat = 0; beat <= RetroTraderJam.BEATS; beat++) {
			int at = beatTick(beat);
			RetroTraderJam.Beat kind = RetroTraderJam.beat(at);
			boolean stomp = kind == RetroTraderJam.Beat.STOMP_RIGHT || kind == RetroTraderJam.Beat.STOMP_LEFT
					|| kind == RetroTraderJam.Beat.FINALE;
			if (stomp) {
				float lift = lift(t - at);
				if (kind == RetroTraderJam.Beat.STOMP_LEFT) {
					leftLift = Math.max(leftLift, lift);
				} else {
					rightLift = Math.max(rightLift, lift);
				}
			}
			float since = t - at;
			if (since >= 0 && since < 5) {
				float fall = 1 - since / 5;
				bounce = Math.max(bounce, (stomp ? 0.7F : 0.35F) * fall * fall);
			}
			if (since >= 0 && since < 8) {
				nod = Math.max(nod, 0.2F * (float) Math.exp(-since / 3.0));
			}
		}
		if (t >= RetroTraderJam.INTRO && t < RetroTraderJam.FINALE) {
			sway = 0.07F * Mth.sin((t - RetroTraderJam.INTRO) * Mth.PI / RetroTraderJam.BEAT / 2);
		}
		jamFace(t);
	}

	/** The tick of beat {@code beat} (0 to BEATS - 1 the groove, BEATS the finale). */
	static int beatTick(int beat) {
		return beat >= RetroTraderJam.BEATS ? RetroTraderJam.FINALE : RetroTraderJam.INTRO + beat * RetroTraderJam.BEAT;
	}

	/** A stomping foot's height {@code since} ticks after its beat: up over five ticks, then slammed down. */
	static float lift(float since) {
		float u = since + 7;
		if (u < 0 || u >= 7) {
			return 0;
		}
		if (u < 5) {
			return LIFT * smooth(u / 5);
		}
		float fall = (u - 5) / 2;
		return LIFT * (1 - fall * fall);
	}

	/** The right arm's pose at {@code t}: unfold, clap on the off-beats, a last clap, ta-da, fold again. */
	static float[] arms(float t) {
		int finale = RetroTraderJam.FINALE;
		if (t < RetroTraderJam.INTRO) {
			return lerp(FOLDED, READY, smooth(t / RetroTraderJam.INTRO));
		}
		if (t < finale) {
			float together = 0;
			for (int beat = 1; beat <= RetroTraderJam.BEATS; beat += 2) {
				together = Math.max(together, clap(t - beatTick(beat)));
			}
			together = Math.max(together, clap(t - finale));
			return lerp(READY, CLAP, together);
		}
		if (t < finale + 6) {
			return lerp(CLAP, TA_DA, smooth((t - finale) / 6));
		}
		if (t < finale + 10) {
			return TA_DA;
		}
		return lerp(TA_DA, FOLDED, smooth(Math.min(1, (t - finale - 10) / (RetroTraderJam.LENGTH - finale - 10))));
	}

	/** How closed the hands are {@code since} ticks after a clap's beat: in fast, out slower. */
	static float clap(float since) {
		if (since >= -5 && since < 0) {
			float u = (since + 5) / 5;
			return u * u;
		}
		if (since >= 0 && since < 6) {
			return 1 - smooth(since / 6);
		}
		return 0;
	}

	private void jamFace(float t) {
		eyes = EYES_HAPPY;
		mouth = MOUTH_GRIN;
		if (t < 4) {
			eyes = EYES_WIDE;
			return;
		}
		if (t >= RetroTraderJam.FINALE - 3) {
			boolean shouting = t < RetroTraderJam.FINALE + 12;
			eyes = shouting ? EYES_WIDE : EYES_OPEN;
			mouth = shouting ? MOUTH_SHOUT : MOUTH_SMILE;
			return;
		}
		for (int beat = 0; beat < RetroTraderJam.BEATS; beat += 2) {
			float since = t - beatTick(beat);
			if (since >= -3 && since < 7) {
				// The first stomp of each bar a squeezed-shut shout, the second a wide-eyed one.
				eyes = beat % 4 == 0 ? EYES_SQUEEZE : EYES_WIDE;
				mouth = MOUTH_SHOUT;
				return;
			}
		}
	}

	// ------------------------------------------------------------------ the everyday face

	private void idleFace(float age, int seed, Mood mood) {
		int tick = Mth.floor(age);
		eyes = EYES_OPEN;
		mouth = MOUTH_NEUTRAL;
		switch (mood) {
			case UNHAPPY -> {
				eyes = EYES_FROWN;
				mouth = MOUTH_FROWN;
			}
			case TALKING -> {
				int phase = Math.floorMod(tick, 40);
				if (phase < 26) {
					int syllable = hash(Math.floorDiv(tick, 3), seed) % 3;
					mouth = syllable == 0 ? MOUTH_TALK : syllable == 1 ? MOUTH_SMILE : MOUTH_OOH;
				} else {
					mouth = MOUTH_SMILE;
				}
			}
			case NEAR -> mouth = MOUTH_SMILE;
			case IDLE -> {
				// Now and then a glance to one side.
				int window = Math.floorDiv(tick + seed * 29, 70);
				if (Math.floorMod(tick + seed * 29, 70) < 40) {
					int pick = hash(window, seed) % 6;
					eyes = pick == 0 ? EYES_LOOK_LEFT : pick == 1 ? EYES_LOOK_RIGHT : EYES_OPEN;
				}
			}
		}
		int blink = blink(tick, seed);
		if (blink != EYES_OPEN && eyes != EYES_FROWN) {
			eyes = blink;
		} else if (blink != EYES_OPEN) {
			eyes = EYES_CLOSED;
		}
	}

	/** His blink at {@code tick}: half, shut, shut, half, about every three to five seconds. */
	static int blink(int tick, int seed) {
		int period = 64 + Math.floorMod(seed, 47);
		int phase = Math.floorMod(tick + seed * 11, period);
		return phase == 0 || phase == 3 ? EYES_HALF : phase < 3 ? EYES_CLOSED : EYES_OPEN;
	}

	// ------------------------------------------------------------------ helpers

	private static void set(float[] right, float[] left, float[] pose) {
		right[0] = pose[0];
		right[1] = pose[1];
		right[2] = pose[2];
		left[0] = pose[0];
		left[1] = -pose[1];
		left[2] = -pose[2];
	}

	static float[] lerp(float[] from, float[] to, float amount) {
		return new float[] {Mth.lerp(amount, from[0], to[0]), Mth.lerp(amount, from[1], to[1]), Mth.lerp(amount, from[2], to[2])};
	}

	static float smooth(float x) {
		float u = Mth.clamp(x, 0, 1);
		return u * u * (3 - 2 * u);
	}

	private static int hash(int a, int b) {
		int h = a * 0x9E3779B1 ^ b * 0x85EBCA6B;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		return h & 0x7FFFFFFF;
	}
}
