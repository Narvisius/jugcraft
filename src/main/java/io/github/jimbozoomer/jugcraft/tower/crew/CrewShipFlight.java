package io.github.jimbozoomer.jugcraft.tower.crew;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The crew drop ship's visit, as a timeline both sides work out alike (approved 6 November 2026: the drop ship sheet,
 * take 4, and its guns' close-ups): where the ship is at each moment from its landing point and how its parts stand.
 * The server keeps the entity on the landing point and only sends the phase and when it began; each player's game
 * draws the flight from that, so it is smooth however far out the ship is.
 *
 * <ol>
 * <li>{@link #ENTRY}: from out of sight high over the land beyond the landing point, nose down toward the tower,
 * wrapped in fire as it comes into the air.</li>
 * <li>{@link #BRAKE}: down to {@link #HOVER_HIGH} over the point, the thrusters swinging down and burning hard.</li>
 * <li>{@link #HOVER}: settling to {@link #HOVER_LOW}, turning round on the spot so its stern faces the tower, the
 * gear and the floodlights coming on, the side doors opening, the guns out.</li>
 * <li>{@link #TOUCHDOWN}: down onto its gear (or, at the roof, to its hover beside it).</li>
 * <li>{@link #OPEN}: the ramp swings down about its hinge at the bay floor's aft edge onto the ground (the rework, 8
 * November 2026: shut, the ramp is itself the ship's slanted back, leaning forward over the opening; no platform and no
 * stern door any more).</li>
 * <li>{@link #UNLOAD}: as long as the crew take to come off (the server says when it's over).</li>
 * <li>{@link #CLOSE}, {@link #LIFT}, {@link #CLIMB}: everything in again and back up out of sight.</li>
 * </ol>
 *
 * The ship's frame: blocks; x to starboard, y up from the ground under its gear, z aft (the nose at -z). Its yaw turns
 * that frame about y (a point's world offset is Ry(yaw) Rx(pitch) p), so the stern faces (sin yaw, 0, cos yaw).
 */
public final class CrewShipFlight {
	public static final int ENTRY = 0, BRAKE = 1, HOVER = 2, TOUCHDOWN = 3, OPEN = 4, UNLOAD = 5, CLOSE = 6, LIFT = 7, CLIMB = 8, GONE = 9;
	/** A ship set down to stay (the creative menu's Drop Ship (Landed)): down, open, lit and on guard, never leaving. */
	public static final int PARKED = 10;
	/** The keep-out ring (Justin, 6 November 2026: "stay back ... if they come within like 10 blocks of the ship, it opens
	 * fire"): an ellipse round the ship 10 blocks out from its wings, nose and ramp (the ship's frame: half-widths across
	 * and along, its middle's z), drawn in red floating at the second block while it's down: 10 blocks ahead of the nose
	 * (z -35.5) and 10 past the ramp's foot (FOOT, z 25.4). Since 7 November 2026 (Justin: "the dropship killed somebody
	 * who wasn't inside the radius ... expand the radius ... to match the actual kill radius"; his pick: a bigger ring, the
	 * guns staying inside it) 4 blocks wider each way, and while it's down the guns only fire at what is inside it. */
	public static final float ZONE_A = 30.0F, ZONE_B = 40.0F, ZONE_Z = 0.0F;
	/** Each phase's length (ticks); UNLOAD's is up to the server. */
	public static final int[] LENGTH = {200, 140, 80, 50, 80, -1, 80, 60, 240};
	/** The belly's height on its gear, the ramp's length (hinge to end), half its thickness (its walking face that far
	 * over the hinge line), and its hinge: its height over the ship's ground and its z (tools/crew_ship.py HINGE). */
	public static final float G = 2.6F, LEAF = 7.5F, RAMP_T = 0.3F, HINGE_Y = G + 0.7F, HINGE = 19.0F;
	/** The stern's z (about: the slanted back's middle). */
	public static final float STERN = 18.2F;
	/** About where the ramp's foot comes on level ground. */
	public static final float FOOT = HINGE + LEAF * 0.9F;
	/** The shut ramp's turn about its hinge (radians about x): leaning forward 52 degrees from the ground, its end up. */
	public static final float SHUT = -(Mth.PI - 52 * Mth.DEG_TO_RAD);
	/** Heights over the landing point: where the braking ends, where the hover ends, how high it lifts before climbing. */
	public static final float HOVER_HIGH = 16, HOVER_LOW = 7, LIFT_HIGH = 12, LIFT_HIGH_ROOF = 6;
	/** How far out it comes from and goes to (blocks), and how high over the landing point at most. */
	public static final float FAR = 700, AWAY = 600, CEILING = 300;
	/** The pitch it comes in at (degrees, nose down), and the flare as it brakes. */
	public static final float ENTRY_PITCH = -18, FLARE = 10, CLIMB_PITCH = 12;

	private CrewShipFlight() {
	}

	/** The ship and its parts at a moment. Angles in radians. */
	public static final class Pose {
		/** Where its frame's origin is, from the landing point (world axes). */
		public double x, y, z;
		public float yaw, pitch;
		/** The thrusters (0 flat, 1 straight down), their burn (0-1), the gear (0 up, 1 down), the side doors (0 shut, 1
		 * open), the ramp (0 shut, 1 down) and its turn about its hinge ({@link #SHUT} shut; down to the ground: + tips its
		 * end down), the fire of entry (0-1). */
		public float tilt, thrust, gear, sides, ramp, leaf, entry;
		/** The floodlights on; the guns on watch; the keep-out ring out. */
		public boolean lights, guns, zone;

		/** A point of the ship's frame, from the landing point (world axes). */
		public Vec3 world(double px, double py, double pz) {
			double cp = Math.cos(pitch), sp = Math.sin(pitch);
			double y1 = py * cp - pz * sp, z1 = py * sp + pz * cp;
			double cy = Math.cos(yaw), sy = Math.sin(yaw);
			return new Vec3(x + px * cy + z1 * sy, y + y1, z - px * sy + z1 * cy);
		}
	}

	/** Smooth from 0 to 1 as u goes 0 to 1 (clamped). */
	public static float smooth(float u) {
		u = Mth.clamp(u, 0, 1);
		return u * u * (3 - 2 * u);
	}

	/** The nose's direction for a yaw. */
	private static double noseX(float yaw) {
		return -Math.sin(yaw);
	}

	private static double noseZ(float yaw) {
		return -Math.cos(yaw);
	}

	/** The ramp's turn down onto the ground (radians, + its end down) for its hinge {@code reach} over the ground at its
	 * foot: its walking face's end on the ground, reach + RAMP_T cos(a) = LEAF sin(a). */
	public static float leafDrop(float reach) {
		double a = 0.3;
		for (int i = 0; i < 30; i++) {
			a = Math.asin(Mth.clamp((reach + RAMP_T * Math.cos(a)) / LEAF, -1, 1));
		}
		return (float) a;
	}

	/** A point on the ramp's walking face {@code t} along it from the hinge, turned {@code a} (the ship's frame: y, z). */
	public static double rampY(float a, double t) {
		return HINGE_Y + RAMP_T * Math.cos(a) - t * Math.sin(a);
	}

	public static double rampZ(float a, double t) {
		return HINGE + RAMP_T * Math.sin(a) + t * Math.cos(a);
	}

	/**
	 * The pose {@code t} ticks into {@code phase} for a ship landing facing {@code yaw}, at the roof (no gear, a shorter
	 * lift) or on the ground, its leaf reaching {@code reach} down, at most {@code ceiling} high over the landing point.
	 */
	public static Pose pose(int phase, float t, float yaw, boolean roof, float reach, float ceiling, Pose p) {
		float h = yaw + Mth.PI;                         // coming in nose first toward the tower, before it turns
		float top = Math.min(ceiling, CEILING);
		float leafDown = leafDrop(reach);
		p.yaw = yaw;
		p.pitch = 0;
		p.tilt = 1;
		p.thrust = 0;
		p.gear = 0;
		p.sides = 0;
		p.ramp = 0;
		p.leaf = SHUT;
		p.entry = 0;
		p.lights = false;
		p.guns = false;
		p.zone = false;
		p.x = p.y = p.z = 0;
		float idle = roof ? 0.55F : 0.3F;
		switch (phase) {
			case ENTRY -> {
				float u = Mth.clamp(t / LENGTH[ENTRY], 0, 1);
				float e = 1 - (1 - u) * (1 - u);
				double sx = -noseX(h) * FAR, sz = -noseZ(h) * FAR, ex = -noseX(h) * 120, ez = -noseZ(h) * 120;
				p.x = Mth.lerp(e, sx, ex);
				p.y = Mth.lerp(e, top, Math.min(70, top));
				p.z = Mth.lerp(e, sz, ez);
				p.yaw = h;
				p.pitch = Mth.lerp(u, ENTRY_PITCH, -6) * Mth.DEG_TO_RAD;
				p.tilt = 0;
				p.entry = Mth.clamp(1 - u / 0.65F, 0, 1);
			}
			case BRAKE -> {
				float u = Mth.clamp(t / LENGTH[BRAKE], 0, 1);
				float e = smooth(u);
				p.x = Mth.lerp(e, -noseX(h) * 120, 0);
				p.y = Mth.lerp(e, Math.min(70, top), HOVER_HIGH);
				p.z = Mth.lerp(e, -noseZ(h) * 120, 0);
				p.yaw = h;
				float f = u < 0.6F ? Mth.lerp(u / 0.6F, -6, FLARE) : Mth.lerp((u - 0.6F) / 0.4F, FLARE, 0);
				p.pitch = f * Mth.DEG_TO_RAD;
				p.tilt = smooth((u - 0.15F) / 0.6F);
				p.thrust = 1;
			}
			case HOVER -> {
				float u = Mth.clamp(t / LENGTH[HOVER], 0, 1);
				p.y = Mth.lerp(smooth(u), HOVER_HIGH, HOVER_LOW);
				p.yaw = yaw + Mth.PI * (1 - smooth(u));
				p.thrust = 0.8F;
				p.gear = roof ? 0 : smooth((u - 0.3F) / 0.7F);
				p.sides = smooth((u - 0.5F) / 0.5F);
				p.lights = p.guns = true;
			}
			case TOUCHDOWN -> {
				float u = Mth.clamp(t / LENGTH[TOUCHDOWN], 0, 1);
				p.y = Mth.lerp(smooth(u), HOVER_LOW, 0);
				p.thrust = Mth.lerp(u, 0.8F, idle);
				p.gear = roof ? 0 : 1;
				p.sides = 1;
				p.lights = p.guns = true;
				p.zone = u > 0.5F;
			}
			case OPEN, UNLOAD, CLOSE, PARKED -> {
				float k = phase == UNLOAD || phase == PARKED ? LENGTH[OPEN] : phase == OPEN ? t : LENGTH[CLOSE] - t;
				p.zone = true;
				p.thrust = idle;
				p.gear = roof ? 0 : 1;
				p.sides = 1;
				// the ramp swings down about its hinge from the shut back onto the ground
				p.ramp = smooth(k / LENGTH[OPEN]);
				p.leaf = Mth.lerp(p.ramp, SHUT, leafDown);
				p.lights = p.guns = true;
			}
			case LIFT -> {
				float u = Mth.clamp(t / LENGTH[LIFT], 0, 1);
				p.y = Mth.lerp(smooth(u), 0, roof ? LIFT_HIGH_ROOF : LIFT_HIGH);
				p.thrust = Mth.lerp(u, idle, 1);
				p.gear = roof ? 0 : 1 - smooth((u - 0.3F) / 0.7F);
				p.sides = 1 - smooth(u / 0.5F);
				p.guns = true;
			}
			case CLIMB -> {
				float u = Mth.clamp(t / LENGTH[CLIMB], 0, 1);
				float e = u * u;
				float lift = roof ? LIFT_HIGH_ROOF : LIFT_HIGH;
				p.x = noseX(yaw) * AWAY * e;
				p.y = Mth.lerp(e, lift, top);
				p.z = noseZ(yaw) * AWAY * e;
				p.tilt = 1 - 0.8F * smooth(u / 0.5F);
				p.pitch = CLIMB_PITCH * smooth(u / 0.4F) * Mth.DEG_TO_RAD;
				p.thrust = 1;
			}
			default -> {
				p.y = top;
			}
		}
		return p;
	}

	/** Whether a point of the ship's frame (x across, z along) is inside the keep-out ring. */
	public static boolean inZone(double x, double z) {
		double u = x / ZONE_A, v = (z - ZONE_Z) / ZONE_B;
		return u * u + v * v <= 1;
	}

	/** Whether the bay turret can see out (the ramp mostly down). */
	public static boolean rampOpen(Pose p) {
		return p.ramp > 0.6F;
	}
}
