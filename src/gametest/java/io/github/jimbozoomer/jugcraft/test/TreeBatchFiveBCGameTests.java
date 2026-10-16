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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;

public class TreeBatchFiveBCGameTests {
    static final List<String> SHAPES = List.of("hawthorn", "hawthorn_bush", "persimmon");
    private static final BlockPos AT = new BlockPos(38000, 100, -38000);
    static Block block(String id) { return JugcraftAgriculture.block(id); }
    static String fruit(String tree) { return "persimmon"; }
    static Block timber(String tree) { return block(tree.equals("persimmon") ? "persimmon_log" : "fruitwood_log"); }

    @GameTest(maxTicks = 400)
    public void shapesAndSaplingsVaryWithinBoundsAndProtectSoilAndObstacles(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String shape : SHAPES) {
            String tree = shape.startsWith("hawthorn") ? "hawthorn" : shape;
            var hashes = new HashSet<Long>();
            for (long seed : TreeBatchFourBCGameTests.SEEDS) {
                TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
                helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), "Shape failed " + shape);
                var size = TreeBatchTwoGameTests.measure(level, AT);
                Jugcraft.LOGGER.info("[tree 5bc shape] {} seed {}: {}", shape, seed, size);
                helper.assertTrue(size.logs() >= 1 && size.leaves() >= 15, "Empty crown " + shape);
                helper.assertTrue(shape.equals("hawthorn_bush") ? size.logs() == 1 && size.height() <= 3 && size.width() <= 5
                        : shape.equals("hawthorn") ? size.height() >= 4 && size.height() <= 5 && size.width() == 6
                        : size.height() >= 7 && size.height() <= 10 && size.width() == 5, "Outside size envelope " + size);
                int exposed = 0;
                for (var at : BlockPos.betweenClosed(AT.offset(-12,0,-12), AT.offset(12,16,12))) {
                    var state = level.getBlockState(at);
                    if (state.is(BlockTags.LOGS)) helper.assertTrue(state.is(timber(tree)), "Wrong timber");
                    if (state.is(BlockTags.LEAVES)) {
                        helper.assertTrue(state.is(block(tree + "_leaves")), "Wrong leaves");
                        if (tree.equals("persimmon")) helper.assertTrue(at.getY() >= AT.getY() + 2, "Oval crown must leave a visible lower trunk");
                    }
                    if (tree.equals("persimmon") && state.is(block(tree + "_leaves")) && FruitingLeavesBlock.canFruit(state, level, at)) exposed++;
                }
                if (tree.equals("persimmon")) helper.assertTrue(exposed > 0, "No fruit-capable crown leaves");
                hashes.add(size.hash());
            }
            helper.assertTrue(hashes.size() >= 3, "No shape variation");
            TreeBatchTwoGameTests.reset(level, AT, Blocks.STONE);
            helper.assertTrue(!TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), "Shape accepted stone");
        }
        for (String tree : List.of("hawthorn", "persimmon")) {
            var sapling = (SaplingBlock) block(tree + "_sapling");
            var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            int bushes = 0, trees = 0;
            for (long seed : TreeBatchFourBCGameTests.SEEDS) {
                TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
                level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
                sapling.advanceTree(level, AT, ready, RandomSource.create(seed));
                var size = TreeBatchTwoGameTests.measure(level, AT);
                helper.assertTrue(size.logs() > 0, "Sapling failed");
                if (size.logs() == 1) bushes++; else trees++;
            }
            if (tree.equals("hawthorn")) helper.assertTrue(bushes > 0 && trees > 0, "Sapling cannot grow both forms");
            TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
            level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
            level.setBlock(AT.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            sapling.advanceTree(level, AT, ready, RandomSource.create(1));
            helper.assertTrue(level.getBlockState(AT).equals(ready) && level.getBlockState(AT.above()).is(Blocks.DIAMOND_BLOCK), "Growth damaged obstacle/sapling");
            TreeBatchTwoGameTests.reset(level, AT, Blocks.STONE);
            helper.assertTrue(!ready.canSurvive(level, AT), "Sapling accepts stone");
        }
        TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
        helper.succeed();
    }

    @GameTest
    public void hawsDropInAllSeasonsWithoutPlacedLeafOrSilkDuplication(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3,3,3));
        var silk = new ItemStack(Items.IRON_AXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        var before = JugcraftSeasons.settings();
        try {
            for (var mode : SeasonCalendar.Mode.values()) for (String shape : List.of("hawthorn", "hawthorn_bush")) {
                JugcraftSeasons.setMode(level.getServer(), mode);
                TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
                helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), "Hawthorn season feature failed");
                for (var at : BlockPos.betweenClosed(AT.offset(-12,0,-12), AT.offset(12,16,12))) {
                    var state = level.getBlockState(at);
                    if (state.is(block("hawthorn_leaves"))) helper.assertTrue(state.getValue(SeasonalLeavesBlock.SEASON) == JugcraftAgriculture.HAWTHORN_LEAVES.on(JugcraftSeasons.today(),at), "Wrong hawthorn generated season");
                }
            }
        } finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
        TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
        for (var look : SeasonalLeavesBlock.Foliage.values()) {
            var state = block("hawthorn_leaves").defaultBlockState().setValue(SeasonalLeavesBlock.SEASON, look).setValue(FruitingLeavesBlock.FRUIT, 2);
            int haws = 0, saplings = 0;
            for (int n = 0; n < 2048; n++) {
                for (var drop : Block.getDrops(state, level, pos, null)) {
                    if (drop.is(JugcraftAgriculture.item("haws"))) haws += drop.getCount();
                    if (drop.is(block("hawthorn_sapling").asItem())) saplings++;
                }
                helper.assertTrue(Block.getDrops(state.setValue(LeavesBlock.PERSISTENT, true).setValue(FruitingLeavesBlock.FRUIT, 0), level, pos, null).stream()
                        .noneMatch(d -> d.is(JugcraftAgriculture.item("haws"))), "Placed leaves farm haws");
            }
            helper.assertTrue(haws >= 2048 && haws <= 4096 && saplings > 0, "Season lost ripe haws or renewal " + look);
            for (int fruit = 0; fruit < 2; fruit++) helper.assertTrue(Block.getDrops(state.setValue(FruitingLeavesBlock.FRUIT, fruit), level, pos, null).stream()
                    .noneMatch(d -> d.is(JugcraftAgriculture.item("haws"))), "Unripe/picked leaves duplicated haws");
            for (var tool : List.of(silk, new ItemStack(Items.SHEARS))) {
                var drops = Block.getDrops(state, level, pos, null, null, tool);
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(state.getBlock().asItem()), "Silk/shears duplicates fruit");
            }
        }
        helper.assertTrue(new ItemStack(JugcraftAgriculture.item("haws")).is(TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.withDefaultNamespace("fox_food"))), "Haws not fox food");
        helper.succeed();
    }

    @GameTest
    public void hawsRightClickHarvestsOnceAndRegrowsOnHedges(GameTestHelper helper) {
        var level = helper.getLevel();
        var relative = new BlockPos(3, 3, 3);
        var pos = helper.absolutePos(relative);
        var leaves = (SeasonalFruitLeavesBlock) block("hawthorn_leaves");
        var natural = leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var area = new AABB(pos).inflate(3);
        var before = JugcraftSeasons.settings();
        try {
            for (var mode : SeasonCalendar.Mode.values()) {
                JugcraftSeasons.setMode(level.getServer(), mode);
                level.setBlock(pos.west(), block("fruitwood_log").defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (var side : List.of(Direction.NORTH, Direction.SOUTH)) level.setBlock(pos.relative(side), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(pos, natural, Block.UPDATE_CLIENTS);
                var random = RandomSource.create(5101);
                for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, random);
                var ripe = level.getBlockState(pos);
                helper.assertTrue(ripe.getValue(FruitingLeavesBlock.FRUIT) == 2, "Grounded hedge failed ripening in " + mode);
                int countBefore = level.getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(JugcraftAgriculture.item("haws"))).stream().mapToInt(e -> e.getItem().getCount()).sum();
                helper.useBlock(relative, player);
                var drops = level.getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(JugcraftAgriculture.item("haws")));
                int countAfter = drops.stream().mapToInt(e -> e.getItem().getCount()).sum();
                helper.assertTrue(countAfter - countBefore >= 1 && countAfter - countBefore <= 2, "Right-click did not drop 1-2 haws");
                helper.assertTrue(drops.stream().allMatch(e -> e.blockPosition().equals(pos.east())), "Haws dropped inside hedge soil");
                helper.assertTrue(level.getBlockState(pos).equals(ripe.setValue(FruitingLeavesBlock.FRUIT, 0)), "Picking changed more than fruit stage");
                helper.useBlock(relative, player);
                helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, area, e -> e.getItem().is(JugcraftAgriculture.item("haws"))).stream().mapToInt(e -> e.getItem().getCount()).sum() == countAfter, "Repeated click duplicated haws");
                for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, random);
                helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 2, "Picked hedge did not regrow");
                for (var item : drops) item.discard();
            }
            for (boolean persistent : List.of(true, false)) {
                level.setBlock(pos, natural.setValue(LeavesBlock.PERSISTENT, persistent), Block.UPDATE_CLIENTS);
                // Natural but fully enclosed leaves, and exposed player-placed leaves, cannot grow haws.
                level.setBlock(pos.east(), persistent ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int i = 0; i < 512; i++) level.getBlockState(pos).randomTick(level, pos, RandomSource.create(i));
                helper.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 0, "Placed/enclosed leaves farm haws");
            }
            level.setBlock(pos, natural.setValue(LeavesBlock.DISTANCE, 7), Block.UPDATE_CLIENTS);
            level.getBlockState(pos).randomTick(level, pos, RandomSource.create(1));
            helper.assertTrue(level.getBlockState(pos).isAir(), "Unsupported hawthorn leaves did not decay");
        } finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); }
        helper.succeed();
    }

    @GameTest(maxTicks = 300)
    public void hawsRipenOnGeneratedTreesAndBushes(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String shape : List.of("hawthorn", "hawthorn_bush")) {
            TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
            helper.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), AT), "Hawthorn failed to grow");
            int ripe = 0;
            var random = RandomSource.create(5102);
            for (var p : BlockPos.betweenClosed(AT.offset(-8, 0, -8), AT.offset(8, 8, 8))) {
                var state = level.getBlockState(p);
                if (!state.is(block("hawthorn_leaves")) || !FruitingLeavesBlock.canFruit(state, level, p)) continue;
                for (int i = 0; i < 512; i++) level.getBlockState(p).randomTick(level, p, random);
                if (level.getBlockState(p).getValue(FruitingLeavesBlock.FRUIT) == 2) ripe++;
            }
            helper.assertTrue(ripe >= 4, "Generated " + shape + " cannot grow accessible haws");
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
			for (String tree : List.of("persimmon")) for (var mode : SeasonCalendar.Mode.values()) {
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
			for (String tree : List.of("persimmon")) {
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
			for (String tree : List.of("persimmon")) {
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
	public void persimmonRecipesAndBerryConsumersUseExistingRoutes(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = helper.makeMockPlayer(GameType.SURVIVAL);
		var pos = new BlockPos(3, 3, 3);
		var plank = block("persimmon_planks").asItem();
		for (String form : List.of("log", "wood")) for (var axis : Direction.Axis.values()) {
			helper.setBlock(pos, block("persimmon_" + form).defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
			var axe = new ItemStack(Items.IRON_AXE); player.setItemInHand(InteractionHand.MAIN_HAND, axe); helper.useBlock(pos, player);
			helper.assertTrue(helper.getBlockState(pos).is(block("stripped_persimmon_" + form)) && helper.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) == axis && axe.getDamageValue() == 1, "Stripping failed");
		}
		for (String form : List.of("persimmon_log", "persimmon_wood", "stripped_persimmon_log", "stripped_persimmon_wood")) {
			PineWoodGameTests.craft(helper, plank, 4, 1, 1, block(form).asItem());
			var saw = MachineRecipes.find(level, MachineKind.SAWMILL, new ItemStack(block(form))).orElseThrow();
			helper.assertTrue(saw.output().create().is(plank) && saw.output().create().getCount() == 6, "Sawmill output wrong");
		}
		PineWoodGameTests.craft(helper, block("persimmon_stairs").asItem(), 4, 3, 3, plank, null, null, plank, plank, null, plank, plank, plank);
		PineWoodGameTests.craft(helper, block("persimmon_slab").asItem(), 6, 3, 1, plank, plank, plank);
		PineWoodGameTests.craft(helper, block("persimmon_fence").asItem(), 3, 3, 2, plank, Items.STICK, plank, plank, Items.STICK, plank);
		PineWoodGameTests.craft(helper, block("persimmon_fence_gate").asItem(), 1, 3, 2, Items.STICK, plank, Items.STICK, Items.STICK, plank, Items.STICK);

        for (String tree : List.of("hawthorn", "persimmon")) {
            var farm = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(block(tree + "_sapling"))).orElseThrow();
            helper.assertTrue(farm.output().create().is(timber(tree).asItem()) && farm.byproducts().stream().anyMatch(p -> p.chance() == 1 && p.result().create().is(block(tree + "_sapling").asItem())), "Farm lost timber or sapling");
        }
        helper.assertTrue(new ItemStack(JugcraftAgriculture.item("persimmon")).is(TagKey.create(Registries.ITEM, Jugcraft.id("fermentable"))), "Persimmon not fermentable");
        PineWoodGameTests.craft(helper, JugcraftAgriculture.item("raw_persimmon_pie"), 1, 2, 2,
                JugcraftAgriculture.item("pastry_dough"), JugcraftAgriculture.item("persimmon"), JugcraftAgriculture.item("persimmon"), Items.SUGAR);
        helper.succeed();
    }

    @GameTest(maxTicks = 400)
    public void bothPreservesCookInReusableJars(GameTestHelper helper) {
        for (int i = 0; i < 2; i++) {
            var at = new BlockPos(3+i*3,3,3);
            helper.setBlock(at.below(), Blocks.CAMPFIRE); helper.setBlock(at, block("cooking_pot"));
            var pot = helper.getBlockEntity(at, CookingPotBlockEntity.class);
            pot.setItem(0, new ItemStack(JugcraftAgriculture.item("mason_jar")));
            pot.setItem(1, new ItemStack(JugcraftAgriculture.item(i == 0 ? "haws" : "persimmon"),6));
            pot.setItem(2, new ItemStack(Items.SUGAR,2));
        }
        helper.succeedWhen(() -> {
            for (int i = 0; i < 2; i++) {
                var pot = helper.getBlockEntity(new BlockPos(3+i*3,3,3), CookingPotBlockEntity.class);
                var made = pot.getItem(CookingPotBlockEntity.RESULT);
                helper.assertTrue(made.is(JugcraftAgriculture.item(i == 0 ? "haw_jelly" : "persimmon_preserve")), "Preserve did not cook");
                var contents = made.get(JugcraftAgriculture.JAR_CONTENTS);
                helper.assertTrue(contents != null && contents.servings() == PreserveJarItem.SERVINGS, "Lost reusable jar contents");
            }
        });
    }

    @GameTest(maxTicks = 40)
    public void persimmonPieBakesAndAppendsAfterEverySavedFilling(GameTestHelper helper) {
        String[] old = {"APPLE", "PUMPKIN_CREAM", "CRANBERRY", "SWEET_POTATO", "CHESTNUT", "CHOCOLATE", "SWEET_BERRY", "PEACH", "LEMON",
            "CARROT_CAKE", "BIRTHDAY_CAKE", "ICE_CREAM_CAKE", "RED_VELVET_CAKE", "CHEESECAKE", "COFFEE_CAKE", "APPLE_CAKE", "STRAWBERRY_PIE",
            "BLUEBERRY_TART", "PLUM_PIE", "BANOFFEE_PIE", "SWEET_BERRY_TART", "LEMON_TART", "WHIPPED_PUMPKIN_PIE", "PORK_PIE", "STRAWBERRY_TART", "COFFEE_TART"};
        for (int i = 0; i < old.length; i++) helper.assertTrue(PieFilling.values()[i].name().equals(old[i]), "Saved filling moved " + old[i]);
        helper.assertTrue(PieFilling.PERSIMMON.ordinal() == 26 && HearthOvenBlockEntity.rawFilling(new ItemStack(JugcraftAgriculture.item("raw_persimmon_pie"))) == PieFilling.PERSIMMON, "New filling ID wrong");
        var at = new BlockPos(3,2,3); helper.setBlock(at, block("hearth_oven"));
        var oven = helper.getBlockEntity(at, HearthOvenBlockEntity.class);
        oven.set(1000, HearthOvenBlockEntity.MAX_HEAT, PieFilling.PERSIMMON, HearthOvenBlockEntity.BAKED - 6);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(oven.takeOut(helper.makeMockPlayer(GameType.SURVIVAL)).is(JugcraftAgriculture.item("persimmon_pie")), "Persimmon pie did not bake");
            helper.succeed();
        });
    }
}
