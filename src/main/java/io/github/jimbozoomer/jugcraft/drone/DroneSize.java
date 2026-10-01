package io.github.jimbozoomer.jugcraft.drone;

/**
 * How much landing-pad room a drone needs. Every 5x5 landing pad has {@link #SLOTS_PER_PAD} slots:
 * four small drones, two medium or one large.
 */
public enum DroneSize {
	SMALL(1),
	MEDIUM(2),
	LARGE(4);

	public static final int SLOTS_PER_PAD = 4;

	private final int slots;

	DroneSize(int slots) {
		this.slots = slots;
	}

	public int slots() {
		return slots;
	}
}
