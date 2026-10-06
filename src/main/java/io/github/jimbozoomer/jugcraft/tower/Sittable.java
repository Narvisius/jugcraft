package io.github.jimbozoomer.jugcraft.tower;

/** Furniture you can sit on (chairs, sofas, benches): using it seats you, facing the way it faces. */
public interface Sittable {
	/** The seat's top above the block's bottom, in blocks. */
	double seatHeight();
}
