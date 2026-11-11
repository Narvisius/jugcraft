package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.mojang.serialization.Codec;

/**
 * The state of one reactor core and the simple plant model that moves it (docs/features/nuclear.md, "How it runs"):
 * rod position sets the fission power, the unit's steam generators, pumps and condenser water decide how much heat
 * is carried away, the rest heats the core. After a shutdown the core keeps making decay heat for a while. The
 * protection system SCRAMs at 370 °C; with it off, 1,200 °C damages the fuel and shuts the core down for good (until
 * it is refuelled). Standard fuel with full cooling can never get there, even with every rod out.
 *
 * <p>Powers are fractions of the rated thermal power. Temperatures are °C. It lives in the reactor vessel's block
 * entity and ticks on the server.
 */
public final class ReactorCore {
	/** Electric output at 100% on standard fuel with four working loops (JE/t). */
	public static final long RATED_JE = 524_288;
	/** Cooling water the condenser needs at 100% (mB/t), once through. */
	public static final int FULL_WATER = 4_000;
	/** Fuel assemblies in a full core: a real four-loop core of 193, laid out in these rows (the console's core map). */
	public static final int[] CORE_ROWS = {7, 11, 13, 13, 15, 15, 15, 15, 15, 15, 15, 13, 13, 11, 7};
	public static final int CORE_SIZE = 193;
	/** Ticks an assembly lasts at 100% power (24 hours of play). */
	public static final double ASSEMBLY_LIFE = 20.0 * 60 * 60 * 24;
	public static final int NORMAL_TEMP = 300, ALARM_TEMP = 350, SCRAM_TEMP = 370, DAMAGE_TEMP = 1_200;
	/** Rod travel per tick: 2% a second when driven, and all the way in within two seconds on a SCRAM. */
	public static final double ROD_SPEED = 0.1, SCRAM_SPEED = 2.5;
	/** Decay heat right after shutdown, as a fraction of the power before it (it is part of the rated heat). */
	public static final double DECAY_SHARE = 0.065;
	/**
	 * How decay heat falls away after the power comes down: as a real core's does, quickly at first and then slowly for
	 * hours, as 1 / sqrt(1 + t / this) (ticks): 6.2% a minute after a trip from full power, 4.6% after ten, 2.8% after
	 * three quarters of an hour, 1.8% after two hours (a real core's falls quicker at first: here, as the fuel's life, the
	 * plant runs quicker). Diesels make real power (27 October 2026): it used to die away
	 * in a few minutes, so a core that lost all its cooling could never get hot enough to damage its fuel; now a station
	 * blackout with nothing feeding the steam generators boils them dry in about three quarters of an hour and damages
	 * the fuel in about an hour and a half (a real plant has hours).
	 * A plant left to itself after a trip cools to 175 C in about 45 minutes; its ARVs opened bring it there in a few,
	 * then its RHR pumps take it down ({@link #RHR_COOL}).
	 */
	public static final double DECAY_TICKS = 12_000;
	/**
	 * Over this (C) the core is uncovering (its water boiling off): it heats four times as fast for the heat left in it,
	 * the fuel's own small heat capacity.
	 */
	public static final double UNCOVERED = 400;
	/** The fastest the plant cools toward where its heat settles it (C a tick). */
	public static final double COOLDOWN = 0.1;
	/** The hottest a core gets: molten (C). */
	public static final double MELT_TEMP = 2_800;
	/**
	 * Natural circulation: with every coolant pump stopped, a hot core still drives this flow round each loop whose
	 * steam generator has water (the cold leg's denser water falling back to the core): enough to carry decay heat.
	 */
	public static final double NAT_CIRC = 0.05, NAT_CIRC_TEMP = 90;
	/**
	 * The residual heat removal pumps in service (under 175 C, with power on a safety bus): the plant settles this much
	 * cooler, and they take this much heat (a share of rated) whatever the loops are doing.
	 */
	public static final double RHR_COOL = 150, RHR_TEMP = 175, RHR_HEAT = 0.02;
	/** Ticks of the temperature trend (ten minutes, one sample a second). */
	public static final int TREND = 600;

	// Alarms (annunciator windows on the console).
	public static final int A_HIGH_TEMP = 1, A_SCRAM = 2, A_LOW_WATER = 4, A_LOOPS = 8, A_NO_POWER_OUT = 16, A_PROTECTION_OFF = 32,
			A_FUEL_SPENT = 64, A_FUEL_DAMAGED = 128, A_NOT_FORMED = 256, A_REFUEL = 512, A_NO_FUEL = 1024;

	// The control room's boards (round 27, nuclear/ControlBoard.java): the reactor trip's first out, and what their
	// working controls set: a coolant pump stopped, the turbine tripped, the generator breaker opened.
	/**
	 * Reactor trip causes (the first out): none, manual, overtemperature delta-T, low flow in one loop, in two loops,
	 * turbine trip; round 28: safety injection, a steam generator's low-low level.
	 */
	public static final int TRIP_NONE = 0, TRIP_MANUAL = 1, TRIP_OTDT = 2, TRIP_FLOW1 = 3, TRIP_FLOW2 = 4, TRIP_TURBINE = 5, TRIP_SI = 6,
			TRIP_SG_LO = 7;
	/**
	 * Permissives (fractions of rated power): above P-7 losing two loops' flow trips the reactor, above P-8 losing one
	 * does, above P-9 a turbine trip does (below it the steam dumps take the heat).
	 */
	public static final double P7 = 0.10, P8 = 0.48, P9 = 0.50;
	/** A loop's flow (fraction) under which the low flow trip counts it lost. */
	public static final double LOW_FLOW = 0.90;
	/** The turbine on its turning gear (a fraction of 1,800 rpm: 3 rpm). */
	public static final double TURNING_GEAR = 3 / 1800.0;
	/** Why the reactor last tripped (TRIP_*), until it is reset. */
	public int tripCause;
	/** Coolant pumps stopped from the reactor coolant board (bit k: loop k + 1's). */
	public int rcpStopped;
	/** Each loop's coolant flow (fraction): it coasts down over seconds when its pump stops, and comes back when it starts. */
	public final double[] loopFlow = {1, 1, 1, 1};
	/** Loops the unit had last tick (0 to 4). */
	public int loops = 4;
	/** The turbine tripped from its board: no power until it is latched again. */
	public boolean turbineTripped;
	/** The generator breaker opened from its board (or by the turbine's trip): no power until it is closed. */
	public boolean genOpen;
	/** The generator breaker's close switch turned: it closes when the synchroscope comes round (ControlBoard). */
	public boolean syncArmed;
	/** The turbine's speed (a fraction of 1,800 rpm): it coasts down when tripped and runs up when latched with steam. */
	public double turbineSpeed = 1;
	/**
	 * Turbine hall part 1 (Justin: "Local trip + turning gear"): the turning gear engaged. Its lever at the turbine puts it
	 * in only at zero speed (TurbineHall); engaged, its motor rolls the shaft at {@link #TURNING_GEAR}; it drops out by
	 * itself once steam rolls the turbine faster than that. A hot rotor left standing without it bows (the turbine
	 * board's ECCENTRICITY HI). Saves from before it stand engaged.
	 */
	public boolean turningGear = true;
	/** Under this the turbine counts as stopped (a fraction of 1,800 rpm: under one rpm): its turning gear may go in. */
	public static final double ZERO_SPEED = 1 / 1800.0;

