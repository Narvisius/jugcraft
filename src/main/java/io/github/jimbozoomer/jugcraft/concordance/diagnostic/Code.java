package io.github.jimbozoomer.jugcraft.concordance.diagnostic;

import org.jspecify.annotations.Nullable;

/**
 * The shared diagnostic codes (plan 03:9; Concordance Chunk C, C-01, C-P6, C-P22): why a recipe, ritual, station or
 * courier waits or refuses, the same words for the same cause everywhere. An id is stable once released: never renamed,
 * reused or removed; new codes are appended. Worded through Diagnostic.text() under compose.jugcraft.diagnostic.<id>;
 * the parameters of the codes the crucible raises are fixed by Diagnostic's factories, the others' by the unit that
 * first raises them. local/peepo/CompanionStatus and the porter's worker/Status map from these at their own edges. Keep
 * equal to CODES in tools/concordance_diagnostics.py, in this order (tools/check_mod_data.py check_diagnostics).
 */
public enum Code {
	// Plan 03:9's list, in its order.
	/** (count, what: a step's words, an offering's item, or "#tag") */
	MISSING_INGREDIENT("missing_ingredient", Kind.SHORTAGE),
	/** (wanted: a step's words, found: the same item as the buffer holds it) */
	WRONG_PREPARATION("wrong_preparation", Kind.SHORTAGE),
	/** (wanted, now: band refs) */
	WRONG_HEAT("wrong_heat", Kind.CONDITION),
	/** (what, held, needed: each a line; fixed by C-03, worded for any power; C-05: a Crucible Stand short of a tick's JE) */
	UNAVAILABLE_POWER("unavailable_power", Kind.SHORTAGE),
	/** () the output is full */
	INSUFFICIENT_CAPACITY("insufficient_capacity", Kind.BLOCKED),
	/** () */
	MISSING_PERMISSION("missing_permission", Kind.BLOCKED),
	/** parameters fixed by their first user (C-06 raises neither: the porter's Status and the Peepo's CompanionStatus say so at their own edges) */
	UNLOADED_TARGET("unloaded_target", Kind.BLOCKED),
	/** (structure: a named pattern, faults to mend) */
	INCOMPLETE_PATTERN("incomplete_pattern", Kind.BLOCKED),
	/** parameters fixed by their first user (C-06 raises neither: the porter's Status and the Peepo's CompanionStatus say so at their own edges) */
	UNAVAILABLE_WORKER("unavailable_worker", Kind.SHORTAGE),
	/** () Concordance Chunk D, D-02: a holder block whose Bound Will is kept at another place or for someone else (a copy): it does nothing and releases nothing */
	CONFLICTING_AGREEMENT("conflicting_agreement", Kind.BLOCKED),
	/** parameters fixed by its first user */
	DORMANT_OCCURRENCE("dormant_occurrence", Kind.CONDITION),
	// Concordance Chunk C (C-01; COURIER_QUOTA is C-06's).
	/** (parts the step pours, whole parts the tank holds) */
	NEEDS_WATER("needs_water", Kind.SHORTAGE),
	/** () */
	NEEDS_CONTAINER("needs_container", Kind.SHORTAGE),
	/** (band ref, seconds left) */
	HELD("held", Kind.CONDITION),
	/** (step from 1, that step's words, the refusal) */
	INCOMPATIBLE("incompatible", Kind.FAULT),
	/** (count, what: a step's words, seconds until it asks again) */
	COURIER_QUOTA("courier_quota", Kind.BLOCKED),
	/** () */
	OUTPUT_DIFFERS("output_differs", Kind.BLOCKED),
	/** () */
	NEEDS_EMPTYING("needs_emptying", Kind.BLOCKED),
	/** () */
	UNREADABLE("unreadable", Kind.FAULT),
	// Concordance Chunk C, C-03 (ritual readiness).
	/** (what: a pylon's words) */
	NOT_LENT("not_lent", Kind.BLOCKED),
	/** (Focus held, Focus it takes) */
	NEEDS_FOCUS("needs_focus", Kind.SHORTAGE),
	/** (light now, the most it allows) */
	TOO_BRIGHT("too_bright", Kind.CONDITION),
	/** (blocks across, blocks above or below) */
	TOO_FAR("too_far", Kind.BLOCKED),
	/** (joined, needed, seconds left to join) */
	PARTICIPANTS("participants", Kind.SHORTAGE),
	// Concordance Chunk C, C-05 (the Crucible Stand).
	/** () nothing stands on it to heat */
	NO_CRUCIBLE("no_crucible", Kind.BLOCKED);

	public final String id;
	public final Kind kind;

	Code(String id, Kind kind) {
		this.id = id;
		this.kind = kind;
	}

	public static @Nullable Code fromId(String id) {
		for (Code code : values()) {
			if (code.id.equals(id)) {
				return code;
			}
		}
		return null;
	}

	/**
	 * What a reason is, which decides the sign shown when it begins (C-01, C-P22): a shortage or a blockage a WANT, a fault
	 * a PERIL with the breaking sound, a condition (heat, a hold, light, an occurrence) none, as the band wait never had one.
	 */
	public enum Kind {
		SHORTAGE, BLOCKED, CONDITION, FAULT
	}
}
