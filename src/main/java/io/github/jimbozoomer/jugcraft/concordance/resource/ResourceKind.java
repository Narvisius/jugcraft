package io.github.jimbozoomer.jugcraft.concordance.resource;

import org.jspecify.annotations.Nullable;

/**
 * The seven kinds of magical resource (docs/ARCANE_CONCORDANCE.md, "Seven resources"). They never stand in for one
 * another: a transfer moves one type between containers of that type, and only an explicit {@link Conversion} turns
 * one into another. Keep the ids equal to RESOURCES in tools/concordance.py.
 */
public enum ResourceKind {
	/** A practitioner's own attention: lives with the player, never in an item or device, never moved. */
	FOCUS("focus", "point", true, false, false),
	/** Working energy in devices and conduits. */
	LEY_CHARGE("ley_charge", "ley", true, true, false),
	/** A measured quantity of one Principle; qualified by the Principle ({@code essence/radiance}). */
	ESSENCE("essence", "measure", true, true, true),
	/** Life given in an offering; never drawn from health except by an explicit offering. */
	VITAE("vitae", "drop", true, true, false),
	/** Alignment gathered from a celestial event; each event pays once ({@link AstralLedger}). */
	ASTRAL_RESONANCE("astral_resonance", "resonance", true, true, false),
	/** An agreement with an identity: records, never amounts ({@link BoundWillLedger}). */
	BOUND_WILL("bound_will", "record", false, false, false),
	/** Equivalence value of mundane matter, rounded down ({@link PrimaValue}). */
	PRIMA_MATERIA("prima_materia", "grain", true, true, false);

	public final String id;
	public final String unit;
	/** Whether amounts of it are interchangeable (Bound Will records are not). */
	public final boolean fungible;
	/** Whether it can move from one container to another at all (Focus cannot). */
	public final boolean portable;
	/** Whether a type of this kind names a Principle. */
	public final boolean qualified;

	ResourceKind(String id, String unit, boolean fungible, boolean portable, boolean qualified) {
		this.id = id;
		this.unit = unit;
		this.fungible = fungible;
		this.portable = portable;
		this.qualified = qualified;
	}

	public static @Nullable ResourceKind fromId(String id) {
		for (ResourceKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}
}
