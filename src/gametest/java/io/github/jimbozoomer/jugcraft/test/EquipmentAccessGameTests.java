package io.github.jimbozoomer.jugcraft.test;

import eu.pb4.trinkets.api.*;
import io.github.jimbozoomer.jugcraft.concordance.spirits.*;
import io.github.jimbozoomer.jugcraft.concordance.rime.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;

/** Fresh survival players, real resolved tags and Trinkets' actual insertion validator. */
public final class EquipmentAccessGameTests {
    @GameTest(maxTicks=40)
    public void allTaggedAccessoriesFitTheirSlotsWithoutResearch(GameTestHelper h) {
        var p=TestPlayers.survivor(h);var attachment=TrinketsApi.getAttachment(p);
        int checked=0, slots=0;var rejected=new java.util.ArrayList<String>();
        for(var entry:attachment.getInventories().entrySet()) {
            var inv=entry.getValue();if(inv.getContainerSize()==0)continue;
            var tag=TagKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath("trinkets",entry.getKey()));
            var access=inv.getOrCreateSlotAccess(0);int tagged=0;
            for(var holder:BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                var stack=new ItemStack(holder.value());checked++;tagged++;
                if(!TrinketSlotUtils.mayPlace(access,stack))rejected.add(holder.getRegisteredName()+" -> "+entry.getKey());
            }
            if(tagged>0) {
                slots++;
                h.assertTrue(!TrinketSlotUtils.mayPlace(access,new ItemStack(Items.DIRT)),"Wrong item remains rejected in "+entry.getKey());
            }
        }
        h.assertTrue(rejected.isEmpty(),"Owned accessories rejected: "+rejected);
        h.assertTrue(slots>=8&&checked>=100,"Broad accessory coverage: "+checked+" item/slot pairs across "+slots+" slots");
        System.out.println("Equipment access: "+checked+" valid item/slot pairs across "+slots+" slot types; no research granted");
        h.succeed();
    }
    @GameTest(maxTicks=40)
    public void ownedBondingCharmBindsAndReleasesWithoutResearch(GameTestHelper h) {
        var p=TestPlayers.survivor(h);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Workers.BONDING_CHARM));
        h.assertTrue(BondingCharmItem.use(p,h.getLevel(),false)==InteractionResult.SUCCESS,"Fresh player binds familiar");
        var roster=WorkerRoster.of(h.getLevel().getServer());
        var first=roster.familiar(p.getUUID());h.assertTrue(first!=null,"Familiar tracked");
        h.assertTrue(BondingCharmItem.use(p,h.getLevel(),false)==InteractionResult.SUCCESS&&first.equals(roster.familiar(p.getUUID())),"Recall retains one familiar");
        h.assertTrue(BondingCharmItem.use(p,h.getLevel(),true)==InteractionResult.SUCCESS&&roster.familiar(p.getUUID())==null,"Release works");h.succeed();
    }
}
