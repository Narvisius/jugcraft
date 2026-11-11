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
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * What a plant overview screen shows (round 34; nuclear/OverviewScreen.java works it out, client/OverviewScreenRenderer
 * draws it), held at the screen's bottom middle block. Twice a second it reads its control room's boards (within
 * {@link OverviewScreen#REACH} blocks: the display wall, else the nearest live board): their plant snapshot, the radiation
 * board's monitors and the cable spreading room's fire panel, the HVAC panel's state, the boards' alarms (as the display
 * wall's banner gathers them); the TSC's emergency class; the diesels' starting air; the condenser's circulating water
 * pumps and its cooling towers; the dry casks. It keeps the last five minutes of the values it trends, and which page
 * is up: a building's, clicked (or shown by itself when the building goes into alarm or trip), for 30 seconds.
 * Nothing is saved: the screen reads again when its chunk loads.
 */
public class OverviewScreenBlockEntity extends BlockEntity {
	/** The board state the screen draws from: the plant, and the alarms on its strip (wallAlarms, wallTotal, wallNew). */
	public final ControlBoard.State board = new ControlBoard.State();
	public boolean linked;
	/** The page up (OverviewScreen.PAGES; -1 the map), until when (game time), and whether a jump to trouble put it up. */
	public int page = -1;
	public long pageUntil;
	public boolean pageAuto;
	/** The TSC's emergency class (TscRoom.CLASSES) and its clocks (day time; -1 none). */
	public int emClass;
	public long emDeclared = -1, emActivated = -1;
	/** The diesels' starting air (psi; -1 no receivers). */
	public final double[] edgAir = {-1, -1};
	public int cwPumps, towers, towersRunning, casks, csrHeads, csrDetectors;
	public double towerLoad, csrAir = SpreadingRoomFire.AIR;
	public long csrSince, csrReleasedAt;
	/** The trends: each value's last SAMPLES samples (OverviewScreen.layout penValues), oldest at head; how many are real. */
	public float[] history = new float[0];
	public int head, filled;
	// Server only.
	private @Nullable BlockPos source, radBoard, esfBoard, tscHub, condenser;
	private final List<BlockPos> towerList = new ArrayList<>();
	private int searchVersion = -1;
	private long lastSearch = Long.MIN_VALUE / 2, lastSample = Long.MIN_VALUE / 2, lastSync = Long.MIN_VALUE / 2, clickedAt = Long.MIN_VALUE / 2;
	private final int[] seen = new int[OverviewScreen.TROUBLE.length];
	private int sentHash;

	public OverviewScreenBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.OVERVIEW_ENTITY, pos, state);
		java.util.Arrays.fill(seen, -1);
	}

	// ------------------------------------------------------------------ the server's tick

	void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (Math.floorMod(now + getBlockPos().hashCode(), 10) == 0) {
			update(level, now);
		}
	}

	/** Looks again now, whatever the tick: finds its boards afresh (tests, and the gametests' screens). */
	public void refresh(ServerLevel level) {
		searchVersion = -1;
		update(level, level.getGameTime());
	}

	private void update(ServerLevel level, long now) {
		List<String> pens = OverviewScreen.layout().penValues();
		if (history.length != pens.size() * ControlBoard.SAMPLES) {
			history = new float[pens.size() * ControlBoard.SAMPLES];
			head = filled = 0;
		}
		find(level, now);
		boolean was = linked;
		read(level, now);
		boolean pageChanged = false;
		if (linked && now - lastSample >= ControlBoard.SAMPLE) {
			lastSample = now;
			for (int i = 0; i < pens.size(); i++) {
				history[i * ControlBoard.SAMPLES + head] = (float) OverviewScreen.value(pens.get(i), this, now);
			}
			head = (head + 1) % ControlBoard.SAMPLES;
			filled = Math.min(ControlBoard.SAMPLES, filled + 1);
			pageChanged = true;
		}
		if (linked) {
			// A building gone into alarm or trip shows its page (unless someone chose a page in the last half minute).
			int jump = OverviewScreen.trouble(this, seen, now);
			if (jump >= 0 && now - clickedAt >= OverviewScreen.PAGE_TICKS) {
				page = jump;
				pageUntil = now + OverviewScreen.PAGE_TICKS;
				pageAuto = true;
				pageChanged = true;
			}
		}
		if (page >= 0 && now >= pageUntil) {
			page = -1;
			pageAuto = false;
			pageChanged = true;
		}
		int hash = hash();
		if (pageChanged || was != linked || hash != sentHash && now - lastSync >= 20 || now - lastSync >= 100) {
			sentHash = hash;
			lastSync = now;
			setChanged();
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** What changes what the screen shows (to send it when it does). */
	private int hash() {
		return java.util.Objects.hash(board.plant.present, board.plant.fission, board.plant.temperature, board.plant.scrammed, board.plant.electric,
				board.plant.alarms, board.plant.turbineSpeed, board.plant.hvState, board.plant.hvAlarms, board.plant.csrPhase, board.plant.csrFlags,
				board.plant.radFound, java.util.Arrays.hashCode(board.plant.rad), java.util.Arrays.hashCode(board.wallAlarms), board.wallTotal, board.wallNew,
				board.plant.edg[0].running, board.plant.edg[1].running, board.plant.edg[0].breakerAt, board.plant.edg[1].breakerAt, emClass, cwPumps,
				towersRunning, java.util.Arrays.hashCode(edgAir), board.plant.si, board.plant.siTime);
	}

	/** Finds the control room's boards, the TSC, the condenser and its towers (again when the plant's equipment changes). */
	private void find(ServerLevel level, long now) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (registry.version() == searchVersion && now - lastSearch < 200) {
			return;
		}
		searchVersion = registry.version();
		lastSearch = now;
		BlockPos here = getBlockPos();
		source = nearest(registry, here, OverviewScreen.REACH, EquipmentKind.PLANT_DISPLAY_WALL);
		if (source == null) {
			source = nearestBoard(registry, here);
		}
		BlockPos room = source == null ? here : source;
		radBoard = nearest(registry, room, 64, EquipmentKind.RADIATION_BOARD);
		esfBoard = nearest(registry, room, 64, EquipmentKind.ESF_BOARD);
		List<LivePanelBlockEntity> hubs = ControlBoard.panels(level, room, PlantInterior.BLOCKS.get(TscRoom.KEYS[TscRoom.HUB]), 48);
		tscHub = hubs.isEmpty() ? null : hubs.get(0).getBlockPos().immutable();
		BlockPos plant = source != null && level.getBlockEntity(source) instanceof EquipmentBlockEntity e && e.board().vessel != null ? e.board().vessel : room;
		condenser = nearest(registry, plant, OverviewScreen.REACH, EquipmentKind.CONDENSER);
		casks = 0;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> a : registry.anchors().entrySet()) {
			if (a.getValue().kind() == EquipmentKind.DRY_STORAGE_CASK && a.getKey().distSqr(plant) <= (double) OverviewScreen.REACH * OverviewScreen.REACH) {
				casks++;
			}
		}
	}

	private static @Nullable BlockPos nearest(EquipmentRegistry registry, BlockPos near, int reach, EquipmentKind kind) {
		BlockPos best = null;
		double bestD = (double) reach * reach;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> a : registry.anchors().entrySet()) {
			double d = a.getKey().distSqr(near);
			if (a.getValue().kind() == kind && d <= bestD) {
				best = a.getKey().immutable();
				bestD = d;
			}
		}
		return best;
	}

	private static @Nullable BlockPos nearestBoard(EquipmentRegistry registry, BlockPos near) {
		BlockPos best = null;
		double bestD = (double) OverviewScreen.REACH * OverviewScreen.REACH;
		for (Map.Entry<BlockPos, EquipmentRegistry.Entry> a : registry.anchors().entrySet()) {
			double d = a.getKey().distSqr(near);
			if (a.getValue().kind().liveBoard() && d <= bestD) {
				best = a.getKey().immutable();
				bestD = d;
			}
		}
		return best;
	}

	private static @Nullable EquipmentBlockEntity board(ServerLevel level, @Nullable BlockPos at) {
		return at != null && level.isLoaded(at) && level.getBlockEntity(at) instanceof EquipmentBlockEntity e && e.kind().liveBoard() ? e : null;
	}

	/** Reads what it found: the plant as its boards have it, the alarms, the class, the air, the water, the towers. */
	private void read(ServerLevel level, long now) {
		EquipmentBlockEntity src = board(level, source);
		linked = src != null;
		if (src == null) {
			board.plant = new BoardPlant();
			return;
		}
		BoardPlant p = copy(src.board().plant, level.registryAccess());
		EquipmentBlockEntity rad = board(level, radBoard), esf = board(level, esfBoard);
		BoardPlant hv = rad != null && rad.board().plant.hvFound ? rad.board().plant : esf != null ? esf.board().plant : null;
		if (hv != null && hv.hvFound) {
			p.hvFound = true;
			p.hvState = hv.hvState;
			p.hvAlarms = hv.hvAlarms;
			p.hvMode = hv.hvMode;
		}
		csrHeads = csrDetectors = 0;
		if (rad != null) {
			BoardPlant r = rad.board().plant;
			System.arraycopy(r.rad, 0, p.rad, 0, BoardPlant.RAD_MONITORS);
			p.radFound = r.radFound;
			p.csrFound = r.csrFound;
			p.csrPhase = r.csrPhase;
			p.csrFlags = r.csrFlags;
			BlockPos csr = rad.board().csrPanel;
			if (csr != null && level.isLoaded(csr) && level.getBlockEntity(csr) instanceof LivePanelBlockEntity panel) {
				csrAir = panel.csrAir;
				csrSince = panel.csrSince;
				csrReleasedAt = panel.csrReleasedAt;
				csrHeads = panel.csrAllHeads.size();
				csrDetectors = panel.csrDetectors.size();
			}
		}
		board.plant = p;
		DisplayWall.alarms(level, src, board, now);
		// The TSC's class.
		emClass = 0;
		emDeclared = emActivated = -1;
		if (tscHub != null && level.isLoaded(tscHub) && level.getBlockEntity(tscHub) instanceof LivePanelBlockEntity hub) {
			emClass = hub.tsClass;
			emDeclared = hub.tsDeclared;
			emActivated = hub.tsActivated;
		}
		// The diesels' starting air (their receivers'), by the boards' A and B.
		for (int k = 0; k < 2; k++) {
			BlockPos at = src.board().edg[k];
			edgAir[k] = at != null && level.isLoaded(at) && level.getBlockEntity(at) instanceof EquipmentBlockEntity edg && edg.airReceivers > 0 ? edg.air : -1;
		}
		// The circulating water: the condenser's pumps, its towers and how hard they work.
		cwPumps = towers = towersRunning = 0;
		towerLoad = 0;
		if (condenser != null && level.isLoaded(condenser) && level.getBlockEntity(condenser) instanceof EquipmentBlockEntity c) {
			cwPumps = c.cwPumpCount();
			for (BlockPos t : c.towerList()) {
				if (level.isLoaded(t) && level.getBlockEntity(t) instanceof CoolingTowerBlockEntity tower && tower.formed()) {
					towers++;
					towerLoad += tower.load();
					if (tower.load() > 0.02F) {
						towersRunning++;
					}
				}
			}
			towerLoad = towers == 0 ? 0 : towerLoad / towers;
		}
	}

	/** A snapshot copied (the source board's own is replaced, not changed, each time it looks: a copy is ours to add to). */
	static BoardPlant copy(BoardPlant p, HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		p.save(out);
		return BoardPlant.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, out.buildResult()));
	}

	// ------------------------------------------------------------------ clicks

	/**
	 * A click on the screen at (x, y) in its board coordinates (x across from its left, y up from its bottom): a building
	 * on the map shows its page; on a page SITE MAP goes back (anywhere else keeps it up another 30 seconds). Returns
	 * what to tell the player.
	 */
	public net.minecraft.network.chat.Component click(ServerLevel level, double x, double y) {
		long now = level.getGameTime();
		OverviewScreen.Layout layout = OverviewScreen.layout();
		net.minecraft.network.chat.Component said;
		if (!linked) {
			return OverviewScreen.status(this);
		}
		if (page >= 0) {
			OverviewScreen.Hit hit = find(layout.hits().get(OverviewScreen.PAGES[page]), x, y);
			if (hit != null && hit.page().equals("map")) {
				page = -1;
				pageAuto = false;
				said = net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.overview_map");
			} else {
				pageUntil = now + OverviewScreen.PAGE_TICKS;
				pageAuto = false;
				said = net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.overview_kept", OverviewScreen.PAGE_TICKS / 20);
			}
		} else {
			OverviewScreen.Hit hit = find(layout.hits().get("map"), x, y);
			if (hit == null) {
				return OverviewScreen.status(this);
			}
			int to = OverviewScreen.page(hit.page());
			if (to < 0) {
				return net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.overview_none", hit.name());
			}
			page = to;
			pageUntil = now + OverviewScreen.PAGE_TICKS;
			pageAuto = false;
			said = net.minecraft.network.chat.Component.translatable("message.jugcraft.nuclear.overview_page", hit.name(), OverviewScreen.PAGE_TICKS / 20);
		}
		clickedAt = now;
		setChanged();
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		return said;
	}

	private static OverviewScreen.@Nullable Hit find(@Nullable List<OverviewScreen.Hit> hits, double x, double y) {
		if (hits != null) {
			for (OverviewScreen.Hit h : hits) {
				if (h.contains(x, y)) {
					return h;
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ to the players

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		write(out);
		return out.buildResult();
	}

	private void write(ValueOutput out) {
		board.plant.save(out);
		out.putIntArray("alarms", board.wallAlarms);
		out.putInt("alarm_total", board.wallTotal);
		out.putInt("alarm_new", board.wallNew);
		out.putBoolean("linked", linked);
		out.putInt("page", page);
		out.putLong("page_until", pageUntil);
		out.putBoolean("page_auto", pageAuto);
		out.putInt("em_class", emClass);
		out.putLong("em_declared", emDeclared);
		out.putLong("em_activated", emActivated);
		out.putIntArray("edg_air", new int[] {(int) Math.round(edgAir[0] * 10), (int) Math.round(edgAir[1] * 10)});
		out.putIntArray("water", new int[] {cwPumps, towers, towersRunning, (int) Math.round(towerLoad * 1000), casks});
		out.putIntArray("csr", new int[] {(int) Math.round(csrAir * 10), csrHeads, csrDetectors});
		out.putLong("csr_since", csrSince);
		out.putLong("csr_released", csrReleasedAt);
		int[] h = new int[history.length];
		for (int i = 0; i < h.length; i++) {
			h[i] = Float.floatToIntBits(history[i]);
		}
		out.putIntArray("history", h);
		out.putInt("head", head);
		out.putInt("filled", filled);
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		board.plant = BoardPlant.load(in);
		board.wallAlarms = in.getIntArray("alarms").orElse(new int[0]);
		board.wallTotal = in.getIntOr("alarm_total", 0);
		board.wallNew = in.getIntOr("alarm_new", 0);
		linked = in.getBooleanOr("linked", false);
		page = in.getIntOr("page", -1);
		pageUntil = in.getLongOr("page_until", 0L);
		pageAuto = in.getBooleanOr("page_auto", false);
		emClass = in.getIntOr("em_class", 0);
		emDeclared = in.getLongOr("em_declared", -1L);
		emActivated = in.getLongOr("em_activated", -1L);
		int[] air = in.getIntArray("edg_air").orElse(new int[] {-10, -10});
		for (int k = 0; k < 2 && k < air.length; k++) {
			edgAir[k] = air[k] / 10.0;
		}
		int[] water = in.getIntArray("water").orElse(new int[5]);
		if (water.length == 5) {
			cwPumps = water[0];
			towers = water[1];
			towersRunning = water[2];
			towerLoad = water[3] / 1000.0;
			casks = water[4];
		}
		int[] csr = in.getIntArray("csr").orElse(new int[0]);
		if (csr.length == 3) {
			csrAir = csr[0] / 10.0;
			csrHeads = csr[1];
			csrDetectors = csr[2];
		}
		csrSince = in.getLongOr("csr_since", 0L);
		csrReleasedAt = in.getLongOr("csr_released", 0L);
		int[] h = in.getIntArray("history").orElse(new int[0]);
		history = new float[h.length];
		for (int i = 0; i < h.length; i++) {
			history[i] = Float.intBitsToFloat(h[i]);
		}
		head = in.getIntOr("head", 0);
		filled = in.getIntOr("filled", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
	}
}
