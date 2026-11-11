package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The turbine building's control room (turbine hall part 3; Justin: "Turbine control room (Recommended)";
 * tools/tcr_room.py draws it): the turbine control system's lineup (its power distribution cabinet, the TMR
 * controllers R, S and T with their networks, two I/O cabinets), the turbine supervisory instruments' rack, the
 * generator's excitation cabinet and its two protection panels, and a console at the TURBINE CONTROL position.
 *
 * <p>Each cabinet joins the nearest main turbine-generator within {@link TurbineHall#FEED_REACH} blocks once a second
 * (LivePanelBlockEntity.panel) and shows it as the real ones would (client/EdgTelemetry.java tcr writes {@link #fill}'s
 * readings): the controllers ONLINE, R IN CONTROL and S and T TRACKING, their networks' link lights flickering; each
 * bearing's shaft vibration (mils, growing with speed squared and peaking through the rotor's critical speed near
 * 1,150 rpm), the thrust position with load, the eccentricity, the speed; the field breaker 41 closing at 95% speed, the
 * field current and volts, the generator's kV and MVAR on load; the 86G lockout tripped (its TRIP and 32 reverse power
 * targets lit) after the turbine trips with the plant hot, reset once it latches again. The cabinets keep their own
 * power (the station batteries and the UPS) with no turbine near: their controllers then say so.
 */
public final class TurbineControlRoom {
	public static final String POWER = "tb_tcs_power", CONTROL = "tb_tcs_control", IO = "tb_tcs_io", TSI = "tb_tsi_rack",
			EXCITATION = "tb_excitation", PROTECTION = "tb_gen_protection";
	/** The cabinets in order (their kinds: {@link #kind}). */
	public static final String[] PANELS = {POWER, CONTROL, IO, TSI, EXCITATION, PROTECTION};
	/** The console's position (ControlRoomConsoles.POSITIONS) that reads a turbine instead of a board. */
	public static final int CONSOLE_POSITION = 3;
	/** Each bearing's vibration at rated speed (mils peak to peak; tools/tcr_room.py's mockup reads the same). */
	static final double[] BEARING = {2.2, 2.8, 3.1, 2.6, 2.4, 3.5, 2.9, 2.1};
	/** The rotor train's first critical speed (rpm), how wide its peak is and how high (mils). */
	static final double CRITICAL = 1150, CRITICAL_WIDTH = 110, CRITICAL_PEAK = 2.6;
	/** The TSI's bars' full scale: vibration and eccentricity 10 mils, the thrust position 20. */
	static final double VIB_SCALE = 10, THRUST_SCALE = 20;
	/** The TSI rack's cards (tools/tcr_room.py TSI_CARDS). */
	static final String[] TSI_CARDS = {"PS1", "PS2", "RIM", "KPH", "1-4", "5-8", "THR", "DE", "ECC", "RLY"};
	/** The field breaker closes at this fraction of rated speed (the generator then has volts). */
	static final double FIELD_FLASH = 0.95;
	/** The generator's rating at full load (MWe; the boards' too: BoardPlant). */
	static final double RATED_MWE = 1150;

	private TurbineControlRoom() {
	}

	/** A turbine control room cabinet's kind (its place in {@link #PANELS}), or -1. */
	public static int kind(BlockState state) {
		for (int i = 0; i < PANELS.length; i++) {
			if (DcRoom.is(state, PANELS[i])) {
				return i;
			}
		}
		return -1;
	}

	/** Once a second: a cabinet (or the TURBINE CONTROL console) joins the nearest turbine-generator in reach. */
	static void tick(LivePanelBlockEntity panel, ServerLevel level) {
		BlockPos found = TurbineHall.nearest(EquipmentRegistry.of(level), Vec3.atCenterOf(panel.getBlockPos()), EquipmentKind.TURBINE_GENERATOR,
				TurbineHall.FEED_REACH);
		if (found == null ? panel.panel != null : !found.equals(panel.panel)) {
			panel.panel = found;
			panel.changedAndSync(level);
		}
	}

	/** The turbine a cabinet or console is joined to, if it is still there. */
	public static @Nullable EquipmentBlockEntity turbine(LivePanelBlockEntity panel, net.minecraft.world.level.Level level) {
		return panel.panel != null && level.getBlockEntity(panel.panel) instanceof EquipmentBlockEntity e && e.kind() == EquipmentKind.TURBINE_GENERATOR
				? e : null;
	}

