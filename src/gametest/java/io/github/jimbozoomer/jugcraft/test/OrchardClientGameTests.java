package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OrchardTree;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PantryShelfBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.PieBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PlacedDishBlock;
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
 * Client game test for the orchards (the kitchen and cooking expansion's slice 6): the pear, peach, lemon and orange trees
 * grown from their saplings, a row hung with ripe fruit and a row in blossom; a table of the peach and lemon meringue pies,
 * orange juice and lemonade set down, the saplings planted before it and a pantry shelf of the three new preserves; then a
 * wall of the slice's items in item frames. CI job {@code client}.
 */
public class OrchardClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("pear", "peach", "lemon", "orange", "pear_seeds", "peach_pit", "lemon_seeds",
			"orange_seeds", "pear_leaves", "peach_leaves", "lemon_leaves", "orange_leaves", "orange_juice", "lemonade", "raw_peach_pie",
			"raw_lemon_meringue_pie", "peach_pie_slice", "lemon_meringue_pie_slice", "orange_marmalade", "peach_preserves", "pear_butter");
	private static final List<String> PRESERVES = List.of("orange_marmalade", "peach_preserves", "pear_butter");
	/** The slice's own trees; the fruit crops' plum and banana are shown by {@link FruitCropClientGameTests}. */
	private static final OrchardTree[] TREES = {OrchardTree.PEAR, OrchardTree.PEACH, OrchardTree.LEMON, OrchardTree.ORANGE};
	/** Items to a row of the wall. */
	private static final int WALL = 7;
	/** Blocks between the trees of a row. */
	private static final int SPACING = 7;
	/** The ripe row's and the blossoming row's z, from the origin. */
	private static final int RIPE_ROW = 0;
	private static final int BLOSSOM_ROW = -14;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 32, x + 30, y - 1, z + 20));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 32, x + 30, y + 13, z + 20));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 1 + i % WALL, y + 3 - i / WALL, z + 15, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 12, y + 4, z + 13, 180, 10, "jugcraft_orchard_ripe");
			shoot(context, singleplayer, x + 12, y + 4, z - 29, 0, 10, "jugcraft_orchard_blossom");
			shoot(context, singleplayer, x + 4, y + 2, z + 11, 180, 30, "jugcraft_orchard_table");
			shoot(context, singleplayer, x + 4, y + 2, z + 19, 180, 5, "jugcraft_orchard_items");
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
		// The orchard: a row of ripe trees and, behind it, a row in blossom.
		OrchardTree[] trees = TREES;
		for (int i = 0; i < trees.length; i++) {
			grow(level, trees[i], new BlockPos(x + 2 + SPACING * i, y, z + RIPE_ROW), FruitingLeavesBlock.RIPE);
			grow(level, trees[i], new BlockPos(x + 2 + SPACING * i, y, z + BLOSSOM_ROW), 1);
		}
		// The table: the two pies (the lemon meringue cut), the two juices set down facing the camera, and the saplings
		// planted in front.
		for (int dx = 0; dx <= 6; dx++) {
			set(level, new BlockPos(x + dx, y, z + 6), Blocks.STRIPPED_OAK_WOOD.defaultBlockState());
		}
		set(level, new BlockPos(x + 1, y + 1, z + 6), block("peach_pie").defaultBlockState());
		set(level, new BlockPos(x + 2, y + 1, z + 6), block("lemon_meringue_pie").defaultBlockState().setValue(PieBlock.BITES, 1));
		set(level, new BlockPos(x + 4, y + 1, z + 6), block("orange_juice").defaultBlockState().setValue(PlacedDishBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 5, y + 1, z + 6), block("lemonade").defaultBlockState().setValue(PlacedDishBlock.FACING, Direction.SOUTH));
		for (int i = 0; i < trees.length; i++) {
			set(level, new BlockPos(x + 1 + 2 * i, y, z + 8), block(trees[i].sapling()).defaultBlockState());
		}
		// The pantry shelf of the new preserves, sealed and fresh, behind the middle of the table, raised on planks to face
		// the camera.
		BlockPos shelfPos = new BlockPos(x + 3, y + 1, z + 5);
		set(level, shelfPos.below(), Blocks.SPRUCE_PLANKS.defaultBlockState());
		set(level, shelfPos.north(), Blocks.SPRUCE_PLANKS.defaultBlockState());
		set(level, shelfPos, block("pantry_shelf").defaultBlockState().setValue(PantryShelfBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(shelfPos) instanceof PantryShelfBlockEntity shelf) {
			for (int slot = 0; slot < PantryShelfBlockEntity.SLOTS; slot++) {
				ItemStack jar = new ItemStack(JugcraftAgriculture.item(PRESERVES.get(slot % PRESERVES.size())));
				PreserveJarItem.cooked(jar, level.getGameTime());
				if (slot % 2 == 0) {
					PreserveJarItem.seal(jar);
				}
				shelf.store(jar);
			}
		}
		// The wall the items hang on.
		for (int dx = -1; dx <= 8; dx++) {
			for (int dy = 0; dy <= 4; dy++) {
				set(level, new BlockPos(x + dx, y + dy, z + 14), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
