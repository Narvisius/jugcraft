package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MushroomColonyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TomatoVineBlock;
import java.util.List;
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
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for the garden crops in the owner's art (tools/garden.py): corn at every age, from planted to ripe and
 * three blocks tall, with ripe ornamental corn beside it; tomatoes climbing their trellises, cabbages and onions at every
 * age, and a tomato vine gone over; the wild corn, tomato, onion, cabbage, carrots, potatoes and beetroots, with the brown
 * and red mushroom colonies at each stage on Rich Soil; then a wall of the crops' items in item frames. CI job
 * {@code client}.
 */
public class GardenClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("cabbage", "cabbage_seeds", "onion", "garden_salad", "tomato", "tomato_seeds",
			"rotten_tomato", "corn", "corn_kernels", "wild_corn", "wild_tomato", "wild_onion", "wild_cabbage", "wild_carrots", "wild_potatoes",
			"wild_beetroots");
	private static final String[] WILD = {"wild_corn", "wild_tomato", "wild_onion", "wild_cabbage", "wild_carrots", "wild_potatoes",
			"wild_beetroots"};
	private static final String[] COLONIES = {"brown_mushroom_colony", "red_mushroom_colony"};
	/** Items to a row of the wall. */
	private static final int WALL = 4;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			// Hide the HUD, hand and chat so the screenshots show only the garden, whatever an earlier test in this client left.
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
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:random_tick_speed 0");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			// Within the fill command's limit of 32768 blocks.
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 3, z - 6, x + 62, y - 1, z + 18));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 6, x + 62, y + 10, z + 18));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// Rows of four, read from the camera's left as it faces them (north, so east is on its right).
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 57 + i % WALL, y + 4 - i / WALL, z + 11, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 1, z + 8, 180, 10, "jugcraft_garden_corn");
			shoot(context, singleplayer, x + 20, y + 3, z + 11, 180, 30, "jugcraft_garden_vegetables");
			shoot(context, singleplayer, x + 40, y + 2, z + 10, 180, 24, "jugcraft_garden_wild");
			shoot(context, singleplayer, x + 58, y + 1, z + 15, 180, -5, "jugcraft_garden_items");
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

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static void farmland(ServerLevel level, BlockPos crop) {
		set(level, crop.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	/** A tall crop planted at {@code pos} and grown to {@code age}; a climbing one on two trellises. */
	private static void tall(ServerLevel level, BlockPos pos, TallCrop crop, int age) {
		farmland(level, pos);
		TallCropBlock block = JugcraftAgriculture.TALL_CROPS.get(crop);
		if (crop.trellis) {
			set(level, pos, block("trellis").defaultBlockState());
			set(level, pos.above(), block("trellis").defaultBlockState());
		}
		set(level, pos, block.defaultBlockState());
		if (age > 0) {
			block.growTo(level, pos, age);
		}
	}

	private static void crop(ServerLevel level, BlockPos pos, String id, int age) {
		farmland(level, pos);
		set(level, pos, ((CropBlock) block(id)).getStateForAge(age));
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// Every growth stage, youngest on the left (the cameras face north, so east is on their right).
		for (int age = 0; age <= TallCropBlock.MAX_AGE; age++) {
			tall(level, new BlockPos(x + age, y, z + 2), TallCrop.CORN, age);
			tall(level, new BlockPos(x + 16 + age, y, z + 2), TallCrop.TOMATO, age);
			crop(level, new BlockPos(x + 16 + age, y, z + 4), "cabbage_crop", age);
			crop(level, new BlockPos(x + 16 + age, y, z + 6), "onion_crop", age);
		}
		// Ripe ornamental corn beside the corn, and a tomato vine gone over beside the tomatoes.
		tall(level, new BlockPos(x + 9, y, z + 2), TallCrop.ORNAMENTAL_CORN, TallCropBlock.MAX_AGE);
		BlockPos over = new BlockPos(x + 25, y, z + 2);
		tall(level, over, TallCrop.TOMATO, TallCropBlock.MAX_AGE);
		((TomatoVineBlock) JugcraftAgriculture.TALL_CROPS.get(TallCrop.TOMATO)).goOver(level, over);
		// The wild plants on the grass, and the mushroom colonies at each stage on Rich Soil before them.
		for (int i = 0; i < WILD.length; i++) {
			set(level, new BlockPos(x + 34 + 2 * i, y, z + 2), block(WILD[i]).defaultBlockState());
		}
		for (int c = 0; c < COLONIES.length; c++) {
			for (int age = 0; age <= MushroomColonyBlock.MAX_AGE; age++) {
				BlockPos pos = new BlockPos(x + 36 + 6 * c + age, y, z + 5);
				set(level, pos.below(), block("rich_soil").defaultBlockState());
				set(level, pos, block(COLONIES[c]).defaultBlockState().setValue(MushroomColonyBlock.AGE, age));
			}
		}
		// The wall the items hang on.
		for (int dx = 0; dx <= 5; dx++) {
			for (int dy = 0; dy <= 5; dy++) {
				set(level, new BlockPos(x + 56 + dx, y + dy, z + 10), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
