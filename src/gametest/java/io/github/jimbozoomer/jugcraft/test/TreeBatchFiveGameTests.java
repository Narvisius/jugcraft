package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import io.github.jimbozoomer.jugcraft.season.JugcraftSeasons;
import io.github.jimbozoomer.jugcraft.season.SeasonCalendar;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;

public class TreeBatchFiveGameTests {
	static final List<String> SHAPES = List.of("rowan", "elder");
	private static final BlockPos AT = new BlockPos(36000, 100, -36000);
	static Block block(String id) { return JugcraftAgriculture.block(id); }
	static String fruit(String tree) { return tree.equals("rowan") ? "rowan_berries" : "elderberries"; }
	static Block timber(String tree) { return tree.equals("rowan") ? block("fruitwood_log") : Blocks.OAK_LOG; }

	@GameTest(maxTicks = 400)
	public void rowanJellyCooksInAReusableMasonJar(GameTestHelper helper) {
		var at = new BlockPos(3, 3, 3);
		helper.setBlock(at.below(), Blocks.CAMPFIRE);
		helper.setBlock(at, block("cooking_pot"));
		var pot = helper.getBlockEntity(at, CookingPotBlockEntity.class);
		pot.setItem(0, new ItemStack(JugcraftAgriculture.item("mason_jar")));
		pot.setItem(1, new ItemStack(JugcraftAgriculture.item("rowan_berries"), 6));
		pot.setItem(2, new ItemStack(Items.SUGAR, 2));
		helper.succeedWhen(() -> {
			var made = pot.getItem(CookingPotBlockEntity.RESULT);
			helper.assertTrue(made.is(JugcraftAgriculture.item("rowan_jelly")), "Rowan jelly did not cook");
			var contents = made.get(JugcraftAgriculture.JAR_CONTENTS);
			helper.assertTrue(contents != null && contents.servings() == PreserveJarItem.SERVINGS, "Jelly lost the serving/container route");
		});
	}

