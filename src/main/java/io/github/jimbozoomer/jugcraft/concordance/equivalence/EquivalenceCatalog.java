package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The catalogued materials, by item, and the declared conversion graph, by id (roadmap step 21), with the problems the
 * whole-catalogue check found ({@link EquivalenceParser#check}). A catalogue with problems is out of balance: the scale
 * weighs nothing until they are fixed.
 */
public final class EquivalenceCatalog {
	public static final EquivalenceCatalog EMPTY = new EquivalenceCatalog(Map.of(), Map.of(), List.of());

	private final Map<String, Material> materials;
	private final Map<String, Transmutation> transmutations;
	private final List<String> problems;

	public EquivalenceCatalog(Map<String, Material> materials, Map<String, Transmutation> transmutations, List<String> problems) {
		this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
		this.transmutations = Collections.unmodifiableMap(new LinkedHashMap<>(transmutations));
		this.problems = List.copyOf(problems);
	}

	public List<String> problems() {
		return problems;
	}

	/** Materials by item id. */
	public Map<String, Material> materials() {
		return materials;
	}

	public Map<String, Transmutation> transmutations() {
		return transmutations;
	}

	public @Nullable Material material(String item) {
		return materials.get(item);
	}

	/** The exact value of one {@code item}, or null if it has none. */
	public @Nullable Exact value(String item) {
		if (Assay.PRIMA.equals(item)) {
			return Exact.ONE;
		}
		Material material = materials.get(item);
		return material == null ? null : material.exact();
	}
}
