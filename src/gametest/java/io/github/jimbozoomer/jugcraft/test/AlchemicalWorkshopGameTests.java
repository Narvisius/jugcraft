package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
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
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;

public class AlchemicalWorkshopGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);
        var at=h.absolutePos(new BlockPos(3,2,3));p.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
        ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);
        ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);Assaying.setBalance(p,100000);return p;
    }
    private static AlchemicalWorkshopBlockEntity place(GameTestHelper h,ServerPlayer p,String name,BlockPos pos) {
        h.setBlock(pos,AlchemicalWorkshops.BLOCKS.get(name));var be=h.getBlockEntity(pos,AlchemicalWorkshopBlockEntity.class);be.placedBy(p);return be;
    }
    private static AlchemicalWorkshopBlockEntity condenser(GameTestHelper h,ServerPlayer p,int tier) {
        var be=place(h,p,"condenser_mk"+tier,new BlockPos(2,2,2));be.setItem(1,new ItemStack(Items.IRON_INGOT));return be;
    }
    private static void press(AlchemicalWorkshopMenu m,ServerPlayer p,int id) {RateGate.forget(p.getUUID());m.clickMenuButton(p,id);}
    private static int outputs(AlchemicalWorkshopBlockEntity be) {int n=0;for(int i=5;i<11;i++)n+=be.getItem(i).getCount();return n;}
    private static ItemStack star(int charge) {var s=new ItemStack(KleinStars.ITEMS.get("klein_star_6"));KleinStars.setStored(s,charge);return s;}

    @GameTest(maxTicks=20) public void condenserUsesExactTabletPriceAndKeepsSample(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setEssence(10000);long cost=be.cost();be.pulse(p);
        h.assertTrue(outputs(be)==1&&be.getItem(5).is(Items.IRON_INGOT)&&be.getItem(1).getCount()==1,"One ordinary output; real sample retained");
        h.assertTrue(be.essence()==10000-cost&&cost==Assay.form(AlchemicalWorkshopBlockEntity.material(be.getItem(1)),1),"Identical conservative price to tablet");h.succeed();
    }
    @GameTest(maxTicks=20) public void secondTierFormsAtMostEightPerPulse(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,2);be.setEssence(100000);long cost=be.cost();be.pulse(p);
        h.assertTrue(outputs(be)==8&&be.essence()==100000-8*cost,"Tier two pays for each of eight outputs");h.succeed();
    }
    @GameTest(maxTicks=20) public void dedicatedFeedSlotsDissolveABoundedBatch(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setItem(2,new ItemStack(Items.GOLD_INGOT,64));
        long gain=Assay.dissolve(AlchemicalWorkshopBlockEntity.material(be.getItem(2)),8);be.pulse(p);
        h.assertTrue(be.getItem(2).getCount()==56&&be.essence()==gain-be.cost()&&outputs(be)==1,"Eight fuel items only; output cost deducted");h.succeed();
    }
    @GameTest(maxTicks=20) public void fullOutputStopsStarAndInputConsumption(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,2);be.setItem(0,star(12345));be.setItem(2,new ItemStack(Items.COAL,64));be.setEssence(9876);
        for(int i=5;i<11;i++)be.setItem(i,new ItemStack(Items.STONE,64));be.pulse(p);
        h.assertTrue(be.essence()==9876&&KleinStars.stored(be.getItem(0))==12345&&be.getItem(2).getCount()==64&&be.status()==AlchemicalWorkshopBlockEntity.OUTPUT_FULL,"Blocked output preserves all resources");h.succeed();
    }
    @GameTest(maxTicks=20) public void missingAndModifiedTargetsRefuseIntake(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setItem(0,star(4000));be.setItem(2,new ItemStack(Items.COAL,12));be.setItem(1,ItemStack.EMPTY);be.pulse(p);
        h.assertTrue(be.status()==AlchemicalWorkshopBlockEntity.NO_SAMPLE&&be.essence()==0,"No target waits");
        var named=new ItemStack(Items.IRON_INGOT);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));be.setItem(1,named);be.pulse(p);
        h.assertTrue(be.status()==AlchemicalWorkshopBlockEntity.INVALID_SAMPLE&&be.essence()==0&&KleinStars.stored(be.getItem(0))==4000&&be.getItem(2).getCount()==12,"Modified target preserved, no intake");h.succeed();
    }
    @GameTest(maxTicks=20) public void changedItemsCannotBeDissolvedEvenIfInsertedByCommand(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);var named=new ItemStack(Items.DIAMOND,8);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keepsake"));be.setItem(2,named);be.setItem(3,star(4000));be.pulse(p);
        h.assertTrue(be.essence()==0&&outputs(be)==0&&be.getItem(2).getCount()==8&&KleinStars.stored(be.getItem(3))==4000,"All ineligible inputs untouched");h.succeed();
    }
    @GameTest(maxTicks=20) public void lowBalanceAndAlmostFullOutputNeverOverspend(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,2);be.setEssence((int)be.cost()-1);be.pulse(p);
        h.assertTrue(outputs(be)==0&&be.essence()==be.cost()-1,"Wait for exact price");
        for(int i=5;i<11;i++)be.setItem(i,new ItemStack(Items.IRON_INGOT,64));be.getItem(5).setCount(63);be.setEssence(10000);be.pulse(p);
        h.assertTrue(outputs(be)==384&&be.essence()==10000-be.cost(),"Only one remaining slot paid; seven impossible outputs not charged");h.succeed();
    }
    @GameTest(maxTicks=20) public void condenserStarDrawConservesAndRespectsItsRate(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setItem(0,star(20000));be.pulse(p);
        h.assertTrue(KleinStars.stored(be.getItem(0))==15904&&be.essence()+KleinStars.stored(be.getItem(0))+be.cost()==20000,"Only 4096 withdrawn; one output paid");h.succeed();
    }
    @GameTest(maxTicks=20) public void collectorTransferHasOneBudgetAcrossNeighboursAndStar(GameTestHelper h) {
        var p=player(h);var col=place(h,p,"collector_mk1",new BlockPos(3,2,2));var con=condenser(h,p,1);col.setItem(0,star(0));col.setEssence(1000);col.pulse(p);
        h.assertTrue(con.essence()==64&&KleinStars.stored(col.getItem(0))==0,"Condenser gets first claim on the single 64 budget");
        h.assertTrue(col.essence()+con.essence()>=1000&&col.essence()+con.essence()<=1001,"Transfer conserves buffer plus at most one daylight essence");h.succeed();
    }
    @GameTest(maxTicks=20) public void foreignOrProtectedCondenserReceivesNothing(GameTestHelper h) {
        var p=player(h);var col=place(h,p,"collector_mk1",new BlockPos(3,2,2));var con=condenser(h,player(h),1);col.setEssence(1000);col.pulse(p);h.assertTrue(con.essence()==0,"Unrelated ownership refuses transfer");
        con.placedBy(p);try(var claims=TestClaims.open()){claims.block(con.getBlockPos(),UUID.randomUUID());col.pulse(p);h.assertTrue(con.essence()==0,"Destination claim also refuses transfer");}h.succeed();
    }
    @GameTest(maxTicks=20) public void collectorChargesStarsWithinActualCapacity(GameTestHelper h) {
        var p=player(h);var col=place(h,p,"collector_mk3",new BlockPos(2,2,2));var s=new ItemStack(KleinStars.ITEMS.get("klein_star_1"));KleinStars.setStored(s,4090);col.setItem(0,s);col.setEssence(4000);col.pulse(p);
        h.assertTrue(KleinStars.stored(s)==4096&&col.essence()>=3994&&col.essence()<=4010,"Only six fit; charge cannot exceed actual star tier");h.succeed();
    }
    @GameTest(maxTicks=20) public void pauseRedstoneDisabledAndAbsentOwnersStopWork(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setEssence(10000);be.setPaused(true);
        h.assertTrue(be.status()==AlchemicalWorkshopBlockEntity.PAUSED&&!be.getBlockState().getValue(AlchemicalWorkshopBlock.ACTIVE),"Pause updates status immediately");
        be.pulse(p);h.assertTrue(outputs(be)==0,"Manual pause");
        be.setPaused(false);h.setBlock(new BlockPos(3,2,2),Blocks.REDSTONE_BLOCK);be.pulse(p);h.assertTrue(outputs(be)==0,"Redstone pause");h.setBlock(new BlockPos(3,2,2),Blocks.AIR);
        be.pulse(null);h.assertTrue(outputs(be)==1&&be.status()==AlchemicalWorkshopBlockEntity.FORMING,"Authorized factory resolves its owner and processes");
        boolean before=Assaying.enabled();try{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,false);be.pulse(p);h.assertTrue(outputs(be)==1&&be.essence()==9744,"Disabled retains buffer");}finally{JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE,before);}h.succeed();
    }
    @GameTest(maxTicks=20) public void freshOwnersCanProduceButClaimsStillGuardMenus(GameTestHelper h) {
        var unknown=h.makeMockServerPlayerInLevel();unknown.setGameMode(GameType.SURVIVAL);var be=condenser(h,unknown,1);be.setEssence(10000);be.pulse(unknown);h.assertTrue(outputs(be)==1&&be.essence()==9744,"Fresh owner produces without research and pays the exact iron price");
        var p=player(h);be.placedBy(p);try(var claims=TestClaims.open()){claims.block(be.getBlockPos(),UUID.randomUUID());be.pulse(p);h.assertTrue(outputs(be)==1&&!new AlchemicalWorkshopMenu(1,p.getInventory(),be).stillValid(p),"Claims stop production and opening");}h.succeed();
    }
    @GameTest(maxTicks=20) public void menuTransfersClampsAndRejectsForeignOrDistantClicks(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,2);var menu=new AlchemicalWorkshopMenu(1,p.getInventory(),be);press(menu,p,1);
        h.assertTrue(be.essence()==100000&&Assaying.balance(p)==0&&menu.essence()==100000&&menu.capacity()==AlchemicalWorkshops.CONDENSER_CAPACITY[1],"Deposit and large menu values exact");
        Assaying.setBalance(p,io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE-1);press(menu,p,2);h.assertTrue(be.essence()==99999&&Assaying.balance(p)==io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE,"Withdrawal stops at ledger cap");
        press(menu,player(h),1);press(menu,p,999);p.snapTo(p.getX()+30,p.getY(),p.getZ(),0,0);press(menu,p,1);
        h.assertTrue(be.essence()==99999,"Foreign, unknown and distant requests refused");h.succeed();
    }
    @GameTest(maxTicks=20) public void saveLoadRetainsInventoryOwnerBufferAndPause(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,2);be.setEssence(123456);be.setPaused(true);be.setItem(0,star(8765));be.setItem(2,new ItemStack(Items.COAL,23));
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new AlchemicalWorkshopBlockEntity(be.getBlockPos(),be.getBlockState());copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.essence()==123456&&copy.paused()&&p.getUUID().equals(copy.owner())&&copy.getItem(2).getCount()==23&&KleinStars.stored(copy.getItem(0))==8765&&copy.getItem(1).is(Items.IRON_INGOT),"Actual block save round trip");h.succeed();
    }
    @GameTest(maxTicks=20) public void lootAndSmithingKeepStoredEssence(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setEssence(45678);
        var drops=Block.getDrops(be.getBlockState(),h.getLevel(),be.getBlockPos(),be,p,new ItemStack(Items.DIAMOND_PICKAXE));
        var block=drops.stream().filter(s->s.is(AlchemicalWorkshops.BLOCKS.get("condenser_mk1").asItem())).findFirst().orElseThrow();
        h.assertTrue(block.getOrDefault(AlchemicalWorkshops.ESSENCE,0)==45678,"Real loot copies buffer");
        block.set(DataComponents.CUSTOM_NAME,Component.literal("Family workshop"));
        var input=new SmithingRecipeInput(new ItemStack(DarkMatter.ITEMS.get("high_covalence_dust")),block,new ItemStack(DarkMatter.ITEMS.get("dark_matter")));
        var upgraded=h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING,input,h.getLevel()).orElseThrow().value().assemble(input);
        h.assertTrue(upgraded.is(AlchemicalWorkshops.BLOCKS.get("condenser_mk2").asItem())&&upgraded.getOrDefault(AlchemicalWorkshops.ESSENCE,0)==45678&&upgraded.getHoverName().getString().equals("Family workshop"),"Vanilla smithing preserves buffer and name");
        var placed=new AlchemicalWorkshopBlockEntity(be.getBlockPos(),AlchemicalWorkshops.BLOCKS.get("condenser_mk2").defaultBlockState());placed.applyComponentsFromItemStack(upgraded);h.assertTrue(placed.essence()==45678,"Placement restores stored essence");h.succeed();
    }
    @GameTest(maxTicks=20) public void workshopRecipesAndExclusionsLoad(GameTestHelper h) {
        Item d=DarkMatter.ITEMS.get("dark_matter"),o=Items.OBSIDIAN,g=Items.GLASS;
        Item[][] grids={{g,Items.GLOWSTONE,g,o,d,o,o,Items.FURNACE,o},{o,d,o,d,Items.CHEST,d,o,d,o}};
        String[] names={"collector_mk1","condenser_mk1"};
        for(int i=0;i<2;i++){var input=CraftingInput.of(3,3,Arrays.stream(grids[i]).map(ItemStack::new).toList());var recipe=h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow().value();h.assertTrue(recipe.assemble(input).is(AlchemicalWorkshops.BLOCKS.get(names[i]).asItem()),"Base recipe "+names[i]);}
        for(var block:AlchemicalWorkshops.BLOCKS.values())h.assertTrue(Assaying.eligibility(new ItemStack(block)).isEmpty(),"Default empty workshop can be exchanged");h.succeed();
    }
    @GameTest(maxTicks=20) public void unsidedAutomationCannotStealStarOrSample(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);be.setItem(0,star(5000));be.setItem(5,new ItemStack(Items.IRON_INGOT,8));
        var storage=ItemStorage.SIDED.find(h.getLevel(),be.getBlockPos(),null);
        try(var t=Transaction.openOuter()){h.assertTrue(storage.extract(ItemVariant.of(be.getItem(0)),1,t)==0&&storage.extract(ItemVariant.of(Items.IRON_INGOT),64,t)==0,"Unsided access is deliberately closed");t.commit();}
        h.assertTrue(Arrays.equals(be.getSlotsForFace(Direction.DOWN),new int[]{5,6,7,8,9,10})&&Arrays.equals(be.getSlotsForFace(Direction.UP),new int[]{2,3,4}),"Only explicit input/output faces exposed");h.succeed();
    }
    @GameTest(maxTicks=20) public void clientPredictionDoesNotRequireTheServerOnlyCatalogue(GameTestHelper h) {
        var p=player(h);var be=condenser(h,p,1);var real=new AlchemicalWorkshopMenu(1,p.getInventory(),be);
        var mirror=new AlchemicalWorkshopMenu(1,p.getInventory(),false);var rules=ConcordanceData.rules();
        try {
            ConcordanceData.replace(io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules.EMPTY);
            h.assertTrue(mirror.getSlot(1).mayPlace(new ItemStack(Items.IRON_INGOT))&&mirror.getSlot(2).mayPlace(new ItemStack(Items.COAL)),"Dedicated-client prediction has no server catalogue dependency");
            h.assertTrue(real.getSlot(1).mayPlace(new ItemStack(Items.IRON_INGOT))&&real.getSlot(2).mayPlace(new ItemStack(Items.COAL)),"Universal values do not disappear with the historical narrow research catalogue");
            h.assertTrue(!mirror.clickMenuButton(p,1),"A client mirror is never a server-authorized transaction");
        } finally {ConcordanceData.replace(rules);}
        h.assertTrue(real.getSlot(1).mayPlace(new ItemStack(Items.IRON_INGOT)),"Restored server catalogue accepts valid target");h.succeed();
    }
}
