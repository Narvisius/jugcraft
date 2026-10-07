package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client game test for the wood repaint and the tree roster: every tree the mod adds, grown side by side in summer, with a
 * sample of its wood (log, stripped log, planks, log ends, slab, stairs, fence and gate) on a wall before them, and
 * vanilla's spruce on the wall beside the cedar; the tree roster's batch-1 shapes in a row of their own, each beside its
 * parent (the cedar beside vanilla's spruce, which it replaces in the Wetland), between vanilla's spruce and oak; and the
 * seasonal trees and shapes again in autumn. Screenshots only, for the owner to judge the textures and shapes in game;
 * each tree's logs and leaves are logged (CI job {@code client}).
 */
public class WoodClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-test");
	/** Each wood, the tree feature that grows it and that tree's leaves. */
	private static final List<String[]> TREES = List.of(
			new String[] {"chestnut", "chestnut", "chestnut_leaves"},
			new String[] {"larch", "larch", "larch_needles"},
			new String[] {"maple", "maple", "maple_leaves"},
			new String[] {"aspen", "aspen", "aspen_leaves"},
			new String[] {"fir", "fir", "fir_needles"},
			new String[] {"dead", "dead_tree", ""},
			new String[] {"jacaranda", "jacaranda", "jacaranda_leaves"},
			new String[] {"willow", "willow", "willow_leaves"},
			new String[] {"palm", "palm", "palm_fronds"},
			new String[] {"cypress", "cypress", "cypress_leaves"},
			new String[] {"redwood", "redwood", "redwood_needles"},
			new String[] {"eucalyptus", "eucalyptus", "eucalyptus_leaves"},
			new String[] {"mahogany", "mahogany", "mahogany_leaves"},
			new String[] {"cedar", "cedar", "cedar_leaves"});
	/**
	 * The tree roster's batch 1 (docs/features/trees-batch-1.md): each new shape beside its parent, between vanilla's spruce
	 * and oak for scale, and the cedar beside the vanilla spruce it replaces. A "minecraft:" feature is vanilla's, placed but
	 * not counted.
	 */
	private static final List<String[]> SHAPES = List.of(
			new String[] {"spruce", "minecraft:spruce", ""},
			new String[] {"cedar", "cedar", "cedar_leaves"},
			new String[] {"fir", "fir", "fir_needles"},
			new String[] {"fir", "stunted_fir", "fir_needles"},
			new String[] {"fir", "bog_fir", "fir_needles"},
			new String[] {"fir", "subalpine_fir", "fir_needles"},
			new String[] {"fir", "fir_bush", "fir_needles"},
			new String[] {"larch", "larch", "larch_needles"},
			new String[] {"larch", "tamarack", "larch_needles"},
			new String[] {"dead", "dead_tree", ""},
			new String[] {"dead", "dead_snag", ""},
			new String[] {"dead", "dead_snag_bent", ""},
			new String[] {"willow", "willow", "willow_leaves"},
			new String[] {"willow", "willow_bush", "willow_leaves"},
			new String[] {"aspen", "aspen", "aspen_leaves"},
			new String[] {"aspen", "young_aspen", "aspen_leaves"},
			new String[] {"maple", "big_maple", "maple_leaves"},
			new String[] {"maple", "mossy_maple", "maple_leaves"},
			new String[] {"oak", "minecraft:oak", ""});
	/** The trees and shapes whose leaves turn, grown again in autumn, each shape beside its parent. */
	private static final List<String[]> AUTUMN = List.of(TREES.get(1), shape("tamarack"), TREES.get(2), shape("mossy_maple"),
			TREES.get(3), shape("young_aspen"), TREES.get(7), shape("willow_bush"));
	private static final int SPACING = 8;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:random_tick_speed 0");
			// A grass field, cleared in slices (each fill within the block limit): wide enough for the batch-1 shapes' row
			// to the north, the longest (19 trees, to x + 94).
			int west = x - 54;
			int east = x + 98;
			int north = z - 58;
			int south = z + 50;
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(west, y - 1, north, east, y - 1, south));
			for (int sx = west; sx <= east; sx += 8) {
				server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(sx, y, north, Math.min(east, sx + 7), y + 29, south));
			}
			context.waitTicks(10);

			// Every tree in summer, in a row along z - 20.
			SeasonCalendar.Mode before = server.computeOnServer(minecraft -> JugcraftSeasons.settings().mode());
			server.runOnServer(minecraft -> JugcraftSeasons.setMode(minecraft, SeasonCalendar.Mode.SUMMER));
			int row = z - 20;
			int first = x - 50;
			for (int i = 0; i < TREES.size(); i++) {
				server.runCommand("place feature %s %d %d %d".formatted(feature(TREES.get(i)[1]), first + i * SPACING, y, row));
			}
			// The batch-1 shapes in summer, in a row along z - 50.
			int shapesRow = z - 50;
			for (int i = 0; i < SHAPES.size(); i++) {
				server.runCommand("place feature %s %d %d %d".formatted(feature(SHAPES.get(i)[1]), first + i * SPACING, y, shapesRow));
			}
			// The seasonal trees and shapes again in autumn, in a row along z + 30.
			server.runOnServer(minecraft -> JugcraftSeasons.setMode(minecraft, SeasonCalendar.Mode.AUTUMN));
			int autumnRow = z + 30;
			int autumnFirst = x - 22;
			for (int i = 0; i < AUTUMN.size(); i++) {
				server.runCommand("place feature %s %d %d %d".formatted(feature(AUTUMN.get(i)[1]), autumnFirst + i * SPACING, y, autumnRow));
			}
			server.runOnServer(minecraft -> JugcraftSeasons.setMode(minecraft, before));
			for (int i = 0; i < TREES.size(); i++) {
				String[] tree = TREES.get(i);
				BlockPos trunk = new BlockPos(first + i * SPACING, y, row);
				int[] counts = server.computeOnServer(minecraft -> count(minecraft.overworld(), trunk, tree));
				LOGGER.info("[wood] {} grown: {} logs, {} leaves", tree[0], counts[0], counts[1]);
			}
			for (int i = 0; i < SHAPES.size(); i++) {
				String[] shape = SHAPES.get(i);
				if (shape[1].startsWith("minecraft:")) {
					continue;
				}
				BlockPos trunk = new BlockPos(first + i * SPACING, y, shapesRow);
				int[] counts = server.computeOnServer(minecraft -> count(minecraft.overworld(), trunk, shape));
				LOGGER.info("[wood] {} grown: {} logs, {} leaves", shape[1], counts[0], counts[1]);
			}

			// A sample of each wood on a wall facing the cameras, along z + 6, and vanilla's spruce last, beside the cedar
			// (the vanilla wood the cedar's colour was moved off, NATURAL_TEXTURES.md rule 3).
			int wall = z + 6;
			for (int i = 0; i < TREES.size(); i++) {
				sample(server, "jugcraft:" + TREES.get(i)[0], first + i * 4, y, wall);
			}
			sample(server, "minecraft:spruce", first + TREES.size() * 4, y, wall);
			context.waitTicks(20);

			// Hide the HUD, hand and chat whatever an earlier test in this client left (as OfrendaClientGameTests does).
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			// The summer trees, three or so to a shot, from in front and a little below their crowns.
			int[][] groups = {{0, 2}, {3, 5}, {6, 8}, {9, 10}, {11, 13}};
			for (int g = 0; g < groups.length; g++) {
				int middle = first + (groups[g][0] + groups[g][1]) * SPACING / 2;
				shoot(context, singleplayer, middle, y + 6, row + 18, 180, -10, "jugcraft_wood_trees_" + (g + 1));
			}
			// The batch-1 shapes, three or four to a shot, the same way: vanilla's spruce, the cedar and the firs; the firs;
			// the fir bush, larch and tamarack; the dead trees; the willows and aspens; the maples and vanilla's oak.
			int[][] shapeGroups = {{0, 3}, {2, 5}, {5, 8}, {9, 11}, {12, 15}, {16, 18}};
			for (int g = 0; g < shapeGroups.length; g++) {
				int middle = first + (shapeGroups[g][0] + shapeGroups[g][1]) * SPACING / 2;
				shoot(context, singleplayer, middle, y + 6, shapesRow + 18, 180, -10, "jugcraft_wood_shapes_" + (g + 1));
			}
			// The wood samples close up, five walls (or four, or five) to a shot, and the mahogany, cedar and vanilla
			// spruce together.
			int[][] samples = {{0, 4}, {5, 8}, {9, 13}, {12, 14}};
			for (int s = 0; s < samples.length; s++) {
				int middle = first + (samples[s][0] + samples[s][1]) * 4 / 2 + 1;
				shoot(context, singleplayer, middle, y + 2, wall + 8, 180, 8, "jugcraft_wood_samples_" + (s + 1));
			}
			// The seasonal trees and shapes in autumn, four to a shot.
			shoot(context, singleplayer, autumnFirst + 12, y + 6, autumnRow + 18, 180, -10, "jugcraft_wood_autumn_1");
			shoot(context, singleplayer, autumnFirst + 44, y + 6, autumnRow + 18, 180, -10, "jugcraft_wood_autumn_2");
		}
	}

	/** A tree feature's ID: Jugcraft's unless it names vanilla's. */
	private static String feature(String id) {
		return id.contains(":") ? id : "jugcraft:" + id;
	}

	/** A batch-1 row's entry by its feature. */
	private static String[] shape(String feature) {
		return SHAPES.stream().filter(entry -> entry[1].equals(feature)).findFirst().orElseThrow();
	}

	/**
	 * A wood's sample ({@code wood} with its namespace): log, stripped log and planks up the face, a log end, a stripped
	 * end and a slab on top, and stairs, a fence and a gate before it.
	 */
	private static void sample(TestServerContext server, String wood, int at, int y, int wall) {
		String namespace = wood.substring(0, wood.indexOf(':') + 1);
		String stripped = namespace + "stripped_" + wood.substring(namespace.length());
		for (int up = 0; up < 2; up++) {
			set(server, at, y + up, wall, wood + "_log[axis=y]");
			set(server, at + 1, y + up, wall, stripped + "_log[axis=y]");
			set(server, at + 2, y + up, wall, wood + "_planks");
		}
		set(server, at, y + 2, wall, wood + "_log[axis=z]");
		set(server, at + 1, y + 2, wall, stripped + "_log[axis=z]");
		set(server, at + 2, y + 2, wall, wood + "_slab[type=bottom]");
		set(server, at, y, wall + 1, wood + "_stairs[facing=north]");
		set(server, at + 1, y, wall + 1, wood + "_fence");
		set(server, at + 2, y, wall + 1, wood + "_fence_gate[facing=south]");
	}

	private static void set(TestServerContext server, int x, int y, int z, String block) {
		server.runCommand("setblock %d %d %d %s".formatted(x, y, z, block));
	}

	/** Logs and leaves of `tree` within its reach of the trunk. */
	private static int[] count(ServerLevel level, BlockPos trunk, String[] tree) {
		Block log = JugcraftAgriculture.block(tree[0] + "_log");
		Block leaves = tree[2].isEmpty() ? null : JugcraftAgriculture.block(tree[2]);
		int[] counts = new int[2];
		for (BlockPos pos : BlockPos.betweenClosed(trunk.offset(-4, 0, -4), trunk.offset(4, 26, 4))) {
			BlockState state = level.getBlockState(pos);
			counts[0] += state.is(log) ? 1 : 0;
			counts[1] += leaves != null && state.is(leaves) ? 1 : 0;
		}
		return counts;
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw,
			int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}
}
