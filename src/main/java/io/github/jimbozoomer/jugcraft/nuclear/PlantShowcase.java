package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Builds all three pre-made nuclear plants side by side, well apart, each on the shore of its own lake, fuelled and
 * running (operators: {@code /nuclear showcase plants}). Started with the environment variable
 * {@code JUGCRAFT_SHOWCASE=nuclear} (14-nuclear-showcase.bat), it also builds them once in a new world when the first
 * player joins, and puts that player in front of them.
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
		if (!"nuclear".equalsIgnoreCase(System.getenv("JUGCRAFT_SHOWCASE"))) {
			return;
		}
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> {
			ServerPlayer player = handler.player;
			Path marker = server.getWorldPath(LevelResource.ROOT).resolve("jugcraft").resolve("nuclear-showcase.done");
			if (Files.exists(marker)) {
				return;
			}
			try {
				Files.createDirectories(marker.getParent());
				Files.writeString(marker, "built\n");
			} catch (java.io.IOException e) {
				Jugcraft.LOGGER.warn("Could not mark the nuclear showcase as built: {}", e.toString());
			}
			ServerLevel level = server.overworld();
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set 6000");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "gamerule doDaylightCycle false");
			server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather clear 1000000");
			BlockPos ground = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
			// The plant under review: only the medium plant for now (all three with /nuclear showcase plants).
			build(level, player, ground, new String[] {"nuclear_medium_pwr"});
		}));
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
			{"Main gate and parking", 150, 1, 196, 0, -1},
			{"Main control room", 151, 15, 104, 0, -1},
			{"Containment: operating floor", 84, 17, 51, 0, 1},
			{"Containment: steam generator level", 83, 9, 48, 0, 1},
			{"Turbine hall: operating floor", 140, 13, 50, 1, 0},
			{"Spent fuel pool", 115, 13, 122, 0, -1},
			{"Intake pump house", 170, 1, 12, 0, -1},
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
