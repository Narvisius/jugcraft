package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A diesel room panel's link to its generator (round 14, Justin: "link the equipment to the nearest diesel
 * generator... a generator connects to at most one"). Each panel links to the nearest emergency diesel generator within
 * {@link #REACH} blocks that has no other panel of its kind yet, so a generator has at most one engine panel, one
 * generator panel, one regulator and so on; a second set of panels put down by a second generator takes that one. The
 * link is kept while the generator stands (checked every two seconds), saved, and sent to the players, whose
 * client/LivePanelRenderer.java draws the panel from that generator's state.
 *
 * <p>The DC rooms' panels link what they read the same way (rounds 25 and 26): a charger, the DC switchboard and a
 * vital inverter their station battery; an inverter its regulating transformer; a regulating transformer its motor
 * control centre and its inverter (8d); the ground detector its station battery (8f); the battery rooms' hydrogen
 * monitor and exhaust fan their battery and each other, and the DC switchboard all its racks, which it keeps even (#9).
 *
 * <p>The relay room's cabinets (round 26, #10: RelayRoom) read the nearest reactor once a second instead.
 */
public class LivePanelBlockEntity extends BlockEntity {
	public static final int REACH = 24;
	/**
	 * A battery charger (round 25): what it holds of its AC supply (JE; a cable into any block of it fills it, from a
	 * motor control centre's CHGR breaker), and what it charges its battery with at most (JE/t, its 150 A limit).
	 */
	public static final long CHARGER_BUFFER = 40_000, CHARGER_RATE = 1_024;
	/**
	 * A battery charger's, the DC switchboard's, a vital inverter's or a ground detector's station battery (the placing
	 * block of the rack), or null (synced).
	 */
	public @Nullable BlockPos battery;
	/** A battery charger: what came in from its supply in the last two seconds (whether its AC is on), and what it put
	 * into its battery a tick, on average (both synced for its meters). */
	public boolean ac;
	public int charging;
	public final io.github.jimbozoomer.jugcraft.energy.@Nullable SimpleEnergyStorage input;
	private long received, put;
	/**
	 * A regulating transformer (round 26, 8d): the motor control centre it is fed from (the nearest in reach), and which
	 * of its REG XF breakers feeds it (1 or 2, by its label: 1A-1 and 1B-1 are REG XF 1, 1A-2 and 1B-2 REG XF 2). It
	 * draws nothing (Justin: "Read the MCC breaker", as an inverter reads its battery): it is alive while that breaker is
	 * closed and the MCC's bus charged ({@link #energized}). Synced.
	 */
	public @Nullable BlockPos mcc;
	public int regXf = 1;
	/**
	 * A regulating transformer: the vital inverter it backs (the nearest in reach), so the client can tell whether it
	 * carries that inverter's load on bypass (synced).
	 */
	public @Nullable BlockPos inverter;
	/**
	 * A vital inverter (round 26, 8g; 8d): its bypass, the regulating transformer beside it (the nearest in reach).
	 * Synced; the client reads the bypass from that transformer.
	 */
	public @Nullable BlockPos xf;
	/**
	 * A vital inverter (round 26, 8e): the 120 VAC vital panel it feeds (the nearest with its label in reach), whose
	 * closed breakers are its load. Synced. (A vital panel keeps its inverter in {@link #inverter}.)
	 */
	public @Nullable BlockPos panel;
	/**
	 * The entrance hall's security post (older rooms round 6: AccessControl): the speed gates' passages in its reach,
	 * oldest first ("dayTime|IN|name"), and when its duress alarm ends (game time). Saved, synced.
	 */
	public final List<String> acLog = new ArrayList<>();
	public long acDuressUntil;
	/**
	 * The security gatehouse (SecurityGatehouse). The metal detector: when its lights go out (game time), whether it
	 * alarmed, how many it has passed; who was in it last tick (not saved). The X-ray: its last bags ("NAME|1" to be
	 * searched), how many it has seen, when its tunnel goes dark. The alarm station console: its zones in alarm (a bit
	 * each). Saved, synced.
	 */
	public long wdUntil, xrUntil;
	public boolean wdAlarm;
	public int wdCount, xrCount, secZones;
	public final java.util.Set<java.util.UUID> wdInside = new java.util.HashSet<>();
	public final List<String> xrLog = new ArrayList<>();
	/**
	 * The administration building's chemistry lab (ChemLab). The plant chemistry status board: its rows
	 * ("value|status|code|time") and the plant's mode as it last worked them out. The gamma spectroscopy station: when
	 * its count started (game time, -1 never) and on the day clock, the sample, how many it has counted, the activities
	 * it found (the nuclides, DEI, gross; uCi/g). Saved, synced.
	 */
	public final List<String> chemRows = new ArrayList<>();
	public String chemMode = "";
	public long gsStart = -1, gsDay;
	public String gsSample = "";
	public int gsCount;
	public final List<Double> gsAct = new ArrayList<>();
	/**
	 * A vital panel (round 26, 8e): its breakers, bit 0 the main and bits 1 to 12 its circuits, set closed
	 * (VitalPanelBlock). Saved and synced.
	 */
	public int breakers;
	/**
	 * A ground detector (round 26, 8f): the test being made, the side (1 the + bus, 2 the - bus, 0 none) and the game
	 * time it lasts to (held, each use renews it: GroundDetectorBlock). Saved and synced; the client reads the test from
	 * them.
	 */
	public int testSide;
	public long testUntil;
	/** A ground detector: when its alarm may beep again (game time; not saved). */
	long beepAt;
	/**
	 * The station battery's racks this panel's DC bus has in parallel (round 26, #9; Justin: "Second string in
	 * parallel"): a DC switchboard's are every rack in reach nearer to it than to any other switchboard, and it keeps
	 * their charge even (balance); a ground detector, hydrogen monitor or exhaust fan takes its nearest switchboard's.
	 * Synced: the client reads their flows (the battery's current) and strings (strings) from them.
	 */
	public final List<BlockPos> racks = new ArrayList<>();
	/**
	 * The DC equipment rooms (older rooms round 3: DcRoom). A charger's, inverter's, distribution panel's or the log
	 * desk laptop's switchboard (its train's nearest in reach). A switchboard's state (DcRoom.BUS ...) and its room's parts
	 * it found (DcRoom.PARTS: chargers, inverters, panels by number; itself where none). A charger's or inverter's own
	 * breakers (DcRoom.AC_OPEN, DC_OPEN set open), a charger's equalize end (game time), the annunciator: its conditions
	 * in, the windows latched and acknowledged, TEST held until (game time). Saved and synced.
	 */
	public @Nullable BlockPos board;
	public int sbFlags, dcFlags, annIn, annLatch, annAck;
	/** A switchboard's strings off its bus, their disconnects open (older rooms round 4: bit s for string s). Saved, synced. */
	public int sbIso;
	public long eqUntil, annTestUntil;
	public final List<BlockPos> dcParts = new ArrayList<>();
	/** (From 1: a piece just built links its battery and room before its first look at its bus.) */
	private int dcTimer = 1;
	/**
	 * How long it has ticked since it was built or loaded (game ticks, up to DcRoom.SETTLE): a charger's or inverter's
	 * annunciator latches nothing before then, while its supply and its switchboard's bus are first worked out.
	 */
	int dcAge;
	/** A charger's AC used by its own controls and the battery's float (JE a tick), so its supply keeps flowing. */
	static final long CHARGER_IDLE = 8;
	/** An exhaust fan's HAND-OFF-AUTO selector (ExhaustFanBlock.MODES); a battery monitor's string on its screen. Saved, synced. */
	public int mode, page;
	/** The linked generator's anchor, or null. */
	public @Nullable BlockPos generator;
	/**
	 * The generator's run time (game ticks) when this panel was linked to it: the engine panel's hour meter counts from
	 * there, so it reads zero when it is linked (Justin, round 14). -1: not linked.
	 */
	public long hoursBase = -1;
	private int timer, balanceTimer;
	/**
	 * A relay room cabinet (round 26, #10): what it last read of the nearest reactor in StatusScreenBlockEntity.RANGE
	 * (RelayRoom.Reactor: status 0 none), and the fission a second before (for the startup rate). Synced, not saved.
	 */
	public int rxStatus, rxAlarms;
	public double rxFission, rxHeat, rxTemp = 30, rxRods = 100, rxSur;
	public boolean rxProtection = true;
	private double lastFission = -1;
	/**
	 * The control room HVAC (round 26, #11: HvacRoom). The control panel: its mimic (HvacRoom.PARTS running or open, and
	 * there), its annunciator and emergency (HvacRoom.A_*), and when the room last went in or out of emergency (game
	 * time); its selector is {@link #mode} (HvacRoom.NORMAL, AUTO, EMERGENCY). A radiation monitor: its count rate
	 * (cpm). Synced; the selector saved.
	 */
	public int hvState, hvAlarms;
	public double hvValue;
	public long hvSince;
	/**
	 * The cable spreading room's pre-action sprinkler system (punch list #15: SpreadingRoomFire), kept by its releasing
	 * panel: its phase (SpreadingRoomFire.NORMAL to DRILL) and since when (game time), its flags, the system's air
	 * (psi), when it released (day time, for its display), when it last saw a fire (game time). Saved and synced. Not
	 * saved: the detectors, heads and valve station it found, the heads open now, when it looks again. A valve station
	 * keeps its releasing panel in {@link #panel}.
	 */
	public int csrPhase, csrFlags;
	public long csrSince, csrQuiet, csrReleasedAt;
	public double csrAir = SpreadingRoomFire.AIR;
	double csrSent;
	long csrScanAt;
	List<BlockPos> csrDetectors = List.of(), csrAllHeads = List.of();
	final List<BlockPos> csrHeads = new ArrayList<>();
	@Nullable BlockPos csrValve;
	/** Part W3: the smoke vents it opens on an alarm (a warehouse's), the next detectors to look round (a big system's in turns). */
	List<BlockPos> csrVents = List.of();
	int csrTurn;
	/** The panel's dampers as last found, and the seconds until it looks again (not saved). */
	@Nullable List<BlockPos> hvDampers;
	int hvRescan;
	/**
	 * The Technical Support Center (round 26, #12: TscRoom). Its classification board: the emergency class (0 none to 4),
	 * when it was declared, when the event began and the TSC was activated (day time, -1 none), the log (each entry its
	 * day time shifted left 5 bits, its TscRoom.EVENTS index below), what it found and read of the plant
	 * (TscRoom.B_*, TscRoom.N_*), the conditions as last seen and how long they have been clear. An area monitor or
	 * frisker: the dose rate where it stands (mSv/h). Synced; the class, clocks, log and conditions saved.
	 */
	public int tsClass, tsBits, tsFlags, tsClear;
	public long tsDeclared = -1, tsStart = -1, tsActivated = -1;
	public final double[] tsNums = new double[TscRoom.NUMS];
	public final List<Long> tsEvents = new ArrayList<>();
	public double tsValue;
	/** What the board found of the plant, and the seconds until it looks again (not saved). */
	TscRoom.@Nullable Found tsFound;
	int tsRescan;
	/**
	 * The operations office (round 26, #13: OpsOffice). Its out-of-service board: everything in the plant switched off
	 * (OpsOffice.Entry, encoded), and the next clearance number. Synced and saved.
	 */
	public final List<String> opsEntries = new ArrayList<>();
	public int opsNext;
	/** What the board found of the plant (not saved). */
	OpsOffice.@Nullable Found opsFound;
	/**
	 * The plant computer room (older rooms round 2: ServerRoom; it only looks live). Its hub (the UPS module): the power
	 * bits (ServerRoom.FED and the CRACs running), the UPS battery's charge (always full), the room's air (degrees C) when
	 * its cooling last changed and that game time (ServerRoom.temp works it out from them), the clean agent system's
	 * phase, flags and since when (game time), when it released (day time), the abort held until (game time), the
	 * cylinders' pressure (psig). Saved and synced. A rack: the number it was given before the clients worked it out
	 * (saved). Not saved: the detectors the hub found and when.
	 */
	public int srvBits = ServerRoom.FED | ServerRoom.CRAC1_RUN | ServerRoom.CRAC2_RUN, srvPhase, srvFlags;
	public double srvBattery = 1, srvTemp = ServerRoom.ROOM, srvPsi = ServerRoom.PSI;
	public long srvSince, srvReleasedAt, srvAbortUntil, srvTempAt;
	public String srvLabel = "";
	List<BlockPos> srvDetectors = List.of();
	long srvScanAt = -1;

	/**
	 * The pieces that only look live (Justin, 25 October 2026: "make it look like it works, but it doesn't actually need to
	 * be calculating any kind of live numbers ... It doesn't need to be constantly updating anything"; wherever they
	 * stand): the plant computer room's (ServerRoom), the battery rooms' (the hydrogen monitor, the exhaust fan, the
	 * string disconnects) and the operations office's wall clock, radio rack and PPE shelf (drawn from the game's clock). Nothing ticks on the server for them (LivePanelBlock gives them no ticker); their screens,
	 * meters and links are worked out on the clients that draw them, and what they do happens when someone uses them.
	 */
	public static boolean simulated(BlockState state) {
		return ServerRoom.kind(state) >= 0 || batteryRoom(state) || DcRoom.isDisconnect(state) || OpsOffice.kind(state) >= OpsOffice.CLOCK;
	}

	@Override
	public void setLevel(net.minecraft.world.level.Level level) {
		super.setLevel(level);
		if (level instanceof ServerLevel server && ServerRoom.kind(getBlockState()) == ServerRoom.HUB) {
			ServerRoom.loaded(server, getBlockPos());
		}
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (level instanceof ServerLevel server && ServerRoom.kind(getBlockState()) == ServerRoom.HUB) {
			ServerRoom.loaded(server, getBlockPos());
		}
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel server && ServerRoom.kind(getBlockState()) == ServerRoom.HUB) {
			ServerRoom.unloaded(server, getBlockPos());
		}
		super.setRemoved();
	}

	public LivePanelBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.LIVE_PANEL_ENTITY, pos, state);
		breakers = defaultBreakers(state);
		mode = defaultMode(state);
		input = isCharger(state) ? new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(CHARGER_BUFFER, 4_096, 0, this::setChanged) {
			@Override
			public long insert(long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
				if ((dcFlags & DcRoom.AC_OPEN) != 0) {
					// Its AC input breaker open (older rooms round 3): nothing comes in.
					return 0;
				}
				long in = super.insert(maxAmount, transaction);
				if (in > 0) {
					transaction.addCloseCallback((t, result) -> {
						if (result.wasCommitted()) received += in;
					});
				}
				return in;
			}
		} : null;
	}

	/** A panel's breakers as built (set closed): a vital or DC panel's, the DC switchboard's. */
	private static int defaultBreakers(BlockState state) {
		return isVitalPanel(state) ? VitalPanelBlock.DEFAULT_BREAKERS : isSwitchboard(state) ? DcRoom.SB_DEFAULT : 0;
	}

	/** A selector's position as built: an exhaust fan's AUTO, the HVAC panel's AUTO. */
	private static int defaultMode(BlockState state) {
		return isExhaustFan(state) ? ExhaustFanBlock.AUTO : HvacRoom.kind(state) == HvacRoom.CONTROL ? HvacRoom.AUTO : 0;
	}

	static boolean isCharger(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("edg_battery_charger") || state.getBlock() == PlantInterior.BLOCKS.get("station_charger");
	}

	static boolean isSwitchboard(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("dc_switchboard");
	}

	public static boolean isInverter(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("vital_inverter");
	}

	public static boolean isTransformer(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("reg_transformer");
	}

	public static boolean isVitalPanel(BlockState state) {
		return state.getBlock() instanceof VitalPanelBlock;
	}

	public static boolean isGroundDetector(BlockState state) {
		return state.getBlock() instanceof GroundDetectorBlock;
	}

	public static boolean isH2Monitor(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("h2_monitor");
	}

	public static boolean isExhaustFan(BlockState state) {
		return state.getBlock() instanceof ExhaustFanBlock;
	}

	/** The battery rooms' panels (round 26, #9): the hydrogen monitor and the exhaust fan. */
	static boolean batteryRoom(BlockState state) {
		return isH2Monitor(state) || isExhaustFan(state);
	}

	/**
	 * A battery room's hydrogen monitor's exhaust fan, or a fan's monitor (round 26, #9; simulated: worked out where it is
	 * drawn, client/EdgTelemetry.java hydrogen): the nearest of the other in reach, or null.
	 */
	public static @Nullable LivePanelBlockEntity batteryRoomPartner(net.minecraft.world.level.Level level, LivePanelBlockEntity piece) {
		List<LivePanelBlockEntity> other = ControlBoard.panels(level, piece.getBlockPos(),
				PlantInterior.BLOCKS.get(isH2Monitor(piece.getBlockState()) ? "exhaust_fan" : "h2_monitor"), REACH);
		return other.isEmpty() ? null : other.get(0);
	}

	/**
	 * A hydrogen monitor's or exhaust fan's racks (simulated: worked out where it is drawn): its nearest switchboard's
	 * (they are synced), else the nearest station battery in reach, else none.
	 */
	public static List<BlockPos> batteryRoomRacks(net.minecraft.world.level.Level level, LivePanelBlockEntity piece) {
		BlockPos here = piece.getBlockPos();
		List<LivePanelBlockEntity> boards = ControlBoard.panels(level, here, PlantInterior.BLOCKS.get("dc_switchboard"), REACH);
		if (!boards.isEmpty()) {
			return List.copyOf(boards.get(0).racks);
		}
		BlockPos best = null;
		int r = (REACH >> 4) + 1;
		for (int cx = (here.getX() >> 4) - r; cx <= (here.getX() >> 4) + r; cx++) {
			for (int cz = (here.getZ() >> 4) - r; cz <= (here.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof StationBatteryBlockEntity bank && bank.getBlockPos().distSqr(here) <= (double) REACH * REACH
							&& (best == null || bank.getBlockPos().distSqr(here) < best.distSqr(here))) {
						best = bank.getBlockPos().immutable();
					}
				}
			}
		}
		return best == null ? List.of() : List.of(best);
	}

	/**
	 * A battery's racks by string (round 26, #9): the racks standing in one line (the same way, end to end) are one
	 * string in series, each line another in parallel; ordered by where they stand, so every player's screen agrees.
	 */
	public static List<List<BlockPos>> strings(net.minecraft.world.level.Level level, List<BlockPos> racks) {
		Map<String, List<BlockPos>> lines = new java.util.TreeMap<>();
		for (BlockPos at : racks) {
			BlockState state = level.getBlockState(at);
			if (!(state.getBlock() instanceof StationBatteryBlock)) {
				continue;
			}
			net.minecraft.core.Direction facing = state.getValue(MultiFurnitureBlock.FACING);
			int row = facing.getAxis() == net.minecraft.core.Direction.Axis.Z ? at.getZ() : at.getX();
			lines.computeIfAbsent(String.format(java.util.Locale.ROOT, "%s %+08d %+04d", facing.getAxis(), row, at.getY()),
					k -> new ArrayList<>()).add(at);
		}
		List<List<BlockPos>> out = new ArrayList<>(lines.values());
		for (List<BlockPos> line : out) {
			line.sort(java.util.Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ()));
		}
		return out;
	}

	/**
	 * A DC switchboard's racks (round 26, #9): every station battery in reach that is nearer to it than to any other
	 * switchboard (so each train's switchboard has its own room's), nearest first.
	 */
	private void linkRacks(ServerLevel level) {
		List<LivePanelBlockEntity> boards = around(level, getBlockState().getBlock());
		BlockPos here = getBlockPos();
		List<BlockPos> found = new ArrayList<>();
		int r = (REACH >> 4) + 1;
		for (int cx = (here.getX() >> 4) - r; cx <= (here.getX() >> 4) + r; cx++) {
			for (int cz = (here.getZ() >> 4) - r; cz <= (here.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof StationBatteryBlockEntity bank) {
						BlockPos at = bank.getBlockPos();
						double d = at.distSqr(here);
						if (d <= (double) REACH * REACH && boards.stream().noneMatch(o -> o.getBlockPos().distSqr(at) < d)) {
							found.add(at.immutable());
						}
					}
				}
			}
		}
		found.sort(java.util.Comparator.<BlockPos>comparingDouble(p -> p.distSqr(here)).thenComparing(p -> p));
		setRacks(level, found);
	}

	/** A ground detector's racks: its nearest switchboard's (else its own battery). */
	private void copyRacks(ServerLevel level) {
		List<LivePanelBlockEntity> boards = around(level, PlantInterior.BLOCKS.get("dc_switchboard"));
		setRacks(level, !boards.isEmpty() ? boards.get(0).racks : battery == null ? List.of() : List.of(battery));
	}

	private void setRacks(ServerLevel level, List<BlockPos> found) {
		if (!found.equals(racks)) {
			racks.clear();
			racks.addAll(found);
			sync(level);
		}
	}

	/**
	 * A DC switchboard keeps its racks' charge even (round 26, #9): strings in parallel on one bus sit at the same
	 * voltage, so a charger feeding one feeds them all. Each second the fullest gives the emptiest up to a second of
	 * the racks' rate, and the next pair the same.
	 */
	private void balance(ServerLevel level) {
		List<StationBatteryBlockEntity> banks = new ArrayList<>();
		// (Older rooms round 4: a string off the bus, its disconnect open, is left out.)
		for (BlockPos at : DcRoom.onBus(level, this)) {
			if (level.isLoaded(at) && level.getBlockEntity(at) instanceof StationBatteryBlockEntity bank) {
				banks.add(bank);
			}
		}
		banks.sort(java.util.Comparator.comparingLong(b -> b.energy.getAmount()));
		for (int i = 0, j = banks.size() - 1; i < j; i++, j--) {
			StationBatteryBlockEntity low = banks.get(i), high = banks.get(j);
			long move = Math.min(StationBatteryBlockEntity.RATE * 20, (high.energy.getAmount() - low.energy.getAmount()) / 2);
			if (move > 0) {
				high.energy.setAmount(high.energy.getAmount() - move);
				low.energy.setAmount(low.energy.getAmount() + move);
				high.changed();
				low.changed();
			}
		}
	}

	/** An exhaust fan's selector turned, a battery monitor's page stepped: saved, and sent to the players. */
	void changedMode(ServerLevel level) {
		sync(level);
	}

	/**
	 * The DC equipment rooms' panels: the station charger, the DC switchboard, the vital inverter, its transformer, its
	 * vital panel and the ground detector.
	 */
	static boolean dcRoom(BlockState state) {
		return state.getBlock() == PlantInterior.BLOCKS.get("station_charger") || isSwitchboard(state) || isInverter(state) || isTransformer(state)
				|| isVitalPanel(state) || isGroundDetector(state) || batteryRoom(state) || DcRoom.isLaptop(state) || DcRoom.isDisconnect(state);
	}

	/** A ground detector's test (GroundDetectorBlock): on side 1 (+) or 2 (-) until game time until; saved, sent. */
	void test(ServerLevel level, int side, long until) {
		testSide = side;
		testUntil = until;
		sync(level);
	}

	/** Whether a ground detector is testing at game time now (fractional on the client). */
	public boolean testing(double now) {
		return testSide != 0 && now < testUntil;
	}

	/** A vital panel's breakers switched (VitalPanelBlock): saved, and sent to the players. */
	void changedBreakers(ServerLevel level) {
		sync(level);
	}

	/**
	 * A vital inverter keeps to the vital panel it feeds, and a vital panel to its inverter (round 26, 8e): the nearest
	 * with the same label in reach, else the nearest.
	 */
	private @Nullable BlockPos labelled(ServerLevel level, Block kind) {
		int mine = variant(getBlockState());
		List<LivePanelBlockEntity> found = around(level, kind);
		for (LivePanelBlockEntity other : found) {
			if (variant(other.getBlockState()) == mine) {
				return other.getBlockPos();
			}
		}
		return found.isEmpty() ? null : found.get(0).getBlockPos();
	}

	private void linkPanel(ServerLevel level) {
		BlockPos found = labelled(level, PlantInterior.BLOCKS.get("vital_panel"));
		if (found == null ? panel != null : !found.equals(panel)) {
			panel = found == null ? null : found.immutable();
			sync(level);
		}
	}

	private void linkFeed(ServerLevel level) {
		BlockPos found = labelled(level, PlantInterior.BLOCKS.get("vital_inverter"));
		if (found == null ? inverter != null : !found.equals(inverter)) {
			inverter = found == null ? null : found.immutable();
			sync(level);
		}
	}

	/**
	 * A charger, inverter, DC panel or the laptop keeps to its switchboard (older rooms round 3): the nearest in reach of
	 * its own train (the laptop the nearest), else the nearest.
	 */
	private void linkBoard(ServerLevel level) {
		List<LivePanelBlockEntity> boards = around(level, PlantInterior.BLOCKS.get(DcRoom.SWITCHBOARD));
		BlockPos found = null;
		if (!DcRoom.isLaptop(getBlockState())) {
			int train = DcRoom.train(getBlockState());
			for (LivePanelBlockEntity b : boards) {
				if (DcRoom.train(b.getBlockState()) == train) {
					found = b.getBlockPos();
					break;
				}
			}
		}
		if (found == null && !boards.isEmpty()) {
			found = boards.get(0).getBlockPos();
		}
		if (found == null ? board != null : !found.equals(board)) {
			board = found == null ? null : found.immutable();
			sync(level);
		}
	}

	/** A battery room's string disconnect finds its switchboard when it is worked (it does not tick: simulated). */
	void linkBoardNow(ServerLevel level) {
		linkBoard(level);
	}

	/** A panel's label (round 26, 8c: 0 to 3 for 1A-1, 1A-2, 1B-1, 1B-2; one put down by hand is 0). */
	public static int variant(BlockState state) {
		net.minecraft.world.level.block.state.properties.IntegerProperty v = state.getBlock() instanceof MultiFurnitureBlock block
				? block.variantProperty() : null;
		return v == null ? 0 : state.getValue(v);
	}

	/**
	 * A regulating transformer keeps to the nearest motor control centre in reach; its REG XF breaker is the one its
	 * label says (round 26, 8d: no longer the inverters' order along the MCC's front), REG XF 1 for 1A-1 and 1B-1, REG XF
	 * 2 for 1A-2 and 1B-2. Two by one MCC with the same number (put down by hand, both 1A-1) share the breakers in their
	 * order along the MCC's front from its left: the first keeps its own, the next takes the other.
	 */
	private void linkMcc(ServerLevel level) {
		BlockPos here = getBlockPos(), best = null;
		double bestD = Double.MAX_VALUE;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			double d = e.getKey().distSqr(here);
			if (e.getValue().kind() == EquipmentKind.MCC_LINEUP && d <= (double) REACH * REACH && d < bestD) {
				bestD = d;
				best = e.getKey().immutable();
			}
		}
		int number = variant(getBlockState()) % 2, before = 0;
		if (best != null) {
			net.minecraft.core.Direction right = EquipmentRegistry.of(level).anchors().get(best).facing().getClockWise();
			double mine = along(here, best, right);
			for (LivePanelBlockEntity other : around(level, PlantInterior.BLOCKS.get("reg_transformer"))) {
				double theirs = along(other.getBlockPos(), best, right);
				if (best.equals(other.mcc) && variant(other.getBlockState()) % 2 == number
						&& (theirs < mine || theirs == mine && other.getBlockPos().compareTo(here) < 0)) {
					before++;
				}
			}
		}
		int breaker = (number + before) % 2 + 1;
		if ((best == null ? mcc != null : !best.equals(mcc)) || breaker != regXf) {
			mcc = best;
			regXf = breaker;
			sync(level);
		}
	}

	private static double along(BlockPos p, BlockPos origin, net.minecraft.core.Direction right) {
		return (p.getX() - origin.getX()) * right.getStepX() + (p.getZ() - origin.getZ()) * right.getStepZ();
	}

	/**
	 * An inverter keeps to a regulating transformer in reach, its bypass (round 26, 8d: in the plants the one beside it;
	 * Justin: transformers beside their inverters): the one that names it, else the nearest no other inverter has, else
	 * the nearest. So in a row of inverters and transformers each pairs with its own, not two with one.
	 */
	private void linkTransformer(ServerLevel level) {
		List<LivePanelBlockEntity> xfs = around(level, PlantInterior.BLOCKS.get("reg_transformer"));
		BlockPos found = null;
		for (LivePanelBlockEntity t : xfs) {
			if (getBlockPos().equals(t.inverter)) {
				found = t.getBlockPos();
				break;
			}
		}
		if (found == null) {
			List<LivePanelBlockEntity> inverters = around(level, PlantInterior.BLOCKS.get("vital_inverter"));
			for (LivePanelBlockEntity t : xfs) {
				if (inverters.stream().noneMatch(i -> t.getBlockPos().equals(i.xf))) {
					found = t.getBlockPos();
					break;
				}
			}
		}
		if (found == null && !xfs.isEmpty()) {
			found = xfs.get(0).getBlockPos();
		}
		if (found == null ? xf != null : !found.equals(xf)) {
			xf = found == null ? null : found.immutable();
			sync(level);
		}
	}

	/**
	 * A regulating transformer keeps to the vital inverter whose load it takes on bypass: the one that names it, else the
	 * nearest that names no other transformer, else the nearest.
	 */
	private void linkInverter(ServerLevel level) {
		List<LivePanelBlockEntity> inverters = around(level, PlantInterior.BLOCKS.get("vital_inverter"));
		BlockPos found = null;
		for (LivePanelBlockEntity i : inverters) {
			if (getBlockPos().equals(i.xf)) {
				found = i.getBlockPos();
				break;
			}
		}
		if (found == null) {
			for (LivePanelBlockEntity i : inverters) {
				if (i.xf == null) {
					found = i.getBlockPos();
					break;
				}
			}
		}
		if (found == null && !inverters.isEmpty()) {
			found = inverters.get(0).getBlockPos();
		}
		if (found == null ? inverter != null : !found.equals(inverter)) {
			inverter = found == null ? null : found.immutable();
			sync(level);
		}
	}

	/**
	 * The live panels of the given kind within {@link #REACH} blocks (their placing blocks), nearest first; ties by
	 * position, so every panel sees them in the same order.
	 */
	/** The HVAC room's panels and monitors find each other (HvacRoom): as {@link #around}. */
	List<LivePanelBlockEntity> panelsAround(ServerLevel level, Block kind) {
		return around(level, kind, REACH);
	}

	/** As {@link #panelsAround(ServerLevel, Block)} within {@code reach} blocks (round 26, #12: the TSC's board). */
	public List<LivePanelBlockEntity> panelsAround(ServerLevel level, Block kind, int reach) {
		return around(level, kind, reach);
	}

	private List<LivePanelBlockEntity> around(ServerLevel level, Block kind) {
		return around(level, kind, REACH);
	}

	private List<LivePanelBlockEntity> around(ServerLevel level, Block kind, int reach) {
		BlockPos here = getBlockPos();
		List<LivePanelBlockEntity> out = new ArrayList<>();
		int r = (reach >> 4) + 1;
		for (int cx = (here.getX() >> 4) - r; cx <= (here.getX() >> 4) + r; cx++) {
			for (int cz = (here.getZ() >> 4) - r; cz <= (here.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be != this && be instanceof LivePanelBlockEntity other && other.getBlockState().getBlock() == kind
							&& other.getBlockPos().distSqr(here) <= (double) reach * reach) {
						out.add(other);
					}
				}
			}
		}
		out.sort(java.util.Comparator.<LivePanelBlockEntity>comparingDouble(o -> o.getBlockPos().distSqr(here))
				.thenComparing(LivePanelBlockEntity::getBlockPos));
		return out;
	}

	/**
	 * Whether a regulating transformer is alive (round 26, 8d): its motor control centre there with its bus charged and
	 * its REG XF breaker closed. It reads the breaker and draws nothing (Justin: "Read the MCC breaker"). Its meters and
	 * lamp (client/EdgTelemetry.java transformer) and its inverter's bypass follow this.
	 */
	public boolean energized(net.minecraft.world.level.Level level) {
		return isTransformer(getBlockState()) && mcc != null && level.getBlockEntity(mcc) instanceof EquipmentBlockEntity board && board.bus != null
				&& board.bus.getAmount() > 0 && (board.breakers & 1 << regXf) != 0;
	}

	public void serverTick(ServerLevel level) {
		int csr = SpreadingRoomFire.kind(getBlockState());
		if (csr >= 0) {
			// The cable spreading room's sprinklers (punch list #15): the releasing panel watches its room twice a second;
			// the valve station keeps to the nearest releasing panel (its gauges read it).
			if (timer-- <= 0) {
				timer = csr == 0 ? 9 : 39;
				if (csr == 0) {
					SpreadingRoomFire.tick(this, level, level.getGameTime());
				} else {
					List<LivePanelBlockEntity> near = ControlBoard.panels(level, getBlockPos(), PlantInterior.BLOCKS.get(SpreadingRoomFire.RELEASE),
							SpreadingRoomFire.REACH);
					near.sort(java.util.Comparator.comparingDouble(p -> p.getBlockPos().distSqr(getBlockPos())));
					BlockPos found = near.isEmpty() ? null : near.get(0).getBlockPos().immutable();
					if (found == null ? panel != null : !found.equals(panel)) {
						panel = found;
						sync(level);
					}
				}
			}
			return;
		}
		if (AccessControl.isStation(getBlockState())) {
			// The entrance hall's security post (older rooms round 6): the gates write to it; nothing to link.
			return;
		}
		if (ChemLab.isBoard(getBlockState())) {
			// The chemistry lab's status board: the plant's chemistry, once a second.
			if (timer-- <= 0) {
				timer = 19;
				ChemLab.boardTick(this, level);
			}
			return;
		}
		if (ChemLab.isGamma(getBlockState())) {
			// The gamma spectroscopy station: its count finishing.
			ChemLab.gammaTick(this, level);
			return;
		}
		if (SecurityGatehouse.isDetector(getBlockState())) {
			// The gatehouse's metal detector: whoever walks through it, every tick.
			SecurityGatehouse.detectorTick(this, level);
			return;
		}
		if (SecurityGatehouse.isXray(getBlockState())) {
			// The gatehouse's X-ray: the bags put through it (a click); nothing to link.
			return;
		}
		if (SecurityGatehouse.isConsole(getBlockState())) {
			// The gatehouse's alarm station console: its post and the perimeter's zones, once a second.
			if (timer-- <= 0) {
				timer = 19;
				SecurityGatehouse.consoleTick(this, level);
			}
			return;
		}
		if (ControlRoomConsoles.isConsole(getBlockState())) {
			// The main control room's consoles (older rooms round 5) link the display wall once a second, never a generator.
			if (timer-- <= 0) {
				timer = 19;
				ControlRoomConsoles.tick(this, level);
			}
			return;
		}
		if (HallCrane.isPendant(getBlockState())) {
			// The turbine hall's crane pendant stations (turbine hall part 3): the nearest overhead crane in reach, once a
			// second (its POWER lamp reads it).
			if (timer-- <= 0) {
				timer = 19;
				EquipmentBlockEntity crane = HallCrane.near(level, getBlockPos());
				BlockPos found = crane == null ? null : crane.getBlockPos().immutable();
				if (found == null ? panel != null : !found.equals(panel)) {
					panel = found;
					sync(level);
				}
			}
			return;
		}
		if (TurbineControlRoom.kind(getBlockState()) >= 0) {
			// The turbine control room's cabinets (turbine hall part 3): the nearest turbine-generator, once a second.
			if (timer-- <= 0) {
				timer = 19;
				TurbineControlRoom.tick(this, level);
			}
			return;
		}
		if (simulated(getBlockState())) {
			// The plant computer room and the battery rooms only look live: nothing to do (they have no ticker).
			return;
		}
		int ops = OpsOffice.kind(getBlockState());
		if (ops >= 0) {
			// The operations office's board reads the plant, its consoles and schedule board find it, once a second (#13).
			if (timer-- <= 0) {
				timer = 19;
				OpsOffice.tick(this, level, ops);
			}
			return;
		}
		int tsc = TscRoom.kind(getBlockState());
		if (tsc >= 0) {
			// The Technical Support Center's panels (round 26, #12) read the plant, or their board, once a second.
			if (timer-- <= 0) {
				timer = 19;
				TscRoom.tick(this, level, tsc);
			}
			return;
		}
		int hvac = HvacRoom.kind(getBlockState());
		if (hvac >= 0) {
			// The control room HVAC's panel and monitors (round 26, #11) read the plant once a second, never a generator.
			if (timer-- <= 0) {
				timer = 19;
				HvacRoom.tick(this, level, hvac);
			}
			return;
		}
		if (SwitchgearRoom.isLaptop(getBlockState())) {
			// The switchgear room's laptop reads its load centre on the client (SwitchgearRoom), never a generator.
			return;
		}
		int relay = RelayRoom.kind(getBlockState());
		if (relay >= 0) {
			// A relay room cabinet reads the reactor (a relay panel nothing: its name is its label), never a generator.
			if (relay != RelayRoom.RELAY && timer-- <= 0) {
				timer = 19;
				relayTick(level);
			}
			return;
		}
		if (input != null) {
			charge(level);
		}
		if (racks.size() > 1 && isSwitchboard(getBlockState()) && balanceTimer-- <= 0) {
			balanceTimer = 19;
			balance(level);
		}
		if ((isSwitchboard(getBlockState()) || DcRoom.isCharger(getBlockState()) || isInverter(getBlockState())) && dcTimer-- <= 0) {
			// The DC rooms (older rooms round 3): the switchboard's bus and room, a charger's or inverter's annunciator.
			dcTimer = 19;
			dcAge = Math.min(DcRoom.SETTLE, dcAge + 20);
			if (isSwitchboard(getBlockState())) {
				DcRoom.tickBoard(level, this);
			} else {
				DcRoom.tickPanel(level, this, DcRoom.isCharger(getBlockState()));
			}
		}
		if (timer-- > 0) {
			return;
		}
		timer = 40;
		// (The battery rooms' hydrogen monitor, exhaust fan and string disconnects only look live: they do not tick, and
		// their links are worked out where they are drawn or when they are worked: batteryRoomPartner, batteryRoomRacks,
		// linkBoardNow.)
		if (DcRoom.isCharger(getBlockState()) || isInverter(getBlockState()) || DcRoom.isPanel(getBlockState()) || DcRoom.isLaptop(getBlockState())) {
			linkBoard(level);
		}
		if (input != null || isSwitchboard(getBlockState()) || isInverter(getBlockState()) || isGroundDetector(getBlockState())) {
			linkBattery(level);
		}
		if (isSwitchboard(getBlockState())) {
			linkRacks(level);
		} else if (isGroundDetector(getBlockState())) {
			copyRacks(level);
		}
		if (isVitalPanel(getBlockState()) && !DcRoom.isPanel(getBlockState())) {
			linkFeed(level);
		}
		if (isInverter(getBlockState())) {
			linkTransformer(level);
			linkPanel(level);
			if (mcc != null) {
				// Its MCC link from round 26's 8g (an inverter now reads its transformer instead).
				mcc = null;
				sync(level);
			}
		}
		if (isTransformer(getBlockState())) {
			linkMcc(level);
			linkInverter(level);
		}
		if (dcRoom(getBlockState())) {
			// The DC rooms' panels read their battery, MCC or transformer, never a diesel generator (one in reach would
			// put their meters on the generator's).
			if (generator != null) {
				generator = null;
				hoursBase = -1;
				sync(level);
			}
			return;
		}
		if (generator != null && isGenerator(level, generator) && generator.distSqr(getBlockPos()) <= (double) REACH * REACH) {
			if (hoursBase < 0 && level.getBlockEntity(generator) instanceof EquipmentBlockEntity edg) {
				// Linked before the hour meter counted from the link: it starts from zero now.
				hoursBase = edg.runTicksAt(level.getGameTime());
				sync(level);
			}
			return;
		}
		BlockPos found = choose(level);
		if (found == null ? generator != null : !found.equals(generator)) {
			generator = found;
			hoursBase = found != null && level.getBlockEntity(found) instanceof EquipmentBlockEntity edg ? edg.runTicksAt(level.getGameTime()) : -1;
			sync(level);
		}
	}

	/** What a relay room cabinet last read of its reactor. */
	public RelayRoom.Reactor reactor() {
		return new RelayRoom.Reactor(rxStatus, rxFission, rxHeat, rxTemp, rxRods, rxAlarms, rxProtection, rxSur);
	}

	/**
	 * A relay room cabinet, once a second: the nearest reactor vessel in range (as a status screen finds it), its state
	 * and the startup rate since the last look; sent to the players when it has moved.
	 */
	/** The HVAC panel and radiation monitors read the nearest reactor as a relay room cabinet does. */
	void readReactor(ServerLevel level) {
		relayTick(level);
	}

	/** Sends this panel's state to the players (and marks it to be saved). */
	void syncNow(ServerLevel level) {
		sync(level);
	}

	private void relayTick(ServerLevel level) {
		EquipmentBlockEntity vessel = nearestVessel(level, getBlockPos());
		RelayRoom.Reactor now;
		if (vessel == null) {
			now = RelayRoom.Reactor.NONE;
			lastFission = -1;
		} else {
			double f = vessel.core.fission;
			now = RelayRoom.Reactor.of(vessel.core, lastFission < 0 ? 0 : RelayRoom.startupRate(lastFission, f));
			lastFission = f;
		}
		boolean moved = now.status() != rxStatus || now.alarms() != rxAlarms || now.protection() != rxProtection
				|| Math.abs(now.fission() - rxFission) > Math.max(0.002, Math.abs(rxFission) * 0.05) || Math.abs(now.heat() - rxHeat) > 0.002
				|| Math.abs(now.temperature() - rxTemp) > 0.2 || Math.abs(now.rods() - rxRods) > 0.2 || Math.abs(now.sur() - rxSur) > 0.05;
		if (moved) {
			rxStatus = now.status();
			rxAlarms = now.alarms();
			rxProtection = now.protection();
			rxFission = now.fission();
			rxHeat = now.heat();
			rxTemp = now.temperature();
			rxRods = now.rods();
			rxSur = now.sur();
			sync(level);
		}
	}

	/** The nearest reactor vessel (any kind) within StatusScreenBlockEntity.RANGE whose chunk is loaded, or null. */
	static @Nullable EquipmentBlockEntity nearestVessel(ServerLevel level, BlockPos here) {
		EquipmentBlockEntity best = null;
		double bestDistance = (double) StatusScreenBlockEntity.RANGE * StatusScreenBlockEntity.RANGE;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			EquipmentKind kind = e.getValue().kind();
			if (kind != EquipmentKind.REACTOR_VESSEL && kind != EquipmentKind.EPR_VESSEL && kind != EquipmentKind.SMALL_MODULAR_REACTOR) {
				continue;
			}
			double d = e.getKey().distSqr(here);
			if (d < bestDistance && level.isLoaded(e.getKey()) && level.getBlockEntity(e.getKey()) instanceof EquipmentBlockEntity v) {
				best = v;
				bestDistance = d;
			}
		}
		return best;
	}

	/** A charger: every tick it charges its battery from what its supply gave it, up to its rate. */
	private void charge(ServerLevel level) {
		if (input == null) {
			return;
		}
		StationBatteryBlockEntity bank = battery != null && level.isLoaded(battery)
				&& level.getBlockEntity(battery) instanceof StationBatteryBlockEntity b ? b : null;
		if (DcRoom.isCharger(getBlockState())) {
			// A station charger (older rooms round 3): its controls take a little of its supply all the time (so a
			// floating charger's supply keeps coming); it charges only with its DC breaker closed and, on a switchboard,
			// the switchboard's main closed (the battery on the bus).
			input.setAmount(Math.max(0, input.getAmount() - CHARGER_IDLE));
			LivePanelBlockEntity sb = board != null && level.getBlockEntity(board) instanceof LivePanelBlockEntity s && isSwitchboard(s.getBlockState()) ? s : null;
			if ((dcFlags & DcRoom.DC_OPEN) != 0 || sb != null && !DcRoom.closed(sb, DcRoom.MAIN)) {
				bank = null;
			} else if (bank != null && sb != null && sb.sbIso != 0 && !DcRoom.rackOnBus(level, sb, bank.getBlockPos())) {
				// Its rack's string off the bus (older rooms round 4: its disconnect open): it charges the string still on.
				List<BlockPos> on = DcRoom.onBus(level, sb);
				bank = on.isEmpty() ? null : StationBatteryBlock.battery(level, on.get(0));
			}
		}
		if (bank != null && input.getAmount() > 0) {
			// (Its input gives nothing back to a cable, so the charge is moved by hand.)
			long in;
			try (net.fabricmc.fabric.api.transfer.v1.transaction.Transaction t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
				in = bank.energy.insert(Math.min(CHARGER_RATE, input.getAmount()), t);
				if (in > 0) {
					t.commit();
				}
			}
			if (in > 0) {
				input.setAmount(input.getAmount() - in);
				put += in;
			}
		}
		if (level.getGameTime() % 40 == 0) {
			boolean on = received > 0;
			int rate = (int) (put / 40);
			received = put = 0;
			if (on != ac || rate != charging) {
				ac = on;
				charging = rate;
				sync(level);
			}
		}
	}

	/**
	 * A charger or the DC switchboard keeps to its station battery: the nearest rack within {@link #REACH} blocks (a
	 * charger takes one no other charger has, if it can). The racks are looked for block by block round it, so racks
	 * put down before batteries held charge are found (and get their charge then).
	 */
	private void linkBattery(ServerLevel level) {
		if (battery != null && StationBatteryBlock.battery(level, battery) != null) {
			return;
		}
		BlockPos here = getBlockPos(), best = null, free = null;
		double bestD = Double.MAX_VALUE, freeD = Double.MAX_VALUE;
		java.util.Set<BlockPos> seen = new java.util.HashSet<>();
		for (BlockPos at : BlockPos.betweenClosed(here.offset(-REACH, -4, -REACH), here.offset(REACH, 4, REACH))) {
			if (!(level.getBlockState(at).getBlock() instanceof StationBatteryBlock)) {
				continue;
			}
			StationBatteryBlockEntity bank = StationBatteryBlock.battery(level, at);
			if (bank == null || !seen.add(bank.getBlockPos())) {
				continue;
			}
			double d = bank.getBlockPos().distSqr(here);
			if (d < bestD) {
				bestD = d;
				best = bank.getBlockPos();
			}
			if (d < freeD && (input == null || !batteryTaken(level, bank.getBlockPos()))) {
				freeD = d;
				free = bank.getBlockPos();
			}
		}
		BlockPos found = free != null ? free : best;
		if (found == null ? battery != null : !found.equals(battery)) {
			battery = found == null ? null : found.immutable();
			sync(level);
		}
	}

	/** Whether another charger already charges this battery. */
	private boolean batteryTaken(ServerLevel level, BlockPos bank) {
		int r = (REACH >> 4) + 1;
		for (int cx = (bank.getX() >> 4) - r; cx <= (bank.getX() >> 4) + r; cx++) {
			for (int cz = (bank.getZ() >> 4) - r; cz <= (bank.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be != this && be instanceof LivePanelBlockEntity other && other.input != null && bank.equals(other.battery)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** What a cable touching a charger finds: its AC input (it takes power, gives none back). */
	public static io.github.jimbozoomer.jugcraft.energy.@Nullable EnergyStorage chargerInput(net.minecraft.world.level.Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LivePanelBlock block) || !isCharger(state)) {
			return null;
		}
		BlockPos origin = pos.subtract(block.offset(state.getValue(LivePanelBlock.FACING), state.getValue(block.partProperty())));
		return level.getBlockEntity(origin) instanceof LivePanelBlockEntity panel ? panel.input : null;
	}

	/** The sprinkler system's state changed (SpreadingRoomFire): saved, and sent to the players. */
	void changedAndSync(ServerLevel level) {
		sync(level);
	}

	private void sync(ServerLevel level) {
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** The hours the linked generator has run since this panel was linked to it, at game time {@code now}. */
	public double hours(EquipmentBlockEntity edg, double now) {
		long base = Math.max(0, hoursBase);
		double total = edg.runTicks + (edg.running ? Math.max(0, now - edg.runningSince) : 0);
		return Math.max(0, total - base) / DieselFuel.TICKS_PER_HOUR;
	}

	private static boolean isGenerator(ServerLevel level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EquipmentBlockEntity e && e.kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR;
	}

	/** The nearest generator in reach with no other panel of this kind linked to it. */
	private @Nullable BlockPos choose(ServerLevel level) {
		List<BlockPos> candidates = new ArrayList<>();
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR && e.getKey().distSqr(getBlockPos()) <= (double) REACH * REACH
					&& isGenerator(level, e.getKey())) {
				candidates.add(e.getKey().immutable());
			}
		}
		candidates.sort(java.util.Comparator.comparingDouble(p -> p.distSqr(getBlockPos())));
		for (BlockPos candidate : candidates) {
			if (!taken(level, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Whether another panel of this kind is already linked to the generator (a room has as many horns and strobes, and
	 * exhaust fans, as it likes: round 31 part 3).
	 */
	private boolean taken(ServerLevel level, BlockPos generator) {
		Block kind = getBlockState().getBlock();
		if (kind == PlantInterior.BLOCKS.get("edg_horn_strobe") || kind == PlantInterior.BLOCKS.get("edg_room_fan")) {
			return false;
		}
		int r = (REACH >> 4) + 1;
		for (int cx = (generator.getX() >> 4) - r; cx <= (generator.getX() >> 4) + r; cx++) {
			for (int cz = (generator.getZ() >> 4) - r; cz <= (generator.getZ() >> 4) + r; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be != this && be instanceof LivePanelBlockEntity other && generator.equals(other.generator)
							&& other.getBlockState().getBlock() == kind) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		if (generator != null) {
			out.store("generator", BlockPos.CODEC, generator);
			out.putLong("hours_base", hoursBase);
		}
		saveBattery(out);
		if (input != null) {
			out.putLong("input", input.getAmount());
		}
	}

	private void saveBattery(ValueOutput out) {
		if (battery != null) {
			out.store("battery", BlockPos.CODEC, battery);
		}
		out.putBoolean("ac", ac);
		out.putInt("charging", charging);
		if (mcc != null) {
			out.store("mcc", BlockPos.CODEC, mcc);
			out.putInt("reg_xf", regXf);
		}
		if (inverter != null) {
			out.store("inverter", BlockPos.CODEC, inverter);
		}
		if (xf != null) {
			out.store("xf", BlockPos.CODEC, xf);
		}
		if (panel != null) {
			out.store("panel", BlockPos.CODEC, panel);
		}
		if (isVitalPanel(getBlockState()) || isSwitchboard(getBlockState())) {
			out.putInt("breakers", breakers);
		}
		saveDc(out);
		if (testSide != 0) {
			out.putInt("test_side", testSide);
			out.putLong("test_until", testUntil);
		}
		if (!racks.isEmpty()) {
			out.store("racks", BlockPos.CODEC.listOf(), List.copyOf(racks));
		}
		if (isExhaustFan(getBlockState()) || isGroundDetector(getBlockState()) || HvacRoom.kind(getBlockState()) == HvacRoom.CONTROL
				|| ServerRoom.kind(getBlockState()) == ServerRoom.COOLER || DcRoom.isCharger(getBlockState()) || isInverter(getBlockState())
				|| DcRoom.isLaptop(getBlockState()) || DcRoom.isDisconnect(getBlockState()) || ControlRoomConsoles.isConsole(getBlockState())
				|| SecurityGatehouse.isConsole(getBlockState())) {
			out.putInt("mode", mode);
			out.putInt("page", page);
		}
		if (AccessControl.isStation(getBlockState())) {
			out.store("ac_log", com.mojang.serialization.Codec.STRING.listOf(), List.copyOf(acLog));
			out.putLong("ac_duress", acDuressUntil);
		}
		if (SecurityGatehouse.isDetector(getBlockState())) {
			out.putLong("wd_until", wdUntil);
			out.putBoolean("wd_alarm", wdAlarm);
			out.putInt("wd_count", wdCount);
		} else if (SecurityGatehouse.isXray(getBlockState())) {
			out.store("xr_log", com.mojang.serialization.Codec.STRING.listOf(), List.copyOf(xrLog));
			out.putInt("xr_count", xrCount);
			out.putLong("xr_until", xrUntil);
		} else if (SecurityGatehouse.isConsole(getBlockState())) {
			out.putInt("sec_zones", secZones);
		} else if (ChemLab.isBoard(getBlockState())) {
			out.store("chem_rows", com.mojang.serialization.Codec.STRING.listOf(), List.copyOf(chemRows));
			out.putString("chem_mode", chemMode);
		} else if (ChemLab.isGamma(getBlockState())) {
			out.putLong("gs_start", gsStart);
			out.putLong("gs_day", gsDay);
			out.putString("gs_sample", gsSample);
			out.putInt("gs_count", gsCount);
			out.store("gs_act", com.mojang.serialization.Codec.DOUBLE.listOf(), List.copyOf(gsAct));
		}
		int srv = ServerRoom.kind(getBlockState());
		if (srv == ServerRoom.HUB) {
			saveSrv(out);
		} else if (srv == ServerRoom.RACK && !srvLabel.isEmpty()) {
			out.putString("srv_label", srvLabel);
		}
		if (TscRoom.kind(getBlockState()) == TscRoom.HUB) {
			saveTsc(out);
		}
		if (OpsOffice.kind(getBlockState()) == OpsOffice.HUB) {
			saveOps(out);
		}
		if (SpreadingRoomFire.kind(getBlockState()) == 0) {
			saveCsr(out);
		}
	}

	/** The DC rooms' state (older rooms round 3: DcRoom; saved and sent). */
	private void saveDc(ValueOutput out) {
		if (board != null) {
			out.store("board", BlockPos.CODEC, board);
		}
		if (isSwitchboard(getBlockState())) {
			out.putInt("sb_flags", sbFlags);
			out.putInt("sb_iso", sbIso);
			out.store("dc_parts", BlockPos.CODEC.listOf(), List.copyOf(dcParts));
		}
		if (DcRoom.isCharger(getBlockState()) || isInverter(getBlockState())) {
			out.putInt("dc_flags", dcFlags);
			out.putLong("eq_until", eqUntil);
			out.putInt("ann_in", annIn);
			out.putInt("ann_latch", annLatch);
			out.putInt("ann_ack", annAck);
			out.putLong("ann_test", annTestUntil);
		}
	}

	/** The plant computer room's state (its hub's: saved and sent). */
	private void saveSrv(ValueOutput out) {
		out.putInt("srv_bits", srvBits);
		out.putInt("srv_phase", srvPhase);
		out.putInt("srv_flags", srvFlags);
		out.putDouble("srv_battery", srvBattery);
		out.putDouble("srv_temp", srvTemp);
		out.putLong("srv_temp_at", srvTempAt);
		out.putDouble("srv_psi", srvPsi);
		out.putLong("srv_since", srvSince);
		out.putLong("srv_released", srvReleasedAt);
		out.putLong("srv_abort", srvAbortUntil);
	}

	/** The sprinkler system's state (saved and sent). */
	private void saveCsr(ValueOutput out) {
		out.putInt("csr_phase", csrPhase);
		out.putInt("csr_flags", csrFlags);
		out.putLong("csr_since", csrSince);
		out.putLong("csr_quiet", csrQuiet);
		out.putLong("csr_released", csrReleasedAt);
		out.putDouble("csr_air", csrAir);
	}

	/** The out-of-service board's list and next clearance number (saved and sent). */
	private void saveOps(ValueOutput out) {
		out.store("ops_entries", com.mojang.serialization.Codec.STRING.listOf(), List.copyOf(opsEntries));
		out.putInt("ops_next", opsNext);
	}

	/** The TSC board's class, clocks and log (saved and sent). */
	private void saveTsc(ValueOutput out) {
		out.putInt("ts_class", tsClass);
		out.putLong("ts_declared", tsDeclared);
		out.putLong("ts_start", tsStart);
		out.putLong("ts_activated", tsActivated);
		out.store("ts_events", com.mojang.serialization.Codec.LONG.listOf(), List.copyOf(tsEvents));
		out.putInt("ts_flags", tsFlags);
		out.putInt("ts_clear", tsClear);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		generator = in.read("generator", BlockPos.CODEC).orElse(null);
		hoursBase = in.getLongOr("hours_base", -1L);
		battery = in.read("battery", BlockPos.CODEC).orElse(null);
		ac = in.getBooleanOr("ac", false);
		charging = in.getIntOr("charging", 0);
		mcc = in.read("mcc", BlockPos.CODEC).orElse(null);
		regXf = in.getIntOr("reg_xf", 1);
		inverter = in.read("inverter", BlockPos.CODEC).orElse(null);
		xf = in.read("xf", BlockPos.CODEC).orElse(null);
		panel = in.read("panel", BlockPos.CODEC).orElse(null);
		breakers = in.getIntOr("breakers", defaultBreakers(getBlockState()));
		board = in.read("board", BlockPos.CODEC).orElse(null);
		sbFlags = in.getIntOr("sb_flags", 0);
		sbIso = in.getIntOr("sb_iso", 0);
		dcParts.clear();
		dcParts.addAll(in.read("dc_parts", BlockPos.CODEC.listOf()).orElse(List.of()));
		dcFlags = in.getIntOr("dc_flags", 0);
		eqUntil = in.getLongOr("eq_until", 0L);
		annIn = in.getIntOr("ann_in", 0);
		annLatch = in.getIntOr("ann_latch", 0);
		annAck = in.getIntOr("ann_ack", 0);
		annTestUntil = in.getLongOr("ann_test", 0L);
		testSide = in.getIntOr("test_side", 0);
		testUntil = in.getLongOr("test_until", 0L);
		racks.clear();
		racks.addAll(in.read("racks", BlockPos.CODEC.listOf()).orElse(List.of()));
		mode = in.getIntOr("mode", defaultMode(getBlockState()));
		hvState = in.getIntOr("hv_state", 0);
		hvAlarms = in.getIntOr("hv_alarms", 0);
		hvValue = in.getDoubleOr("hv_value", 0);
		hvSince = in.getLongOr("hv_since", 0L);
		page = in.getIntOr("page", 0);
		acLog.clear();
		acLog.addAll(in.read("ac_log", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()));
		acDuressUntil = in.getLongOr("ac_duress", 0L);
		wdUntil = in.getLongOr("wd_until", 0L);
		wdAlarm = in.getBooleanOr("wd_alarm", false);
		wdCount = in.getIntOr("wd_count", 0);
		xrLog.clear();
		xrLog.addAll(in.read("xr_log", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()));
		xrCount = in.getIntOr("xr_count", 0);
		xrUntil = in.getLongOr("xr_until", 0L);
		secZones = in.getIntOr("sec_zones", 0);
		chemRows.clear();
		chemRows.addAll(in.read("chem_rows", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()));
		chemMode = in.getStringOr("chem_mode", "");
		gsStart = in.getLongOr("gs_start", -1L);
		gsDay = in.getLongOr("gs_day", 0L);
		gsSample = in.getStringOr("gs_sample", "");
		gsCount = in.getIntOr("gs_count", 0);
		gsAct.clear();
		gsAct.addAll(in.read("gs_act", com.mojang.serialization.Codec.DOUBLE.listOf()).orElse(List.of()));
		rxStatus = in.getIntOr("rx_status", 0);
		rxAlarms = in.getIntOr("rx_alarms", 0);
		rxProtection = in.getBooleanOr("rx_protection", true);
		rxFission = in.getDoubleOr("rx_fission", 0);
		rxHeat = in.getDoubleOr("rx_heat", 0);
		rxTemp = in.getDoubleOr("rx_temp", 30);
		rxRods = in.getDoubleOr("rx_rods", 100);
		rxSur = in.getDoubleOr("rx_sur", 0);
		tsClass = in.getIntOr("ts_class", 0);
		tsDeclared = in.getLongOr("ts_declared", -1L);
		tsStart = in.getLongOr("ts_start", -1L);
		tsActivated = in.getLongOr("ts_activated", -1L);
		tsEvents.clear();
		tsEvents.addAll(in.read("ts_events", com.mojang.serialization.Codec.LONG.listOf()).orElse(List.of()));
		tsFlags = in.getIntOr("ts_flags", 0);
		tsClear = in.getIntOr("ts_clear", 0);
		tsBits = in.getIntOr("ts_bits", 0);
		List<Double> nums = in.read("ts_nums", com.mojang.serialization.Codec.DOUBLE.listOf()).orElse(List.of());
		for (int i = 0; i < tsNums.length; i++) {
			tsNums[i] = i < nums.size() ? nums.get(i) : 0;
		}
		tsValue = in.getDoubleOr("ts_value", 0);
		opsEntries.clear();
		opsEntries.addAll(in.read("ops_entries", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()));
		opsNext = in.getIntOr("ops_next", 0);
		csrPhase = in.getIntOr("csr_phase", 0);
		csrFlags = in.getIntOr("csr_flags", 0);
		csrSince = in.getLongOr("csr_since", 0L);
		csrQuiet = in.getLongOr("csr_quiet", 0L);
		csrReleasedAt = in.getLongOr("csr_released", 0L);
		csrAir = in.getDoubleOr("csr_air", SpreadingRoomFire.AIR);
		// (Saved before the room only looked live: its supply read from the load centres. It is always fed now.)
		srvBits = ServerRoom.FED | in.getIntOr("srv_bits", srvBits) & (ServerRoom.CRAC1_RUN | ServerRoom.CRAC2_RUN);
		srvPhase = in.getIntOr("srv_phase", 0);
		srvFlags = in.getIntOr("srv_flags", 0);
		srvBattery = 1;
		srvTemp = in.getDoubleOr("srv_temp", ServerRoom.ROOM);
		srvTempAt = in.getLongOr("srv_temp_at", 0L);
		srvPsi = in.getDoubleOr("srv_psi", ServerRoom.PSI);
		srvSince = in.getLongOr("srv_since", 0L);
		srvReleasedAt = in.getLongOr("srv_released", 0L);
		srvAbortUntil = in.getLongOr("srv_abort", 0L);
		srvLabel = in.getStringOr("srv_label", "");
		if (input != null) {
			input.setAmount(in.getLongOr("input", 0L));
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		if (generator != null) {
			out.store("generator", BlockPos.CODEC, generator);
			out.putLong("hours_base", hoursBase);
		}
		saveBattery(out);
		if (rxStatus != 0) {
			// A relay room cabinet's reading (not saved: it reads again after a load).
			out.putInt("rx_status", rxStatus);
			out.putInt("rx_alarms", rxAlarms);
			out.putBoolean("rx_protection", rxProtection);
			out.putDouble("rx_fission", rxFission);
			out.putDouble("rx_heat", rxHeat);
			out.putDouble("rx_temp", rxTemp);
			out.putDouble("rx_rods", rxRods);
			out.putDouble("rx_sur", rxSur);
		}
		if (HvacRoom.kind(getBlockState()) >= 0) {
			// The HVAC panel's mimic and annunciator, a radiation monitor's count rate (read again after a load).
			out.putInt("hv_state", hvState);
			out.putInt("hv_alarms", hvAlarms);
			out.putDouble("hv_value", hvValue);
			out.putLong("hv_since", hvSince);
		}
		int tsc = TscRoom.kind(getBlockState());
		if (tsc == TscRoom.HUB) {
			// The TSC board's class, clocks, log and what it read of the plant (read again after a load).
			saveTsc(out);
			out.putInt("ts_bits", tsBits);
			java.util.List<Double> nums = new ArrayList<>();
			for (double d : tsNums) {
				nums.add(d);
			}
			out.store("ts_nums", com.mojang.serialization.Codec.DOUBLE.listOf(), nums);
		} else if (tsc == TscRoom.AREA || tsc == TscRoom.FRISKER) {
			out.putDouble("ts_value", tsValue);
		}
		if (OpsOffice.kind(getBlockState()) == OpsOffice.HUB) {
			saveOps(out);
		}
		if (SpreadingRoomFire.kind(getBlockState()) == 0) {
			saveCsr(out);
		}
		int srv = ServerRoom.kind(getBlockState());
		if (srv == ServerRoom.HUB) {
			saveSrv(out);
		} else if (srv == ServerRoom.RACK) {
			out.putString("srv_label", srvLabel);
		} else if (srv == ServerRoom.COOLER) {
			out.putInt("mode", mode);
		}
		return out.buildResult();
	}
}
