package io.github.jimbozoomer.jugcraft.blueprint;

import io.github.jimbozoomer.jugcraft.drone.BuildJobs;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A placed blueprint: which one, how it is turned, who placed it and whether it is Personal or Party. It
 * offers its missing blocks to drone depots as build jobs ({@link BuildJobs.Source}), bottom-up, and pops off
 * (returning the blueprint) once every block is in place. Hand-placed blocks count the same as drone ones.
 * Blocks that need an owner (the Drone Tower Core, the depot terminal) are placed by hand only.
 */
public class SurveyStakeBlockEntity extends BlockEntity {
	private static final int CHECK_INTERVAL = 40;

	private String blueprintId = "";
	private Rotation rotation = Rotation.NONE;
	private @Nullable UUID owner;
	private UseMode mode = UseMode.PERSONAL;
	private final UUID jobId = UUID.randomUUID();
	private @Nullable Jobs jobs;
	private @Nullable Map<BlockPos, BlockState> wanted;

	public SurveyStakeBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftBlueprints.STAKE_ENTITY, pos, state);
	}

	public void setup(String blueprintId, Rotation rotation, UUID owner) {
		this.blueprintId = blueprintId;
		this.rotation = rotation;
		this.owner = owner;
		this.wanted = null;
		setChanged();
		sync();
	}

	public @Nullable Blueprint blueprint() {
		return Blueprint.get(blueprintId, level != null && level.isClientSide());
	}

	/** Blocks that need an owner and are placed by hand only (drones never place them). */
	public static boolean handOnly(BlockState state) {
		return state.is(io.github.jimbozoomer.jugcraft.tower.JugcraftTower.CORE) || state.is(io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.TERMINAL);
	}

	/** Turns the blueprint a quarter turn clockwise about the stake. */
	public void rotate() {
		rotation = rotation.getRotated(Rotation.CLOCKWISE_90);
		wanted = null;
		setChanged();
		sync();
	}

	/** Pulls the stake up and gives the blueprint back. */
	public void removeAndReturn(Player player) {
		if (level == null) {
			return;
		}
		ItemStack stack = BlueprintItem.stack(blueprintId, blueprint() == null ? blueprintId : blueprint().name);
		if (!player.getInventory().add(stack)) {
			net.minecraft.world.Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), stack);
		}
		blueprintId = "";
		level.removeBlock(getBlockPos(), false);
	}

	/** Progress for the stake screen. */
	public io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork.StakeInfoPayload info(Player player, boolean open) {
		Blueprint blueprint = blueprint();
		int done = 0;
		int wrong = 0;
		Map<Item, Integer> missing = new LinkedHashMap<>();
		Map<Item, Integer> hand = new LinkedHashMap<>();
		if (level != null) {
			for (Map.Entry<BlockPos, BlockState> entry : wanted().entrySet()) {
				BlockState now = level.getBlockState(entry.getKey());
				if (matches(now, entry.getValue())) {
					done++;
				} else if (!now.canBeReplaced()) {
					wrong++;
				} else {
					(handOnly(entry.getValue()) ? hand : missing).merge(entry.getValue().getBlock().asItem(), 1, Integer::sum);
				}
			}
		}
		List<String> items = new ArrayList<>();
		List<Integer> counts = new ArrayList<>();
		missing.entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()).limit(16).forEach(e -> {
			items.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getKey()).toString());
			counts.add(e.getValue());
		});
		List<String> handOnly = new ArrayList<>();
		hand.keySet().forEach(item -> handOnly.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()));
		return new io.github.jimbozoomer.jugcraft.blueprint.BlueprintNetwork.StakeInfoPayload(getBlockPos(),
				blueprint == null ? "?" : blueprint.name, done, wanted().size(), mode.id(), items, counts, wrong, handOnly, mayChange(player), open);
	}

	public String blueprintId() {
		return blueprintId;
	}

	public Rotation rotation() {
		return rotation;
	}

	public UseMode mode() {
		return mode;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	/** True when {@code world} counts as the wanted block (same block; states that change on their own are ignored). */
	public static boolean matches(BlockState world, BlockState wanted) {
		return world.is(wanted.getBlock());
	}

	/** Every block of the blueprint, absolute, in order (bottom layer first). */
	public Map<BlockPos, BlockState> wanted() {
		if (wanted == null) {
			wanted = new LinkedHashMap<>();
			Blueprint blueprint = blueprint();
			if (blueprint != null) {
				for (Blueprint.Cell cell : blueprint.cells(rotation)) {
					wanted.put(getBlockPos().offset(cell.offset()), cell.state());
				}
			}
		}
		return wanted;
	}

	public boolean mayChange(Player player) {
		return owner == null || owner.equals(player.getUUID())
				|| (mode == UseMode.PARTY && JugcraftParties.sameParty(owner, player.getUUID()) && JugcraftParties.isLeader(player.getUUID()));
	}

	public void toggleMode(Player player) {
		mode = mode == UseMode.PERSONAL ? UseMode.PARTY : UseMode.PERSONAL;
		setChanged();
		sync();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.blueprint.mode", mode.id().toUpperCase(java.util.Locale.ROOT)));
	}

	/** Progress in chat: blocks in place, what is still needed (most first), blocks in the way. */
	public void report(Player player) {
		Blueprint blueprint = blueprint();
		if (blueprint == null || level == null) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.unknown"));
			return;
		}
		int done = 0;
		int wrong = 0;
		Map<Item, Integer> missing = new LinkedHashMap<>();
		Map<Item, Integer> handOnly = new LinkedHashMap<>();
		for (Map.Entry<BlockPos, BlockState> entry : wanted().entrySet()) {
			BlockState now = level.getBlockState(entry.getKey());
			if (matches(now, entry.getValue())) {
				done++;
			} else if (!now.canBeReplaced()) {
				wrong++;
			} else {
				(handOnly(entry.getValue()) ? handOnly : missing).merge(entry.getValue().getBlock().asItem(), 1, Integer::sum);
			}
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.progress", blueprint.name, done, wanted().size(),
				mode.id().toUpperCase(java.util.Locale.ROOT)));
		if (!missing.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.missing", list(missing)));
		}
		if (!handOnly.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.hand_only", list(handOnly)));
		}
		if (wrong > 0) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.wrong", wrong));
		}
	}

	private static String list(Map<Item, Integer> counts) {
		List<String> parts = new ArrayList<>();
		counts.entrySet().stream().sorted(Map.Entry.<Item, Integer>comparingByValue().reversed()).limit(6)
				.forEach(e -> parts.add(e.getValue() + " " + new net.minecraft.world.item.ItemStack(e.getKey()).getHoverName().getString()));
		if (counts.size() > 6) {
			parts.add("...");
		}
		return String.join(", ", parts);
	}

	public void serverTick(ServerLevel server) {
		if (jobs == null) {
			jobs = new Jobs();
			BuildJobs.register(jobs);
		}
		if ((server.getGameTime() + getBlockPos().hashCode()) % CHECK_INTERVAL != 0 || wanted().isEmpty()) {
			return;
		}
		for (Map.Entry<BlockPos, BlockState> entry : wanted().entrySet()) {
			if (!server.isLoaded(entry.getKey()) || !matches(server.getBlockState(entry.getKey()), entry.getValue())) {
				return;
			}
		}
		complete(server);
	}

	private void complete(ServerLevel server) {
		Blueprint blueprint = blueprint();
		BlockPos pos = getBlockPos();
		if (owner != null && server.getServer().getPlayerList().getPlayer(owner) instanceof Player player && blueprint != null) {
			player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.complete", blueprint.name));
		}
		Block.popResource(server, pos, BlueprintItem.stack(blueprintId, blueprint == null ? blueprintId : blueprint.name));
		server.removeBlock(pos, false);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (jobs != null) {
			BuildJobs.unregister(jobs);
			jobs = null;
		}
	}

	private void sync() {
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
		return output.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		blueprintId = input.getStringOr("blueprint", "");
		rotation = Rotation.values()[Math.floorMod(input.getIntOr("rotation", 0), 4)];
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		mode = "party".equals(input.getStringOr("mode", "personal")) ? UseMode.PARTY : UseMode.PERSONAL;
		wanted = null;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("blueprint", blueprintId);
		output.putInt("rotation", rotation.ordinal());
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("mode", mode == UseMode.PARTY ? "party" : "personal");
	}

	/** The stake's build jobs for drone depots: missing blocks, bottom-up, one depot per position at a time. */
	private final class Jobs implements BuildJobs.Source {
		private final Map<BlockPos, UUID> reservedBy = new HashMap<>();
		private final Map<BlockPos, Long> reservedUntil = new HashMap<>();

		private boolean open(ServerLevel level, BlockPos pos, BlockState state) {
			if (!level.isLoaded(pos)) {
				return false;
			}
			BlockState now = level.getBlockState(pos);
			return !matches(now, state) && (now.canBeReplaced() || obstacle(level, pos, now));
		}

		/**
		 * Something in the way that the drone clears before placing (terrain, a tree, a stray block): anything
		 * breakable that isn't a block entity. A chest or machine in the footprint is never touched; that cell
		 * waits for the player.
		 */
		private boolean obstacle(ServerLevel level, BlockPos pos, BlockState now) {
			return !now.isAir() && level.getBlockEntity(pos) == null && now.getDestroySpeed(level, pos) >= 0;
		}

		/** Is the block under {@code pos} ready to build on: done, solid ground, or a cell drones never fill? */
		private boolean supported(ServerLevel level, BlockPos pos, Map<BlockPos, BlockState> all) {
			BlockPos under = pos.below();
			BlockState wantedBelow = all.get(under);
			if (wantedBelow == null || handOnly(wantedBelow)) {
				return true;
			}
			BlockState now = level.getBlockState(under);
			return matches(now, wantedBelow) || level.getBlockEntity(under) != null;
		}

		@Override
		public List<BuildJobs.Target> openTargets(ServerLevel level, BlockPos center, int radius, UUID depotOwner, UseMode depotMode, int max) {
			if (owner == null || isRemoved() || level != SurveyStakeBlockEntity.this.level
					|| !JugcraftParties.mayServe(depotOwner, depotMode, owner, mode)) {
				return List.of();
			}
			long now = level.getGameTime();
			Map<BlockPos, BlockState> all = wanted();
			// Layer by layer: drones come down from above, so nothing goes in until every lower block that drones
			// can place is done (a roof placed early would cover the pews under it).
			int lowest = Integer.MAX_VALUE;
			for (Map.Entry<BlockPos, BlockState> e : all.entrySet()) {
				if (e.getKey().getY() < lowest && !handOnly(e.getValue()) && open(level, e.getKey(), e.getValue())) {
					lowest = e.getKey().getY();
				}
			}
			int layer = lowest;
			List<BuildJobs.Target> out = new ArrayList<>();
			all.entrySet().stream()
					.filter(e -> Math.abs(e.getKey().getX() - center.getX()) <= radius && Math.abs(e.getKey().getZ() - center.getZ()) <= radius)
					.filter(e -> e.getKey().getY() == layer)
					.filter(e -> !handOnly(e.getValue()))
					.filter(e -> reservedUntil.getOrDefault(e.getKey(), Long.MIN_VALUE) < now)
					.filter(e -> open(level, e.getKey(), e.getValue()))
					// Bottom-up: never a block above one of this blueprint that is still missing.
					.filter(e -> supported(level, e.getKey(), all))
					.sorted(Comparator.comparingInt((Map.Entry<BlockPos, BlockState> e) -> e.getKey().getY())
							.thenComparingDouble(e -> e.getKey().distSqr(center)))
					.limit(max)
					.forEach(e -> out.add(new BuildJobs.Target(this, jobId, owner, mode, e.getKey(), e.getValue(), null)));
			return out;
		}

		@Override
		public boolean reserve(ServerLevel level, BuildJobs.Target target, UUID depot, long untilTick) {
			UUID holder = reservedBy.get(target.pos());
			if (holder != null && !holder.equals(depot) && reservedUntil.getOrDefault(target.pos(), Long.MIN_VALUE) >= level.getGameTime()) {
				return false;
			}
			reservedBy.put(target.pos(), depot);
			reservedUntil.put(target.pos(), untilTick);
			return true;
		}

		@Override
		public void release(ServerLevel level, BuildJobs.Target target, UUID depot) {
			if (depot.equals(reservedBy.get(target.pos()))) {
				reservedBy.remove(target.pos());
				reservedUntil.remove(target.pos());
			}
		}

		@Override
		public boolean stillWanted(ServerLevel level, BuildJobs.Target target, UUID depotOwner, UseMode depotMode) {
			BlockState state = wanted().get(target.pos());
			return state != null && !isRemoved() && owner != null && JugcraftParties.mayServe(depotOwner, depotMode, owner, mode)
					&& open(level, target.pos(), state);
		}

		@Override
		public boolean fill(ServerLevel level, BuildJobs.Target target, BlockState state) {
			BlockState now = level.getBlockState(target.pos());
			if (!now.canBeReplaced() && !obstacle(level, target.pos(), now)) {
				return false;
			}
			// Clears whatever is in the way (no drops: site clearance), then puts the block in.
			level.setBlockAndUpdate(target.pos(), state);
			return true;
		}
	}
}
