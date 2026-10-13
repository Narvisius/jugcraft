package io.github.jimbozoomer.jugcraft.concordance.logistics;

import org.jspecify.annotations.Nullable;

/**
 * Where an open request stands (roadmap step 18). It is worked out from the request's progress, never stored apart
 * from it, so the state and the cargo cannot disagree. A request done or cancelled leaves the ledger (its post's history
 * keeps the record).
 */
public enum RequestState {
	/** Filed; no courier has it. */
	OPEN("open"),
	/** A courier has it and may hold a reservation at a source; nothing picked up yet. */
	CLAIMED("claimed"),
	/** Picked up: the cargo is in transit, held by the ledger, aboard its courier. */
	IN_TRANSIT("in_transit"),
	/** Cargo in transit with no courier aboard (its courier was removed or lapsed): it waits at the post. */
	STRANDED("stranded"),
	/** Cancelled, or its destination gone, with cargo still to take back. */
	RETURNING("returning");

	public final String id;

	RequestState(String id) {
		this.id = id;
	}

	public static @Nullable RequestState fromId(String id) {
		for (RequestState state : values()) {
			if (state.id.equals(id)) {
				return state;
			}
		}
		return null;
	}
}
