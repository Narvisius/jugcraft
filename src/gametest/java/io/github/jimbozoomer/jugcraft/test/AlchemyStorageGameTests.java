package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;

public class AlchemyStorageGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);
        var at=h.absolutePos(new BlockPos(2,2,2));p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);return p;}
    private static ItemStack item(String name){return new ItemStack(AlchemyStorage.ITEMS.get(name));}
    private static AlchemyStorageMenu bag(ServerPlayer p,ItemStack stack){p.getInventory().setItem(0,stack);return new AlchemyStorageMenu(1,p.getInventory(),InteractionHand.MAIN_HAND,stack);}
    private static AlchemyStorageBlockEntity chest(GameTestHelper h,ServerPlayer p){var pos=new BlockPos(2,2,2);h.setBlock(pos,AlchemyStorage.BLOCKS.get("alchemical_chest"));var be=h.getBlockEntity(pos,AlchemyStorageBlockEntity.class);be.placedBy(p);return be;}
    private static void use(ServerPlayer p,ItemStack s){p.getInventory().setItem(0,s);RateGate.forget(p.getUUID());s.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);}

    @GameTest(maxTicks=20) public void bagColorsArePhysicalAndIndependent(GameTestHelper h){
        var p=player(h);var first=item("white_alchemical_bag");var menu=bag(p,first);menu.contents().setItem(0,new ItemStack(Items.IRON_INGOT,23));menu.removed(p);
        var second=item("white_alchemical_bag");var other=bag(p,second);h.assertTrue(other.contents().isEmpty(),"Same color never shares another bag's contents");
        var reopened=bag(p,first);h.assertTrue(reopened.contents().getItem(0).getCount()==23,"Physical bag persists contents");h.succeed();
    }
    @GameTest(maxTicks=20) public void everyBagColorHasTheSameRealCapacity(GameTestHelper h){
        var p=player(h);for(String color:AlchemyStorage.COLORS){var stack=item(color+"_alchemical_bag");var m=bag(p,stack);m.contents().setItem(53,new ItemStack(Items.COAL,17));
            h.assertTrue(m.contents().getContainerSize()==54&&stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).size()==54,"All sixteen persist last slot");}
        h.succeed();
    }
    @GameTest(maxTicks=20) public void nestedBagsBundlesAndContainerBlocksAreRefused(GameTestHelper h){
        var p=player(h);var m=bag(p,item("red_alchemical_bag"));
        for(ItemStack bad:List.of(item("blue_alchemical_bag"),new ItemStack(Items.BUNDLE),new ItemStack(Items.SHULKER_BOX))){h.assertTrue(!m.slots.get(0).mayPlace(bad),"Portable nesting refused even when empty");}
        var chestStack=new ItemStack(Items.CHEST);chestStack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.COAL))));
        h.assertTrue(!AlchemyStorage.canStore(chestStack),"Chest carrying items cannot nest");
        for(Item block:List.of(Items.CHEST,Items.BARREL,Items.FURNACE))h.assertTrue(AlchemyStorage.canStore(new ItemStack(block)),"Ordinary empty container blocks remain building supplies");
        var payloadItem=new ItemStack(Items.PAPER);payloadItem.set(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        h.assertTrue(!AlchemyStorage.canStore(payloadItem),"Empty arbitrary portable container payload still refused");h.succeed();
    }
    @GameTest(maxTicks=20) public void bagLockRejectsPickupShiftAndHotbarSwap(GameTestHelper h){
        var p=player(h);var stack=item("blue_alchemical_bag");var m=bag(p,stack);m.contents().setItem(0,new ItemStack(Items.COAL,8));
        m.clicked(81,0,ContainerInput.PICKUP,p);h.assertTrue(m.getCarried().isEmpty()&&p.getInventory().getItem(0)==stack,"Cannot pick up open bag");
        h.assertTrue(m.quickMoveStack(p,81).isEmpty(),"Cannot shift open bag");m.clicked(0,0,ContainerInput.SWAP,p);
        h.assertTrue(p.getInventory().getItem(0)==stack&&m.contents().getItem(0).getCount()==8,"Number-key swap cannot replace locked bag");h.succeed();
    }
    @GameTest(maxTicks=20) public void offhandBagCannotBeSwappedThroughOtherSlots(GameTestHelper h){
        var p=player(h);var stack=item("green_alchemical_bag");p.setItemInHand(InteractionHand.OFF_HAND,stack);var m=new AlchemyStorageMenu(1,p.getInventory(),InteractionHand.OFF_HAND,stack);
        m.contents().setItem(0,new ItemStack(Items.COAL,12));m.clicked(0,40,ContainerInput.SWAP,p);
        h.assertTrue(p.getOffhandItem()==stack&&m.contents().getItem(0).getCount()==12,"Offhand swap is blocked");h.succeed();
    }
    @GameTest(maxTicks=20) public void movingBagExternallyInvalidatesItsMenuWithoutCopyingItems(GameTestHelper h){
        var p=player(h);var stack=item("lime_alchemical_bag");var m=bag(p,stack);m.contents().setItem(0,new ItemStack(Items.GOLD_INGOT,3));p.getInventory().setItem(0,ItemStack.EMPTY);
        m.clicked(0,0,ContainerInput.PICKUP,p);h.assertTrue(!m.stillValid(p)&&m.getCarried().isEmpty(),"Stale menu cannot take persisted inventory");
        m.removed(p);h.assertTrue(p.getInventory().countItem(Items.GOLD_INGOT)==0,"Closing does not duplicate bag contents into inventory");
        var n=bag(p,stack);h.assertTrue(n.contents().getItem(0).getCount()==3,"Original physical bag remains intact");h.succeed();
    }
    @GameTest(maxTicks=20) public void bagPreservesNamesAndOtherOrdinaryItemComponents(GameTestHelper h){
        var p=player(h);var stack=item("orange_alchemical_bag");var m=bag(p,stack);var pick=new ItemStack(Items.IRON_PICKAXE);pick.setDamageValue(31);pick.set(DataComponents.CUSTOM_NAME,Component.literal("My pick"));m.contents().setItem(17,pick);m.removed(p);
        var copy=stack.copy();var reopened=bag(p,copy);h.assertTrue(ItemStack.matches(reopened.contents().getItem(17),pick),"Damage and names survive physical component storage");h.succeed();
    }
    @GameTest(maxTicks=20) public void chestChecksOwnerClaimsAndNoAutomation(GameTestHelper h){
        var p=player(h);var be=chest(h,p);h.assertTrue(be.mayUse(p)&&!be.mayUse(player(h)),"Owner access, unrelated player refused");
        try(var claims=TestClaims.open()){claims.block(be.getBlockPos(),UUID.randomUUID());h.assertTrue(!be.mayUse(p),"Claim protection also applies");}
        for(Direction side:Direction.values()){h.assertTrue(be.getSlotsForFace(side).length==0,"Hoppers cannot bypass owner");
            var store=ItemStorage.SIDED.find(h.getLevel(),be.getBlockPos(),side);try(var t=Transaction.openOuter()){h.assertTrue(store==null||store.insert(ItemVariant.of(Items.COAL),1,t)==0,"Fabric automation refused");}}
        h.succeed();
    }
    @GameTest(maxTicks=20) public void chestSaveLoadPreservesPhysicalInventoryAndOwner(GameTestHelper h){
        var p=player(h);var be=chest(h,p);be.setItem(53,new ItemStack(Items.GOLD_INGOT,29));var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
        var copy=new AlchemyStorageBlockEntity(be.getBlockPos(),be.getBlockState());copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));copy.setLevel(h.getLevel());
        h.assertTrue(copy.getItem(53).getCount()==29&&copy.mayUse(p),"Inventory and owner survive actual block NBT");h.succeed();
    }
    @GameTest(maxTicks=20) public void mindStoneConservesRealPointsEvenWhenTotalExperienceIsStale(GameTestHelper h){
        var p=player(h);p.experienceLevel=0;p.experienceProgress=0;p.totalExperience=0;p.giveExperiencePoints(827);p.totalExperience=0;
        var stone=item("mind_stone");int before=AlchemyStorage.currentXp(p);int deposit=AlchemyStorage.transferXp(p,stone,false);
        h.assertTrue(deposit==100&&AlchemyStorage.currentXp(p)+AlchemyStorage.storedXp(stone)==before,"Uses actual bar, not stale lifetime counter");
        h.assertTrue(AlchemyStorage.transferXp(p,stone,true)==100&&AlchemyStorage.currentXp(p)==before&&AlchemyStorage.storedXp(stone)==0,"Exact round trip");h.succeed();
    }
    @GameTest(maxTicks=20) public void mindStoneRespectsZeroAndCapacity(GameTestHelper h){
        var p=player(h);p.experienceLevel=0;p.experienceProgress=0;p.totalExperience=0;var stone=item("mind_stone");h.assertTrue(AlchemyStorage.transferXp(p,stone,false)==0,"No negative XP");
        p.giveExperiencePoints(7);stone.set(AlchemyStorage.EXPERIENCE,AlchemyStorage.XP_CAPACITY-3);h.assertTrue(AlchemyStorage.transferXp(p,stone,false)==3,"Only remaining capacity is filled");
        h.assertTrue(AlchemyStorage.currentXp(p)==4&&AlchemyStorage.storedXp(stone)==AlchemyStorage.XP_CAPACITY,"Conserved at capacity boundary");h.succeed();
    }
    @GameTest(maxTicks=20) public void studyTomeCannotDeliverGhostSamples(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,item("alchemy_tome"));var m=new AlchemyStorageCatalogueMenu(1,p.getInventory());
        h.assertTrue(!m.slots.get(0).getItem().isEmpty(),"Server catalogue is actually shown");
        for(var input:ContainerInput.values()){RateGate.forget(p.getUUID());m.clicked(0,0,input,p);}
        h.assertTrue(m.getCarried().isEmpty()&&m.quickMoveStack(p,0).isEmpty()&&p.getInventory().getItem(1).isEmpty(),"All click variants leave display samples read-only");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.assertTrue(!m.stillValid(p),"Tome must still be held");h.succeed();
    }
    @GameTest(maxTicks=20) public void tabletAndDensityGemOpenTheirAuthorizedMenus(GameTestHelper h){
        var p=player(h);for(String name:List.of("transmutation_tablet","gem_of_eternal_density")){
            use(p,item(name));h.assertTrue(name.equals("transmutation_tablet")?p.containerMenu instanceof AlchemyPatternMenu:p.containerMenu instanceof AlchemyAutomationMenu,"Tablet opens paid conversion; Density opens explicit filters");var m=p.containerMenu;
            h.assertTrue(m.stillValid(p),"Own held instrument grants access");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);h.assertTrue(!m.stillValid(p),"Cannot use after dropping instrument");p.closeContainer();}
        h.succeed();
    }
    @GameTest(maxTicks=20) public void voidRingRequiresSecondUseAndPreservesModifiedItems(GameTestHelper h){
        var p=player(h);var ring=item("void_ring");p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COAL,4));Assaying.setBalance(p,0);use(p,ring);
        h.assertTrue(p.getOffhandItem().getCount()==4&&Assaying.balance(p)==0,"First use previews without consumption");use(p,ring);
        h.assertTrue(p.getOffhandItem().isEmpty()&&Assaying.balance(p)==512,"Second use conserves existing coal value");
        var named=new ItemStack(Items.COAL,4);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));p.setItemInHand(InteractionHand.OFF_HAND,named);use(p,ring);use(p,ring);
        h.assertTrue(named.getCount()==4&&Assaying.balance(p)==512,"Named items never dissolve");h.succeed();
    }
}
