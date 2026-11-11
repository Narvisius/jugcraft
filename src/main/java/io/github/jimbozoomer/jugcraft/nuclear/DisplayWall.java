package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The main control room's plant display wall (round 32; Justin: "Display wall live", "Overview + safety", "Alarm banner,
 * no horn", "Fixed screens", "Rear-projection cubes"; tools/display_wall.py draws it). It is a live board
 * ({@link ControlBoard}): it watches the plant as the boards do (its snapshot, its trends), and once each half second it
 * gathers the boards' alarms in its room: every window in alarm, newest first, for its banner (flashing until
 * acknowledged at its board, slowly once cleared until reset; it never sounds the horn itself). Both sides work out
 * the rest from the snapshot: the critical safety functions' colours (Westinghouse status trees), the plant mimic's
 * lines and symbols ({@link #signal}) and every figure on it ({@link #text}).
 */
public final class DisplayWall {
	/** How far the wall looks for the boards whose alarms it shows (blocks): its own control room. */
	public static final int REACH = 32;
	/** The banner's slots. */
	public static final int SLOTS = 6;
	/** The boards whose alarms it shows, and their names on the banner. */
	static final EquipmentKind[] BOARDS = {EquipmentKind.ESF_BOARD, EquipmentKind.RCS_BOARD, EquipmentKind.SECONDARY_BOARD,
			EquipmentKind.TURBINE_BOARD, EquipmentKind.ELECTRICAL_BOARD, EquipmentKind.RADIATION_BOARD, EquipmentKind.CONTROL_CONSOLE};
	static final String[] BOARD_NAMES = {"ESF", "RCS", "SECONDARY", "TURBINE", "ELECTRICAL", "RADIATION", "RX CONTROL"};
	/** The safety functions in their order of priority, their colours' names and the procedure each colour calls for. */
	public static final String[] CSF_LETTERS = {"S", "C", "H", "P", "Z", "I"};
	public static final String[] CSF_NAMES = {"SUBCRITICALITY", "CORE COOLING", "HEAT SINK", "INTEGRITY", "CONTAINMENT", "INVENTORY"};
	public static final String[] CSF_STATES = {"SATISFIED", "NOT SATISFIED", "SEVERE CHALLENGE", "EXTREME CHALLENGE"};
	static final String[][] PROCEDURES = {{"", "FR-S.2", "FR-S.1", "FR-S.1"}, {"", "FR-C.3", "FR-C.2", "FR-C.1"}, {"", "FR-H.3", "FR-H.2", "FR-H.1"},
			{"", "FR-P.2", "FR-P.1", "FR-P.1"}, {"", "FR-Z.3", "FR-Z.2", "FR-Z.1"}, {"", "FR-I.2", "FR-I.1", "FR-I.1"}};
	/** Colours (ARGB): the figures' white, out of their band amber and red, in band green; labels' grey; the tiles' ink. */
	public static final int WHITE = 0xFFECF0F6, AMBER = 0xFFFFC440, RED = 0xFFFF6050, GREEN = 0xFF6EE68C, GREY = 0xFFA8B0BC, CYAN = 0xFF78C6EE,
			INK = 0xFF0C0E10;

	private DisplayWall() {
	}

	// ------------------------------------------------------------------ the banner (server)

	/** An alarm as the banner keeps it: the board's kind's ordinal, the window's index, its state (packed). */
	public static int pack(EquipmentKind kind, int window, byte state) {
		return kind.ordinal() << 20 | window << 4 | state;
	}

	public static EquipmentKind kind(int packed) {
		return EquipmentKind.values()[packed >>> 20];
	}

	public static int window(int packed) {
		return packed >>> 4 & 0xFFFF;
	}

	public static byte state(int packed) {
		return (byte) (packed & 15);
	}

	/** The banner's name of a board. */
	public static String boardName(EquipmentKind kind) {
		for (int i = 0; i < BOARDS.length; i++) {
			if (BOARDS[i] == kind) {
				return BOARD_NAMES[i];
			}
		}
		return kind.id.toUpperCase(Locale.ROOT);
	}

	/**
	 * Gathers the alarms of the boards in the wall's room: each window not clear, newest first (when the wall first saw
	 * it), the first {@link #SLOTS} kept with the count of all and of those not yet acknowledged. Whether anything shown
	 * changed.
	 */
	static boolean alarms(ServerLevel level, EquipmentBlockEntity wall, ControlBoard.State s, long now) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (s.wallBoards == null || registry.version() != s.wallSearch) {
			s.wallSearch = registry.version();
			List<BlockPos> found = new ArrayList<>();
			for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : registry.anchors().entrySet()) {
				EquipmentKind k = e.getValue().kind();
				if (List.of(BOARDS).contains(k) && e.getKey().distSqr(wall.getBlockPos()) <= (double) REACH * REACH) {
					found.add(e.getKey().immutable());
				}
			}
			s.wallBoards = found;
		}
		Map<Integer, Long> seen = new HashMap<>();
		List<int[]> active = new ArrayList<>();
		int unacked = 0;
		for (BlockPos at : s.wallBoards) {
			if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof EquipmentBlockEntity board) || !board.kind().liveBoard()) {
				continue;
			}
			byte[] windows = board.board().windows;
			for (int i = 0; i < windows.length; i++) {
				if (windows[i] == ControlBoard.CLEAR) {
					continue;
				}
				int key = board.kind().ordinal() << 16 | i;
				long since = s.wallSince.getOrDefault(key, now);
				seen.put(key, since);
				active.add(new int[] {pack(board.kind(), i, windows[i]), (int) Math.min(Integer.MAX_VALUE, now - since), key});
				if (windows[i] == ControlBoard.ALERT) {
					unacked++;
				}
			}
		}
		s.wallSince.clear();
		s.wallSince.putAll(seen);
		// Newest first (the least time since it came in), then by board and window.
		active.sort((a, b) -> a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[2], b[2]));
		int[] shown = new int[Math.min(SLOTS, active.size())];
		for (int i = 0; i < shown.length; i++) {
			shown[i] = active.get(i)[0];
		}
		boolean changed = !java.util.Arrays.equals(shown, s.wallAlarms) || s.wallTotal != active.size() || s.wallNew != unacked;
		s.wallAlarms = shown;
		s.wallTotal = active.size();
		s.wallNew = unacked;
		return changed;
	}

	// ------------------------------------------------------------------ the mimic's lines (both sides)

	/** The mimic's own lights (tools/display_wall.py "dw." names): which lines carry flow, the core, the buses. */
	public static boolean signal(BoardPlant p, String name, double time) {
		boolean on = p.present;
		if (name.length() == 9 && name.startsWith("dw.steam") && Character.isDigit(name.charAt(8))) {
			int k = name.charAt(8) - '1';
			return on && k >= 0 && k < 4 && p.sg(k) && p.hot();
		}
		if (name.length() == 13 && name.startsWith("dw.steam_hdr") && Character.isDigit(name.charAt(12))) {
			int k = name.charAt(12) - '1';
			return on && k >= 0 && k < 4 && p.sg(k) && p.hot() && p.msivOpen(k);
		}
		return switch (name) {
			case "dw.rcs_flow" -> {
				boolean any = false;
				for (int k = 0; k < Math.min(4, p.loops); k++) {
					any |= p.loopFlow[k] > 0.05;
				}
				yield on && p.pumps && any;
			}
			case "dw.steam_hdr" -> steamHeader(p);
			case "dw.turbine" -> p.latched() && steamHeader(p);
			case "dw.feed_pumps", "dw.feed_hdr" -> on && p.mainFeed();
			case "dw.condensate" -> on;
			case "dw.critical" -> on && !p.scrammed && p.prAverage() > 1;
			case "dw.bus1a", "dw.bus1b" -> p.value(name.equals("dw.bus1a") ? "bus1a.kv" : "bus1b.kv", time) > 1;
			default -> false;
		};
	}

	private static boolean steamHeader(BoardPlant p) {
		for (int k = 0; k < 4; k++) {
			if (p.present && p.sg(k) && p.hot() && p.msivOpen(k)) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ the safety functions (both sides)

	/** The core exit thermocouples' average (deg F): a little over Tavg, more with the core's heat (as the TSC's). */
	public static double coreExit(BoardPlant p) {
		return p.present ? p.tavg() + 6 + 30 * Math.max(0, p.heat) : 0;
	}

	/** Saturation at the pressurizer's pressure (a fit to the steam tables) less the core exit temperature (deg F). */
	public static double subcooling(BoardPlant p) {
		if (!p.present) {
			return 0;
		}
		double tsat = 212 + 441 * Math.pow(Math.max(0, p.pzrPress() + 14.7) / 2250, 0.32);
		return tsat - coreExit(p);
	}

	/**
	 * A critical safety function's colour (0 green, 1 yellow, 2 orange, 3 red), by the Westinghouse status trees as the
	 * game's plant can show them: subcriticality (after a trip, power range over 5 % red, the startup rate positive
	 * orange); core cooling (core exit over 1200 deg F red, over 700 orange, saturated yellow); heat sink (hot with no
	 * steam generator over 11 % narrow range: under 445 gpm of feed red, else yellow; one on its relief valve orange; one
	 * dry yellow); integrity (pressurizer over 2735 psig red, over 2485 orange, cold and pressurized orange); containment
	 * (pressure over 50 psig red, over 20 or damaged fuel orange, over 3.5 yellow); inventory (the loops losing water
	 * orange, the pressurizer's level under 17 % or over 92 % yellow).
	 */
	public static int csf(BoardPlant p, int k, double time) {
		if (!p.present) {
			return 0;
		}
		switch (k) {
			case 0 -> {
				if (!p.scrammed) {
					return 0;
				}
				return p.prAverage() > 5 ? 3 : p.sur > 0.05 ? 2 : 0;
			}
			case 1 -> {
				double cet = coreExit(p);
				return cet > 1200 ? 3 : cet > 700 ? 2 : subcooling(p) < 0 ? 1 : 0;
			}
			case 2 -> {
				if (!p.hot()) {
					return 0;
				}
				int wet = 0, dry = 0;
				boolean relief = false;
				for (int s = 0; s < 4; s++) {
					if (!p.sg(s)) {
						continue;
					}
					if (p.nr(s) > 0.11) {
						wet++;
					} else {
						dry++;
					}
					relief |= p.arvOpen(s) && p.sgPress(s) >= 1125;
				}
				double feed = p.value("afw.total", time) + (p.mainFeed() ? 1000 : 0);
				if (wet == 0) {
					return feed < 445 ? 3 : 1;
				}
				return relief ? 2 : dry > 0 ? 1 : 0;
			}
			case 3 -> {
				double press = p.pzrPress();
				double tcold = p.value("loop1.tcold", time);
				return press > 2735 ? 3 : press > 2485 || tcold < 250 && press > 500 ? 2 : 0;
			}
			case 4 -> {
				double press = p.value("cnt.press", time);
				return press > 50 ? 3 : press > 20 || (p.alarms & ReactorCore.A_FUEL_DAMAGED) != 0 ? 2 : press > 3.5 ? 1 : 0;
			}
			default -> {
				double level = p.pzrLevel();
				return (p.alarms & ReactorCore.A_LOOPS) != 0 ? 2 : level < 17 || level > 92 ? 1 : 0;
			}
		}
	}

	/** The plant's mode (Westinghouse technical specifications), as the operations office shows it. */
	public static String mode(BoardPlant p) {
		if (!p.present) {
			return "NO REACTOR";
		}
		if (p.refuel) {
			return "MODE 6  REFUELING";
		}
		if (!p.scrammed && p.fission > 0.0001) {
			return p.fission * 100 >= 5 ? "MODE 1  POWER OPERATION" : "MODE 2  STARTUP";
		}
		double f = p.tavg();
		return f >= 350 ? "MODE 3  HOT STANDBY" : f >= 200 ? "MODE 4  HOT SHUTDOWN" : "MODE 5  COLD SHUTDOWN";
	}

	static String tripCause(BoardPlant p) {
		return switch (p.tripCause) {
			case ReactorCore.TRIP_OTDT -> "OT DELTA-T";
			case ReactorCore.TRIP_FLOW1 -> "LO FLOW 1 LOOP";
			case ReactorCore.TRIP_FLOW2 -> "LO FLOW 2 LOOPS";
			case ReactorCore.TRIP_TURBINE -> "TURBINE TRIP";
			case ReactorCore.TRIP_SI -> "SAFETY INJECTION";
			case ReactorCore.TRIP_SG_LO -> "SG LO-LO LEVEL";
			default -> "MANUAL TRIP";
		};
	}

	// ------------------------------------------------------------------ the figures (both sides)

	/** Words the wall writes: what, and its colour (0 the place's own). */
	public record Text(String text, int colour) {
	}

	private static Text t(String s) {
		return new Text(s, 0);
	}

	private static Text t(String s, int colour) {
		return new Text(s, colour);
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}

	/** A figure's colour: white in its band, amber past its alarm, red past its trip (lo or hi; NaN none). */
	private static int band(double v, double redLo, double amberLo, double amberHi, double redHi) {
		return v < redLo || v > redHi ? RED : v < amberLo || v > amberHi ? AMBER : WHITE;
	}

	/**
	 * What the wall writes in each place (tools/display_wall.py "texts"): s the wall's state (its banner), dayTime the
	 * world's time of day (its clock).
	 */
	public static Text text(String id, BoardPlant p, ControlBoard.State s, double time, long dayTime) {
		boolean on = p.present;
		String dash = "----";
		if (id.startsWith("dw.csf_letter")) {
			return t(CSF_LETTERS[id.charAt(13) - '0']);
		}
		if (id.startsWith("dw.csf_name")) {
			return t(CSF_NAMES[id.charAt(11) - '0']);
		}
		if (id.startsWith("dw.csf_state")) {
			return t(CSF_STATES[csf(p, id.charAt(12) - '0', time)]);
		}
		if (id.startsWith("dw.loop") && id.length() > 9) {
			int k = id.charAt(7) - '1';
			if (!on || k >= p.loops) {
				return t("--", GREY);
			}
			return switch (id.substring(9)) {
				case "flow" -> {
					double v = p.value("loop" + (k + 1) + ".flow", time);
					yield t(f0(v) + "%", v < 90 ? AMBER : WHITE);
				}
				case "thot" -> t(f0(p.value("loop" + (k + 1) + ".thot", time)), WHITE);
				default -> t(f0(p.value("loop" + (k + 1) + ".tcold", time)), WHITE);
			};
		}
		if (id.startsWith("dw.sg") && id.endsWith("_level")) {
			int k = id.charAt(5) - '1';
			if (!on || !p.sg(k)) {
				return t("--", GREY);
			}
			double v = p.nr(k) * 100;
			return t(f0(v) + "%", v < 11 ? RED : v > 69 ? AMBER : WHITE);
		}
		if (id.startsWith("dw.trend")) {
			return switch (id.charAt(8)) {
				case '0' -> t(on ? f1(p.prAverage()) : dash);
				case '1' -> t(on ? f0(p.pzrPress()) : dash);
				default -> t(on ? f0(p.tavg()) : dash);
			};
		}
		return switch (id) {
			case "dw.alarm_count" -> s.wallTotal == 0 ? t("NO ACTIVE ALARMS", GREEN)
					: t(s.wallTotal + " ACTIVE  " + s.wallNew + " NEW", s.wallNew > 0 ? AMBER : WHITE);
			case "dw.csf_priority" -> {
				if (!on) {
					yield t("NO REACTOR", GREY);
				}
				int worst = 0, which = -1;
				for (int k = 0; k < 6; k++) {
					int c = csf(p, k, time);
					if (c > worst) {
						worst = c;
						which = k;
					}
				}
				yield switch (worst) {
					case 0 -> t("ALL FUNCTIONS SATISFIED", GREEN);
					case 1 -> t("YELLOW  " + CSF_LETTERS[which] + "  SEE " + PROCEDURES[which][1], 0xFFEED636);
					case 2 -> t("ORANGE PATH  " + CSF_LETTERS[which] + "  GO TO " + PROCEDURES[which][2], 0xFFFFA03C);
					default -> t("RED PATH  " + CSF_LETTERS[which] + "  GO TO " + PROCEDURES[which][3], RED);
				};
			}
			// The key parameters.
			case "dw.power" -> on ? t(f1(p.prAverage()), WHITE) : t(dash, GREY);
			case "dw.tavg" -> on ? t(f0(p.tavg()) + " / " + f0(p.tref()), Math.abs(p.tavg() - p.tref()) > 15 ? RED : Math.abs(p.tavg() - p.tref()) > 5 ? AMBER : WHITE)
					: t(dash, GREY);
			case "dw.pzr_press" -> on ? t(f0(p.pzrPress()), band(p.pzrPress(), 1865, 2185, 2285, 2385)) : t(dash, GREY);
			case "dw.pzr_level" -> on ? t(f1(p.pzrLevel()), band(p.pzrLevel(), 17, 22, 70, 92)) : t(dash, GREY);
			case "dw.subcool" -> on ? t(f0(subcooling(p)), band(subcooling(p), 0, 20, 1e9, 1e9)) : t(dash, GREY);
			case "dw.cet" -> on ? t(f0(coreExit(p)), band(coreExit(p), -1e9, -1e9, 650, 700)) : t(dash, GREY);
			case "dw.cnt_press" -> on ? t(f1(p.value("cnt.press", time)), band(p.value("cnt.press", time), -1e9, -1e9, 1.5, 3.5)) : t(dash, GREY);
			case "dw.rwst" -> t(f1(p.rwstLevel()), band(p.rwstLevel(), 10, 30, 1e9, 1e9));
			case "dw.mode" -> t(mode(p), CYAN);
			case "dw.clock" -> t(TscRoom.clock(dayTime), CYAN);
			// The mimic.
			case "dw.core_power" -> on ? t(f0(p.prAverage()) + "%", p.scrammed || p.prAverage() < 1 ? 0xFFC8B4A0 : 0) : t("", 0);
			case "dw.steam_press" -> {
				double v = p.value("stm.hdr", time);
				yield on ? t(f0(v) + " PSIG", p.hot() && v < 900 ? AMBER : WHITE) : t(dash, GREY);
			}
			case "dw.turb_speed" -> on ? t(f0(p.value("turb.speed", time)) + " RPM", WHITE) : t(dash, GREY);
			case "dw.gen_mw" -> on ? t(f0(p.value("gen.mw", time)) + " MWE", WHITE) : t(dash, GREY);
			case "dw.gen_g" -> t("G");
			case "dw.vacuum" -> on ? t(f1(30 - p.value("turb.vacuum", time)) + " IN HGA", p.value("turb.vacuum", time) < 25 ? AMBER : WHITE) : t(dash, GREY);
			case "dw.afw_a" -> t(f0(p.value("afw_a.flow", time)) + " GPM", WHITE);
			case "dw.afw_b" -> t(f0(p.value("afw_b.flow", time)) + " GPM", WHITE);
			case "dw.afw_td" -> t(f0(p.value("afw_td.flow", time)) + " GPM", WHITE);
			case "dw.afw_total" -> t(f0(p.value("afw.total", time)), WHITE);
			case "dw.cst" -> t(f0(p.cstLevel()) + "%", band(p.cstLevel(), 10, 30, 1e9, 1e9));
			case "dw.pzr_p" -> on ? t(f0(p.pzrPress()) + " PSIG", band(p.pzrPress(), 1865, 2185, 2285, 2385)) : t(dash, GREY);
			case "dw.pzr_l" -> on ? t(f0(p.pzrLevel()) + " %", band(p.pzrLevel(), 17, 22, 70, 92)) : t(dash, GREY);
			case "dw.si_state" -> !on ? t(dash, GREY) : p.siOn() ? t(p.siResettable(time) ? "SI  RESET PERMITTED" : "SI ACTUATED", AMBER) : t("NORMAL", GREEN);
			case "dw.rx_state" -> !on ? t("NO REACTOR", GREY) : p.scrammed ? t("TRIPPED", RED) : p.fission > 0.0001 ? t(p.fission * 100 >= 5 ? "AT POWER" : "CRITICAL", WHITE)
					: t("SHUT DOWN", WHITE);
			case "dw.rx_cause" -> !on ? t("", 0) : p.scrammed ? t(tripCause(p) + (p.siOn() ? "  SI " + TscRoom.span((long) (time - p.siTime)) : ""), AMBER)
					: t((p.rodAuto ? "RODS IN AUTO" : "RODS IN MANUAL") + "  BANK D " + f0(p.bankSteps(7)), GREY);
			// The electrical lineup.
			case "dw.swyd_kv" -> t(f0(p.value("swyd.kv", time)) + " KV", WHITE);
			case "dw.gen_hz" -> !on ? t(dash, GREY) : p.online() ? t(String.format(Locale.ROOT, "%.2f HZ", p.value("gen.hz", time)), WHITE) : t("OFF LINE", GREY);
			case "dw.bus1a_kv", "dw.bus1b_kv" -> {
				double v = p.value(id.equals("dw.bus1a_kv") ? "bus1a.kv" : "bus1b.kv", time);
				yield t(String.format(Locale.ROOT, "%.2f KV", v), v > 1 ? WHITE : RED);
			}
			case "dw.edg_a_kw", "dw.edg_b_kw" -> {
				int k = id.charAt(7) - 'a';
				BoardPlant.Edg e = p.edg[k];
				yield !e.present ? t("NOT FOUND", GREY) : e.running ? t("RUN  " + f0(p.value(k == 0 ? "edg_a.kw" : "edg_b.kw", time)) + " KW", AMBER)
						: p.edgReady(k) ? t("READY", GREEN) : t("NOT READY", AMBER);
			}
			case "dw.lc" -> !p.lc ? t("NOT FOUND", GREY) : p.lcCharge > 0.02 ? t("480 V", WHITE) : t("0 V", RED);
			case "dw.mcc_a", "dw.mcc_b" -> {
				int k = id.charAt(7) - 'a';
				yield !p.mcc[k] ? t("NOT FOUND", GREY) : p.mccCharge[k] > 0.02 ? t("480 V", WHITE) : t("0 V", RED);
			}
			case "dw.dc_a" -> t(f1(p.value("dc_a.volts", time)) + " V", p.value("dc_a.volts", time) < 125 ? AMBER : WHITE);
			case "dw.dc_b" -> t(f1(p.value("dc_b.volts", time)) + " V", p.value("dc_b.volts", time) < 125 ? AMBER : WHITE);
			case "dw.vital" -> on ? t("120 V", WHITE) : t("0 V", GREY);
			default -> t("");
		};
	}

	/** What a click on the wall says: the reactor's state (as the wall writes it) and the alarms on its banner. */
	public static net.minecraft.network.chat.Component status(ControlBoard.State s) {
		BoardPlant p = s.plant;
		String rx = !p.present ? "NO REACTOR" : p.scrammed ? "REACTOR TRIPPED, " + tripCause(p) : mode(p);
		return net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.display_wall", rx, s.wallTotal, s.wallNew);
	}
}
