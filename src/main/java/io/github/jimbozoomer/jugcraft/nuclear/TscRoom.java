package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Technical Support Center (round 26, #12; Justin: "Emergency class board", "Plant data video wall", "Desk screens
 * and clocks", "New call-centre consoles", "Status boards", "Communications corner", "Records and procedures", "Frisker
 * at the door"). Its emergency classification board is the room's hub: once a second it reads the plant the way the
 * room's displays need it, works out the emergency class, and keeps the event's clocks and log.
 *
 * <p>What it reads: the nearest reactor (as a relay room cabinet does); the control room HVAC panel (in emergency or
 * not) and its intake radiation monitors' count rates; the dose rate at the TSC's area radiation monitor; the plant's
 * emergency diesel generators (running or not), its 480 V load centres' and motor control centres' buses (their
 * charge) and the main transformer (its output). Everything the TSC's screens, boards and clocks show comes from these,
 * from the game's clock and weather, and from the players about.
 *
 * <p>The class (a much simplified set of emergency action levels): an UNUSUAL EVENT on a reactor trip or the control
 * room HVAC in emergency; an ALERT on the core at its high temperature alarm, HIGH radiation at a control room intake or
 * the TSC's area monitor at its alert; a SITE AREA EMERGENCY on damaged fuel; a GENERAL EMERGENCY on damaged fuel with
 * the core at its damage temperature or a high dose rate in the TSC. The class only goes up while the event lasts (as a
 * real one is only ever escalated); when every condition has been clear for {@link #TERMINATE} ticks the emergency is
 * terminated. The TSC is activated at an ALERT or higher. The times are the game's clock (day time: an hour a 1,000
 * ticks); the notifications are due 15 minutes (state and local) and an hour (the NRC) after the declaration.
 *
 * <p>The other TSC panels link to their nearest board in reach ({@link LivePanelBlockEntity#panel}) and the client draws
 * them from it (client/EdgTelemetry.java tsc). The area radiation monitor and the frisker read the dose rate where
 * they are.
 */
public final class TscRoom {
	public static final String[] KEYS = {"tsc_class_board", "tsc_video_wall", "tsc_console", "tsc_comms_console", "tsc_nrc_desk",
			"tsc_status_board", "tsc_area_monitor", "tsc_frisker"};
	public static final int HUB = 0, VIDEO = 1, CONSOLE = 2, COMMS_DESK = 3, NRC = 4, BOARD = 5, AREA = 6, FRISKER = 7;
	public static final String[] CLASSES = {"NO EMERGENCY", "UNUSUAL EVENT", "ALERT", "SITE AREA EMERGENCY", "GENERAL EMERGENCY"};
	public static final String[] CLASS_SHORT = {"NO EMERGENCY", "UNUSUAL EVENT", "ALERT", "SITE AREA EMERG", "GENERAL EMERG"};
	/** The event log's entries. */
	public static final String[] EVENTS = {"REACTOR TRIP", "HIGH CORE TEMP", "FUEL DAMAGE", "CR HVAC EMERG", "CR HVAC NORMAL", "NOUE DECLARED",
			"ALERT DECLARED", "SAE DECLARED", "GE DECLARED", "TSC ACTIVATED", "EDG 1A START", "EDG 1B START", "EDG 1A STOP", "EDG 1B STOP",
			"HIGH RAD INTAKE", "EMERG TERMINATED", "REACTOR RESET"};
	public static final int E_TRIP = 0, E_HOT = 1, E_FUEL = 2, E_HVAC = 3, E_HVAC_NORMAL = 4, E_DECLARED = 5, E_TSC = 9, E_EDG_START = 10,
			E_EDG_STOP = 12, E_HIGH_RAD = 14, E_TERMINATED = 15, E_RESET = 16;
	public static final int LOG = 8;
	/** What the board found ({@link LivePanelBlockEntity#tsBits}). */
	public static final int B_HVAC = 1, B_HVAC_EMERG = 2, B_EDG_A = 4, B_EDG_A_RUN = 8, B_EDG_B = 16, B_EDG_B_RUN = 32, B_LC_A = 64, B_LC_B = 128,
			B_MCC_A = 256, B_MCC_B = 512, B_XFMR = 1024, B_XFMR_LIVE = 2048, B_INTAKE_A = 4096, B_INTAKE_B = 8192;
	/** What it read ({@link LivePanelBlockEntity#tsNums}): the intakes' count rates (cpm), the dose rate in the TSC (mSv/h), the buses' charge (0 to 1). */
	public static final int N_INTAKE_A = 0, N_INTAKE_B = 1, N_AREA = 2, N_LC_A = 3, N_LC_B = 4, N_MCC_A = 5, N_MCC_B = 6, NUMS = 7;
	/** The conditions as last seen, for the log (saved): tripped, hot, fuel damaged, HVAC emergency, each EDG running, a high intake. */
	static final int F_TRIP = 1, F_HOT = 2, F_FUEL = 4, F_HVAC = 8, F_EDG_A = 16, F_EDG_B = 32, F_HIGH_RAD = 64, F_SEEN = 128;
	/** The TSC's area monitor's ALERT and HIGH (mSv/h: 2.5 and 15 mR/h), and the dose rate that makes a GENERAL EMERGENCY (100 mR/h). */
	public static final double AREA_ALERT = 0.025, AREA_HIGH = 0.15, GE_DOSE = 1.0;
	/** A frisker's background: its count rate a mR/h, and with none. */
	public static final double FRISK_PER_MR = 3_300, FRISK_BACKGROUND = 40;
	/** Ticks every condition must stay clear before the emergency is terminated (five minutes). */
	public static final int TERMINATE = 6_000;
	/** Day time ticks an hour; the notifications' deadlines after the declaration (15 minutes, an hour); the briefings. */
	public static final long HOUR = 1_000, STATE_DUE = 250, NRC_DUE = 1_000, BRIEF = 500;
	/** How far a TSC panel looks for its board; how far the board looks for the HVAC panel and monitors, and the plant's equipment. */
	public static final int REACH = 24, HVAC_REACH = 48, PLANT_REACH = 192;
	/** The main generator's rating (MWe). */
	public static final double RATED_MWE = 1_150;
	/** Each console's two positions (its variant), the communicators, the staffing board's positions (tools/tsc_room.py). */
	public static final String[][] POSITIONS = {{"OPS COORDINATOR", "OPS RECORDER"}, {"RAD COORDINATOR", "DOSE ASSESSMENT"},
			{"ENGINEERING COORD", "REACTOR ENGINEER"}, {"ELECTRICAL ENGINEER", "MECHANICAL ENGINEER"}, {"MAINTENANCE COORD", "MAINT PLANNER"},
			{"ADMIN COORDINATOR", "SECURITY COORD"}, {"SITE EMERGENCY MGR", "TSC LOG KEEPER"}};
	public static final String[] COMMS = {"ENS COMMUNICATOR", "HPN COMMUNICATOR", "OFFSITE COMMUNICATOR"};
	public static final String[] STAFF = {"SITE EMERGENCY MGR", "OPS COORDINATOR", "RAD COORDINATOR", "ENGINEERING COORD", "MAINTENANCE COORD",
			"ENS COMMUNICATOR", "SECURITY COORD", "ADMIN COORDINATOR"};
	public static final String[] CSF = {"SUBCRITICALITY", "CORE COOLING", "HEAT SINK", "RCS INTEGRITY", "CONTAINMENT", "RCS INVENTORY"};
	public static final String[] CSF_SHORT = {"SUBCRIT", "CORE COOL", "HEAT SINK", "INTEGRITY", "CONTAINMT", "INVENTORY"};
	public static final String[] SECTORS = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
	/** The roster's names (Justin: "generate random white American names"). */
	static final String[] FIRST = {"James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles", "Daniel",
			"Matthew", "Mark", "Steven", "Paul", "Andrew", "Kevin", "Brian", "Gary", "Timothy", "Jeffrey", "Scott", "Eric", "Gregory", "Dale",
			"Wayne", "Craig", "Todd", "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Karen", "Sarah", "Lisa", "Nancy",
			"Emily", "Michelle", "Amanda", "Melissa", "Laura", "Rebecca", "Heather", "Amy", "Megan", "Rachel", "Katherine", "Christine", "Diane"};
	static final String[] LAST = {"Miller", "Keller", "Anderson", "Thompson", "Wilson", "Taylor", "Moore", "Clark", "Lewis", "Walker", "Hall",
			"Allen", "Young", "King", "Wright", "Hill", "Scott", "Green", "Baker", "Adams", "Nelson", "Carter", "Mitchell", "Roberts", "Turner",
			"Phillips", "Campbell", "Parker", "Evans", "Edwards", "Collins", "Stewart", "Morris", "Murphy", "Cook", "Rogers", "Morgan", "Cooper",
			"Peterson", "Reed", "Bailey", "Bell", "Kelly", "Howard", "Ward", "Brooks", "Gray", "Hughes", "Price", "Sanders", "Bennett", "Wood",
			"Barnes", "Ross", "Henderson", "Jenkins", "Perry", "Powell", "Patterson", "Fisher", "Hansen", "Larson", "Schmidt", "Novak", "Becker",
			"Shaw", "Lawson", "Harper", "Fletcher", "Holt", "Brandt", "Arnold", "Sullivan", "Olson", "Weber", "Gardner", "Russell", "Burke"};

	private TscRoom() {
	}

	/** Which kind a block is ({@link #HUB} to {@link #FRISKER}), or -1 for anything else. */
	public static int kind(BlockState state) {
		for (int k = 0; k < KEYS.length; k++) {
			if (state.getBlock() == PlantInterior.BLOCKS.get(KEYS[k])) {
				return k;
			}
		}
		return -1;
	}

	/** Once a second: the board reads the plant; the monitors read their dose rate; the rest find their board. */
	public static void tick(LivePanelBlockEntity panel, ServerLevel level, int kind) {
		switch (kind) {
			case HUB -> hub(panel, level);
			case AREA, FRISKER -> dose(panel, level);
			default -> {
				// Its board, looked for again every ten seconds.
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
		}
	}

	/** An area monitor or frisker: the dose rate where it stands (mSv/h), sent when it has moved. */
	private static void dose(LivePanelBlockEntity panel, ServerLevel level) {
		double rate = Math.max(0, Radiation.rateAt(level, Vec3.atCenterOf(panel.getBlockPos().above())));
		double was = panel.tsValue;
		if (Math.abs(rate - was) > Math.max(2e-5, was * 0.05)) {
			panel.tsValue = rate;
			panel.syncNow(level);
		}
	}

	/** The board: reads the plant, works out the class, keeps the clocks and the log. */
	static void hub(LivePanelBlockEntity hub, ServerLevel level) {
		hub.readReactor(level);
		RelayRoom.Reactor r = hub.reactor();
		if (hub.tsFound == null || hub.tsRescan-- <= 0) {
			hub.tsFound = find(hub, level);
			hub.tsRescan = 9;
		}
		Found f = hub.tsFound;
		int bits = 0;
		double[] n = new double[NUMS];
		if (f.hvac != null && level.getBlockEntity(f.hvac) instanceof LivePanelBlockEntity hv) {
			bits |= B_HVAC | ((hv.hvAlarms & HvacRoom.EMERGENCY_ON) != 0 ? B_HVAC_EMERG : 0);
		}
		for (int k = 0; k < 2; k++) {
			BlockPos at = f.intakes[k];
			if (at != null && level.getBlockEntity(at) instanceof LivePanelBlockEntity m) {
				bits |= B_INTAKE_A << k;
				n[N_INTAKE_A + k] = Math.max(HvacRoom.RAD_BACKGROUND, m.hvValue);
			}
			if (equipment(level, f.edgs, k) instanceof EquipmentBlockEntity edg) {
				bits |= (B_EDG_A | (edg.running ? B_EDG_A_RUN : 0)) << 2 * k;
			}
			if (equipment(level, f.lcs, k) instanceof EquipmentBlockEntity lc && lc.bus != null) {
				bits |= B_LC_A << k;
				n[N_LC_A + k] = lc.bus.getAmount() / (double) Math.max(1, lc.busCapacity());
			}
			if (equipment(level, f.mccs, k) instanceof EquipmentBlockEntity mcc && mcc.bus != null) {
				bits |= B_MCC_A << k;
				n[N_MCC_A + k] = mcc.bus.getAmount() / (double) Math.max(1, mcc.busCapacity());
			}
		}
		if (equipment(level, f.xfmr, 0) instanceof EquipmentBlockEntity xf && xf.output != null) {
			bits |= B_XFMR | (xf.output.getAmount() > 0 ? B_XFMR_LIVE : 0);
		}
		n[N_AREA] = f.area != null && level.getBlockEntity(f.area) instanceof LivePanelBlockEntity am ? am.tsValue
				: Math.max(0, Radiation.rateAt(level, Vec3.atCenterOf(hub.getBlockPos())));
		boolean changed = bits != hub.tsBits;
		for (int i = 0; i < NUMS; i++) {
			double was = hub.tsNums[i];
			changed |= Math.abs(n[i] - was) > Math.max(i == N_AREA ? 2e-5 : 0.005, Math.abs(was) * 0.03);
		}
		hub.tsBits = bits;
		if (changed) {
			System.arraycopy(n, 0, hub.tsNums, 0, NUMS);
		}
		changed |= classify(hub, r, level.getOverworldClockTime());
		if (changed) {
			hub.syncNow(level);
		}
	}

	/** The conditions the log follows. */
	static int flags(RelayRoom.Reactor r, int bits, double[] n) {
		int f = 0;
		if (r.status() == 3) f |= F_TRIP;
		if (r.present() && r.temperature() >= ReactorCore.ALARM_TEMP) f |= F_HOT;
		if ((r.alarms() & ReactorCore.A_FUEL_DAMAGED) != 0) f |= F_FUEL;
		if ((bits & B_HVAC_EMERG) != 0) f |= F_HVAC;
		if ((bits & B_EDG_A_RUN) != 0) f |= F_EDG_A;
		if ((bits & B_EDG_B_RUN) != 0) f |= F_EDG_B;
		if (Math.max(n[N_INTAKE_A], n[N_INTAKE_B]) >= HvacRoom.RAD_HIGH) f |= F_HIGH_RAD;
		return f;
	}

	/** The emergency class the plant's conditions call for now (0 none to 4 a GENERAL EMERGENCY). */
	public static int condition(RelayRoom.Reactor r, int bits, double[] n) {
		boolean fuel = (r.alarms() & ReactorCore.A_FUEL_DAMAGED) != 0;
		if (fuel && (r.temperature() >= ReactorCore.DAMAGE_TEMP || n[N_AREA] >= GE_DOSE)) {
			return 4;
		}
		if (fuel) {
			return 3;
		}
		if (r.present() && r.temperature() >= ReactorCore.ALARM_TEMP || Math.max(n[N_INTAKE_A], n[N_INTAKE_B]) >= HvacRoom.RAD_HIGH
				|| n[N_AREA] >= AREA_ALERT) {
			return 2;
		}
		return r.status() == 3 || (bits & B_HVAC_EMERG) != 0 ? 1 : 0;
	}

	/** The log, the class, its clocks: true when anything changed. */
	static boolean classify(LivePanelBlockEntity hub, RelayRoom.Reactor r, long now) {
		boolean changed = false;
		int was = hub.tsFlags, f = flags(r, hub.tsBits, hub.tsNums);
		if ((was & F_SEEN) != 0) {
			int up = f & ~was, down = was & ~f;
			if ((up & F_TRIP) != 0) changed |= log(hub, now, E_TRIP);
			if ((down & F_TRIP) != 0) changed |= log(hub, now, E_RESET);
			if ((up & F_HOT) != 0) changed |= log(hub, now, E_HOT);
			if ((up & F_FUEL) != 0) changed |= log(hub, now, E_FUEL);
			if ((up & F_HVAC) != 0) changed |= log(hub, now, E_HVAC);
			if ((down & F_HVAC) != 0) changed |= log(hub, now, E_HVAC_NORMAL);
			for (int k = 0; k < 2; k++) {
				if ((up & F_EDG_A << k) != 0) changed |= log(hub, now, E_EDG_START + k);
				if ((down & F_EDG_A << k) != 0) changed |= log(hub, now, E_EDG_STOP + k);
			}
			if ((up & F_HIGH_RAD) != 0) changed |= log(hub, now, E_HIGH_RAD);
		}
		hub.tsFlags = f | F_SEEN;
		int cond = condition(r, hub.tsBits, hub.tsNums);
		if (cond > hub.tsClass) {
			if (hub.tsStart < 0) {
				hub.tsStart = now;
			}
			hub.tsClass = cond;
			hub.tsDeclared = now;
			hub.tsClear = 0;
			log(hub, now, E_DECLARED + cond - 1);
			if (cond >= 2 && hub.tsActivated < 0) {
				hub.tsActivated = now;
				log(hub, now, E_TSC);
			}
			changed = true;
		} else if (hub.tsClass > 0) {
			hub.tsClear = cond == 0 ? hub.tsClear + 20 : 0;
			if (hub.tsClear >= TERMINATE) {
				log(hub, now, E_TERMINATED);
				hub.tsClass = 0;
				hub.tsDeclared = hub.tsStart = hub.tsActivated = -1;
				hub.tsClear = 0;
				changed = true;
			}
		}
		return changed;
	}

	private static boolean log(LivePanelBlockEntity hub, long now, int event) {
		hub.tsEvents.add(now << 5 | event);
		while (hub.tsEvents.size() > LOG) {
			hub.tsEvents.remove(0);
		}
		return true;
	}

	/** What the board found about the plant (found again every ten seconds). */
	static final class Found {
		@Nullable BlockPos hvac, area;
		final @Nullable BlockPos[] intakes = new BlockPos[2];
		final List<BlockPos> edgs = new ArrayList<>(), lcs = new ArrayList<>(), mccs = new ArrayList<>(), xfmr = new ArrayList<>();
	}

	private static Found find(LivePanelBlockEntity hub, ServerLevel level) {
		Found f = new Found();
		BlockPos here = hub.getBlockPos();
		List<LivePanelBlockEntity> hvac = hub.panelsAround(level, PlantInterior.BLOCKS.get(HvacRoom.KEYS[HvacRoom.CONTROL]), HVAC_REACH);
		f.hvac = hvac.isEmpty() ? null : hvac.get(0).getBlockPos().immutable();
		for (LivePanelBlockEntity m : hub.panelsAround(level, PlantInterior.BLOCKS.get(HvacRoom.KEYS[HvacRoom.RAD]), HVAC_REACH)) {
			int train = Math.min(1, LivePanelBlockEntity.variant(m.getBlockState()));
			if (f.intakes[train] == null) {
				f.intakes[train] = m.getBlockPos().immutable();
			}
		}
		List<LivePanelBlockEntity> area = hub.panelsAround(level, PlantInterior.BLOCKS.get(KEYS[AREA]), REACH);
		f.area = area.isEmpty() ? null : area.get(0).getBlockPos().immutable();
		double reach = (double) PLANT_REACH * PLANT_REACH;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getKey().distSqr(here) > reach) {
				continue;
			}
			switch (e.getValue().kind()) {
				case EMERGENCY_DIESEL_GENERATOR -> f.edgs.add(e.getKey());
				case SWITCHGEAR_LINEUP -> f.lcs.add(e.getKey());
				case MCC_LINEUP -> f.mccs.add(e.getKey());
				case MAIN_TRANSFORMER -> f.xfmr.add(e.getKey());
				default -> {
				}
			}
		}
		// The trains in a fixed order (west to east, then north to south): every board agrees which is 1A.
		Comparator<BlockPos> order = Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()).thenComparingInt(p -> p.getY());
		f.edgs.sort(order);
		f.lcs.sort(order);
		f.mccs.sort(order);
		f.xfmr.sort(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(here)));
		return f;
	}

	private static @Nullable EquipmentBlockEntity equipment(ServerLevel level, List<BlockPos> list, int k) {
		if (k >= list.size() || !level.isLoaded(list.get(k))) {
			return null;
		}
		return level.getBlockEntity(list.get(k)) instanceof EquipmentBlockEntity e ? e : null;
	}

	// ------------------------------------------------------------------------------------------------ what it shows

	/** A day time as the clocks show it (the game's day starts at 06:00). */
	public static String clock(long dayTime) {
		long t = Math.floorMod(dayTime + 6_000, 24_000L);
		return String.format(Locale.ROOT, "%02d:%02d", t / HOUR, t % HOUR * 60 / HOUR);
	}

	/** A length of day time as hours and minutes. */
	public static String span(long ticks) {
		long m = Math.max(0, ticks) * 60 / HOUR;
		return String.format(Locale.ROOT, "%02d:%02d", m / 60 % 100, m % 60);
	}

	/**
	 * The wind (no weather station is modelled: worked out from the day and the weather, the same for every player):
	 * its speed (mph), where it blows from (degrees) and the stability class (A to F).
	 */
	public static double[] wind(long dayTime, boolean rain, boolean thunder) {
		double day = dayTime / 24_000.0;
		double from = Math.floorMod((long) Math.round(200 + 70 * Math.sin(day * 1.7) + 40 * Math.sin(day * 5.3 + 1.1)), 360L);
		double speed = (thunder ? 24 : rain ? 13 : 6) + 3 * Math.sin(day * 9.1 + 0.4) + 1.5 * Math.sin(day * 23.0);
		long t = Math.floorMod(dayTime, 24_000L);
		boolean daylight = t < 12_500 || t > 23_500;
		int stability = rain || thunder ? 3 : daylight ? (speed < 5 ? 1 : 2) : (speed < 5 ? 5 : 4);
		return new double[] {Math.max(1, speed), from, stability};
	}

	/** A direction's sector (16 points). */
	public static String sector(double degrees) {
		return SECTORS[(int) Math.floorMod(Math.round(degrees / 22.5), 16L)];
	}

	/** The roster: names for the plant's TSC positions and its NRC site team, the same for a board every time. */
	public static String[] roster(BlockPos hub, int count, int salt) {
		Random rng = new Random(hub.asLong() * 31 + salt);
		String[] out = new String[count];
		List<String> used = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			String name;
			do {
				name = FIRST[rng.nextInt(FIRST.length)] + " " + LAST[rng.nextInt(LAST.length)];
			} while (used.contains(name));
			used.add(name);
			out[i] = name.toUpperCase(Locale.ROOT);
		}
		return out;
	}

	/** Players near the board: in the TSC (its room, the floor it is on) and about the site. */
	public static int[] players(Level level, BlockPos hub) {
		int in = 0, site = 0;
		for (Player p : level.players()) {
			double dx = p.getX() - hub.getX() - 0.5, dz = p.getZ() - hub.getZ() - 0.5, dy = p.getY() - hub.getY();
			double d = dx * dx + dz * dz;
			if (d <= 20.0 * 20.0 && dy > -5 && dy < 2) {
				in++;
			}
			if (d <= (double) PLANT_REACH * PLANT_REACH) {
				site++;
			}
		}
		return new int[] {in, site};
	}

	/**
	 * Everything the TSC shows, worked out from its board's findings, the clock and the weather (on the server for the
	 * use messages, on the client for the displays).
	 */
	public static final class View {
		public final int cls;
		public final boolean emergency, present, tripped, hot, fuel, hvac, hvacEmerg, release;
		public final String clock, event, declared, stateDue, nrcDue, activated;
		public final RelayRoom.Reactor r;
		public final double power, tavg, pzrP, pzrL, sg, flow, rods, cet, mwe, subcooling, decay;
		/** Each critical safety function: 0 no reactor, 1 satisfied, 2 challenged. */
		public final int[] csf = new int[6];
		public final double intakeA, intakeB, areaMr, windSpeed, windFrom;
		public final String stability, downwind, sky, rxState, vent;
		/** MAIN GENERATOR, MAIN XFMR, 480V LC 1A, 1B, EDG 1A, 1B. */
		public final String[] elec = new String[6];
		/** The buses' charge (%; -1 none): LC 1A, 1B, MCC 1A, 1B. */
		public final double[] buses = new double[4];
		public final boolean[] edgRun = new boolean[2], edgThere = new boolean[2];
		/** The log, oldest first: its times and entries. */
		public final List<String> times = new ArrayList<>(), events = new ArrayList<>();
		public final String[] par = new String[3];
		public final List<String> priorities = new ArrayList<>();
		public final String brief;
		public final int inTsc, onSite;

		public View(LivePanelBlockEntity hub, long now, boolean rain, boolean thunder, int[] players) {
			r = hub.reactor();
			cls = Math.max(0, Math.min(4, hub.tsClass));
			emergency = cls > 0;
			present = r.present();
			tripped = r.status() == 3;
			hot = present && r.temperature() >= ReactorCore.ALARM_TEMP;
			fuel = (r.alarms() & ReactorCore.A_FUEL_DAMAGED) != 0;
			int bits = hub.tsBits;
			double[] n = hub.tsNums;
			hvac = (bits & B_HVAC) != 0;
			hvacEmerg = (bits & B_HVAC_EMERG) != 0;
			clock = clock(now);
			event = emergency && hub.tsStart >= 0 ? span(now - hub.tsStart) : "--:--";
			declared = emergency && hub.tsDeclared >= 0 ? clock(hub.tsDeclared) : "--:--";
			activated = hub.tsActivated >= 0 ? clock(hub.tsActivated) : "--:--";
			stateDue = emergency && hub.tsDeclared >= 0 && now < hub.tsDeclared + STATE_DUE ? clock(hub.tsDeclared + STATE_DUE) : "";
			nrcDue = emergency && hub.tsDeclared >= 0 && now < hub.tsDeclared + NRC_DUE ? clock(hub.tsDeclared + NRC_DUE) : "";
			power = present ? r.fission() * 100 : 0;
			tavg = r.tavgF();
			pzrP = r.pzrPressure();
			pzrL = r.pzrLevel();
			sg = r.sgLevel();
			flow = r.rcsFlow();
			rods = present ? r.rods() : 0;
			cet = present ? tavg + 6 + 30 * Math.max(0, r.heat()) : 0;
			mwe = present && r.status() == 1 ? RATED_MWE * Math.max(0, Math.min(1, r.heat())) : 0;
			// Saturation at the pressurizer's pressure (a fit to the steam tables), less the core exit temperature.
			double tsat = 212 + 441 * Math.pow(Math.max(0, pzrP + 14.7) / 2250, 0.32);
			subcooling = present ? tsat - cet : 0;
			decay = present ? Math.max(0, r.heat()) * 100 : 0;
			if (present) {
				csf[0] = tripped && r.fission() > 0.05 ? 2 : 1;
				csf[1] = hot || fuel ? 2 : 1;
				csf[2] = (r.alarms() & ReactorCore.A_LOW_WATER) != 0 ? 2 : 1;
				csf[3] = pzrP > 2485 ? 2 : 1;
				csf[4] = fuel || n[N_AREA] >= AREA_HIGH ? 2 : 1;
				csf[5] = (r.alarms() & ReactorCore.A_LOOPS) != 0 || pzrL < 17 ? 2 : 1;
			}
			rxState = !present ? "NO REACTOR" : tripped ? "REACTOR TRIPPED" : r.status() == 2 ? "REACTOR SHUTDOWN" : "REACTOR AT POWER";
			intakeA = (bits & B_INTAKE_A) != 0 ? n[N_INTAKE_A] : -1;
			intakeB = (bits & B_INTAKE_B) != 0 ? n[N_INTAKE_B] : -1;
			areaMr = n[N_AREA] * 100;
			release = fuel;
			vent = fuel ? "HIGH" : "NORMAL";
			double[] w = wind(now, rain, thunder);
			windSpeed = w[0];
			windFrom = w[1];
			stability = String.valueOf((char) ('A' + (int) w[2]));
			downwind = sector(windFrom + 180);
			sky = thunder ? "STORM" : rain ? "RAIN" : "CLEAR";
			elec[0] = String.format(Locale.ROOT, "%.0f", mwe);
			elec[1] = (bits & B_XFMR) == 0 ? "----" : (bits & B_XFMR_LIVE) != 0 ? "AVAILABLE" : "NO OUTPUT";
			for (int k = 0; k < 2; k++) {
				buses[k] = (bits & B_LC_A << k) != 0 ? n[N_LC_A + k] * 100 : -1;
				buses[2 + k] = (bits & B_MCC_A << k) != 0 ? n[N_MCC_A + k] * 100 : -1;
				elec[2 + k] = buses[k] < 0 ? "----" : buses[k] > 1 ? "ENERGIZED" : "DEAD";
				edgThere[k] = (bits & B_EDG_A << 2 * k) != 0;
				edgRun[k] = (bits & B_EDG_A_RUN << 2 * k) != 0;
				elec[4 + k] = !edgThere[k] ? "----" : edgRun[k] ? "RUNNING" : "STANDBY";
			}
			for (long e : hub.tsEvents) {
				times.add(clock(e >> 5));
				events.add(EVENTS[(int) Math.floorMod(e & 31, (long) EVENTS.length)]);
			}
			switch (cls) {
				case 4 -> {
					par[0] = "EVACUATE 2 MI";
					par[1] = "EVACUATE 5 MI " + downwind;
					par[2] = "SHELTER 5-10 MI";
				}
				case 3 -> {
					par[0] = "NONE";
					par[1] = "PREPARE TO";
					par[2] = "EVACUATE 2 MI";
				}
				case 2, 1 -> {
					par[0] = "NONE";
					par[1] = "NOT REQUIRED";
					par[2] = cls == 2 ? "AT ALERT" : "AT UNUSUAL EVENT";
				}
				default -> {
					par[0] = "NONE";
					par[1] = "";
					par[2] = "";
				}
			}
			if (!emergency) {
				priorities.add("NO EMERGENCY DECLARED");
				priorities.add("TSC IN STANDBY");
				brief = "";
			} else {
				priorities.add(cls >= 2 ? "TSC ACTIVATED " + activated : "UNUSUAL EVENT: TSC OPTIONAL");
				for (int i = 0; i < 6; i++) {
					if (csf[i] == 2) {
						priorities.add("RESTORE " + CSF[i]);
					}
				}
				if (hvacEmerg) {
					priorities.add("CR HVAC IN EMERGENCY");
				}
				if (!stateDue.isEmpty()) {
					priorities.add("STATE/LOCAL CALL BY " + stateDue);
				}
				if (!nrcDue.isEmpty()) {
					priorities.add("NRC ENS CALL BY " + nrcDue);
				}
				if (cls == 4) {
					priorities.add("PAR: " + par[0] + ", " + par[1]);
				}
				for (int k = 0; k < 2; k++) {
					if (buses[k] >= 0 && buses[k] <= 1) {
						priorities.add("RESTORE 480V LC 1" + (char) ('A' + k));
					}
				}
				priorities.add(present ? "MONITOR CORE EXIT TC" : "MONITOR PLANT STATUS");
				long since = hub.tsDeclared >= 0 ? now - hub.tsDeclared : 0;
				brief = "NEXT BRIEF " + clock((hub.tsDeclared >= 0 ? hub.tsDeclared : now) + BRIEF * (since / BRIEF + 1));
			}
			inTsc = players[0];
			onSite = players[1];
		}

		/** The priorities numbered, as the board has them. */
		public String priority(int i) {
			if (i >= priorities.size()) {
				return "";
			}
			return emergency ? (i + 1) + ". " + priorities.get(i) : priorities.get(i);
		}

		/** One line of the use messages. */
		public String summary() {
			StringBuilder s = new StringBuilder(cls == 0 ? "No emergency declared" : CLASSES[cls] + " declared at " + declared + " (event time " + event + ")");
			s.append("; ").append(rxState.toLowerCase(Locale.ROOT));
			if (present) {
				s.append(String.format(Locale.ROOT, ", core exit %.0f F", cet));
			}
			if (hvacEmerg) {
				s.append("; CR HVAC in emergency");
			}
			return s.toString();
		}
	}

	/** The use message of a TSC panel (from its board). */
	public static String describe(LivePanelBlockEntity panel, ServerLevel level) {
		int kind = kind(panel.getBlockState());
		if (kind == AREA) {
			double mr = panel.tsValue * 100;
			return String.format(Locale.ROOT, "Area radiation monitor RM-TSC-1: %.2f mR/hr (%s)", mr,
					panel.tsValue >= AREA_HIGH ? "HIGH: leave the TSC" : panel.tsValue >= AREA_ALERT ? "ALERT" : "normal");
		}
		if (kind == FRISKER) {
			return String.format(Locale.ROOT, "Frisker: background %.0f cpm. Frisk hands, feet, then body; over 100 cpm above background, call RP.",
					FRISK_BACKGROUND + panel.tsValue * 100 * FRISK_PER_MR);
		}
		LivePanelBlockEntity hub = kind == HUB ? panel
				: panel.panel != null && level.getBlockEntity(panel.panel) instanceof LivePanelBlockEntity h ? h : null;
		if (hub == null) {
			return "No emergency classification board in reach: nothing to show";
		}
		View v = new View(hub, level.getOverworldClockTime(), level.isRaining(), level.isThundering(), players(level, hub.getBlockPos()));
		return v.summary();
	}
}
