package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineRecipes;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

public class TropicalTreeGameTests {
    static final List<String> SHAPES = List.of("flame_tree", "sea_grape", "osmanthus", "banyan", "mango");
    private static final BlockPos AT = new BlockPos(57000, 100, -57000);
    static Block block(String id) { return JugcraftAgriculture.block(id); }

    @GameTest(maxTicks = 600)
    public void treesGrowFromSaplingsVaryAndPreserveObstacles(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String tree : SHAPES) {
            var hashes = new HashSet<Long>(); int roots = 0;
            for (long seed : TreeBatchFourBCGameTests.SEEDS) {
                TreeBatchTwoGameTests.reset(level, AT, tree.equals("sea_grape") ? Blocks.SAND : Blocks.GRASS_BLOCK);
                helper.assertTrue(TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), "Failed " + tree);
                var size = TreeBatchTwoGameTests.measure(level, AT); hashes.add(size.hash());
                Jugcraft.LOGGER.info("[tropical shape] {} seed {}: {}", tree, seed, size);
                int minHeight = switch (tree) { case "sea_grape" -> 2; case "osmanthus" -> 4; case "banyan", "mango" -> 7; default -> 5; };
                int maxHeight = switch (tree) { case "sea_grape" -> 3; case "osmanthus" -> 6; case "banyan" -> 14; case "mango" -> 9; default -> 8; };
                int maxWidth = tree.equals("banyan") ? 19 : tree.equals("flame_tree") ? 15 : 9;
                helper.assertTrue(size.height() >= minHeight && size.height() <= maxHeight && size.width() <= maxWidth && size.leaves() >= 8, "Shape bounds " + tree + " " + size);
                if (tree.equals("sea_grape")) helper.assertTrue(size.logs() == 1, "Bush has an exposed tall trunk");
                for (var p : BlockPos.betweenClosed(AT.offset(-14, 0, -14), AT.offset(14, 18, 14))) {
                    var state = level.getBlockState(p);
                    if (state.is(Blocks.HANGING_ROOTS)) {
                        roots++; helper.assertTrue(state.canSurvive(level, p) && level.getBlockState(p.above()).is(block("banyan_leaves")), "Unsupported aerial root");
                    }
                    if (state.is(BlockTags.LOGS)) {
                        var wood = switch (tree) { case "flame_tree" -> Blocks.ACACIA_LOG; case "sea_grape" -> Blocks.OAK_LOG; case "osmanthus" -> block("olive_log"); case "banyan" -> block("fig_log"); default -> block("mango_log"); };
                        helper.assertTrue(state.is(wood), "Wrong tree timber " + tree);
                    }
                    if (state.is(BlockTags.LEAVES)) {
                        helper.assertTrue(state.is(block(tree + "_leaves")), "Wrong foliage");
                        helper.assertTrue(!state.getValue(LeavesBlock.PERSISTENT), "Generated leaves are persistent");
                        if (state.hasProperty(FruitingLeavesBlock.FRUIT)) helper.assertTrue(state.getValue(FruitingLeavesBlock.FRUIT) == 0, "Generated ripe");
                    }
                }
                TreeBatchTwoGameTests.reset(level, AT, tree.equals("sea_grape") ? Blocks.SAND : Blocks.GRASS_BLOCK);
                TreeBatchTwoGameTests.feature(level, tree).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
                helper.assertTrue(TreeBatchTwoGameTests.measure(level, AT).equals(size), "Seed changed shape");
            }
            if (tree.equals("banyan")) helper.assertTrue(roots > 0, "Banyan has no aerial roots");
            helper.assertTrue(hashes.size() >= 3, "No shape variation: " + tree);
            var sapling = (SaplingBlock) block(tree + "_sapling"); var ready = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1);
            TreeBatchTwoGameTests.reset(level, AT, tree.equals("sea_grape") ? Blocks.SAND : Blocks.GRASS_BLOCK);
            level.setBlock(AT, ready, Block.UPDATE_CLIENTS);
            helper.assertTrue(ready.canSurvive(level, AT), "Invalid sapling soil");
            level.setBlock(AT.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            sapling.advanceTree(level, AT, ready, RandomSource.create(3));
            helper.assertTrue(level.getBlockState(AT).equals(ready) && level.getBlockState(AT.above()).is(Blocks.DIAMOND_BLOCK), "Obstacle/sapling destroyed");
            level.setBlock(AT.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            sapling.advanceTree(level, AT, ready, RandomSource.create(3));
            helper.assertTrue(!level.getBlockState(AT).is(sapling) && TreeBatchTwoGameTests.measure(level, AT).logs() > 0, "Sapling failed " + tree);
        }
        TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK); helper.succeed();
    }

    @GameTest(maxTicks = 600)
    public void banyanBranchesStayConnectedAndStopAtObstacles(GameTestHelper helper) {
        var level = helper.getLevel(); var wood = block("fig_log");
        for (long seed : TreeBatchFourBCGameTests.SEEDS) {
            TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
            helper.assertTrue(TreeBatchTwoGameTests.feature(level, "banyan").place(level,
                    level.getChunkSource().getGenerator(), RandomSource.create(seed), AT), "Banyan failed");
            var logs = new HashSet<BlockPos>(); int horizontal = 0;
            for (var p : BlockPos.betweenClosed(AT.offset(-8, 0, -8), AT.offset(8, 16, 8))) {
                var state = level.getBlockState(p);
                if (!state.is(wood)) continue;
                logs.add(p.immutable());
                if (state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) horizontal++;
            }
            helper.assertTrue(horizontal >= 12, "Too few spreading branch logs: " + horizontal);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                helper.assertTrue(logs.stream().anyMatch(p -> p.getY() >= AT.getY() + 3
                        && (p.getX() - AT.getX()) * side.getStepX() + (p.getZ() - AT.getZ()) * side.getStepZ() >= 3),
                        "Missing main limb on " + side);
            }
            var reached = new HashSet<BlockPos>(); var queue = new ArrayDeque<BlockPos>();
            queue.add(AT.above()); reached.add(AT.above());
            while (!queue.isEmpty()) {
                var p = queue.remove();
                for (Direction side : Direction.values()) {
                    var next = p.relative(side);
                    if (logs.contains(next) && reached.add(next)) queue.add(next);
                }
            }
            helper.assertTrue(logs.stream().filter(p -> p.getY() >= AT.getY() + 3).allMatch(reached::contains),
                    "Banyan branch has a detached log");
        }
        TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK);
        for (var p : BlockPos.betweenClosed(AT.offset(2, 3, -6), AT.offset(2, 14, 6)))
            level.setBlock(p, Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertTrue(TreeBatchTwoGameTests.feature(level, "banyan").place(level,
                level.getChunkSource().getGenerator(), RandomSource.create(3), AT), "Blocked side prevented whole tree");
        for (var p : BlockPos.betweenClosed(AT.offset(2, 3, -6), AT.offset(2, 14, 6)))
            helper.assertTrue(level.getBlockState(p).is(Blocks.DIAMOND_BLOCK), "Branch overwrote obstacle");
        for (var p : BlockPos.betweenClosed(AT.offset(3, 3, -6), AT.offset(8, 14, 6)))
            helper.assertFalse(level.getBlockState(p).is(wood), "Branch continued through obstacle");
        TreeBatchTwoGameTests.reset(level, AT, Blocks.GRASS_BLOCK); helper.succeed();
    }

    @GameTest
    public void fruitAndFlowersRegrowAndPickOnceByHandOrWithShears(GameTestHelper helper) {
        var level = helper.getLevel(); var pos = new BlockPos(2, 2, 2); var at = helper.absolutePos(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (String tree : List.of("sea_grape", "osmanthus", "mango")) {
            var leaves = (TropicalFruitLeavesBlock) block(tree + "_leaves");
            var young = leaves.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
            helper.setBlock(pos.below(), tree.equals("mango") ? Blocks.AIR : Blocks.DIRT);
            helper.setBlock(pos.north(), Blocks.OAK_LOG); helper.setBlock(pos, young);
            for (int i = 0; i < 300 && helper.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) != 2; i++)
                helper.getBlockState(pos).randomTick(level, at, RandomSource.create(500 + i));
            helper.assertTrue(helper.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 2, "Never ripens " + tree);
            var produce = JugcraftAgriculture.item(tree.equals("sea_grape") ? "sea_grapes" : tree.equals("osmanthus") ? "osmanthus_flowers" : "mango");
            for (var tool : List.of(ItemStack.EMPTY, new ItemStack(Items.SHEARS))) {
                helper.setBlock(pos, young.setValue(FruitingLeavesBlock.FRUIT, 2));
                for (var item : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4))) item.discard();
                player.setItemInHand(InteractionHand.MAIN_HAND, tool); helper.useBlock(pos, player);
                helper.assertTrue(helper.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT) == 0, "Use did not reset " + tree);
                int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4)).stream().filter(e -> e.getItem().is(produce)).mapToInt(e -> e.getItem().getCount()).sum();
                helper.assertTrue(count >= 1 && count <= 2, "Wrong harvest " + count); helper.assertFalse(leaves.pick(level, at), "Duplicate harvest");
            }
            helper.setBlock(pos, young.setValue(LeavesBlock.PERSISTENT, true).setValue(FruitingLeavesBlock.FRUIT, 2));
            helper.assertFalse(leaves.pick(level, at), "Persistent decoration yields fruit");
            var ripe = young.setValue(FruitingLeavesBlock.FRUIT, 2);
            var drops = Block.getDrops(ripe, level, at, null, null, new ItemStack(Items.SHEARS));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(leaves.asItem()), "Shears duplicated fruit");
        }
        helper.assertTrue(CandyFlavour.of(new ItemStack(JugcraftAgriculture.item("osmanthus_flowers"))) == CandyFlavour.OSMANTHUS, "Candy flavour missing");
        helper.succeed();
    }

    @GameTest
    public void aerialRootsHaveOnlyUndersideSupportAndDisappearWithCanopy(GameTestHelper helper) {
        var pos = new BlockPos(2, 3, 2); var at = helper.absolutePos(pos); var level = helper.getLevel();
        var leaves = block("banyan_leaves").defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        helper.setBlock(pos, leaves); helper.setBlock(pos.below(), Blocks.HANGING_ROOTS);
        for (var direction : net.minecraft.core.Direction.values())
            helper.assertTrue(leaves.isFaceSturdy(level, at, direction) == (direction == net.minecraft.core.Direction.DOWN), "Unexpected canopy support face " + direction);
        helper.assertTrue(helper.getBlockState(pos.below()).canSurvive(level, at.below()), "Roots cannot hang");
        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertTrue(helper.getBlockState(pos.below()).isAir(), "Root floats after canopy removal");
        helper.succeed();
    }

    @GameTest
    public void newWoodFamiliesAndSharedTimberRemainRenewable(GameTestHelper helper) {
        var level = helper.getLevel();
        for (String wood : List.of("fig", "mango")) {
            for (String id : List.of(wood + "_log", wood + "_wood", "stripped_" + wood + "_log", "stripped_" + wood + "_wood"))
                PineWoodGameTests.craft(helper, block(wood + "_planks").asItem(), 4, 1, 1, block(id).asItem());
            var saw = MachineRecipes.find(level, MachineKind.SAWMILL, new ItemStack(block(wood + "_log"))).orElseThrow();
            helper.assertTrue(saw.output().create().is(block(wood + "_planks").asItem()) && saw.output().create().getCount() == 6, "Sawmill route");
        }
        for (String tree : SHAPES) {
            var farm = MachineRecipes.find(level, MachineKind.TREE_FARM, new ItemStack(block(tree + "_sapling"))).orElseThrow();
            var expected = switch (tree) { case "banyan" -> block("fig_log"); case "osmanthus" -> block("olive_log"); case "sea_grape" -> Blocks.OAK_LOG; case "flame_tree" -> Blocks.ACACIA_LOG; default -> block("mango_log"); };
            helper.assertTrue(farm.output().create().is(expected.asItem()) && farm.byproducts().stream().anyMatch(p -> p.chance() == 1 && p.result().create().is(block(tree + "_sapling").asItem())), "Tree farm mapping " + tree);
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 600)
    public void tropicalHarvestsCookWithContainersAndMooncakeFilling(GameTestHelper helper) {
        var positions = List.of(new BlockPos(1, 2, 1), new BlockPos(4, 2, 1), new BlockPos(7, 2, 1));
        var inputs = List.of(
            List.of(new ItemStack(JugcraftAgriculture.item("mason_jar")), new ItemStack(JugcraftAgriculture.item("sea_grapes"), 6), new ItemStack(Items.SUGAR, 2)),
            List.of(new ItemStack(JugcraftAgriculture.item("mason_jar")), new ItemStack(JugcraftAgriculture.item("mango"), 4), new ItemStack(JugcraftAgriculture.item("cider_vinegar")), new ItemStack(Items.SUGAR)),
            List.of(new ItemStack(Items.WHEAT, 2), new ItemStack(Items.SUGAR), new ItemStack(Items.EGG), new ItemStack(JugcraftAgriculture.item("osmanthus_flowers"), 2)));
        var pots = new ArrayList<CookingPotBlockEntity>();
        for (int i = 0; i < positions.size(); i++) {
            var at = positions.get(i); helper.setBlock(at.below(), Blocks.CAMPFIRE); helper.setBlock(at, block("cooking_pot"));
            var pot = helper.getBlockEntity(at, CookingPotBlockEntity.class); pots.add(pot);
            for (int slot = 0; slot < inputs.get(i).size(); slot++) pot.setItem(slot, inputs.get(i).get(slot));
        }
        helper.succeedWhen(() -> {
            var outputs = List.of("sea_grape_jelly", "mango_chutney", "osmanthus_mooncake");
            for (int i = 0; i < pots.size(); i++) {
                var output = pots.get(i).getItem(CookingPotBlockEntity.RESULT);
                helper.assertTrue(output.is(JugcraftAgriculture.item(outputs.get(i))) && output.getCount() == (i == 2 ? 4 : 1), "Cooking output " + outputs.get(i));
                if (i < 2) helper.assertTrue(output.get(JugcraftAgriculture.JAR_CONTENTS) != null && output.get(JugcraftAgriculture.JAR_CONTENTS).servings() == 4, "Preserve contents missing");
            }
            helper.assertTrue(pots.get(1).getItem(CookingPotBlockEntity.RESULT + 1).is(Items.GLASS_BOTTLE), "Vinegar bottle lost");
        });
    }

    @GameTest(maxTicks = 200)
    public void driftwoodNeedsShoreAndFullSupportAndNeverOverwrites(GameTestHelper helper) {
        var level = helper.getLevel(); var generator = level.getChunkSource().getGenerator();
        var at = new BlockPos(57400, generator.getSeaLevel(), -57000); var feature = new DriftwoodFeature();
        var axes = new HashSet<net.minecraft.core.Direction.Axis>(); var lengths = new HashSet<Integer>();
        for (long seed : TreeBatchFourBCGameTests.SEEDS) {
            TreeBatchTwoGameTests.reset(level, at, Blocks.SAND);
            level.setBlock(at.offset(4, -1, 4), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertTrue(feature.place(level, generator, RandomSource.create(seed), at), "Driftwood failed on shore");
            int count = 0;
            for (var p : BlockPos.betweenClosed(at.offset(-6, 0, -6), at.offset(6, 0, 6))) if (level.getBlockState(p).is(block("stripped_dead_log"))) {
                count++; axes.add(level.getBlockState(p).getValue(RotatedPillarBlock.AXIS));
                helper.assertTrue(level.getBlockState(p.below()).is(Blocks.SAND), "Floating driftwood");
            }
            helper.assertTrue(count >= 3 && count <= 6, "Wrong log length"); lengths.add(count);
        }
        helper.assertTrue(axes.size() == 2 && lengths.size() >= 3, "Missing driftwood variety");
        TreeBatchTwoGameTests.reset(level, at, Blocks.GRAVEL);
        level.setBlock(at.offset(4, -1, 4), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertTrue(feature.place(level, generator, RandomSource.create(3), at), "Gravel shore rejected");
        for (boolean obstacle : List.of(false, true)) {
            TreeBatchTwoGameTests.reset(level, at, Blocks.SAND);
            for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) level.setBlock(at.relative(d), (obstacle ? Blocks.DIAMOND_BLOCK : Blocks.AIR).defaultBlockState(), Block.UPDATE_CLIENTS);
            if (obstacle) level.setBlock(at.offset(4, -1, 4), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            else level.setBlock(at.offset(4, -1, 4), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            helper.assertFalse(feature.place(level, generator, RandomSource.create(3), at), "Accepted obstacle/inland");
            helper.assertTrue(level.getBlockState(at).isAir(), "Partial write");
        }
        TreeBatchTwoGameTests.reset(level, at, Blocks.SAND); level.setBlock(at.offset(4, -1, 4), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) level.setBlock(at.relative(d).below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertFalse(feature.place(level, generator, RandomSource.create(3), at), "Unsupported log placed");
        helper.succeed();
    }
}
