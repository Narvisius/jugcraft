package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalWallBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client game test for diagonal connections: in a creative world, rows of fences, panes, bars and walls run diagonally
 * (oak and bamboo fences in zigzags, a wrought-iron cemetery fence and an aspen fence in diamonds, glass panes, stained
 * glass, iron and copper bars on a slant; a cobblestone wall diamond, a stone brick wall run with a torch on its middle,
 * an andesite wall zigzag and a mossy stone brick wall run two high, its lower walls tall), photographed from above and from the side (CI job
 * {@code client}). The log lists each block's joins and each wall's post, so a screenshot can be read against them.
 */
public class DiagonalClientGameTests implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-diagonal-client-tests");

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 3, z - 30, x + 30, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 30, x + 30, y + 10, z + 6));
			context.waitTicks(10);
			List<String> joins = server.computeOnServer(minecraft -> build(minecraft.overworld(), origin));
			LOGGER.info("Diagonal scene joins: {}", joins);
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 13, y + 14, z - 12, 0, 90, "jugcraft_diagonal_from_above");
			shoot(context, singleplayer, x + 13, y + 5, z + 4, 180, 30, "jugcraft_diagonal_fences");
			shoot(context, singleplayer, x + 16, y + 5, z - 2, 180, 30, "jugcraft_diagonal_panes_and_bars");
			shoot(context, singleplayer, x + 6, y + 4, z - 12, 180, 30, "jugcraft_diagonal_cemetery_fence");
			shoot(context, singleplayer, x + 21, y + 13, z - 21, 0, 90, "jugcraft_diagonal_walls_from_above");
			shoot(context, singleplayer, x + 20, y + 5, z - 11, 180, 30, "jugcraft_diagonal_walls");
			shoot(context, singleplayer, x + 16, y + 4, z - 19, 180, 25, "jugcraft_diagonal_walls_two_high");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static Block vanilla(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
	}

	/** Places `block` along `cells` (x, z offsets) one by one, as the world sets blocks, so each joins the last. */
	private static void run(ServerLevel level, BlockPos origin, Block block, int[][] cells) {
		for (int[] cell : cells) {
			level.setBlock(origin.offset(cell[0], 0, cell[1]), block.defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static List<String> build(ServerLevel level, BlockPos origin) {
		// A zigzag: two steps north-east, one straight east, two steps south-east, and so on.
		int[][] zigzag = {{0, 0}, {1, -1}, {2, -2}, {3, -2}, {4, -1}, {5, 0}, {6, 0}, {7, -1}, {8, -2}};
		run(level, origin.offset(9, 0, 0), Blocks.OAK_FENCE, zigzag);
		run(level, origin.offset(9, 0, -4), Blocks.BAMBOO_FENCE, zigzag);
		// Diamonds: four diagonal sides meeting at four corners.
		int[][] diamond = {{2, 0}, {1, -1}, {0, -2}, {1, -3}, {2, -4}, {3, -3}, {4, -2}, {3, -1}};
		run(level, origin.offset(1, 0, -16), JugcraftAgriculture.block("cemetery_fence"), diamond);
		run(level, origin.offset(7, 0, -16), BuiltInRegistries.BLOCK.getValue(Jugcraft.id("aspen_fence")), diamond);
		// Panes and bars on a slant.
		int[][] slant = {{0, 0}, {1, -1}, {2, -2}, {3, -3}};
		run(level, origin.offset(9, 0, -9), Blocks.GLASS_PANE, slant);
		run(level, origin.offset(13, 0, -9), vanilla("light_blue_stained_glass_pane"), slant);
		run(level, origin.offset(17, 0, -9), Blocks.IRON_BARS, slant);
		run(level, origin.offset(21, 0, -9), vanilla("copper_bars"), slant);
		// Walls: a diamond, a straight diagonal run with a torch on its middle, a zigzag, and a run two high.
		run(level, origin.offset(14, 0, -16), vanilla("cobblestone_wall"), diamond);
		int[][] line = {{0, 0}, {1, -1}, {2, -2}, {3, -3}, {4, -4}};
		run(level, origin.offset(20, 0, -16), vanilla("stone_brick_wall"), line);
		level.setBlock(origin.offset(22, 1, -18), Blocks.TORCH.defaultBlockState(), Block.UPDATE_ALL);
		run(level, origin.offset(20, 0, -23), vanilla("andesite_wall"), zigzag);
		run(level, origin.offset(14, 0, -23), vanilla("mossy_stone_brick_wall"), slant);
		run(level, origin.offset(14, 1, -23), vanilla("mossy_stone_brick_wall"), slant);
		List<String> joins = new ArrayList<>();
		for (int dy = 0; dy <= 1; dy++) {
			for (int dx = 0; dx <= 30; dx++) {
				for (int dz = -28; dz <= 2; dz++) {
					BlockPos pos = origin.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					int mask = DiagonalConnections.mask(state);
					if (mask != 0) {
						String post = state.hasProperty(WallBlock.UP) ? state.getValue(WallBlock.UP) ? "+post" : "-post" : "";
						post += state.hasProperty(DiagonalWallBlock.TALL) && state.getValue(DiagonalWallBlock.TALL) ? "+tall" : "";
						joins.add("%d,%d,%d=%s%s".formatted(dx, dy, dz, Integer.toBinaryString(mask), post));
					}
				}
			}
		}
		return joins;
	}
}
