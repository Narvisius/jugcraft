package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CornMazeGateBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CornMazeGateBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/**
 * Client game test for the corn maze: a medium maze (15 by 15) planted from its gate on a grass field, photographed from
 * above, where its paths show, and from the gate looking in between the corn (CI job {@code client}).
 */
public class MazeClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 12, y - 3, z - 22, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 22, x + 12, y + 20, z + 6));
			context.waitTicks(10);
			BlockPos gate = new BlockPos(x, y, z - 3);
			server.runOnServer(minecraft -> plant(minecraft.overworld(), gate));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			shoot(context, singleplayer, x, y + 16, z + 2, 180, 55, "jugcraft_corn_maze");
			shoot(context, singleplayer, x, y, z - 1, 180, 8, "jugcraft_corn_maze_gate");
		}
	}

	/** A medium maze, from a fixed seed, planted from a gate at {@code pos} facing north. */
	private static void plant(ServerLevel level, BlockPos pos) {
		level.setBlock(pos, JugcraftAgriculture.block("corn_maze_gate").defaultBlockState().setValue(CornMazeGateBlock.FACING, Direction.NORTH),
				Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof CornMazeGateBlockEntity gate) {
			while (gate.size() != 2) {
				gate.nextSize();
			}
			List<BlockPos> columns = gate.survey(level, 1031L);
			if (columns != null) {
				gate.plant(level, 1031L, columns);
			}
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		place(context, singleplayer, x, y, z, yaw, pitch);
		context.waitTicks(20);
		context.takeScreenshot(name);
	}
}
