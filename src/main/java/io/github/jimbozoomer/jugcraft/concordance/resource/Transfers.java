package io.github.jimbozoomer.jugcraft.concordance.resource;

import java.util.UUID;

/**
 * The one way an amount of resource moves between two containers. A transfer is worked out completely before
 * anything changes and then returned whole (both containers and the budget after it), so it is never half done:
 * <ol>
 * <li>Both containers must hold the same type. A transfer never converts; that takes a {@link Conversion}.</li>
 * <li>The type must be portable (Focus is not; Bound Will moves as records through {@link BoundWillLedger}).</li>
 * <li>The actor must be allowed to take from the source and to put into the target ({@link Ownership}).</li>
 * <li>At most the source's amount, the rate budget's allowance and the request move.</li>
 * <li>What does not fit follows the target's {@link Overflow}: refused whole, left in the source, or lost.</li>
 * </ol>
 */
public final class Transfers {
	private Transfers() {
	}

	public enum Outcome {
		MOVED,
		/** Some moved; the rest stayed in the source (or was lost, with {@link Overflow#VOID}). */
		PARTIAL,
		WRONG_TYPE,
		NOT_PORTABLE,
		NOT_PERMITTED,
		NOTHING_TO_MOVE,
		FULL,
		RATE_LIMITED;

		public boolean moved() {
			return this == MOVED || this == PARTIAL;
		}
	}

	/** One side of a transfer: the container and who controls it. */
	public record Side(Reservoir reservoir, Ownership ownership) {
	}

	/**
	 * The result: the source and target after it, the budget after it, how much left the source, how much arrived and
	 * how much was lost (only with {@link Overflow#VOID}).
	 */
	public record Transfer(Reservoir source, Reservoir target, RateBudget budget, long sent, long received, long voided,
			Outcome outcome) {
	}

	public static Transfer move(Side from, Side to, UUID actor, long requested, RateBudget budget, long now, Overflow overflow) {
		Reservoir source = from.reservoir();
		Reservoir target = to.reservoir();
		if (requested < 0) {
			throw new IllegalArgumentException("negative amount");
		}
		Transfer refused = new Transfer(source, target, budget, 0L, 0L, 0L, Outcome.NOTHING_TO_MOVE);
		if (!source.type().equals(target.type())) {
			return with(refused, Outcome.WRONG_TYPE);
		}
		if (!source.type().kind().portable) {
			return with(refused, Outcome.NOT_PORTABLE);
		}
		if (!from.ownership().mayExtract(actor) || !to.ownership().mayInsert(actor)) {
			return with(refused, Outcome.NOT_PERMITTED);
		}
		long allowance = budget.allowance(now);
		if (allowance == 0 && requested > 0) {
			return with(refused, Outcome.RATE_LIMITED);
		}
		long offered = Math.min(Math.min(requested, source.amount()), allowance);
		if (offered == 0) {
			return refused;
		}
		Reservoir.Insertion insertion = target.insert(source.type(), offered, overflow);
		if (insertion.accepted() == 0) {
			return with(refused, Outcome.FULL);
		}
		long sent = insertion.accepted() + insertion.voided();
		Reservoir.Extraction extraction = source.extract(source.type(), sent, true);
		Outcome outcome = sent == requested && insertion.voided() == 0 ? Outcome.MOVED : Outcome.PARTIAL;
		return new Transfer(extraction.reservoir(), insertion.reservoir(), budget.spend(now, sent), sent, insertion.accepted(),
				insertion.voided(), outcome);
	}

	private static Transfer with(Transfer transfer, Outcome outcome) {
		return new Transfer(transfer.source(), transfer.target(), transfer.budget(), 0L, 0L, 0L, outcome);
	}
}
