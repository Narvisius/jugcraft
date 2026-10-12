package io.github.jimbozoomer.jugcraft.machine.form;

/**
 * The eight states a formed machine reports (docs/features/industrial-factory-implementation-plan.md). The order is
 * the precedence the controller reports them in: a broken structure first, then the first thing stopping a batch from
 * starting, then the state of the batches already running. Its ordinal is synced to the client, so add states only
 * at the end.
 */
public enum MachineLifecycle {
	/** A part is missing or a clearance is blocked: nothing runs, and paid work waits. */
	UNFORMED(true),
	/** Formed, with nothing to do (or paused by its operator). */
	IDLE(false),
	/** Holding inputs that make no complete batch yet. */
	WAITING_INPUT(true),
	/** A batch needs a reusable tool its sockets do not hold. */
	WAITING_TOOL(true),
	/** A batch is running but the machine cannot pay this tick's energy. */
	WAITING_ENERGY(true),
	/** The first paid ticks of a batch started from cold. */
	WARMING(false),
	/** A batch is running. */
	PROCESSING(false),
	/** A batch cannot start (or finish) until an output has room. */
	OUTPUT_BLOCKED(true);

	private final boolean warning;

	MachineLifecycle(boolean warning) {
		this.warning = warning;
	}

	/** Whether the player has something to fix. */
	public boolean warning() {
		return warning;
	}

	/** Whether batches are making paid progress. */
	public boolean working() {
		return this == WARMING || this == PROCESSING;
	}

	public String translationKey() {
		return "container.jugcraft.form.state." + name().toLowerCase();
	}

	public static MachineLifecycle byOrdinal(int ordinal) {
		MachineLifecycle[] values = values();
		return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IDLE;
	}
}
