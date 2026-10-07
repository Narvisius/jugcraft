package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for the Agriculture branch: plants a corn maze, a cornfield, a sunflower field,
 * rows of the other crops and every growth stage in a real client, and saves screenshots (CI job
 * {@code client}), so the crops can be judged from actual game renders.
 */
public class AgricultureClientGameTests implements FabricClientGameTest {
	/** '#' is corn, '.' is a grass path. The entrance is at the bottom (south) edge. */
	private static final String[] MAZE = {
			"#############",
			"#.....#.....#",
			"#.###.#.###.#",
			"#.#...#...#.#",
			"#.#.#####.#.#",
			"#...#...#...#",
			"###.#.#.#.###",
			"#.....#.....#",
			"#.#########.#",
			"#...........#",
			"######.######",
	};

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 1, z - 34, x + 30, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 34, x + 30, y + 12, z + 4));
			server.runOnServer(minecraft -> plantFarm(minecraft.overworld(), origin));

			// Keep the chat clear: no feedback from the teleports below (the rule's name differs between
			// versions, so try both), then wait for the setup messages to fade.
			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("tp @p %d %d %d 180 30".formatted(x + 6, y, z));
			context.waitTicks(220);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 6, y + 17, z - 19, 180, 68, "jugcraft_corn_maze");
			shoot(context, singleplayer, x + 6, y, z - 17, 180, -5, "jugcraft_corn_maze_entrance");
			shoot(context, singleplayer, x + 13, y + 8, z + 1, 180, 30, "jugcraft_fall_farm");
			shoot(context, singleplayer, x + 21, y + 1, z - 7, 180, 4, "jugcraft_sunflowers");
			shoot(context, singleplayer, x + 8, y + 1, z - 1, 180, 25, "jugcraft_crop_stages");
		}
	}

	/** Stands the player at a spot (on an invisible barrier if above ground) and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		// The centre of block (x, z) is x + 0.5 even for negative coordinates.
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void plantFarm(ServerLevel level, BlockPos origin) {
		// The maze (x .. x+12, z-30 .. z-20) and a solid cornfield beside it.
		for (int row = 0; row < MAZE.length; row++) {
			for (int col = 0; col < MAZE[row].length(); col++) {
				if (MAZE[row].charAt(col) == '#') {
					tall(level, origin.offset(col, 0, -30 + row), TallCrop.CORN, 7);
				}
			}
		}
		for (int dx = 16; dx <= 27; dx++) {
			for (int dz = -30; dz <= -20; dz++) {
				tall(level, origin.offset(dx, 0, dz), TallCrop.CORN, 7);
			}
			for (int dz = -17; dz <= -12; dz++) {
				tall(level, origin.offset(dx, 0, dz), TallCrop.SUNFLOWER, 7);
			}
		}
		// Rows of the one-block crops.
		String[] rows = {"bean_crop", "bean_crop", "sweet_potato_crop", "sweet_potato_crop", "flax_crop", "flax_crop"};
		for (int i = 0; i < rows.length; i++) {
			for (int dx = 0; dx <= 12; dx++) {
				crop(level, origin.offset(dx, 0, -17 + i), rows[i], i == 4 ? 5 : 7);
			}
		}
		// Every growth stage, youngest on the left.
		for (int age = 0; age <= 7; age++) {
			tall(level, origin.offset(age, 0, -8), TallCrop.CORN, age);
			tall(level, origin.offset(10 + age, 0, -8), TallCrop.SUNFLOWER, age);
			crop(level, origin.offset(age, 0, -5), "bean_crop", age);
			crop(level, origin.offset(10 + age, 0, -5), "sweet_potato_crop", age);
			crop(level, origin.offset(age, 0, -3), "flax_crop", age);
		}
		// The wild plants, on grass.
		String[] wild = {"wild_corn", "wild_sunflower", "wild_beans", "wild_sweet_potato", "wild_flax"};
		for (int i = 0; i < wild.length; i++) {
			level.setBlock(origin.offset(10 + i * 2, 0, -3), JugcraftAgriculture.block(wild[i]).defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static void farmland(ServerLevel level, BlockPos crop) {
		level.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), Block.UPDATE_ALL);
	}

	private static void tall(ServerLevel level, BlockPos pos, TallCrop crop, int age) {
		farmland(level, pos);
		TallCropBlock block = JugcraftAgriculture.TALL_CROPS.get(crop);
		level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
		if (age > 0) {
			block.growTo(level, pos, age);
		}
	}

	private static void crop(ServerLevel level, BlockPos pos, String id, int age) {
		farmland(level, pos);
		level.setBlock(pos, ((CropBlock) JugcraftAgriculture.block(id)).getStateForAge(age), Block.UPDATE_ALL);
	}
}
