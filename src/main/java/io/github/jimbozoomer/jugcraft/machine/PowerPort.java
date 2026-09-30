package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The one place a machine takes power from cables: a face of one part, written for a
 * north-facing machine and turned with it. Machines without a port accept power on every face
 * of every block, and cables connect to whichever side they touch.
 */
public record PowerPort(int part, Direction face) {
	/** Whether a cable touching {@code side} of part {@code statePart} reaches the port. */
	public boolean allows(int statePart, Direction facing, @Nullable Direction side) {
		return side == null || (statePart == part && side == Footprint.rotate(face, facing));
	}
}
