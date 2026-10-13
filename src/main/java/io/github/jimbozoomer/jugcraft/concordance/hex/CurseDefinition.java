package io.github.jimbozoomer.jugcraft.concordance.hex;

/**
 * A curse (roadmap step 22, data: concordance/curse): the reagent that casts it through a link, the status it gives
 * (amplifier, how long each pulse lasts, how often it pulses) for how long in all, the link strength and Focus casting
 * needs, and the remedy item that lifts it. Every curse is bounded: a status at most level II, pulses at most 30 s, at
 * most ten minutes in all.
 */
public record CurseDefinition(String id, String reagent, String status, int amplifier, int pulseDuration, int pulseTicks, int totalTicks,
		int strength, int focus, String remedy) {
}
