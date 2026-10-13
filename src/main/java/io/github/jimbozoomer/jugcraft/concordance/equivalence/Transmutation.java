package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One declared conversion between materials (roadmap step 21, data: concordance/transmutation): what it takes, what it
 * makes, the containers it gives back, its byproducts, and its catalysts (needed, never consumed). A recipe moves value
 * and must never add any; a source is a declared external input (a cobblestone generator, a farm), which the cycle audit
 * keeps apart. {@code via} names how it happens (crafting, smelting, breaking, farming...); a crafting recipe gives its
 * grid ({@code pattern} and {@code key}) so tests can check it against the game's own recipes.
 */
public record Transmutation(String id, Kind kind, String via, Map<String, Integer> inputs, Map<String, Integer> outputs,
		Map<String, Integer> returns, Map<String, Integer> byproducts, Map<String, Integer> catalysts, List<String> pattern,
		Map<String, String> key) {
	public enum Kind {
		RECIPE("recipe"),
		SOURCE("source");

		public final String id;

		Kind(String id) {
			this.id = id;
		}
	}

	/** Everything it gives: its outputs, the containers it returns and its byproducts. */
	public Map<String, Integer> gives() {
		Map<String, Integer> all = new LinkedHashMap<>(outputs);
		returns.forEach((item, count) -> all.merge(item, count, Integer::sum));
		byproducts.forEach((item, count) -> all.merge(item, count, Integer::sum));
		return all;
	}
}
