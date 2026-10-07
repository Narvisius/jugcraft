package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.client.CookingPotScreen;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for the Kitchen Garden: plants tomatoes on trellises, rows of the new crops,
 * every growth stage and a Cooking Pot on a campfire in a real client, opens the pot's screen and
 * saves screenshots (CI job {@code client}).
 */
public class KitchenGardenClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 1, z - 30, x + 34, y - 1, z + 4));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 30, x + 34, y + 12, z + 4));
			server.runOnServer(minecraft -> plantGarden(minecraft.overworld(), origin));

			server.runCommand("gamerule sendCommandFeedback false");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("tp @p %d %d %d 180 30".formatted(x + 6, y, z));
			context.waitTicks(220);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 14, y + 9, z + 4, 180, 35, "jugcraft_kitchen_garden");
			shoot(context, singleplayer, x + 5, y, z - 13, 180, 2, "jugcraft_tomato_trellis");
			shoot(context, singleplayer, x + 12, y + 3, z + 2, 180, 35, "jugcraft_kitchen_stages");
			shoot(context, singleplayer, x + 22, y + 1, z - 8, 180, 40, "jugcraft_cooking_pot");

			// The pot's screen: stand by it and use it.
			context.getInput().lookAt(origin.offset(22, 0, -10));
			context.waitTick();
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(CookingPotScreen.class);
			context.takeScreenshot("jugcraft_cooking_pot_screen");
			context.setScreen(() -> null);
		}
	}

	/** Stands the player at a spot (on an invisible barrier) and takes a screenshot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z,
			int yaw, int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	private static void plantGarden(ServerLevel level, BlockPos origin) {
		// Tomatoes on two-high trellises: an empty trellis row, then two ripe rows and a green one.
		for (int dx = 0; dx <= 10; dx++) {
			trellis(level, origin.offset(dx, 0, -25), 2);
			tomato(level, origin.offset(dx, 0, -22), 7);
			tomato(level, origin.offset(dx, 0, -19), 6);
			tomato(level, origin.offset(dx, 0, -16), 7);
		}
		// Rows of the one-block crops, and peppers.
		String[] rows = {"onion_crop", "garlic_crop", "cabbage_crop"};
		for (int dx = 0; dx <= 10; dx++) {
			pepper(level, origin.offset(dx, 0, -11), 7);
			for (int i = 0; i < rows.length; i++) {
				crop(level, origin.offset(dx, 0, -10 + i), rows[i], 7);
			}
		}
		// A field of oats and a field of barley.
		for (int dx = 14; dx <= 19; dx++) {
			for (int dz = -24; dz <= -16; dz++) {
				crop(level, origin.offset(dx, 0, dz), "oat_crop", 7);
				crop(level, origin.offset(dx + 8, 0, dz), "barley_crop", 7);
			}
		}
		// Every growth stage, youngest on the left.
		for (int age = 0; age <= 7; age++) {
			tomato(level, origin.offset(age, 0, -7), age);
			pepper(level, origin.offset(9 + age, 0, -7), age);
			crop(level, origin.offset(age, 0, -5), "cabbage_crop", age);
			crop(level, origin.offset(9 + age, 0, -5), "oat_crop", age);
			crop(level, origin.offset(18 + age, 0, -5), "barley_crop", age);
			crop(level, origin.offset(age, 0, -3), "onion_crop", age);
			crop(level, origin.offset(9 + age, 0, -3), "garlic_crop", age);
		}
		String[] wild = {"wild_tomato", "wild_pepper", "wild_onion", "wild_garlic", "wild_cabbage", "wild_oats", "wild_barley"};
		for (int i = 0; i < wild.length; i++) {
			level.setBlock(origin.offset(18 + i * 2, 0, -3), JugcraftAgriculture.block(wild[i]).defaultBlockState(), Block.UPDATE_ALL);
		}
		// A Cooking Pot on a campfire, cooking chili, with a finished bowl waiting.
		BlockPos pot = origin.offset(22, 0, -10);
		level.setBlock(pot.below(), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(pot, JugcraftAgriculture.block("cooking_pot").defaultBlockState(), Block.UPDATE_ALL);
		if (level.getBlockEntity(pot) instanceof CookingPotBlockEntity entity) {
			entity.setItem(0, new ItemStack(Items.BOWL, 4));
			entity.setItem(1, new ItemStack(JugcraftAgriculture.item("beans"), 4));
			entity.setItem(2, new ItemStack(JugcraftAgriculture.item("tomato"), 4));
			entity.setItem(3, new ItemStack(JugcraftAgriculture.item("pepper"), 4));
			entity.setItem(4, new ItemStack(JugcraftAgriculture.item("onion"), 4));
			entity.setItem(5, new ItemStack(Items.BEEF, 4));
			entity.setItem(CookingPotBlockEntity.RESULT, new ItemStack(JugcraftAgriculture.item("chili")));
		}
	}

	private static void farmland(ServerLevel level, BlockPos crop) {
		level.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7), Block.UPDATE_ALL);
	}

	private static void trellis(ServerLevel level, BlockPos pos, int height) {
		farmland(level, pos);
		for (int i = 0; i < height; i++) {
			level.setBlock(pos.above(i), JugcraftAgriculture.block("trellis").defaultBlockState(), Block.UPDATE_ALL);
		}
	}

	private static void tomato(ServerLevel level, BlockPos pos, int age) {
		trellis(level, pos, 2);
		TallCropBlock block = JugcraftAgriculture.TALL_CROPS.get(TallCrop.TOMATO);
		level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
		if (age > 0) {
			block.growTo(level, pos, age);
		}
	}

	private static void pepper(ServerLevel level, BlockPos pos, int age) {
		farmland(level, pos);
		level.setBlock(pos, JugcraftAgriculture.TALL_CROPS.get(TallCrop.PEPPER).defaultBlockState().setValue(TallCropBlock.AGE, age),
				Block.UPDATE_ALL);
	}

	private static void crop(ServerLevel level, BlockPos pos, String id, int age) {
		farmland(level, pos);
		level.setBlock(pos, ((CropBlock) JugcraftAgriculture.block(id)).getStateForAge(age), Block.UPDATE_ALL);
	}
}
