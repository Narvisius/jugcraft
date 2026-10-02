package io.github.jimbozoomer.jugcraft.drone;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Depot Terminal: the depot's brain. It owns the fleet, the pooled power buffer, the
 * platform layout and the flight scheduler. Everything is decided on the server. Clients get a small
 * {@link DepotView} snapshot whenever something changes (and every two seconds for the power readout),
 * and draw the drones, the pickup animation and the screens from it.
 *
 * <p>Per tick it does a fixed amount of work: one power calculation and one progress update per
 * active flight. Job requests run once a second, platform rescans at most once a second and only
 * after a platform block changed.
 */
public class DroneTerminalBlockEntity extends BlockEntity {
	public static final long ENERGY_CAPACITY = 200_000;
	public static final long ENERGY_INPUT = 4_096;
	/** Horizontal reach of the depot, in blocks from the platform centre. */
	public static final int RANGE = 96;
	public static final int JOB_INTERVAL = 20;
	public static final int SCAN_INTERVAL = 20;
	/** How long a reservation lasts; flights are much shorter, so an abandoned one frees itself. */
	public static final int RESERVE_TICKS = 20 * 60;
	/** The platform must have a block within this distance of the terminal, 1 to 8 blocks below it. */
	public static final int SEED_RADIUS = 8;
	public static final int SEED_DEPTH = 8;
	public static final int MAX_TARGETS_PER_REQUEST = 64;
	/** Changes reach clients at most this often (ticks), and the readout refreshes at least this often. */
	public static final int VIEW_MIN_INTERVAL = 5;
	public static final int VIEW_REFRESH = 40;

	/** One delivery: where it goes, what is carried and whether it was placed. */
	static final class Cargo {
		final BuildJobs.@Nullable Target target;
		final BlockPos pos;
		final ItemStack item;
		final BlockState state;
		/** The materials were paid by the job source (the tower's modules): nothing goes back to the packager. */
		final boolean supplied;
		boolean placed;

		Cargo(BuildJobs.@Nullable Target target, BlockPos pos, ItemStack item, BlockState state, boolean supplied) {
			this.target = target;
			this.pos = pos;
			this.item = item;
			this.state = state;
			this.supplied = supplied;
		}
	}

	/** Where a drone of a tower depot docks, and the point outside its hangar it flies out to (or null). */
	record Spot(Vec3 pos, double @Nullable [] exit) {
	}

	private final SimpleEnergyStorage energy = new SimpleEnergyStorage(ENERGY_CAPACITY, ENERGY_INPUT, 0, this::setChanged);
	private final DroneFleet fleet = new DroneFleet();
	private final FlightScheduler<Cargo> scheduler = new FlightScheduler<>();
	private final Set<BlockPos> inFlight = new HashSet<>();
	private @Nullable UUID owner;
	private UUID depotId = UUID.randomUUID();
	private UseMode mode = UseMode.PERSONAL;

	private PlatformLayout layout = PlatformLayout.EMPTY;
	private int platformY;
	/** The base level of the layout that pads were last formed for. */
	private int oldPlatformY;
	private @Nullable BlockPos packagerPos;
	/** North-west corner {x, z} of the supply pickup the drones use, or null. */
	private int @Nullable [] pickupCorner;
	private boolean platformDirty = true;
	/** Path of each flying drone (relative to this block), for the clients and for saving. */
	private final Map<Integer, FlightPath> paths = new HashMap<>();
	private List<DockLayout.Dock> docks = List.of();
	/** With a Drone Tower: where each drone docks (pads and hangars). */
	private List<Spot> spots = List.of();
	/** The Drone Tower Core this depot belongs to (set when the tower's Command Post is built), or null. */
	private @Nullable BlockPos towerPos;
	/** Highest drone tier a depot without a tower can use (game tests and the dev command raise it). */
	private int standaloneMaxTier = 0;
	/** Test power (development command only): the terminal never runs out. */
	private boolean unlimitedPower;
	private boolean docksDirty = true;
	private boolean viewDirty = true;
	private long lastViewSync = Long.MIN_VALUE / 2;
	private long currentRate = FlightScheduler.FULL_RATE;
	/** Client side: the last snapshot from the server. */
	private @Nullable DepotView view;
	private long lastScan = Long.MIN_VALUE / 2;

	// Status for the terminal display (not saved).
	private boolean lowPower;
	private int coveredSkipped;
	private int openTargets;
	private final Map<Item, Integer> missing = new LinkedHashMap<>();

	private final FlightScheduler.Listener<Cargo> listener = new FlightScheduler.Listener<>() {
		@Override
		public void onDeliver(FlightScheduler.Flight<Cargo> flight, int index) {
			deliver(flight.stops().get(index).cargo());
		}

		@Override
		public void onLanded(FlightScheduler.Flight<Cargo> flight) {
			paths.remove(flight.drone());
			viewDirty = true;
			for (FlightScheduler.Stop<Cargo> stop : flight.stops()) {
				Cargo cargo = stop.cargo();
				inFlight.remove(cargo.pos);
				if (!cargo.placed && !cargo.supplied) {
					returnItem(cargo.item);
				}
			}
			setChanged();
		}
	};

