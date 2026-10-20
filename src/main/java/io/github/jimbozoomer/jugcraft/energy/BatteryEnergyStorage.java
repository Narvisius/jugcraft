package io.github.jimbozoomer.jugcraft.energy;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** Shared battery policy and transactional per-tick budgets across every exposed face. */
public final class BatteryEnergyStorage extends SimpleEnergyStorage {
	private final LongSupplier clock, rate;
	private final IntSupplier mode, reserve, priority, outputPercent;
	private final BooleanSupplier available;
	private final Budget budget = new Budget();
	private boolean dischargeReady;

	public BatteryEnergyStorage(long capacity, Runnable changed, LongSupplier ceiling,
			LongSupplier clock, LongSupplier rate, IntSupplier mode, IntSupplier reserve,
			IntSupplier priority, IntSupplier outputPercent, BooleanSupplier available) {
		super(capacity, Long.MAX_VALUE, Long.MAX_VALUE, changed, ceiling);
		this.clock = clock; this.rate = rate; this.mode = mode; this.reserve = reserve;
		this.priority = priority; this.outputPercent = outputPercent; this.available = available;
	}

	/** Tick only, outside transactions: 5% hysteresis prevents reserve-threshold chatter. */
	public void tickPolicy() {
		long floor = getCapacity() * reserve.getAsInt() / 100;
		if (reserve.getAsInt() == 0) dischargeReady = true;
		else if (getAmount() <= floor) dischargeReady = false;
		else if (getAmount() >= floor + getCapacity() * 5 / 100) dischargeReady = true;
	}

	@Override public int chargePriority() { return priority.getAsInt(); }
	@Override public boolean supportsInsertion() { return available.getAsBoolean() && (mode.getAsInt() == 0 || mode.getAsInt() == 1); }
	@Override public boolean supportsExtraction() { return available.getAsBoolean() && (mode.getAsInt() == 0 || mode.getAsInt() == 2); }

	@Override public long insert(long maximum, TransactionContext tx) {
		if (!supportsInsertion() || maximum <= 0) return 0;
		budget.prepare(clock.getAsLong(), tx);
		long moved = super.insert(Math.max(0, Math.min(maximum, rate.getAsLong() - budget.in)), tx);
		budget.in += moved;
		return moved;
	}

	@Override public long extract(long maximum, TransactionContext tx) {
		if (!supportsExtraction() || maximum <= 0 || (reserve.getAsInt() > 0 && !dischargeReady)) return 0;
		budget.prepare(clock.getAsLong(), tx);
		long floor = getCapacity() * reserve.getAsInt() / 100;
		long limit = rate.getAsLong() * outputPercent.getAsInt() / 100;
		long moved = super.extract(Math.max(0, Math.min(maximum, Math.min(limit - budget.out, getAmount() - floor))), tx);
		budget.out += moved;
		return moved;
	}

	private record State(long tick, long in, long out) {}
	private static final class Budget extends SnapshotParticipant<State> {
		long tick = Long.MIN_VALUE, in, out;
		void prepare(long now, TransactionContext tx) {
			updateSnapshots(tx);
			if (now != tick) { tick = now; in = 0; out = 0; }
		}
		@Override protected State createSnapshot() { return new State(tick, in, out); }
		@Override protected void readSnapshot(State s) { tick = s.tick(); in = s.in(); out = s.out(); }
	}
}
