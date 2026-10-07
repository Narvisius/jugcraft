package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BatHouseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BatHouseBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for the Bat House: four houses on a barn wall, their ledges holding no guano, a little, more and a
 * pile, a pile of Bat Guano in a frame, and from above, the guano in their trays; then at dusk, the houses letting their
 * bats out. CI job {@code client}.
 */
public class BatHouseClientGameTests implements FabricClientGameTest {
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
			server.runCommand("time set 12000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 12, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 12, x + 12, y + 10, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:bat_guano\",count:1}}"
					.formatted(x + 9, y + 1, z - 6));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 1, z - 2, 180, 0, "jugcraft_bat_houses");
			// From above and close, to see the guano piling up in the trays.
			shoot(context, singleplayer, x + 4, y + 4, z - 3, 180, 45, "jugcraft_bat_house_guano");
			server.runCommand("time set 13200");
			server.runOnServer(minecraft -> {
				ServerLevel level = minecraft.overworld();
				for (int i = 0; i < 4; i++) {
					if (level.getBlockEntity(new BlockPos(x + 1 + 2 * i, y + 3, z - 6)) instanceof BatHouseBlockEntity house) {
						house.set(BatHouseBlockEntity.CAPACITY, house.guano());
						house.release(level);
					}
				}
			});
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_bat_houses_dusk");
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

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A barn wall of pale birch, four houses hung on it, each with more guano on its ledge.
		for (int dx = 0; dx <= 10; dx++) {
			for (int dy = 0; dy <= 5; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 7), Blocks.BIRCH_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		for (int i = 0; i < 4; i++) {
			BlockPos pos = new BlockPos(x + 1 + 2 * i, y + 3, z - 6);
			level.setBlock(pos, JugcraftAgriculture.block("bat_house").defaultBlockState().setValue(BatHouseBlock.FACING, Direction.SOUTH),
					Block.UPDATE_ALL);
			if (level.getBlockEntity(pos) instanceof BatHouseBlockEntity house) {
				house.set(i, new int[] {0, 3, 8, 16}[i]);
			}
		}
	}
}
