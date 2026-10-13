package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * The profitable-cycle diagnostic (roadmap step 21). Over the declared conversion graph plus the scale's own dissolving
 * and forming ({@link Assay#conversions}), it reports:
 * <ul>
 * <li>{@code unvalued}: an item a conversion takes or gives that has no value, so nothing about it can be certified;</li>
 * <li>{@code gains_value}: a recipe whose gives (outputs, returned containers, byproducts) are worth more than what it
 * takes, at exact values;</li>
 * <li>{@code cycle}: a closed path of recipes that returns at least as much of its first item as was fed in while
 * gaining value, found by searching every simple cycle up to {@value #MAX_LENGTH} steps.</li>
 * </ul>
 * Sources (declared external inputs) are kept apart: they may add value, and no cycle runs through them. If every item
 * has a positive value and no recipe gains value, no closed cycle of recipes can create value however it is run, since
 * a cycle's gain is the sum of its steps' gains; the cycle search is the second, independent look.
 */
public final class CycleAudit {
	public static final int MAX_LENGTH = 6;
	public static final int MAX_CYCLES = 20_000;

	/** One thing wrong with the graph: what kind, which conversion (or cycle), the exact gain, and the items involved. */
	public record Finding(String kind, String subject, Exact gain, List<String> items) {
		@Override
		public String toString() {
			return kind + " " + subject + (gain.signum() == 0 ? "" : " (gains " + gain + " grains)") + (items.isEmpty() ? "" : " " + items);
		}
	}

	/**
	 * A closed path: its items in order (the first is fed in and comes back), the conversions between them, how much of
	 * the first item comes back for one fed in, and the value gained on the way, counting every other input and output.
	 */
	public record Cycle(List<String> items, List<String> conversions, Exact yield, Exact gain) {
		public boolean profitable() {
			return yield.compareTo(Exact.ONE) >= 0 && gain.signum() > 0;
		}
	}

	private CycleAudit() {
	}

	/** The exact value a conversion's gives are worth beyond what it takes, or null if an item has no value. */
	public static @Nullable Exact gain(Transmutation conversion, EquivalenceCatalog catalog) {
		Exact gain = Exact.ZERO;
		for (Map.Entry<String, Integer> entry : conversion.gives().entrySet()) {
			Exact value = catalog.value(entry.getKey());
			if (value == null) {
				return null;
			}
			gain = gain.plus(value.times(entry.getValue()));
		}
		for (Map.Entry<String, Integer> entry : conversion.inputs().entrySet()) {
			Exact value = catalog.value(entry.getKey());
			if (value == null) {
				return null;
			}
			gain = gain.minus(value.times(entry.getValue()));
		}
		return gain;
	}

	/** Every finding for the catalogue's declared graph and the scale's own conversions. Empty means certified. */
	public static List<Finding> audit(EquivalenceCatalog catalog) {
		List<Transmutation> all = new ArrayList<>(catalog.transmutations().values());
		all.addAll(Assay.conversions(catalog));
		return audit(catalog, all);
	}

	/** Every finding for {@code conversions} (the server adds the scale's own to the declared graph). */
	public static List<Finding> audit(EquivalenceCatalog catalog, List<Transmutation> conversions) {
		List<Finding> findings = new ArrayList<>();
		List<Transmutation> recipes = new ArrayList<>();
		for (Transmutation conversion : conversions) {
			Set<String> unvalued = new java.util.TreeSet<>();
			for (String item : conversion.inputs().keySet()) {
				if (catalog.value(item) == null) {
					unvalued.add(item);
				}
			}
			for (String item : conversion.gives().keySet()) {
				if (catalog.value(item) == null) {
					unvalued.add(item);
				}
			}
			if (!unvalued.isEmpty()) {
				findings.add(new Finding("unvalued", conversion.id(), Exact.ZERO, List.copyOf(unvalued)));
				continue;
			}
			if (conversion.kind() == Transmutation.Kind.SOURCE) {
				continue;
			}
			recipes.add(conversion);
			Exact gain = gain(conversion, catalog);
			if (gain != null && gain.signum() > 0) {
				findings.add(new Finding("gains_value", conversion.id(), gain, List.of()));
			}
		}
		for (Cycle cycle : cycles(catalog, recipes, MAX_LENGTH, MAX_CYCLES)) {
			if (cycle.profitable()) {
				findings.add(new Finding("cycle", String.join(" > ", cycle.conversions()), cycle.gain(), cycle.items()));
			}
		}
		return findings;
	}

	/**
	 * Every simple cycle of at most {@code maxLength} steps through {@code recipes} (at most {@code maxCycles}), each
	 * listed once, from its least item, with its exact yield and gain.
	 */
	public static List<Cycle> cycles(EquivalenceCatalog catalog, List<Transmutation> recipes, int maxLength, int maxCycles) {
		Map<String, List<Step>> edges = new TreeMap<>();
		for (Transmutation recipe : recipes) {
			Exact gain = gain(recipe, catalog);
			if (gain == null) {
				continue;
			}
			for (Map.Entry<String, Integer> in : recipe.inputs().entrySet()) {
				for (Map.Entry<String, Integer> out : recipe.gives().entrySet()) {
					edges.computeIfAbsent(in.getKey(), unused -> new ArrayList<>()).add(new Step(recipe.id(), in.getKey(), out.getKey(),
							in.getValue(), out.getValue(), gain));
				}
			}
		}
		List<Cycle> cycles = new ArrayList<>();
		for (String start : edges.keySet()) {
			search(start, start, edges, new ArrayList<>(), new HashSet<>(), maxLength, maxCycles, cycles);
			if (cycles.size() >= maxCycles) {
				break;
			}
		}
		return cycles;
	}

	/** One way an item becomes another: a recipe that takes {@code takes} of one and gives {@code gives} of the other. */
	private record Step(String conversion, String from, String to, int takes, int gives, Exact gain) {
	}

	private static void search(String start, String at, Map<String, List<Step>> edges, List<Step> path, Set<String> visited, int maxLength,
			int maxCycles, List<Cycle> cycles) {
		if (cycles.size() >= maxCycles || path.size() >= maxLength) {
			return;
		}
		for (Step step : edges.getOrDefault(at, List.of())) {
			if (step.to().equals(start)) {
				path.add(step);
				cycles.add(measure(path));
				path.remove(path.size() - 1);
			} else if (step.to().compareTo(start) > 0 && !visited.contains(step.to())) {
				path.add(step);
				visited.add(step.to());
				search(start, step.to(), edges, path, visited, maxLength, maxCycles, cycles);
				visited.remove(step.to());
				path.remove(path.size() - 1);
			}
			if (cycles.size() >= maxCycles) {
				return;
			}
		}
	}

	/** Feeds one unit of the first item round the path: each step runs as often as what reaches it allows. */
	private static Cycle measure(List<Step> path) {
		Exact fed = Exact.ONE;
		Exact gain = Exact.ZERO;
		List<String> items = new ArrayList<>();
		List<String> conversions = new ArrayList<>();
		for (Step step : path) {
			Exact runs = fed.divide(Exact.of(step.takes()));
			gain = gain.plus(runs.times(step.gain()));
			fed = runs.times(step.gives());
			items.add(step.from());
			conversions.add(step.conversion());
		}
		return new Cycle(List.copyOf(items), List.copyOf(conversions), fed, gain);
	}
}
