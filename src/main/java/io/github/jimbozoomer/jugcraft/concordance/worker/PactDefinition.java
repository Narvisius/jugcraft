package io.github.jimbozoomer.jugcraft.concordance.worker;

import java.util.List;

/**
 * Concordance Chunk D, D-04 (D-S13, D-S15, D-S23): a pact's offer, as data (data/<ns>/concordance/pact/<name>.json, read
 * by {@link PactParser}): its agreement id, the research that must be understood, its service (work, area round its
 * perch, hours, daily quota), its upkeep (an offering tag, a count every period, the grace, the most periods kept ahead),
 * who benefits, the Open Hand Compact (ordinary materials and Astral Resonance) and the names of its dialogue beats. Its
 * counterpart is its id, the spirit's entity id. Sealing copies the service, upkeep and beneficiaries onto the Bound Will
 * (BoundWill.Pact), so a sealed pact keeps its terms whatever a reload does to this.
 */
public record PactDefinition(String id, String agreement, String research, String work, int radius, int from, int to, int quota,
		String offering, int count, long period, long grace, int cap, String beneficiaries, List<Cost> compact, int resonance,
		List<String> beats) {
	/** Stolas, the pact hall's first spirit (D-S7): the pact perch offers him. StolasEntity.COUNTERPART is this. */
	public static final String STOLAS = "jugcraft:stolas";

	/** One material of the compact: an item id and how many. */
	public record Cost(String item, int count) {
	}

	public PactDefinition {
		compact = List.copyOf(compact);
		beats = List.copyOf(beats);
	}
}
