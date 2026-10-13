package io.github.jimbozoomer.jugcraft.concordance.progression;

import java.util.List;

/**
 * A stage of the Concordance (roadmap step 24, data: concordance/stage): Initiate, Practitioner, Adept, Master and
 * Architect, in {@code order}. A stage is reached by any one of its routes, once every earlier stage is reached.
 */
public record StageDefinition(String id, int order, List<Route> routes) {
	public StageDefinition {
		routes = List.copyOf(routes);
	}
}
