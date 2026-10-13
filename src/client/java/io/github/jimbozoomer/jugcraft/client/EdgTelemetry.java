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
	/**
	 * A battery room's hydrogen (round 26, #9): its monitor's WARN and ALARM (% LEL); hydrogen given off a cell's worth of
	 * charging amps; what the fan carries away on high, low and off (the room's own leakage); how fast it follows (game
	 * ticks); each monitor's reading as last drawn, and when.
	 */
	public static final double H2_WARN = 10, H2_ALARM = 25, H2_BASE = 0.3, H2_GAS = 0.12, H2_FLOAT_GAS = 0.04, H2_TAU = 600;
	static final double[] H2_VENT = {2.5, 1.0, 0.05};
	private static final Map<BlockPos, double[]> HYDROGEN = new HashMap<>();
	/** An exhaust fan: its blades' turn (degrees), speed (turns a second) and when, as last drawn; its two speeds. */
	private static final Map<BlockPos, double[]> FAN = new HashMap<>();
	static final double FAN_LOW = 0.6, FAN_HIGH = 1.5, FAN_CHARGING = 20;

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
		/** A vital panel (round 26, 8e): POWER ON, and its breakers (bit 0 the main, 1 to 12 its circuits, set closed). */
		public boolean vbOn;
		public int vbBreakers;
		/**
		 * A ground detector and battery monitor (round 26, 8f): its DC (the bus alive), each side's volts to ground and
		 * which side is grounded (its test: 0 none, 1 +, 2 -); the monitor's screen alive, the string's volts and amps
		 * (in positive), its charge (%), temperature (degrees C) and state, each cell's volts, its ALARM lamp.
		 */
		public boolean gdOn, bmOn, bmAlarm;
		public int gdGround;
		public double gdPos, gdNeg, bmV, bmA, bmSoc, bmTemp;
		public String bmMode = "", bmString = "";
		public final double[] cells = new double[60];
		/**
		 * A battery room's hydrogen monitor (round 26, #9): hydrogen in the air (% of its lower explosive limit), and its
		 * exhaust fan: its selector (ExhaustFanBlock.MODES), running, on high speed, its blades' turn (degrees).
		 */
		public double h2Lel, fanAngle;
		public int fanMode = -1;
		public boolean fanRun, fanHigh;
		/** Sequencer steps done, and the one whose motor is starting now (-1 none). */
		public int steps, starting = -1;
		public String avr1 = "", avr2 = "", sequence = "";
		public final String[] relays = new String[8];
		/** Alarm windows: 0 dark, 1 lit, 2 flashing (new). */
		public final int[] alarms = new int[12];
		public double time;
		/** A relay room cabinet (round 26, #10): its meters (0 to 100 of their scales), lamps (bit k: rr_l<k>), name. */
		public final double[] rr = new double[2];
		public int rrLamps;
		public String label = "";
		/**
		 * The control room HVAC's panel and monitors (round 26, #11: {@link #hvac}) and the Technical Support Center's
		 * panels (#12: {@link #tsc}): their meters (0 to 100 of their scales), lamps and windows (0 dark, 1 lit, 2 its
		 * second colour or flashing) and displays, by channel (hv_..., ts_...).
		 */
		public final Map<String, Double> hvNumbers = new HashMap<>();
		public final Map<String, Integer> hvLit = new HashMap<>();
		public final Map<String, String> hvText = new HashMap<>();

		/** A meter's or gauge's reading. */
		public double number(String channel) {
			if (channel.startsWith("bm_c")) {
				int i = Integer.parseInt(channel.substring(4));
				return bmOn && i >= 0 && i < cells.length ? cells[i] : 0;
			}
			if (channel.startsWith("hv_") || channel.startsWith("ts_")) {
				return hvNumbers.getOrDefault(channel, 0.0);
			}
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
				case "gd_pos_v" -> gdPos;
				case "gd_neg_v" -> gdNeg;
				case "ef_spin" -> fanAngle;
				case "rr_m0" -> rr[0];
				case "rr_m1" -> rr[1];
				default -> 0;
			};
		}

		/** A lamp's or window's state: 0 dark, 1 lit (its colour), 2 its second colour or flashing. */
		public int lit(String channel) {
			if (channel.startsWith("rr_l")) {
				return rrLamps >> Integer.parseInt(channel.substring(4)) & 1;
			}
			if (channel.startsWith("hv_") || channel.startsWith("ts_")) {
				return hvLit.getOrDefault(channel, 0);
			}
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
				// A vital panel's POWER ON lamp; its breakers' handles (1 closed: ON).
				case "vb_on" -> vbOn ? 1 : 0;
				// A ground detector's lamps: DC ON, the grounded side's GND, the monitor's ALARM (flashing).
				case "gd_dc" -> gdOn ? 1 : 0;
				case "gd_pos" -> gdGround == 1 ? 1 : 0;
				case "gd_neg" -> gdGround == 2 ? 1 : 0;
				case "bm_alarm" -> bmAlarm ? 1 : 0;
				// A battery room's hydrogen monitor and exhaust fan (round 26, #9).
				case "h2_ok" -> h2Lel < H2_WARN ? 1 : 0;
				case "h2_warn" -> h2Lel >= H2_WARN && h2Lel < H2_ALARM ? 1 : 0;
				case "h2_alarm" -> h2Lel >= H2_ALARM ? 1 : 0;
				case "ef_run" -> fanRun ? 1 : 0;
				case "ef_high" -> fanHigh ? 1 : 0;
				case "ef_mode_0" -> fanMode == 0 ? 1 : 0;
				case "ef_mode_1" -> fanMode == 1 ? 1 : 0;
				case "ef_mode_2" -> fanMode == 2 ? 1 : 0;
				default -> channel.startsWith("vb_") ? vbBreakers >> Integer.parseInt(channel.substring(3)) & 1 : 0;
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
			if (channel.startsWith("hv_") || channel.startsWith("ts_")) {
				return hvText.getOrDefault(channel, "");
			}
			if (channel.startsWith("bm_")) {
				// The battery monitor's screen, dark with no DC.
				if (!bmOn) {
					return "";
				}
				return switch (channel) {
					case "bm_v" -> String.format(Locale.ROOT, "%.1f", bmV);
					case "bm_a" -> String.format(Locale.ROOT, "%+.1f", bmA);
					case "bm_soc" -> String.format(Locale.ROOT, "%.0f", bmSoc);
					case "bm_t" -> String.format(Locale.ROOT, "%.1f", bmTemp);
					case "bm_mode" -> bmMode;
					case "bm_str" -> bmString;
					default -> "";
				};
			}
			if (channel.equals("h2_lel")) {
				return String.format(Locale.ROOT, "%.1f", h2Lel);
			}
			if (channel.equals("inv_hz")) {
				// The frequency display goes dark with the inverter's own control power (its DC) and no bypass.
				return invOnInv || invOnBypass ? String.format(Locale.ROOT, "%.2f", invHz) : "";
			}
			return switch (channel) {
				case "label" -> label;
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
		// A switchboard's battery current is all its racks' in parallel (round 26, #9).
		double rate = charger ? panel.charging : panel.racks.isEmpty() ? bank.flow : flow(level, panel.racks);
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
		// Its load (round 26, 8e): what its vital panel's closed breakers draw, with a real meter's small wander; with no
		// panel linked, the 46 A it always carried.
		double load = panel.panel != null && level.getBlockEntity(panel.panel) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity vb
				&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isVitalPanel(vb.getBlockState())
				? io.github.jimbozoomer.jugcraft.nuclear.VitalPanelBlock.load(vb.breakers) : 46;
		o.invAcA = o.invOnInv || o.invOnBypass ? load * (1 + 0.06 * noise(t * 0.4, seed, 3)) : 0;
		if (!o.invDc) {
			o.dcV = Math.min(o.dcV, 105);
		}
	}

	/**
	 * A 120 VAC vital panel (round 26, 8e; Justin: "Switchable breakers"): POWER ON lit while its inverter's output is
	 * alive (on its battery or on its bypass) and its main is closed; its breakers' handles as they are set.
	 */
	public static void vitalPanel(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level,
			double now) {
		o.vbBreakers = panel.breakers;
		boolean alive = false;
		if (panel.inverter != null && level.getBlockEntity(panel.inverter) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity inv
				&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isInverter(inv.getBlockState())) {
			Snapshot feed = new Snapshot();
			inverter(feed, inv, level, now);
			alive = feed.invOnInv || feed.invOnBypass;
		}
		o.vbOn = alive && (panel.breakers & 1) != 0;
	}

	/**
	 * A ground detector and battery monitor (round 26, 8f; Justin: "Healthy, with TEST", "Yes, with a screen"): on its
	 * station battery's DC (read as the switchboard reads it, battery()). Its voltmeters read each side of the bus to
	 * ground: half the bus each, a hair apart, while neither side is grounded (no ground faults are modelled); while a
	 * test button is held (nuclear/GroundDetectorBlock.java) that side's reads about 2 V and the other the rest of the
	 * bus, its GND lamp lights, the screen says which side and the ALARM lamp flashes. The monitor: the string's volts,
	 * its amps (in positive, out negative: a couple of amps in on float, the charge rate on recharge or equalize, the
	 * load on discharge), its charge, its temperature (a little warmer charging hard), its state, and its 60 cells'
	 * volts, each the string's share with its own small spread (a healthy battery: none below the alarm level until it
	 * runs down). ALARM also flashes with the bus low (under 124 V). No battery: everything dark and at zero.
	 */
	public static void groundDetector(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level,
			double now) {
		if (panel.battery == null
				|| !(level.getBlockEntity(panel.battery) instanceof io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity bank)) {
			o.dcV = 0;
			o.dcA = 0;
			return;
		}
		double soc = bank.charge();
		double t = now / 20.0, seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		double bus = io.github.jimbozoomer.jugcraft.nuclear.GroundDetectorBlock.busVolts(bank);
		o.gdOn = true;
		o.bmOn = true;
		o.gdGround = panel.testing(now) ? panel.testSide : 0;
		double gnd = 1.8 + 0.3 * noise(t * 0.5, seed, 4);
		double half = bus / 2 + 0.3 + 0.2 * noise(t * 0.2, seed, 5);
		o.gdPos = o.gdGround == 1 ? gnd : o.gdGround == 2 ? bus - gnd : half;
		o.gdNeg = o.gdGround == 2 ? gnd : o.gdGround == 1 ? bus - gnd : bus - half;
		o.bmV = bus + 0.05 * noise(t * 0.3, seed, 6);
		// The string on the screen (round 26, #9; MENU steps through them): its racks' current and charge.
		java.util.List<java.util.List<BlockPos>> strings = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.strings(level, panel.racks);
		java.util.List<BlockPos> shown = strings.isEmpty() ? java.util.List.of(panel.battery) : strings.get(Math.floorMod(panel.page, strings.size()));
		o.bmString = strings.size() > 1 ? String.format(Locale.ROOT, "STR %d/%d", Math.floorMod(panel.page, strings.size()) + 1, strings.size()) : "";
		int f = flow(level, shown);
		soc = charge(level, shown, soc);
		double rate = f / 8.0;
		boolean full = soc > 0.999;
		o.bmA = (f != 0 ? rate : full ? 2.0 / Math.max(1, strings.size()) : 0) + 0.1 * noise(t * 0.4, seed, 7);
		o.bmSoc = soc * 100;
		o.bmTemp = 25 + (f > 0 ? Math.min(3, rate / 50) : 0) + 0.15 * noise(t * 0.05, seed, 8);
		o.bmMode = o.gdGround == 1 ? "+ GND FAULT" : o.gdGround == 2 ? "- GND FAULT"
				: f > 0 ? (rate > 35 ? "EQUALIZE" : "RECHARGE") : f < 0 ? "DISCHARGE" : full ? "FLOAT" : "OPEN CKT";
		for (int i = 0; i < o.cells.length; i++) {
			double h = Math.sin(i * 12.9898 + seed * 78.233) * 43758.5453;
			o.cells[i] = o.bmV / o.cells.length + 0.03 * ((h - Math.floor(h)) * 2 - 1) + 0.002 * noise(t * 0.3, seed + i, 9);
		}
		boolean blink = ((long) (now / 10)) % 2 == 0;
		o.bmAlarm = (o.gdGround != 0 || bus < 124) && blink;
		o.dcV = bus;
	}

	/** What a battery's racks take in (+) or give out (-) together (JE a tick, as their meters read it). */
	static int flow(net.minecraft.world.level.Level level, java.util.List<BlockPos> racks) {
		int f = 0;
		for (BlockPos at : racks) {
			if (at != null && level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity bank) {
				f += bank.flow;
			}
		}
		return f;
	}

	/** Racks' average charge (0..1), or fallback with none there. */
	static double charge(net.minecraft.world.level.Level level, java.util.List<BlockPos> racks, double fallback) {
		double sum = 0;
		int n = 0;
		for (BlockPos at : racks) {
			if (at != null && level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.StationBatteryBlockEntity bank) {
				sum += bank.charge();
				n++;
			}
		}
		return n == 0 ? fallback : sum / n;
	}

	/**
	 * An exhaust fan's speed (round 26, #9; Justin: "Hydrogen monitor and fan"), as its selector sets it: HAND high, OFF
	 * stopped, AUTO low while the battery floats and high while it charges hard (over FAN_CHARGING amps, when it gases).
	 * 0 stopped, 1 low, 2 high.
	 */
	static int fanSpeed(int mode, double chargingAmps) {
		return switch (mode) {
			case io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.HAND -> 2;
			case io.github.jimbozoomer.jugcraft.nuclear.ExhaustFanBlock.OFF -> 0;
			default -> chargingAmps > FAN_CHARGING ? 2 : 1;
		};
	}

	/** What a battery room's racks take in charging (amps; 0 floating or discharging). */
	static double chargingAmps(net.minecraft.world.level.Level level, java.util.List<BlockPos> racks) {
		return Math.max(0, flow(level, racks)) / 8.0;
	}

	/**
	 * A battery room's exhaust fan (round 26, #9): its RUN and HIGH lamps, the selector's pointer, and its blades
	 * turning, spinning up and running down over a second or two when its speed changes.
	 */
	public static void exhaustFan(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level,
			double now) {
		o.fanMode = Math.floorMod(panel.mode, 3);
		int speed = fanSpeed(o.fanMode, chargingAmps(level, panel.racks));
		o.fanRun = speed > 0;
		o.fanHigh = speed == 2;
		double target = speed == 2 ? FAN_HIGH : speed == 1 ? FAN_LOW : 0;
		double[] m = FAN.get(panel.getBlockPos());
		if (m == null && FAN.size() > 256) {
			FAN.clear();
		}
		if (m == null) {
			m = new double[] {(panel.getBlockPos().hashCode() & 0xFF) * 1.4, target, now};
			FAN.put(panel.getBlockPos().immutable(), m);
		}
		double dt = Math.max(0, Math.min(40, now - m[2]));
		m[1] = target + (m[1] - target) * Math.exp(-dt / 25);
		m[0] = (m[0] + 360 * m[1] * dt / 20) % 360;
		m[2] = now;
		o.fanAngle = m[0];
	}

	/**
	 * A battery room's hydrogen monitor (round 26, #9): the hydrogen in the air (% LEL). A floating battery gives off a
	 * little, one charging hard (its cells gassing) more with its current; the exhaust fan (its panel) carries it away,
	 * fast on high, less on low, and with the fan off only the room's own leakage does, so the reading creeps up. The
	 * reading follows slowly (two thirds of the way in about half a minute); NORMAL under WARN, then WARN, then ALARM.
	 */
	public static void hydrogen(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level,
			double now) {
		double amps = chargingAmps(level, panel.racks);
		int speed = 0;
		if (panel.panel != null && level.getBlockEntity(panel.panel) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity fan
				&& io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.isExhaustFan(fan.getBlockState())) {
			speed = fanSpeed(Math.floorMod(fan.mode, 3), amps);
		}
		double gas = H2_FLOAT_GAS + H2_GAS * amps;
		double target = Math.min(100, H2_BASE + gas / H2_VENT[2 - speed]);
		double[] m = HYDROGEN.get(panel.getBlockPos());
		if (m == null && HYDROGEN.size() > 256) {
			HYDROGEN.clear();
		}
		if (m == null) {
			m = new double[] {target, now};
			HYDROGEN.put(panel.getBlockPos().immutable(), m);
		}
		double dt = Math.max(0, Math.min(H2_TAU, now - m[1]));
		m[0] = target + (m[0] - target) * Math.exp(-dt / H2_TAU);
		m[1] = now;
		double seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		o.h2Lel = Math.max(0, m[0] + 0.05 * noise(now / 20.0 * 0.2, seed, 10));
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
	/**
	 * A relay room cabinet (round 26, #10): a relay panel's name on its nameplate; the others what they read of the
	 * reactor (nuclear/RelayRoom.java readings), each meter with its own small wander while there is a reactor to read.
	 */
	public static void relayRoom(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, double now) {
		int kind = io.github.jimbozoomer.jugcraft.nuclear.RelayRoom.kind(panel.getBlockState());
		int variant = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(panel.getBlockState());
		if (kind == io.github.jimbozoomer.jugcraft.nuclear.RelayRoom.RELAY) {
			o.label = io.github.jimbozoomer.jugcraft.nuclear.RelayRoom.label(kind, variant);
			return;
		}
		io.github.jimbozoomer.jugcraft.nuclear.RelayRoom.Readings r = io.github.jimbozoomer.jugcraft.nuclear.RelayRoom.readings(kind, variant, panel.reactor());
		double t = now / 20.0, seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		double wander = panel.rxStatus != 0 ? 0.35 : 0;
		o.rr[0] = r.m0() + wander * noise(t * 0.3, seed, 11);
		o.rr[1] = r.m1() + wander * noise(t * 0.25, seed, 12);
		o.rrLamps = r.lamps();
	}

	// ------------------------------------------------------------------ the control room HVAC (round 26, #11)

	/**
	 * The control room's differential pressure (inches of water) normally (the air handlers' outside air) and on
	 * filtered air (the filtration units' booster fans: 1/8 inch, a little more with both); its temperature (deg F)
	 * cooled, with an air handler but no chilled water, and with no air moving; chilled water supply (deg F) with a
	 * chiller running and without; and how fast each follows (game ticks).
	 */
	static final double HV_DP_NORMAL = 0.06, HV_DP_EMERGENCY = 0.125, HV_TEMP = 72.5, HV_TEMP_WARM = 82, HV_TEMP_HOT = 86, HV_CHW = 44.2,
			HV_CHW_WARM = 58;
	static final double HV_TAU_DP = 60, HV_TAU_FLOW = 80, HV_TAU_TEMP = 2400, HV_TAU_CHW = 900;
	/** An alarm window flashes this long after it comes in, then stays lit (game ticks). */
	static final double HV_FLASH = 200;
	/** Each HVAC panel's readings as last drawn (pressure, temperature, chilled water, flow, when), and when each window came in. */
	private static final Map<BlockPos, double[]> HVAC = new HashMap<>();
	private static final Map<BlockPos, double[]> HVAC_ALARMS = new HashMap<>();

	/**
	 * The control room HVAC's panel and wall monitors (round 26, #11; nuclear/HvacRoom.java): the panel's mimic and
	 * annunciator as the server found its room, its selector and lamps, and its meters following what runs; a radiation
	 * monitor's count rate (its scatter, a 30 second count's, new every two seconds); the toxic gas analysers' and the
	 * refrigerant monitor's clean air.
	 */
	public static void hvac(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, double now) {
		int kind = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.kind(panel.getBlockState());
		int variant = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(panel.getBlockState());
		o.label = variant == 1 ? "B" : "A";
		double seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		long bucket = (long) Math.floor(now / 40);
		double frac = now / 40 - bucket;
		switch (kind) {
			case io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.CONTROL -> hvacPanel(o, panel, now, seed);
			case io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.RAD -> {
				double cpm = Math.max(io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.RAD_BACKGROUND, panel.hvValue);
				double sd = 1 / Math.sqrt(Math.max(1, cpm * 0.5));
				double a = Math.max(1, cpm * (1 + sd * scatter(bucket, seed))), b = Math.max(1, cpm * (1 + sd * scatter(bucket + 1, seed)));
				double needle = a + (b - a) * frac;
				o.hvNumbers.put("hv_rad_m", (Math.log10(Math.max(10, needle)) - 1) / 5 * 100);
				o.hvText.put("hv_rad_cpm", a < 999_999.5 ? String.format(Locale.ROOT, "%.0f", a) : String.format(Locale.ROOT, "%.2E", a));
				boolean high = cpm >= io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.RAD_HIGH;
				boolean alert = cpm >= io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.RAD_ALERT;
				o.hvLit.put("hv_rad_ok", alert ? 0 : 1);
				o.hvLit.put("hv_rad_alert", alert && !high ? 1 : 0);
				o.hvLit.put("hv_rad_high", high ? 1 : 0);
			}
			case io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.TOX -> {
				// Clean air: chlorine and ammonia at the analyser's zero, now and then its last digit.
				o.hvText.put("hv_cl2", String.format(Locale.ROOT, "%.1f", Math.max(0, 0.02 + 0.04 * scatter(bucket, seed + 3))));
				o.hvText.put("hv_nh3", String.format(Locale.ROOT, "%.0f", Math.max(0, 0.2 + 0.3 * scatter(bucket, seed + 7))));
				o.hvLit.put("hv_tox_ok", 1);
			}
			default -> {
				// No leak: each chiller's sensor reads the room's few ppm of R-134a.
				o.hvText.put("hv_rf_a", String.format(Locale.ROOT, "%.0f", Math.max(0, 1.2 + 0.8 * scatter(bucket, seed + 11))));
				o.hvText.put("hv_rf_b", String.format(Locale.ROOT, "%.0f", Math.max(0, 1.0 + 0.8 * scatter(bucket, seed + 13))));
				o.hvLit.put("hv_rf_ok", 1);
			}
		}
	}

	private static void hvacPanel(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, double now, double seed) {
		int s = panel.hvState, a = panel.hvAlarms;
		boolean emergency = (a & io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.EMERGENCY_ON) != 0;
		int efus = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.running(s, io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.EFU);
		int ahus = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.running(s, io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.AHU);
		int chillers = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.running(s, io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.CH);
		boolean cooled = false;
		for (int t = 0; t < 2; t++) {
			cooled |= io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.on(s, io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.AHU, t)
					&& io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.on(s, io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.CH, t);
		}
		double dp = emergency ? (efus > 0 ? HV_DP_EMERGENCY + 0.01 * (efus - 1) : 0.02) : ahus > 0 ? HV_DP_NORMAL : 0;
		double temp = cooled ? HV_TEMP : ahus > 0 ? HV_TEMP_WARM : HV_TEMP_HOT;
		double chw = chillers > 0 ? HV_CHW : HV_CHW_WARM;
		double flow = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.EFU_CFM * efus;
		BlockPos at = panel.getBlockPos().immutable();
		double[] m = HVAC.computeIfAbsent(at, k -> new double[] {dp, temp, chw, flow, now});
		double dt = Math.max(0, Math.min(400, now - m[4]));
		m[4] = now;
		m[0] += (dp - m[0]) * (1 - Math.exp(-dt / HV_TAU_DP));
		m[1] += (temp - m[1]) * (1 - Math.exp(-dt / HV_TAU_TEMP));
		m[2] += (chw - m[2]) * (1 - Math.exp(-dt / HV_TAU_CHW));
		m[3] += (flow - m[3]) * (1 - Math.exp(-dt / HV_TAU_FLOW));
		double t = now / 20.0;
		o.hvNumbers.put("hv_m_dp", m[0] / 0.25 * 100 + (m[0] > 0.005 ? 0.6 * noise(t * 0.4, seed, 21) : 0));
		o.hvNumbers.put("hv_m_temp", (m[1] - 60) / 30 * 100 + 0.4 * noise(t * 0.1, seed, 22));
		o.hvNumbers.put("hv_m_chw", (m[2] - 30) / 40 * 100 + 0.3 * noise(t * 0.15, seed, 23));
		o.hvNumbers.put("hv_m_flow", m[3] / 3000 * 100 + (m[3] > 10 ? 0.5 * noise(t * 0.3, seed, 24) : 0));
		// The selector's pointer, the mode lamps.
		for (int k = 0; k < 3; k++) {
			o.hvLit.put("hv_sel" + k, Math.floorMod(panel.mode, 3) == k ? 1 : 0);
		}
		o.hvLit.put("hv_l_normal", !emergency && ahus > 0 ? 1 : 0);
		o.hvLit.put("hv_l_emerg", emergency ? 1 : 0);
		o.hvLit.put("hv_l_press", (a & io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.A_LOW_DP) == 0 ? 1 : 0);
		// The mimic: red running (open), green stopped (shut), dark where there is none.
		for (int train = 0; train < 2; train++) {
			for (int part = 0; part < io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.PARTS.length; part++) {
				int lit = io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.on(s, part, train) ? 1
						: io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.present(s, part, train) ? 2 : 0;
				o.hvLit.put("hv_" + io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.PARTS[part] + "_" + (train == 0 ? "a" : "b"), lit);
			}
		}
		// The annunciator: a window flashes when it comes in, then stays lit.
		double[] came = HVAC_ALARMS.computeIfAbsent(at, k -> {
			double[] c = new double[8];
			java.util.Arrays.fill(c, -1);
			return c;
		});
		for (int k = 0; k < 8; k++) {
			if ((a >> k & 1) == 0) {
				came[k] = -1;
				o.hvLit.put("hv_a" + k, 0);
			} else {
				if (came[k] < 0) {
					came[k] = now;
				}
				o.hvLit.put("hv_a" + k, now - came[k] < HV_FLASH ? 2 : 1);
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ the TSC (#12)
	/** Each TSC board's view of the plant, worked out once a tick (its game tick, the view). */
	private static final Map<BlockPos, Object[]> TSC_VIEWS = new HashMap<>();

	/**
	 * The Technical Support Center's panels (round 26, #12; nuclear/TscRoom.java): each draws its board's view of the
	 * plant (the class and its clocks, the reactor, the CR HVAC and its intakes, the dose rate in the TSC, the electrical
	 * plant, the log), the game's clock and weather and the players about. The area radiation monitor and the frisker
	 * read the dose rate where they are (the frisker's count scattering as a real one's does).
	 */
	public static void tsc(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel, net.minecraft.world.level.Level level, double now) {
		int kind = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.kind(panel.getBlockState());
		int variant = io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity.variant(panel.getBlockState());
		Map<String, String> t = o.hvText;
		Map<String, Integer> lit = o.hvLit;
		double seed = (panel.getBlockPos().hashCode() & 0xFFFF) * 0.0137;
		if (kind == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.AREA) {
			double mr = Math.max(0, panel.tsValue) * 100;
			o.hvNumbers.put("ts_am_m", (Math.log10(Math.max(0.01, mr)) + 2) / 5 * 100);
			t.put("ts_am_v", mr < 10 ? String.format(Locale.ROOT, "%.2f", mr) : mr < 1000 ? String.format(Locale.ROOT, "%.0f", mr) : String.format(Locale.ROOT, "%.1E", mr));
			boolean high = panel.tsValue >= io.github.jimbozoomer.jugcraft.nuclear.TscRoom.AREA_HIGH;
			boolean alert = panel.tsValue >= io.github.jimbozoomer.jugcraft.nuclear.TscRoom.AREA_ALERT;
			lit.put("ts_am_ok", alert ? 0 : 1);
			lit.put("ts_am_alert", alert && !high ? 1 : 0);
			lit.put("ts_am_high", high ? 1 : 0);
			return;
		}
		if (kind == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.FRISKER) {
			double cpm = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.FRISK_BACKGROUND
					+ Math.max(0, panel.tsValue) * 100 * io.github.jimbozoomer.jugcraft.nuclear.TscRoom.FRISK_PER_MR;
			long bucket = (long) Math.floor(now / 10);
			double shown = cpm + Math.sqrt(cpm) * scatter(bucket, seed);
			o.hvNumbers.put("ts_fr_m", Math.max(0, shown) / 1000 * 100);
			return;
		}
		io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hub = kind == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB ? panel
				: panel.panel != null && level.getBlockEntity(panel.panel) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity h
						&& io.github.jimbozoomer.jugcraft.nuclear.TscRoom.kind(h.getBlockState()) == io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB ? h : null;
		long day = level.getOverworldClockTime();
		t.put("ts_clock", io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(day));
		if (hub == null) {
			// No board in reach: the screens say so; nothing else lights.
			for (int k = 0; k < 3; k++) {
				t.put("ts_p" + k + "_m0_t", "NO DATA");
				t.put("ts_c" + k + "_t", "NO DATA");
				t.put("ts_n" + k + "_t", "NO DATA");
			}
			return;
		}
		io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v = view(hub, level, day);
		switch (kind) {
			case io.github.jimbozoomer.jugcraft.nuclear.TscRoom.HUB -> {
				for (int i = 0; i < 5; i++) {
					lit.put("ts_cls" + i, v.cls == i ? 1 : 0);
				}
				t.put("ts_event", v.event);
				t.put("ts_declared", v.declared);
			}
			case io.github.jimbozoomer.jugcraft.nuclear.TscRoom.VIDEO -> videoWall(o, v, variant);
			case io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CONSOLE -> {
				for (int k = 0; k < 2; k++) {
					int pos = Math.floorMod(variant, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.POSITIONS.length);
					t.put("ts_name" + k, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.POSITIONS[pos][k]);
					for (int m = 0; m < 2; m++) {
						screen(t, "ts_p" + k + "_m" + m, page(v, pos * 2 + k, m));
					}
				}
			}
			case io.github.jimbozoomer.jugcraft.nuclear.TscRoom.COMMS_DESK -> {
				for (int k = 0; k < 3; k++) {
					t.put("ts_cname" + k, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.COMMS[k]);
					screen(t, "ts_c" + k, comms(v, k));
				}
			}
			case io.github.jimbozoomer.jugcraft.nuclear.TscRoom.NRC -> {
				for (int k = 0; k < 2; k++) {
					screen(t, "ts_n" + k, nrc(v, k));
				}
			}
			case io.github.jimbozoomer.jugcraft.nuclear.TscRoom.BOARD -> statusBoard(t, v, hub, variant);
			default -> {
			}
		}
	}

	private static io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View view(io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hub,
			net.minecraft.world.level.Level level, long day) {
		long tick = level.getGameTime();
		Object[] cached = TSC_VIEWS.get(hub.getBlockPos());
		if (cached != null && (long) cached[0] == tick && cached[2] == hub) {
			return (io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View) cached[1];
		}
		if (TSC_VIEWS.size() > 64) {
			TSC_VIEWS.clear();
		}
		io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v = new io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View(hub, day, level.isRaining(),
				level.isThundering(), io.github.jimbozoomer.jugcraft.nuclear.TscRoom.players(level, hub.getBlockPos()));
		TSC_VIEWS.put(hub.getBlockPos().immutable(), new Object[] {tick, v, hub});
		return v;
	}

	private static String f0(double d) {
		return String.format(Locale.ROOT, "%.0f", d);
	}

	/** A count rate, or dashes for a monitor not found. */
	private static String cpm(double d) {
		return d < 0 ? "----" : d < 1e6 ? f0(d) : String.format(Locale.ROOT, "%.1E", d);
	}

	private static String hvacMode(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v) {
		return !v.hvac ? "----" : v.hvacEmerg ? "EMERG" : "NORMAL";
	}

	private static void videoWall(Snapshot o, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v, int variant) {
		Map<String, String> t = o.hvText;
		switch (variant) {
			case 0 -> {
				for (int i = 0; i < 6; i++) {
					o.hvLit.put("ts_csf" + i, v.csf[i]);
				}
				String[] spds = {String.format(Locale.ROOT, "%.1f", v.power), f0(v.tavg), f0(v.pzrP), f0(v.pzrL), f0(v.sg), f0(v.flow), f0(v.rods), f0(v.cet)};
				for (int i = 0; i < spds.length; i++) {
					t.put("ts_spds" + i, v.present ? spds[i] : "----");
				}
				o.hvLit.put("ts_rx_band", !v.present ? 0 : v.tripped ? 2 : 1);
				t.put("ts_rx_state", v.rxState);
			}
			case 1 -> {
				String[] rad = {cpm(v.intakeA), cpm(v.intakeB), hvacMode(v), String.format(Locale.ROOT, "%.2f", v.areaMr), v.vent,
						String.format(Locale.ROOT, "%.1f", v.windSpeed), f0(v.windFrom), v.stability};
				for (int i = 0; i < rad.length; i++) {
					t.put("ts_rad" + i, rad[i]);
				}
				t.put("ts_downwind", "DOWNWIND  " + v.downwind);
				o.hvLit.put("ts_release_band", v.release ? 2 : 1);
				t.put("ts_release", v.release ? "RELEASE IN PROGRESS" : "NO RELEASE");
			}
			default -> {
				for (int i = 0; i < 6; i++) {
					t.put("ts_elec" + i, v.elec[i]);
				}
				soe(t, v);
				t.put("ts_class", io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CLASS_SHORT[v.cls]);
			}
		}
	}

	/** The last six entries of the log. */
	private static void soe(Map<String, String> t, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v) {
		int n = v.events.size(), from = Math.max(0, n - 6);
		for (int i = 0; i < 6; i++) {
			int k = from + i;
			t.put("ts_soe_t" + i, k < n ? v.times.get(k) : "");
			t.put("ts_soe_e" + i, k < n ? v.events.get(k) : "");
		}
	}

	private static void statusBoard(Map<String, String> t, io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v,
			io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity hub, int variant) {
		switch (variant) {
			case 0 -> {
				// The positions' T-cards: the plant's roster (the IN column waits for its staff).
				String[] names = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.roster(hub.getBlockPos(), 8, 0);
				for (int i = 0; i < 8; i++) {
					t.put("ts_staff" + i, names[i]);
					t.put("ts_in" + i, "");
				}
			}
			case 1 -> {
				for (int i = 0; i < 7; i++) {
					t.put("ts_pri" + i, v.priority(i));
				}
				t.put("ts_brief", v.brief);
			}
			case 2 -> {
				soe(t, v);
				for (int i = 0; i < 3; i++) {
					t.put("ts_par" + i, v.par[i]);
				}
				t.put("ts_wind", String.format(Locale.ROOT, "%.0f DEG  %.0f MPH", v.windFrom, v.windSpeed));
			}
			default -> {
				String due = v.emergency && hub.tsDeclared >= 0
						? io.github.jimbozoomer.jugcraft.nuclear.TscRoom.clock(hub.tsDeclared + io.github.jimbozoomer.jugcraft.nuclear.TscRoom.NRC_DUE) : "--:--";
				String[] fields = {io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CLASS_SHORT[v.cls], v.declared, due, v.event};
				for (int i = 0; i < fields.length; i++) {
					t.put("ts_nrc" + i, fields[i]);
				}
				String[] team = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.roster(hub.getBlockPos(), 2, 1);
				t.put("ts_nrc_t0", "LEAD  " + team[0]);
				t.put("ts_nrc_t1", "RESIDENT  " + team[1]);
				for (int i = 0; i < 4; i++) {
					int k = i + (v.emergency ? 1 : 0);
					t.put("ts_nrc_f" + i, k < v.priorities.size() ? v.priorities.get(k) : "");
				}
			}
		}
	}

	/** A screen: its title, then each line's name (left) and value (right). */
	private static void screen(Map<String, String> t, String stem, String[] page) {
		t.put(stem + "_t", page[0]);
		for (int i = 0; i < 6; i++) {
			t.put(stem + "_l" + i, page[1 + 2 * i]);
			t.put(stem + "_r" + i, page[2 + 2 * i]);
		}
	}

	/** A page: its title and up to six lines, each a name and a value (with its unit). */
	private static String[] lines(String title, String... rows) {
		String[] out = new String[13];
		java.util.Arrays.fill(out, "");
		out[0] = title;
		for (int i = 0; i < Math.min(12, rows.length); i++) {
			out[1 + i] = rows[i];
		}
		return out;
	}

	private static String[] csfPage(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v, String title) {
		String[] rows = new String[12];
		for (int i = 0; i < 6; i++) {
			rows[2 * i] = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CSF_SHORT[i];
			rows[2 * i + 1] = v.csf[i] == 0 ? "----" : v.csf[i] == 2 ? "RED" : "SAT";
		}
		return lines(title, rows);
	}

	private static String[] logPage(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v, String title) {
		String[] rows = new String[12];
		java.util.Arrays.fill(rows, "");
		int n = v.events.size(), from = Math.max(0, n - 6);
		for (int i = 0; from + i < n; i++) {
			rows[2 * i] = v.times.get(from + i) + " " + v.events.get(from + i);
		}
		if (n == 0) {
			rows[0] = "NO ENTRIES";
		}
		return lines(title, rows);
	}

	private static String[] clockPage(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v) {
		return lines("CLOCK", "LOCAL", v.clock, "EVENT", v.event, "DECLARED", v.declared);
	}

	private static String bus(double pct) {
		return pct < 0 ? "----" : f0(pct) + " %";
	}

	/** A console position's (0 to 13) monitor m. */
	static String[] page(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v, int pos, int m) {
		String cls = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CLASS_SHORT[v.cls];
		String rx = !v.present ? "----" : v.tripped ? "TRIPPED" : v.r.status() == 2 ? "SHUTDOWN" : "AT POWER";
		String state = v.stateDue.isEmpty() ? "--:--" : v.stateDue, nrc = v.nrcDue.isEmpty() ? "--:--" : v.nrcDue;
		int steps = (int) Math.round(228 * (1 - v.rods / 100));
		String stp = v.present ? steps + " STP" : "----";
		return switch (pos * 2 + m) {
			// OPS COORDINATOR
			case 0 -> lines("PLANT STATUS", "REACTOR", rx, "POWER", String.format(Locale.ROOT, "%.1f %%", v.power), "TAVG", f0(v.tavg) + " F",
					"PZR PRESS", f0(v.pzrP) + " PSIG", "PZR LEVEL", f0(v.pzrL) + " %", "SG LEVEL", f0(v.sg) + " %");
			case 1 -> csfPage(v, "CSF STATUS");
			// OPS RECORDER
			case 2 -> logPage(v, "OPS LOG");
			case 3 -> lines("TRENDS", "TAVG", f0(v.tavg) + " F", "CORE EXIT", f0(v.cet) + " F", "RODS IN", f0(v.rods) + " %", "RCS FLOW",
					f0(v.flow) + " %", "PZR PRESS", f0(v.pzrP) + " PSIG", "SUR", String.format(Locale.ROOT, "%.2f DPM", v.r.sur()));
			// RAD COORDINATOR
			case 4 -> lines("RAD MONITORS", "INTAKE A", cpm(v.intakeA) + " CPM", "INTAKE B", cpm(v.intakeB) + " CPM", "TSC AREA",
					String.format(Locale.ROOT, "%.2f MR", v.areaMr), "PLANT VENT", v.vent, "CR HVAC", hvacMode(v), "RELEASE", v.release ? "YES" : "NONE");
			case 5 -> lines("HABITABILITY", "TSC AREA", String.format(Locale.ROOT, "%.2f MR", v.areaMr), "FRISKER BKG",
					f0(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.FRISK_BACKGROUND + v.areaMr * io.github.jimbozoomer.jugcraft.nuclear.TscRoom.FRISK_PER_MR) + " CPM",
					"CR HVAC", hvacMode(v), "INTAKE HIGH", Math.max(v.intakeA, v.intakeB) >= io.github.jimbozoomer.jugcraft.nuclear.HvacRoom.RAD_HIGH ? "YES" : "NO",
					"AREA LIMIT", "15 MR");
			// DOSE ASSESSMENT
			case 6 -> lines("MET DATA", "WIND FROM", f0(v.windFrom) + " DEG", "SPEED", String.format(Locale.ROOT, "%.1f MPH", v.windSpeed), "STABILITY",
					v.stability, "DOWNWIND", v.downwind, "SKY", v.sky);
			case 7 -> lines("RELEASE", "FUEL", v.fuel ? "DAMAGED" : "INTACT", "RELEASE", v.release ? "IN PROGRESS" : "NONE", "PLANT VENT", v.vent,
					"PAR", v.par[0]);
			// ENGINEERING COORD
			case 8 -> lines("CORE", "CORE EXIT", f0(v.cet) + " F", "TAVG", f0(v.tavg) + " F", "DECAY HEAT", String.format(Locale.ROOT, "%.1f %%", v.decay),
					"RODS IN", f0(v.rods) + " %", "SUBCOOLING", f0(v.subcooling) + " F", "FUEL", v.fuel ? "DAMAGED" : "INTACT");
			case 9 -> csfPage(v, "CSF STATUS");
			// REACTOR ENGINEER
			case 10 -> lines("NUCLEAR INST", "SOURCE RNG", !v.present ? "----" : v.r.p10() ? "OFF" : String.format(Locale.ROOT, "%.1E CPS", v.r.srCps()),
					"INTERM RNG", v.present ? String.format(Locale.ROOT, "%.1E A", v.r.irAmps()) : "----", "POWER RNG", String.format(Locale.ROOT, "%.1f %%", v.power),
					"SUR", String.format(Locale.ROOT, "%.2f DPM", v.r.sur()), "SHUTDOWN", v.present && v.r.status() != 1 ? "YES" : "NO");
			case 11 -> lines("ROD POSITION", "CONTROL A", stp, "CONTROL B", stp, "CONTROL C", stp, "CONTROL D", stp, "SHUTDOWN", stp);
			// ELECTRICAL ENGINEER
			case 12 -> lines("ELECTRICAL", "MAIN GEN", v.elec[0] + " MWE", "MAIN XFMR", v.elec[1], "LC 1A", v.elec[2], "LC 1B", v.elec[3], "EDG 1A",
					v.elec[4], "EDG 1B", v.elec[5]);
			case 13 -> lines("BUSES", "LC 1A BUS", bus(v.buses[0]), "LC 1B BUS", bus(v.buses[1]), "MCC 1A BUS", bus(v.buses[2]), "MCC 1B BUS", bus(v.buses[3]));
			// MECHANICAL ENGINEER
			case 14 -> lines("SECONDARY", "SG LEVEL", f0(v.sg) + " %", "RCS FLOW", f0(v.flow) + " %", "TURBINE",
					!v.present ? "----" : v.tripped ? "TRIPPED" : v.r.status() == 1 ? "ON LINE" : "OFF LINE", "MAIN GEN", v.elec[0] + " MWE", "HEAT SINK",
					v.csf[2] == 2 ? "RED" : v.csf[2] == 1 ? "SAT" : "----");
			case 15 -> lines("CORE COOLING", "CORE EXIT", f0(v.cet) + " F", "TAVG", f0(v.tavg) + " F", "SUBCOOLING", f0(v.subcooling) + " F", "HEAT SINK",
					v.csf[2] == 2 ? "RED" : v.csf[2] == 1 ? "SAT" : "----");
			// MAINTENANCE COORD
			case 16 -> lines("EQUIPMENT", "EDG 1A", v.elec[4], "EDG 1B", v.elec[5], "LC 1A BUS", bus(v.buses[0]), "LC 1B BUS", bus(v.buses[1]),
					"MCC 1A BUS", bus(v.buses[2]), "MCC 1B BUS", bus(v.buses[3]));
			case 17 -> lines("CR HVAC", "MODE", hvacMode(v), "CREFS A", !v.hvac ? "----" : v.hvacEmerg ? "RUNNING" : "STBY", "CREFS B",
					!v.hvac ? "----" : v.hvacEmerg ? "RUNNING" : "STBY", "INTAKE A", cpm(v.intakeA) + " CPM", "INTAKE B", cpm(v.intakeB) + " CPM");
			// MAINT PLANNER
			case 18 -> {
				String[] rows = new String[12];
				java.util.Arrays.fill(rows, "");
				for (int i = 0; i < 6 && i < v.priorities.size(); i++) {
					rows[2 * i] = v.priority(i);
				}
				yield lines("PRIORITIES", rows);
			}
			case 19 -> clockPage(v);
			// ADMIN COORDINATOR
			case 20 -> lines("NOTIFICATIONS", "CLASS", cls, "DECLARED", v.declared, "STATE/LOCAL BY", state, "NRC ENS BY", nrc);
			case 21 -> lines("TSC", "ACTIVATED", v.activated, "IN TSC NOW", String.valueOf(v.inTsc), "LOCAL", v.clock);
			// SECURITY COORD
			case 22 -> lines("SECURITY", "CLASS", cls, "IN TSC NOW", String.valueOf(v.inTsc), "ON SITE NOW", String.valueOf(v.onSite));
			case 23 -> clockPage(v);
			// SITE EMERGENCY MGR
			case 24 -> lines("EMERGENCY", "CLASS", cls, "DECLARED", v.declared, "EVENT TIME", v.event, "STATE/LOCAL BY", state, "NRC ENS BY", nrc,
					"PAR", v.par[0]);
			case 25 -> csfPage(v, "CSF SUMMARY");
			// TSC LOG KEEPER
			case 26 -> logPage(v, "TSC LOG");
			default -> clockPage(v);
		};
	}

	/** The communications console's screens: the ENS, HPN and offsite communicators'. */
	static String[] comms(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v, int k) {
		String cls = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CLASS_SHORT[v.cls];
		String state = v.stateDue.isEmpty() ? "--:--" : v.stateDue, nrc = v.nrcDue.isEmpty() ? "--:--" : v.nrcDue;
		return switch (k) {
			case 0 -> lines("ENS - NRC OPS CENTER", "CLASS", cls, "DECLARED", v.declared, "NOTIFY BY", nrc, "EVENT TIME", v.event);
			case 1 -> lines("HPN - NRC HEALTH PHYS", "LINE", v.cls >= 3 ? "OPEN" : "STANDBY", "RELEASE", v.release ? "IN PROGRESS" : "NONE", "INTAKE A",
					cpm(v.intakeA) + " CPM", "WIND FROM", f0(v.windFrom) + " DEG");
			default -> lines("OFFSITE NOTIFICATIONS", "CLASS", cls, "DECLARED", v.declared, "STATE/LOCAL BY", state, "PAR", v.par[0], "WIND FROM",
					f0(v.windFrom) + " DEG");
		};
	}

	/** The NRC desk's two screens. */
	static String[] nrc(io.github.jimbozoomer.jugcraft.nuclear.TscRoom.View v, int k) {
		String cls = io.github.jimbozoomer.jugcraft.nuclear.TscRoom.CLASS_SHORT[v.cls];
		if (k == 0) {
			return lines("NRC SITE TEAM", "CLASS", cls, "DECLARED", v.declared, "ENS CALL BY", v.nrcDue.isEmpty() ? "--:--" : v.nrcDue, "EVENT TIME", v.event,
					"PAR", v.par[0]);
		}
		return lines("PLANT STATUS", "REACTOR", !v.present ? "----" : v.tripped ? "TRIPPED" : v.r.status() == 2 ? "SHUTDOWN" : "AT POWER", "CORE EXIT",
				f0(v.cet) + " F", "CORE COOLING", v.csf[1] == 2 ? "RED" : v.csf[1] == 1 ? "SAT" : "----", "PZR PRESS", f0(v.pzrP) + " PSIG", "CR HVAC",
				hvacMode(v), "RELEASE", v.release ? "IN PROGRESS" : "NONE");
	}

	/** A count's scatter, about -1.7 to 1.7 (a sum of three uniform draws), the same for every player for a bucket. */
	static double scatter(long bucket, double seed) {
		long x = bucket * 0x9E3779B97F4A7C15L + (long) (seed * 1_000_003);
		double sum = 0;
		for (int i = 0; i < 3; i++) {
			x += 0x9E3779B97F4A7C15L;
			long z = x;
			z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
			z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
			z ^= z >>> 31;
			sum += (z >>> 11) * 0x1.0p-53;
		}
		return (sum - 1.5) * 2;
	}

	static double noise(double t, double seed, int channel) {
		double c = seed + channel * 1.913;
		return (Math.sin(t * 2.31 + c) + Math.sin(t * 5.17 + c * 2.7) * 0.6 + Math.sin(t * 11.3 + c * 4.1) * 0.3) / 1.9;
	}
}
