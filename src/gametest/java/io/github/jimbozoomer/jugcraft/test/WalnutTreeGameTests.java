package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.*;
import io.github.jimbozoomer.jugcraft.menagerie.Ecology;
import io.github.jimbozoomer.jugcraft.menagerie.Menagerie;
import io.github.jimbozoomer.jugcraft.season.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public class WalnutTreeGameTests {
    static final List<String> SHAPES = List.of("walnut", "large_walnut", "bristlecone");
    static final BlockPos AT = new BlockPos(62000, 100, -62000);
    static Block block(String id) { return JugcraftAgriculture.block(id); }
    static void reset(GameTestHelper h, BlockPos at, Block soil) {
        TreeBatchTwoGameTests.reset(h.getLevel(), at, soil);
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-16,-2,-16), at.offset(16,-2,16)))
            h.getLevel().setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
    @GameTest(maxTicks = 600)
    public void shapesAreConnectedVariableBoundedAndReproducible(GameTestHelper h) {
        var level = h.getLevel(); var before = JugcraftSeasons.settings();
        try {
            JugcraftSeasons.setMode(level.getServer(), SeasonCalendar.Mode.AUTUMN);
            for (String shape : SHAPES) {
                var hashes = new HashSet<Long>(); var sizes = new ArrayList<String>();
                int stripped = 0, bark = 0;
                for (long seed : TreeBatchFourBCGameTests.SEEDS) {
                    reset(h, AT, shape.equals("bristlecone") ? Blocks.GRAVEL : Blocks.GRASS_BLOCK);
                    h.assertTrue(TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(),
                            RandomSource.create(seed), AT), "Placement failed " + shape);
                    var size = TreeBatchTwoGameTests.measure(level, AT);
                    int[] limits = shape.equals("bristlecone") ? new int[]{4,7,5,8}
                            : shape.equals("walnut") ? new int[]{8,10,7,9} : new int[]{12,16,10,14};
                    h.assertTrue(size.height() >= limits[0] && size.height() <= limits[1]
                            && size.width() >= limits[2] && size.width() <= limits[3], shape + " bad bounds " + size);
                    var logs = new HashSet<BlockPos>(); var leaves = new HashMap<BlockPos, BlockState>();
                    for (BlockPos p : BlockPos.betweenClosed(AT.offset(-10,0,-10), AT.offset(10,20,10))) {
                        var state = level.getBlockState(p);
                        if (state.is(BlockTags.LOGS)) logs.add(p.immutable());
                        if (state.is(BlockTags.LEAVES)) leaves.put(p.immutable(), state);
                        if (state.is(block("stripped_pine_log"))) stripped++;
                        if (state.is(block("pine_log"))) bark++;
                        if (state.hasProperty(SeasonalLeavesBlock.SEASON))
                            h.assertTrue(state.getValue(SeasonalLeavesBlock.SEASON) == WalnutLeavesBlock.SCHEDULE.on(JugcraftSeasons.today(),p), "Wrong season");
                    }
                    var queue = new ArrayDeque<BlockPos>(); var reached = new HashSet<BlockPos>();
                    queue.add(AT); reached.add(AT);
                    while (!queue.isEmpty()) {
                        BlockPos p = queue.remove();
                        for (Direction d : Direction.values()) {
                            var n = p.relative(d);
                            if (logs.contains(n) && reached.add(n)) queue.add(n);
                        }
                    }
                    h.assertTrue(reached.equals(logs), "Disconnected wood " + shape + " seed " + seed);
                    for (var state : leaves.values()) h.assertTrue(state.getValue(LeavesBlock.DISTANCE) < 7
                            && !state.getValue(LeavesBlock.PERSISTENT), "Unsupported/persistent foliage");
                    hashes.add(size.hash()); sizes.add(size.height()+"h/"+size.width()+"w");
                    reset(h, AT, shape.equals("bristlecone") ? Blocks.GRAVEL : Blocks.GRASS_BLOCK);
                    TreeBatchTwoGameTests.feature(level, shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), AT);
                    h.assertTrue(TreeBatchTwoGameTests.measure(level, AT).equals(size), "Seed drift " + shape);
                }
                h.assertTrue(hashes.size() >= 3, "No shape variation");
                if (shape.equals("bristlecone")) h.assertTrue(stripped / (double)(stripped+bark) > .55
                        && stripped / (double)(stripped+bark) < .85, "Wrong weathered timber balance");
                Jugcraft.LOGGER.info("[batch 13 measured] {} {}", shape, sizes);
            }
        } finally { JugcraftSeasons.setMode(level.getServer(), before.mode()); reset(h, AT, Blocks.GRASS_BLOCK); }
        h.succeed();
    }
    @GameTest(maxTicks = 300)
    public void rockySupportAndObstaclesAreRespectedAtomically(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(80,0,0);
        for (Block soil : List.of(Blocks.GRAVEL, Blocks.STONE, Blocks.ANDESITE)) {
            reset(h, at, soil);
            h.assertTrue(new WalnutTree("bristlecone").place(level, level.getChunkSource().getGenerator(), RandomSource.create(1), at), "Rejected rock");
            h.assertTrue(level.getBlockState(at.below()).is(soil), "Manufactured dirt pedestal");
        }
        for (String shape : SHAPES) {
            reset(h, at, shape.equals("bristlecone") ? Blocks.GRAVEL : Blocks.GRASS_BLOCK);
            level.setBlock(at.above(), Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            h.assertFalse(new WalnutTree(shape).place(level, level.getChunkSource().getGenerator(), RandomSource.create(2), at), "Overwrote obstacle");
            h.assertTrue(TreeBatchTwoGameTests.measure(level, at).logs() == 0, "Partial failed tree");
        }
        reset(h, at, Blocks.GRAVEL);
        level.setBlock(at.below(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        h.assertFalse(new WalnutTree("bristlecone").place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), at), "Floating gravel footing");
        reset(h, at, Blocks.GRASS_BLOCK);
        h.assertFalse(new WalnutTree("bristlecone").place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), at), "Bristlecone grew on grass");
        reset(h, at, Blocks.STONE);
        h.assertFalse(new WalnutTree("walnut").place(level, level.getChunkSource().getGenerator(), RandomSource.create(3), at), "Walnut grew on stone");
        reset(h, at, Blocks.GRASS_BLOCK); h.succeed();
    }
    @GameTest(maxTicks = 200)
    public void bothSaplingsGrowTheUnifiedTree(GameTestHelper h) {
        var level = h.getLevel(); var at = AT.offset(160,0,0);
        for (Block young : List.of(block("walnut_sapling"), Ecology.block("walnut_sapling"))) {
            reset(h, at, Blocks.GRASS_BLOCK);
            level.setBlock(at, young.defaultBlockState().setValue(SaplingBlock.STAGE,1), Block.UPDATE_ALL);
            ((SaplingBlock)young).advanceTree(level, at, level.getBlockState(at), RandomSource.create(7));
            h.assertTrue(level.getBlockState(at).is(block("walnut_log")), "Sapling did not grow canonical timber");
            h.assertTrue(TreeBatchTwoGameTests.measure(level, at).leaves() > 30, "Missing crown");
        }
        reset(h, at, Blocks.GRASS_BLOCK); h.succeed();
    }
    @GameTest
    public void recoveredTimberFeedsTheSameRecipesAndDrops(GameTestHelper h) {
        for (String name : List.of("walnut_log","walnut_wood","stripped_walnut_log","stripped_walnut_wood")) {
            WalnutWoodGameTests.craft(h, JugcraftAgriculture.item("walnut_planks"),4,1,1, Ecology.block(name).asItem());
            var drops = Block.getDrops(Ecology.block(name).defaultBlockState(),h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),null);
            h.assertTrue(drops.size()==1 && drops.getFirst().is(block(name).asItem()),"Legacy log drops duplicate timber family");
        }
        Item p = JugcraftAgriculture.item("walnut_planks");
        WalnutWoodGameTests.craft(h, Menagerie.item("ecology_walnut_door"),3,2,3,p,p,p,p,p,p);
        WalnutWoodGameTests.craft(h, Menagerie.item("ecology_walnut_boat"),1,3,2,p,null,p,p,p,p);
        WalnutWoodGameTests.craft(h, net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.withDefaultNamespace("brown_dye")),1,1,1,JugcraftAgriculture.item("walnut_husk"));
        h.assertTrue(new ItemStack(Menagerie.item("ecology_walnut")).is(Squirrel.FOOD),"Raw walnut not squirrel food");
        h.assertTrue(new ItemStack(JugcraftAgriculture.item("roasted_walnuts")).is(Squirrel.FOOD),"Roasted walnut not squirrel food");
        h.succeed();
    }
    @GameTest
    public void ripeLeavesHarvestOnceWithHandsAndToolsButPlacedLeavesDoNot(GameTestHelper h) {
        var level=h.getLevel(); var at=new BlockPos(2,3,2); var player=h.makeMockPlayer(GameType.SURVIVAL);
        var before=JugcraftSeasons.settings();
        try {
            JugcraftSeasons.setMode(level.getServer(),SeasonCalendar.Mode.SUMMER);
            for(Block leaf:List.of(block("walnut_leaves"),Ecology.block("walnut_leaves"))) {
                h.setBlock(at.east(),block("walnut_log"));
                for(Item tool:List.of(Items.AIR,Items.SHEARS,Items.STICK)) {
                    h.setBlock(at,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,2));
                    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));
                    h.useBlock(at,player);
                    h.assertTrue(h.getBlockState(at).getValue(FruitingLeavesBlock.FRUIT)==0,"Right click failed with "+tool);
                    h.assertItemEntityPresent(Menagerie.item("ecology_walnut"),at.below(),3);
                    h.assertItemEntityPresent(JugcraftAgriculture.item("walnut_husk"),at.below(),3);
                    h.assertFalse(((WalnutLeavesBlock)leaf).pick(level,h.absolutePos(at)),"Repeat harvest duplicated fruit");
                }
                h.setBlock(at,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(LeavesBlock.PERSISTENT,true).setValue(FruitingLeavesBlock.FRUIT,2));
                h.assertFalse(((WalnutLeavesBlock)leaf).pick(level,h.absolutePos(at)),"Player placed leaves harvested");
                h.setBlock(at,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,1).setValue(FruitingLeavesBlock.FRUIT,2).setValue(SeasonalLeavesBlock.SEASON,SeasonalLeavesBlock.Foliage.BARE));
                h.assertFalse(((WalnutLeavesBlock)leaf).pick(level,h.absolutePos(at)),"Bare leaves harvested");
            }
        } finally { JugcraftSeasons.setMode(level.getServer(),before.mode()); }
        h.succeed();
    }
    @GameTest
    public void leavesRegrowFruitChangeSeasonsAndDecay(GameTestHelper h) {
        var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(2,3,2));
        var before=JugcraftSeasons.settings();
        try {
            for(Block leaf:List.of(block("walnut_leaves"),Ecology.block("walnut_leaves"))) {
                JugcraftSeasons.setMode(level.getServer(),SeasonCalendar.Mode.SUMMER);
                level.setBlock(pos.east(),block("walnut_log").defaultBlockState(),Block.UPDATE_ALL);
                level.setBlock(pos,leaf.defaultBlockState().setValue(LeavesBlock.DISTANCE,1),Block.UPDATE_ALL);
                for(int i=0;i<512;i++)level.getBlockState(pos).randomTick(level,pos,RandomSource.create(i*49157L));
                h.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==2,"Fruit never ripened");
                h.assertTrue(((WalnutLeavesBlock)leaf).pick(level,pos),"Ripe harvest failed");
                for(int i=0;i<512;i++)level.getBlockState(pos).randomTick(level,pos,RandomSource.create(i*49157L));
                h.assertTrue(level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==2,"Fruit did not regrow");
                JugcraftSeasons.setMode(level.getServer(),SeasonCalendar.Mode.WINTER);
                level.getBlockState(pos).randomTick(level,pos,RandomSource.create(4));
                h.assertTrue(level.getBlockState(pos).getValue(SeasonalLeavesBlock.SEASON)==SeasonalLeavesBlock.Foliage.BARE
                        && level.getBlockState(pos).getValue(FruitingLeavesBlock.FRUIT)==0,"Winter retained fruit");
                level.setBlock(pos,level.getBlockState(pos).setValue(LeavesBlock.DISTANCE,7),Block.UPDATE_CLIENTS);
                level.getBlockState(pos).randomTick(level,pos,RandomSource.create(4));
                h.assertTrue(level.getBlockState(pos).isAir(),"Unsupported leaves did not decay");
            }
        } finally { JugcraftSeasons.setMode(level.getServer(),before.mode()); }
        h.succeed();
    }
    @GameTest(maxTicks = 450)
    public void rawWalnutRoastsInAFurnace(GameTestHelper h) {
        var at=new BlockPos(2,2,2);h.setBlock(at,Blocks.FURNACE);
        var furnace=h.getBlockEntity(at,net.minecraft.world.level.block.entity.FurnaceBlockEntity.class);
        furnace.setItem(0,new ItemStack(Menagerie.item("ecology_walnut")));
        furnace.setItem(1,new ItemStack(Items.COAL));
        h.succeedWhen(()->{
            h.assertTrue(furnace.getItem(2).is(JugcraftAgriculture.item("roasted_walnuts"))
                    && furnace.getItem(2).getCount()==1,"Raw walnut did not roast");
            h.assertTrue(furnace.getItem(0).isEmpty(),"Raw walnut not consumed");
        });
    }
    @GameTest(maxTicks = 700)
    public void roastedWalnutsCookIntoMooncakes(GameTestHelper h) {
        var at=new BlockPos(2,2,2); h.setBlock(at.below(),Blocks.CAMPFIRE); h.setBlock(at,block("cooking_pot"));
        var pot=h.getBlockEntity(at,CookingPotBlockEntity.class);
        pot.setItem(0,new ItemStack(Items.WHEAT,2)); pot.setItem(1,new ItemStack(Items.SUGAR));
        pot.setItem(2,new ItemStack(Items.EGG)); pot.setItem(3,new ItemStack(JugcraftAgriculture.item("roasted_walnuts"),2));
        h.succeedWhen(()-> {
            var result=pot.getItem(CookingPotBlockEntity.RESULT);
            h.assertTrue(result.is(JugcraftAgriculture.item("walnut_mooncake"))&&result.getCount()==4,"Mooncakes not cooked");
            for(int slot=0;slot<4;slot++)h.assertTrue(pot.getItem(slot).isEmpty(),"Ingredients not consumed");
        });
    }
}
