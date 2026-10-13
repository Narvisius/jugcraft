package io.github.jimbozoomer.jugcraft.concordance.wonder;

import java.util.List;

/**
 * A wonder players raise together over time (roadmap step 25, data: concordance/wonder): the Concord Spire. Its heart
 * block, the Concordance stage its founder needs, the ritual whose completion near the heart is its rite, how far from
 * the heart that ritual may be, how many days a keeper may go without the configuration's practice before the spire
 * stops answering (attendance), how often its field pulses, and its phases in order.
 */
public record SpireDefinition(String id, String heart, String stage, String rite, int riteRange, int attendanceDays, int pulseTicks,
		List<Phase> phases) {
	/** A phase's structure that is the configuration's crown rather than a fixed one. */
	public static final String CROWN = "crown";

	/**
	 * One phase: the structure that must stand around the heart once it is finished ("" none; {@link #CROWN} the
	 * configuration's crown), how many of the configuration's practices the keeper's side must carry through after it
	 * began, whether it needs the rite, and how many days of upkeep in a row it needs held.
	 */
	public record Phase(String id, String structure, int practices, boolean rite, int sustain) {
	}

	public SpireDefinition {
		phases = List.copyOf(phases);
	}

	/** Every structure that stands once phase {@code index} is finished, in order (the crown as {@link #CROWN}). */
	public List<String> structuresThrough(int index) {
		return phases.subList(0, Math.min(index + 1, phases.size())).stream().map(Phase::structure).filter(structure -> !structure.isEmpty())
				.toList();
	}
}
