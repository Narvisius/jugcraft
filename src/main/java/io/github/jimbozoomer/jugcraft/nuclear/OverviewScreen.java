package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * The plant overview screens made live (round 34; Justin: "Overview screens live", "Live site map", "Dark display
 * style", "Alarm strip", "Emergency class", "Radiation readings", "Power trend", "Click a building for detail", "Jump to
 * the trouble"; tools/overview_screen.py lays the pages out). What a screen shows is worked out here from its block
 * entity (OverviewScreenBlockEntity: the plant as its control room's boards see it, their alarms, the TSC's emergency
 * class, the diesels' starting air, the circulating water and the towers): each building's state (its fill), each
 * line's flow (a signal), each bar's and trace's value, and every figure it writes. Both sides use it: the server for
 * the jumps to trouble, the client to draw.
 */
public final class OverviewScreen {
	/** The screen's sections in control_boards.json.gz: the frame, then each page as KEY/page. */
	public static final String KEY = "plant_map_screen";
	/** The pages (tools/overview_screen.py PAGES but the map): a page's index here is the block entity's page. */
	public static final String[] PAGES = {"reactor", "turbine", "electrical", "diesels", "control", "aux", "fuel", "cooling", "nosignal"};
	public static final int NO_SIGNAL = 8;
	/** How long a building's page stays up (ticks: 30 seconds), and how far the screen looks for its control room's boards. */
	public static final int PAGE_TICKS = 600, REACH = 256;
	/** A building's state (its fill's colour): no data, standby, running, in alarm, tripped or on fire. */
	public static final int NONE = 0, STANDBY = 1, RUNNING = 2, ALARM = 3, TRIP = 4;
	/**
	 * The buildings a jump to trouble may show, most important first: each its state's fill and its page. A building
	 * going into alarm or trip shows its page by itself (unless a page was clicked in the last half minute).
	 */
	static final String[][] TROUBLE = {{"ov.b.reactor", "reactor"}, {"ov.b.grid", "electrical"}, {"ov.b.edg_a", "diesels"}, {"ov.b.edg_b", "diesels"},
			{"ov.b.turbine", "turbine"}, {"ov.b.aux", "aux"}, {"ov.b.control", "control"}, {"ov.b.intake", "cooling"}, {"ov.b.cw", "cooling"}, {"ov.b.fuel", "fuel"},
			{"ov.b.rwst", "aux"}, {"ov.b.cst", "aux"}};
	/** The radiation monitors (BoardPlant.RAD_MONITORS, numbered from 1) in each building with a badge. */
	static final Map<String, int[]> RAD_BUILDINGS = Map.of("ov.rad.cntmt", new int[] {2, 3, 12}, "ov.rad.turb", new int[] {7}, "ov.rad.aux",
			new int[] {6, 11}, "ov.rad.fuel", new int[] {4, 5, 8}, "ov.rad.cr", new int[] {1, 9, 10});
	/** The pages' HVAC lamps (tools/overview_screen.py page_control) by their machines (HvacRoom.PARTS). */
	static final int[] HVAC_PARTS = {HvacRoom.EFU, HvacRoom.AHU, HvacRoom.CH, HvacRoom.PUMP};
	/** Colours (ARGB): figures' white, amber, red, green, labels' grey, and the ink on a lit banner. */
	public static final int WHITE = 0xFFECF0F6, AMBER = 0xFFFFC440, RED = 0xFFFF6050, GREEN = 0xFF6EE68C, GREY = 0xFFA8B0BC, INK = 0xFF0E1014;

	private OverviewScreen() {
	}

	// ------------------------------------------------------------------ the layout (both sides)

	/** Where a click goes: a page (or "map"), its name, its rectangle (the screen's board coordinates). */
	public record Hit(String page, String name, double x0, double y0, double x1, double y1) {
		public boolean contains(double x, double y) {
			return x >= x0 && x <= x1 && y >= y0 && y <= y1;
		}
	}

	public record Layout(List<String> penValues, Map<String, List<Hit>> hits) {
	}

	private static Layout layout;

	/** The screen's layout from control_boards.json.gz: the values it trends, and each section's hits. */
	public static Layout layout() {
		if (layout == null) {
			List<String> pens = new ArrayList<>();
			Map<String, List<Hit>> hits = new HashMap<>();
			try (InputStream in = OverviewScreen.class.getResourceAsStream("/assets/jugcraft/control_boards.json.gz")) {
				if (in != null) {
					JsonObject all = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)).getAsJsonObject();
					if (all.has(KEY)) {
						for (JsonElement e : all.getAsJsonObject(KEY).getAsJsonObject("live").getAsJsonArray("penValues")) {
							pens.add(e.getAsString());
						}
					}
					List<String> sections = new ArrayList<>(List.of("map"));
					sections.addAll(List.of(PAGES));
					for (String page : sections) {
						String key = KEY + "/" + page;
						List<Hit> list = new ArrayList<>();
						if (all.has(key)) {
							for (JsonElement e : all.getAsJsonObject(key).getAsJsonObject("live").getAsJsonArray("hits")) {
								JsonObject h = e.getAsJsonObject();
								list.add(new Hit(h.get("page").getAsString(), h.get("name").getAsString(), h.get("x0").getAsDouble(), h.get("y0").getAsDouble(),
										h.get("x1").getAsDouble(), h.get("y1").getAsDouble()));
							}
						}
						hits.put(page, List.copyOf(list));
					}
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the plant overview screen's layout", e);
			}
			layout = new Layout(List.copyOf(pens), Map.copyOf(hits));
		}
		return layout;
	}

	/** A page's index (PAGES), or -1 for the map. */
	public static int page(String name) {
		for (int i = 0; i < PAGES.length; i++) {
			if (PAGES[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/** The section a screen draws over its frame: its page's, or the map's. */
	public static String section(int page) {
		return KEY + "/" + (page < 0 ? "map" : PAGES[page]);
	}

	// ------------------------------------------------------------------ states (both sides)

	/** A radiation monitor's state (the badges' colours): 0 no monitor, 1 normal, 2 alert, 3 high. */
	public static int rad(BoardPlant p, int n) {
		if (!p.radFound(n)) {
			return 0;
		}
		double v = p.radReading(n);
		return v >= BoardPlant.RAD_HIGH[n - 1] ? 3 : v >= BoardPlant.RAD_ALERT[n - 1] ? 2 : 1;
	}

	/** The worst of several monitors (and which: the first at that state). */
	static int[] worstRad(BoardPlant p, int... monitors) {
		int worst = 0, which = 0;
		for (int n : monitors) {
			int r = rad(p, n);
			if (r > worst) {
				worst = r;
				which = n;
			}
		}
		return new int[] {worst, which};
	}

	static boolean csrFire(BoardPlant p) {
		return p.csrFound && (p.csrPhase == SpreadingRoomFire.ALARM || p.csrPhase == SpreadingRoomFire.RELEASED);
	}

	/** A diesel's state: none, standby (ready), running loaded, alarm (starting, running light or not ready), out of fuel. */
	static int edg(BoardPlant p, int k, double time) {
		BoardPlant.Edg e = p.edg[k];
		if (!e.present) {
			return NONE;
		}
		if (e.fuelOut) {
			return TRIP;
		}
		if (e.running) {
			return p.edgBreaker(k, time) ? RUNNING : ALARM;
		}
		return p.edgReady(k) ? STANDBY : ALARM;
	}

	/** A patch's state (tools/overview_screen.py "fills": the buildings, the chips, the badges, the banner, the core). */
	public static int fill(String id, OverviewScreenBlockEntity s, double time) {
		BoardPlant p = s.board.plant;
		boolean on = s.linked && p.present;
		if (!s.linked) {
			return 0;
		}
		if (id.startsWith("ov.rm")) {
			return rad(p, Integer.parseInt(id.substring(5)));
		}
		int[] monitors = RAD_BUILDINGS.get(id);
		if (monitors != null) {
			return worstRad(p, monitors)[0];
		}
		return switch (id) {
			case "ov.b.reactor" -> !on ? NONE : p.scrammed ? TRIP : p.fission * 100 >= 1 ? (p.alarms != 0 ? ALARM : RUNNING) : STANDBY;
			case "ov.b.turbine" -> !on ? NONE : p.turbineTripped ? TRIP : p.online() || p.latched() && p.turbineSpeed > 0.5 ? RUNNING : STANDBY;
			case "ov.b.grid" -> p.signal("st.offsite", time) ? RUNNING : TRIP;
			case "ov.b.xfmr" -> p.online() ? RUNNING : STANDBY;
			// Towers and pump houses build: the lake intake (make-up and service water) and the circ pump house apart; a plant
			// with no intake found keeps the old intake's state (its circulating water pumps).
			case "ov.b.intake" -> p.sw < 0 ? s.cwPumps > 0 ? RUNNING : on && p.hot() ? ALARM : STANDBY
					: (p.sw & 7) == 7 ? RUNNING : on && (p.sw & 3) != 3 ? ALARM : STANDBY;
			case "ov.b.cw" -> s.cwPumps > 0 ? RUNNING : on && p.hot() ? ALARM : STANDBY;
			case "ov.b.tower" -> s.towersRunning > 0 ? RUNNING : s.towers > 0 ? STANDBY : NONE;
			case "ov.b.aux" -> !on ? NONE : p.siOn() ? ALARM : p.signal("ccp_a.run", time) || p.signal("ccp_b.run", time) ? RUNNING : STANDBY;
			case "ov.b.fuel" -> {
				int r = worstRad(p, 4, 5, 8)[0];
				yield (p.alarms & ReactorCore.A_FUEL_DAMAGED) != 0 || r == 3 ? TRIP : r == 2 ? ALARM : STANDBY;
			}
			case "ov.b.control" -> csrFire(p) ? TRIP : p.hvEmergency() ? ALARM : p.hvFound ? RUNNING : STANDBY;
			case "ov.b.rwst" -> !on ? NONE : p.rwstLevel() < 10 ? TRIP : p.rwstLevel() < 30 ? ALARM : STANDBY;
			case "ov.b.cst" -> !on ? NONE : p.cstLevel() < 10 ? TRIP : p.cstLevel() < 30 ? ALARM : STANDBY;
			case "ov.b.edg_a" -> edg(p, 0, time);
			case "ov.b.edg_b" -> edg(p, 1, time);
			case "ov.st.edg" -> Math.max(edg(p, 0, time), edg(p, 1, time));
			case "ov.st.hvac" -> !p.hvFound ? NONE : p.hvEmergency() ? ALARM : RUNNING;
			case "ov.st.fire" -> !p.csrFound ? NONE : csrFire(p) || p.csrPhase == SpreadingRoomFire.FIRE_OUT ? TRIP
					: p.csrPhase == SpreadingRoomFire.DRILL || (p.csrFlags & (SpreadingRoomFire.NO_VALVE | SpreadingRoomFire.NO_DETECTORS
							| SpreadingRoomFire.NO_HEADS)) != 0 ? ALARM : RUNNING;
			case "ov.st.rad" -> worstRad(p, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)[0];
			case "ov.st.class" -> s.emClass == 0 ? RUNNING : s.emClass == 1 ? STANDBY : s.emClass == 2 ? ALARM : TRIP;
			case "ov.class" -> s.emClass;
			case "ov.core" -> !on || p.fuelLoaded == 0 ? 0 : (p.alarms & ReactorCore.A_FUEL_DAMAGED) != 0 ? 4 : p.fission * 100 >= 1 ? 2 : 1;
			case "ov.st.si" -> !on ? NONE : p.siOn() ? ALARM : STANDBY;
			case "ov.st.iso" -> !on ? NONE : p.cntIso ? ALARM : STANDBY;
			case "ov.st.fuel" -> !on ? NONE : (p.alarms & ReactorCore.A_FUEL_DAMAGED) != 0 ? TRIP : RUNNING;
			default -> 0;
		};
	}

	/** Whether a line flows or a lamp is lit (tools/overview_screen.py "flows" and "lamps"): the screen's own, else the plant's. */
	public static boolean signal(String name, OverviewScreenBlockEntity s, double time) {
		BoardPlant p = s.board.plant;
		if (!s.linked) {
			return false;
		}
		boolean on = p.present;
		if (name.startsWith("ov.seq_")) {
			return p.edgSteps(name.charAt(7) - 'a', time) >= name.charAt(8) - '0';
		}
		if (name.startsWith("ov.hv")) {
			return p.hvPart(HVAC_PARTS[name.charAt(5) - '0'], name.charAt(6) - 'a');
		}
		if (name.startsWith("ov.cwp")) {
			return s.cwPumps > name.charAt(6) - '0';
		}
		boolean drill = p.csrPhase == SpreadingRoomFire.DRILL;
		return switch (name) {
			case "ov.beat" -> ((long) time) % 20 < 6;
			case "ov.steam" -> on && DisplayWall.signal(p, "dw.steam_hdr", time);
			case "ov.feed" -> on && p.mainFeed();
			case "ov.afw" -> on && p.value("afw.total", time) > 1;
			case "ov.si", "ov.eccs", "ov.eccs_out" -> name.equals("ov.si") ? on && p.siOn()
					: on && (p.siOn() || p.signal("ccp_a.run", time) || p.signal("ccp_b.run", time));
			case "ov.gen" -> on && p.online();
			case "ov.offsite" -> p.signal("st.offsite", time);
			case "ov.cw" -> s.cwPumps > 0;
			case "ov.plume" -> s.towersRunning > 0;
			case "ov.turbine" -> on && p.latched() && p.turbineSpeed > 0.05;
			case "ov.condensate" -> on && (p.mainFeed() || p.hot());
			case "ov.rcs_flow" -> on && DisplayWall.signal(p, "dw.rcs_flow", time);
			case "ov.pzr_htr" -> on && p.value("pzr.htr_prop", time) > 1;
			case "ov.lc_bus" -> p.lc && p.lcCharge > 0.02;
			case "ov.csr_power", "ov.csr_supply" -> p.csrFound;
			case "ov.csr_alarm" -> p.csrFound && (drill || p.csrPhase != SpreadingRoomFire.NORMAL);
			case "ov.csr_rel", "ov.csr_open" -> p.csrFound && (drill && name.equals("ov.csr_rel") || p.csrPhase == SpreadingRoomFire.RELEASED
					|| p.csrPhase == SpreadingRoomFire.FIRE_OUT);
			case "ov.csr_water" -> p.csrFound && ((p.csrFlags & SpreadingRoomFire.FLOWING) != 0 || p.csrPhase == SpreadingRoomFire.RELEASED);
			case "ov.csr_sup" -> p.csrFound && (drill || (p.csrFlags & SpreadingRoomFire.NO_VALVE) != 0);
			case "ov.csr_trbl" -> p.csrFound && (drill || (p.csrFlags & (SpreadingRoomFire.NO_DETECTORS | SpreadingRoomFire.NO_HEADS)) != 0);
			case "ov.csr_air" -> p.csrFound && (drill || s.csrAir < SpreadingRoomFire.LOW_AIR);
			default -> p.signal(name, time);
		};
	}

	/** A bar's or a trace's value (tools/overview_screen.py "levels" and "pens"): the screen's own, else the plant's. */
	public static double value(String name, OverviewScreenBlockEntity s, double time) {
		BoardPlant p = s.board.plant;
		if (!s.linked) {
			return 0;
		}
		if (name.startsWith("ov.rod")) {
			// The rods hanging into the core, in as far as their banks are: the shutdown banks, then control banks A-D.
			int k = name.charAt(6) - '0';
			double steps = k == 0 ? p.sdBanks : p.bankSteps(3 + k);
			return p.present ? 1 - steps / ReactorCore.STEPS : 1;
		}
		return switch (name) {
			case "ov.core_loaded" -> p.coreSize <= 0 ? 0 : p.fuelLoaded / (double) p.coreSize;
			case "ov.cw_flow" -> Math.min(100, s.cwPumps * 25);
			case "edg_a.air", "edg_b.air" -> Math.max(0, s.edgAir[name.charAt(4) - 'a']);
			case "edg_a.day", "edg_b.day" -> Math.max(0, p.edg[name.charAt(4) - 'a'].day * 100);
			default -> p.value(name, time);
		};
	}

	// ------------------------------------------------------------------ the figures (the client)

	/** Words the screen writes, and their colour (0: the place's own). */
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

	/** A radiation monitor's reading in its units (as its meter: mR/h, R/h or counts a minute). */
	static Text radText(BoardPlant p, int n) {
		if (!p.radFound(n)) {
			return t("FAIL", GREY);
		}
		double v = p.radReading(n);
		String unit = switch (BoardPlant.RAD_UNIT[n - 1]) {
			case 0 -> " MR/H";
			case 1 -> " R/H";
			default -> " CPM";
		};
		String fig = v >= 100 ? f0(v) : v >= 10 ? f1(v) : v >= 0.1 ? String.format(Locale.ROOT, "%.2f", v) : String.format(Locale.ROOT, "%.3f", v);
		int r = rad(p, n);
		return t(fig + unit, r == 3 ? RED : r == 2 ? AMBER : WHITE);
	}

	private static String edgShort(BoardPlant p, int k, double time) {
		return switch (edg(p, k, time)) {
			case RUNNING -> "RUN";
			case ALARM -> p.edg[k].running ? "START" : "NOT RDY";
			case TRIP -> "NO FUEL";
			case STANDBY -> "STBY";
			default -> "--";
		};
	}

	private static String modeNumber(BoardPlant p) {
		String m = DisplayWall.mode(p);
		return m.startsWith("MODE ") ? m.substring(5) : m;
	}

	/**
	 * What the screen writes in each place (tools/overview_screen.py "texts"): time the game's (ticks), dayTime its
	 * world's time of day (the clocks), rain and thunder its weather (the wind).
	 */
	public static Text text(String id, OverviewScreenBlockEntity s, double time, long dayTime, boolean rain, boolean thunder) {
		BoardPlant p = s.board.plant;
		boolean on = s.linked && p.present;
		String dash = "--";
		if (id.equals("ov.clock")) {
			return t(TscRoom.clock(dayTime));
		}
		if (id.equals("ov.g")) {
			return t("G");
		}
		if (id.equals("ov.engine")) {
			return t("ENGINE");
		}
		if (id.equals("ov.page_left")) {
			long left = Math.max(0, (s.pageUntil - (long) time + 19) / 20);
			return t((s.pageAuto ? "SHOWN FOR AN ALARM  ·  " : "") + "BACK TO THE MAP IN " + left + " S");
		}
		if (!s.linked) {
			return switch (id) {
				case "ov.alarm_count" -> t("NO BOARDS IN REACH", GREY);
				case "ov.mode" -> t("NO SIGNAL", AMBER);
				default -> t(dash, GREY);
			};
		}
		if (id.startsWith("ov.rm")) {
			return radText(p, Integer.parseInt(id.substring(5)));
		}
		if (id.startsWith("ov.loop") && id.length() > 9) {
			int k = id.charAt(7) - '1';
			if (!on || k >= p.loops) {
				return t(dash, GREY);
			}
			return switch (id.substring(9)) {
				case "flow" -> {
					double v = p.value("loop" + (k + 1) + ".flow", time);
					yield t(f0(v) + " %", v < 90 ? AMBER : WHITE);
				}
				case "thot" -> t(f0(p.value("loop" + (k + 1) + ".thot", time)) + " °F", WHITE);
				default -> t(f0(p.value("loop" + (k + 1) + ".tcold", time)) + " °F", WHITE);
			};
		}
		if (id.startsWith("ov.rcp")) {
			int k = id.charAt(6) - '1';
			return !on || k >= p.loops ? t(dash, GREY) : p.rcpRunning(k) ? t("RUN", WHITE) : t("OFF", AMBER);
		}
		if (id.startsWith("ov.sg") && id.endsWith("_level")) {
			return dw("dw.sg" + id.charAt(5) + "_level", s, time, dayTime);
		}
		if (id.startsWith("ov.sg") && id.endsWith("_press")) {
			int k = id.charAt(5) - '1';
			return on && p.sg(k) ? t(f0(p.sgPress(k)) + " PSIG", WHITE) : t(dash, GREY);
		}
		if (id.startsWith("ov.edg_") && id.length() > 9) {
			return edgText(id.substring(9), id.charAt(7) - 'a', s, time, dayTime);
		}
		if (id.startsWith("ov.t.edg_")) {
			int k = id.charAt(9) - 'a';
			BoardPlant.Edg e = p.edg[k];
			return switch (edg(p, k, time)) {
				case RUNNING -> t("RUN " + f0(p.value(k == 0 ? "edg_a.kw" : "edg_b.kw", time)) + " KW", WHITE);
				case ALARM -> e.running ? t("STARTING", AMBER) : t("NOT READY", AMBER);
				case TRIP -> t("OUT OF FUEL", RED);
				case STANDBY -> t("STANDBY", WHITE);
				default -> t("NOT FOUND", GREY);
			};
		}
		return switch (id) {
			// The title bar and the alarm strip.
			case "ov.class" -> s.emClass <= 0 ? t("") : t(TscRoom.CLASSES[s.emClass] + "  ·  DECLARED " + TscRoom.clock(s.emDeclared),
					s.emClass == 4 ? WHITE : INK);
			case "ov.mode" -> !on ? t("NO REACTOR", GREY) : t("MODE " + modeNumber(p).split(" ")[0], GREY);
			case "ov.alarm_count" -> s.board.wallTotal == 0 ? t("NO ACTIVE ALARMS", GREEN)
					: t(s.board.wallTotal + " IN  ·  " + s.board.wallNew + " NEW", s.board.wallNew > 0 ? AMBER : WHITE);
			// The map's tags.
			case "ov.t.reactor" -> !on ? t("NO REACTOR", GREY) : p.scrammed ? t("TRIPPED", RED)
					: t(f0(p.prAverage()) + " %  MODE " + modeNumber(p).split(" ")[0], WHITE);
			case "ov.t.turbine" -> {
				double rpm = p.value("turb.speed", time);
				yield !on ? t(dash, GREY) : p.turbineTripped ? t("TRIPPED  " + f0(rpm) + " RPM", RED)
						: p.online() ? t(f0(rpm) + " RPM  " + f0(p.value("gen.mw", time)) + " MWE", WHITE) : rpm < 1 ? t("STOPPED", WHITE) : t(f0(rpm) + " RPM", WHITE);
			}
			case "ov.t.grid" -> !p.signal("st.offsite", time) ? t("NO VOLTAGE", RED)
					: t(f0(p.value("swyd.kv", time)) + " KV  " + (on && p.online() ? f0(mwOut(p, time)) + " MW OUT" : "NO OUTPUT"), WHITE);
			case "ov.t.intake" -> p.sw < 0 ? t("NOT FOUND", GREY)
					: t("MU " + ((p.sw & 4) != 0 ? "15,000" : "0") + "  SW " + Integer.bitCount(p.sw & 3) + "/2", (p.sw & 7) == 7 ? WHITE : AMBER);
			case "ov.t.cw" -> s.cwPumps > 0 ? t(s.cwPumps + (s.cwPumps == 1 ? " PUMP" : " PUMPS") + " RUNNING", WHITE)
					: t("NO PUMPS", on && p.hot() ? AMBER : GREY);
			case "ov.t.aux" -> !on ? t(dash, GREY) : p.siOn() ? t("SI ACTUATED", AMBER)
					: p.value("afw.total", time) > 1 ? t("AFW " + f0(p.value("afw.total", time)) + " GPM", WHITE) : t("SI READY  AFW OFF", WHITE);
			case "ov.t.fuel" -> {
				Text r = radText(p, 4);
				yield t("SFP " + r.text(), r.colour());
			}
			case "ov.t.control" -> csrFire(p) ? t("FIRE  CABLE SPREADING", RED) : p.hvEmergency() ? t("HVAC EMERGENCY", AMBER)
					: p.hvFound ? t("HVAC NORMAL", WHITE) : t("HVAC NOT FOUND", GREY);
			case "ov.t.rwst", "ov.rwst" -> on ? t(f0(p.rwstLevel()) + " %", p.rwstLevel() < 30 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.t.cst", "ov.cst" -> on ? t(f0(p.cstLevel()) + " %", p.cstLevel() < 30 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.t.tower" -> s.towersRunning > 0 ? t("IN SERVICE", WHITE) : s.towers > 0 ? t("NO FLOW", GREY) : t("NOT FOUND", GREY);
			// The status column.
			case "ov.power", "ov.power_big" -> on ? t(f1(p.prAverage()) + (id.equals("ov.power") ? " %" : ""), p.scrammed ? AMBER : WHITE) : t(dash, GREY);
			case "ov.gen_mw" -> on ? t(f0(p.value("gen.mw", time)) + " MWE", WHITE) : t(dash, GREY);
			case "ov.grid" -> p.signal("st.offsite", time) ? t(f0(p.value("swyd.kv", time)) + " KV", WHITE) : t("LOST", RED);
			case "ov.mode_name" -> !on ? t("NO REACTOR", GREY) : t(modeNumber(p), WHITE);
			case "ov.edgs" -> t("1A " + edgShort(p, 0, time) + "  1B " + edgShort(p, 1, time), WHITE);
			case "ov.hvac" -> !p.hvFound ? t("NOT FOUND", GREY) : p.hvEmergency() ? t("EMERGENCY", AMBER) : t("NORMAL", GREEN);
			case "ov.fire" -> !p.csrFound ? t("NOT FOUND", GREY) : switch (p.csrPhase) {
				case SpreadingRoomFire.ALARM -> t("FIRE ALARM", RED);
				case SpreadingRoomFire.RELEASED -> t("RELEASED", RED);
				case SpreadingRoomFire.FIRE_OUT -> t("FIRE OUT  RESET", AMBER);
				case SpreadingRoomFire.DRILL -> t("DRILL", AMBER);
				default -> (p.csrFlags & (SpreadingRoomFire.NO_VALVE | SpreadingRoomFire.NO_DETECTORS | SpreadingRoomFire.NO_HEADS)) != 0
						? t("TROUBLE", AMBER) : t("NORMAL", GREEN);
			};
			case "ov.rad" -> {
				int[] w = worstRad(p, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
				yield p.radFound == 0 ? t("NO MONITORS", GREY) : w[0] == 3 ? t("HIGH  RM-" + w[1], RED) : w[0] == 2 ? t("ALERT  RM-" + w[1], AMBER)
						: t("NORMAL", GREEN);
			}
			case "ov.class_row" -> s.emClass <= 0 ? t("NONE", GREEN) : t(TscRoom.CLASS_SHORT[s.emClass], s.emClass >= 3 ? RED : AMBER);
			case "ov.trend_power" -> on ? t(f1(p.prAverage())) : t(dash, GREY);
			case "ov.trend_mw" -> on ? t(f0(p.value("gen.mw", time))) : t(dash, GREY);
			// The reactor's page.
			case "ov.rx_state" -> dw("dw.rx_state", s, time, dayTime);
			case "ov.rx_cause" -> on && p.scrammed ? t(DisplayWall.tripCause(p), AMBER) : t("NONE", GREY);
			case "ov.rods" -> on ? t(p.rodAuto ? "AUTO" : "MANUAL", WHITE) : t(dash, GREY);
			case "ov.bank_d" -> on ? t(f0(p.bankSteps(7)) + " STEPS", WHITE) : t(dash, GREY);
			case "ov.sur" -> on ? t(String.format(Locale.ROOT, "%.2f DPM", p.sur), p.sur > 1 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.fuel_count" -> t(p.fuelLoaded + " OF " + p.coreSize, WHITE);
			case "ov.fuel" -> t(p.fuelLoaded + " / " + p.coreSize + " ASSEMBLIES", WHITE);
			case "ov.tavg" -> {
				Text d = dw("dw.tavg", s, time, dayTime);
				yield on ? t(d.text() + " °F", d.colour()) : d;
			}
			case "ov.pzr_press" -> dw("dw.pzr_p", s, time, dayTime);
			case "ov.pzr_level" -> dw("dw.pzr_l", s, time, dayTime);
			case "ov.subcool" -> on ? t(f0(DisplayWall.subcooling(p)) + " °F", DisplayWall.subcooling(p) < 20 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.cet" -> on ? t(f0(DisplayWall.coreExit(p)) + " °F", DisplayWall.coreExit(p) > 650 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.cnt_press" -> on ? t(f1(p.value("cnt.press", time)) + " PSIG", p.value("cnt.press", time) > 1.5 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.si" -> !on ? t(dash, GREY) : p.siOn() ? t("ACTUATED", AMBER) : t("READY", WHITE);
			case "ov.cnt_iso" -> !on ? t(dash, GREY) : p.cntIso ? t("ISOLATED", AMBER) : t("NORMAL", WHITE);
			case "ov.tr_tavg" -> on ? t(f0(p.tavg())) : t(dash, GREY);
			case "ov.tr_pzr" -> on ? t(f0(p.pzrPress())) : t(dash, GREY);
			// The turbine's page.
			case "ov.steam_press" -> dw("dw.steam_press", s, time, dayTime);
			case "ov.turb_speed" -> dw("dw.turb_speed", s, time, dayTime);
			case "ov.mvar" -> on ? t(f0(p.value("gen.mvar", time)) + " MVAR", WHITE) : t(dash, GREY);
			case "ov.gen_kv" -> on ? t(f1(p.value("gen.kv", time)) + " KV", WHITE) : t(dash, GREY);
			case "ov.gen_hz" -> dw("dw.gen_hz", s, time, dayTime);
			case "ov.vacuum" -> on ? t(f1(p.value("turb.vacuum", time)) + " IN HG", p.value("turb.vacuum", time) < 25 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.hotwell" -> on ? t(f0(p.value("turb.hotwell", time)) + " %", WHITE) : t(dash, GREY);
			case "ov.fw_press" -> on && p.mainFeed() ? t(f0(p.value("fw.hdr_press", time)) + " PSIG", WHITE) : t(on ? "0 PSIG" : dash, GREY);
			case "ov.turb_state" -> !on ? t(dash, GREY) : p.turbineTripped ? t("TRIPPED", RED) : p.online() ? t("ON LINE", GREEN)
					: p.turbineSpeed > 0.95 ? t("AT SPEED", WHITE) : p.turbineSpeed > 0.01 ? t("ROLLING", WHITE) : t("STOPPED", WHITE);
			case "ov.tr_rpm" -> on ? t(f0(p.value("turb.speed", time))) : t(dash, GREY);
			// The electrical page.
			case "ov.swyd_kv" -> p.signal("st.offsite", time) ? dw("dw.swyd_kv", s, time, dayTime) : t("0 KV", RED);
			case "ov.mw_out" -> on && p.online() ? t(f0(mwOut(p, time)) + " MW", WHITE) : t("0 MW", WHITE);
			case "ov.bus1a_kv", "ov.bus1b_kv", "ov.lc", "ov.mcc_a", "ov.mcc_b", "ov.dc_a", "ov.dc_b", "ov.vital" -> dw("dw." + id.substring(3), s, time, dayTime);
			case "ov.tr_kv" -> t(f0(p.signal("st.offsite", time) ? p.value("swyd.kv", time) : 0));
			// The control building's page.
			case "ov.dampers" -> !p.hvFound ? t(dash, GREY) : p.hvEmergency() ? t("EMERGENCY LINEUP", AMBER) : t("NORMAL", WHITE);
			case "ov.cr_temp" -> p.hvFound ? t(f0(p.value("cr.temp", time)) + " °F", p.value("cr.temp", time) > 80 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.cr_dp" -> p.hvFound ? t(String.format(Locale.ROOT, "+%.3f IN WG", p.value("cr.dp", time)), WHITE) : t(dash, GREY);
			case "ov.csr_line1", "ov.csr_line2" -> {
				if (!p.csrFound) {
					yield t(id.endsWith("1") ? "NO PANEL IN REACH" : "", GREY);
				}
				String[] lines = SpreadingRoomFire.display(p.csrPhase, p.csrFlags, s.csrSince, time, s.csrReleasedAt);
				yield t(lines[id.endsWith("1") ? 0 : 1]);
			}
			case "ov.csr_valve" -> !p.csrFound ? t(dash, GREY) : (p.csrFlags & SpreadingRoomFire.NO_VALVE) != 0 ? t("NOT FOUND", AMBER)
					: p.csrPhase == SpreadingRoomFire.RELEASED || p.csrPhase == SpreadingRoomFire.FIRE_OUT ? t("OPEN", RED) : t("CLOSED  SET", WHITE);
			case "ov.csr_air_psi" -> p.csrFound ? t(f0(s.csrAir) + " PSI", s.csrAir < SpreadingRoomFire.LOW_AIR ? AMBER : WHITE) : t(dash, GREY);
			case "ov.csr_heads" -> p.csrFound ? t(String.valueOf(s.csrHeads), s.csrHeads == 0 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.csr_detectors" -> p.csrFound ? t(String.valueOf(s.csrDetectors), s.csrDetectors == 0 ? AMBER : WHITE) : t(dash, GREY);
			case "ov.class_big" -> s.emClass <= 0 ? t("NO EMERGENCY", GREY) : t(TscRoom.CLASSES[s.emClass], s.emClass == 4 ? WHITE : INK);
			case "ov.declared" -> s.emClass <= 0 ? t(dash, GREY)
					: t(TscRoom.clock(s.emDeclared) + "  ·  " + Math.max(0, (dayTime - s.emDeclared) * 60 / TscRoom.HOUR) + " MIN AGO", WHITE);
			case "ov.tsc" -> s.emActivated >= 0 ? t("ACTIVATED " + TscRoom.clock(s.emActivated), WHITE) : t("NOT ACTIVATED", GREY);
			case "ov.notify_state", "ov.notify_nrc" -> {
				if (s.emClass <= 0) {
					yield t(dash, GREY);
				}
				long due = s.emDeclared + (id.equals("ov.notify_state") ? TscRoom.STATE_DUE : TscRoom.NRC_DUE);
				yield t((dayTime > due ? "PAST DUE " : "DUE ") + TscRoom.clock(due), dayTime > due ? RED : dayTime > due - TscRoom.STATE_DUE / 3 ? AMBER : WHITE);
			}
			case "ov.wind" -> {
				double[] w = TscRoom.wind(dayTime, rain, thunder);
				yield t("FROM " + TscRoom.sector(w[1]) + "  " + f0(w[0]) + " MPH", WHITE);
			}
			// The auxiliary building's page.
			case "ov.ccp_a", "ov.ccp_b", "ov.sip_a", "ov.sip_b", "ov.rhr_a", "ov.rhr_b", "ov.afw_a", "ov.afw_b", "ov.afw_td" -> {
				String pump = id.substring(3);
				if (!on) {
					yield t(dash, GREY);
				}
				if (!p.signal(pump + ".run", time)) {
					yield t("STBY", GREY);
				}
				yield t("RUN  " + f0(p.value(pump + ".flow", time)) + " GPM", WHITE);
			}
			case "ov.si_flow" -> on ? t(f0(p.value("sip.flow", time)) + " GPM", WHITE) : t(dash, GREY);
			case "ov.rhr_flow" -> on ? t(f0(p.value("rhr.flow", time)) + " GPM", WHITE) : t(dash, GREY);
			case "ov.chg_flow" -> on ? t(f0(p.value("ccp.flow", time)) + " GPM", WHITE) : t(dash, GREY);
			case "ov.ccw" -> on ? t(f0(p.value("ccw.flow", time)) + " GPM", WHITE) : t(dash, GREY);
			case "ov.afw_total" -> on ? t(f0(p.value("afw.total", time)) + " GPM", WHITE) : t(dash, GREY);
			case "ov.tr_si" -> on ? t(f0(p.value("sip.flow", time))) : t(dash, GREY);
			case "ov.tr_afw" -> on ? t(f0(p.value("afw.total", time))) : t(dash, GREY);
			// The fuel building's page.
			case "ov.burnup" -> on ? t(f1(p.burnup * 100) + " % OF DESIGN", WHITE) : t(dash, GREY);
			case "ov.head" -> !on ? t(dash, GREY) : p.refuel ? t("OFF  REFUELLING", AMBER) : t("ON", WHITE);
			case "ov.fuel_damage" -> !on ? t(dash, GREY) : (p.alarms & ReactorCore.A_FUEL_DAMAGED) != 0 ? t("DAMAGED", RED) : t("NONE", GREEN);
			case "ov.casks" -> t(String.valueOf(s.casks), WHITE);
			// The circulating water's page.
			case "ov.cw_pumps" -> t(s.cwPumps + " RUNNING", s.cwPumps == 0 && on && p.hot() ? AMBER : WHITE);
			case "ov.cw_flow" -> t(f0(Math.min(100, s.cwPumps * 25)) + " %", WHITE);
			case "ov.tower_load" -> s.towers > 0 ? t(f0(s.towerLoad * 100) + " %", WHITE) : t(dash, GREY);
			case "ov.tr_cw" -> t(f0(Math.min(100, s.cwPumps * 25)));
			case "ov.tr_vac" -> on ? t(f1(p.value("turb.vacuum", time))) : t(dash, GREY);
			default -> t("");
		};
	}

	/** The display wall's way of writing a figure (nuclear/DisplayWall.java text), its colour kept. */
	private static Text dw(String id, OverviewScreenBlockEntity s, double time, long dayTime) {
		DisplayWall.Text d = DisplayWall.text(id, s.board.plant, s.board, time, dayTime);
		return new Text(d.text(), d.colour());
	}

	/** The power the plant sends out: the generator's less the house load. */
	static double mwOut(BoardPlant p, double time) {
		return p.value("gen.mw", time) * 0.967;
	}

	/** A diesel's own figures on its page. */
	private static Text edgText(String what, int k, OverviewScreenBlockEntity s, double time, long dayTime) {
		BoardPlant p = s.board.plant;
		BoardPlant.Edg e = p.edg[k];
		String n = k == 0 ? "edg_a" : "edg_b";
		if (!e.present) {
			return t(what.equals("state") ? "NOT FOUND" : "--", GREY);
		}
		double speed = p.edgSpeed(k, time);
		return switch (what) {
			case "state" -> switch (edg(p, k, time)) {
				case RUNNING -> t("RUNNING  ·  LOADED", WHITE);
				case ALARM -> e.running ? t("STARTING", AMBER) : t("NOT READY", AMBER);
				case TRIP -> t("OUT OF FUEL", WHITE);
				default -> t("STANDBY", WHITE);
			};
			case "since" -> e.running ? t("RUNNING " + (long) Math.max(0, (time - e.since) * 60 / TscRoom.HOUR) + " MIN", GREY)
					: p.edgReady(k) ? t("READY  ·  AUTO START ARMED", GREY) : t(e.auto ? "NOT READY" : "CONTROL IN MANUAL", AMBER);
			case "rpm" -> t(f0(p.value(n + ".rpm", time)) + " RPM", WHITE);
			case "kw" -> t(f0(p.value(n + ".kw", time)) + " KW", WHITE);
			case "air" -> s.edgAir[k] < 0 ? t("--", GREY) : t(f0(s.edgAir[k]) + " PSI", s.edgAir[k] < StartingAir.MIN ? AMBER : WHITE);
			case "kv" -> t(String.format(Locale.ROOT, "%.2f KV", 4.16 * Math.min(1, speed)), WHITE);
			case "hz" -> t(f1(60 * Math.min(1, speed)) + " HZ", WHITE);
			case "mode" -> t(e.auto ? "AUTO" : "MANUAL", e.auto ? WHITE : AMBER);
			case "day" -> e.day < 0 ? t("--", GREY) : t(f0(e.day * 100) + " %", e.day < DieselFuel.LOW ? AMBER : WHITE);
			case "seq" -> {
				int steps = p.edgSteps(k, time);
				yield !p.edgBreaker(k, time) ? t(e.running ? "WAITING FOR BREAKER" : "READY", GREY)
						: steps >= 8 ? t("ALL 8 STEPS LOADED", GREEN) : t("STEP " + steps + " OF 8", AMBER);
			}
			default -> t("");
		};
	}

	/** What a click elsewhere on the screen says: the plant's mode and the alarms on its strip. */
	public static net.minecraft.network.chat.Component status(OverviewScreenBlockEntity s) {
		BoardPlant p = s.board.plant;
		if (!s.linked) {
			return net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.overview_nosignal", REACH);
		}
		String rx = !p.present ? "NO REACTOR" : p.scrammed ? "REACTOR TRIPPED, " + DisplayWall.tripCause(p) : DisplayWall.mode(p);
		return net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.overview_status", rx, s.board.wallTotal, s.board.wallNew);
	}

	/** The pages a jump to trouble could show now: the most important building gone into alarm or trip since last time. */
	static int trouble(OverviewScreenBlockEntity s, int[] before, double time) {
		int show = -1;
		for (int i = 0; i < TROUBLE.length; i++) {
			int now = fill(TROUBLE[i][0], s, time);
			if (now >= ALARM && before[i] < ALARM && before[i] >= 0 && show < 0) {
				show = page(TROUBLE[i][1]);
			}
			before[i] = now;
		}
		return show;
	}

	/** The section names (the frame's, the map's and the pages'). */
	public static List<String> sections() {
		List<String> out = new ArrayList<>(List.of(KEY, KEY + "/map"));
		for (String page : PAGES) {
			out.add(KEY + "/" + page);
		}
		return out;
	}
}