	@GameTest(maxTicks = 400)
	public void nativeShapesAreRenewableBoundedAndProtectObstacles(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String tree : SHAPES) {
			var hashes = new HashSet<Long>();
			var sapling = (SaplingBlock) block(tree + "_sapling");
			for (long seed : TreeBatchFourBCGameTests.SEEDS) {
				TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
				var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
				level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
				sapling.advanceTree(level, AT, ready, RandomSource.create(seed));
				var size = TreeBatchTwoGameTests.measure(level, AT);
				Jugcraft.LOGGER.info("[tree 5 shape] {} seed {}: {}", tree, seed, size);
				helper.assertTrue(size.logs() >= 1 && size.leaves() >= 15, "Failed sapling or empty crown: " + tree + size);
				helper.assertTrue(tree.equals("elder") ? size.logs() == 1 && size.height() <= 4 && size.width() <= 9
						: size.height() >= 4 && size.height() <= 10 && size.width() <= 11, "Shape outside envelope " + tree + size);
				int exposedFruitLeaves = 0;
				for (var p : BlockPos.betweenClosed(AT.offset(-12, 0, -12), AT.offset(12, 16, 12))) {
					var state = level.getBlockState(p);
					if (state.is(BlockTags.LOGS)) helper.assertTrue(state.is(timber(tree)), "Wrong timber in " + tree);
					if (state.is(BlockTags.LEAVES)) helper.assertTrue(state.is(block(tree + "_leaves")), "Wrong foliage in " + tree);
					if (state.is(block(tree + "_leaves")) && FruitingLeavesBlock.canFruit(state, level, p)) exposedFruitLeaves++;
				}
				helper.assertTrue(exposedFruitLeaves > 0, "Real " + tree + " crown has no leaves capable of bearing fruit");
				if (tree.equals("elder")) helper.assertTrue(exposedFruitLeaves == 4, "Elder must retain four fruiting openings");
				hashes.add(size.hash());
			}
			helper.assertTrue(hashes.size() >= 3, "No natural variation for " + tree);
			TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
			var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
			level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
			level.setBlock(AT.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, AT, ready, RandomSource.create(1));
			helper.assertTrue(level.getBlockState(AT).equals(ready) && level.getBlockState(AT.above()).is(Blocks.DIAMOND_BLOCK), "Obstructed tree damaged blocks");
			TreeBatchTwoGameTests.reset(level, AT, Blocks.STONE);
			helper.assertTrue(!ready.canSurvive(level, AT), "Sapling accepted stone");
			helper.assertTrue(!TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), "Tree accepted stone");
			if (tree.equals("elder")) {
				TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
				level.setBlock(AT.north(2), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
				TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT);
				helper.assertTrue(level.getBlockState(AT.north(2)).is(Blocks.DIAMOND_BLOCK), "Fruit opening removed a foreign block");
			}
		}
		TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
		helper.succeed();
	}

	@GameTest
	public void fruitRipensAndHarvestsInEverySeasonWithoutDuplicateHarvest(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(new BlockPos(3, 3, 3));
		var before = JugcraftSeasons.settings();
		try {
			for (String tree : SHAPES) for (var mode : SeasonCalendar.Mode.values()) {
				JugcraftSeasons.setMode(level.getServer(), mode);
				var leaves = (SeasonalFruitLeavesBlock) block(tree + "_leaves");
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(pos.west(), timber(tree).defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.setBlock(pos, leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1), Block.UPDATE_CLIENTS);
				var random = RandomSource.create(5001);
				for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, random);
				var state = level.getBlockState(pos);
				helper.assertTrue(state.getValue(FruitingLeavesBlock.FRUIT) == 2, tree + " failed ripening in " + mode);
				helper.assertTrue(state.getValue(SeasonalLeavesBlock.SEASON) == leaves.schedule().on(JugcraftSeasons.today(), pos), "Wrong seasonal look");
				int count = Block.getDrops(state, level, pos, null).stream().filter(s -> s.is(JugcraftAgriculture.item(fruit(tree)))).mapToInt(ItemStack::getCount).sum();
				helper.assertTrue(count >= 1 && count <= 2, "Ripe berry loot missing");
				helper.assertTrue(leaves.pick(level, pos) && !leaves.pick(level, pos), "Harvest duplicated");
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 0
						&& level.getBlockState(pos).getValue(SeasonalLeavesBlock.SEASON) == state.getValue(SeasonalLeavesBlock.SEASON), "Picking changed season");
				for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, random);
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 2, "Harvest did not renew");
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
		helper.succeed();
	}

	@GameTest
	public void seasonCatchupPreservesFruitAndPlacedBuriedOrUnsupportedLeavesCannotFarm(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(new BlockPos(3, 3, 3));
		var before = JugcraftSeasons.settings();
		try {
			for (String tree : SHAPES) {
				var leaves = (SeasonalFruitLeavesBlock) block(tree + "_leaves");
				var natural = leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
				JugcraftSeasons.setMode(level.getServer(), SeasonCalendar.Mode.SUMMER);
				level.setBlock(pos, natural.setValue(FruitingLeavesBlock.FRUIT, 2), Block.UPDATE_CLIENTS);
				level.setBlock(pos.east(), natural.setValue(FruitingLeavesBlock.FRUIT, 1), Block.UPDATE_CLIENTS);
				JugcraftSeasons.setMode(level.getServer(), SeasonCalendar.Mode.WINTER);
				level.getBlockState(pos).randomTick(level, pos, RandomSource.create(2));
				for (var p : List.of(pos, pos.east())) helper.assertTrue(level.getBlockState(p).getValue(SeasonalLeavesBlock.SEASON) == SeasonalLeavesBlock.Foliage.BARE, "Adjacent leaves did not catch up");
				helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 2 && level.getBlockState(pos.east()).getValue(FruitingLeavesBlock.FRUIT) == 1, "Season change replaced fruit state");
				for (boolean persistent : List.of(true, false)) {
					level.setBlock(pos, natural.setValue(LeavesBlock.PERSISTENT, persistent), Block.UPDATE_CLIENTS);
					level.setBlock(pos.below(), persistent ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
					for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, RandomSource.create(i));
					helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 0, "Placed or buried leaves grew berries");
				}
				level.setBlock(pos, natural.setValue(LeavesBlock.DISTANCE, 7).setValue(FruitingLeavesBlock.FRUIT, 2), Block.UPDATE_CLIENTS);
				level.getBlockState(pos).randomTick(level, pos, RandomSource.create(3));
				helper.assertTrue(level.getBlockState(pos).isAir(), "Ripe unsupported leaves survived decay");
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
		helper.succeed();
	}

	@GameTest(maxTicks = 300)
	public void generatedSeasonsAndRenewableLootWorkForBothTrees(GameTestHelper helper) {
		var level = helper.getLevel();
		var before = JugcraftSeasons.settings();
		try {
			for (String tree : SHAPES) {
				for (var mode : SeasonCalendar.Mode.values()) {
					JugcraftSeasons.setMode(level.getServer(), mode);
					TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
					helper.assertTrue(TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), "Generated tree failed");
					for (var p : BlockPos.betweenClosed(AT.offset(-12, 0, -12), AT.offset(12, 16, 12))) {
						var state = level.getBlockState(p);
						if (state.is(block(tree + "_leaves"))) helper.assertTrue(state.getValue(SeasonalLeavesBlock.SEASON) == ((SeasonalFoliage)state.getBlock()).schedule().on(JugcraftSeasons.today(), p), "Generated season was wrong");
					}
				}
				var ripe = block(tree + "_leaves").defaultBlockState().setValue(FruitingLeavesBlock.FRUIT, 2);
				var silk = new ItemStack(Items.IRON_AXE);
				silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
				for (var tool : List.of(silk, new ItemStack(Items.SHEARS))) {
					var drops = Block.getDrops(ripe, level, AT, null, null, tool);
					helper.assertTrue(drops.size() == 1 && drops.getFirst().is(ripe.getBlock().asItem()), "Shears/Silk duplicated fruit");
				}
				int saplings = 0;
				for (int i = 0; i < 512; i++) for (var drop : Block.getDrops(ripe.setValue(FruitingLeavesBlock.FRUIT, 0), level, AT, null)) {
					helper.assertTrue(drop.is(block(tree + "_sapling").asItem()) || drop.is(Items.STICK), "Unexpected unripe loot");
					if (drop.is(block(tree + "_sapling").asItem())) saplings++;
				}
				helper.assertTrue(saplings > 0, "No renewable saplings");
			}
		} finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
		TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
		helper.succeed();
	}

	@GameTest
	public void fruitwoodRecipesAndBerryConsumersUseExistingRoutes(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var pos = new BlockPos(3, 3, 3);
		var plank = block("fruitwood_planks").asItem();
		for (String form : List.of("log", "wood")) for (var axis : Direction.Axis.values()) {
			helper.setBlock(pos, block("fruitwood_" + form).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
			var axe = new ItemStack(Items.IRON_AXE); player.setItemInHand(InteractionHand.MAIN_HAND, axe); helper.useBlock(pos, player);
			helper.assertTrue(helper.getBlockState(pos).is(block("stripped_fruitwood_" + form)) && helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) == axis && axe.getDamageValue() == 1, "Stripping failed");
		}
		for (String form : List.of("fruitwood_log", "fruitwood_wood", "stripped_fruitwood_log", "stripped_fruitwood_wood")) {
			PineWoodGameTests.craft(helper, plank, 4, 1, 1, block(form).asItem());
			var saw = MachineRecipes.find(level, MachineKind.SAWMILL, new ItemStack(block(form))).orElseThrow();
			helper.assertTrue(saw.output().create().is(plank) && saw.output().create().getCount() == 6, "Sawmill output wrong");
		}
		PineWoodGameTests.craft(helper, block("fruitwood_stairs").asItem(), 4, 3, 3, plank, null, null, plank, plank, null, plank, plank, plank);
		PineWoodGameTests.craft(helper, block("fruitwood_slab").asItem(), 6, 3, 1, plank, plank, plank);
		PineWoodGameTests.craft(helper, block("fruitwood_fence").asItem(), 3, 3, 2, plank, Items.STICK, plank, plank, Items.STICK, plank);
		PineWoodGameTests.craft(helper, block("fruitwood_fence_gate").asItem(), 1, 3, 2, Items.STICK, plank, Items.STICK, Items.STICK, plank, Items.STICK);
		PineWoodGameTests.craft(helper, net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
				net.minecraft.resources.Identifier.withDefaultNamespace("purple_dye")), 1, 1, 1, JugcraftAgriculture.item("elderberries"));
		for (String tree : SHAPES) {
			var farm = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(block(tree + "_sapling"))).orElseThrow();
			helper.assertTrue(farm.output().create().is(timber(tree).asItem()) && farm.byproducts().stream().anyMatch(p -> p.chance() == 1 && p.result().create().is(block(tree + "_sapling").asItem())), "Farm lost timber or sapling");
			helper.assertTrue(new ItemStack(JugcraftAgriculture.item(fruit(tree))).is(TagKey.create(Registries.ITEM, Jugcraft.id("candy_flavours/berry"))), "Berry candy tag missing");
		}
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("rowan_berries")).is(TagKey.create(Registries.ITEM, Jugcraft.id("candle_scents/warding"))), "Warding scent missing");
		helper.assertTrue(new ItemStack(JugcraftAgriculture.item("elderberries")).is(HornedSkullCauldronBlock.FLOATS), "Elderberry floater missing");
		helper.succeed();
	}
}
