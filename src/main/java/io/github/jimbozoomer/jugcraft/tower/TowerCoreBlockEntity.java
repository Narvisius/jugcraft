package io.github.jimbozoomer.jugcraft.tower;

import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.BitSet;
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
import net.minecraft.world.Containers;
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
 * The Drone Tower Core: the block the tower grows from. It sits in the middle of a 15x15 plinth of chiseled
 * stone bricks. It holds the tower modules players (or pipes and hoppers) put in, and its tier.
 *
 * <p>UPGRADE DRONE TOWER (from the status screen) pays the next tier's modules and starts building it.
 * Tier 1, the Command Post with its command room, landing field and depot terminal, the core builds itself,
 * {@link #CORE_BLOCKS_PER_TICK} blocks a tick. Every later tier is built by the depot's drones: the core
 * offers its blocks as build jobs ({@link TowerBuildJobs}), one 3x3 tile per drone stop, bottom up. When the
 * last tile is in, the tower reaches that tier: more hangars, more pickups, a bigger fleet and the next drone
 * tier unlocked.
 */
public class TowerCoreBlockEntity extends BlockEntity {
	public static final int CORE_BLOCKS_PER_TICK = 16;
	/** Most of each module the core holds. */
	public static final int MODULE_CAPACITY = 1024;
	/** How close (squared blocks) a player must be to press UPGRADE. */
	public static final double REACH_SQR = 10 * 10;
	/**
	 * How far a tower's drones build, in chunks each way from the core's chunk (from tier 1 on). Another tower of
	 * the same player must stand outside it.
	 */
	public static final int BUILD_RADIUS_CHUNKS = 50;
	/** The tower itself (its 39x39 field, exchanges and pads) lies within this many blocks of the core. */
	public static final int FOOTPRINT = 24;
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
		net.minecraft.world.level.ChunkPos min = net.minecraft.world.level.ChunkPos.containing(core.offset(-FOOTPRINT, 0, -FOOTPRINT));
		net.minecraft.world.level.ChunkPos max = net.minecraft.world.level.ChunkPos.containing(core.offset(FOOTPRINT, 0, FOOTPRINT));
		for (int x = min.x(); x <= max.x(); x++) {
			for (int z = min.z(); z <= max.z(); z++) {
				var chunk = new net.minecraft.world.level.ChunkPos(x, z);
				if (loaded) {
					level.getChunkSource().addTicketWithRadius(CHUNK_TICKET, chunk, 2);
				} else if (!TowerRegistry.get(level).needsChunk(chunk, core)) {
					level.getChunkSource().removeTicketWithRadius(CHUNK_TICKET, chunk, 2);
				}
			}
		}
	}

	/** Set once the chunks are forced after loading (cores placed before chunk loading existed get it too). */
	private boolean chunksForced;

	private @Nullable UUID owner;
	private int tier;
	/** The tier being built (0: none). */
	private int building;
	/** Tier 1: index of the next block the core places. */
	private int progress;
	/** Tiers 2-9: tiles already placed. */
	private BitSet filled = new BitSet();
	private final int[] modules = new int[JugcraftTower.MODULES.length];
	private @Nullable TowerBuildJobs jobs;
	/** Client side: the share of the current build that is done (0 to 1). */
	private float clientProgress;

	private final ModuleInlet inlet = new ModuleInlet();

	public TowerCoreBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftTower.CORE_ENTITY, pos, state);
	}

	public int tier() {
		return tier;
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

	public int modules(int index) {
		return modules[index];
	}

	/** How far the current build is, 0 to 1. */
	public float buildProgress() {
		if (level != null && level.isClientSide()) {
			return clientProgress;
		}
		if (building == 1) {
			return (float) progress / Math.max(1, TowerData.get().tier(1).place.length + TowerData.get().tier(1).air.length);
		}
		if (building >= 2) {
			return (float) filled.cardinality() / Math.max(1, TowerData.get().tier(building).tiles.size());
		}
		return 0;
	}

	/** Where tier 1 puts the depot terminal. */
	public BlockPos terminalPos() {
		int[] t = TowerData.get().terminal;
		return getBlockPos().offset(t[0], t[1], t[2]);
	}

	public @Nullable DroneTerminalBlockEntity terminal() {
		return level != null && level.getBlockEntity(terminalPos()) instanceof DroneTerminalBlockEntity terminal ? terminal : null;
	}

	/** The core's module store (pipes, hoppers and extractors can fill it). */
	public InsertionOnlyStorage<ItemVariant> moduleStorage() {
		return inlet;
	}

	private static int moduleIndex(Item item) {
		for (int i = 0; i < JugcraftTower.MODULES.length; i++) {
			if (JugcraftTower.MODULE_ITEMS.get(JugcraftTower.MODULES[i]) == item) {
				return i;
			}
		}
		return -1;
	}

	/** A player hands over the modules in their hand. Returns true if any were taken. */
	public boolean deposit(ItemStack stack) {
		int index = moduleIndex(stack.getItem());
		if (index < 0) {
			return false;
		}
		int take = Math.min(stack.getCount(), MODULE_CAPACITY - modules[index]);
		if (take <= 0) {
			return false;
		}
		modules[index] += take;
		stack.shrink(take);
		changed();
		return true;
	}

	/** True when the 15x15 plinth of chiseled stone bricks round the core is complete. */
	public boolean plinthComplete() {
		if (level == null) {
			return false;
		}
		int r = TowerData.get().plinth;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if ((dx != 0 || dz != 0) && !level.getBlockState(getBlockPos().offset(dx, 0, dz)).is(Blocks.CHISELED_STONE_BRICKS)) {
					return false;
				}
			}
		}
		return true;
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

	/** UPGRADE DRONE TOWER: pays the next tier's modules and starts building it. Messages the player either way. */
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
		}
		if (building != 0) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.busy"));
			return;
		}
		if (tier >= TowerData.TIERS) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.max"));
			return;
		}
		if (tier == 0 && !plinthComplete()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.no_plinth"));
			return;
		}
		if (tier >= 1 && terminal() == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.no_terminal"));
			return;
		}
		TowerData.Tier next = TowerData.get().tier(tier + 1);
		for (Map.Entry<String, Integer> cost : next.modules.entrySet()) {
			int index = java.util.Arrays.asList(JugcraftTower.MODULES).indexOf(cost.getKey());
			if (index < 0 || modules[index] < cost.getValue()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.missing", cost.getValue() - (index < 0 ? 0 : modules[index]),
						Component.translatable("item.jugcraft." + cost.getKey())));
				return;
			}
		}
		for (Map.Entry<String, Integer> cost : next.modules.entrySet()) {
			modules[java.util.Arrays.asList(JugcraftTower.MODULES).indexOf(cost.getKey())] -= cost.getValue();
		}
		startBuilding(server, tier + 1);
		player.sendOverlayMessage(Component.translatable("message.jugcraft.tower.started", tier + 1,
				Component.translatable("tower.jugcraft.tier." + (tier + 1))));
	}

	private void startBuilding(ServerLevel server, int target) {
		building = target;
		progress = 0;
		filled = new BitSet();
		TowerData.Tier data = TowerData.get().tier(target);
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
		if (!chunksForced) {
			chunksForced = true;
			keepLoaded(server, getBlockPos(), true);
		}
		if (building == 1) {
			TowerData.Tier data = TowerData.get().tier(1);
			// Progress counts the cells to clear first, then the blocks to place.
			int total = data.air.length + data.place.length;
			int end = Math.min(total, progress + CORE_BLOCKS_PER_TICK);
			for (; progress < end; progress++) {
				if (progress < data.air.length) {
					int[] c = data.air[progress];
					clear(server, getBlockPos().offset(c[0], c[1], c[2]));
					continue;
				}
				int[] b = data.place[progress - data.air.length];
				place(server, getBlockPos().offset(b[0], b[1], b[2]), TowerData.get().state(b[3]));
			}
			if (progress >= total) {
				finish(server);
			}
			changed();
		} else if (building >= 2) {
			if (jobs == null) {
				registerJobs();
			}
			if (filled.cardinality() >= TowerData.get().tier(building).tiles.size()) {
				finish(server);
			}
		}
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
		if (!server.isLoaded(at) || server.getBlockState(at) == state) {
			return;
		}
		BlockEntity existing = server.getBlockEntity(at);
		if (existing != null && !(existing instanceof DroneTerminalBlockEntity) && server.getBlockState(at).getBlock() != state.getBlock()) {
			return;
		}
		server.setBlock(at, state, Block.UPDATE_ALL);
	}

	private void finish(ServerLevel server) {
		if (building == 1) {
			pinDisplays(server);
		}
		tier = building;
		building = 0;
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
		}
		changed();
	}

	/**
	 * Ties the command post's plotting table and video wall to this tower's own terminal, so with several towers
	 * about each hologram shows its own tower.
	 */
	private void pinDisplays(ServerLevel server) {
		TowerData data = TowerData.get();
		BlockPos terminal = getBlockPos().offset(data.terminal[0], data.terminal[1], data.terminal[2]);
		for (int[] b : data.tier(1).place) {
			BlockPos at = getBlockPos().offset(b[0], b[1], b[2]);
			if (server.isLoaded(at) && server.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display) {
				display.pin(terminal);
			}
		}
	}

	/** Builds every tier up to {@code target} at once (game tests and the dev command). */
	public void buildInstantly(ServerLevel server, int target) {
		for (int t = tier + 1; t <= Math.min(TowerData.TIERS, target); t++) {
			TowerData.Tier data = TowerData.get().tier(t);
			for (int[] c : data.clear) {
				server.setBlock(getBlockPos().offset(c[0], c[1], c[2]), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int[] c : data.air) {
				clear(server, getBlockPos().offset(c[0], c[1], c[2]));
			}
			for (int[] b : data.place) {
				place(server, getBlockPos().offset(b[0], b[1], b[2]), TowerData.get().state(b[3]));
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

	public void dropContents(Level level, BlockPos pos) {
		for (int i = 0; i < modules.length; i++) {
			int count = modules[i];
			Item item = JugcraftTower.MODULE_ITEMS.get(JugcraftTower.MODULES[i]);
			while (count > 0) {
				int n = Math.min(count, 64);
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, new ItemStack(item, n));
				count -= n;
			}
			modules[i] = 0;
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
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		tier = Math.max(0, Math.min(TowerData.TIERS, input.getIntOr("tier", 0)));
		building = Math.max(0, Math.min(TowerData.TIERS, input.getIntOr("building", 0)));
		progress = input.getIntOr("progress", 0);
		filled = new BitSet();
		for (int index : input.getIntArray("filled").orElse(new int[0])) {
			filled.set(index);
		}
		int[] saved = input.getIntArray("modules").orElse(new int[0]);
		for (int i = 0; i < modules.length; i++) {
			modules[i] = i < saved.length ? saved[i] : 0;
		}
		clientProgress = input.getFloatOr("progress_shown", 0f);
		if (jobs != null) {
			jobs.reset(building, filled);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putInt("tier", tier);
		output.putInt("building", building);
		output.putInt("progress", progress);
		output.putIntArray("filled", filled.stream().toArray());
		output.putIntArray("modules", modules.clone());
	}

	/** Pipes, hoppers and extractors put modules in; nothing comes out. */
	private final class ModuleInlet extends SnapshotParticipant<int[]> implements InsertionOnlyStorage<ItemVariant> {
		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);
			int index = moduleIndex(resource.getItem());
			if (index < 0 || resource.hasComponents()) {
				return 0;
			}
			long take = Math.min(maxAmount, MODULE_CAPACITY - modules[index]);
			if (take <= 0) {
				return 0;
			}
			updateSnapshots(transaction);
			modules[index] += (int) take;
			return take;
		}

		@Override
		protected int[] createSnapshot() {
			return modules.clone();
		}

		@Override
		protected void readSnapshot(int[] snapshot) {
			System.arraycopy(snapshot, 0, modules, 0, modules.length);
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
