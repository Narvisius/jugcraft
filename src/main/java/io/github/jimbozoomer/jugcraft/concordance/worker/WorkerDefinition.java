package io.github.jimbozoomer.jugcraft.concordance.worker;

/**
 * The terms of a kind of worker (roadmap step 17; data: {@code data/<ns>/concordance/worker/}). Three separate models,
 * never one: a familiar keeps close and supports its person by its bond; a spirit works only within an agreement; a
 * construct carries what its body allows and runs on energy and repair.
 */
public sealed interface WorkerDefinition {
	String id();

	/** A familiar: it follows within {@code follow} blocks and, by its bond, mends its person with {@code effect}. */
	record Familiar(String id, int follow, String effect, int amplifier, int duration) implements WorkerDefinition {
	}

	/**
	 * A spirit: the one {@code work} it may agree to, within {@code radius} of its anchor, in the world clock's hours
	 * {@code from} to {@code to}, at most {@code quota} tasks a day, carrying at most {@code carry} items at a time.
	 */
	record Spirit(String id, String work, int radius, int from, int to, int quota, int carry) implements WorkerDefinition {
	}

	/**
	 * A construct: {@code integrity} worn by {@code wear} a trip and mended by {@code repairAmount} with each
	 * {@code repairItem}; {@code energy} Ley Charge, {@code tripEnergy} a trip; at most {@code carry} items a trip.
	 */
	record Construct(String id, int integrity, int wear, String repairItem, int repairAmount, int energy, int tripEnergy, int carry)
			implements WorkerDefinition {
	}
}
