package io.github.jimbozoomer.jugcraft.client;

import io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;

/**
 * What an emergency diesel generator's panels read (round 14: they stood still), worked out on the client from the
 * generator's state (running, when it was started or stopped, hours run): client/LivePanelRenderer.java draws it.
 * One story, played the same way every time, as a real plant's emergency start on a loss of offsite power:
 * <ol>
 * <li>Start (0 to 1.5 s): the air start motors crank the engine (start air drops about 5 bar, lube oil pressure
 * comes up), the battery chargers lose their AC supply (their alarm comes in).</li>
 * <li>Run up (to 4 s, as the engine is drawn): it fires and runs up to 514 rpm (60 Hz), overshooting a little.
 * At about 3 s the field is flashed and the voltage builds to 4.16 kV.</li>
 * <li>At 5 s the generator breaker closes onto the dead safety bus and the load sequencer starts the safety loads
 * every 5 s: the bus itself, then the safety injection, residual heat removal, component cooling, service water and
 * containment spray pumps, the HVAC fans and last the battery chargers. Each motor start pulls the voltage and
 * frequency down for a second (more for the big pumps), with a burst of kilovars, and the regulator and governor pull
 * them back: the field forces up. It ends at full load, 6,800 kW at 0.88 power factor (about 1,070 A).</li>
 * <li>Running: everything steady but for the small wander a real meter shows; jacket water warms over minutes, the
 * start air recharges, the hour meter counts (an hour a 1,000 game ticks); the chargers, back on, recharge the
 * battery at their 150 A limit (equalize), tapering back to float.</li>
 * <li>Stopped: the breaker opens and the loads drop, the regulator de-excites, the engine coasts down over 4 s,
 * jacket water cools back to its keep-warm 52 degrees over the next ten minutes.</li>
 * </ol>
 */
public final class EdgTelemetry {
	public static final double RATED_RPM = 514, RATED_KV = 4.16, RATED_HZ = 60;
	/** Seconds from the start to the breaker closing, and between the sequencer's steps. */
	public static final double BREAKER = io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.BREAKER, STEP = io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.STEP;
	public static final String[] LOADS = {"4160V BUS 1A", "SAFETY INJ PUMP", "RHR PUMP", "COMP COOL PUMP", "SERVICE WTR PUMP",
			"CONT SPRAY PUMP", "HVAC FANS", "BATT CHARGERS"};
	static final double[] STEP_KW = io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.STEP_KW;
	static final boolean[] MOTOR = {false, true, true, true, true, true, true, false};
	public static final double AIR = 30, LUBE_STANDBY = 0.4, JACKET_STANDBY = 52, FLOAT_V = 132, FLOAT_A = 12;
	/** The chargers come back with the sequence's last step. */
	static final double CHARGERS = BREAKER + 7 * STEP;
	private static final Map<BlockPos, Memory> MEMORY = new HashMap<>();
	/**
	 * A regulating transformer's winding temperature (round 26, 8d; degrees C): dead (room temperature), alive on no
	 * load, carrying its inverter's load; and how fast it gets there (game ticks).
	 */
	static final double XF_ROOM = 25, XF_NO_LOAD = 58, XF_LOADED = 96, XF_TAU = 1800;
	/** Each regulating transformer's winding temperature as last drawn, and when (game ticks). */
	private static final Map<BlockPos, double[]> WINDING = new HashMap<>();

	private EdgTelemetry() {
	}

	/** What the panels show at one moment. */
	public static final class Snapshot {
		public boolean running, breaker, chargerAc, equalize, ready;
		public double rpm, air, lube, jacket, kv, amps, kw, kvar, hz, pf, fieldV, fieldA, dcV, dcA, hours;
		/** A station charger's equalize timer: the hours of its equalize charge left (0 on float; round 26). */
		public double eqHours;
		/**
		 * A vital inverter (round 26, 8g): its DC in (the battery not flat), running, the load on it or on the bypass,
		 * the bypass there to take it; its output volts, amps and frequency.
		 */
		public boolean invDc, invRun, invOnInv, invOnBypass, invBypassOk;
		public double invAcV, invAcA, invHz;
		/**
		 * A regulating transformer (round 26, 8d): POWER ON; its input and output volts, output amps and winding
		 * temperature (degrees C).
		 */
		public boolean xfOn;
		public double xfInV, xfOutV, xfOutA, xfTemp;
		/** Sequencer steps done, and the one whose motor is starting now (-1 none). */
		public int steps, starting = -1;
		public String avr1 = "", avr2 = "", sequence = "";
		public final String[] relays = new String[8];
		/** Alarm windows: 0 dark, 1 lit, 2 flashing (new). */
		public final int[] alarms = new int[12];
		public double time;

