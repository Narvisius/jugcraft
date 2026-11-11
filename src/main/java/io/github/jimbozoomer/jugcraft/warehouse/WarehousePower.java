package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Part W5: the warehouse runs on power brought in by cable to its switchboards (SwitchboardBlockEntity). The terminal
 * works out each second what its six circuits draw and takes it from the switchboards' stores; short of power, charging
 * slows first, then the machines' circuits are shed (a cycle under way finishes; no new one starts). The JE a tick each
 * thing draws while it works.
 */
public final class WarehousePower {
	/** The six outgoing circuits, as the board's breakers are labelled. */
	public enum Circuit {
		DOCKS("DOCKS", "chargers"), LIFTS("LIFTS", "Pallet Lifts"), STATIONS("STATIONS", "intake, palletizing"),
		READY_STORAGE("READY STORAGE", "lift module, hatch"), LIQUIDS("LIQUIDS", "filling, emptying"), LIGHTS("LIGHTS · CONTROL", "terminal, beacons");

		public final String label, what;

		Circuit(String label, String what) {
			this.label = label;
			this.what = what;
		}
	}

	public static final int BASE = 8, CHARGING = 24, LIFT = 64, INTAKE = 12, STATION = 8, READY = 32, FILLER = 16, EMPTIER = 16;
	/** A switchboard's store and how much a tick its cable may bring in. */
	public static final long CAPACITY = 100_000, MAX_IN = 4_096;
	/** Under this much of the stores (and drawing more than comes in): LOW POWER. */
	public static final double LOW = 0.1;

	private WarehousePower() {
	}

	/** Whether a warehouse machine at pos may start a cycle on a circuit (no warehouse: it isn't one's to stop). */
	public static boolean ok(Level level, BlockPos pos, Circuit circuit) {
		if (level.isClientSide()) {
			return true;
		}
		TerminalBlockEntity t = Warehouses.terminalOf(level, pos);
		return t == null || t.powered(circuit);
	}
}