	/** What the turbine shows at a moment: its flags, speed and load, and what follows from them. */
	public record Turbine(boolean present, boolean latched, boolean online, boolean hot, boolean turningGear, double rpm, double load) {
		public static Turbine of(@Nullable EquipmentBlockEntity e) {
			if (e == null) {
				return new Turbine(false, false, false, false, false, 0, 0);
			}
			int f = e.turbineFlags;
			boolean present = (f & TurbineHall.PRESENT) != 0;
			return new Turbine(present, present && (f & TurbineHall.LATCHED) != 0, present && (f & TurbineHall.ONLINE) != 0,
					present && (f & TurbineHall.HOT) != 0, present && (f & TurbineHall.TURNING_GEAR) != 0, present ? e.turbineRpm : 0,
					present ? e.turbineLoad : 0);
		}

		/** The field breaker closed: latched and over 95% speed (BoardPlant.field). */
		public boolean excited() {
			return latched && rpm > 1800 * FIELD_FLASH;
		}

		/** The rotor standing hot with its turning gear out: it bows (BoardPlant.rotorBowing; the board's ECCENTRICITY HI). */
		public boolean bowing() {
			return present && !latched && !turningGear && hot && speed() < ReactorCore.ZERO_SPEED;
		}

		/** The 86G lockout tripped: the turbine tripped with the plant hot (it resets when the turbine latches again). */
		public boolean lockout() {
			return present && !latched && hot;
		}

		public double speed() {
			return rpm / 1800;
		}

		/** Bearing k's shaft vibration (mils). */
		public double vibration(int k, double time) {
			if (!present || rpm < 1) {
				return 0;
			}
			double s = speed(), c = (rpm - CRITICAL) / CRITICAL_WIDTH;
			double v = BEARING[k] * s * s + CRITICAL_PEAK * Math.exp(-c * c) * (0.8 + 0.05 * k) + 0.25;
			return v * (1 + 0.03 * Math.sin(time * 0.37 + k * 1.7));
		}

		/** The thrust collar's position (mils toward the generator): pushed further with load (BoardPlant "tsi.thrust"). */
		public double thrust() {
			return present ? 6 + 4 * load : 0;
		}

		/** The LP rotor's differential expansion (inches long; "tsi.diff_exp"). */
		public double diffExpansion() {
			return present ? 0.1 + 0.25 * load : 0;
		}

		/** The shaft's eccentricity (mils; "tsi.eccen"): high while it bows, a little turning slowly, least at speed. */
		public double eccentricity() {
			return !present ? 0 : bowing() ? 5.8 : speed() < 0.05 ? 1.8 : 1.1;
		}

		public double mwe() {
			return online ? load * RATED_MWE : 0;
		}

		/** The generator's field current (kA; "gen.field_amps") and volts ("gen.exc_volts"), its kV, MVAR and frequency. */
		public double fieldKa() {
			return excited() ? (1500 + 2400 * load) / 1000 : 0;
		}

		public double fieldV() {
			return excited() ? 150 + 260 * load : 0;
		}

		public double genKv() {
			return excited() ? 22 * Math.min(1, speed()) : 0;
		}

		public double mvar() {
			return online ? 50 + 0.14 * mwe() : 0;
		}

		public double hz() {
			return online ? 60 : 60 * speed();
		}

		/** The flags the plant's gauges (TurbineHall.value) read for this turbine. */
		public int flags() {
			return (present ? TurbineHall.PRESENT : 0) | (latched ? TurbineHall.LATCHED : 0) | (online ? TurbineHall.ONLINE : 0)
					| (hot ? TurbineHall.HOT : 0) | (turningGear ? TurbineHall.TURNING_GEAR : 0);
		}
	}

	/** A steady flicker for activity lights: lit or not in each bucket of game time, by seed. */
	static boolean flicker(double seed, double time, double bucketTicks, double duty) {
		long bucket = (long) Math.floor(time / bucketTicks);
		long h = Double.doubleToLongBits(seed) * 0x9E3779B97F4A7C15L + bucket * 0xC2B2AE3D27D4EB4FL;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return (h >>> 11) * 0x1.0p-53 < duty;
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%,.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}

