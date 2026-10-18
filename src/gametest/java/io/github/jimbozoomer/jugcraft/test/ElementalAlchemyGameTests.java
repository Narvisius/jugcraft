package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

public class ElementalAlchemyGameTests {
    private static Item star(int tier){return KleinStars.ITEMS.get("klein_star_"+tier);}
    private static ServerPlayer player(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);
        var at=h.absolutePos(new BlockPos(3,2,3));p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);
        ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);Assaying.setBalance(p,100000);return p;
    }
    private static void press(KleinStarMenu m,ServerPlayer p,int id){RateGate.forget(p.getUUID());m.clickMenuButton(p,id);}
    private static KleinStarMenu menu(ServerPlayer p,int tier){var m=new KleinStarMenu(1,p.getInventory());m.getSlot(0).set(new ItemStack(star(tier)));m.broadcastChanges();return m;}
    private static ItemStack active(Item i){var s=new ItemStack(i);s.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);return s;}
    @GameTest(maxTicks=20) public void transfersConserveAndCloseReturnsTheRealStar(GameTestHelper h){
        var p=player(h);var m=menu(p,1);press(m,p,0);
        h.assertTrue(m.stored()==1024&&Assaying.balance(p)==98976,"Store debits ledger once");
        press(m,p,2);h.assertTrue(m.stored()==4096&&Assaying.balance(p)==95904&&!m.canStore(),"All stops at capacity");
        press(m,p,4);h.assertTrue(m.stored()==0&&Assaying.balance(p)==100000&&!m.canWithdraw(),"Take clamps to actual charge");
        press(m,p,0);m.removed(p);m.removed(p);
        h.assertTrue(p.getInventory().countItem(star(1))==1&&m.getSlot(0).getItem().isEmpty(),"Repeated close cannot duplicate star");
        int charge=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(star(1)))charge+=KleinStars.stored(p.getInventory().getItem(i));
        h.assertTrue(charge==1024,"Returned stack retains charge in whichever inventory slot received it");h.succeed();
    }
    @GameTest(maxTicks=20) public void largeChargesAndLedgerCapStayExact(GameTestHelper h){
        var p=player(h);var m=menu(p,6);KleinStars.setStored(m.getSlot(0).getItem(),4194304);Assaying.setBalance(p,io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE-1);m.broadcastChanges();
        h.assertTrue(m.stored()==4194304&&m.capacity()==4194304&&m.balance()==io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE-1,"Large values survive menu data");
        press(m,p,5);h.assertTrue(m.stored()==4194303&&Assaying.balance(p)==io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE&&!m.canWithdraw(),"Only ledger room is withdrawn");
        press(m,p,2);h.assertTrue(m.stored()==4194304&&Assaying.balance(p)==io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE-1,"Only star room is deposited");h.succeed();
    }
    @GameTest(maxTicks=20) public void foreignClicksInvalidItemsAndRapidClicksAreRefused(GameTestHelper h){
        var p=player(h);var m=menu(p,1);var other=player(h);press(m,other,2);press(m,p,999);
        h.assertTrue(m.stored()==0&&Assaying.balance(p)==100000,"Invalid callers cannot transfer");
        press(m,p,0);m.clickMenuButton(p,0);h.assertTrue(m.stored()==1024,"Duplicate same-tick click throttled");
        m.getSlot(0).set(new ItemStack(Items.DIAMOND));press(m,p,2);
        h.assertTrue(Assaying.balance(p)==98976&&!m.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)),"Other items cannot store essence");h.succeed();
    }
    @GameTest(maxTicks=20) public void starCanBeShiftMovedWithoutDuplicatingCharge(GameTestHelper h){
        var p=player(h);var m=menu(p,2);press(m,p,1);m.quickMoveStack(p,0);m.broadcastChanges();
        h.assertTrue(m.getSlot(0).getItem().isEmpty()&&!m.canWithdraw()&&p.getInventory().countItem(star(2))==1,"Quick move returns one star");
        press(m,p,5);h.assertTrue(Assaying.balance(p)==83616,"Empty menu cannot withdraw old preview charge");h.succeed();
    }
    @GameTest(maxTicks=20) public void allSmithingUpgradesPreserveChargeAndName(GameTestHelper h){
        ItemStack s=new ItemStack(star(1));KleinStars.setStored(s,3456);s.set(DataComponents.CUSTOM_NAME,Component.literal("Family reserve"));
        for(int tier=2;tier<=6;tier++){
            var input=new SmithingRecipeInput(new ItemStack(DarkMatter.ITEMS.get("high_covalence_dust")),s,new ItemStack(DarkMatter.ITEMS.get("dark_matter")));
            var recipe=h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING,input,h.getLevel()).orElseThrow();
            s=recipe.value().assemble(input);
            h.assertTrue(s.is(star(tier))&&KleinStars.stored(s)==3456&&s.getHoverName().getString().equals("Family reserve"),"Smithing preserves charge and name, tier "+tier);
        }
        var ops=h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var read=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,s).getOrThrow()).getOrThrow();
        h.assertTrue(read.is(star(6))&&KleinStars.stored(read)==3456,"Charge persists through save/load");h.succeed();
    }
    @GameTest(maxTicks=20) public void initialStarAndAmuletRecipesLoadAndReturnBuckets(GameTestHelper h){
        Item gold=Items.GOLD_INGOT,dark=DarkMatter.ITEMS.get("dark_matter"),dust=DarkMatter.ITEMS.get("high_covalence_dust");
        Item[][] grids={{gold,Items.LAPIS_LAZULI,gold,Items.LAPIS_LAZULI,dark,Items.LAPIS_LAZULI,gold,Items.LAPIS_LAZULI,gold},
                {gold,dark,gold,Items.WATER_BUCKET,dust,Items.WATER_BUCKET,gold,dark,gold},
                {gold,dark,gold,Items.LAVA_BUCKET,dust,Items.LAVA_BUCKET,gold,dark,gold}};
        Item[] outputs={star(1),ElementalAmulets.EVERTIDE,ElementalAmulets.VOLCANITE};
        for(int n=0;n<3;n++){
            var input=CraftingInput.of(3,3,Arrays.stream(grids[n]).map(ItemStack::new).toList());
            var recipe=h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value();
            h.assertTrue(recipe.assemble(input).is(outputs[n]),"Crafting output "+n);
            if(n>0)h.assertTrue(recipe.getRemainingItems(input).stream().filter(s->s.is(Items.BUCKET)).count()==2,"Two empty buckets returned");
        }h.succeed();
    }
    @GameTest(maxTicks=20) public void bucketSupplyPaysExactCostsAndSupportsEitherHand(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ElementalAmulets.EVERTIDE));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET));
        h.assertTrue(ElementalAmulets.fillBucket(p,InteractionHand.MAIN_HAND,true)&&p.getOffhandItem().is(Items.WATER_BUCKET)&&Assaying.balance(p)==99936,"Water replaces only its bucket");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ElementalAmulets.VOLCANITE));p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
        h.assertTrue(ElementalAmulets.fillBucket(p,InteractionHand.OFF_HAND,false)&&p.getMainHandItem().is(Items.LAVA_BUCKET)&&Assaying.balance(p)==95840,"Offhand lava pays 4096");h.succeed();
    }
    @GameTest(maxTicks=20) public void stackedBucketsRequireSpaceBeforeCharging(GameTestHelper h){
        var p=player(h);for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ElementalAmulets.EVERTIDE));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET,3));
        h.assertTrue(!ElementalAmulets.fillBucket(p,InteractionHand.MAIN_HAND,true)&&Assaying.balance(p)==100000&&p.getOffhandItem().getCount()==3,"Full inventory consumes nothing");
        p.getInventory().setItem(12,ItemStack.EMPTY);
        h.assertTrue(ElementalAmulets.fillBucket(p,InteractionHand.MAIN_HAND,true)&&p.getOffhandItem().getCount()==2&&p.getInventory().getItem(12).is(Items.WATER_BUCKET),"One output, two remaining buckets");h.succeed();
    }
    @GameTest(maxTicks=20) public void scarceEssenceAndModifiedBucketsArePreserved(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ElementalAmulets.VOLCANITE));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET));Assaying.setBalance(p,4095);
        h.assertTrue(!ElementalAmulets.fillBucket(p,InteractionHand.MAIN_HAND,false)&&Assaying.balance(p)==4095,"No overdraft");
        Assaying.setBalance(p,100000);p.getOffhandItem().set(DataComponents.CUSTOM_NAME,Component.literal("Do not replace"));
        h.assertTrue(!ElementalAmulets.fillBucket(p,InteractionHand.MAIN_HAND,false)&&Assaying.balance(p)==100000,"Named buckets preserved");h.succeed();
    }
    @GameTest(maxTicks=20) public void duplicateFireWardsPayOnceAndLeaveLongPotionsAlone(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,active(ElementalAmulets.VOLCANITE));p.setItemInHand(InteractionHand.OFF_HAND,active(ElementalAmulets.VOLCANITE));
        ElementalAmulets.pulse(p);h.assertTrue(Assaying.balance(p)==100000,"Safe ground is free");
        p.igniteForTicks(200);ElementalAmulets.pulse(p);
        h.assertTrue(p.hasEffect(MobEffects.FIRE_RESISTANCE)&&Assaying.balance(p)==99992,"Two amulets pay one ward");
        ElementalAmulets.pulse(p);h.assertTrue(Assaying.balance(p)==99992,"Already-covered ticks free");
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,1200,0));ElementalAmulets.pulse(p);
        h.assertTrue(p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration()==1200&&Assaying.balance(p)==99992,"Long potion preserved");h.succeed();
    }
    @GameTest(maxTicks=20) public void charmSlotWorksAndEmptyLedgerStopsWards(GameTestHelper h){
        var p=player(h);var inv=eu.pb4.trinkets.api.TrinketsApi.getAttachment(p).getInventory("legs/charm");
        inv.setItem(0,active(ElementalAmulets.VOLCANITE));p.igniteForTicks(200);Assaying.setBalance(p,7);ElementalAmulets.pulse(p);
        h.assertTrue(!p.hasEffect(MobEffects.FIRE_RESISTANCE)&&Assaying.balance(p)==7,"No ward on insufficient funds");
        Assaying.setBalance(p,8);ElementalAmulets.pulse(p);h.assertTrue(p.hasEffect(MobEffects.FIRE_RESISTANCE)&&Assaying.balance(p)==0,"Worn charm spends exact last cost");h.succeed();
    }
    @GameTest(maxTicks=40) public void evertideWardRequiresSubmergedEyes(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,active(ElementalAmulets.EVERTIDE));ElementalAmulets.pulse(p);
        h.assertTrue(!p.hasEffect(MobEffects.WATER_BREATHING)&&Assaying.balance(p)==100000,"Dry land doesn't charge");
        var at=p.blockPosition();p.level().setBlockAndUpdate(at,Blocks.WATER.defaultBlockState());p.level().setBlockAndUpdate(at.above(),Blocks.WATER.defaultBlockState());
        // Advance the movement/entity tick normally driven by client packets, not just ServerPlayer.tick.
        p.doTick();p.doTick();
        h.assertTrue(p.isUnderWater(),"Fixture puts the player eyes below water");
        ElementalAmulets.pulse(p);
        h.assertTrue(p.isUnderWater()&&p.hasEffect(MobEffects.WATER_BREATHING)&&Assaying.balance(p)==99996,"Underwater ward costs four");h.succeed();
    }
    @GameTest(maxTicks=20) public void freshPlayersCanUseEquipmentButFeatureGateStillApplies(GameTestHelper h){
        var unknown=h.makeMockServerPlayerInLevel();unknown.setGameMode(GameType.SURVIVAL);Assaying.setBalance(unknown,100000);
        var m=menu(unknown,1);press(m,unknown,2);h.assertTrue(m.stored()==4096&&m.stillValid(unknown)&&Assaying.balance(unknown)==95904,"Fresh players can charge a star without research; payment remains exact");
        var p=player(h);m=menu(p,1);p.setItemInHand(InteractionHand.MAIN_HAND,active(ElementalAmulets.VOLCANITE));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.BUCKET));p.igniteForTicks(200);
        boolean before=JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
        try{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,false);press(m,p,2);ElementalAmulets.pulse(p);
            h.assertTrue(m.stored()==0&&!ElementalAmulets.fillBucket(p,InteractionHand.MAIN_HAND,false)&&!p.hasEffect(MobEffects.FIRE_RESISTANCE)&&Assaying.balance(p)==100000,"Feature gate prevents all new work");
        }finally{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,before);}h.succeed();
    }
    @GameTest(maxTicks=20) public void emptyStarsAndAmuletsCanBeTransmutedButChargeIsProtected(GameTestHelper h){
        var p=player(h);var items=new ArrayList<>(KleinStars.ITEMS.values());items.add(ElementalAmulets.EVERTIDE);items.add(ElementalAmulets.VOLCANITE);
        for(var item:items){var stack=new ItemStack(item);h.assertTrue(Assaying.eligibility(stack).isEmpty(),"Default magical equipment is valued");if(KleinStars.capacity(stack)>0){KleinStars.setStored(stack,123);
            h.assertTrue(!Assaying.eligibility(stack).isEmpty(),"Charged state remains protected");}}h.succeed();
    }
}
