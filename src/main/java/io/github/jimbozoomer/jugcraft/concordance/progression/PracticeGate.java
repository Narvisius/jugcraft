package io.github.jimbozoomer.jugcraft.concordance.progression;

import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import java.util.List;

/**
 * What makes a practice or a milestone possible at all (roadmap step 24, data: concordance/practice): the research states
 * a player needs before the deed can happen (a ritual needs Circle Lore understood, an observation the Celestial
 * Attunement), the stage it needs reached first ("" none: roadmap step 25's raised Concord Spire needs the Master stage),
 * and the tradition it belongs to ("" none). A mastery or a stage route that asks for it therefore needs what it needs.
 */
public record PracticeGate(String activity, String tradition, List<Definitions.Requirement> requires, String stage) {
	public PracticeGate {
		requires = List.copyOf(requires);
	}
}
