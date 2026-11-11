package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Warehouse Control Terminal: the warehouse's books and its fleet's dispatcher (claude/jugcraft-warehouse-plan.md,
 * "server-light by design"). It finds its warehouse (Network) when a warehouse block near it changes; keeps the
 * drones (no entities: records, each with its current leg), carries out their legs' events as their ticks come, and
 * gives idle drones jobs (Planner) every half second; it keeps the stock index (what is stored where, from the racks'
 * pallets) and the put-away log. Players get the drones' legs once each (DronePayload) and its STOCK and FLEET pages
 * while they have its screen open.
 */
public class TerminalBlockEntity extends BlockEntity implements PieceEntity, AnalogSource, ExtendedMenuProvider<BlockPos> {
	/** How far its search for its warehouse goes (blocks across), and how many blocks at most. */
	public static final int REACH = 96, MAX_BLOCKS = 400_000;
	private static final int LOG = 8;

	// ---- found (not saved: found again after loading)
	private Network network = new Network();
	private final Router router = new Router();
	private boolean changed = true, primary = true;
	private long nextScan;
	private final Map<BlockPos, Integer> reserved = new HashMap<>();
	/** Part W3: where a pallet a drone is taking to a lift is bound (a place, or an order's Ready Storage) and its order. */
	private final Map<Integer, LiftBound> liftBound = new HashMap<>();

	record LiftBound(BlockPos dest, int order) {
	}
	private final Map<ItemVariant, Stock> stock = new LinkedHashMap<>();
	private int placesUsed;
	// ---- kept
	private final List<Drone> drones = new ArrayList<>();
	private int nextId = 1;
	private final Deque<PutAway> log = new ArrayDeque<>();
	private long itemsIn;
	// ---- part W2: orders out, streams, Ready Storage's pool, the Output Port's buffer
	private final List<Order> orders = new ArrayList<>();
	private final List<Order.Stream> streams = new ArrayList<>();
	/** Part W4: the liquid streams (out of the Liquids Out panels' OUT flanges); their refills' tag is LIQUID_STREAM + k + 1. */
	private final List<Order.Stream> liquidStreams = new ArrayList<>();
	public static final int LIQUID_STREAM = 100;
	private ReadyPool pool = new ReadyPool();
	private PortBuffer port = new PortBuffer();
	/** The Output Port: OFF, ORDERS (only), ORDERS + STREAMS. */
	private int portMode = PORT_STREAMS;
	/** Part W4: the Liquids Out panels' OUT flanges: OFF, ORDERS (only), ORDERS + STREAMS. */
	private int liquidMode = PORT_STREAMS;
	private int nextOrder = 1000;
	/** The last minute's seconds the dispatch hatches were busy (for the ORDERS page's bar). */
	private final boolean[] busy = new boolean[60];
	private int busyAt;
	public static final int PORT_OFF = 0, PORT_ORDERS = 1, PORT_STREAMS = 2;
	/** Done orders kept for the screens. */
	private static final int KEEP_DONE = 12;
	public static final int MAX_STREAMS = 4;
	// ---- the client's copy
	private List<Drone.View> clientFleet = new ArrayList<>();
	// Part W3, the BUILDING page: the floor being added (its stake, the floors before it), who staked the building and
	// with how many floors; the building as last found (WarehouseBuilding.detect) and the next floor's site, kept a while.
	private @Nullable BlockPos pendingFloor;
	private int pendingFrom;
	private String builtBy = "";
	private int floorsChosen;
	private WarehouseBuilding.@Nullable Found building;
	private long buildingAt = Long.MIN_VALUE;
	private io.github.jimbozoomer.jugcraft.blueprint.@Nullable BuildSite nextSite;
	// ---- part W5 (kept): the owner and who may use it; power without a switchboard (tests, the demo); the circuits
	// switched on; the job priorities; housekeeping; the fleet paused; when each place was last counted and what the
	// books say is on it; the last adjustments a count made; today's peak draw.
	private java.util.@Nullable UUID owner;
	private String ownerName = "";
	/** When it got its owner (game time; 0 an old save's): a warehouse stays its first owner's. */
	private long ownedSince;
	/** Part W5: no consolidation started before this (one that could not finish waits a while). */
	private long consolPauseUntil;
	private io.github.jimbozoomer.jugcraft.party.UseMode useMode = io.github.jimbozoomer.jugcraft.party.UseMode.PERSONAL;
	private boolean unlimitedPower, fleetPaused;
	private int circuitsOn = 0x3F;
	private final int[] priorities = {ControlPayload.ORDERS, ControlPayload.PUTAWAY, ControlPayload.LIQUIDS, ControlPayload.HOUSEKEEPING};
	private int consolidateMode = ControlPayload.WHEN_IDLE, countMode = ControlPayload.COUNT_DAILY;
	private final Map<BlockPos, Long> counted = new HashMap<>();
	private final Map<BlockPos, Integer> books = new HashMap<>();
	private final Deque<String> adjustments = new ArrayDeque<>();
	private long peak, peakDay;
	// ---- part W5 (worked out): power each second, the signal boxes' inputs, housekeeping's status, the map
	private boolean poweredNow, lowPower, mainOff;
	private int shedMask, boardCount;
	private float chargeFactor;
	private long[] loads = new long[6];
	private long supplyNow, drawNow, storedNow, capacityNow, lastWork;
	private final Map<BlockPos, long[]> signals = new HashMap<>();
	private String house = "", next = "";
	private int scanVersion, mapCellsVersion = -1, mapVersion;
	private @Nullable List<MapPayload.Floor> mapCells;
	private @Nullable MapPayload map;
	private long mapColumnsAt;
	private int lastAnalog = -1;

	/** What is stored of one item: how many, in how many cases, on which pallets. */
	public static final class Stock {
		public final ItemStack item;
		public long count;
		public int cases;
		public final List<BlockPos> places = new ArrayList<>();

		Stock(ItemStack item) {
			this.item = item.copyWithCount(1);
		}
	}

