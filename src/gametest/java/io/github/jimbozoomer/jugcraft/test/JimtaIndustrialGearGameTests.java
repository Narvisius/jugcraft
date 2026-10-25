package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.tools.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;

/** Finite shared JE, existing flight and protected world edits; written for the final combined test run. */
public final class JimtaIndustrialGearGameTests {
    private static ItemStack gear(String id,long charge){var s=new ItemStack(IndustrialGear.ITEMS.get("jimta_"+id));Chargeable.setEnergy(s,charge);return s;}
    @GameTest public void batteryTransfersRealChargeOnceAndCannotChargeAnotherSupply(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var supply=gear("re_battery",500);p.setItemInHand(InteractionHand.MAIN_HAND,supply);
            var other=gear("advanced_battery",0);p.getInventory().setItem(1,other);var drill=new ItemStack(JugcraftTools.MINING_DRILL);p.getInventory().setItem(2,drill);
            supply.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
            h.assertTrue(Chargeable.energy(supply)==0&&Chargeable.energy(drill)==500&&Chargeable.energy(other)==0,"Supply loses exactly the JE received by a real tool; batteries do not charge each other");
            supply.getItem().use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(Chargeable.energy(drill)==500,"Empty supply cannot create charge");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void batteryPackChargesOnlyWhileWornAndEnabled(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var pack=gear("battery_pack",1000);var drill=new ItemStack(JugcraftTools.MINING_DRILL);p.getInventory().setItem(2,drill);p.getInventory().setItem(3,pack);p.tickCount=20;
            IndustrialGear.tick(p);h.assertTrue(Chargeable.energy(drill)==0,"Stowed battery pack has no passive effect");
            p.getInventory().setItem(3,ItemStack.EMPTY);p.setItemSlot(EquipmentSlot.CHEST,pack);IndustrialGear.tick(p);
            h.assertTrue(Chargeable.energy(drill)==256&&Chargeable.energy(pack)==744,"Worn backpack pays the entire transfer");
            CustomData.update(DataComponents.CUSTOM_DATA,pack,t->t.putBoolean("JimtaGearOff",true));IndustrialGear.tick(p);
            h.assertTrue(Chargeable.energy(drill)==256&&Chargeable.energy(pack)==744,"Disabled equipment performs no transfer");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void nanoShieldScalesToActualAvailableEnergy(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var suit=gear("nano_chestplate",500);p.setItemSlot(EquipmentSlot.CHEST,suit);
            h.assertTrue(IndustrialGear.absorb(p,p.damageSources().generic(),10)==10&&Chargeable.energy(suit)==500,"Armor-bypassing damage leaves shield charge untouched");
            var attacker=h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE,2,2,2);
            float damage=IndustrialGear.absorb(p,p.damageSources().mobAttack(attacker),10);
            h.assertTrue(Math.abs(damage-9.5F)<.0001F&&Chargeable.energy(suit)==0,"Five hundred JE can absorb only half one damage point");
            h.assertTrue(IndustrialGear.absorb(p,p.damageSources().mobAttack(attacker),10)==10,"Empty shield cannot reduce another hit");
            Chargeable.setEnergy(suit,10000);CustomData.update(DataComponents.CUSTOM_DATA,suit,t->t.putBoolean("JimtaGearOff",true));
            h.assertTrue(IndustrialGear.absorb(p,p.damageSources().mobAttack(attacker),10)==10&&Chargeable.energy(suit)==10000,"Disabled shield spends no power and grants no bonus");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void quantumChestUsesExistingPaidJetpackInput(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var suit=gear("quantum_chestplate",50);p.setItemSlot(EquipmentSlot.CHEST,suit);
            h.assertTrue(RocketPackItem.canThrust(p),"Existing flight input recognizes charged quantum chest");RocketPackItem.thrust(p);
            h.assertTrue(Chargeable.energy(suit)==0&&!RocketPackItem.canThrust(p),"One real thrust pays shared fifty JE and exhausts the battery");
            Chargeable.setEnergy(suit,100);CustomData.update(DataComponents.CUSTOM_DATA,suit,t->t.putBoolean("JimtaGearOff",true));
            h.assertTrue(!RocketPackItem.canThrust(p),"Equipment toggle disables flight without changing research or global permissions");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
    @GameTest public void electricWrenchCannotRotateClaimedMachineOrSpendItsCharge(GameTestHelper h){
        var p=JimtaRobotGameTests.player(h);try{
            var at=new BlockPos(2,2,2);h.setBlock(at,JimtaIndustry.MACHINES.get("jimta_macerator"));var absolute=h.absolutePos(at);
            var wrench=gear("electric_wrench",200);p.setItemInHand(InteractionHand.MAIN_HAND,wrench);
            var ctx=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(absolute),Direction.NORTH,absolute,false));
            var facing=h.getBlockState(at).getValue(MachineBlock.FACING);
            try(var claim=TestClaims.open().block(absolute,UUID.randomUUID())){wrench.getItem().useOn(ctx);
                h.assertTrue(h.getBlockState(at).getValue(MachineBlock.FACING)==facing&&Chargeable.energy(wrench)==200,"Protection precedes machine mutation and charge debit");}
            wrench.getItem().useOn(ctx);h.assertTrue(h.getBlockState(at).getValue(MachineBlock.FACING)!=facing&&Chargeable.energy(wrench)==100,"Authorized turn pays once");
        }finally{h.getLevel().getServer().getPlayerList().remove(p);}h.succeed();
    }
}
