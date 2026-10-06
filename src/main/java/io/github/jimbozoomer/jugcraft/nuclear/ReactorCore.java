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
	/** Ticks of the temperature trend (ten minutes, one sample a second). */
	public static final int TREND = 600;

	// Alarms (annunciator windows on the console).
	public static final int A_HIGH_TEMP = 1, A_SCRAM = 2, A_LOW_WATER = 4, A_LOOPS = 8, A_NO_POWER_OUT = 16, A_PROTECTION_OFF = 32,
			A_FUEL_SPENT = 64, A_FUEL_DAMAGED = 128, A_NOT_FORMED = 256, A_REFUEL = 512, A_NO_FUEL = 1024;

	// Driven by the console.
	public double rodTarget = 100;
	public boolean protection = true;
	public boolean scrammed;
	public boolean refuelOpen;
	// Plant state.
	public double rods = 100;
	public double fission;
	public double decay;
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
		double goal = scrammed || fuelDamaged || refuelOpen ? 100 : rodTarget;
		double speed = scrammed || fuelDamaged ? SCRAM_SPEED : ROD_SPEED;
		rods += Math.max(-speed, Math.min(speed, goal - rods));
		// Fission follows the rods (and the fuel) with a few seconds' lag; decay heat follows what fission did.
		double target = (1 - rods / 100) * activeFuel();
		fission += (target - fission) * (target > fission ? 0.01 : 0.2);
		if (fission < 1e-5) {
			fission = 0;
		}
		decay = Math.max(DECAY_SHARE * fission, decay * Math.exp(-1 / 6_000.0));
		if (decay < 1e-5) {
			decay = 0;
		}
		double heat = heat();
		// Cooling: the loops carry heat to the steam generators, and the condenser needs water to take it away.
		cooling = plant.cooling();
		double tower = Math.max(0, Math.min(1, plant.towerShare()));
		int need = (int) Math.ceil(design.water() * Math.min(1, heat) * ((1 - tower) + tower * MAKEUP));
		this.need = need;
		int got = need > 0 && cooling > 0 ? water.applyAsInt(need) : 0;
		waterShare = need <= 0 ? 1 : got / (double) need;
		removed = Math.min(heat, cooling * waterShare);
		double settled = 40 + 245 * Math.min(1, heat * 20) + 20 * heat;
		if (heat > removed + 1e-9) {
			temperature += (heat - removed) * 0.4;
		} else {
			temperature += (settled - temperature) * 0.01;
		}
		temperature = Math.max(20, temperature);
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
		// Protection.
		if (protection && temperature >= SCRAM_TEMP) {
			scrammed = true;
		}
		if (temperature >= DAMAGE_TEMP && !fuelDamaged && !fuel.isEmpty()) {
			fuelDamaged = true;
			scrammed = true;
		}
		// The turbine trips with the reactor: decay heat after a shutdown is dumped, not turned into power.
		electric = plant.steamToPower() && !scrammed && fission >= 0.01 ? Math.min(plant.generatorLimit(), Math.round(removed * design.ratedJe())) : 0;
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

	/** Clears a SCRAM once the core is below the alarm temperature (the rods stay in until moved). */
	public boolean reset() {
		if (!scrammed || temperature >= ALARM_TEMP || fuelDamaged) {
			return false;
		}
		scrammed = false;
		rodTarget = 100;
		return true;
	}

	public void save(ValueOutput out) {
		out.putDouble("rod_target", rodTarget);
		out.putBoolean("protection", protection);
		out.putBoolean("scrammed", scrammed);
		out.putBoolean("refuel", refuelOpen);
		out.putDouble("rods", rods);
		out.putDouble("fission", fission);
		out.putDouble("decay", decay);
		out.putDouble("temperature", temperature);
		out.putBoolean("fuel_damaged", fuelDamaged);
		out.store("fuel", Codec.DOUBLE.listOf(), List.copyOf(fuel));
	}

	public void load(ValueInput in) {
		rodTarget = in.getDoubleOr("rod_target", 100);
		protection = in.getBooleanOr("protection", true);
		scrammed = in.getBooleanOr("scrammed", false);
		refuelOpen = in.getBooleanOr("refuel", false);
		rods = in.getDoubleOr("rods", 100);
		fission = in.getDoubleOr("fission", 0);
		decay = in.getDoubleOr("decay", 0);
		temperature = in.getDoubleOr("temperature", 30);
		fuelDamaged = in.getBooleanOr("fuel_damaged", false);
		fuel.clear();
		fuel.addAll(in.read("fuel", Codec.DOUBLE.listOf()).orElse(List.of()));
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
	}
}
