package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CanningKettleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PreserveJarItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the preserves pantry: a Canning Kettle of boiling water over a campfire with four jars in its rack,
 * and two Pantry Shelves of jars, sealed with their gingham caps and not (and one empty Mason Jar). Photographed by day
 * (CI job {@code client}).
 */
public class PantryClientGameTests implements FabricClientGameTest {
	private static final List<String> PRESERVES = List.of("sweet_berry_jam", "apple_butter", "pumpkin_butter", "cranberry_preserves",
			"glow_berry_jelly", "pickled_beets", "pickled_peppers", "corn_relish");

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 12, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 12, x + 12, y + 8, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 1, z - 2, 180, 15, "jugcraft_preserves_pantry");
			shoot(context, singleplayer, x + 1, y + 2, z - 4, 180, 45, "jugcraft_canning_kettle");
			shoot(context, singleplayer, x + 4, y, z - 4, 180, 20, "jugcraft_pantry_shelves");
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

	private static ItemStack jar(ServerLevel level, String preserve, boolean sealed) {
		ItemStack jar = new ItemStack(JugcraftAgriculture.item(preserve));
		PreserveJarItem.cooked(jar, level.getGameTime());
		if (sealed) {
			PreserveJarItem.seal(jar);
		}
		return jar;
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// A kettle at the boil over a campfire, four jars in its rack.
		BlockPos kettle = new BlockPos(x + 1, y + 1, z - 6);
		set(level, kettle.below(), Blocks.CAMPFIRE.defaultBlockState());
		set(level, kettle, JugcraftAgriculture.block("canning_kettle").defaultBlockState());
		if (level.getBlockEntity(kettle) instanceof CanningKettleBlockEntity pot) {
			pot.fill();
			for (int i = 0; i < 4; i++) {
				pot.add(jar(level, PRESERVES.get(i), false));
			}
			pot.boil();
		}
		// Two shelves of jars, against a wall of planks.
		for (int i = 0; i < 2; i++) {
			BlockPos pos = new BlockPos(x + 3 + i, y, z - 7);
			set(level, pos, JugcraftAgriculture.block("pantry_shelf").defaultBlockState().setValue(PantryShelfBlock.FACING, Direction.SOUTH));
			set(level, pos.north(), Blocks.SPRUCE_PLANKS.defaultBlockState());
			if (level.getBlockEntity(pos) instanceof PantryShelfBlockEntity shelf) {
				for (int slot = 0; slot < PantryShelfBlockEntity.SLOTS; slot++) {
					String preserve = PRESERVES.get((i * PantryShelfBlockEntity.SLOTS + slot) % PRESERVES.size());
					shelf.store(slot == 5 && i == 1 ? new ItemStack(JugcraftAgriculture.item("mason_jar")) : jar(level, preserve, slot % 2 == 0));
				}
			}
		}
	}
}
