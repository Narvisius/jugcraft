package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialHeat;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;

/** Authored with the integration; reserved for the owner's final combined test batch. */
public final class JimtaHeatGameTests {
    @GameTest public void superheatedSteamUsesMoreHeatAndExhaustFeedsASecondTurbine(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var source=node(h,"electric_heat_generator",new BlockPos(2,2,2),Direction.EAST);source.owner=p.getUUID();source.heat=192;
            var boiler=node(h,"steam_generator",new BlockPos(3,2,2),Direction.WEST);boiler.owner=p.getUUID();boiler.input.fill(net.minecraft.world.level.material.Fluids.WATER,16);
            var saved=boiler.saveWithoutMetadata(h.getLevel().registryAccess());saved.putBoolean("superheated",true);
            boiler.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));boiler.step(h.getLevel());
            h.assertTrue(source.heat==0&&boiler.input.millibuckets()==0&&boiler.output.has(IndustrialHeat.SUPERHEATED_STEAM.fluid(),160),"192 heat and 16 water produce exactly 160 superheated steam");
            var first=node(h,"steam_kinetic_generator",new BlockPos(5,2,2),Direction.NORTH);first.owner=p.getUUID();first.input.fill(IndustrialHeat.SUPERHEATED_STEAM.fluid(),160);first.step(h.getLevel());
            h.assertTrue(first.input.millibuckets()==0&&first.output.has(IndustrialHeat.STEAM.fluid(),160),"First stage exhaust is the same 160 mB as ordinary steam");
            var second=node(h,"steam_kinetic_generator",new BlockPos(5,2,4),Direction.NORTH);second.owner=p.getUUID();second.input.fill(IndustrialHeat.STEAM.fluid(),160);second.step(h.getLevel());
            h.assertTrue(second.input.millibuckets()==0&&second.output.has(net.minecraft.world.level.material.Fluids.WATER,16),"Second stage recovers exactly 16 water, without a material-gain loop");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    private static IndustrialHeat.Node node(GameTestHelper h,String kind,BlockPos at,Direction facing){
        h.setBlock(at,IndustrialHeat.BLOCKS.get(kind).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,facing));
        return (IndustrialHeat.Node)h.getLevel().getBlockEntity(h.absolutePos(at));
    }
    @GameTest public void electricalHeatAndStirlingHaveNetLoss(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var heat=node(h,"electric_heat_generator",new BlockPos(2,2,2),Direction.EAST);
            var generator=node(h,"stirling_generator",new BlockPos(3,2,2),Direction.WEST);
            heat.owner=p.getUUID();generator.owner=p.getUUID();heat.energy.setAmount(320);heat.step(h.getLevel());
            h.assertTrue(heat.energy.getAmount()==0&&heat.heat==256,"320 JE becomes exactly 256 heat");
            generator.step(h.getLevel());generator.step(h.getLevel());
            h.assertTrue(heat.heat==0&&generator.energy.getAmount()==128,"Stirling consumes all 256 heat for 128 JE; a round trip cannot multiply energy");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void fullHeatBufferPreservesUnburnedFuel(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var source=node(h,"solid_heat_generator",new BlockPos(2,2,2),Direction.NORTH);source.owner=p.getUUID();source.heat=4096;source.setItem(0,new ItemStack(Items.COAL,2));source.step(h.getLevel());
            h.assertTrue(source.getItem(0).getCount()==2&&source.burn==0,"Full heat buffer does not consume fuel");
            source.heat=4088;source.step(h.getLevel());
            h.assertTrue(source.heat==4096&&source.getItem(0).getCount()==1&&source.burn==1599,"Exactly one real fuel item pays for a finite burn");
            source.step(h.getLevel());h.assertTrue(source.burn==1599,"Stored burn pauses when heat cannot fit");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void motorWithoutLoadDoesNotConsumePower(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var motor=node(h,"electric_kinetic_generator",new BlockPos(2,2,2),Direction.NORTH);motor.owner=p.getUUID();motor.energy.setAmount(100);motor.step(h.getLevel());
            h.assertTrue(motor.energy.getAmount()==100,"Disconnected shaft does not spend JE");
            var source=node(h,"electric_heat_generator",new BlockPos(4,2,2),Direction.NORTH);source.energy.setAmount(100);source.step(h.getLevel());
            h.assertTrue(source.heat==0&&source.energy.getAmount()==100,"Missing owner pauses conversion");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void reversedHeatFacesDoNotTransfer(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var source=node(h,"solid_heat_generator",new BlockPos(2,2,2),Direction.NORTH);source.owner=p.getUUID();source.heat=256;
            var receiver=node(h,"stirling_generator",new BlockPos(3,2,2),Direction.WEST);receiver.owner=p.getUUID();receiver.step(h.getLevel());
            h.assertTrue(source.heat==256&&receiver.energy.getAmount()==0,"Side of heat source is not an output face");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }

    @GameTest public void hotCoolantPausesWithoutOutputRoom(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var machine=node(h,"liquid_heat_exchanger",new BlockPos(2,2,2),Direction.EAST);machine.owner=p.getUUID();
            machine.input.fill(IndustrialHeat.HOT_COOLANT.source(),1000);machine.output.fill(IndustrialHeat.COOLANT.source(),16000);
            machine.step(h.getLevel());h.assertTrue(machine.input.millibuckets()==1000&&machine.heat==0,"Full coolant output does not consume hot coolant");
            machine.output.drain(64);machine.step(h.getLevel());
            h.assertTrue(machine.input.millibuckets()==936&&machine.output.millibuckets()==16000&&machine.heat==256,"64 mB hot coolant returns 64 mB cold and exactly 256 heat");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void boilerConservesHeatAndWater(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var source=node(h,"electric_heat_generator",new BlockPos(2,2,2),Direction.EAST);source.owner=p.getUUID();source.heat=128;
            var boiler=node(h,"steam_generator",new BlockPos(3,2,2),Direction.WEST);boiler.owner=p.getUUID();boiler.input.fill(net.minecraft.world.level.material.Fluids.WATER,32);boiler.step(h.getLevel());
            h.assertTrue(source.heat==0&&boiler.input.millibuckets()==0&&boiler.output.millibuckets()==320,"128 heat and 32 water produce 320 steam");
            var turbine=node(h,"steam_kinetic_generator",new BlockPos(5,2,2),Direction.NORTH);turbine.owner=p.getUUID();turbine.input.fill(IndustrialHeat.STEAM.fluid(),320);turbine.step(h.getLevel());
            h.assertTrue(turbine.input.millibuckets()==0&&turbine.output.millibuckets()==32,"Steam condenses back into exactly its source water");
            turbine.input.fill(IndustrialHeat.STEAM.fluid(),320);turbine.step(h.getLevel());
            h.assertTrue(turbine.input.millibuckets()==320&&turbine.output.millibuckets()==32,"A disconnected turbine retains its kinetic credit instead of burning another batch");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void brokenVesselStopsFuelBeforeSwitchingModes(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var center=new BlockPos(3,3,3);h.setBlock(center,io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor.CORE);
            var core=(io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor.Core)h.getLevel().getBlockEntity(h.absolutePos(center));core.owner=p.getUUID();
            for(int x=-2;x<=2;x++)for(int y=-2;y<=2;y++)for(int z=-2;z<=2;z++){
                if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))==2)h.setBlock(center.offset(x,y,z),io.github.jimbozoomer.jugcraft.jimtafacts.industry.ReactorVessel.VESSEL);
                else if(Math.abs(x)+Math.abs(y)+Math.abs(z)==1)h.setBlock(center.offset(x,y,z),io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor.CHAMBER);
                else if(x!=0||y!=0||z!=0)h.setBlock(center.offset(x,y,z),Blocks.AIR);
            }
            h.setBlock(center.offset(0,0,-2),io.github.jimbozoomer.jugcraft.jimtafacts.industry.ReactorVessel.FLUID_PORT);core.heat=400;core.coolant.fill(IndustrialHeat.COOLANT.source(),100);core.step(h.getLevel());
            h.assertTrue(core.fluidMode&&core.heat==0&&core.hotCoolant.millibuckets()==100,"A complete vessel transfers hull heat into equal-volume hot coolant");
            core.enabled=true;h.setBlock(center.offset(2,0,0),Blocks.AIR);core.step(h.getLevel());
            h.assertTrue(!core.enabled&&core.fault&&!core.fluidMode,"Breaking the shell stops the reactor, with a fault that requires manual reset");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }

    @GameTest public void laserTransfersOnlyThroughClearPath(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var from=new BlockPos(2,2,2);var to=new BlockPos(5,2,2);
            h.setBlock(from,io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopLaser.BLOCK.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopLaser.FACING,Direction.EAST));
            h.setBlock(to,io.github.jimbozoomer.jugcraft.jimtafacts.industry.JimtaIndustry.MACHINES.get("jimta_assembly_table"));
            var laser=(io.github.jimbozoomer.jugcraft.jimtafacts.industry.WorkshopLaser.Node)h.getLevel().getBlockEntity(h.absolutePos(from));laser.owner=p.getUUID();laser.energy.setAmount(500);
            var target=io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(h.getLevel(),h.absolutePos(to),Direction.WEST);long before=target.getAmount();
            laser.step(h.getLevel());long spent=500-laser.energy.getAmount();
            h.assertTrue(spent>0&&target.getAmount()-before==spent&&laser.beamDistance==3,"Visible beam corresponds to a conserved transfer into the real table");
            h.setBlock(new BlockPos(3,2,2),Blocks.STONE);before=laser.energy.getAmount();laser.step(h.getLevel());
            h.assertTrue(laser.energy.getAmount()==before&&laser.beamDistance==0,"Obstruction stops both power and its visual beam");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void isotopeFuelWearPersistsOnThePellet(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var source=node(h,"radioisotope_heat_generator",new BlockPos(2,2,2),Direction.NORTH);source.owner=p.getUUID();
            var pellet=new ItemStack(io.github.jimbozoomer.jugcraft.jimtafacts.industry.CompactReactor.PARTS.get("jimta_rtg_pellet"));source.setItem(0,pellet);source.step(h.getLevel());
            h.assertTrue(source.heat==4&&source.getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getIntOr("RtgTicks",0)==1,"Used time follows the item, not only its former machine");
            source.heat=4096;source.step(h.getLevel());h.assertTrue(source.getItem(0).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getIntOr("RtgTicks",0)==1,"Full buffer preserves isotope lifetime");
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,pellet,tag->tag.putInt("RtgTicks",399999));source.heat=0;source.step(h.getLevel());
            h.assertTrue(source.heat==4&&source.getItem(0).isEmpty(),"Last paid tick consumes the exhausted pellet");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
}
