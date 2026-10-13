package io.github.jimbozoomer.jugcraft.concordance.resource;

/**
 * What a container does with more than it has room for. Every container states one; there is no silent default.
 */
public enum Overflow {
	/** All or nothing: an insertion that does not fit is refused whole and nothing moves. */
	REJECT,
	/** Take what fits; the rest stays where it came from. */
	FILL,
	/** Take what fits; the rest is lost. Only for sinks that say so (burning off, venting). */
	VOID
}