	// Turbine hall part 2 (Justin: "Pumps you can switch, plant responds"): the hall's three 50% condensate pumps and two
	// 60% turbine-driven main feed pumps (TurbineHall). A plant without them in its hall runs as before: main feed has no
	// limit. With them, main feed is no more than the pumps running can carry; a standby condensate pump in AUTO starts
	// when those running can't carry the feed; an MFP lost above 60% runs the plant back to 55% with the rods in AUTO (in
	// manual the steam generators' levels fall to the low-low trip); both MFPs lost at power start the motor-driven
	// auxiliary feed pumps.
	/** The condensate pumps 1A to 1C running, standing by in AUTO, and started by AUTO (bits; 1C stands by to begin with). */
	public int cpRunning = 0b011, cpAuto = 0b100, cpAutoStarted;
	/** The main feed pumps 1A and 1B in service, and tripped (bits): a tripped one is reset at its station. */
	public int mfpRunning = 0b11, mfpTripped;
	/** Each pump at the hall heard from in the last two seconds (ticks left; not saved: they say so each half second). */
	public final int[] cpSeen = new int[3], mfpSeen = new int[2];
	/** A condensate pump stopped by hand stands by in AUTO only after this many ticks (not saved): the others start first. */
	public final int[] cpHold = new int[3];
	/** Main feed's capacity last tick (a fraction of a generator's full-power feed): {@link #FEED_FREE} with no pumps at the hall. */
	public double feedCap = FEED_FREE;
	/** Last tick: the condensate pumps running couldn't carry the feed (the condensate header's pressure low). */
	public boolean cpShort;
	public static final double FEED_FREE = 1.3, CP_SHARE = 0.5, MFP_SHARE = 0.6, RUNBACK = 0.55;
	/** An MFP needs steam to turn: the plant above this (C). */
	public static final double MFP_STEAM = 200;
	/** Ticks the running MFPs have had no condensate pump (they trip on low suction after two seconds); MFPs running last tick. */
	private int mfpStarved, mfpOnWas = -1;

	// Round 28: the safety systems and the steam generators (Justin: manual SI "Full actuation", the secondary board's
	// "MSIVs and aux feed"). Nothing here changes how a plant run from its console alone behaves: main feed keeps every
	// generator at its level, and only the boards close an MSIV, stop a pump or actuate safety injection.
	/** Ticks after safety injection before it can be reset (a minute). */
	public static final int SI_RESET = 1_200;
	/**
	 * A steam generator's water (a fraction of its normal inventory) at its narrow range's 0 and 100%, and under which
	 * its tubes uncover and it carries less heat (none dry).
	 */
	public static final double NR_LOW = 0.8, NR_HIGH = 1.2, DRY = 0.15;
	/** Ticks a generator at full power boils its normal water away with no feed (two minutes). */
	public static final double BOIL = 2_400;
	/** Each auxiliary feed pump's flow into each generator it feeds (a fraction of the generator's full-power feed). */
	public static final double AFW_FLOW = 0.06;
	/** The low-low level (narrow range 11%): it trips the reactor and starts the auxiliary feed pumps. */
	public static final double LO_LO = 0.11;
	/** Safety injection actuated, and when (game time; it can be reset {@link #SI_RESET} ticks later). */
	public boolean siActuated;
	public long siTime;
	/** Feedwater isolated, and the containment isolated (phase A): by safety injection, until it is reset. */
	public boolean feedIsolated, cntIsolated;
	/** The main steam isolation valves closed (bit k: steam generator k + 1's): its steam goes out the relief valves. */
	public int msivClosed;
	/** The auxiliary feed pumps running: bit 0 motor-driven A (generators 1 and 2), 1 motor-driven B (3 and 4), 2 the turbine-driven pump (all). */
	public int afwRunning;
	/** Each steam generator's water (a fraction of its normal inventory; 1 is 50% on the narrow range). */
	public final double[] sgWater = {1, 1, 1, 1};
	/** Last tick's flows at each generator (a fraction of its full-power flow): steam out, main feed and auxiliary feed in. */
	public final double[] sgSteam = new double[4], sgFeed = new double[4], sgAux = new double[4];
	/** What has been drawn from the refuelling water storage tank and the condensate storage tank (fractions of them). */
	public double rwstUsed, cstUsed;
	/** Generators at low-low level last tick (the auxiliary feed pumps start as a generator gets there). */
	private int loLoWas;
	/**
	 * Round 29 (the remote shutdown panel, and the steam and feedwater board): the auxiliary feed flow control valves
	 * shut (bit k: generator k + 1's gets no aux feed), and the ARVs opened by hand (bit k: generator k + 1 steams to
	 * atmosphere: it boils off more and cools the plant, {@link #ARV_COOL} degrees each while hot).
	 */
	public int afwValveShut, arvManual;
	/** An ARV open by hand: the extra steam it takes (a fraction of the generator's full-power flow), the cooling. */
	public static final double ARV_STEAM = 0.03, ARV_COOL = 30;
	/** ARVs open by hand on the unit's generators last tick. */
	private int arvsOpen;

	// Diesels make real power (Justin, 27 October 2026: "Safety loads need real power", "Storms and a test switch"):
	// the plant's electrical state as its vessel works it out each tick (PlantPower). A core nothing sets keeps all of
	// it powered, as before.
	/** The grid at the switchyard, the plant's own buses (the coolant, condensate and circulating water pumps, main feed), safety buses 1A and 1B live. */
	public static final int PW_GRID = 1, PW_PLANT = 2, PW_1A = 4, PW_1B = 8, PW_ALL = 15;
	/** What has power this tick (PW_* bits; not saved: worked out each tick). */
	public int power = PW_ALL;
	/** The reserve auxiliary transformers' breakers to safety buses 1A and 1B open (bits 0 and 1): tripped by a loss of offsite power, or from the board. */
	public int resOpen;
	/** The motor-driven aux feed pumps A and B with power (bits 0, 1: their bus live and, on its diesel, the load sequencer at their step). */
	public int afwPower = 3;
	/** The RHR pumps with power (bits 0, 1), the turbine-driven aux feed pump's DC control power. */
	public int rhrPower = 3;
	public boolean tdafwDc = true;
	/** The condensate pumps running when their bus went dead (bits): they start again when it comes back. */
	public int cpShed;
	/** When the switchyard's lines reclose (game time; -1: not lost, or not known), for the boards and the overview. */
	public long gridBack = -1;

