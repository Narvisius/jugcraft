package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The main control room's operators' consoles and the shift manager's desk (older rooms round 5; Justin: "Control room
 * furniture"; tools/cr_room.py draws them). Each console is one position (its variant: REACTOR OPERATOR, BALANCE OF
 * PLANT, CONTROL ROOM SUPERVISOR) with three of the plant process computer's screens; the shift manager's desk has two.
 * A console links the control room's display wall in reach (else the nearest live board: {@link ControlBoard}) once a
 * second, and its screens read that board's plant snapshot and the wall's alarms (client/EdgTelemetry.java crConsole
 * writes them, as {@link #lines} works them out). The turbine building's control room's console (turbine hall part 3:
 * the TURBINE CONTROL position) links the nearest turbine-generator instead (TurbineControlRoom.lines). A click on a screen steps it to its position's next page; each
 * screen's page is kept in the panel's {@code mode} (four bits a screen, counted on from the screen's own first page).
 * The remote shutdown room's station (older rooms round 7; tools/rsr_room.py) is built the same way with two screens:
 * it links the remote shutdown panel in reach first, and its pages are AOP-8's checks against the plant and the hot
 * standby readings (then the RCS, steam generator and aux feed pages).
 */
public final class ControlRoomConsoles {
	public static final String CONSOLE = "cr_console", SS_DESK = "ss_desk", RSR_DESK = "rsr_desk";
	/** How far a console looks for its board (blocks): its own control room. */
	public static final int REACH = 32;
	public static final String[] POSITIONS = {"REACTOR OPERATOR", "BALANCE OF PLANT", "CONTROL ROOM SUPERVISOR", "TURBINE CONTROL"};
	/** Each position's pages (tools/cr_room.py PAGES), the shift manager's. */
	public static final String[][] PAGES = {{"REACTOR", "RCS", "NIS", "RODS"}, {"SECONDARY", "TURBINE GEN", "ELECTRICAL", "AUX FEED"},
			{"ALARMS", "CSF STATUS", "PLANT STATUS", "SAFETY INJ"},
			// (Turbine hall part 3: the turbine building's control room's console reads its turbine: TurbineControlRoom.)
			{"TURBINE GEN", "TSI", "EHC", "TURBINE OIL"}};
	public static final String[] SS_PAGES = {"PLANT STATUS", "ALARMS", "CSF STATUS"};
	/** The remote shutdown station's pages (tools/rsr_room.py PAGES). */
	public static final String[] RSR_PAGES = {"AOP-8 EVAC", "HOT STANDBY", "RCS", "SECONDARY", "AUX FEED"};
	/** The screens' middles across the piece, their half width, bottom, top and face (model pixels; tools/cr_room.py). */
	static final double[] CON_MON = {12.6, 24.0, 35.4}, SS_MON = {17.4, 30.6};
	static final double MON_HALF = 5.5, MON_Y0 = 14.2, MON_Y1 = 21.6;
	/** The lines a page has under its title. */
	public static final int LINES = 7;
	static final String[] CSF_SHORT = {"SUBCRIT", "CORE COOL", "HEAT SINK", "INTEGRITY", "CONTAINMT", "INVENTORY"};
	static final String[] CSF_WORD = {"SATISFIED", "YELLOW", "ORANGE", "RED"};
	public static final int YELLOW = 0xFFEED636, ORANGE = 0xFFFFA03C;

	private ControlRoomConsoles() {
	}

	public static boolean isConsole(BlockState state) {
		return DcRoom.is(state, CONSOLE) || DcRoom.is(state, SS_DESK) || DcRoom.is(state, RSR_DESK);
	}

	/** A console at the TURBINE CONTROL position (turbine hall part 3): it reads the nearest turbine-generator. */
	public static boolean isTurbineConsole(BlockState state) {
		return DcRoom.is(state, CONSOLE) && position(state) == TurbineControlRoom.CONSOLE_POSITION;
	}

	public static boolean isRsrDesk(BlockState state) {
		return DcRoom.is(state, RSR_DESK);
	}

	public static boolean isSsDesk(BlockState state) {
		return DcRoom.is(state, SS_DESK);
	}

	/** A console's position (0 to 3), from its variant. */
	public static int position(BlockState state) {
		return Math.floorMod(LivePanelBlockEntity.variant(state), POSITIONS.length);
	}

	/** The pages a console's screens step through. */
	public static String[] pages(BlockState state) {
		return isSsDesk(state) ? SS_PAGES : isRsrDesk(state) ? RSR_PAGES : PAGES[position(state)];
	}

	public static int screens(BlockState state) {
		return isSsDesk(state) || isRsrDesk(state) ? SS_MON.length : CON_MON.length;
	}

	/** The screen at a point of the piece's front (x across it, y up, model pixels), or -1 (two: the desks' two screens). */
	public static int screenAt(boolean two, double x, double y) {
		double[] mids = two ? SS_MON : CON_MON;
		if (y < MON_Y0 - 0.3 || y > MON_Y1 + 0.3) {
			return -1;
		}
		for (int m = 0; m < mids.length; m++) {
			if (Math.abs(x - mids[m]) <= MON_HALF + 0.2) {
				return m;
			}
		}
		return -1;
	}

	/** The page screen m shows: its own first page (the m-th), stepped on by the clicks kept in mode. */
	public static int page(LivePanelBlockEntity panel, int m, int count) {
		return (m + (panel.mode >> 4 * m & 15)) % count;
	}

	/** Steps screen m to its next page. */
	public static void step(LivePanelBlockEntity panel, int m, int count) {
		int k = ((panel.mode >> 4 * m & 15) + 1) % count;
		panel.mode = panel.mode & ~(15 << 4 * m) | k << 4 * m;
	}

	/**
	 * Once a second: links the control room's display wall in reach, else the nearest live board, else nothing (the
	 * remote shutdown station: the remote shutdown panel in reach, before the wall).
	 */
	static void tick(LivePanelBlockEntity panel, ServerLevel level) {
		if (isTurbineConsole(panel.getBlockState())) {
			TurbineControlRoom.tick(panel, level);
			return;
		}
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		BlockPos here = panel.getBlockPos(), best = null, board = null;
		double bestD = (double) REACH * REACH, boardD = bestD;
		EquipmentKind first = isRsrDesk(panel.getBlockState()) ? EquipmentKind.REMOTE_SHUTDOWN_PANEL : EquipmentKind.PLANT_DISPLAY_WALL;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> a : registry.anchors().entrySet()) {
			double d = a.getKey().distSqr(here);
			EquipmentKind kind = a.getValue().kind();
			if (kind == first && d <= bestD) {
				best = a.getKey().immutable();
				bestD = d;
			} else if (kind.liveBoard() && d <= boardD) {
				board = a.getKey().immutable();
				boardD = d;
			}
		}
		BlockPos found = best != null ? best : board;
		if (found == null ? panel.panel != null : !found.equals(panel.panel)) {
			panel.panel = found;
			panel.changedAndSync(level);
		}
	}

	/** A screen's line: its name on the left, its value on the right in its colour (ARGB; 0 the screen's white). */
	public record Line(String label, String value, int colour) {
	}

	static Line l(String label, String value) {
		return new Line(label, value, 0);
	}

	static Line l(String label, DisplayWall.Text t) {
		return new Line(label, t.text(), t.colour());
	}

	static Line l(String label, String value, int colour) {
		return new Line(label, value, colour);
	}

	private static String f0(double v) {
		return String.format(Locale.ROOT, "%.0f", v);
	}

	private static String f1(double v) {
		return String.format(Locale.ROOT, "%.1f", v);
	}

	/** A screen with no board to read. */
	public static Line[] noData() {
		Line[] out = blank();
		out[0] = l("PLANT COMPUTER", "NO LINK", DisplayWall.AMBER);
		out[2] = l("NO CONTROL BOARD", "");
		out[3] = l("WITHIN " + REACH + " BLOCKS", "");
		return out;
	}

	static Line[] blank() {
		Line[] out = new Line[LINES];
		java.util.Arrays.fill(out, l("", ""));
		return out;
	}

	/**
	 * A page's lines from a board's state (its plant snapshot), at game time {@code time} (ticks) and the world's time of
	 * day. The alarms page's rows are the wall's banner (client/EdgTelemetry.java: their words are the client's).
	 */
	public static Line[] lines(String page, BoardPlant p, ControlBoard.State s, double time, long dayTime) {
		boolean on = p.present;
		String dash = "----";
		Line[] out = blank();
		java.util.function.Function<String, DisplayWall.Text> w = id -> DisplayWall.text(id, p, s, time, dayTime);
		switch (page) {
			case "REACTOR" -> {
				out[0] = l("REACTOR", w.apply("dw.rx_state"));
				out[1] = l("POWER (NIS)", on ? f1(p.prAverage()) + " %" : dash);
				DisplayWall.Text tavg = w.apply("dw.tavg");
				out[2] = l("TAVG / TREF", on ? tavg.text() + " F" : dash, tavg.colour());
				out[3] = l("CORE EXIT", on ? f0(DisplayWall.coreExit(p)) + " F" : dash, w.apply("dw.cet").colour());
				out[4] = l("SUBCOOLING", on ? f0(DisplayWall.subcooling(p)) + " F" : dash, w.apply("dw.subcool").colour());
				out[5] = l("ROD CONTROL", on ? p.rodAuto ? "AUTO" : "MANUAL" : dash);
				out[6] = l("BANK D", on ? f0(p.bankSteps(7)) + " STEPS" : dash);
			}
			case "RCS" -> {
				out[0] = l("PZR PRESSURE", w.apply("dw.pzr_p"));
				out[1] = l("PZR LEVEL", w.apply("dw.pzr_l"));
				for (int k = 0; k < 4; k++) {
					out[2 + k] = l("LOOP " + (k + 1) + " FLOW", w.apply("dw.loop" + (k + 1) + "_flow"));
				}
				out[6] = l("THOT / TCOLD", on ? f0(p.value("loop1.thot", time)) + " / " + f0(p.value("loop1.tcold", time)) + " F" : dash);
			}
			case "NIS" -> {
				out[0] = l("SOURCE RANGE", !on ? dash : p.srBlocked ? "HV OFF" : String.format(Locale.ROOT, "%.1E CPS", p.srCps()));
				out[1] = l("INTERMEDIATE", on ? String.format(Locale.ROOT, "%.1E A", Math.pow(10, p.value("ir1.log", time))) : dash);
				for (int k = 0; k < 4; k++) {
					out[2 + k] = l("N4" + (k + 1), on ? f1(p.pr(k)) + " %" : dash);
				}
				out[6] = l("STARTUP RATE", on ? String.format(Locale.ROOT, "%.2f DPM", p.sur) : dash, Math.abs(p.sur) > 1 ? DisplayWall.AMBER : 0);
			}
			case "RODS" -> {
				out[0] = l("SHUTDOWN BANKS", on ? f0(p.sdBanks) + " STEPS" : dash);
				for (int k = 0; k < 4; k++) {
					out[1 + k] = l("CONTROL " + "ABCD".charAt(k), on ? f0(p.bankSteps(4 + k)) + " STEPS" : dash);
				}
				out[5] = l("ROD SPEED", on ? f0(p.rodSpeed()) + " SPM" : dash);
				out[6] = l("ROD CONTROL", on ? p.rodAuto ? "AUTO" : "MANUAL" : dash);
			}
			case "SECONDARY" -> {
				for (int k = 0; k < 4; k++) {
					out[k] = l("SG " + (k + 1) + " LEVEL NR", w.apply("dw.sg" + (k + 1) + "_level"));
				}
				out[4] = l("STEAM HEADER", w.apply("dw.steam_press"));
				out[5] = l("MAIN FEED", !on ? dash : p.mainFeed() ? "IN SERVICE" : "ISOLATED", p.mainFeed() ? 0 : DisplayWall.AMBER);
				out[6] = l("MSIVS", !on ? dash : p.msivAllShut() ? "ALL SHUT" : p.msivClosed > 0 ? Integer.bitCount(p.msivClosed) + " SHUT" : "OPEN",
						p.msivClosed > 0 ? DisplayWall.AMBER : 0);
			}
			case "TURBINE GEN" -> {
				out[0] = l("TURBINE", !on ? dash : p.turbineTripped ? "TRIPPED" : p.latched() ? "LATCHED" : "UNLATCHED",
						p.turbineTripped ? DisplayWall.RED : 0);
				out[1] = l("SPEED", w.apply("dw.turb_speed"));
				out[2] = l("GENERATOR", w.apply("dw.gen_mw"));
				out[3] = l("FREQUENCY", w.apply("dw.gen_hz"));
				out[4] = l("GEN BREAKER", !on ? dash : p.genOpen ? "OPEN" : "CLOSED");
				out[5] = l("VACUUM", w.apply("dw.vacuum"));
				out[6] = l("SWITCHYARD", w.apply("dw.swyd_kv"));
			}
			case "ELECTRICAL" -> {
				out[0] = l("4160 V BUS 1A", w.apply("dw.bus1a_kv"));
				out[1] = l("4160 V BUS 1B", w.apply("dw.bus1b_kv"));
				out[2] = l("EDG 1A", w.apply("dw.edg_a_kw"));
				out[3] = l("EDG 1B", w.apply("dw.edg_b_kw"));
				out[4] = l("480 V LC", w.apply("dw.lc"));
				out[5] = l("125 VDC A", w.apply("dw.dc_a"));
				out[6] = l("125 VDC B", w.apply("dw.dc_b"));
			}
			case "AUX FEED" -> {
				out[0] = l("AFW PUMP A", w.apply("dw.afw_a"));
				out[1] = l("AFW PUMP B", w.apply("dw.afw_b"));
				out[2] = l("TD AFW PUMP", w.apply("dw.afw_td"));
				out[3] = l("AFW TOTAL", w.apply("dw.afw_total").text() + " GPM");
				out[4] = l("CST LEVEL", w.apply("dw.cst"));
				out[5] = l("SG LO-LO", !on ? dash : p.anyLoLo() ? "YES" : "NO", p.anyLoLo() ? DisplayWall.RED : 0);
				out[6] = l("SAFETY INJ", w.apply("dw.si_state"));
			}
			case "CSF STATUS" -> {
				int worst = 0, which = -1;
				for (int k = 0; k < 6; k++) {
					int c = DisplayWall.csf(p, k, time);
					out[k] = l(DisplayWall.CSF_LETTERS[k] + " " + CSF_SHORT[k], on ? CSF_WORD[c] : dash, on ? csfColour(c) : 0);
					if (c > worst) {
						worst = c;
						which = k;
					}
				}
				out[6] = l("GO TO", !on ? dash : worst == 0 ? "NONE" : DisplayWall.PROCEDURES[which][worst], csfColour(worst));
			}
			case "PLANT STATUS" -> {
				out[0] = l("MODE", DisplayWall.mode(p).replace("MODE ", ""), DisplayWall.CYAN);
				out[1] = l("REACTOR", w.apply("dw.rx_state"));
				out[2] = l("POWER", on ? f1(p.prAverage()) + " %" : dash);
				out[3] = l("GENERATOR", w.apply("dw.gen_mw"));
				out[4] = l("SAFETY INJ", w.apply("dw.si_state"));
				out[5] = l("ALARMS", s.wallTotal + " ACTIVE", s.wallNew > 0 ? DisplayWall.AMBER : 0);
				out[6] = l("TIME", TscRoom.clock(dayTime), DisplayWall.CYAN);
			}
			case "SAFETY INJ" -> {
				out[0] = l("SAFETY INJ", w.apply("dw.si_state"));
				out[1] = l("SI TIME", on && p.siOn() ? TscRoom.span((long) (time - p.siTime)) : "--");
				out[2] = l("CNT PRESSURE", w.apply("dw.cnt_press").text() + " PSIG", w.apply("dw.cnt_press").colour());
				out[3] = l("RWST LEVEL", w.apply("dw.rwst").text() + " %", w.apply("dw.rwst").colour());
				out[4] = l("PZR PRESSURE", w.apply("dw.pzr_p"));
				out[5] = l("SUBCOOLING", on ? f0(DisplayWall.subcooling(p)) + " F" : dash, w.apply("dw.subcool").colour());
				out[6] = l("FEED ISOL", !on ? dash : p.feedIso ? "ISOLATED" : "NORMAL", p.feedIso ? DisplayWall.AMBER : 0);
			}
			case "AOP-8 EVAC" -> {
				// Control room evacuation (tools/rsr_room.py AOP8_STEPS): each check against the plant, DONE (or its
				// reading in band) green, still open amber.
				double afw = 0;
				for (int k = 0; k < 3; k++) {
					afw += p.afwFlow(k);
				}
				double lo = 1, hi = 0;
				for (int k = 0; k < 4; k++) {
					if (p.sg(k)) {
						lo = Math.min(lo, p.nr(k));
						hi = Math.max(hi, p.nr(k));
					}
				}
				boolean sgs = hi >= lo;
				out[0] = step("1 REACTOR TRIPPED", on, p.scrammed);
				out[1] = step("2 TURBINE TRIPPED", on, p.turbineTripped || !p.latched());
				out[2] = step("3 CONTROL AT RSP", on, p.remote);
				out[3] = l("4 AFW FLOW", !on ? dash : afw > 1 ? f0(afw) + " GPM" : "NO FLOW", !on ? 0 : afw > 1 ? DisplayWall.GREEN : DisplayWall.AMBER);
				out[4] = l("5 SG LEVELS NR", !on || !sgs ? dash : f0(lo * 100) + "-" + f0(hi * 100) + " %",
						!on || !sgs ? 0 : lo >= 0.25 && hi <= 0.65 ? DisplayWall.GREEN : DisplayWall.AMBER);
				out[5] = l("6 TAVG 547 F", on ? f0(p.tavg()) + " F" : dash, !on ? 0 : Math.abs(p.tavg() - 547) <= 10 ? DisplayWall.GREEN : DisplayWall.AMBER);
				out[6] = l("7 PZR LVL > 17 %", on ? f0(p.pzrLevel()) + " %" : dash, !on ? 0 : p.pzrLevel() > 17 ? DisplayWall.GREEN : DisplayWall.AMBER);
			}
			case "HOT STANDBY" -> {
				out[0] = l("MODE", DisplayWall.mode(p).replace("MODE ", ""), DisplayWall.CYAN);
				out[1] = l("CONTROL", !on ? dash : p.remote ? "RSP (LOCAL)" : "CONTROL ROOM", p.remote ? DisplayWall.AMBER : 0);
				out[2] = l("TAVG", on ? f0(p.tavg()) + " F" : dash);
				out[3] = l("PZR PRESSURE", w.apply("dw.pzr_p"));
				out[4] = l("PZR LEVEL", w.apply("dw.pzr_l"));
				out[5] = l("STEAM HEADER", w.apply("dw.steam_press"));
				out[6] = l("SOURCE RANGE", !on ? dash : p.srBlocked ? "HV OFF" : String.format(Locale.ROOT, "%.1E CPS", p.srCps()));
			}
			default -> {
			}
		}
		return out;
	}

	/** An AOP-8 step's line: DONE green once the plant shows it, else OPEN amber. */
	static Line step(String label, boolean on, boolean done) {
		return !on ? l(label, "----") : done ? l(label, "DONE", DisplayWall.GREEN) : l(label, "OPEN", DisplayWall.AMBER);
	}

	/** How many of a page's lines are done (green): the AOP-8 page's count in its title bar. */
	public static int done(Line[] lines) {
		int n = 0;
		for (Line l : lines) {
			if (l.colour() == DisplayWall.GREEN) {
				n++;
			}
		}
		return n;
	}

	static int csfColour(int c) {
		return switch (c) {
			case 0 -> DisplayWall.GREEN;
			case 1 -> YELLOW;
			case 2 -> ORANGE;
			default -> DisplayWall.RED;
		};
	}

	/** What a click on a console (off its screens) says: its position, its link, its screens' pages. */
	public static String describe(LivePanelBlockEntity panel, @Nullable BlockPos source, ServerLevel level) {
		BlockState state = panel.getBlockState();
		String who = isSsDesk(state) ? "Shift manager's desk" : isRsrDesk(state) ? "Remote shutdown station" : POSITIONS[position(state)];
		String link = source == null ? isTurbineConsole(state) ? "no turbine-generator in reach" : "no control board in reach" : level.getBlockEntity(source) instanceof EquipmentBlockEntity e
				? e.kind() == EquipmentKind.PLANT_DISPLAY_WALL ? "reading the display wall"
				: e.kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL ? "reading the remote shutdown panel" : "reading the " + e.kind().id.replace('_', ' ') : "no link";
		StringBuilder pages = new StringBuilder();
		String[] all = pages(state);
		for (int m = 0; m < screens(state); m++) {
			pages.append(m == 0 ? "" : ", ").append(all[page(panel, m, all.length)]);
		}
		return who + ": " + link + " (" + pages + ")";
	}
}
