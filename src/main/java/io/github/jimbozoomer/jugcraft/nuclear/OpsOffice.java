package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The operations office (round 26, #13; Justin: "Work control + tagout", live "Status screen + out-of-service board",
 * "Radio charging rack", "PPE and flashlights", "Shift schedule whiteboard", "Procedure shelves + printer"): the
 * control building's work control centre. Its equipment out-of-service board is the room's hub: once a second it reads
 * the plant's electrical equipment and lists everything in it that is switched off, as a real plant's work control
 * centre tracks equipment out of service and the technical specification actions (LCOs) it starts.
 *
 * <p>What it reads: the plant's 480 V load centres (each feeder breaker but the spares), its motor control centres (each
 * starter but the spare), the 120 VAC vital panels (their main breakers and branch circuits) and the battery rooms'
 * exhaust fans (on OFF). Each item out of service gets a clearance number the first time it is seen out, and keeps it
 * and the time it went out until it is back in service. The load centres' main breakers and their INVERTER and DC MCC
 * feeders, the MCCs' battery charger starters and the vital panels' main breakers start an LCO (amber on the board).
 * The board also reads the nearest reactor, for the plant's mode.
 *
 * <p>The consoles and the schedule board link to their nearest board in reach ({@link LivePanelBlockEntity#panel}) and
 * the client draws them from it (client/EdgTelemetry.java ops); the master clock and the chargers' lamps follow the
 * game's clock.
 */
public final class OpsOffice {
	public static final String[] KEYS = {"ops_oos_board", "ops_console", "ops_schedule_board", "ops_wall_clock", "ops_radio_rack", "ops_ppe_shelf"};
	public static final int HUB = 0, CONSOLE = 1, SCHEDULE = 2, CLOCK = 3, RADIO = 4, PPE = 5;
	/** Each console's two positions (its variant; tools/ops_office.py POSITIONS). */
	public static final String[][] POSITIONS = {{"WORK CONTROL SRO", "WORK CONTROL CLERK"}, {"TAGOUT COORDINATOR", "CLEARANCE WRITER"}};
	/** How far a console or the schedule board looks for its board; how far the board looks for the plant's equipment and panels. */
	public static final int REACH = 16, PLANT_REACH = 192, PANEL_REACH = 64;
	/** The board's rows; the first clearance number. */
	public static final int ROWS = 9, FIRST_CLEARANCE = 1041;
	/** The crews and their week (tools/ops_office.py ROTATION: D days, N nights, O off, T training), Monday first. */
	public static final String CREWS = "ABCDEF";
	public static final String[] ROTATION = {"DDOOTOO", "NNDDOOO", "OONNDDO", "OOOONNO", "TOOTOOD", "OTOOOTN"};
	public static final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
	/** The technical specification actions an item can start: none, distribution, inverters, DC sources. */
	public static final String[] LCOS = {"", "3.8.9 DISTRIBUTION", "3.8.7 INVERTERS", "3.8.4 DC SOURCES"};
	/** Each action's completion time (hours). */
	public static final int[] LCO_HOURS = {0, 8, 24, 2};
	/** The vital panels' and battery room fans' labels by variant (tools/plant_panels.py). */
	static final String[] VP_LABELS = {"1A-1", "1A-2", "1B-1", "1B-2"}, FAN_LABELS = {"1A", "1B"};

	private OpsOffice() {
	}

	/** Which kind a block is ({@link #HUB} to {@link #PPE}), or -1 for anything else. */
	public static int kind(BlockState state) {
		for (int k = 0; k < KEYS.length; k++) {
			if (state.getBlock() == PlantInterior.BLOCKS.get(KEYS[k])) {
				return k;
			}
		}
		return -1;
	}

	/** Once a second: the board reads the plant; the consoles and schedule board find their board. */
	public static void tick(LivePanelBlockEntity panel, ServerLevel level, int kind) {
		switch (kind) {
			case HUB -> hub(panel, level);
			case CONSOLE, SCHEDULE -> {
				if (panel.panel != null && panel.tsRescan-- > 0) {
					return;
				}
				panel.tsRescan = 9;
				List<LivePanelBlockEntity> hubs = panel.panelsAround(level, PlantInterior.BLOCKS.get(KEYS[HUB]), REACH);
				BlockPos found = hubs.isEmpty() ? null : hubs.get(0).getBlockPos().immutable();
				if (found == null ? panel.panel != null : !found.equals(panel.panel)) {
					panel.panel = found;
					panel.syncNow(level);
				}
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------------------------------------ what is out

	/**
	 * One item out of service: its id (stable while it is out), its clearance number, what it is, its train, how many
	 * tags hang on it, what it shows (OPEN, OFF), when it went out (game time) and the action it started (LCOS index).
	 */
	public record Entry(String id, int number, String equipment, String train, int tags, String status, long since, int lco) {
		String encode() {
			return String.join("|", id, Integer.toString(number), equipment, train, Integer.toString(tags), status, Long.toString(since),
					Integer.toString(lco));
		}

		public static @Nullable Entry decode(String s) {
			String[] p = s.split("\\|", -1);
			if (p.length != 8) {
				return null;
			}
			try {
				return new Entry(p[0], Integer.parseInt(p[1]), p[2], p[3], Integer.parseInt(p[4]), p[5], Long.parseLong(p[6]), Integer.parseInt(p[7]));
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	/** An item as found this second (its number and time come from the board's list). */
	private record Out(String id, String equipment, String train, int tags, String status, int lco) {
	}

	static final class Found {
		final List<BlockPos> lcs = new ArrayList<>(), mccs = new ArrayList<>(), panels = new ArrayList<>(), fans = new ArrayList<>();
	}

	private static Found find(LivePanelBlockEntity hub, ServerLevel level) {
		Found f = new Found();
		BlockPos here = hub.getBlockPos();
		double reach = (double) PLANT_REACH * PLANT_REACH;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getKey().distSqr(here) > reach) {
				continue;
			}
			switch (e.getValue().kind()) {
				case SWITCHGEAR_LINEUP -> f.lcs.add(e.getKey());
				case MCC_LINEUP -> f.mccs.add(e.getKey());
				default -> {
				}
			}
		}
		// The trains in a fixed order (west to east, then north to south), as the TSC orders them: 1A then 1B.
		Comparator<BlockPos> order = Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getY());
		f.lcs.sort(order);
		f.mccs.sort(order);
		for (String key : new String[] {"vital_panel", "exhaust_fan"}) {
			for (LivePanelBlockEntity p : hub.panelsAround(level, PlantInterior.BLOCKS.get(key), PANEL_REACH)) {
				(key.equals("vital_panel") ? f.panels : f.fans).add(p.getBlockPos().immutable());
			}
		}
		f.panels.sort(order);
		f.fans.sort(order);
		return f;
	}

	private static String train(int k) {
		return k == 0 ? "A" : k == 1 ? "B" : "-";
	}

	/** Everything in the plant switched off this second. */
	private static List<Out> survey(ServerLevel level, Found f) {
		List<Out> out = new ArrayList<>();
		for (int k = 0; k < f.lcs.size(); k++) {
			if (!level.isLoaded(f.lcs.get(k)) || !(level.getBlockEntity(f.lcs.get(k)) instanceof EquipmentBlockEntity lc)) {
				continue;
			}
			// (Older rooms round 1: each lineup by its own tag; a breaker racked out or tripped says so.)
			String tag = LoadCentre.tag(lc);
			String name = "480V LC " + tag;
			String tr = tag.endsWith("A") ? "A" : tag.endsWith("B") ? "B" : "-";
			for (int b = 0; b < Switchgear.BREAKERS; b++) {
				if ((Switchgear.DEFAULT_BREAKERS & 1 << b) == 0 || Switchgear.carrying(lc, b)) {
					continue;
				}
				String feeder = Switchgear.NAMES[b];
				int lco = b == 0 || feeder.equals("DC MCC") ? 1 : feeder.equals("INVERTER") ? 2 : 0;
				String status = lc.lc == null ? "OPEN" : lc.lc.target(b) ? "TRIPPED" : lc.lc.rack[b] == LoadCentre.TEST ? "TEST" : lc.lc.rack[b] == LoadCentre.DISCONNECT ? "RACKED" : "OPEN";
				out.add(new Out("lc" + k + ":" + b, name + (b == 0 ? "  MAIN BKR" : "  FDR " + LoadCentre.load(tag, b)), tr, 2, status, lco));
			}
		}
		for (int k = 0; k < f.mccs.size(); k++) {
			if (!level.isLoaded(f.mccs.get(k)) || !(level.getBlockEntity(f.mccs.get(k)) instanceof EquipmentBlockEntity mcc)) {
				continue;
			}
			String name = "MCC 1" + (char) ('A' + Math.min(k, 25));
			for (int b = 0; b < MotorControlCenter.BREAKERS; b++) {
				if ((MotorControlCenter.DEFAULT_BREAKERS & 1 << b) == 0 || (mcc.breakers & 1 << b) != 0) {
					continue;
				}
				String unit = MotorControlCenter.NAMES[b];
				int lco = b == 0 ? 1 : unit.startsWith("CHGR") ? 3 : 0;
				out.add(new Out("mcc" + k + ":" + b, name + "  " + unit, train(k), 1, "OFF", lco));
			}
		}
		for (BlockPos at : f.panels) {
			if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof LivePanelBlockEntity vp)) {
				continue;
			}
			int v = LivePanelBlockEntity.variant(vp.getBlockState());
			String label = VP_LABELS[Math.floorMod(v, VP_LABELS.length)];
			for (int b = 0; b <= 10; b++) {
				if ((VitalPanelBlock.DEFAULT_BREAKERS & 1 << b) == 0 || (vp.breakers & 1 << b) != 0) {
					continue;
				}
				out.add(new Out("vp" + at.asLong() + ":" + b, "VP " + label + (b == 0 ? "  MAIN BKR" : "  CKT " + b), label.substring(1, 2), 1,
						"OPEN", b == 0 ? 1 : 0));
			}
		}
		for (BlockPos at : f.fans) {
			if (!level.isLoaded(at) || !(level.getBlockEntity(at) instanceof LivePanelBlockEntity fan) || fan.mode != ExhaustFanBlock.OFF) {
				continue;
			}
			String label = FAN_LABELS[Math.floorMod(LivePanelBlockEntity.variant(fan.getBlockState()), FAN_LABELS.length)];
			out.add(new Out("fan" + at.asLong(), "EXH FAN  BATT RM " + label, label.substring(1, 2), 1, "OFF", 0));
		}
		return out;
	}

	/** The board: reads the reactor (for the mode) and the plant's equipment, and keeps its list. */
	static void hub(LivePanelBlockEntity hub, ServerLevel level) {
		hub.readReactor(level);
		if (hub.opsFound == null || hub.tsRescan-- <= 0) {
			hub.opsFound = find(hub, level);
			hub.tsRescan = 9;
		}
		Map<String, Entry> was = new HashMap<>();
		for (String s : hub.opsEntries) {
			Entry e = Entry.decode(s);
			if (e != null) {
				was.put(e.id(), e);
			}
		}
		long now = level.getGameTime();
		List<String> next = new ArrayList<>();
		for (Out o : survey(level, hub.opsFound)) {
			Entry old = was.get(o.id());
			Entry e = old != null ? new Entry(o.id(), old.number(), o.equipment(), o.train(), o.tags(), o.status(), old.since(), o.lco())
					: new Entry(o.id(), Math.max(FIRST_CLEARANCE, hub.opsNext++), o.equipment(), o.train(), o.tags(), o.status(), now, o.lco());
			next.add(e.encode());
		}
		if (hub.opsNext < FIRST_CLEARANCE) {
			hub.opsNext = FIRST_CLEARANCE;
		}
		// Oldest out first (lowest clearance number), as the board lists them.
		next.sort(Comparator.comparingInt(s -> {
			Entry e = Entry.decode(s);
			return e == null ? 0 : e.number();
		}));
		if (!next.equals(hub.opsEntries)) {
			hub.opsEntries.clear();
			hub.opsEntries.addAll(next);
			hub.syncNow(level);
		}
	}

	/** The board's list, decoded. */
	public static List<Entry> entries(LivePanelBlockEntity hub) {
		List<Entry> out = new ArrayList<>();
		for (String s : hub.opsEntries) {
			Entry e = Entry.decode(s);
			if (e != null) {
				out.add(e);
			}
		}
		return out;
	}

	// ------------------------------------------------------------------------------------------------ what it shows

	/** The plant's mode from the reactor as the board read it (Westinghouse technical specification modes). */
	public static String mode(RelayRoom.Reactor r) {
		if (r.status() == 0) {
			return "----";
		}
		if (r.status() == 1) {
			return r.fission() * 100 >= 5 ? "1  POWER OPERATION" : "2  STARTUP";
		}
		if (r.status() == 3) {
			return "3  HOT STANDBY  (TRIP)";
		}
		double f = r.temperature() * 9 / 5 + 32;
		return f >= 350 ? "3  HOT STANDBY" : f >= 200 ? "4  HOT SHUTDOWN" : "5  COLD SHUTDOWN";
	}

	/**
	 * The crew on shift (its index), whether it is the night shift, the day's number and its weekday, from the game's day
	 * clock: a shift day runs from 06:00 (day time 0), days to 18:00 and nights on to 06:00 the next morning.
	 */
	public static int[] shift(long dayTime) {
		long day = Math.floorDiv(dayTime, 24_000L);
		int weekday = (int) Math.floorMod(day, 7L);
		boolean night = Math.floorMod(dayTime, 24_000L) >= 12_000;
		char want = night ? 'N' : 'D';
		int crew = 0;
		for (int c = 0; c < ROTATION.length; c++) {
			if (ROTATION[c].charAt(weekday) == want) {
				crew = c;
				break;
			}
		}
		return new int[] {crew, night ? 1 : 0, (int) day + 1, weekday};
	}

	/** A crew's shift manager, senior reactor operator and two reactor operators (the plant's roster, by crew). */
	public static String[] crew(BlockPos hub, int crew) {
		String[] names = TscRoom.roster(hub, 4, 9_100 + crew);
		String[] out = new String[4];
		for (int i = 0; i < 4; i++) {
			String[] p = names[i].split(" ");
			out[i] = p[0].charAt(0) + ". " + p[p.length - 1];
		}
		return out;
	}

	/** Right-clicking a piece: what it shows, in a line. */
	public static String describe(LivePanelBlockEntity panel, ServerLevel level) {
		int kind = kind(panel.getBlockState());
		long day = level.getOverworldClockTime();
		if (kind == CLOCK) {
			return "Unit 1 master clock: " + TscRoom.clock(day);
		}
		if (kind == RADIO) {
			return "Operator radios on their chargers: green ready, red charging";
		}
		if (kind == PPE) {
			return "Operator PPE: hard hats, glasses, ear defenders; flashlights on their chargers (green ready)";
		}
		LivePanelBlockEntity hub = kind == HUB ? panel
				: panel.panel != null && level.getBlockEntity(panel.panel) instanceof LivePanelBlockEntity h && kind(h.getBlockState()) == HUB ? h : null;
		if (hub == null) {
			return "No equipment out-of-service board in reach: nothing to show";
		}
		List<Entry> list = entries(hub);
		int lco = 0, tags = 0;
		for (Entry e : list) {
			lco += e.lco() != 0 ? 1 : 0;
			tags += e.tags();
		}
		if (kind == SCHEDULE) {
			int[] s = shift(day);
			return String.format(Locale.ROOT, "Day %d: crew %c on %s; mode %s", s[2], CREWS.charAt(s[0]), s[1] == 1 ? "nights" : "days",
					mode(hub.reactor()).trim());
		}
		return String.format(Locale.ROOT, "Equipment out of service: %d (%d LCO actions, %d clearance tags hung); mode %s", list.size(), lco, tags,
				mode(hub.reactor()).trim());
	}
}
