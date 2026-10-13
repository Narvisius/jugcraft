package io.github.jimbozoomer.jugcraft.concordance.alchemy;

/**
 * Something that can go into a mixture (data: {@code data/<ns>/concordance/ingredient/}): the item, the properties it
 * carries and the contaminant it brings, both in milli-units. An item is at most one ingredient.
 */
public record Ingredient(String id, String item, Vector properties, long contaminant) {
	public Ingredient {
		if (contaminant < 0) {
			throw new IllegalArgumentException("negative contaminant");
		}
	}
}
