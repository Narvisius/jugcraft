package io.github.jimbozoomer.jugcraft.concordance.ecology;

import org.jspecify.annotations.Nullable;

/**
 * A living thing a Greenwarden grows (roadmap step 14; data: {@code data/<ns>/concordance/organism/}). Two roles:
 * <ul>
 * <li>A <b>crop</b> grows on a Verdant Bed through {@link #STAGES} steps, each costing {@link #cost} nutrients from the
 * bed (or, for a nitrogen fixer, giving {@link #fix} to the beds round it) and drying the bed by one. It grows only
 * on random ticks (vanilla's, or a sprinkler's), only while its {@link #niche} is met, and twice as slowly while a
 * factor is merely tolerable. Mature, it is harvested for {@link #produce} of its item and {@link #chaff} Verdant
 * Chaff, and falls back to stage {@link #replant} to grow again.</li>
 * <li>A <b>producer</b> (the Verdant Heart) beats instead of growing: each beat in its niche spends {@link #cost}
 * nutrients and makes {@link #thriving} Verdance (or {@link #tolerating} while a factor is merely tolerable).</li>
 * </ul>
 * {@link #growth} scales a crop's chance per random tick as vanilla's growth time does (1.0 is wheat's pace for one
 * step).
 */
public record Organism(String id, Role role, String block, @Nullable String item, double growth, int cost, int fix,
		int replant, int produce, int chaff, int thriving, int tolerating, Niche niche) {
	/** A crop's steps from planted (age 0) to mature. The crop block's age property; keep equal to the generator. */
	public static final int STAGES = 3;

	public enum Role {
		CROP("crop"),
		PRODUCER("producer");

		public final String id;

		Role(String id) {
			this.id = id;
		}
	}

	public boolean crop() {
		return role == Role.CROP;
	}
}
