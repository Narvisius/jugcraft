package io.github.jimbozoomer.jugcraft.concordance.ecology;

/**
 * One reading of a habitat: the five {@link Factor}s at one plant, and the strain its bed's harvests left. Moisture and
 * nutrients are the bed's own, light is read at the plant, and diversity and disturbance come from the area round it
 * ({@link Sampler}). The strain (Concordance Chunk C, C-08, C-P17, C-P59) is not a sixth factor: {@link Verdict#of} counts it
 * against a plant's disturbance limit only ({@link #strained}), never as disturbance a plant needs.
 */
public record Habitat(int moisture, int light, int nutrients, int diversity, int disturbance, int strain) {
	/** The most nutrients a bed holds. Keep equal to tools/concordance_ecology.py. */
	public static final int MAX_NUTRIENTS = 32;
	/** The most strain harvests leave on a bed (C-P17). Keep equal to tools/concordance_ecology.py. */
	public static final int MAX_STRAIN = 3;

	public Habitat {
		check(Factor.MOISTURE, moisture);
		check(Factor.LIGHT, light);
		check(Factor.NUTRIENTS, nutrients);
		check(Factor.DIVERSITY, diversity);
		check(Factor.DISTURBANCE, disturbance);
		if (strain < 0 || strain > MAX_STRAIN) {
			throw new IllegalArgumentException("strain " + strain + " outside 0.." + MAX_STRAIN);
		}
	}

	/** A reading of a bed no harvest has strained. */
	public Habitat(int moisture, int light, int nutrients, int diversity, int disturbance) {
		this(moisture, light, nutrients, diversity, disturbance, 0);
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

	/** The disturbance with the strain counted against the plant, within the factor's range. */
	public int strained() {
		return Math.min(disturbance + strain, Factor.DISTURBANCE.max);
	}

	/** This reading with one factor changed (clamped to its range); the strain stays. */
	public Habitat with(Factor factor, int value) {
		int v = Math.clamp(value, 0, factor.max);
		return new Habitat(factor == Factor.MOISTURE ? v : moisture, factor == Factor.LIGHT ? v : light,
				factor == Factor.NUTRIENTS ? v : nutrients, factor == Factor.DIVERSITY ? v : diversity,
				factor == Factor.DISTURBANCE ? v : disturbance, strain);
	}
}