	/** The plant's own buses live (fed by the generator, or from the grid through SAT-1). */
	public boolean plantBus() {
		return (power & PW_PLANT) != 0;
	}

	// Round 30: the reactor control console (Justin: the rods driven by holding the lever, "Overlap, shutdown on reset",
	// "AUTO holds power", the nuclear instruments' trip blocks "Working, no new trips"). Nothing here changes how a plant
	// run from the console's screen behaves: the rods follow the screen's setting as before and nothing new trips.
	/** A rod bank's full travel (steps), and how far a control bank is out when the next starts (their overlap). */
	public static final int STEPS = 228, OVERLAP = 128;
	/** The control banks' sequence: A, B, C and D in their overlap, all in (0) to all out. */
	public static final int SEQUENCE = OVERLAP * 3 + STEPS;
	/** The shutdown banks' travel a tick (steps): out in four seconds once the trip is reset, in in two with it. */
	public static final double SD_OUT = STEPS / 80.0, SD_IN = STEPS / 40.0;
	/** The rods' lever: each use while it is held moves the rods' setting this far past them (percent of travel). */
	public static final double LEVER_STEP = 0.6;
	/** AUTO: how far (percent of travel) the rods' setting must be off what holds the power before they step. */
	public static final double AUTO_DEADBAND = 0.25;
	/**
	 * The nuclear instruments: the intermediate range's current at rated power (amps), the source range's counts a
	 * second per unit of power and from the neutron source alone; P-6 (the intermediate range above 1E-10 amps) and P-10
	 * (the power range above 10%) as levels of power.
	 */
	public static final double IR_AMPS = 5e-4, SR_CPS = 1e11, SR_SOURCE = 150, P6 = 1e-10 / IR_AMPS, P10 = 0.10;
	/**
	 * The instruments follow the neutrons, which rise at most half a decade a second (a startup's climb through the
	 * ranges) and after a trip drop at once to 6% of the power before it, then fall a third of a decade a minute.
	 */
	static final double NIS_RISE = Math.pow(10, 0.5 / 20), NIS_FALL = Math.pow(10, -1 / 3.0 / 1200), PROMPT = 0.06;
	/** Rod control in AUTO, and the power it holds (a fraction of rated). */
	public boolean rodAuto;
	public double autoSetpoint;
	/** The shutdown banks' position (steps; the four move together). */
	public double sdBanks = STEPS;
	/** The source range's, the intermediate range's and the power range's low setpoint trips blocked (above P-6, P-10). */
	public boolean srBlocked, irBlocked, prLoBlocked;
	/**
	 * The rods last set from the console's screen: then the screen blocks the instruments' trips itself as the power
	 * passes P-6 and P-10 (a startup from the screen alone raises no alarms), until the lever, AUTO or a block button
	 * on the console's desk is used.
	 */
	public boolean screenRods;
	/**
	 * Last tick's (not saved): the nuclear instruments' level (a fraction of rated power), the startup rate (decades a
	 * minute), the level's change over the last two seconds, and the rods' move (percent of travel; in is positive).
	 */
	public double nis, sur, nisChange, rodsMoved;
	private double nisDelayed, nisLog = Double.NaN, nisTwo;
	private int nisTicks;

	// Driven by the console.
	public double rodTarget = 100;
	public boolean protection = true;
	public boolean scrammed;
	public boolean refuelOpen;
	/**
	 * Control is at a remote shutdown panel (its transfer switches turned): the control room's console can then only
	 * trip the reactor, until a remote shutdown panel hands control back.
	 */
	public boolean remote;
	// Plant state.
	public double rods = 100;
	public double fission;
	public double decay;
	/** Decay heat as it began falling, and the ticks since (it falls by {@link #DECAY_TICKS}); last tick's (to see it set from outside). */
	private double decayFrom, decayWas = -1;
	private long decayTime;
	public double temperature = 30;
	public boolean fuelDamaged;
	/** Burnup of each loaded assembly (0 fresh, 1 spent). */
	public final List<Double> fuel = new ArrayList<>();
	// Last tick's readouts.
	public double removed, waterShare = 1, cooling;
	public long electric;
	/** Cooling water asked for last tick (mB): less with a cooling tower. */
	public int need;
	public int alarms;
	public final int[] trend = new int[TREND];
	public int trendHead;
	private int trendTicks;

	/**
	 * A core design: the console's core map rows (assemblies per row), the assembly count, the electric output at 100%
	 * with full cooling (JE/t) and the cooling water the condenser needs at 100% with no cooling tower (mB/t).
	 */
	public record Design(String id, int[] rows, int size, long ratedJe, int water) {
		/** The stage-1 four-loop PWR: 193 assemblies. */
		public static final Design STANDARD = new Design("pwr", CORE_ROWS, CORE_SIZE, RATED_JE, FULL_WATER);
		/** The EPR: 241 assemblies (a 17-wide core), 1.6 times the output; it needs two turbine-generators to use it all. */
		public static final Design EPR = new Design("epr", new int[] {7, 9, 13, 15, 17, 17, 17, 17, 17, 17, 17, 17, 17, 15, 13, 9, 7}, 241,
				RATED_JE * 8 / 5, FULL_WATER * 8 / 5);
		/** A small modular reactor: 37 short assemblies, an eighth of the standard output. */
		public static final Design SMALL_MODULAR = new Design("smr", new int[] {4, 5, 6, 7, 6, 5, 4}, 37, RATED_JE / 8, FULL_WATER / 8);
	}

	/** The core's design (set by the vessel from its kind; not saved). */
	public Design design = Design.STANDARD;

	/**
	 * The plant around the core, from {@link ReactorUnit}: the share of heat its loops can carry, whether steam reaches a
	 * generator, whether it is complete, the most electric power its generators can make (JE/t) and the cooling tower
	 * capacity on its cooling water (a share of the core's heat whose water only needs make-up).
	 */
	public record Plant(double cooling, boolean steamToPower, boolean formed, long generatorLimit, double towerShare) {
		public Plant(double cooling, boolean steamToPower, boolean formed) {
			this(cooling, steamToPower, formed, Long.MAX_VALUE, 0);
		}
	}

