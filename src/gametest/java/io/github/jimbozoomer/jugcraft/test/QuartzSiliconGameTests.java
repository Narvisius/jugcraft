package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.workshop.*;
import io.github.jimbozoomer.jugcraft.kinetic.*;
import io.github.jimbozoomer.jugcraft.materials.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;

public class QuartzSiliconGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(5,2,5);
    private static ItemStack item(String s,int n){return StarterWorkshopGameTests.stack("jugcraft:"+s,n);}
    private WorkshopBlockEntity setup(GameTestHelper h){
        var block=JugcraftWorkshop.BLOCKS[2];var state=block.defaultBlockState();
        h.setBlock(AT.below(),Blocks.STONE);h.setBlock(AT.east().below(),Blocks.STONE);
        h.setBlock(AT,state);block.setPlacedBy(h.getLevel(),h.absolutePos(AT),state,null,ItemStack.EMPTY);
        var f=h.getBlockEntity(AT,WorkshopBlockEntity.class);
        h.assertTrue(f.select(Jugcraft.id("starter_workshop/silicon")),"Silicon recipe loaded");
        f.setItem(0,new ItemStack(Items.QUARTZ,2));f.setItem(1,new ItemStack(Items.CHARCOAL,2));f.setItem(4,new ItemStack(Items.COAL));
        f.setItem(6,item("basic_ceramic_lining",1));f.setItem(7,item("ceramic_crucible",1));f.setItem(8,item("crucible_heat_insert",1));
        h.setBlock(AT.east(),JugcraftWorkshop.BELLOWS.defaultBlockState().setValue(BellowsBlock.FACING,Direction.WEST));
        return f;
    }
    private ServerPlayer worker(GameTestHelper h,WorkshopBlockEntity f){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);
        p.snapTo(f.getBlockPos().getX()+.5,f.getBlockPos().getY(),f.getBlockPos().getZ()-1.5,0,0);
        p.containerMenu=f.createMenu(42,p.getInventory(),p);return p;
    }
    private void pump(GameTestHelper h,WorkshopBlockEntity f,ServerPlayer p,int ticks){
        for(int i=0;i<ticks;i++){h.assertTrue(((WorkshopMenu)p.containerMenu).clickMenuButton(p,WorkshopMenu.PULSE),"Authorized pumping");f.tick(h.getLevel());}
    }
    @GameTest public void quartzMiningUsesVanillaQuartzAndStoneTools(GameTestHelper h){
        var pick=new ItemStack(Items.STONE_PICKAXE);var silk=pick.copy();var fortune=pick.copy();
        var ench=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        silk.enchant(ench.getOrThrow(Enchantments.SILK_TOUCH),1);fortune.enchant(ench.getOrThrow(Enchantments.FORTUNE),3);
        h.assertTrue(JugcraftMaterials.QUARTZ.item==Items.QUARTZ,"No duplicate quartz currency");
        for(var ore:List.of(JugcraftMaterials.QUARTZ.ore,JugcraftMaterials.QUARTZ.deepslateOre)){
            var s=ore.defaultBlockState();h.assertTrue(pick.isCorrectToolForDrops(s),"Stone mines quartz");
            h.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(s),"Wood cannot mine quartz");
            var drops=Block.getDrops(s,h.getLevel(),h.absolutePos(AT),null,null,pick);
            h.assertTrue(drops.size()==1&&drops.getFirst().is(Items.QUARTZ)&&drops.getFirst().getCount()==1,"Exactly one vanilla quartz");
            var preserved=Block.getDrops(s,h.getLevel(),h.absolutePos(AT),null,null,silk);
            h.assertTrue(preserved.size()==1&&preserved.getFirst().is(ore.asItem()),"Silk preserves ore");
            int max=0;for(int n=0;n<64;n++)max=Math.max(max,Block.getDrops(s,h.getLevel(),h.absolutePos(AT),null,null,fortune).getFirst().getCount());
            h.assertTrue(max>1&&max<=4,"Fortune works with bounded yield");
        }h.succeed();
    }
    @GameTest public void quartzBiomePoliciesStaySeparate(GameTestHelper h){
        var registry=h.getLevel().registryAccess();var features=registry.lookupOrThrow(Registries.FEATURE);
        var primary=(SurfaceLinkedOreFeature)features.getValue(Jugcraft.id("ore_quartz"));
        var secondary=(SurfaceLinkedOreFeature)features.getValue(Jugcraft.id("ore_quartz_secondary"));
        h.assertTrue(primary.ore().size()==20&&primary.outcropChance()==3&&primary.outcropMax()==6,"Primary balance");
        h.assertTrue(secondary.ore().size()==12&&secondary.outcropChance()==0,"Forest veins have no marker");
        var biomes=registry.lookupOrThrow(Registries.BIOME);
        h.assertTrue(biomes.getOrThrow(primary.surfaceBiomes()).size()==8&&biomes.getOrThrow(secondary.surfaceBiomes()).size()==9,"Explicit surface policies");
        for(var b:biomes.getOrThrow(primary.surfaceBiomes()))h.assertTrue(!b.is(secondary.surfaceBiomes()),"No double frequency overlap");
        h.succeed();
    }
    @GameTest(structure=ARENA) public void manualSiliconPausesAndResumesWithoutDoublePayment(GameTestHelper h){
        var f=setup(h);var p=worker(h,f);f.tick(h.getLevel());h.assertTrue(f.status()==11&&!f.busy()&&f.fuelCredit()==0,"No airflow means no input/fuel cost");
        pump(h,f,p,73);int heat=f.fuelCredit();h.assertTrue(f.progress()==73&&f.getItem(0).getCount()==1&&f.getItem(1).getCount()==1,"Reserve inputs once");
        f.release(p);for(int i=0;i<20;i++)f.tick(h.getLevel());h.assertTrue(f.progress()==73&&f.fuelCredit()==heat,"Pause retains heat/work");
        var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());f.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        f.tick(h.getLevel());h.assertTrue(f.progress()==73&&f.status()==11,"Reload does not resume manual operation");
        pump(h,f,p,127);f.release(p);h.assertTrue(f.getItem(5).is(JugcraftMaterials.SILICON)&&f.getItem(5).getCount()==1,"Exactly one silicon after 200 work ticks");
        h.assertTrue(f.fuelCredit()==1400&&f.getItem(0).getCount()==1&&f.getItem(1).getCount()==1,"Heating coal distinct from recipe charcoal");h.succeed();
    }
    @GameTest(structure=ARENA) public void missingUpgradeFuelBellowsAndFullOutputStopWork(GameTestHelper h){
        var f=setup(h);var p=worker(h,f);f.setItem(8,ItemStack.EMPTY);h.assertTrue(f.status()==9,"Missing insert readable");
        f.setItem(8,item("crucible_heat_insert",1));h.setBlock(AT.east(),Blocks.AIR);h.assertTrue(f.status()==10,"Missing bellows readable");
        h.setBlock(AT.east(),JugcraftWorkshop.BELLOWS.defaultBlockState().setValue(BellowsBlock.FACING,Direction.WEST));
        f.setItem(4,ItemStack.EMPTY);h.assertTrue(f.status()==4,"No fuel");f.setItem(4,new ItemStack(Items.COAL));
        pump(h,f,p,40);f.setItem(5,new ItemStack(JugcraftMaterials.SILICON,64));int heat=f.fuelCredit();pump(h,f,p,20);
        h.assertTrue(f.progress()==40&&f.fuelCredit()==heat&&f.status()==5,"Full output freezes paid work and fuel");
        h.assertTrue(!f.canTakeItemThroughFace(8,f.getItem(8),Direction.DOWN)&&f.locked(8),"Upgrade cannot be extracted during committed work");
        f.setItem(5,ItemStack.EMPTY);h.assertTrue(f.cancel(),"Cancellation returns reserved inputs");
        h.assertTrue(f.getItem(0).getCount()==2&&f.getItem(1).getCount()==2&&f.fuelCredit()==heat,"Cancel doesn't refund spent heat");h.succeed();
    }
    @GameTest(structure=ARENA) public void onlyCurrentAuthorizedOperatorCanPump(GameTestHelper h){
        var f=setup(h);var first=worker(h,f);var second=worker(h,f);
        h.assertTrue(f.pulse(first)&&!f.pulse(second),"Concurrent operators cannot stack work");
        first.containerMenu.removed(first);h.assertTrue(f.pulse(second),"Leaving releases work");
        second.snapTo(0,200,0,0,0);h.assertTrue(!f.pulse(second),"Remote menu cannot supply work");
        f.tick(h.getLevel());h.assertTrue(!f.busy(),"Invalid presence supplies no progress");h.succeed();
    }
    @GameTest(structure=ARENA) public void disabledSiliconAndProtectedBellowsSpendNothing(GameTestHelper h){
        var f=setup(h);var p=worker(h,f);
        io.github.jimbozoomer.jugcraft.config.JugcraftConfig.setFeatureEnabled("silicon",false);
        try {pump(h,f,p,5);h.assertTrue(f.status()==7&&!f.busy()&&f.fuelCredit()==0,"Disabled silicon spends nothing");}
        finally {io.github.jimbozoomer.jugcraft.config.JugcraftConfig.setFeatureEnabled("silicon",true);}
        var protectedCell=new java.util.concurrent.atomic.AtomicReference<>(f.bellows().getBlockPos());
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level,actor,pos,state,entity)->!pos.equals(protectedCell.get()));
        try {h.assertTrue(!f.pulse(p),"Bellows protection prevents manual pumping");f.tick(h.getLevel());h.assertTrue(!f.busy(),"Protected work spends nothing");}
        finally {protectedCell.set(null);}
        h.succeed();
    }
    @GameTest(structure=ARENA) public void shaftAirChargesOnlyForDemandAndCannotStack(GameTestHelper h){
        var f=setup(h);var b=f.bellows();
        h.assertTrue(b.acceptKinetic(Direction.NORTH,100)==0,"Wrong face rejects shaft");
        h.assertTrue(b.acceptKinetic(Direction.EAST,4)==4&&f.status()==11,"Half airflow cannot run furnace");
        h.assertTrue(b.acceptKinetic(Direction.EAST,100)==4&&b.acceptKinetic(Direction.EAST,100)==0,"All sources share eight KE capacity");
        f.tick(h.getLevel());var p=worker(h,f);var menu=(WorkshopMenu)p.containerMenu;
        h.assertTrue(!b.hasAir()&&menu.data(8)==2&&menu.data(4)==0,"Consumed airflow remains visible as this tick's shaft work, without becoming reusable energy");
        h.assertTrue(f.progress()==1&&b.acceptKinetic(Direction.EAST,100)==8&&b.acceptKinetic(Direction.EAST,100)==0,"Later sources can buffer the next step, never advance work or overfill");
        pump(h,f,p,1);h.assertTrue(b.acceptKinetic(Direction.EAST,100)==0,"Manual pumping doesn't consume extra shaft power");
        f.release(p);f.setItem(5,new ItemStack(JugcraftMaterials.SILICON,64));h.assertTrue(!f.wantsShaftAir(),"Blocked output has no shaft demand");h.succeed();
    }
    @GameTest(structure=ARENA,maxTicks=260) public void realShaftNetworkCompletesOneSilicon(GameTestHelper h){
        var f=setup(h);f.setItem(0,new ItemStack(Items.QUARTZ));f.setItem(1,new ItemStack(Items.CHARCOAL));
        var source=AT.east(3);h.setBlock(AT.east(2),JugcraftKinetics.IRON_SHAFT.defaultBlockState().setValue(ShaftBlock.AXIS,Direction.Axis.X));
        h.setBlock(source,JugcraftKinetics.HAND_CRANK.defaultBlockState().setValue(HandCrankBlock.FACING,Direction.WEST));
        h.getBlockEntity(source,HandCrankBlockEntity.class).addTurns(240);KineticNetworks.invalidate(h.getLevel());
        h.succeedWhen(()->{h.assertTrue(f.getItem(5).is(JugcraftMaterials.SILICON)&&f.getItem(5).getCount()==1,"Real shaft produced silicon");
            h.assertTrue(f.fuelCredit()==1400&&f.getItem(0).isEmpty()&&f.getItem(1).isEmpty(),"Exact paid inputs and heat");});
    }
    @GameTest(structure=ARENA) public void oldEightSlotSavesAndOrdinaryAlloysRemainValid(GameTestHelper h){
        var f=setup(h);f.setItem(8,ItemStack.EMPTY);h.setBlock(AT.east(),Blocks.AIR);
        h.assertTrue(f.select(Jugcraft.id("starter_workshop/bronze_ingot")),"Select ordinary alloy");
        f.setItem(0,new ItemStack(Items.COPPER_INGOT,3));f.setItem(1,item("tin_ingot",1));
        var saved=f.saveWithoutMetadata(h.getLevel().registryAccess());saved.remove("high_heat");
        f.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        for(int i=0;i<200;i++)f.tick(h.getLevel());
        h.assertTrue(f.getItem(5).is(JugcraftMaterials.BRONZE.ingot)&&f.getItem(5).getCount()==4,"Legacy alloys need no insert/bellows/operator");h.succeed();
    }
}
