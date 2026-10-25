package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.minecraft.core.Direction;
import java.util.UUID;

public final class JimtaProductionGameTests {
    @GameTest public void cellsConserveFluidAcrossCapacityRollbackAndMixedInsert(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(IndustrialProduction.ITEMS.get("jimta_fluid_cell")));
            var storage=IndustrialProduction.Cell.storage(ContainerItemContext.ofPlayerHand(player,InteractionHand.MAIN_HAND));var water=FluidVariant.of(Fluids.WATER);long capacity=IndustrialProduction.Cell.CAPACITY;
            try(var tx=Transaction.openOuter()){h.assertTrue(storage.insert(water,capacity*2,tx)==capacity,"Cell caps fill at exactly one bucket");}
            h.assertTrue(storage.iterator().next().getAmount()==0,"Uncommitted insertion rolls back item component");
            try(var tx=Transaction.openOuter()){storage.insert(water,capacity,tx);tx.commit();}
            try(var tx=Transaction.openOuter()){h.assertTrue(storage.insert(FluidVariant.of(Fluids.LAVA),capacity,tx)==0,"Different fluid cannot overwrite water");tx.commit();}
            try(var tx=Transaction.openOuter()){h.assertTrue(storage.extract(water,capacity/2,tx)==capacity/2,"Partial extraction conserved");tx.commit();}
            h.assertTrue(IndustrialProduction.Cell.contents(player.getMainHandItem()).amount()==capacity/2,"Remaining half bucket stays on actual item");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void protectedTankUseDoesNotMutateCell(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=h.absolutePos(new BlockPos(2,2,2));var item=IndustrialProduction.ITEMS.get("jimta_fluid_cell");
            player.setItemInHand(InteractionHand.MAIN_HAND,IndustrialProduction.Cell.filled(new ItemStack(item),FluidVariant.of(Fluids.WATER),IndustrialProduction.Cell.CAPACITY));
            try(var claims=TestClaims.open().block(pos,UUID.randomUUID())){
                var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                h.assertTrue(item.useOn(context)==net.minecraft.world.InteractionResult.FAIL,"Claim blocks cell access before looking up a transfer target");
            }
            h.assertTrue(IndustrialProduction.Cell.contents(player.getMainHandItem()).amount()==IndustrialProduction.Cell.CAPACITY,"Denied use preserves all stored fluid");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
}
