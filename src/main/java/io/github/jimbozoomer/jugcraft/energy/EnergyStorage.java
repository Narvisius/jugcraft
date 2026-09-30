package io.github.jimbozoomer.jugcraft.energy;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * Jugcraft's shared electricity interface, measured in Jugcraft Energy (JE).
 *
 * <p>Every generator, battery, cable endpoint and machine exposes one of these through
 * {@link #SIDED}, queried with the side being accessed (or {@code null} for no side).
 * Transfers happen inside Fabric transactions, so a move either fully commits or is
 * rolled back: energy can never be duplicated or lost by an aborted transfer.
 */
public interface EnergyStorage {
	BlockApiLookup<EnergyStorage, @Nullable Direction> SIDED =
			BlockApiLookup.get(Jugcraft.id("energy"), EnergyStorage.class, Direction.class);

	boolean supportsInsertion();

	/** Inserts up to {@code maxAmount} JE and returns how much was accepted. */
	long insert(long maxAmount, TransactionContext transaction);

	boolean supportsExtraction();

	/** Extracts up to {@code maxAmount} JE and returns how much was removed. */
	long extract(long maxAmount, TransactionContext transaction);

	long getAmount();

	long getCapacity();
}
