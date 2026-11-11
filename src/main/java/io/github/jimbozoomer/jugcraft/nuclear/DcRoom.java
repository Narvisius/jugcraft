package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The control building's DC equipment rooms working together (older rooms round 3; Justin, 15 October 2026: the
 * switchboard "Real 3D, breakers work"; "Charger switches", "Inverter bypass switch", "Annunciator buttons", "Log desk
 * laptop"; the feeders "Cut what it feeds"; the equalize "24 game hours"; the alarms "Lit until RESET").
 *
 * <p>The 125 VDC switchboard's bus: its main breaker 72-1A (bit {@link #MAIN} of its breakers) puts the station battery
 * on it, and the chargers whose AC and DC breakers are closed and whose supply is on feed it; it is alive while either
 * does ({@link #BUS}). Its twelve feeders (bits 1 to 12) each carry what they feed, and what each feeds goes dead with
 * it: feeders 01 and 02 the vital inverters (on DC lost, the static switch puts their load on the bypass), 03 and 04 the
 * DC distribution panels, 05 the train's 480 V load centre's control power (its breakers can't be worked electrically,
 * its lamps go dark: LoadCentre), 07 its diesel generator's control power (it won't start: DieselPanels). The others
 * show on the laptop. The chargers charge the battery only through a closed main.
 *
 * <p>A charger's MODE selector: FLOAT, or EQUALIZE, which raises the bus to 139.8 V and runs the EQ TIMER down from 24
 * game hours (a game day), then goes back to FLOAT by itself. The chargers' and inverters' six-window annunciators
 * latch (ISA sequence, Justin's "Lit until RESET"): a new alarm flashes until ACK, then stays lit; once it has cleared
 * it stays lit until RESET; TEST lights every window three seconds.
 *
 * <p>The switchboard keeps what it found of its room (its chargers, inverters and distribution panels by number:
 * LivePanelBlockEntity dcParts) and its state ({@link #BUS}, {@link #BATT}, {@link #CHARGING}, {@link #EQUAL}), both
 * synced, so the clients read the room from it (client/EdgTelemetry.java). The load centres and diesels, further off,
 * read it from a list the switchboards keep a second at a time ({@link #feederLive}); a plant with no switchboard
 * near them (one built before these rooms, a lineup put down alone) keeps its control power.
 */
public final class DcRoom {
	public static final String SWITCHBOARD = "dc_switchboard", CHARGER = "station_charger", INVERTER = "vital_inverter", PANEL = "dc_dist_panel",
			LAPTOP = "dc_desk_laptop", DETECTOR = "dc_ground_detector", DISCONNECT = "battery_disconnect";
	/** A battery room's string disconnect switch (older rooms round 4: BatteryDisconnectBlock; LivePanelBlockEntity mode). */
	public static final int CLOSED = 0, OPEN = 1;
	/** The switchboard's breakers: bit 0 the main, bit k feeder 72-1A-k (1 to 12). */
	public static final int MAIN = 0, INV1 = 1, INV2 = 2, PNL1 = 3, PNL2 = 4, LC_CTL = 5, SWGR_CTL = 6, EDG_CTL = 7, FEEDERS = 12;
	/** As built: the main and feeders 01 to 11 closed, the spare (12) open. */
	public static final int SB_DEFAULT = (1 << FEEDERS) - 1;
	/** The switchboard's state: the bus alive, the battery on it (main closed, charged), a charger feeding it, one equalizing. */
	public static final int BUS = 1, BATT = 2, CHARGING = 4, EQUAL = 8;
	/** A charger's or inverter's breakers (LivePanelBlockEntity dcFlags): set open. */
	public static final int AC_OPEN = 1, DC_OPEN = 2;
	/** A charger's MODE (LivePanelBlockEntity mode), an inverter's manual bypass switch. */
	public static final int FLOAT = 0, EQUALIZE = 1, NORMAL = 0, BYPASS = 1;
	/** The equalize charge (game ticks: 24 game hours), TEST (ticks). */
	public static final long EQ_TICKS = 24_000, TICKS_PER_HOUR = 1_000;
	public static final int TEST_TICKS = 60;
	/** How long a charger or inverter settles after it is built or loaded before its annunciator latches (game ticks). */
	public static final int SETTLE = 60;
	/** The charger's windows (tools/plant_panels.py SC_WINDOWS) and the inverter's (VI_WINDOWS), by bit. */
	public static final int C_AC_FAIL = 0, C_DC_LOW = 1, C_DC_HIGH = 2, C_CHGR_FAIL = 3, C_GND = 4, C_EQUAL = 5;
	public static final int I_FAIL = 0, I_LOW_DC = 1, I_BYP_FAIL = 2, I_ON_BYP = 3, I_SYNC = 4, I_FAN = 5;
	public static final int WINDOWS = 6;
	/** The bus's alarm levels (V). */
	public static final double DC_LOW = 124, DC_HIGH = 142, INV_LOW_DC = 112;
	/** The bus's volts: floating, equalizing. */
	public static final double FLOAT_V = 132, EQUAL_V = 139.8;
	/** How far a load centre or diesel generator looks for its train's switchboard (blocks). */
	public static final int LOOK = 256;
	/** A switchboard's room parts (LivePanelBlockEntity dcParts): charger 1 and 2, inverter 1 and 2, panel 1 and 2. */
	public static final int PARTS = 6;
	/**
	 * What the feeders 05 to 12 carry (amps: control power, the turbine's or AFW pump's controls, the emergency
	 * lighting's charge, the fuel transfer's, the UPS on standby); by train, A then B.
	 */
	static final int[][] FEEDER_AMPS = {{4, 6, 3, 0, 2, 2, 0, 0}, {4, 6, 3, 2, 2, 1, 0, 0}};
	/** A DC distribution panel's circuits' loads (amps), panel -1 and -2 (tools/dc_room.py dp_circuits). */
	static final int[][] PANEL_LOADS = {{3, 3, 2, 2, 2, 2, 3, 2, 1, 1, 0, 0}, {2, 2, 2, 2, 2, 2, 1, 2, 1, 1, 0, 0}};
	/** The panels' circuits (tools/dc_room.py dp_circuits), %s the train. */
	static final String[][] PANEL_NAMES = {{"MCR BOARDS TRAIN %s", "RX TRIP SWGR CONTROL %s", "SSPS LOGIC TRAIN %s", "RELAY ROOM CAB %s",
			"RELAY ROOM CAB %s", "REMOTE SHUTDOWN PANEL %s", "ESF LOAD SEQUENCER %s", "ANNUNCIATOR TRAIN %s", "CR HVAC CONTROL %s",
			"FIRE DETECTION PANEL %s", "SPARE", "SPARE"}, {"AFW VALVES TRAIN %s", "MSIV SOLENOIDS TRAIN %s", "PORV BLOCK VALVES %s",
			"CCW VALVES TRAIN %s", "SERVICE WATER VALVES %s", "CONT ISOLATION VALVES %s", "EDG 1%s FIELD FLASH", "TURB BLDG EMERG LIGHTING",
			"SECURITY SYSTEM", "PLANT PAGE SYSTEM", "SPARE", "SPARE"}};
	/** The relay room cabinets the first panel's circuits 4 and 5 feed, by train. */
	static final String[][] RELAY_CABS = {{"1-3", "4-6"}, {"7-9", "10-12"}};
	/** Each switchboard's train, breakers, state and when it last told (game time), by level and position. */
	private static final Map<ServerLevel, Map<BlockPos, long[]>> BOARDS = new WeakHashMap<>();

	private DcRoom() {
	}

	public static boolean is(BlockState state, String key) {
		return state.getBlock() == PlantInterior.BLOCKS.get(key);
	}

	public static boolean isSwitchboard(BlockState state) {
		return is(state, SWITCHBOARD);
	}

	public static boolean isCharger(BlockState state) {
		return is(state, CHARGER);
	}

	public static boolean isPanel(BlockState state) {
		return is(state, PANEL);
	}

	public static boolean isLaptop(BlockState state) {
		return is(state, LAPTOP);
	}

	public static boolean isDisconnect(BlockState state) {
		return is(state, DISCONNECT);
	}

	/** A piece's train (0 A, 1 B): a switchboard's or ground detector's label, a charger's, inverter's or panel's (1A-1 ...). */
	public static int train(BlockState state) {
		int v = LivePanelBlockEntity.variant(state);
		return isSwitchboard(state) || is(state, DETECTOR) ? v % 2 : v / 2 % 2;
	}

	/** A charger's, inverter's or panel's number in its train: 0 for -1, 1 for -2. */
	public static int number(BlockState state) {
		return LivePanelBlockEntity.variant(state) % 2;
	}

	public static String trainName(int train) {
		return train == 1 ? "1B" : "1A";
	}

	/** Feeder k's (1 to 12) load on the train's switchboard (tools/dc_room.py sb_feeders). */
	public static String feederName(int train, int k) {
		String l = trainName(train);
		boolean a = train == 0;
		return switch (k) {
			case 1 -> "INVERTER " + l + "-1";
			case 2 -> "INVERTER " + l + "-2";
			case 3 -> "DC DIST PNL " + l + "-1";
			case 4 -> "DC DIST PNL " + l + "-2";
			case 5 -> "480V LC " + l + " CONTROL";
			case 6 -> "4160V SWGR " + l + " CTL";
			case 7 -> "EDG " + l + " CONTROL";
			case 8 -> a ? "TURB AUX OIL PUMP" : "MD AFW PUMP CTL";
			case 9 -> "EMERG LIGHTING " + l;
			case 10 -> a ? "AFW TURB CONTROL" : "DIESEL FUEL XFER";
			case 11 -> "COMPUTER UPS " + l;
			default -> "SPARE";
		};
	}

	/** Its short name on the laptop's screen. */
	public static String feederShort(int train, int k) {
		String l = trainName(train);
		boolean a = train == 0;
		return String.format(Locale.ROOT, "%02d %s", k, switch (k) {
			case 1 -> "INV " + l + "-1";
			case 2 -> "INV " + l + "-2";
			case 3 -> "PNL " + l + "-1";
			case 4 -> "PNL " + l + "-2";
			case 5 -> "LC " + l + " CTL";
			case 6 -> "SWGR CTL";
			case 7 -> "EDG " + l + " CTL";
			case 8 -> a ? "TURB AOP" : "AFW CTL";
			case 9 -> "EM LTG";
			case 10 -> a ? "AFW TURB" : "FUEL XFER";
			case 11 -> "UPS " + l;
			default -> "SPARE";
		});
	}

	/** A DC panel's circuit k (1 to 12) by its label (variant 0 to 3). */
	public static String circuitName(int k, int v) {
		int train = v / 2 % 2, n = v % 2;
		String name = PANEL_NAMES[n][k - 1];
		if (n == 0 && (k == 4 || k == 5)) {
			return String.format(Locale.ROOT, name, RELAY_CABS[train][k - 4]);
		}
		return name.contains("%s") ? String.format(Locale.ROOT, name, train == 1 ? "B" : "A") : name;
	}

	/** What a DC panel's closed circuits draw (amps), nothing with its main open. */
	public static int panelLoad(int breakers, int v) {
		if ((breakers & 1) == 0) {
			return 0;
		}
		int amps = 0;
		for (int k = 1; k <= 12; k++) {
			if ((breakers & 1 << k) != 0) {
				amps += PANEL_LOADS[v % 2][k - 1];
			}
		}
		return amps;
	}

	// ------------------------------------------------------------------------------------------------ reading the room

	/** A piece's switchboard (its link), or null. */
	public static @Nullable LivePanelBlockEntity board(Level level, LivePanelBlockEntity piece) {
		if (isSwitchboard(piece.getBlockState())) {
			return piece;
		}
		if (isDisconnect(piece.getBlockState()) && level instanceof ServerLevel server && (piece.board == null
				|| !(level.getBlockEntity(piece.board) instanceof LivePanelBlockEntity was && isSwitchboard(was.getBlockState())))) {
			// (A string disconnect does not tick, LivePanelBlockEntity.simulated: it finds its switchboard when asked.)
			piece.linkBoardNow(server);
		}
		return piece.board != null && level.getBlockEntity(piece.board) instanceof LivePanelBlockEntity b && isSwitchboard(b.getBlockState()) ? b : null;
	}

	/** The switchboard whose battery a rack is (its racks hold it), within LivePanelBlockEntity.REACH, or null. */
	public static @Nullable LivePanelBlockEntity boardOf(Level level, BlockPos rack) {
		int reach = LivePanelBlockEntity.REACH, r = (reach >> 4) + 1;
		for (int cx = (rack.getX() >> 4) - r; cx <= (rack.getX() >> 4) + r; cx++) {
			for (int cz = (rack.getZ() >> 4) - r; cz <= (rack.getZ() >> 4) + r; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof LivePanelBlockEntity p && isSwitchboard(p.getBlockState()) && p.racks.contains(rack)) {
						return p;
					}
				}
			}
		}
		return null;
	}

	/** One of a switchboard's room parts ({@link #PARTS}: charger 1, 2, inverter 1, 2, panel 1, 2), or null. */
	public static @Nullable LivePanelBlockEntity part(Level level, LivePanelBlockEntity board, int i) {
		BlockPos at = i < board.dcParts.size() ? board.dcParts.get(i) : null;
		return at != null && !at.equals(board.getBlockPos()) && level.getBlockEntity(at) instanceof LivePanelBlockEntity p ? p : null;
	}

	/** Whether a switchboard's breaker b is closed. */
	public static boolean closed(LivePanelBlockEntity board, int b) {
		return (board.breakers & 1 << b) != 0;
	}

	/** Whether a feeder is alive: the bus alive and the feeder closed. */
	public static boolean feederLive(LivePanelBlockEntity board, int k) {
		return (board.sbFlags & BUS) != 0 && closed(board, k);
	}

	/** A charger putting out onto the bus: its supply on, its AC and DC breakers closed. */
	public static boolean delivering(LivePanelBlockEntity charger) {
		return charger.ac && (charger.dcFlags & (AC_OPEN | DC_OPEN)) == 0;
	}

	/** The battery's racks a switchboard has (else its own battery). */
	static List<BlockPos> racks(Level level, LivePanelBlockEntity board) {
		return onBus(level, board);
	}

	/**
	 * The racks on a switchboard's bus (older rooms round 4): its racks less those of the strings whose disconnect is
	 * open (sbIso: bit s for string s + 1, as each rack's label says: StationBatteryBlock, its variant); with no racks of
	 * its own, its battery.
	 */
	public static List<BlockPos> onBus(Level level, LivePanelBlockEntity board) {
		if (board.racks.isEmpty()) {
			return board.battery == null || (board.sbIso & 1 << string(level, board.battery)) != 0 ? List.of() : List.of(board.battery);
		}
		if (board.sbIso == 0) {
			return board.racks;
		}
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos rack : board.racks) {
			if ((board.sbIso >> string(level, rack) & 1) == 0) {
				out.add(rack);
			}
		}
		return out;
	}

	/** A rack's string (0 for string 1, 1 for string 2), by its label (one put down by hand is string 1). */
	public static int string(Level level, BlockPos rack) {
		return LivePanelBlockEntity.variant(level.getBlockState(rack)) / 2 % 2;
	}

	/** Whether a rack is on its switchboard's bus (not in a string whose disconnect is open). */
	public static boolean rackOnBus(Level level, LivePanelBlockEntity board, BlockPos rack) {
		return board.sbIso == 0 || onBus(level, board).contains(rack);
	}

	/** The racks' average charge (0..1; 0 with none). */
	public static double charge(Level level, List<BlockPos> racks) {
		double sum = 0;
		int n = 0;
		for (BlockPos at : racks) {
			if (at != null && level.getBlockEntity(at) instanceof StationBatteryBlockEntity bank) {
				sum += bank.charge();
				n++;
			}
		}
		return n == 0 ? 0 : sum / n;
	}

	/** What the racks take in (+) or give out (-) together (JE a tick). */
	public static int flow(Level level, List<BlockPos> racks) {
		int f = 0;
		for (BlockPos at : racks) {
			if (at != null && level.getBlockEntity(at) instanceof StationBatteryBlockEntity bank) {
				f += bank.flow();
			}
		}
		return f;
	}

	/**
	 * The bus's volts: chargers feeding it hold it at float (132 V), or at 139.8 V equalizing, a little lower while the
	 * battery takes a recharge; the battery alone on it (its main closed) 105 V flat to 129 V full; dead, nothing.
	 */
	public static double busVolts(Level level, LivePanelBlockEntity board) {
		int f = board.sbFlags;
		double soc = charge(level, racks(level, board));
		if ((f & CHARGING) != 0) {
			boolean recharging = (f & BATT) != 0 && flow(level, racks(level, board)) > 0;
			double v = (f & EQUAL) != 0 ? EQUAL_V : FLOAT_V;
			return recharging ? Math.min(v, 126 + 14 * soc) : v;
		}
		return (f & BATT) != 0 ? 105 + 24 * soc : 0;
	}

	/** What an inverter does (as its panel shows it): its DC in, running, the load on it or on its bypass, the bypass there. */
	public record Inverter(boolean dc, boolean run, boolean onInv, boolean onBypass, boolean bypassOk, double dcV) {
		public boolean output() {
			return onInv || onBypass;
		}
	}

	/**
	 * An inverter: its DC from its switchboard's feeder 01 or 02 through its own DC input breaker (with no switchboard,
	 * its battery's, as before these rooms); it runs on DC; the bypass is its regulating transformer alive. On NORMAL its
	 * static switch carries the load on the inverter, and on the bypass with the inverter down; on BYPASS (its manual
	 * bypass switch) the load is on the bypass whatever the inverter does.
	 */
	public static Inverter inverter(Level level, LivePanelBlockEntity inv) {
		boolean breaker = (inv.dcFlags & DC_OPEN) == 0;
		LivePanelBlockEntity board = board(level, inv);
		boolean dc;
		double volts;
		if (board != null) {
			dc = breaker && feederLive(board, number(inv.getBlockState()) == 0 ? INV1 : INV2);
			volts = dc ? busVolts(level, board) : 0;
		} else if (inv.battery != null && level.getBlockEntity(inv.battery) instanceof StationBatteryBlockEntity bank) {
			double soc = bank.charge();
			dc = breaker && (soc > 0.005 || bank.flow() > 0);
			volts = !dc ? 0 : bank.flow() > 0 ? 129 + 6 * soc : soc > 0.999 ? FLOAT_V : 105 + 24 * soc;
		} else {
			dc = false;
			volts = 0;
		}
		boolean bypassOk = inv.xf != null && level.getBlockEntity(inv.xf) instanceof LivePanelBlockEntity xf && xf.energized(level);
		boolean manual = inv.mode == BYPASS;
		boolean onInv = dc && !manual;
		return new Inverter(dc, dc, onInv, !onInv && bypassOk, bypassOk, volts);
	}

	/** An inverter's AC load (amps): its vital panel's closed breakers (with none linked, the 46 A it always carried). */
	public static double inverterLoad(Level level, LivePanelBlockEntity inv) {
		return inv.panel != null && level.getBlockEntity(inv.panel) instanceof LivePanelBlockEntity vb && LivePanelBlockEntity.isVitalPanel(vb.getBlockState())
				? VitalPanelBlock.load(vb.breakers) : 46;
	}

	/** Whether a DC panel is alive: its switchboard's feeder 03 or 04 alive and its main closed. */
	public static boolean panelLive(Level level, LivePanelBlockEntity panel) {
		LivePanelBlockEntity board = board(level, panel);
		return board != null && (panel.breakers & 1) != 0 && feederLive(board, number(panel.getBlockState()) == 0 ? PNL1 : PNL2);
	}

	/** What feeder k carries (amps; nothing open or with the bus dead). */
	public static double feederAmps(Level level, LivePanelBlockEntity board, int k) {
		if (!feederLive(board, k)) {
			return 0;
		}
		int train = train(board.getBlockState());
		if (k == INV1 || k == INV2) {
			// An inverter on its DC draws its AC load's worth and its losses (120 V out of 125 V in, 90 % efficient).
			LivePanelBlockEntity inv = part(level, board, 2 + k - INV1);
			return inv != null && inverter(level, inv).onInv() ? inverterLoad(level, inv) * 120 / 125 / 0.9 : 0;
		}
		if (k == PNL1 || k == PNL2) {
			LivePanelBlockEntity p = part(level, board, 4 + k - PNL1);
			return p == null ? 0 : panelLoad(p.breakers, LivePanelBlockEntity.variant(p.getBlockState()));
		}
		return k <= FEEDERS ? FEEDER_AMPS[train][k - LC_CTL] : 0;
	}

	/** What the bus's closed feeders carry together (amps). */
	public static double busLoad(Level level, LivePanelBlockEntity board) {
		double sum = 0;
		for (int k = 1; k <= FEEDERS; k++) {
			sum += feederAmps(level, board, k);
		}
		return sum;
	}

	/** How many of a switchboard's chargers feed its bus. */
	public static int feeding(Level level, LivePanelBlockEntity board) {
		int n = 0;
		for (int i = 0; i < 2; i++) {
			LivePanelBlockEntity c = part(level, board, i);
			if (c != null && delivering(c)) {
				n++;
			}
		}
		return n;
	}

	/** A charger's output (amps): its share of the bus's load and what it charges the battery with, a little more equalizing. */
	public static double chargerAmps(Level level, LivePanelBlockEntity charger) {
		if (!delivering(charger)) {
			return 0;
		}
		LivePanelBlockEntity board = board(level, charger);
		double load = board == null ? 2 : busLoad(level, board) / Math.max(1, feeding(level, board));
		return load + charger.charging / 8.0 + (charger.mode == EQUALIZE ? 3 : 0);
	}

	/** A charger's equalize hours left (0 floating), at game time now. */
	public static double eqHours(LivePanelBlockEntity charger, double now) {
		return charger.mode == EQUALIZE ? Math.max(0, Math.min(24, (charger.eqUntil - now) / TICKS_PER_HOUR)) : 0;
	}

	/** A window as the annunciator shows it at game time now: 0 dark, 1 lit, 2 flashing (new, not acknowledged). */
	public static int window(LivePanelBlockEntity panel, int k, double now) {
		if (now < panel.annTestUntil) {
			return 1;
		}
		if ((panel.annLatch >> k & 1) == 0) {
			return 0;
		}
		return (panel.annAck >> k & 1) != 0 ? 1 : 2;
	}

	/** How many windows are lit on a charger's or inverter's annunciator. */
	public static int alarms(LivePanelBlockEntity panel) {
		return Integer.bitCount(panel.annLatch);
	}

	// ------------------------------------------------------------------------------------------------ the server

	/**
	 * A switchboard, once a second: the room's parts it finds (its train's chargers, inverters and panels linked to it,
	 * by number), its bus's state; what it tells the load centres and diesels.
	 */
	public static void tickBoard(ServerLevel level, LivePanelBlockEntity board) {
		List<BlockPos> parts = new ArrayList<>();
		for (int i = 0; i < PARTS; i++) {
			parts.add(board.getBlockPos());
		}
		String[] kinds = {CHARGER, INVERTER, PANEL};
		for (int g = 0; g < kinds.length; g++) {
			for (LivePanelBlockEntity p : board.panelsAround(level, PlantInterior.BLOCKS.get(kinds[g]))) {
				int slot = 2 * g + number(p.getBlockState());
				if (board.getBlockPos().equals(p.board) && parts.get(slot).equals(board.getBlockPos())) {
					parts.set(slot, p.getBlockPos().immutable());
				}
			}
		}
		boolean feeding = false, equal = false;
		for (int i = 0; i < 2; i++) {
			BlockPos at = parts.get(i);
			if (!at.equals(board.getBlockPos()) && level.getBlockEntity(at) instanceof LivePanelBlockEntity c && delivering(c)) {
				feeding = true;
				equal |= c.mode == EQUALIZE;
			}
		}
		// The battery rooms' string disconnects linked to it (older rooms round 4): each one open takes its string off.
		int iso = 0;
		for (LivePanelBlockEntity d : board.panelsAround(level, PlantInterior.BLOCKS.get(DISCONNECT))) {
			if (d.mode == OPEN && board(level, d) == board) {
				iso |= 1 << number(d.getBlockState());
			}
		}
		boolean isoChanged = iso != board.sbIso;
		board.sbIso = iso;
		boolean batt = closed(board, MAIN) && charge(level, racks(level, board)) > 0.002;
		int flags = (feeding || batt ? BUS : 0) | (batt ? BATT : 0) | (feeding ? CHARGING : 0) | (equal ? EQUAL : 0);
		if (flags != board.sbFlags || !parts.equals(board.dcParts) || isoChanged) {
			board.sbFlags = flags;
			board.dcParts.clear();
			board.dcParts.addAll(parts);
			board.syncNow(level);
		}
		report(level, board);
	}

	/** A switchboard tells where it is and how it stands (for the load centres and diesels). */
	static void report(ServerLevel level, LivePanelBlockEntity board) {
		BOARDS.computeIfAbsent(level, l -> new HashMap<>()).put(board.getBlockPos().immutable(),
				new long[] {train(board.getBlockState()), board.breakers, board.sbFlags, level.getGameTime()});
	}

	/**
	 * Whether feeder k of the train's switchboard is alive for something at {@code near}: the nearest switchboard of that
	 * train within {@link #LOOK} blocks that has told in the last five seconds; with none, yes (a plant without these
	 * rooms keeps its control power).
	 */
	public static boolean feederLive(ServerLevel level, BlockPos near, int train, int k) {
		Map<BlockPos, long[]> boards = BOARDS.get(level);
		if (boards == null) {
			return true;
		}
		long now = level.getGameTime();
		long[] best = null;
		double bestD = (double) LOOK * LOOK;
		for (java.util.Iterator<Map.Entry<BlockPos, long[]>> it = boards.entrySet().iterator(); it.hasNext();) {
			Map.Entry<BlockPos, long[]> e = it.next();
			long[] b = e.getValue();
			if (now - b[3] > 100 || now < b[3]) {
				it.remove();
				continue;
			}
			double d = e.getKey().distSqr(near);
			if (b[0] == train && d <= bestD) {
				bestD = d;
				best = b;
			}
		}
		return best == null || (best[2] & BUS) != 0 && (best[1] & 1L << k) != 0;
	}

	/** The windows a charger's conditions put in (bits {@link #C_AC_FAIL} ...). */
	public static int chargerAlarms(ServerLevel level, LivePanelBlockEntity c, long now) {
		LivePanelBlockEntity board = board(level, c);
		double volts = board != null ? busVolts(level, board) : delivering(c) ? FLOAT_V : 0;
		int in = 0;
		if (!c.ac || (c.dcFlags & AC_OPEN) != 0) {
			in |= 1 << C_AC_FAIL;
		}
		if (volts < DC_LOW) {
			in |= 1 << C_DC_LOW;
		}
		if (volts > DC_HIGH) {
			in |= 1 << C_DC_HIGH;
		}
		if (!delivering(c)) {
			in |= 1 << C_CHGR_FAIL;
		}
		if (grounded(level, c, now)) {
			in |= 1 << C_GND;
		}
		if (c.mode == EQUALIZE) {
			in |= 1 << C_EQUAL;
		}
		return in;
	}

	/** Whether the room's ground detector has a ground on the bus (a test held within the last second). */
	static boolean grounded(ServerLevel level, LivePanelBlockEntity piece, long now) {
		int train = train(piece.getBlockState());
		for (LivePanelBlockEntity gd : piece.panelsAround(level, PlantInterior.BLOCKS.get(DETECTOR))) {
			if (train(gd.getBlockState()) == train && gd.testSide != 0 && gd.testUntil > now - 20) {
				return true;
			}
		}
		return false;
	}

	/** The windows an inverter's conditions put in (bits {@link #I_FAIL} ...). */
	public static int inverterAlarms(ServerLevel level, LivePanelBlockEntity inv) {
		Inverter s = inverter(level, inv);
		int in = 0;
		if (!s.run()) {
			in |= 1 << I_FAIL;
		}
		if (s.dcV() < INV_LOW_DC) {
			in |= 1 << I_LOW_DC;
		}
		if (!s.bypassOk()) {
			in |= 1 << I_BYP_FAIL;
		}
		if (s.onBypass()) {
			in |= 1 << I_ON_BYP;
		}
		if (s.run() && !s.bypassOk()) {
			in |= 1 << I_SYNC;
		}
		return in;
	}

	/**
	 * A charger or inverter, once a second: its equalize timer (back to FLOAT when it runs out), its annunciator (a
	 * condition come in latches its window, flashing until acknowledged, with a beep).
	 */
	public static void tickPanel(ServerLevel level, LivePanelBlockEntity p, boolean charger) {
		long now = level.getGameTime();
		boolean changed = false;
		if (charger && p.mode == EQUALIZE && now >= p.eqUntil) {
			p.mode = FLOAT;
			p.eqUntil = 0;
			changed = true;
		}
		int in = charger ? chargerAlarms(level, p, now) : inverterAlarms(level, p);
		if (p.dcAge < SETTLE) {
			// Just built or loaded: nothing latches until it has settled (what is in then comes in, a dead plant's too).
			if (changed) {
				p.syncNow(level);
			}
			return;
		}
		int fresh = in & ~p.annIn;
		if (in != p.annIn) {
			p.annIn = in;
			changed = true;
		}
		if (fresh != 0) {
			p.annLatch |= fresh;
			p.annAck &= ~fresh;
			level.playSound(null, p.getBlockPos().above(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.2F);
		}
		if (changed || fresh != 0) {
			p.syncNow(level);
		}
	}

	/** ACK: the flashing windows go steady. */
	public static boolean acknowledge(LivePanelBlockEntity p) {
		boolean any = (p.annLatch & ~p.annAck) != 0;
		p.annAck |= p.annLatch;
		return any;
	}

	/** RESET: the acknowledged windows whose conditions have cleared go dark. */
	public static int reset(LivePanelBlockEntity p) {
		int clear = p.annLatch & p.annAck & ~p.annIn;
		p.annLatch &= ~clear;
		p.annAck &= p.annLatch;
		return Integer.bitCount(clear);
	}

	// ------------------------------------------------------------------------------------------------ the laptop

	/** The laptop's pages: the room, the feeders 01 to 09, 10 to 12. */
	public static final int PAGES = 3, ROWS = 9;

	/**
	 * The laptop's screen into its text channels (dcl_t, dcl_v; dcl_n, dcl_g / dcl_y / dcl_w and dcl_a a row; dcl_f,
	 * dcl_x): its switchboard's room on page 0 (the battery, the chargers, the inverters, the main, the feeders, the
	 * panels), the feeders one a line on pages 1 and 2; the ground detector and the alarms at its foot.
	 */
	public static void screen(Level level, @Nullable LivePanelBlockEntity board, int page, double now, Map<String, String> t) {
		if (board == null) {
			t.put("dcl_t", "DC SYSTEM MONITOR");
			t.put("dcl_n0", "NO SWITCHBOARD IN REACH");
			return;
		}
		int train = train(board.getBlockState());
		String l = trainName(train);
		boolean bus = (board.sbFlags & BUS) != 0;
		double volts = busVolts(level, board);
		page = Math.floorMod(page, PAGES);
		t.put("dcl_t", page == 0 ? "DC BUS " + l : String.format(Locale.ROOT, "SWBD %s FEEDERS %d/2", l, page));
		t.put("dcl_v", bus ? String.format(Locale.ROOT, "%.1f V", volts) : "BUS DEAD");
		if (page == 0) {
			List<BlockPos> racks = racks(level, board);
			double soc = charge(level, racks) * 100;
			int f = flow(level, racks);
			boolean main = closed(board, MAIN);
			String batt = !main || racks.isEmpty() ? "ISOLATED" : board.sbIso != 0 ? "STR " + (Integer.numberOfTrailingZeros(board.sbIso) + 1) + " OPEN"
					: f > 0 ? "RECHARGE" : f < 0 || (board.sbFlags & CHARGING) == 0 ? "DISCH" : "FLOAT";
			row(t, 0, "BATT " + l, batt.equals("FLOAT") || batt.equals("RECHARGE") ? 'g' : batt.equals("ISOLATED") ? 'w' : 'y',
					String.format(Locale.ROOT, "%s %.0f%%", batt, soc), !main ? "--" : String.format(Locale.ROOT, "%+.0f A",
							(board.sbFlags & CHARGING) == 0 ? -busLoad(level, board) : f / 8.0));
			for (int i = 0; i < 2; i++) {
				LivePanelBlockEntity c = part(level, board, i);
				String name = "CHGR " + l + "-" + (i + 1);
				if (c == null) {
					row(t, 1 + i, name, 'w', "NOT FOUND", "--");
				} else if ((c.dcFlags & AC_OPEN) != 0 || !c.ac) {
					row(t, 1 + i, name, 'y', (c.dcFlags & AC_OPEN) != 0 ? "AC OPEN" : "AC FAIL", "--");
				} else if ((c.dcFlags & DC_OPEN) != 0) {
					row(t, 1 + i, name, 'w', "DC OPEN", "--");
				} else {
					row(t, 1 + i, name, c.mode == EQUALIZE ? 'y' : 'g', c.mode == EQUALIZE ? "EQUALIZE" : "FLOAT",
							String.format(Locale.ROOT, "%.0f A", chargerAmps(level, c)));
				}
			}
			for (int i = 0; i < 2; i++) {
				LivePanelBlockEntity v = part(level, board, 2 + i);
				String name = "INV " + l + "-" + (i + 1);
				if (v == null) {
					row(t, 3 + i, name, 'w', "NOT FOUND", "--");
					continue;
				}
				Inverter s = inverter(level, v);
				String state = s.onInv() ? "ON INV" : s.onBypass() ? (v.mode == BYPASS ? "MAN BYPASS" : "ON BYPASS") : "NO OUTPUT";
				row(t, 3 + i, name, s.onInv() ? 'g' : 'y', state, s.output() ? String.format(Locale.ROOT, "%.0f A", inverterLoad(level, v)) : "--");
			}
			row(t, 5, "MAIN 72-" + l, main ? 'g' : 'w', main ? "CLOSED" : "OPEN", main && bus ? String.format(Locale.ROOT, "%.0f A",
					Math.abs((board.sbFlags & CHARGING) == 0 ? busLoad(level, board) : f / 8.0)) : "--");
			int open = 0;
			for (int k = 1; k < FEEDERS; k++) {
				open += closed(board, k) ? 0 : 1;
			}
			row(t, 6, "FEEDERS", open == 0 ? 'g' : 'y', (FEEDERS - 1 - open) + " CLOSED", open + " OPEN");
			for (int i = 0; i < 2; i++) {
				LivePanelBlockEntity p = part(level, board, 4 + i);
				String name = "PNL " + l + "-" + (i + 1);
				if (p == null) {
					row(t, 7 + i, name, 'w', "NOT FOUND", "--");
				} else {
					boolean live = panelLive(level, p);
					row(t, 7 + i, name, live ? 'g' : 'y', live ? "LIVE" : (p.breakers & 1) == 0 ? "MAIN OPEN" : "DEAD",
							live ? panelLoad(p.breakers, LivePanelBlockEntity.variant(p.getBlockState())) + " A" : "--");
				}
			}
		} else {
			int first = page == 1 ? 1 : 10, last = page == 1 ? 9 : FEEDERS;
			for (int k = first; k <= last; k++) {
				boolean c = closed(board, k);
				String state = !c ? "OPEN" : bus ? "CLOSED" : "NO VOLTS";
				row(t, k - first, feederShort(train, k), !c ? 'w' : bus ? 'g' : 'y', state,
						c && bus ? String.format(Locale.ROOT, "%.0f A", feederAmps(level, board, k)) : "--");
			}
		}
		int side = 0;
		for (LivePanelBlockEntity gd : nearby(level, board, DETECTOR)) {
			if (train(gd.getBlockState()) == train && gd.testing(now)) {
				side = gd.testSide;
			}
		}
		t.put("dcl_f", side == 0 ? "GROUND: NONE" : side == 1 ? "GROUND: + BUS" : "GROUND: - BUS");
		int alarms = 0;
		for (int i = 0; i < 4; i++) {
			LivePanelBlockEntity p = part(level, board, i);
			alarms += p == null ? 0 : alarms(p);
		}
		t.put("dcl_x", "ALARMS " + alarms);
	}

	private static void row(Map<String, String> t, int k, String name, char colour, String state, String amps) {
		t.put("dcl_n" + k, name);
		t.put("dcl_" + colour + k, state);
		t.put("dcl_a" + k, amps);
	}

	/** The live panels of a kind near a piece (its chunks' block entities, within LivePanelBlockEntity.REACH), on either side. */
	static List<LivePanelBlockEntity> nearby(Level level, LivePanelBlockEntity piece, String key) {
		List<LivePanelBlockEntity> out = new ArrayList<>();
		BlockPos here = piece.getBlockPos();
		int reach = LivePanelBlockEntity.REACH, r = (reach >> 4) + 1;
		for (int cx = (here.getX() >> 4) - r; cx <= (here.getX() >> 4) + r; cx++) {
			for (int cz = (here.getZ() >> 4) - r; cz <= (here.getZ() >> 4) + r; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof LivePanelBlockEntity p && is(p.getBlockState(), key) && p.getBlockPos().distSqr(here) <= (double) reach * reach) {
						out.add(p);
					}
				}
			}
		}
		return out;
	}

	/** What a click on the laptop says: the bus, the battery, the chargers, the inverters, what is open. */
	public static String describe(Level level, @Nullable LivePanelBlockEntity board) {
		if (board == null) {
			return "no DC switchboard in reach";
		}
		int train = train(board.getBlockState());
		StringBuilder out = new StringBuilder("DC bus " + trainName(train) + " ");
		out.append((board.sbFlags & BUS) != 0 ? String.format(Locale.ROOT, "%.1f V", busVolts(level, board)) : "DEAD");
		out.append(String.format(Locale.ROOT, ", battery %.0f%%%s", charge(level, racks(level, board)) * 100, closed(board, MAIN) ? "" : " (main OPEN)"));
		out.append(", chargers feeding ").append(feeding(level, board));
		if (board.sbIso != 0) {
			out.append(", string disconnect open: ").append(Integer.bitCount(board.sbIso) == 2 ? "both" : "string " + (Integer.numberOfTrailingZeros(board.sbIso) + 1));
		}
		StringBuilder open = new StringBuilder();
		for (int k = 1; k <= FEEDERS; k++) {
			if (!closed(board, k) && k != FEEDERS) {
				open.append(open.length() == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%02d", k));
			}
		}
		if (open.length() > 0) {
			out.append(", feeders open: ").append(open);
		}
		return out.toString();
	}

	// ------------------------------------------------------------------------------------------------ the buttons

	/** The chargers', inverters' and panels' labels by variant. */
	public static final String[] LABELS = {"1A-1", "1A-2", "1B-1", "1B-2"};

	/** An annunciator's ACK (0), RESET (1) or TEST (2) pressed; what it says. */
	static void button(ServerLevel level, LivePanelBlockEntity p, int which, BlockPos pos, net.minecraft.world.entity.player.Player player, String name) {
		level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.5F);
		net.minecraft.network.chat.Component said;
		if (which == 0) {
			said = net.minecraft.network.chat.Component.translatable(acknowledge(p) ? "message.jugcraft.nuclear.ann_ack" : "message.jugcraft.nuclear.ann_ack_none", name);
		} else if (which == 1) {
			int n = reset(p);
			said = n > 0 ? net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.ann_reset", name, n)
					: net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.ann_reset_none", name);
		} else {
			p.annTestUntil = level.getGameTime() + TEST_TICKS;
			said = net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.ann_test", name);
		}
		p.syncNow(level);
		player.sendOverlayMessage(said);
	}

	/** Which of three buttons in a column (x, their middles' y) is at this point, or -1. */
	static int buttonAt(double x, double y, double bx, double[] ys) {
		for (int k = 0; k < ys.length; k++) {
			if (Math.abs(x - bx) <= 0.65 && Math.abs(y - ys[k]) <= 0.65) {
				return k;
			}
		}
		return -1;
	}

	/** A cabinet's breaker through its door's cut-out (tools/plant_panels.py _breaker_boxes) at this point: its middle x. */
	static boolean breakerAt(double x, double y, double cx) {
		return Math.abs(x - cx) <= 2.0 && y >= 4.6 && y <= 7.2;
	}

	static net.minecraft.network.chat.Component state(boolean closed) {
		return net.minecraft.network.chat.Component.translatable(closed ? "message.jugcraft.nuclear.breaker_closed" : "message.jugcraft.nuclear.breaker_open");
	}
}
