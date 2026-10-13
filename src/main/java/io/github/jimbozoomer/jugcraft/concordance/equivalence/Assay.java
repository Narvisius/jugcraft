package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What an Assayer's Scale pays and charges (roadmap step 21), conservatively rounded: dissolving gives the exact value
 * of the whole batch rounded down once; forming costs {@value #FORM_NUMERATOR}/{@value #FORM_DENOMINATOR} of the exact
 * value rounded up once. So a batch dissolved and formed again always loses, and splitting a batch never earns more
 * than the whole. A player's grains are capped at {@value #MAX_BALANCE}.
 */
public final class Assay {
	/** The pseudo-item the audit uses for grains of Prima Materia: one grain is worth one. */
	public static final String PRIMA = "jugcraft:prima_materia";
	public static final long FORM_NUMERATOR = 5;
	public static final long FORM_DENOMINATOR = 4;
	/** The most grains one player's ledger holds. */
	public static final long MAX_BALANCE = 1_000_000L;
	/** The most items one dissolving or forming moves. */
	public static final int MAX_BATCH = 64;

	private Assay() {
	}

	/** The grains dissolving {@code count} of {@code material} gives: its exact value, rounded down once. */
	public static long dissolve(Material material, long count) {
		return material.value().grains(count);
	}

	/** The grains forming {@code count} of {@code material} costs: its exact value times the markup, rounded up once. */
	public static long form(Material material, long count) {
		BigInteger cost = BigInteger.valueOf(material.value().numerator()).multiply(BigInteger.valueOf(count)).multiply(BigInteger.valueOf(FORM_NUMERATOR));
		BigInteger per = BigInteger.valueOf(material.value().denominator()).multiply(BigInteger.valueOf(FORM_DENOMINATOR));
		return new Exact(cost, per).ceil().longValueExact();
	}

	/**
	 * The scale's own conversions, for the cycle audit: for each dissolvable material, the smallest whole batch whose
	 * value is whole grains becomes those grains; for each formable one, one unit costs its rounded-up price.
	 */
	public static List<Transmutation> conversions(EquivalenceCatalog catalog) {
		List<Transmutation> scale = new ArrayList<>();
		for (Material material : catalog.materials().values()) {
			if (material.dissolvable()) {
				long batch = material.value().denominator();
				scale.add(new Transmutation("scale/dissolve/" + material.item(), Transmutation.Kind.RECIPE, "scale",
						Map.of(material.item(), (int) Math.min(Integer.MAX_VALUE, batch)), Map.of(PRIMA, (int) Math.min(Integer.MAX_VALUE, dissolve(material, batch))),
						Map.of(), Map.of(), Map.of(), List.of(), Map.of()));
			}
			if (material.formable()) {
				scale.add(new Transmutation("scale/form/" + material.item(), Transmutation.Kind.RECIPE, "scale",
						Map.of(PRIMA, (int) Math.min(Integer.MAX_VALUE, form(material, 1))), Map.of(material.item(), 1), Map.of(), Map.of(),
						Map.of(material.item(), 1), List.of(), Map.of()));
			}
		}
		return scale;
	}
}
