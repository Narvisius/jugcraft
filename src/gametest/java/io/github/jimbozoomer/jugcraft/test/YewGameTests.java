package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FruitingLeavesBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.YewLeavesBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.HashSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;

/** Yew placement, renewable growth, fruit and the existing timber processing route. */
public class YewGameTests {
	private static final BlockPos AT = new BlockPos(26000, 100, -26000);
	private static Block block(String name) { return JugcraftAgriculture.block(name); }

	@GameTest(maxTicks = 400)
	public void normalAndAncientYewsVaryAndRespectGround(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String shape : List.of("yew", "ancient_yew")) {
			var hashes = new HashSet<Long>();
			for (long seed : new long[] {1, 2, 3, 4096, 8192, 65537, -7, 987654321, 314159265, -271828182, Long.MAX_VALUE, Long.MIN_VALUE}) {
				TreeBatchTwoGameTests.reset(level, AT, Blocks.DIRT);
				helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), shape + " failed to grow");
				var size = TreeBatchTwoGameTests.measure(level, AT);
				helper.assertTrue(size.height() >= 5 && size.height() <= 14 && size.width() <= 17 && size.leaves() >= 40, shape + " unexpected shape: " + size);
				hashes.add(size.hash());
				TreeBatchTwoGameTests.reset(level, AT, Blocks.DIRT);
				TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
				helper.assertTrue(size.equals(TreeBatchTwoGameTests.measure(level, AT)), shape + " is not reproducible");
				Jugcraft.LOGGER.info("[yew shape] {} seed {}: {}", shape, seed, size);
			}
			helper.assertTrue(hashes.size() >= 3, shape + " lacks shape variation");
			TreeBatchTwoGameTests.reset(level, AT, Blocks.STONE);
			helper.assertTrue(!TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), shape + " grew on stone");
		}
		TreeBatchTwoGameTests.reset(level, AT, Blocks.DIRT);
		helper.succeed();
	}

	@GameTest
	public void saplingsGrowAndBlockedAncientTreesKeepTheirSquare(GameTestHelper helper) {
		var level = helper.getLevel();
		var at = AT.offset(80, 0, 0);
		var sapling = (SaplingBlock) block("yew_sapling");
		TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
		var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
		level.setBlock(at, ready, Block.UPDATE_CLIENTS);
		sapling.advanceTree(level, at, ready, RandomSource.create(1));
		helper.assertTrue(level.getBlockState(at).is(block("yew_log")), "Single yew sapling did not grow");
		TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
		var square = List.of(at, at.east(), at.south(), at.east().south());
		for (int i = 0; i < 4; i++) level.setBlock(square.get(i), ready.setValue(SaplingBlock.STAGE, i % 2), Block.UPDATE_CLIENTS);
		var original = square.stream().map(level::getBlockState).toList();
		level.setBlock(at.east().above(2), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		sapling.advanceTree(level, square.get(3), ready, RandomSource.create(2));
		for (int i = 0; i < 4; i++) helper.assertTrue(level.getBlockState(square.get(i)).equals(original.get(i)), "Blocked ancient yew consumed a sapling");
		level.setBlock(at.east().above(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		sapling.advanceTree(level, square.get(3), ready, RandomSource.create(2));
		for (var pos : square) helper.assertTrue(level.getBlockState(pos).is(block("yew_log")), "Ancient yew failed to make its 2x2 base");
		TreeBatchTwoGameTests.reset(level, at, Blocks.DIRT);
		helper.succeed();
	}

	@GameTest
	public void naturalNeedlesRipenOnceAndUnsupportedRipeLeavesStillDecay(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(new BlockPos(3, 3, 3));
		var leaves = (YewLeavesBlock) block("yew_needles");
		var natural = leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1).setValue(LeavesBlock.PERSISTENT, false);
		level.setBlock(pos.west(), block("yew_log").defaultBlockState(), Block.UPDATE_CLIENTS);
		level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		for (boolean persistent : List.of(true, false)) {
			level.setBlock(pos, natural.setValue(LeavesBlock.PERSISTENT, persistent), Block.UPDATE_CLIENTS);
			var random = RandomSource.create(3021);
			for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, random);
			helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == (persistent ? 0 : 2), "Incorrect yew fruiting eligibility");
		}
		helper.assertTrue(!level.getBlockState(pos).isRandomlyTicking(), "Supported ripe evergreen leaves still random tick");
		var drops = Block.getDrops(level.getBlockState(pos), level, pos, null);
		int fruit = drops.stream().filter(s -> s.is(JugcraftAgriculture.item("yew_arils"))).mapToInt(ItemStack::getCount).sum();
		helper.assertTrue(fruit >= 1 && fruit <= 2, "Ripe needles should drop one or two arils");
		helper.assertTrue(leaves.pick(level, pos) && !leaves.pick(level, pos), "Yew harvest duplicated or failed");
		helper.assertTrue(level.getBlockState(pos).isRandomlyTicking(), "Harvested leaves do not resume ripening");
		level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, RandomSource.create(i));
		helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 0, "Buried needles grew arils");
		var unsupported = natural.setValue(LeavesBlock.DISTANCE, 7).setValue(FruitingLeavesBlock.FRUIT, 2);
		level.setBlock(pos, unsupported, Block.UPDATE_CLIENTS);
		helper.assertTrue(unsupported.isRandomlyTicking(), "Unsupported ripe leaves stopped decaying");
		unsupported.randomTick(level, pos, RandomSource.create(1));
		helper.assertTrue(level.getBlockState(pos).isAir(), "Unsupported ripe needles did not decay");
		helper.succeed();
	}

	@GameTest
	public void leavesProvideSaplingsAndShearsDoNotDuplicateArils(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(new BlockPos(2, 2, 2));
		var ripe = block("yew_needles").defaultBlockState().setValue(FruitingLeavesBlock.FRUIT, 2);
		var silk = new ItemStack(Items.IRON_AXE);
		silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
		for (var tool : List.of(new ItemStack(Items.SHEARS), silk)) {
			var drops = Block.getDrops(ripe, level, pos, null, null, tool);
			helper.assertTrue(drops.size() == 1 && drops.getFirst().is(block("yew_needles").asItem())
					&& drops.getFirst().getCount() == 1, "Preserved leaves also duplicated fruit or saplings: " + drops);
		}
		int saplings = 0;
		for (int i = 0; i < 512; i++) {
			var drops = Block.getDrops(ripe.setValue(FruitingLeavesBlock.FRUIT, 0), level, pos, null);
			for (var stack : drops) {
				helper.assertTrue(stack.is(JugcraftAgriculture.item("yew_sapling")) || stack.is(Items.STICK), "Unripe leaves dropped an unexpected item");
				if (stack.is(JugcraftAgriculture.item("yew_sapling"))) saplings += stack.getCount();
			}
		}
		helper.assertTrue(saplings > 0, "Yew has no renewable sapling drop");
		var recipe = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(JugcraftAgriculture.item("yew_sapling")))
				.orElseThrow();
		var output = recipe.output().create();
		helper.assertTrue(output.is(JugcraftAgriculture.item("yew_log")) && output.getCount() == 6, "Wrong yew tree-farm timber");
		helper.assertTrue(recipe.byproducts().stream().anyMatch(extra -> extra.chance() == 1.0F
				&& extra.result().create().is(JugcraftAgriculture.item("yew_sapling")) && extra.result().create().getCount() == 1),
				"Tree farm did not return exactly one yew sapling");
		helper.succeed();
	}

	@GameTest
	public void arilsAndTimberUseTheRealRecipes(GameTestHelper helper) {
		var arils = JugcraftAgriculture.item("yew_arils");
		PineWoodGameTests.craft(helper, net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_dye")), 1, 1, 1, arils);
		var player = helper.makeMockServerPlayerInLevel();
		new ItemStack(arils).finishUsingItem(helper.getLevel(), player);
		helper.assertTrue(player.hasEffect(MobEffects.POISON), "Yew arils were not poisonous");
		for (String form : List.of("yew_log", "yew_wood", "stripped_yew_log", "stripped_yew_wood")) {
			var log = JugcraftAgriculture.item(form);
			PineWoodGameTests.craft(helper, JugcraftAgriculture.item("yew_planks"), 4, 1, 1, log);
			var recipe = MachineRecipes.find(helper.getLevel(), MachineKind.SAWMILL, new ItemStack(log));
			helper.assertTrue(recipe.isPresent(), "Sawmill lacks " + form);
			helper.assertTrue(new ItemStack(log).has(DataComponents.COOKING_FUEL), form + " is not fuel");
		}
		var plank = JugcraftAgriculture.item("yew_planks");
		helper.assertTrue(new ItemStack(plank).is(ItemTags.PLANKS), "Yew planks lack their common tag");
		PineWoodGameTests.craft(helper, JugcraftAgriculture.item("yew_stairs"), 4, 3, 3, plank, null, null, plank, plank, null, plank, plank, plank);
		PineWoodGameTests.craft(helper, JugcraftAgriculture.item("yew_slab"), 6, 3, 1, plank, plank, plank);
		PineWoodGameTests.craft(helper, JugcraftAgriculture.item("yew_fence"), 3, 3, 2, plank, Items.STICK, plank, plank, Items.STICK, plank);
		PineWoodGameTests.craft(helper, JugcraftAgriculture.item("yew_fence_gate"), 1, 3, 2, Items.STICK, plank, Items.STICK, Items.STICK, plank, Items.STICK);
		helper.succeed();
	}
}
