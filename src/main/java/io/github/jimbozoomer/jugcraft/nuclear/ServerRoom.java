package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The control building's plant computer room (older rooms round 2; Justin: racks "Real 3D IT racks", working "Rack
 * lights show traffic", "Plant computer console", "UPS and power", "Room cooling"; fittings "Clean-agent fire system",
 * "Overhead cable ladder", "Cooled floor and CRACs", "Signs and labels"; rack doors "Open on click"; room power "Fed from
 * load centres"; tools/server_room.py).
 *
 * <p>The room's hub is its UPS module (a live panel). The room only looks live (Justin, 25 October 2026: "make it look like
 * it works, but it doesn't actually need to be calculating any kind of live numbers"; for these pieces wherever they
 * stand): nothing in it ticks on the server. The hub keeps what the room's pieces draw (client/EdgTelemetry.java srv),
 * changed only when something happens, and the players' clients draw the numbers from it:
 * <ul>
 *   <li>power: the UPS always on its input (its battery full, the racks fed; no load centre is read);</li>
 *   <li>cooling: each CRAC runs while its isolator is on (a click on it); the room's air is worked out where it is drawn
 *   from where it stood when its cooling last changed ({@link #temp}): with both running it holds the setpoint, with
 *   one a little over, with none it climbs past {@link #HIGH_TEMP} in a few minutes (the racks' status lights amber,
 *   the CRACs' alarm);</li>
 *   <li>the clean agent (FM-200) system: a fire put down in reach of the hub (mixin/ServerRoomFireMixin) has it look at
 *   the room's smoke detectors; one seeing a fire puts it in ALARM, two (zone 1 and zone 2) start the {@link #PRE}
 *   second pre-discharge countdown (the horn faster, the strobe flashing); the abort station held (a click holds it
 *   {@link #ABORT_HOLD} seconds) holds the countdown, which starts again when it lets go; at zero the agent floods the
 *   room (every fire in it out, the cylinders empty) and the panel waits for RESET (new cylinders). The manual release
 *   station starts the countdown at once and no abort stops it. DRILL runs the alarm and countdown without a
 *   discharge; SILENCE quiets the horn; ACK acknowledges. Only while the alarm, a countdown or a drill runs does the
 *   hub tick, once a second (a scheduled block tick), and stops when it ends.</li>
 * </ul>
 * The racks' numbers (A-01 ...) are worked out from where they stand (their rows, west to east), and each piece's hub
 * (the nearest in reach), on the clients that draw them.
 */
public final class ServerRoom {
	public static final String[] RACKS = {"server_rack", "network_rack", "san_rack", "ppc_rack"};
	public static final String UPS = "ups_module", BATT = "ups_battery", MBP = "ups_bypass", PDU = "pdu_cabinet", CRAC = "crac_unit",
			CYL = "agent_cylinder", PANEL = "agent_release_panel", HORN = "agent_horn_strobe", CONSOLE = "ppc_console";
	public static final int RACK = 0, HUB = 1, BATTERY = 2, BYPASS = 3, DIST = 4, COOLER = 5, CYLINDER = 6, RELEASE = 7, ALARM_HORN = 8,
			PPC = 9;
	private static final String[] KINDS = {null, UPS, BATT, MBP, PDU, CRAC, CYL, PANEL, HORN, CONSOLE};
	/** How far a piece looks for its hub, the hub for the room's racks, CRACs and detectors, the load centres. */
	public static final int REACH = 24, HIGH = 8, LC_REACH = 64;
	/** The load centre breakers it is fed from: the UPS's input (09 LIGHTING), the CRACs' (08 CR HVAC). */
	public static final int UPS_BREAKER = 8, CRAC_BREAKER = 7;
	/** The UPS battery's run time at the room's load (seconds), and its recharge (seconds, flat to full). */
	public static final int RUNTIME = 15 * 60, RECHARGE = 10 * 60;
	/** The room's air: the CRACs' setpoint, where it settles with one CRAC, where it heads with none, the alarm (degrees C). */
	public static final double SETPOINT = 22.0, ROOM = 21.8, ONE_CRAC = 23.4, NO_COOLING = 45, HIGH_TEMP = 27.0;
	/** How fast it warms (no cooling) and cools (seconds: the time constants). */
	public static final double TAU_WARM = 1800, TAU_COOL = 300;
	/** The agent: the cylinders' pressure (psig at room temperature), the pre-discharge (seconds), how long a click holds the abort. */
	public static final double PSI = 360;
	public static final int PRE = 30, ABORT_HOLD = 5, DRILL_TIME = 30;
	/** A detector sees fire this far across and down. */
	static final int SEE = 4, BELOW = 7;
	// The power bits: input live, output on, on battery, CRAC-1 and CRAC-2 fed and running, no load centre found, the room hot.
	public static final int IN_OK = 1, OUT_ON = 2, ON_BATT = 4, CRAC1_FED = 8, CRAC2_FED = 16, CRAC1_RUN = 32, CRAC2_RUN = 64, NO_LC = 128,
			HOT = 256;
	// The agent system's phases and flags.
	public static final int NORMAL = 0, ALARM = 1, PRE_DISCHARGE = 2, RELEASED = 3, DRILL = 4;
	public static final int SILENCED = 1, ACKED = 2, MANUAL = 4, ABORTED = 8, NO_DETECTORS = 16, NO_CYLINDERS = 32;

	private ServerRoom() {
	}

	/** Which piece a block is (RACK to PPC), or -1. */
	public static int kind(BlockState state) {
		Block b = state.getBlock();
		for (String r : RACKS) {
			if (b == PlantInterior.BLOCKS.get(r)) {
				return RACK;
			}
		}
		for (int k = 1; k < KINDS.length; k++) {
			if (b == PlantInterior.BLOCKS.get(KINDS[k])) {
				return k;
			}
		}
		return -1;
	}

	// ------------------------------------------------------------------------------------------------ what the hub keeps

	/** The power bits the room always has: its UPS on its input, its output on, both CRACs fed. */
	public static final int FED = IN_OK | OUT_ON | CRAC1_FED | CRAC2_FED;
	/** How long the hub's list of detectors and cylinders holds before it looks again (when it next needs it; ticks). */
	static final int RESCAN = 200;
	/** The hubs loaded in each level, for a fire to find (LivePanelBlockEntity adds and drops them as they load and go). */
	private static final Map<ServerLevel, Set<BlockPos>> HUBS = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

	static void loaded(ServerLevel level, BlockPos hub) {
		HUBS.computeIfAbsent(level, l -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(hub.immutable());
	}

	static void unloaded(ServerLevel level, BlockPos hub) {
		Set<BlockPos> hubs = HUBS.get(level);
		if (hubs != null) {
			hubs.remove(hub);
		}
	}

	/**
	 * The room's air at game time now (fractional on the client): from where it stood when its cooling last changed
	 * ({@link LivePanelBlockEntity#srvTemp} at {@link LivePanelBlockEntity#srvTempAt}), heading for where the running
	 * CRACs hold it (warming slowly, cooling fast).
	 */
	public static double temp(LivePanelBlockEntity hub, double now) {
		int running = Integer.bitCount(hub.srvBits & (CRAC1_RUN | CRAC2_RUN));
		double target = running >= 2 ? ROOM : running == 1 ? ONE_CRAC : NO_COOLING;
		double from = hub.srvTemp;
		double tau = target > from ? TAU_WARM : TAU_COOL;
		return target + (from - target) * Math.exp(-Math.max(0, now - hub.srvTempAt) / 20.0 / tau);
	}

	/** The hub's power bits at game time now: always fed, its CRACs as their isolators stand, HOT with the room past the alarm. */
	public static int bits(LivePanelBlockEntity hub, double now) {
		return FED | hub.srvBits & (CRAC1_RUN | CRAC2_RUN) | (temp(hub, now) >= HIGH_TEMP ? HOT : 0);
	}

	/** The CRACs running changed: the air carries on from where it is now towards their new hold. */
	static void cooling(LivePanelBlockEntity hub, long now, int run) {
		if ((hub.srvBits & (CRAC1_RUN | CRAC2_RUN)) != run || (hub.srvBits & ~(CRAC1_RUN | CRAC2_RUN)) != FED) {
			hub.srvTemp = temp(hub, now);
			hub.srvTempAt = now;
			hub.srvBits = FED | run;
		}
	}

	/** Which CRACs in the hub's reach have their isolators on. */
	static int running(LivePanelBlockEntity hub, ServerLevel level) {
		int run = 0;
		for (LivePanelBlockEntity crac : ControlBoard.panels(level, hub.getBlockPos(), PlantInterior.BLOCKS.get(CRAC), REACH)) {
			if (crac.mode == 0) {
				run |= LivePanelBlockEntity.variant(crac.getBlockState()) == 0 ? CRAC1_RUN : CRAC2_RUN;
			}
		}
		return run;
	}

	/** A CRAC's isolator switched (ServerRoomPanelBlock): its hub's cooling follows. */
	public static void isolator(ServerLevel level, BlockPos crac) {
		LivePanelBlockEntity hub = nearestHub(level, crac);
		if (hub != null) {
			cooling(hub, level.getGameTime(), running(hub, level));
			hub.syncNow(level);
		}
	}

	static @Nullable LivePanelBlockEntity nearestHub(Level level, BlockPos at) {
		List<LivePanelBlockEntity> hubs = ControlBoard.panels(level, at, PlantInterior.BLOCKS.get(UPS), REACH);
		hubs.sort(Comparator.comparingDouble(p -> p.getBlockPos().distSqr(at)));
		return hubs.isEmpty() ? null : hubs.get(0);
	}

	/** The hub a piece keeps to, as the clients draw it: the nearest in reach (the hub itself for the UPS module). */
	public static @Nullable LivePanelBlockEntity hubOf(Level level, LivePanelBlockEntity piece) {
		return kind(piece.getBlockState()) == HUB ? piece : nearestHub(level, piece.getBlockPos());
	}

	/** What the hub keeps to: the room's CRACs, detectors and cylinders (looked for when it needs them). */
	static void scan(LivePanelBlockEntity hub, ServerLevel level) {
		BlockPos here = hub.getBlockPos();
		cooling(hub, level.getGameTime(), running(hub, level));
		List<BlockPos> detectors = new ArrayList<>();
		int cylinders = 0;
		for (BlockPos p : BlockPos.betweenClosed(here.offset(-REACH, -HIGH, -REACH), here.offset(REACH, HIGH, REACH))) {
			if (!level.isLoaded(p)) {
				continue;
			}
			BlockState s = level.getBlockState(p);
			if (s.getBlock() == PlantInterior.BLOCKS.get("smoke_detector")) {
				detectors.add(p.immutable());
			} else if (s.getBlock() == PlantInterior.BLOCKS.get(CYL)) {
				cylinders++;
			}
		}
		hub.srvDetectors = detectors;
		hub.srvScanAt = level.getGameTime();
		int flags = hub.srvFlags & ~(NO_DETECTORS | NO_CYLINDERS);
		hub.srvFlags = flags | (detectors.isEmpty() ? NO_DETECTORS : 0) | (cylinders == 0 ? NO_CYLINDERS : 0);
	}

	/** The hub's detectors, looked for again if it has not looked lately. */
	static void fresh(LivePanelBlockEntity hub, ServerLevel level) {
		if (hub.srvScanAt < 0 || level.getGameTime() - hub.srvScanAt > RESCAN) {
			scan(hub, level);
		}
	}

	// ------------------------------------------------------------------------------------------------ when something happens

	/** A fire put down (mixin/ServerRoomFireMixin): each hub in reach looks at its detectors on its next second. */
	public static void fire(ServerLevel level, BlockPos at) {
		Set<BlockPos> hubs = HUBS.get(level);
		if (hubs == null || hubs.isEmpty()) {
			return;
		}
		for (BlockPos h : List.copyOf(hubs)) {
			if (Math.abs(h.getX() - at.getX()) > REACH + SEE || Math.abs(h.getZ() - at.getZ()) > REACH + SEE
					|| Math.abs(h.getY() - at.getY()) > HIGH + BELOW || !level.isLoaded(h)) {
				continue;
			}
			if (level.getBlockEntity(h) instanceof LivePanelBlockEntity hub && kind(hub.getBlockState()) == HUB
					&& (hub.srvPhase == NORMAL || hub.srvPhase == ALARM)) {
				wake(hub, level, 10);
			}
		}
	}

	/** The hub ticks in delay ticks (a scheduled block tick: one at a time). */
	static void wake(LivePanelBlockEntity hub, ServerLevel level, int delay) {
		level.scheduleTick(hub.getBlockPos(), hub.getBlockState().getBlock(), delay);
	}

	/** Whether the agent system is busy (its alarm, countdown or drill running): the hub ticks once a second while it is. */
	static boolean busy(LivePanelBlockEntity hub) {
		return hub.srvPhase == ALARM || hub.srvPhase == PRE_DISCHARGE || hub.srvPhase == DRILL;
	}

	/**
	 * The hub's tick (ServerRoomPanelBlock: scheduled, never on its own): put down (the room's pieces looked for), a fire
	 * near, or the agent system busy; it ticks again in a second while it is busy.
	 */
	public static void step(LivePanelBlockEntity hub, ServerLevel level) {
		int phase = hub.srvPhase, flags = hub.srvFlags, bits = hub.srvBits;
		fresh(hub, level);
		agent(hub, level, level.getGameTime());
		if (busy(hub)) {
			wake(hub, level, 20);
		}
		if (phase != hub.srvPhase || flags != hub.srvFlags || bits != hub.srvBits) {
			hub.syncNow(level);
		}
	}

	// ------------------------------------------------------------------------------------------------ the clean agent

	/** The detectors that see a fire (each looks across and down from where it is). */
	static int detecting(LivePanelBlockEntity hub, ServerLevel level, @Nullable Set<BlockPos> fires) {
		int n = 0;
		for (BlockPos d : hub.srvDetectors) {
			boolean sees = false;
			if (level.isLoaded(d)) {
				for (BlockPos p : BlockPos.betweenClosed(d.offset(-SEE, -BELOW, -SEE), d.offset(SEE, 0, SEE))) {
					if (level.isLoaded(p) && level.getBlockState(p).is(BlockTags.FIRE)) {
						sees = true;
						if (fires == null) {
							break;
						}
						fires.add(p.immutable());
					}
				}
			}
			n += sees ? 1 : 0;
		}
		return n;
	}

	static void agent(LivePanelBlockEntity hub, ServerLevel level, long now) {
		int seen = detecting(hub, level, null);
		long t = now - hub.srvSince;
		switch (hub.srvPhase) {
			case NORMAL -> {
				if (seen >= 2) {
					start(hub, now, PRE_DISCHARGE, 0);
				} else if (seen == 1) {
					start(hub, now, ALARM, 0);
				}
			}
			case ALARM -> {
				if (seen >= 2) {
					hub.srvPhase = PRE_DISCHARGE;
					hub.srvSince = now;
				}
			}
			case PRE_DISCHARGE -> {
				boolean held = hub.srvAbortUntil > now && (hub.srvFlags & MANUAL) == 0;
				if (held) {
					// The abort station held: the countdown waits, and starts again from the top when it lets go.
					hub.srvSince = now;
					hub.srvFlags |= ABORTED;
				} else {
					hub.srvFlags &= ~ABORTED;
					if (t >= PRE * 20L) {
						release(hub, level, now);
					}
				}
			}
			case DRILL -> {
				if (t >= (PRE + DRILL_TIME) * 20L) {
					hub.srvPhase = NORMAL;
					hub.srvSince = now;
					hub.srvFlags &= ~(SILENCED | ACKED | ABORTED);
				}
			}
			default -> {
			}
		}
		boolean sounding = (hub.srvPhase == ALARM || hub.srvPhase == PRE_DISCHARGE || hub.srvPhase == DRILL) && (hub.srvFlags & SILENCED) == 0;
		if (sounding) {
			BlockPos at = hub.getBlockPos();
			level.playSound(null, at, JugcraftNuclear.CO2_HORN, SoundSource.BLOCKS, 1.4F, hub.srvPhase == ALARM ? 1.0F : 1.25F);
		}
	}

	static void start(LivePanelBlockEntity hub, long now, int phase, int flags) {
		hub.srvPhase = phase;
		hub.srvSince = now;
		hub.srvFlags = (hub.srvFlags & ~(SILENCED | ACKED | MANUAL | ABORTED)) | flags;
	}

	/** The discharge: the agent floods the room, every fire in it is out; the cylinders are empty. */
	static void release(LivePanelBlockEntity hub, ServerLevel level, long now) {
		hub.srvPhase = RELEASED;
		hub.srvSince = now;
		hub.srvReleasedAt = level.getOverworldClockTime();
		hub.srvPsi = 0;
		hub.srvFlags &= ~ABORTED;
		fresh(hub, level);
		Set<BlockPos> fires = new HashSet<>();
		detecting(hub, level, fires);
		// The room: the detectors' spread (the agent fills it floor to ceiling).
		int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
		for (BlockPos d : hub.srvDetectors) {
			x0 = Math.min(x0, d.getX());
			z0 = Math.min(z0, d.getZ());
			x1 = Math.max(x1, d.getX());
			z1 = Math.max(z1, d.getZ());
			y1 = Math.max(y1, d.getY());
		}
		if (x0 <= x1) {
			BlockPos lo = new BlockPos(x0 - SEE, y1 - BELOW, z0 - SEE), hi = new BlockPos(x1 + SEE, y1, z1 + SEE);
			for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
				if (level.isLoaded(p) && level.getBlockState(p).is(BlockTags.FIRE)) {
					fires.add(p.immutable());
				}
			}
			for (Entity e : level.getEntities((Entity) null, new AABB(lo.getX(), lo.getY(), lo.getZ(), hi.getX() + 1, hi.getY() + 1, hi.getZ() + 1),
					Entity::isOnFire)) {
				e.clearFire();
			}
			// The nozzles' cloud.
			for (BlockPos d : hub.srvDetectors) {
				level.sendParticles(ParticleTypes.CLOUD, d.getX() + 0.5, d.getY() - 0.5, d.getZ() + 0.5, 30, 2.0, 1.5, 2.0, 0.05);
			}
		}
		for (BlockPos f : fires) {
			level.removeBlock(f, false);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, f.getX() + 0.5, f.getY() + 0.5, f.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
		}
		BlockPos at = hub.getBlockPos();
		level.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 0.6F);
		level.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 2.0F, 0.5F);
	}

	// ------------------------------------------------------------------------------------------------ the controls

	/** The releasing panel's keys (ServerRoomPanelBlock): 0 ACK, 1 SILENCE, 2 RESET, 3 DRILL. */
	public static void key(LivePanelBlockEntity hub, ServerLevel level, int key, @Nullable Player player) {
		long now = level.getGameTime();
		switch (key) {
			case 0 -> {
				if (hub.srvPhase == NORMAL) {
					tell(player, "srv_nothing");
				} else {
					hub.srvFlags |= ACKED;
					tell(player, "srv_acked");
				}
			}
			case 1 -> {
				if (hub.srvPhase == ALARM || hub.srvPhase == PRE_DISCHARGE || hub.srvPhase == DRILL) {
					hub.srvFlags |= SILENCED;
					tell(player, "srv_silenced");
				} else {
					tell(player, "srv_nothing");
				}
			}
			case 2 -> {
				scan(hub, level);
				if (detecting(hub, level, null) > 0) {
					tell(player, "srv_still_fire");
				} else {
					boolean was = hub.srvPhase != NORMAL;
					boolean refilled = hub.srvPhase == RELEASED;
					start(hub, now, NORMAL, 0);
					hub.srvPsi = PSI;
					tell(player, refilled ? "srv_refilled" : was ? "srv_reset" : "srv_normal");
				}
			}
			default -> {
				if (hub.srvPhase == NORMAL) {
					start(hub, now, DRILL, 0);
					tell(player, "srv_drill");
				} else {
					tell(player, "srv_busy");
				}
			}
		}
		if (busy(hub)) {
			wake(hub, level, 20);
		}
		hub.syncNow(level);
	}

	/** The abort station: held (each click {@link #ABORT_HOLD} seconds), the countdown waits. */
	public static void abort(ServerLevel level, BlockPos station, @Nullable Player player) {
		LivePanelBlockEntity hub = nearestHub(level, station);
		if (hub == null) {
			tell(player, "srv_no_panel");
			return;
		}
		long now = level.getGameTime();
		if (hub.srvPhase == PRE_DISCHARGE && (hub.srvFlags & MANUAL) != 0) {
			tell(player, "srv_abort_manual");
			return;
		}
		hub.srvAbortUntil = now + ABORT_HOLD * 20L;
		if (hub.srvPhase == PRE_DISCHARGE) {
			hub.srvSince = now;
			hub.srvFlags |= ABORTED;
			tell(player, "srv_abort_held");
		} else {
			tell(player, "srv_abort_idle");
		}
		hub.syncNow(level);
	}

	/** The manual release station: the countdown starts at once (no abort holds it). */
	public static void pull(ServerLevel level, BlockPos station, @Nullable Player player) {
		LivePanelBlockEntity hub = nearestHub(level, station);
		if (hub == null) {
			tell(player, "srv_no_panel");
			return;
		}
		if (hub.srvPhase == RELEASED) {
			tell(player, "srv_already");
			return;
		}
		if (hub.srvPhase == PRE_DISCHARGE && (hub.srvFlags & MANUAL) != 0) {
			tell(player, "srv_already_counting");
			return;
		}
		start(hub, level.getGameTime(), PRE_DISCHARGE, MANUAL);
		tell(player, "srv_pulled");
		wake(hub, level, 20);
		hub.syncNow(level);
	}

	private static void tell(@Nullable Player player, String key) {
		if (player != null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.nuclear." + key));
		}
	}

	// ------------------------------------------------------------------------------------------------ what it shows

	/** The releasing panel's display (two lines) for the hub's state at game time now. */
	public static String[] display(int phase, int flags, long since, double now, long releasedAt) {
		int left = Math.max(0, (int) Math.ceil(PRE - (now - since) / 20.0));
		return switch (phase) {
			case ALARM -> new String[] {"ALARM  ZONE 1", "AWAITING ZONE 2"};
			case PRE_DISCHARGE -> new String[] {(flags & MANUAL) != 0 ? "MANUAL RELEASE" : "ALARM  ZONE 1 + 2",
					(flags & ABORTED) != 0 ? "ABORT HELD" : String.format(Locale.ROOT, "RELEASE IN %d S", left)};
			case RELEASED -> new String[] {"AGENT RELEASED", SpreadingRoomFire.clock(releasedAt) + "  RESET"};
			case DRILL -> new String[] {"DRILL", now - since < PRE * 20L ? String.format(Locale.ROOT, "COUNTDOWN %d S", left) : "NO DISCHARGE"};
			default -> (flags & NO_DETECTORS) != 0 ? new String[] {"TROUBLE", "NO DETECTORS"}
					: (flags & NO_CYLINDERS) != 0 ? new String[] {"TROUBLE", "NO AGENT CYLINDERS"}
					: new String[] {"SYSTEM NORMAL", String.format(Locale.ROOT, "AGENT %.0f PSIG", PSI)};
		};
	}

	/** A rack's number: its row (A, B, ...: the rows of racks round it, north to south or west to east) and its place in it. */
	public static String label(Level level, LivePanelBlockEntity rack) {
		BlockPos here = rack.getBlockPos();
		Direction facing = rack.getBlockState().getValue(MultiFurnitureBlock.FACING);
		boolean alongX = facing.getAxis() == Direction.Axis.Z;
		Map<Integer, List<BlockPos>> rows = new HashMap<>();
		for (String r : RACKS) {
			for (LivePanelBlockEntity p : ControlBoard.panels(level, here, PlantInterior.BLOCKS.get(r), REACH)) {
				BlockState s = p.getBlockState();
				if (s.getValue(MultiFurnitureBlock.FACING).getAxis() != facing.getAxis() || p.getBlockPos().getY() != here.getY()) {
					continue;
				}
				BlockPos at = p.getBlockPos();
				rows.computeIfAbsent(alongX ? at.getZ() : at.getX(), k -> new ArrayList<>()).add(at);
			}
		}
		List<Integer> keys = new ArrayList<>(rows.keySet());
		keys.sort(Integer::compare);
		int row = keys.indexOf(alongX ? here.getZ() : here.getX());
		List<BlockPos> mine = rows.getOrDefault(alongX ? here.getZ() : here.getX(), List.of(here));
		mine.sort(Comparator.comparingInt(p -> alongX ? p.getX() : p.getZ()));
		int n = mine.indexOf(here);
		char letter = (char) ('A' + Math.max(0, Math.min(25, row)));
		return String.format(Locale.ROOT, "%c-%02d", letter, n + 1);
	}

	/** What a click on a piece says. */
	public static String describe(@Nullable LivePanelBlockEntity hub, int kind) {
		if (hub == null) {
			return "no UPS in reach";
		}
		long now = hub.getLevel() == null ? 0 : hub.getLevel().getGameTime();
		int bits = bits(hub, now);
		String ups = "UPS-1 online, battery 100 %";
		String air = String.format(Locale.ROOT, "room %.1f C, CRAC-1 %s, CRAC-2 %s", temp(hub, now), (bits & CRAC1_RUN) != 0 ? "run" : "off",
				(bits & CRAC2_RUN) != 0 ? "run" : "off");
		String[] agent = display(hub.srvPhase, hub.srvFlags, hub.srvSince, now, hub.srvReleasedAt);
		return switch (kind) {
			case COOLER -> air;
			case CYLINDER, RELEASE, ALARM_HORN -> agent[0] + ", " + agent[1];
			default -> ups + "; " + air;
		};
	}
}