	/** Part W5: when a place was last counted; what the books say is on a place (its contents' hash). */
	record CountedPlace(BlockPos pos, long time) {
		static final com.mojang.serialization.Codec<CountedPlace> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(CountedPlace::pos), com.mojang.serialization.Codec.LONG.fieldOf("t").forGetter(CountedPlace::time))
				.apply(i, CountedPlace::new));
	}

	record BookedPlace(BlockPos pos, int contents) {
		static final com.mojang.serialization.Codec<BookedPlace> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(BookedPlace::pos), com.mojang.serialization.Codec.INT.fieldOf("c").forGetter(BookedPlace::contents))
				.apply(i, BookedPlace::new));
	}

	/** A pallet put away: when (game time), its first item, how many items, where. */
	public record PutAway(long time, ItemStack item, int count, String place) {
		public static final com.mojang.serialization.Codec<PutAway> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
				com.mojang.serialization.Codec.LONG.fieldOf("time").forGetter(PutAway::time), ItemStack.CODEC.fieldOf("item").forGetter(PutAway::item),
				com.mojang.serialization.Codec.INT.fieldOf("count").forGetter(PutAway::count),
				com.mojang.serialization.Codec.STRING.fieldOf("place").forGetter(PutAway::place)).apply(i, PutAway::new));
	}

	public TerminalBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.TERMINAL_ENTITY, pos, state);
	}

	Network network() {
		return network;
	}

	Router router() {
		return router;
	}

	public List<Drone> drones() {
		return drones;
	}

	public List<Drone.View> clientFleet() {
		return clientFleet;
	}

	public boolean primary() {
		return primary;
	}

	public Map<ItemVariant, Stock> stock() {
		return stock;
	}

	public int placesUsed() {
		return placesUsed;
	}

	public int placesTotal() {
		return network.places.size();
	}

	public Deque<PutAway> log() {
		return log;
	}

	public long itemsIn() {
		return itemsIn;
	}

	public void markChanged() {
		changed = true;
		buildingAt = Long.MIN_VALUE;
	}

	// ------------------------------------------------------------------ the building (part W3)

	public @Nullable BlockPos pendingFloor() {
		return pendingFloor;
	}

	public int pendingFrom() {
		return pendingFrom;
	}

	void setPendingFloor(@Nullable BlockPos stake, int from) {
		pendingFloor = stake == null ? null : stake.immutable();
		pendingFrom = from;
		buildingAt = Long.MIN_VALUE;
		setChanged();
	}

	public String builtBy() {
		return builtBy;
	}

	public int floorsChosen() {
		return floorsChosen;
	}

	void setBuiltFrom(String who, int floors) {
		builtBy = who;
		floorsChosen = floors;
		setChanged();
	}

	/** The pre-made warehouse this terminal is in, as found at most five seconds ago (null: not one). */
	WarehouseBuilding.@Nullable Found building(ServerLevel level) {
		long now = level.getGameTime();
		if (now - buildingAt > 100) {
			building = WarehouseBuilding.detect(level, worldPosition);
			buildingAt = now;
			nextSite = null;
		}
		return building;
	}

	/** Where the next floor would be staked (what it would need), for the BUILDING page. */
	io.github.jimbozoomer.jugcraft.blueprint.@Nullable BuildSite nextFloorSite(ServerLevel level, WarehouseBuilding.Found f) {
		if (nextSite == null) {
			var add = io.github.jimbozoomer.jugcraft.blueprint.Blueprint.get(f.stack().addId(), false);
			if (add != null) {
				nextSite = new io.github.jimbozoomer.jugcraft.blueprint.BuildSite(add, f.rotation(), f.addStake());
			}
		}
		return nextSite;
	}

	/** The pallet places with a pallet in them. */
	java.util.Set<BlockPos> occupied() {
		java.util.Set<BlockPos> out = new java.util.HashSet<>();
		for (Stock s : stock.values()) {
			out.addAll(s.places);
		}
		return out;
	}

	public List<Order> orders() {
		return orders;
	}

	public List<Order.Stream> streams() {
		return streams;
	}

	public List<Order.Stream> liquidStreams() {
		return liquidStreams;
	}

	public ReadyPool pool() {
		return pool;
	}

	public PortBuffer port() {
		return port;
	}

	public int portMode() {
		return portMode;
	}

	public int liquidMode() {
		return liquidMode;
	}

	public void setLiquidMode(int mode) {
		liquidMode = Math.max(PORT_OFF, Math.min(PORT_STREAMS, mode));
		setChanged();
	}

	public @Nullable Order order(int id) {
		for (Order o : orders) {
			if (o.id == id) {
				return o;
			}
		}
		return null;
	}

	/** Whether a block is part of this warehouse (as last found). */
	public boolean contains(BlockPos pos) {
		return network.members.contains(pos.asLong());
	}

	boolean reserved(BlockPos place) {
		return reserved.containsKey(place);
	}

	/** Who keeps a place: a drone's id (putting a pallet in), minus an order's (its pallet is out), or null. */
	@Nullable Integer reservedBy(BlockPos place) {
		return reserved.get(place);
	}

	/** Books a place for a drone (its id; game tests book one by hand). */
	public void reserve(BlockPos place, int drone) {
		reserved.put(place.immutable(), drone);
	}

	/** Part W4: whether a pallet is on its way to a place through a lift (on a drone, a landing's bed, a carriage). */
	boolean liftBoundFor(BlockPos dest) {
		for (LiftBound b : liftBound.values()) {
			if (b.dest().equals(dest)) {
				return true;
			}
		}
		if (level != null) {
			for (Network.Landing l : network.landings) {
				if (level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift && (dest.equals(lift.bedDest()) || dest.equals(lift.carDest()))) {
					return true;
				}
			}
		}
		return false;
	}

	void liftBound(int drone, BlockPos dest, int order) {
		liftBound.put(drone, new LiftBound(dest.immutable(), order));
	}

	// ------------------------------------------------------------------ the tick

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		Warehouses.add(level, worldPosition);
		if (changed && now >= nextScan) {
			changed = false;
			scan(level, now);
			// A big warehouse (floor on floor) is found again less often while it is being built.
			nextScan = now + Math.max(20, network.members.size() / 2000);
		}
		if (!primary) {
			return;
		}
		for (Drone d : drones) {
			events(level, d, now);
		}
		if (now % 20 == 0) {
			router.prune(now);
			power(level, now);
			battery(level);
			outbound(level, now);
			signals.values().removeIf(sg -> now - sg[1] > 100);
			int a = analog();
			if (a != lastAnalog) {
				lastAnalog = a;
				PieceBlock.analogChanged(level, worldPosition, getBlockState());
			}
		}
		if (now % 10 == 0) {
			new Planner(this, level).assign(now);
			beacons(level, now);
		}
		if (now % 40 == 0) {
			// The desk's monitors (and anyone's open screen gets the full pages from its menu).
			List<ServerPlayer> near = new ArrayList<>(PlayerLookup.tracking(level, worldPosition));
			if (!near.isEmpty()) {
				TerminalPayload light = snapshot(now, false);
				for (ServerPlayer player : near) {
					ServerPlayNetworking.send(player, light);
				}
			}
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null) {
			Warehouses.remove(level, worldPosition);
		}
	}

	private List<BlockPos> ownBlocks() {
		List<BlockPos> out = new ArrayList<>();
		if (getBlockState().getBlock() instanceof PieceBlock piece) {
			Direction facing = getBlockState().getValue(PieceBlock.FACING);
			for (int p = 0; p < piece.piece.parts(); p++) {
				out.add(worldPosition.offset(piece.piece.offset(facing, p)));
			}
		} else {
			out.add(worldPosition);
		}
		return out;
	}

	/** Finds the warehouse again: its cells, places (with their codes), machines, docks and drones; the stock. */
	private void scan(ServerLevel level, long now) {
		network = Network.scan(level, worldPosition, ownBlocks(), REACH, MAX_BLOCKS);
		scanVersion++;
		primary = network.terminals.stream().allMatch(t -> t.asLong() > worldPosition.asLong());
		adoptSeniorOwner(level);
		// The docks: a drone parked at each that has one; drones whose dock is gone (or emptied) leave the fleet.
		Map<Long, Integer> owners = new HashMap<>();
		boolean fleetChanged = false;
		List<Drone> keep = new ArrayList<>();
		for (BlockPos dockPos : network.docks) {
			if (!(level.getBlockEntity(dockPos) instanceof DockBlockEntity dock) || dock.kind() == null) {
				owners.put(dockPos.asLong(), -1);
				continue;
			}
			Drone d = drones.stream().filter(x -> x.dock.equals(dockPos)).findFirst().orElse(null);
			if (d == null || d.kind != dock.kind()) {
				int id = dock.droneId() > 0 && drones.stream().noneMatch(x -> x.id == dock.droneId()) ? dock.droneId() : nextId++;
				nextId = Math.max(nextId, id + 1);
				Direction out = level.getBlockState(dockPos).getValue(DockBlock.FACING).getOpposite();
				int h = Router.heading(out);
				List<Float> parts = dock.kind() == DroneKind.STACKER ? Kinematics.PARKED : List.of(0F, 0F, 0F);
				Leg.Key pose = new Leg.Key(0, dockPos.getX() + 0.5F, dockPos.getZ() + 0.5F, Router.yaw(h), parts, -1);
				d = new Drone(id, dock.kind(), dockPos, Leg.still(now, dockPos.getY(), pose), dockPos.asLong(), h);
				dock.setDrone(dock.kind(), id);
				fleetChanged = true;
			}
			owners.put(dockPos.asLong(), d.id);
			keep.add(d);
		}
		for (Drone d : drones) {
			if (!keep.contains(d)) {
				lost(level, d, now);
				fleetChanged = true;
			}
		}
		drones.clear();
		drones.addAll(keep);
		router.setCells(network.cells, owners);
		// Book where every drone is (and where its leg will take it).
		for (Drone d : drones) {
			router.release(d.id);
			bookLeg(d, now);
		}
		// Places: their codes onto the racks; the reservations (not saved) again from the drones' jobs, the orders' pallets
		// and the pallets at the hatches; those no longer valid go.
		for (Drone d : drones) {
			if ((d.job == Drone.PUTAWAY || d.job == Drone.RETURN) && d.target != null && network.placeAt.containsKey(d.target)) {
				reserved.putIfAbsent(d.target, d.id);
			}
		}
		for (Order o : orders) {
			if (o.state == Order.FETCHING && o.pallet != null) {
				reserved.putIfAbsent(o.pallet, -o.id);
			}
		}
		for (Network.Vlm v : network.vlms) {
			if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.state() != ReadyStorageBlockEntity.IDLE && rs.from() != null) {
				reserved.putIfAbsent(rs.from(), -Math.max(1, rs.orderId()));
			}
		}
		// Part W5: a consolidation's places: the second pallet's (kept for it, or for the drone on its way to it).
		for (Network.Station st : network.stations) {
			if (level.getBlockEntity(st.origin()) instanceof StationBlockEntity s && s.heldFor() != null) {
				reserved.putIfAbsent(s.heldFor(), Planner.CONSOL_HOLD);
			}
		}
		for (Drone d : drones) {
			if (d.job == Drone.CONSOLIDATE) {
				for (int k = d.done; k < d.leg.events().size(); k++) {
					Leg.Event e = d.leg.events().get(k);
					if (e.type() == Planner.CONSOL_TAKE && network.placeAt.containsKey(e.pos())) {
						reserved.put(e.pos(), d.id);
						break;
					}
				}
			}
		}
		// Part W4: the pallets out on the Drum Emptying Stations.
		for (Network.Station e : network.emptiers) {
			if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.state() != EmptierBlockEntity.IDLE && em.from() != null) {
				reserved.putIfAbsent(em.from(), -Math.max(1, em.orderId()));
			}
		}
		// Part W3: places kept for pallets in the lifts (on a bed, on a carriage).
		for (Network.Landing l : network.landings) {
			if (level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift) {
				for (BlockPos dest : new BlockPos[] {lift.bedDest(), lift.carDest()}) {
					if (dest != null && network.placeAt.containsKey(dest)) {
						reserved.putIfAbsent(dest, Planner.LIFT_HOLD);
					}
				}
			}
		}
		reserved.keySet().removeIf(p -> !network.placeAt.containsKey(p));
		signs(level);
		for (Network.Place p : network.places) {
			if (level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack) {
				String ground = p.level() == 1 ? p.label().substring(0, p.label().lastIndexOf('-')) + "-0" : "";
				rack.setLabels(p.label(), ground);
			}
		}
		recount(level);
		// Ready Storage: each with the extension modules standing on it; their slots make the pool.
		int modules = 0;
		for (Network.Vlm v : network.vlms) {
			if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs) {
				int m = modulesAt(level, v);
				rs.setModules(m, worldPosition);
				modules += m;
			}
		}
		pool.setCapacity(modules * ReadyPool.MODULE);
		if (fleetChanged) {
			sync();
		}
	}

	/** How many modules a Ready Storage has: itself and the extensions stacked on it the same way round. */
	private static int modulesAt(ServerLevel level, Network.Vlm v) {
		int m = 1;
		for (int k = 1; k < 8; k++) {
			BlockPos up = v.origin().above(4 * k);
			BlockState s = level.getBlockState(up);
			if (s.getBlock() instanceof PieceBlock piece && piece.piece == WarehousePiece.READY_STORAGE_EXT && piece.isOrigin(s)
					&& s.getValue(PieceBlock.FACING) == v.facing()) {
				m++;
			} else {
				break;
			}
		}
		return m;
	}

	/** Books a drone's cells from its leg (after finding the warehouse again: the bookings aren't saved). */
	private void bookLeg(Drone d, long now) {
		Leg leg = d.leg;
		List<Leg.Key> keys = leg.keys();
		for (int i = 1; i < keys.size(); i++) {
			long t1 = leg.start() + keys.get(i).t();
			if (t1 < now) {
				continue;
			}
			long t0 = leg.start() + keys.get(i - 1).t();
			for (Leg.Key k : new Leg.Key[] {keys.get(i - 1), keys.get(i)}) {
				router.book(BlockPos.asLong((int) Math.floor(k.x()), leg.y(), (int) Math.floor(k.z())), t0, t1, d.id);
			}
		}
		router.book(d.cell, Math.max(now, leg.end()), Long.MAX_VALUE, d.id);
	}

	/** A drone that left the fleet (its dock broken or emptied): what it was carrying drops where it is. */
	private void lost(ServerLevel level, Drone d, long now) {
		for (Order o : orders) {
			if (o.state == Order.FETCHING && o.pallet != null && (o.pallet.equals(d.source) || o.pallet.equals(d.target))) {
				reserved.remove(o.pallet, -o.id);
				o.state = Order.QUEUED;
				o.pallet = null;
			}
		}
		router.release(d.id);
		reserved.values().removeIf(id -> id == d.id);
		Leg.Key at = d.leg.at(now);
		List<ItemStack> drop = new ArrayList<>();
		if (d.held != null) {
			drop.addAll(d.held.stacks());
		}
		if (d.carried != null) {
			drop.addAll(d.carried.stacks());
			drop.add(new ItemStack(JugcraftWarehouse.PALLET));
		}
		for (ItemStack s : drop) {
			Containers.dropItemStack(level, at.x(), d.leg.y() + 0.5, at.z(), s);
		}
	}

	// ---- taken at once (the Drone Tower's purchases from the storage piped to its Cargo Exchange: tower/TowerSupply)

	/**
	 * How many of {@code item} lie on the racks' pallets that no drone, order or lift has booked: what {@link #takeNow}
	 * can take at once (Justin, 10 November 2026: "If the system is hooked up to there, it should be able to detect
	 * everything in the warehouse and drag it from there too"; his pick: "Take instantly").
	 */
	public long free(ServerLevel level, net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item) {
		long n = 0;
		for (Network.Place p : network.places) {
			if (reserved.containsKey(p.pos()) || !level.isLoaded(p.pos()) || !(level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack)
					|| rack.load() == null) {
				continue;
			}
			for (CaseLoad c : rack.load().cases()) {
				if (c.count() > 0 && item.matches(c.item())) {
					n += c.count();
				}
			}
		}
		return n;
	}

	/**
	 * Takes up to {@code count} of {@code item} at once off pallets nobody has booked (the fullest last, so part cases go
	 * first); the books and the stock index follow. Returns how many were taken.
	 */
	public long takeNow(ServerLevel level, net.fabricmc.fabric.api.transfer.v1.item.ItemVariant item, long count) {
		long left = count;
		for (Network.Place p : network.places) {
			if (left <= 0) {
				break;
			}
			if (reserved.containsKey(p.pos()) || !level.isLoaded(p.pos()) || !(level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack)
					|| rack.load() == null) {
				continue;
			}
			List<CaseLoad> cases = new ArrayList<>(rack.load().cases());
			boolean changed = false;
			for (int i = 0; i < cases.size() && left > 0; i++) {
				CaseLoad c = cases.get(i);
				if (c.count() <= 0 || !item.matches(c.item())) {
					continue;
				}
				long took = Math.min(left, c.count());
				left -= took;
				changed = true;
				if (took >= c.count()) {
					cases.remove(i--);
				} else {
					cases.set(i, new CaseLoad(c.item(), (int) (c.count() - took)));
				}
			}
			if (changed) {
				PalletLoad load = cases.isEmpty() ? null : new PalletLoad(cases);
				rack.setLoad(load);
				booked(p.pos(), load);
			}
		}
		if (left < count) {
			recount(level);
			mapColumnsAt = 0;
			setChanged();
		}
		return count - left;
	}

	/** The stock index, counted again from the racks' pallets. */
	private void recount(ServerLevel level) {
		stock.clear();
		placesUsed = 0;
		for (Network.Place p : network.places) {
			if (level.isLoaded(p.pos()) && level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack && rack.load() != null) {
				placesUsed++;
				add(rack.load(), p.pos());
			}
		}
	}

	private void add(PalletLoad load, BlockPos place) {
		for (CaseLoad c : load.cases()) {
			if (c.count() <= 0) {
				// (An emptied drum riding back on its pallet: nothing in it to count.)
				continue;
			}
			Stock s = stock.computeIfAbsent(ItemVariant.of(c.item()), v -> new Stock(c.item()));
			s.count += c.count();
			s.cases++;
			if (!s.places.contains(place)) {
				s.places.add(place);
			}
		}
	}

	/** Carries out a drone's leg's events whose ticks have come. */
	private void events(ServerLevel level, Drone d, long now) {
		List<Leg.Event> events = d.leg.events();
		while (d.done < events.size() && d.leg.start() + events.get(d.done).t() <= now) {
			Leg.Event e = events.get(d.done++);
			if (!level.isLoaded(e.pos())) {
				continue;
			}
			switch (e.type()) {
				case Planner.PICK_CASE -> {
					if (level.getBlockEntity(e.pos()) instanceof IntakeBlockEntity intake) {
						d.held = intake.takeHead(now);
					}
				}
				case Planner.PLACE_CASE -> {
					if (d.held != null && level.getBlockEntity(e.pos()) instanceof StationBlockEntity station && !station.addCase(d.held, now)) {
						d.held.stacks().forEach(s -> Containers.dropItemStack(level, e.pos().getX() + 0.5, e.pos().getY() + 1.2, e.pos().getZ() + 0.5, s));
					}
					d.held = null;
				}
				case Planner.WRAP -> {
					if (level.getBlockEntity(e.pos()) instanceof StationBlockEntity station) {
						station.startWrap(now, e.arg());
					}
				}
				case Planner.PICK_PALLET -> {
					if (level.getBlockEntity(e.pos()) instanceof StationBlockEntity station) {
						d.carried = station.takePallet(now);
					} else if (level.getBlockEntity(e.pos()) instanceof FillerBlockEntity filler) {
						d.carried = filler.takePallet(now);
					}
				}
				case Planner.PUT_PALLET -> {
					reserved.remove(e.pos());
					liftBound.remove(d.id);
					if (d.carried != null && level.getBlockEntity(e.pos()) instanceof RackBlockEntity rack && rack.load() == null) {
						rack.setLoad(d.carried);
						booked(e.pos(), d.carried);
						add(d.carried, e.pos());
						placesUsed++;
						Network.Place place = network.placeAt.get(e.pos());
						CaseLoad first = d.carried.cases().get(0);
						int total = d.carried.cases().stream().mapToInt(CaseLoad::count).sum();
						if (!Drums.isLiquid(d.carried)) {
							itemsIn += total;
						}
						log.addFirst(new PutAway(now, first.item(), total, place == null ? "?" : place.label()));
						while (log.size() > LOG) {
							log.removeLast();
						}
						setChanged();
					} else if (d.carried != null) {
						// The place was taken or is gone: the pallet goes down where it is.
						d.carried.stacks().forEach(s -> Containers.dropItemStack(level, e.pos().getX() + 0.5, e.pos().getY() + 0.5, e.pos().getZ() + 0.5, s));
						Containers.dropItemStack(level, e.pos().getX() + 0.5, e.pos().getY() + 0.5, e.pos().getZ() + 0.5, new ItemStack(JugcraftWarehouse.PALLET));
					}
					d.carried = null;
				}
				case Planner.TAKE_PLACE -> {
					// An order's pallet out of its place (the place stays kept for it to come back to).
					Order o = order(e.arg());
					if (level.getBlockEntity(e.pos()) instanceof RackBlockEntity rack && rack.load() != null) {
						d.carried = rack.load().copy();
						rack.setLoad(null);
						booked(e.pos(), null);
						recount(level);
						if (o != null) {
							o.fetched = now;
						}
					} else {
						// Its pallet is gone: the order waits for another.
						reserved.remove(e.pos());
						if (o != null && o.state == Order.FETCHING) {
							o.state = Order.QUEUED;
							o.pallet = null;
						}
					}
				}
				case Planner.PUT_BED -> {
					liftBound.remove(d.id);
					Order o = order(e.arg());
					if (d.carried != null && level.getBlockEntity(e.pos()) instanceof ReadyStorageBlockEntity rs && rs.free()) {
						Order use = o != null && o.open() ? o : new Order(0, null, "", ItemStack.EMPTY, 0, Order.WINDOW, now, "");
						long room = use.id == 0 ? 0 : use.dest == Order.WINDOW ? pool.room(use.item, use.id) : port.room(use.item, use.id);
						rs.begin(d.carried, use, use.pallet != null ? use.pallet : d.source, room, stationTakesBack(level), now);
						if (o != null && o.open()) {
							o.state = Order.PICKING;
							o.atHatch = now;
						}
						setChanged();
					} else {
						if (d.carried != null) {
							drop(level, e.pos(), d.carried);
						}
						if (d.source != null) {
							reserved.remove(d.source);
						}
						if (o != null && o.open()) {
							o.state = Order.QUEUED;
							o.pallet = null;
						}
					}
					d.carried = null;
				}
				case Planner.TAKE_BED -> {
					if (level.getBlockEntity(e.pos()) instanceof ReadyStorageBlockEntity rs) {
						d.carried = rs.takeBed();
					}
				}
				case Planner.PUT_BACK -> {
					reserved.remove(e.pos());
					if (d.carried != null && !d.carried.isEmpty() && level.getBlockEntity(e.pos()) instanceof RackBlockEntity rack && rack.load() == null) {
						rack.setLoad(d.carried);
						booked(e.pos(), d.carried);
						recount(level);
						setChanged();
					} else if (d.carried != null) {
						drop(level, e.pos(), d.carried);
					}
					d.carried = null;
				}
				case Planner.PUT_STATION -> {
					// (Only an empty pallet goes into a magazine: one still with cases on it goes down where it is.)
					if (d.carried != null && d.carried.isEmpty() && level.getBlockEntity(e.pos()) instanceof StationBlockEntity station
							&& station.receivePallet(now)) {
						d.carried = null;
					} else if (d.carried != null) {
						drop(level, e.pos(), d.carried);
						d.carried = null;
					}
				}
				case Planner.PUT_LIFT -> {
					LiftBound bound = liftBound.remove(d.id);
					if (d.carried != null && level.getBlockEntity(e.pos()) instanceof LiftBlockEntity lift && lift.bed() == null) {
						lift.putBed(d.carried, bound == null ? null : bound.dest(), bound == null ? 0 : bound.order());
					} else if (d.carried != null) {
						drop(level, e.pos(), d.carried);
						if (bound != null) {
							reserved.remove(bound.dest(), Planner.LIFT_HOLD);
						}
					}
					d.carried = null;
					setChanged();
				}
				case Planner.TAKE_LIFT -> {
					if (level.getBlockEntity(e.pos()) instanceof LiftBlockEntity lift && lift.bed() != null) {
						int order = lift.bedOrder();
						BlockPos dest = lift.bedDest();
						d.carried = lift.takeBed();
						if (order != 0 && dest != null) {
							liftBound.put(d.id, new LiftBound(dest, order));
						}
					}
				}
				case Planner.PUT_EMPTIER -> {
					liftBound.remove(d.id);
					Order o = order(e.arg());
					if (d.carried != null && level.getBlockEntity(e.pos()) instanceof EmptierBlockEntity em && em.free()) {
						// (Its order gone meanwhile: nothing is pumped, the pallet waits on the platform to go back.)
						boolean live = o != null && o.open();
						em.begin(d.carried, live ? o : new Order(0, null, "", ItemStack.EMPTY, 0, Order.WINDOW, now, ""),
								live && o.pallet != null ? o.pallet : d.source, now);
						if (live) {
							o.state = Order.PICKING;
							o.atHatch = now;
						}
						setChanged();
					} else {
						if (d.carried != null) {
							drop(level, e.pos(), d.carried);
						}
						if (d.source != null) {
							reserved.remove(d.source);
						}
						if (o != null && o.open()) {
							o.state = Order.QUEUED;
							o.pallet = null;
						}
					}
					d.carried = null;
				}
				case Planner.TAKE_EMPTIER -> {
					if (level.getBlockEntity(e.pos()) instanceof EmptierBlockEntity em) {
						d.carried = em.takeBed();
					}
				}
				case Planner.PUT_FILLER -> {
					if (d.carried != null && level.getBlockEntity(e.pos()) instanceof FillerBlockEntity filler && filler.receivePallet(d.carried, now)) {
						d.carried = null;
					} else if (d.carried != null) {
						drop(level, e.pos(), d.carried);
						d.carried = null;
					}
				}
				// ---- part W5: consolidation (two part pallets onto one at a station) and cycle counts
				case Planner.CONSOL_TAKE -> {
					reserved.remove(e.pos());
					if (level.getBlockEntity(e.pos()) instanceof RackBlockEntity rack && rack.load() != null) {
						d.carried = rack.load().copy();
						rack.setLoad(null);
						booked(e.pos(), null);
						recount(level);
					}
				}
				case Planner.CONSOL_SET -> {
					if (d.carried != null && level.getBlockEntity(e.pos()) instanceof StationBlockEntity station && station.receiveBuild(d.carried, now)) {
						d.carried = null;
					} else if (d.carried != null) {
						// (The station was taken meanwhile: the pallet goes down where it is.)
						drop(level, e.pos(), d.carried);
						d.carried = null;
					}
				}
				case Planner.CONSOL_PICK -> {
					// The stacker lifts the top case off the pallet the pallet drone (arg) holds out to it.
					Drone holder = drones.stream().filter(x -> x.id == e.arg()).findFirst().orElse(null);
					if (holder != null && holder.carried != null && !holder.carried.isEmpty()) {
						d.held = holder.carried.cases().remove(holder.carried.size() - 1);
					}
				}
				case Planner.CONSOL_DONE -> {
					if (level.getBlockEntity(e.pos()) instanceof StationBlockEntity station) {
						station.consolidated(now);
					}
				}
				case Planner.COUNT -> countPlace(level, e.pos(), now);
				case Planner.DOCK -> {
					d.docked = true;
					if (level.getBlockEntity(e.pos()) instanceof DockBlockEntity dock) {
						dock.status(true, d.battery);
					}
				}
				case Planner.UNDOCK -> {
					d.docked = false;
					if (level.getBlockEntity(e.pos()) instanceof DockBlockEntity dock) {
						dock.status(false, d.battery);
					}
				}
				default -> {
				}
			}
		}
	}

	/** Whether an order's pallet is in a lift now (on a landing's bed, or on its carriage). */
	private boolean inLift(Level level, int order) {
		for (Network.Landing l : network.landings) {
			if (level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift && (lift.bed() != null && lift.bedOrder() == order
					|| lift.car() != null && lift.carOrder() == order)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Part W3: the drone crossings' beacons flash (on and off each half second) while a drone is booked through a
	 * crossing beside them in the next two seconds.
	 */
	private void beacons(ServerLevel level, long now) {
		// (Part W5: on the LIGHTS · CONTROL circuit: dark while it is off or the warehouse has no power.)
		boolean phase = (now / 10) % 2 == 0 && powered(WarehousePower.Circuit.LIGHTS);
		for (BlockPos b : network.beacons) {
			BlockState s = level.getBlockState(b);
			if (!(s.getBlock() instanceof BeaconBlock)) {
				continue;
			}
			boolean coming = false;
			for (int dx = -2; dx <= 2 && !coming; dx++) {
				for (int dz = -2; dz <= 2 && !coming; dz++) {
					long cell = BlockPos.asLong(b.getX() + dx, b.getY(), b.getZ() + dz);
					coming = network.crossings.contains(cell) && !router.free(cell, now, now + 40, Integer.MIN_VALUE);
				}
			}
			boolean lit = coming && phase;
			if (s.getValue(BeaconBlock.LIT) != lit) {
				level.setBlock(b, s.setValue(BeaconBlock.LIT, lit), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** Part W3: each aisle sign shows the aisle it hangs over (the row of places it is at the end of), its bays and levels. */
	private void signs(ServerLevel level) {
		for (BlockPos sign : network.signs) {
			if (!(level.getBlockEntity(sign) instanceof AisleSignBlockEntity be)) {
				continue;
			}
			// The aisle whose places come nearest the sign (on its floor), across the aisle cell it hangs over.
			Network.Aisle best = null;
			double bestDist = Double.MAX_VALUE;
			for (Network.Aisle a : network.aisles) {
				for (BlockPos p : a.places()) {
					if (p.getY() > sign.getY() || sign.getY() - p.getY() > 8) {
						continue;
					}
					double dist = Math.abs(p.getX() - sign.getX()) + Math.abs(p.getZ() - sign.getZ());
					if (dist < bestDist) {
						best = a;
						bestDist = dist;
					}
				}
			}
			if (best == null) {
				be.set("", "", "");
				continue;
			}
			int maxBay = 0, maxLevel = 0;
			for (BlockPos p : best.places()) {
				Network.Place place = network.placeAt.get(p);
				if (place != null) {
					String[] parts = place.label().split("-");
					maxBay = Math.max(maxBay, Integer.parseInt(parts[1]));
					maxLevel = Math.max(maxLevel, place.level());
				}
			}
			be.set("AISLE " + best.name(), String.format(java.util.Locale.ROOT, "BAYS 01 - %02d", maxBay), "LEVELS 0 - " + maxLevel);
		}
	}

	private static void drop(ServerLevel level, BlockPos pos, PalletLoad load) {
		for (ItemStack s : load.stacks()) {
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
		}
		Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(JugcraftWarehouse.PALLET));
	}

	/** Whether any station could take an empty pallet back now. */
	boolean stationTakesBack(ServerLevel level) {
		for (Network.Station st : network.stations) {
			if (level.getBlockEntity(st.origin()) instanceof StationBlockEntity s && s.takesBack()) {
				return true;
			}
		}
		return false;
	}

	/** Part W5: lets go of the places a kind of hold keeps (Planner.CONSOL_HOLD) but those still waited for. */
	void releaseHolds(int hold, java.util.Set<BlockPos> keep) {
		reserved.entrySet().removeIf(e -> e.getValue() == hold && !keep.contains(e.getKey()));
	}

	/** Lets go of a place only if this hold keeps it. */
	void unreserveIf(@Nullable BlockPos place, int hold) {
		if (place != null) {
			reserved.remove(place, hold);
		}
	}

	void unreserve(@Nullable BlockPos place) {
		if (place != null) {
			reserved.remove(place);
		}
	}

	// ------------------------------------------------------------------ part W2: orders

	/** How many of an item can still be ordered: what is stored, less what open orders still want of it. */
	public long available(ItemStack item) {
		Stock s = stock.get(ItemVariant.of(item));
		long have = (s == null ? 0 : s.count) + inTransit(item);
		for (Order o : orders) {
			if (o.open() && o.sameItem(item)) {
				have -= o.remaining();
			}
		}
		return Math.max(0, have);
	}

	/** How many of an item are on pallets out of their places: on drones, at the hatches. */
	long inTransit(ItemStack item) {
		long n = 0;
		List<PalletLoad> loads = new ArrayList<>();
		for (Drone d : drones) {
			if (d.carried != null) {
				loads.add(d.carried);
			}
		}
		if (level != null) {
			for (Network.Vlm v : network.vlms) {
				if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.pallet() != null) {
					loads.add(rs.pallet());
				}
			}
			for (Network.Station e : network.emptiers) {
				if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.pallet() != null) {
					loads.add(em.pallet());
				}
			}
			// Part W5: a consolidation's pallet on a station (stock out of the racks till it is put away again).
			for (Network.Station st : network.stations) {
				if (level.getBlockEntity(st.origin()) instanceof StationBlockEntity s && s.restock()) {
					loads.add(s.build());
				}
			}
		}
		for (PalletLoad l : loads) {
			for (CaseLoad c : l.cases()) {
				if (c.sameItem(item)) {
					n += c.count();
				}
			}
		}
		// (Part W5: a case a stacker is moving from one pallet to another in a consolidation.)
		for (Drone d : drones) {
			if (d.held != null && d.job == Drone.CONSOLIDATE && d.held.sameItem(item)) {
				n += d.held.count();
			}
		}
		return n;
	}

	/** Open orders a player may have waiting at once. */
	public static final int MAX_OPEN = 16;

	public int openOrders(java.util.@Nullable UUID owner) {
		int n = 0;
		for (Order o : orders) {
			if (o.live() && o.stream == 0 && java.util.Objects.equals(o.owner, owner)) {
				n++;
			}
		}
		return n;
	}

	/** Places an order (as much of it as can be had); null when there is none to be had. */
	public @Nullable Order placeOrder(java.util.@Nullable UUID owner, String who, ItemStack item, long amount, int dest, String from, long now) {
		long n = Math.min(amount, available(item));
		if (n <= 0 || item.isEmpty()) {
			return null;
		}
		Order o = new Order(nextOrder++, owner, who, item, n, dest == Order.PIPES ? Order.PIPES : Order.WINDOW, now, from);
		orders.add(o);
		setChanged();
		return o;
	}

	private static boolean mayChange(Order o, @Nullable Player player) {
		return player == null || player.isCreative() || o.owner == null || o.owner.equals(player.getUUID());
	}

	/** Cancels an open order (its owner, or anyone in creative); what is already in Ready Storage stays there. */
	public boolean cancel(int id, @Nullable Player player) {
		Order o = order(id);
		if (o == null || !o.open() || !mayChange(o, player)) {
			return false;
		}
		o.state = o.filled > 0 ? (o.dest == Order.WINDOW ? Order.READY : Order.SENT) : Order.CANCELLED;
		setChanged();
		return true;
	}

	/** Sends an open order to the Output Port instead of the window. */
	public boolean toPipes(int id, @Nullable Player player) {
		Order o = order(id);
		if (o == null || !o.open() || !mayChange(o, player)) {
			return false;
		}
		o.dest = Order.PIPES;
		if (Drums.isLiquid(o.item) && level != null) {
			for (Network.Station e : network.emptiers) {
				if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em) {
					em.toPipes(id);
				}
			}
		}
		setChanged();
		return true;
	}

	/** Part W4: a liquid order ready at the fill point (its share in a holding tank) sent to pipes instead. */
	public boolean liquidToPipes(int id, @Nullable Player player) {
		Order o = order(id);
		if (o == null || !Drums.isLiquid(o.item) || !mayChange(o, player) || o.state != Order.READY) {
			return false;
		}
		o.dest = Order.PIPES;
		o.state = Order.SENT;
		if (level != null) {
			for (Network.Station e : network.emptiers) {
				if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em) {
					em.toPipes(id);
				}
			}
		}
		setChanged();
		return true;
	}

	public void setPortMode(int mode) {
		portMode = Math.max(PORT_OFF, Math.min(PORT_STREAMS, mode));
		setChanged();
	}

	public boolean addStream(ItemStack item, int rate, long reserve, @Nullable Player player) {
		boolean liquid = Drums.isLiquid(item);
		List<Order.Stream> list = liquid ? liquidStreams : streams;
		if (item.isEmpty() || list.size() >= MAX_STREAMS || list.stream().anyMatch(st -> ItemStack.isSameItemSameComponents(st.item, item))) {
			return false;
		}
		int r = liquid ? Math.max(LIQUID_RATE_MIN, Math.min(LIQUID_RATE_MAX, rate)) : Math.max(1, Math.min(64, rate));
		list.add(new Order.Stream(item, r, Math.max(0, reserve), player == null ? null : player.getUUID(),
				player == null ? "" : player.getName().getString()));
		setChanged();
		return true;
	}

	public void setLiquidStream(int k, int rate, long reserve) {
		if (k >= 0 && k < liquidStreams.size()) {
			liquidStreams.get(k).rate = Math.max(LIQUID_RATE_MIN, Math.min(LIQUID_RATE_MAX, rate));
			liquidStreams.get(k).reserve = Math.max(0, reserve);
			setChanged();
		}
	}

	/**
	 * Removes a liquid stream: its refill not yet under way is cancelled, one under way (or pumped, in a holding tank)
	 * finishes as a plain pipe order; the streams after it move up a place (their refills' and shares' tags with them).
	 */
	public void removeLiquidStream(int k) {
		if (k < 0 || k >= liquidStreams.size()) {
			return;
		}
		liquidStreams.remove(k);
		int tag = LIQUID_STREAM + k + 1;
		for (Order o : orders) {
			if (o.stream == tag) {
				if (o.state == Order.QUEUED) {
					o.state = o.filled > 0 ? Order.SENT : Order.CANCELLED;
				}
				o.stream = 0;
			} else if (o.stream > tag) {
				o.stream--;
			}
		}
		if (level != null) {
			for (Network.Station e : network.emptiers) {
				if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em) {
					em.streamRemoved(tag);
				}
			}
		}
		setChanged();
	}

	/** A liquid stream's rate (mB a second): from a tenth of a bucket a second to ten. */
	public static final int LIQUID_RATE_MIN = 100, LIQUID_RATE_MAX = 10_000;

	public void setStream(int k, int rate, long reserve) {
		if (k >= 0 && k < streams.size()) {
			streams.get(k).rate = Math.max(1, Math.min(64, rate));
			streams.get(k).reserve = Math.max(0, reserve);
			setChanged();
		}
	}

	public void removeStream(int k) {
		if (k >= 0 && k < streams.size()) {
			streams.remove(k);
			setChanged();
		}
	}

	/**
	 * A Ready Storage's pick cell is done with a pallet (ReadyStorageBlockEntity's timeline): what was lifted off goes to
	 * Ready Storage's pool (for the window) or the port's buffer (for pipes), as far as there is room; the order is
	 * READY or SENT once it has all it wants, else it waits for another pallet.
	 */
	void picked(ReadyStorageBlockEntity rs, PalletLoad pallet, List<Integer> take, int orderId, long now) {
		Order o = order(orderId);
		long moved = 0;
		for (int i = 0; i < pallet.size() && i < take.size(); i++) {
			int n = take.get(i);
			if (n <= 0 || o == null || !o.open()) {
				continue;
			}
			CaseLoad c = pallet.cases().get(i);
			long in = o.dest == Order.WINDOW ? pool.add(c.item(), Math.min(n, c.count()), o.owner, o.id) : port.add(c.item(), Math.min(n, c.count()), o.id);
			if (in > 0) {
				rs.lessen(i, (int) in);
			}
			moved += in;
		}
		if (o != null && o.open()) {
			o.filled += moved;
			o.pallet = null;
			if (o.filled >= o.amount) {
				o.state = o.dest == Order.WINDOW ? Order.READY : Order.SENT;
				o.readyAt = now;
			} else {
				o.state = Order.QUEUED;
			}
		}
		setChanged();
	}

	/** Once a second: orders collected or sent out are done, streams order their refills, old orders go. */
	private void outbound(ServerLevel level, long now) {
		boolean any = false;
		for (Network.Vlm v : network.vlms) {
			if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.state() != ReadyStorageBlockEntity.IDLE) {
				any = true;
			}
		}
		busy[busyAt++ % busy.length] = any;
		for (Order o : orders) {
			boolean liquid = Drums.isLiquid(o.item);
			if (liquid ? (o.state == Order.READY || o.state == Order.SENT) && !inTank(level, o.id)
					: o.state == Order.READY && !pool.hasOrder(o.id) || o.state == Order.SENT && !port.has(o.id)) {
				o.state = Order.DONE;
				setChanged();
			}
		}
		// An order whose pallet is no longer on its way (a drone lost, a Ready Storage broken) waits for another.
		for (Order o : orders) {
			if (o.state == Order.FETCHING) {
				boolean going = false;
				for (Drone d : drones) {
					if ((d.job == Drone.FETCH || d.job == Drone.TO_LIFT) && o.pallet != null && o.pallet.equals(d.source)) {
						going = true;
					}
					LiftBound bound = liftBound.get(d.id);
					if (bound != null && bound.order() == o.id && (d.job == Drone.TO_LIFT || d.job == Drone.FROM_LIFT)) {
						going = true;
					}
				}
				going |= inLift(level, o.id);
				if (!going) {
					if (o.pallet != null) {
						reserved.remove(o.pallet, -o.id);
					}
					o.state = Order.QUEUED;
					o.pallet = null;
				}
			} else if (o.state == Order.PICKING) {
				boolean at = false;
				for (Network.Vlm v : network.vlms) {
					if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.state() == ReadyStorageBlockEntity.WORKING && rs.orderId() == o.id) {
						at = true;
					}
				}
				for (Network.Station e : network.emptiers) {
					if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.state() == EmptierBlockEntity.WORKING && em.orderId() == o.id) {
						at = true;
					}
				}
				if (!at) {
					o.state = Order.QUEUED;
				}
			}
		}
		for (int j = 0; j < streams.size() + liquidStreams.size(); j++) {
			boolean liquid = j >= streams.size();
			int k = liquid ? j - streams.size() : j;
			Order.Stream st = liquid ? liquidStreams.get(k) : streams.get(k);
			st.allowance = Math.min(st.rate * 2.0, st.allowance + st.rate);
			Stock have = stock.get(ItemVariant.of(st.item));
			long count = have == null ? 0 : have.count;
			// (Part W5: a Signal Box's STREAMS OFF holds them all.)
			st.held = count <= st.reserve || streamsOff();
			if ((liquid ? liquidMode : portMode) != PORT_STREAMS || st.held) {
				continue;
			}
			int tag = (liquid ? LIQUID_STREAM : 0) + k + 1;
			boolean pending = orders.stream().anyMatch(o -> o.stream == tag && o.open());
			long queued = liquid ? streamInTank(level, tag) : port.count(st.item);
			if (!pending && queued < st.rate * 4L) {
				// (A liquid stream: a pallet's worth at a time.)
				long n = Math.min(liquid ? Drums.CAPACITY * Drums.PER_PALLET : CaseLoad.capacity(st.item), count - st.reserve);
				Order o = placeOrder(st.owner, st.who, st.item, n, Order.PIPES, "STREAM", now);
				if (o != null) {
					o.stream = tag;
				}
			}
		}
		// Keep the open ones and the last few done.
		int done = 0;
		for (int i = orders.size() - 1; i >= 0; i--) {
			if (!orders.get(i).live() && ++done > KEEP_DONE) {
				orders.remove(i);
			}
		}
	}

	/** Part W4: whether a liquid order still has liquid in a holding tank. */
	private boolean inTank(ServerLevel level, int id) {
		for (Network.Station e : network.emptiers) {
			if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.hasOrder(id)) {
				return true;
			}
		}
		return false;
	}

	/** What the holding tanks hold for a stream (mB). */
	private long streamInTank(ServerLevel level, int tag) {
		long n = 0;
		for (Network.Station e : network.emptiers) {
			if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em) {
				for (EmptierBlockEntity.Share s : em.shares()) {
					if (s.stream == tag) {
						n += Drums.mb(s.amount);
					}
				}
			}
		}
		return n;
	}

	/**
	 * Part W4: a Drum Emptying Station's pump is done with a pallet (EmptierBlockEntity's run): what it pumped into the
	 * holding tank is the order's; the order is READY (for the fill point) or SENT (for pipes) once it has all it wants,
	 * else it waits for another pallet.
	 */
	void pumped(EmptierBlockEntity em, int orderId, long moved, long now) {
		Order o = order(orderId);
		if (o != null && o.open()) {
			o.filled += moved;
			o.pallet = null;
			if (o.filled >= o.amount) {
				o.state = o.dest == Order.WINDOW ? Order.READY : Order.SENT;
				o.readyAt = now;
			} else {
				o.state = Order.QUEUED;
			}
		}
		setChanged();
	}

	/** The hatches' share of the last minute busy (0..1). */
	public float hatchBusy() {
		int n = 0;
		for (boolean b : busy) {
			if (b) {
				n++;
			}
		}
		return n / (float) busy.length;
	}

	/** An order as the screens show it: who, what, where it is now, how far along. */
	TerminalPayload.OrderLine line(Order o, long now) {
		String where = "";
		float progress = 0;
		boolean liquid = Drums.isLiquid(o.item);
		switch (o.state) {
			case Order.QUEUED -> {
				int ahead = 0;
				for (Order x : orders) {
					if (x == o) {
						break;
					}
					if (x.open()) {
						ahead++;
					}
				}
				where = ahead == 0 ? "next" : ordinal(ahead + 1) + " in line";
			}
			case Order.FETCHING -> {
				progress = 0.2F;
				if (level != null && inLift(level, o.id)) {
					where = "in the lift";
					progress = 0.35F;
				}
				for (Drone d : drones) {
					if (d.job == Drone.FETCH && o.pallet != null && o.pallet.equals(d.source)) {
						Network.Place place = network.placeAt.get(o.pallet);
						where = d.name() + (o.fetched > 0 ? (liquid ? " to the emptying station" : " to the hatch") : " -> " + (place == null ? "?" : place.label()));
						progress = o.fetched > 0 ? 0.4F : 0.2F;
					}
				}
			}
			case Order.PICKING -> {
				where = liquid ? "pumping into the holding tank" : "at the hatch";
				progress = 0.6F;
				for (Network.Station e : network.emptiers) {
					if (liquid && level != null && level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.orderId() == o.id && em.plan() != null) {
						progress = 0.5F + 0.5F * Math.min(1, (now - em.plan().start()) / (float) Math.max(1, EmptierMotion.duration(em.plan())));
					}
				}
				for (Network.Vlm v : network.vlms) {
					if (level != null && level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.orderId() == o.id && rs.duration() > 0) {
						progress = 0.5F + 0.5F * Math.min(1, (now - rs.start()) / (float) rs.duration());
					}
				}
			}
			case Order.READY -> {
				where = liquid ? "at the fill point" : "in Ready Storage";
				progress = 1;
			}
			case Order.SENT -> {
				where = liquid ? "going out of the OUT flange" : "in the port's buffer";
				progress = 1;
			}
			default -> progress = 1;
		}
		return new TerminalPayload.OrderLine(o.id, o.who, o.item, o.amount, o.filled, o.dest, o.state, where, progress,
				List.of(o.placed, o.fetched, o.atHatch, o.readyAt), o.stream, o.owner == null ? "" : o.owner.toString(), o.rush);
	}

	private static String ordinal(int n) {
		return n + (n % 100 >= 11 && n % 100 <= 13 ? "th" : switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		});
	}

	/** The orders' lines and Ready Storage's and the port's state, for the screens. */
	public List<TerminalPayload.OrderLine> orderLines(long now) {
		List<TerminalPayload.OrderLine> out = new ArrayList<>();
		for (Order o : orders) {
			if (o.live() && out.size() < LINES) {
				out.add(line(o, now));
			}
		}
		for (Order o : orders) {
			if (!o.live() && out.size() < LINES) {
				out.add(line(o, now));
			}
		}
		return out;
	}

	/** Order lines sent to the screens at most. */
	private static final int LINES = 64;

	public TerminalPayload.Outbound outboundView() {
		String hatch = network.vlms.isEmpty() ? "NONE" : "IDLE";
		if (level != null) {
			for (Network.Vlm v : network.vlms) {
				if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.state() != ReadyStorageBlockEntity.IDLE) {
					hatch = rs.state() == ReadyStorageBlockEntity.BED ? "PALLET OUT" : "PICKING";
				}
			}
		}
		long liquids = 0;
		for (Stock st : stock.values()) {
			if (Drums.isLiquid(st.item)) {
				liquids += st.count;
			}
		}
		StringBuilder in = new StringBuilder();
		if (level != null) {
			for (Network.Station f : network.fillers) {
				if (level.getBlockEntity(f.origin()) instanceof FillerBlockEntity filler && !filler.liquid().isBlank()) {
					String name = Drums.name(filler.liquid()).getString();
					if (in.indexOf(name) < 0) {
						in.append(in.isEmpty() ? "" : ", ").append(name);
					}
				}
			}
		}
		return new TerminalPayload.Outbound(pool.used(), pool.capacity(), network.vlms.size(), Math.round(hatchBusy() * 100), hatch, portMode,
				port.used(), streams.size(), liquids, network.emptiers.size(), network.fillers.size(), liquidMode, in.toString());
	}

	private void battery(ServerLevel level) {
		long now = level.getGameTime();
		for (Drone d : drones) {
			boolean charging = d.docked && now >= d.leg.end();
			// (Part W5: the docks charge as fast as there is power for them.)
			d.battery = Math.max(0, Math.min(1, d.battery + (charging ? Planner.CHARGE * chargeFactor() : -Planner.DRAIN * (d.busy(now) ? 1 : 0.2F)) * 20));
			if (level.isLoaded(d.dock) && level.getBlockEntity(d.dock) instanceof DockBlockEntity dock) {
				dock.status(d.docked, d.battery);
			}
		}
	}

	/** A drone started a leg: the players near get it. */
	void legStarted(Drone d) {
		setChanged();
		if (level != null && d.job != Drone.IDLE && d.job != Drone.HOME && d.job != Drone.COUNT && d.job != Drone.CONSOLIDATE) {
			lastWork = level.getGameTime();
		}
		if (level instanceof ServerLevel server) {
			DronePayload payload = new DronePayload(worldPosition, d.view());
			for (ServerPlayer player : PlayerLookup.tracking(server, worldPosition)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** On the client: a drone's new leg. */
	public void receive(Drone.View view) {
		List<Drone.View> fleet = new ArrayList<>(clientFleet);
		fleet.removeIf(v -> v.id() == view.id());
		fleet.add(view);
		clientFleet = fleet;
	}

	// ------------------------------------------------------------------ part W5: access, power, control, the map

	/** Whether a player may use the warehouse (its panels, terminal, signal boxes): its owner, the owner's party in PARTY. */
	public boolean mayUse(Player player) {
		if (owner == null || player.getAbilities().instabuild || owner.equals(player.getUUID())) {
			return true;
		}
		return useMode == io.github.jimbozoomer.jugcraft.party.UseMode.PARTY
				&& io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner, player.getUUID());
	}

	/** Whether a player may change who uses it (its owner only). */
	public boolean mayChangeAccess(Player player) {
		return owner == null || player.getAbilities().instabuild || owner.equals(player.getUUID());
	}

	public void deny(Player player) {
		player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.not_yours", ownerName.isEmpty() ? "?" : ownerName));
	}

	public java.util.@Nullable UUID owner() {
		return owner;
	}

	public String ownerName() {
		return ownerName;
	}

	public io.github.jimbozoomer.jugcraft.party.UseMode useMode() {
		return useMode;
	}

	/** Its owner: who placed it (or staked the building), else the first player to open it. */
	public void setOwner(Player player) {
		owner = player.getUUID();
		ownerName = player.getName().getString();
		ownedSince = level == null ? 1 : Math.max(1, level.getGameTime());
		setChanged();
	}

	/** Whether a player (by id: a Signal Box's) may use the warehouse. */
	public boolean mayUse(java.util.UUID id) {
		if (owner == null || owner.equals(id)) {
			return true;
		}
		return useMode == io.github.jimbozoomer.jugcraft.party.UseMode.PARTY && io.github.jimbozoomer.jugcraft.party.JugcraftParties.sameParty(owner, id);
	}

	/**
	 * Part W5: a warehouse stays its first owner's: a terminal joined to it later (which may come to run it) takes that
	 * owner and access on, so a stranger can't take a warehouse over by putting down a terminal of their own.
	 */
	private void adoptSeniorOwner(ServerLevel level) {
		TerminalBlockEntity senior = owner == null ? null : this;
		for (BlockPos p : network.terminals) {
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof TerminalBlockEntity t && t != this && t.owner != null
					&& (senior == null || t.ownedSince < senior.ownedSince || t.ownedSince == senior.ownedSince
					&& p.asLong() < senior.getBlockPos().asLong())) {
				senior = t;
			}
		}
		if (senior != null && senior != this) {
			owner = senior.owner;
			ownerName = senior.ownerName;
			useMode = senior.useMode;
			ownedSince = senior.ownedSince;
			setChanged();
		}
	}

	long consolPauseUntil() {
		return consolPauseUntil;
	}

	/** A consolidation could not be finished: none is started for five minutes. */
	void consolidationFailed(long now) {
		consolPauseUntil = now + 6000;
	}

	public boolean setUseMode(Player player, boolean party) {
		if (!mayChangeAccess(player)) {
			deny(player);
			return false;
		}
		useMode = party ? io.github.jimbozoomer.jugcraft.party.UseMode.PARTY : io.github.jimbozoomer.jugcraft.party.UseMode.PERSONAL;
		player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.access." + useMode.id()));
		setChanged();
		return true;
	}

	/** Tests and the demo: power without a switchboard. */
	public void setUnlimitedPower(boolean on) {
		unlimitedPower = on;
		setChanged();
	}

	public boolean unlimitedPower() {
		return unlimitedPower;
	}

	/** Whether a machine on a circuit may start a cycle now (switched on, not shed, the warehouse powered). */
	public boolean powered(WarehousePower.Circuit c) {
		if (unlimitedPower) {
			return true;
		}
		return (circuitsOn >> c.ordinal() & 1) != 0 && (shedMask >> c.ordinal() & 1) == 0 && poweredNow;
	}

	public boolean lowPower() {
		return !unlimitedPower && (lowPower || !poweredNow);
	}

	public void toggleCircuit(int k) {
		circuitsOn ^= 1 << k;
		setChanged();
	}

	/** How fast the docks charge now (0..1): what was left for them. */
	float chargeFactor() {
		return unlimitedPower ? 1 : (circuitsOn & 1) == 0 ? 0 : chargeFactor;
	}

	/** The switchboards powering this warehouse (the ones whose nearest terminal is this). */
	private List<SwitchboardBlockEntity> boards(ServerLevel level) {
		List<SwitchboardBlockEntity> out = new ArrayList<>();
		for (BlockPos p : Warehouses.switchboards(level).toArray(BlockPos[]::new)) {
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof SwitchboardBlockEntity sb && worldPosition.equals(sb.terminalPos())) {
				out.add(sb);
			}
		}
		return out;
	}

	/**
	 * Each second: what each circuit draws (JE a tick), taken from the switchboards; short of power, the lights and
	 * control first, then the machines (shed whole if there isn't enough for them), then charging (what is left).
	 */
	private void power(ServerLevel level, long now) {
		long[] load = new long[6];
		for (Drone d : drones) {
			if (d.docked && now >= d.leg.end() && d.battery < 0.999F) {
				load[WarehousePower.Circuit.DOCKS.ordinal()] += WarehousePower.CHARGING;
			}
		}
		for (Network.Landing l : network.landings) {
			if (level.getBlockEntity(l.origin()) instanceof LiftBlockEntity lift && lift.controller() == lift && lift.moving()) {
				load[WarehousePower.Circuit.LIFTS.ordinal()] += WarehousePower.LIFT;
			}
		}
		for (Network.Intake in : network.intakes) {
			if (level.getBlockEntity(in.origin()) instanceof IntakeBlockEntity intake && intake.packing() != null) {
				load[WarehousePower.Circuit.STATIONS.ordinal()] += WarehousePower.INTAKE;
			}
		}
		for (Network.Station st : network.stations) {
			if (level.getBlockEntity(st.origin()) instanceof StationBlockEntity s && (s.state() == StationBlockEntity.DISPENSING
					|| s.state() == StationBlockEntity.WRAPPING || s.state() == StationBlockEntity.RETURNING)) {
				load[WarehousePower.Circuit.STATIONS.ordinal()] += WarehousePower.STATION;
			}
		}
		for (Network.Vlm v : network.vlms) {
			if (level.getBlockEntity(v.origin()) instanceof ReadyStorageBlockEntity rs && rs.state() != ReadyStorageBlockEntity.IDLE) {
				load[WarehousePower.Circuit.READY_STORAGE.ordinal()] += WarehousePower.READY;
			}
		}
		for (Network.Station f : network.fillers) {
			if (level.getBlockEntity(f.origin()) instanceof FillerBlockEntity fe && fe.state() != FillerBlockEntity.EMPTY && fe.state() != FillerBlockEntity.READY) {
				load[WarehousePower.Circuit.LIQUIDS.ordinal()] += WarehousePower.FILLER;
			}
		}
		for (Network.Station e : network.emptiers) {
			if (level.getBlockEntity(e.origin()) instanceof EmptierBlockEntity em && em.state() == EmptierBlockEntity.WORKING) {
				load[WarehousePower.Circuit.LIQUIDS.ordinal()] += WarehousePower.EMPTIER;
			}
		}
		load[WarehousePower.Circuit.LIGHTS.ordinal()] = WarehousePower.BASE;
		for (int k = 0; k < 6; k++) {
			if ((circuitsOn >> k & 1) == 0) {
				load[k] = 0;
			}
		}
		List<SwitchboardBlockEntity> sbs = boards(level);
		boardCount = sbs.size();
		long have = 0, cap = 0, in = 0;
		boolean anyMain = false;
		for (SwitchboardBlockEntity sb : sbs) {
			// (A board with its main switch off gives the warehouse nothing, whatever its cable brings.)
			have += sb.available();
			cap += sb.energy.getCapacity();
			in += sb.mainOn() ? sb.supply() : 0;
			anyMain |= sb.mainOn();
		}
		mainOff = !sbs.isEmpty() && !anyMain;
		long base = load[WarehousePower.Circuit.LIGHTS.ordinal()];
		long machines = load[1] + load[2] + load[3] + load[4];
		long docks = load[0];
		long want = base + machines + docks;
		int shed = 0;
		float factor = 1;
		long drawn;
		if (unlimitedPower) {
			drawn = want;
			poweredNow = true;
		} else {
			long e = have / 20;
			poweredNow = e > 0 || in > 0;
			long left = e;
			long b = Math.min(base, left);
			left -= b;
			long m = Math.min(machines, left);
			left -= m;
			if (m < machines || !poweredNow || left + m < 32) {
				// Not enough for the machines: their circuits are shed (what is under way finishes; nothing new starts).
				shed |= 0b011110;
			}
			long c = Math.min(docks, left);
			factor = docks == 0 ? (poweredNow ? 1 : 0) : c / (float) docks;
			drawn = b + m + c;
			long need = drawn * 20;
			for (SwitchboardBlockEntity sb : sbs) {
				if (need <= 0) {
					break;
				}
				need -= sb.take(need);
			}
		}
		shedMask = shed;
		chargeFactor = factor;
		loads = load;
		drawNow = drawn;
		supplyNow = unlimitedPower ? want : in;
		storedNow = 0;
		capacityNow = cap;
		for (SwitchboardBlockEntity sb : sbs) {
			storedNow += sb.energy.getAmount();
		}
		lowPower = !unlimitedPower && (sbs.isEmpty() || storedNow < cap * WarehousePower.LOW && supplyNow < want);
		long day = now / 24000L;
		if (day != peakDay) {
			peakDay = day;
			peak = 0;
		}
		peak = Math.max(peak, drawn);
		for (SwitchboardBlockEntity sb : sbs) {
			sb.show(circuitsOn, shedMask, supplyNow, drawNow);
		}
	}

	// ---- the Signal Boxes' inputs (each box tells the terminal while its redstone is on)

	void signalIn(BlockPos box, int rule, long now) {
		signals.put(box.immutable(), new long[] {rule, now});
	}

	private boolean signal(int rule) {
		if (level == null) {
			return false;
		}
		long now = level.getGameTime();
		for (long[] s : signals.values()) {
			if (s[0] == rule && now - s[1] <= 30) {
				return true;
			}
		}
		return false;
	}

	/** The fleet held: paused here (PAUSE ALL DRONES) or by a Signal Box. */
	public boolean fleetHeld() {
		return fleetPaused || signal(SignalBoxBlockEntity.PAUSE_FLEET);
	}

	public boolean streamsOff() {
		return signal(SignalBoxBlockEntity.STREAMS_OFF);
	}

	public boolean intakeOff() {
		return signal(SignalBoxBlockEntity.INTAKE_OFF);
	}

	public boolean liquidsInOff() {
		return signal(SignalBoxBlockEntity.LIQUIDS_IN_OFF);
	}

	boolean consolidateNow() {
		return signal(SignalBoxBlockEntity.CONSOLIDATE_NOW);
	}

	// ---- the CONTROL page's settings

	public int[] priorities() {
		return priorities;
	}

	public void movePriority(int group, int dir) {
		for (int i = 0; i < priorities.length; i++) {
			int j = i + dir;
			if (priorities[i] == group && j >= 0 && j < priorities.length) {
				priorities[i] = priorities[j];
				priorities[j] = group;
				setChanged();
				return;
			}
		}
	}

	public int consolidateMode() {
		return consolidateMode;
	}

	public int countMode() {
		return countMode;
	}

	public void setConsolidateMode(int m) {
		consolidateMode = Math.max(0, Math.min(1, m));
		setChanged();
	}

	public void setCountMode(int m) {
		countMode = Math.max(0, Math.min(2, m));
		setChanged();
	}

	/** PAUSE ALL DRONES (the button; a Signal Box's PAUSE FLEET is fleetHeld). */
	public boolean fleetPaused() {
		return fleetPaused;
	}

	public void setFleetPaused(boolean paused) {
		fleetPaused = paused;
		setChanged();
	}

	/** A drone's buttons on LAYOUT and FLEET: PAUSE (again: go on), SEND HOME, OUT OF SERVICE (again: back), LOCATE. */
	public boolean droneAction(int id, int action) {
		Drone d = drones.stream().filter(x -> x.id == id).findFirst().orElse(null);
		if (d == null || level == null) {
			return false;
		}
		long now = level.getGameTime();
		switch (action) {
			case 0 -> d.paused = !d.paused;
			case 1 -> d.sendHome = true;
			case 2 -> d.outOfService = !d.outOfService;
			case 3 -> d.locateUntil = now + 200;
			default -> {
				return false;
			}
		}
		setChanged();
		legStarted(d);
		return true;
	}

	/** RUSH: an open order goes ahead of everything (again: back in line). */
	public boolean rush(int id, @Nullable Player player) {
		Order o = order(id);
		if (o == null || !o.open()) {
			return false;
		}
		o.rush = !o.rush;
		setChanged();
		return true;
	}

	/** When a drone last started a job that wasn't housekeeping or going home (the fleet idle since then). */
	long lastWork() {
		return lastWork;
	}

	// ---- cycle counts: when each place was last counted, what the books said was on it

	public long counted(BlockPos place) {
		return counted.getOrDefault(place, 0L);
	}

	/** What is on a pallet, as a number the same from one run of the game to the next (the item's id, its components). */
	private int contents(@Nullable PalletLoad load) {
		if (load == null) {
			return 0;
		}
		int h = 1;
		for (CaseLoad c : load.cases()) {
			h = h * 31 + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(c.item().getItem()).toString().hashCode();
			net.minecraft.core.component.DataComponentPatch patch = c.item().getComponentsPatch();
			if (!patch.isEmpty() && level != null) {
				h = h * 31 + net.minecraft.core.component.DataComponentPatch.CODEC
						.encodeStart(level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), patch).result()
						.map(Object::toString).orElse("").hashCode();
			}
			h = h * 31 + c.count();
		}
		return h;
	}

	/** The warehouse itself set or took a place's pallet: the books know it. */
	void booked(BlockPos place, @Nullable PalletLoad load) {
		if (load == null || load.isEmpty()) {
			books.remove(place);
		} else {
			books.put(place.immutable(), contents(load));
		}
	}

	/** A drone scanned a place's label: what is on it is what the books say (a difference is fixed and logged). */
	private void countPlace(ServerLevel level, BlockPos place, long now) {
		counted.put(place.immutable(), now);
		if (!(level.getBlockEntity(place) instanceof RackBlockEntity rack)) {
			return;
		}
		Integer book = books.get(place);
		int found = contents(rack.load());
		if (book == null) {
			if (found != 0) {
				books.put(place.immutable(), found);
			}
		} else if (book != found) {
			Network.Place p = network.placeAt.get(place);
			String what = rack.load() == null || rack.load().isEmpty() ? "empty" : rack.load().cases().get(0).item().getHoverName().getString()
					+ " " + rack.load().cases().stream().mapToLong(CaseLoad::count).sum();
			adjustments.addFirst(String.format(java.util.Locale.ROOT, "%s  %s  found %s (books fixed)", clock(now), p == null ? "?" : p.label(), what));
			while (adjustments.size() > 6) {
				adjustments.removeLast();
			}
			booked(place, rack.load());
			recount(level);
		}
		mapColumnsAt = 0;
		setChanged();
	}

	private static String clock(long now) {
		long t = (now + 6000) % 24000;
		return String.format(java.util.Locale.ROOT, "%02d:%02d", t / 1000, t % 1000 * 60 / 1000);
	}

	/** Places counted today (in the last day), for the CONTROL page. */
	private int countedToday(long now) {
		int n = 0;
		for (Network.Place p : network.places) {
			if (now - counted(p.pos()) < 24000 && counted(p.pos()) > 0) {
				n++;
			}
		}
		return n;
	}

	/** Part W5, for the Status Board: what the warehouse draws (JE a tick), its stores' level (%), the places counted today (%). */
	public long drawNow() {
		return drawNow;
	}

	public int storedPercent() {
		return unlimitedPower ? 100 : capacityNow <= 0 ? 0 : (int) Math.round(100.0 * storedNow / capacityNow);
	}

	public int countedPercent(long now) {
		return network.places.isEmpty() ? 0 : (int) Math.round(100.0 * countedToday(now) / network.places.size());
	}

	/** What housekeeping is doing now ("" nothing). */
	public String house() {
		return house;
	}

	void setHouse(String house, String next) {
		this.house = house;
		this.next = next;
	}

	@Override
	public int analog() {
		int total = network.places.size();
		return placesUsed <= 0 || total == 0 ? 0 : 1 + 14 * placesUsed / total;
	}

	/** The CONTROL page for a player. */
	public ControlPayload controlView(ServerPlayer player) {
		List<Integer> pr = new ArrayList<>();
		for (int g : priorities) {
			pr.add(g);
		}
		List<Long> ld = new ArrayList<>();
		for (long l : loads) {
			ld.add(l);
		}
		List<String> sig = new ArrayList<>();
		if (level != null) {
			long now = level.getGameTime();
			for (Map.Entry<BlockPos, long[]> e : signals.entrySet()) {
				if (now - e.getValue()[1] <= 30) {
					sig.add(SignalBoxBlockEntity.IN_RULES[(int) Math.max(0, Math.min(SignalBoxBlockEntity.IN_RULES.length - 1, e.getValue()[0]))]
							+ " (signal box " + e.getKey().getX() + " " + e.getKey().getY() + " " + e.getKey().getZ() + ")");
				}
			}
		}
		long now = level == null ? 0 : level.getGameTime();
		return new ControlPayload(worldPosition, ownerName, useMode == io.github.jimbozoomer.jugcraft.party.UseMode.PARTY, mayUse(player),
				mayChangeAccess(player), pr, consolidateMode, countMode, countedToday(now), network.places.size(), new ArrayList<>(adjustments), house,
				next, unlimitedPower, boardCount, supplyNow, drawNow, storedNow, capacityNow, peak, circuitsOn, shedMask, ld, lowPower(), mainOff,
				fleetPaused, sig);
	}

	/**
	 * The floor map (LAYOUT, the Status Board): the cells found again when the warehouse is, the rack columns (in use,
	 * part pallets, counted) every two seconds.
	 */
	public MapPayload map(ServerLevel level) {
		long now = level.getGameTime();
		boolean fresh = false;
		if (mapCells == null || mapCellsVersion != scanVersion) {
			mapCells = mapFloors(level);
			mapCellsVersion = scanVersion;
			mapColumnsAt = 0;
			fresh = true;
		}
		if (map == null || now >= mapColumnsAt) {
			mapColumnsAt = now + 40;
			List<MapPayload.Floor> floors = new ArrayList<>();
			Map<Long, List<Network.Place>> columns = new LinkedHashMap<>();
			for (Network.Place p : network.places) {
				columns.computeIfAbsent(BlockPos.asLong(p.pos().getX(), network.floor(p.pos().getY()), p.pos().getZ()), k -> new ArrayList<>()).add(p);
			}
			for (int i = 0; i < mapCells.size(); i++) {
				MapPayload.Floor f = mapCells.get(i);
				int fi = network.floors.indexOf(f.y());
				List<MapPayload.Column> racks = new ArrayList<>();
				for (Map.Entry<Long, List<Network.Place>> e : columns.entrySet()) {
					if (BlockPos.getY(e.getKey()) != fi) {
						continue;
					}
					int used = 0, parts = 0;
					long oldest = Long.MAX_VALUE;
					String label = "";
					for (Network.Place p : e.getValue()) {
						if (level.isLoaded(p.pos()) && level.getBlockEntity(p.pos()) instanceof RackBlockEntity rack && rack.load() != null) {
							used++;
							if (!rack.load().full() && !(Drums.isLiquid(rack.load()) && rack.load().size() >= Drums.PER_PALLET)) {
								parts++;
							}
						}
						oldest = Math.min(oldest, counted(p.pos()));
						if (label.isEmpty()) {
							label = p.label().substring(0, Math.max(0, p.label().lastIndexOf('-')));
						}
					}
					racks.add(new MapPayload.Column(BlockPos.getX(e.getKey()), BlockPos.getZ(e.getKey()), label, used, parts, e.getValue().size(),
							oldest == Long.MAX_VALUE ? 0 : oldest));
				}
				floors.add(new MapPayload.Floor(f.y(), f.x0(), f.z0(), f.w(), f.d(), f.cells(), f.pieces(), racks));
			}
			// (A new version only when something on it changed: the screens and boards send it again then.)
			boolean same = !fresh && map != null && map.floors().size() == floors.size();
			for (int i = 0; same && i < floors.size(); i++) {
				same = floors.get(i).racks().equals(map.floors().get(i).racks());
			}
			if (!same) {
				map = new MapPayload(worldPosition, ++mapVersion, floors);
			}
		}
		return map;
	}

	/** Each floor's cells and pieces, read off the blocks (once each time the warehouse is found again). */
	private List<MapPayload.Floor> mapFloors(ServerLevel level) {
		List<MapPayload.Floor> out = new ArrayList<>();
		for (int i = 0; i < network.floors.size(); i++) {
			int y = network.floors.get(i);
			int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
			for (long c : network.cells) {
				if (BlockPos.getY(c) == y) {
					x0 = Math.min(x0, BlockPos.getX(c));
					x1 = Math.max(x1, BlockPos.getX(c));
					z0 = Math.min(z0, BlockPos.getZ(c));
					z1 = Math.max(z1, BlockPos.getZ(c));
				}
			}
			for (Network.Place p : network.places) {
				if (network.floor(p.pos().getY()) == i) {
					x0 = Math.min(x0, p.pos().getX());
					x1 = Math.max(x1, p.pos().getX());
					z0 = Math.min(z0, p.pos().getZ());
					z1 = Math.max(z1, p.pos().getZ());
				}
			}
			if (x0 > x1) {
				continue;
			}
			// The pieces on this floor (their footprints widen the map to take them in).
			List<MapPayload.Piece> pieces = new ArrayList<>();
			int st = 0;
			java.util.Set<BlockPos> seen = new java.util.HashSet<>();
			for (long m : network.members) {
				BlockPos p = BlockPos.of(m);
				if (Math.abs(p.getY() - y) > 1) {
					continue;
				}
				BlockState s = level.getBlockState(p);
				if (!(s.getBlock() instanceof PieceBlock pb) || !seen.add(pb.origin(p, s))) {
					continue;
				}
				BlockPos o = pb.origin(p, s);
				if (o.getY() != y) {
					continue;
				}
				Direction facing = s.getValue(PieceBlock.FACING);
				int px0 = Integer.MAX_VALUE, pz0 = Integer.MAX_VALUE, px1 = Integer.MIN_VALUE, pz1 = Integer.MIN_VALUE;
				for (int k = 0; k < pb.piece.parts(); k++) {
					BlockPos q = o.offset(pb.piece.offset(facing, k));
					px0 = Math.min(px0, q.getX());
					px1 = Math.max(px1, q.getX());
					pz0 = Math.min(pz0, q.getZ());
					pz1 = Math.max(pz1, q.getZ());
				}
				String name = switch (pb.piece) {
					case INTAKE -> "IN";
					case STATION -> "ST" + (++st);
					case TERMINAL -> "WMS";
					case GOODS_IN, GOODS_OUT, LIQUIDS_IN, LIQUIDS_OUT, STATUS_BOARD, SIGNAL_BOX -> "";
					case READY_STORAGE, READY_STORAGE_EXT -> "READY";
					case LIFT_LANDING, LIFT_SHAFT, LIFT_DRIVE -> "LIFT";
					case DRUM_FILLER -> "DF";
					case DRUM_EMPTIER -> "DE";
					case SWITCHBOARD -> "MDB";
				};
				if (!name.isEmpty()) {
					pieces.add(new MapPayload.Piece(name, px0, pz0, px1, pz1));
					x0 = Math.min(x0, px0);
					x1 = Math.max(x1, px1);
					z0 = Math.min(z0, pz0);
					z1 = Math.max(z1, pz1);
				}
			}
			x0 -= 1;
			z0 -= 1;
			x1 += 1;
			z1 += 1;
			int w = x1 - x0 + 1, d = z1 - z0 + 1;
			if (w * d > 1 << 16) {
				continue;
			}
			byte[] cells = new byte[w * d];
			BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					byte code = MapPayload.NONE;
					BlockState at = level.getBlockState(q.set(x, y, z)), under = level.getBlockState(q.set(x, y - 1, z));
					if (at.getBlock() instanceof RackBlock) {
						code = MapPayload.RACK;
					} else if (at.getBlock() instanceof DockBlock) {
						code = MapPayload.DOCK;
					} else if (at.getBlock() instanceof PieceBlock) {
						code = MapPayload.PIECE;
					} else if (under.getBlock() instanceof CrossingBlock) {
						code = MapPayload.CROSSING;
					} else if (under.getBlock() instanceof WalkwayBlock) {
						code = MapPayload.WALKWAY;
					} else if (network.cells.contains(BlockPos.asLong(x, y, z)) || under.getBlock() instanceof LaneBlock) {
						code = MapPayload.LANE;
					}
					cells[(z - z0) * w + (x - x0)] = code;
				}
			}
			out.add(new MapPayload.Floor(y, x0, z0, w, d, cells, pieces, List.of()));
		}
		return out;
	}

	// ------------------------------------------------------------------ players

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (!level.isClientSide()) {
			// Part W5: a warehouse nobody owns yet is the first opener's.
			if (owner == null && !player.isSpectator()) {
				setOwner(player);
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.claimed"));
			}
			player.openMenu(this);
		}
		return InteractionResult.SUCCESS;
	}

	/** Broken: Ready Storage's pool and the port's buffer drop here (the racks keep their pallets). */
	@Override
	public void dropContents() {
		if (level == null) {
			return;
		}
		List<ItemStack> out = new ArrayList<>(pool.drain());
		out.addAll(port.drain());
		for (ItemStack s : out) {
			Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, s);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		dropContents();
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.jugcraft.warehouse_terminal");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new TerminalMenu(id, inventory, worldPosition);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	/** The STOCK and FLEET pages as they are now (the screen's data); without the stock list for the desk's monitors. */
	public TerminalPayload snapshot(long now) {
		return snapshot(now, true);
	}

	public TerminalPayload snapshot(long now, boolean withItems) {
		List<TerminalPayload.Item> items = new ArrayList<>();
		long stored = 0;
		for (Stock s : stock.values()) {
			if (!Drums.isLiquid(s.item)) {
				stored += s.count;
			}
			if (!withItems) {
				continue;
			}
			List<String> places = new ArrayList<>();
			for (BlockPos p : s.places) {
				Network.Place place = network.placeAt.get(p);
				places.add(place == null ? "?" : place.label());
			}
			items.add(new TerminalPayload.Item(s.item, s.count, s.cases, s.places.size(), places));
		}
		items.sort((a, b) -> Long.compare(b.count(), a.count()));
		List<TerminalPayload.DroneLine> fleet = new ArrayList<>();
		for (Drone d : drones) {
			String where = d.target == null ? "" : network.placeAt.containsKey(d.target) ? network.placeAt.get(d.target).label() : "";
			String job = d.busy(now) ? Drone.JOBS[d.job] : d.outOfService ? "OUT OF SERVICE" : d.paused || fleetHeld() ? "PAUSED"
					: d.docked ? (d.battery < 0.999F ? (chargeFactor() <= 0 ? "NO POWER" : "CHARGING") : "READY") : "IDLE";
			fleet.add(new TerminalPayload.DroneLine(d.name(), d.kind.ordinal(), job, where, d.battery, network.floor(d.dock.getY()) + 1, d.id,
					d.flags(now)));
		}
		List<TerminalPayload.Logged> logged = new ArrayList<>();
		for (PutAway p : log) {
			logged.add(new TerminalPayload.Logged(p.time(), p.item(), p.count(), p.place()));
		}
		int cases = 0;
		for (Network.Intake in : network.intakes) {
			if (level != null && level.getBlockEntity(in.origin()) instanceof IntakeBlockEntity intake) {
				cases += intake.conveyor().size() + (intake.packing() == null ? 0 : 1);
			}
		}
		return new TerminalPayload(worldPosition, primary, items, fleet, logged, placesUsed, network.places.size(), cases, itemsIn,
				network.truncated, stored, stock.size(), orderLines(now), outboundView());
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** The clients get the fleet's legs (to draw the drones). */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		List<Drone.View> views = drones.stream().map(Drone::view).toList();
		tag.put("fleet_view", Drone.View.CODEC.listOf().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), views).getOrThrow());
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		drones.clear();
		drones.addAll(input.read("drones", Drone.CODEC.listOf()).orElse(List.of()));
		nextId = input.getIntOr("next_id", 1);
		log.clear();
		log.addAll(input.read("log", PutAway.CODEC.listOf()).orElse(List.of()));
		itemsIn = input.getLongOr("items_in", 0);
		orders.clear();
		orders.addAll(input.read("orders", Order.CODEC.listOf()).orElse(List.of()));
		streams.clear();
		streams.addAll(input.read("streams", Order.Stream.CODEC.listOf()).orElse(List.of()));
		liquidStreams.clear();
		liquidStreams.addAll(input.read("liquid_streams", Order.Stream.CODEC.listOf()).orElse(List.of()));
		pool = input.read("pool", ReadyPool.CODEC).orElseGet(ReadyPool::new);
		port = input.read("port", PortBuffer.CODEC).orElseGet(PortBuffer::new);
		portMode = input.getIntOr("port_mode", PORT_STREAMS);
		liquidMode = input.getIntOr("liquid_mode", PORT_STREAMS);
		nextOrder = input.getIntOr("next_order", 1000);
		clientFleet = new ArrayList<>(input.read("fleet_view", Drone.View.CODEC.listOf()).orElse(List.of()));
		pendingFloor = input.read("pending_floor", BlockPos.CODEC).orElse(null);
		pendingFrom = input.getIntOr("pending_from", 0);
		builtBy = input.getStringOr("built_by", "");
		floorsChosen = input.getIntOr("floors_chosen", 0);
		owner = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
		ownerName = input.getStringOr("owner_name", "");
		ownedSince = input.getLongOr("owned_since", 0);
		useMode = "party".equals(input.getStringOr("use_mode", "personal")) ? io.github.jimbozoomer.jugcraft.party.UseMode.PARTY
				: io.github.jimbozoomer.jugcraft.party.UseMode.PERSONAL;
		unlimitedPower = input.getBooleanOr("unlimited_power", false);
		fleetPaused = input.getBooleanOr("fleet_paused", false);
		circuitsOn = input.getIntOr("circuits", 0x3F);
		List<Integer> pr = input.read("priorities", com.mojang.serialization.Codec.INT.listOf()).orElse(List.of());
		if (pr.size() == priorities.length && new java.util.HashSet<>(pr).size() == priorities.length) {
			for (int i = 0; i < priorities.length; i++) {
				priorities[i] = pr.get(i);
			}
		}
		consolidateMode = input.getIntOr("consolidate", ControlPayload.WHEN_IDLE);
		countMode = input.getIntOr("count", ControlPayload.COUNT_DAILY);
		counted.clear();
		for (CountedPlace c : input.read("counted", CountedPlace.CODEC.listOf()).orElse(List.of())) {
			counted.put(c.pos(), c.time());
		}
		books.clear();
		for (BookedPlace b : input.read("books", BookedPlace.CODEC.listOf()).orElse(List.of())) {
			books.put(b.pos(), b.contents());
		}
		adjustments.clear();
		adjustments.addAll(input.read("adjustments", com.mojang.serialization.Codec.STRING.listOf()).orElse(List.of()));
		peak = input.getLongOr("peak", 0);
		peakDay = input.getLongOr("peak_day", 0);
		changed = true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("drones", Drone.CODEC.listOf(), drones);
		output.putInt("next_id", nextId);
		output.store("log", PutAway.CODEC.listOf(), new ArrayList<>(log));
		output.putLong("items_in", itemsIn);
		output.store("orders", Order.CODEC.listOf(), orders);
		output.store("streams", Order.Stream.CODEC.listOf(), streams);
		output.store("liquid_streams", Order.Stream.CODEC.listOf(), liquidStreams);
		output.store("pool", ReadyPool.CODEC, pool);
		output.store("port", PortBuffer.CODEC, port);
		output.putInt("port_mode", portMode);
		output.putInt("liquid_mode", liquidMode);
		output.putInt("next_order", nextOrder);
		if (pendingFloor != null) {
			output.store("pending_floor", BlockPos.CODEC, pendingFloor);
			output.putInt("pending_from", pendingFrom);
		}
		output.putString("built_by", builtBy);
		output.putInt("floors_chosen", floorsChosen);
		if (owner != null) {
			output.store("owner", net.minecraft.core.UUIDUtil.CODEC, owner);
			output.putLong("owned_since", ownedSince);
		}
		output.putString("owner_name", ownerName);
		output.putString("use_mode", useMode.id());
		output.putBoolean("unlimited_power", unlimitedPower);
		output.putBoolean("fleet_paused", fleetPaused);
		output.putInt("circuits", circuitsOn);
		List<Integer> pr = new ArrayList<>();
		for (int g : priorities) {
			pr.add(g);
		}
		output.store("priorities", com.mojang.serialization.Codec.INT.listOf(), pr);
		output.putInt("consolidate", consolidateMode);
		output.putInt("count", countMode);
		output.store("counted", CountedPlace.CODEC.listOf(), counted.entrySet().stream().map(e -> new CountedPlace(e.getKey(), e.getValue())).toList());
		output.store("books", BookedPlace.CODEC.listOf(), books.entrySet().stream().map(e -> new BookedPlace(e.getKey(), e.getValue())).toList());
		output.store("adjustments", com.mojang.serialization.Codec.STRING.listOf(), new ArrayList<>(adjustments));
		output.putLong("peak", peak);
		output.putLong("peak_day", peakDay);
	}
}
