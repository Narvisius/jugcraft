package io.github.jimbozoomer.jugcraft.concordance.ecology;

/** How one factor of a habitat suits an organism: ideal, tolerable, or outside its range on one side or the other. */
public enum Fit {
	IDEAL("ideal"),
	TOLERABLE("tolerable"),
	/** Below the least it can live with. */
	SHORT("short"),
	/** Above the most it can live with. */
	EXCESS("excess");

	public final String id;

	Fit(String id) {
		this.id = id;
	}

	public boolean outside() {
		return this == SHORT || this == EXCESS;
	}
}
