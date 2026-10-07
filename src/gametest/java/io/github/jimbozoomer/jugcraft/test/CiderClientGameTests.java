package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.AppleLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CiderBarrelBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CiderBarrelBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CiderPressBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the cider mill: an apple tree grown from its sapling, its leaves in blossom and hung with apples;
 * two Cider Presses (one with apples waiting and pulp ground, one halfway through pressing with juice in its trough); and
 * three Cider Barrels at each stage, their chalk marks showing. Photographed by day (CI job {@code client}).
 */
public class CiderClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 16, x + 14, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 16, x + 14, y + 14, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 1, z + 2, 180, 10, "jugcraft_cider_mill");
			shoot(context, singleplayer, x - 3, y + 2, z - 3, 180, 15, "jugcraft_apple_tree");
			shoot(context, singleplayer, x + 3, y + 2, z - 6, 180, 50, "jugcraft_cider_presses");
			shoot(context, singleplayer, x + 9, y + 1, z - 5, 180, 20, "jugcraft_cider_barrels");
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

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// An apple tree, its leaves over air blossoming and hung with ripe apples.
		BlockPos sapling = new BlockPos(x - 3, y, z - 9);
		set(level, sapling, JugcraftAgriculture.block("apple_sapling").defaultBlockState());
		JugcraftAgriculture.APPLE_GROWER.growTree(level, level.getChunkSource().getGenerator(), sapling, level.getBlockState(sapling),
				level.getRandom());
		for (BlockPos pos : BlockPos.betweenClosed(sapling.offset(-3, 0, -3), sapling.offset(3, 9, 3))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(JugcraftAgriculture.block("apple_leaves")) && level.isEmptyBlock(pos.below())) {
				int fruit = (pos.getX() + pos.getZ()) % 3 == 0 ? 1 : AppleLeavesBlock.RIPE;
				set(level, pos.immutable(), state.setValue(AppleLeavesBlock.FRUIT, fruit));
			}
		}
		// Two presses: one with apples waiting and some ground, one halfway through pressing a full cheese.
		long time = level.getGameTime() - 1000;
		BlockPos filling = new BlockPos(x + 2, y, z - 8);
		set(level, filling, JugcraftAgriculture.block("cider_press").defaultBlockState().setValue(CiderPressBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(filling) instanceof CiderPressBlockEntity press) {
			for (int i = 0; i < 7; i++) {
				press.addApple();
			}
			for (int i = 0; i < 3; i++) {
				press.grind(time);
			}
		}
		BlockPos pressing = new BlockPos(x + 4, y, z - 8);
		set(level, pressing, JugcraftAgriculture.block("cider_press").defaultBlockState().setValue(CiderPressBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(pressing) instanceof CiderPressBlockEntity press) {
			for (int i = 0; i < CiderPressBlockEntity.CAPACITY; i++) {
				press.addApple();
				press.grind(time);
			}
			press.turn(time);
			press.turn(time);
		}
		// Three barrels: sweet, sparkling and aged.
		long now = level.getGameTime();
		long[] ages = {0, CiderBarrelBlockEntity.SPARKLING_TICKS, CiderBarrelBlockEntity.AGED_TICKS};
		for (int i = 0; i < ages.length; i++) {
			BlockPos pos = new BlockPos(x + 8 + i, y, z - 8);
			set(level, pos, JugcraftAgriculture.block("cider_barrel").defaultBlockState().setValue(CiderBarrelBlock.FACING, Direction.SOUTH));
			if (level.getBlockEntity(pos) instanceof CiderBarrelBlockEntity barrel) {
				for (int serving = 0; serving < 6; serving++) {
					barrel.fill(now);
				}
				barrel.setStarted(now - ages[i]);
			}
		}
		set(level, new BlockPos(x + 8, y, z - 9), Blocks.SPRUCE_PLANKS.defaultBlockState());
	}
}
