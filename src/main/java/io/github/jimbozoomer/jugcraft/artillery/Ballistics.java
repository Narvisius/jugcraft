package io.github.jimbozoomer.jugcraft.artillery;

import org.jspecify.annotations.Nullable;

/**
 * Works out the elevation that lands a shell on a target, by stepping a shell's flight exactly as a thrown projectile
 * moves each tick (move by its speed, lose 1% to drag, then fall by its gravity) and searching between the gun's
 * elevation limits. High arcs (mortars) search 45 to 85 degrees, low arcs up to 45.
 */
public final class Ballistics {
	private static final double DRAG = 0.99;
	private static final int MAX_TICKS = 1200;

	private Ballistics() {
	}

	/**
	 * The elevation in degrees (up is positive) that lands a shell fired at {@code speed} with {@code gravity} a
	 * horizontal distance {@code distance} away and {@code rise} blocks higher (negative: lower), or null if it is out of
	 * reach. {@code high} picks the high arc.
	 */
	public static @Nullable Float solve(double speed, double gravity, double distance, double rise, boolean high,
			float minPitch, float maxPitch) {
		float lo = high ? Math.max(45.0F, minPitch) : minPitch;
		float hi = high ? maxPitch : Math.min(45.0F, maxPitch);
		if (lo >= hi) {
			return null;
		}
		double reachLo = range(speed, gravity, lo, rise);
		double reachHi = range(speed, gravity, hi, rise);
		// On the low arc range grows with elevation; on the high arc it shrinks.
		double near = Math.min(reachLo, reachHi);
		double far = Math.max(reachLo, reachHi);
		if (distance < near - 0.5 || distance > far + 0.5) {
			return null;
		}
		for (int i = 0; i < 40; i++) {
			float mid = (lo + hi) / 2;
			double reach = range(speed, gravity, mid, rise);
			boolean tooFar = reach > distance;
			// Low arc: too far means aim lower; high arc: too far means aim higher.
			if (tooFar != high) {
				hi = mid;
			} else {
				lo = mid;
			}
		}
		return (lo + hi) / 2;
	}

	/** How far a shell travels horizontally before falling back through {@code rise}; 0 if it never gets there. */
	public static double range(double speed, double gravity, float pitch, double rise) {
		double rad = Math.toRadians(pitch);
		double vx = speed * Math.cos(rad);
		double vy = speed * Math.sin(rad);
		double x = 0;
		double y = 0;
		for (int tick = 0; tick < MAX_TICKS; tick++) {
			double nx = x + vx;
			double ny = y + vy;
			if (vy < 0 && ny <= rise) {
				double t = (y - rise) / (y - ny);
				return x + (nx - x) * t;
			}
			x = nx;
			y = ny;
			vx *= DRAG;
			vy = vy * DRAG - gravity;
		}
		return 0;
	}
}