	/**
	 * A cabinet's readings by channel (tools/tcr_room.py's live parts): meters and bars in their units, lamps (0 dark, 1
	 * lit, 2 the second colour) and texts, for its kind and variant at game time {@code time}.
	 */
	public static void fill(Map<String, Double> numbers, Map<String, Integer> lit, Map<String, String> text, int kind, int variant,
			@Nullable EquipmentBlockEntity e, double time) {
		Turbine t = Turbine.of(e);
		boolean linked = e != null;
		switch (kind) {
			case 0 -> {
				// The power distribution cabinet: both DC feeds and the UPS (the station's own), its two power supplies.
				numbers.put("tcp_dc_a", 132 + 0.3 * Math.sin(time * 0.05));
				numbers.put("tcp_dc_b", 131 + 0.3 * Math.sin(time * 0.043 + 1));
				for (String ch : new String[] {"tcp_a", "tcp_b", "tcp_ups", "tcp_ps0", "tcp_ps1"}) {
					lit.put(ch, 1);
				}
				lit.put("tcp_gnd", 0);
			}
			case 1 -> {
				// The TMR controllers: R in control, S and T tracking it; with no turbine their I/O has nothing to read.
				String[] names = {"r", "s", "t"};
				for (int i = 0; i < 3; i++) {
					String n = names[i];
					text.put("tcc_" + n + "0", n.toUpperCase(Locale.ROOT) + "  ONLINE");
					text.put("tcc_" + n + "1", !linked ? "IO NO LINK" : i == 0 ? "IN CONTROL" : "TRACKING");
					lit.put("tcc_" + n + "_l0", 1);
					lit.put("tcc_" + n + "_l1", i == 0 ? 1 : 0);
					lit.put("tcc_" + n + "_l2", !linked && ((long) (time / 10)) % 2 == 0 ? 1 : 0);
					lit.put("tcc_" + n + "_l3", 0);
				}
				// The network switches' link lights: the ports in use flicker with traffic.
				for (int r = 0; r < 2; r++) {
					for (int k = 0; k < 8; k++) {
						boolean used = (k * 3 + r) % 4 != 0;
						lit.put("tcn_" + r + "_" + k, used && flicker(r * 17 + k, time + k, 3, linked ? 0.75 : 0.95) ? 1 : 0);
					}
				}
			}
			case 2 -> {
				// An I/O cabinet's packs: power, attention (none), network (talking to the controllers).
				for (int c = 0; c < 2; c++) {
					for (int r = 0; r < 5; r++) {
						String s = "tio_" + c + r + "_";
						lit.put(s + "0", 1);
						lit.put(s + "1", 0);
						lit.put(s + "2", linked && flicker(variant * 50 + c * 10 + r, time, 2 + r % 3, 0.6) ? 1 : 0);
					}
				}
			}
			case 3 -> {
				// The TSI rack: its screen's bars (of their full scale), the speed; each card's OK, and its ALERT.
				text.put("tsi_rpm", f0(t.rpm()) + " RPM");
				boolean alert14 = false, alert58 = false;
				for (int k = 0; k < 8; k++) {
					double v = t.vibration(k, time);
					numbers.put("tsi_b" + (k + 1), v / VIB_SCALE);
					if (v >= VIB_SCALE * 0.5) {
						if (k < 4) {
							alert14 = true;
						} else {
							alert58 = true;
						}
					}
				}
				numbers.put("tsi_thrust", t.thrust() / THRUST_SCALE);
				numbers.put("tsi_ecc", t.eccentricity() / VIB_SCALE);
				for (int i = 0; i < TSI_CARDS.length; i++) {
					String s = TSI + "_led" + i + "_";
					boolean alert = switch (TSI_CARDS[i]) {
						case "1-4" -> alert14;
						case "5-8" -> alert58;
						case "ECC" -> t.bowing();
						case "RLY" -> alert14 || alert58 || t.bowing();
						default -> false;
					};
					boolean ok = switch (TSI_CARDS[i]) {
						case "PS1", "PS2", "RIM", "RLY" -> true;
						case "KPH" -> t.rpm() >= 1;
						default -> t.present();
					};
					lit.put(s + "0", 1);
					lit.put(s + "1", ok ? 1 : 0);
					lit.put(s + "2", alert && ((long) (time / 10)) % 2 == 0 ? 1 : 0);
				}
			}
			case 4 -> {
				// The excitation cabinet: its screen, lamps, field meters and the field breaker's flag.
				boolean ex = t.excited();
				text.put("exc_t", "EXCITATION    " + (!linked ? "NO TURBINE" : ex ? "CH A IN CONTROL" : "FIELD BKR OPEN"));
				String[][] rows = {{"GEN VOLTS", ex ? f1(t.genKv()) + " KV" : "0.0 KV"}, {"GEN MVAR", t.online() ? "+" + f0(t.mvar()) : "0"},
						{"FIELD CURRENT", f0(t.fieldKa() * 1000) + " A"}, {"FIELD VOLTAGE", f0(t.fieldV()) + " V"},
						{"PSS  UEL  OEL", (t.online() ? "ON" : "OFF") + "  OK  OK"}};
				for (int i = 0; i < rows.length; i++) {
					text.put("exc_l" + i, rows[i][0]);
					text.put("exc_r" + i, rows[i][1]);
				}
				lit.put("exc_cha", ex ? 1 : 0);
				lit.put("exc_chb", 1);
				lit.put("exc_pss", t.online() ? 1 : 0);
				lit.put("exc_41", ex ? 1 : 0);
				lit.put("exc_alarm", 0);
				numbers.put("exc_fa", t.fieldKa());
				numbers.put("exc_fv", t.fieldV());
				lit.put("exc_41_flag", ex ? 1 : 2);
			}
			case 5 -> {
				// A generator protection panel: its relays' screens and targets, the 86G lockout, its lamps.
				boolean trip = t.lockout();
				text.put("gp_r00", trip ? "TRIP  32 REV PWR" : "GEN 87G 40 32 21");
				text.put("gp_r01", t.online() ? "52G CLOSED" : "52G OPEN");
				text.put("gp_r10", "XFMR 87T 87U");
				text.put("gp_r11", "NORMAL");
				for (int r = 0; r < 2; r++) {
					for (int k = 0; k < 8; k++) {
						lit.put("gp_t" + r + k, r == 0 && trip && (k == 0 || k == 3) ? 1 : 0);
					}
				}
				lit.put("gp_lor_target", trip ? 1 : 0);
				numbers.put("gp_lor", trip ? 1.0 : 0.0);
				lit.put("gp_dc", 1);
				lit.put("gp_trip_ckt", t.online() ? 1 : 0);
			}
			default -> {
			}
		}
	}