	/** With a cooling tower, the water a condenser still needs is the evaporation and blowdown it makes up: 5%. */
	public static final double MAKEUP = 0.05;

	/** Heat produced this tick (fraction of rated). */
	public double heat() {
		return (1 - DECAY_SHARE) * fission + decay;
	}

	/** Fresh-enough assemblies' share of a full core. */
	public double activeFuel() {
		if (fuelDamaged) {
			return 0;
		}
		double sum = 0;
		for (double burnup : fuel) {
			if (burnup < 1) {
				sum += 1;
			}
		}
		return sum / design.size();
	}

	public int spentCount() {
		int n = 0;
		for (double burnup : fuel) {
			if (burnup >= 1 || fuelDamaged) {
				n++;
			}
		}
		return n;
	}

	public boolean shutDown() {
		return rods >= 99.9 && fission < 0.01;
	}

	/**
	 * One tick. {@code water} gives the cooling water available (mB) and takes what is used: it is asked for the
	 * full need and returns what it could supply. Returns the electric output for this tick (JE).
	 */
	public long tick(Plant plant, java.util.function.IntUnaryOperator water) {
		// Rods: driven toward the operator's setting, or all in on a SCRAM (or with the fuel damaged).
		// AUTO (round 30): the rods step to hold the power it was given (as assemblies are used up).
		if (rodAuto && !scrammed && !fuelDamaged && !refuelOpen) {
			double active = activeFuel();
			if (active > 0) {
				double need = Math.max(0, Math.min(100, 100 * (1 - autoSetpoint / active)));
				if (Math.abs(need - rodTarget) > AUTO_DEADBAND) {
					rodTarget = need;
				}
			}
		}
		double goal = scrammed || fuelDamaged || refuelOpen ? 100 : rodTarget;
		double speed = scrammed || fuelDamaged ? SCRAM_SPEED : ROD_SPEED;
		double before = rods;
		rods += Math.max(-speed, Math.min(speed, goal - rods));
		rodsMoved = rods - before;
		// The shutdown banks: in with a trip (or for refuelling), out by themselves once it is reset.
		double sdGoal = scrammed || fuelDamaged || refuelOpen ? 0 : STEPS;
		sdBanks += Math.max(-SD_IN, Math.min(SD_OUT, sdGoal - sdBanks));
		// Fission follows the rods (and the fuel) with a few seconds' lag; decay heat follows what fission did.
		double target = (1 - rods / 100) * activeFuel();
		fission += (target - fission) * (target > fission ? 0.01 : 0.2);
		if (fission < 1e-5) {
			fission = 0;
		}
		if (decay != decayWas) {
			// (set from outside: a showcase, a test, an old save)
			decayFrom = decay;
			decayTime = 0;
		}
		double steady = DECAY_SHARE * fission;
		double falling = decayFrom / Math.sqrt(1 + decayTime / DECAY_TICKS);
		if (steady >= falling) {
			decay = decayFrom = steady;
			decayTime = 0;
		} else {
			decayTime++;
			decay = falling;
		}
		if (decay < 1e-5) {
			decay = decayFrom = 0;
			decayTime = 0;
		}
		decayWas = decay;
		instruments();
		double heat = heat();
		// Cooling: the loops carry heat to the steam generators, and the condenser needs water to take it away. A loop
		// whose pump is stopped carries none once its flow has coasted down.
		int n = (int) Math.round(Math.max(0, Math.min(1, plant.cooling())) * 4);
		loops = n;
		double flows = 0;
		double[] carried = new double[4];
		boolean anyPump = false;
		for (int k = 0; k < n; k++) {
			anyPump |= (rcpStopped & 1 << k) == 0;
		}
		for (int k = 0; k < 4; k++) {
			double want = (rcpStopped & 1 << k) == 0 ? 1 : 0;
			if (k >= n) {
				loopFlow[k] = want;
				continue;
			}
			// Every pump stopped: a hot core drives natural circulation round the loops (one pump stopped among others
			// running only has its loop's flow reversed: none through its generator).
			if (want == 0 && !anyPump && temperature > NAT_CIRC_TEMP) {
				want = NAT_CIRC;
			}
			loopFlow[k] += (want - loopFlow[k]) * (want > loopFlow[k] ? 0.01 : 0.005);
			if (Math.abs(want - loopFlow[k]) < 1e-3) {
				loopFlow[k] = want;
			}
			// A loop carries heat through its steam generator only while the generator's tubes are covered.
			carried[k] = loopFlow[k] * Math.max(0, Math.min(1, sgWater[k] / DRY));
			flows += carried[k];
		}
		cooling = n == 0 ? plant.cooling() : plant.cooling() * flows / n;
		double tower = Math.max(0, Math.min(1, plant.towerShare()));
		int need = (int) Math.ceil(design.water() * Math.min(1, heat) * ((1 - tower) + tower * MAKEUP));
		this.need = need;
		// With the plant's own buses dead there is no condenser (no circulating water, no vacuum): the generators' steam
		// goes out their relief valves to the air (the ARVs and safeties open by themselves), which takes any heat.
		boolean condenser = plantBus();
		int got = condenser && need > 0 && cooling > 0 ? water.applyAsInt(need) : 0;
		waterShare = need <= 0 || !condenser ? 1 : got / (double) need;
		// (and under 175 C with a safety bus live, the RHR pumps take the plant down to cold shutdown)
		boolean rhr = temperature < RHR_TEMP && rhrPower != 0;
		removed = Math.min(heat, cooling * waterShare + (rhr ? RHR_HEAT : 0));
		feedTrain(n, plant.steamToPower());
		double toTurbine = steamGenerators(n, carried, flows, plant.steamToPower());
		// Each ARV opened by hand cools the plant (the steam it lets go takes heat from the coolant).
		double settled = Math.max(40, 40 + 245 * Math.min(1, heat * 20) + 20 * heat - ARV_COOL * arvsOpen - (rhr ? RHR_COOL : 0));
		if (heat > removed + 1e-9) {
			temperature += (heat - removed) * (temperature > UNCOVERED ? 1.6 : 0.4);
		} else {
			// (cooling no faster than 2 C a second: the plant's metal holds its heat, as the cooldown limits have it)
			temperature += Math.max(-COOLDOWN, (settled - temperature) * 0.01);
		}
		// (a molten core gets no hotter than its melt: it is spreading by then)
		temperature = Math.max(20, Math.min(MELT_TEMP, temperature));
		// Burnup.
		if (fission > 0) {
			// Each assembly carries its share of the power: in a full core at 100%, it lasts ASSEMBLY_LIFE ticks.
			double per = fission / Math.max(1e-6, activeFuel()) / ASSEMBLY_LIFE;
			for (int i = 0; i < fuel.size(); i++) {
				if (fuel.get(i) < 1) {
					fuel.set(i, Math.min(1, fuel.get(i) + per));
				}
			}
		}
		// Protection: overtemperature, low flow above P-8 (one loop) or P-7 (two), a turbine trip above P-9.
		if (protection && temperature >= SCRAM_TEMP) {
			trip(TRIP_OTDT);
		}
		if (protection) {
			int low = 0;
			for (int k = 0; k < n; k++) {
				if (loopFlow[k] < LOW_FLOW) {
					low++;
				}
			}
			if (fission > P8 && low >= 1) {
				trip(TRIP_FLOW1);
			} else if (fission > P7 && low >= 2) {
				trip(TRIP_FLOW2);
			}
			if (turbineTripped && fission > P9) {
				trip(TRIP_TURBINE);
			}
		}
		// A steam generator at low-low level trips the reactor (round 28).
		if (protection && n > 0) {
			for (int k = 0; k < n; k++) {
				if (nr(k) < LO_LO) {
					trip(TRIP_SG_LO);
				}
			}
		}
		if (temperature >= DAMAGE_TEMP && !fuelDamaged && !fuel.isEmpty()) {
			fuelDamaged = true;
			trip(TRIP_OTDT);
		}
		// The turbine trips with the reactor: decay heat after a shutdown is dumped, not turned into power. Tripped or
		// with its generator's breaker open from the boards, it makes none either.
		electric = plant.steamToPower() && !scrammed && fission >= 0.01 && !turbineTripped && !genOpen
				? Math.min(plant.generatorLimit(), Math.round(removed * toTurbine * design.ratedJe())) : 0;
		// The turbine: held at speed by the grid while it makes power; latched with steam it runs up in half a minute;
		// tripped (or with the reactor tripped, or no steam) it coasts down to its turning gear.
		if (electric > 0) {
			turbineSpeed = 1;
		} else if (!turbineTripped && !scrammed && plant.steamToPower() && temperature > 250) {
			turbineSpeed = Math.min(1, turbineSpeed + 1 / 600.0);
		} else {
			// Coasting down: to rest, or with its turning gear in, to the gear's speed (its motor brings it up from rest in
			// five seconds).
			double floor = plant.steamToPower() && turningGear ? TURNING_GEAR : 0;
			turbineSpeed = turbineSpeed < floor ? Math.min(floor, turbineSpeed + TURNING_GEAR / 100)
					: Math.max(floor, turbineSpeed - turbineSpeed / 900 - 0.0001);
		}
		// Rolled off its turning gear by steam: the gear drops out once the shaft runs faster than it.
		if (turningGear && turbineSpeed > TURNING_GEAR * 3) {
			turningGear = false;
		}
		// Annunciators.
		int a = 0;
		if (temperature >= ALARM_TEMP) a |= A_HIGH_TEMP;
		if (scrammed) a |= A_SCRAM;
		if (waterShare < 0.95 && heat > 0.01) a |= A_LOW_WATER;
		if (cooling < 1) a |= A_LOOPS;
		if (!plant.steamToPower()) a |= A_NO_POWER_OUT;
		if (!protection) a |= A_PROTECTION_OFF;
		if (spentCount() > 0 && !fuelDamaged) a |= A_FUEL_SPENT;
		if (fuelDamaged) a |= A_FUEL_DAMAGED;
		if (!plant.formed()) a |= A_NOT_FORMED;
		if (refuelOpen) a |= A_REFUEL;
		if (fuel.isEmpty()) a |= A_NO_FUEL;
		alarms = a;
		if (++trendTicks >= 20) {
			trendTicks = 0;
			trend[trendHead] = (int) Math.round(temperature);
			trendHead = (trendHead + 1) % TREND;
		}
		return electric;
	}