		/** A meter's or gauge's reading. */
		public double number(String channel) {
			return switch (channel) {
				case "rpm" -> rpm;
				case "air" -> air;
				case "lube" -> lube;
				case "jw" -> jacket;
				case "kv" -> kv;
				case "amps" -> amps;
				case "kw" -> kw;
				case "kvar" -> kvar;
				case "hz" -> hz;
				case "pf" -> pf;
				case "field_v" -> fieldV;
				case "field_a" -> fieldA;
				case "dc_v" -> dcV;
				case "dc_a" -> dcA;
				case "eq_hours" -> eqHours;
				case "inv_dc_v" -> dcV;
				case "inv_ac_v" -> invAcV;
				case "inv_ac_a" -> invAcA;
				case "xf_in_v" -> xfInV;
				case "xf_out_v" -> xfOutV;
				case "xf_out_a" -> xfOutA;
				case "xf_temp" -> xfTemp;
				default -> 0;
			};
		}

		/** A lamp's or window's state: 0 dark, 1 lit (its colour), 2 its second colour or flashing. */
		public int lit(String channel) {
			if (channel.startsWith("ann_")) {
				int i = switch (channel) {
					case "ann_low_lube" -> 0;
					case "ann_high_jw" -> 1;
					case "ann_overspeed" -> 2;
					case "ann_fail_start" -> 3;
					case "ann_low_air" -> 4;
					case "ann_low_day_tank" -> 5;
					case "ann_crankcase" -> 6;
					case "ann_charger_fail" -> 7;
					case "ann_bkr_trip" -> 8;
					case "ann_estop" -> 9;
					case "ann_running" -> 10;
					case "ann_ready" -> 11;
					default -> -1;
				};
				return i < 0 ? 0 : alarms[i];
			}
			if (channel.startsWith("seq_lamp")) {
				return channel.charAt(8) - '0' < steps ? 1 : 0;
			}
			if (channel.startsWith("seq_row")) {
				int k = channel.charAt(7) - '0';
				return k == starting ? 2 : k < steps ? 1 : 0;
			}
			return switch (channel) {
				case "lamp_ready" -> ready ? 1 : 0;
				case "lamp_run" -> running ? 1 : 0;
				case "bkr_open" -> breaker ? 0 : 1;
				case "bkr_closed" -> breaker ? 1 : 0;
				case "chg_ac" -> chargerAc ? 1 : 0;
				case "chg_dc" -> 1;
				case "chg_float" -> chargerAc && !equalize ? 1 : 0;
				case "chg_equal" -> chargerAc && equalize ? 1 : 0;
				// A station charger's annunciator (round 26): the DC bus low (the battery running down with no charger) or
				// high; no ground faults are modelled, so that window stays dark.
				case "chg_dc_low" -> dcV < 124 ? 1 : 0;
				case "chg_dc_high" -> dcV > 140 ? 1 : 0;
				// A DC switchboard's ground detector: a lamp each side of the bus to ground, both lit while the bus is
				// alive and neither side grounded (no ground faults are modelled).
				case "gnd_lamp" -> dcV > 20 ? 1 : 0;
				// A vital inverter's lamps, mimic and annunciator.
				case "inv_dc", "inv_seg_dc" -> invDc ? 1 : 0;
				case "inv_run", "inv_seg_inv" -> invRun ? 1 : 0;
				case "inv_on" -> invOnInv ? 1 : 0;
				case "inv_byp" -> invOnBypass ? 1 : 0;
				case "inv_seg_byp" -> invBypassOk ? 1 : 0;
				case "inv_seg_out" -> invOnInv ? 1 : invOnBypass ? 2 : 0;
				case "inv_fail" -> invRun ? 0 : 1;
				case "inv_low_dc" -> dcV < 112 ? 1 : 0;
				case "inv_byp_fail" -> invBypassOk ? 0 : 1;
				case "inv_sync" -> invRun && !invBypassOk ? 1 : 0;
				// A regulating transformer's POWER ON lamp.
				case "xf_on" -> xfOn ? 1 : 0;
				default -> 0;
			};
		}

