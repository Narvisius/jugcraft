package io.github.jimbozoomer.jugcraft.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/**
 * An energy buffer with fixed capacity and per-call rate limits. This is the "battery"
 * inside every Jugcraft generator, battery box and machine.
 */
public class SimpleEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
	private final long capacity;
	private final long maxInsert;
	private final long maxExtract;
	private final Runnable onChange;
	private long amount;

	public SimpleEnergyStorage(long capacity, long maxInsert, long maxExtract, Runnable onChange) {
		this.capacity = capacity;
		this.maxInsert = maxInsert;
		this.maxExtract = maxExtract;
		this.onChange = onChange;
	}

	@Override
	public boolean supportsInsertion() {
		return maxInsert > 0;
	}

	@Override
	public long insert(long maxAmount, TransactionContext transaction) {
		long accepted = Math.min(Math.min(maxAmount, maxInsert), capacity - amount);
		if (accepted > 0) {
			updateSnapshots(transaction);
			amount += accepted;
		}
		return Math.max(accepted, 0);
	}

	@Override
	public boolean supportsExtraction() {
		return maxExtract > 0;
	}

	@Override
	public long extract(long maxAmount, TransactionContext transaction) {
		long removed = Math.min(Math.min(maxAmount, maxExtract), amount);
		if (removed > 0) {
			updateSnapshots(transaction);
			amount -= removed;
		}
		return Math.max(removed, 0);
	}

	@Override
	public long getAmount() {
		return amount;
	}

	@Override
	public long getCapacity() {
		return capacity;
	}

	/** Internal use by the owner (e.g. a generator producing or a machine consuming), outside transactions. */
	public void setAmount(long amount) {
		this.amount = Math.max(0, Math.min(capacity, amount));
	}

	@Override
	protected Long createSnapshot() {
		return amount;
	}

	@Override
	protected void readSnapshot(Long snapshot) {
		amount = snapshot;
	}

	@Override
	protected void onFinalCommit() {
		onChange.run();
	}
}
