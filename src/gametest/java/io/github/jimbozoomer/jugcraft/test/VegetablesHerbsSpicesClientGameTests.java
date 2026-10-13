package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.HerbBundleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.SpiceRackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpiceRackBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client game test for garden crops, herbs and spices, part b (tools/vegetables.py, herbs.py, spices.py): the vegetables at
 * every age (the cucumber on its trellis, the eggplant and zucchini bushes, lettuce, spinach, radishes and peas); the herbs
 * hung in bundles fresh and dried, grown in Planter Boxes, two of them through their stages, and potted; the black pepper
 * and vanilla vines, ginger, mustard and the saffron crocus at every age and a grown Cinnamon Tree; two Spice Racks close
 * up; the twenty wild plants; then a wall of the new items in item frames. CI job {@code client}.
 */
public class VegetablesHerbsSpicesClientGameTests implements FabricClientGameTest {
	private static final List<String> ITEMS = List.of("lettuce", "lettuce_seeds", "spinach", "spinach_seeds", "radish", "peas", "cucumber",
			"cucumber_seeds", "eggplant", "eggplant_seeds", "zucchini", "zucchini_seeds", "roasted_eggplant", "grilled_zucchini", "green_salad",
			"pea_soup", "basil", "mint", "rosemary", "thyme", "parsley", "sage", "dill", "chives", "dried_herbs", "pesto_pasta",
			"sage_and_onion_stuffing", "herb_roasted_mutton", "garden_herb_soup", "herb_roasted_potatoes", "mint_tea", "dill_pickles",
			"peppercorns", "vanilla_pods", "ginger", "mustard_seeds", "crocus_bulb", "saffron", "cinnamon", "dried_chili", "paprika",
			"gingerbread_cookie", "chicken_curry", "saffron_rice", "vanilla_custard", "basil_bundle", "planter_box", "cinnamon_sapling");
	private static final String[] WILD = {"wild_lettuce", "wild_spinach", "wild_radishes", "wild_peas", "wild_cucumbers", "wild_eggplant",
			"wild_zucchini", "wild_basil", "wild_mint", "wild_rosemary", "wild_thyme", "wild_parsley", "wild_sage", "wild_dill", "wild_chives",
			"wild_peppercorns", "wild_vanilla", "wild_ginger", "wild_mustard", "wild_saffron"};
	private static final TallCrop[] HERBS = {TallCrop.BASIL, TallCrop.MINT, TallCrop.ROSEMARY, TallCrop.THYME, TallCrop.PARSLEY,
			TallCrop.SAGE, TallCrop.DILL, TallCrop.CHIVES};
	private static final String[] SPICES = {"peppercorns", "vanilla_pods", "ginger", "mustard_seeds", "saffron", "cinnamon", "paprika",
			"dried_herbs"};
	/** Items to a row of the wall. */
	private static final int WALL = 8;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 4, y - 3, z - 6, x + 76, y - 1, z + 20));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 6, x + 76, y + 10, z + 20));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			// Rows of eight, read from the camera's left as it faces them (north, so east is on its right).
			for (int i = 0; i < ITEMS.size(); i++) {
				server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}"
						.formatted(x + 65 + i % WALL, y + 6 - i / WALL, z + 11, ITEMS.get(i)));
			}
			context.waitTicks(20);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 3, y + 3, z + 12, 180, 25, "jugcraft_vegetables");
			shoot(context, singleplayer, x + 22, y + 3, z + 10, 180, 22, "jugcraft_herbs");
			shoot(context, singleplayer, x + 41, y + 4, z + 13, 180, 25, "jugcraft_spices");
			shoot(context, singleplayer, x + 34, y + 2, z + 11, 180, 32, "jugcraft_spice_racks");
			shoot(context, singleplayer, x + 56, y + 3, z + 10, 180, 30, "jugcraft_vegetables_herbs_spices_wild");
			shoot(context, singleplayer, x + 68, y + 3, z + 18, 180, -2, "jugcraft_vegetables_herbs_spices_items");
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

	/** A tall crop planted at {@code pos} (on farmland, or on what is already there when {@code soil} is false) grown to {@code age}. */
	private static void tall(ServerLevel level, BlockPos pos, TallCrop crop, int age, boolean soil) {
		if (soil) {
			farmland(level, pos);
		}
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

	/** A Spice Rack against the wall north of it, facing the cameras, holding {@code count} of the spices. */
	private static void rack(ServerLevel level, BlockPos pos, int count) {
		set(level, pos, block("spice_rack").defaultBlockState().setValue(SpiceRackBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(pos) instanceof SpiceRackBlockEntity rack) {
			for (int i = 0; i < count; i++) {
				rack.store(new ItemStack(JugcraftAgriculture.item(SPICES[i % SPICES.length])));
			}
		}
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The vegetables, every growth stage, youngest on the left (the cameras face north, so east is on their right): the
		// cucumber vines at the back, then the bushes, then the low crops.
		for (int age = 0; age <= TallCropBlock.MAX_AGE; age++) {
			tall(level, new BlockPos(x + age, y, z), TallCrop.CUCUMBER, age, true);
			tall(level, new BlockPos(x + age, y, z + 2), TallCrop.EGGPLANT, age, true);
			tall(level, new BlockPos(x + age, y, z + 3), TallCrop.ZUCCHINI, age, true);
			crop(level, new BlockPos(x + age, y, z + 5), "lettuce_crop", age);
			crop(level, new BlockPos(x + age, y, z + 6), "spinach_crop", age);
			crop(level, new BlockPos(x + age, y, z + 7), "radish_crop", age);
			crop(level, new BlockPos(x + age, y, z + 8), "pea_crop", age);
		}

		// The herbs, from the back: the bundles hung from a beam, fresh and dried by turns; each herb grown in a Planter Box;
		// rosemary and thyme through their stages; and a row of potted herbs at the front.
		for (int i = 0; i < HERBS.length; i++) {
			String herb = HERBS[i].produceId;
			set(level, new BlockPos(x + 19 + i, y + 3, z - 1), Blocks.STRIPPED_OAK_LOG.defaultBlockState()
					.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
			set(level, new BlockPos(x + 19 + i, y + 2, z - 1), block(herb + "_bundle").defaultBlockState().setValue(HerbBundleBlock.DRIED, i % 2 == 1));
			BlockPos box = new BlockPos(x + 19 + i, y, z + 1);
			set(level, box, block("planter_box").defaultBlockState());
			tall(level, box.above(), HERBS[i], TallCropBlock.MAX_AGE, false);
			int age = 1 + 2 * (i % 4);
			tall(level, new BlockPos(x + 19 + i, y, z + 3), i < 4 ? TallCrop.ROSEMARY : TallCrop.THYME, age, true);
			set(level, new BlockPos(x + 19 + i, y, z + 5), block("potted_" + herb).defaultBlockState());
		}
		for (int dy = 0; dy <= 2; dy++) {
			set(level, new BlockPos(x + 18, y + dy, z - 1), Blocks.STRIPPED_OAK_LOG.defaultBlockState());
			set(level, new BlockPos(x + 27, y + dy, z - 1), Blocks.STRIPPED_OAK_LOG.defaultBlockState());
		}

		// The spices: the black pepper and vanilla vines at the back, ginger, mustard and saffron before them, a grown cinnamon
		// tree to the right, and two spice racks on a wall to the left.
		for (int age = 0; age <= TallCropBlock.MAX_AGE; age++) {
			tall(level, new BlockPos(x + 37 + age, y, z), TallCrop.PEPPERCORN, age, true);
			tall(level, new BlockPos(x + 37 + age, y, z + 3), TallCrop.VANILLA, age, true);
			crop(level, new BlockPos(x + 37 + age, y, z + 6), "ginger_crop", age);
			crop(level, new BlockPos(x + 37 + age, y, z + 7), "mustard_crop", age);
			crop(level, new BlockPos(x + 37 + age, y, z + 8), "saffron_crop", age);
		}
		BlockPos tree = new BlockPos(x + 48, y, z + 4);
		set(level, tree.below(), Blocks.DIRT.defaultBlockState());
		set(level, tree, block("cinnamon_sapling").defaultBlockState());
		JugcraftAgriculture.CINNAMON_GROWER.growTree(level, level.getChunkSource().getGenerator(), tree, level.getBlockState(tree), level.getRandom());
		for (int dx = 0; dx <= 2; dx++) {
			for (int dy = 0; dy <= 2; dy++) {
				set(level, new BlockPos(x + 33 + dx, y + dy, z + 7), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
		rack(level, new BlockPos(x + 33, y + 1, z + 8), SpiceRackBlockEntity.SLOTS);
		rack(level, new BlockPos(x + 35, y + 1, z + 8), 5);

		// The wild plants on the grass, two rows of ten.
		for (int i = 0; i < WILD.length; i++) {
			set(level, new BlockPos(x + 52 + i % 10, y, z + 2 + 2 * (i / 10)), block(WILD[i]).defaultBlockState());
		}

		// The wall the items hang on.
		for (int dx = 0; dx <= 9; dx++) {
			for (int dy = 0; dy <= 7; dy++) {
				set(level, new BlockPos(x + 64 + dx, y + dy, z + 10), Blocks.SPRUCE_PLANKS.defaultBlockState());
			}
		}
	}
}
