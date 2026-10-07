package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.HighStrikerBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Midway;
import io.github.jimbozoomer.jugcraft.agriculture.PlushBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RingTossBlock;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the fall fair midway (fall addition 26): a booth of striped awning over a counter of plush
 * prizes, two High Strikers (one with its puck half way up and its lamps lit to there, one rung, its bell glowing),
 * and Ring Toss with a ringer on a bottle; up close on the prizes; and at dusk, the lamps alight. CI job {@code client}.
 */
public class MidwayClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat whatever an earlier test in this client left: CI shares the client tests out
			// between parallel jobs, and only the first job's opening test hides them.
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 10, y - 3, z - 16, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 10, y, z - 16, x + 12, y + 12, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 1, y + 2, z - 1, 180, 14, "jugcraft_midway");
			shoot(context, singleplayer, x - 2, y + 1, z - 3, 180, 25, "jugcraft_midway_prizes");
			shoot(context, singleplayer, x + 6, y + 1, z - 4, 180, 28, "jugcraft_ring_toss");
			server.runCommand("time set 13000");
			context.waitTicks(10);
			shoot(context, singleplayer, x + 1, y + 2, z, 180, 12, "jugcraft_midway_dusk");
			server.runCommand("time set noon");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		// Only into air, so standing on the ground leaves no hole in it for the later pictures.
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
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

	private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
		level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
	}

	/** A High Striker at (x, y, z) facing south, its puck at {@code puck}. */
	private static void striker(ServerLevel level, int x, int y, int z, int puck) {
		BlockState state = JugcraftAgriculture.block("high_striker").defaultBlockState().setValue(HighStrikerBlock.FACING, Direction.SOUTH)
				.setValue(HighStrikerBlock.LEVEL, puck);
		for (int part = 0; part < HighStrikerBlock.PARTS; part++) {
			set(level, x, y + part, z, state.setValue(HighStrikerBlock.PART, part));
		}
	}

	/**
	 * The midway: a plank floor; two High Strikers; a prize booth (a counter of planks under a red-and-white awning, the
	 * plushes along it facing south, the jumbo plush on the ground beside it); Ring Toss on a hay bale, a ring on a bottle.
	 */
	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		for (int dx = -8; dx <= 10; dx++) {
			for (int dz = -12; dz <= 2; dz++) {
				set(level, x + dx, y - 1, z + dz, ((dx + dz) & 1) == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState());
			}
		}
		striker(level, x + 1, y, z - 8, 5);
		striker(level, x + 3, y, z - 8, HighStrikerBlock.RUNG);
		// The prize booth: a counter, posts, and a striped awning.
		BlockState red = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("red_wool")).defaultBlockState();
		BlockState white = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_wool")).defaultBlockState();
		for (int dx = -5; dx <= -1; dx++) {
			set(level, x + dx, y, z - 7, Blocks.SPRUCE_PLANKS.defaultBlockState());
			set(level, x + dx, y + 3, z - 7, (dx & 1) == 0 ? red : white);
			set(level, x + dx, y + 3, z - 8, (dx & 1) == 0 ? red : white);
			set(level, x + dx, y, z - 9, Blocks.SPRUCE_PLANKS.defaultBlockState());
		}
		for (int dy = 1; dy <= 2; dy++) {
			set(level, x - 5, y + dy, z - 7, Blocks.SPRUCE_FENCE.defaultBlockState());
			set(level, x - 1, y + dy, z - 7, Blocks.SPRUCE_FENCE.defaultBlockState());
		}
		String[] counter = {"pumpkin_plush", "ghost_plush", "bat_plush"};
		for (int i = 0; i < counter.length; i++) {
			set(level, x - 4 + i, y + 1, z - 7, JugcraftAgriculture.block(counter[i]).defaultBlockState().setValue(PlushBlock.FACING, Direction.SOUTH));
		}
		String[] back = {"black_cat_plush", "squirrel_plush", "werewolf_plush"};
		for (int i = 0; i < back.length; i++) {
			set(level, x - 4 + i, y + 1, z - 9, JugcraftAgriculture.block(back[i]).defaultBlockState().setValue(PlushBlock.FACING, Direction.SOUTH));
		}
		set(level, x - 6, y, z - 6, JugcraftAgriculture.block(Midway.JACKPOT).defaultBlockState().setValue(PlushBlock.FACING, Direction.SOUTH));
		// Ring Toss on a hay bale, a ringer on the middle bottle.
		set(level, x + 6, y, z - 7, Blocks.HAY_BLOCK.defaultBlockState());
		set(level, x + 6, y + 1, z - 7, JugcraftAgriculture.block("ring_toss").defaultBlockState().setValue(RingTossBlock.RINGED, 5));
		set(level, x + 7, y, z - 7, JugcraftAgriculture.block("ring_toss").defaultBlockState());
	}
}
