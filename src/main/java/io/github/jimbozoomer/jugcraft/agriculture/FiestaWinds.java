package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The winds aloft a hot-air balloon and a pibal drift on (fall addition 29). From sea level up the sky is
 * {@value #LAYERS} layers, each {@value #LAYER} blocks deep, and each has its own direction and speed for the day:
 * {@value #BASE} blocks a tick at the bottom and {@value #PER_LAYER} more for each layer up, a fifth either way by the
 * day. The lowest two blow roughly opposite ways (within {@value #BOX_SPREAD} degrees), so a pilot can go out low and
 * come home higher: the fiesta's "box". Through the day each layer swings {@value #SWAY} degrees either way. In rain
 * they blow {@value #STORM} times as hard.
 *
 * <p>The winds come from the world's seed and the day, so everything aloft in a world shares them, and a pibal shows
 * a pilot what their balloon will meet. They change only at layer boundaries (blending over the top {@value #BLEND}
 * of a layer) and through the day, never by chance from one tick to the next.
 */
public final class FiestaWinds {
	public static final int LAYER = 16;
	public static final int LAYERS = 8;
	public static final double BASE = 0.05;
	public static final double PER_LAYER = 0.015;
	public static final double BOX_SPREAD = 30.0;
	public static final double SWAY = 30.0;
	public static final double STORM = 1.5;
	public static final double BLEND = 0.3;
	public static final long DAY = 24000L;

	private FiestaWinds() {
	}

	/** A number from 0 to 1, the same every time for the same seed, day and salt. */
	static double unit(long seed, long day, int salt) {
		long z = seed + day * 0x9E3779B97F4A7C15L + (salt + 1) * 0xD1B54A32D192ED03L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		z ^= z >>> 31;
		return (z >>> 11) * 0x1.0p-53;
	}

	/** Which layer height {@code y} is in, from 0 at sea level to the top one. */
	public static int layer(int seaLevel, double y) {
		return Mth.clamp(Mth.floor((y - seaLevel) / LAYER), 0, LAYERS - 1);
	}

	/** The way layer {@code layer} blows on day {@code day} at sunrise, in degrees (0 towards east, 90 towards south). */
	public static double heading(long seed, long day, int layer) {
		double ground = unit(seed, day, 0) * 360.0;
		if (layer == 0) {
			return ground;
		}
		if (layer == 1) {
			return ground + 180.0 + (unit(seed, day, 1) * 2.0 - 1.0) * BOX_SPREAD;
		}
		return unit(seed, day, layer + 1) * 360.0;
	}

	/** How hard layer {@code layer} blows on day {@code day}, in blocks a tick. */
	public static double speed(long seed, long day, int layer) {
		return (BASE + PER_LAYER * layer) * (0.8 + 0.4 * unit(seed, day, 100 + layer));
	}

	/** Layer {@code layer}'s wind at {@code gameTime}: its heading for the day, swung by the time of day. */
	public static Vec3 layerWind(long seed, long gameTime, int layer, boolean storm) {
		long day = Math.floorDiv(gameTime, DAY);
		double swing = SWAY * Math.sin(Math.floorMod(gameTime, DAY) / (double) DAY * Math.PI * 2.0 + layer);
		double angle = Math.toRadians(heading(seed, day, layer) + swing);
		double speed = speed(seed, day, layer) * (storm ? STORM : 1.0);
		return new Vec3(Math.cos(angle) * speed, 0.0, Math.sin(angle) * speed);
	}

	/** The wind at height {@code y}: its layer's, blending into the next layer's over the top of this one. */
	public static Vec3 at(long seed, int seaLevel, long gameTime, double y, boolean storm) {
		double f = Mth.clamp((y - seaLevel) / LAYER, 0.0, LAYERS - 1.0);
		int low = Mth.floor(f);
		Vec3 wind = layerWind(seed, gameTime, low, storm);
		double blend = (f - low - (1.0 - BLEND)) / BLEND;
		if (low + 1 < LAYERS && blend > 0.0) {
			double t = blend * blend * (3.0 - 2.0 * blend);
			wind = wind.lerp(layerWind(seed, gameTime, low + 1, storm), t);
		}
		return wind;
	}

	public static Vec3 at(ServerLevel level, double y) {
		return at(level.getSeed(), level.getSeaLevel(), level.getGameTime(), y, level.isRaining());
	}

	/** A wind's compass point, as a pilot reads it: where it blows towards. */
	public static String compass(Vec3 wind) {
		String[] points = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};
		double degrees = Math.toDegrees(Math.atan2(wind.z, wind.x));
		return points[Math.floorMod(Math.round(degrees / 45.0), 8)];
	}
}
