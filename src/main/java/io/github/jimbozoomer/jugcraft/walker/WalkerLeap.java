package io.github.jimbozoomer.jugcraft.walker;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Client-only state of the Howitzer Walker's jump clip (jump_pose in tools/howitzer_walker.py), fitted to the real leap:
 * the crouch from the tick the server says it crouched, the stretch as it springs off, a tucked pose in the air read
 * from how fast it climbs or falls (a leap lasts as long as the ground below it makes it), and the squash and bouncy
 * settle from the tick it lands. A long fall plays the air and landing too, so a slam or a drop off a ledge lands soft.
 */
public final class WalkerLeap {
	/** Where each section of the clip starts, in its ticks: the launch, the air, the landing and the end. */
	public static final float LAUNCH = 6.0F;
	public static final float AIR = 9.0F;
	public static final float LAND = 20.0F;
	public static final float END = 30.0F;
	/** Falling at least this fast, in blocks a tick (after about two blocks), with no clip playing starts the air section. */
	private static final float FALLING = 0.45F;
	private static final int TIMEOUT = 200;
	private static final int NONE = -1000;
	private int crouchedAt = NONE;
	private int landedAt = NONE;
	private boolean aloft;
	private int still;
	private float previousClimb;
	private float climb;

	/** The server says it crouched to leap. */
	public void crouched(int tick) {
		crouchedAt = tick;
		landedAt = NONE;
		aloft = false;
		still = 0;
	}

	public void tick(Entity walker) {
		previousClimb = climb;
		climb = (float) (walker.getY() - walker.yo);
		int since = walker.tickCount - crouchedAt;
		boolean playing = crouchedAt != NONE && since <= TIMEOUT && (landedAt < crouchedAt || walker.tickCount - landedAt < END - LAND);
		if (!playing) {
			crouchedAt = NONE;
			if (climb < -FALLING && !walker.onGround()) {
				// Falling with no leap: join the clip in the air.
				crouchedAt = walker.tickCount - (int) AIR;
				landedAt = NONE;
				aloft = true;
				still = 0;
			}
			return;
		}
		if (landedAt >= crouchedAt || since < LAUNCH) {
			return;
		}
		if (!aloft) {
			if (!walker.onGround() || climb > 0.05F) {
				aloft = true;
			} else if (since > AIR + 6) {
				crouchedAt = NONE; // It never left the ground (a ceiling, or out of fuel).
			}
			return;
		}
		still = Math.abs(climb) < 1.0E-4F ? still + 1 : 0;
		if (walker.onGround() && climb <= 0.0F || still >= 2) {
			landedAt = walker.tickCount;
		}
	}

	/** Where the clip is, in its ticks (0 to {@link #END}), or -1 when it is not playing. */
	public float time(Entity walker, float partialTick, double leapSpeed) {
		if (crouchedAt == NONE) {
			return -1.0F;
		}
		if (landedAt >= crouchedAt) {
			float landed = walker.tickCount - landedAt + partialTick;
			return landed < END - LAND ? LAND + landed : -1.0F;
		}
		float since = walker.tickCount - crouchedAt + partialTick;
		if (since < AIR) {
			return since;
		}
		if (!aloft) {
			return AIR;
		}
		// Rising at leap speed is the start of the air section, falling as fast the end of it.
		float rise = Mth.lerp(partialTick, previousClimb, climb);
		float u = Mth.clamp((float) ((leapSpeed - rise) / (2.0 * leapSpeed)), 0.0F, 1.0F);
		return AIR + (LAND - AIR) * u * 0.999F;
	}

	/** How firmly the clip holds the walker at clip time {@code t}, 0 to 1: the walk gives way to it and takes over again. */
	public static float weight(float t) {
		if (t < 0) {
			return 0.0F;
		}
		if (t < 3.0F) {
			return t / 3.0F;
		}
		return t > END - 4.0F ? (END - t) / 4.0F : 1.0F;
	}
}
