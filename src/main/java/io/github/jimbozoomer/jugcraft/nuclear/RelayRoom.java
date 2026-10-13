package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The control building's relay room cabinets (round 26, #10; Justin: "Real cabinet types", "Unique labels", "Yes, read
 * the reactor"): the 7300 process protection racks (four channels of three), the solid state protection system's two
 * trains of input, logic and output bays, the nuclear instrumentation drawers and the protective relay panels, each
 * labelled (its variant: the names below, in tools/relay_room.py's order). Every cabinet but the relay panels reads the
 * nearest reactor once a second (LivePanelBlockEntity relayTick) and shows it as a Westinghouse plant's would:
 * {@link #readings} turns the reactor into each cabinet's two meters (0 to 100 of their printed scales) and its lamps
 * (bit k lights rr_l&lt;k&gt; in tools/relay_room.py's layout); client/EdgTelemetry.java relayRoom draws them. The relay
 * panels' nameplates are lettered live with their names ({@link #RELAY_LABELS}).
 */
public final class RelayRoom {
	public static final String[] KEYS = {"protection_rack", "ssps_cabinet", "nis_cabinet", "relay_panel"};
	public static final int PROTECTION = 0, SSPS = 1, NIS = 2, RELAY = 3;
	static final String[] CHANNELS = {"I", "II", "III", "IV"};
	public static final String[] PROTECTION_LABELS = new String[12];
	public static final String[] SSPS_LABELS = {"TRAIN A INPUT BAY", "TRAIN A LOGIC BAY", "TRAIN A OUTPUT BAY", "TRAIN B INPUT BAY",
			"TRAIN B LOGIC BAY", "TRAIN B OUTPUT BAY"};
	public static final String[] NIS_LABELS = {"N31 N32", "N35 N36", "N41 N42", "N43 N44", "NC41 NM41"};
	public static final String[] RELAY_LABELS = {"RCP 1A 1B MOTORS", "RCP 1C 1D MOTORS", "MAIN GENERATOR", "MAIN TRANSFORMER", "UNIT AUX XFMR",
			"4160V BUS 1A", "4160V BUS 1B", "4160V BUS 1C", "4160V BUS 1D", "EDG 1A", "EDG 1B", "480V LC 1A", "480V LC 1B", "STARTUP XFMR",
			"345KV BREAKERS", "MAIN STEAM ISOL", "FEEDWATER ISOL", "SI PUMPS", "RHR PUMPS", "CONT SPRAY PUMPS", "CHARGING PUMPS", "CCW PUMPS",
			"SERVICE WATER", "AFW PUMPS", "CONDENSATE PUMPS", "FEEDWATER PUMPS", "CIRC WATER PUMPS", "DIESEL SEQ A", "DIESEL SEQ B"};

	static {
		for (int c = 0; c < 4; c++) {
			for (int r = 0; r < 3; r++) {
				PROTECTION_LABELS[c * 3 + r] = "CH " + CHANNELS[c] + " RACK " + (r + 1);
			}
		}
	}

	private RelayRoom() {
	}

	/** Which kind a block is ({@link #PROTECTION} to {@link #RELAY}), or -1 for anything else. */
	public static int kind(BlockState state) {
		for (int k = 0; k < KEYS.length; k++) {
			if (state.getBlock() == PlantInterior.BLOCKS.get(KEYS[k])) {
				return k;
			}
		}
		return -1;
	}

	public static String[] labels(int kind) {
		return switch (kind) {
			case PROTECTION -> PROTECTION_LABELS;
			case SSPS -> SSPS_LABELS;
			case NIS -> NIS_LABELS;
			default -> RELAY_LABELS;
		};
	}

	/** A cabinet's name, by its kind and variant. */
	public static String label(int kind, int variant) {
		String[] all = labels(kind);
		return all[Math.floorMod(variant, all.length)];
	}

	/**
	 * What a relay room cabinet reads of its reactor (synced once a second): 0 none in range, 1 running, 2 shut down, 3
	 * tripped; its fission and heat (fractions of rated), core temperature (the game's degrees: 300 normal), rods (100
	 * all in), alarms (ReactorCore.A_*), whether its protection is on, and the startup rate (decades a minute).
	 */
	public record Reactor(int status, double fission, double heat, double temperature, double rods, int alarms, boolean protection, double sur) {
		public static final Reactor NONE = new Reactor(0, 0, 0, 30, 100, 0, true, 0);

		public static Reactor of(ReactorCore core, double sur) {
			int status = core.scrammed ? 3 : core.shutDown() ? 2 : 1;
			return new Reactor(status, core.fission, core.heat(), core.temperature, core.rods, core.alarms, core.protection, sur);
		}

		boolean present() {
			return status != 0;
		}

		/** The loops' average temperature (deg F): the core's in the game's degrees, read as Celsius. */
		public double tavgF() {
			return present() ? temperature * 9 / 5 + 32 : 0;
		}

		/** The pressurizer's pressure (psig): 400 cold, 2235 at no-load temperature and above, rising past the alarm. */
		public double pzrPressure() {
			if (!present()) {
				return 0;
			}
			double p = 400 + (2235 - 400) * Math.max(0, Math.min(1, (temperature - 30) / 250));
			return temperature > ReactorCore.NORMAL_TEMP ? p + (temperature - ReactorCore.NORMAL_TEMP) * 4 : p;
		}

		/** The pressurizer's level (%): its programme, 25 at no load to 60 at full power. */
		public double pzrLevel() {
			return present() ? 25 + 35 * Math.max(0, Math.min(1, heat)) : 0;
		}

		/** The steam generators' narrow range level (%): 44, or 10 with too little water. */
		public double sgLevel() {
			return !present() ? 0 : (alarms & ReactorCore.A_LOW_WATER) != 0 ? 10 : 44;
		}

		/** Reactor coolant flow (%): 100, 60 with a loop out. */
		public double rcsFlow() {
			return !present() ? 0 : (alarms & ReactorCore.A_LOOPS) != 0 ? 60 : 100;
		}

		/** Source range (counts a second, the detectors' high voltage off above P-10), intermediate range (amps). */
		public double srCps() {
			return present() ? 20 + 1e10 * fission : 0;
		}

		public double irAmps() {
			return present() ? 1e-11 + 1e-3 * fission : 0;
		}

		public boolean p6() {
			return irAmps() > 1e-10;
		}

		public boolean p10() {
			return fission > 0.10;
		}
	}

	/** A cabinet's readings: its two meters (0 to 100 of their scales) and its lamps (bit k: rr_l&lt;k&gt;). */
	public record Readings(double m0, double m1, int lamps) {
	}

	// The protection racks' bistables (lit tripped), by rack: as tools/relay_room.py PROTECTION_RACK_KINDS.
	static boolean[] bistables(int rack, Reactor r) {
		if (!r.present()) {
			return new boolean[4];
		}
		double p = r.pzrPressure();
		return switch (rack) {
			case 0 -> new boolean[] {r.temperature >= ReactorCore.ALARM_TEMP || (r.alarms & ReactorCore.A_HIGH_TEMP) != 0, r.heat > 1.09,
					r.tavgF() < 553, r.fission > 1.09};
			case 1 -> new boolean[] {p < 1865, p > 2385, r.pzrLevel() > 92, p < 1715};
			default -> new boolean[] {(r.alarms & ReactorCore.A_LOW_WATER) != 0, false, (r.alarms & ReactorCore.A_LOOPS) != 0, false};
		};
	}

	private static double scale(double v, double lo, double hi) {
		return (v - lo) / (hi - lo) * 100;
	}

	private static double log(double v, double lo, double decades) {
		return v <= 0 ? -5 : (Math.log10(v) - lo) / decades * 100;
	}

	/** What cabinet {@code kind}, variant {@code variant}, shows of reactor {@code r}. */
	public static Readings readings(int kind, int variant, Reactor r) {
		switch (kind) {
			case PROTECTION -> {
				int rack = Math.floorMod(variant, 3);
				double m0, m1;
				if (rack == 0) {
					m0 = scale(r.tavgF(), 530, 630);
					m1 = scale(r.present() ? r.heat * 100 : 0, 0, 150);
				} else if (rack == 1) {
					m0 = scale(r.pzrPressure(), 1700, 2500);
					m1 = r.pzrLevel();
				} else {
					m0 = r.sgLevel();
					m1 = scale(r.rcsFlow(), 0, 120);
				}
				boolean[] b = bistables(rack, r);
				int lamps = 1 << 4;                       // the rack's power, from its vital bus
				for (int k = 0; k < 4; k++) {
					lamps |= b[k] ? 1 << k : 0;
				}
				return new Readings(m0, m1, lamps);
			}
			case SSPS -> {
				int bay = Math.floorMod(variant, 3);
				return new Readings(0, 0, ssps(bay, r) | 1 << 17);
			}
			case NIS -> {
				return nis(Math.floorMod(variant, NIS_LABELS.length), r);
			}
			default -> {
				return new Readings(0, 0, 0);
			}
		}
	}

	/** An SSPS bay's windows (bits 0 to 15, row by row) and its left lamp (bit 16). */
	static int ssps(int bay, Reactor r) {
		if (!r.present()) {
			return 0;
		}
		boolean trip = r.status == 3;
		boolean[] b0 = bistables(0, r), b1 = bistables(1, r), b2 = bistables(2, r);
		boolean si = b1[3] && r.temperature > 200;
		boolean fwIsol = trip && r.tavgF() < 564;
		boolean afw = b2[0] || trip;
		int out = 0;
		switch (bay) {
			case 0 -> {
				// A row a function, a column a channel: every channel sees the same plant.
				boolean[] rows = {b1[0], b0[0], b2[0], b2[2]};
				for (int row = 0; row < 4; row++) {
					out |= rows[row] ? 0xF << row * 4 : 0;
				}
			}
			case 1 -> {
				boolean[] w = {r.status >= 2, r.p6(), r.fission > 0.10, r.fission > 0.48, r.p10(), r.pzrPressure() > 1915, r.tavgF() < 553,
						r.status == 1 && r.heat > 0.10, trip, si, fwIsol, false, si, false, afw, trip};
				for (int k = 0; k < 16; k++) {
					out |= w[k] ? 1 << k : 0;
				}
				// GENERAL WARNING: a train in test or bypassed (the console's protection off).
				out |= r.protection ? 0 : 1 << 16;
			}
			default -> {
				boolean[] w = {trip, si, si, fwIsol, fwIsol, false, si, false, false, afw, b2[0], trip, si, si, trip, false};
				for (int k = 0; k < 16; k++) {
					out |= w[k] ? 1 << k : 0;
				}
			}
		}
		return out;
	}

	/** A nuclear instrument rack's two drawers: their meters and lamps (green, red, amber each). */
	static Readings nis(int rack, Reactor r) {
		if (!r.present()) {
			return new Readings(-5, -5, 0);
		}
		switch (rack) {
			case 0 -> {
				// Source range: its high voltage off above P-10 (the needle down, the channel blocked).
				boolean hv = !r.p10();
				double m = hv ? log(r.srCps(), 0, 6) : -5;
				boolean high = hv && r.srCps() > 1e5;
				int one = (hv ? 1 : 0) | (high ? 2 : 0) | (hv ? 0 : 4);
				return new Readings(m, m, one | one << 3);
			}
			case 1 -> {
				double m = log(r.irAmps(), -11, 8);
				int one = 1 | (r.fission > 1.09 ? 2 : 0) | (r.fission > 1.03 ? 4 : 0);
				return new Readings(m, m, one | one << 3);
			}
			case 2, 3 -> {
				double m = scale(r.fission * 100, 0, 120);
				int one = 1 | (r.fission > 1.09 ? 2 : 0) | (r.fission > 1.03 ? 4 : 0);
				return new Readings(m, m, one | one << 3);
			}
			default -> {
				// Comparator (the channels agree: the middle) and the startup rate (-1 to 5 decades a minute).
				double sur = Math.max(-1.05, Math.min(5.05, r.sur));
				int lamps = 1 | 1 << 3 | (r.sur > 1.0 ? 1 << 5 : 0);
				return new Readings(50, scale(sur, -1, 5), lamps);
			}
		}
	}

	/** The startup rate (decades a minute) between two fission readings a second apart. */
	public static double startupRate(double before, double now) {
		double a = Math.max(before, 1e-9), b = Math.max(now, 1e-9);
		return Math.log10(b / a) * 60;
	}

	/** A cabinet's name and what it reads, for its use message. */
	public static String describe(int kind, int variant, Reactor r) {
		Readings d = readings(kind, variant, r);
		return switch (kind) {
			case PROTECTION -> String.format(Locale.ROOT, "%s: %d bistable(s) tripped", label(kind, variant), Integer.bitCount(d.lamps() & 0xF));
			case SSPS -> String.format(Locale.ROOT, "SSPS %s: %d window(s) lit", label(kind, variant), Integer.bitCount(d.lamps() & 0xFFFF));
			case NIS -> String.format(Locale.ROOT, "NIS %s: %.0f%% power", label(kind, variant), r.fission * 100);
			default -> label(kind, variant);
		};
	}
}
