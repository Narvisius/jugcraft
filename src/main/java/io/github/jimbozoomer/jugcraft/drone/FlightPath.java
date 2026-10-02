package io.github.jimbozoomer.jugcraft.drone;

/**
 * The shape and timing of one drone flight. No Minecraft types, so the server (leg lengths for the
 * {@link FlightScheduler}) and every client (where to draw the drone) use exactly the same maths.
 *
 * <p>Points: home (the drone's dock), the supply pickup, each stop, home again. Each leg climbs
 * straight up to its cruise height, flies level, and descends straight down, so its length is the
 * horizontal distance plus both vertical parts. A drone docked in a hangar (or under the tower's hangar
 * deck) first flies straight out, level, to its exit point and climbs from there; coming home it comes
 * down at the exit point and flies straight in. Stops under a roof or overhang work the same way: the drone
 * comes down beside them where the sky is open and flies in level ({@link #approach}). Timings match {@link FlightScheduler#launch}: fly to the
 * pickup, winch the crate up for {@link FlightScheduler#PICKUP_TICKS}, fly to each stop and hover
 * {@link FlightScheduler#DROP_TICKS} while the block fills in, fly home and charge for
 * {@link FlightScheduler#RECHARGE_TICKS}.
 */
public final class FlightPath {
	/** Height of the hover point above the pickup plates, where the crate is winched up to. */
	public static final double PICKUP_HOVER = 1.6;
	/** Cable length while carrying (crate just under the drone). */
	public static final double CARRY_CABLE = 0.35;

	private final double[] xs;
	private final double[] ys;
	private final double[] zs;
	private final double[] cruise;
	/**
	 * For each point, the {x, z} where the drone climbs and comes down for it, reached level at the point's own
	 * height, or null (straight up and down at the point). Used for hangars (fly out of the door first) and for
	 * targets under a roof or overhang (fly in sideways from the nearest open column).
	 */
	private final double[] @org.jspecify.annotations.Nullable [] approach;
	private final int speed;
	/** Tick at which each leg starts and ends. */
	private final long[] legStart;
	private final long[] legEnd;
	private final long total;

	/**
	 * @param points {x, y, z} for home, pickup, stops..., home (at least 3 points)
	 * @param cruise cruise height of each leg (points - 1 entries); raised to at least both ends
	 * @param speed blocks per second
	 */
	public FlightPath(double[][] points, double[] cruise, int speed) {
		this(points, cruise, speed, null, null);
	}

	/**
	 * @param exitOut {x, z} to fly out to (level, at home's height) before the first climb, or null
	 * @param exitIn  {x, z} to come down at before flying in (level) to home at the end, or null
	 */
	public FlightPath(double[][] points, double[] cruise, int speed, double @org.jspecify.annotations.Nullable [] exitOut,
			double @org.jspecify.annotations.Nullable [] exitIn) {
		this(points, cruise, speed, ends(points.length, exitOut, exitIn));
	}

	private static double[][] ends(int n, double @org.jspecify.annotations.Nullable [] exitOut, double @org.jspecify.annotations.Nullable [] exitIn) {
		double[][] approach = new double[Math.max(0, n)][];
		if (n > 0) {
			approach[0] = exitOut;
			approach[n - 1] = exitIn;
		}
		return approach;
	}

	/** @param approach per point {x, z} to climb and come down at (reached level at the point's height), or null entries */
	public FlightPath(double[][] points, double[] cruise, int speed, double[] @org.jspecify.annotations.Nullable [] approach) {
		int n = points.length;
		this.approach = new double[n][];
		for (int i = 0; i < n && i < approach.length; i++) {
			this.approach[i] = approach[i] == null ? null : approach[i].clone();
		}
		if (n < 3 || cruise.length != n - 1 || speed <= 0) {
			throw new IllegalArgumentException("bad flight path");
		}
		xs = new double[n];
		ys = new double[n];
		zs = new double[n];
		for (int i = 0; i < n; i++) {
			xs[i] = points[i][0];
			ys[i] = points[i][1];
			zs[i] = points[i][2];
		}
		this.cruise = new double[n - 1];
		for (int i = 0; i < n - 1; i++) {
			this.cruise[i] = Math.max(cruise[i], Math.max(ys[i], ys[i + 1]));
		}
		this.speed = speed;
		legStart = new long[n - 1];
		legEnd = new long[n - 1];
		long time = 0;
		for (int i = 0; i < n - 1; i++) {
			legStart[i] = time;
			time += FlightScheduler.legTicks(legLength(i), speed);
			legEnd[i] = time;
			if (i == 0) {
				time += FlightScheduler.PICKUP_TICKS;
			} else if (i < n - 2) {
				time += FlightScheduler.DROP_TICKS;
			}
		}
		total = time + FlightScheduler.RECHARGE_TICKS;
	}

	public int points() {
		return xs.length;
	}

	public int stops() {
		return xs.length - 3;
	}

	public double x(int i) {
		return xs[i];
	}

	public double y(int i) {
		return ys[i];
	}

	public double z(int i) {
		return zs[i];
	}

	public double cruise(int leg) {
		return cruise[leg];
	}

	public int speed() {
		return speed;
	}

	public double @org.jspecify.annotations.Nullable [] exitOut() {
		return approach(0);
	}

	public double @org.jspecify.annotations.Nullable [] exitIn() {
		return approach(xs.length - 1);
	}

	/** Where the drone climbs and comes down for point {@code i}, or null (straight at the point). */
	public double @org.jspecify.annotations.Nullable [] approach(int i) {
		return approach[i] == null ? null : approach[i].clone();
	}

