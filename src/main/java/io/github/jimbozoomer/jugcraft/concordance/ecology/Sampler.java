package io.github.jimbozoomer.jugcraft.concordance.ecology;

import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Reads the area round a plant, within fixed bounds, for the two factors a bed cannot know by itself: diversity (how
 * many different kinds grow in the {@code (2 * RADIUS + 1)} square at the plant's height, itself included) and
 * disturbance (the sum of what the blocks in that square, one above and one below, give off, capped at
 * {@value #MAX_DISTURBANCE}). One sample reads exactly {@value #READS} positions, whatever is there; a bed keeps its
 * sample for {@value #FRESH_TICKS} ticks, and a level takes at most {@link SampleBudget#PER_TICK} samples a tick.
 * Keep the numbers equal to tools/concordance_ecology.py.
 */
public final class Sampler {
	public static final int RADIUS = 2;
	public static final int HEIGHT = 1;
	public static final int MAX_DIVERSITY = 8;
	public static final int MAX_DISTURBANCE = 15;
	/** 25 positions for the kinds plus 75 for disturbance. */
	public static final int READS = (2 * RADIUS + 1) * (2 * RADIUS + 1) * (2 * HEIGHT + 2);
	public static final int FRESH_TICKS = 200;

	private Sampler() {
	}

	/** What the sampler may read, by offset from the plant. */
	public interface Probe {
		/** The kind of plant growing at {@code (dx, 0, dz)}, or null if none. */
		@Nullable String species(int dx, int dz);

		/** What the block at {@code (dx, dy, dz)} gives off (0 for almost everything). */
		int disturbance(int dx, int dy, int dz);
	}

	/** One sample of the area: its diversity and disturbance, and how many positions it read. */
	public record Area(int diversity, int disturbance, int reads) {
		public static final Area NONE = new Area(1, 0, 0);
	}

	public static Area sample(Probe probe) {
		Set<String> kinds = new HashSet<>();
		int disturbance = 0;
		int reads = 0;
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				String species = probe.species(dx, dz);
				reads++;
				if (species != null) {
					kinds.add(species);
				}
				for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
					disturbance += Math.max(0, probe.disturbance(dx, dy, dz));
					reads++;
				}
			}
		}
		return new Area(Math.clamp(kinds.size(), 1, MAX_DIVERSITY), Math.min(disturbance, MAX_DISTURBANCE), reads);
	}

	/** The habitat at a plant: the bed's moisture and nutrients, the light at the plant, and the area's sample. */
	public static Habitat habitat(int moisture, int light, int nutrients, Area area) {
		return new Habitat(Math.clamp(moisture, 0, Factor.MOISTURE.max), Math.clamp(light, 0, Factor.LIGHT.max),
				Math.clamp(nutrients, 0, Habitat.MAX_NUTRIENTS), area.diversity(), area.disturbance());
	}
}
