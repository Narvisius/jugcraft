package io.github.jimbozoomer.jugcraft.concordance.ecology;

/**
 * One reading of a habitat: the five {@link Factor}s at one plant. Moisture and nutrients are the bed's own, light is
 * read at the plant, and diversity and disturbance come from the area round it ({@link Sampler}).
 */
public record Habitat(int moisture, int light, int nutrients, int diversity, int disturbance) {
	/** The most nutrients a bed holds. Keep equal to tools/concordance_ecology.py. */
	public static final int MAX_NUTRIENTS = 32;

	public Habitat {
		check(Factor.MOISTURE, moisture);
		check(Factor.LIGHT, light);
		check(Factor.NUTRIENTS, nutrients);
		check(Factor.DIVERSITY, diversity);
		check(Factor.DISTURBANCE, disturbance);
	}

	private static void check(Factor factor, int value) {
		if (value < 0 || value > factor.max) {
			throw new IllegalArgumentException(factor.id + " " + value + " outside 0.." + factor.max);
		}
	}

	public int get(Factor factor) {
		return switch (factor) {
			case MOISTURE -> moisture;
			case LIGHT -> light;
			case NUTRIENTS -> nutrients;
			case DIVERSITY -> diversity;
			case DISTURBANCE -> disturbance;
		};
	}

	/** This reading with one factor changed (clamped to its range). */
	public Habitat with(Factor factor, int value) {
		int v = Math.clamp(value, 0, factor.max);
		return new Habitat(factor == Factor.MOISTURE ? v : moisture, factor == Factor.LIGHT ? v : light,
				factor == Factor.NUTRIENTS ? v : nutrients, factor == Factor.DIVERSITY ? v : diversity,
				factor == Factor.DISTURBANCE ? v : disturbance);
	}
}