	/**
	 * The steam generators a tick (round 28): each loop's heat boils its generator's water into steam; main feed
	 * (unless isolated) holds the level at its programme, the auxiliary feed pumps add theirs when running; a
	 * generator at low-low level starts the auxiliary feed pumps. Closing every MSIV cuts the turbine off: it trips
	 * (above P-9 the reactor follows). Returns the share of the steam that reaches the turbine (the rest goes out
	 * the relief valves of generators with their MSIV shut).
	 */
	private double steamGenerators(int n, double[] carried, double flows, boolean steamToPower) {
		double steam = 0, open = 0;
		int loLo = 0, closedAll = 0;
		boolean hot = temperature > 150;
		arvsOpen = 0;
		for (int k = 0; k < 4; k++) {
			if (k >= n) {
				sgWater[k] = 1;
				sgSteam[k] = sgFeed[k] = sgAux[k] = 0;
				continue;
			}
			double s = flows > 1e-9 ? removed * carried[k] / flows * n : 0;
			double w = sgWater[k];
			// An ARV opened by hand on a generator with water: it steams to atmosphere (more boil-off, the plant cools).
			double vent = (arvManual & 1 << k) != 0 && temperature > 100 && w > DRY ? ARV_STEAM : 0;
			if (vent > 0) {
				arvsOpen++;
			}
			double main = feedIsolated ? 0 : Math.max(0, Math.min(feedCap, s + (1 - w) * 4 + ((arvManual & 1 << k) != 0 && temperature > 100 ? ARV_STEAM : 0)));
			// The auxiliary feed pumps' flow into this generator (A feeds 1 and 2, B 3 and 4, the turbine-driven pump all
			// four, on steam while the plant is hot), throttled to hold the level.
			// (a motor-driven pump only with power on its bus; the turbine-driven one needs its DC control power)
			double cap = (afwValveShut & 1 << k) != 0 ? 0
					: ((afwRunning & afwPower & (k < 2 ? 1 : 2)) != 0 ? AFW_FLOW : 0) + ((afwRunning & 4) != 0 && hot && tdafwDc ? AFW_FLOW : 0);
			double aux = Math.max(0, Math.min(cap, s + vent + (1 - w) * 2 - main));
			w += (main + aux - s - vent) / BOIL;
			sgWater[k] = Math.max(0, Math.min(NR_HIGH + 0.05, w));
			sgSteam[k] = s + vent;
			sgFeed[k] = main;
			sgAux[k] = aux;
			cstUsed = Math.min(1, cstUsed + aux / 20_000.0);
			steam += s;
			if ((msivClosed & 1 << k) == 0) {
				open += s;
			} else {
				closedAll++;
			}
			if (nr(k) < LO_LO) {
				loLo++;
			}
		}
		if (afwRunning == 0) {
			// The condensate storage tank is made up from the demineralised water when the pumps are not drawing on it.
			cstUsed = Math.max(0, cstUsed - 1 / 72_000.0);
		}
		// A generator reaching low-low level starts the motor-driven pumps; two start the turbine-driven one too.
		if (loLo > loLoWas) {
			afwRunning |= 3;
			if (loLo >= 2) {
				afwRunning |= 4;
			}
		}
		loLoWas = loLo;
		// Safety injection draws the boron injection tank's and the charging pumps' flow from the RWST.
		if (siActuated) {
			rwstUsed = Math.min(1, rwstUsed + 300.0 / 60 / 20 / 400_000);
		}
		if (n > 0 && closedAll == n && steamToPower && !turbineTripped) {
			// Every MSIV shut: no steam reaches the turbine, the generator motors and trips on reverse power.
			turbineTripped = true;
			genOpen = true;
			syncArmed = false;
		}
		return n == 0 ? 1 : steam > 1e-9 ? open / steam : closedAll == n ? 0 : 1;
	}

