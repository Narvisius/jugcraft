package io.github.jimbozoomer.jugcraft.nuclear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The control room's live boards (round 27; Justin: "Control room boards live", the reactor coolant, turbine and
 * electrical boards first, "Real alarms with horn", "Key controls work", values "worked out from the plant"). Each
 * board watches the nearest reactor, its diesel generators, its 480 V load centre and its motor control centres, and
 * once every half second:
 *
 * <ul>
 *   <li>takes a {@link BoardPlant} snapshot of them, sent to the players, who work out every meter and lamp from it;
 *   <li>runs its annunciator: a window whose alarm comes in flashes fast and sounds the horn until ACKNOWLEDGE, then
 *       stays lit while the alarm lasts; when it clears it flashes slowly until RESET (the ISA "R" sequence). TEST
 *       lights every window for three seconds;
 *   <li>records its recorders' pens every five seconds (the last five minutes).
 * </ul>
 *
 * Its working controls (where they are: the board's "controls" in tools/board_live.py): the reactor coolant pumps
 * (stopping one above P-8 trips the reactor on low flow), the turbine trip under its guard (the first click lifts the
 * guard, the second trips; above P-9 the reactor trips too) and the turbine's LATCH, the generator breaker (closing it
 * waits for the synchroscope), the diesel generators' START and STOP, and the annunciator's buttons. The layout is read
 * from the boards' file (assets/jugcraft/control_boards.json.gz) on both sides.
 *
 * <p>Round 28 (Justin: "Boards round 2", "Full actuation", "MSIVs and aux feed", "Real radiation"): the safety systems
 * board's two manual safety injection switches under their guards (either actuates both trains: the reactor and
 * turbine trip, the diesels start, the safety injection, residual heat removal and charging pumps run, feedwater and
 * the containment isolate, auxiliary feed starts) and its SI RESET buttons (a minute after); the steam and feedwater
 * board's MSIVs and auxiliary feed pumps; the radiation board's monitors, which read the game's radiation where each
 * stands (the control room, above the reactor, beside it, the spent fuel pool, the fuel handling machine, the
 * auxiliary and turbine buildings, the casks; the control room's intakes from its HVAC room's own monitors; the plant
 * vent and the containment's air in counts a minute with the release from damaged fuel), and its control room HVAC
 * mimic from the HVAC panel.
 */
public final class ControlBoard {
	/** How far a board looks for its plant's diesel generators, load centre and motor control centres (blocks). */
	public static final int REACH = 256;
	/** Window states: clear, in alarm and not acknowledged (fast flash, horn), acknowledged (steady), cleared and not reset (slow flash). */
	public static final byte CLEAR = 0, ALERT = 1, ACKED = 2, RINGBACK = 3;
	/** Recorder samples kept (one every {@link #SAMPLE} ticks: five minutes). */
	public static final int SAMPLES = 60, SAMPLE = 100;
	/** Ticks the turbine trip's guard stays lifted, and the annunciator's test lasts. */
	public static final int GUARD = 100, TEST = 60;
	/** Ticks between the radiation monitors' readings (they look at the radiation field: two seconds). */
	public static final int RAD_EVERY = 40;
	/** The area monitors' background (mSv/h: 15 microrem an hour). */
	public static final double AREA_BACKGROUND = 1.5e-4;
	public static SoundEvent HORN;

	private ControlBoard() {
	}

	public static void register() {
		HORN = Registry.register(BuiltInRegistries.SOUND_EVENT, Jugcraft.id("control_room.annunciator"),
				SoundEvent.createFixedRangeEvent(Jugcraft.id("control_room.annunciator"), 32));
	}

	// ------------------------------------------------------------------ the boards' layouts

	/** A window: its id and whether it is a status light (lit while true, never flashing or sounding). */
	public record Window(String id, boolean status) {
	}

	/** A working control's place on the desk (x across the board, s from the desk's front edge to its back). */
	public record Control(String id, double x0, double x1, double s0, double s1) {
	}

	/** A recorder pen: what it draws, and its scale. */
	public record Pen(String value, double lo, double hi) {
	}

	public record Layout(List<Window> windows, List<Control> controls, List<Pen> pens) {
	}

	private static Map<EquipmentKind, Layout> layouts;

	/** A live board's layout, or null for any other equipment. */
	public static @Nullable Layout layout(EquipmentKind kind) {
		if (layouts == null) {
			Map<EquipmentKind, Layout> out = new EnumMap<>(EquipmentKind.class);
			try (InputStream in = ControlBoard.class.getResourceAsStream("/assets/jugcraft/control_boards.json.gz")) {
				if (in != null) {
					JsonObject all = JsonParser.parseReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8)).getAsJsonObject();
					for (EquipmentKind k : EquipmentKind.values()) {
						if (!k.liveBoard() || !all.has(k.id)) {
							continue;
						}
						JsonObject live = all.getAsJsonObject(k.id).getAsJsonObject("live");
						List<Window> windows = new ArrayList<>();
						for (JsonElement e : live.getAsJsonArray("windows")) {
							JsonObject w = e.getAsJsonObject();
							windows.add(new Window(w.get("id").getAsString(), w.get("k").getAsInt() == 1));
						}
						List<Control> controls = new ArrayList<>();
						for (JsonElement e : live.getAsJsonArray("controls")) {
							JsonObject c = e.getAsJsonObject();
							controls.add(new Control(c.get("id").getAsString(), c.get("x0").getAsDouble(), c.get("x1").getAsDouble(),
									c.get("s0").getAsDouble(), c.get("s1").getAsDouble()));
						}
						List<Pen> pens = new ArrayList<>();
						for (JsonElement e : live.getAsJsonArray("pens")) {
							for (JsonElement q : e.getAsJsonObject().getAsJsonArray("pens")) {
								JsonObject pen = q.getAsJsonObject();
								pens.add(new Pen(pen.get("v").getAsString(), pen.get("lo").getAsDouble(), pen.get("hi").getAsDouble()));
							}
						}
						out.put(k, new Layout(List.copyOf(windows), List.copyOf(controls), List.copyOf(pens)));
					}
				}
			} catch (Exception e) {
				Jugcraft.LOGGER.error("Could not read the control boards' layouts", e);
			}
			layouts = out;
		}
		return layouts.get(kind);
	}

	// ------------------------------------------------------------------ a board's state

	/** What a live board holds: its snapshot of the plant, its windows, its recorders, its test and guard, what it found. */
	public static final class State {
		public BoardPlant plant = new BoardPlant();
		public byte[] windows = new byte[0];
		/** The test's end, the guards' (the turbine trip's or safety injection train A's; train B's). */
		public long testUntil, guardUntil, guardBUntil;
		/** Round 30: the reactor console's rod lever, held out (1) or in (-1) until then (it springs back to HOLD). */
		public int lever;
		public long leverUntil;
		/** The recorders' pens: each pen's samples in a row (1 to 65535 across its scale, 0 none), newest at {@link #head} - 1. */
		public int[] history = new int[0];
		public int head;
		/** Client: each needle's and figure's reading as last drawn (they swing to a new reading), and when (nanoseconds). */
		public float @Nullable [] shown;
		public long shownNanos;
		// Server only.
		@Nullable BoardPlant sent;
		long lastSync, lastHorn, lastSample = Long.MIN_VALUE / 2, lastSearch = Long.MIN_VALUE / 2;
		int searchVersion = -1;
		@Nullable BlockPos vessel, lc;
		final @Nullable BlockPos[] edg = new BlockPos[2], mcc = new BlockPos[2];
		boolean wasOnline;
		// Round 28: the control room HVAC panel and its radiation monitors; the radiation monitors' spots and readings.
		@Nullable BlockPos hvPanel;
		/** Punch list #15: the cable spreading room's releasing panel (the radiation board's: the nearest in reach). */
		@Nullable BlockPos csrPanel;
		final @Nullable BlockPos[] hvRad = new BlockPos[2];
		final @Nullable Vec3[] radSpots = new Vec3[BoardPlant.RAD_MONITORS];
		final double[] rad = new double[BoardPlant.RAD_MONITORS];
		int radFound;
		long lastRad = Long.MIN_VALUE / 2;
		/**
		 * Round 32, the plant display wall: its banner's alarms (DisplayWall.pack: board, window, state; newest first),
		 * how many are in and how many not acknowledged. Server only: the boards it found, and when it first saw each
		 * alarm (board kind's ordinal << 16 | window).
		 */
		public int[] wallAlarms = new int[0];
		public int wallTotal, wallNew;
		@Nullable List<BlockPos> wallBoards;
		int wallSearch = -1;
		final java.util.Map<Integer, Long> wallSince = new java.util.HashMap<>();

		void size(Layout layout) {
			if (windows.length != layout.windows().size()) {
				windows = new byte[layout.windows().size()];
			}
			if (history.length != layout.pens().size() * SAMPLES) {
				history = new int[layout.pens().size() * SAMPLES];
				head = 0;
			}
		}

		/** Keeps what only this side holds when a new state arrives from the server (the drawn readings). */
		public void copyLocal(State old) {
			shown = old.shown;
			shownNanos = old.shownNanos;
		}

		public boolean unacked() {
			for (byte w : windows) {
				if (w == ALERT) {
					return true;
				}
			}
			return false;
		}

		public boolean cleared() {
			for (byte w : windows) {
				if (w == RINGBACK) {
					return true;
				}
			}
			return false;
		}

		public void save(ValueOutput parent) {
			ValueOutput out = parent.child("board");
			plant.save(out);
			int[] w = new int[windows.length];
			for (int i = 0; i < w.length; i++) {
				w[i] = windows[i];
			}
			out.putIntArray("windows", w);
			out.putLong("test", testUntil);
			out.putLong("guard", guardUntil);
			out.putLong("guard_b", guardBUntil);
			out.putInt("lever", lever);
			out.putLong("lever_until", leverUntil);
			out.putIntArray("history", history);
			out.putInt("head", head);
			if (wallAlarms.length > 0 || wallTotal > 0) {
				out.putIntArray("wall_alarms", wallAlarms);
				out.putInt("wall_total", wallTotal);
				out.putInt("wall_new", wallNew);
			}
		}

		public static State load(ValueInput parent) {
			State s = new State();
			Optional<ValueInput> child = parent.child("board");
			if (child.isEmpty()) {
				return s;
			}
			ValueInput in = child.get();
			s.plant = BoardPlant.load(in);
			int[] w = in.getIntArray("windows").orElse(new int[0]);
			s.windows = new byte[w.length];
			for (int i = 0; i < w.length; i++) {
				s.windows[i] = (byte) w[i];
			}
			s.testUntil = in.getLongOr("test", 0L);
			s.guardUntil = in.getLongOr("guard", 0L);
			s.guardBUntil = in.getLongOr("guard_b", 0L);
			s.lever = in.getIntOr("lever", 0);
			s.leverUntil = in.getLongOr("lever_until", 0L);
			s.history = in.getIntArray("history").orElse(new int[0]);
			s.head = in.getIntOr("head", 0);
			s.wallAlarms = in.getIntArray("wall_alarms").orElse(new int[0]);
			s.wallTotal = in.getIntOr("wall_total", 0);
			s.wallNew = in.getIntOr("wall_new", 0);
			return s;
		}
	}

	// ------------------------------------------------------------------ the server's tick

	public static void tick(ServerLevel level, EquipmentBlockEntity e) {
		Layout layout = layout(e.kind());
		if (layout == null) {
			return;
		}
		State s = e.board();
		s.size(layout);
		long now = level.getGameTime();
		if (Math.floorMod(now + e.getBlockPos().hashCode(), 10) != 0) {
			return;
		}
		find(level, e, s, now);
		if (e.kind() == EquipmentKind.RADIATION_BOARD && now - s.lastRad >= RAD_EVERY) {
			s.lastRad = now;
			radiation(level, s);
		}
		BoardPlant p = snapshot(level, s);
		s.plant = p;
		boolean dirty = annunciate(s, layout, p, now);
		if (e.kind() == EquipmentKind.PLANT_DISPLAY_WALL) {
			// Round 32: the display wall's banner, from the boards in its room.
			dirty |= DisplayWall.alarms(level, e, s, now);
		}
		if (now - s.lastSample >= SAMPLE) {
			s.lastSample = now;
			sample(s, layout, p, now);
			dirty = true;
		}
		Vec3 middle = e.middle();
		if (s.unacked() && now - s.lastHorn >= 40) {
			s.lastHorn = now;
			level.playSound(null, middle.x, middle.y, middle.z, HORN, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		if (p.online() != s.wasOnline) {
			// The generator breaker closing (through the synchroscope) or tripping.
			s.wasOnline = p.online();
			if (e.kind() == EquipmentKind.ELECTRICAL_BOARD) {
				level.playSound(null, middle.x, middle.y, middle.z, p.online() ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN,
						SoundSource.BLOCKS, 0.8F, 0.6F);
			}
		}
		if (dirty || p.differs(s.sent) || now - s.lastSync >= 100) {
			s.sent = p;
			s.lastSync = now;
			e.changed();
		}
	}

	/** Looks at the plant again now and runs the windows a step, whatever the tick (tests, the controls). */
	public static State refresh(ServerLevel level, EquipmentBlockEntity e) {
		State s = e.board();
		Layout layout = layout(e.kind());
		if (layout == null) {
			return s;
		}
		s.size(layout);
		s.searchVersion = -1;
		find(level, e, s, level.getGameTime());
		if (e.kind() == EquipmentKind.RADIATION_BOARD) {
			s.lastRad = level.getGameTime();
			radiation(level, s);
		}
		s.plant = snapshot(level, s);
		annunciate(s, layout, s.plant, level.getGameTime());
		if (e.kind() == EquipmentKind.PLANT_DISPLAY_WALL) {
			s.wallSearch = -1;
			DisplayWall.alarms(level, e, s, level.getGameTime());
		}
		return s;
	}

	/** Runs the windows' sequence a step; whether any window changed. */
	static boolean annunciate(State s, Layout layout, BoardPlant p, long now) {
		boolean changed = false;
		for (int i = 0; i < s.windows.length; i++) {
			Window w = layout.windows().get(i);
			if (w.status()) {
				continue;
			}
			byte before = s.windows[i];
			s.windows[i] = next(before, p.alarm(w.id(), now));
			changed |= before != s.windows[i];
		}
		return changed;
	}

	/** A window's next state with its alarm in or out (ACKNOWLEDGE and RESET are {@link #acknowledge}, {@link #reset}). */
	public static byte next(byte state, boolean alarm) {
		return switch (state) {
			case CLEAR -> alarm ? ALERT : CLEAR;
			case ACKED -> alarm ? ACKED : RINGBACK;
			case RINGBACK -> alarm ? ALERT : RINGBACK;
			default -> ALERT;
		};
	}

	/** ACKNOWLEDGE: windows in alert go steady, or to ringback if their alarm has already cleared. */
	public static boolean acknowledge(State s, Layout layout, BoardPlant p, long now) {
		boolean any = false;
		for (int i = 0; i < s.windows.length; i++) {
			if (s.windows[i] == ALERT) {
				s.windows[i] = p.alarm(layout.windows().get(i).id(), now) ? ACKED : RINGBACK;
				any = true;
			}
		}
		return any;
	}

	/** RESET: windows in ringback go dark. */
	public static boolean reset(State s) {
		boolean any = false;
		for (int i = 0; i < s.windows.length; i++) {
			if (s.windows[i] == RINGBACK) {
				s.windows[i] = CLEAR;
				any = true;
			}
		}
		return any;
	}

	static void sample(State s, Layout layout, BoardPlant p, long now) {
		for (int k = 0; k < layout.pens().size(); k++) {
			Pen pen = layout.pens().get(k);
			double f = BoardPlant.clamp((p.value(pen.value(), now) - pen.lo()) / (pen.hi() - pen.lo()), 0, 1);
			// 1 to 65535 across the scale (0: not recorded yet).
			s.history[k * SAMPLES + s.head] = 1 + (int) Math.round(f * 65534);
		}
		s.head = (s.head + 1) % SAMPLES;
	}

	/** Finds the board's reactor (the nearest), its diesel generators, load centre and motor control centres. */
	static void find(ServerLevel level, EquipmentBlockEntity e, State s, long now) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (registry.version() == s.searchVersion && now - s.lastSearch < 200) {
			return;
		}
		s.searchVersion = registry.version();
		s.lastSearch = now;
		BlockPos here = e.getBlockPos();
		EquipmentBlockEntity vessel = e.kind() == EquipmentKind.CONTROL_CONSOLE ? null : LivePanelBlockEntity.nearestVessel(level, here);
		if ((e.kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL || e.kind() == EquipmentKind.CONTROL_CONSOLE) && e.linked != null
				&& level.isLoaded(e.linked) && level.getBlockEntity(e.linked) instanceof EquipmentBlockEntity linked && linked.kind().reactor()) {
			// The remote shutdown panel watches the reactor it has linked (its own search, within 256 blocks); the
			// reactor control console (round 30) the one its link card linked it to.
			vessel = linked;
		}
		s.vessel = vessel == null ? null : vessel.getBlockPos().immutable();
		List<BlockPos> edgs = new ArrayList<>(), mccs = new ArrayList<>(), filters = new ArrayList<>();
		BlockPos lc = null;
		double lcBest = (double) REACH * REACH;
		// The radiation board's spots' equipment, nearest the board: the spent fuel racks, the fuel handling machine,
		// the turbine, the casks, and (outside the containment, 24 blocks or more from the reactor) a heat exchanger or
		// pump skid of the auxiliary building.
		Map<EquipmentKind, BlockPos> nearest = new EnumMap<>(EquipmentKind.class);
		Map<EquipmentKind, Double> best = new EnumMap<>(EquipmentKind.class);
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> a : registry.anchors().entrySet()) {
			double d = a.getKey().distSqr(here);
			if (d > (double) REACH * REACH) {
				continue;
			}
			EquipmentKind kind = a.getValue().kind();
			switch (kind) {
				case EMERGENCY_DIESEL_GENERATOR -> edgs.add(a.getKey().immutable());
				case MCC_LINEUP -> mccs.add(a.getKey().immutable());
				case CR_FILTRATION_UNIT -> filters.add(a.getKey().immutable());
				case SWITCHGEAR_LINEUP -> {
					if (d < lcBest) {
						lcBest = d;
						lc = a.getKey().immutable();
					}
				}
				case SPENT_FUEL_RACK, FUEL_HANDLING_MACHINE, TURBINE_GENERATOR, SMALL_TURBINE, DRY_STORAGE_CASK, HEAT_EXCHANGER, PUMP_SKID -> {
					if ((kind == EquipmentKind.HEAT_EXCHANGER || kind == EquipmentKind.PUMP_SKID)
							&& (s.vessel == null || a.getKey().distSqr(s.vessel) < 24 * 24)) {
						continue;
					}
					EquipmentKind key = kind == EquipmentKind.SMALL_TURBINE ? EquipmentKind.TURBINE_GENERATOR
							: kind == EquipmentKind.PUMP_SKID ? EquipmentKind.HEAT_EXCHANGER : kind;
					if (d < best.getOrDefault(key, Double.MAX_VALUE)) {
						best.put(key, d);
						nearest.put(key, a.getKey().immutable());
					}
				}
				default -> {
				}
			}
		}
		s.lc = lc;
		// The two nearest diesels, named A and B by where they stand.
		edgs.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(here)));
		List<BlockPos> two = new ArrayList<>(edgs.subList(0, Math.min(2, edgs.size())));
		two.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
		s.edg[0] = two.size() > 0 ? two.get(0) : null;
		s.edg[1] = two.size() > 1 ? two.get(1) : null;
		// The motor control centres by their train (the anchor's variant: 0 A, 1 B), else the nearest two.
		mccs.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(here)));
		s.mcc[0] = s.mcc[1] = null;
		for (BlockPos m : mccs) {
			int train = level.isLoaded(m) && level.getBlockState(m).hasProperty(EquipmentAnchorBlock.VARIANT)
					? Math.min(1, level.getBlockState(m).getValue(EquipmentAnchorBlock.VARIANT)) : 0;
			if (s.mcc[train] == null) {
				s.mcc[train] = m;
			} else if (s.mcc[1 - train] == null) {
				s.mcc[1 - train] = m;
			}
		}
		// The control room's HVAC panel and its radiation monitors (the safety systems and radiation boards): in the
		// HVAC room round the nearest filtration unit, else round the board.
		s.hvPanel = s.hvRad[0] = s.hvRad[1] = null;
		if (e.kind() == EquipmentKind.ESF_BOARD || e.kind() == EquipmentKind.RADIATION_BOARD) {
			filters.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(here)));
			BlockPos room = filters.isEmpty() ? here : filters.get(0);
			List<LivePanelBlockEntity> panels = panels(level, room, PlantInterior.BLOCKS.get(HvacRoom.KEYS[HvacRoom.CONTROL]), HvacRoom.REACH);
			s.hvPanel = panels.isEmpty() ? null : panels.get(0).getBlockPos().immutable();
			List<LivePanelBlockEntity> monitors = panels(level, room, PlantInterior.BLOCKS.get(HvacRoom.KEYS[HvacRoom.RAD]), HvacRoom.REACH);
			List<BlockPos> intakes = new ArrayList<>();
			for (int k = 0; k < Math.min(2, monitors.size()); k++) {
				intakes.add(monitors.get(k).getBlockPos().immutable());
			}
			intakes.sort(java.util.Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
			for (int k = 0; k < intakes.size(); k++) {
				s.hvRad[k] = intakes.get(k);
			}
		}
		if (e.kind() == EquipmentKind.RADIATION_BOARD) {
			radSpots(level, e, s, vessel, nearest, filters);
			List<LivePanelBlockEntity> csr = panels(level, here, PlantInterior.BLOCKS.get(SpreadingRoomFire.RELEASE), CSR_REACH);
			s.csrPanel = csr.isEmpty() ? null : csr.get(0).getBlockPos().immutable();
		} else {
			s.csrPanel = null;
		}
	}

	/** How far the radiation board looks for the cable spreading room's releasing panel (punch list #15). */
	static final int CSR_REACH = 48;

	/** The live panels of a kind within reach of a spot (their chunks' block entities), nearest first (on a client too). */
	static List<LivePanelBlockEntity> panels(net.minecraft.world.level.Level level, BlockPos near, net.minecraft.world.level.block.@Nullable Block kind, int reach) {
		List<LivePanelBlockEntity> out = new ArrayList<>();
		if (kind == null) {
			return out;
		}
		int r = (reach >> 4) + 1;
		for (int cx = (near.getX() >> 4) - r; cx <= (near.getX() >> 4) + r; cx++) {
			for (int cz = (near.getZ() >> 4) - r; cz <= (near.getZ() >> 4) + r; cz++) {
				net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof LivePanelBlockEntity panel && be.getBlockState().getBlock() == kind
							&& be.getBlockPos().distSqr(near) <= (double) reach * reach) {
						out.add(panel);
					}
				}
			}
		}
		out.sort(java.util.Comparator.<LivePanelBlockEntity>comparingDouble(p -> p.getBlockPos().distSqr(near))
				.thenComparingInt(p -> p.getBlockPos().getX()).thenComparingInt(p -> p.getBlockPos().getZ()));
		return out;
	}

	/** In front of a piece of equipment (two blocks out from its front, the middle of its width), at eye height. */
	private static Vec3 before(EquipmentBlockEntity e) {
		EquipmentKind k = e.kind();
		return Vec3.atCenterOf(e.getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(k.wide / 2, 0, -2), e.facing()))).add(0, 1.1, 0);
	}

	/** On top of a piece of equipment, at eye height. */
	private static Vec3 above(EquipmentBlockEntity e) {
		Vec3 m = e.middle();
		return new Vec3(m.x, e.getBlockPos().getY() + e.kind().tall + 1.6, m.z);
	}

	/**
	 * Where the radiation board's monitors read (RM-1 to RM-12; tools/board_live.py RAD_MONITORS): the control room in
	 * front of the board; above the reactor's head and beside its core; at the spent fuel pool's surface over the
	 * nearest rack; on the fuel handling machine; beside the auxiliary building's nearest heat exchanger; on the
	 * turbine; beside the nearest cask; the control room's intakes (the HVAC room's monitors, else its filtration
	 * units); the plant vent above the auxiliary building; the containment's air above the reactor. A monitor whose
	 * spot is not there reads nothing and shows FAIL.
	 */
	static void radSpots(ServerLevel level, EquipmentBlockEntity board, State s, @Nullable EquipmentBlockEntity vessel,
			Map<EquipmentKind, BlockPos> nearest, List<BlockPos> filters) {
		Vec3[] spot = s.radSpots;
		java.util.Arrays.fill(spot, null);
		spot[0] = before(board);
		if (vessel != null) {
			spot[1] = Vec3.atCenterOf(vessel.centre()).add(0, 2.5, 0);
			Vec3 core = Vec3.atCenterOf(vessel.coreMiddle());
			Vec3 toward = new Vec3(board.middle().x - core.x, 0, board.middle().z - core.z);
			double len = toward.length();
			Vec3 dir = len < 1e-6 ? new Vec3(1, 0, 0) : toward.scale(1 / len);
			spot[2] = core.add(dir.scale(Math.max(vessel.kind().wide, vessel.kind().deep) / 2.0 + 3));
			spot[11] = spot[1];
		}
		EquipmentBlockEntity rack = loaded(level, nearest.get(EquipmentKind.SPENT_FUEL_RACK));
		if (rack != null) {
			// Up through the pool's water to its surface.
			Vec3 m = rack.middle();
			BlockPos at = BlockPos.containing(m.x, rack.getBlockPos().getY() + rack.kind().tall, m.z);
			for (int k = 0; k < 24 && level.isLoaded(at) && !level.getBlockState(at).isAir(); k++) {
				at = at.above();
			}
			spot[3] = Vec3.atCenterOf(at).add(0, 0.6, 0);
		}
		EquipmentBlockEntity fhm = loaded(level, nearest.get(EquipmentKind.FUEL_HANDLING_MACHINE));
		if (fhm != null) {
			spot[4] = above(fhm);
		}
		EquipmentBlockEntity aux = loaded(level, nearest.get(EquipmentKind.HEAT_EXCHANGER));
		if (aux != null) {
			spot[5] = before(aux);
			spot[10] = aux.middle().add(0, 15, 0);
		} else if (vessel != null) {
			spot[10] = Vec3.atCenterOf(vessel.centre()).add(0, 30, 0);
		}
		EquipmentBlockEntity turbine = loaded(level, nearest.get(EquipmentKind.TURBINE_GENERATOR));
		if (turbine != null) {
			spot[6] = above(turbine);
		}
		EquipmentBlockEntity cask = loaded(level, nearest.get(EquipmentKind.DRY_STORAGE_CASK));
		if (cask != null) {
			spot[7] = before(cask);
		}
		// The intakes: the HVAC room's own monitors when it has them (radiation() reads theirs), else above the
		// filtration units (by train).
		for (BlockPos f : filters) {
			EquipmentBlockEntity unit = loaded(level, f);
			if (unit == null) {
				continue;
			}
			int train = unit.getBlockState().hasProperty(EquipmentAnchorBlock.VARIANT) ? Math.min(1, unit.getBlockState().getValue(EquipmentAnchorBlock.VARIANT)) : 0;
			if (spot[8 + train] == null) {
				spot[8 + train] = above(unit);
			}
		}
	}

	/** The radiation monitors read (every two seconds, the radiation board's): the dose rate at each spot, in its meter's units. */
	static void radiation(ServerLevel level, State s) {
		boolean plume = (s.plant.alarms & ReactorCore.A_FUEL_DAMAGED) != 0;
		int found = 0;
		for (int k = 0; k < BoardPlant.RAD_MONITORS; k++) {
			double reading;
			BlockPos intake = k == 8 || k == 9 ? s.hvRad[k - 8] : null;
			if (intake != null && level.isLoaded(intake) && level.getBlockEntity(intake) instanceof LivePanelBlockEntity monitor) {
				// The HVAC room's own intake monitor: its count rate (the plume from damaged fuel already in it).
				reading = monitor.hvValue > 0 ? monitor.hvValue : HvacRoom.RAD_BACKGROUND;
			} else if (s.radSpots[k] != null && level.isLoaded(BlockPos.containing(s.radSpots[k]))) {
				double msv = Math.max(0, Radiation.rateAt(level, s.radSpots[k]));
				reading = switch (BoardPlant.RAD_UNIT[k]) {
					case 0 -> (msv + AREA_BACKGROUND) * 100;
					case 1 -> (msv + AREA_BACKGROUND) / 10;
					default -> HvacRoom.RAD_BACKGROUND + msv * HvacRoom.CPM_PER_MSV + (plume ? HvacRoom.PLUME : 0);
				};
			} else {
				s.rad[k] = 0;
				continue;
			}
			s.rad[k] = reading;
			found |= 1 << k;
		}
		s.radFound = found;
	}

	static BoardPlant snapshot(ServerLevel level, State s) {
		EquipmentBlockEntity vessel = loaded(level, s.vessel);
		BoardPlant p = vessel != null && vessel.kind().reactor() ? BoardPlant.of(vessel) : new BoardPlant();
		for (int k = 0; k < 2; k++) {
			EquipmentBlockEntity edg = loaded(level, s.edg[k]);
			if (edg != null) {
				p.edg[k].present = true;
				p.edg[k].running = edg.running;
				p.edg[k].since = edg.runningSince;
				// (Out of fuel or of starting air: either way it failed to start, round 31.)
				p.edg[k].fuelOut = edg.fuelOut || edg.airFail;
				p.edg[k].day = edg.dayLevel;
				// Round 31 part 2: its breaker as its panel has it, and whether it answers the board (MODE at AUTO).
				p.edg[k].breakerAt = edg.breakerAt;
				p.edg[k].auto = DieselPanels.remoteReady(edg);
			}
			EquipmentBlockEntity mcc = loaded(level, s.mcc[k]);
			if (mcc != null && mcc.bus != null) {
				p.mcc[k] = true;
				p.mccCharge[k] = mcc.bus.getAmount() / (double) mcc.busCapacity();
			}
		}
		// Round 28: the control room HVAC panel and the radiation monitors.
		if (s.hvPanel != null && level.isLoaded(s.hvPanel) && level.getBlockEntity(s.hvPanel) instanceof LivePanelBlockEntity panel) {
			p.hvFound = true;
			p.hvState = panel.hvState;
			p.hvAlarms = panel.hvAlarms;
			p.hvMode = Math.floorMod(panel.mode, 3);
		}
		if (s.csrPanel != null && level.isLoaded(s.csrPanel) && level.getBlockEntity(s.csrPanel) instanceof LivePanelBlockEntity panel) {
			p.csrFound = true;
			p.csrPhase = panel.csrPhase;
			p.csrFlags = panel.csrFlags;
		}
		System.arraycopy(s.rad, 0, p.rad, 0, BoardPlant.RAD_MONITORS);
		p.radFound = s.radFound;
		// Towers and pump houses build: the lake intake's service water (its pumps, the trains flowing).
		BlockPos near = s.vessel != null ? s.vessel : s.edg[0];
		p.sw = near == null ? -1 : WaterWorks.boardState(level, near);
		EquipmentBlockEntity lc = loaded(level, s.lc);
		if (lc != null && lc.bus != null) {
			p.lc = true;
			p.lcCharge = lc.bus.getAmount() / (double) lc.busCapacity();
			p.lcMain = Switchgear.carrying(lc, 0);
			for (int b = 1; b < Switchgear.BREAKERS; b++) {
				if (!"SPARE".equals(Switchgear.NAMES[b]) && !Switchgear.carrying(lc, b)) {
					p.lcOpen++;
				}
			}
		}
		return p;
	}

	private static @Nullable EquipmentBlockEntity loaded(Level level, @Nullable BlockPos pos) {
		return pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EquipmentBlockEntity e ? e : null;
	}

	/**
	 * The generator breaker's synchronizing: armed by its close switch, it closes when the synchroscope comes round to
	 * 12 o'clock with the turbine latched at speed (and disarms if the turbine trips or slows). Called by the reactor's
	 * vessel each tick.
	 */
	public static void synchronize(ReactorCore core, boolean steamToPower, long now) {
		if (!core.syncArmed) {
			return;
		}
		// (and a grid at the switchyard to close onto: diesels make real power)
		boolean ready = steamToPower && !core.turbineTripped && !core.scrammed && core.turbineSpeed >= 0.99 && (core.power & ReactorCore.PW_GRID) != 0;
		if (!ready) {
			core.syncArmed = false;
			return;
		}
		double phase = (now * 4.5) % 360;
		if (phase < 9 || phase > 351) {
			core.genOpen = false;
			core.syncArmed = false;
		}
	}

	// ------------------------------------------------------------------ the controls

	/**
	 * Where the player is looking on the board's desk: across (x) and from its front edge to its back (s), or null when
	 * the look misses the desk (the board's frame blocks are whole cubes, so the click's own spot is not the desk's: the
	 * look is followed down onto the sloping desk, a little above it where the switches' handles stand).
	 */
	static double @Nullable [] desk(EquipmentBlockEntity e, Vec3 eye, Vec3 look) {
		Vec3 l = Switchgear.local(e, eye);
		net.minecraft.core.Direction facing = e.facing(), right = facing.getClockWise();
		double dx = look.x * right.getStepX() + look.z * right.getStepZ(), dy = look.y, dz = look.x * facing.getStepX() + look.z * facing.getStepZ();
		// The desk's surface: from its front edge (y 0.86, z 0.12) up to the board (y 1.14, z 1.2); its unit tangent and
		// its normal (up, toward the operators).
		double len = Math.hypot(0.28, 1.08), ty = 0.28 / len, tz = 1.08 / len, ny = tz, nz = -ty, h = 0.02;
		double ry = l.y - 0.86, rz = l.z - 0.12;
		double dn = dy * ny + dz * nz;
		if (dn > -1e-6) {
			return null;
		}
		double lambda = (h - (ry * ny + rz * nz)) / dn;
		if (lambda <= 0 || lambda > 8) {
			return null;
		}
		double qy = ry + lambda * dy, qz = rz + lambda * dz, x = l.x + lambda * dx;
		double s = (qy * ty + qz * tz) / len;
		if (s < -0.03 || s > 1.03 || x < 0 || x > e.kind().wide) {
			return null;
		}
		return new double[] {x, s};
	}

	/**
	 * Where the player is looking on a vertical panel's face (the remote shutdown panel, round 29): across (x) and up
	 * (y), where the look meets the plane its switches' handles stand in, or null when it misses the face.
	 */
	static double @Nullable [] face(EquipmentBlockEntity e, Vec3 eye, Vec3 look) {
		Vec3 l = Switchgear.local(e, eye);
		net.minecraft.core.Direction facing = e.facing(), right = facing.getClockWise();
		double dx = look.x * right.getStepX() + look.z * right.getStepZ(), dy = look.y, dz = look.x * facing.getStepX() + look.z * facing.getStepZ();
		if (dz < 1e-6) {
			return null;
		}
		double lambda = (0.27 - l.z) / dz;
		if (lambda <= 0 || lambda > 8) {
			return null;
		}
		double x = l.x + lambda * dx, y = l.y + lambda * dy;
		return x < 0 || x > e.kind().wide || y < 0 || y > e.kind().tall ? null : new double[] {x, y};
	}

	/** The working control the player is looking at on a live board (its desk, or a vertical panel's face), or null. */
	public static @Nullable Control control(EquipmentBlockEntity e, Player player) {
		Layout layout = layout(e.kind());
		if (layout == null) {
			return null;
		}
		double[] at = e.kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL ? face(e, player.getEyePosition(), player.getViewVector(1.0F))
				: desk(e, player.getEyePosition(), player.getViewVector(1.0F));
		if (at == null) {
			return null;
		}
		for (Control c : layout.controls()) {
			if (at[0] >= c.x0() && at[0] <= c.x1() && at[1] >= c.s0() && at[1] <= c.s1()) {
				return c;
			}
		}
		return null;
	}

	/** Right-clicking a live board: a working control at that spot, or else what the board shows of the plant. */
	public static void use(Level level, EquipmentBlockEntity e, Player player, @Nullable Vec3 hit) {
		if (level.isClientSide() || !(level instanceof ServerLevel server)) {
			return;
		}
		Layout layout = layout(e.kind());
		if (layout == null) {
			return;
		}
		State s = e.board();
		s.size(layout);
		long now = level.getGameTime();
		find(server, e, s, now);
		s.plant = snapshot(server, s);
		Control control = control(e, player);
		if (control == null) {
			info(player, s.plant);
			return;
		}
		act(server, e, s, layout, player, control.id(), now);
	}

	static void info(Player player, BoardPlant p) {
		if (!p.present) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_reactor", StatusScreenBlockEntity.RANGE));
			return;
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_info", Math.round(p.fission * 100), Math.round(p.tavg()),
				Math.round(p.pzrPress()), Math.round(p.value("gen.mw", 0))));
	}

	/** Works a control (also used by tests); whether it did anything. */
	public static boolean act(ServerLevel level, EquipmentBlockEntity e, State s, Layout layout, Player player, String id, long now) {
		Vec3 m = e.middle();
		switch (id) {
			case "ack" -> {
				click(level, m, 1.4F);
				if (acknowledge(s, layout, s.plant, now)) {
					e.changed();
				}
				return true;
			}
			case "reset" -> {
				click(level, m, 1.4F);
				if (reset(s)) {
					e.changed();
				}
				return true;
			}
			case "test" -> {
				click(level, m, 1.4F);
				s.testUntil = now + TEST;
				e.changed();
				return true;
			}
			default -> {
			}
		}
		if (id.startsWith("edg_")) {
			int k = id.charAt(4) - 'a';
			EquipmentBlockEntity edg = loaded(level, s.edg[k]);
			if (edg == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_edg", k == 0 ? "A" : "B", REACH));
				return false;
			}
			click(level, m, 1.4F);
			boolean run = id.endsWith("start");
			if (edg.running == run) {
				player.sendOverlayMessage(Component.translatable(run ? "message.jugcraft.nuclear.board_edg_running" : "message.jugcraft.nuclear.board_edg_stopped",
						k == 0 ? "A" : "B"));
				return false;
			}
			// (Round 31 part 2: only with its MODE at AUTO, its 86G and emergency stop reset; it says why not.)
			return EquipmentPlacement.dieselSwitch(level, edg, player, run);
		}
		EquipmentBlockEntity vessel = loaded(level, s.vessel);
		if (vessel == null || !vessel.kind().reactor()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_reactor", StatusScreenBlockEntity.RANGE));
			return false;
		}
		if (!vessel.mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.not_owner"));
			return false;
		}
		ReactorCore core = vessel.core;
		ReactorUnit unit = vessel.unit();
		boolean steam = unit != null && unit.steamToPower();
		boolean done = false;
		boolean panel = e.kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL;
		boolean transferred = id.startsWith("afw_") || id.startsWith("fcv") || id.startsWith("arv");
		if (e.kind() == EquipmentKind.CONTROL_CONSOLE && core.remote && !id.startsWith("rx_trip")) {
			// Round 30: while a remote shutdown panel has control the console can only trip the reactor.
			click(level, m, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
			return false;
		}
		if (transferred && panel && !core.remote) {
			// The panel's own controls work once its transfer keys have taken control (round 29).
			click(level, m, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rsp_not_local"));
			return false;
		}
		if (transferred && !panel && core.remote) {
			// And while it has control, the control room's boards have lost them.
			click(level, m, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
			return false;
		}
		if (id.startsWith("rcp")) {
			int k = id.charAt(3) - '1';
			String name = "1" + (char) ('A' + k);
			if (vessel.kind() == EquipmentKind.SMALL_MODULAR_REACTOR) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_pumps"));
				return false;
			}
			if (k >= core.loops) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rcp_none", name, k + 1));
				return false;
			}
			boolean stopped = (core.rcpStopped & 1 << k) != 0;
			if (stopped && core.remote) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
				return false;
			}
			if (stopped && !core.plantBus()) {
				// (Diesels make real power: its bus dead with the grid lost and the generator off.)
				click(level, m, 1.0F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rcp_no_power", name));
				return false;
			}
			core.rcpStopped ^= 1 << k;
			player.sendOverlayMessage(Component.translatable(stopped ? "message.jugcraft.nuclear.board_rcp_start" : "message.jugcraft.nuclear.board_rcp_stop", name));
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, stopped ? 0.9F : 0.7F);
			done = true;
		} else if (id.equals("res_a") || id.equals("res_b")) {
			// Diesels make real power: the reserve auxiliary transformer's breaker to its safety bus (the test switch).
			// Opened, the bus loses the grid and its diesel (at AUTO) starts and takes it; closed with the grid there, the
			// bus is back on the grid and its diesel comes off it (PlantPower), running unloaded until it is stopped.
			int k = id.charAt(4) - 'a';
			String name = k == 0 ? "A" : "B";
			boolean open = (core.resOpen & 1 << k) != 0;
			if (open && (core.power & ReactorCore.PW_GRID) == 0) {
				click(level, m, 1.0F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_res_no_grid", name));
				return false;
			}
			core.resOpen ^= 1 << k;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, open ? 0.9F : 0.7F);
			player.sendOverlayMessage(Component.translatable(open ? "message.jugcraft.nuclear.board_res_closed" : "message.jugcraft.nuclear.board_res_opened",
					name));
			done = true;
		} else if (id.equals("transfer")) {
			// A transfer key: control of the unit's transferable equipment to this panel, or back to the control room.
			core.remote = !core.remote;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, core.remote ? 1.2F : 0.9F);
			player.sendOverlayMessage(Component.translatable(core.remote ? "message.jugcraft.nuclear.panel_control_here"
					: "message.jugcraft.nuclear.panel_control_returned"));
			e.linkShutdownPanel(level);
			done = true;
		} else if (id.equals("rsp_trip_a") || id.equals("rsp_trip_b")) {
			// The panel's reactor trips under their covers: the first click lifts the cover, the second trips.
			boolean b = id.equals("rsp_trip_b");
			if (now > (b ? s.guardBUntil : s.guardUntil)) {
				if (b) {
					s.guardBUntil = now + GUARD;
				} else {
					s.guardUntil = now + GUARD;
				}
				level.playSound(null, m.x, m.y, m.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rsp_guard", b ? "B" : "A"));
				e.changed();
				return true;
			}
			if (b) {
				s.guardBUntil = 0;
			} else {
				s.guardUntil = 0;
			}
			if (core.scrammed) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rsp_already"));
				e.changed();
				return false;
			}
			core.trip(ReactorCore.TRIP_MANUAL);
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.9F, 0.6F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.panel_tripped"));
			done = true;
		} else if (id.equals("rx_trip_a") || id.equals("rx_trip_b")) {
			// Round 30: the console's reactor trip switches under their guards: the first click lifts the guard, the
			// second turns the switch to TRIP, or with the reactor tripped to CLOSE (the trip breakers closed again: the
			// trip reset, once the core is below its alarm temperature and safety injection is reset).
			boolean b = id.equals("rx_trip_b");
			if (now > (b ? s.guardBUntil : s.guardUntil)) {
				if (b) {
					s.guardBUntil = now + GUARD;
				} else {
					s.guardUntil = now + GUARD;
				}
				level.playSound(null, m.x, m.y, m.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
				player.sendOverlayMessage(Component.translatable(core.scrammed ? "message.jugcraft.nuclear.board_rx_guard_close"
						: "message.jugcraft.nuclear.board_rx_guard", b ? "B" : "A"));
				e.changed();
				return true;
			}
			if (b) {
				s.guardBUntil = 0;
			} else {
				s.guardUntil = 0;
			}
			if (core.scrammed) {
				if (core.remote) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
					e.changed();
					return false;
				}
				if (!core.reset()) {
					player.sendOverlayMessage(Component.translatable(core.siActuated ? "message.jugcraft.nuclear.cannot_reset_si"
							: "message.jugcraft.nuclear.cannot_reset"));
					e.changed();
					return false;
				}
				level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.9F, 1.1F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rx_closed"));
			} else {
				core.trip(ReactorCore.TRIP_MANUAL);
				level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.9F, 0.6F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rx_tripped"));
			}
			done = true;
		} else if (id.equals("rods_in") || id.equals("rods_out")) {
			// The rods' lever, held (the game uses it again every few ticks while the player holds the button down).
			int dir = id.equals("rods_out") ? 1 : -1;
			if (core.rodAuto) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rods_in_auto"));
				return false;
			}
			if (core.scrammed || core.fuelDamaged) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rods_tripped"));
				return false;
			}
			if (core.refuelOpen) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rods_refuel"));
				return false;
			}
			if (now > s.leverUntil) {
				level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, 1.3F);
			}
			core.drive(dir);
			s.lever = dir;
			s.leverUntil = now + 6;
			// The leading bank moving (the last of the overlap's banks between its ends: either way, the latest one), and
			// where it is.
			int moving = core.bankSteps(7) >= ReactorCore.STEPS ? 7 : 4;
			for (int k = 4; k < 8; k++) {
				double steps = core.bankSteps(k);
				if (steps > 0 && steps < ReactorCore.STEPS) {
					moving = k;
				}
			}
			player.sendOverlayMessage(Component.translatable(dir > 0 ? "message.jugcraft.nuclear.board_rods_out" : "message.jugcraft.nuclear.board_rods_in",
					String.valueOf((char) ('A' + moving - 4)), Math.round(core.bankSteps(moving)), Math.round(core.fission * 100)));
			done = true;
		} else if (id.equals("rod_mode")) {
			click(level, m, 1.1F);
			if (core.scrammed && !core.rodAuto) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_rods_tripped"));
				return false;
			}
			core.rodControl(!core.rodAuto);
			player.sendOverlayMessage(core.rodAuto ? Component.translatable("message.jugcraft.nuclear.board_rods_auto",
					String.format(java.util.Locale.ROOT, "%.1f", core.autoSetpoint * 100)) : Component.translatable("message.jugcraft.nuclear.board_rods_man"));
			done = true;
		} else if (id.equals("sr_block") || id.equals("ir_block") || id.equals("pr_block")) {
			// The nuclear instruments' trip blocks: pressed above their permissive (P-6 for the source range, P-10 for
			// the others) they block, pressed again they reset; below it they clear by themselves.
			click(level, m, 1.3F);
			boolean sr = id.equals("sr_block"), ir = id.equals("ir_block");
			boolean blocked = sr ? core.srBlocked : ir ? core.irBlocked : core.prLoBlocked;
			String name = sr ? "SOURCE RANGE" : ir ? "INTERMEDIATE RANGE" : "POWER RANGE LOW SETPOINT";
			if (!blocked && core.nis < (sr ? ReactorCore.P6 : ReactorCore.P10)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_block_wait", name, sr ? "P-6" : "P-10"));
				return false;
			}
			// The operator has the blocks now (not the console's screen).
			core.screenRods = false;
			if (sr) {
				core.srBlocked = !blocked;
			} else if (ir) {
				core.irBlocked = !blocked;
			} else {
				core.prLoBlocked = !blocked;
			}
			player.sendOverlayMessage(Component.translatable(blocked ? "message.jugcraft.nuclear.board_block_reset" : "message.jugcraft.nuclear.board_block_on", name));
			done = true;
		} else if (id.equals("rps")) {
			// The reactor protection switch (the console screen's protection switch): NORMAL or BYPASS.
			core.protection = !core.protection;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, core.protection ? 0.9F : 0.7F);
			player.sendOverlayMessage(Component.translatable(core.protection ? "message.jugcraft.nuclear.board_rps_normal" : "message.jugcraft.nuclear.board_rps_bypass"));
			done = true;
		} else if (id.startsWith("fcv") || id.startsWith("arv")) {
			// An aux feed flow control valve, or a generator's ARV opened or closed by hand.
			int k = id.charAt(3) - '1';
			if (vessel.kind() == EquipmentKind.SMALL_MODULAR_REACTOR || k < 0 || k >= core.loops) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_sg", k + 1));
				return false;
			}
			boolean fcv = id.startsWith("fcv");
			boolean open;
			if (fcv) {
				core.afwValveShut ^= 1 << k;
				open = (core.afwValveShut & 1 << k) == 0;
			} else {
				core.arvManual ^= 1 << k;
				open = (core.arvManual & 1 << k) != 0;
			}
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, open ? 0.9F : 0.7F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_" + (fcv ? "fcv" : "arv") + (open ? "_open" : "_shut"), k + 1));
			done = true;
		} else if (id.equals("si_a") || id.equals("si_b")) {
			// Manual safety injection under its guard: the first click lifts the guard, the second actuates.
			boolean b = id.equals("si_b");
			if (now > (b ? s.guardBUntil : s.guardUntil)) {
				if (b) {
					s.guardBUntil = now + GUARD;
				} else {
					s.guardUntil = now + GUARD;
				}
				level.playSound(null, m.x, m.y, m.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_si_guard", b ? "B" : "A"));
				e.changed();
				return true;
			}
			if (b) {
				s.guardBUntil = 0;
			} else {
				s.guardUntil = 0;
			}
			if (core.siActuated) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_si_already"));
				e.changed();
				return false;
			}
			core.safetyInjection(now);
			// Both diesel generators start.
			for (int k = 0; k < 2; k++) {
				EquipmentBlockEntity edg = loaded(level, s.edg[k]);
				if (edg != null && !edg.running) {
					EquipmentPlacement.dieselSwitch(level, edg, null, true);
				}
			}
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.9F, 0.6F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_si_actuated", b ? "B" : "A"));
			done = true;
		} else if (id.equals("si_reset_a") || id.equals("si_reset_b")) {
			click(level, m, 1.2F);
			if (!core.siActuated) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_si_not"));
				return false;
			}
			if (!core.resetSafetyInjection(now)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_si_reset_wait",
						Math.max(1, (core.siTime + ReactorCore.SI_RESET - now + 19) / 20)));
				return false;
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_si_reset"));
			done = true;
		} else if (id.startsWith("msiv")) {
			int k = id.charAt(4) - '1';
			if (vessel.kind() == EquipmentKind.SMALL_MODULAR_REACTOR || k < 0 || k >= core.loops) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_sg", k + 1));
				return false;
			}
			boolean closed = (core.msivClosed & 1 << k) != 0;
			core.msivClosed ^= 1 << k;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, closed ? 0.9F : 0.7F);
			player.sendOverlayMessage(Component.translatable(closed ? "message.jugcraft.nuclear.board_msiv_open" : "message.jugcraft.nuclear.board_msiv_closed",
					k + 1));
			done = true;
		} else if (id.equals("afw_a") || id.equals("afw_b") || id.equals("afw_td")) {
			int pump = id.equals("afw_a") ? 0 : id.equals("afw_b") ? 1 : 2;
			String name = pump == 0 ? "A" : pump == 1 ? "B" : "TD";
			if (vessel.kind() == EquipmentKind.SMALL_MODULAR_REACTOR || core.loops == 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_afw"));
				return false;
			}
			boolean running = (core.afwRunning & 1 << pump) != 0;
			core.afwRunning ^= 1 << pump;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, running ? 0.7F : 0.9F);
			player.sendOverlayMessage(Component.translatable(running ? "message.jugcraft.nuclear.board_afw_stop" : "message.jugcraft.nuclear.board_afw_start", name));
			done = true;
		} else if (id.equals("mfp_a") || id.equals("mfp_b")) {
			// Turbine hall part 2: the hall's main feed pumps start and stop from here (a tripped one is reset at its
			// station first); a plant with none at its hall has its main feed run by itself.
			int k = id.charAt(4) - 'a';
			String name = k == 0 ? "A" : "B";
			if (!core.mfpPresent(k)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_mfp", name));
				return false;
			}
			if ((core.mfpTripped & 1 << k) != 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_mfp_tripped", name));
				return false;
			}
			boolean running = (core.mfpRunning & 1 << k) != 0;
			core.mfpRunning ^= 1 << k;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.7F, running ? 0.7F : 0.9F);
			player.sendOverlayMessage(Component.translatable(running ? "message.jugcraft.nuclear.board_mfp_stop"
					: core.temperature > ReactorCore.MFP_STEAM && steam ? "message.jugcraft.nuclear.board_mfp_start" : "message.jugcraft.nuclear.board_mfp_no_steam", name));
			done = true;
		} else if (id.equals("turbine_trip")) {
			if (!steam) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_turbine"));
				return false;
			}
			if (now > s.guardUntil) {
				s.guardUntil = now + GUARD;
				level.playSound(null, m.x, m.y, m.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.5F, 1.4F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_guard"));
				e.changed();
				return true;
			}
			s.guardUntil = 0;
			if (core.turbineTripped) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_turbine_already"));
				e.changed();
				return false;
			}
			core.turbineTripped = true;
			core.genOpen = true;
			core.syncArmed = false;
			level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.9F, 0.6F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_turbine_tripped"));
			done = true;
		} else if (id.equals("latch")) {
			click(level, m, 1.2F);
			if (!steam) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_turbine"));
				return false;
			}
			if (!core.turbineTripped) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_latch_already"));
				return false;
			}
			if (core.remote) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
				return false;
			}
			if (core.scrammed) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_latch_blocked"));
				return false;
			}
			core.turbineTripped = false;
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_latched"));
			done = true;
		} else if (id.equals("gen_bkr")) {
			if (!steam) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_no_turbine"));
				return false;
			}
			if (core.electric > 0 || core.syncArmed) {
				core.genOpen = true;
				core.syncArmed = false;
				level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.7F);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_gen_open"));
			} else {
				level.playSound(null, m.x, m.y, m.z, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.8F, 0.9F);
				if (core.remote) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.control_at_panel"));
					return false;
				}
				if (core.turbineTripped || core.scrammed || core.turbineSpeed < 0.99) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_gen_blocked", Math.round(core.turbineSpeed * 1800)));
					return false;
				}
				if (core.fission < 0.01) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_gen_no_steam"));
					return false;
				}
				if ((core.power & ReactorCore.PW_GRID) == 0) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_gen_no_grid"));
					return false;
				}
				if (!core.genOpen) {
					player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_gen_closed"));
					return false;
				}
				core.syncArmed = true;
				player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear.board_gen_sync"));
			}
			done = true;
		}
		if (done) {
			vessel.changed();
			s.plant = snapshot(level, s);
			e.changed();
		}
		return done;
	}

	private static void click(Level level, Vec3 m, float pitch) {
		level.playSound(null, m.x, m.y, m.z, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, pitch);
	}
}
