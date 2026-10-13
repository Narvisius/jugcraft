package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CookingPotRecipe;
import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.OrchardLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.OrchardTree;
import io.github.jimbozoomer.jugcraft.agriculture.PieFilling;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the orchards (the kitchen and cooking expansion's slice 6, tools/orchard.py, and the plum and banana
 * of the fruit crops, tools/fruit_crops.py): each fruit tree grows from its sapling into its trunk (oak, or the banana's
 * own stem) and its own leaves; tree-grown leaves blossom, ripen and are picked for their fruit, placed
 * leaves never fruit, and ripe leaves broken drop their fruit; a fruit crafts into its seed, which plants the sapling; the
 * juices are drinks; and the recipes, loot tables and wild trees load.
 */
public class OrchardGameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	// ---------------------------------------------------------------- the trees grow

	/** The block a tree's trunk is made of (tools/orchard.py log): oak, but the banana's own stem. */
	private static Block trunk(OrchardTree tree) {
		return tree == OrchardTree.BANANA ? block("banana_stem") : Blocks.OAK_LOG;
	}

	/**
	 * A sapling grows into its tree: its trunk under a crown of its own leaves, grown by the tree (not persistent) and
	 * bare of fruit. A tree needs more free space above it than the test area has, so the sapling goes on top of whatever
	 * closes the area above (as the apple tree's test does).
	 */
	private static void growsIntoItsTree(GameTestHelper helper, OrchardTree tree) {
		BlockPos sapling = new BlockPos(3, 2, 3);
		for (int dy = 0; dy <= 24; dy++) {
			if (!helper.getBlockState(sapling.above(dy)).isAir()) {
				sapling = sapling.above(dy + 1);
				break;
			}
		}
		helper.setBlock(sapling.below(), Blocks.DIRT);
		helper.setBlock(sapling, block(tree.sapling()));
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(sapling);
		boolean grown = tree.grower.growTree(level, level.getChunkSource().getGenerator(), absolute, level.getBlockState(absolute),
				level.getRandom());
		helper.assertTrue(grown, "The " + tree.id + " sapling at " + sapling + " should grow into a tree");
		helper.assertBlockPresent(trunk(tree), sapling);
		int leaves = 0;
		for (BlockPos pos : BlockPos.betweenClosed(absolute.offset(-4, 0, -4), absolute.offset(4, 12, 4))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(block(tree.leaves()))) {
				leaves++;
				helper.assertTrue(!state.getValue(LeavesBlock.PERSISTENT) && state.getValue(FruitingLeavesBlock.FRUIT) == 0,
						"A grown " + tree.id + " tree's leaves are its own, and bare");
			}
		}
		helper.assertTrue(leaves >= 12, "Expected a crown of " + tree.leaves() + ", found " + leaves);
		helper.succeed();
	}

	@GameTest
	public void pearSaplingGrowsATree(GameTestHelper helper) {
		growsIntoItsTree(helper, OrchardTree.PEAR);
	}

	@GameTest
	public void peachSaplingGrowsATree(GameTestHelper helper) {
		growsIntoItsTree(helper, OrchardTree.PEACH);
	}

	@GameTest
	public void lemonSaplingGrowsATree(GameTestHelper helper) {
		growsIntoItsTree(helper, OrchardTree.LEMON);
	}

	@GameTest
	public void orangeSaplingGrowsATree(GameTestHelper helper) {
		growsIntoItsTree(helper, OrchardTree.ORANGE);
	}

	@GameTest
	public void plumSaplingGrowsATree(GameTestHelper helper) {
		growsIntoItsTree(helper, OrchardTree.PLUM);
	}

	@GameTest
	public void bananaSaplingGrowsATree(GameTestHelper helper) {
		growsIntoItsTree(helper, OrchardTree.BANANA);
	}

	// ---------------------------------------------------------------- the fruit

	/** Where tree {@code i}'s log stands at height {@code y}: two columns of three, so every tree fits the test area. */
	private static BlockPos column(int i, int y) {
		return new BlockPos(2 + 4 * (i / 3), y, 1 + 2 * (i % 3));
	}

	/** Where tree {@code i}'s block goes in a test of single blocks: two rows of four. */
	private static BlockPos spot(int i, int y) {
		return new BlockPos(1 + 2 * (i % 4), y, 2 + 3 * (i / 4));
	}

	/**
	 * For each tree, leaves the tree grew (over air, by a log) blossom and then ripen on random ticks, and a right-click
	 * picks the fruit, which drops below; leaves a player placed never fruit.
	 */
	@GameTest(maxTicks = 40)
	public void leavesBlossomRipenAndArePicked(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		OrchardTree[] trees = OrchardTree.values();
		for (int i = 0; i < trees.length; i++) {
			OrchardTree tree = trees[i];
			BlockPos log = column(i, 4);
			BlockPos natural = log.east();
			BlockPos placed = log.west();
			helper.setBlock(log, trunk(tree));
			helper.assertTrue(block(tree.leaves()) instanceof OrchardLeavesBlock leavesBlock && leavesBlock.tree() == tree,
					tree.leaves() + " are the " + tree.id + " tree's leaves");
			BlockState leaves = block(tree.leaves()).defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
			helper.setBlock(natural, leaves.setValue(LeavesBlock.PERSISTENT, false));
			helper.setBlock(placed, leaves.setValue(LeavesBlock.PERSISTENT, true));
			boolean blossomed = false;
			for (int tick = 0; tick < 400; tick++) {
				for (BlockPos pos : new BlockPos[] {natural, placed}) {
					helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
				}
				blossomed |= helper.getBlockState(natural).getValue(FruitingLeavesBlock.FRUIT) == 1;
			}
			helper.assertTrue(blossomed, "The " + tree.id + " tree's leaves should blossom on the way");
			helper.assertTrue(helper.getBlockState(natural).getValue(FruitingLeavesBlock.FRUIT) == FruitingLeavesBlock.RIPE,
					"The " + tree.id + " tree's leaves should ripen, found " + helper.getBlockState(natural));
			helper.assertTrue(helper.getBlockState(placed).getValue(FruitingLeavesBlock.FRUIT) == 0, "Placed " + tree.leaves() + " should not fruit");
			helper.useBlock(natural, helper.makeMockPlayer(GameType.SURVIVAL));
			helper.assertTrue(helper.getBlockState(natural).getValue(FruitingLeavesBlock.FRUIT) == 0, "Picking should leave bare leaves");
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < trees.length; i++) {
				helper.assertItemEntityPresent(item(trees[i].id), column(i, 4).east(), 2.0);
			}
		});
	}

	/** Ripe leaves broken (by hand, no shears) drop their fruit. */
	@GameTest(maxTicks = 40)
	public void ripeLeavesBrokenDropTheirFruit(GameTestHelper helper) {
		OrchardTree[] trees = OrchardTree.values();
		for (int i = 0; i < trees.length; i++) {
			BlockPos pos = spot(i, 3);
			helper.setBlock(pos.below(), Blocks.STONE);
			helper.setBlock(pos, block(trees[i].leaves()).defaultBlockState().setValue(LeavesBlock.DISTANCE, 1)
					.setValue(LeavesBlock.PERSISTENT, false).setValue(FruitingLeavesBlock.FRUIT, FruitingLeavesBlock.RIPE));
			helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
		}
		helper.succeedWhen(() -> {
			for (int i = 0; i < trees.length; i++) {
				helper.assertItemEntityPresent(item(trees[i].id), spot(i, 3), 1.5);
			}
		});
	}

	/** A fruit crafts into its seed; the seed, used on dirt, plants the tree's sapling. */
	@GameTest(maxTicks = 20)
	public void fruitGivesSeedsThatPlantSaplings(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		OrchardTree[] trees = OrchardTree.values();
		for (int i = 0; i < trees.length; i++) {
			OrchardTree tree = trees[i];
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(tree.seed))).isPresent(),
					"A " + tree.id + " crafts into " + tree.seed);
			BlockPos ground = spot(i, 1);
			helper.setBlock(ground, Blocks.DIRT);
			BlockPos absolute = helper.absolutePos(ground);
			player.setPos(absolute.getX() + 0.5, absolute.getY() + 1, absolute.getZ() + 3.5);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(tree.seed)));
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
			player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
			helper.assertBlockPresent(block(tree.sapling()), ground.above());
			helper.assertTrue(player.getMainHandItem().isEmpty(), "Planting uses the " + tree.seed);
		}
		helper.succeed();
	}

	/** Orange juice and lemonade are drunk even when full, give their effects and leave their bottles. */
	@GameTest(maxTicks = 20)
	public void juicesAreDrinks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// In survival: a creative player keeps the drink and gets no bottle back.
		player.setGameMode(GameType.SURVIVAL);
		for (String juice : List.of("orange_juice", "lemonade")) {
			FoodProperties food = new ItemStack(item(juice)).get(DataComponents.FOOD);
			helper.assertTrue(food != null && food.canAlwaysEat(), juice + " can be drunk on a full stomach");
			helper.assertTrue(new ItemStack(item(juice)).finishUsingItem(level, player).is(Items.GLASS_BOTTLE), juice + " leaves its bottle");
		}
		helper.assertTrue(player.hasEffect(MobEffects.HEALTH_BOOST) && player.hasEffect(MobEffects.SPEED),
				"Orange juice gives Health Boost and lemonade Speed");
		helper.succeed();
	}

	/**
	 * The recipes (seeds, juices, the two pies' raw pies and the three preserves in the Cooking Pot), every leaves' and
	 * sapling's loot table, and each tree's wild patch and checked placement load; peaches, sugar and a jar cook into peach
	 * preserves.
	 */
	@GameTest
	public void orchardDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> recipes = new ArrayList<>(List.of("orange_juice", "lemonade", PieFilling.PEACH.rawPie(), PieFilling.LEMON.rawPie(),
				"pot_cooking/orange_marmalade", "pot_cooking/peach_preserves", "pot_cooking/pear_butter"));
		for (String id : recipes) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
		for (OrchardTree tree : OrchardTree.values()) {
			for (String table : List.of("blocks/" + tree.leaves(), "blocks/" + tree.sapling())) {
				helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
						!= LootTable.EMPTY, "Loot table " + table + " loads");
			}
			for (String feature : List.of("patch_" + tree.id + "_tree", tree.id + "_checked")) {
				ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id(feature));
				helper.assertTrue(placed.get(key).isPresent(), "Wild " + tree.id + " trees are placed (" + feature + ")");
			}
		}
		for (Map.Entry<String, List<ItemStack>> pot : Map.of(
				"peach_preserves", List.of(new ItemStack(item("mason_jar")), new ItemStack(item("peach"), 3), new ItemStack(Items.SUGAR, 2)),
				"pear_butter", List.of(new ItemStack(item("mason_jar")), new ItemStack(item("pear"), 4), new ItemStack(Items.SUGAR))).entrySet()) {
			List<ItemStack> slots = new ArrayList<>(pot.getValue());
			while (slots.size() < CookingPotBlockEntity.INPUTS) {
				slots.add(ItemStack.EMPTY);
			}
			Optional<CookingPotRecipe.Match> match = CookingPotRecipe.find(level.getServer(), slots);
			helper.assertTrue(match.isPresent() && match.get().recipe().output().create().is(item(pot.getKey())),
					"The Cooking Pot makes " + pot.getKey() + " from " + pot.getValue());
		}
		helper.succeed();
	}
}
