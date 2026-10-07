package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for autumn foraging: a forest floor of podzol and moss under a few trees, with a patch of each wild
 * mushroom (chanterelles, porcini, puffballs, fly agarics and glowing jack o'lantern mushrooms) and a fairy ring of fly
 * agarics in a clearing; by day, and at night, when the jack o'lantern mushrooms glow; and the Foraging Basket and the
 * mushroom dishes in frames. CI job {@code client}.
 */
public class ForagingClientGameTests implements FabricClientGameTest {
	private static final int[][] RING = {{3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}, {-2, -2}, {0, -3}, {2, -2}};
	private static final String[] FRAMED = {"foraging_basket", "sauteed_chanterelles", "roasted_porcini", "fried_puffball", "foragers_stew"};

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
			server.runCommand("gamerule minecraft:random_tick_speed 0");
			server.runCommand("fill %d %d %d %d %d %d minecraft:dirt".formatted(x - 8, y - 3, z - 16, x + 14, y - 2, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:podzol".formatted(x - 8, y - 1, z - 16, x + 14, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 16, x + 14, y + 12, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < FRAMED.length; i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 8 + i, y + 1, z - 13, FRAMED[i]));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 2, y + 2, z + 2, 180, 25, "jugcraft_forest_floor");
			shoot(context, singleplayer, x + 1, y + 1, z - 3, 180, 35, "jugcraft_wild_mushrooms");
			shoot(context, singleplayer, x + 10, y + 1, z - 9, 180, 10, "jugcraft_foraging_basket");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 2, y + 3, z + 2, 180, 30, "jugcraft_fairy_ring_night");
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

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static BlockState mushroom(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	/** A small oak: a trunk and a round crown of leaves. */
	private static void tree(ServerLevel level, BlockPos base) {
		for (int dy = 0; dy < 5; dy++) {
			set(level, base.above(dy), Blocks.OAK_LOG.defaultBlockState());
		}
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 3; dy <= 5; dy++) {
					if (Math.abs(dx) + Math.abs(dz) <= (dy == 5 ? 2 : 3) && level.isEmptyBlock(base.offset(dx, dy, dz))) {
						set(level, base.offset(dx, dy, dz), Blocks.OAK_LEAVES.defaultBlockState()
								.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
					}
				}
			}
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		tree(level, new BlockPos(x - 4, y, z - 9));
		tree(level, new BlockPos(x + 6, y, z - 11));
		tree(level, new BlockPos(x - 2, y, z - 14));
		for (int dx = -3; dx <= 3; dx++) {
			set(level, new BlockPos(x + dx, y - 1, z - 5), Blocks.MOSS_BLOCK.defaultBlockState());
		}
		// A patch of each mushroom round the trees' feet.
		int[][] patches = {{-3, -7}, {-1, -8}, {4, -9}, {5, -8}, {-5, -11}, {7, -10}, {-2, -5}, {0, -5}, {2, -6}, {3, -11}};
		String[] kinds = {"chanterelle", "chanterelle", "porcini", "porcini", "fly_agaric", "puffball", "puffball", "jack_o_lantern_mushroom",
				"jack_o_lantern_mushroom", "jack_o_lantern_mushroom"};
		for (int i = 0; i < patches.length; i++) {
			set(level, new BlockPos(x + patches[i][0], y, z + patches[i][1]), mushroom(kinds[i]));
		}
		// The fairy ring, in the clearing.
		BlockPos centre = new BlockPos(x + 2, y, z - 2);
		for (int[] point : RING) {
			set(level, centre.offset(point[0], 0, point[1]), mushroom("fly_agaric"));
		}
		// The wall for the frames.
		for (int dx = 7; dx <= 13; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 14), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
