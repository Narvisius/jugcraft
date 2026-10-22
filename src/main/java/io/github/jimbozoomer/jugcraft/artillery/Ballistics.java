package io.github.jimbozoomer.jugcraft.artillery;

import org.jspecify.annotations.Nullable;

/**
 * Works out the elevation that lands a shell on a target, by stepping a shell's flight exactly as a thrown projectile
 * moves each tick (move by its speed, lose 1% to drag, then fall by its gravity) and searching between the gun's
 * elevation limits. High arcs (mortars) search down from the highest elevation;
 * direct fire searches upward from the lowest, including steep shots at aircraft.
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
        if (distance <= 0 || speed <= 0 || gravity < 0 || !Double.isFinite(distance)
                || !Double.isFinite(rise) || !Double.isFinite(speed) || !Double.isFinite(gravity)) return null;
        float low = high ? Math.max(45F, minPitch) : minPitch;
        float upper = maxPitch;
        if (low >= upper) return null;
        // Test height at the target's horizontal distance, not only the descending
        // crossing of its height. The latter missed nearby elevated blimps entirely.
        float previous = high ? upper : low;
        double before = heightAtDistance(speed, gravity, previous, distance) - rise;
        if (Double.isFinite(before) && Math.abs(before) < .001) return previous;
        int steps = (int)Math.ceil((upper-low)*2);
        for (int step=1; step<=steps; step++) {
            float angle = high ? Math.max(low, upper-step*.5F) : Math.min(upper, low+step*.5F);
            double after = heightAtDistance(speed, gravity, angle, distance) - rise;
            if (Double.isFinite(after) && Math.abs(after)<.001) return angle;
            // Only the first crossing from below the target belongs to this
            // branch. Do not silently substitute a lob for an unavailable low shot.
            if (before < 0 && after >= 0) {
                float a=previous,b=angle; double fa=before;
                for(int n=0;n<28;n++) {
                    float mid=(a+b)*.5F;
                    double fm=heightAtDistance(speed,gravity,mid,distance)-rise;
                    if ((fa<0)==(fm<0)) {a=mid;fa=fm;} else b=mid;
                }
                float result=(a+b)*.5F;
                if (Math.abs(heightAtDistance(speed,gravity,result,distance)-rise)<.1) return result;
            }
            previous=angle;before=after;
        }
        return null;
    }

    /** First horizontal crossing; negative infinity means drag prevents reaching the point. */
    public static double heightAtDistance(double speed,double gravity,float pitch,double distance) {
        double rad=Math.toRadians(pitch),vx=speed*Math.cos(rad),vy=speed*Math.sin(rad),x=0,y=0;
        for(int tick=0;tick<MAX_TICKS;tick++) {
            if(x+vx>=distance) return y+vy*(distance-x)/vx;
            x+=vx;y+=vy;vx*=DRAG;vy=vy*DRAG-gravity;
            if(vx<1e-6) break;
        }
        return Double.NEGATIVE_INFINITY;
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
