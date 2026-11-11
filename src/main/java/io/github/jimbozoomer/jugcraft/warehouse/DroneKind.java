package io.github.jimbozoomer.jugcraft.warehouse;

/** The warehouse's two drones: the stacker (a mobile robot arm: cases onto pallets, wrapping) and the pallet drone (a turret truck: pallets into the racks). */
public enum DroneKind {
	STACKER("SD"), PALLET("PD");

	/** The fleet's name prefix (SD-01, PD-02). */
	public final String prefix;

	DroneKind(String prefix) {
		this.prefix = prefix;
	}
}