	/** A TURBINE CONTROL console screen with no turbine to read. */
	public static ControlRoomConsoles.Line[] noTurbine() {
		ControlRoomConsoles.Line[] out = ControlRoomConsoles.blank();
		out[0] = ControlRoomConsoles.l("TURBINE CONTROL", "NO LINK", DisplayWall.AMBER);
		out[2] = ControlRoomConsoles.l("NO TURBINE-GENERATOR", "");
		out[3] = ControlRoomConsoles.l("WITHIN " + TurbineHall.FEED_REACH + " BLOCKS", "");
		return out;
	}

	/** A TURBINE CONTROL console page's lines (tools/cr_room.py PAGES' fourth row) from its turbine. */
	public static ControlRoomConsoles.Line[] lines(String page, EquipmentBlockEntity e, double time) {
		Turbine t = Turbine.of(e);
		ControlRoomConsoles.Line[] out = ControlRoomConsoles.blank();
		String dash = "----";
		boolean on = t.present();
		int f = t.flags();
		java.util.function.BiFunction<String, String, ControlRoomConsoles.Line> l = ControlRoomConsoles::l;
		switch (page) {
			case "TURBINE GEN" -> {
				out[0] = ControlRoomConsoles.l("TURBINE", !on ? dash : t.latched() ? "LATCHED" : t.turningGear() ? "ON TURNING GEAR" : "TRIPPED",
						on && !t.latched() && !t.turningGear() ? DisplayWall.RED : 0);
				out[1] = l.apply("SPEED", on ? f0(t.rpm()) + " RPM" : dash);
				out[2] = l.apply("GENERATOR", on ? f0(t.mwe()) + " MWE" : dash);
				out[3] = l.apply("FREQUENCY", on ? String.format(Locale.ROOT, "%.2f HZ", t.hz()) : dash);
				out[4] = l.apply("GEN BREAKER", !on ? dash : t.online() ? "CLOSED" : "OPEN");
				out[5] = l.apply("VACUUM", on ? f1(TurbineHall.value("VACUUM", f, t.rpm(), t.load())) + " IN HG" : dash);
				out[6] = l.apply("FIELD BREAKER", !on ? dash : t.excited() ? "41 CLOSED" : "41 OPEN");
			}
			case "TSI" -> {
				for (int k = 0; k < 4; k++) {
					double a = t.vibration(2 * k, time), b = t.vibration(2 * k + 1, time);
					boolean high = Math.max(a, b) >= VIB_SCALE * 0.5;
					out[k] = ControlRoomConsoles.l("BRG " + (2 * k + 1) + " / " + (2 * k + 2), on ? f1(a) + " / " + f1(b) + " MILS" : dash,
							high ? DisplayWall.AMBER : 0);
				}
				out[4] = l.apply("THRUST", on ? String.format(Locale.ROOT, "%+.0f MILS", t.thrust()) : dash);
				out[5] = l.apply("DIFF EXP LP", on ? String.format(Locale.ROOT, "%+.2f IN", t.diffExpansion()) : dash);
				out[6] = ControlRoomConsoles.l("ECCENTRICITY", on ? f1(t.eccentricity()) + " MILS" : dash, t.bowing() ? DisplayWall.AMBER : 0);
			}
			case "EHC" -> {
				out[0] = l.apply("CONTROL", !on ? dash : t.online() ? "AUTO  LOAD" : t.latched() ? "AUTO  SPEED" : "TRIPPED");
				out[1] = l.apply("SPEED REF", !on ? dash : t.latched() ? "1800 RPM" : "0 RPM");
				out[2] = l.apply("LOAD REF", !on ? dash : f0(t.mwe()) + " MWE");
				// The governor valves open in sequence with load (GV 1 to 3 first, GV 4 last); at speed with no load a little.
				double g = !t.latched() ? 0 : t.online() ? Math.min(1.2, t.load()) : 0.08;
				int gv = (int) Math.round(Math.min(100, g * 78)), gv4 = (int) Math.round(Math.max(0, Math.min(100, (g - 0.5) * 82)));
				out[3] = l.apply("GOV VALVES", !on ? dash : gv + " " + gv + " " + Math.max(0, gv - 2) + " " + gv4 + " %");
				out[4] = l.apply("INTERCEPT VLVS", !on ? dash : t.latched() ? "100 %" : "0 %");
				out[5] = l.apply("EHC PRESS", on ? f0(TurbineHall.value("EHC PRESS", f, t.rpm(), t.load())) + " PSIG" : dash);
				out[6] = l.apply("EHC RESERVOIR", on ? f0(TurbineHall.level("EHC RESERVOIR", f) * 100) + " %" : dash);
			}
			case "TURBINE OIL" -> {
				out[0] = l.apply("BRG OIL HEADER", on ? f0(TurbineHall.value("BRG OIL HDR", f, t.rpm(), t.load())) + " PSIG" : dash);
				out[1] = l.apply("BRG OIL TEMP", on ? f0(TurbineHall.value("OIL TEMP", f, t.rpm(), t.load())) + " F" : dash);
				out[2] = l.apply("LO RESERVOIR", on ? f0(TurbineHall.level("LO RESERVOIR", f) * 100) + " %" : dash);
				out[3] = l.apply("OIL PUMPS", !on ? dash : TurbineHall.lamp("tg_oil", f, t.rpm()) ? "TG OIL PUMP"
						: TurbineHall.lamp("aux_oil", f, t.rpm()) ? "AUX OIL PUMP" : "SHAFT MOP");
				out[4] = l.apply("LIFT OIL", !on ? dash : TurbineHall.lamp("lift_oil", f, t.rpm()) ? "RUNNING" : "OFF");
				out[5] = l.apply("SEAL OIL DP", on ? f0(TurbineHall.value("SEAL DP", f, t.rpm(), t.load())) + " PSID" : dash);
				out[6] = l.apply("H2 PURITY", on ? f1(TurbineHall.value("H2 PURITY", f, t.rpm(), t.load())) + " %" : dash);
			}
			default -> {
			}
		}
		return out;
	}
}