	/**
	 * The nuclear instruments a tick (round 30): their level follows the neutrons (up at most half a decade a second;
	 * down at once to what the delayed neutrons hold up, which falls a third of a decade a minute), the startup rate
	 * each second, the change over two seconds; below P-6 the source range's block clears by itself, below P-10 the
	 * intermediate range's and the power range's low setpoint's.
	 */
	private void instruments() {
		if (Double.isNaN(nisLog)) {
			// Loaded or new: the instruments read what the core does.
			nis = fission;
			nisDelayed = PROMPT * fission;
			nisTwo = nis;
			nisLog = Math.log10(nis + 1.5e-9);
		}
		if (fission >= nis) {
			nis = Math.min(fission, Math.max(nis, 1e-10) * NIS_RISE);
		} else {
			nis = Math.max(fission, Math.min(nis, nisDelayed));
		}
		nisDelayed = Math.max(nisDelayed * NIS_FALL, PROMPT * fission);
		if (nis < 1e-14) {
			nis = 0;
		}
		nisTicks++;
		if (nisTicks % 20 == 0) {
			double log = Math.log10(nis + 1.5e-9);
			sur = sur * 0.5 + (log - nisLog) * 60 * 0.5;
			if (Math.abs(sur) < 1e-3) {
				sur = 0;
			}
			nisLog = log;
		}
		if (nisTicks % 40 == 0) {
			nisChange = nis - nisTwo;
			nisTwo = nis;
		}
		if (nis < P6) {
			srBlocked = false;
		}
		if (nis < P10) {
			irBlocked = false;
			prLoBlocked = false;
		}
		if (screenRods && nis >= P6) {
			srBlocked = true;
		}
		if (screenRods && nis >= P10) {
			irBlocked = true;
			prLoBlocked = true;
		}
	}

	/** A rod bank's position (steps out; banks 0 to 3 the shutdown banks, 4 to 7 control banks A to D in their overlap). */
	public double bankSteps(int bank) {
		if (bank < 4) {
			return sdBanks;
		}
		double demand = (1 - rods / 100) * SEQUENCE;
		return Math.max(0, Math.min(STEPS, demand - OVERLAP * (bank - 4)));
	}

	/**
	 * The rods' lever held one way (round 30): OUT (1) withdraws, IN (-1) inserts, a little past the rods each time it
	 * is used, so they move while it is held and stop when it is let go. Not in AUTO, tripped or refuelling.
	 */
	public boolean drive(int dir) {
		if (rodAuto || scrammed || fuelDamaged || refuelOpen) {
			return false;
		}
		rodTarget = Math.max(0, Math.min(100, rods - dir * LEVER_STEP));
		screenRods = false;
		return true;
	}

	/** Rod control to AUTO (holding the power the rods give where they stand) or to MAN (the rods stop where they are). */
	public void rodControl(boolean auto) {
		rodAuto = auto;
		screenRods = false;
		if (auto) {
			autoSetpoint = Math.max(0, (1 - rods / 100) * activeFuel());
		}
		rodTarget = rods;
	}

	/** A steam generator's narrow range level (a fraction: 0 to 1) from its water. */
	public double nr(int k) {
		return Math.max(0, Math.min(1, (sgWater[k] - NR_LOW) / (NR_HIGH - NR_LOW)));
	}

	/** Condensate pump k (0 to 2) at the hall, main feed pump k (0, 1) at the hall. */
	public boolean cpPresent(int k) {
		return cpSeen[k] > 0;
	}

	public boolean mfpPresent(int k) {
		return mfpSeen[k] > 0;
	}

	/** The condensate pumps at the hall running, the main feed pumps at the hall in service and not tripped. */
	public int cpOn() {
		int n = 0;
		for (int k = 0; k < 3; k++) {
			if (cpPresent(k) && (cpRunning & 1 << k) != 0) n++;
		}
		return n;
	}

	public int mfpOn() {
		int n = 0;
		for (int k = 0; k < 2; k++) {
			if (mfpPresent(k) && (mfpRunning & 1 << k) != 0 && (mfpTripped & 1 << k) == 0) n++;
		}
		return n;
	}

	/** The hall has condensate pumps, main feed pumps. */
	public boolean hasCps() {
		return cpSeen[0] > 0 || cpSeen[1] > 0 || cpSeen[2] > 0;
	}

	public boolean hasMfps() {
		return mfpSeen[0] > 0 || mfpSeen[1] > 0;
	}

	/** The feed the generators want (a fraction of one's full-power feed): their steam last tick, the most of any. */
	public double feedDemand() {
		double d = 0;
		for (int k = 0; k < loops; k++) {
			d = Math.max(d, sgSteam[k]);
		}
		return d;
	}

