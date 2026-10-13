package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BasketBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OrganicCompostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RichFarmlandBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Client game test for soil, compost and storage (the kitchen and cooking expansion's slice 5): a bed of Rich Soil, a
 * plot of Rich Soil Farmland (dry, and moist by a water channel) growing wheat and corn, Organic Compost at each of its
 * stages, the seven produce crates and the Bag of Corn Kernels, and the wooden and bamboo baskets (one holding carrots);
 * then a wall of the slice's items in item frames. CI job {@code client}.
 */
public class SoilClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("rich_soil", "organic_compost", "beetroot_crate", "cabbage_crate", "carrot_crate",
			"corn_crate", "onion_crate", "potato_crate", "tomato_crate", "corn_kernel_bag", "wooden_basket", "bamboo_basket");
	private static final List<String> CRATES = List.of("beetroot_crate", "cabbage_crate", "carrot_crate", "corn_crate", "onion_crate",
			"potato_crate", "tomato_crate");
	/** Items to a row of the wall. */
	private static final int WALL = 6;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 16, x + 18, y - 1, z + 16));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 16, x + 18, y + 10, z + 16));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// The crops go in once the light has reached the new ground: a crop needs light 8 to stay.
			context.waitTicks(20);
			server.runOnServer(minecraft -> plant(minecraft.overworld(), origin));
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % WALL, y + 3 - i / WALL, z - 11, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 3, z - 2, 0, 40, "jugcraft_soil_garden");
			shoot(context, singleplayer, x + 4, y + 3, z + 4, 0, 45, "jugcraft_soil_storage");
			shoot(context, singleplayer, x + 4, y + 7, z - 4, 0, 55, "jugcraft_soil_overview");
			shoot(context, singleplayer, x + 3, y + 2, z - 5, 180, 5, "jugcraft_soil_items");
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
		// The back wall the items hang on.
		for (int dx = -1; dx <= 8; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), Blocks.BRICKS);
			}
		}
		// The garden: a row of Rich Soil, then the farmland, dry on the left and moist on the right of a water channel.
		for (int dx = 0; dx <= 8; dx++) {
			set(level, new BlockPos(x + dx, y - 1, z + 1), block("rich_soil"));
			boolean moist = dx >= 5;
			if (dx == 4) {
				set(level, new BlockPos(x + dx, y - 1, z + 2), Blocks.WATER);
				set(level, new BlockPos(x + dx, y - 1, z + 3), Blocks.WATER);
				continue;
			}
			for (int row = 2; row <= 3; row++) {
				level.setBlock(new BlockPos(x + dx, y - 1, z + row), block("rich_soil_farmland").defaultBlockState()
						.setValue(RichFarmlandBlock.MOISTURE, moist ? RichFarmlandBlock.MAX_MOISTURE : 0), Block.UPDATE_ALL);
			}
		}
		// The compost heap at each of its stages, then Rich Soil, what it becomes.
		for (int stage = 0; stage <= OrganicCompostBlock.LAST_STAGE; stage++) {
			level.setBlock(new BlockPos(x + stage, y, z + 5), block("organic_compost").defaultBlockState().setValue(OrganicCompostBlock.COMPOSTING, stage),
					Block.UPDATE_ALL);
		}
		set(level, new BlockPos(x + 4, y, z + 5), block("rich_soil"));
		// The storehouse: the crates, the kernel bag, and the baskets, one with carrots in it.
		for (int i = 0; i < CRATES.size(); i++) {
			set(level, new BlockPos(x + i, y, z + 8), block(CRATES.get(i)));
		}
		set(level, new BlockPos(x + 2, y + 1, z + 8), block("carrot_crate"));
		level.setBlock(new BlockPos(x + 7, y, z + 8), block("corn_kernel_bag").defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
		set(level, new BlockPos(x + 2, y, z + 6), block("wooden_basket"));
		set(level, new BlockPos(x + 4, y, z + 6), block("bamboo_basket"));
		if (level.getBlockEntity(new BlockPos(x + 2, y, z + 6)) instanceof BasketBlockEntity basket) {
			basket.insert(new ItemStack(Items.CARROT, 32));
		}
	}

	/** Wheat and corn on the farmland, ripening from left to right. */
	private static void plant(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		CropBlock wheat = (CropBlock) Blocks.WHEAT;
		TallCropBlock corn = JugcraftAgriculture.TALL_CROPS.get(TallCrop.CORN);
		for (int dx = 0; dx <= 8; dx++) {
			if (dx == 4) {
				continue;
			}
			int age = Math.min(7, dx);
			level.setBlock(new BlockPos(x + dx, y, z + 2), wheat.getStateForAge(age), Block.UPDATE_ALL);
			BlockPos bottom = new BlockPos(x + dx, y, z + 3);
			set(level, bottom, corn);
			corn.growTo(level, bottom, age);
		}
	}
}
