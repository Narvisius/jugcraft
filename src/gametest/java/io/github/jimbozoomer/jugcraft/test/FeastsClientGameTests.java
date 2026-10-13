package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FeastBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FeastDish;
import io.github.jimbozoomer.jugcraft.agriculture.FoodDisplayBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShowcaseBlockEntity;
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
 * Client game test for feasts and food displays: a long table laid with the five feasts whole, then eaten down a row
 * at a time to their leftovers; a table of the pies in the owner's art (apple, chocolate, the sweet berry cheesecake and
 * a pumpkin pie set down), whole and cut; the plate, the platter and the serving tray laid with food and a pie; and a
 * wall of the new items in item frames. CI job {@code client}.
 */
public class FeastsClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("roast_chicken", "honey_glazed_ham", "shepherds_pie", "stuffed_pumpkin",
			"gleaming_salad", "bowl_of_roast_chicken", "bowl_of_honey_glazed_ham", "bowl_of_shepherds_pie", "bowl_of_stuffed_pumpkin",
			"bowl_of_gleaming_salad", "apple_pie", "apple_pie_slice", "chocolate_pie", "chocolate_pie_slice", "raw_chocolate_pie",
			"sweet_berry_cheesecake", "sweet_berry_cheesecake_slice", "raw_sweet_berry_cheesecake", "pumpkin_pie_slice", "plate",
			"platter", "serving_tray");
	private static final List<String> PIES = List.of("apple_pie", "chocolate_pie", "sweet_berry_cheesecake", "pumpkin_pie");

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
						.formatted(x + i % 11, y + 3 - i / 11, z - 11, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 2, z - 3, 180, 35, "jugcraft_feasts");
			shoot(context, singleplayer, x + 8, y + 2, z - 5, 180, 35, "jugcraft_feast_pies");
			shoot(context, singleplayer, x + 8, y + 2, z - 1, 180, 45, "jugcraft_food_displays");
			shoot(context, singleplayer, x + 5, y + 2, z - 4, 180, 5, "jugcraft_feast_items");
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

	private static void set(ServerLevel level, BlockPos pos, Block block) {
		level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The dining room floor and the back wall the items hang on.
		for (int dx = -1; dx <= 12; dx++) {
			for (int dz = -12; dz <= -2; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS);
			}
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), Blocks.BRICKS);
			}
		}
		// The feast table: each feast whole at the back, then half eaten, then its leftovers at the front.
		FeastDish[] dishes = FeastDish.values();
		int[] servings = {FeastBlock.SERVINGS, FeastBlock.SERVINGS / 2, 0};
		for (int i = 0; i < dishes.length; i++) {
			for (int row = 0; row < servings.length; row++) {
				BlockPos table = new BlockPos(x + 1 + i, y, z - 9 + row);
				set(level, table, Blocks.STRIPPED_SPRUCE_LOG);
				level.setBlock(table.above(), block(dishes[i].id).defaultBlockState().setValue(FeastBlock.FACING, Direction.SOUTH)
						.setValue(FeastBlock.SERVINGS_LEFT, servings[row]), Block.UPDATE_ALL);
			}
		}
		// The pie table: each pie in the owner's art whole, and half eaten in front.
		for (int i = 0; i < PIES.size(); i++) {
			for (int row = 0; row < 2; row++) {
				BlockPos table = new BlockPos(x + 7 + i, y, z - 9 + row);
				set(level, table, Blocks.SPRUCE_PLANKS);
				level.setBlock(table.above(), block(PIES.get(i)).defaultBlockState().setValue(PieBlock.BITES, row * 2), Block.UPDATE_ALL);
			}
		}
		// The displays: a plate of pie, a platter of roast food, a serving tray with a cake on it.
		List<String> displays = List.of("plate", "platter", "serving_tray");
		List<List<ItemStack>> food = List.of(
				List.of(new ItemStack(JugcraftAgriculture.item("chocolate_pie_slice"))),
				List.of(new ItemStack(Items.COOKED_CHICKEN), new ItemStack(Items.BAKED_POTATO), new ItemStack(Items.BREAD),
						new ItemStack(JugcraftAgriculture.item("cooked_bacon"))),
				List.of(new ItemStack(Items.CAKE), new ItemStack(Items.COOKIE), new ItemStack(JugcraftAgriculture.item("apple_pie")),
						new ItemStack(Items.SWEET_BERRIES)));
		for (int i = 0; i < displays.size(); i++) {
			BlockPos table = new BlockPos(x + 7 + i, y, z - 4);
			set(level, table, Blocks.SPRUCE_PLANKS);
			level.setBlock(table.above(), block(displays.get(i)).defaultBlockState().setValue(FoodDisplayBlock.FACING, Direction.SOUTH),
					Block.UPDATE_ALL);
			if (level.getBlockEntity(table.above()) instanceof ShowcaseBlockEntity shown) {
				for (int place = 0; place < food.get(i).size(); place++) {
					shown.put(place, food.get(i).get(place));
				}
			}
		}
	}
}
