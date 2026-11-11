package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Drone Tower Core: the block the tower grows from, set into the ground where the middle of the control building's
 * hall will be. It holds the blocks players (or pipes and hoppers) put in for the tower, and its tier.
 *
 * <p>Every tower is the Tri-Pillar ({@link TowerData#CURRENT_LAYOUT}). Each step costs exactly the blocks it places
 * ({@link TowerData#cost}); the core takes any of them the steps still to come need, and no more.
 * UPGRADE DRONE TOWER (from the status screen) pays the next step's blocks and starts building it. The foundation (the control building with its exchanges and
 * supply yard, depot terminal included) and tier 1 (the six roof pads) the core builds itself,
 * {@link #TRIPILLAR_BLOCKS_PER_TICK} blocks a tick. Every later tier is built by the depot's drones: the core offers
 * its blocks as build jobs ({@link TowerBuildJobs}), one tile per drone stop, bottom up. When the last tile is in, the
 * tower reaches that tier: more hangars, more pickups, a bigger fleet and the next drone tier unlocked.
 *
 * <p>A tower saved in an earlier design is converted from its first tick after loading: its old blocks are cleared, the Tri-Pillar is
 * built up to the same tier, and its depot (owner, settings, drones, the packager's store) carries over; a tier it was
 * building is credited to its store as the Tri-Pillar tier's blocks (see {@link #serverTick}).
 */
public class TowerCoreBlockEntity extends BlockEntity {
	public static final int CORE_BLOCKS_PER_TICK = 16;
	/** The Tri-Pillar control building's own rate (about a minute for its 65,000 blocks). */
	public static final int TRIPILLAR_BLOCKS_PER_TICK = 64;
	/** Converting an old tower: blocks cleared or placed a tick (a whole tower in well under a minute). */
	public static final int CONVERT_BLOCKS_PER_TICK = 2048;
	/** Conversion stages. */
	public static final int CONVERT_CLEARING = 1, CONVERT_BUILDING = 2;
	/** The core's store as saved and as carried by its item: item, count. */
	public static final com.mojang.serialization.Codec<Map<Item, Integer>> STORE_CODEC =
			com.mojang.serialization.Codec.unboundedMap(net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec(),
					com.mojang.serialization.Codec.INT);
	/** How close (squared blocks) a player must be to press UPGRADE. */
	public static final double REACH_SQR = 10 * 10;
	/**
	 * How far a tower's drones build, in chunks each way from the core's chunk (from tier 1 on). Another tower of
	 * the same player must stand outside it.
	 */
	public static final int BUILD_RADIUS_CHUNKS = 250;
	/** Earlier saved towers retain their original loaded footprint. */
	public static final int FOOTPRINT = 24;
	public static final int LARGE_EXCHANGE_FOOTPRINT = 32;
	/** The current design includes an independently removable 81x81 paved foundation. */
	public static final int APRON_FOOTPRINT = 40;
	/** The Tri-Pillar tower: its 104-wide control building, the exchanges on its flanks and the supply yard. */
	public static final int TRIPILLAR_FOOTPRINT = 64;
	public static int footprintRadius(ServerLevel level, BlockPos core) {
		if (level.isLoaded(core) && level.getBlockEntity(core) instanceof TowerCoreBlockEntity tower) {
			if (tower.layoutVersion() >= TowerData.TRIPILLAR_LAYOUT || tower.converting != 0) return TRIPILLAR_FOOTPRINT;
			if (tower.layoutVersion() >= TowerData.APRON_LAYOUT) return APRON_FOOTPRINT;
			return tower.layoutVersion() >= TowerData.LARGE_INPUT_LAYOUT ? LARGE_EXCHANGE_FOOTPRINT : FOOTPRINT;
		}
		// A removed/unloaded core may have held the largest layout's tickets. Never load it just to inspect its BE.
		return TRIPILLAR_FOOTPRINT;
	}
	public static final net.minecraft.server.level.TicketType CHUNK_TICKET = new net.minecraft.server.level.TicketType(0,
			net.minecraft.server.level.TicketType.FLAG_PERSIST | net.minecraft.server.level.TicketType.FLAG_LOADING
			| net.minecraft.server.level.TicketType.FLAG_SIMULATION | net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

	/** Distance between two chunks, counted in chunks along the longer axis (a square radius). */
	public static int chunkDistance(net.minecraft.world.level.ChunkPos a, net.minecraft.world.level.ChunkPos b) {
		return a.getChessboardDistance(b);
	}

	/**
	 * Keeps the tower's own chunks loaded (or lets them go): the depot, its drones and its exchanges keep working
	 * with nobody nearby. Only the tower is kept loaded; drones build at a site only while its chunks are loaded,
	 * and nothing in between needs to be.
	 */
	public static void keepLoaded(ServerLevel level, BlockPos core, boolean loaded) {
		int radius = footprintRadius(level, core);
		net.minecraft.world.level.ChunkPos min = net.minecraft.world.level.ChunkPos.containing(core.offset(-radius, 0, -radius));
		net.minecraft.world.level.ChunkPos max = net.minecraft.world.level.ChunkPos.containing(core.offset(radius, 0, radius));
		for (int x = min.x(); x <= max.x(); x++) {
			for (int z = min.z(); z <= max.z(); z++) {
				var chunk = new net.minecraft.world.level.ChunkPos(x, z);
				if (loaded) {
					level.getChunkSource().addTicketWithRadius(CHUNK_TICKET, chunk, 2);
				} else if (!TowerRegistry.get(level).needsChunk(level, chunk, core)) {
					level.getChunkSource().removeTicketWithRadius(CHUNK_TICKET, chunk, 2);
				}
			}
		}
	}

	/** Set once the chunks are forced after loading (cores placed before chunk loading existed get it too). */
	private boolean chunksForced;
	/** Set once the core has listed itself in its dimension's {@link TowerRegistry} since it loaded (not saved). */
	private boolean tracked;

	private @Nullable UUID owner;
	/** New cores use the current design; saves without this field are the original design (and get converted). */
	private int layoutVersion = TowerData.CURRENT_LAYOUT;
	/** The foundation (a design with one): 0 not started, 1 the core is building it, 2 built. */
	private int foundation;
	private int tier;
	/** The tier being built (0: none). */
	private int building;
	/** Tier 1: index of the next block the core places. */
	private int progress;
	/** Tiers 2-9: tiles already placed. */
	private BitSet filled = new BitSet();
	/** The blocks put in for the steps to come, by item (only what those steps still need is taken). */
	private final Map<Item, Integer> store = new java.util.LinkedHashMap<>();
	private @Nullable TowerBuildJobs jobs;
	/** Client side: the share of the current build that is done (0 to 1). */
	private float clientProgress;
	/** Converting from an earlier design: 0 no, {@link #CONVERT_CLEARING} or {@link #CONVERT_BUILDING}. */
	private int converting;
	/** The design being cleared, and the tier the tower is rebuilt to. */
	private int convertFrom, convertTier;
	/** Where the conversion has got to: the step (clearing: old tier; building: 0 foundation, then tiers) and block index. */
	private int convertStep, convertIndex;
	/** The depot the old terminal handed over, until the rebuilt terminal takes it. */
	private DroneTerminalBlockEntity.@Nullable Handover carried;
	/** What was in the old design's storage (a rack, a cabinet) the conversion cleared: handed back on the terminal at the end. */
	private final java.util.List<ItemStack> keptStores = new java.util.ArrayList<>();

	/**
	 * BUILD FIRST LAYER was pressed while the foundation still had to be built: when it is done, tier 1 starts at once
	 * from the blocks already in.
	 */
	private boolean firstLayerQueued;

	private final StoreInlet inlet = new StoreInlet(false);
	private final StoreInlet storageInlet = new StoreInlet(true);

	public TowerCoreBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTower.CORE_ENTITY, pos, state);
	}

	public int tier() {
		return tier;
	}

	public int layoutVersion() {
		return layoutVersion;
	}

	public TowerData layout() {
		return TowerData.get(layoutVersion);
	}

	/** True while the core rebuilds a tower of an earlier design as the current one. */
	public boolean converting() {
		return converting != 0;
	}

	/** True when the design has a foundation and it is not built yet (the next step is the foundation). */
	public boolean needsFoundation() {
		return layout().foundation != null && foundation != 2;
	}

	/** The foundation: 0 not started, 1 building, 2 built (always 0 for a design without one). */
	public int foundationState() {
		return layout().foundation == null ? 0 : foundation;
	}

	/** True while the core is building the foundation. */
	public boolean buildingFoundation() {
		return foundation == 1 && layout().foundation != null;
	}

	/** The next step UPGRADE pays for: the foundation, the next tier, or null at the top. */
	public TowerData.@Nullable Tier nextStep() {
		if (needsFoundation()) {
			return layout().foundation;
		}
		return tier < TowerData.TIERS ? layout().tier(tier + 1) : null;
	}

	public int building() {
		return building;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** How many of {@code item} the core holds for the tower. */
	public int stored(Item item) {
		return store.getOrDefault(item, 0);
	}

	/** Everything the core holds, in the order it came in. */
	public Map<Item, Integer> store() {
		return java.util.Collections.unmodifiableMap(store);
	}

	/** Takes up to {@code count} of {@code item} out of the store (requisitions, tower/squad/Requisitions: used up); how
	 * many it took. */
	public int take(Item item, int count) {
		int have = stored(item), took = Math.min(have, Math.max(0, count));
		if (took > 0) {
			if (have - took > 0) {
				store.put(item, have - took);
			} else {
				store.remove(item);
			}
			changed();
		}
		return took;
	}

	/** Fills the store with everything the steps still to pay for need (tests and {@code /dronetest stock}). */
	public void stockAll() {
		java.util.Set<Item> items = new java.util.LinkedHashSet<>();
		TowerData data = layout();
		if (data.foundation != null) {
			items.addAll(data.cost(data.foundation).keySet());
		}
		for (TowerData.Tier t : data.tiers) {
			items.addAll(data.cost(t).keySet());
		}
		for (Item item : items) {
			int room = Math.max(0, stillNeeded(item) - stored(item));
			if (room > 0) {
				store.merge(item, room, Integer::sum);
			}
		}
		changed();
	}

	/** A creative player's LOAD ALL on the first layer's screen: everything the foundation and tier 1 still need. */
	public void fillFirstLayer() {
		for (Map.Entry<Item, Integer> need : firstLayerCost().entrySet()) {
			int room = room(need.getKey());
			if (room > 0) {
				store.merge(need.getKey(), room, Integer::sum);
			}
		}
		changed();
	}

	/** Takes over the store of the core this one replaces (a core that set itself into the ground). */
	void adoptStore(Map<Item, Integer> from) {
		store.clear();
		store.putAll(from);
		changed();
	}

	/**
	 * How many of {@code item} the steps still to pay for need in all: the next step (the foundation first) up to tier 9.
	 * A step being built is paid already; while converting, the tiers being rebuilt are free.
	 */
	public int stillNeeded(Item item) {
		TowerData data = layout();
		int from;
		if (converting != 0) {
			from = convertTier + 1;
		} else if (needsFoundation()) {
			from = buildingFoundation() ? 1 : 0;
		} else {
			from = Math.max(tier, building) + 1;
		}
		int need = 0;
		for (int step = Math.max(0, from); step <= TowerData.TIERS; step++) {
			TowerData.Tier t = step == 0 ? data.foundation : data.tier(step);
			if (t != null) {
				need += data.cost(t).getOrDefault(item, 0);
			}
		}
		return need;
	}

	/**
	 * The first layer (Justin, 28 October 2026): the foundation and tier 1, which the core builds itself. Until tier 1 is
	 * started the core takes their blocks by hand (its loading screen), from the terminal or from a pipe; after that the
	 * tower's blocks only go in through the Cargo Exchange, into the tower storage ({@link #storageRoom}).
	 */
	public boolean firstLayer() {
		return converting == 0 && tier == 0 && building == 0;
	}

	/** True when tier 1 is set to start by itself the moment the foundation is done. */
	public boolean firstLayerQueued() {
		return firstLayerQueued;
	}

	/** The steps of the first layer still to pay for: the foundation (unless it is paid) and tier 1. */
	public List<TowerData.Tier> firstLayerSteps() {
		List<TowerData.Tier> steps = new java.util.ArrayList<>();
		if (!firstLayer()) {
			return steps;
		}
		if (needsFoundation() && !buildingFoundation()) {
			steps.add(layout().foundation);
		}
		if (!firstLayerQueued) {
			steps.add(layout().tier(1));
		}
		return steps;
	}

	/** Every block the first layer still needs in all (the foundation's and tier 1's together), most first. */
	public Map<Item, Integer> firstLayerCost() {
		Map<Item, Integer> cost = new java.util.HashMap<>();
		for (TowerData.Tier step : firstLayerSteps()) {
			layout().cost(step).forEach((item, n) -> cost.merge(item, n, Integer::sum));
		}
		Map<Item, Integer> sorted = new java.util.LinkedHashMap<>();
		cost.entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()).forEach(e -> sorted.put(e.getKey(), e.getValue()));
		return sorted;
	}

	/** How many more of {@code item} the core takes by hand, from the terminal or from a pipe: the first layer's only. */
	public int room(Item item) {
		if (!firstLayer()) {
			return 0;
		}
		return Math.max(0, firstLayerCost().getOrDefault(item, 0) - stored(item));
	}

	/** The tower storage's size in stacks (Justin: "10 double chests worth of storage"). */
	public static final int STORAGE_STACKS = 540;

	/** How many stacks the core's blocks fill (each kind in whole stacks, as in a chest). */
	public int storageStacksUsed() {
		int used = 0;
		for (Map.Entry<Item, Integer> e : store.entrySet()) {
			int max = Math.max(1, e.getKey().getDefaultMaxStackSize());
			used += (e.getValue() + max - 1) / max;
		}
		return used;
	}

	/**
	 * How many more of {@code item} the tower storage takes through the Cargo Exchange (after the first layer): what the
	 * tower still needs up to tier 9, so more than the next tier is fine, as long as it fits in {@link #STORAGE_STACKS}.
	 */
	public int storageRoom(Item item) {
		if (firstLayer() || converting != 0) {
			return 0;
		}
		if (item instanceof io.github.jimbozoomer.jugcraft.drone.DroneItem drone) {
			return droneRoom(drone);
		}
		int need = Math.max(0, stillNeeded(item) - stored(item));
		if (need == 0) {
			return 0;
		}
		int max = Math.max(1, item.getDefaultMaxStackSize());
		int held = stored(item);
		int partial = held % max == 0 ? 0 : max - held % max;
		long fits = partial + (long) Math.max(0, STORAGE_STACKS - storageStacksUsed()) * max;
		return (int) Math.min(need, fits);
	}

	/**
	 * Drones put in the tower storage (Justin, 7 November 2026: "tell them to insert drones into the tower's inventory"):
	 * any it can fly (up to its tier; the Creative Drone too), as many as its fleet still has room for counting those
	 * already waiting; they go to a free pad that fits ({@link #linkWaitingDrones}) and wait in the storage till one is.
	 */
	public int droneRoom(io.github.jimbozoomer.jugcraft.drone.DroneItem drone) {
		io.github.jimbozoomer.jugcraft.drone.DroneTier t = drone.tier();
		if (t.number() > tier && !(t.creative() && tier >= 1)) {
			return 0;
		}
		io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity terminal = terminal();
		if (terminal == null) {
			return 0;
		}
		int waiting = 0;
		for (Map.Entry<Item, Integer> e : store.entrySet()) {
			if (e.getKey() instanceof io.github.jimbozoomer.jugcraft.drone.DroneItem) {
				waiting += e.getValue();
			}
		}
		int room = terminal.maxDrones() - terminal.fleet().size() - waiting;
		int max = Math.max(1, drone.getDefaultMaxStackSize());
		int held = stored(drone);
		int partial = held % max == 0 ? 0 : max - held % max;
		long fits = partial + (long) Math.max(0, STORAGE_STACKS - storageStacksUsed()) * max;
		return (int) Math.max(0, Math.min(room, fits));
	}

	/** Sends the drones waiting in the storage to free pads that fit (each one linked to the tower's fleet). */
	void linkWaitingDrones() {
		io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity terminal = terminal();
		if (terminal == null) {
			return;
		}
		boolean any = false;
		for (Item item : new java.util.ArrayList<>(store.keySet())) {
			if (!(item instanceof io.github.jimbozoomer.jugcraft.drone.DroneItem drone)) {
				continue;
			}
			int n = store.getOrDefault(item, 0);
			while (n > 0 && terminal.linkFromStorage(drone.tier())) {
				n--;
				any = true;
			}
			if (n <= 0) {
				store.remove(item);
			} else {
				store.put(item, n);
			}
		}
		if (any) {
			changed();
		}
	}

	/**
	 * How many more of {@code item} the tower storage takes by hand at the Cargo Exchange's storage screen (Justin, 10
	 * November 2026: "you can't physically put things into the cargo storage ... I'd like there to be a way for you to
	 * physically put things in"; his pick: a storage screen that takes any item): any plain item, as long as it fits in
	 * {@link #STORAGE_STACKS} (TowerStorageMenu). Tower blocks piped in stay limited to what the tower still needs
	 * ({@link #storageRoom}).
	 */
	public int anyRoom(Item item) {
		if (firstLayer() || converting != 0) {
			return 0;
		}
		int max = Math.max(1, item.getDefaultMaxStackSize());
		int held = stored(item);
		int partial = held % max == 0 ? 0 : max - held % max;
		long fits = partial + (long) Math.max(0, STORAGE_STACKS - storageStacksUsed()) * max;
		return (int) Math.min(Integer.MAX_VALUE, fits);
	}

	/** Puts what fits of {@code stack} in the tower storage, whatever it is (a plain item: no name, enchantment or damage);
	 * how many went in. */
	public int depositAny(ItemStack stack) {
		if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) {
			return 0;
		}
		int take = Math.min(stack.getCount(), anyRoom(stack.getItem()));
		if (take <= 0) {
			return 0;
		}
		store.merge(stack.getItem(), take, Integer::sum);
		stack.shrink(take);
		changed();
		return take;
	}

	/** True when the tower storage would take some of {@code stack} (a plain item, as {@link #wants}). */
	public boolean storageWants(ItemStack stack) {
		return !stack.isEmpty() && stack.getComponentsPatch().isEmpty() && storageRoom(stack.getItem()) > 0;
	}

	/** Puts what the tower storage takes of {@code stack} in. Returns how many were taken. */
	public int depositToStorage(ItemStack stack) {
		if (!storageWants(stack)) {
			return 0;
		}
		int take = Math.min(stack.getCount(), storageRoom(stack.getItem()));
		store.merge(stack.getItem(), take, Integer::sum);
		stack.shrink(take);
		changed();
		return take;
	}

	/** LOAD ALL at the Cargo Exchange: everything in {@code player}'s inventory the tower storage takes. */
	public int depositToStorageFrom(Player player) {
		var inventory = player.getInventory();
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (storageWants(stack)) {
				taken += depositToStorage(stack);
			}
		}
		if (taken > 0) {
			inventory.setChanged();
		}
		return taken;
	}

	/** The Cargo Exchange's way into the tower storage (pipes, conveyors, hoppers): tower blocks only. */
	public InsertionOnlyStorage<ItemVariant> storageInlet() {
		return storageInlet;
	}

	/** True when what the tower pays from (TowerSupply) holds all of the next step's blocks. */
	public boolean canPayNext() {
		TowerData.Tier next = nextStep();
		if (next == null) {
			return false;
		}
		return TowerSupply.of(this).has(layout().cost(next));
	}

	/** Which tiles of the tier being built the drones have put in (synced to clients for the hologram). */
	public BitSet filledTiles() {
		return (BitSet) filled.clone();
	}

	/** How far the current build is, 0 to 1. */
	public float buildProgress() {
		if (level != null && level.isClientSide()) {
			return clientProgress;
		}
		if (converting != 0) {
			return convertProgress();
		}
		if (buildingFoundation()) {
			TowerData.Tier f = layout().foundation;
			return (float) progress / Math.max(1, f.place.length + f.air.length);
		}
		if (building == 1) {
			return (float) progress / Math.max(1, layout().tier(1).place.length + layout().tier(1).air.length);
		}
		if (building >= 2) {
			return (float) filled.cardinality() / Math.max(1, layout().tier(building).tiles.size());
		}
		return 0;
	}

	/** Where tier 1 puts the depot terminal. */
	public BlockPos terminalPos() {
		int[] t = layout().terminal;
		return getBlockPos().offset(t[0], t[1], t[2]);
	}

	public @Nullable DroneTerminalBlockEntity terminal() {
		return level != null && level.getBlockEntity(terminalPos()) instanceof DroneTerminalBlockEntity terminal ? terminal : null;
	}

	/** The core's block store (pipes, hoppers and extractors can fill it with what the first layer still needs). */
	public InsertionOnlyStorage<ItemVariant> blockStorage() {
		return inlet;
	}

	/** True when {@code stack} is something the first layer still needs (a plain item, renamed or enchanted ones aren't taken). */
	public boolean wants(ItemStack stack) {
		return !stack.isEmpty() && stack.getComponentsPatch().isEmpty() && room(stack.getItem()) > 0;
	}

	/** Takes what the first layer still needs of {@code stack}. Returns how many were taken. */
	public int deposit(ItemStack stack) {
		if (!wants(stack)) {
			return 0;
		}
		int take = Math.min(stack.getCount(), room(stack.getItem()));
		store.merge(stack.getItem(), take, Integer::sum);
		stack.shrink(take);
		changed();
		return take;
	}

	/**
	 * Takes everything in {@code player}'s inventory the first layer still needs. A creative player's endless supply fills
	 * the store with everything the whole tower still needs. Returns how many items were taken.
	 */
	public int depositFrom(Player player) {
		if (player.hasInfiniteMaterials()) {
			int before = TowerCosts.total(store);
			stockAll();
			return TowerCosts.total(store) - before;
		}
		var inventory = player.getInventory();
		int taken = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (wants(stack)) {
				taken += deposit(stack);
			}
		}
		if (taken > 0) {
			inventory.setChanged();
		}
		return taken;
	}

	/** May {@code player} run this tower: its owner, or a party member when the depot is in Party mode. */
	public boolean mayUse(Player player) {
		if (owner == null || owner.equals(player.getUUID())) {
			return true;
		}
		DroneTerminalBlockEntity terminal = terminal();
		UseMode mode = terminal == null ? UseMode.PERSONAL : terminal.mode();
		return JugcraftParties.mayServe(player.getUUID(), UseMode.PERSONAL, owner, mode);
	}

	/** UPGRADE DRONE TOWER: pays the next step's blocks from the store and starts building it. Messages the player either way. */
	public void tryUpgrade(Player player) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if (!mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.not_owner"));
			return;
		}
		if (owner == null) {
			owner = player.getUUID();
			tracked = false;                         // listed under its owner from the next tick
		}
		if (building != 0 || foundation == 1 || converting != 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.busy"));
			return;
		}
		if (tier >= TowerData.TIERS) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.max"));
			return;
		}
		boolean groundWork = tier == 0 && (layout().foundation == null || needsFoundation());
		if (!groundWork && terminal() == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.no_terminal"));
			return;
		}
		TowerData.Tier next = nextStep();
		Map<Item, Integer> price = layout().cost(next);
		// paid from everything the tower pays from: its store, its network, and what's hooked up to its Cargo
		// Exchanges (chests, pipes, warehouses: TowerSupply)
		TowerSupply supply = TowerSupply.of(this);
		Map<Item, Long> missing = supply.shortOf(price);
		if (!missing.isEmpty()) {
			Map.Entry<Item, Long> first = missing.entrySet().iterator().next();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.missing_blocks", first.getValue(),
					first.getKey().getDefaultInstance().getHoverName(), missing.size()));
			return;
		}
		supply.take(price);
		changed();
		if (next == layout().foundation) {
			foundation = 1;
			progress = 0;
			changed();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.foundation_started"));
			return;
		}
		startBuilding(server, tier + 1);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.started", tier + 1,
				Component.translatable("tower.jugcraft.tier." + (tier + 1))));
	}

	/** Takes {@code price} from what the tower pays from (its store first: TowerSupply; every block of it must be there). */
	private void pay(Map<Item, Integer> price) {
		TowerSupply.of(this).take(price);
		changed();
	}

	/**
	 * BUILD FIRST LAYER on the core's loading screen: with every block of the foundation and tier 1 in, the core builds
	 * the foundation and then goes straight on to tier 1 (it pays each as it starts it). Messages the player either way.
	 * Returns whether something started (or was set to start).
	 */
	public boolean buildFirstLayer(Player player) {
		if (!(level instanceof ServerLevel server)) {
			return false;
		}
		if (!mayUse(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.not_owner"));
			return false;
		}
		if (!firstLayer() || firstLayerQueued) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.busy"));
			return false;
		}
		if (owner == null) {
			owner = player.getUUID();
			tracked = false;                         // listed under its owner from the next tick
		}
		Map<Item, Integer> cost = firstLayerCost();
		// (no depot yet at the first layer: TowerSupply finds the store alone)
		Map<Item, Long> missing = TowerSupply.of(this).shortOf(cost);
		if (!missing.isEmpty()) {
			Map.Entry<Item, Long> first = missing.entrySet().iterator().next();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.missing_blocks", first.getValue(),
					first.getKey().getDefaultInstance().getHoverName(), missing.size()));
			return false;
		}
		if (buildingFoundation()) {
			// the foundation is going up already: tier 1 follows it
			firstLayerQueued = true;
			changed();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.first_layer_queued"));
			return true;
		}
		if (needsFoundation()) {
			pay(layout().cost(layout().foundation));
			foundation = 1;
			progress = 0;
			firstLayerQueued = true;
			changed();
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.first_layer_started"));
			return true;
		}
		pay(layout().cost(layout().tier(1)));
		startBuilding(server, 1);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.started", 1, Component.translatable("tower.jugcraft.tier.1")));
		return true;
	}

	private void startBuilding(ServerLevel server, int target) {
		building = target;
		progress = 0;
		filled = new BitSet();
		TowerData.Tier data = layout().tier(target);
		// Clear first: the lower tier's antenna where the new floors go.
		for (int[] c : data.clear) {
			BlockPos at = getBlockPos().offset(c[0], c[1], c[2]);
			if (server.isLoaded(at)) {
				server.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		if (target >= 2) {
			registerJobs();
		}
		changed();
	}

	private void registerJobs() {
		if (jobs != null) {
			BuildJobs.unregister(jobs);
		}
		jobs = new TowerBuildJobs(this, building, filled);
		BuildJobs.register(jobs);
	}

	/** Called by {@link TowerBuildJobs} when a drone has put a tile in. */
	void tileFilled(int index) {
		filled.set(index);
		changed();
	}

	public void serverTick(ServerLevel server) {
		if (!tracked) {
			tracked = true;
			TowerRegistry.get(server).track(owner, getBlockPos());
		}
		if (converting == 0 && layoutVersion != TowerData.CURRENT_LAYOUT) {
			startConversion(server);
		}
		if (!chunksForced) {
			chunksForced = true;
			keepLoaded(server, getBlockPos(), true);
		}
		if (converting != 0) {
			lightsNight = false;         // a refit puts the lamps back on
			lightsCursor = -1;
			convertTick(server);
			return;
		}
		nightLights(server);
		if (!firstLayer() && (server.getGameTime() + getBlockPos().hashCode()) % 40 == 0) {
			linkWaitingDrones();                   // drones put in the storage, to their pads
		}
		if (buildingFoundation()) {
			TowerData.Tier data = layout().foundation;
			progress = placeSome(server, data, progress, TRIPILLAR_BLOCKS_PER_TICK);
			if (progress >= data.air.length + data.place.length) {
				finishFoundation(server);
			}
			changed();
			return;
		}
		if (building == 1) {
			TowerData.Tier data = layout().tier(1);
			// Progress counts the cells to clear first, then the blocks to place.
			int total = data.air.length + data.place.length;
			if (layoutVersion >= TowerData.TRIPILLAR_LAYOUT) {
				progress = placeSome(server, data, progress, TRIPILLAR_BLOCKS_PER_TICK);
				if (progress >= total) {
					finish(server);
				}
				changed();
				return;
			}
			int end = Math.min(total, progress + CORE_BLOCKS_PER_TICK);
			for (; progress < end; progress++) {
				if (progress < data.air.length) {
					int[] c = data.air[progress];
					clear(server, getBlockPos().offset(c[0], c[1], c[2]));
					continue;
				}
				int[] b = data.place[progress - data.air.length];
				place(server, getBlockPos().offset(b[0], b[1], b[2]), layout().state(b[3]));
			}
			if (progress >= total) {
				finish(server);
			}
			changed();
		} else if (building >= 2) {
			if (jobs == null) {
				registerJobs();
			}
			if (filled.cardinality() >= layout().tier(building).tiles.size()) {
				finish(server);
			}
		}
	}

	/** Whether the lamps on the crew floors are at their night modes (or being switched to them). */
	private boolean lightsNight;
	/** Switching the lamps: the next of the layout's night lights, or -1. */
	private int lightsCursor = -1;
	/** What they are being switched to. */
	private boolean lightsTo;

	/** Whether the tower's lamps are at their night modes. */
	public boolean lightsNight() {
		return lightsNight;
	}

	/**
	 * The night lighting (TowerNightLights): once the foundation stands, at 20:00 the lamps on the crew floors go to their
	 * night modes and at 05:00 back on, a few dozen a tick.
	 */
	private void nightLights(ServerLevel server) {
		int[][] lamps = layout().nightLights();
		if (lamps.length == 0 || foundationState() != 2 && tier() < 1) {
			return;
		}
		if (lightsCursor < 0) {
			if (server.getGameTime() % TowerNightLights.CHECK_TICKS != 0) {
				return;
			}
			boolean want = TowerNightLights.night(server.getOverworldClockTime());
			if (want == lightsNight) {
				return;
			}
			lightsTo = want;
			lightsCursor = 0;
		}
		int end = Math.min(lamps.length, lightsCursor + TowerNightLights.PER_TICK);
		for (; lightsCursor < end; lightsCursor++) {
			int[] l = lamps[lightsCursor];
			TowerNightLights.set(server, getBlockPos().offset(l[0], l[1], l[2]), lightsTo ? TowerLightBlock.Mode.values()[l[3]] : TowerLightBlock.Mode.ON);
		}
		if (lightsCursor >= lamps.length) {
			lightsNight = lightsTo;
			lightsCursor = -1;
			setChanged();
		}
	}

	/**
	 * The core builds a step itself (the foundation, the Tri-Pillar's tier 1): from {@code index} on, clears the step's air
	 * cells, then places its blocks, up to {@code budget} blocks of work. Cells already empty cost nothing; positions not
	 * loaded yet are waited for. Returns the new index (the cells to clear count first).
	 */
	private int placeSome(ServerLevel server, TowerData.Tier data, int index, int budget) {
		int total = data.air.length + data.place.length;
		int work = 0;
		for (int checked = 0; index < total && work < budget && checked < 4096; checked++) {
			int[] c = index < data.air.length ? data.air[index] : data.place[index - data.air.length];
			BlockPos at = getBlockPos().offset(c[0], c[1], c[2]);
			if (!server.isLoaded(at)) {
				break;
			}
			if (index < data.air.length) {
				if (clearable(server, at)) {
					clear(server, at);
					work++;
				}
			} else {
				place(server, at, layout().state(c[3]));
				work++;
			}
			index++;
		}
		return index;
	}

	private void finishFoundation(ServerLevel server) {
		foundation = 2;
		progress = 0;
		reshapeNeonLines(server, 0);
		if (firstLayerQueued) {
			// BUILD FIRST LAYER: tier 1 goes up straight after, from the blocks put in with the foundation's
			firstLayerQueued = false;
			if (canPayNext()) {
				pay(layout().cost(layout().tier(1)));
				startBuilding(server, 1);
			}
		}
		pinDisplays(server);
		ownDoors(server, 0);
		DroneTerminalBlockEntity terminal = terminal();
		if (terminal != null) {
			if (terminal.owner() == null && owner != null) {
				terminal.setOwner(owner);
			}
			// the hall's desks, consoles and tables work the tower from now on (it holds no drones until tier 1), and
			// the exchanges feed the depot
			terminal.linkTower(getBlockPos());
			ExchangeStations.bindBuilt(server, this);
		}
		changed();
	}

	/**
	 * May a stray block at {@code at} be cleared for the tower: anything but air, a block entity (a player's
	 * chest stays where it is) or bedrock-like unbreakable blocks.
	 */
	static boolean clearable(ServerLevel server, BlockPos at) {
		BlockState state = server.getBlockState(at);
		return !state.isAir() && server.getBlockEntity(at) == null && state.getDestroySpeed(server, at) >= 0;
	}

	/** Clears one stray block inside the tower (no drops: it is site clearance). Liquids go too. */
	static void clear(ServerLevel server, BlockPos at) {
		if (server.isLoaded(at) && clearable(server, at)) {
			server.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	/** Places one tower block: never over another block entity (a player's chest stays where it is). */
	static void place(ServerLevel server, BlockPos at, BlockState state) {
		state = TowerPanelBlock.at(state, at);
		if (!server.isLoaded(at) || server.getBlockState(at) == state) {
			return;
		}
		BlockEntity existing = server.getBlockEntity(at);
		if (existing != null && !(existing instanceof DroneTerminalBlockEntity) && server.getBlockState(at).getBlock() != state.getBlock()) {
			return;
		}
		server.setBlock(at, state, Block.UPDATE_ALL);
	}

	// ------------------------------------------------------------------ repairing

	/** What {@link #repair} did: the blocks put back, and the ones it couldn't (unloaded, or something else there). */
	public record Repair(int placed, int blocked, int unloaded) {
	}

	/**
	 * Puts back every block of what the tower has built (its foundation and finished tiers) that is gone: where the
	 * design has a block and there is air (or water, grass, anything a block is put over) instead. A block someone put
	 * in its place, or a chest, stays. Then, as when a tier finishes, the displays are pinned, the doors owned, the hall
	 * linked to the terminal and the Cargo Exchanges bound again (/dronetest repair; Justin, 10 November 2026). Returns
	 * null while it is building or converting, or has built nothing.
	 */
	public @Nullable Repair repair(ServerLevel server) {
		if (converting != 0 || building != 0 || foundation != 2 || layoutVersion != TowerData.CURRENT_LAYOUT) {
			return null;
		}
		TowerData data = layout();
		java.util.List<TowerData.Tier> steps = new java.util.ArrayList<>();
		if (data.foundation != null) {
			steps.add(data.foundation);
		}
		for (int t = 1; t <= Math.min(tier, TowerData.TIERS); t++) {
			steps.add(data.tier(t));
		}
		java.util.Map<Long, Integer> design = new java.util.LinkedHashMap<>();
		for (TowerData.Tier step : steps) {
			for (int[] c : step.clear) {
				design.remove(BlockPos.asLong(c[0], c[1], c[2]));
			}
			for (int[] b : step.place) {
				if (b[0] != 0 || b[1] != 0 || b[2] != 0) {
					design.put(BlockPos.asLong(b[0], b[1], b[2]), b[3]);
				}
			}
		}
		int placed = 0, blocked = 0, unloaded = 0;
		for (java.util.Map.Entry<Long, Integer> e : design.entrySet()) {
			BlockPos at = getBlockPos().offset(BlockPos.of(e.getKey()));
			if (!server.isLoaded(at)) {
				unloaded++;
				continue;
			}
			BlockState want = data.state(e.getValue());
			BlockState here = server.getBlockState(at);
			if (want.isAir() || here.getBlock() == want.getBlock()) {
				continue;
			}
			if (!here.canBeReplaced() || server.getBlockEntity(at) != null) {
				blocked++;
				continue;
			}
			place(server, at, want);
			if (server.getBlockState(at).getBlock() == want.getBlock()) {
				placed++;
			}
		}
		if (placed > 0) {
			reshapeNeonLines(server, tier);
			pinDisplays(server);
			ownDoors(server, tier);
		}
		DroneTerminalBlockEntity terminal = terminal();
		if (terminal != null) {
			if (terminal.owner() == null && owner != null) {
				terminal.setOwner(owner);
			}
			terminal.linkTower(getBlockPos());
			ExchangeStations.bindBuilt(server, this);
		}
		changed();
		Jugcraft.LOGGER.info("Drone Tower at {}: repaired, {} blocks put back ({} with something else there, {} not loaded)",
				getBlockPos().toShortString(), placed, blocked, unloaded);
		return new Repair(placed, blocked, unloaded);
	}

	// ------------------------------------------------------------------ converting a tower of an earlier design

	/** Clearing an old tower's blocks: no drops, no block-entity side effects (the depot was handed over first), no neighbour updates. */
	private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
			| Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
	/** The highest old tier there is anything of to clear. */
	private int convertClearTo;

	/**
	 * A tower saved in an earlier design starts converting to the current one. The tier it was building is called off and
	 * its blocks credited to the store; the tiers it finished are what it is rebuilt to. Its chunks out to the Tri-Pillar's footprint
	 * are kept loaded from now on. A core that never built anything simply takes the current design.
	 */
	private void startConversion(ServerLevel server) {
		TowerData old = TowerData.get(layoutVersion);
		if (jobs != null) {
			BuildJobs.unregister(jobs);
			jobs = null;
		}
		if (tier == 0 && building == 0 && (old.foundation == null || foundation == 0)) {
			if (chunksForced) {
				keepLoaded(server, getBlockPos(), false);
				chunksForced = false;
			}
			layoutVersion = TowerData.CURRENT_LAYOUT;
			foundation = 0;
			changed();
			return;
		}
		if (building != 0) {
			// The tier it was building was paid for: its Tri-Pillar counterpart's blocks go into the store.
			TowerData current = TowerData.get(TowerData.CURRENT_LAYOUT);
			current.cost(current.tier(building)).forEach((item, n) -> store.merge(item, n, Integer::sum));
		}
		convertFrom = layoutVersion;
		convertTier = tier;
		convertClearTo = Math.max(tier, building);
		building = 0;
		progress = 0;
		filled = new BitSet();
		converting = CONVERT_CLEARING;
		convertStep = 0; // first the depot is handed over, once the whole area is loaded
		convertIndex = 0;
		keepLoaded(server, getBlockPos(), true);
		chunksForced = true;
		Jugcraft.LOGGER.info("Drone Tower at {}: converting layout {} tier {} to the Tri-Pillar (layout {})", getBlockPos().toShortString(),
				convertFrom, convertTier, TowerData.CURRENT_LAYOUT);
		changed();
	}

	private boolean areaLoaded(ServerLevel server, int radius) {
		BlockPos p = getBlockPos();
		for (int cx = (p.getX() - radius) >> 4; cx <= (p.getX() + radius) >> 4; cx++) {
			for (int cz = (p.getZ() - radius) >> 4; cz <= (p.getZ() + radius) >> 4; cz++) {
				if (!server.hasChunk(cx, cz)) {
					return false;
				}
			}
		}
		return true;
	}

	/** What the new design ends up with (up to the tier being rebuilt), cell by cell: those blocks are not cleared. */
	private java.util.Map<Long, net.minecraft.world.level.block.Block> convertKeep;

	private java.util.Map<Long, net.minecraft.world.level.block.Block> convertKeep() {
		if (convertKeep == null) {
			java.util.Map<Long, net.minecraft.world.level.block.Block> out = new java.util.HashMap<>();
			TowerData data = TowerData.get(TowerData.CURRENT_LAYOUT);
			java.util.List<TowerData.Tier> steps = new java.util.ArrayList<>();
			if (data.foundation != null) {
				steps.add(data.foundation);
			}
			for (int t = 1; t <= Math.min(convertTier, TowerData.TIERS); t++) {
				steps.add(data.tier(t));
			}
			for (TowerData.Tier step : steps) {
				for (int[] c : step.clear) {
					out.remove(BlockPos.asLong(c[0], c[1], c[2]));
				}
				for (int[] b : step.place) {
					out.put(BlockPos.asLong(b[0], b[1], b[2]), data.state(b[3]).getBlock());
				}
			}
			convertKeep = out;
		}
		return convertKeep;
	}

	private void convertTick(ServerLevel server) {
		if (!areaLoaded(server, TRIPILLAR_FOOTPRINT)) {
			return; // the chunk tickets are in; wait for the chunks
		}
		if (converting == CONVERT_CLEARING) {
			TowerData old = TowerData.get(convertFrom);
			if (convertStep == 0) {
				int[] t = old.terminal;
				if (carried == null && server.getBlockEntity(getBlockPos().offset(t[0], t[1], t[2])) instanceof DroneTerminalBlockEntity terminal) {
					carried = terminal.handOver();
				}
				convertStep = 1;
				convertIndex = 0;
			}
			int work = 0;
			// the old tiers, then (an earlier Tri-Pillar's) its foundation; what the new design has in the same place stays
			int lastClear = convertClearTo + (old.foundation != null ? 1 : 0);
			java.util.Map<Long, net.minecraft.world.level.block.Block> keep = convertKeep();
			while (convertStep <= lastClear && work < CONVERT_BLOCKS_PER_TICK) {
				int[][] place = convertStep > convertClearTo ? old.foundation.place : old.tier(convertStep).place;
				for (; convertIndex < place.length && work < CONVERT_BLOCKS_PER_TICK; convertIndex++) {
					int[] b = place[convertIndex];
					if (b[0] == 0 && b[1] == 0 && b[2] == 0) {
						continue;
					}
					BlockPos at = getBlockPos().offset(b[0], b[1], b[2]);
					BlockState here = server.getBlockState(at);
					if (b[1] == 0 && Math.abs(b[0]) <= old.plinth && Math.abs(b[2]) <= old.plinth && here.is(Blocks.CHISELED_STONE_BRICKS)) {
						continue; // the plinth stays
					}
					if (!here.isAir() && here.getBlock() == old.state(b[3]).getBlock()
							&& keep.get(BlockPos.asLong(b[0], b[1], b[2])) != here.getBlock()) {
						// what someone kept in it isn't lost with it: it comes back with the drones that had no dock
						if (server.getBlockEntity(at) instanceof net.minecraft.world.Container store) {
							for (int i = 0; i < store.getContainerSize(); i++) {
								if (!store.getItem(i).isEmpty()) {
									keptStores.add(store.getItem(i).copy());
								}
							}
							store.clearContent();
						}
						server.setBlock(at, Blocks.AIR.defaultBlockState(), SILENT);
						work++;
					}
				}
				if (convertIndex >= place.length) {
					convertStep++;
					convertIndex = 0;
				}
			}
			if (convertStep > lastClear) {
				layoutVersion = TowerData.CURRENT_LAYOUT;
				foundation = 0;
				tier = 0;
				converting = CONVERT_BUILDING;
				convertStep = 0;
				convertIndex = 0;
				convertKeep = null;
				if (convertTier == 0 && old.foundation == null) {
					endConversion(server);
				}
			}
			changed();
			return;
		}
		// Building: the foundation (step 0), then each tier up to the one the old tower had reached.
		TowerData data = layout();
		int budget = CONVERT_BLOCKS_PER_TICK;
		while (budget > 0 && converting == CONVERT_BUILDING) {
			TowerData.Tier step = convertStep == 0 ? data.foundation : data.tier(convertStep);
			if (step == null) {
				convertStep++;
				continue;
			}
			if (convertIndex == 0 && convertStep > 0) {
				for (int[] c : step.clear) {
					clear(server, getBlockPos().offset(c[0], c[1], c[2]));
				}
			}
			int before = convertIndex;
			convertIndex = placeSome(server, step, convertIndex, budget);
			budget -= Math.max(1, convertIndex - before);
			if (convertIndex >= step.air.length + step.place.length) {
				if (convertStep == 0) {
					foundation = 2;
				} else {
					tier = convertStep;
				}
				convertStep++;
				convertIndex = 0;
				if (convertStep > convertTier) {
					endConversion(server);
				}
			} else if (convertIndex == before) {
				break; // waiting for a position to load
			}
		}
		changed();
	}

	/**
	 * A neon line bends its tube round a corner onto a line touching it only there ({@link NeonLineBlock#NE} ...): that
	 * hangs on a block beside it diagonally, which the game never tells it about, so a line placed before that one, or
	 * shaped again by a later neighbour before it came, lost its chamfer. Once a step is up, every line of the tower so
	 * far takes its shape from what is round it (only the lines are read from the world).
	 */
	private void reshapeNeonLines(ServerLevel server, int upTo) {
		TowerData data = layout();
		java.util.List<TowerData.Tier> steps = new java.util.ArrayList<>();
		if (data.foundation != null) {
			steps.add(data.foundation);
		}
		for (int t = 1; t <= Math.min(upTo, TowerData.TIERS); t++) {
			steps.add(data.tier(t));
		}
		for (TowerData.Tier step : steps) {
			if (step == null) continue;
			for (int[] b : step.place) {
				Block laid = data.state(b[3]).getBlock();
				if (!(laid instanceof NeonLineBlock) && !(laid instanceof FacedLineBlock)) continue;
				BlockPos at = getBlockPos().offset(b[0], b[1], b[2]);
				BlockState here = server.getBlockState(at);
				BlockState shaped = here.getBlock() instanceof NeonLineBlock ? NeonLineBlock.shaped(here, server, at)
						: here.getBlock() instanceof FacedLineBlock ? FacedLineBlock.shaped(here, server, at) : here;
				if (shaped != here) {
					server.setBlock(at, shaped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
				}
			}
		}
	}

	/**
	 * The old design was cleared without telling the blocks beside it (no drops, no side effects), so a block kept from it,
	 * or one put back the same, still had the shape it took from what stood beside it then: a neon line grooved open
	 * towards a line that is gone, framed glass joined to a pane that went. Each block of the rebuilt tower takes its
	 * shape from what is round it now, as a build's own updates leave it. Only its shape: no block turns into another or
	 * into air here.
	 */
	private void reshapeAfterConversion(ServerLevel server) {
		TowerData data = layout();
		java.util.List<TowerData.Tier> steps = new java.util.ArrayList<>();
		steps.add(data.foundation);
		for (int t = 1; t <= tier; t++) {
			steps.add(data.tier(t));
		}
		int changed = 0;
		for (TowerData.Tier step : steps) {
			if (step == null) continue;
			for (int[] b : step.place) {
				BlockPos at = getBlockPos().offset(b[0], b[1], b[2]);
				BlockState here = server.getBlockState(at);
				if (here.isAir()) continue;
				BlockState shaped = Block.updateFromNeighbourShapes(here, server, at);
				if (shaped != here && shaped.getBlock() == here.getBlock()) {
					server.setBlock(at, shaped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
					changed++;
				}
			}
		}
		if (changed > 0) {
			Jugcraft.LOGGER.info("Drone Tower at {}: {} blocks took their shapes from what is round them after the conversion", getBlockPos().toShortString(), changed);
		}
	}

	/** The rebuilt tower takes over: displays pinned, the depot handed back to the new terminal, the exchanges bound. */
	private void endConversion(ServerLevel server) {
		converting = 0;
		tier = convertTier;
		building = 0;
		progress = 0;
		java.util.List<ItemStack> back = new java.util.ArrayList<>();
		if (tier >= 1 || foundation == 2) {
			foundation = 2;
			reshapeAfterConversion(server);
			pinDisplays(server);
			ownDoors(server, tier);
			DroneTerminalBlockEntity terminal = terminal();
			if (terminal != null) {
				if (terminal.owner() == null && owner != null) {
					terminal.setOwner(owner);
				}
				terminal.linkTower(getBlockPos());
				if (carried != null) {
					back.addAll(terminal.takeOver(carried));
					carried = null;
				}
				ExchangeStations.bindBuilt(server, this);
			}
		}
		if (carried != null) {
			// No depot to take it over (the old tower had not finished its first tier): everything comes back as items.
			for (var drone : carried.drones()) {
				Item item = io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.DRONES.get(drone);
				if (item != null) {
					back.add(new ItemStack(item));
				}
			}
			back.addAll(carried.items());
			carried = null;
		}
		back.addAll(keptStores);
		keptStores.clear();
		handBack(server, back);
		int strays = clearStrays(server);
		Jugcraft.LOGGER.info("Drone Tower at {}: converted to the Tri-Pillar at tier {} ({} item stacks handed back, {} stray mobs cleared)",
				getBlockPos().toShortString(), tier, back.size(), strays);
		changed();
	}

	/**
	 * The mobs that wandered or spawned into the tower before nothing could spawn in it (layout 45; Justin, 5 November
	 * 2026: "theres a lot of mobs spawning inside the tower"): gone when it is rebuilt. Only the wild ones: never a named,
	 * tamed, leashed or ridden mob, one that must stay (persistence), the crew, or anything not a creature. Nothing else
	 * clears mobs out (Justin, 6 November 2026: "I want to be able to manually spawn mobs in here ... to test out
	 * security's working ... I shouldn't like just delete any mob that walks in there"): nothing spawns in or on the tower
	 * of itself (TowerSpawnMixin), but a mob may walk in through the doors, and a spawn egg works.
	 */
	int clearStrays(ServerLevel server) {
		TowerData.Extent e = TowerData.get().extent();
		BlockPos c = getBlockPos();
		var box = new net.minecraft.world.phys.AABB(c.getX() + e.minX(), c.getY() + e.minY() - 1, c.getZ() + e.minZ(), c.getX() + e.maxX() + 1,
				c.getY() + e.maxY() + TowerRegistry.OVER_TOP + 1, c.getZ() + e.maxZ() + 1);
		var registry = TowerRegistry.get(server);
		var strays = server.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, mob -> stray(mob) && registry.inTower(server, mob.blockPosition()));
		for (var mob : strays) {
			mob.discard();
		}
		return strays.size();
	}

	/** Whether a mob is a wild stray a refit clears: a creature or monster, not the crew, not kept, named, owned, led or riding. */
	public static boolean stray(net.minecraft.world.entity.Mob mob) {
		return mob.getType().getCategory() != net.minecraft.world.entity.MobCategory.MISC && !(mob instanceof io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew)
				&& !mob.isPersistenceRequired() && !mob.hasCustomName() && !mob.isLeashed() && !mob.isVehicle() && !mob.isPassenger()
				&& !(mob instanceof net.minecraft.world.entity.OwnableEntity owned && owned.getOwnerReference() != null);
	}

	/** Items that found no place in the rebuilt tower: left on the depot terminal (or the core), never despawning. */
	private void handBack(ServerLevel server, java.util.List<ItemStack> items) {
		BlockPos at = layout().terminal != null && server.getBlockEntity(terminalPos()) instanceof DroneTerminalBlockEntity ? terminalPos().above() : getBlockPos().above();
		for (ItemStack stack : items) {
			if (stack.isEmpty()) {
				continue;
			}
			var entity = new net.minecraft.world.entity.item.ItemEntity(server, at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5, stack);
			entity.setUnlimitedLifetime();
			entity.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
			server.addFreshEntity(entity);
		}
	}

	private float convertProgress() {
		if (converting == CONVERT_CLEARING) {
			if (convertStep == 0 || convertClearTo == 0 && TowerData.get(convertFrom).foundation == null) {
				return 0;
			}
			TowerData old = TowerData.get(convertFrom);
			int steps = convertClearTo + (old.foundation != null ? 1 : 0);
			int[][] place = convertStep > convertClearTo ? old.foundation.place : old.tier(Math.min(convertStep, convertClearTo)).place;
			return 0.3f * Math.min(1f, (convertStep - 1 + (float) convertIndex / Math.max(1, place.length)) / Math.max(1, steps));
		}
		TowerData data = layout();
		TowerData.Tier step = convertStep == 0 ? data.foundation : data.tier(Math.min(TowerData.TIERS, Math.max(1, convertStep)));
		float within = step == null ? 0 : (float) convertIndex / Math.max(1, step.air.length + step.place.length);
		return 0.3f + 0.7f * Math.min(1f, (convertStep + within) / (convertTier + 1));
	}

	private static void writeHandover(ValueOutput out, DroneTerminalBlockEntity.Handover h) {
		if (h.owner() != null) {
			out.store("owner", UUIDUtil.CODEC, h.owner());
		}
		out.store("depot_id", UUIDUtil.CODEC, h.depotId());
		out.putString("mode", h.mode().id());
		out.putLong("energy", h.energy());
		out.putBoolean("test_power", h.testPower());
		out.putInt("standalone_max_tier", h.standaloneMaxTier());
		out.putIntArray("drones", h.drones().stream().mapToInt(io.github.jimbozoomer.jugcraft.drone.DroneTier::number).toArray());
		var items = out.list("items", ItemStack.CODEC);
		for (ItemStack stack : h.items()) {
			if (!stack.isEmpty()) {
				items.add(stack);
			}
		}
	}

	private static DroneTerminalBlockEntity.Handover readHandover(ValueInput in) {
		java.util.List<io.github.jimbozoomer.jugcraft.drone.DroneTier> drones = new java.util.ArrayList<>();
		for (int n : in.getIntArray("drones").orElse(new int[0])) {
			io.github.jimbozoomer.jugcraft.drone.DroneTier.byNumber(n).ifPresent(drones::add);
		}
		java.util.List<ItemStack> items = new java.util.ArrayList<>();
		for (ItemStack stack : in.listOrEmpty("items", ItemStack.CODEC)) {
			items.add(stack);
		}
		return new DroneTerminalBlockEntity.Handover(in.read("owner", UUIDUtil.CODEC).orElse(null),
				in.read("depot_id", UUIDUtil.CODEC).orElseGet(UUID::randomUUID),
				"party".equals(in.getStringOr("mode", "personal")) ? UseMode.PARTY : UseMode.PERSONAL, in.getLongOr("energy", 0L),
				in.getBooleanOr("test_power", false), in.getIntOr("standalone_max_tier", 0), drones, items);
	}

	private void finish(ServerLevel server) {
		if (building == 1) {
			pinDisplays(server);
		}
		ownDoors(server, building);
		tier = building;
		building = 0;
		reshapeNeonLines(server, tier);
		if (jobs != null) {
			BuildJobs.unregister(jobs);
			jobs = null;
		}
		DroneTerminalBlockEntity terminal = terminal();
		if (terminal != null) {
			if (terminal.owner() == null && owner != null) {
				terminal.setOwner(owner);
			}
			terminal.linkTower(getBlockPos());
			if (layoutVersion >= TowerData.FIRST_INDUSTRIAL_LAYOUT) {
				ExchangeStations.bindBuilt(server, this);
			}
		}
		changed();
	}

	/**
	 * Ties the command post's plotting table and video wall to this tower's own terminal, so with several towers
	 * about each hologram shows its own tower.
	 */
	private void pinDisplays(ServerLevel server) {
		TowerData data = layout();
		BlockPos terminal = getBlockPos().offset(data.terminal[0], data.terminal[1], data.terminal[2]);
		for (TowerData.Tier step : data.foundation != null ? List.of(data.foundation, data.tier(1)) : List.of(data.tier(1))) {
			for (int[] b : step.place) {
				BlockPos at = getBlockPos().offset(b[0], b[1], b[2]);
				if (server.isLoaded(at) && server.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display) {
					display.pin(terminal);
				}
			}
		}
	}

	/**
	 * The badge readers of the tower's Maintenance Doors (work list item 10) let in its owner and their party: each door
	 * of the foundation and of the tiers up to {@code upTo} with no owner yet takes the tower's.
	 */
	private void ownDoors(ServerLevel server, int upTo) {
		if (owner == null) {
			return;
		}
		TowerData data = layout();
		List<TowerData.Tier> steps = new java.util.ArrayList<>();
		if (data.foundation != null) {
			steps.add(data.foundation);
		}
		for (int t = 1; t <= Math.min(upTo, TowerData.TIERS); t++) {
			steps.add(data.tier(t));
		}
		for (TowerData.Tier step : steps) {
			for (int[] b : step.place) {
				if (!(data.state(b[3]).getBlock() instanceof MaintenanceDoorBlock)) {
					continue;
				}
				BlockPos at = getBlockPos().offset(b[0], b[1], b[2]);
				if (server.isLoaded(at) && server.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlockEntity door
						&& door.owner() == null) {
					door.setOwner(owner);
					door.setChanged();
				}
			}
		}
	}

	/** Builds every tier up to {@code target} at once (game tests and the dev command); the foundation first. */
	public void buildInstantly(ServerLevel server, int target) {
		// every chunk the tower stands in, loaded (or generated) first: a block in a chunk that isn't would be skipped
		int reach = footprintRadius(server, getBlockPos());
		for (int cx = (getBlockPos().getX() - reach) >> 4; cx <= (getBlockPos().getX() + reach) >> 4; cx++) {
			for (int cz = (getBlockPos().getZ() - reach) >> 4; cz <= (getBlockPos().getZ() + reach) >> 4; cz++) {
				server.getChunk(cx, cz);
			}
		}
		// target 0 builds a design's foundation alone
		if (target >= 0 && needsFoundation() && converting == 0) {
			TowerData.Tier f = layout().foundation;
			for (int[] c : f.air) {
				clear(server, getBlockPos().offset(c[0], c[1], c[2]));
			}
			for (int[] b : f.place) {
				place(server, getBlockPos().offset(b[0], b[1], b[2]), layout().state(b[3]));
			}
			finishFoundation(server);
		}
		for (int t = tier + 1; t <= Math.min(TowerData.TIERS, target); t++) {
			TowerData.Tier data = layout().tier(t);
			for (int[] c : data.clear) {
				server.setBlock(getBlockPos().offset(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int[] c : data.air) {
				clear(server, getBlockPos().offset(c[0], c[1], c[2]));
			}
			for (int[] b : data.place) {
				place(server, getBlockPos().offset(b[0], b[1], b[2]), layout().state(b[3]));
			}
			building = t;
			finish(server);
		}
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** A broken core keeps its store in its item (see the loot table), so tens of thousands of blocks aren't spilled. */
	@Override
	protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!store.isEmpty()) {
			components.set(JugcraftTower.TOWER_STORE, Map.copyOf(store));
		}
	}

	@Override
	protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
		super.applyImplicitComponents(components);
		Map<Item, Integer> carried = components.get(JugcraftTower.TOWER_STORE);
		if (carried != null) {
			store.clear();
			carried.forEach((item, n) -> {
				if (n > 0) {
					store.put(item, n);
				}
			});
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (jobs != null) {
			BuildJobs.unregister(jobs);
			jobs = null;
		}
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, registries);
		saveAdditional(output);
		output.putFloat("progress_shown", buildProgress());
		return output.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		int savedLayout = input.getIntOr("layout_version", TowerData.LEGACY_LAYOUT);
		layoutVersion = TowerData.supportedLayout(savedLayout)
				? savedLayout : TowerData.LEGACY_LAYOUT;
		// A Tri-Pillar saved before the foundation was its own step built it with tier 1.
		foundation = Math.max(0, Math.min(2, input.getIntOr("foundation", input.getIntOr("tier", 0) >= 1 ? 2 : 0)));
		converting = input.getIntOr("converting", 0);
		convertFrom = input.getIntOr("convert_from", 0);
		convertTier = input.getIntOr("convert_tier", 0);
		convertStep = input.getIntOr("convert_step", 0);
		convertIndex = input.getIntOr("convert_index", 0);
		convertClearTo = input.getIntOr("convert_clear_to", 0);
		carried = input.child("carried").map(TowerCoreBlockEntity::readHandover).orElse(null);
		keptStores.clear();
		for (ItemStack stack : input.listOrEmpty("kept_stores", ItemStack.CODEC)) {
			keptStores.add(stack);
		}
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		tier = Math.max(0, Math.min(TowerData.TIERS, input.getIntOr("tier", 0)));
		firstLayerQueued = input.getBooleanOr("first_layer_queued", false);
		building = Math.max(0, Math.min(TowerData.TIERS, input.getIntOr("building", 0)));
		progress = input.getIntOr("progress", 0);
		if (layoutVersion >= TowerData.TRIPILLAR_LAYOUT && input.getIntOr("foundation", -1) < 0 && building == 1 && tier == 0) {
			// Saved while the first Tri-Pillar build placed the building and pads as one tier 1: that is now the foundation
			// (started again from the top; what is in already costs only a check).
			foundation = 1;
			building = 0;
			progress = 0;
		}
		filled = new BitSet();
		for (int index : input.getIntArray("filled").orElse(new int[0])) {
			filled.set(index);
		}
		// Modules (the old upgrade currency) are gone: a core saved with some simply no longer has them.
		store.clear();
		input.read("store", STORE_CODEC).ifPresent(saved -> saved.forEach((item, n) -> {
			if (n > 0) {
				store.put(item, n);
			}
		}));
		clientProgress = input.getFloatOr("progress_shown", 0f);
		lightsNight = input.getBooleanOr("lights_night", false);
		lightsCursor = -1;
		if (jobs != null) {
			jobs.reset(building, filled);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("layout_version", layoutVersion);
		output.putInt("foundation", foundation);
		if (converting != 0) {
			output.putInt("converting", converting);
			output.putInt("convert_from", convertFrom);
			output.putInt("convert_tier", convertTier);
			output.putInt("convert_step", convertStep);
			output.putInt("convert_index", convertIndex);
			output.putInt("convert_clear_to", convertClearTo);
		}
		if (carried != null) {
			writeHandover(output.child("carried"), carried);
		}
		if (!keptStores.isEmpty()) {
			var kept = output.list("kept_stores", ItemStack.CODEC);
			for (ItemStack stack : keptStores) {
				kept.add(stack);
			}
		}
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putInt("tier", tier);
		output.putInt("building", building);
		if (firstLayerQueued) {
			output.putBoolean("first_layer_queued", true);
		}
		output.putInt("progress", progress);
		output.putIntArray("filled", filled.stream().toArray());
		if (lightsNight) {
			output.putBoolean("lights_night", true);     // (saved mid-switch, the switch runs again from the start: it costs a check)
		}
		if (!store.isEmpty()) {
			output.store("store", STORE_CODEC, new java.util.LinkedHashMap<>(store));
		}
	}

	/** Pipes, hoppers and extractors put the tower's blocks in (only what it still needs); nothing comes out. */
	private final class StoreInlet extends SnapshotParticipant<Map<Item, Integer>> implements InsertionOnlyStorage<ItemVariant> {
		/** The tower storage's inlet (the Cargo Exchange, after the first layer) rather than the core's own (the first layer). */
		private final boolean storage;

		StoreInlet(boolean storage) {
			this.storage = storage;
		}

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);
			if (resource.hasComponents()) {
				return 0;
			}
			long take = Math.min(maxAmount, storage ? storageRoom(resource.getItem()) : room(resource.getItem()));
			if (take <= 0) {
				return 0;
			}
			updateSnapshots(transaction);
			store.merge(resource.getItem(), (int) take, Integer::sum);
			return take;
		}

		@Override
		protected Map<Item, Integer> createSnapshot() {
			return new java.util.LinkedHashMap<>(store);
		}

		@Override
		protected void readSnapshot(Map<Item, Integer> snapshot) {
			store.clear();
			store.putAll(snapshot);
		}

		@Override
		protected void onFinalCommit() {
			changed();
		}
	}

	static boolean isTowerBlock(BlockState state) {
		return state.is(JugcraftDrones.TERMINAL) || JugcraftTower.BLOCKS.containsValue(state.getBlock());
	}
}
