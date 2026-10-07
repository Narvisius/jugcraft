package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Builds all three pre-made nuclear plants side by side, well apart, each on the shore of its own lake, fuelled and
 * running (operators: {@code /nuclear showcase plants}). Started with the environment variable
 * {@code JUGCRAFT_SHOWCASE=nuclear} (14-nuclear-showcase.bat), it also builds them once in a new world when the first
 * player joins, and puts that player in front of them. A world built that way has its plant brought up to date in
 * place whenever it is joined after the plant has changed, however the game was started.
 */
public final class PlantShowcase {
	/** The plants, west to east, and the space left between them (blocks). */
	private static final String[] PLANTS = {"nuclear_small_modular", "nuclear_medium_pwr", "nuclear_large_epr"};
	private static final int GAP = 250;
	/** The lake in front of each plant: how far out from the shore and how deep. */
	private static final int LAKE_OUT = 50, LAKE_DEEP = 5;

	private PlantShowcase() {
	}

	public static void register() {
		boolean buildNew = "nuclear".equalsIgnoreCase(System.getenv("JUGCRAFT_SHOWCASE"));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			ServerPlayer player = handler.player;
			Path marker = server.getWorldPath(LevelResource.ROOT).resolve("jugcraft").resolve("nuclear-showcase.done");
			ServerLevel level = server.overworld();
			if (Files.exists(marker)) {
				// A showcase world, however the game was started (6-play.bat too): if the plant has changed since (a new
				// build of the mod), bring it up to date in place once the player's surroundings have loaded.
				pending = new Pending(player, marker, 40, 30);
				return;
			}
			if (!buildNew) {
				return;
			}
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 6000");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "gamerule advance_time false");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather clear 1000000");
			BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
			// The plant under review: only the medium plant for now (all three with /nuclear showcase plants).
			List<BlockPos> stakes = build(level, player, ground, new String[] {REVIEWED});
			if (!stakes.isEmpty()) {
				writeMarker(marker, stakes.get(0), fingerprint(Blueprint.get(REVIEWED, false)));
			}
		}));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Pending p = pending;
			if (p == null || --p.wait > 0) {
				return;
			}
			if (p.player.isRemoved()) {
				pending = null;
				return;
			}
			if (refreshIfChanged(p.player.level(), p.player, p.marker)) {
				pending = null;
			} else if (--p.tries <= 0) {
				pending = null;
				p.player.sendSystemMessage(Component.literal("The nuclear plant may be out of date, but its reactor is too far away to find. "
						+ "Go to the plant and use /nuclear showcase refresh.").withStyle(ChatFormatting.GRAY));
			} else {
				p.wait = 40;
			}
		});
	}

	/** The plant the showcase builds (the one under review). */
	private static final String REVIEWED = "nuclear_medium_pwr";

	private static final class Pending {
		final ServerPlayer player;
		final Path marker;
		int wait, tries;

		Pending(ServerPlayer player, Path marker, int wait, int tries) {
			this.player = player;
			this.marker = marker;
			this.wait = wait;
			this.tries = tries;
		}
	}

	private static volatile Pending pending;

	/** A fingerprint of every block in a blueprint, so a world built from an older one can tell. */
	static long fingerprint(Blueprint blueprint) {
		long h = 1125899906842597L;
		if (blueprint == null) {
			return h;
		}
		for (int i = 0; i < blueprint.size(); i++) {
			h = 31 * h + blueprint.offset(i, Rotation.NONE).asLong();
			h = 31 * h + blueprint.state(i, Rotation.NONE).toString().hashCode();
		}
		return h;
	}

	private static void writeMarker(Path marker, BlockPos stake, long fingerprint) {
		try {
			Files.createDirectories(marker.getParent());
			Files.writeString(marker, "plant=" + REVIEWED + "\nstake=" + stake.getX() + "," + stake.getY() + "," + stake.getZ()
					+ "\nfingerprint=" + fingerprint + "\n");
		} catch (java.io.IOException e) {
			Jugcraft.LOGGER.warn("Could not mark the nuclear showcase as built: {}", e.toString());
		}
	}

	/**
	 * If the showcase plant in this world was built from an older blueprint, rebuilds it in place (its buildings cleared
	 * and built again). Returns false if it could not tell where the plant is yet (try again once more has loaded).
	 */
	static boolean refreshIfChanged(ServerLevel level, ServerPlayer player, Path marker) {
		Blueprint blueprint = Blueprint.get(REVIEWED, false);
		if (blueprint == null) {
			return true;
		}
		BlockPos stake = null;
		long built = 0;
		try {
			for (String line : Files.readAllLines(marker)) {
				if (line.startsWith("stake=")) {
					String[] xyz = line.substring(6).split(",");
					stake = new BlockPos(Integer.parseInt(xyz[0].trim()), Integer.parseInt(xyz[1].trim()), Integer.parseInt(xyz[2].trim()));
				} else if (line.startsWith("fingerprint=")) {
					built = Long.parseLong(line.substring(12).trim());
				}
			}
		} catch (Exception e) {
			Jugcraft.LOGGER.warn("Could not read the nuclear showcase marker: {}", e.toString());
		}
		long now = fingerprint(blueprint);
		if (stake != null && built == now) {
			return true;
		}
		if (stake == null) {
			// A world built before the marker kept the stake: find the plant by its reactor (it is the only one).
			stake = locate(level, blueprint, player.blockPosition());
			if (stake == null) {
				return false;
			}
		}
		player.sendSystemMessage(Component.literal("The nuclear plant has changed since this world was built: rebuilding it in place (a few seconds)...")
				.withStyle(ChatFormatting.GRAY));
		rebuild(level, blueprint, stake, player);
		writeMarker(marker, stake, now);
		player.sendSystemMessage(Component.literal("Done: the plant is up to date.").withStyle(ChatFormatting.GRAY));
		return true;
	}

	/** Where the plant's stake is, from its reactor vessel (loaded near the player), or null. */
	public static BlockPos locate(ServerLevel level, Blueprint blueprint, BlockPos near) {
		BlockPos offset = null;
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, Rotation.NONE).getBlock() instanceof EquipmentAnchorBlock anchor && anchor.kind == EquipmentKind.REACTOR_VESSEL) {
				offset = blueprint.offset(i, Rotation.NONE);
				break;
			}
		}
		if (offset == null) {
			return null;
		}
		BlockPos best = null;
		for (java.util.Map.Entry<BlockPos, EquipmentRegistry.Entry> e : EquipmentRegistry.of(level).anchors().entrySet()) {
			if (e.getValue().kind() == EquipmentKind.REACTOR_VESSEL && (best == null || e.getKey().distSqr(near) < best.distSqr(near))) {
				best = e.getKey();
			}
		}
		return best == null ? null : best.subtract(offset);
	}

	/** Clears a block for the rebuild, keeping what a storage rack or a storage tank held. */
	private static void clear(ServerLevel level, BlockPos.MutableBlockPos at, java.util.Map<BlockPos, List<net.minecraft.world.item.ItemStack>> stored,
			java.util.Map<BlockPos, io.github.jimbozoomer.jugcraft.fluid.StoredFluid> tanks, int flags) {
		if (level.isOutsideBuildHeight(at) || level.getBlockState(at).isAir()) {
			return;
		}
		if (level.getBlockEntity(at) instanceof StorageRackBlockEntity rack && !rack.isEmpty()) {
			List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
			for (int i = 0; i < rack.getContainerSize(); i++) {
				items.add(rack.getItem(i).copy());
			}
			stored.put(at.immutable(), items);
		} else if (level.getBlockEntity(at) instanceof StorageTankPortBlockEntity port && port.stored() != null) {
			tanks.put(at.immutable(), port.stored());
		}
		level.setBlock(at, Blocks.AIR.defaultBlockState(), flags);
	}

	/**
	 * Clears every building of the plant above the ground (its box, a block round it for the signs on its walls, two
	 * over its roof) and builds the plant again, running. What was stored in its storage racks is put back in the rack
	 * built in the same place (or dropped there if there is none now), and its storage tanks keep their fluid.
	 */
	public static void rebuild(ServerLevel level, Blueprint blueprint, BlockPos stake, ServerPlayer player) {
		int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		java.util.Map<BlockPos, List<net.minecraft.world.item.ItemStack>> stored = new java.util.HashMap<>();
		java.util.Map<BlockPos, io.github.jimbozoomer.jugcraft.fluid.StoredFluid> tanks = new java.util.HashMap<>();
		// Each column cleared so far: up to this height over the stake.
		java.util.Map<Long, Integer> cleared = new java.util.HashMap<>();
		for (Blueprint.PartRef part : blueprint.parts) {
			int x0 = part.x() - blueprint.anchorX(), y0 = part.y() - blueprint.anchorY(), z0 = part.z() - blueprint.anchorZ();
			for (int x = x0 - 1; x <= x0 + part.sizeX(); x++) {
				for (int z = z0 - 1; z <= z0 + part.sizeZ(); z++) {
					// Only from the ground up: what is below is overwritten by the new blocks, and the ground round a
					// sunk building (its box's corners) stays.
					for (int y = Math.max(y0, 0); y <= y0 + part.sizeY() + 1; y++) {
						clear(level, at.set(stake.getX() + x, stake.getY() + y, stake.getZ() + z), stored, tanks, flags);
					}
					cleared.merge(BlockPos.asLong(x, 0, z), y0 + part.sizeY() + 1, Math::max);
				}
			}
		}
		// Then what stands outside or above the buildings (pipe bridges, pipes, the bus, stacks), column by column up
		// to a block over its top: when the buildings come down a block, what went with them leaves nothing behind
		// where it was.
		java.util.Map<Long, Integer> tops = new java.util.HashMap<>();
		for (int i = 0; i < blueprint.size(); i++) {
			BlockPos o = blueprint.offset(i, Rotation.NONE);
			if (o.getY() >= 0) {
				tops.merge(BlockPos.asLong(o.getX(), 0, o.getZ()), o.getY(), Math::max);
			}
		}
		tops.forEach((column, top) -> {
			for (int y = Math.max(0, cleared.getOrDefault(column, -1) + 1); y <= top + 1; y++) {
				clear(level, at.set(stake.getX() + BlockPos.getX(column), stake.getY() + y, stake.getZ() + BlockPos.getZ(column)), stored, tanks, flags);
			}
		});
		PlantBuilder.Result result = PlantBuilder.build(level, blueprint, stake, Rotation.NONE, player.getUUID(), true);
		start(level, result);
		// The storage tanks keep what was in them.
		tanks.forEach((pos, fluid) -> {
			if (level.getBlockEntity(pos) instanceof StorageTankPortBlockEntity port) {
				port.restore(fluid);
				port.check(level);
			}
		});
		for (java.util.Map.Entry<BlockPos, List<net.minecraft.world.item.ItemStack>> e : stored.entrySet()) {
			BlockPos pos = e.getKey();
			List<net.minecraft.world.item.ItemStack> items = e.getValue();
			if (level.getBlockEntity(pos) instanceof StorageRackBlockEntity rack) {
				for (int i = 0; i < items.size() && i < rack.getContainerSize(); i++) {
					rack.setItem(i, items.get(i));
				}
				rack.setChanged();
			} else {
				for (net.minecraft.world.item.ItemStack stack : items) {
					if (!stack.isEmpty()) {
						net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
					}
				}
			}
		}
	}

	/**
	 * Builds the three plants in a row eastward from {@code start} (the first plant's stake, on the ground), each facing
	 * north from its lake, and tells {@code player} where each one is (click to go there). Returns the stakes.
	 */
	public static List<BlockPos> build(ServerLevel level, ServerPlayer player, BlockPos start) {
		return build(level, player, start, PLANTS);
	}

	/**
	 * Places in each plant worth walking to, for the medium plant (site coordinates as in tools/medium_plant.py: x across
	 * the site, y up from the ground, z inland from the shore; then the direction to look, as site x/z, and a name).
	 */
	private static final Object[][] MEDIUM_TOUR = {
			{"Main gate and security", 140, 0, 175, 0, -1},
			{"Security: visitors' hall", 144, 0, 166, -1, 0},
			{"Main control room", 151, 15, 103, 0, -1},
			{"Plant computer room", 151, 8, 108, 1, 0},
			{"Containment: operating floor", 84, 17, 51, 0, 1},
			{"Containment: steam generator level", 83, 9, 48, 0, 1},
			{"Turbine hall: operating floor", 140, 13, 50, 1, 0},
			{"Turbine hall: ground floor", 150, 1, 70, 1, 0},
			{"Spent fuel pool", 115, 13, 122, 0, -1},
			{"Intake pump house", 170, 1, 12, 0, -1},
			{"Warehouse", 280, 1, 117, 1, 0},
			{"Administration: front lobby", 70, 0, 158, 0, 1},
			{"Administration: locker room", 52, 0, 167, 1, 0},
			{"Administration: board room", 80, 5, 167, 1, 0},
			{"Administration: chemistry lab", 52, 5, 167, 1, 0},
			{"Administration: manager's office", 80, 10, 167, 1, 0},
			{"Cooling towers", 185, 30, 160, 0, 1},
	};

	public static List<BlockPos> build(ServerLevel level, ServerPlayer player, BlockPos start, String[] plants) {
		List<BlockPos> stakes = new ArrayList<>();
		int x = start.getX();
		player.sendSystemMessage(Component.literal("Building the nuclear plant" + (plants.length > 1 ? "s" : "") + " (a few seconds)...")
				.withStyle(ChatFormatting.GRAY));
		for (String id : plants) {
			Blueprint blueprint = Blueprint.get(id, false);
			if (blueprint == null) {
				continue;
			}
			// With the stake at the shore, the plant runs from (stake - (sizeX - 1 - anchorX)) to (stake + anchorX)... turned:
			// built unturned, file x maps to stake.x + (fileX - anchorX).
			int west = -blueprint.anchorX(), east = blueprint.sizeX - 1 - blueprint.anchorX();
			BlockPos stake = new BlockPos(x - west, start.getY(), start.getZ());
			x = stake.getX() + east + GAP;
			lake(level, stake, west - 20, east + 20);
			PlantBuilder.Result result = PlantBuilder.build(level, blueprint, stake, Rotation.NONE, player.getUUID(), true);
			start(level, result);
			stakes.add(stake);
			BlockPos view = stake.offset(0, 40, 70);
			player.sendSystemMessage(Component.literal("» " + blueprint.name + "  (" + view.toShortString() + ", click to fly there)")
					.withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent.RunCommand(
							"/tp @s " + view.getX() + " " + view.getY() + " " + view.getZ() + " 180 20"))));
			if (id.equals("nuclear_medium_pwr")) {
				tour(player, blueprint, stake);
			}
		}
		if (!stakes.isEmpty()) {
			BlockPos middle = stakes.get(stakes.size() / 2);
			player.teleportTo(level, middle.getX() + 0.5, middle.getY() + 60, middle.getZ() + 110, java.util.Set.of(), 180, 25, true);
		}
		player.sendSystemMessage(Component.literal("The reactors are running at 60%. Link Card not needed: the control room consoles are linked.")
				.withStyle(ChatFormatting.GRAY));
		return stakes;
	}

	/** Clickable chat links to the places worth seeing inside a plant. */
	private static void tour(ServerPlayer player, Blueprint blueprint, BlockPos stake) {
		// Site to world: built unturned, the plant reaches north from the stake with its east-west turned round.
		int ax = blueprint.anchorX(), sizeX = blueprint.sizeX;
		player.sendSystemMessage(Component.literal("Walk through it:").withStyle(ChatFormatting.GRAY));
		for (Object[] spot : MEDIUM_TOUR) {
			int sx = (Integer) spot[1], sy = (Integer) spot[2], sz = (Integer) spot[3], lx = (Integer) spot[4], lz = (Integer) spot[5];
			int wx = stake.getX() + sizeX - 1 - sx - ax, wy = stake.getY() + sy, wz = stake.getZ() - sz;
			// Looking along site +x is world -x (west, yaw 90); along site +z is world -z (north, yaw 180).
			int yaw = lx > 0 ? 90 : lx < 0 ? -90 : lz > 0 ? 180 : 0;
			player.sendSystemMessage(Component.literal("   » " + spot[0])
					.withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent.RunCommand(
							"/tp @s " + wx + " " + wy + " " + wz + " " + yaw + " 10"))));
		}
	}

	/** A lake in front of the shore (south of the stake), for the intake: water where there is ground or air. */
	private static void lake(ServerLevel level, BlockPos stake, int fromX, int toX) {
		int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		int bottom = Math.max(level.getMinY() + 1, stake.getY() - LAKE_DEEP);
		for (int dx = fromX; dx <= toX; dx++) {
			for (int dz = 1; dz <= LAKE_OUT; dz++) {
				for (int y = stake.getY() + 3; y >= bottom; y--) {
					BlockPos p = new BlockPos(stake.getX() + dx, y, stake.getZ() + dz);
					level.setBlock(p, y < stake.getY() ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), flags);
				}
				// A sand floor and shore.
				level.setBlock(new BlockPos(stake.getX() + dx, bottom - 1, stake.getZ() + dz), Blocks.SAND.defaultBlockState(), flags);
			}
		}
	}

	/** Fuels and starts each reactor at 60%, with cooling water in hand so it runs from the first tick. */
	private static void start(ServerLevel level, PlantBuilder.Result result) {
		for (BlockPos vessel : result.vessels()) {
			if (level.getBlockEntity(vessel) instanceof EquipmentBlockEntity v) {
				v.core.rodTarget = 40;
				v.core.rods = 40;
				v.core.fission = 0.6;
				v.core.decay = ReactorCore.DECAY_SHARE * 0.6;
				v.core.temperature = 300;
				v.changed();
				ReactorUnit unit = ReactorUnit.find(level, vessel);
				for (BlockPos c : unit.condensers) {
					if (level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser && condenser.water != null) {
						condenser.water.fill(Fluids.WATER, EquipmentBlockEntity.CONDENSER_TANK);
					}
				}
			}
		}
	}
}
