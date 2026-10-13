package io.github.jimbozoomer.jugcraft.machine.form;

/**
 * What one position inside a machine form's envelope is (see {@link MachineForm}): its occupancy mask. A form never
 * needs every position of its envelope to be a block.
 */
public enum FormCell {
	/** A block of the machine (a part). Placing the machine needs it free; breaking any part removes the machine. */
	STRUCTURE,
	/**
	 * Room a moving mechanism sweeps through (a press head, a rotor): placing the machine needs it free, and while
	 * anything stands there the machine stops, as unformed, without losing its work.
	 */
	CLEARANCE,
	/** Open space inside the envelope, such as a walkway or the gap under a hood. Anything may stand there. */
	ACCESS;

	/** The layout character for this cell: {@code #} (or {@code C} for the controller), {@code ~} and {@code .}. */
	public static FormCell of(char symbol) {
		return switch (symbol) {
			case '#', 'C' -> STRUCTURE;
			case '~' -> CLEARANCE;
			case '.' -> ACCESS;
			default -> throw new IllegalArgumentException("Unknown form cell '" + symbol + "': use C, #, ~ or .");
		};
	}
}