	/**
	 * The feed train a tick (turbine hall part 2): the pumps not heard from drop out; feedwater isolation trips the MFPs;
	 * a standby condensate pump in AUTO starts when those running can't carry the feed; the MFPs trip with no condensate
	 * pump running; main feed's capacity is what the pumps running carry (the MFPs only with steam to turn them); an MFP
	 * lost above what one carries runs the plant back in AUTO; the last one lost at power starts the motor-driven
	 * auxiliary feed pumps.
	 */
	private void feedTrain(int n, boolean steamToPower) {
		for (int k = 0; k < 3; k++) {
			if (cpSeen[k] > 0) cpSeen[k]--;
			if (cpHold[k] > 0) cpHold[k]--;
		}
		for (int k = 0; k < 2; k++) {
			if (mfpSeen[k] > 0) mfpSeen[k]--;
		}
		boolean cps = hasCps(), mfps = hasMfps();
		if (!cps && !mfps) {
			// (no pumps at the hall: main feed has no limit while the plant's buses are live, none without them)
			feedCap = plantBus() ? FEED_FREE : 0;
			cpShort = false;
			mfpOnWas = -1;
			return;
		}
		double demand = feedDemand();
		if (feedIsolated) {
			for (int k = 0; k < 2; k++) {
				if (mfpPresent(k) && (mfpRunning & 1 << k) != 0 && (mfpTripped & 1 << k) == 0) {
					mfpTripped |= 1 << k;
				}
			}
		}
		double cpCap = FEED_FREE;
		cpShort = false;
		if (cps) {
			cpCap = CP_SHARE * cpOn();
			if (demand > cpCap + 0.02) {
				// The condensate header's pressure falls: a standby pump in AUTO starts (one held off after a stop last).
				int pick = -1;
				for (int k = 0; k < 3; k++) {
					if (cpPresent(k) && (cpRunning & 1 << k) == 0 && (cpAuto & 1 << k) != 0 && cpHold[k] == 0 && plantBus()) {
						pick = k;
						break;
					}
				}
				if (pick >= 0) {
					cpRunning |= 1 << pick;
					cpAuto &= ~(1 << pick);
					cpAutoStarted |= 1 << pick;
					cpCap = CP_SHARE * cpOn();
				}
				cpShort = demand > cpCap + 0.02;
			}
		}
		if (cps && mfps && cpOn() == 0 && mfpOn() > 0) {
			// No condensate pump: the MFPs lose their suction and trip.
			if (++mfpStarved > 40) {
				for (int k = 0; k < 2; k++) {
					if (mfpPresent(k) && (mfpRunning & 1 << k) != 0) mfpTripped |= 1 << k;
				}
			}
		} else {
			mfpStarved = 0;
		}
		int on = mfpOn();
		double mfpCap = FEED_FREE;
		if (mfps) {
			mfpCap = steamToPower && temperature > MFP_STEAM ? MFP_SHARE * on : 0;
			if (mfpOnWas > on && n > 0) {
				// An MFP lost: above what those left carry, the rods in AUTO run the plant back; the last one lost at power
				// starts the motor-driven auxiliary feed pumps.
				if (rodAuto && fission > MFP_SHARE * on && on > 0) {
					autoSetpoint = Math.min(autoSetpoint, RUNBACK);
				}
				if (on == 0 && fission > 0.02) {
					afwRunning |= 3;
				}
			}
		}
		mfpOnWas = on;
		feedCap = plantBus() ? Math.min(cpCap, mfpCap) : 0;
	}

	/**
	 * Safety injection (round 28, the ESF board's manual switches; Justin: "Full actuation"): the reactor trips (the
	 * turbine with it), feedwater and the containment isolate, the auxiliary feed pumps start. The board starts the
	 * diesel generators; the safety injection, residual heat removal and charging pumps run while it is in.
	 */
	public void safetyInjection(long now) {
		if (!siActuated) {
			siTime = now;
		}
		siActuated = true;
		feedIsolated = true;
		cntIsolated = true;
		afwRunning |= 7;
		trip(TRIP_SI);
	}

	/** Resets safety injection a minute after it came in: the isolations clear and the pumps it started stand down. */
	public boolean resetSafetyInjection(long now) {
		if (!siActuated || now - siTime < SI_RESET) {
			return false;
		}
		siActuated = false;
		feedIsolated = false;
		cntIsolated = false;
		return true;
	}

	/** Clears a SCRAM once the core is below the alarm temperature (the rods stay in until moved); not while safety injection is in. */
	public boolean reset() {
		if (!scrammed || temperature >= ALARM_TEMP || fuelDamaged || siActuated) {
			return false;
		}
		scrammed = false;
		tripCause = TRIP_NONE;
		rodTarget = 100;
		return true;
	}

	/** Trips the reactor for a cause, keeping the first one while it stays tripped (the first-out windows). */
	public void trip(int cause) {
		if (!scrammed) {
			tripCause = cause;
		}
		scrammed = true;
		// Rod control drops to MAN: after the reset the rods stay in until someone drives them out.
		rodAuto = false;
	}

	public void save(ValueOutput out) {
		out.putDouble("rod_target", rodTarget);
		out.putBoolean("protection", protection);
		out.putBoolean("scrammed", scrammed);
		out.putBoolean("refuel", refuelOpen);
		out.putBoolean("remote", remote);
		out.putDouble("rods", rods);
		out.putDouble("fission", fission);
		out.putDouble("decay", decay);
		out.putDouble("decay_from", decayFrom);
		out.putLong("decay_time", decayTime);
		out.putDouble("temperature", temperature);
		out.putBoolean("fuel_damaged", fuelDamaged);
		out.store("fuel", Codec.DOUBLE.listOf(), List.copyOf(fuel));
		out.putInt("trip_cause", tripCause);
		out.putInt("rcp_stopped", rcpStopped);
		out.store("loop_flow", Codec.DOUBLE.listOf(), List.of(loopFlow[0], loopFlow[1], loopFlow[2], loopFlow[3]));
		out.putInt("loops", loops);
		out.putBoolean("turbine_tripped", turbineTripped);
		out.putBoolean("gen_open", genOpen);
		out.putBoolean("sync_armed", syncArmed);
		out.putDouble("turbine_speed", turbineSpeed);
		out.putBoolean("turning_gear", turningGear);
		out.putBoolean("si", siActuated);
		out.putLong("si_time", siTime);
		out.putBoolean("feed_isolated", feedIsolated);
		out.putBoolean("cnt_isolated", cntIsolated);
		out.putInt("msiv_closed", msivClosed);
		out.putInt("afw", afwRunning);
		out.store("sg_water", Codec.DOUBLE.listOf(), List.of(sgWater[0], sgWater[1], sgWater[2], sgWater[3]));
		out.putDouble("rwst_used", rwstUsed);
		out.putDouble("cst_used", cstUsed);
		out.putInt("lo_lo", loLoWas);
		out.putInt("afw_shut", afwValveShut);
		out.putInt("arv_manual", arvManual);
		out.putBoolean("rod_auto", rodAuto);
		out.putDouble("auto_sp", autoSetpoint);
		out.putDouble("sd_banks", sdBanks);
		out.putInt("nis_blocks", (srBlocked ? 1 : 0) | (irBlocked ? 2 : 0) | (prLoBlocked ? 4 : 0) | (screenRods ? 8 : 0));
		out.putInt("feed_train", cpRunning | cpAuto << 3 | cpAutoStarted << 6 | mfpRunning << 9 | mfpTripped << 11);
		out.putInt("res_open", resOpen);
		out.putInt("cp_shed", cpShed);
	}

