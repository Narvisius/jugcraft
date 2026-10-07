package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastTableBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the harvest feast: a Harvest Feast Table four lengths long (legs at its ends), set with eight
 * dishes heaped to different heights, with hay bale seats along both sides, photographed by day (CI job
 * {@code client}).
 */
public class FeastClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 14, x + 10, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 14, x + 10, y + 8, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), new BlockPos(x, y, z - 6)));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			shoot(context, singleplayer, x - 2, y + 2, z - 2, 215, 32, "jugcraft_harvest_feast");
			shoot(context, singleplayer, x + 2, y + 1, z - 4, 180, 45, "jugcraft_feast_dishes");
		}
	}

	private static void build(ServerLevel level, BlockPos start) {
		BlockState length = JugcraftAgriculture.block("feast_table").defaultBlockState().setValue(FeastTableBlock.AXIS, Direction.Axis.X);
		for (int i = 0; i < 4; i++) {
			level.setBlock(start.east(i), length, Block.UPDATE_ALL);
		}
		for (int i = 0; i < 4; i++) {
			BlockPos at = start.east(i);
			level.setBlock(at, Block.updateFromNeighbourShapes(level.getBlockState(at), level, at), Block.UPDATE_ALL);
		}
		Item[] foods = {Items.BREAD, JugcraftAgriculture.item("roasted_corn"), Items.PUMPKIN_PIE, Items.COOKED_CHICKEN, Items.APPLE,
				JugcraftAgriculture.item("chestnut_mooncake"), Items.BAKED_POTATO, Items.COOKIE};
		int[] servings = {8, 5, 2, 6, 7, 3, 4, 8};
		for (int i = 0; i < foods.length; i++) {
			if (level.getBlockEntity(start.east(i / 2)) instanceof FeastTableBlockEntity table) {
				table.serve(i % 2, new ItemStack(foods[i], servings[i]));
			}
		}
		BlockState seat = JugcraftAgriculture.block("hay_bale_seat").defaultBlockState();
		for (int i = 0; i < 4; i++) {
			level.setBlock(start.east(i).north(), seat, Block.UPDATE_ALL);
			level.setBlock(start.east(i).south(), seat, Block.UPDATE_ALL);
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
