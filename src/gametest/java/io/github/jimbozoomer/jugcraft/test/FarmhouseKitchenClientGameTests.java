package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CuttingBoardBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenCabinetBlock;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenStoveBlock;
import io.github.jimbozoomer.jugcraft.agriculture.KitchenStoveBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.SkilletBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SkilletBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Client game test for the Farmhouse Kitchen: a counter of kitchen cabinets in every wood (every other one open) with
 * cutting boards on it; in front, four Kitchen Stoves facing the camera (lit with six foods on its hob, lit with a skillet
 * of beef, lit with a Cooking Pot, and out); and a wall of the knives and the new foods in item frames. CI job
 * {@code client}.
 */
public class FarmhouseKitchenClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("flint_knife", "iron_knife", "bronze_knife", "golden_knife", "steel_knife",
			"diamond_knife", "netherite_knife", "fried_egg", "bacon", "cooked_bacon", "minced_beef", "beef_patty", "chicken_cuts",
			"cooked_chicken_cuts", "mutton_chops", "cooked_mutton_chops", "cod_slice", "cooked_cod_slice", "salmon_slice",
			"cooked_salmon_slice", "cabbage_leaf", "pumpkin_slice", "cake_slice");

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
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + i % 12, y + 3 - i / 12, z - 10, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			// The hob cooks its food in 300 ticks, so the stoves are shot first.
			shoot(context, singleplayer, x + 4, y + 1, z - 3, 180, 35, "jugcraft_kitchen_stoves");
			shoot(context, singleplayer, x + 6, y + 2, z + 1, 180, 20, "jugcraft_farmhouse_kitchen");
			shoot(context, singleplayer, x + 5, y + 1, z - 4, 180, 15, "jugcraft_kitchen_cabinets");
			shoot(context, singleplayer, x + 6, y + 2, z - 2, 180, 5, "jugcraft_kitchen_items");
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
		context.waitTicks(10);
		context.takeScreenshot(name);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void stove(ServerLevel level, BlockPos pos, boolean lit) {
		level.setBlock(pos, block("kitchen_stove").defaultBlockState().setValue(KitchenStoveBlock.FACING, Direction.SOUTH)
				.setValue(KitchenStoveBlock.LIT, lit), Block.UPDATE_ALL);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The kitchen floor and the back wall the knives and food hang on.
		for (int dx = -1; dx <= 12; dx++) {
			for (int dz = -11; dz <= -2; dz++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int dy = 0; dy <= 4; dy++) {
				level.setBlock(new BlockPos(x + dx, y + dy, z - 11), Blocks.BRICKS.defaultBlockState(), Block.UPDATE_ALL);
			}
		}
		// The counter: a cabinet in every wood, every other one open, with cutting boards on it.
		for (int i = 0; i < JugcraftAgriculture.CABINET_WOODS.size(); i++) {
			level.setBlock(new BlockPos(x + i, y, z - 9), block(JugcraftAgriculture.CABINET_WOODS.get(i) + "_cabinet").defaultBlockState()
					.setValue(KitchenCabinetBlock.FACING, Direction.SOUTH).setValue(KitchenCabinetBlock.OPEN, i % 2 == 1), Block.UPDATE_ALL);
		}
		List<ItemStack> onBoards = List.of(new ItemStack(Items.PORKCHOP), new ItemStack(Items.SALMON), new ItemStack(Items.CAKE),
				new ItemStack(JugcraftAgriculture.item("cabbage")));
		for (int i = 0; i < onBoards.size(); i++) {
			BlockPos board = new BlockPos(x + 1 + 2 * i, y + 1, z - 9);
			level.setBlock(board, block("cutting_board").defaultBlockState().setValue(CuttingBoardBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
			if (level.getBlockEntity(board) instanceof CuttingBoardBlockEntity entity) {
				entity.put(onBoards.get(i));
			}
		}
		// The stoves: a full hob, a skillet of beef, a Cooking Pot, and one out.
		BlockPos hob = new BlockPos(x + 2, y, z - 6);
		stove(level, hob, true);
		if (level.getBlockEntity(hob) instanceof KitchenStoveBlockEntity stove) {
			for (ItemStack food : List.of(new ItemStack(Items.BEEF), new ItemStack(Items.PORKCHOP), new ItemStack(Items.CHICKEN),
					new ItemStack(Items.COD), new ItemStack(Items.SALMON), new ItemStack(Items.POTATO))) {
				stove.place(level, food, null);
			}
		}
		BlockPos pan = new BlockPos(x + 4, y, z - 6);
		stove(level, pan, true);
		level.setBlock(pan.above(), block("skillet").defaultBlockState().setValue(SkilletBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
		if (level.getBlockEntity(pan.above()) instanceof SkilletBlockEntity skillet) {
			skillet.add(level, new ItemStack(Items.BEEF, 4), null);
		}
		BlockPos pot = new BlockPos(x + 6, y, z - 6);
		stove(level, pot, true);
		level.setBlock(pot.above(), block("cooking_pot").defaultBlockState(), Block.UPDATE_ALL);
		stove(level, new BlockPos(x + 8, y, z - 6), false);
	}
}
