package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.List;
import io.github.jimbozoomer.jugcraft.test.JimtaRobotGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Deferred end-to-end paged capture, reservation, cancellation, rollback and restoration. */
public final class LargeSpatialGameTests {
    private java.util.UUID residentId;
    @GameTest(maxTicks=400)
    public void pagedExchangeLocksRegionAndCancellationConservesCellAndEnergy(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);player.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(0,2,0))));
        for(int x:new int[]{2,5})for(int y:new int[]{2,5})for(int z:new int[]{2,5})h.setBlock(new BlockPos(x,y,z),DigitalSpatial.PYLON);
        for(var p:BlockPos.betweenClosed(new BlockPos(3,3,3),new BlockPos(4,4,4)))h.setBlock(p,Blocks.AIR);
        var inside=h.absolutePos(new BlockPos(3,3,3));h.getLevel().setBlockAndUpdate(inside,Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(inside.east(),Blocks.BARREL.defaultBlockState());
        ((net.minecraft.world.level.block.entity.BarrelBlockEntity)h.getLevel().getBlockEntity(inside.east())).setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,3));
        var resident=net.minecraft.world.entity.EntityTypes.CHICKEN.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        h.assertTrue(resident!=null,"Chicken can be created");resident.setNoAi(true);resident.snapTo(inside.getX()+.5,inside.getY()+1,inside.getZ()+.5,0,0);residentId=resident.getUUID();h.getLevel().addFreshEntity(resident);
        var portPos=new BlockPos(0,2,2);h.setBlock(portPos,DigitalSpatial.PORT);h.setBlock(portPos.above(),Blocks.AIR);h.setBlock(portPos.above(2),Blocks.AIR);
        var port=(DigitalSpatial.Port)h.getLevel().getBlockEntity(h.absolutePos(portPos));port.owner=player.getUUID();port.energy.setAmount(10000);
        var cell=new ItemStack(DigitalStorage.ITEMS.get("spatial_storage_cell_128"));cell.set(DigitalSpatial.CORNERS,List.of(GlobalPos.of(h.getLevel().dimension(),h.absolutePos(new BlockPos(2,2,2))),GlobalPos.of(h.getLevel().dimension(),h.absolutePos(new BlockPos(5,5,5)))));port.setItem(0,cell);
        String started=LargeSpatialExchange.start(port,player);
        h.assertTrue(started.startsWith("Region reserved"),"Large cell starts asynchronously: "+started);
        h.assertTrue(!h.getLevel().setBlockAndUpdate(inside,Blocks.DIAMOND_BLOCK.defaultBlockState())&&port.removeItem(0,1).isEmpty(),"Reserved blocks and the input cell cannot be edited during capture");
        wait(h,port,player,inside,0,0);
    }
    private void wait(GameTestHelper h,DigitalSpatial.Port port,ServerPlayer player,BlockPos inside,int stage,int ticks){
        try{
            h.assertTrue(ticks<350,"Paged transfer must finish within its test budget");
            if(stage==1&&h.getLevel().getBlockState(inside).is(Blocks.STONE)&&LargeSpatialExchange.busy(port)){
                LargeSpatialExchange.start(port,player);h.runAfterDelay(1,()->wait(h,port,player,inside,2,ticks+1));return;
            }
            if(!LargeSpatialExchange.busy(port)){
                if(stage==0){
                    h.assertTrue(h.getLevel().getBlockState(inside).isAir()&&port.energy.getAmount()==8600&&port.getItem(1).has(DigitalSpatial.KEY)&&h.getLevel().getEntity(residentId)==null,"Capture consumes one fee, stores the resident and issues one keyed output cell");
                    h.runAfterDelay(4,()->{port.setItem(0,port.removeItem(1,1));h.assertTrue(LargeSpatialExchange.start(port,player).startsWith("Region reserved"),"Stored page can be restored");wait(h,port,player,inside,1,ticks+4);});return;
                }
                if(stage==2){
                    h.assertTrue(h.getLevel().getBlockState(inside).isAir()&&port.energy.getAmount()==8600&&port.getItem(1).isEmpty()&&port.getItem(0).get(DigitalSpatial.KEY).revision()==1,"Cancellation after writes rolls back without a charge or new revision");
                    h.runAfterDelay(4,()->{h.assertTrue(LargeSpatialExchange.start(port,player).startsWith("Region reserved"),"A rolled-back revision remains usable");wait(h,port,player,inside,3,ticks+4);});return;
                }
                if(stage==3){h.assertTrue(h.getLevel().getBlockState(inside).is(Blocks.STONE)&&port.energy.getAmount()==7200&&port.getItem(1).get(DigitalSpatial.KEY).revision()==2,"Completed restore returns the original block and charges only once");
                    h.assertTrue(h.getLevel().getBlockEntity(inside.east()) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel&&barrel.getItem(0).getCount()==3,"Container cargo survives capture, partial restoration rollback and final restoration");
                    var resident=h.getLevel().getEntity(residentId);h.assertTrue(resident!=null,"Resident keeps its UUID after paged transfer");resident.discard();h.getLevel().getServer().getPlayerList().remove(player);h.succeed();return;}
                h.fail("Restoration completed before cancellation was observed");return;
            }
            h.runAfterDelay(1,()->wait(h,port,player,inside,stage,ticks+1));
        }catch(RuntimeException|AssertionError failure){if(LargeSpatialExchange.busy(port))LargeSpatialExchange.start(port,player);h.getLevel().getServer().getPlayerList().remove(player);throw failure;}
    }
}
