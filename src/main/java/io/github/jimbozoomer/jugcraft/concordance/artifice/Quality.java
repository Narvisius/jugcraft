package io.github.jimbozoomer.jugcraft.concordance.artifice;

import java.util.SplittableRandom;
import org.jspecify.annotations.Nullable;

/**
 * Craft quality (roadmap step 19), rolled once when an artifice is forged and kept by everything but salvage: it moves
 * the substrate's capacity and sets how many affixes a roll may give. Weights out of 100.
 */
public enum Quality {
	CRUDE("crude", -1, 1, 15),
	SOUND("sound", 0, 1, 55),
	FINE("fine", 1, 2, 24),
	MASTERWORK("masterwork", 2, 3, 6);

	public final String id;
	/** Capacity added to the substrate's. */
	public final int bonus;
	/** The most affixes a roll gives. */
	public final int affixes;
	public final int weight;

	Quality(String id, int bonus, int affixes, int weight) {
		this.id = id;
		this.bonus = bonus;
		this.affixes = affixes;
		this.weight = weight;
	}

	static Quality roll(SplittableRandom random) {
		int pick = random.nextInt(100);
		for (Quality quality : values()) {
			pick -= quality.weight;
			if (pick < 0) {
				return quality;
			}
		}
		return SOUND;
	}

	public static @Nullable Quality fromId(String id) {
		for (Quality quality : values()) {
			if (quality.id.equals(id)) {
				return quality;
			}
		}
		return null;
	}
}
