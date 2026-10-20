package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

public class HangingFruitGameTests {
	static final List<String> SHAPES = List.of("baobab", "giant_baobab", "date_palm");
	private static final BlockPos AT = new BlockPos(54000, 100, -54000);
	static Block block(String name) { return JugcraftAgriculture.block(name); }

	@GameTest(maxTicks = 400)
	public void treesVaryStayBoundedAndHangFruitFromTheirOwnLeaves(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String shape : SHAPES) {
			var hashes = new HashSet<Long>(); int fruit = 0;
			for (long seed : TreeBatchFourBCGameTests.SEEDS) {
				TreeBatchTwoGameTests.reset(level, AT, Blocks.RED_SAND);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), "Placement failed " + shape);
				var size = TreeBatchTwoGameTests.measure(level, AT); hashes.add(size.hash());
				Jugcraft.LOGGER.info("[hanging fruit shape] {} seed {}: {}", shape, seed, size);
				helper.assertTrue(size.height() >= 5 && size.height() <= 15 && size.width() <= 11 && size.leaves() >= 15, "Shape envelope " + shape + " " + size);
				String wood = shape.equals("date_palm") ? "palm" : "baobab";
				for (var p : BlockPos.betweenClosed(AT.offset(-12, 0, -12), AT.offset(12, 18, 12))) {
					var state = level.getBlockState(p);
					if (state.is(BlockTags.LOGS)) helper.assertTrue(state.is(block(wood + "_log")), "Wrong wood");
					if (state.getBlock() instanceof HangingFruitBlock) {
						fruit++; helper.assertTrue(state.canSurvive(level, p), "Floating/wrong-tree fruit");
						helper.assertTrue(state.getValue(HangingFruitBlock.AGE) == 0, "Fruit generated ripe");
					}
				}
				if (shape.equals("giant_baobab")) for (var p : List.of(AT, AT.east(), AT.south(), AT.east().south()))
					helper.assertTrue(level.getBlockState(p).is(block("baobab_log")), "Giant trunk is not two blocks wide");
				TreeBatchTwoGameTests.reset(level, AT, Blocks.RED_SAND);
				TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
				helper.assertTrue(size.equals(TreeBatchTwoGameTests.measure(level, AT)), "Seed is not reproducible");
			}
			helper.assertTrue(hashes.size() >= 3 && fruit > 0, "Missing variation or hanging fruit: " + shape);
		}
		TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK); helper.succeed();
	}

	@GameTest(maxTicks = 400)
	public void saplingsGrowOnSandAndBlockedGiantsKeepAllFourSaplings(GameTestHelper helper) {
		var level = helper.getLevel(); var at = AT.offset(80, 0, 0);
		for (String tree : List.of("baobab", "palm")) {
			var sapling = (SaplingBlock) block(tree + "_sapling");
			var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
			int dates = 0, ordinary = 0;
			for (long seed : TreeBatchFourBCGameTests.SEEDS) {
				TreeBatchTwoGameTests.reset(level, at, Blocks.SAND); level.setBlock(at, ready, Block.UPDATE_CLIENTS);
				helper.assertTrue(ready.canSurvive(level, at), tree + " rejects sand");
				sapling.advanceTree(level, at, ready, RandomSource.create(seed));
				helper.assertTrue(level.getBlockState(at).is(block(tree + "_log")), "Sapling did not grow " + tree);
				if (tree.equals("palm")) {
					boolean hasDates = false;
					for (var p : BlockPos.betweenClosed(at.offset(-8, 0, -8), at.offset(8, 16, 8))) if (level.getBlockState(p).is(block("date_cluster"))) hasDates = true;
					if (hasDates) dates++; else ordinary++;
				}
			}
			if (tree.equals("palm")) helper.assertTrue(dates > 0 && ordinary > 0, "Palm saplings lost one of their forms");
			TreeBatchTwoGameTests.reset(level, at, Blocks.STONE); helper.assertFalse(ready.canSurvive(level, at), "Sapling accepts stone");
		}
		var sapling = (SaplingBlock) block("baobab_sapling"); var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
		for (int corner = 0; corner < 4; corner++) {
			TreeBatchTwoGameTests.reset(level, at, Blocks.RED_SAND);
			var square = List.of(at, at.east(), at.south(), at.east().south());
			for (var p : square) level.setBlock(p, ready, Block.UPDATE_CLIENTS);
			level.setBlock(at.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, square.get(corner), ready, RandomSource.create(3));
			for (var p : square) helper.assertTrue(level.getBlockState(p).equals(ready), "Blocked giant consumed sapling");
			helper.assertTrue(level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK), "Obstacle destroyed");
			level.setBlock(at.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			sapling.advanceTree(level, square.get(corner), ready, RandomSource.create(3));
			for (var p : square) helper.assertTrue(level.getBlockState(p).is(block("baobab_log")), "2x2 failed from corner " + corner);
		}
		TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); helper.succeed();
	}

	@GameTest
	public void fruitRipensHarvestsOnceByHandOrWithShearsAndRequiresItsCanopy(GameTestHelper helper) {
		var level = helper.getLevel(); var pos = new BlockPos(2, 2, 2); var at = helper.absolutePos(pos);
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		for (String id : List.of("baobab_pod", "date_cluster")) {
			boolean dates = id.equals("date_cluster"); var fruit = (HangingFruitBlock) block(id);
			var leaves = block(dates ? "palm_fronds" : "baobab_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
			helper.setBlock(pos.above(), leaves); helper.setBlock(pos, fruit.defaultBlockState());
			var random = RandomSource.create(24);
			for (int i = 0; i < 200 && helper.getBlockState(pos).getValue(HangingFruitBlock.AGE) < 2; i++)
				helper.getBlockState(pos).randomTick(level, at, random);
			helper.assertTrue(helper.getBlockState(pos).getValue(HangingFruitBlock.AGE) == 2, "Never ripens");
			for (var tool : List.of(ItemStack.EMPTY, new ItemStack(Items.SHEARS))) {
				helper.setBlock(pos, fruit.defaultBlockState().setValue(HangingFruitBlock.AGE, 2));
				for (var item : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3))) item.discard();
				player.setItemInHand(InteractionHand.MAIN_HAND, tool); helper.useBlock(pos, player);
				helper.assertTrue(helper.getBlockState(pos).getValue(HangingFruitBlock.AGE) == 0, "Right-click did not reset fruit");
				int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(3)).stream().filter(e -> e.getItem().is(JugcraftAgriculture.item(dates ? "dates" : "baobab_fruit"))).mapToInt(e -> e.getItem().getCount()).sum();
				helper.assertTrue(count >= (dates ? 2 : 1) && count <= (dates ? 4 : 2), "Wrong harvest yield " + count);
				helper.assertFalse(fruit.pick(level, at), "Repeated use duplicated fruit");
			}
			helper.setBlock(pos.above(), Blocks.OAK_LEAVES); helper.assertFalse(fruit.defaultBlockState().canSurvive(level, at), "Wrong canopy accepted");
			helper.assertTrue(helper.getBlockState(pos).isAir(), "Fruit floats after canopy removal");
		}
		helper.succeed();
	}

	@GameTest
	public void fruitLootPreservesOnlyUnripeBlocksAndSeedsAndWoodRemainRenewable(GameTestHelper helper) {
		var level = helper.getLevel(); var at = helper.absolutePos(new BlockPos(2, 2, 2));
		var silk = new ItemStack(Items.IRON_AXE); silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		for (String id : List.of("baobab_pod", "date_cluster")) {
			for (int age = 0; age <= 2; age++) {
				var state = block(id).defaultBlockState().setValue(HangingFruitBlock.AGE, age);
				for (var tool : List.of(new ItemStack(Items.SHEARS), silk)) {
					var drops = Block.getDrops(state, level, at, null, null, tool);
					helper.assertTrue(drops.size() == 1 && drops.getFirst().is(block(id).asItem()) && drops.getFirst().getCount() == 1, "Preservation duplicates fruit");
				}
				var drops = Block.getDrops(state, level, at, null);
				helper.assertTrue(age == 2 ? drops.size() == 1 && drops.getFirst().is(JugcraftAgriculture.item(id.equals("date_cluster") ? "dates" : "baobab_fruit")) : drops.isEmpty(), "Wrong ordinary fruit loot");
			}
		}
		PineWoodGameTests.craft(helper, JugcraftAgriculture.item("baobab_seeds"), 1, 1, 1, JugcraftAgriculture.item("baobab_fruit"));
		helper.assertTrue(block("baobab_sapling").asItem() == JugcraftAgriculture.item("baobab_sapling"), "Seeds replaced the canonical sapling item");
		var planter = helper.makeMockPlayer(GameType.SURVIVAL);
		planter.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftAgriculture.item("baobab_seeds"), 2));
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.SAND);
		helper.useBlock(new BlockPos(2, 1, 2), planter, new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at.below()), Direction.UP, at.below(), false));
		helper.assertTrue(level.getBlockState(at).is(block("baobab_sapling")) && planter.getMainHandItem().getCount() == 1, "Seed did not plant one sapling");
		for (String form : List.of("log", "wood", "stripped_log", "stripped_wood")) {
			String id = form.startsWith("stripped_") ? "stripped_baobab_" + form.substring(9) : "baobab_" + form;
			PineWoodGameTests.craft(helper, block("baobab_planks").asItem(), 4, 1, 1, block(id).asItem());
		}
		var farm = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(block("baobab_sapling"))).orElseThrow();
		helper.assertTrue(farm.output().create().is(block("baobab_log").asItem()) && farm.byproducts().stream().anyMatch(p -> p.chance() == 1 && p.result().create().is(block("baobab_sapling").asItem())), "Tree farm lost renewal");
		var saw = MachineRecipes.find(level, MachineKind.SAWMILL, new ItemStack(block("baobab_log"))).orElseThrow();
		helper.assertTrue(saw.output().create().is(block("baobab_planks").asItem()) && saw.output().create().getCount() == 6, "Sawmill route");
		helper.succeed();
	}
}
