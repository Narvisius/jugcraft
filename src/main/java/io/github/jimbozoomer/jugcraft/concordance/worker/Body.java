package io.github.jimbozoomer.jugcraft.concordance.worker;

import org.jspecify.annotations.Nullable;

/**
 * A construct's body (roadmap step 17): its integrity (worn by each trip, mended only by repair) and the Ley Charge it
 * runs on (spent by each trip, drawn from a Ley Pylon its keeper's party may use). Its limits come from its definition
 * ({@link WorkerDefinition.Construct}). Immutable.
 */
public record Body(int integrity, long energy) {
	/** Null when it can make a trip; otherwise why not. */
	public @Nullable Status ready(WorkerDefinition.Construct terms) {
		if (integrity <= 0) {
			return Status.NEEDS_REPAIR;
		}
		if (energy < terms.tripEnergy()) {
			return Status.NO_ENERGY;
		}
		return null;
	}

	/** After one trip: its energy spent and its wear taken. */
	public Body trip(WorkerDefinition.Construct terms) {
		return new Body(Math.max(0, integrity - terms.wear()), Math.max(0L, energy - terms.tripEnergy()));
	}

	/** After a repair with one of its repair items. */
	public Body repaired(WorkerDefinition.Construct terms) {
		return new Body(Math.min(terms.integrity(), integrity + terms.repairAmount()), energy);
	}

	public Body charged(WorkerDefinition.Construct terms, long amount) {
		return new Body(integrity, Math.min(terms.energy(), energy + Math.max(0L, amount)));
	}

	/** How much Ley Charge it can take before it is full. */
	public long room(WorkerDefinition.Construct terms) {
		return Math.max(0L, terms.energy() - energy);
	}
}
