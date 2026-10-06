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
	/** How many layers above the lowest unfinished one drones may work on at once (each column still bottom-up). */
	public static final int LAYER_WINDOW = 3;

	private String blueprintId = "";
	private Rotation rotation = Rotation.NONE;
	private @Nullable UUID owner;
	private UseMode mode = UseMode.PERSONAL;
	private final UUID jobId = UUID.randomUUID();
	private @Nullable Jobs jobs;
	private @Nullable BuildSite site;
	/** Large builds: the lowest layer that may still have work (every layer under it is done). */
	private int cursor;
	/** Large builds: ticks between full checks of which blocks are in place. */
	private static final int VERIFY_INTERVAL = 600;

	public SurveyStakeBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftBlueprints.STAKE_ENTITY, pos, state);
	}

	public void setup(String blueprintId, Rotation rotation, UUID owner) {
		this.blueprintId = blueprintId;
		this.rotation = rotation;
		this.owner = owner;
		this.site = null;
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
		site = null;
		setChanged();
		sync();
	}

	/** Pulls the stake up; the blueprint is not given back (it was used up when placed). */
	public void remove(Player player) {
		if (level == null) {
			return;
		}
		// The blueprint was used up when it was placed: taking the stake down gives nothing back.
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
		BuildSite site = site();
		if (level != null && site != null) {
			for (int i = 0; i < site.size(); i++) {
				BlockState now = level.getBlockState(site.blockPos(i));
				if (site.matches(now, i)) {
					done++;
				} else if (!now.canBeReplaced()) {
					wrong++;
				} else {
					(handOnly(site.state(i)) ? hand : missing).merge(site.state(i).getBlock().asItem(), 1, Integer::sum);
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
				blueprint == null ? "?" : blueprint.name, done, site == null ? 0 : site.size(), mode.id(), items, counts, wrong, handOnly, mayChange(player), open);
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

	/** The placed blueprint's cells in the world (null while its blueprint is unknown). */
	public @Nullable BuildSite site() {
		if (site == null) {
			Blueprint blueprint = blueprint();
			if (blueprint != null) {
				site = new BuildSite(blueprint, rotation, getBlockPos());
				cursor = 0;
			}
		}
		return site;
	}

	/** Every block of the blueprint, absolute, in order (bottom layer first). A copy: for tests and small builds. */
	public Map<BlockPos, BlockState> wanted() {
		Map<BlockPos, BlockState> out = new LinkedHashMap<>();
		BuildSite site = site();
		if (site != null) {
			for (int i = 0; i < site.size(); i++) {
				out.put(site.blockPos(i), site.state(i));
			}
		}
		return out;
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
		BuildSite site = site();
		int total = site == null ? 0 : site.size();
		for (int i = 0; i < total; i++) {
			BlockState now = level.getBlockState(site.blockPos(i));
			if (site.matches(now, i)) {
				done++;
			} else if (!now.canBeReplaced()) {
				wrong++;
			} else {
				(handOnly(site.state(i)) ? handOnly : missing).merge(site.state(i).getBlock().asItem(), 1, Integer::sum);
			}
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.blueprint.progress", blueprint.name, done, total,
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
		BuildSite site = site();
		if (site == null || site.size() == 0) {
			return;
		}
		long phase = server.getGameTime() + getBlockPos().hashCode();
		if (site.blueprint.large()) {
			// A big build is checked in full only now and then; in between drones mark the blocks they place.
			if (Math.floorMod(phase, VERIFY_INTERVAL) != 0) {
				return;
			}
			boolean all = true;
			BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
			for (int i = 0; i < site.size(); i++) {
				at.set(site.pos(i));
				boolean in = server.isLoaded(at) && site.matches(server.getBlockState(at), i);
				site.done.set(i, in);
				all &= in;
			}
			cursor = 0;
			if (all) {
				complete(server);
			}
			return;
		}
		if (Math.floorMod(phase, CHECK_INTERVAL) != 0) {
			return;
		}
		for (int i = 0; i < site.size(); i++) {
			BlockPos p = site.blockPos(i);
			if (!server.isLoaded(p) || !site.matches(server.getBlockState(p), i)) {
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
		// The blueprint was used up when it was placed: the stake just goes, nothing drops.
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
		site = null;
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

		private static long column(BlockPos pos) {
			return ((long) pos.getX() << 32) | (pos.getZ() & 0xFFFFFFFFL);
		}

		private boolean open(ServerLevel level, BlockPos pos, BlockState state) {
			if (!level.isLoaded(pos)) {
				return false;
			}
			BlockState now = level.getBlockState(pos);
			return !matches(now, state) && (now.canBeReplaced() || obstacle(level, pos, now));
		}

		/**
		 * Something in the way (a red cell) that the drone breaks and replaces with the right block: terrain, a
		 * tree, a stray block, or a chest or vanilla machine, which is broken with its drops so nothing in it is
		 * lost. Never unbreakable blocks (bedrock), nor Jugcraft's own blocks with a block entity (a depot, a
		 * stake, a tower core): those wait for the player.
		 */
		private boolean obstacle(ServerLevel level, BlockPos pos, BlockState now) {
			if (now.isAir() || now.getDestroySpeed(level, pos) < 0) {
				return false;
			}
			return level.getBlockEntity(pos) == null
					|| !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(now.getBlock()).getNamespace().equals(io.github.jimbozoomer.jugcraft.Jugcraft.MOD_ID);
		}

		/** A wanted cell with a wrong block in it (drawn red): replacing it never covers anything new. */
		private boolean blocked(ServerLevel level, BlockPos pos) {
			return !level.getBlockState(pos).canBeReplaced();
		}

		/** Is the block under {@code pos} ready to build on: done, solid ground, or a cell drones never fill? */
		private boolean supported(ServerLevel level, BlockPos pos, BuildSite site) {
			BlockPos under = pos.below();
			int below = site.find(under);
			if (below < 0 || handOnly(site.state(below))) {
				return true;
			}
			BlockState now = level.getBlockState(under);
			return site.matches(now, below) || level.getBlockEntity(under) != null;
		}

		@Override
		public List<BuildJobs.Target> openTargets(ServerLevel level, BlockPos center, int radius, UUID depotOwner, UseMode depotMode, int max) {
			BuildSite site = site();
			if (owner == null || isRemoved() || level != SurveyStakeBlockEntity.this.level || site == null
					|| !JugcraftParties.mayServe(depotOwner, depotMode, owner, mode)) {
				return List.of();
			}
			long now = level.getGameTime();
			boolean large = site.blueprint.large();
			// The blocks this depot could place: in its range, not hand-only, still missing.
			List<Integer> missing = new ArrayList<>();
			// The lowest missing block of each column: drones come down from above, so a block only goes in once
			// everything under it in its column is done (a roof placed early would cover the pews under it).
			Map<Long, Integer> columnBottom = new HashMap<>();
			// A large build is looked at a few layers at a time, from the lowest layer with work in it: the rest waits.
			if (large) {
				while (cursor < site.layers() && site.done.nextClearBit(site.layerFrom(cursor)) >= site.layerFrom(cursor + 1)) {
					cursor++;
				}
			}
			int lowestHere = Integer.MAX_VALUE;
			BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
			for (int i = large ? site.layerFrom(cursor) : 0; i < site.size(); i++) {
				if (large) {
					i = site.done.nextClearBit(i);
					if (i >= site.size()) {
						break;
					}
					if (lowestHere != Integer.MAX_VALUE && site.layerOf(i) > lowestHere + LAYER_WINDOW + 1) {
						break;
					}
				}
				BlockState state = site.state(i);
				if (handOnly(state)) {
					continue;
				}
				at.set(site.pos(i));
				boolean inRange = Math.abs(at.getX() - center.getX()) <= radius && Math.abs(at.getZ() - center.getZ()) <= radius;
				if (large && !inRange) {
					// Out of this depot's reach: assume it is still to do (the full check marks it done when it is).
					columnBottom.merge(column(at), at.getY(), Math::min);
					continue;
				}
				if (!level.isLoaded(at)) {
					continue;
				}
				BlockState world = level.getBlockState(at);
				if (site.matches(world, i)) {
					site.done.set(i);
					continue;
				}
				if (!(world.canBeReplaced() || obstacle(level, at, world))) {
					continue;
				}
				columnBottom.merge(column(at), at.getY(), Math::min);
				if (inRange) {
					missing.add(i);
					if (!blocked(level, at)) {
						lowestHere = Math.min(lowestHere, site.layerOf(i));
					}
				}
			}
			List<BuildJobs.Target> out = new ArrayList<>();
			// Red cells first, from the top down: a wrong block is swapped for the right one straight away (it was
			// solid already, so this covers nothing), and clearing the top ones opens the way to those under them.
			missing.stream()
					.filter(i -> blocked(level, site.blockPos(i)))
					.filter(i -> reservedUntil.getOrDefault(site.blockPos(i), Long.MIN_VALUE) < now)
					.sorted(Comparator.comparingInt((Integer i) -> -site.layerOf(i)).thenComparingDouble(i -> site.blockPos(i).distSqr(center)))
					.limit(max)
					.forEach(i -> out.add(new BuildJobs.Target(this, jobId, owner, mode, site.blockPos(i), site.state(i), null)));
			// Then the empty cells, roughly layer by layer, a few layers at a time: while the last blocks of a layer
			// are still in the air the next layers go ahead, so a big fleet keeps working instead of waiting.
			int lowest = lowestHere;
			missing.stream()
					.filter(i -> !blocked(level, site.blockPos(i)))
					.filter(i -> site.layerOf(i) <= lowest + LAYER_WINDOW)
					.filter(i -> reservedUntil.getOrDefault(site.blockPos(i), Long.MIN_VALUE) < now)
					.filter(i -> columnBottom.get(column(site.blockPos(i))) == BlockPos.getY(site.pos(i)))
					// Bottom-up: never a block straight above one that is still missing (also outside the blueprint's
					// own column bookkeeping, e.g. a cell another depot is placing right now).
					.filter(i -> supported(level, site.blockPos(i), site))
					.sorted(Comparator.comparingInt((Integer i) -> site.layerOf(i)).thenComparingDouble(i -> site.blockPos(i).distSqr(center)))
					.limit(Math.max(0, max - out.size()))
					.forEach(i -> out.add(new BuildJobs.Target(this, jobId, owner, mode, site.blockPos(i), site.state(i), null)));
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
			BuildSite site = site();
			int i = site == null ? -1 : site.find(target.pos());
			BlockState state = i < 0 ? null : site.state(i);
			return state != null && !isRemoved() && owner != null && JugcraftParties.mayServe(depotOwner, depotMode, owner, mode)
					&& open(level, target.pos(), state);
		}

		@Override
		public boolean fill(ServerLevel level, BuildJobs.Target target, BlockState state) {
			BlockState now = level.getBlockState(target.pos());
			if (!now.canBeReplaced() && !obstacle(level, target.pos(), now)) {
				return false;
			}
			// Clears whatever is in the way, then puts the block in: terrain and stray blocks go without drops (site
			// clearance), a chest or machine is broken with its drops so its contents are not lost.
			if (level.getBlockEntity(target.pos()) != null) {
				level.destroyBlock(target.pos(), true);
			}
			if (state.getBlock() instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock anchor) {
				// Nuclear equipment is placed whole: its body is cleared of terrain first, then it goes in like from its item.
				net.minecraft.core.Direction facing = state.getValue(io.github.jimbozoomer.jugcraft.nuclear.EquipmentAnchorBlock.FACING);
				for (BlockPos offset : anchor.kind.frames(facing)) {
					BlockPos cell = target.pos().offset(offset);
					BlockState there = level.getBlockState(cell);
					if (!there.canBeReplaced() && obstacle(level, cell, there)) {
						level.destroyBlock(cell, level.getBlockEntity(cell) != null);
					}
				}
				if (!level.getBlockState(target.pos()).canBeReplaced()) {
					level.removeBlock(target.pos(), false);
				}
				if (!io.github.jimbozoomer.jugcraft.nuclear.EquipmentPlacement.place(level, target.pos(), anchor.kind, facing, null)) {
					return false;
				}
				if (level.getBlockEntity(target.pos()) instanceof io.github.jimbozoomer.jugcraft.nuclear.EquipmentBlockEntity equipment) {
					equipment.setOwner(owner);
				}
			} else {
				// Panes, bars, fences and pipes join their neighbours as if placed by hand.
				level.setBlockAndUpdate(target.pos(), Block.updateFromNeighbourShapes(state, level, target.pos()));
			}
			BuildSite site = site();
			int i = site == null ? -1 : site.find(target.pos());
			if (i >= 0) {
				site.done.set(i);
			}
			return true;
		}
	}
}
