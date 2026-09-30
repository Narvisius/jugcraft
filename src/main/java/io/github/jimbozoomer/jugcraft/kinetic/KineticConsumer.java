package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.Direction;

/** A block entity that takes kinetic energy (KE) from a shaft, gearbox or source touching it. */
public interface KineticConsumer {
	/**
	 * Takes up to {@code maxAmount} KE arriving through {@code side} of this block and returns how much
	 * it took. Called once per source per tick; the energy is used or stored at once.
	 */
	long acceptKinetic(Direction side, long maxAmount);
}
