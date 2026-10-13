package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FullTatamiMatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RollMedleyBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TallCrop;
import io.github.jimbozoomer.jugcraft.agriculture.TallCropBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TatamiBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TatamiMatBlock;
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
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Client game test for rice and wet farming (the kitchen and cooking expansion's slice 4): a paddy with rice at every
 * age, from the seedling in the water to the ripe panicles two blocks up; wild rice in its shallows; the Bag of Rice and
 * the bales, standing and laid down; a tatami floor (a pair and a lone one), full and half tatami mats; the Rice Roll
 * Medley whole and served down to the bare platter; the rice dishes set down; and a wall of the slice's items in item
 * frames. CI job {@code client}.
 */
public class RiceClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("rice", "rice_panicle", "straw", "wild_rice", "rice_bag", "rice_bale", "straw_bale",
			"tatami", "full_tatami_mat", "half_tatami_mat", "cooked_rice", "fried_rice", "mushroom_rice", "salmon_roll", "cod_roll", "kelp_roll",
			"kelp_roll_slice", "rice_roll_medley");
	private static final List<String> DISHES = List.of("cooked_rice", "fried_rice", "mushroom_rice", "salmon_roll", "cod_roll", "kelp_roll",
			"kelp_roll_slice");
	/** Items to a row of the wall. */
	private static final int WALL = 9;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 3, z - 16, x + 18, y - 1, z + 16));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 16, x + 18, y + 10, z + 16));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// The paddy is planted once the light has reached its new water: a crop needs light 8 to stay, as vanilla's do.
			context.waitTicks(20);
			server.runOnServer(minecraft -> plant(minecraft.overworld(), origin));
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % WALL, y + 3 - i / WALL, z - 11, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 4, y + 2, z - 1, 0, 30, "jugcraft_rice_paddy");
			shoot(context, singleplayer, x + 5, y + 6, z - 3, 0, 50, "jugcraft_rice_overview");
			shoot(context, singleplayer, x + 4, y + 3, z + 4, 0, 50, "jugcraft_rice_storage_and_tatami");
			shoot(context, singleplayer, x + 5, y + 3, z + 9, 0, 50, "jugcraft_rice_medley_and_dishes");
			shoot(context, singleplayer, x + 5, y + 2, z - 5, 180, 5, "jugcraft_rice_items");
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

	/** A still water source at {@code pos}, over mud. */
	private static void shallows(ServerLevel level, BlockPos pos) {
		set(level, pos.below(), Blocks.MUD);
		set(level, pos, Blocks.WATER);
	}

	/** The paddy: rice at every age in a row of shallows, a second row ripe behind it, and wild rice beyond. */
	private static void plant(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		TallCropBlock rice = JugcraftAgriculture.TALL_CROPS.get(TallCrop.RICE);
		for (int age = 0; age <= TallCropBlock.MAX_AGE; age++) {
			for (int row = 0; row < 2; row++) {
				BlockPos bottom = new BlockPos(x + age, y - 1, z + 2 + row);
				set(level, bottom, rice);
				rice.growTo(level, bottom, row == 0 ? age : TallCropBlock.MAX_AGE);
			}
		}
		for (int i = 0; i < 4; i++) {
			BlockPos lower = new BlockPos(x + 9 + i % 2, y - 1, z + 2 + i / 2);
			level.setBlock(lower, block("wild_rice").defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
			level.setBlock(lower.above(), block("wild_rice").defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER),
					Block.UPDATE_ALL);
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The back wall the items hang on.
		for (int dx = -1; dx <= 11; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z - 12), Blocks.BRICKS);
			}
		}
		// The paddy's shallows and wild rice's (planted by plant()).
		for (int age = 0; age <= TallCropBlock.MAX_AGE; age++) {
			for (int row = 0; row < 2; row++) {
				shallows(level, new BlockPos(x + age, y - 1, z + 2 + row));
			}
		}
		for (int i = 0; i < 4; i++) {
			shallows(level, new BlockPos(x + 9 + i % 2, y - 1, z + 2 + i / 2));
		}
		// The storehouse: the Bag of Rice and the bales, standing and laid on their sides.
		int store = z + 7;
		level.setBlock(new BlockPos(x, y, store), block("rice_bag").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH),
				Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 1, y, store), block("rice_bag").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST),
				Block.UPDATE_ALL);
		set(level, new BlockPos(x + 3, y, store), block("rice_bale"));
		level.setBlock(new BlockPos(x + 4, y, store), block("rice_bale").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X),
				Block.UPDATE_ALL);
		set(level, new BlockPos(x + 6, y, store), block("straw_bale"));
		level.setBlock(new BlockPos(x + 7, y, store), block("straw_bale").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z),
				Block.UPDATE_ALL);
		set(level, new BlockPos(x + 6, y + 1, store), block("straw_bale"));
		// The tatami room: a pair and a lone tatami laid as the floor, a full mat and two half mats on it.
		int room = z + 9;
		level.setBlock(new BlockPos(x, y - 1, room), block("tatami").defaultBlockState().setValue(TatamiBlock.FACING, Direction.EAST)
				.setValue(TatamiBlock.PAIRED, true), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 1, y - 1, room), block("tatami").defaultBlockState().setValue(TatamiBlock.FACING, Direction.WEST)
				.setValue(TatamiBlock.PAIRED, true), Block.UPDATE_ALL);
		set(level, new BlockPos(x + 2, y - 1, room), block("tatami"));
		for (int dx = 4; dx <= 8; dx++) {
			set(level, new BlockPos(x + dx, y - 1, room), Blocks.SPRUCE_PLANKS);
		}
		level.setBlock(new BlockPos(x + 4, y, room), block("full_tatami_mat").defaultBlockState().setValue(FullTatamiMatBlock.FACING, Direction.EAST)
				.setValue(FullTatamiMatBlock.PART, BedPart.FOOT), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 5, y, room), block("full_tatami_mat").defaultBlockState().setValue(FullTatamiMatBlock.FACING, Direction.EAST)
				.setValue(FullTatamiMatBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 7, y, room), block("half_tatami_mat").defaultBlockState().setValue(TatamiMatBlock.FACING, Direction.NORTH),
				Block.UPDATE_ALL);
		level.setBlock(new BlockPos(x + 8, y, room), block("half_tatami_mat").defaultBlockState().setValue(TatamiMatBlock.FACING, Direction.EAST),
				Block.UPDATE_ALL);
		// The table: the medley whole, part served and bare, then every rice dish set down, facing the camera to the north.
		int table = z + 12;
		int[] rolls = {RollMedleyBlock.MAX, 6, 3, 0};
		for (int i = 0; i < rolls.length + DISHES.size(); i++) {
			BlockPos top = new BlockPos(x + i, y, table);
			set(level, top, Blocks.STRIPPED_SPRUCE_LOG);
			if (i < rolls.length) {
				level.setBlock(top.above(), block("rice_roll_medley").defaultBlockState().setValue(RollMedleyBlock.FACING, Direction.NORTH)
						.setValue(RollMedleyBlock.ROLLS, rolls[i]), Block.UPDATE_ALL);
			} else {
				level.setBlock(top.above(), block(DISHES.get(i - rolls.length)).defaultBlockState().setValue(PlacedDishBlock.FACING, Direction.NORTH),
						Block.UPDATE_ALL);
			}
		}
	}
}
