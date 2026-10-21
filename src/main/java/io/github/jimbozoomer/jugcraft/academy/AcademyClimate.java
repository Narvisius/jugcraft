package io.github.jimbozoomer.jugcraft.academy;

/** Pure geometry matching winterhold_layout.json. No weather/biome mutation or chunk access. */
public final class AcademyClimate {
	private AcademyClimate() {}
	public static double influence(double x, double y, double z) {
		double distance = Math.hypot(x, z)-36;
		distance = Math.min(distance, Math.hypot(x, z+52)-23);
		distance = Math.min(distance, Math.hypot(x-44, z+51)-22);
		distance = Math.min(distance, Math.hypot(x+41, z+9)-14);
		distance = Math.min(distance, Math.hypot(x-41, z-7)-14);
		// Bridge corridor, south of the court; avoid a giant circular aura over the valley.
		distance = Math.min(distance, Math.hypot(Math.max(Math.abs(x)-5, 0), Math.max(Math.max(34-z, z-75), 0)));
		double horizontal = fade(distance, 16);
		double vertical = fade(Math.max(Math.max(-3-y, y-55), 0), 12);
		return horizontal * vertical;
	}
	private static double fade(double distance, double width) {
		double t = Math.clamp(distance / width, 0, 1);
		return 1 - t*t*(3-2*t);
	}
}
