package io.github.jimbozoomer.jugcraft.machine.form;

/**
 * A logical port: the one face of one structural block of a form where pipes, cables, conveyors and hoppers reach a
 * particular tank, group of slots or the power input (or, on a generator, the power output). Every other face of the
 * machine is closed to them, and tool sockets are never reachable through any port.
 *
 * @param name a stable name, shown as the port's label
 * @param kind what the port connects to
 * @param target the input tank (fluid input), output tank (fluid output) or item slot (counted among the inputs or
 *        the outputs) it reaches; {@link #ALL} for every item input or every item output. Ignored for power.
 * @param column the port's block: columns from the viewer's left
 * @param row rows from the front
 * @param layer layers from the bottom
 * @param side the face of that block the port is on; it must face out of the machine
 */
public record FormPort(String name, Kind kind, int target, int column, int row, int layer, FormSide side) {
	/** An item port reaching every input (or every output) slot. */
	public static final int ALL = -1;

	public enum Kind {
		FLUID_IN, FLUID_OUT, ITEM_IN, ITEM_OUT, ENERGY_IN,
		/** A generator's power output: cables join it here and it pushes its power out. */
		ENERGY_OUT;

		public boolean fluid() {
			return this == FLUID_IN || this == FLUID_OUT;
		}

		public boolean item() {
			return this == ITEM_IN || this == ITEM_OUT;
		}

		/** A power port of either direction, where cables connect. */
		public boolean energy() {
			return this == ENERGY_IN || this == ENERGY_OUT;
		}
	}

	/** Whether this item port reaches item slot {@code slot} (counted among its own inputs or outputs). */
	public boolean reaches(int slot) {
		return target == ALL || target == slot;
	}
}
