package io.github.jimbozoomer.jugcraft.concordance.worker;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** Concordance Chunk D, D-04 (D-S23): the pacts' offers in force, and why any did not load. Immutable; a reload swaps it whole. */
public final class PactCatalog {
	public static final PactCatalog EMPTY = new PactCatalog(Map.of(), List.of());

	private final Map<String, PactDefinition> pacts;
	private final List<String> problems;

	public PactCatalog(Map<String, PactDefinition> pacts, List<String> problems) {
		this.pacts = Map.copyOf(pacts);
		this.problems = List.copyOf(problems);
	}

	public @Nullable PactDefinition get(String id) {
		return pacts.get(id);
	}

	public Map<String, PactDefinition> pacts() {
		return pacts;
	}

	public List<String> problems() {
		return problems;
	}

	/**
	 * Reads every pact file ({@code id -> json}), drops one naming research that does not exist, and reports a pact the game
	 * offers (PactParser.REQUIRED) that did not load, unless no research loaded at all (empty rules).
	 */
	public static PactCatalog parse(Map<String, JsonElement> files, Set<String> research) {
		PactParser parser = new PactParser();
		Map<String, PactDefinition> pacts = new TreeMap<>();
		new TreeMap<>(files).forEach((id, json) -> {
			PactDefinition pact = parser.pact(id, json);
			if (pact != null) {
				pacts.put(pact.id(), pact);
			}
		});
		List<String> problems = new ArrayList<>(parser.problems());
		pacts.values().removeIf(pact -> {
			boolean missing = !research.contains(pact.research());
			if (missing) {
				problems.add("pact " + pact.id() + ": research " + pact.research() + " does not exist");
			}
			return missing;
		});
		if (!research.isEmpty()) {
			for (String required : PactParser.REQUIRED) {
				if (!pacts.containsKey(required)) {
					problems.add("pact " + required + ": no definition loaded; the pact perch refuses new invitations until one does");
				}
			}
		}
		return new PactCatalog(pacts, problems);
	}
}