	public void load(ValueInput in) {
		rodTarget = in.getDoubleOr("rod_target", 100);
		protection = in.getBooleanOr("protection", true);
		scrammed = in.getBooleanOr("scrammed", false);
		refuelOpen = in.getBooleanOr("refuel", false);
		remote = in.getBooleanOr("remote", false);
		rods = in.getDoubleOr("rods", 100);
		fission = in.getDoubleOr("fission", 0);
		decay = in.getDoubleOr("decay", 0);
		decayFrom = in.getDoubleOr("decay_from", decay);
		decayTime = in.getLongOr("decay_time", 0L);
		decayWas = decay;
		temperature = in.getDoubleOr("temperature", 30);
		fuelDamaged = in.getBooleanOr("fuel_damaged", false);
		fuel.clear();
		fuel.addAll(in.read("fuel", Codec.DOUBLE.listOf()).orElse(List.of()));
		tripCause = in.getIntOr("trip_cause", scrammed ? TRIP_MANUAL : TRIP_NONE);
		rcpStopped = in.getIntOr("rcp_stopped", 0);
		List<Double> flows = in.read("loop_flow", Codec.DOUBLE.listOf()).orElse(List.of());
		for (int k = 0; k < 4; k++) {
			loopFlow[k] = k < flows.size() ? flows.get(k) : (rcpStopped & 1 << k) == 0 ? 1 : 0;
		}
		loops = in.getIntOr("loops", 4);
		turbineTripped = in.getBooleanOr("turbine_tripped", false);
		genOpen = in.getBooleanOr("gen_open", false);
		syncArmed = in.getBooleanOr("sync_armed", false);
		turbineSpeed = in.getDoubleOr("turbine_speed", 1);
		turningGear = in.getBooleanOr("turning_gear", true);
		siActuated = in.getBooleanOr("si", false);
		siTime = in.getLongOr("si_time", 0L);
		feedIsolated = in.getBooleanOr("feed_isolated", false);
		cntIsolated = in.getBooleanOr("cnt_isolated", false);
		msivClosed = in.getIntOr("msiv_closed", 0);
		afwRunning = in.getIntOr("afw", 0);
		List<Double> water = in.read("sg_water", Codec.DOUBLE.listOf()).orElse(List.of());
		for (int k = 0; k < 4; k++) {
			sgWater[k] = k < water.size() ? water.get(k) : 1;
		}
		rwstUsed = in.getDoubleOr("rwst_used", 0);
		cstUsed = in.getDoubleOr("cst_used", 0);
		loLoWas = in.getIntOr("lo_lo", 0);
		afwValveShut = in.getIntOr("afw_shut", 0);
		arvManual = in.getIntOr("arv_manual", 0);
		rodAuto = in.getBooleanOr("rod_auto", false);
		autoSetpoint = in.getDoubleOr("auto_sp", 0);
		sdBanks = in.getDoubleOr("sd_banks", scrammed ? 0 : STEPS);
		// (A reactor from before round 30 was run from its screen.)
		int blocks = in.getIntOr("nis_blocks", 8);
		srBlocked = (blocks & 1) != 0;
		irBlocked = (blocks & 2) != 0;
		prLoBlocked = (blocks & 4) != 0;
		screenRods = (blocks & 8) != 0;
		// (A reactor from before turbine hall part 2: two condensate pumps running, the third in AUTO; both MFPs in service.)
		int feed = in.getIntOr("feed_train", 0b011 | 0b100 << 3 | 0b11 << 9);
		cpRunning = feed & 7;
		cpAuto = feed >> 3 & 7;
		cpAutoStarted = feed >> 6 & 7;
		mfpRunning = feed >> 9 & 3;
		mfpTripped = feed >> 11 & 3;
		resOpen = in.getIntOr("res_open", 0);
		cpShed = in.getIntOr("cp_shed", 0);
	}

	/** A remote shutdown panel's copy of its reactor's state (what its screen, lamps and window show). */
	public void copyStatus(ReactorCore from) {
		design = from.design;
		rodTarget = from.rodTarget;
		protection = from.protection;
		scrammed = from.scrammed;
		refuelOpen = from.refuelOpen;
		remote = from.remote;
		rods = from.rods;
		fission = from.fission;
		decay = from.decay;
		temperature = from.temperature;
		fuelDamaged = from.fuelDamaged;
		electric = from.electric;
		alarms = from.alarms;
	}

	/** The status a remote shutdown panel sends its clients (no fuel list, no trend). */
	public void saveStatus(ValueOutput out) {
		out.putString("design", design.id());
		out.putBoolean("scrammed", scrammed);
		out.putBoolean("refuel", refuelOpen);
		out.putBoolean("remote", remote);
		out.putDouble("rods", rods);
		out.putDouble("fission", fission);
		out.putDouble("temperature", temperature);
		out.putBoolean("fuel_damaged", fuelDamaged);
		out.putLong("electric", electric);
		out.putInt("alarms", alarms);
	}

	public void loadStatus(ValueInput in) {
		String id = in.getStringOr("design", "pwr");
		design = id.equals(Design.EPR.id()) ? Design.EPR : id.equals(Design.SMALL_MODULAR.id()) ? Design.SMALL_MODULAR : Design.STANDARD;
		scrammed = in.getBooleanOr("scrammed", false);
		refuelOpen = in.getBooleanOr("refuel", false);
		remote = in.getBooleanOr("remote", false);
		rods = in.getDoubleOr("rods", 100);
		fission = in.getDoubleOr("fission", 0);
		temperature = in.getDoubleOr("temperature", 30);
		fuelDamaged = in.getBooleanOr("fuel_damaged", false);
		electric = in.getLongOr("electric", 0);
		alarms = in.getIntOr("alarms", 0);
	}

	/** The readouts the console shows (sent to clients with the vessel). */
	public void saveReadouts(ValueOutput out) {
		save(out);
		out.putDouble("removed", removed);
		out.putDouble("water_share", waterShare);
		out.putDouble("cooling", cooling);
		out.putLong("electric", electric);
		out.putInt("need", need);
		out.putInt("alarms", alarms);
		out.putIntArray("trend", trend);
		out.putInt("trend_head", trendHead);
		out.putInt("power", power);
	}

	public void loadReadouts(ValueInput in) {
		load(in);
		removed = in.getDoubleOr("removed", 0);
		waterShare = in.getDoubleOr("water_share", 1);
		cooling = in.getDoubleOr("cooling", 0);
		electric = in.getLongOr("electric", 0);
		need = in.getIntOr("need", 0);
		alarms = in.getIntOr("alarms", 0);
		in.getIntArray("trend").ifPresent(t -> System.arraycopy(t, 0, trend, 0, Math.min(t.length, TREND)));
		trendHead = in.getIntOr("trend_head", 0);
		power = in.getIntOr("power", PW_ALL);
	}
}
