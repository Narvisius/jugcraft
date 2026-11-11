package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Optional;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What the control room's live boards read of the plant (round 27, nuclear/ControlBoard.java): a snapshot the server
 * takes once a second of the reactor a board watches (the nearest), its unit, its diesel generators, its 480 V load
 * centre and motor control centres. It is sent to the players with the board, and both sides work out every meter's
 * reading ({@link #value}), every lamp and status light ({@link #signal}) and every alarm window ({@link #alarm}) from
 * it, the way a Westinghouse four-loop plant's would read (tools/board_live.py lays the boards out by these names).
 * Values the game does not simulate are worked out from the plant (Justin: "Work them out from the plant").
 */
public final class BoardPlant {
	// The reactor and its unit.
	public boolean present;
	/** The unit has reactor coolant pumps (not a small modular reactor's natural circulation). */
	public boolean pumps = true;
	public double fission, heat, temperature = 30, rods = 100, cooling, waterShare = 1, turbineSpeed;
	public long electric, ratedJe = ReactorCore.RATED_JE, limit = Long.MAX_VALUE;
	public boolean scrammed, protection = true, refuel, remote, turbineTripped, genOpen, syncArmed, steamToPower;
	/** Turbine hall part 1: the turbine's turning gear engaged (ReactorCore.turningGear). */
	public boolean turningGear = true;
	/**
	 * Turbine hall part 2 (ReactorCore's feed train): the condensate pumps at the hall, running, standing by in AUTO and
	 * started by AUTO; the main feed pumps at the hall, in service and tripped (bits); the condensate header short; main
	 * feed's capacity (a fraction of a generator's full-power feed).
	 */
	public int cpFound, cpRunning, cpAuto, cpAutoStarted, mfpFound, mfpRunning, mfpTripped;
	public boolean cpShort;
	public double feedCap = ReactorCore.FEED_FREE;
	public int alarms, tripCause, rcpStopped, loops;
	public final double[] loopFlow = new double[4];
	/** The diesel generators, A and B. */
	public final Edg[] edg = {new Edg(), new Edg()};
	/** The 480 V load centre: found, its bus's charge (fraction), its main breaker closed, feeders in use left open. */
	public boolean lc, lcMain;
	public double lcCharge;
	public int lcOpen;
	/** The DC rooms' motor control centres, A and B: found, and their buses' charge (fraction). */
	public final boolean[] mcc = new boolean[2];
	public final double[] mccCharge = new double[2];
	/**
	 * Diesels make real power (27 October 2026; PlantPower): what has power (ReactorCore.PW_* bits, and bits 4 and 5 the
	 * motor-driven aux feed pumps', 6 the turbine-driven one's DC control power), the RES XFMR breakers open (bits), when
	 * the switchyard's lines reclose (game time; -1 not lost).
	 */
	public int power = PW_DEFAULT, resOpen;
	public long gridBack = -1;
	public static final int PW_AFW_A = 16, PW_AFW_B = 32, PW_TDAFW = 64, PW_DEFAULT = ReactorCore.PW_ALL | PW_AFW_A | PW_AFW_B | PW_TDAFW;

	// Round 28: safety injection and the steam generators (ReactorCore), the control room's HVAC (its panel,
	// HvacRoom), the radiation monitors (nuclear/ControlBoard.java reads them where they stand).
	public boolean si, feedIso, cntIso;
	public long siTime;
	public int msivClosed, afwRunning;
	/** Round 29: the aux feed flow control valves shut, the ARVs opened by hand (bit k: generator k + 1). */
	public int afwShut, arvManual;
	public final double[] sgWater = {1, 1, 1, 1}, sgSteam = new double[4], sgFeed = new double[4], sgAux = new double[4];
	public double rwstUsed, cstUsed;
	/** The control room's HVAC panel: found, its mimic's bits and its alarms (HvacRoom), its selector. */
	public boolean hvFound;
	public int hvState, hvAlarms, hvMode;
	/**
	 * Punch list #15: the cable spreading room's releasing panel (SpreadingRoomFire): found, its phase and flags (the
	 * radiation and ventilation board's FIRE CABLE SPREADING, FIRE PUMP RUNNING and FIRE DETECTION TROUBLE windows).
	 */
	public boolean csrFound;
	public int csrPhase, csrFlags;
	/** The radiation monitors' readings in their meters' units (mR/h, R/h, counts a minute), and those found (bit n - 1). */
	public final double[] rad = new double[RAD_MONITORS];
	public int radFound;
	/**
	 * The lake intake's service water as the boards show it (towers and pump houses build, WaterWorks.boardState): -1
	 * with no intake near (the old readings), else bit 0 train A flowing, bit 1 train B, bit 2 make-up, bits 8 to 13 its
	 * pumps running.
	 */
	public int sw = -1;

	// Round 30: the reactor control console. Rod control (AUTO and its setpoint, the shutdown banks, the rods' last
	// move), the nuclear instruments (their level, startup rate and two seconds' change, the trip blocks) and the core's
	// four quadrants' power as the power range channels see it (each a share of the average, from the fuel loaded).
	public boolean rodAuto, srBlocked, irBlocked, prLoBlocked;
	public double autoSetpoint, sdBanks = ReactorCore.STEPS, rodsMoved, nis, sur, nisChange;
	public final double[] tilt = {1, 1, 1, 1};
	/** Assemblies loaded, the core's size, their average burnup (for the plant computer's reactor page). */
	public int fuelLoaded, coreSize = ReactorCore.CORE_SIZE;
	public double burnup;
	/** The power range channels' calibration (each reads a little off the others, as real ones do). */
	static final double[] PR_CAL = {1.004, 0.997, 1.002, 0.995};

	/** The radiation monitors (tools/board_live.py RAD_MONITORS): 12, numbered from 1. */
	public static final int RAD_MONITORS = 12;
	/** Each monitor's unit (0 mR/h, 1 R/h, 2 counts a minute) and its ALERT and HIGH setpoints in it. */
	public static final int[] RAD_UNIT = {0, 0, 1, 0, 0, 0, 0, 0, 2, 2, 2, 2};
	public static final double[] RAD_ALERT = {2.5, 100, 100, 25, 25, 10, 2.5, 10, 1e3, 1e3, 1e4, 1e5};
	public static final double[] RAD_HIGH = {10, 1000, 1000, 100, 100, 100, 10, 100, 1e4, 1e4, 1e5, 1e6};
	/** Each unit's scale's bottom (the log meters' first decade). */
	static final double[] RAD_FLOOR = {0.01, 1, 10};
	/** A steam generator's full-power steam (and feed) flow, Mlb/h, and a gallon a minute's worth of it. */
	static final double SG_MLB = 3.79, GPM = 3.79e6 / 500;

	/**
	 * A diesel generator as the board reads it: found, running and since when (game time), short of fuel, its day tank;
	 * (round 31 part 2) when its breaker closed (-1 open), and whether it can be started from here (its MODE at AUTO, its
	 * 86G and emergency stop reset).
	 */
	public static final class Edg {
		public boolean present, running, fuelOut, auto = true;
		public long since, breakerAt = -1;
		public float day = -1;
	}

	// ------------------------------------------------------------------ taking it

	/** The snapshot of a reactor vessel's core (the rest is filled in by the board). */
	public static BoardPlant of(EquipmentBlockEntity vessel) {
		BoardPlant p = new BoardPlant();
		ReactorCore c = vessel.core;
		p.present = true;
		p.pumps = vessel.kind() != EquipmentKind.SMALL_MODULAR_REACTOR;
		p.fission = c.fission;
		p.heat = c.heat();
		p.temperature = c.temperature;
		p.rods = c.rods;
		p.cooling = c.cooling;
		p.waterShare = c.waterShare;
		p.turbineSpeed = c.turbineSpeed;
		p.turningGear = c.turningGear;
		for (int k = 0; k < 3; k++) {
			if (c.cpPresent(k)) p.cpFound |= 1 << k;
		}
		for (int k = 0; k < 2; k++) {
			if (c.mfpPresent(k)) p.mfpFound |= 1 << k;
		}
		p.cpRunning = c.cpRunning;
		p.cpAuto = c.cpAuto;
		p.cpAutoStarted = c.cpAutoStarted;
		p.mfpRunning = c.mfpRunning;
		p.mfpTripped = c.mfpTripped;
		p.cpShort = c.cpShort;
		p.feedCap = c.feedCap;
		p.electric = c.electric;
		p.ratedJe = c.design.ratedJe();
		ReactorUnit unit = vessel.unit();
		p.limit = unit != null ? unit.generatorLimit() : Long.MAX_VALUE;
		p.steamToPower = unit != null && unit.steamToPower();
		p.scrammed = c.scrammed;
		p.protection = c.protection;
		p.refuel = c.refuelOpen;
		p.remote = c.remote;
		p.turbineTripped = c.turbineTripped;
		p.genOpen = c.genOpen;
		p.syncArmed = c.syncArmed;
		p.alarms = c.alarms;
		p.tripCause = c.tripCause;
		p.rcpStopped = c.rcpStopped;
		p.power = c.power | (c.afwPower & 3) << 4 | (c.tdafwDc ? PW_TDAFW : 0);
		p.resOpen = c.resOpen;
		p.gridBack = c.gridBack;
		p.loops = p.pumps ? c.loops : 0;
		System.arraycopy(c.loopFlow, 0, p.loopFlow, 0, 4);
		p.si = c.siActuated;
		p.siTime = c.siTime;
		p.feedIso = c.feedIsolated;
		p.cntIso = c.cntIsolated;
		p.msivClosed = c.msivClosed;
		p.afwRunning = c.afwRunning;
		p.afwShut = c.afwValveShut;
		p.arvManual = c.arvManual;
		System.arraycopy(c.sgWater, 0, p.sgWater, 0, 4);
		System.arraycopy(c.sgSteam, 0, p.sgSteam, 0, 4);
		System.arraycopy(c.sgFeed, 0, p.sgFeed, 0, 4);
		System.arraycopy(c.sgAux, 0, p.sgAux, 0, 4);
		p.rwstUsed = c.rwstUsed;
		p.cstUsed = c.cstUsed;
		p.rodAuto = c.rodAuto;
		p.autoSetpoint = c.autoSetpoint;
		p.sdBanks = c.sdBanks;
		p.rodsMoved = c.rodsMoved;
		p.nis = c.nis;
		p.sur = c.sur;
		p.nisChange = c.nisChange;
		p.srBlocked = c.srBlocked;
		p.irBlocked = c.irBlocked;
		p.prLoBlocked = c.prLoBlocked;
		quadrants(c, p.tilt);
		p.fuelLoaded = c.fuel.size();
		p.coreSize = c.design.size();
		double sum = 0;
		for (double b : c.fuel) {
			sum += Math.min(1, b);
		}
		p.burnup = c.fuel.isEmpty() ? 0 : sum / c.fuel.size();
		return p;
	}

	/**
	 * The core's four quadrants' power, each as a share of their average (round 30): every loaded assembly that is not
	 * used up gives its quadrant its power (more toward the middle, less as it burns up, as the console's core map
	 * draws it); an assembly on a middle row or column gives half to each side. A part-loaded core tilts.
	 */
	static void quadrants(ReactorCore c, double[] out) {
		int[] rows = c.design.rows();
		int wide = 0;
		for (int n : rows) {
			wide = Math.max(wide, n);
		}
		double midRow = (rows.length - 1) / 2.0, midCol = (wide - 1) / 2.0;
		double[] q = new double[4];
		int index = 0;
		for (int r = 0; r < rows.length; r++) {
			int start = (wide - rows[r]) / 2;
			for (int k = 0; k < rows[r]; k++, index++) {
				if (index >= c.fuel.size() || c.fuelDamaged || c.fuel.get(index) >= 1) {
					continue;
				}
				int col = start + k;
				double dr = (r - midRow) / Math.max(1, midRow), dc = (col - midCol) / Math.max(1, midCol);
				double w = (1.15 - 0.45 * (dr * dr + dc * dc)) * (1 - c.fuel.get(index) * 0.3);
				double top = r < midRow ? 1 : r > midRow ? 0 : 0.5, left = col < midCol ? 1 : col > midCol ? 0 : 0.5;
				q[0] += w * top * left;
				q[1] += w * top * (1 - left);
				q[2] += w * (1 - top) * left;
				q[3] += w * (1 - top) * (1 - left);
			}
		}
		double avg = (q[0] + q[1] + q[2] + q[3]) / 4;
		for (int k = 0; k < 4; k++) {
			out[k] = avg > 0 ? q[k] / avg : 1;
		}
	}

	/** Whether it moved enough since {@code o} that the players should be sent it again. */
	public boolean differs(BoardPlant o) {
		if (o == null || present != o.present || scrammed != o.scrammed || protection != o.protection || turbineTripped != o.turbineTripped
				|| genOpen != o.genOpen || syncArmed != o.syncArmed || alarms != o.alarms || tripCause != o.tripCause || rcpStopped != o.rcpStopped
				|| power != o.power || resOpen != o.resOpen || gridBack != o.gridBack
				|| loops != o.loops || refuel != o.refuel || remote != o.remote || steamToPower != o.steamToPower || lc != o.lc || lcMain != o.lcMain
				|| lcOpen != o.lcOpen || (electric > 0) != (o.electric > 0) || si != o.si || feedIso != o.feedIso || cntIso != o.cntIso
				|| msivClosed != o.msivClosed || afwRunning != o.afwRunning || afwShut != o.afwShut || arvManual != o.arvManual || hvFound != o.hvFound || hvState != o.hvState || hvAlarms != o.hvAlarms
				|| csrFound != o.csrFound || csrPhase != o.csrPhase || csrFlags != o.csrFlags
				|| hvMode != o.hvMode || radFound != o.radFound || sw != o.sw || rodAuto != o.rodAuto || srBlocked != o.srBlocked || irBlocked != o.irBlocked
				|| prLoBlocked != o.prLoBlocked || (rodsMoved > 1e-6) != (o.rodsMoved > 1e-6) || (rodsMoved < -1e-6) != (o.rodsMoved < -1e-6)
				|| fuelLoaded != o.fuelLoaded || Math.abs(sdBanks - o.sdBanks) > 1 || Math.abs(autoSetpoint - o.autoSetpoint) > 0.0005
				|| Math.abs(sur - o.sur) > 0.02 || (nisChange > 0.03) != (o.nisChange > 0.03) || (nisChange < -0.05) != (o.nisChange < -0.05)
				|| Math.abs(nis - o.nis) > 0.002 || nis > 0 != o.nis > 0
				|| nis > 0 && o.nis > 0 && Math.abs(Math.log10(nis) - Math.log10(o.nis)) > 0.02) {
			return true;
		}
		for (int k = 0; k < 4; k++) {
			if (Math.abs(sgWater[k] - o.sgWater[k]) > 0.002 || Math.abs(sgSteam[k] - o.sgSteam[k]) > 0.005 || Math.abs(sgFeed[k] - o.sgFeed[k]) > 0.005
					|| Math.abs(sgAux[k] - o.sgAux[k]) > 0.002) {
				return true;
			}
		}
		for (int k = 0; k < RAD_MONITORS; k++) {
			double a = rad[k], b = o.rad[k];
			if (Math.abs(a - b) > 0.05 * Math.max(Math.abs(a), Math.abs(b)) + 1e-9) {
				return true;
			}
		}
		if (Math.abs(rwstUsed - o.rwstUsed) > 0.001 || Math.abs(cstUsed - o.cstUsed) > 0.001) {
			return true;
		}
		for (int k = 0; k < 2; k++) {
			if (edg[k].present != o.edg[k].present || edg[k].running != o.edg[k].running || edg[k].fuelOut != o.edg[k].fuelOut
					|| edg[k].auto != o.edg[k].auto || edg[k].breakerAt != o.edg[k].breakerAt || Math.abs(edg[k].day - o.edg[k].day) > 0.01 || mcc[k] != o.mcc[k] || Math.abs(mccCharge[k] - o.mccCharge[k]) > 0.01) {
				return true;
			}
		}
		for (int k = 0; k < 4; k++) {
			if (Math.abs(loopFlow[k] - o.loopFlow[k]) > 0.005) {
				return true;
			}
		}
		return Math.abs(fission - o.fission) > 0.002 || Math.abs(heat - o.heat) > 0.002 || Math.abs(temperature - o.temperature) > 0.1
				|| Math.abs(rods - o.rods) > 0.2 || Math.abs(turbineSpeed - o.turbineSpeed) > 0.002 || Math.abs(electric - o.electric) > ratedJe / 500
				|| Math.abs(lcCharge - o.lcCharge) > 0.01 || turningGear != o.turningGear || feedBits() != o.feedBits() || Math.abs(feedCap - o.feedCap) > 0.01;
	}

	/** The feed train's state in one int (turbine hall part 2), as sent. */
	int feedBits() {
		return cpFound | cpRunning << 3 | cpAuto << 6 | cpAutoStarted << 9 | mfpFound << 12 | mfpRunning << 14 | mfpTripped << 16 | (cpShort ? 1 << 18 : 0);
	}

	public void save(ValueOutput parent) {
		ValueOutput out = parent.child("plant");
		out.putBoolean("present", present);
		out.putBoolean("pumps", pumps);
		out.putDouble("fission", fission);
		out.putDouble("heat", heat);
		out.putDouble("temperature", temperature);
		out.putDouble("rods", rods);
		out.putDouble("cooling", cooling);
		out.putDouble("water_share", waterShare);
		out.putDouble("turbine_speed", turbineSpeed);
		out.putLong("electric", electric);
		out.putLong("rated", ratedJe);
		out.putLong("limit", limit);
		out.putInt("flags", (scrammed ? 1 : 0) | (protection ? 2 : 0) | (refuel ? 4 : 0) | (remote ? 8 : 0) | (turbineTripped ? 16 : 0)
				| (genOpen ? 32 : 0) | (syncArmed ? 64 : 0) | (steamToPower ? 128 : 0) | (lc ? 256 : 0) | (lcMain ? 512 : 0) | (mcc[0] ? 1024 : 0)
				| (mcc[1] ? 2048 : 0) | (turningGear ? 4096 : 0));
		out.putInt("alarms", alarms);
		out.putInt("trip_cause", tripCause);
		out.putInt("rcp_stopped", rcpStopped);
		out.putInt("power", power);
		out.putInt("res_open", resOpen);
		out.putLong("grid_back", gridBack);
		out.putInt("loops", loops);
		out.putIntArray("flow", new int[] {(int) Math.round(loopFlow[0] * 10000), (int) Math.round(loopFlow[1] * 10000),
				(int) Math.round(loopFlow[2] * 10000), (int) Math.round(loopFlow[3] * 10000)});
		out.putDouble("lc_charge", lcCharge);
		out.putInt("lc_open", lcOpen);
		out.putDouble("mcc_a", mccCharge[0]);
		out.putDouble("mcc_b", mccCharge[1]);
		for (int k = 0; k < 2; k++) {
			Edg e = edg[k];
			out.putInt("edg" + k, (e.present ? 1 : 0) | (e.running ? 2 : 0) | (e.fuelOut ? 4 : 0) | (e.auto ? 0 : 8));
			out.putLong("edg_since" + k, e.since);
			out.putLong("edg_bkr" + k, e.breakerAt);
			out.putFloat("edg_day" + k, e.day);
		}
		out.putInt("esf", (si ? 1 : 0) | (feedIso ? 2 : 0) | (cntIso ? 4 : 0) | (hvFound ? 8 : 0));
		out.putLong("si_time", siTime);
		out.putInt("msiv", msivClosed);
		out.putInt("afw", afwRunning);
		out.putInt("afw_shut", afwShut);
		out.putInt("arv_manual", arvManual);
		int[] sg = new int[16];
		for (int k = 0; k < 4; k++) {
			sg[k] = (int) Math.round(sgWater[k] * 10000);
			sg[4 + k] = (int) Math.round(sgSteam[k] * 10000);
			sg[8 + k] = (int) Math.round(sgFeed[k] * 10000);
			sg[12 + k] = (int) Math.round(sgAux[k] * 10000);
		}
		out.putIntArray("sg", sg);
		out.putDouble("rwst_used", rwstUsed);
		out.putDouble("cst_used", cstUsed);
		out.putIntArray("hv", new int[] {hvState, hvAlarms, hvMode});
		out.putIntArray("csr", new int[] {csrFound ? 1 : 0, csrPhase, csrFlags});
		int[] r = new int[RAD_MONITORS];
		for (int k = 0; k < RAD_MONITORS; k++) {
			r[k] = Float.floatToIntBits((float) rad[k]);
		}
		out.putIntArray("rad", r);
		out.putInt("rad_found", radFound);
		out.putInt("sw", sw);
		out.putInt("console", (rodAuto ? 1 : 0) | (srBlocked ? 2 : 0) | (irBlocked ? 4 : 0) | (prLoBlocked ? 8 : 0));
		out.putDouble("auto_sp", autoSetpoint);
		out.putDouble("sd_banks", sdBanks);
		out.putDouble("rods_moved", rodsMoved);
		out.putDouble("nis", nis);
		out.putDouble("sur", sur);
		out.putDouble("nis_change", nisChange);
		out.putIntArray("tilt", new int[] {(int) Math.round(tilt[0] * 10000), (int) Math.round(tilt[1] * 10000), (int) Math.round(tilt[2] * 10000),
				(int) Math.round(tilt[3] * 10000)});
		out.putIntArray("fuel", new int[] {fuelLoaded, coreSize, (int) Math.round(burnup * 10000)});
		out.putInt("feed", feedBits());
		out.putDouble("feed_cap", feedCap);
	}

	public static BoardPlant load(ValueInput parent) {
		BoardPlant p = new BoardPlant();
		Optional<ValueInput> child = parent.child("plant");
		if (child.isEmpty()) {
			return p;
		}
		ValueInput in = child.get();
		p.present = in.getBooleanOr("present", false);
		p.pumps = in.getBooleanOr("pumps", true);
		p.fission = in.getDoubleOr("fission", 0);
		p.heat = in.getDoubleOr("heat", 0);
		p.temperature = in.getDoubleOr("temperature", 30);
		p.rods = in.getDoubleOr("rods", 100);
		p.cooling = in.getDoubleOr("cooling", 0);
		p.waterShare = in.getDoubleOr("water_share", 1);
		p.turbineSpeed = in.getDoubleOr("turbine_speed", 0);
		p.electric = in.getLongOr("electric", 0);
		p.ratedJe = in.getLongOr("rated", ReactorCore.RATED_JE);
		p.limit = in.getLongOr("limit", Long.MAX_VALUE);
		int f = in.getIntOr("flags", 2);
		p.scrammed = (f & 1) != 0;
		p.protection = (f & 2) != 0;
		p.refuel = (f & 4) != 0;
		p.remote = (f & 8) != 0;
		p.turbineTripped = (f & 16) != 0;
		p.genOpen = (f & 32) != 0;
		p.syncArmed = (f & 64) != 0;
		p.steamToPower = (f & 128) != 0;
		p.lc = (f & 256) != 0;
		p.lcMain = (f & 512) != 0;
		p.mcc[0] = (f & 1024) != 0;
		p.mcc[1] = (f & 2048) != 0;
		p.turningGear = (f & 4096) != 0;
		p.alarms = in.getIntOr("alarms", 0);
		p.tripCause = in.getIntOr("trip_cause", 0);
		p.rcpStopped = in.getIntOr("rcp_stopped", 0);
		p.power = in.getIntOr("power", PW_DEFAULT);
		p.resOpen = in.getIntOr("res_open", 0);
		p.gridBack = in.getLongOr("grid_back", -1L);
		p.loops = in.getIntOr("loops", 0);
		int[] flow = in.getIntArray("flow").orElse(new int[4]);
		for (int k = 0; k < 4 && k < flow.length; k++) {
			p.loopFlow[k] = flow[k] / 10000.0;
		}
		p.lcCharge = in.getDoubleOr("lc_charge", 0);
		p.lcOpen = in.getIntOr("lc_open", 0);
		p.mccCharge[0] = in.getDoubleOr("mcc_a", 0);
		p.mccCharge[1] = in.getDoubleOr("mcc_b", 0);
		for (int k = 0; k < 2; k++) {
			int e = in.getIntOr("edg" + k, 0);
			p.edg[k].present = (e & 1) != 0;
			p.edg[k].running = (e & 2) != 0;
			p.edg[k].fuelOut = (e & 4) != 0;
			p.edg[k].auto = (e & 8) == 0;
			p.edg[k].since = in.getLongOr("edg_since" + k, 0);
			p.edg[k].breakerAt = in.getLongOr("edg_bkr" + k, p.edg[k].running ? p.edg[k].since + Math.round(DieselFuel.BREAKER * 20) : -1);
			p.edg[k].day = in.getFloatOr("edg_day" + k, -1F);
		}
		int esf = in.getIntOr("esf", 0);
		p.si = (esf & 1) != 0;
		p.feedIso = (esf & 2) != 0;
		p.cntIso = (esf & 4) != 0;
		p.hvFound = (esf & 8) != 0;
		p.siTime = in.getLongOr("si_time", 0L);
		p.msivClosed = in.getIntOr("msiv", 0);
		p.afwRunning = in.getIntOr("afw", 0);
		p.afwShut = in.getIntOr("afw_shut", 0);
		p.arvManual = in.getIntOr("arv_manual", 0);
		int[] sg = in.getIntArray("sg").orElse(new int[0]);
		if (sg.length == 16) {
			for (int k = 0; k < 4; k++) {
				p.sgWater[k] = sg[k] / 10000.0;
				p.sgSteam[k] = sg[4 + k] / 10000.0;
				p.sgFeed[k] = sg[8 + k] / 10000.0;
				p.sgAux[k] = sg[12 + k] / 10000.0;
			}
		}
		p.rwstUsed = in.getDoubleOr("rwst_used", 0);
		p.cstUsed = in.getDoubleOr("cst_used", 0);
		int[] hv = in.getIntArray("hv").orElse(new int[3]);
		if (hv.length == 3) {
			p.hvState = hv[0];
			p.hvAlarms = hv[1];
			p.hvMode = hv[2];
		}
		int[] csr = in.getIntArray("csr").orElse(new int[3]);
		if (csr.length == 3) {
			p.csrFound = csr[0] != 0;
			p.csrPhase = csr[1];
			p.csrFlags = csr[2];
		}
		int[] r = in.getIntArray("rad").orElse(new int[0]);
		for (int k = 0; k < RAD_MONITORS && k < r.length; k++) {
			p.rad[k] = Float.intBitsToFloat(r[k]);
		}
		p.radFound = in.getIntOr("rad_found", 0);
		p.sw = in.getIntOr("sw", -1);
		int console = in.getIntOr("console", 0);
		p.rodAuto = (console & 1) != 0;
		p.srBlocked = (console & 2) != 0;
		p.irBlocked = (console & 4) != 0;
		p.prLoBlocked = (console & 8) != 0;
		p.autoSetpoint = in.getDoubleOr("auto_sp", 0);
		p.sdBanks = in.getDoubleOr("sd_banks", ReactorCore.STEPS);
		p.rodsMoved = in.getDoubleOr("rods_moved", 0);
		p.nis = in.getDoubleOr("nis", 0);
		p.sur = in.getDoubleOr("sur", 0);
		p.nisChange = in.getDoubleOr("nis_change", 0);
		int[] t = in.getIntArray("tilt").orElse(new int[0]);
		for (int k = 0; k < 4 && k < t.length; k++) {
			p.tilt[k] = t[k] / 10000.0;
		}
		int[] fuel = in.getIntArray("fuel").orElse(new int[0]);
		if (fuel.length == 3) {
			p.fuelLoaded = fuel[0];
			p.coreSize = fuel[1];
			p.burnup = fuel[2] / 10000.0;
		}
		int feed = in.getIntOr("feed", 0);
		p.cpFound = feed & 7;
		p.cpRunning = feed >> 3 & 7;
		p.cpAuto = feed >> 6 & 7;
		p.cpAutoStarted = feed >> 9 & 7;
		p.mfpFound = feed >> 12 & 3;
		p.mfpRunning = feed >> 14 & 3;
		p.mfpTripped = feed >> 16 & 3;
		p.cpShort = (feed & 1 << 18) != 0;
		p.feedCap = in.getDoubleOr("feed_cap", ReactorCore.FEED_FREE);
		return p;
	}

	// ------------------------------------------------------------------ the plant worked out

	static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	static double f(double celsius) {
		return celsius * 9 / 5 + 32;
	}

	/** The share of rated electric output the generator is carrying. */
	public double load() {
		return ratedJe <= 0 ? 0 : clamp(electric / (double) ReactorCore.RATED_JE, 0, 2);
	}

	public boolean online() {
		return present && electric > 0;
	}

	/** The turbine latched: not tripped from its board, nor by the reactor's trip, with a turbine to latch. */
	/**
	 * Turbine hall part 1: the turbine's rotor standing hot with its turning gear out (it bows; the turbine board's
	 * ECCENTRICITY HI and its eccentricity meter high).
	 */
	public boolean rotorBowing() {
		return present && steamToPower && !latched() && !turningGear && turbineSpeed < ReactorCore.ZERO_SPEED && temperature > 150;
	}

	public boolean latched() {
		return present && steamToPower && !turbineTripped && !scrammed;
	}

	/** The generator's field breaker: closed with the turbine latched and near its speed. */
	public boolean field() {
		return latched() && turbineSpeed > 0.95;
	}

	public boolean hot() {
		return present && temperature > 250;
	}

	/** The loops' average temperature (deg F). */
	public double tavg() {
		return present ? f(temperature) : 0;
	}

	/**
	 * Tref (deg F): Tavg's programme from the turbine's load, from no-load 545 deg F to 581 at full load (the plant's
	 * own settled temperatures); with the turbine off line, no-load.
	 */
	public double tref() {
		return present ? f(285 + 20 * Math.min(1, load())) : 0;
	}

	/** Pressurizer pressure (psig): 400 cold, 2235 at no-load temperature and above, rising past it as it overheats. */
	public double pzrPress() {
		if (!present) {
			return 0;
		}
		double p = 400 + (2235 - 400) * clamp((temperature - 30) / 250, 0, 1);
		return temperature > 305 ? p + (temperature - 305) * 4 : p;
	}

	/** Pressurizer level (%): its programme from 25 at no load to 60 at full power, swelling as the coolant overheats. */
	public double pzrLevel() {
		return present ? clamp(25 + 35 * Math.min(1, heat) + Math.max(0, temperature - 305) * 0.6, 0, 100) : 0;
	}

	/** Each running loop's temperature rise (deg F): 61 at full power with full flow, more as flow falls. */
	double deltaT() {
		double flows = 0;
		for (int k = 0; k < loops; k++) {
			flows += loopFlow[k];
		}
		if (flows < 0.05) {
			return 0;
		}
		double removed = Math.min(heat, cooling * waterShare);
		return Math.min(120, 61 * 4 * removed / flows);
	}

	public boolean rcpRunning(int k) {
		return present && pumps && k < loops && (rcpStopped & 1 << k) == 0;
	}

	public boolean loopLow(int k) {
		return present && pumps && k < loops && loopFlow[k] < ReactorCore.LOW_FLOW;
	}

	/** A diesel generator's run: seconds since its start (negative stopped). */
	public double edgSeconds(int k, double time) {
		Edg e = edg[k];
		return e.present && e.running ? Math.max(0, (time - e.since) / 20.0) : -1;
	}

	/** A diesel's speed (fraction of 514 rpm): cranked on air for a second and a half, then running up to speed. */
	public double edgSpeed(int k, double time) {
		double s = edgSeconds(k, time);
		if (s < 0) {
			return 0;
		}
		return s < 1.5 ? 0.12 * Math.min(1, s / 0.25) : s < 4 ? 0.12 + (s - 1.5) / 2.5 * 0.88 : 1;
	}

	/** Seconds since its output breaker closed (-1 open): it closes five seconds after a start (DieselFuel.BREAKER). */
	public double edgBreakerSeconds(int k, double time) {
		Edg e = edg[k];
		return e.present && e.running && e.breakerAt >= 0 && time >= e.breakerAt ? (time - e.breakerAt) / 20.0 : -1;
	}

	/** Its output breaker closed (round 31 part 2: it can be tripped and closed again from its panel). */
	public boolean edgBreaker(int k, double time) {
		return edgBreakerSeconds(k, time) >= 0;
	}

	/** Load sequencer steps picked up since the breaker closed (0 to 8). */
	public int edgSteps(int k, double time) {
		double s = edgBreakerSeconds(k, time);
		int n = 0;
		for (int j = 0; j < DieselFuel.STEP_KW.length; j++) {
			if (s >= DieselFuel.STEP * j) {
				n = j + 1;
			}
		}
		return n;
	}

	public boolean edgReady(int k) {
		Edg e = edg[k];
		return e.present && !e.running && !e.fuelOut && e.auto && e.day >= DieselFuel.LOW;
	}

	// ------------------------------------------------------------------ round 28: the safety systems, steam and feed

	/** Safety injection in (with a reactor to inject into). */
	public boolean siOn() {
		return present && si;
	}

	/** Safety injection can be reset: in for a minute. */
	public boolean siResettable(double time) {
		return siOn() && time - siTime >= ReactorCore.SI_RESET;
	}

	/** Residual heat removal on shutdown cooling (the coolant below 175 deg C, the pumps on the hot legs). */
	public boolean shutdownCooling() {
		return present && temperature < 175;
	}

	/** A steam generator's narrow range level (fraction), wide range level (fraction). */
	public double nr(int k) {
		return clamp((sgWater[k] - ReactorCore.NR_LOW) / (ReactorCore.NR_HIGH - ReactorCore.NR_LOW), 0, 1);
	}

	public double wr(int k) {
		return clamp(sgWater[k] / ReactorCore.NR_HIGH, 0, 1);
	}

	/** The plant has steam generator k + 1 (a loop of the unit's). */
	public boolean sg(int k) {
		return present && pumps && k < loops;
	}

	public boolean msivOpen(int k) {
		return sg(k) && (msivClosed & 1 << k) == 0;
	}

	/** Every MSIV of the unit's generators shut. */
	public boolean msivAllShut() {
		if (!present || !pumps || loops == 0) {
			return false;
		}
		for (int k = 0; k < loops; k++) {
			if ((msivClosed & 1 << k) == 0) {
				return false;
			}
		}
		return true;
	}

	/** A generator's atmospheric relief valve open: opened by hand, or relieving (its MSIV shut with steam to let go). */
	public boolean arvOpen(int k) {
		return sg(k) && ((arvManual & 1 << k) != 0 || (msivClosed & 1 << k) != 0 && sgSteam[k] > 0.005);
	}

	/** A generator's aux feed flow control valve open (they stand open, ready). */
	public boolean afwValveOpen(int k) {
		return sg(k) && (afwShut & 1 << k) == 0;
	}

	/** A generator's steam pressure (psig): no-load 1092, 1000 at full load; on its relief valve (MSIV shut) 1125. */
	public double sgPress(int k) {
		if (!sg(k) || temperature < 100) {
			return 0;
		}
		if (!hot()) {
			return 1092 * Math.pow(clamp((temperature - 100) / 150, 0, 1), 2);
		}
		// On its relief valve 1125 psig; vented by hand it falls with the plant's cooling.
		if ((arvManual & 1 << k) != 0) {
			return clamp(1092 * Math.pow(clamp((temperature - 100) / 185, 0, 1), 2), 0, 1092);
		}
		return arvOpen(k) ? 1125 : 1092 - 92 * Math.min(1, load());
	}

	/** The auxiliary feed pump p (0 A, 1 B, 2 turbine-driven) running; the turbine-driven one turns on steam while the plant is hot. */
	public boolean afw(int pump) {
		// (diesels make real power: a motor-driven pump runs with power on its bus, the turbine-driven one with its DC)
		return present && (afwRunning & 1 << pump) != 0 && (power & (pump == 0 ? PW_AFW_A : pump == 1 ? PW_AFW_B : PW_TDAFW)) != 0;
	}

	/** An auxiliary feed pump turned on (its switch at START), whether or not it has power. */
	public boolean afwOn(int pump) {
		return present && (afwRunning & 1 << pump) != 0;
	}

	/** The grid at the switchyard (diesels make real power: lost to a strike on its lines). */
	public boolean grid() {
		return (power & ReactorCore.PW_GRID) != 0;
	}

	/** The plant's own buses live (the generator or the grid through SAT-1). */
	public boolean plantBus() {
		return (power & ReactorCore.PW_PLANT) != 0;
	}

	/** Safety bus 1A (k 0) or 1B (1) live. */
	public boolean bus(int k) {
		return (power & (k == 0 ? ReactorCore.PW_1A : ReactorCore.PW_1B)) != 0;
	}

	/** Safety bus k fed from the grid (through its RES XFMR's breaker). */
	public boolean offsite(int k) {
		return grid() && (resOpen & 1 << k) == 0;
	}

	/** The load sequencer's step reached on safety bus k: all of them on the grid, its diesel's sequence on it, none dead. */
	public int busSteps(int k, double time) {
		return offsite(k) ? DieselFuel.STEP_KW.length : edgBreaker(k, time) ? edgSteps(k, time) : 0;
	}

	/** A load on safety bus k at sequencer step {@code step} with power. */
	public boolean busLoad(int k, int step, double time) {
		return present && busSteps(k, time) >= step;
	}

	/** An auxiliary feed pump's flow (gpm): each generator's auxiliary feed shared between the pumps feeding it. */
	public double afwFlow(int pump) {
		double sum = 0;
		boolean td = afw(2) && temperature > 150;
		for (int k = 0; k < 4; k++) {
			if (!sg(k)) {
				continue;
			}
			boolean motor = afw(k < 2 ? 0 : 1);
			int feeding = (motor ? 1 : 0) + (td ? 1 : 0);
			boolean mine = pump == 2 ? td : motor && pump == (k < 2 ? 0 : 1);
			if (mine && feeding > 0) {
				sum += sgAux[k] * GPM / feeding;
			}
		}
		return sum;
	}

	public boolean mainFeed() {
		return present && steamToPower && !feedIso && feedCap > 0;
	}

	/**
	 * Main feed pump k (0 A, 1 B) running (turbine hall part 2: the hall's own, in service, untripped and with steam to
	 * turn it; with none at the hall, while main feed runs).
	 */
	public boolean mfpRun(int k) {
		if ((mfpFound & 1 << k) == 0) {
			return mfpFound == 0 && mainFeed();
		}
		return present && steamToPower && (mfpRunning & 1 << k) != 0 && (mfpTripped & 1 << k) == 0 && temperature > ReactorCore.MFP_STEAM;
	}

	/** Main feed pump k's switch: in service and untripped (with none at the hall, while main feed runs). */
	public boolean mfpInService(int k) {
		if ((mfpFound & 1 << k) == 0) {
			return mfpFound == 0 && mainFeed();
		}
		return present && (mfpRunning & 1 << k) != 0 && (mfpTripped & 1 << k) == 0;
	}

	public boolean anyLoLo() {
		for (int k = 0; k < 4; k++) {
			if (sg(k) && nr(k) < ReactorCore.LO_LO) {
				return true;
			}
		}
		return false;
	}

	public double rwstLevel() {
		return present ? 98 * (1 - rwstUsed) : 0;
	}

	public double cstLevel() {
		return present ? 92 * (1 - cstUsed) : 0;
	}

	/** The steam header's pressure (psig): the highest of the generators whose MSIV is open (none: it bleeds down). */
	public double steamHeader() {
		double best = 0;
		for (int k = 0; k < 4; k++) {
			if (msivOpen(k)) {
				best = Math.max(best, sgPress(k));
			}
		}
		return best;
	}

	/** An ECCS pump train's flows and pressures (gpm, psig): the coolant's pressure decides who can inject. */
	double eccs(String what, int train, double p) {
		boolean si = siOn(), sdc = shutdownCooling();
		boolean ccp = present && (train == 0 || si), rhr = si || sdc;
		return switch (what) {
			case "ccp.flow" -> ccp ? si ? 150 : 120 : 0;
			case "ccp.press" -> ccp ? 2630 : 0;
			case "sip.flow" -> si ? p >= 1520 ? 30 : 30 + 620 * (1520 - p) / 1520 : 0;
			case "sip.press" -> si ? p >= 1520 ? 1520 : p + 60 : 0;
			case "rhr.flow" -> si ? p > 190 ? 500 : 4000 : sdc ? 3000 : 0;
			case "rhr.press" -> si ? p > 190 ? 182 : p + 15 : sdc ? p + 20 : 0;
			case "rhr.hx_out" -> !present ? 0 : sdc ? Math.max(90, f(temperature) - 30) : si ? 95 : 86;
			case "rhr.hx_ctl" -> sdc ? 60 : 0;
			case "rhr.byp_ctl" -> si ? 100 : 0;
			case "ccw.flow" -> present && (train == 0 || si) ? si ? 7400 : 6200 : 0;
			case "ccw.ctl" -> present && (train == 0 || si) ? 55 : 0;
			case "sw.press" -> !present ? 0 : sw < 0 || (sw & 1 << train) != 0 ? 86 : 0;
			default -> 0;
		};
	}

	/** Component cooling's in-service heat exchanger without its service water (train A; B too once SI runs it). */
	public boolean swLost() {
		return sw >= 0 && ((sw & 1) == 0 || siOn() && (sw & 2) == 0);
	}

	/** A radiation monitor's reading (its meter's units) and its log (its scale's bottom when it reads less). */
	public double radReading(int n) {
		return n >= 1 && n <= RAD_MONITORS && (radFound & 1 << n - 1) != 0 ? rad[n - 1] : 0;
	}

	public boolean radFound(int n) {
		return n >= 1 && n <= RAD_MONITORS && (radFound & 1 << n - 1) != 0;
	}

	/** The control room's HVAC: its emergency mode, a part running or open (HvacRoom PARTS) by train. */
	public boolean hvEmergency() {
		return hvFound && (hvAlarms & HvacRoom.EMERGENCY_ON) != 0;
	}

	public boolean hvPart(int part, int train) {
		return hvFound && (hvState & 1 << train * 7 + part) != 0;
	}

	/** The synchroscope's angle (degrees, 0 in phase at 12 o'clock): locked on line, turning FAST at speed, resting off. */
	public double syncPhase(double time) {
		if (online()) {
			return 0;
		}
		if (latched() && turbineSpeed >= 0.99) {
			return (time * 4.5) % 360;
		}
		if (latched() && turbineSpeed > 0.5) {
			return 360 - (time * 31) % 360;
		}
		return 180;
	}

	// ------------------------------------------------------------------ round 30: the reactor control console

	/** A rod bank's position (steps out: 0 to 3 the shutdown banks, 4 to 7 control banks A to D in their overlap). */
	public double bankSteps(int bank) {
		if (!present) {
			return 0;
		}
		if (bank < 4) {
			return sdBanks;
		}
		double demand = (1 - rods / 100) * ReactorCore.SEQUENCE;
		return clamp(demand - ReactorCore.OVERLAP * (bank - 4), 0, ReactorCore.STEPS);
	}

	/** A bank's index by its name on the boards (sa, sb, sc, sd, ca, cb, cc, cd), or -1. */
	static int bank(String name) {
		return switch (name) {
			case "sa" -> 0;
			case "sb" -> 1;
			case "sc" -> 2;
			case "sd" -> 3;
			case "ca" -> 4;
			case "cb" -> 5;
			case "cc" -> 6;
			case "cd" -> 7;
			default -> -1;
		};
	}

	/** The source range's count rate (counts a second, before its detectors' range pegs it at a million). */
	public double srCps() {
		return present ? ReactorCore.SR_SOURCE + nis * ReactorCore.SR_CPS : 0;
	}

	/** A power range channel's reading (percent; channels N41 to N44 as 0 to 3): its quadrant's share, its calibration. */
	public double pr(int k) {
		return present ? nis * 100 * tilt[k] * PR_CAL[k] : 0;
	}

	public double prAverage() {
		return (pr(0) + pr(1) + pr(2) + pr(3)) / 4;
	}

	/** The power range channels' spread (percent). */
	public double prSpread() {
		double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
		for (int k = 0; k < 4; k++) {
			lo = Math.min(lo, pr(k));
			hi = Math.max(hi, pr(k));
		}
		return hi - lo;
	}

	/** The rods' speed (steps a minute of the control banks' sequence; nothing while they fall in on a trip). */
	public double rodSpeed() {
		return present && !scrammed ? Math.abs(rodsMoved) / 100 * ReactorCore.SEQUENCE * 1200 : 0;
	}

	/**
	 * A meter's reading by name (tools/board_live.py), in its scale's units, at game time {@code time} (ticks). Unknown
	 * names read 0.
	 */
	public double value(String name, double time) {
		double t = temperature;
		boolean on = present;
		double load = Math.min(1, load());
		double mw = present ? electric / (double) ReactorCore.RATED_JE * 1150 : 0;
		if (name.startsWith("loop") && name.length() > 5) {
			int k = name.charAt(4) - '1';
			String what = name.substring(6);
			boolean running = k < loops && loopFlow[k] > 0.05;
			double dt = deltaT();
			double tc = tavg() - dt / 2;
			return switch (what) {
				case "flow" -> on && pumps && k < loops ? loopFlow[k] * 100 : 0;
				case "thot" -> !on ? 0 : running ? tavg() + dt / 2 : tc;
				case "tcold" -> !on ? 0 : tc;
				default -> 0;
			};
		}
		if (name.startsWith("rcp") && name.endsWith(".amps")) {
			int k = name.charAt(3) - '1';
			return rcpRunning(k) ? 750 - 160 * clamp((t - 30) / 260, 0, 1) : 0;
		}
		if (name.startsWith("sg") && name.length() > 4 && name.charAt(3) == '.') {
			int k = name.charAt(2) - '1';
			if (k < 0 || k > 3) {
				return 0;
			}
			boolean frv = sg(k) && mainFeed() && (sgFeed[k] > 1e-4 || hot());
			return switch (name.substring(4)) {
				case "nr" -> sg(k) ? nr(k) * 100 : 0;
				case "wr" -> sg(k) ? wr(k) * 100 : 0;
				case "steam" -> sg(k) ? sgSteam[k] * SG_MLB : 0;
				case "feed" -> sg(k) ? sgFeed[k] * SG_MLB : 0;
				case "afw" -> sg(k) ? sgAux[k] * GPM : 0;
				case "press" -> sgPress(k);
				case "frv_out" -> frv ? clamp(sgFeed[k] * 70, 3, 100) : 0;
				case "arv_out" -> arvOpen(k) ? clamp(sgSteam[k] * 400, 5, 100) : 0;
				default -> 0;
			};
		}
		if (name.startsWith("acc") && name.length() > 4 && name.charAt(4) == '.') {
			double p = pzrPress();
			boolean dis = siOn() && p < 650;
			return switch (name.substring(5)) {
				case "press" -> !present ? 0 : dis ? Math.max(p, 150) : 650 - (name.charAt(3) - '0');
				case "level" -> !present ? 0 : dis ? 62 * Math.max(0, p - 150) / 500 : 61.5 + (name.charAt(3) - '0') * 0.3;
				default -> 0;
			};
		}
		if (train(name, "ccp", "sip", "rhr", "ccw")) {
			// The ECCS trains: ccp_a.flow, sip_b.press, rhr_a.hx_out, ccw_b.flow...
			return eccs(name.substring(0, 3) + name.substring(5), name.charAt(4) - 'a', pzrPress());
		}
		if (name.equals("sw_a.press") || name.equals("sw_b.press")) {
			return eccs("sw.press", name.charAt(3) - 'a', pzrPress());
		}
		if (name.startsWith("rm") && Character.isDigit(name.charAt(name.length() - 1)) || name.startsWith("rm") && name.endsWith(".log")) {
			boolean log = name.endsWith(".log");
			int n;
			try {
				n = Integer.parseInt(name.substring(2, log ? name.length() - 4 : name.length()));
			} catch (NumberFormatException e) {
				return 0;
			}
			double v = radReading(n);
			if (!log) {
				return v;
			}
			double floor = n >= 1 && n <= RAD_MONITORS ? RAD_FLOOR[RAD_UNIT[n - 1]] : 1;
			return Math.log10(Math.max(floor, v));
		}
		if (name.startsWith("bank.") && name.length() == 7) {
			int b = bank(name.substring(5));
			return b < 0 ? 0 : bankSteps(b);
		}
		if (name.length() == 3 && name.startsWith("pr") && name.charAt(2) >= '1' && name.charAt(2) <= '4') {
			return pr(name.charAt(2) - '1');
		}
		if (name.startsWith("edg_")) {
			int k = name.charAt(4) - 'a';
			double sp = edgSpeed(k, time), s = edgSeconds(k, time);
			double kw = s < 0 ? 0 : DieselFuel.kw(edgBreakerSeconds(k, time)), kvar = 0.45 * kw;
			double build = s < 3 ? 0 : 1 - Math.exp(-(s - 3) / 0.3) * (1 + (s - 3) / 0.3);
			return switch (name.substring(6)) {
				case "kv" -> 4.16 * build * Math.min(1, sp / 0.97);
				case "amps" -> Math.sqrt(kw * kw + kvar * kvar) / (Math.sqrt(3) * 4.16);
				case "kw" -> kw;
				case "kvar" -> kvar;
				case "hz" -> 60 * sp;
				case "rpm" -> 514 * sp;
				default -> 0;
			};
		}
		return switch (name) {
			// The reactor coolant system.
			case "rcs.tavg" -> tavg();
			case "rcs.tref" -> tref();
			case "rcs.press_wr" -> on ? pzrPress() + 15 : 0;
			case "pzr.press" -> pzrPress();
			case "pzr.level" -> pzrLevel();
			case "pzr.press_out" -> on ? clamp(50 + (2235 - pzrPress()) * 0.5, 0, 100) : 0;
			case "pzr.level_out" -> on ? 40 + 20 * Math.min(1, heat) : 0;
			case "pzr.spray_a", "pzr.spray_b" -> on ? clamp((pzrPress() - 2260) / 50 * 100, 0, 100) : 0;
			// Chemical and volume control.
			case "cvcs.vct_level" -> on ? 45 : 0;
			case "cvcs.charging" -> on ? 87 : 0;
			case "cvcs.letdown" -> on ? 75 : 0;
			case "cvcs.seal_inj" -> on ? 32 : 0;
			case "cvcs.ltdn_press" -> on ? 350 : 0;
			case "cvcs.ltdn_temp" -> on ? 115 : 70;
			case "cvcs.bast_level" -> 78;
			case "cvcs.vct_press" -> on ? 30 : 0;
			case "cvcs.chg_out" -> on ? 43 : 0;
			case "cvcs.ltdn_p_out" -> on ? 45 : 0;
			case "cvcs.ltdn_t_out" -> on ? 40 : 0;
			case "cvcs.seal_out" -> on ? 50 : 0;
			case "cvcs.vct_out" -> 0;
			// The turbine and the generator.
			case "turb.speed" -> turbineSpeed * 1800;
			case "turb.load_ref" -> online() ? mw : 0;
			case "turb.throttle" -> !on || t < 100 ? 0 : hot() ? 1092 - 92 * load : 1092 * Math.pow(clamp((t - 100) / 150, 0, 1), 2);
			case "turb.first_stage" -> online() ? 720 * load : 0;
			case "turb.vacuum" -> !on ? 0 : (alarms & ReactorCore.A_LOW_WATER) != 0 ? 21.5 : 27.8;
			case "turb.hotwell" -> on ? 52 : 0;
			case "turb.ehc_press" -> on ? 1600 : 0;
			case "turb.brg_oil" -> on ? 24 : 0;
			case "turb.hood_temp" -> !on ? 70 : online() ? 112 : latched() && turbineSpeed > 0.9 ? 168 : 100;
			case "turb.gland" -> hot() ? 4.2 : 0;
			case "turb.vib1", "turb.vib2" -> {
				double base = name.endsWith("1") ? 2.4 : 3.1;
				double s = turbineSpeed;
				// Through the rotor's critical speeds (about 1,000 to 1,300 rpm) the vibration peaks.
				double peak = 4.5 * Math.exp(-Math.pow((s - 0.64) / 0.06, 2));
				yield on ? base * Math.min(1, s * 1.5 + 0.2) + peak : 0;
			}
			case "gen.mw" -> mw;
			case "gen.mvar" -> online() ? 50 + 0.14 * mw : 0;
			case "gen.kv" -> field() ? 22 * Math.min(1, turbineSpeed) : 0;
			case "gen.ka" -> {
				double mvar = online() ? 50 + 0.14 * mw : 0;
				yield online() ? Math.sqrt(mw * mw + mvar * mvar) / (Math.sqrt(3) * 22) : 0;
			}
			case "gen.hz" -> online() ? 60 : 60 * turbineSpeed;
			case "gen.h2_press" -> on ? 60 : 0;
			case "gen.stator_flow" -> on ? 560 : 0;
			case "gen.field_amps" -> field() ? 1500 + 2400 * load : 0;
			case "gen.exc_volts" -> field() ? 150 + 260 * load : 0;
			case "gen.h2_purity" -> on ? 97.5 : 0;
			case "ehc.load_ref" -> online() ? load * 100 : 0;
			case "ehc.load_limit" -> latched() ? 100 : 0;
			case "ehc.valve_pos" -> online() ? load * 95 : latched() && turbineSpeed > 0.9 ? 4 : 0;
			case "ehc.speed_ref" -> latched() ? 90 : 0;
			case "tsi.eccen" -> !on ? 0 : rotorBowing() ? 5.8 : turbineSpeed < 0.05 ? 1.8 : 1.1;
			case "tsi.diff_exp" -> on ? 0.1 + 0.25 * load : 0;
			case "tsi.shell_exp" -> on ? 0.3 + 0.9 * clamp((t - 30) / 275, 0, 1) : 0;
			case "tsi.thrust" -> on ? 6 + 4 * load : 0;
			case "tsi.brg_metal" -> on ? 120 + 60 * Math.min(1, turbineSpeed) : 0;
			case "tsi.oil_temp" -> on ? 110 + 8 * Math.min(1, turbineSpeed) : 0;
			// The station's electrical system.
			case "swyd.kv" -> grid() ? 352 : 0;
			case "bus1a.kv" -> on && bus(0) ? 4.16 : 0;
			case "bus1b.kv" -> on && bus(1) ? 4.16 : 0;
			case "bus1a.amps" -> on && bus(0) ? 680 + (signal("bus.rhr_a", time) ? 130 : 0) + (signal("bus.si_a", time) ? 220 : 0) : 0;
			case "bus1b.amps" -> on && bus(1) ? 610 + (signal("bus.rhr_b", time) ? 130 : 0) + (signal("bus.si_b", time) ? 220 : 0) : 0;
			case "sync.phase" -> syncPhase(time);
			case "dc_a.volts" -> on ? mcc[0] && mccCharge[0] > 0.02 || !mcc[0] ? 132 : 124 : 0;
			case "dc_b.volts" -> on ? mcc[1] && mccCharge[1] > 0.02 || !mcc[1] ? 132 : 124 : 0;
			case "dc_a.amps" -> on ? mcc[0] && mccCharge[0] <= 0.02 ? -45 : 18 : 0;
			case "dc_b.amps" -> on ? mcc[1] && mccCharge[1] <= 0.02 ? -45 : 15 : 0;
			// Round 28: the safety systems.
			case "rwst.level" -> rwstLevel();
			case "cnt.press" -> on ? 0.31 + ((alarms & ReactorCore.A_FUEL_DAMAGED) != 0 ? 0.11 : 0) : 0;
			// Steam and feedwater.
			case "cst.level" -> cstLevel();
			case "afw.press" -> on && afwRunning != 0 ? 1240 : 0;
			case "afw_a.flow" -> afwFlow(0);
			case "afw_b.flow" -> afwFlow(1);
			case "afw_td.flow" -> afwFlow(2);
			case "afw_td.speed" -> afw(2) && t > 150 ? 3900 : 0;
			case "afw_td.spd_out" -> afw(2) && t > 150 ? 78 : 0;
			case "afw.total" -> afwFlow(0) + afwFlow(1) + afwFlow(2);
			case "fw.hdr_press" -> mainFeed() ? 1185 : 0;
			case "mfp_a.spd_out" -> mfpRun(0) ? 40 + 30 * load : 0;
			case "mfp_b.spd_out" -> mfpRun(1) ? 40 + 30 * load : 0;
			case "fw.total" -> {
				double sum = 0;
				for (int k = 0; k < 4; k++) {
					sum += sg(k) ? sgFeed[k] * SG_MLB : 0;
				}
				yield sum;
			}
			case "stm.hdr" -> steamHeader();
			// The control room's air and the buildings' exhausts.
			case "cr.dp" -> hvFound ? hvEmergency() ? 0.152 : 0.125 : 0;
			case "cr.temp" -> hvFound ? hvPart(HvacRoom.CH, 0) || hvPart(HvacRoom.CH, 1) ? 72.4 : 84 : 0;
			case "efu_a.flow" -> hvPart(HvacRoom.EFU, 0) ? HvacRoom.EFU_CFM : 0;
			case "efu_b.flow" -> hvPart(HvacRoom.EFU, 1) ? HvacRoom.EFU_CFM : 0;
			case "vent.flow" -> on ? 118 - (cntIso ? 26 : 0) : 0;
			case "purge.flow" -> on && !cntIso ? 1000 : 0;
			case "aux.flow" -> on ? 60 : 0;
			// Round 29: the remote shutdown panel's source range (counts a second, log: its detectors' range is 1 to a
			// million, past which it pegs) and startup rate (decades a minute: a third of one falling after a trip).
			// (Round 30: from the nuclear instruments, its own channel always on.)
			case "sr.log" -> on ? Math.log10(clamp(srCps(), 1, 1e6)) : 0;
			case "rsp.sur" -> on ? sur : 0;
			// Round 30: the reactor control console's nuclear instruments (the source range's high voltage off while
			// blocked: its meters read nothing), rod control.
			case "sr1.log" -> on && !srBlocked ? Math.log10(clamp(srCps(), 1, 1e6)) : 0;
			case "sr2.log" -> on && !srBlocked ? Math.log10(clamp(srCps() * 1.08, 1, 1e6)) : 0;
			case "sr.sur" -> on && !srBlocked ? sur : 0;
			case "ir1.log" -> Math.log10(Math.max(1e-11, on ? nis * ReactorCore.IR_AMPS : 0));
			case "ir2.log" -> Math.log10(Math.max(1e-11, on ? nis * ReactorCore.IR_AMPS * 0.92 : 0));
			case "ir.sur" -> on ? sur : 0;
			case "pr.avg" -> prAverage();
			case "rod.speed" -> rodSpeed();
			case "rod.sp" -> on && rodAuto ? autoSetpoint * 100 : 0;
			case "fb.flow" -> on ? 24 : 0;
			default -> 0;
		};
	}

	/** A lamp's or status light's state by name (tools/board_live.py). Unknown names are off. */
	public boolean signal(String name, double time) {
		boolean on = present;
		if (name.startsWith("dw.")) {
			// Round 32: the plant display wall's mimic (its lines' flow, the core, the buses).
			return DisplayWall.signal(this, name, time);
		}
		if (name.startsWith("rcp") && name.endsWith(".run")) {
			return rcpRunning(name.charAt(3) - '1');
		}
		if (name.startsWith("sg") && name.length() > 4 && name.charAt(3) == '.') {
			int k = name.charAt(2) - '1';
			if (k < 0 || k > 3) {
				return false;
			}
			return switch (name.substring(4)) {
				case "arv" -> arvOpen(k);
				case "frv" -> sg(k) && mainFeed() && (sgFeed[k] > 1e-4 || hot());
				case "frv_auto" -> sg(k) && mainFeed();
				case "afwv" -> afwValveOpen(k);
				default -> false;
			};
		}
		if (name.startsWith("msiv") && name.endsWith(".open")) {
			return msivOpen(name.charAt(4) - '1');
		}
		if (name.startsWith("bank.") && name.endsWith(".bottom") && name.length() == 14) {
			// A rod bank's rod bottom lights (round 30).
			int b = bank(name.substring(5, 7));
			return on && b >= 0 && bankSteps(b) < 6;
		}
		if (name.startsWith("fwi.sg")) {
			return sg(name.charAt(6) - '1') && feedIso;
		}
		if (name.startsWith("rm") && name.indexOf('.') > 2) {
			int dot = name.indexOf('.');
			int n;
			try {
				n = Integer.parseInt(name.substring(2, dot));
			} catch (NumberFormatException e) {
				return false;
			}
			if (n < 1 || n > RAD_MONITORS) {
				return false;
			}
			double v = radReading(n);
			boolean found = radFound(n);
			return switch (name.substring(dot + 1)) {
				case "high" -> found && v >= RAD_HIGH[n - 1];
				case "alert" -> found && v >= RAD_ALERT[n - 1];
				case "alarm" -> found && (v >= RAD_ALERT[n - 1]);
				case "ok" -> found;
				case "fail" -> !found;
				default -> false;
			};
		}
		if (name.startsWith("rm") && name.endsWith("_fail")) {
			try {
				return !radFound(Integer.parseInt(name.substring(2, name.length() - 5)));
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (name.startsWith("hv.") && name.length() > 5 && name.charAt(name.length() - 2) == '_') {
			int train = name.charAt(name.length() - 1) - '0';
			int part = switch (name.substring(3, name.length() - 2)) {
				case "d1" -> HvacRoom.D1;
				case "ahu" -> HvacRoom.AHU;
				case "d2" -> HvacRoom.D2;
				case "efu" -> HvacRoom.EFU;
				case "d3" -> HvacRoom.D3;
				case "ch" -> HvacRoom.CH;
				case "pump" -> HvacRoom.PUMP;
				default -> -1;
			};
			return part >= 0 && train >= 0 && train <= 1 && hvPart(part, train);
		}
		if (train(name, "ccp", "sip", "rhr", "ccw", "cfc", "afw", "mfp")) {
			// The trains' equipment: ccp_a.run, sip_b.run, cfc_a.slow, afw_a.run, mfp_b.run...
			int train = name.charAt(4) - 'a';
			String what = name.substring(0, 3) + name.substring(5);
			boolean si = siOn();
			return switch (what) {
				case "ccp.run" -> on && (train == 0 || si);
				case "sip.run" -> si;
				case "rhr.run" -> si || shutdownCooling();
				case "ccw.run" -> on && (train == 0 || si);
				case "cfc.run" -> on;
				case "cfc.fast" -> on && !si;
				case "cfc.slow" -> si;
				case "afw.run" -> afw(train);
				case "mfp.run" -> mfpRun(train);
				default -> false;
			};
		}
		return switch (name) {
			case "plant.present", "auto" -> on;
			case "p8" -> on && fission > ReactorCore.P8;
			case "p9" -> on && fission > ReactorCore.P9;
			// Round 30: the source range's high voltage goes off when its trip is blocked (above P-6).
			case "sr_hv_off" -> on && srBlocked;
			case "p6" -> on && nis >= ReactorCore.P6;
			case "p7" -> on && fission > ReactorCore.P7;
			case "p10" -> on && nis >= ReactorCore.P10;
			case "p13" -> online() && load() > 0.1;
			case "sr_blk", "sr.blocked" -> on && srBlocked;
			case "ir_blk", "ir.blocked" -> on && irBlocked;
			case "pr_blk", "prlo.blocked" -> on && prLoBlocked;
			case "rod_auto", "rods.auto" -> on && rodAuto;
			case "rod_man" -> on && !rodAuto;
			case "sd_out" -> on && sdBanks >= ReactorCore.STEPS - 0.01;
			case "rps_normal", "rps.normal" -> on && protection;
			case "rods.moving_in" -> on && !scrammed && rodsMoved > 1e-6;
			case "rods.moving_out" -> on && !scrammed && rodsMoved < -1e-6;
			case "backup_htrs", "pzr.htr_backup" -> on && temperature > 60 && (pzrPress() < 2210 || temperature < 280);
			case "pzr.htr_prop" -> on && temperature > 60 && pzrPress() < 2260;
			case "pzr.porv" -> pzrPress() > 2335;
			case "ltop", "rhr_on" -> on && temperature < 175;
			case "cvcs.chg_a", "cvcs.chg_line", "cvcs.orifice" -> on;
			// Letdown isolates with the containment (phase A); the second charging pump starts on safety injection.
			case "cvcs.ltdn_line" -> on && !cntIso;
			case "cvcs.chg_b" -> siOn();
			case "cvcs.chg_c", "cvcs.aux_spray", "cvcs.orifice45" -> false;
			// The turbine.
			case "turb.valves", "st.latched", "turb_latched", "ehc.oper_auto", "st.oper_auto", "ehc.ref", "ehc.s1800", "ehc.med" -> latched();
			// The EHC's modes and tests not in use, the standby pumps.
			case "ehc.clear", "ehc.demand", "ehc.fast", "ehc.imp_out", "ehc.mw_out", "ehc.ost", "ehc.s100", "ehc.s500", "ehc.single", "ehc.slow",
					"ehc.turb_manual", "ehc.vtest", "st.ehc_b", "st.emerg_oil", "st.ost", "st.turb_manual", "st.valve_test" -> false;
			case "st.tripped" -> on && steamToPower && !latched();
			case "st.online", "gen_online", "st.gen_online", "gen.bkr", "ehc.load_rate", "ehc.mw_in", "ehc.imp_in", "st.uat" -> online();
			case "st.at_speed" -> on && turbineSpeed > 0.99;
			case "st.turn_gear", "turn_gear" -> on && steamToPower && !latched() && turningGear && turbineSpeed <= ReactorCore.TURNING_GEAR + 1e-4;
			case "st.lift_oil", "lift_oil" -> on && steamToPower && turbineSpeed < 0.33;
			case "st.ehc_a", "st.seal_oil" -> on;
			case "st.aux_oil" -> on && turbineSpeed < 0.9;
			case "st.dump_armed", "dump_armed" -> on && temperature > 280 && !online();
			case "st.hood_spray" -> latched() && turbineSpeed > 0.9 && !online();
			case "st.hp_drains", "hp_drains" -> on && steamToPower && (!online() || load() < 0.1);
			case "st.load_limit" -> online() && electric >= limit;
			case "ehc.go" -> latched() && turbineSpeed < 0.99;
			case "ehc.hold" -> latched() && turbineSpeed >= 0.99 && !online();
			case "ehc.runback" -> latched() && temperature >= 340;
			case "gen.field" -> field();
			// The electrical system.
			// Diesels make real power: the grid at the switchyard (lost to a strike), the RES XFMRs' breakers to the buses.
			case "st.offsite", "st.reserve", "swyd.rat_a", "swyd.rat_b" -> grid();
			case "on_reserve" -> on && !online() && grid();
			case "sync.lamp" -> !online() && latched() && turbineSpeed > 0.5 && Math.cos(Math.toRadians(syncPhase(time))) < 0;
			case "sync_perm" -> !online() && latched() && turbineSpeed >= 0.99 && Math.cos(Math.toRadians(syncPhase(time))) > 0.985;
			case "close_blocked" -> on && steamToPower && !online() && !(latched() && turbineSpeed >= 0.99);
			case "bus.rat_a" -> (resOpen & 1) == 0;
			case "bus.rat_b" -> (resOpen & 2) == 0;
			case "edg_a.running", "edg_a_run" -> edg[0].present && edg[0].running;
			case "edg_b.running", "edg_b_run" -> edg[1].present && edg[1].running;
			case "edg_a.bkr", "edg_a_bkr", "b1a_edg" -> edgBreaker(0, time);
			case "edg_b.bkr", "edg_b_bkr", "b1b_edg" -> edgBreaker(1, time);
			case "st.edg_a_ready" -> edgReady(0);
			case "st.edg_b_ready" -> edgReady(1);
			// (each with power on its bus: on its diesel, once the load sequencer has reached its step)
			case "bus.ccw_a" -> busLoad(0, 4, time);
			case "bus.chg_a" -> busLoad(0, 8, time);
			// Towers and pump houses build: with the lake intake found, its service water pumps (SWP-1A, 1B: train A).
			case "bus.sw_a" -> sw < 0 ? busLoad(0, PlantPower.STEP_SW, time) : on && (sw & 3 << 9) != 0;
			// Train B's pumps stand by, started by safety injection (round 28).
			case "bus.ccw_b" -> siOn() && busLoad(1, 4, time);
			case "bus.chg_b" -> siOn() && busLoad(1, 8, time);
			case "bus.sw_b" -> sw < 0 ? siOn() && busLoad(1, PlantPower.STEP_SW, time) : on && (sw & 3 << 11) != 0;
			case "bus.lc_1a" -> (on || lc) && bus(0);
			case "bus.lc_1b" -> (on || lc) && bus(1);
			case "bus.rhr_a" -> busLoad(0, PlantPower.STEP_RHR, time) && (on && temperature < 175 || siOn() || !offsite(0));
			case "bus.rhr_b" -> busLoad(1, PlantPower.STEP_RHR, time) && (on && temperature < 175 || siOn() || !offsite(1));
			case "bus.si_a" -> siOn() && busLoad(0, 2, time);
			case "bus.si_b" -> siOn() && busLoad(1, 2, time);
			case "st.lc" -> lc && lcCharge > 0.02;
			case "st.lc_main" -> lc && lcMain;
			case "st.mcc_a" -> mcc[0] && mccCharge[0] > 0.02;
			case "st.mcc_b" -> mcc[1] && mccCharge[1] > 0.02;
			case "st.dc_a", "st.dc_b" -> on;
			// Round 28: the safety systems.
			case "si.on", "bit_flow", "chg_rwst", "ebfs_a.run", "ebfs_b.run" -> siOn();
			case "sw_b.run" -> sw < 0 ? siOn() : on && (sw & 2) != 0;
			case "sw_a.run" -> sw < 0 ? on : on && (sw & 1) != 0;
			case "si.reset_ok" -> siResettable(time);
			case "cia.on", "cia_done" -> on && cntIso;
			case "cia.open" -> on && !cntIso;
			case "fwi.open" -> on && !feedIso;
			case "rhr.rwst" -> on && !shutdownCooling();
			case "afw.valves" -> on && afwRunning != 0 && (afwShut & 15) != 15;
			// Round 29: the remote shutdown panel.
			case "rsp.mcr" -> on && !remote;
			case "rsp.local" -> on && remote;
			case "rx.tripped" -> on && scrammed;
			case "ba_a.run", "ba_b.run" -> false;
			case "seq_a.done" -> edgSteps(0, time) >= 8;
			case "seq_b.done" -> edgSteps(1, time) >= 8;
			case "bd.iso" -> on && (afwRunning != 0 || si);
			case "p11", "si_blocked" -> on && pzrPress() < 1915;
			case "p4" -> on && scrammed;
			case "acc_iso" -> on && pzrPress() < 1000 && !si;
			case "rhr_sdc", "rhr_suct" -> shutdownCooling();
			case "afw_signal" -> siOn() || anyLoLo();
			case "cs_a.run", "cs_b.run", "csa_a", "csa_b", "h2r_a", "h2r_b", "rms.check" -> false;
			// Steam and feedwater.
			case "p12" -> on && tavg() < 540;
			case "dump_armed2" -> on && temperature > 280 && !online() && !msivAllShut();
			case "afw_td.run", "afw_td_run" -> afw(2);
			case "afw_a_run" -> afw(0);
			case "afw_b_run" -> afw(1);
			case "td_steam" -> afw(2) && temperature > 150;
			case "cst_makeup" -> on && cstUsed > 0.01 && afwRunning == 0;
			// The control room's HVAC and the ventilation.
			case "hv.normal" -> hvFound && !hvEmergency();
			case "hv.emergency" -> hvEmergency();
			case "hv.auto", "hv_auto" -> hvFound && hvMode == HvacRoom.AUTO;
			case "hv.found" -> hvFound;
			case "hv_missing" -> !hvFound;
			case "purge.fan", "purge.open" -> on && !cntIso;
			case "aux.fan_a", "aux.fan_b", "fb.fan", "vent_norm" -> on;
			default -> false;
		};
	}

	/** An annunciator window's alarm by its id (tools/board_live.py): whether its condition is in. Unknown ids never are. */
	public boolean alarm(String id, double time) {
		boolean on = present;
		double p = pzrPress();
		if (id.length() == 9 && id.startsWith("rcp") && id.endsWith("_trip")) {
			int k = id.charAt(3) - '1';
			return on && pumps && k < loops && (rcpStopped & 1 << k) != 0;
		}
		if (id.length() == 10 && id.startsWith("loop") && id.endsWith("_flow")) {
			return loopLow(id.charAt(4) - '1');
		}
		// Round 28: the steam generators' windows by number (sg1_lo, sf2_mis, arv3_open, msiv4_shut...).
		for (String head : new String[] {"msiv", "arv", "sg", "sf", "sp"}) {
			int h = head.length();
			if (id.length() > h + 2 && id.startsWith(head) && id.charAt(h) >= '1' && id.charAt(h) <= '4' && id.charAt(h + 1) == '_') {
				int k = id.charAt(h) - '1';
				if (!sg(k)) {
					return false;
				}
				return switch (head + "_" + id.substring(h + 2)) {
					case "sg_lo" -> nr(k) < 0.35;
					case "sg_lolo" -> nr(k) < ReactorCore.LO_LO;
					case "sg_dev" -> hot() && Math.abs(nr(k) - 0.5) > 0.07;
					case "sf_mis" -> sgSteam[k] - sgFeed[k] - sgAux[k] > 0.1;
					case "sp_lo" -> hot() && sgPress(k) < 800;
					case "arv_open" -> arvOpen(k);
					case "msiv_shut" -> (msivClosed & 1 << k) != 0;
					default -> false;
				};
			}
		}
		if (id.startsWith("rm") && (id.endsWith("_high") || id.endsWith("_alert"))) {
			int us = id.lastIndexOf('_');
			try {
				return signal("rm" + id.substring(2, us) + "." + id.substring(us + 1), time);
			} catch (NumberFormatException e) {
				return false;
			}
		}
		if (id.startsWith("edg_a_") || id.startsWith("edg_b_")) {
			int k = id.charAt(4) - 'a';
			Edg e = edg[k];
			double s = edgSeconds(k, time);
			return switch (id.substring(6)) {
				case "fail" -> e.present && e.fuelOut;
				case "day" -> e.present && e.day < DieselFuel.LOW;
				case "na" -> !e.present;
				case "lo" -> s >= 0 && s < 3;
				case "air" -> s >= 0 && s < 30;
				default -> false;
			};
		}
		return switch (id) {
			// The reactor's trip: the first out, and its state.
			case "trip_manual" -> scrammed && tripCause == ReactorCore.TRIP_MANUAL;
			case "trip_otdt" -> scrammed && tripCause == ReactorCore.TRIP_OTDT;
			case "trip_flow1" -> scrammed && tripCause == ReactorCore.TRIP_FLOW1;
			case "trip_flow2" -> scrammed && tripCause == ReactorCore.TRIP_FLOW2;
			case "trip_turbine" -> scrammed && tripCause == ReactorCore.TRIP_TURBINE;
			case "rx_tripped", "rtb_open" -> on && scrammed;
			case "rps_bypass" -> on && !protection;
			case "loop_out" -> on && pumps && loops < 4;
			// The core and the coolant.
			case "tavg_hi" -> on && tavg() > 590;
			case "tavg_dev" -> online() && Math.abs(tavg() - tref()) > 5;
			case "otdt_alert" -> on && temperature >= 340;
			case "cet_hi" -> (alarms & ReactorCore.A_HIGH_TEMP) != 0;
			case "subcool_lo" -> on && temperature >= 345;
			case "fuel_fail", "cnt_rad" -> (alarms & ReactorCore.A_FUEL_DAMAGED) != 0;
			case "fuel_burnup" -> (alarms & ReactorCore.A_FUEL_SPENT) != 0;
			case "core_empty" -> (alarms & ReactorCore.A_NO_FUEL) != 0;
			case "refuel_open" -> (alarms & ReactorCore.A_REFUEL) != 0;
			case "unit_incomplete", "ltdn_iso", "ltdn_flow_lo" -> (alarms & ReactorCore.A_NOT_FORMED) != 0;
			case "rod_limit" -> on && fission > 0.02 && rods > 75;
			// Round 30: the reactor control console's rod control and nuclear instruments.
			case "rod_bottom" -> on && scrammed && rods >= 99.5;
			case "sd_banks" -> on && sdBanks < ReactorCore.STEPS - 0.01;
			case "auto_dev" -> on && rodAuto && !scrammed && Math.abs(fission - autoSetpoint) > 0.03;
			case "sr_hi" -> on && !srBlocked && srCps() > 1e5;
			case "ir_hi" -> on && !irBlocked && nis > 0.2;
			case "pr_lo_hi" -> on && !prLoBlocked && nis > 0.25;
			case "pr_pos" -> on && nisChange > 0.03;
			case "pr_neg" -> on && nisChange < -0.05;
			case "sur_hi" -> on && sur > 1;
			case "nis_dev" -> on && nis >= ReactorCore.P10 && prSpread() > 2;
			case "qptr" -> on && nis >= ReactorCore.P10 && Math.max(Math.max(tilt[0], tilt[1]), Math.max(tilt[2], tilt[3])) > 1.02;
			// The pressurizer.
			case "pzr_p_hi" -> p > 2310;
			case "pzr_p_lo" -> on && heat > 0.02 && temperature > 150 && p < 2185;
			case "porv_open", "prt_p_hi", "prt_t_hi" -> p > 2335;
			case "pzr_safety" -> p > 2485;
			case "spray_open" -> p > 2260;
			case "pzr_l_hi" -> pzrLevel() > 70;
			case "cw_flow_lo" -> on && (alarms & ReactorCore.A_LOW_WATER) != 0;
			// (Component cooling's heat exchangers lose their service water: the intake's train A, or B once SI runs it.)
			case "ccw_t_hi" -> on && ((alarms & ReactorCore.A_LOW_WATER) != 0 || swLost());
			// The turbine.
			case "tt_manual" -> on && turbineTripped;
			case "tt_reactor" -> on && steamToPower && scrammed && !turbineTripped;
			case "turb_tripped", "sv_closed", "cv_closed", "iv_closed" -> on && steamToPower && !latched();
			case "runback" -> latched() && temperature >= 340;
			case "speed_dev" -> latched() && turbineSpeed < 0.99;
			case "load_limit", "gen_limited" -> online() && electric >= limit;
			case "turb_vib" -> on && !online() && turbineSpeed > 0.58 && turbineSpeed < 0.7;
			case "gen_bkr_open" -> on && steamToPower && !online();
			case "field_open" -> on && steamToPower && !field();
			case "vac_lo" -> on && temperature > 100 && (alarms & ReactorCore.A_LOW_WATER) != 0;
			case "dump_open" -> on && temperature > 280 && heat > 0.02 && !online();
			case "hood_t_hi", "hood_spray" -> latched() && turbineSpeed > 0.9 && !online();
			// Turbine hall part 1: a hot rotor standing without its turning gear bows.
			case "eccen_hi" -> rotorBowing();
			// The electrical system.
			case "gen_bkr_trip", "fast_xfer" -> on && steamToPower && !online() && (scrammed || turbineTripped);
			// Diesels make real power: the grid lost at the switchyard, the safety buses dead or shed onto their diesels.
			case "swyd_v_lo", "line1_trip", "line2_trip" -> on && !grid();
			case "rat_a_trbl" -> on && !offsite(0);
			case "rat_b_trbl" -> on && !offsite(1);
			case "b1a_uv" -> on && !bus(0);
			case "b1b_uv" -> on && !bus(1);
			case "load_shed" -> on && (!offsite(0) || !offsite(1)) && !grid();
			case "mt_missing" -> on && (alarms & ReactorCore.A_NO_POWER_OUT) != 0;
			case "rhr_a_auto" -> edgSteps(0, time) >= 3;
			case "rhr_b_auto" -> edgSteps(1, time) >= 3;
			case "si_a_auto" -> siOn() && edgSteps(0, time) >= 2;
			case "si_b_auto" -> siOn() && edgSteps(1, time) >= 2;
			case "sequencer" -> edgBreaker(0, time) && edgSteps(0, time) < 8 || edgBreaker(1, time) && edgSteps(1, time) < 8;
			case "lc_v_lo" -> lc && lcCharge <= 0.02;
			case "lc_main_open" -> lc && !lcMain;
			case "lc_fdr_open" -> lc && lcOpen > 0;
			case "mcc_a_lo", "chg_a_trbl" -> mcc[0] && mccCharge[0] <= 0.02;
			case "mcc_b_lo", "chg_b_trbl" -> mcc[1] && mccCharge[1] <= 0.02;
			// Round 28: safety injection and what it brings.
			case "si_act", "si_man_a", "si_man_b", "sip_a_as", "sip_b_as", "rhr_a_as", "rhr_b_as", "ccp_b_as", "ccw_b_as", "sw_b_as",
					"cfc_slow", "cfc_slow2", "ccw_nonsafe" -> siOn();
			case "si_timer" -> siOn() && !siResettable(time);
			case "fwi", "fwi2", "frv_shut" -> on && feedIso;
			case "cia", "purge_iso", "purge_iso2" -> on && cntIso;
			case "msi", "msi2" -> msivAllShut();
			case "si_mini" -> siOn() && p >= 1520;
			case "rhr_mini" -> siOn() && p > 190;
			case "cold_inj" -> siOn() && p < 1520;
			case "acc1_dis", "acc2_dis", "acc3_dis", "acc4_dis" -> siOn() && p < 650;
			case "rwst_lo" -> on && rwstLevel() < 90;
			case "rwst_lolo" -> on && rwstLevel() < 30;
			case "rwst_empty" -> on && rwstLevel() < 8;
			case "recirc" -> siOn() && rwstLevel() < 30;
			case "h2_hi", "plume" -> (alarms & ReactorCore.A_FUEL_DAMAGED) != 0;
			case "cnt_hirad" -> (alarms & ReactorCore.A_FUEL_DAMAGED) != 0 || signal("rm3.high", time);
			case "ccw_hx_t" -> on && ((alarms & ReactorCore.A_LOW_WATER) != 0 || swLost());
			case "edg_a_si" -> siOn() && edg[0].present;
			case "edg_b_si" -> siOn() && edg[1].present;
			case "seq_a" -> edgBreaker(0, time) && edgSteps(0, time) < 8;
			case "seq_b" -> edgBreaker(1, time) && edgSteps(1, time) < 8;
			case "esf_dc_a" -> mcc[0] && mccCharge[0] <= 0.02;
			case "esf_dc_b" -> mcc[1] && mccCharge[1] <= 0.02;
			case "cr_emerg", "hv_emerg" -> hvEmergency();
			// The steam generators and feedwater.
			case "sg_hi" -> sgAny(0.7, true);
			case "sg_hihi" -> sgAny(0.8, true);
			case "sg_dry" -> {
				boolean dry = false;
				for (int k = 0; k < 4; k++) {
					dry |= sg(k) && wr(k) < 0.25;
				}
				yield dry;
			}
			case "stm_hdr_lo" -> hot() && steamHeader() < 800;
			case "stm_hdr_hi" -> steamHeader() > 1100;
			case "mssv" -> {
				boolean lift = false;
				for (int k = 0; k < 4; k++) {
					lift |= arvOpen(k) && sgSteam[k] > 0.1;
				}
				yield lift;
			}
			case "dump_open2" -> on && temperature > 280 && heat > 0.02 && !online() && !msivAllShut();
			case "cond_na" -> on && (alarms & ReactorCore.A_LOW_WATER) != 0;
			case "dump_blk" -> hot() && msivClosed != 0 && !online();
			case "mfp_a_trip", "mfp_b_trip" -> {
				// Turbine hall part 2: the hall's MFP tripped (it stays so until reset at its station).
				int k = id.charAt(4) - 'a';
				yield (mfpFound & 1 << k) != 0 ? on && (mfpTripped & 1 << k) != 0 : on && steamToPower && feedIso;
			}
			// Turbine hall part 2: the condensate pumps can't carry the feed; a standby pump started by AUTO.
			case "cond_p_lo" -> on && cpFound != 0 && cpShort;
			case "cond_auto" -> on && (cpAutoStarted & cpFound & cpRunning) != 0;
			case "fw_hdr_lo" -> hot() && !mainFeed();
			case "afw_as" -> on && afwRunning != 0 && (si || anyLoLo());
			case "afw_flow_lo" -> {
				double total = 0;
				for (int k = 0; k < 4; k++) {
					total += sg(k) ? sgAux[k] : 0;
				}
				yield siOn() && total < 0.01;
			}
			case "cst_lo" -> on && cstLevel() < 60;
			case "cst_lolo" -> on && cstLevel() < 20;
			case "afw_sw" -> on && cstLevel() < 10 && afwRunning != 0;
			case "afw_suct" -> on && cstLevel() < 1 && afwRunning != 0;
			case "bd_iso", "sample_iso" -> on && (afwRunning != 0 || si);
			// Radiation and ventilation.
			case "head_open" -> on && refuel;
			case "rsp_local" -> on && remote;
			case "cr_hirad", "hv_rad" -> hvFound && (hvAlarms & HvacRoom.A_HIGH_RAD) != 0;
			case "hv_rx_trip" -> hvFound && (hvAlarms & HvacRoom.A_RX_TRIP) != 0;
			case "hv_rx_temp" -> hvFound && (hvAlarms & HvacRoom.A_RX_HIGH_TEMP) != 0;
			case "hv_tox" -> hvFound && (hvAlarms & HvacRoom.A_TOXIC) != 0;
			case "hv_lowflow" -> hvFound && (hvAlarms & HvacRoom.A_LOW_FLOW) != 0;
			case "hv_chiller" -> hvFound && (hvAlarms & HvacRoom.A_CHILLER) != 0;
			case "hv_dp" -> hvFound && (hvAlarms & HvacRoom.A_LOW_DP) != 0;
			// The cable spreading room's sprinklers (punch list #15: SpreadingRoomFire).
			case "fire_csr" -> csrFound && (csrPhase == SpreadingRoomFire.ALARM || csrPhase == SpreadingRoomFire.RELEASED
					|| csrPhase == SpreadingRoomFire.FIRE_OUT);
			case "fire_pump" -> csrFound && (csrFlags & SpreadingRoomFire.FLOWING) != 0;
			case "fire_trbl" -> csrFound && (csrFlags & (SpreadingRoomFire.NO_VALVE | SpreadingRoomFire.NO_DETECTORS | SpreadingRoomFire.NO_HEADS)) != 0;
			case "eff_limit" -> signal("rm11.high", time);
			case "purge_rad" -> signal("rm12.high", time);
			case "aux_rad" -> signal("rm6.high", time);
			case "fb_emerg" -> signal("rm4.high", time) || signal("rm5.high", time);
			default -> false;
		};
	}

	/** A train's equipment's name: prefix_a.what or prefix_b.what for one of the prefixes. */
	private static boolean train(String name, String... prefixes) {
		if (name.length() < 7 || name.charAt(3) != '_' || name.charAt(5) != '.' || name.charAt(4) != 'a' && name.charAt(4) != 'b') {
			return false;
		}
		for (String prefix : prefixes) {
			if (name.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	/** Any of the unit's steam generators above (or below) a narrow range level. */
	private boolean sgAny(double level, boolean above) {
		for (int k = 0; k < 4; k++) {
			if (sg(k) && (above ? nr(k) > level : nr(k) < level)) {
				return true;
			}
		}
		return false;
	}
}
