package io.github.jimbozoomer.jugcraft.season;

/**
 * The seasonal colour maths, kept free of client classes so server game tests can check it; the client applies it
 * to grass and foliage tints ({@code client/SeasonColors}). Keyframes through the year say how far each tint moves
 * from its vanilla colour towards a seasonal one; between keyframes the result blends smoothly. Summer (day 196) is
 * vanilla. Autumn foliage mixes gold, orange and red in patches about 12 blocks across, with smaller speckles, so
 * neighbouring trees turn different colours rather than one flat one.
 */
public final class SeasonPalette {
	/**
	 * One point in the year: the foliage colour aimed for and how far towards it (0 vanilla, 1 fully), how much of
	 * the patchy autumn hue it mixes into that colour (0 none, 1 all), then grass the same way.
	 */
	public record Keyframe(int day, int foliage, float foliageStrength, float variety, int grass, float grassStrength) {
	}

	/** In day order; the year wraps from the last back to the first. */
	public static final Keyframe[] YEAR = {
			new Keyframe(15, 0x86704c, 0.70F, 0.0F, 0x9c9472, 0.60F), // midwinter: dormant, grey-brown
			new Keyframe(75, 0x7c8a40, 0.40F, 0.0F, 0x94a462, 0.35F), // mid-March: first green
			new Keyframe(120, 0x80e23c, 0.45F, 0.0F, 0x86de52, 0.40F), // spring: fresh, bright green
			new Keyframe(196, 0x000000, 0.0F, 0.0F, 0x000000, 0.0F), // midsummer: vanilla
			new Keyframe(244, 0xc8b43a, 0.45F, 0.3F, 0xb0b04a, 0.25F), // September: first yellowing
			new Keyframe(293, 0xe07020, 0.95F, 1.0F, 0xc0a24c, 0.50F), // 20 October: peak autumn
			new Keyframe(330, 0x96542a, 0.85F, 0.4F, 0xa88e56, 0.55F), // late November: russet and bare
	};

	public static final int GOLD = 0xe8b028;
	public static final int ORANGE = 0xe86a1c;
	public static final int RED = 0xc8301e;
	/** Blocks across an autumn colour patch, and across the smaller speckles inside it. */
	public static final double PATCH = 12.0;
	public static final double SPECKLE = 5.0;

	private SeasonPalette() {
	}

	/**
	 * The seasonal tint at block (x, z) on season day {@code day} (1 to 365; 0 or less leaves {@code vanilla} as it
	 * is), for a foliage or a grass tint whose vanilla colour is {@code vanilla}. The vanilla alpha byte is kept.
	 */
	public static int colour(int day, int vanilla, boolean foliage, double x, double z) {
		if (day <= 0 || day > SeasonCalendar.DAYS) {
			return vanilla;
		}
		Keyframe from = YEAR[YEAR.length - 1];
		int fromDay = from.day() - SeasonCalendar.DAYS;
		Keyframe to = YEAR[0];
		int toDay = to.day();
		for (int i = 0; i < YEAR.length && YEAR[i].day() <= day; i++) {
			from = YEAR[i];
			fromDay = from.day();
			to = YEAR[(i + 1) % YEAR.length];
			toDay = i + 1 < YEAR.length ? to.day() : to.day() + SeasonCalendar.DAYS;
		}
		float t = (float) (day - fromDay) / (toDay - fromDay);
		t = t * t * (3.0F - 2.0F * t);
		return mix(at(from, vanilla, foliage, x, z), at(to, vanilla, foliage, x, z), t);
	}

	/** The tint a single keyframe gives. */
	private static int at(Keyframe key, int vanilla, boolean foliage, double x, double z) {
		if (!foliage) {
			return mix(vanilla, key.grass(), key.grassStrength());
		}
		int target = key.variety() > 0.0F ? mix(key.foliage(), autumnHue(x, z), key.variety()) : key.foliage();
		return mix(vanilla, target, key.foliageStrength());
	}

	/** Gold, orange or red (or between), in patches with speckles, the same at a position every time. */
	public static int autumnHue(double x, double z) {
		double n = 0.7 * valueNoise(x / PATCH, z / PATCH, 0) + 0.3 * valueNoise(x / SPECKLE, z / SPECKLE, 1);
		// Averaged noise bunches around one half; stretch it so all three colours show.
		n = Math.clamp((n - 0.5) * 1.8 + 0.5, 0.0, 1.0);
		return n < 0.45 ? mix(GOLD, ORANGE, (float) (n / 0.45)) : mix(ORANGE, RED, (float) ((n - 0.45) / 0.55));
	}

	/** {@code a} moved {@code t} (0 to 1) of the way towards {@code b}, per colour channel; {@code a}'s alpha is kept. */
	public static int mix(int a, int b, float t) {
		if (t <= 0.0F) {
			return a;
		}
		int rgb = 0;
		for (int shift = 0; shift <= 16; shift += 8) {
			int from = a >> shift & 0xFF;
			int to = b >> shift & 0xFF;
			rgb |= Math.round(from + (to - from) * Math.min(t, 1.0F)) << shift;
		}
		return a & 0xFF000000 | rgb;
	}

	private static double valueNoise(double x, double z, int salt) {
		int x0 = (int) Math.floor(x);
		int z0 = (int) Math.floor(z);
		double fx = smooth(x - x0);
		double fz = smooth(z - z0);
		double top = lerp(lattice(x0, z0, salt), lattice(x0 + 1, z0, salt), fx);
		double bottom = lerp(lattice(x0, z0 + 1, salt), lattice(x0 + 1, z0 + 1, salt), fx);
		return lerp(top, bottom, fz);
	}

	private static double smooth(double t) {
		return t * t * (3.0 - 2.0 * t);
	}

	private static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/** A fixed value in [0, 1) for each lattice point (a 64-bit mix of its coordinates). */
	private static double lattice(int x, int z, int salt) {
		long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53;
	}
}
