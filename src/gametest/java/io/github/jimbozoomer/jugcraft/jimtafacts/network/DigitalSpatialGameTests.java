package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Authored for the final combined run, including real world exchange and stale-cell conservation. */
public final class DigitalSpatialGameTests {
    private record Setup(ServerPlayer player,DigitalSpatial.Port port,BlockPos inside) {}
    private static Setup setup(GameTestHelper h){
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
        player.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1))));
        for(int x:new int[]{2,5})for(int y:new int[]{2,5})for(int z:new int[]{2,5})h.setBlock(new BlockPos(x,y,z),DigitalSpatial.PYLON);
        for(int x=3;x<5;x++)for(int y=3;y<5;y++)for(int z=3;z<5;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        var at=new BlockPos(1,2,2);h.setBlock(at,DigitalSpatial.PORT);
        var port=(DigitalSpatial.Port)h.getLevel().getBlockEntity(h.absolutePos(at));port.owner=player.getUUID();port.energy.setAmount(10000);
        var cell=new ItemStack(DigitalStorage.ITEMS.get("spatial_storage_cell_2"));cell.set(DigitalSpatial.CORNERS,List.of(GlobalPos.of(h.getLevel().dimension(),h.absolutePos(new BlockPos(2,2,2))),GlobalPos.of(h.getLevel().dimension(),h.absolutePos(new BlockPos(5,5,5)))));port.setItem(0,cell);
        return new Setup(player,port,new BlockPos(3,3,3));
    }
    @GameTest(maxTicks=300) public void exchangeRestoresBlocksAndRejectsCopiedOldCell(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.STONE);
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.startsWith("Exchanged"),"First exchange succeeds: "+result);
        h.assertTrue(h.getBlockState(s.inside).isAir()&&s.port.energy.getAmount()==8600,"Eight-block plot consumes exactly 1,400 JE and removes stone");
        ItemStack stale=s.port.getItem(1).copy();s.port.setItem(0,s.port.removeItem(1,1));
        h.runAfterDelay(101,()->{
            String restored=DigitalSpatialJournal.exchange(s.port,s.player);
            h.assertTrue(restored.startsWith("Exchanged")&&h.getBlockState(s.inside).is(Blocks.STONE),"Same plot restores original block: "+restored);
            h.assertTrue(s.port.energy.getAmount()==7200,"Restore consumes exactly the same fee");
            s.port.setItem(1,ItemStack.EMPTY);s.port.setItem(0,stale);
            h.runAfterDelay(101,()->{
                String rejected=DigitalSpatialJournal.exchange(s.port,s.player);
                h.assertTrue(rejected.contains("stale or copied")&&h.getBlockState(s.inside).is(Blocks.STONE)&&s.port.energy.getAmount()==7200,"Copied revision neither restores twice nor spends JE: "+rejected);h.succeed();
            });
        });
    }
    @GameTest public void unsupportedContainerDoesNotConsumeCellOrPower(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.BARREL);var before=s.port.getItem(0).copy();
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.startsWith("Unsupported")&&s.port.energy.getAmount()==10000&&s.port.getItem(1).isEmpty()&&ItemStack.isSameItemSameComponents(before,s.port.getItem(0)),"Container rejection keeps cell, inventory and JE: "+result);h.succeed();
    }
    @GameTest public void missingPylonAndBlockedOutputNeverChangeWorld(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.STONE);s.port.setItem(1,new ItemStack(net.minecraft.world.item.Items.DIRT));
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.contains("clear the output")&&h.getBlockState(s.inside).is(Blocks.STONE)&&s.port.energy.getAmount()==10000,"Blocked output does not start exchange");
        s.port.setItem(1,ItemStack.EMPTY);h.setBlock(new BlockPos(5,5,5),Blocks.AIR);
        result=DigitalSpatialJournal.exchange(s.port,s.player);h.assertTrue(result.contains("Eight loaded")&&h.getBlockState(s.inside).is(Blocks.STONE)&&s.port.energy.getAmount()==10000,"Broken frame prevents exchange: "+result);h.succeed();
    }
}
