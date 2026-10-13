package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The conversions in force. Built from all loaded recipes in id order: each recipe is added only if, with those
 * already accepted, no loop of conversions returns at least what went in (a ratio product of 1 or more around a
 * cycle). Such a loop would make a resource from nothing, so the recipe that would close it is refused with a reason.
 */
public final class ConversionTable {
	public static final ConversionTable EMPTY = new ConversionTable(Map.of(), List.of());
	/** Products this close to 1 count as breaking even (and so are refused). */
	private static final double EPSILON = 1e-9;

	private final Map<String, Conversion> conversions;
	private final List<String> problems;

	private ConversionTable(Map<String, Conversion> conversions, List<String> problems) {
		this.conversions = conversions;
		this.problems = problems;
	}

	public Map<String, Conversion> conversions() {
		return conversions;
	}

	public List<String> problems() {
		return problems;
	}

	public @Nullable Conversion get(String id) {
		return conversions.get(id);
	}

	public static ConversionTable build(List<Conversion> candidates) {
		List<Conversion> sorted = new ArrayList<>(candidates);
		sorted.sort(Comparator.comparing(Conversion::id));
		Map<String, Conversion> accepted = new LinkedHashMap<>();
		List<String> problems = new ArrayList<>();
		for (Conversion conversion : sorted) {
			if (accepted.containsKey(conversion.id())) {
				problems.add("conversion " + conversion.id() + ": defined twice");
				continue;
			}
			accepted.put(conversion.id(), conversion);
			String loop = gainingLoop(accepted.values());
			if (loop != null) {
				accepted.remove(conversion.id());
				problems.add("conversion " + conversion.id() + ": would let " + loop + " convert round without loss");
			}
		}
		return new ConversionTable(Collections.unmodifiableMap(accepted), List.copyOf(problems));
	}

	/** A type that some loop of these conversions returns to with no loss, or null if every loop loses. */
	private static @Nullable String gainingLoop(Iterable<Conversion> conversions) {
		List<ResourceType> types = new ArrayList<>();
		for (Conversion conversion : conversions) {
			if (!types.contains(conversion.from())) {
				types.add(conversion.from());
			}
			if (!types.contains(conversion.to())) {
				types.add(conversion.to());
			}
		}
		int n = types.size();
		// best[i][j]: the most of j obtainable from one unit of i (0 when j cannot be reached).
		double[][] best = new double[n][n];
		for (Conversion conversion : conversions) {
			int i = types.indexOf(conversion.from());
			int j = types.indexOf(conversion.to());
			best[i][j] = Math.max(best[i][j], (double) conversion.toAmount() / conversion.fromAmount());
		}
		for (int k = 0; k < n; k++) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					best[i][j] = Math.max(best[i][j], best[i][k] * best[k][j]);
				}
			}
		}
		for (int i = 0; i < n; i++) {
			if (best[i][i] >= 1.0 - EPSILON) {
				return types.get(i).id();
			}
		}
		return null;
	}
}
