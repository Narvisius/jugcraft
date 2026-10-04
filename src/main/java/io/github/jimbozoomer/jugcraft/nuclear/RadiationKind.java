package io.github.jimbozoomer.jugcraft.nuclear;

/**
 * What kind of radiation a source gives off, and how much of it a full hazmat suit stops. Nuclear radiation (spent
 * fuel, corium, fallout) is mostly stopped by the suit; the Hawking radiation of micro black holes (later) is far
 * harder and needs the advanced radiation suit, the normal suit only slowing it down.
 */
public enum RadiationKind {
	NUCLEAR(0.90),
	HAWKING(0.30);

	/** Fraction stopped by a full hazmat suit (each of the four pieces stops a quarter of that). */
	public final double hazmatStops;

	RadiationKind(double hazmatStops) {
		this.hazmatStops = hazmatStops;
	}
}
