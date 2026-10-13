package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FeastBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MenuDishes;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
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
 * Client game test for the menu (the kitchen and cooking expansion's slice 3): a long table laid with every dish set
 * down, in the order of tools/menu.py (drinks, soups and stews, plated meals, sandwiches, sweets, corn, pet food, then
 * the foods that now wear the owner's art and the popcorn box), seen whole and from close by; the Cooking Pot in the
 * owner's pot, empty and cooking on a campfire, beside the nachos whole, half eaten and down to the last chip; and a
 * wall of the new items in item frames. CI job {@code client}.
 */
public class MenuClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("hot_cocoa", "creamy_corn_drink", "melon_juice", "glow_berry_custard", "milk_bottle",
			"beef_stew", "chicken_soup", "baked_cod_stew", "fish_stew", "bone_broth", "corn_soup", "noodle_soup", "tomato_sauce", "fruit_salad",
			"nether_salad", "creamed_corn", "bacon_and_eggs", "steak_and_potatoes", "roasted_mutton_chops", "grilled_salmon", "ratatouille",
			"pasta_with_meatballs", "pasta_with_mutton_chop", "squid_ink_pasta", "vegetable_noodles", "cornbread_stuffing", "hamburger",
			"bacon_sandwich", "chicken_sandwich", "egg_sandwich", "mutton_wrap", "taco", "stuffed_potato", "dumplings", "ham", "smoked_ham",
			"barbecue_stick", "corn_dog", "classic_corn_dog", "honey_cookie", "sweet_berry_cookie", "caramel_popcorn", "corn_popsicle",
			"melon_popsicle", "boiled_corn", "cornbread", "tortilla", "tortilla_chip", "wheat_dough", "raw_pasta", "cornbread_batter",
			"tortilla_raw", "corncob", "dog_food", "horse_feed", "onion_soup", "vegetable_soup", "pumpkin_soup", "cabbage_rolls", "roasted_corn",
			"mulled_cider", "nachos", "bowl_of_nachos", "cooking_pot");
	/** Dishes to a row of the table. */
	private static final int ROW = 10;
	/** Items to a row of the wall. */
	private static final int WALL = 13;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 16, x + 18, y - 1, z + 10));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 16, x + 18, y + 10, z + 10));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + i % WALL, y + 4 - i / WALL, z - 11, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			// The pot is cooking first, while it has a recipe in it.
			shoot(context, singleplayer, x + 13, y + 2, z - 1, 0, 40, "jugcraft_menu_pot");
			shoot(context, singleplayer, x + 5, y + 4, z - 2, 0, 45, "jugcraft_menu_table");
			shoot(context, singleplayer, x + 3, y + 2, z - 1, 0, 45, "jugcraft_menu_table_left");
			shoot(context, singleplayer, x + 8, y + 2, z - 1, 0, 45, "jugcraft_menu_table_right");
			shoot(context, singleplayer, x + 5, y + 3, z + 3, 0, 55, "jugcraft_menu_table_back");
			shoot(context, singleplayer, x + 6, y + 2, z - 4, 180, 5, "jugcraft_menu_items");
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
		for (int dx = -1; dx <= 15; dx++) {
			for (int dz = -12; dz <= 8; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), Blocks.SPRUCE_PLANKS);
			}
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), Blocks.BRICKS);
			}
		}
		// The table: every dish set down, facing the camera to the north of it.
		List<MenuDishes.Dish> dishes = MenuDishes.PLACED;
		for (int i = 0; i < dishes.size(); i++) {
			BlockPos table = new BlockPos(x + 1 + i % ROW, y, z + 1 + i / ROW);
			set(level, table, Blocks.STRIPPED_SPRUCE_LOG);
			level.setBlock(table.above(), block(dishes.get(i).id()).defaultBlockState().setValue(PlacedDishBlock.FACING, Direction.NORTH),
					Block.UPDATE_ALL);
		}
		// The Cooking Pot: one on a campfire cooking tomato soup batch after batch, one empty beside it.
		BlockPos fire = new BlockPos(x + 12, y, z + 2);
		level.setBlock(fire, Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
		set(level, fire.above(), block("cooking_pot"));
		if (level.getBlockEntity(fire.above()) instanceof CookingPotBlockEntity pot) {
			pot.setItem(0, new ItemStack(Items.BOWL, 16));
			pot.setItem(1, new ItemStack(JugcraftAgriculture.item("tomato"), 32));
			pot.setItem(2, new ItemStack(JugcraftAgriculture.item("onion"), 16));
		}
		set(level, new BlockPos(x + 14, y, z + 2), Blocks.SPRUCE_PLANKS);
		set(level, new BlockPos(x + 14, y + 1, z + 2), block("cooking_pot"));
		// The nachos: whole, half eaten and down to the last chip.
		int[] servings = {FeastBlock.SERVINGS, FeastBlock.SERVINGS / 2, 0};
		for (int i = 0; i < servings.length; i++) {
			BlockPos table = new BlockPos(x + 12 + i, y, z + 4);
			set(level, table, Blocks.SPRUCE_PLANKS);
			level.setBlock(table.above(), block("nachos").defaultBlockState().setValue(FeastBlock.FACING, Direction.NORTH)
					.setValue(FeastBlock.SERVINGS_LEFT, servings[i]), Block.UPDATE_ALL);
		}
	}
}
