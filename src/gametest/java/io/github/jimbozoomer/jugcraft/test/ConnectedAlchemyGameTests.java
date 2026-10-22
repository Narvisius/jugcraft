package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.mixin.AlchemyExperienceAccess;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public class ConnectedAlchemyGameTests {
    static ServerPlayer player(GameTestHelper h){var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"alchemy-connect");
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,net.minecraft.server.level.ClientInformation.createDefault());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false));p.setGameMode(GameType.SURVIVAL);
        h.assertTrue(!p.isCreative()&&!p.getAbilities().mayfly,"Actual survival player, not vanilla's permanently creative mock");p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);
        var at=h.absolutePos(new BlockPos(2,3,3));p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);Assaying.setBalance(p,10000);return p;}
    private static ItemStack item(String id){return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(id)));}
    private static ItemStack star(int amount){var s=item("klein_star_6");KleinStars.setStored(s,amount);return s;}
    private static boolean press(AlchemyPatternMenu m,ServerPlayer p,int id){RateGate.forget(p.getUUID());return m.clickMenuButton(p,id);}
    private static AlchemyMachinesBlockEntity pedestal(GameTestHelper h,ServerPlayer p,String artifact){var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemyMachines.BLOCKS.get("dark_matter_pedestal"));var be=h.getBlockEntity(pos,AlchemyMachinesBlockEntity.class);be.placedBy(p);be.setEssence(1000);be.setItem(1,item(artifact));return be;}
    private static AlchemicalWorkshopBlockEntity collector(GameTestHelper h,ServerPlayer p){var pos=new BlockPos(2,3,2);h.setBlock(pos,AlchemicalWorkshops.BLOCKS.get("collector_mk3"));h.setBlock(pos.above(),Blocks.STONE);var be=h.getBlockEntity(pos,AlchemicalWorkshopBlockEntity.class);be.placedBy(p);return be;}
    @GameTest(maxTicks=20)public void patternsDissolveLearnAndFormWithoutSample(GameTestHelper h){
        var p=player(h);var m=new AlchemyPatternMenu(1,p.getInventory(),()->true,ItemStack.EMPTY);m.getSlot(0).set(new ItemStack(Items.OAK_LOG));press(m,p,0);press(m,p,0);
        h.assertTrue(AlchemyPatterns.known(p).contains("minecraft:oak_log")&&!m.getSlot(0).hasItem(),"Dissolving consumes the sample and teaches its identity");
        m.getSlot(0).set(ItemStack.EMPTY);long balance=Assaying.balance(p);press(m,p,1);
        h.assertTrue(m.getSlot(1).getItem().is(Items.OAK_LOG)&&Assaying.balance(p)==balance-m.price(),"Known pattern forms without a sample at audited cost");h.succeed();
    }
    @GameTest(maxTicks=20)public void patternGhostsNeverBecomeInventory(GameTestHelper h){
        var p=player(h);AlchemyPatterns.learn(p,new ItemStack(Items.OAK_LOG));var m=new AlchemyPatternMenu(1,p.getInventory(),()->true,ItemStack.EMPTY);
        for(var input:ContainerInput.values()){RateGate.forget(p.getUUID());m.clicked(AlchemyPatternMenu.GHOST_START,0,input,p);}
        h.assertTrue(m.getCarried().isEmpty()&&m.quickMoveStack(p,AlchemyPatternMenu.GHOST_START).isEmpty(),"Every ghost click and shift move is read-only");m.removed(p);
        h.assertTrue(p.getInventory().isEmpty(),"Closing returns only real slots");h.succeed();
    }
    @GameTest(maxTicks=20)public void patternSearchFavoritesAndForeignPlayers(GameTestHelper h){
        var p=player(h);AlchemyPatterns.learn(p,new ItemStack(Items.OAK_LOG));AlchemyPatterns.learn(p,new ItemStack(Items.SPRUCE_LOG));AlchemyPatterns.favorite(p,"minecraft:oak_log");
        var m=new AlchemyPatternMenu(1,p.getInventory(),()->true,ItemStack.EMPTY);m.search("spruce");h.assertTrue(m.getSlot(38).getItem().is(Items.SPRUCE_LOG)&&m.getSlot(39).getItem().isEmpty(),"Search filters authoritative identities");
        m.search("");press(m,p,5);h.assertTrue(m.getSlot(38).getItem().is(Items.OAK_LOG),"Favorites contain only the saved selection");
        var other=player(h);h.assertTrue(!m.clickMenuButton(other,1)&&AlchemyPatterns.known(other).isEmpty(),"No shared knowledge or foreign menu mutation");h.succeed();
    }
    @GameTest(maxTicks=20)public void modifiedAndExcludedPatternsCannotBeLearned(GameTestHelper h){
        var p=player(h);var named=new ItemStack(Items.OAK_LOG);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keepsake"));
        h.assertTrue(!AlchemyPatterns.learn(p,named)&&AlchemyPatterns.learn(p,item("klein_star_1"))&&!AlchemyPatterns.known(p).contains("minecraft:oak_log"),"Patched matter is refused; an empty default star is learnable");h.succeed();
    }
    @GameTest(maxTicks=20)public void fullPatternOutputAndLostAccessDoNotSpend(GameTestHelper h){
        var p=player(h);boolean[] access={true};var m=new AlchemyPatternMenu(1,p.getInventory(),()->access[0],ItemStack.EMPTY);
        m.getSlot(0).set(new ItemStack(Items.OAK_LOG));press(m,p,0);press(m,p,0);m.getSlot(1).set(new ItemStack(Items.OAK_LOG,64));long before=Assaying.balance(p);press(m,p,1);
        h.assertTrue(Assaying.balance(p)==before,"Full output refuses payment");access[0]=false;m.getSlot(1).set(ItemStack.EMPTY);h.assertTrue(!press(m,p,1)&&Assaying.balance(p)==before,"Walking away closes authority");h.succeed();
    }
    @GameTest(maxTicks=20)public void carriedStarsPayAndRefundConservatively(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,3);p.getInventory().setItem(5,star(10));p.getInventory().setItem(6,star(20));
        h.assertTrue(AlchemyPower.pay(p,18)&&Assaying.balance(p)==0&&KleinStars.stored(p.getInventory().getItem(5))==0&&KleinStars.stored(p.getInventory().getItem(6))==15,"Wallet then physical stars in order");
        long before=AlchemyPower.available(p);h.assertTrue(!AlchemyPower.pay(p,16)&&AlchemyPower.available(p)==before,"Insufficient total cannot partially debit");AlchemyPower.refund(p,18);h.assertTrue(AlchemyPower.available(p)==33,"Failed paid action refunds exact total");h.succeed();
    }
    @GameTest(maxTicks=20)public void nestedStarsAreNotEquipmentPower(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,0);var bag=item("white_alchemical_bag");bag.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(star(1000))));p.getInventory().setItem(0,bag);
        h.assertTrue(AlchemyPower.available(p)==0&&!AlchemyPower.pay(p,1),"No hidden nested-container withdrawal");h.succeed();
    }
    @GameTest(maxTicks=20)public void automaticVoidNeedsFilterAndProtectsPatchedStacks(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,0);var a=item("void_ring");a.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);var c=new SimpleContainer(5);c.setItem(0,a);c.setItem(1,new ItemStack(Items.COAL,32));
        h.assertTrue(!AlchemyAutomation.convert(p,c,a,0,5)&&c.getItem(1).getCount()==32,"Empty filter is inert");a.set(AlchemyAutomation.FILTER,List.of("minecraft:coal"));
        var named=new ItemStack(Items.COAL,8);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keepsake"));c.setItem(2,named);
        h.assertTrue(AlchemyAutomation.convert(p,c,a,0,5)&&c.getItem(1).getCount()==16&&c.getItem(2).getCount()==8,"Bounded plain feed only");
        long expected=Assay.dissolve(AlchemicalWorkshopBlockEntity.material(new ItemStack(Items.COAL)),16);h.assertTrue(Assaying.balance(p)==expected,"Exact existing ledger value");h.succeed();
    }
    @GameTest(maxTicks=20)public void densityBlocksOnFullOutputAndMakesSelectedTier(GameTestHelper h){
        var p=player(h);var a=item("gem_of_eternal_density");a.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);a.set(AlchemyAutomation.FILTER,List.of("minecraft:stone"));a.set(AlchemyPatterns.TARGET,"minecraft:oak_log");
        var c=new SimpleContainer(3);c.setItem(0,a);c.setItem(1,new ItemStack(Items.STONE,32));c.setItem(2,new ItemStack(Items.GLASS,64));long before=Assaying.balance(p);
        h.assertTrue(!AlchemyAutomation.convert(p,c,a,0,3)&&Assaying.balance(p)==before&&c.getItem(1).getCount()==32,"No free output space means no input consumed");c.setItem(2,ItemStack.EMPTY);
        h.assertTrue(AlchemyAutomation.convert(p,c,a,0,3)&&c.getItem(2).is(Items.OAK_LOG),"Consolidates into chosen output");h.succeed();
    }
    @GameTest(maxTicks=20)public void storageRepairUsesStarAndRetainsComponents(GameTestHelper h){
        var p=player(h);Assaying.setBalance(p,0);p.getInventory().setItem(6,star(10));var c=new SimpleContainer(4);var talisman=new ItemStack(AlchemicalGear.TALISMAN);talisman.set(AlchemicalGear.ACTIVE,Unit.INSTANCE);c.setItem(0,talisman);
        var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(20);tool.set(DataComponents.CUSTOM_NAME,Component.literal("Favorite"));c.setItem(1,tool);
        h.assertTrue(AlchemyAutomation.pulse(p,c,p.position())&&tool.getDamageValue()==16&&tool.has(DataComponents.CUSTOM_NAME)&&KleinStars.stored(p.getInventory().getItem(6))==6,"Repair preserves state and pays four points");h.succeed();
    }
    private static int fuelPrice(int tier){return (int)(Assay.form(Assaying.catalog().material(AlchemicalWorkshopBlockEntity.FUELS[tier+1]),1)-Assay.dissolve(Assaying.catalog().material(AlchemicalWorkshopBlockEntity.FUELS[tier]),1));}
    @GameTest(maxTicks=20)public void collectorFuelStopsAtChosenTier(GameTestHelper h){
        var p=player(h);var be=collector(h,p);be.setFuelTarget(1);be.setItem(2,new ItemStack(Items.COAL,2));
        // A full buffer isolates the upgrade debit from asynchronous skylight updates in a freshly placed test roof.
        be.setEssence(be.capacity());be.pulse(p);
        h.assertTrue(be.getItem(2).getCount()==1&&be.getItem(5).is(DarkMatter.ITEMS.get("alchemical_coal"))&&be.essence()==be.capacity()-fuelPrice(0),"One fuel step, exact value difference");
        be.setEssence(be.capacity());be.pulse(p);h.assertTrue(be.getItem(5).getCount()==2&&be.essence()==be.capacity()-fuelPrice(0),"Second fuel has the same exact cost");
        be.setEssence(be.capacity());be.pulse(p);h.assertTrue(be.getItem(5).getCount()==2&&be.essence()==be.capacity(),"Selected final tier never upgraded even with a full buffer");h.succeed();
    }
    @GameTest(maxTicks=20)public void collectorFuelProgressesAndRespectsPause(GameTestHelper h){
        var p=player(h);var be=collector(h,p);be.setFuelTarget(3);be.setItem(2,new ItemStack(Items.COAL));int spent=0;
        for(int price:new int[]{fuelPrice(0),fuelPrice(1),fuelPrice(2)}){be.setEssence(be.capacity());be.pulse(p);int debit=be.capacity()-be.essence();h.assertTrue(debit==price,"Each stage charges its exact upgrade price");spent+=debit;}
        int count=0;for(int i=5;i<11;i++)if(be.getItem(i).is(DarkMatter.ITEMS.get("aeternalis_fuel")))count+=be.getItem(i).getCount();
        h.assertTrue(count==1&&spent==fuelPrice(0)+fuelPrice(1)+fuelPrice(2),"One coal reaches the selected final fuel in exactly three paid steps");be.setPaused(true);be.setItem(2,new ItemStack(Items.COAL));int before=be.essence();be.pulse(p);h.assertTrue(be.getItem(2).getCount()==1&&be.essence()==before,"Pause preserves fuel and buffer");h.succeed();
    }
    @GameTest(maxTicks=20)public void collectorSavesTargetAndAllInventorySlots(GameTestHelper h){
        var p=player(h);var be=collector(h,p);be.setFuelTarget(2);be.setItem(10,item("mobius_fuel"));
        var tag=be.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new AlchemicalWorkshopBlockEntity(be.getBlockPos(),be.getBlockState());
        restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
        h.assertTrue(restored.fuelTarget()==2&&restored.getContainerSize()==11&&restored.getItem(10).is(DarkMatter.ITEMS.get("mobius_fuel")),"Old slot zero expands compatibly; target and output persist");h.succeed();
    }
    @GameTest(maxTicks=20)public void portableCraftingReturnsIngredientsOnClose(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AlchemicalGear.STONE));PhilosopherTransforms.crafting(p);
        h.assertTrue(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,"Real portable crafting menu");p.containerMenu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS,4));p.closeContainer();
        h.assertTrue(p.getInventory().countItem(Items.OAK_PLANKS)==4,"Closing returns every real crafting input");h.succeed();
    }
    @GameTest(maxTicks=20)public void stoneTransformsRespectClaimsAndPayment(GameTestHelper h){
        var p=player(h);var stone=new ItemStack(AlchemicalGear.STONE);p.setItemInHand(InteractionHand.MAIN_HAND,stone);var at=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());
        long before=AlchemyPower.available(p);try(var claims=TestClaims.open()){claims.block(at,UUID.randomUUID());h.assertTrue(PhilosopherTransforms.transform(p,stone,at,Direction.UP)==0&&AlchemyPower.available(p)==before,"Protected terrain untouched");}
        h.assertTrue(PhilosopherTransforms.transform(p,stone,at,Direction.UP)==1&&h.getLevel().getBlockState(at).is(Blocks.COBBLESTONE)&&AlchemyPower.available(p)<before,"Normal conversion paid");h.succeed();
    }
    @GameTest(maxTicks=20)public void mercurialBuildingNeedsLearnedPatternAndNeverConsumesEye(GameTestHelper h){
        var p=player(h);var eye=item("mercurial_eye");eye.set(AlchemyPatterns.TARGET,"minecraft:stone");p.setItemInHand(InteractionHand.MAIN_HAND,eye);var at=h.absolutePos(new BlockPos(2,1,2));h.getLevel().setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());
        var hit=new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false);
        h.assertTrue(MercurialBuilding.build(p,eye,hit)==0,"Forged unknown selection refused");AlchemyPatterns.learn(p,new ItemStack(Items.STONE));long before=AlchemyPower.available(p);
        int placed=MercurialBuilding.build(p,eye,hit);long each=Assay.form(AlchemicalWorkshopBlockEntity.material(new ItemStack(Items.STONE)),1)+8;
        h.assertTrue(placed>0&&placed<=9&&eye.getCount()==1&&AlchemyPower.available(p)==before-placed*each,"Native collision-safe placement paid for each success");h.succeed();
    }
    @GameTest(maxTicks=20)public void mindPedestalConservesMergedExperience(GameTestHelper h){
        var p=player(h);var be=pedestal(h,p,"mind_stone");var center=Vec3.atCenterOf(be.getBlockPos());var orb=new ExperienceOrb(p.level(),center.x+1,center.y+.5,center.z,7);((AlchemyExperienceAccess)orb).jugcraft$count(20);p.level().addFreshEntity(orb);
        be.pulse(p);int stored=AlchemyStorage.storedXp(be.getItem(1));int remaining=p.level().getEntitiesOfClass(ExperienceOrb.class,new AABB(be.getBlockPos()).inflate(6)).stream().filter(Entity::isAlive).mapToInt(e->e.getValue()*((AlchemyExperienceAccess)e).jugcraft$count()).sum();
        h.assertTrue(stored>0&&stored<=100&&stored+remaining==140&&be.getItem(1).has(AlchemicalGear.ACTIVE),"Merged orb multiplicity conserved and active icon set");h.succeed();
    }
    @GameTest(maxTicks=20)public void mindPedestalSplitsLargeOrbWithoutLosingExperience(GameTestHelper h){
        var p=player(h);var be=pedestal(h,p,"mind_stone");var center=Vec3.atCenterOf(be.getBlockPos());var orb=new ExperienceOrb(p.level(),center.x+1,center.y+.5,center.z,2477);((AlchemyExperienceAccess)orb).jugcraft$count(3);p.level().addFreshEntity(orb);
        be.pulse(p);int stored=AlchemyStorage.storedXp(be.getItem(1));int remaining=p.level().getEntitiesOfClass(ExperienceOrb.class,new AABB(be.getBlockPos()).inflate(6)).stream().filter(Entity::isAlive).mapToInt(e->e.getValue()*((AlchemyExperienceAccess)e).jugcraft$count()).sum();
        h.assertTrue(stored==100&&stored+remaining==7431,"Large merged XP orb splits exactly at the pulse budget");h.succeed();
    }
    @GameTest(maxTicks=20)public void pickupPedestalTransfersIntoAdjacentStorage(GameTestHelper h){
        var p=player(h);var be=pedestal(h,p,"black_hole_band");var chestPos=be.getBlockPos().east();p.level().setBlockAndUpdate(chestPos,Blocks.BARREL.defaultBlockState());
        var point=Vec3.atCenterOf(be.getBlockPos()).add(0,1,1);var drop=new ItemEntity(p.level(),point.x,point.y,point.z,new ItemStack(Items.IRON_INGOT,7));drop.setNoPickUpDelay();p.level().addFreshEntity(drop);be.pulse(p);
        var chest=(net.minecraft.world.Container)p.level().getBlockEntity(chestPos);h.assertTrue(!drop.isAlive()&&chest.getItem(0).getCount()==7&&be.essence()==999,"Transfer uses exact inserted count and one buffer essence");h.succeed();
    }
    @GameTest(maxTicks=20)public void harvestPedestalReplantsMatureCrops(GameTestHelper h){
        var p=player(h);var be=pedestal(h,p,"harvest_goddess_band");var at=be.getBlockPos().east().below();p.level().setBlockAndUpdate(at.below(),Blocks.FARMLAND.defaultBlockState());p.level().setBlockAndUpdate(at,Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));
        for(int i=0;i<3;i++)be.pulse(p);h.assertTrue(p.level().getBlockState(at).is(Blocks.WHEAT)&&p.level().getBlockState(at).getValue(CropBlock.AGE)<7&&be.essence()<1000,"Harvest replants and spends buffer");h.succeed();
    }
    @GameTest(maxTicks=20)public void gemFlightSharesLeaseAndStopsOnRemoval(GameTestHelper h){
        var p=player(h);p.setItemSlot(EquipmentSlot.FEET,item("gem_boots"));RateGate.forget(p.getUUID());h.assertTrue(GemAbilities.activate(p,3)&&p.getAbilities().mayfly,"Boots grant paid lease");
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);AlchemyArtifacts.pulse(p);h.assertTrue(!p.getAbilities().mayfly,"Removal revokes our flight");h.succeed();
    }
    @GameTest(maxTicks=20)public void gemAbilitiesRequireTheirWornPiece(GameTestHelper h){
        var p=player(h);long before=AlchemyPower.available(p);h.assertTrue(!GemAbilities.activate(p,0)&&AlchemyPower.available(p)==before,"No helmet means no action");p.setItemSlot(EquipmentSlot.HEAD,item("gem_helmet"));RateGate.forget(p.getUUID());
        h.assertTrue(GemAbilities.activate(p,0),"Worn helmet accepts toggle");GemAbilities.pulse(p);h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION)&&AlchemyPower.available(p)==before-4,"Night vision has a real paid effect");h.succeed();
    }
    @GameTest(maxTicks=20)public void chestOpenStateCountsConcurrentViewers(GameTestHelper h){
        var p=player(h);var at=new BlockPos(2,3,2);h.setBlock(at,AlchemyStorage.BLOCKS.get("alchemical_chest"));var be=h.getBlockEntity(at,AlchemyStorageBlockEntity.class);be.placedBy(p);
        var first=new AlchemyStorageMenu(1,p.getInventory(),be);var second=new AlchemyStorageMenu(2,p.getInventory(),be);first.removed(p);AlchemyStorageBlockEntity.tick(p.level(),be.getBlockPos(),be.getBlockState(),be);
        h.assertTrue(be.viewers==1&&be.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN),"One remaining viewer keeps lid open");second.removed(p);AlchemyStorageBlockEntity.tick(p.level(),be.getBlockPos(),be.getBlockState(),be);
        h.assertTrue(!be.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN),"Last close shuts lid");h.succeed();
    }
}