	public DroneTerminalBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftDrones.TERMINAL_ENTITY, pos, state);
	}

	// ------------------------------------------------------------------ access

	public SimpleEnergyStorage energy() {
		return energy;
	}

	public DroneFleet fleet() {
		return fleet;
	}

	public FlightScheduler<Cargo> scheduler() {
		return scheduler;
	}

	public PlatformLayout layout() {
		return layout;
	}

	public UseMode mode() {
		return mode;
	}

	/** Client side: the depot as last sent by the server, or null before the first update. */
	public @Nullable DepotView view() {
		return view;
	}

	/** Sends the view to nearby clients on the next tick (a player opened the screen). */
	public void refreshClients() {
		viewDirty = true;
		lastViewSync = Long.MIN_VALUE / 2;
	}

	/** The supply pickup's north-west corner {x, z}, or null. */
	public int @Nullable [] pickupCorner() {
		return pickupCorner;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	// ------------------------------------------------------------------ Drone Tower

	/** The tower's core, if this depot belongs to a Drone Tower that has reached tier 1. */
	public io.github.jimbozoomer.jugcraft.tower.@Nullable TowerCoreBlockEntity tower() {
		if (towerPos == null || level == null || !level.isLoaded(towerPos)) {
			return null;
		}
		return level.getBlockEntity(towerPos) instanceof io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity core && core.tier() >= 1
				? core : null;
	}

	public @Nullable BlockPos towerPos() {
		return towerPos;
	}

	/** The tower reached a new tier (or was just built): more docks, pickups and drone tiers. */
	public void linkTower(BlockPos core) {
		towerPos = core.immutable();
		platformDirty = true;
		docksDirty = true;
		viewDirty = true;
		setChanged();
		rescanNow();
	}

	/** Highest drone tier this depot can use: its tower's tier, or tier 1 without a tower. */
	public int maxTier() {
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		return tower != null ? tower.tier() : standaloneMaxTier;
	}

	/**
	 * Endless power and building blocks for testing (the development-only {@code /dronetest supplies}
	 * command): the terminal never runs out and its packager hands out any block. Returns false without a packager.
	 */
	public boolean setTestSupplies(boolean on) {
		unlimitedPower = on;
		setChanged();
		CargoPackagerBlockEntity packager = packager();
		if (packager != null) {
			packager.setUnlimited(on);
		}
		return packager != null;
	}

	/** Lets a terminal without a Drone Tower fly drones up to {@code tier}: game tests and the dev command only (in survival a depot is always part of a tower). */
	public void allowTiersWithoutTower(int tier) {
		standaloneMaxTier = Math.max(0, Math.min(9, tier));
		setChanged();
	}

	/** Most drones this depot holds: its tower's capacity, or the platform limit without a tower. */
	public int maxDrones() {
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		// The cap grows as the depot does: a tower's capacity at its current tier, or for a ground platform as many
		// small drones as the pads built so far can hold (4 a pad), up to the platform's 100.
		return tower != null ? io.github.jimbozoomer.jugcraft.tower.TowerData.get().capacity(tower.tier())
				: Math.min(PlatformLayout.MAX_DRONES, layout.slots() / DroneSize.SMALL.slots());
	}

	/** Every supply pickup the drones use: {x, plate level, z} of its north-west plate (absolute). */
	public List<int[]> activePickups() {
		List<int[]> out = new ArrayList<>();
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		if (tower != null && tower.tier() >= 2) {
			BlockPos core = tower.getBlockPos();
			for (int t = 1; t <= tower.tier(); t++) {
				for (io.github.jimbozoomer.jugcraft.tower.TowerData.Pickup p : io.github.jimbozoomer.jugcraft.tower.TowerData.get().tier(t).pickups) {
					out.add(new int[] {core.getX() + p.x(), core.getY() + p.y(), core.getZ() + p.z()});
				}
			}
		} else if (pickupCorner != null) {
			out.add(new int[] {pickupCorner[0], platformY + 1, pickupCorner[1]});
		}
		return out;
	}

	/**
	 * Where every drone of a tower depot docks: largest first, each in a free hangar of its size (or the next
	 * size up), otherwise on a ground pad (four small, two medium or one large per pad). Null when they don't all fit.
	 */
	private @Nullable List<Spot> towerSpots(List<DroneTier> drones, io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower) {
		io.github.jimbozoomer.jugcraft.tower.TowerData data = io.github.jimbozoomer.jugcraft.tower.TowerData.get();
		BlockPos core = tower.getBlockPos();
		List<io.github.jimbozoomer.jugcraft.tower.TowerData.Dock> hangars = new ArrayList<>();
		for (int t = 1; t <= tower.tier(); t++) {
			hangars.addAll(data.tier(t).docks);
		}
		boolean[] hangarUsed = new boolean[hangars.size()];
		List<long[]> pads = layout.pads();
		int[] padUsed = new int[pads.size()]; // a bit per quarter slot, as DockLayout
		Spot[] out = new Spot[drones.size()];
		for (DroneSize size : new DroneSize[] {DroneSize.LARGE, DroneSize.MEDIUM, DroneSize.SMALL}) {
			for (int i = 0; i < drones.size(); i++) {
				if (drones.get(i).size() != size) {
					continue;
				}
				Spot spot = null;
				for (int fit = size.ordinal(); fit < DroneSize.values().length && spot == null; fit++) {
					for (int h = 0; h < hangars.size(); h++) {
						io.github.jimbozoomer.jugcraft.tower.TowerData.Dock d = hangars.get(h);
						if (!hangarUsed[h] && d.size().ordinal() == fit) {
							hangarUsed[h] = true;
							spot = new Spot(new Vec3(core.getX() + d.x(), core.getY() + d.y(), core.getZ() + d.z()),
									new double[] {core.getX() + d.exitX(), core.getZ() + d.exitZ()});
							break;
						}
					}
				}
				// Otherwise on a ground pad: four small drones round its charger port, two medium or one large.
				DockLayout.Dock slot = spot == null ? DockLayout.place(padUsed, size) : null;
				if (slot != null) {
					long[] pad = pads.get(slot.pad());
					double[] exit = null;
					if (tower.tier() >= 2) {
						for (io.github.jimbozoomer.jugcraft.tower.TowerData.Pad tp : data.pads) {
							if (core.getX() + tp.x() == pad[0] && core.getZ() + tp.z() == pad[1]) {
								exit = new double[] {core.getX() + tp.exitX(), core.getZ() + tp.exitZ()};
							}
						}
					}
					spot = new Spot(new Vec3(pad[0] + PlatformLayout.PAD_SIZE / 2.0 + slot.dx(), plateTop(),
							pad[1] + PlatformLayout.PAD_SIZE / 2.0 + slot.dz()), exit);
				}
				if (spot == null) {
					return null;
				}
				out[i] = spot;
			}
		}
		return List.of(out);
	}

	/** Gives the plates of each tower pickup their tile numbers (one lift hatch each). */
	private void formTowerPickups(ServerLevel level) {
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		if (tower == null || tower.tier() < 2) {
			return;
		}
		for (int[] p : activePickups()) {
			for (int dx = 0; dx < PlatformLayout.PICKUP_SIZE; dx++) {
				for (int dz = 0; dz < PlatformLayout.PICKUP_SIZE; dz++) {
					BlockPos at = new BlockPos(p[0] + dx, p[1], p[2] + dz);
					BlockState state = level.isLoaded(at) ? level.getBlockState(at) : null;
					if (state != null && state.getBlock() instanceof SupplyPickupBlock && state.getValue(SupplyPickupBlock.PART) != SupplyPickupBlock.part(dx, dz)) {
						level.setBlock(at, state.setValue(SupplyPickupBlock.PART, SupplyPickupBlock.part(dx, dz)), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		setChanged();
	}

	public boolean isOwner(Player player) {
		return owner == null || owner.equals(player.getUUID());
	}

	void markPlatformDirty() {
		platformDirty = true;
	}

	/** True for a base block of this depot's platform, or a pad plate on top of it. */
	boolean platformContains(BlockPos pos) {
		return layout.isValid() && (pos.getY() == platformY || pos.getY() == platformY + 1) && layout.contains(pos.getX(), pos.getZ());
	}

	/** Forces a platform rescan now (used by game tests and after placement). */
	public void rescanNow() {
		if (level instanceof ServerLevel serverLevel) {
			rescan(serverLevel);
		}
	}

	// ------------------------------------------------------------------ player actions

	/** Links a drone of {@code tier}; true if linked (the caller then consumes the item). */
	public boolean linkDrone(Player player, DroneTier tier) {
		if (!isOwner(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.not_owner"));
			return false;
		}
		rescanNow();
		if (tier.number() > maxTier() && !(tier.creative() && maxTier() >= 1)) {
			player.sendOverlayMessage(Component.translatable(tower() == null ? "message.jugcraft.drone.link.needs_tower"
					: "message.jugcraft.drone.link.tower_tier", tier.number()));
			return false;
		}
		DroneFleet.LinkResult result;
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		if (tower != null) {
			List<DroneTier> wanted = new ArrayList<>(fleet.drones());
			wanted.add(tier);
			result = fleet.size() >= maxDrones() ? DroneFleet.LinkResult.FLEET_FULL
					: towerSpots(wanted, tower) == null ? DroneFleet.LinkResult.NO_PAD_ROOM : fleet.link(tier, Integer.MAX_VALUE, DroneFleet.MAX_FLEET);
		} else {
			result = fleet.link(tier, layout.slots());
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.link." + result.name().toLowerCase(java.util.Locale.ROOT),
				fleet.size(), maxDrones(), fleet.slotsUsed(), layout.slots()));
		if (result == DroneFleet.LinkResult.OK) {
			docksDirty = true;
			viewDirty = true;
			setChanged();
			return true;
		}
		return false;
	}

	/** Unlinks the last docked drone and returns its item (empty if none, or not the owner). */
	public ItemStack unlinkDrone(Player player) {
		if (!isOwner(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.not_owner"));
			return ItemStack.EMPTY;
		}
		for (int i = fleet.size() - 1; i >= 0; i--) {
			if (!scheduler.isFlying(i)) {
				DroneTier tier = fleet.drones().get(i);
				fleet.remove(i);
				scheduler.droneRemoved(i);
				Map<Integer, FlightPath> moved = new HashMap<>();
				int removed = i;
				paths.forEach((drone, path) -> moved.put(drone > removed ? drone - 1 : drone, path));
				paths.clear();
				paths.putAll(moved);
				docksDirty = true;
				viewDirty = true;
				setChanged();
				player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.unlinked", fleet.size()));
				Item item = JugcraftDrones.DRONES.get(tier);
				return item == null ? ItemStack.EMPTY : new ItemStack(item);
			}
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.none_docked"));
		return ItemStack.EMPTY;
	}

	public void toggleMode(Player player) {
		if (!isOwner(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.not_owner"));
			return;
		}
		mode = mode == UseMode.PERSONAL ? UseMode.PARTY : UseMode.PERSONAL;
		viewDirty = true;
		setChanged();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.drone.mode." + mode.id()));
	}

	/** The terminal readout, one line per entry. */
	public List<Component> status() {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("message.jugcraft.drone.status.title", Component.translatable("message.jugcraft.drone.mode_name." + mode.id())));
		lines.add(Component.translatable("message.jugcraft.drone.status.platform." + layout.status().name().toLowerCase(java.util.Locale.ROOT),
				layout.width(), layout.depth(), layout.padCount(), fleet.slotsUsed(), layout.slots()));
		lines.add(Component.translatable("message.jugcraft.drone.status.fleet", fleet.size(), maxDrones(), scheduler.activeFlights()));
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		lines.add(tower == null ? Component.translatable("message.jugcraft.drone.status.no_tower", maxTier())
				: Component.translatable("message.jugcraft.drone.status.tower", tower.tier(), maxTier()));
		StringBuilder tiers = new StringBuilder();
		fleet.countByTier().forEach((tier, count) -> tiers.append(tiers.isEmpty() ? "" : ", ").append("T").append(tier.number()).append("×").append(count));
		if (!tiers.isEmpty()) {
			lines.add(Component.translatable("message.jugcraft.drone.status.tiers", tiers.toString()));
		}
		lines.add(Component.translatable(lowPower ? "message.jugcraft.drone.status.power_low" : "message.jugcraft.drone.status.power",
				energy.getAmount(), energy.getCapacity(), scheduler.activeFlights() > 0 ? fleet.workingDraw() : fleet.standbyDraw(),
				fleet.workingDraw(), fleet.standbyDraw()));
		lines.add(Component.translatable(pickupCorner == null ? "message.jugcraft.drone.status.no_pickup"
				: packagerPos == null ? "message.jugcraft.drone.status.no_packager" : "message.jugcraft.drone.status.packager"));
		lines.add(Component.translatable("message.jugcraft.drone.status.jobs", openTargets, coveredSkipped));
		if (!missing.isEmpty()) {
			StringBuilder text = new StringBuilder();
			missing.forEach((item, count) -> text.append(text.isEmpty() ? "" : ", ").append(count).append("× ").append(new ItemStack(item).getHoverName().getString()));
			lines.add(Component.translatable("message.jugcraft.drone.status.missing", text.toString()));
		}
		return lines;
	}

	// ------------------------------------------------------------------ ticking

	void serverTick(ServerLevel level, BlockPos pos) {
		DroneDepots.add(level, pos);
		if (unlimitedPower && energy.getAmount() < ENERGY_CAPACITY) {
			energy.setAmount(ENERGY_CAPACITY);
		}
		scheduler.startTick();
		long now = level.getGameTime();
		if (platformDirty && now - lastScan >= SCAN_INTERVAL) {
			rescan(level);
		}

		boolean active = scheduler.activeFlights() > 0;
		long draw = active ? fleet.workingDraw() : fleet.standbyDraw();
		long have = energy.getAmount();
		long rate;
		if (draw <= 0 || have >= draw) {
			energy.setAmount(have - Math.max(0, draw));
			rate = FlightScheduler.FULL_RATE;
		} else {
			rate = FlightScheduler.FULL_RATE * have / draw;
			energy.setAmount(0);
		}
		boolean wasLow = lowPower;
		lowPower = rate < FlightScheduler.FULL_RATE;
		if (active && Math.abs(rate - currentRate) > FlightScheduler.FULL_RATE / 10 || wasLow != lowPower) {
			viewDirty = true;
		}
		currentRate = rate;
		if (active) {
			scheduler.tick(rate, listener);
			setChanged();
		}
		if (!lowPower && JugcraftDrones.enabled() && ((now + pos.hashCode()) % JOB_INTERVAL == 0 || dispatchAgain)) {
			dispatchAgain = false;
			dispatch(level, now);
		}
		if (viewDirty && now - lastViewSync >= VIEW_MIN_INTERVAL || now - lastViewSync >= VIEW_REFRESH) {
			viewDirty = false;
			lastViewSync = now;
			level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	private void rescan(ServerLevel level) {
		lastScan = level.getGameTime();
		platformDirty = false;
		BlockPos seed = findSeed(level);
		if (seed == null) {
			unformPads(level);
			packagerPos = null;
			pickupCorner = null;
			viewDirty = true;
			return;
		}
		platformY = seed.getY();
		PlatformLayout old = layout;
		// The base layer is landing platform; a base cell with a pad (or pickup) plate on top is a pad (or pickup) cell.
		layout = PlatformLayout.scan(seed.getX(), seed.getZ(), (x, z) -> {
			BlockPos at = new BlockPos(x, platformY, z);
			if (!level.isLoaded(at) || !level.getBlockState(at).is(JugcraftDrones.LANDING_PLATFORM)) {
				return PlatformLayout.Cell.NONE;
			}
			BlockState plate = level.getBlockState(at.above());
			return plate.is(JugcraftDrones.LANDING_PAD) ? PlatformLayout.Cell.PAD
					: plate.is(JugcraftDrones.SUPPLY_PICKUP) ? PlatformLayout.Cell.PICKUP : PlatformLayout.Cell.PLATFORM;
		});
		formPads(level, old);
		pickupCorner = layout.pickups().isEmpty() ? null
				: new int[] {(int) layout.pickups().getFirst()[0], (int) layout.pickups().getFirst()[1]};
		packagerPos = findPackager(level);
		formTowerPickups(level);
		docksDirty = true;
		viewDirty = true;
	}

	/**
	 * Gives every plate of each complete 5x5 pad (and 3x3 supply pickup) its tile number, so together
	 * they show one pad with a charger port (or one loading square with a lift hatch), and sets any
	 * other plate on the platform (or on the previous platform) back to loose. Only plates whose state
	 * changes are touched.
	 */
	private void formPads(ServerLevel level, PlatformLayout old) {
		Map<Long, Integer> parts = new HashMap<>();
		// Plates form on a big-enough platform, even before it has a pad (so a lone pickup shows it was sensed).
		if (layout.status() == PlatformLayout.Status.OK || layout.status() == PlatformLayout.Status.NO_PAD) {
			for (long[] pad : layout.pads()) {
				for (int dx = 0; dx < PlatformLayout.PAD_SIZE; dx++) {
					for (int dz = 0; dz < PlatformLayout.PAD_SIZE; dz++) {
						parts.put(BlockPos.asLong((int) pad[0] + dx, platformY + 1, (int) pad[1] + dz), LandingPadBlock.part(dx, dz));
					}
				}
			}
			for (long[] pickup : layout.pickups()) {
				for (int dx = 0; dx < PlatformLayout.PICKUP_SIZE; dx++) {
					for (int dz = 0; dz < PlatformLayout.PICKUP_SIZE; dz++) {
						parts.put(BlockPos.asLong((int) pickup[0] + dx, platformY + 1, (int) pickup[1] + dz), SupplyPickupBlock.part(dx, dz));
					}
				}
			}
		}
		Set<Long> plates = new HashSet<>(parts.keySet());
		for (PlatformLayout source : new PlatformLayout[] {old, layout}) {
			if (source.status() == PlatformLayout.Status.NO_PLATFORM) {
				continue;
			}
			for (long key : source.cellKeys()) {
				plates.add(BlockPos.asLong(PlatformLayout.keyX(key), (source == old ? oldPlatformY : platformY) + 1, PlatformLayout.keyZ(key)));
			}
		}
		oldPlatformY = platformY;
		for (long key : plates) {
			BlockPos at = BlockPos.of(key);
			BlockState state = level.isLoaded(at) ? level.getBlockState(at) : null;
			if (state != null && state.getBlock() instanceof LandingPadBlock) {
				int part = parts.getOrDefault(key, 0);
				if (state.getValue(LandingPadBlock.PART) != part) {
					level.setBlock(at, state.setValue(LandingPadBlock.PART, part), Block.UPDATE_CLIENTS);
				}
			} else if (state != null && state.getBlock() instanceof SupplyPickupBlock) {
				int part = parts.getOrDefault(key, 0);
				if (state.getValue(SupplyPickupBlock.PART) != part) {
					level.setBlock(at, state.setValue(SupplyPickupBlock.PART, part), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** Sets all plates of this depot's pads back to loose (the terminal is going away). */
	private void unformPads(ServerLevel level) {
		PlatformLayout old = layout;
		layout = PlatformLayout.EMPTY;
		formPads(level, old);
	}

	private @Nullable BlockPos findSeed(ServerLevel level) {
		BlockPos origin = getBlockPos();
		for (int dy = 1; dy <= SEED_DEPTH; dy++) {
			for (int r = 0; r <= SEED_RADIUS; r++) {
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
							continue;
						}
						BlockPos at = origin.offset(dx, -dy, dz);
						if (level.isLoaded(at) && level.getBlockState(at).is(JugcraftDrones.LANDING_PLATFORM)) {
							return at;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * A cargo packager beside the supply pickup: touching one of its nine plates from the side, standing
	 * on the platform (one level up) or set into the base layer next to it.
	 */
	private @Nullable BlockPos findPackager(ServerLevel level) {
		if (pickupCorner == null) {
			return null;
		}
		int size = PlatformLayout.PICKUP_SIZE;
		for (int dy = 1; dy >= 0; dy--) {
			for (int i = -1; i <= size; i++) {
				for (int[] at : new int[][] {{i, -1}, {i, size}, {-1, i}, {size, i}}) {
					if ((at[0] < 0 || at[0] >= size) && (at[1] < 0 || at[1] >= size)) {
						continue; // corners don't touch
					}
					BlockPos pos = new BlockPos(pickupCorner[0] + at[0], platformY + dy, pickupCorner[1] + at[1]);
					if (level.isLoaded(pos) && level.getBlockState(pos).is(JugcraftDrones.CARGO_PACKAGER)) {
						return pos;
					}
				}
			}
		}
		return null;
	}

	/** The depot's cargo packager (its store of building materials), or null (for the tower's Cargo Exchange Port). */
	public @Nullable CargoPackagerBlockEntity packagerEntity() {
		return packager();
	}

	private @Nullable CargoPackagerBlockEntity packager() {
		if (packagerPos == null || level == null) {
			return null;
		}
		return level.getBlockEntity(packagerPos) instanceof CargoPackagerBlockEntity packager ? packager : null;
	}

	/**
	 * True when the last dispatch stopped at the launch limit with idle drones and jobs left over: the next tick
	 * dispatches again, so a big fleet gets going within a second instead of five drones a second.
	 */
	private boolean dispatchAgain;

	/**
	 * Sends idle drones out: reads open positions from every job source and packs crates. Jobs are shared out so
	 * every idle drone gets work before any drone takes a second stop.
	 */
	private void dispatch(ServerLevel level, long now) {
		CargoPackagerBlockEntity packager = packager();
		if (!layout.isValid() || pickupCorner == null || packager == null || owner == null || fleet.size() == 0) {
			return;
		}
		List<Integer> idle = new ArrayList<>();
		int wanted = 0;
		for (int i = 0; i < fleet.size(); i++) {
			if (!scheduler.isFlying(i)) {
				idle.add(i);
				wanted += fleet.get(i).capacity();
			}
		}
		if (idle.isEmpty()) {
			return;
		}
		BlockPos center = BlockPos.containing(layout.centerX(), platformY + 1, layout.centerZ());
		// One list per job source (each placed blueprint, the tower): drones are shared out between them in turn,
		// so every job in range gets drones at once instead of the first ones taking the whole fleet.
		List<List<BuildJobs.Target>> jobs = new ArrayList<>();
		int total = 0;
		for (BuildJobs.Source source : BuildJobs.sources()) {
			List<BuildJobs.Target> open = source.openTargets(level, center, RANGE, owner, mode, Math.min(MAX_TARGETS_PER_REQUEST, wanted));
			if (!open.isEmpty()) {
				jobs.add(open);
				total += open.size();
			}
		}
		openTargets = total;
		coveredSkipped = 0;
		missing.clear();
		int[] next = new int[jobs.size()];
		int turn = 0;
		// Stops per drone: just enough to share the jobs out among all idle drones.
		int share = Math.max(1, (total + idle.size() - 1) / idle.size());
		for (int drone : idle) {
			int job = -1;
			for (int k = 0; k < jobs.size(); k++) {
				int candidate = (turn + k) % jobs.size();
				if (next[candidate] < jobs.get(candidate).size()) {
					job = candidate;
					break;
				}
			}
			if (!scheduler.canLaunch() || job < 0) {
				dispatchAgain = !scheduler.canLaunch() && job >= 0;
				break;
			}
			turn = job + 1;
			List<BuildJobs.Target> targets = jobs.get(job);
			DroneTier tier = fleet.get(drone);
			List<FlightScheduler.Stop<Cargo>> stops = new ArrayList<>();
			while (next[job] < targets.size() && stops.size() < Math.min(share, tier.capacity())) {
				BuildJobs.Target target = targets.get(next[job]++);
				if (inFlight.contains(target.pos())) {
					continue;
				}
				// A target needs open sky above it, or a way in from the side under a roof or overhang.
				if (!DroneRoutes.reachable(level, Vec3.atBottomCenterOf(target.pos()).add(0, STOP_HOVER, 0))) {
					// Paid-for work no drone can get to (a spot to clear deep under the hangar deck, a tile walled in):
					// the tower's own crew does it on the spot, so a build never stalls at 99%.
					if (target.supplied() && target.source().crewMayFinish() && target.source().reserve(level, target, depotId, now + RESERVE_TICKS)) {
						try {
							target.source().fill(level, target, target.state());
						} finally {
							target.source().release(level, target, depotId);
						}
						continue;
					}
					coveredSkipped++;
					continue;
				}
				Cargo cargo = pack(level, packager, target, now);
				if (cargo != null) {
					stops.add(new FlightScheduler.Stop<>(cargo));
				}
			}
			if (stops.isEmpty()) {
				continue;
			}
			if (!launch(level, drone, tier, stops)) {
				for (FlightScheduler.Stop<Cargo> stop : stops) {
					Cargo cargo = stop.cargo();
					inFlight.remove(cargo.pos);
					if (cargo.target != null) {
						cargo.target.source().release(level, cargo.target, depotId);
					}
					if (!cargo.supplied) {
						returnItem(cargo.item);
					}
				}
			}
		}
		setChanged();
	}

	/** Reserves the position and takes one matching item from the packager, or returns null. */
	private @Nullable Cargo pack(ServerLevel level, CargoPackagerBlockEntity packager, BuildJobs.Target target, long now) {
		BlockState wanted = target.state();
		if (target.supplied()) {
			// Paid for by the source (the tower's modules): reserve it and fly it, nothing from the packager.
			if (!target.source().reserve(level, target, depotId, now + RESERVE_TICKS)) {
				return null;
			}
			inFlight.add(target.pos());
			return new Cargo(target, target.pos(), new ItemStack(wanted.getBlock().asItem()), wanted, true);
		}
		if ((packager.unlimited() || CreativeSupplyCrateBlock.near(level, getBlockPos())) && target.anyOf() == null
				&& wanted.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
			// The endless test supply (a Creative Supply Crate nearby, or /dronetest supplies): any block, nothing taken.
			if (!target.source().reserve(level, target, depotId, now + RESERVE_TICKS)) {
				return null;
			}
			inFlight.add(target.pos());
			// Marked supplied: nothing goes back into the packager if the delivery is cancelled.
			return new Cargo(target, target.pos(), new ItemStack(wanted.getBlock().asItem()), wanted, true);
		}
		java.util.function.Predicate<ItemStack> matches;
		if (target.anyOf() != null) {
			matches = stack -> stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock().defaultBlockState().is(target.anyOf());
		} else {
			Item item = wanted.getBlock().asItem();
			matches = stack -> stack.is(item);
		}
		if (packager.count(matches) == 0) {
			Item shown = target.anyOf() != null ? null : wanted.getBlock().asItem();
			if (shown != null) {
				missing.merge(shown, 1, Integer::sum);
			}
			return null;
		}
		if (!target.source().reserve(level, target, depotId, now + RESERVE_TICKS)) {
			return null;
		}
		ItemStack item = packager.takeOne(matches);
		if (item.isEmpty()) {
			target.source().release(level, target, depotId);
			return null;
		}
		BlockState state = wanted;
		if (target.anyOf() != null && item.getItem() instanceof BlockItem blockItem) {
			Block block = blockItem.getBlock();
			state = block.defaultBlockState();
		}
		inFlight.add(target.pos());
		return new Cargo(target, target.pos(), item, state, false);
	}

	/**
	 * Plans the flight (dock, supply pickup, each stop, dock) with a cruise height per leg over the
	 * terrain, and starts it. Positions are stored relative to this block for the clients.
	 */
	private boolean launch(ServerLevel level, int drone, DroneTier tier, List<FlightScheduler.Stop<Cargo>> stops) {
		List<int[]> pickups = activePickups();
		if (pickups.isEmpty()) {
			return false;
		}
		List<Vec3> points = new ArrayList<>();
		Vec3 home = dockPosition(drone);
		double[] exit = dockExit(drone);
		Vec3 out = exit == null ? home : new Vec3(exit[0], home.y, exit[1]);
		points.add(home);
		points.add(pickupHover(pickups, out));
		for (FlightScheduler.Stop<Cargo> stop : stops) {
			points.add(Vec3.atBottomCenterOf(stop.cargo().pos).add(0, STOP_HOVER, 0));
		}
		points.add(home);
		BlockPos origin = getBlockPos();
		int n = points.size();
		// Where the drone climbs and comes down for each point: the hangar's exit at home, the side approach for
		// a point under a roof or overhang, otherwise straight at the point.
		double[][] climb = new double[n][];
		climb[0] = exit;
		climb[n - 1] = exit;
		for (int i = 1; i < n - 1; i++) {
			climb[i] = DroneRoutes.approach(level, points.get(i));
		}
		double[][] relative = new double[n][];
		double[][] approachRel = new double[n][];
		double[] cruise = new double[n - 1];
		for (int i = 0; i < n; i++) {
			Vec3 point = points.get(i);
			relative[i] = new double[] {point.x - origin.getX(), point.y - origin.getY(), point.z - origin.getZ()};
			approachRel[i] = climb[i] == null ? null : new double[] {climb[i][0] - origin.getX(), climb[i][1] - origin.getZ()};
			if (i + 1 < n) {
				Vec3 next = points.get(i + 1);
				Vec3 from = climb[i] == null ? point : new Vec3(climb[i][0], point.y, climb[i][1]);
				Vec3 to = climb[i + 1] == null ? next : new Vec3(climb[i + 1][0], next.y, climb[i + 1][1]);
				double height = DroneRoutes.cruiseHeight(level, from, to);
				if (Double.isNaN(height)) {
					return false;
				}
				cruise[i] = height - origin.getY();
			}
		}
		FlightPath path = new FlightPath(relative, cruise, tier.speed(), approachRel);
		if (scheduler.launch(drone, tier.speed(), path.legLengths(), stops) == null) {
			return false;
		}
		paths.put(drone, path);
		viewDirty = true;
		return true;
	}

	/** Height above a delivered block's base where the drone hovers while it fills in. */
	public static final double STOP_HOVER = 1.6;

	/** Top surface of the pad and pickup plates. */
	private double plateTop() {
		return platformY + 1 + LandingPadBlock.HEIGHT / 16.0;
	}

	/** Where drone {@code index} docks: its hangar in the tower, or its slot round its pad's charger port (absolute). */
	Vec3 dockPosition(int index) {
		if (docksDirty) {
			docks = DockLayout.assign(fleet.drones(), layout.pads().size());
			io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
			List<Spot> assigned = tower == null ? null : towerSpots(fleet.drones(), tower);
			spots = assigned == null ? List.of() : assigned;
			docksDirty = false;
		}
		if (!spots.isEmpty() && index < spots.size()) {
			return spots.get(index).pos();
		}
		List<long[]> pads = layout.pads();
		if (pads.isEmpty() || index >= docks.size()) {
			return Vec3.atCenterOf(getBlockPos());
		}
		DockLayout.Dock dock = docks.get(index);
		long[] pad = pads.get(dock.pad() % pads.size());
		return new Vec3(pad[0] + PlatformLayout.PAD_SIZE / 2.0 + dock.dx(), plateTop(), pad[1] + PlatformLayout.PAD_SIZE / 2.0 + dock.dz());
	}

	/** Where drone {@code index} docks (absolute); public for game tests. */
	public Vec3 dockOf(int index) {
		return dockPosition(index);
	}

	/** The point outside drone {@code index}'s hangar (or beyond the hangar deck) it flies out to, or null. */
	double @Nullable [] dockExit(int index) {
		dockPosition(index);
		return !spots.isEmpty() && index < spots.size() ? spots.get(index).exit() : null;
	}

	/** Where drones hover over a supply pickup's lift hatch to winch up a crate: the pickup nearest {@code from} (absolute). */
	private Vec3 pickupHover(List<int[]> pickups, Vec3 from) {
		Vec3 best = null;
		for (int[] p : pickups) {
			Vec3 hover = new Vec3(p[0] + PlatformLayout.PICKUP_SIZE / 2.0, p[1] + LandingPadBlock.HEIGHT / 16.0 + FlightPath.PICKUP_HOVER,
					p[2] + PlatformLayout.PICKUP_SIZE / 2.0);
			if (best == null || hover.distanceToSqr(from) < best.distanceToSqr(from)) {
				best = hover;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------ client view

	/** The snapshot sent to clients (positions relative to this block). */
	DepotView buildView() {
		DepotView v = new DepotView();
		BlockPos origin = getBlockPos();
		v.syncTime = level == null ? 0 : level.getGameTime();
		v.mode = mode.id();
		v.status = layout.status().ordinal();
		v.minX = layout.minX();
		v.minZ = layout.minZ();
		v.width = layout.width();
		v.depth = layout.depth();
		v.platformY = platformY;
		for (long[] pad : layout.pads()) {
			v.pads.add(new int[] {(int) pad[0], (int) pad[1]});
		}
		v.pickup = pickupCorner == null ? null : pickupCorner.clone();
		v.pickups.addAll(activePickups());
		io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity tower = tower();
		v.towerTier = tower == null ? 0 : tower.tier();
		v.towerPos = towerPos;
		if (tower != null) {
			// Every built hangar's roll-up bay door (absolute), with its dock relative to this terminal, for the renderer.
			BlockPos core = tower.getBlockPos();
			io.github.jimbozoomer.jugcraft.tower.TowerData data = io.github.jimbozoomer.jugcraft.tower.TowerData.get();
			for (int t = 1; t <= tower.tier(); t++) {
				for (io.github.jimbozoomer.jugcraft.tower.TowerData.Dock d : data.tier(t).docks) {
					int[] door = d.door();
					if (door != null) {
						v.doors.add(new int[] {core.getX() + door[0], core.getZ() + door[1], core.getX() + door[2], core.getZ() + door[3],
								core.getY() + door[4], door[5], door[6], door[7],
								(int) Math.round((core.getX() + d.x() - getBlockPos().getX()) * 2),
								(int) Math.round((core.getZ() + d.z() - getBlockPos().getZ()) * 2)});
					}
				}
			}
		}
		v.maxDrones = maxDrones();
		v.maxTier = maxTier();
		for (int i = 0; i < fleet.size(); i++) {
			if (!scheduler.isFlying(i)) {
				Vec3 dock = dockPosition(i);
				v.docks.add(new DepotView.DockView(fleet.get(i).number(), dock.x - origin.getX(), dock.y - origin.getY(), dock.z - origin.getZ()));
			}
		}
		for (FlightScheduler.Flight<Cargo> flight : scheduler.flights()) {
			FlightPath path = paths.get(flight.drone());
			if (path != null && flight.drone() < fleet.size()) {
				v.flights.add(new DepotView.FlightView(flight.drone(), fleet.get(flight.drone()).number(), path, flight.progress(),
						flight.total(), currentRate));
			}
		}
		v.slotsUsed = fleet.slotsUsed();
		v.slots = layout.slots();
		v.energy = energy.getAmount();
		v.capacity = energy.getCapacity();
		v.workingDraw = fleet.workingDraw();
		v.standbyDraw = fleet.standbyDraw();
		v.lowPower = lowPower;
		v.packager = packagerPos != null;
		v.openTargets = openTargets;
		v.covered = coveredSkipped;
		int shown = 0;
		for (Map.Entry<Item, Integer> entry : missing.entrySet()) {
			if (shown++ >= 8) {
				break;
			}
			v.missing.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(), entry.getValue());
		}
		return v;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput
				.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, registries);
		buildView().write(output.child("view"));
		return output.buildResult();
	}

	private void deliver(Cargo cargo) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		BuildJobs.Target target = cargo.target;
		if (target == null || owner == null) {
			return;
		}
		try {
			if (serverLevel.isLoaded(cargo.pos) && target.source().stillWanted(serverLevel, target, owner, mode)
					&& target.source().fill(serverLevel, target, cargo.state)) {
				cargo.placed = true;
			}
		} finally {
			target.source().release(serverLevel, target, depotId);
		}
	}

	private void returnItem(ItemStack item) {
		if (item.isEmpty() || level == null) {
			return;
		}
		CargoPackagerBlockEntity packager = packager();
		ItemStack rest = packager == null ? item : packager.giveBack(item);
		if (!rest.isEmpty()) {
			BlockPos pos = getBlockPos();
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, rest);
		}
	}

	// ------------------------------------------------------------------ removal and saving

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level == null || level.isClientSide()) {
			return;
		}
		for (FlightScheduler.Flight<Cargo> flight : scheduler.abortAll()) {
			for (FlightScheduler.Stop<Cargo> stop : flight.stops()) {
				Cargo cargo = stop.cargo();
				if (cargo.target != null && level instanceof ServerLevel serverLevel) {
					cargo.target.source().release(serverLevel, cargo.target, depotId);
				}
				if (!cargo.placed && !cargo.supplied) {
					returnItem(cargo.item);
				}
			}
		}
		inFlight.clear();
		if (level instanceof ServerLevel serverLevel) {
			unformPads(serverLevel);
		}
		for (DroneTier tier : fleet.clear()) {
			Item item = JugcraftDrones.DRONES.get(tier);
			if (item != null) {
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(item));
			}
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null) {
			DroneDepots.remove(level, getBlockPos());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		java.util.Optional<ValueInput> update = input.child("view");
		if (update.isPresent()) {
			// A client update from the server: only the view, nothing else.
			view = DepotView.read(update.get());
			return;
		}
		energy.setAmount(input.getLongOr("energy", 0L));
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		depotId = input.read("depot_id", UUIDUtil.CODEC).orElseGet(UUID::randomUUID);
		mode = "party".equals(input.getStringOr("mode", "personal")) ? UseMode.PARTY : UseMode.PERSONAL;
		towerPos = input.getLong("tower").map(BlockPos::of).orElse(null);
		standaloneMaxTier = Math.max(0, input.getIntOr("standalone_max_tier", 0));
		unlimitedPower = input.getBooleanOr("test_power", false);
		List<DroneTier> saved = new ArrayList<>();
		for (int number : input.getIntArray("drones").orElse(new int[0])) {
			DroneTier.byNumber(number).ifPresent(saved::add);
		}
		fleet.load(saved);
		scheduler.abortAll();
		inFlight.clear();
		paths.clear();
		docksDirty = true;
		for (ValueInput flight : input.childrenListOrEmpty("flights")) {
			List<FlightScheduler.Stop<Cargo>> stops = new ArrayList<>();
			List<Long> arrive = new ArrayList<>();
			List<Boolean> delivered = new ArrayList<>();
			for (ValueInput stop : flight.childrenListOrEmpty("stops")) {
				ItemStack item = stop.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
				BlockState state = stop.read("state", BlockState.CODEC).orElse(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
				// Job sources are not saved: after a restart a stop can no longer be filled, so its item
				// simply returns to the packager when the drone lands. Nothing is lost or duplicated.
				Cargo cargo = new Cargo(null, BlockPos.of(stop.getLongOr("pos", 0L)), item, state, stop.getBooleanOr("supplied", false));
				cargo.placed = stop.getBooleanOr("placed", false);
				stops.add(new FlightScheduler.Stop<>(cargo));
				arrive.add(stop.getLongOr("arrive", 0L));
				delivered.add(stop.getBooleanOr("delivered", false));
			}
			long[] arriveAt = arrive.stream().mapToLong(Long::longValue).toArray();
			boolean[] done = new boolean[delivered.size()];
			for (int i = 0; i < done.length; i++) {
				done[i] = delivered.get(i);
			}
			int drone = flight.getIntOr("drone", -1);
			if (drone >= 0 && drone < fleet.size()
					&& scheduler.restore(drone, stops, arriveAt, done, flight.getLongOr("total", 0L), flight.getLongOr("progress", 0L))) {
				FlightPath path = DepotView.decode(flight.getIntArray("path").orElse(new int[0]), flight.getIntOr("speed", 0));
				if (path != null) {
					paths.put(drone, path);
				}
			}
		}
		platformDirty = true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("schema", 1);
		output.putLong("energy", energy.getAmount());
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.store("depot_id", UUIDUtil.CODEC, depotId);
		output.putString("mode", mode.id());
		if (towerPos != null) {
			output.putLong("tower", towerPos.asLong());
		}
		if (standaloneMaxTier != 0) {
			output.putInt("standalone_max_tier", standaloneMaxTier);
		}
		if (unlimitedPower) {
			output.putBoolean("test_power", true);
		}
		output.putIntArray("drones", fleet.drones().stream().mapToInt(DroneTier::number).toArray());
		ValueOutput.ValueOutputList flights = output.childrenList("flights");
		for (FlightScheduler.Flight<Cargo> flight : scheduler.flights()) {
			ValueOutput out = flights.addChild();
			out.putInt("drone", flight.drone());
			out.putLong("total", flight.total());
			out.putLong("progress", flight.progress());
			FlightPath path = paths.get(flight.drone());
			if (path != null) {
				out.putInt("speed", path.speed());
				out.putIntArray("path", DepotView.encode(path));
			}
			ValueOutput.ValueOutputList stops = out.childrenList("stops");
			for (int i = 0; i < flight.stops().size(); i++) {
				Cargo cargo = flight.stops().get(i).cargo();
				ValueOutput stop = stops.addChild();
				stop.putLong("pos", cargo.pos.asLong());
				if (!cargo.item.isEmpty()) {
					stop.store("item", ItemStack.CODEC, cargo.item);
				}
				stop.store("state", BlockState.CODEC, cargo.state);
				stop.putLong("arrive", flight.arriveAt(i));
				stop.putBoolean("delivered", flight.delivered(i));
				stop.putBoolean("placed", cargo.placed);
				stop.putBoolean("supplied", cargo.supplied);
			}
		}
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		if (!level.isClientSide()) {
			DroneDepots.add(level, getBlockPos());
		}
	}
}
