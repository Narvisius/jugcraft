package io.github.jimbozoomer.jugcraft.test;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialInfrastructure;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class JimtaInfrastructureGameTests {
    @GameTest public void chargePadTransfersRealEnergyAndCannotChargeThroughSomeoneElsesClaim(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var at=new BlockPos(2,2,2);h.setBlock(at,IndustrialInfrastructure.BLOCKS.get("chargepad_batbox"));var n=h.getBlockEntity(at, IndustrialInfrastructure.Node.class);n.energy.setAmount(1000);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(JugcraftTools.MINING_DRILL));p.snapTo(Vec3.atBottomCenterOf(h.absolutePos(at.above())));
        try(var claim=TestClaims.open().block(h.absolutePos(at),UUID.randomUUID())){n.step(h.getLevel());h.assertTrue(n.energy.getAmount()==1000&&Chargeable.energy(p.getMainHandItem())==0,"Claim denies taking power from another player's pad");}
        n.step(h.getLevel());h.assertTrue(Chargeable.energy(p.getMainHandItem())==160&&n.energy.getAmount()==840,"Tier-one pad transfers one finite 160 JE step");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
    @GameTest public void teleporterRequiresClearLoadedReciprocalPadAndRealPower(GameTestHelper h){var p=JimtaRobotGameTests.player(h);try{
        var a=new BlockPos(2,2,2);var b=new BlockPos(5,2,2);h.setBlock(a,IndustrialInfrastructure.BLOCKS.get("teleporter"));h.setBlock(b,IndustrialInfrastructure.BLOCKS.get("teleporter"));var first=h.getBlockEntity(a, IndustrialInfrastructure.Node.class);var second=h.getBlockEntity(b, IndustrialInfrastructure.Node.class);first.target=h.absolutePos(b);second.target=h.absolutePos(a);first.energy.setAmount(20000);h.setBlock(b.above(),Blocks.STONE);
        h.assertTrue(!first.teleport(h.getLevel(),p)&&first.energy.getAmount()==20000,"Blocked arrival must not teleport or consume JE");h.setBlock(b.above(),Blocks.AIR);h.setBlock(b.above(2),Blocks.AIR);long cost=first.teleportCost();
        h.assertTrue(first.teleport(h.getLevel(),p)&&p.blockPosition().equals(h.absolutePos(b.above()))&&first.energy.getAmount()==20000-cost,"Explicit travel lands safely above the paired pad and charges once");
        second.target=null;h.assertTrue(!first.teleport(h.getLevel(),p)&&first.energy.getAmount()==20000-cost,"Broken reciprocal link fails closed");
    }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();}
}
