package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class AlchemicalGearGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        p.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        ConcordanceProgress.grant(p, "jugcraft:first_light", ResearchState.UNDERSTOOD);
        ConcordanceProgress.grant(p, Assaying.RESEARCH, ResearchState.UNDERSTOOD);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AlchemicalGear.STONE));
        Assaying.setBalance(p, 1000); RateGate.forget(p.getUUID()); return p;
    }
    private static ItemStack active(Item item) { var s = new ItemStack(item); s.set(AlchemicalGear.ACTIVE, Unit.INSTANCE); return s; }
    private static void press(TransmutationMenu menu, ServerPlayer p, int id) { RateGate.forget(p.getUUID()); menu.clickMenuButton(p, id); }
    private static void slot(TransmutationMenu m, int i, ItemStack stack) { m.getSlot(i).set(stack); m.broadcastChanges(); }
    private static ItemEntity drop(ServerPlayer p, double x, double z) {
        var d = new ItemEntity(p.level(), p.getX() + x, p.getY() + .2, p.getZ() + z, new ItemStack(Items.IRON_INGOT));
        d.setDeltaMovement(Vec3.ZERO); d.setNoPickUpDelay(); p.level().addFreshEntity(d); return d;
    }

    @GameTest(maxTicks=20) public void stoneConfirmsAndKeepsItsSample(GameTestHelper h) {
        var p=player(h); var m=new TransmutationMenu(1,p.getInventory());
        slot(m,0,new ItemStack(Items.IRON_INGOT,4)); slot(m,1,new ItemStack(Items.IRON_INGOT));
        press(m,p,0); h.assertTrue(m.confirming() && m.getSlot(0).getItem().getCount()==4 && Assaying.balance(p)==1000,"First click only confirms");
        press(m,p,0); h.assertTrue(m.getSlot(0).getItem().isEmpty() && Assaying.balance(p)==2024,"Four ingots dissolve once");
        press(m,p,1); h.assertTrue(m.getSlot(1).getItem().getCount()==1 && m.getSlot(2).getItem().getCount()==1 && Assaying.balance(p)==1768,"Sample kept; output charged 256");
        m.removed(p); h.assertTrue(p.getInventory().countItem(Items.IRON_INGOT)==2,"Closing returns sample and output"); h.succeed();
    }
    @GameTest(maxTicks=20) public void changedInputRequiresFreshConfirmation(GameTestHelper h) {
        var p=player(h); var m=new TransmutationMenu(1,p.getInventory());
        slot(m,0,new ItemStack(Items.IRON_INGOT,2)); press(m,p,0);
        slot(m,0,new ItemStack(Items.IRON_INGOT,3)); press(m,p,0);
        h.assertTrue(Assaying.balance(p)==1000 && m.getSlot(0).getItem().getCount()==3 && m.confirming(),"Changed quantity cannot inherit confirmation"); h.succeed();
    }
    @GameTest(maxTicks=20) public void modifiedMatterAndFullOutputNeverCharge(GameTestHelper h) {
        var p=player(h); var m=new TransmutationMenu(1,p.getInventory());
        var named=new ItemStack(Items.IRON_INGOT); named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));
        slot(m,0,named); slot(m,1,named.copy()); press(m,p,0); press(m,p,1);
        h.assertTrue(!m.canDissolve() && !m.canForm() && Assaying.balance(p)==1000,"Named matter refused");
        slot(m,1,new ItemStack(Items.IRON_INGOT)); slot(m,2,new ItemStack(Items.IRON_INGOT,64)); press(m,p,1);
        h.assertTrue(m.formStatus()==4 && Assaying.balance(p)==1000,"Full output never debits");
        slot(m,2,new ItemStack(Items.COPPER_INGOT)); press(m,p,1);
        h.assertTrue(m.formStatus()==4 && Assaying.balance(p)==1000,"Different output never overwritten"); h.succeed();
    }
    @GameTest(maxTicks=20) public void ledgerCapAndLargePreviewAreExact(GameTestHelper h) {
        var p=player(h); Assaying.setBalance(p,Assay.MAX_BALANCE-1); var m=new TransmutationMenu(1,p.getInventory());
        slot(m,0,new ItemStack(Items.GOLD_BLOCK,64));
        h.assertTrue(m.balance()==Assay.MAX_BALANCE-1 && m.gain()==1179648 && !m.canDissolve(),"32-bit values survive short menu data; full ledger blocks whole stack");
        press(m,p,0); press(m,p,0); h.assertTrue(Assaying.balance(p)==Assay.MAX_BALANCE-1 && m.getSlot(0).getItem().getCount()==64,"No partial surprise dissolve"); h.succeed();
    }
    @GameTest(maxTicks=20) public void missingStoneAndForeignPlayerCannotTransact(GameTestHelper h) {
        var p=player(h); var m=new TransmutationMenu(1,p.getInventory()); slot(m,1,new ItemStack(Items.IRON_INGOT));
        var other=player(h); press(m,other,1); h.assertTrue(Assaying.balance(p)==1000 && m.getSlot(2).getItem().isEmpty(),"Foreign owner rejected");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); press(m,p,1);
        h.assertTrue(!m.stillValid(p) && Assaying.balance(p)==1000,"Losing stone invalidates menu"); h.succeed();
    }
    @GameTest(maxTicks=20) public void repairPaysOnlyActualDamageAndPreservesName(GameTestHelper h) {
        var p=player(h); var tool=new ItemStack(Items.IRON_PICKAXE); tool.setDamageValue(3); tool.set(DataComponents.CUSTOM_NAME,Component.literal("Family pick"));
        p.setItemInHand(InteractionHand.MAIN_HAND,tool); p.setItemInHand(InteractionHand.OFF_HAND,active(AlchemicalGear.TALISMAN));
        AlchemicalGear.pulse(p); h.assertTrue(tool.getDamageValue()==0 && Assaying.balance(p)==997 && tool.getHoverName().getString().equals("Family pick"),"Only 3 points paid; name intact");
        AlchemicalGear.pulse(p); h.assertTrue(Assaying.balance(p)==997,"No charge for healthy tools");
        tool.setDamageValue(20); Assaying.setBalance(p,2); AlchemicalGear.pulse(p);
        h.assertTrue(tool.getDamageValue()==18 && Assaying.balance(p)==0,"Insufficient fuel repairs only affordable points"); h.succeed();
    }
    @GameTest(maxTicks=20) public void duplicateTalismansRepairOnlyOnce(GameTestHelper h) {
        var p=player(h); p.setItemInHand(InteractionHand.MAIN_HAND,active(AlchemicalGear.TALISMAN)); p.setItemInHand(InteractionHand.OFF_HAND,active(AlchemicalGear.TALISMAN));
        var helmet=new ItemStack(Items.IRON_HELMET); helmet.setDamageValue(20); p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,helmet);
        AlchemicalGear.pulse(p); h.assertTrue(helmet.getDamageValue()==16 && Assaying.balance(p)==996,"Two talismans still repair four"); h.succeed();
    }
    @GameTest(maxTicks=20) public void talismanWorksInExistingCharmSlot(GameTestHelper h) {
        var p=player(h); var tool=new ItemStack(Items.IRON_PICKAXE); tool.setDamageValue(20);
        p.setItemInHand(InteractionHand.MAIN_HAND,tool);
        var inventory=eu.pb4.trinkets.api.TrinketsApi.getAttachment(p).getInventory("legs/charm");
        h.assertTrue(inventory!=null,"Existing Charm slot is available");
        inventory.setItem(0,active(AlchemicalGear.TALISMAN)); AlchemicalGear.pulse(p);
        h.assertTrue(tool.getDamageValue()==16 && Assaying.balance(p)==996,"Worn charm repairs without occupying either hand"); h.succeed();
    }
    @GameTest(maxTicks=20) public void tradedGearWorksWithoutResearch(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);Assaying.setBalance(p,100);
        var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(20);
        p.setItemInHand(InteractionHand.MAIN_HAND,tool);p.setItemInHand(InteractionHand.OFF_HAND,active(AlchemicalGear.TALISMAN));
        AlchemicalGear.pulse(p);h.assertTrue(tool.getDamageValue()==16 && Assaying.balance(p)==96,"Traded active gear works immediately and still pays essence");h.succeed();
    }
    @GameTest(maxTicks=20) public void inactiveAndDisabledGearDoesNothing(GameTestHelper h) {
        var p=player(h); var tool=new ItemStack(Items.IRON_PICKAXE); tool.setDamageValue(20);
        p.setItemInHand(InteractionHand.MAIN_HAND,tool); p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(AlchemicalGear.TALISMAN));
        AlchemicalGear.pulse(p); h.assertTrue(tool.getDamageValue()==20,"Defaults off");
        p.getOffhandItem().set(AlchemicalGear.ACTIVE,Unit.INSTANCE);
        boolean before=JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
        try { JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,false); AlchemicalGear.pulse(p); }
        finally { JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,before); }
        h.assertTrue(tool.getDamageValue()==20 && Assaying.balance(p)==1000,"Feature switch prevents work"); h.succeed();
    }
    @GameTest(maxTicks=20) public void bandRespectsReservationsDelayAndThrower(GameTestHelper h) {
        var p=player(h); p.setItemInHand(InteractionHand.MAIN_HAND,active(AlchemicalGear.BAND));
        var reserved=drop(p,3,0); reserved.setTarget(UUID.randomUUID());
        var delayed=drop(p,3,1); delayed.setPickUpDelay(40);
        var thrown=drop(p,3,2); thrown.setThrower(player(h));
        var free=drop(p,3,-1); AlchemicalGear.pulse(p);
        h.assertTrue(reserved.getDeltaMovement().equals(Vec3.ZERO) && delayed.getDeltaMovement().equals(Vec3.ZERO) && thrown.getDeltaMovement().equals(Vec3.ZERO),"Other player's drops and delayed drops stay still");
        h.assertTrue(free.getDeltaMovement().x<0 && Assaying.balance(p)==999,"Only free visible drop moves and costs one: " + free.getDeltaMovement() + ", grains=" + Assaying.balance(p)); h.succeed();
    }
    @GameTest(maxTicks=20) public void bandWorkAndDuplicatesAreBounded(GameTestHelper h) {
        var p=player(h); p.setItemInHand(InteractionHand.MAIN_HAND,active(AlchemicalGear.BAND)); p.setItemInHand(InteractionHand.OFF_HAND,active(AlchemicalGear.BAND));
        var drops=new java.util.ArrayList<ItemEntity>(); for(int i=0;i<12;i++) drops.add(drop(p,3,i*.05));
        AlchemicalGear.pulse(p); h.assertTrue(drops.stream().filter(d->d.getDeltaMovement().lengthSqr()>0).count()==8 && Assaying.balance(p)==992,"At most eight moves and eight grains despite two bands"); h.succeed();
    }
    @GameTest(maxTicks=20) public void bandCannotPullThroughWallOrIntoFullInventory(GameTestHelper h) {
        var p=player(h); p.setItemInHand(InteractionHand.OFF_HAND,active(AlchemicalGear.BAND)); var d=drop(p,4,0);
        var wall=p.blockPosition().offset(2,0,0);
        for(int y=0;y<4;y++) for(int z=-2;z<=2;z++) p.level().setBlockAndUpdate(wall.offset(0,y,z),Blocks.STONE.defaultBlockState());
        AlchemicalGear.pulse(p); h.assertTrue(d.getDeltaMovement().equals(Vec3.ZERO) && Assaying.balance(p)==1000,"Solid wall blocks attraction");
        var visible=drop(p,0,3); for(int i=0;i<36;i++) p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        AlchemicalGear.pulse(p); h.assertTrue(visible.getDeltaMovement().equals(Vec3.ZERO) && Assaying.balance(p)==1000,"No room means no movement or payment"); h.succeed();
    }
}
