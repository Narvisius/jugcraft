package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

/** Universal prices and real transaction/ownership boundaries, including the former default-diamond regression. */
public class AlchemyPlayabilityGameTests {
    private static ServerPlayer player(GameTestHelper h){return ConnectedAlchemyGameTests.player(h);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(id)));}
    private static void press(net.minecraft.world.inventory.AbstractContainerMenu m,ServerPlayer p,int action){RateGate.forget(p.getUUID());m.clickMenuButton(p,action);}
    private static AlchemyPatternMenu tablet(ServerPlayer p){return new AlchemyPatternMenu(1,p.getInventory(),()->true,ItemStack.EMPTY);}
    private static ServerPlayer fresh(GameTestHelper h){
        // Vanilla's in-level mock always reports Creative; use a real player for mode-sensitive checks.
        var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"fresh-alchemy-player");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        h.assertTrue(!p.isCreative()&&!p.isSpectator()&&!Assaying.knows(p),"Fixture is an ordinary survival player without research");return p;
    }
    @GameTest(maxTicks=20)public void freshPlayerDissolvesLearnsAndWithdrawsWithoutResearch(GameTestHelper h){
        var p=fresh(h);var m=tablet(p);m.getSlot(0).set(new ItemStack(Items.OAK_LOG));
        h.assertTrue(m.stillValid(p),"Tablet opens immediately");press(m,p,0);press(m,p,0);
        h.assertTrue(Assaying.balance(p)==32&&AlchemyPatterns.known(p).contains("minecraft:oak_log"),"Dissolution still consumes and teaches the pattern");
        press(m,p,1);h.assertTrue(m.getSlot(1).getItem().is(Items.OAK_LOG)&&Assaying.balance(p)==0,"Withdrawal pays full price without a research gate");
        h.assertTrue(!Assaying.knows(p),"Using equipment does not silently grant unrelated research");h.succeed();
    }
    @GameTest(maxTicks=20)public void eyeHiddenTransactionSlotsNeverSwallowShiftClickedItems(GameTestHelper h){
        var p=fresh(h);p.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,12));
        var menu=new AlchemyPatternMenu(1,p.getInventory(),()->true,item("mercurial_eye"));
        h.assertTrue(menu.eye()&&!menu.getSlot(0).isActive()&&!menu.getSlot(1).isActive(),"Eye has no transaction sockets");
        menu.quickMoveStack(p,2);
        h.assertTrue(menu.getSlot(0).getItem().isEmpty()&&p.getInventory().getItem(9).getCount()==12,"Shift-click leaves real items visible in the inventory");
        h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.GOLD_INGOT)),"Hidden socket cannot accept direct placement either");h.succeed();
    }
    @GameTest(maxTicks=20)public void freshPlayerCanEquipTradedCharms(GameTestHelper h){
        var p=fresh(h);
        for(var i:List.of(AlchemicalGear.TALISMAN,AlchemicalGear.BAND,ElementalAmulets.EVERTIDE,ElementalAmulets.VOLCANITE,AlchemyArtifacts.ITEMS.get("harvest_goddess_band"))){
            h.assertTrue(((eu.pb4.trinkets.api.callback.TrinketCallback)i).canEquip(new ItemStack(i),null,p),"Traded charm is equippable: "+i);
        }h.succeed();
    }
    @GameTest(maxTicks=20)public void equippedGemHelmetNeedsNoResearchOrHeldItemActivation(GameTestHelper h){
        var p=fresh(h);var helmet=item("gem_helmet");p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,helmet);Assaying.setBalance(p,20);
        h.assertTrue(GemAbilities.activate(p,0)&&helmet.has(AlchemicalGear.ACTIVE),"Explicit control enables the actual equipped item");
        GemAbilities.pulse(p);h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)&&Assaying.balance(p)==16,"Ability still pays its normal essence cost");
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,ItemStack.EMPTY);RateGate.forget(p.getUUID());
        h.assertTrue(!GemAbilities.activate(p,0),"No owned equipped item means no ability");h.succeed();
    }
    @GameTest(maxTicks=20)public void oldUnresearchedFactoryMigratesOfflineWithoutLosingContents(GameTestHelper h){
        var p=fresh(h);var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemicalWorkshops.BLOCKS.get("condenser_mk1"));
        var be=h.getBlockEntity(pos,AlchemicalWorkshopBlockEntity.class);be.placedBy(p);be.setEssence(512);be.setItem(1,new ItemStack(Items.OAK_LOG));
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());saved.putBoolean("factory_authorized",false);
        be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.getLevel().getServer().getPlayerList().remove(p);be.pulse(null);
        h.assertTrue(be.getItem(5).is(Items.OAK_LOG)&&be.essence()==480,"Legacy false research flag no longer locks a legitimately owned machine");
        try(var claims=TestClaims.open()){claims.block(be.getBlockPos(),UUID.randomUUID());be.pulse(null);h.assertTrue(be.essence()==480,"Migration preserves claim boundaries");}h.succeed();
    }
    @GameTest(maxTicks=20)public void unresearchedScaleWorksButSpectatorsCannotDissolve(GameTestHelper h){
        var p=fresh(h);var iron=new ItemStack(Items.IRON_INGOT);
        Assaying.use(p,p.level(),iron,ItemStack.EMPTY);Assaying.use(p,p.level(),iron,ItemStack.EMPTY);
        h.assertTrue(iron.isEmpty()&&Assaying.balance(p)==256,"Scale is usable without research");
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);h.assertTrue(p.isSpectator(),"Fixture entered spectator mode");var gold=new ItemStack(Items.GOLD_INGOT);
        Assaying.use(p,p.level(),gold,ItemStack.EMPTY);Assaying.use(p,p.level(),gold,ItemStack.EMPTY);
        h.assertTrue(!gold.isEmpty()&&Assaying.balance(p)==256&&!AlchemicalGear.ready(p),"Spectator state still forbids equipment transactions");h.succeed();
    }
    @GameTest(maxTicks=20)public void everyRegisteredDefaultItemHasPositiveValue(GameTestHelper h){
        int checked=0;for(var item:BuiltInRegistries.ITEM){if(item==Items.AIR)continue;var m=AlchemicalWorkshopBlockEntity.material(new ItemStack(item));
            h.assertTrue(m!=null&&m.formable()&&m.dissolvable()&&Assay.form(m,1)>0,"Positive default-item value: "+BuiltInRegistries.ITEM.getKey(item));checked++;}
        h.assertTrue(checked>3000,"Full registry, not the historical 45-item whitelist");h.succeed();
    }
    @GameTest(maxTicks=20)public void familiarValuesAndNoFormationSurcharge(GameTestHelper h){
        var expected=Map.of(Items.DIAMOND,8192L,Items.IRON_INGOT,256L,Items.GOLD_INGOT,2048L,Items.COAL,128L,Items.OAK_LOG,32L,Items.OAK_PLANKS,8L);
        expected.forEach((item,value)->{var m=AlchemicalWorkshopBlockEntity.material(new ItemStack(item));h.assertTrue(Assay.form(m,1)==value&&Assay.dissolve(m,1)==value,"Reference value and no surcharge: "+item);});h.succeed();
    }
    @GameTest(maxTicks=20)public void dissolveIsRequiredAndConfirmationBindsExactStack(GameTestHelper h){
        var p=player(h);var m=tablet(p);m.getSlot(0).set(new ItemStack(Items.DIAMOND,2));press(m,p,0);
        h.assertTrue(AlchemyPatterns.known(p).isEmpty()&&m.confirming(),"Preview cannot teach a retained sample");
        m.getSlot(0).getItem().shrink(1);press(m,p,0);h.assertTrue(AlchemyPatterns.known(p).isEmpty()&&m.getSlot(0).getItem().getCount()==1,"Changed input needs a fresh confirmation");
        press(m,p,0);h.assertTrue(AlchemyPatterns.known(p).contains("minecraft:diamond")&&!m.getSlot(0).hasItem()&&Assaying.balance(p)==18192,"Consume exactly once and learn");h.succeed();
    }
    @GameTest(maxTicks=20)public void learnedBulkWithdrawalPaysWalletThenStars(GameTestHelper h){
        var p=player(h);var m=tablet(p);m.getSlot(0).set(new ItemStack(Items.OAK_PLANKS));press(m,p,0);press(m,p,0);
        Assaying.setBalance(p,4);var star=item("klein_star_6");KleinStars.setStored(star,155);p.getInventory().setItem(4,star);
        press(m,p,7);press(m,p,1);h.assertTrue(m.getSlot(1).getItem().getCount()==16&&Assaying.balance(p)==0&&KleinStars.stored(star)==31,"16 planks cost exactly 128 across wallet and star");
        m.getSlot(1).set(ItemStack.EMPTY);press(m,p,9);press(m,p,1);h.assertTrue(m.getSlot(1).getItem().getCount()==3&&KleinStars.stored(star)==7,"Max creates only affordable items");h.succeed();
    }
    @GameTest(maxTicks=20)public void learnedBulkFitsOutputWithoutSpillingOrOvercharging(GameTestHelper h){
        var p=player(h);var m=tablet(p);m.getSlot(0).set(new ItemStack(Items.OAK_PLANKS));press(m,p,0);press(m,p,0);m.getSlot(1).set(new ItemStack(Items.OAK_PLANKS,63));
        long before=AlchemyPower.available(p);press(m,p,8);press(m,p,1);h.assertTrue(m.getSlot(1).getItem().getCount()==64&&AlchemyPower.available(p)==before-8,"Only the one output space is purchased");
        press(m,p,1);h.assertTrue(AlchemyPower.available(p)==before-8,"Full output cannot debit again");h.succeed();
    }
    @GameTest(maxTicks=20)public void storedContentsAndChargeCannotBeDestroyedByDissolving(GameTestHelper h){
        var p=player(h);var m=tablet(p);var bag=item("white_alchemical_bag");bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,64))));
        m.getSlot(0).set(bag);press(m,p,0);press(m,p,0);h.assertTrue(m.inputStatus()==2&&m.getSlot(0).getItem()==bag&&AlchemyPatterns.known(p).isEmpty(),"Filled bag retained with explicit reason");
        var star=item("klein_star_1");KleinStars.setStored(star,100);m.getSlot(0).set(star);press(m,p,0);h.assertTrue(!m.canDissolve()&&KleinStars.stored(star)==100,"Charged star never silently loses stored essence");h.succeed();
    }
    @GameTest(maxTicks=20)public void buildingDensityDefaultWorksAndRespectsReserve(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,1048);var gem=item("gem_of_eternal_density");gem.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);gem.set(AlchemyAutomation.FILTER,List.of("minecraft:stone"));gem.set(AlchemyAutomation.RESERVE,1024);gem.set(AlchemyAutomation.BUDGET,4);
        var c=new SimpleContainer(4);c.setItem(0,gem);c.setItem(1,new ItemStack(Items.STONE,16));
        AlchemyAutomation.convert(p,c,gem,0,4);h.assertTrue(c.getItem(1).getCount()==12&&c.getItem(2).isEmpty(),"Only four inputs; reserve blocks early output");
        AlchemyAutomation.convert(p,c,gem,0,4);h.assertTrue(c.getItem(2).is(Items.OAK_LOG)&&Assaying.balance(p)==1024,"Default log output costs32 and preserves reserve");h.succeed();
    }
    @GameTest(maxTicks=20)public void densityCanSpendSavedEssenceWithoutNewInput(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,32);var gem=item("gem_of_eternal_density");gem.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);gem.set(AlchemyAutomation.FILTER,List.of("minecraft:stone"));var c=new SimpleContainer(3);c.setItem(0,gem);
        h.assertTrue(AlchemyAutomation.convert(p,c,gem,0,3)&&c.getItem(1).is(Items.OAK_LOG)&&Assaying.balance(p)==0,"Saved essence is usable on subsequent pulses");h.succeed();
    }
    @GameTest(maxTicks=20)public void densityLegacyValuablesPauseAndPickerOnlyOffersSupplies(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,20000);var gem=item("gem_of_eternal_density");gem.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);gem.set(AlchemyAutomation.FILTER,List.of("minecraft:stone"));
        var c=new SimpleContainer(4);c.setItem(0,gem);c.setItem(1,new ItemStack(Items.STONE,16));
        for(String target:List.of("minecraft:diamond","minecraft:gold_ingot","minecraft:iron_ingot","minecraft:diamond_block")){
            gem.set(AlchemyPatterns.TARGET,target);
            h.assertTrue(!AlchemyAutomation.convert(p,c,gem,0,4)&&Assaying.balance(p)==20000&&c.getItem(1).getCount()==16&&c.getItem(2).isEmpty(),"Valuable target does not consume feed or wallet: "+target);
            h.assertTrue(gem.get(AlchemyPatterns.TARGET).equals(target)&&gem.get(AlchemyAutomation.STATUS)==4,"Old target retained with blocked status");
        }
        p.getInventory().setItem(0,gem);var menu=new AlchemyAutomationMenu(1,p.getInventory(),gem,0);
        h.assertTrue(menu.target()==-1&&menu.outputPrice()==0&&menu.status()==4,"Menu honestly shows legacy target unavailable");
        for(int i=0;i<AlchemyAutomationMenu.TARGETS.size();i++){
            RateGate.forget(p.getUUID());menu.clickMenuButton(p,1);
            var selected=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(gem.get(AlchemyPatterns.TARGET))));
            h.assertTrue(TabletOutputs.allowed(selected)&&menu.outputPrice()>0,"Picker only selects approved supplies");
        }
        gem.set(AlchemyPatterns.TARGET,"minecraft:oak_log");
        h.assertTrue(AlchemyAutomation.convert(p,c,gem,0,4)&&c.countItem(Items.OAK_LOG)==1&&Assaying.balance(p)==19984,"Owner can resume and reuse the emptied feed slot at the exact price");h.succeed();
    }
    @GameTest(maxTicks=20)public void storageConsolidationPreservesIdentityAndContents(GameTestHelper h){
        var p=player(h);var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemyStorage.BLOCKS.get("alchemical_chest"));var be=h.getBlockEntity(pos,AlchemyStorageBlockEntity.class);be.placedBy(p);
        be.setItem(0,new ItemStack(Items.COAL,30));be.setItem(7,new ItemStack(Items.COAL,40));var named=new ItemStack(Items.COAL,5);named.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keep"));be.setItem(8,named);
        var m=new AlchemyStorageMenu(1,p.getInventory(),be);press(m,p,1);h.assertTrue(be.getItem(0).getCount()==64&&be.getItem(1).getCount()==6&&be.getItem(2).has(DataComponents.CUSTOM_NAME)&&be.getItem(2).getCount()==5,"Consolidates exact identities only");m.removed(p);h.succeed();
    }
    @GameTest(maxTicks=20)public void offlineFactoryUsesOwnBufferAndStillHonorsClaims(GameTestHelper h){
        var p=player(h);var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemicalWorkshops.BLOCKS.get("condenser_mk1"));var be=h.getBlockEntity(pos,AlchemicalWorkshopBlockEntity.class);be.placedBy(p);be.setItem(1,new ItemStack(Items.OAK_LOG));be.setEssence(1024);
        h.getLevel().getServer().getPlayerList().remove(p);be.pulse(null);
        h.assertTrue(be.getItem(5).is(Items.OAK_LOG)&&be.essence()==992,"Offline loaded condenser spends only its saved buffer");
        try(var claims=TestClaims.open()){claims.block(be.getBlockPos(),UUID.randomUUID());be.pulse(null);h.assertTrue(be.essence()==992&&be.getItem(5).getCount()==1,"Offline actor cannot bypass current claims");}h.succeed();
    }
    @GameTest(maxTicks=20)public void offlineAuthorizationSurvivesSaveAndReload(GameTestHelper h){
        var p=player(h);var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemyMachines.BLOCKS.get("red_matter_furnace"));var be=h.getBlockEntity(pos,AlchemyMachinesBlockEntity.class);be.placedBy(p);be.setEssence(64);be.setItem(1,new ItemStack(Items.RAW_IRON));
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.getLevel().getServer().getPlayerList().remove(p);be.pulse(null);h.assertTrue(be.getItem(4).is(Items.IRON_INGOT)&&be.essence()==48,"Persisted authorized furnace works offline without touching a wallet");h.succeed();
    }
    @GameTest(maxTicks=20)public void eyeAreaIsBoundedAndMenuOwned(GameTestHelper h){
        var p=player(h);var eye=item("mercurial_eye");p.setItemInHand(InteractionHand.MAIN_HAND,eye);var m=new AlchemyPatternMenu(1,p.getInventory(),()->p.getMainHandItem()==eye,eye);
        press(m,p,10);h.assertTrue(m.area()==5,"3 to 5 area");press(m,p,10);h.assertTrue(m.area()==1,"5 wraps to 1");
        var other=player(h);press(m,other,10);h.assertTrue(m.area()==1,"Foreign player cannot change eye settings");h.succeed();
    }
    @GameTest(maxTicks=20)public void fractionalItemCanBeLearnedWithoutInventingEssence(GameTestHelper h){
        var p=player(h);var m=tablet(p);var slab=new ItemStack(Items.STONE_SLAB);var material=AlchemicalWorkshopBlockEntity.material(slab);
        h.assertTrue(Assay.dissolve(material,1)==0&&Assay.form(material,1)==1,"Half-essence slab retains rational value");
        m.getSlot(0).set(slab);long before=Assaying.balance(p);press(m,p,0);press(m,p,0);
        h.assertTrue(!m.getSlot(0).hasItem()&&AlchemyPatterns.known(p).contains("minecraft:stone_slab")&&Assaying.balance(p)==before,"Actual low-value input can teach without creating a free essence unit");
        press(m,p,7);press(m,p,1);h.assertTrue(m.getSlot(1).getItem().getCount()==16&&Assaying.balance(p)==before-8,"Bulk transaction rounds once, without surcharge");h.succeed();
    }
    @GameTest(maxTicks=20)public void upgradedGearKeepsItsValueButCannotBeCondensedAfterReload(GameTestHelper h){
        var p=player(h);var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemicalWorkshops.BLOCKS.get("condenser_mk2"));
        var be=h.getBlockEntity(pos,AlchemicalWorkshopBlockEntity.class);be.placedBy(p);var target=item("gem_chestplate");
        long price=Assay.form(AlchemicalWorkshopBlockEntity.material(target),1);
        h.assertTrue(price>262144&&price<be.capacity(),"Smithing-based Gem gear is valuable and fits the expanded Mk II buffer");
        be.setItem(1,target);be.setEssence((int)price);
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());be.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        be.pulse(p);h.assertTrue(be.getItem(5).isEmpty()&&be.essence()==price&&be.getItem(1).is(target.getItem())&&be.status()==AlchemicalWorkshopBlockEntity.INVALID_SAMPLE,"Reload retains valuable gear sample and its whole buffer without manufacturing equipment");h.succeed();
    }
    @GameTest(maxTicks=20)public void valuableKnowledgeSurvivesButAllTabletRoutesRefuseWithdrawal(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,1000000);
        for(var item:List.of(Items.DIAMOND,Items.DIAMOND_BLOCK,Items.DIAMOND_ORE,Items.IRON_INGOT,Items.GOLD_BLOCK,Items.NETHERITE_INGOT,Items.DIAMOND_SWORD,Items.ENCHANTED_GOLDEN_APPLE)){
            var stack=new ItemStack(item);AlchemyPatterns.learn(p,stack);AlchemyPatterns.favorite(p,BuiltInRegistries.ITEM.getKey(item).toString());
            var menu=tablet(p);menu.getSlot(0).set(stack);press(menu,p,0);press(menu,p,0);
            long saved=Assaying.balance(p);press(menu,p,1);
            h.assertTrue(!menu.canMake()&&menu.getSlot(1).getItem().isEmpty()&&Assaying.balance(p)==saved,"Learned valuable cannot be withdrawn: "+item);
            for(int n=0;n<AlchemyPatternMenu.PAGE_SIZE;n++)h.assertTrue(menu.getSlot(AlchemyPatternMenu.GHOST_START+n).getItem().isEmpty(),"Valuables hidden from learned menu");
            h.assertTrue(AlchemyPatterns.known(p).contains(BuiltInRegistries.ITEM.getKey(item).toString()),"Saved knowledge retained");
            var legacy=new TransmutationMenu(2,p.getInventory());legacy.getSlot(1).set(new ItemStack(item));press(legacy,p,1);
            h.assertTrue(!legacy.canForm()&&legacy.getSlot(2).getItem().isEmpty()&&!Assaying.form(p,new ItemStack(item))&&Assaying.balance(p)==saved,"Legacy menu and scale cannot bypass policy");
        }
        h.assertTrue(!AlchemyPatterns.building(new ItemStack(Items.DIAMOND_BLOCK)),"Building eye cannot select valuable storage blocks");h.succeed();
    }
    @GameTest(maxTicks=20)public void convenienceCatalogueAllowsTorchesAndBuildingButNotUnknownContent(GameTestHelper h){
        var p=player(h);
        for(var item:List.of(Items.TORCH,Items.COBBLESTONE,Items.OAK_PLANKS,Items.GLASS,Items.STONE_BRICKS,Items.LADDER)){
            var menu=tablet(p);menu.getSlot(0).set(new ItemStack(item));press(menu,p,0);press(menu,p,0);
            long saved=Assaying.balance(p);long price=Assay.form(AlchemicalWorkshopBlockEntity.material(new ItemStack(item)),1);
            press(menu,p,1);h.assertTrue(menu.getSlot(1).getItem().is(item)&&Assaying.balance(p)==saved-price,"Permitted withdrawal costs its normal value: "+item);
        }
        h.assertFalse(TabletOutputs.allowed(item("klein_star_1")),"Modded equipment defaults to denied");h.succeed();
    }

}
