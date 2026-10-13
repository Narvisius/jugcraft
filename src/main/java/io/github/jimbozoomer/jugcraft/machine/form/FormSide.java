package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.machine.Footprint;
import net.minecraft.core.Direction;

/**
 * A face of a machine form as a player standing in front of it sees it. A north-facing form's front faces north, so
 * the viewer looks south: their left is east and their right is west. The form turns with the machine, so a port on
 * the left stays on the viewer's left whichever way the machine faces.
 */
public enum FormSide {
	FRONT(Direction.NORTH, 0, -1, 0),
	BACK(Direction.SOUTH, 0, 1, 0),
	LEFT(Direction.EAST, -1, 0, 0),
	RIGHT(Direction.WEST, 1, 0, 0),
	TOP(Direction.UP, 0, 0, 1),
	BOTTOM(Direction.DOWN, 0, 0, -1);

	private final Direction north;
	/** The step to the neighbouring cell through this face, in columns, rows and layers. */
	final int column;
	final int row;
	final int layer;

	FormSide(Direction north, int column, int row, int layer) {
		this.north = north;
		this.column = column;
		this.row = row;
		this.layer = layer;
	}

	/** The world direction of this face when the machine faces {@code facing}. */
	public Direction world(Direction facing) {
		return Footprint.rotate(north, facing);
	}

	/** The form face that points {@code side} in the world when the machine faces {@code facing}. */
	public static FormSide of(Direction side, Direction facing) {
		for (FormSide face : values()) {
			if (face.world(facing) == side) {
				return face;
			}
		}
		throw new IllegalArgumentException("No face points " + side);
	}
}