		/** A display's text. */
		public String text(String channel) {
			if (channel.startsWith("hours")) {
				// The hour meter's six wheels: thousands of hours down to tenths.
				long tenths = (long) Math.floor(hours * 10);
				String digits = String.format(Locale.ROOT, "%06d", tenths % 1_000_000);
				return String.valueOf(digits.charAt(channel.charAt(5) - '0'));
			}
			if (channel.startsWith("relay")) {
				return relays[channel.charAt(5) - '0'];
			}
			if (channel.equals("inv_hz")) {
				// The frequency display goes dark with the inverter's own control power (its DC) and no bypass.
				return invOnInv || invOnBypass ? String.format(Locale.ROOT, "%.2f", invHz) : "";
			}
			return switch (channel) {
				case "avr1" -> avr1;
				case "avr2" -> avr2;
				case "seq_head" -> sequence;
				default -> "";
			};
		}
	}

	/** What the client remembers of a generator it saw running, for its run-down after a stop. */
	private static final class Memory {
		boolean seen;
		double speed, air, jacket, dcA, kv, fieldV;
	}

	/** The panels' readings for a generator at game time {@code now} (fractional ticks); null: no generator near. */
	public static Snapshot of(EquipmentBlockEntity edg, double now) {
		Snapshot o = new Snapshot();
		if (edg == null) {
			standby(o);
			return o;
		}
		double seed = (edg.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		Memory m = MEMORY.computeIfAbsent(edg.getBlockPos().immutable(), k -> new Memory());
		double s = (now - edg.runningSince) / 20.0;
		o.time = s;
		if (edg.running) {
			running(o, Math.max(0, s), seed);
			m.seen = true;
			m.speed = o.rpm / RATED_RPM;
			m.air = o.air;
			m.jacket = o.jacket;
			m.dcA = o.dcA;
			m.kv = o.kv;
			m.fieldV = o.fieldV;
		} else {
			if (m.seen && edg.runningSince > 0) {
				stopping(o, Math.max(0, s), m, seed);
			} else {
				standby(o);
			}
		}
		fuel(o, edg);
		return o;
	}

	/**
	 * The fuel (nuclear/DieselFuel.java): LOW DAY TANK LEVEL below a quarter (or no day tank found); FAIL TO START when
	 * it found no fuel to start on or ran dry, and then it isn't ready for an auto start.
	 */
	static void fuel(Snapshot o, EquipmentBlockEntity edg) {
		if (edg.dayLevel < io.github.jimbozoomer.jugcraft.nuclear.DieselFuel.LOW) {
			o.alarms[5] = 1;
		}
		if (edg.fuelOut && !edg.running) {
			o.alarms[3] = 1;
		}
		if (!edg.running && (edg.fuelOut || edg.dayLevel <= 0)) {
			o.alarms[11] = 0;
			o.ready = false;
		}
	}

	/** The panels' readings for a generator, with the hour meter counting from {@code panel}'s link. */
	public static Snapshot of(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, EquipmentBlockEntity edg, double now) {
		Snapshot o = of(edg, now);
		o.hours = edg == null ? 0 : panel.hours(edg, now);
		return o;
	}

	/**
	 * A DC room's charger or switchboard with no generator near but its station battery (round 25: the batteries hold
	 * charge): what the battery does. A charger with its AC supply on charges at up to 150 A (equalize, the volts up to
	 * 135), then floats at 132 V once the battery is full; with no supply its AC lamp goes out, CHARGER FAIL comes in and
	 * the bus reads the battery alone (about 105 V flat to 129 V full). The switchboard reads the battery's own current,
	 * in or out.
	 */
	public static void battery(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level) {
		if (panel.battery == null
				|| !(level.getBlockEntity(panel.battery) instanceof io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity bank)) {
			return;
		}
		double soc = bank.charge();
		boolean charger = panel.input != null;
		double rate = charger ? panel.charging : bank.flow;
		o.chargerAc = charger ? panel.ac : bank.flow >= 0 && soc > 0.999 || bank.flow > 0;
		if (rate > 0) {
			o.dcA = rate / 8.0;
			o.equalize = o.dcA > 35;
			o.dcV = 129 + 6 * soc;
			// The equalize timer runs down as the battery fills: up to a day's equalize for a flat battery.
			o.eqHours = o.equalize ? 2 + 22 * (1 - soc) : 0;
		} else if (o.chargerAc) {
			o.dcA = 2;
			o.equalize = false;
			o.dcV = FLOAT_V;
		} else {
			o.dcA = rate < 0 ? -rate / 8.0 : 0;
			o.equalize = false;
			o.dcV = 105 + 24 * soc;
		}
		o.alarms[7] = o.chargerAc ? 0 : 1;
	}

	/**
	 * A vital inverter (round 26, 8g; Justin: "any meters and such are actually animated to work realistically"): on its
	 * station battery's DC it runs and carries the vital bus, 120 V at about 46 A and 60.00 Hz, each with a real meter's
	 * small wander. With the battery flat its static switch puts the load on the bypass (its regulating transformer
	 * beside it, alive while its MCC's REG XF breaker is closed: round 26, 8d), the transformer's 119 V or so on the
	 * grid's frequency; with that gone too, the output is dead and the display dark. The annunciator shows the inverter
	 * failed, low DC, the bypass source failed, the load on bypass, and the inverter running out of sync with no bypass to
	 * follow. Called after battery(), which reads the DC bus.
	 */
	public static void inverter(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level,
			double now) {
		double soc = 0;
		boolean charging = false;
		if (panel.battery != null && level.getBlockEntity(panel.battery) instanceof io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity bank) {
			soc = bank.charge();
			charging = bank.flow > 0;
		} else {
			o.dcV = 0;
		}
		o.invDc = soc > 0.005 || charging;
		o.invRun = o.invDc;
		// The bypass is there while its regulating transformer is alive (round 26, 8d: the transformer reads the breaker).
		o.invBypassOk = panel.xf != null && level.getBlockEntity(panel.xf) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity xf
				&& xf.energized(level);
		o.invOnInv = o.invRun;
		o.invOnBypass = !o.invRun && o.invBypassOk;
		double t = now / 20.0, seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		if (o.invOnInv) {
			o.invAcV = 120 + 0.4 * noise(t * 0.3, seed, 1);
			o.invHz = 60 + 0.01 * noise(t * 0.2, seed, 2);
		} else if (o.invOnBypass) {
			// On bypass its output is its regulating transformer's, so its AC VOLTS reads what the transformer's OUTPUT
			// VOLTS does (round 26, 8d: the same sag and wander, transformerVolts).
			o.invAcV = transformerVolts(panel.xf, true, now);
			o.invHz = 60 + 0.03 * noise(t * 0.1, seed, 2);
		} else {
			o.invAcV = 0;
			o.invHz = 0;
		}
		o.invAcA = o.invOnInv || o.invOnBypass ? 46 + 3 * noise(t * 0.4, seed, 3) : 0;
		if (!o.invDc) {
			o.dcV = Math.min(o.dcV, 105);
		}
	}

	/**
	 * A regulating transformer (round 26, 8d; Justin: "Meters and a lamp", animated to work realistically): a 480 V to
	 * 120 V constant-voltage (ferroresonant) transformer, its inverter's standby source. It reads its MCC's REG XF breaker
	 * and draws nothing (Justin: "Read the MCC breaker"); alive (LivePanelBlockEntity.energized), POWER ON is lit, its
	 * input reads about 480 V with a slow wander and its output a very steady 120 V. While its inverter runs on
	 * its battery it carries no load (0 A); with the battery flat the inverter's static switch puts the load on it, and
	 * its output amps read what the inverter's AC AMPS meter does (about 46 A, the same wander) while the output sags a
	 * little, to about 119.3 V. Dead: every meter at zero, the lamp out. Its winding temperature follows its losses
	 * slowly (a time constant of about 90 s of game time): room temperature dead, about 58 degrees on no load, 96
	 * carrying the load.
	 */
	public static void transformer(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level,
			double now) {
		boolean on = panel.energized(level), loaded = false;
		double amps = 0;
		if (on && panel.inverter != null && level.getBlockEntity(panel.inverter) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity inv
				&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isInverter(inv.getBlockState()) && panel.getBlockPos().equals(inv.xf)) {
			// Its inverter as that inverter's own panel reads it: on bypass, the load is this transformer's.
			Snapshot load = new Snapshot();
			inverter(load, inv, level, now);
			loaded = load.invOnBypass;
			amps = load.invAcA;
		}
		double t = now / 20.0, seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		o.xfOn = on;
		if (on) {
			o.xfInV = 480 + 2 * noise(t * 0.1, seed, 1);
			o.xfOutV = transformerVolts(panel.getBlockPos(), loaded, now);
			o.xfOutA = loaded ? amps : 0;
		} else {
			o.xfInV = 0;
			o.xfOutV = 0;
			o.xfOutA = 0;
		}
		double target = !on ? XF_ROOM : loaded ? XF_LOADED : XF_NO_LOAD;
		double[] w = WINDING.get(panel.getBlockPos());
		if (w == null && WINDING.size() > 256) {
			// Positions from worlds and places left behind: forgotten (each starts again at its target).
			WINDING.clear();
		}
		if (w == null) {
			w = new double[] {target, now};
			WINDING.put(panel.getBlockPos().immutable(), w);
		}
		// A long gap (its chunk unloaded and loaded again) counts as one time constant at most, so the needle carries on
		// from where it was rather than jumping to its end; time going back (another world) counts as none.
		double dt = Math.max(0, Math.min(XF_TAU, now - w[1]));
		w[0] = target + (w[0] - target) * Math.exp(-dt / XF_TAU);
		w[1] = now;
		o.xfTemp = w[0];
	}

	/**
	 * A live regulating transformer's output volts (round 26, 8d): a very steady 120 V (ferroresonant), sagging to about
	 * 119.3 V carrying its inverter's load; its wander seeded by its position, so its own meter and its inverter's on
	 * bypass read the same.
	 */
	static double transformerVolts(@org.jspecify.annotations.Nullable BlockPos xf, boolean loaded, double now) {
		double seed = xf == null ? 0 : (xf.hashCode() & 0xFFFF) * 0.0137;
		return (loaded ? 119.3 : 120) + 0.15 * noise(now / 20.0 * 0.3, seed, 2);
	}

	/** Nothing running, all settled. */
	static void standby(Snapshot o) {
		o.air = AIR;
		o.lube = LUBE_STANDBY;
		o.jacket = JACKET_STANDBY;
		o.dcV = FLOAT_V;
		o.dcA = FLOAT_A;
		o.chargerAc = true;
		o.ready = true;
		o.alarms[11] = 1;
		o.avr1 = line(0, 0);
		o.avr2 = "AVR AUTO  STANDBY";
		o.sequence = "LOOP SEQUENCE   STANDBY";
		relaysIdle(o);
	}

	static void running(Snapshot o, double s, double seed) {
		o.running = true;
		// The engine: cranked on air, then firing and running up, the governor's overshoot, and each load step's dip.
		double speed;
		if (s < 1.5) {
			speed = 0.12 * Math.min(1, s / 0.25);
		} else if (s < 4) {
			speed = 0.12 + (s - 1.5) / 2.5 * 0.88;
		} else {
			speed = 1 + 0.03 * Math.exp(-(s - 4) / 0.6) * Math.sin(Math.PI * (s - 4) / 0.9);
		}
		// The generator's field: flashed at about 3 s, building to rated voltage, with the regulator's overshoot.
		double build = s < 3 ? 0 : 1 - Math.exp(-(s - 3) / 0.3) * (1 + (s - 3) / 0.3);
		double volts = build * Math.min(1, speed / 0.97) + (s >= 3.4 ? 0.04 * pulse(s - 3.4, 0.15, 0.5) : 0);
		double p = 0, q = 0, forcing = 0;
		o.breaker = s >= BREAKER;
		if (o.breaker) {
			for (int k = 0; k < STEP_KW.length; k++) {
				double dt = s - (BREAKER + STEP * k);
				if (dt < 0) {
					break;
				}
				o.steps = k + 1;
				double size = STEP_KW[k] / 1500 * (MOTOR[k] ? 1 : 0.4);
				speed -= 0.032 * size * pulse(dt, 0.12, 0.9);
				volts -= 0.10 * size * pulse(dt, 0.04, 0.45);
				forcing += 75 * size * pulse(dt, 0.05, 0.6);
				p += STEP_KW[k] * (1 - Math.exp(-dt / 0.6)) + (MOTOR[k] ? 0.45 * STEP_KW[k] * pulse(dt, 0.05, 0.9) : 0);
				q += STEP_KW[k] * 0.54 * (1 - Math.exp(-dt / 0.6)) + (MOTOR[k] ? 2.6 * STEP_KW[k] * pulse(dt, 0.04, 1.1) : 0);
				if (MOTOR[k] && dt < 2.5) {
					o.starting = k;
				}
			}
			p += 10 * noise(s, seed, 1);
			q += 8 * noise(s, seed, 2);
		}
		if (s >= 4) {
			speed += 0.0005 * noise(s, seed, 3);
			volts += 0.0015 * noise(s, seed, 4);
		}
		o.rpm = RATED_RPM * Math.max(0, speed);
		o.hz = RATED_HZ * Math.max(0, speed);
		o.kv = RATED_KV * Math.max(0, volts);
		o.kw = Math.max(0, p);
		o.kvar = q;
		double va = Math.hypot(o.kw, q);
		o.amps = o.kv > 0.3 ? va / (Math.sqrt(3) * o.kv) : 0;
		o.pf = va > 1 ? o.kw / va : 0;
		o.fieldV = s < 3 ? 0 : Math.max(0, build * (62 + 115 * va / 7950) + forcing + 0.6 * noise(s, seed, 5));
		o.fieldA = o.fieldV / 1.62;
		// The engine's systems.
		o.air = s < 1.5 ? AIR - 5.5 * s / 1.5 : Math.min(AIR, AIR - 5.5 + 0.06 * (s - 1.5));
		// Lube oil: the prelube pump's 0.4 bar, the engine-driven pump's while cranking, then rising with speed.
		double engineDriven = 4.2 * Math.min(1, speed) * Math.min(1, speed);
		o.lube = LUBE_STANDBY + Math.max(0.9 * Math.min(1, s / 1.5), engineDriven) + (s >= 4 ? 0.03 * noise(s, seed, 6) : 0);
		o.jacket = JACKET_STANDBY + 27 * (1 - Math.exp(-s / 300)) + 0.1 * noise(s, seed, 7);
		// The battery chargers: no AC until their step, then recharging at their limit, tapering back to float.
		if (s < CHARGERS) {
			o.chargerAc = false;
			o.dcA = 0;
			o.dcV = FLOAT_V - 6 * (1 - Math.exp(-s / 25));
		} else {
			double c = s - CHARGERS;
			o.chargerAc = true;
			o.dcA = c < 25 ? 150 * Math.min(1, c / 0.8) : 20 + 130 * Math.exp(-(c - 25) / 70);
			o.equalize = o.dcA > 35;
			double from = FLOAT_V - 6 * (1 - Math.exp(-CHARGERS / 25));
			o.dcV = from + ((o.equalize ? 135 : FLOAT_V) - from) * (1 - Math.exp(-c / 8));
		}
		// The alarms: ENGINE RUNNING once up to speed; BATTERY CHGR FAIL while the chargers are off (flashing at first).
		o.alarms[10] = s > 4.2 ? 1 : 0;
		o.alarms[7] = s > 1 && s < CHARGERS ? (s < 6 ? 2 : 1) : 0;
		o.avr1 = line(o.kv, o.hz);
		o.avr2 = s < 3 ? "AVR AUTO  WAIT SPEED" : s < 4.5 ? "AVR AUTO  FIELD FLASH" : o.starting >= 0 ? "AVR AUTO  FIELD FORCE" : "AVR AUTO  REGULATING";
		if (!o.breaker) {
			o.sequence = "LOOP   DG STARTING";
		} else if (o.steps < STEP_KW.length || s < CHARGERS + 2) {
			o.sequence = String.format(Locale.ROOT, "LOOP   STEP %d OF 8   T+%02ds", o.steps, (int) (s - BREAKER));
		} else {
			o.sequence = "LOOP   SEQUENCE COMPLETE";
		}
		// The relays show what they measure, in turn with their number, once the generator is on the bus.
		if (o.breaker && ((int) (s / 3)) % 2 == 1) {
			o.relays[0] = String.format(Locale.ROOT, "Id%.1f%%", 0.2 + 0.05 * noise(s, seed, 8));
			o.relays[1] = String.format(Locale.ROOT, "%4.0fA", o.amps);
			o.relays[2] = String.format(Locale.ROOT, "P%+.1fMW", o.kw / 1000);
			o.relays[3] = String.format(Locale.ROOT, "Q%+.1fMV", o.kvar / 1000);
			o.relays[4] = String.format(Locale.ROOT, "I2 %.1f%%", 0.9 + 0.1 * noise(s, seed, 9));
			o.relays[5] = String.format(Locale.ROOT, "%.2fHz", o.hz);
			o.relays[6] = String.format(Locale.ROOT, "%4.0fV", o.kv * 1000);
			o.relays[7] = String.format(Locale.ROOT, "Vn%.1fV", 0.4 + 0.05 * noise(s, seed, 10));
		} else {
			relaysIdle(o);
		}
	}

	/** After a stop: breaker open, loads gone, the regulator de-excites and the engine coasts down. */
	static void stopping(Snapshot o, double s, Memory m, double seed) {
		double speed = Math.max(0, m.speed - 0.25 * s);
		o.rpm = RATED_RPM * speed;
		o.hz = RATED_HZ * speed;
		o.kv = speed > 0 ? m.kv * Math.exp(-s / 0.35) : 0;
		o.fieldV = m.fieldV * Math.exp(-s / 0.15);
		o.fieldA = o.fieldV / 1.62;
		o.lube = LUBE_STANDBY + 4.2 * speed * speed;
		o.air = Math.min(AIR, m.air + 0.06 * s);
		o.jacket = JACKET_STANDBY + (m.jacket - JACKET_STANDBY) * Math.exp(-s / 600);
		// Offsite power is back: the chargers finish recharging and go back to float.
		o.chargerAc = true;
		o.dcA = m.dcA > FLOAT_A ? FLOAT_A + (m.dcA - FLOAT_A) * Math.exp(-s / 40) : FLOAT_A;
		o.equalize = o.dcA > 35;
		o.dcV = o.equalize ? 135 : FLOAT_V;
		o.ready = speed <= 0 && s > 5;
		o.alarms[11] = o.ready ? 1 : 0;
		o.avr1 = line(o.kv, o.hz);
		o.avr2 = s < 4 ? "AVR AUTO  DE-EXCITED" : "AVR AUTO  STANDBY";
		o.sequence = s < 4 ? "LOOP SEQUENCE   RESET" : "LOOP SEQUENCE   STANDBY";
		relaysIdle(o);
	}

	private static void relaysIdle(Snapshot o) {
		String[] numbers = {"87G", "51V", "32", "40", "46", "81", "27/59", "64G"};
		System.arraycopy(numbers, 0, o.relays, 0, numbers.length);
	}

	private static String line(double kv, double hz) {
		return String.format(Locale.ROOT, "GEN %5.2f kV  %4.1f Hz", kv, hz);
	}

	/** A bump that rises in about {@code rise} seconds and dies away over {@code fall}, peaking at 1. */
	static double pulse(double t, double rise, double fall) {
		if (t <= 0) {
			return 0;
		}
		double peakT = Math.log(fall / rise) * rise * fall / (fall - rise);
		double peak = Math.exp(-peakT / fall) - Math.exp(-peakT / rise);
		return (Math.exp(-t / fall) - Math.exp(-t / rise)) / peak;
	}

	/** A slow, smooth wander between -1 and 1 (a real meter's restlessness), different for each reading. */
	static double noise(double t, double seed, int channel) {
		double c = seed + channel * 1.913;
		return (Math.sin(t * 2.31 + c) + Math.sin(t * 5.17 + c * 2.7) * 0.6 + Math.sin(t * 11.3 + c * 4.1) * 0.3) / 1.9;
	}
}
