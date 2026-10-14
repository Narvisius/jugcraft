package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.placement.StructureReach;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * A storage tank placed from a kit comes down whole (Justin, October 8 2026: "once you break an MBS, it just drops the
 * entire MBS for you"; asked, he chose the kit back with the liquid lost): breaking any block the kit laid, the shell,
 * a nozzle, the ladder, the railing, a fitting or the foundation, takes the whole tank down and drops one empty kit of
 * its size and rating (none in creative). Whatever was in it is lost. The ground the foundation went into comes back.
 *
 * <p>The tank's outlet remembers what its kit laid ({@link StorageTankPortBlockEntity#setKit}). A tank that is no
 * longer as its kit laid it (a block gone in an explosion, swapped for another) forgets its kit the next time a block
 * of it is broken and from then on breaks a block at a time, like a hand-built tank, so nothing comes back twice.
 * Hand-built tanks are left as they were: Justin chose that only MBS placed from one item come down whole.
 *
 * <p>Tanks placed from a kit before the outlet remembered it are recognised once, the first time the outlet looks its
 * tank over ({@link #adopt}): if the tank stands exactly as a kit lays it, the outlet takes that kit on.
 */
public final class TankKitBreaking {
	private static final String[] KITS = {"general_small", "general_medium", "general_large", "flammable_small", "flammable_medium",
			"flammable_large", "corrosive_small", "corrosive_medium", "corrosive_large", "cryogenic"};
	/** Every kind of block a kit lays or a re-rating fits: a block broken that is none of these is no tank's. */
	private static @Nullable Set<Block> members;

	private TankKitBreaking() {
	}

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) ->
				!(level instanceof ServerLevel server) || !breakWhole(server, player, pos, state));
	}

	private static synchronized Set<Block> members() {
		if (members == null) {
			Set<Block> set = new HashSet<>();
			for (String kit : KITS) {
				Blueprint blueprint = TankKitItem.blueprint(kit);
				if (blueprint != null) {
					for (Blueprint.Cell cell : blueprint.rawCells()) {
						if (!cell.state().isAir()) {
							set.add(cell.state().getBlock());
						}
					}
				}
			}
			for (TankRating rating : TankRating.values()) {
				set.add(TankFittings.vent(rating));
			}
			for (String fitting : new String[] {"tank_foam_chamber", "tank_foam_line", "tank_earthing_boss"}) {
				Block block = StorageTanks.BLOCKS.get(fitting);
				if (block != null) {
					set.add(block);
				}
			}
			members = set;
		}
		return members;
	}

	/**
	 * {@code player} breaks the block at {@code pos}: if a kit laid it and its tank still stands as laid, the whole tank
	 * comes down and true is returned (the ordinary break is called off). Otherwise false, and the block breaks as usual.
	 */
	public static boolean breakWhole(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		if (!members().contains(state.getBlock())) {
			return false;
		}
		// Beyond the ordinary reach (a far-placing item in hand), breaking is refused anyway (StructureReach).
		if (StructureReach.bonus(player) > 0 && !StructureReach.withinOrdinaryReach(player, pos)) {
			return false;
		}
		StorageTankPortBlockEntity tank = owner(level, pos);
		if (tank == null) {
			return false;
		}
		if (!tank.kitIntact(level)) {
			tank.forgetKit();
			return false;
		}
		Item kit = kitItem(tank);
		takeDown(level, tank);
		if (!player.preventsBlockDrops() && kit != null) {
			Block.popResource(level, pos, new ItemStack(kit));
		}
		if (!player.getAbilities().instabuild) {
			player.getMainHandItem().mineBlock(level, state, pos, player);
		}
		return true;
	}

	/** The outlet of the kit-placed tank that the block at {@code pos} is part of, or null. */
	public static @Nullable StorageTankPortBlockEntity owner(ServerLevel level, BlockPos pos) {
		int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
				if (chunk == null) {
					continue;
				}
				for (BlockEntity entity : chunk.getBlockEntities().values()) {
					if (entity instanceof StorageTankPortBlockEntity port && port.kit() != null && port.kitHas(pos)) {
						return port;
					}
				}
			}
		}
		return null;
	}

	/** The kit to give back: the tank's size, in the rating it has now (a re-rated tank gives the kit it now matches). */
	static @Nullable Item kitItem(StorageTankPortBlockEntity tank) {
		String kit = tank.kit();
		if (kit == null) {
			return null;
		}
		String now = kit;
		if (!kit.equals("cryogenic")) {
			now = tank.rating().getSerializedName() + kit.substring(kit.lastIndexOf('_'));
		}
		Item item = StorageTanks.ITEMS.get("tank_kit_" + now);
		return item != null ? item : StorageTanks.ITEMS.get("tank_kit_" + kit);
	}

	/**
	 * Takes a kit-placed tank down: the space inside, every block the kit laid (the outlet last, so it doesn't unform
	 * a tank that is going anyway) and its liquid, quietly (nothing drops), then the ground its foundation went into
	 * comes back and what stands round it hears that it has gone.
	 */
	public static void takeDown(ServerLevel level, StorageTankPortBlockEntity tank) {
		BlockPos outlet = tank.getBlockPos().immutable();
		List<BlockPos> cells = tank.kitCells();
		Map<BlockPos, BlockState> ground = tank.kitGround();
		Set<BlockPos> gone = new HashSet<>(cells);
		gone.addAll(tank.interiorPositions());
		int quiet = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
		// Some dust and the sound of it coming down, spread over the tank.
		Map<BlockPos, BlockState> before = new LinkedHashMap<>();
		for (BlockPos p : cells) {
			before.put(p, level.getBlockState(p));
		}
		for (BlockPos p : tank.interiorPositions()) {
			if (level.getBlockState(p).is(StorageTanks.INTERIOR)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), quiet);
			}
		}
		for (BlockPos p : cells) {
			if (!p.equals(outlet)) {
				level.setBlock(p, Blocks.AIR.defaultBlockState(), quiet);
			}
		}
		tank.fluid.amount = 0;
		level.setBlock(outlet, Blocks.AIR.defaultBlockState(), quiet);
		// The ground the foundation took, as it was.
		for (Map.Entry<BlockPos, BlockState> e : ground.entrySet()) {
			if (!e.getValue().isAir()) {
				level.setBlock(e.getKey(), e.getValue(), Block.UPDATE_ALL);
				gone.remove(e.getKey());
			}
		}
		// What stands round it learns it has gone (a torch on the shell drops, pipes let go, grass loses its snow).
		for (BlockPos p : gone) {
			BlockState was = before.getOrDefault(p, StorageTanks.INTERIOR.defaultBlockState());
			boolean edge = false;
			for (Direction d : Direction.values()) {
				BlockPos q = p.relative(d);
				if (gone.contains(q)) {
					continue;
				}
				edge = true;
				BlockState there = level.getBlockState(q);
				BlockState updated = Block.updateFromNeighbourShapes(there, level, q);
				if (updated != there) {
					Block.updateOrDestroy(there, updated, level, q, Block.UPDATE_ALL);
				}
			}
			if (edge) {
				level.updateNeighborsAt(p, was.getBlock(), null);
			}
		}
		int k = 0;
		for (Map.Entry<BlockPos, BlockState> e : before.entrySet()) {
			if (k++ % 9 == 0 && !e.getValue().isAir()) {
				level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, e.getKey(), Block.getId(e.getValue()));
			}
		}
		level.playSound(null, outlet, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 0.8F, 0.7F);
		level.playSound(null, outlet, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
	}

	/**
	 * A tank placed from a kit before tanks remembered their kits: if it stands exactly as one of the kits lays it
	 * (every block the kit lays where the kit lays it, the fittings apart, which a re-rating swaps), its outlet takes
	 * that kit on, the ground under it unknown.
	 */
	static void adopt(ServerLevel level, StorageTankPortBlockEntity tank) {
		BlockPos outlet = tank.getBlockPos();
		Set<Block> fittings = new HashSet<>();
		for (TankRating rating : TankRating.values()) {
			fittings.add(TankFittings.vent(rating));
		}
		for (String fitting : new String[] {"tank_foam_chamber", "tank_foam_line", "tank_earthing_boss"}) {
			fittings.add(StorageTanks.BLOCKS.get(fitting));
		}
		// The kits of the tank's own rating first; then those that lay the most (a corrosive kit's bund, a flammable
		// kit's foam), so a kit that lays less never takes a tank that has more.
		String own = tank.rating().getSerializedName();
		List<String> order = new ArrayList<>();
		for (String prefix : new String[] {own, "corrosive", "flammable", "general", "cryogenic"}) {
			for (String kit : KITS) {
				if (kit.startsWith(prefix) && !order.contains(kit)) {
					order.add(kit);
				}
			}
		}
		for (String kit : order) {
			Blueprint blueprint = TankKitItem.blueprint(kit);
			if (blueprint == null) {
				continue;
			}
			for (Rotation rotation : Rotation.values()) {
				int at = -1;
				for (int i = 0; i < blueprint.size(); i++) {
					if (blueprint.state(i, rotation).getBlock() instanceof StorageTankOutletBlock
							&& blueprint.state(i, rotation).getValue(StorageTankOutletBlock.FACING) == tank.getBlockState().getValue(StorageTankPortBlock.FACING)) {
						at = i;
					}
				}
				if (at < 0) {
					continue;
				}
				BlockPos stake = outlet.subtract(blueprint.offset(at, rotation));
				List<BlockPos> cells = new ArrayList<>();
				List<BlockPos> groundCells = new ArrayList<>();
				List<BlockState> ground = new ArrayList<>();
				boolean matches = true;
				for (int i = 0; i < blueprint.size() && matches; i++) {
					BlockState want = blueprint.state(i, rotation);
					BlockPos p = stake.offset(blueprint.offset(i, rotation));
					if (!level.isLoaded(p)) {
						matches = false;
						break;
					}
					BlockState there = level.getBlockState(p);
					if (want.isAir()) {
						matches = there.isAir() || there.is(StorageTanks.INTERIOR);
					} else if (fittings.contains(want.getBlock())) {
						if (!there.isAir()) {
							cells.add(p);
						}
					} else {
						matches = there.is(want.getBlock());
						cells.add(p);
						if (blueprint.offsetY(i) < 0) {
							// What the ground was is not known: earth, grass on top.
							groundCells.add(p);
							ground.add(blueprint.offsetY(i) == -1 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState());
						}
					}
				}
				if (matches) {
					// The fittings a re-rating put on since, beside the shell.
					Set<BlockPos> laid = new HashSet<>(cells);
					for (BlockPos p : tank.platePositions()) {
						for (Direction d : Direction.values()) {
							BlockPos q = p.relative(d);
							if (!laid.contains(q) && fittings.contains(level.getBlockState(q).getBlock())) {
								cells.add(q);
								laid.add(q);
							}
						}
					}
					tank.setKit(level, kit, cells, groundCells, ground);
					Jugcraft.LOGGER.debug("Storage tank at {} recognised as kit {}", outlet, kit);
					return;
				}
			}
		}
	}

	/** The block ids a kit lays, for tests. */
	static List<String> memberIds() {
		List<String> out = new ArrayList<>();
		for (Block block : members()) {
			out.add(BuiltInRegistries.BLOCK.getKey(block).toString());
		}
		return out;
	}
}
