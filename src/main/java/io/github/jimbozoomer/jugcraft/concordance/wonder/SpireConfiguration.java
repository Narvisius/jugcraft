package io.github.jimbozoomer.jugcraft.concordance.wonder;

import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import java.util.List;

/**
 * One way to raise a wonder (roadmap step 25, data: concordance/wonder_configuration): which tradition it belongs to and
 * the research its founder needs, the practice the keeper's side keeps carrying through (the phases' demonstrations and
 * the spire's attendance), the crown structure that tops it, its daily upkeep (an item delivered to the heart and Ley
 * Charge drawn from its pylons) and what its field does, how far and to how many things a pulse.
 */
public record SpireConfiguration(String id, String wonder, String tradition, List<Definitions.Requirement> requires, String practice,
		String crown, String upkeepItem, int upkeepCount, int upkeepLey, FieldKind field, int radius, int count) {
	public SpireConfiguration {
		requires = List.copyOf(requires);
	}
}
