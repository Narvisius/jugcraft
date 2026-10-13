package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OrchardTree;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for the fruit crops (tools/fruit_crops.py): rows of strawberry plants, blueberry bushes and coffee
 * plants on farmland at every age, from planted to ripe; the plum and banana trees grown from their saplings, a row hung
 * with ripe fruit and a row in blossom, with the three wild bushes before them; then a wall of the crops' items in item
 * frames. CI job {@code client}.
 */
public class FruitCropClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("strawberry", "strawberry_seeds", "blueberries", "blueberry_seeds", "coffee_cherries",
			"coffee_seeds", "coffee_beans", "plum", "plum_pit", "plum_leaves", "banana", "banana_pup", "banana_leaves", "banana_stem",
			"wild_strawberries", "wild_blueberries", "wild_coffee", "strawberry_jam", "blueberry_jam", "plum_jam");
	/** The bushes' rows, back to front. */
	private static final TallCrop[] BUSHES = {TallCrop.COFFEE, TallCrop.BLUEBERRY, TallCrop.STRAWBERRY};
	private static final OrchardTree[] TREES = {OrchardTree.PLUM, OrchardTree.BANANA};
	private static final String[] WILD = {"wild_strawberries", "wild_blueberries", "wild_coffee"};
	/** Items to a row of the wall. */
	private static final int WALL = 7;
	/** The ripe row's and the blossoming row's z, from the origin. */
	private static final int RIPE_ROW = 2;
	private static final int BLOSSOM_ROW = -8;

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
			// Within the fill command's limit of 32768 blocks.
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 3, z - 24, x + 28, y - 1, z + 24));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 24, x + 28, y + 13, z + 24));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % WALL, y + 3 - i / WALL, z + 19, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 3, z + 12, 180, 28, "jugcraft_fruit_bushes");
			shoot(context, singleplayer, x + 20, y + 4, z + 16, 180, 8, "jugcraft_fruit_trees_ripe");
			shoot(context, singleplayer, x + 20, y + 4, z - 21, 0, 8, "jugcraft_fruit_trees_blossom");
			shoot(context, singleplayer, x + 4, y + 2, z + 23, 180, 5, "jugcraft_fruit_items");
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

	/** Grows the tree from its sapling at {@code pos}, then sets every one of its leaves to {@code fruit}. */
	private static void grow(ServerLevel level, OrchardTree tree, BlockPos pos, int fruit) {
		set(level, pos, block(tree.sapling()).defaultBlockState());
		tree.grower.growTree(level, level.getChunkSource().getGenerator(), pos, level.getBlockState(pos), level.getRandom());
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-4, 0, -4), pos.offset(4, 12, 4))) {
			BlockState state = level.getBlockState(at);
			if (state.is(block(tree.leaves()))) {
				set(level, at.immutable(), state.setValue(FruitingLeavesBlock.FRUIT, fruit));
			}
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The bushes: a row of each on moist farmland, its age rising from 0 at the left to ripe at the right, with water
		// between the rows.
		for (int row = 0; row < BUSHES.length; row++) {
			TallCropBlock bush = JugcraftAgriculture.TALL_CROPS.get(BUSHES[row]);
			for (int age = 0; age <= TallCropBlock.MAX_AGE; age++) {
				BlockPos pos = new BlockPos(x + age, y, z + 2 + 2 * row);
				set(level, pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
				set(level, pos, bush.defaultBlockState().setValue(TallCropBlock.AGE, age));
				if (row > 0) {
					set(level, pos.below().north(), Blocks.WATER.defaultBlockState());
				}
			}
		}
		// The trees: a row hung with ripe fruit and, behind it, a row in blossom.
		for (int i = 0; i < TREES.length; i++) {
			grow(level, TREES[i], new BlockPos(x + 16 + 8 * i, y, z + RIPE_ROW), FruitingLeavesBlock.RIPE);
			grow(level, TREES[i], new BlockPos(x + 16 + 8 * i, y, z + BLOSSOM_ROW), 1);
		}
		// The wild bushes, in pairs before the ripe trees.
		for (int i = 0; i < WILD.length; i++) {
			set(level, new BlockPos(x + 15 + 4 * i, y, z + 8), block(WILD[i]).defaultBlockState());
			set(level, new BlockPos(x + 16 + 4 * i, y, z + 9), block(WILD[i]).defaultBlockState());
		}
		// The wall the items hang on.
		for (int dx = -1; dx <= 8; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z + 18), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
