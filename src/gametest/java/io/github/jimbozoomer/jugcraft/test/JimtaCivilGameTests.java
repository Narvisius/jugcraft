package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.CivilWorkshops;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

public final class JimtaCivilGameTests {
    private CivilWorkshops.Node node(GameTestHelper h,String kind){var p=new BlockPos(2,2,2);h.setBlock(p,CivilWorkshops.BLOCKS.get(kind));return h.getBlockEntity(p, CivilWorkshops.Node.class);}
    @GameTest public void quarryChecksClaimAndOutputCapacityBeforeConsumingPowerOrTool(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"quarry");n.owner=p.getUUID();var at=new BlockPos(3,2,2);h.setBlock(at,Blocks.STONE);n.setArea(h.absolutePos(at),h.absolutePos(at));n.running=true;n.energy.setAmount(1000);n.setItem(0,new ItemStack(Items.IRON_PICKAXE));
        try(var claim=TestClaims.open().block(h.absolutePos(at),UUID.randomUUID())){n.step(h.getLevel());h.assertTrue(h.getBlockState(at).is(Blocks.STONE)&&n.energy.getAmount()==1000&&n.cursor==0,"Protected work waits without advancing or consuming");}
        for(int i=14;i<27;i++)n.setItem(i,new ItemStack(Items.DIRT,64));n.step(h.getLevel());h.assertTrue(h.getBlockState(at).is(Blocks.STONE)&&n.getItem(0).getDamageValue()==0&&n.energy.getAmount()==1000,"Blocked output cannot destroy the block or spend power/tool durability");
        n.setItem(14,ItemStack.EMPTY);n.step(h.getLevel());h.assertTrue(h.getBlockState(at).isAir()&&n.getItem(14).is(Items.COBBLESTONE)&&n.energy.getAmount()==744&&n.getItem(0).getDamageValue()==1,"Successful mine transfers one real drop and charges once");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
    @GameTest public void replacingAndFillingConsumeRealMaterials(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"replacer");n.owner=p.getUUID();var at=new BlockPos(3,2,2);h.setBlock(at,Blocks.STONE);n.setArea(h.absolutePos(at),h.absolutePos(at));n.running=true;n.energy.setAmount(512);n.setItem(0,new ItemStack(Items.IRON_PICKAXE));n.setItem(1,new ItemStack(Items.OAK_PLANKS,2));n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).is(Blocks.OAK_PLANKS)&&n.getItem(1).getCount()==1&&n.getItem(14).is(Items.COBBLESTONE)&&n.energy.getAmount()==256,"Replacement conserves old block drops, consumes one material and power");
        n.cursor=0;n.step(h.getLevel());h.assertTrue(n.getItem(1).getCount()==1&&n.energy.getAmount()==256,"Identical desired block is skipped without charging");h.assertTrue(!n.setArea(h.absolutePos(at),h.absolutePos(at.offset(32,0,0))),"32-block maximum rejects oversized work volumes");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
    @GameTest public void floodGateRequiresAnEnclosedReservoirAndFiniteFluid(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"flood_gate");n.owner=p.getUUID();var at=new BlockPos(4,2,4);h.setBlock(at,Blocks.AIR);n.setArea(h.absolutePos(at),h.absolutePos(at));n.running=true;n.energy.setAmount(512);n.fluid.fill(Fluids.WATER,1000);n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).isAir()&&n.fluid.millibuckets()==1000&&n.energy.getAmount()==512,"An open edge prevents unselected land from being flooded");
        for(var d:net.minecraft.core.Direction.values())if(d!=net.minecraft.core.Direction.UP)h.setBlock(at.relative(d),Blocks.STONE);n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).is(Blocks.WATER)&&n.fluid.millibuckets()==0&&n.energy.getAmount()==256,"One placed source consumes a bucket of water and one work charge");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
    @GameTest public void publicLandDoesNotGrantSomeoneElsesWorkshopControls(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);
        try{
            var n=node(h,"quarry");n.owner=UUID.randomUUID();
            h.assertTrue(!n.permitted(player)&&!n.stillValid(player),"Public land alone cannot grant another owner's machine menu or survey control");
            n.owner=player.getUUID();h.assertTrue(n.permitted(player),"Nearby owner can configure the workshop");
            try(var claim=TestClaims.open().block(n.getBlockPos(),UUID.randomUUID())){
                h.assertTrue(!n.permitted(player)&&!n.stillValid(player),"Revoked land access also revokes an already open workshop");
            }
            player.snapTo(net.minecraft.world.phys.Vec3.atCenterOf(n.getBlockPos().offset(20,0,0)));
            h.assertTrue(!n.permitted(player),"A stale remote menu cannot reconfigure machinery");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void invalidSavedSurveyCannotResumeAnOldWorkArea(GameTestHelper h){
        var n=node(h,"quarry");var at=h.absolutePos(new BlockPos(3,2,2));n.setArea(at,at);n.running=true;
        n.setItem(14,new ItemStack(Items.COBBLESTONE,17));n.energy.setAmount(512);
        var saved=n.saveWithoutMetadata(h.getLevel().registryAccess());saved.remove("Minimum");saved.remove("Maximum");
        n.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(n.minimum==null&&n.maximum==null&&!n.running,"Reload clears stale coordinates and pauses work without a valid survey");
        h.assertTrue(n.getItem(14).getCount()==17&&n.energy.getAmount()==512,"Invalid survey does not erase cargo or energy");h.succeed();
    }

    @GameTest public void fillerShapesHaveExactBoundariesAndInversion(GameTestHelper h){
        var min=BlockPos.ZERO;var max=new BlockPos(2,2,2);int box=0,frame=0,inside=0;
        for(var at:BlockPos.betweenClosed(min,max)){
            if(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FillerPatterns.selected(1,false,at,min,max,net.minecraft.core.Direction.NORTH))box++;
            if(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FillerPatterns.selected(2,false,at,min,max,net.minecraft.core.Direction.NORTH))frame++;
            if(io.github.jimbozoomer.jugcraft.jimtafacts.industry.FillerPatterns.selected(1,true,at,min,max,net.minecraft.core.Direction.NORTH))inside++;
        }
        h.assertTrue(box==26&&frame==20&&inside==1,"3-cube shell, frame and complementary interior are disjoint exact geometry");
        h.assertTrue(!io.github.jimbozoomer.jugcraft.jimtafacts.industry.FillerPatterns.selected(1,true,new BlockPos(3,0,0),min,max,net.minecraft.core.Direction.NORTH),"Inversion never includes positions outside the survey");h.succeed();
    }
    @GameTest public void clearingFillerConservesDropsToolAndPower(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"filler");n.owner=p.getUUID();var at=new BlockPos(3,2,2);h.setBlock(at,Blocks.STONE);
        n.setArea(h.absolutePos(at),h.absolutePos(at));n.fillerPattern=8;n.running=true;n.energy.setAmount(512);n.setItem(0,new ItemStack(Items.IRON_PICKAXE));
        n.step(h.getLevel());h.assertTrue(h.getBlockState(at).isAir()&&n.getItem(14).is(Items.COBBLESTONE)&&n.energy.getAmount()==256&&n.getItem(0).getDamageValue()==1,"Clear uses the real excavation path, output reserve and costs");
        h.setBlock(at,Blocks.CHEST);n.cursor=0;n.running=true;n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).is(Blocks.CHEST)&&n.energy.getAmount()==256,"Clear cannot erase container contents");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}

    @GameTest public void basicMinerDeploysAndRetractsPaidRealPipes(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var n=node(h,"miner");n.owner=p.getUUID();var at=new BlockPos(2,1,2);h.setBlock(at,Blocks.STONE);
        var pipe=io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.BLOCKS.get("mining_pipe");
        var tip=io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.BLOCKS.get("mining_tip");
        h.assertTrue(n.setArea(h.absolutePos(at),h.absolutePos(at)),"Miner survey begins directly below its body");
        n.running=true;n.energy.setAmount(512);n.setItem(0,new ItemStack(Items.IRON_PICKAXE));n.setItem(1,new ItemStack(pipe,2));n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).is(tip)&&n.getItem(1).getCount()==1&&n.getItem(14).is(Items.COBBLESTONE)&&n.energy.getAmount()==256&&n.deployedPipeCount()==1,"One segment consumes one real pipe, preserves drops and costs power");
        h.assertTrue(!n.setArea(h.absolutePos(at),h.absolutePos(at)),"Deployed columns cannot be abandoned by resetting the survey");
        n.running=false;h.assertTrue(n.requestRetraction(),"Paused miner can retract");n.step(h.getLevel());
        h.assertTrue(h.getBlockState(at).isAir()&&n.deployedPipeCount()==0&&n.getItem(15).is(pipe.asItem())&&n.getItem(15).getCount()==1&&n.energy.getAmount()==224,"Retraction returns exactly one pipe with no new ore");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}

    @GameTest public void chargedScannerDeploysAndRetractsRealOreBranchAcrossReload(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var n=node(h,"miner");n.owner=p.getUUID();var head=new BlockPos(2,1,2);var ore=head.east();h.setBlock(head,Blocks.STONE);h.setBlock(ore,Blocks.IRON_ORE);
            n.setArea(h.absolutePos(head),h.absolutePos(head));n.setItem(0,new ItemStack(Items.IRON_PICKAXE));n.setItem(1,new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.BLOCKS.get("mining_pipe"),4));
            var scanner=new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialGear.ITEMS.get("jimta_ore_scanner"));io.github.jimbozoomer.jugcraft.tools.Chargeable.setEnergy(scanner,500);n.setItem(13,scanner);n.energy.setAmount(4096);n.running=true;
            n.step(h.getLevel());n.step(h.getLevel());
            h.assertTrue(io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(n.getItem(13))==400,"One depth survey costs exactly 100 scanner JE");
            n.step(h.getLevel());h.assertTrue(h.getBlockState(ore).is(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.BLOCKS.get("mining_tip"))&&n.deployedPipeCount()==2,"Ore is reached by a real lateral pipe tip");
            var saved=n.saveWithoutMetadata(h.getLevel().registryAccess());n.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            n.step(h.getLevel());n.step(h.getLevel());
            h.assertTrue(h.getBlockState(ore).isAir()&&n.deployedPipeCount()==1,"Reload preserves branch recovery; main drill remains");
            int iron=0,pipes=0;for(int slot=14;slot<27;slot++){if(n.getItem(slot).is(Items.RAW_IRON))iron+=n.getItem(slot).getCount();if(n.getItem(slot).is(io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.BLOCKS.get("mining_pipe").asItem()))pipes+=n.getItem(slot).getCount();}
            h.assertTrue(iron==1&&pipes==1&&n.getItem(1).getCount()==2,"One ore drop and one recovered branch pipe, with two supplied pipes spent");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
}
