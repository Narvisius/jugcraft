package io.github.jimbozoomer.jugcraft.concordance.resource;

/**
 * A container of one fungible resource type: how much it holds and how much it can. Amounts are whole units in a
 * {@code long}, so accounting is exact and nothing is lost or made by rounding. Immutable: every change returns the
 * new reservoir with the result, so a change that is refused leaves both sides as they were.
 */
public record Reservoir(ResourceType type, long amount, long capacity) {
	public Reservoir {
		if (!type.kind().fungible) {
			throw new IllegalArgumentException(type + " is not held in amounts");
		}
		if (capacity < 0 || amount < 0 || amount > capacity) {
			throw new IllegalArgumentException("amount " + amount + " outside 0.." + capacity);
		}
	}

	public static Reservoir empty(ResourceType type, long capacity) {
		return new Reservoir(type, 0L, capacity);
	}

	public long space() {
		return capacity - amount;
	}

	public boolean isEmpty() {
		return amount == 0;
	}

	public enum Outcome {
		/** Everything asked for was done. */
		DONE,
		/** Part was done (the rest stayed or, with {@link Overflow#VOID}, was lost). */
		PARTIAL,
		/** Nothing was done: the resource is of another type. */
		WRONG_TYPE,
		/** Nothing was done: no room (or not room for all, with {@link Overflow#REJECT}). */
		FULL,
		/** Nothing was done: nothing (or not enough, when all was required) to take. */
		EMPTY
	}

	/** After an insertion: the reservoir, what it took, what it refused (still the caller's) and what was lost. */
	public record Insertion(Reservoir reservoir, long accepted, long refused, long voided, Outcome outcome) {
	}

	/** After an extraction: the reservoir and what was taken. */
	public record Extraction(Reservoir reservoir, long taken, Outcome outcome) {
	}

	public Insertion insert(ResourceType offered, long requested, Overflow overflow) {
		if (requested < 0) {
			throw new IllegalArgumentException("negative amount");
		}
		if (!offered.equals(type)) {
			return new Insertion(this, 0L, requested, 0L, Outcome.WRONG_TYPE);
		}
		long fits = Math.min(requested, space());
		if (fits < requested && overflow == Overflow.REJECT || fits == 0 && requested > 0) {
			return new Insertion(this, 0L, requested, 0L, Outcome.FULL);
		}
		long rest = requested - fits;
		Reservoir after = new Reservoir(type, amount + fits, capacity);
		return new Insertion(after, fits, overflow == Overflow.VOID ? 0L : rest, overflow == Overflow.VOID ? rest : 0L,
				rest == 0 ? Outcome.DONE : Outcome.PARTIAL);
	}

	/** Takes up to {@code requested}; with {@code exact}, all of it or nothing. */
	public Extraction extract(ResourceType wanted, long requested, boolean exact) {
		if (requested < 0) {
			throw new IllegalArgumentException("negative amount");
		}
		if (!wanted.equals(type)) {
			return new Extraction(this, 0L, Outcome.WRONG_TYPE);
		}
		long taken = Math.min(requested, amount);
		if (taken == 0 && requested > 0 || exact && taken < requested) {
			return new Extraction(this, 0L, Outcome.EMPTY);
		}
		return new Extraction(new Reservoir(type, amount - taken, capacity), taken, taken == requested ? Outcome.DONE : Outcome.PARTIAL);
	}
}
