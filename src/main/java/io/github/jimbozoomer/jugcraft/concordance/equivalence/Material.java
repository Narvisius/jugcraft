package io.github.jimbozoomer.jugcraft.concordance.equivalence;

import io.github.jimbozoomer.jugcraft.concordance.resource.PrimaValue;

/**
 * A catalogued mundane material (roadmap step 21, data: concordance/material): its item, its exact Prima Materia value
 * per unit, and whether an Assayer's Scale may dissolve it into grains or form it from them. A material may be valued
 * (so the conversion graph can be checked) without being dissolvable or formable.
 */
public record Material(String item, PrimaValue value, boolean dissolvable, boolean formable) {
	public Exact exact() {
		return Exact.of(value.numerator(), value.denominator());
	}
}
