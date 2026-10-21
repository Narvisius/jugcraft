package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RainforestGameTests {
    static final List<String> SHAPES = List.of("tree_fern", "rubber_tree", "emergent_mahogany", "banana");
    static final BlockPos AT = new BlockPos(59000, 100, -59000);
    static Block block(String id) { return JugcraftAgriculture.block(id); }
    static void grow(GameTestHelper h, String shape, BlockPos at, long seed) {
        TreeBatchTwoGameTests.reset(h.getLevel(), at, Blocks.GRASS_BLOCK);
        h.assertTrue(TreeBatchTwoGameTests.feature(h.getLevel(), shape).place(h.getLevel(),
                h.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), at), "Failed " + shape + " seed " + seed);
    }
    @GameTest(maxTicks = 600)
    public void treesVaryStayBoundedAndReproduce(GameTestHelper h) {
        for (String shape : SHAPES) {
            var hashes = new HashSet<Long>(); var sizes = new ArrayList<String>();
            for (long seed : TreeBatchFourBCGameTests.SEEDS) {
                grow(h, shape, AT, seed); var size = TreeBatchTwoGameTests.measure(h.getLevel(), AT);
                int[] bounds = switch(shape) { case "tree_fern" -> new int[]{3, 7, 7}; case "rubber_tree" -> new int[]{9, 14, 7}; case "banana" -> new int[]{4, 9, 11}; default -> new int[]{18, 40, 25}; };
                h.assertTrue(size.height() >= bounds[0] && size.height() <= bounds[1] && size.width() <= bounds[2]
                        && size.logs() >= 3 && size.leaves() >= 8, shape + " bad shape " + size);
                hashes.add(size.hash()); sizes.add(size.height()+"h/"+size.width()+"w/"+size.logs()+"logs/"+size.leaves()+"leaves");
                grow(h, shape, AT, seed);
                h.assertTrue(size.equals(TreeBatchTwoGameTests.measure(h.getLevel(), AT)), "Seed changed " + shape);
            }
            Jugcraft.LOGGER.info("[tree 12] {} {}", shape, sizes);
            h.assertTrue(hashes.size() >= 3, "No variation " + shape);
        }
        TreeBatchTwoGameTests.reset(h.getLevel(), AT, Blocks.GRASS_BLOCK); h.succeed();
    }
    @GameTest(maxTicks = 400)
    public void mahoganyBaseColumnsHaveSupport(GameTestHelper h) {
        var at = AT.offset(80, 0, 0);
        for (long seed : TreeBatchFourBCGameTests.SEEDS) {
            grow(h, "emergent_mahogany", at, seed);
            for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++) {
                BlockPos p = at.offset(x, 0, z);
                for (int y = 0; y < 6 && !h.getLevel().getBlockState(p).is(block("mahogany_log")); y++) p = p.above();
                var below = h.getLevel().getBlockState(p.below());
                h.assertTrue(below.is(BlockTags.DIRT) || below.is(block("mahogany_log")), "Floating mahogany column " + x + "," + z + " seed " + seed);
            }
        }
        TreeBatchTwoGameTests.reset(h.getLevel(), at, Blocks.GRASS_BLOCK); h.succeed();
    }
    @GameTest(maxTicks = 300)
    public void soilObstaclesAndSaplingsWork(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(160, 0, 0);
        for (String shape : SHAPES) {
            TreeBatchTwoGameTests.reset(level, at, Blocks.STONE);
            h.assertFalse(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), at), "Grew on stone " + shape);
            TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK);
            level.setBlock(at.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.assertFalse(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), at), "Ignored trunk obstacle " + shape);
            h.assertTrue(level.getBlockState(at.above()).is(Blocks.DIAMOND_BLOCK), "Overwrote obstacle");
        }
        for (String tree : List.of("tree_fern", "rubber_tree")) {
            TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK);
            var sapling = (SaplingBlock) block(tree + "_sapling");
            level.setBlock(at, sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 1), Block.UPDATE_ALL);
            sapling.advanceTree(level, at, level.getBlockState(at), RandomSource.create(4));
            h.assertTrue(TreeBatchTwoGameTests.measure(level, at).logs() >= 3, "Sapling failed " + tree);
        }
        TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); h.succeed();
    }
    @GameTest(maxTicks = 200)
    public void tapsRejectStockpilesAndDuplicateTapsAndRequireLivingCanopy(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(240, 0, 0);
        grow(h, "rubber_tree", at, 2);
        var tapAt = at.above().north(); var tap = (TreeTapBlock) block("tree_tap");
        h.assertTrue(TreeTapBlock.productive(level, tapAt, Direction.NORTH), "Natural tree not tappable");
        level.setBlock(tapAt, tap.defaultBlockState().setValue(TreeTapBlock.FACING, Direction.NORTH), Block.UPDATE_ALL);
        h.assertFalse(TreeTapBlock.productive(level, at.above().south(), Direction.SOUTH), "Second tap accepted");
        for (int i=0; i<7; i++) tap.tick(level.getBlockState(tapAt), level, tapAt, RandomSource.create(i));
        h.assertTrue(level.getBlockState(tapAt).getValue(TreeTapBlock.LATEX)==4, "Tap not capped");
        level.setBlock(tapAt, level.getBlockState(tapAt).setValue(TreeTapBlock.LATEX, 0), Block.UPDATE_ALL);
        for (var p: BlockPos.betweenClosed(at.offset(-4, 0, -4), at.offset(4, 16, 4)))
            if (level.getBlockState(p).is(block("rubber_leaves"))) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        tap.tick(level.getBlockState(tapAt), level, tapAt, RandomSource.create(1));
        h.assertTrue(level.getBlockState(tapAt).getValue(TreeTapBlock.LATEX)==0, "Leafless trunk produces latex");
        grow(h, "rubber_tree", at, 2);
        level.setBlock(at, block("rubber_log").defaultBlockState(), Block.UPDATE_ALL);
        h.assertFalse(TreeTapBlock.productive(level, tapAt, Direction.NORTH), "Replaced base counted as living");
        TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); h.succeed();
    }
    @GameTest(maxTicks = 700)
    public void latexCooksIntoRubberReturningEveryBottle(GameTestHelper h) {
        var at = new BlockPos(2, 2, 2); h.setBlock(at.below(), Blocks.CAMPFIRE); h.setBlock(at, block("cooking_pot"));
        var pot = h.getBlockEntity(at, CookingPotBlockEntity.class);
        pot.setItem(0, new ItemStack(JugcraftAgriculture.item("latex_bottle")));
        pot.setItem(1, new ItemStack(JugcraftAgriculture.item("cider_vinegar")));
        h.succeedWhen(() -> {
            var output = pot.getItem(CookingPotBlockEntity.RESULT);
            h.assertTrue(output.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Jugcraft.id("rubber"))) && output.getCount()==1, "No rubber");
            int bottles=0; for(int i=0; i<CookingPotBlockEntity.SLOTS; i++) if(pot.getItem(i).is(Items.GLASS_BOTTLE)) bottles+=pot.getItem(i).getCount();
            h.assertTrue(bottles==2, "Bottle loss/duplication " + bottles);
        });
    }

    /** Inspect the real chunk scheduler, including duplicate entries, without advancing the world's clock. */
    private static long pendingTapTick(GameTestHelper h, BlockPos pos) {
        var tap = block("tree_tap");
        var ticks = (net.minecraft.world.ticks.LevelChunkTicks<Block>) h.getLevel().getChunkAt(pos).getBlockTicks();
        var entries = ticks.getAll()
                .filter(t -> t.pos().equals(pos) && t.type() == tap).toList();
        h.assertTrue(entries.size() == 1, "Expected exactly one pending tap tick at " + pos + ", got " + entries.size());
        return entries.getFirst().triggerTick();
    }
    /** Keep the production-delay assertion; accelerate only this fixture's already-verified tick. */
    private static void dispatchTapSoon(GameTestHelper h, BlockPos pos, long expectedDue) {
        h.assertTrue(TreeTapBlock.INTERVAL == 1200, "Production interval must remain one minute");
        h.assertTrue(pendingTapTick(h, pos) == expectedDue, "Tap changed its scheduled deadline");
        var level = h.getLevel(); var tap = block("tree_tap");
        var ticks = (net.minecraft.world.ticks.LevelChunkTicks<Block>) level.getChunkAt(pos).getBlockTicks();
        ticks.removeIf(t -> t.pos().equals(pos) && t.type() == tap);
        level.scheduleTick(pos, tap, 8);
    }
    @GameTest(maxTicks = 100)
    public void tapSchedulesItsOwnProductionAndHarvestConsumesBottleOnce(GameTestHelper h) {
        var level=h.getLevel(); var at=AT.offset(320,0,0); grow(h,"rubber_tree",at,3);
        var chunk = net.minecraft.world.level.ChunkPos.containing(at);
        level.setChunkForced(chunk.x(), chunk.z(), true);
        var player=h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var support=at.above(); var tapAt=support.north(); player.setPos(tapAt.getX()+.5,tapAt.getY(),tapAt.getZ()-2.5);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block("tree_tap")));
        var supportHit=new BlockHitResult(Vec3.atCenterOf(support).relative(Direction.NORTH,.5),Direction.NORTH,support,false);
        player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,supportHit);
        h.assertTrue(level.getBlockState(tapAt).is(block("tree_tap")) && player.getMainHandItem().isEmpty(),"Tap item placement failed");
        dispatchTapSoon(h, tapAt, level.getGameTime() + TreeTapBlock.INTERVAL);
        h.runAfterDelay(3,()->h.assertTrue(level.getBlockState(tapAt).getValue(TreeTapBlock.LATEX)==0,"Tap filled early"));
        h.runAfterDelay(12,()-> {
            h.assertTrue(level.getBlockState(tapAt).getValue(TreeTapBlock.LATEX)==1,"Scheduled fill missing");
            level.setBlock(tapAt,level.getBlockState(tapAt).setValue(TreeTapBlock.LATEX,4),Block.UPDATE_CLIENTS);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            var hit=new BlockHitResult(Vec3.atCenterOf(tapAt),Direction.NORTH,tapAt,false);
            for(int i=0;i<2;i++) player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
            h.assertTrue(player.getMainHandItem().getCount()==1,"Incorrect bottle consumption");
            h.assertTrue(player.getInventory().countItem(JugcraftAgriculture.item("latex_bottle"))==1,"Duplicate latex reward");
            h.assertTrue(level.getBlockState(tapAt).getValue(TreeTapBlock.LATEX)==0,"Tap did not empty");
            level.setBlock(support,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            h.assertFalse(level.getBlockState(tapAt).is(block("tree_tap")),"Tap floats after support removed");
            TreeBatchTwoGameTests.reset(level,at,Blocks.GRASS_BLOCK);
            level.setChunkForced(chunk.x(), chunk.z(), false); h.succeed();
        });
    }

    @GameTest
    public void fernAndRubberPropagateAndUseTheirOwnTimberRoutes(GameTestHelper h) {
        for(var tree:List.of("tree_fern","rubber_tree")) {
            String leaves=tree.equals("tree_fern")?"tree_fern_fronds":"rubber_leaves";
            int young=0;
            for(int i=0;i<512;i++) for(var drop:Block.getDrops(block(leaves).defaultBlockState(),h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),null)) {
                h.assertTrue(drop.is(block(tree+"_sapling").asItem()) || (tree.equals("rubber_tree") && drop.is(Items.STICK)),"Wrong leaf loot");
                if(drop.is(block(tree+"_sapling").asItem())) young++;
            }
            h.assertTrue(young>0,"No renewable saplings " + tree);
            var sheared=Block.getDrops(block(leaves).defaultBlockState(),h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),null,null,new ItemStack(Items.SHEARS));
            h.assertTrue(sheared.size()==1 && sheared.getFirst().is(block(leaves).asItem()),"Sheared leaf wrong");
        }
        var recipe=io.github.jimbozoomer.jugcraft.machine.MachineRecipes.find(h.getLevel(),io.github.jimbozoomer.jugcraft.machine.MachineKind.SAWMILL,new ItemStack(block("rubber_log"))).orElseThrow();
        h.assertTrue(recipe.output().create().is(Items.JUNGLE_PLANKS) && recipe.output().create().getCount()==6,"Rubber sawmill route");
        h.assertFalse(io.github.jimbozoomer.jugcraft.machine.MachineRecipes.find(h.getLevel(),io.github.jimbozoomer.jugcraft.machine.MachineKind.SAWMILL,new ItemStack(block("tree_fern_trunk"))).isPresent(),"Fern makes planks");
        h.assertTrue(((RubberLogBlock)block("rubber_log")).stripped()==Blocks.STRIPPED_JUNGLE_LOG,"Wrong stripped timber");
        h.succeed();
    }

    private static void useTap(net.minecraft.server.level.ServerPlayer player, BlockPos at, InteractionHand hand) {
        player.gameMode.useItemOn(player, player.level(), player.getItemInHand(hand), hand,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
    }
    @GameTest
    public void tapSaveStatesRetainOldProgressAndBucketContents(GameTestHelper h) {
        var codec = net.minecraft.world.level.block.state.BlockState.CODEC;
        var ops = com.mojang.serialization.JsonOps.INSTANCE;
        var legacy = com.google.gson.JsonParser.parseString("{\"id\":\"jugcraft:tree_tap\",\"properties\":{\"facing\":\"west\",\"latex\":\"3\"}}");
        var loaded = codec.parse(ops, legacy).getOrThrow();
        h.assertTrue(!loaded.getValue(TreeTapBlock.UPGRADED) && loaded.getValue(TreeTapBlock.LATEX) == 3
                && loaded.getValue(TreeTapBlock.FACING) == Direction.WEST, "Legacy tap lost progress or orientation");
        for (int fill : new int[]{0, 3, 19, 32}) {
            var state = loaded.setValue(TreeTapBlock.UPGRADED, true).setValue(TreeTapBlock.LATEX, fill);
            h.assertTrue(codec.parse(ops, codec.encodeStart(ops, state).getOrThrow()).getOrThrow().equals(state), "Bucket fill did not persist: " + fill);
        }
        h.succeed();
    }
    @GameTest
    public void bucketUpgradeCapsAtEightAndBottlesInBatches(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(400, 0, 0); grow(h, "rubber_tree", at, 2);
        var pos = at.above().north(); var tap = (TreeTapBlock) block("tree_tap");
        level.setBlock(pos, tap.defaultBlockState().setValue(TreeTapBlock.LATEX, 3), Block.UPDATE_ALL);
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET, 2));
        useTap(player, pos, InteractionHand.MAIN_HAND); useTap(player, pos, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "Upgrade spent more than one bucket");
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.UPGRADED)
                && level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 3, "Upgrade lost partial latex");
        for (int i = 0; i < 40; i++) tap.tick(level.getBlockState(pos), level, pos, RandomSource.create(i));
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 32, "Eight-bottle cap failed");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        useTap(player, pos, InteractionHand.MAIN_HAND);
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 24, "Small batch drained extra latex");
        player.getInventory().setItem(10, player.getMainHandItem());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 20));
        useTap(player, pos, InteractionHand.MAIN_HAND); useTap(player, pos, InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getCount() == 14, "Batch bottle count wrong");
        h.assertTrue(player.getInventory().countItem(JugcraftAgriculture.item("latex_bottle")) == 8, "Lost or duplicated batch");
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 0, "Not drained");
        h.assertTrue(level.getBlockTicks().hasScheduledTick(pos, tap), "Drained tap did not resume");
        TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); h.succeed();
    }

    @GameTest(maxTicks = 100)
    public void upgradeAndBottlingPreservePartialFillAndPendingTick(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(480, 0, 0); grow(h, "rubber_tree", at, 2);
        var chunk = net.minecraft.world.level.ChunkPos.containing(at); level.setChunkForced(chunk.x(), chunk.z(), true);
        var pos = at.above().north(); var tap = (TreeTapBlock) block("tree_tap");
        level.setBlock(pos, tap.defaultBlockState().setValue(TreeTapBlock.LATEX, 2), Block.UPDATE_ALL);
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        long originalDue = level.getGameTime() + TreeTapBlock.INTERVAL;
        h.assertTrue(pendingTapTick(h, pos) == originalDue, "Incorrect production interval");
        h.runAfterDelay(2, () -> {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            useTap(player, pos, InteractionHand.MAIN_HAND);
            h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 2, "Upgrade reset progress");
            h.assertTrue(pendingTapTick(h, pos) == originalDue, "Upgrade postponed the pending tick");
        });
        h.runAfterDelay(4, () -> {
            level.setBlock(pos, level.getBlockState(pos).setValue(TreeTapBlock.LATEX, 7), Block.UPDATE_CLIENTS);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 8));
            useTap(player, pos, InteractionHand.MAIN_HAND); useTap(player, pos, InteractionHand.MAIN_HAND);
            h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 3, "Bottling discarded partial fill");
            h.assertTrue(player.getMainHandItem().getCount() == 7, "Incomplete bottle was collected");
            dispatchTapSoon(h, pos, originalDue);
        });
        h.runAfterDelay(7, () -> h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 3, "Fill tick accelerated"));
        h.runAfterDelay(16, () -> {
            h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 4, "Upgrade or bottling postponed the pending fill tick");
            TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); level.setChunkForced(chunk.x(), chunk.z(), false); h.succeed();
        });
    }

    @GameTest
    public void bucketCraftAndMovedTapKeepUpgradeButNotContents(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(560, 0, 0); grow(h, "rubber_tree", at, 2);
        var pos = at.above().north(); var tap = block("tree_tap");
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(new ItemStack(tap), new ItemStack(Items.BUCKET)));
        var recipe = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).orElseThrow().value();
        var crafted = recipe.assemble(input);
        h.assertTrue(crafted.is(tap.asItem()), "Wrong upgrade recipe result");
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, crafted);
        var support = at.above();
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support).relative(Direction.NORTH, .5), Direction.NORTH, support, false));
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.UPGRADED), "Crafted upgrade did not place upgraded");
        var filled = level.getBlockState(pos).setValue(TreeTapBlock.LATEX, 23);
        var drops = Block.getDrops(filled, level, pos, null);
        h.assertTrue(drops.size() == 1 && drops.getFirst().getCount() == 1, "Upgrade drop duplicated or lost");
        var properties = drops.getFirst().get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
        h.assertTrue(properties != null && properties.apply(tap.defaultBlockState()).getValue(TreeTapBlock.UPGRADED), "Drop lost paid bucket");
        h.assertTrue(properties.apply(tap.defaultBlockState()).getValue(TreeTapBlock.LATEX) == 0, "Drop retained unbottled latex");
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        player.setItemInHand(InteractionHand.MAIN_HAND, drops.getFirst());
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support).relative(Direction.NORTH, .5), Direction.NORTH, support, false));
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.UPGRADED)
                && level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 0, "Moved upgrade not restored empty");
        TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); h.succeed();
    }

    @GameTest(maxTicks = 100)
    public void fullStoppedTapsResumeAfterUpgradeOrBatchCollection(GameTestHelper h) {
        var level = h.getLevel(); var tap = (TreeTapBlock) block("tree_tap");
        var bases = List.of(AT.offset(720, 0, 0), AT.offset(800, 0, 0));
        for (int i = 0; i < bases.size(); i++) {
            var at = bases.get(i); grow(h, "rubber_tree", at, 2);
            var chunk = net.minecraft.world.level.ChunkPos.containing(at); level.setChunkForced(chunk.x(), chunk.z(), true);
            level.setBlock(at.above().north(), tap.defaultBlockState().setValue(TreeTapBlock.UPGRADED, i == 1)
                    .setValue(TreeTapBlock.LATEX, i == 1 ? 32 : 4), Block.UPDATE_ALL);
            dispatchTapSoon(h, at.above().north(), level.getGameTime() + TreeTapBlock.INTERVAL);
        }
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.runAfterDelay(12, () -> {
            for (var at : bases) h.assertFalse(level.getBlockTicks().hasScheduledTick(at.above().north(), tap), "Full tap kept scheduling");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            useTap(player, bases.getFirst().above().north(), InteractionHand.MAIN_HAND);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 8));
            useTap(player, bases.getLast().above().north(), InteractionHand.MAIN_HAND);
            for (var at : bases) dispatchTapSoon(h, at.above().north(), level.getGameTime() + TreeTapBlock.INTERVAL);
        });
        h.runAfterDelay(24, () -> {
            h.assertTrue(level.getBlockState(bases.getFirst().above().north()).getValue(TreeTapBlock.LATEX) == 5, "Full basic tap did not continue after upgrade");
            h.assertTrue(level.getBlockState(bases.getLast().above().north()).getValue(TreeTapBlock.LATEX) == 1, "Full bucket did not refill after collecting");
            for (var at : bases) {
                TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK);
                var chunk = net.minecraft.world.level.ChunkPos.containing(at); level.setChunkForced(chunk.x(), chunk.z(), false);
            }
            h.succeed();
        });
    }

    @GameTest
    public void batchBottlingRespectsInventorySpaceAndOffHand(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(640, 0, 0); grow(h, "rubber_tree", at, 2);
        var pos = at.above().north(); var tap = block("tree_tap");
        level.setBlock(pos, tap.defaultBlockState().setValue(TreeTapBlock.UPGRADED, true).setValue(TreeTapBlock.LATEX, 19), Block.UPDATE_ALL);
        var player = h.makeMockServerPlayerInLevel(); player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE, 8));
        useTap(player, pos, InteractionHand.OFF_HAND);
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 19 && player.getOffhandItem().getCount() == 8, "Full inventory consumed bottles or latex");
        player.getInventory().setItem(10, new ItemStack(JugcraftAgriculture.item("latex_bottle"), 15));
        useTap(player, pos, InteractionHand.OFF_HAND);
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 15 && player.getOffhandItem().getCount() == 7, "Did not collect only what fits");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        useTap(player, pos, InteractionHand.MAIN_HAND);
        h.assertTrue(level.getBlockState(pos).getValue(TreeTapBlock.LATEX) == 11, "Freed hotbar slot not used");
        h.assertTrue(player.getInventory().countItem(JugcraftAgriculture.item("latex_bottle")) == 17, "Inventory batch reward wrong");
        TreeBatchTwoGameTests.reset(level, at, Blocks.GRASS_BLOCK); h.succeed();
    }
}
