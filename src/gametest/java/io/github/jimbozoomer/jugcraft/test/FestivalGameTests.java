package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AttachedGourdStemBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ChestnutLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.CranberryBushBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GourdStemBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** In-game tests for the Agriculture branch's festival crops: gourds, turnips, cranberries and the chestnut tree. */
public class FestivalGameTests {
	private static final BlockPos SOIL = new BlockPos(2, 1, 2);
	private static final BlockPos CROP = SOIL.above();

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void farmland(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
	}

	private static BlockHitResult hitTop(GameTestHelper helper, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
	}

	private static Player holding(GameTestHelper helper, ItemStack stack) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		return player;
	}

	private static int itemsAround(GameTestHelper helper, BlockPos pos, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(3.0))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
			}
		}
		return count;
	}

	private static boolean drops(GameTestHelper helper, BlockState state, BlockPos pos, Item item) {
		for (ItemStack drop : Block.getDrops(state, helper.getLevel(), helper.absolutePos(pos), null)) {
			if (drop.is(item)) {
				return true;
			}
		}
		return false;
	}

	/** A fully grown stem at {@link #CROP} on farmland, with grass on its east side to grow a gourd on. */
	private static GourdStemBlock grownStem(GameTestHelper helper, String gourd) {
		farmland(helper, SOIL);
		helper.setBlock(SOIL.east(), Blocks.GRASS_BLOCK);
		GourdStemBlock stem = (GourdStemBlock) block(gourd + "_stem");
		helper.setBlock(CROP, stem.defaultBlockState().setValue(GourdStemBlock.AGE, GourdStemBlock.MAX_AGE));
		return stem;
	}

	// ---------------------------------------------------------------- gourds

	/** Gourd seeds plant a stem on farmland, and not on grass. */
	@GameTest
	public void gourdSeedsPlantStemsOnFarmland(GameTestHelper helper) {
		BlockPos grass = new BlockPos(4, 1, 2);
		farmland(helper, SOIL);
		helper.setBlock(grass, Blocks.GRASS_BLOCK);
		Player player = holding(helper, new ItemStack(item("butternut_squash_seeds"), 2));
		helper.useBlock(SOIL, player, hitTop(helper, SOIL));
		helper.assertBlockPresent(block("butternut_squash_stem"), CROP);
		helper.useBlock(grass, player, hitTop(helper, grass));
		helper.assertBlockNotPresent(block("butternut_squash_stem"), grass.above());
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Only the stem on farmland should use a seed");
		helper.succeed();
	}

	/**
	 * A fully grown stem puts its gourd on soil beside it (not over empty space), facing away, and bends
	 * towards it; taking the gourd away straightens the stem to grow another.
	 */
	@GameTest
	public void stemsGrowTheirGourds(GameTestHelper helper) {
		GourdStemBlock stem = grownStem(helper, "warty_gourd");
		BlockPos stemPos = helper.absolutePos(CROP);
		helper.assertTrue(!stem.growGourd(helper.getLevel(), stemPos, Direction.WEST), "A gourd grew over empty space");
		helper.assertTrue(stem.growGourd(helper.getLevel(), stemPos, Direction.EAST), "A grown stem should grow a gourd onto grass");
		BlockState gourd = helper.getBlockState(CROP.east());
		helper.assertTrue(gourd.is(block("warty_gourd")) && gourd.getValue(HorizontalDirectionalBlock.FACING) == Direction.EAST,
				"Expected a warty gourd facing east, found " + gourd);
		BlockState attached = helper.getBlockState(CROP);
		helper.assertTrue(attached.is(block("attached_warty_gourd_stem")) && attached.getValue(AttachedGourdStemBlock.FACING) == Direction.EAST,
				"Expected the stem bent east, found " + attached);
		helper.setBlock(CROP.east(), Blocks.AIR);
		BlockState straight = helper.getBlockState(CROP);
		helper.assertTrue(straight.is(stem) && straight.getValue(GourdStemBlock.AGE) == GourdStemBlock.MAX_AGE,
				"Taking the gourd should leave a grown stem, found " + straight);
		helper.succeed();
	}

	/** One sickle swing cuts a gourd off its stem, which then grows another; a gourd a player set down is left alone. */
	@GameTest(maxTicks = 40)
	public void sickleCutsGourdsOffTheirStems(GameTestHelper helper) {
		GourdStemBlock stem = grownStem(helper, "butternut_squash");
		stem.growGourd(helper.getLevel(), helper.absolutePos(CROP), Direction.EAST);
		BlockPos display = new BlockPos(4, 2, 3);
		helper.setBlock(display.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(display, block("butternut_squash"));
		helper.useBlock(CROP.east(), holding(helper, new ItemStack(item("flint_sickle"))));
		helper.assertBlockNotPresent(block("butternut_squash"), CROP.east());
		helper.assertBlockPresent(block("butternut_squash"), display);
		helper.assertBlockPresent(stem, CROP);
		helper.succeedWhen(() -> helper.assertItemEntityPresent(item("butternut_squash"), CROP.east(), 3.0));
	}

	// ---------------------------------------------------------------- cranberries

	/** Water with stone around it, so nothing flows: a bog cell over mud, `depth` blocks deep. */
	private static void bogCell(GameTestHelper helper, BlockPos water, int depth) {
		helper.setBlock(water.below(), Blocks.MUD);
		for (int i = 0; i < depth; i++) {
			BlockPos pos = water.above(i);
			helper.setBlock(pos, Blocks.WATER);
			for (Direction side : Direction.Plane.HORIZONTAL) {
				helper.setBlock(pos.relative(side), Blocks.STONE);
			}
		}
	}

	/** Cranberries plant only into shallow water over bog soil; the bush keeps its water and leaves it when broken. */
	@GameTest
	public void cranberriesArePlantedInShallowWater(GameTestHelper helper) {
		BlockPos shallow = CROP;
		BlockPos deep = new BlockPos(5, 2, 2);
		BlockPos dry = new BlockPos(2, 2, 5);
		bogCell(helper, shallow, 1);
		bogCell(helper, deep, 2);
		helper.setBlock(dry.below(), Blocks.MUD);
		Player player = holding(helper, new ItemStack(item("cranberries"), 3));
		helper.useBlock(shallow.below(), player, hitTop(helper, shallow.below()));
		helper.useBlock(deep.below(), player, hitTop(helper, deep.below()));
		helper.useBlock(dry.below(), player, hitTop(helper, dry.below()));
		helper.assertBlockPresent(block("cranberry_bush"), shallow);
		helper.assertBlockNotPresent(block("cranberry_bush"), deep);
		helper.assertBlockNotPresent(block("cranberry_bush"), dry);
		helper.assertTrue(helper.getBlockState(shallow).getFluidState().is(FluidTags.WATER), "The bush should hold its water");
		helper.getLevel().destroyBlock(helper.absolutePos(shallow), true);
		helper.assertBlockPresent(Blocks.WATER, shallow);
		helper.succeed();
	}

	/** Ripe cranberries are picked by hand or with a sickle, and the bushes flower again. */
	@GameTest(maxTicks = 40)
	public void cranberriesArePickedAndFlowerAgain(GameTestHelper helper) {
		BlockPos second = new BlockPos(4, 2, 2);
		bogCell(helper, CROP, 1);
		bogCell(helper, second, 1);
		BlockState ripe = block("cranberry_bush").defaultBlockState().setValue(CranberryBushBlock.AGE, CranberryBushBlock.MAX_AGE);
		helper.setBlock(CROP, ripe);
		helper.setBlock(second, ripe);
		helper.useBlock(CROP, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(helper.getBlockState(CROP).getValue(CranberryBushBlock.AGE) == CranberryBushBlock.PICK_RESET,
				"Picking should set the bush back to flowering");
		helper.useBlock(second, holding(helper, new ItemStack(item("flint_sickle"))));
		helper.assertTrue(helper.getBlockState(second).getValue(CranberryBushBlock.AGE) == CranberryBushBlock.PICK_RESET,
				"A sickle should pick the bush");
		helper.succeedWhen(() -> {
			int berries = itemsAround(helper, CROP, item("cranberries"));
			helper.assertTrue(berries >= 2 * CranberryBushBlock.PICK_MIN, "Expected at least 4 cranberries, found " + berries);
		});
	}

	/** Random ticks ripen a bush in shallow water, and not one with water over it. */
	@GameTest(maxTicks = 60)
	public void cranberriesGrowOnlyWithAirAbove(GameTestHelper helper) {
		BlockPos deep = new BlockPos(5, 2, 2);
		bogCell(helper, CROP, 1);
		bogCell(helper, deep, 2);
		helper.setBlock(CROP, block("cranberry_bush"));
		helper.setBlock(deep, block("cranberry_bush"));
		helper.setBlock(CROP.above().north(), Blocks.GLOWSTONE);
		helper.setBlock(deep.above(2).north(), Blocks.GLOWSTONE);
		// Give the light engine time to light the new blocks, then tick.
		helper.runAtTickTime(40, () -> {
			ServerLevel level = helper.getLevel();
			for (int i = 0; i < 400; i++) {
				for (BlockPos pos : new BlockPos[] {CROP, deep}) {
					helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
				}
			}
			helper.assertTrue(CranberryBushBlock.isRipe(helper.getBlockState(CROP)), "The shallow bush should ripen, found "
					+ helper.getBlockState(CROP) + " at light " + level.getRawBrightness(helper.absolutePos(CROP.above()), 0));
			helper.assertTrue(helper.getBlockState(deep).getValue(CranberryBushBlock.AGE) == 0, "A bush under water grew");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the chestnut tree

	/**
	 * A chestnut sapling grows into a chestnut tree of logs and leaves (worldgen/feature/chestnut.json). A tree
	 * needs about 10 free blocks above it, more than the test area's height, so the sapling goes on top of
	 * whatever closes the test area above (its barrier ceiling) if there is one.
	 */
	@GameTest
	public void chestnutSaplingsGrowTrees(GameTestHelper helper) {
		BlockPos sapling = new BlockPos(3, 2, 3);
		for (int dy = 0; dy <= 24; dy++) {
			if (!helper.getBlockState(sapling.above(dy)).isAir()) {
				sapling = sapling.above(dy + 1);
				break;
			}
		}
		helper.setBlock(sapling.below(), Blocks.DIRT);
		helper.setBlock(sapling, block("chestnut_sapling"));
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(sapling);
		boolean grown = JugcraftAgriculture.CHESTNUT_GROWER.growTree(level, level.getChunkSource().getGenerator(), absolute,
				level.getBlockState(absolute), level.getRandom());
		helper.assertTrue(grown, "The chestnut sapling at " + sapling + " should grow into a tree");
		helper.assertBlockPresent(block("chestnut_log"), sapling);
		int leaves = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-4, 0, -4), absolute.offset(4, 12, 4))) {
			if (level.getBlockState(pos).is(block("chestnut_leaves"))) {
				leaves++;
			}
		}
		helper.assertTrue(leaves >= 20, "Expected a crown of chestnut leaves, found " + leaves);
		helper.succeed();
	}

	/** Tree-grown leaves over air grow burs that ripen, and a right-click picks the chestnuts; placed leaves never fruit. */
	@GameTest(maxTicks = 40)
	public void chestnutLeavesFruitAndArePicked(GameTestHelper helper) {
		BlockPos log = new BlockPos(2, 4, 2);
		BlockPos natural = log.east();
		BlockPos placed = log.west();
		helper.setBlock(log, block("chestnut_log"));
		BlockState leaves = block("chestnut_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
		helper.setBlock(natural, leaves.setValue(LeavesBlock.PERSISTENT, false));
		helper.setBlock(placed, leaves.setValue(LeavesBlock.PERSISTENT, true));
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < 400; i++) {
			for (BlockPos pos : new BlockPos[] {natural, placed}) {
				helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
			}
		}
		helper.assertTrue(helper.getBlockState(natural).getValue(ChestnutLeavesBlock.FRUIT) == ChestnutLeavesBlock.RIPE,
				"Tree leaves should ripen a bur, found " + helper.getBlockState(natural));
		helper.assertTrue(helper.getBlockState(placed).getValue(ChestnutLeavesBlock.FRUIT) == 0, "Placed leaves should not fruit");
		helper.useBlock(natural, helper.makeMockPlayer(GameType.SURVIVAL));
		helper.assertTrue(helper.getBlockState(natural).getValue(ChestnutLeavesBlock.FRUIT) == 0, "Picking should empty the bur");
		helper.succeedWhen(() -> helper.assertItemEntityPresent(item("chestnut"), natural, 3.0));
	}

	/** Any axe strips a chestnut log, keeping its axis; the wood joins vanilla's wood tags and burns as fuel. */
	@GameTest
	public void chestnutWoodWorksLikeWood(GameTestHelper helper) {
		helper.setBlock(CROP, block("chestnut_log").defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		Player player = holding(helper, new ItemStack(Items.IRON_AXE));
		helper.useBlock(CROP, player);
		BlockState stripped = helper.getBlockState(CROP);
		helper.assertTrue(stripped.is(block("stripped_chestnut_log")) && stripped.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
				"An axe should strip the log along its axis, found " + stripped);
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Stripping should wear the axe");
		helper.assertTrue(stripped.is(BlockTags.LOGS), "Chestnut logs should be logs");
		helper.assertTrue(block("chestnut_leaves").defaultBlockState().is(BlockTags.LEAVES), "Chestnut leaves should be leaves");
		helper.assertTrue(new ItemStack(item("chestnut_planks")).is(ItemTags.PLANKS), "Chestnut planks should make sticks and tables");
		helper.assertTrue(new ItemStack(item("chestnut_stairs")).is(ItemTags.WOODEN_STAIRS), "Chestnut stairs should be wooden stairs");
		helper.assertTrue(new ItemStack(item("chestnut_fence")).is(ItemTags.WOODEN_FENCES), "Chestnut fences should be wooden fences");
		for (String wood : new String[] {"chestnut_log", "chestnut_planks", "chestnut_slab", "chestnut_fence_gate"}) {
			helper.assertTrue(new ItemStack(item(wood)).has(DataComponents.COOKING_FUEL), wood + " should burn as furnace fuel");
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- drops, light, food and recipes

	/** Ripe bushes, burs and turnips drop their crop, gourds drop themselves, and the Turnip Lantern gives light 13. */
	@GameTest
	public void festivalBlocksDropTheirCrops(GameTestHelper helper) {
		BlockState ripeBush = block("cranberry_bush").defaultBlockState().setValue(CranberryBushBlock.AGE, CranberryBushBlock.MAX_AGE);
		helper.assertTrue(drops(helper, ripeBush, CROP, item("cranberries")), "A ripe cranberry bush should drop cranberries");
		BlockState ripeLeaves = block("chestnut_leaves").defaultBlockState().setValue(ChestnutLeavesBlock.FRUIT, ChestnutLeavesBlock.RIPE);
		helper.assertTrue(drops(helper, ripeLeaves, CROP, item("chestnut")), "Leaves with a ripe bur should drop chestnuts");
		helper.assertTrue(drops(helper, ((CropBlock) block("turnip_crop")).getStateForAge(7), CROP, item("turnip")), "Ripe turnips should drop turnips");
		for (String gourd : new String[] {"butternut_squash", "acorn_squash", "warty_gourd"}) {
			helper.assertTrue(drops(helper, block(gourd).defaultBlockState(), CROP, item(gourd)), gourd + " should drop itself");
		}
		helper.assertTrue(block("turnip_lantern").defaultBlockState().getLightEmission() == 13, "The Turnip Lantern should give light 13");
		helper.succeed();
	}

	/** Festival foods restore the values in docs/branches/AGRICULTURE.md; raw chestnuts are not food; the sauce does not stack. */
	@GameTest
	public void festivalFoodsHaveTheirValues(GameTestHelper helper) {
		String[] foods = {"turnip", "cranberries", "roasted_chestnuts", "baked_acorn_squash", "squash_pie", "candy_corn",
				"butternut_squash_soup", "harvest_stew", "cranberry_sauce"};
		int[] nutrition = {3, 2, 4, 6, 8, 2, 8, 10, 5};
		for (int i = 0; i < foods.length; i++) {
			FoodProperties food = new ItemStack(item(foods[i])).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.nutrition() == nutrition[i], foods[i] + " should restore " + nutrition[i] + ", has " + food);
		}
		helper.assertTrue(new ItemStack(item("chestnut")).get(DataComponents.FOOD) == null, "Raw chestnuts should not be edible");
		helper.assertTrue(new ItemStack(item("cranberry_sauce")).getMaxStackSize() == 1, "Bowl foods should not stack");
		helper.succeed();
	}

	/** The festival Cooking Pot recipes load and are found from their ingredients in any order. */
	@GameTest
	public void festivalPotRecipesLoad(GameTestHelper helper) {
		Object[][] recipes = {
				{"butternut_squash_soup", new Item[] {item("garlic"), Items.BOWL, item("butternut_squash"), item("onion")}},
				{"harvest_stew", new Item[] {Items.MUTTON, item("turnip"), Items.BOWL, Items.CARROT, item("onion")}},
				{"cranberry_sauce", new Item[] {item("cranberries"), Items.SUGAR, item("cranberries"), Items.BOWL}}};
		for (Object[] recipe : recipes) {
			List<ItemStack> slots = new ArrayList<>();
			for (Item ingredient : (Item[]) recipe[1]) {
				slots.add(new ItemStack(ingredient));
			}
			while (slots.size() < CookingPotBlockEntity.INPUTS) {
				slots.add(ItemStack.EMPTY);
			}
			Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(helper.getLevel().getServer(), slots);
			helper.assertTrue(match.isPresent() && match.get().recipe().output().create().is(item((String) recipe[0])),
					"Ingredients for " + recipe[0] + " found " + match.map(m -> m.recipe().output().create()));
		}
		helper.succeed();
	}

	/** The festival's worldgen loads: the wild turnip, gourd, cranberry and chestnut tree patches. */
	@GameTest
	public void festivalWorldgenLoads(GameTestHelper helper) {
		var placed = helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (String patch : new String[] {"wild_turnip", "butternut_squash", "acorn_squash", "warty_gourd", "cranberry_bush", "chestnut_tree"}) {
			ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id("patch_" + patch));
			helper.assertTrue(placed.get(key).isPresent(), "Missing placed feature " + key);
		}
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE).get(JugcraftAgriculture.CHESTNUT_TREE).isPresent(),
				"Missing the chestnut tree feature");
		helper.succeed();
	}
}
