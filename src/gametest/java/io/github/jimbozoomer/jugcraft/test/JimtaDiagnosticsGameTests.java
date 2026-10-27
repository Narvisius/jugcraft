package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.FactoryDiagnostics;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

public final class JimtaDiagnosticsGameTests {
    @GameTest public void diagnosticLoadCountsOnlyCommittedEnergyAndHonorsCachedPortPause(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,FactoryDiagnostics.TESTER);var node=h.getBlockEntity(pos,FactoryDiagnostics.Node.class);var port=node.energy;
        try(var t=Transaction.openOuter()){port.insert(500,t);}
        h.assertTrue(node.total()==0&&port.getAmount()==0,"Rolled-back simulation cannot increase totals");
        try(var t=Transaction.openOuter()){port.insert(700,t);t.commit();}
        h.assertTrue(node.total()==700&&port.getAmount()==700,"Committed input counted once");node.tick();
        h.assertTrue(node.total()==700&&port.getAmount()==0,"Consumed JE cannot be extracted or counted twice");
        h.setBlock(pos.east(),Blocks.REDSTONE_BLOCK);
        try(var t=Transaction.openOuter()){h.assertTrue(port.insert(100,t)==0,"Already cached port also respects redstone pause");t.commit();}
        h.assertTrue(node.total()==700,"Paused insertion does not change counters");h.succeed();
    }
    @GameTest public void magneticLiftRequiresBootsPowerAndConnectedAuthorizedFence(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);
        try{
            var pos=new BlockPos(2,2,3);var fence=pos.north();
            h.setBlock(pos,io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialMagnetizer.BLOCK);
            h.setBlock(fence,io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.IRON_FENCE);
            h.setBlock(fence.above(),io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.IRON_FENCE);
            var node=h.getBlockEntity(pos,io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialMagnetizer.Node.class);
            node.owner=player.getUUID();node.energy.setAmount(100);
            player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(fence)).add(.95,0,0));
            player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);node.tick();
            h.assertTrue(node.energy.getAmount()==100&&player.getDeltaMovement().y==0,"No boots means no lift or power cost");
            player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_BOOTS));
            node.tick();h.assertTrue(node.energy.getAmount()==96&&player.getDeltaMovement().y>0,"Boots lift at four JE per tick");
            node.tick();h.assertTrue(node.energy.getAmount()==96,"A rider is charged at most once per game tick");
            player.setShiftKeyDown(true);
            h.runAfterDelay(1,()->{
                try{
                    node.tick();h.assertTrue(player.getDeltaMovement().y<0,"Sneaking selects controlled descent on the next tick");
                    player.setShiftKeyDown(false);h.setBlock(pos.east(),Blocks.REDSTONE_BLOCK);long before=node.energy.getAmount();node.tick();
                    h.assertTrue(node.energy.getAmount()==before,"Redstone pauses the elevator");
                    h.setBlock(pos.east(),Blocks.AIR);
                    try(var claim=TestClaims.open().block(h.absolutePos(fence),java.util.UUID.randomUUID())){node.tick();h.assertTrue(node.energy.getAmount()==before,"Protected front column cannot be energized");}
                    h.succeed();
                }finally{h.getLevel().getServer().getPlayerList().remove(player);}
            });
        }catch(RuntimeException|AssertionError failure){h.getLevel().getServer().getPlayerList().remove(player);throw failure;}
    }
}
