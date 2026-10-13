package io.github.jimbozoomer.jugcraft.concordance.conclave;

/**
 * The Starbound Conclave's ranks (roadmap step 23). Each asks for renown, breadth (traditions in which at least
 * {@link Conclave#TRADITION_RENOWN} renown was earned) and variety (different kinds of contribution), so no single
 * repeated deed reaches one; and each opens a commission tier and, from Fellow, projects.
 */
public enum Rank {
	ASPIRANT("aspirant", 0, 0, 0, 1, false),
	FELLOW("fellow", 25, 2, 2, 2, true),
	COMPANION("companion", 80, 4, 2, 3, true),
	LUMINARY("luminary", 160, 5, 3, 3, true),
	STARBOUND("starbound", 260, 6, 3, 3, true);

	public final String id;
	/** Renown needed. */
	public final int renown;
	/** Traditions with at least {@link Conclave#TRADITION_RENOWN} renown needed. */
	public final int traditions;
	/** Different kinds of contribution needed. */
	public final int kinds;
	/** The highest commission tier this rank may take. */
	public final int tier;
	/** Whether this rank may begin a project (personal, or its party's). */
	public final boolean projects;

	Rank(String id, int renown, int traditions, int kinds, int tier, boolean projects) {
		this.id = id;
		this.renown = renown;
		this.traditions = traditions;
		this.kinds = kinds;
		this.tier = tier;
		this.projects = projects;
	}
}
