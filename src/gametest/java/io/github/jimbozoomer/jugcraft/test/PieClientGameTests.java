package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import java.util.ArrayList;
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
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for pie baking: three Hearth Ovens on a brick floor, their mouths to the camera (lit with an apple pie
 * baked golden, lit with a pumpkin cream pie just gone in as pale dough, and cold with a cranberry pie burnt black); a
 * table of pies, whole and with one, two and three slices gone, and a burnt one; and a wall of framed pastry dough, the
 * five raw pies, the five slices and a Hearth Oven. CI job {@code client}.
 */
public class PieClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 16, x + 16, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 16, x + 16, y + 10, z + 8));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			List<String> items = new ArrayList<>(List.of("pastry_dough"));
			for (PieFilling filling : PieFilling.values()) {
				items.add(filling.rawPie());
			}
			items.add("hearth_oven");
			for (PieFilling filling : PieFilling.values()) {
				items.add(filling.slice());
			}
			items.add("burnt_pie");
			for (int i = 0; i < items.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % 7, y + 2 - i / 7, z - 11, items.get(i)));
			}
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 7, y + 3, z + 1, 180, 30, "jugcraft_pie_baking");
			shoot(context, singleplayer, x + 3, y + 1, z - 2, 180, 20, "jugcraft_hearth_ovens");
			shoot(context, singleplayer, x + 11, y + 1, z - 3, 180, 45, "jugcraft_pies");
			shoot(context, singleplayer, x + 4, y + 1, z - 8, 180, 5, "jugcraft_pie_items");
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

	/**
	 * A Hearth Oven at {@code pos} facing the camera, holding {@code filling} baked {@code points}; a lit one has a full
	 * fire but starts cold, so the pie hardly changes while the screenshots are taken.
	 */
	private static void oven(ServerLevel level, BlockPos pos, boolean lit, PieFilling filling, int points) {
		level.setBlock(pos, JugcraftAgriculture.block("hearth_oven").defaultBlockState().setValue(HearthOvenBlock.FACING, Direction.SOUTH)
				.setValue(HearthOvenBlock.LIT, lit), Block.UPDATE_ALL);
		if (level.getBlockEntity(pos) instanceof HearthOvenBlockEntity oven) {
			oven.set(lit ? HearthOvenBlockEntity.MAX_BURN : 0, 0, filling, points);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The bakehouse floor, and the ovens on it.
		for (int dx = 0; dx <= 14; dx++) {
			for (int dz = -8; dz <= -3; dz++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.BRICKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		oven(level, new BlockPos(x + 1, y, z - 5), true, PieFilling.APPLE, HearthOvenBlockEntity.BAKED);
		oven(level, new BlockPos(x + 3, y, z - 5), true, PieFilling.PUMPKIN_CREAM, 0);
		oven(level, new BlockPos(x + 5, y, z - 5), false, PieFilling.CRANBERRY, HearthOvenBlockEntity.BURNT);
		// The table of pies: whole, cut down slice by slice, and burnt.
		String[] pies = {"apple_pie", "pumpkin_cream_pie", "cranberry_pie", "sweet_potato_pie", "chestnut_pie", "burnt_pie"};
		int[] bites = {0, 1, 2, 3, 0, 1};
		for (int i = 0; i < pies.length; i++) {
			BlockPos table = new BlockPos(x + 8 + i, y, z - 5);
			level.setBlock(table, Blocks.STRIPPED_OAK_WOOD.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(table.above(), JugcraftAgriculture.block(pies[i]).defaultBlockState().setValue(PieBlock.BITES, bites[i]),
					Block.UPDATE_ALL);
		}
		// The wall the pastry hangs on.
		for (int dx = 0; dx <= 8; dx++) {
			for (int dy = 0; dy <= 3; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 12), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}
}