	/** {sx, sz, ex, ez, pre, post}: where leg {@code i} climbs and comes down, and its level parts before and after. */
	private double[] geometry(int i) {
		double sx = xs[i], sz = zs[i], ex = xs[i + 1], ez = zs[i + 1], pre = 0, post = 0;
		double[] out = approach[i];
		double[] in = approach[i + 1];
		if (out != null) {
			pre = Math.hypot(out[0] - sx, out[1] - sz);
			sx = out[0];
			sz = out[1];
		}
		if (in != null) {
			post = Math.hypot(ex - in[0], ez - in[1]);
			ex = in[0];
			ez = in[1];
		}
		return new double[] {sx, sz, ex, ez, pre, post};
	}

	/** Whole flight in ticks, including the recharge; equals the scheduler's total / FULL_RATE. */
	public long totalTicks() {
		return total;
	}

	/** Length in blocks of leg {@code i} (point i to point i + 1). */
	public double legLength(int i) {
		double[] g = geometry(i);
		double dx = g[2] - g[0];
		double dz = g[3] - g[1];
		return g[4] + Math.sqrt(dx * dx + dz * dz) + (cruise[i] - ys[i]) + (cruise[i] - ys[i + 1]) + g[5];
	}

	/** All leg lengths, for {@link FlightScheduler#launch}. */
	public double[] legLengths() {
		double[] lengths = new double[cruise.length];
		for (int i = 0; i < lengths.length; i++) {
			lengths[i] = legLength(i);
		}
		return lengths;
	}

	/** Tick the drone arrives over the pickup. */
	public long pickupArrive() {
		return legEnd[0];
	}

	/** Tick the drone leaves the pickup with its crate. */
	public long pickupLeave() {
		return legEnd[0] + FlightScheduler.PICKUP_TICKS;
	}

	/** Tick the drone arrives over the last stop (the crate is used up there). */
	public long lastDrop() {
		return legEnd[cruise.length - 2];
	}

	/** Where the drone is at {@code t} ticks into the flight. */
	public Pose poseAt(double t) {
		int n = cruise.length;
		double yaw = yawOf(0);
		for (int i = 0; i < n; i++) {
			yaw = yawOf(i);
			if (t < legStart[i]) {
				// Waiting at point i (pickup or a stop) before this leg.
				return new Pose(xs[i], ys[i], zs[i], yawOf(i - 1), cable(t), 0);
			}
			if (t <= legEnd[i]) {
				double f = legEnd[i] == legStart[i] ? 1 : (t - legStart[i]) / (legEnd[i] - legStart[i]);
				return along(i, f, yaw, t);
			}
		}
		return new Pose(xs[n], ys[n], zs[n], yaw, -1, 0);
	}

	/** A point on leg {@code i}, {@code f} of the way along (by distance). */
	private Pose along(int i, double f, double yaw, double t) {
		double[] g = geometry(i);
		double sx = g[0], sz = g[1], ex = g[2], ez = g[3], pre = g[4], post = g[5];
		double up = cruise[i] - ys[i];
		double down = cruise[i] - ys[i + 1];
		double dx = ex - sx;
		double dz = ez - sz;
		double across = Math.sqrt(dx * dx + dz * dz);
		double length = pre + up + across + down + post;
		double d = f * length;
		if (length <= 0) {
			return new Pose(xs[i + 1], ys[i + 1], zs[i + 1], yaw, cable(t), 0);
		}
		if (d < pre) {
			// Flying out of the hangar, level, nose first.
			double h = d / pre;
			double outYaw = Math.atan2(sx - xs[i], sz - zs[i]);
			return new Pose(xs[i] + (sx - xs[i]) * h, ys[i], zs[i] + (sz - zs[i]) * h, outYaw, cable(t), 0);
		}
		d -= pre;
		if (d < up) {
			return new Pose(sx, ys[i] + d, sz, yaw, cable(t), 0);
		}
		if (d < up + across) {
			double h = (d - up) / across;
			// Ease into and out of forward flight over the first and last 2 blocks of the level part.
			double level = Math.min(1, Math.min(d - up, up + across - d) / 2);
			return new Pose(sx + dx * h, cruise[i], sz + dz * h, yaw, cable(t), level);
		}
		if (d < up + across + down) {
			return new Pose(ex, cruise[i] - (d - up - across), ez, yaw, cable(t), 0);
		}
		// Flying back into the hangar, level.
		double h = post <= 0 ? 1 : (d - up - across - down) / post;
		double inYaw = Math.atan2(xs[i + 1] - ex, zs[i + 1] - ez);
		return new Pose(ex + (xs[i + 1] - ex) * h, ys[i + 1], ez + (zs[i + 1] - ez) * h, inYaw, cable(t), 0);
	}

	/** Cable length below the drone at {@code t}, or -1 when it carries nothing. */
	private double cable(double t) {
		if (t < pickupArrive() || t >= lastDrop()) {
			return -1;
		}
		if (t < pickupLeave()) {
			double f = (t - pickupArrive()) / FlightScheduler.PICKUP_TICKS;
			return PICKUP_HOVER + (CARRY_CABLE - PICKUP_HOVER) * f;
		}
		return CARRY_CABLE;
	}

	private double yawOf(int leg) {
		if (leg < 0) {
			return 0;
		}
		double[] g = geometry(leg);
		double dx = g[2] - g[0];
		double dz = g[3] - g[1];
		return dx == 0 && dz == 0 ? 0 : Math.atan2(dx, dz);
	}

	/**
	 * A drone's position, heading (radians, 0 = south, towards +z), the length of the cable its crate
	 * hangs from (negative: no crate), and how far it is into forward flight (0 hovering or climbing, 1
	 * cruising level), which tilting designs use.
	 */
	public record Pose(double x, double y, double z, double yaw, double cable, double level) {
	}
}
