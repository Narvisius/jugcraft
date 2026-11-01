package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.botany.BotanicalManaItems;
import io.github.jimbozoomer.jugcraft.botany.Jugtania;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/** Conservation checks across the new portable mana consumers and sources. */
public final class NewestManaGameTests {
    @GameTest public void generatorDropPreservesCooldownWithoutCopyingInventory(GameTestHelper h){
        var at=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));
        h.getLevel().setBlockAndUpdate(at,Jugtania.block("thermalily").defaultBlockState());
        var flower=(io.github.jimbozoomer.jugcraft.botany.BotanicalWorkshop)h.getLevel().getBlockEntity(at);
        var saved=flower.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putInt("flower_cooldown",3210);saved.putInt("flower_age",1234);
        flower.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        flower.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,13));
        var drops=net.minecraft.world.level.block.Block.getDrops(flower.getBlockState(),h.getLevel(),at,flower);
        h.assertTrue(drops.size()==1&&drops.getFirst().has(io.github.jimbozoomer.jugcraft.botany.BotanicalFlowerMemory.TYPE),"Native loot carries flower memory");
        var replacement=new io.github.jimbozoomer.jugcraft.botany.BotanicalWorkshop(at,flower.getBlockState());replacement.setLevel(h.getLevel());
        replacement.applyComponentsFromItemStack(drops.getFirst());
        var restored=replacement.saveWithoutMetadata(h.getLevel().registryAccess());
        h.assertTrue(restored.getIntOr("flower_cooldown",0)==3210&&restored.getIntOr("flower_age",0)==1234,"Breaking does not reset cooldown or lifespan");
        h.assertTrue(replacement.getItem(0).isEmpty(),"Flower memory cannot duplicate an inventory");h.succeed();
    }
    @GameTest public void paymentSpansCarriedReservesWithoutSurcharge(GameTestHelper h){
        var p=TestPlayers.survivor(h);var a=new ItemStack(Jugtania.item("mana_ring"));var b=new ItemStack(Jugtania.item("mana_ring_greater"));
        BotanicalManaItems.stored(a,40);BotanicalManaItems.stored(b,80);p.getInventory().setItem(0,a);p.getInventory().setItem(1,b);
        h.assertTrue(BotanicalManaItems.pay(p,100),"Combined reserves pay exact cost");
        h.assertTrue(BotanicalManaItems.available(p)==20,"No mana duplication or surcharge");h.succeed();
    }
    @GameTest public void insufficientAndNegativePaymentsDoNotDrainItems(GameTestHelper h){
        var p=TestPlayers.survivor(h);var ring=new ItemStack(Jugtania.item("mana_ring"));BotanicalManaItems.stored(ring,75);p.getInventory().setItem(0,ring);
        h.assertTrue(!BotanicalManaItems.pay(p,76)&&!BotanicalManaItems.pay(p,-1),"Invalid payments refused");
        h.assertTrue(BotanicalManaItems.stored(ring)==75,"Failed requests preserve balance");h.succeed();
    }
    @GameTest public void chargingClampsCapacityAndReturnsActualAcceptedAmount(GameTestHelper h){
        var p=TestPlayers.survivor(h);var ring=new ItemStack(Jugtania.item("mana_ring"));int cap=BotanicalManaItems.capacity(ring);BotanicalManaItems.stored(ring,cap-7);p.getInventory().setItem(0,ring);
        h.assertTrue(BotanicalManaItems.fill(p,100)==7&&BotanicalManaItems.stored(ring)==cap,"Only available room accepted");
        h.assertTrue(BotanicalManaItems.fill(p,5)==0,"Full capacity consumes no offered mana");h.succeed();
    }
    @GameTest public void terraPickExperienceIsNotSpendablePortableMana(GameTestHelper h){
        var p=TestPlayers.survivor(h);var pick=new ItemStack(Jugtania.item("terra_pick"));BotanicalManaItems.stored(pick,1000);p.getInventory().setItem(0,pick);
        h.assertTrue(BotanicalManaItems.available(p)==0&&!BotanicalManaItems.pay(p,1),"Mining progression cannot pay equipment costs");
        h.assertTrue(BotanicalManaItems.stored(pick)==1000,"Mining progression preserved");h.succeed();
    }
}
