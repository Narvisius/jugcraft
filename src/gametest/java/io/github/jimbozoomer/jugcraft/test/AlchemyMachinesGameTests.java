package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.*;
import io.github.jimbozoomer.jugcraft.concordance.assay.*;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;

/** Written with the whole EE package; intentionally run only at the final package validation stage. */
public class AlchemyMachinesGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(2,3,3));p.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);ConcordanceProgress.grant(p,"jugcraft:first_light",ResearchState.UNDERSTOOD);ConcordanceProgress.grant(p,Assaying.RESEARCH,ResearchState.UNDERSTOOD);Assaying.setBalance(p,100000);return p;}
    private static AlchemyMachinesBlockEntity place(GameTestHelper h,ServerPlayer p,String id){return place(h,p,id,new BlockPos(2,3,2));}
    private static AlchemyMachinesBlockEntity place(GameTestHelper h,ServerPlayer p,String id,BlockPos pos){h.setBlock(pos,AlchemyMachines.BLOCKS.get(id));var be=h.getBlockEntity(pos,AlchemyMachinesBlockEntity.class);be.placedBy(p);return be;}
    private static ItemStack star(int amount){var s=new ItemStack(KleinStars.ITEMS.get("klein_star_6"));KleinStars.setStored(s,amount);return s;}
    private static ItemStack item(String name){return new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(name)));}
    private static int output(AlchemyMachinesBlockEntity be){int n=0;for(int i=4;i<7;i++)n+=be.getItem(i).getCount();return n;}

    @GameTest(maxTicks=20)public void darkFurnaceUsesOrdinaryRecipesAndExactCharge(GameTestHelper h){
        var p=player(h);var be=place(h,p,"dark_matter_furnace");be.setEssence(1000);be.setItem(1,new ItemStack(Items.RAW_IRON,8));be.pulse(p);
        h.assertTrue(output(be)==0&&be.progress()==20&&be.essence()==1000,"First second is progress only");be.pulse(p);
        h.assertTrue(be.getItem(4).is(Items.IRON_INGOT)&&output(be)==1&&be.getItem(1).getCount()==7&&be.essence()==984,"One smelt for exactly16 essence");h.succeed();
    }
    @GameTest(maxTicks=20)public void redFurnaceThroughputIsFourAndNeverOverdraws(GameTestHelper h){
        var p=player(h);var be=place(h,p,"red_matter_furnace");be.setItem(1,new ItemStack(Items.RAW_IRON,64));be.setEssence(1000);be.pulse(p);
        h.assertTrue(output(be)==4&&be.getItem(1).getCount()==60&&be.essence()==936,"At most4 per pulse");be.setEssence(31);be.pulse(p);
        h.assertTrue(output(be)==5&&be.essence()==15,"One affordable item only");be.pulse(p);h.assertTrue(output(be)==5&&be.essence()==15,"Below price waits");h.succeed();
    }
    @GameTest(maxTicks=20)public void furnaceFullOutputAndModifiedInputArePreserved(GameTestHelper h){
        var p=player(h);var be=place(h,p,"red_matter_furnace");be.setEssence(1000);be.setItem(1,new ItemStack(Items.RAW_IRON,8));for(int i=4;i<7;i++)be.setItem(i,new ItemStack(Items.STONE,64));be.pulse(p);
        h.assertTrue(be.getItem(1).getCount()==8&&be.essence()==1000&&be.status()==AlchemyMachinesBlockEntity.FULL,"Blocked outputs consume neither input nor essence");
        for(int i=4;i<7;i++)be.setItem(i,ItemStack.EMPTY);var named=new ItemStack(Items.RAW_IRON);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keepsake"));be.setItem(1,named);be.pulse(p);
        h.assertTrue(output(be)==0&&be.essence()==1000&&!be.canPlaceItem(1,named),"Modified input not silently destroyed");h.succeed();
    }
    @GameTest(maxTicks=20)public void relayDissolvesConservativelyAndChargesOneBudget(GameTestHelper h){
        var p=player(h);var be=place(h,p,"relay_mk1");be.setItem(1,new ItemStack(Items.COAL,64));be.setItem(0,star(0));be.pulse(p);
        h.assertTrue(be.getItem(1).getCount()==56,"Eight items maximum");long gained=io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.dissolve(AlchemicalWorkshopBlockEntity.material(new ItemStack(Items.COAL)),8);
        h.assertTrue(be.essence()+KleinStars.stored(be.getItem(0))==gained&&KleinStars.stored(be.getItem(0))<=256,"All essence conserved; outgoing rate bounded");h.succeed();
    }
    @GameTest(maxTicks=20)public void relayUsesOnlyFrontFaceAndChecksRecipientPermission(GameTestHelper h){
        var p=player(h);var relay=place(h,p,"relay_mk1");h.getLevel().setBlock(relay.getBlockPos(),relay.getBlockState().setValue(AlchemyMachinesBlock.FACING,Direction.EAST),3);
        var front=place(h,p,"dark_matter_furnace",new BlockPos(3,3,2));var back=place(h,p,"dark_matter_furnace",new BlockPos(1,3,2));relay.setEssence(1000);relay.pulse(p);
        h.assertTrue(front.essence()==256&&back.essence()==0&&relay.essence()==744,"Only front output receives budget");
        try(var claims=TestClaims.open()){claims.block(front.getBlockPos(),UUID.randomUUID());relay.pulse(p);h.assertTrue(front.essence()==256&&relay.essence()==744,"Protected destination refuses transfer");}h.succeed();
    }
    @GameTest(maxTicks=20)public void collectorFeedsRelayAndRelayFeedsExistingCondenser(GameTestHelper h){
        var p=player(h);var relay=place(h,p,"relay_mk1");h.getLevel().setBlock(relay.getBlockPos(),relay.getBlockState().setValue(AlchemyMachinesBlock.FACING,Direction.EAST),3);
        h.setBlock(new BlockPos(3,3,2),AlchemicalWorkshops.BLOCKS.get("condenser_mk1"));var condenser=h.getBlockEntity(new BlockPos(3,3,2),AlchemicalWorkshopBlockEntity.class);condenser.placedBy(p);condenser.setItem(1,new ItemStack(Items.IRON_INGOT));
        h.setBlock(new BlockPos(1,3,2),AlchemicalWorkshops.BLOCKS.get("collector_mk1"));var collector=h.getBlockEntity(new BlockPos(1,3,2),AlchemicalWorkshopBlockEntity.class);collector.placedBy(p);collector.setEssence(1000);collector.pulse(p);
        h.assertTrue(relay.essence()==64,"Collector supplies the new network");relay.pulse(p);h.assertTrue(condenser.essence()==64&&relay.essence()==0,"No loss crossing relay");h.succeed();
    }
    @GameTest(maxTicks=20)public void absentPausedAndRedstoneMachinesDoNoWork(GameTestHelper h){
        var p=player(h);var be=place(h,p,"red_matter_furnace");be.setItem(1,new ItemStack(Items.RAW_IRON,8));be.setEssence(500);be.pulse(null);h.assertTrue(output(be)==4,"Resolves authorized factory owner");
        be.setPaused(true);be.pulse(p);h.assertTrue(output(be)==4,"Pause retains prior output");be.setPaused(false);h.setBlock(new BlockPos(3,3,2),Blocks.REDSTONE_BLOCK);be.pulse(p);h.assertTrue(output(be)==4&&be.essence()==436,"Redstone preserves resources");h.succeed();
    }
    @GameTest(maxTicks=20)public void pedestalGrowsOnlyPermittedCropsAndKeepsArtifact(GameTestHelper h){
        var p=player(h);var be=place(h,p,"dark_matter_pedestal");be.setItem(1,item("harvest_goddess_band"));be.setEssence(1000);h.setBlock(new BlockPos(3,1,2),Blocks.FARMLAND);h.setBlock(new BlockPos(3,2,2),Blocks.WHEAT);
        var crop=h.absolutePos(new BlockPos(3,2,2));try(var claims=TestClaims.open()){claims.block(crop,UUID.randomUUID());be.pulse(p);h.assertTrue(h.getLevel().getBlockState(crop).getValue(CropBlock.AGE)==0&&be.essence()==1000,"Foreign garden untouched");}
        for(int i=0;i<3;i++)be.pulse(p);h.assertTrue(h.getLevel().getBlockState(crop).getValue(CropBlock.AGE)>0&&be.getItem(1).getCount()==1&&be.essence()<1000,"Rolling growth consumes charge, retains artifact");h.succeed();
    }
    @GameTest(maxTicks=20)public void pedestalRejectsUnknownArtifactAndModifiedRelayFeed(GameTestHelper h){
        var p=player(h);var pedestal=place(h,p,"dark_matter_pedestal");pedestal.setItem(1,new ItemStack(Items.STICK));pedestal.setEssence(100);pedestal.pulse(p);h.assertTrue(pedestal.essence()==100&&pedestal.status()==AlchemyMachinesBlockEntity.BAD_ARTIFACT,"Unsupported artifact is inert");
        var relay=place(h,p,"relay_mk2",new BlockPos(3,3,2));var named=new ItemStack(Items.COAL,8);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep"));relay.setItem(1,named);relay.pulse(p);h.assertTrue(relay.essence()==0&&relay.getItem(1).getCount()==8,"No modified-input dissolution");h.succeed();
    }
    @GameTest(maxTicks=20)public void torchHasStandingAndWallGeometryAndRequiresSupport(GameTestHelper h){
        var block=AlchemyMachines.BLOCKS.get("interdiction_torch");var pos=h.absolutePos(new BlockPos(2,3,2));h.setBlock(new BlockPos(2,2,2),Blocks.STONE);h.setBlock(new BlockPos(1,3,2),Blocks.STONE);
        h.assertTrue(block.defaultBlockState().canSurvive(h.getLevel(),pos),"Ground support");var wall=block.defaultBlockState().setValue(AlchemyMachinesBlock.WALL,true).setValue(AlchemyMachinesBlock.FACING,Direction.EAST);h.assertTrue(wall.canSurvive(h.getLevel(),pos),"Wall support");h.setBlock(new BlockPos(1,3,2),Blocks.AIR);h.assertTrue(!wall.canSurvive(h.getLevel(),pos),"Missing support rejects wall placement");h.succeed();
    }
    @GameTest(maxTicks=20)public void wardMovesHostileCreaturesWithoutHurtingAnimals(GameTestHelper h){
        var p=player(h);h.setBlock(new BlockPos(2,2,2),Blocks.STONE);var be=place(h,p,"interdiction_torch");be.setEssence(100);
        var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(3,3,2));var cow=h.spawn(EntityTypes.COW,new BlockPos(3,3,3));zombie.setNoAi(true);cow.setNoAi(true);float health=zombie.getHealth();be.pulse(p);
        h.assertTrue(be.essence()==96&&zombie.getDeltaMovement().horizontalDistanceSqr()>0&&zombie.getHealth()==health,"One monster repelled for four, no damage");h.assertTrue(cow.getDeltaMovement().lengthSqr()==0,"Animals unaffected");h.succeed();
    }
    @GameTest(maxTicks=20)public void wardHonorsSharedGuardsAndImmuneCreaturesWithoutSpending(GameTestHelper h){
        var p=player(h);h.setBlock(new BlockPos(2,2,2),Blocks.STONE);var be=place(h,p,"interdiction_torch");be.setEssence(100);
        var zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(3,3,2));zombie.setNoAi(true);var immune=h.spawn(EntityTypes.WARDEN,new BlockPos(4,3,3));immune.setNoAi(true);
        var guard=new java.util.concurrent.atomic.AtomicBoolean(true);UUID guarded=zombie.getUUID();ConcordanceEffects.guard((context,spec,target)->guard.get()&&target.getUUID().equals(guarded));
        try{var movement=zombie.getDeltaMovement();var protectedMovement=immune.getDeltaMovement();be.pulse(p);h.assertTrue(be.essence()==100&&zombie.getDeltaMovement().equals(movement)&&immune.getDeltaMovement().equals(protectedMovement),"Shared guard and immunity both refuse repulsion without charging");}finally{guard.set(false);}
        be.pulse(p);h.assertTrue(be.essence()==96&&zombie.getDeltaMovement().horizontalDistanceSqr()>0&&immune.getDeltaMovement().lengthSqr()==0,"Removing guard charges only actual eligible target; immune creature remains untouched");h.succeed();
    }
    @GameTest(maxTicks=20)public void novaClearsOnlyEligibleBlocksAndChargesOnce(GameTestHelper h){
        var p=player(h);var be=place(h,p,"nova_catalyst");h.setBlock(new BlockPos(3,3,2),Blocks.STONE);h.setBlock(new BlockPos(1,3,2),Blocks.CHEST);h.setBlock(new BlockPos(2,4,2),Blocks.BEDROCK);
        long before=Assaying.balance(p);h.assertTrue(be.activateNova(p),"Owner activates a nonempty eligible clearing");h.assertTrue(h.getBlockState(new BlockPos(3,3,2)).isAir()&&h.getBlockState(new BlockPos(2,3,2)).isAir(),"Stone and charge removed");h.assertTrue(h.getBlockState(new BlockPos(1,3,2)).is(Blocks.CHEST)&&h.getBlockState(new BlockPos(2,4,2)).is(Blocks.BEDROCK),"Inventories and unbreakables preserved");h.assertTrue(Assaying.balance(p)==before-4096&&!be.activateNova(p),"Exactly one payment, stale block cannot detonate twice");h.succeed();
    }
    @GameTest(maxTicks=20)public void novaRefusesForeignActivationInsufficientFundsAndProtectedBlocks(GameTestHelper h){
        var p=player(h);var be=place(h,p,"nova_cataclysm");h.setBlock(new BlockPos(3,3,2),Blocks.STONE);h.assertTrue(!be.activateNova(player(h)),"Foreign player refused");Assaying.setBalance(p,16383);h.assertTrue(!be.activateNova(p),"Exact cost required");Assaying.setBalance(p,20000);
        try(var claims=TestClaims.open()){claims.block(h.absolutePos(new BlockPos(3,3,2)),UUID.randomUUID());be.activateNova(p);h.assertTrue(h.getBlockState(new BlockPos(3,3,2)).is(Blocks.STONE),"Protected block never mined");}h.succeed();
    }
    @GameTest(maxTicks=20)public void inventoriesOwnerChargeAndProgressSurviveSave(GameTestHelper h){
        var p=player(h);var be=place(h,p,"dark_matter_furnace");be.setEssence(12345);be.setItem(1,new ItemStack(Items.RAW_IRON,23));be.pulse(p);be.setItem(0,star(9876));be.setPaused(true);
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new AlchemyMachinesBlockEntity(be.getBlockPos(),be.getBlockState());copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));h.assertTrue(copy.essence()==12345&&copy.progress()==20&&copy.paused()&&p.getUUID().equals(copy.owner())&&copy.getItem(1).getCount()==23&&KleinStars.stored(copy.getItem(0))==9876,"Actual saved payload round trip");h.succeed();
    }
    @GameTest(maxTicks=20)public void relayLootAndUpgradePreserveChargeAndCustomName(GameTestHelper h){
        var p=player(h);var be=place(h,p,"relay_mk1");be.setEssence(12345);var drops=Block.getDrops(be.getBlockState(),h.getLevel(),be.getBlockPos(),be,p,new ItemStack(Items.NETHERITE_PICKAXE));var block=drops.stream().filter(s->s.is(AlchemyMachines.BLOCKS.get("relay_mk1").asItem())).findFirst().orElseThrow();block.set(DataComponents.CUSTOM_NAME,Component.literal("Workshop relay"));
        var input=new SmithingRecipeInput(item("high_covalence_dust"),block,item("dark_matter"));var result=h.getLevel().recipeAccess().getRecipeFor(RecipeType.SMITHING,input,h.getLevel()).orElseThrow().value().assemble(input);h.assertTrue(result.is(AlchemyMachines.BLOCKS.get("relay_mk2").asItem())&&result.getOrDefault(AlchemicalWorkshops.ESSENCE,0)==12345&&result.getHoverName().getString().equals("Workshop relay"),"Loot and vanilla upgrade preserve components");h.succeed();
    }
    @GameTest(maxTicks=20)public void menuConservesLedgerAndRejectsForeignAndDistantRequests(GameTestHelper h){
        var p=player(h);var be=place(h,p,"relay_mk3");var m=new AlchemyMachinesMenu(1,p.getInventory(),be);RateGate.forget(p.getUUID());h.assertTrue(m.clickMenuButton(p,1)&&be.essence()==100000&&Assaying.balance(p)==0&&m.essence()==100000,"Unsigned split menu data and ledger conservation");
        RateGate.forget(p.getUUID());Assaying.setBalance(p,io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE-1);m.clickMenuButton(p,2);h.assertTrue(be.essence()==99999&&Assaying.balance(p)==io.github.jimbozoomer.jugcraft.concordance.equivalence.Assay.MAX_BALANCE,"Wallet cap");h.assertTrue(!m.clickMenuButton(player(h),1),"Foreign request");p.snapTo(p.getX()+30,p.getY(),p.getZ(),0,0);RateGate.forget(p.getUUID());h.assertTrue(!m.clickMenuButton(p,1),"Distance checked live");h.succeed();
    }
    @GameTest(maxTicks=20)public void unsidedAutomationCannotTakeStarsAndManualArtifacts(GameTestHelper h){
        var p=player(h);var be=place(h,p,"dark_matter_pedestal");be.setItem(0,star(1000));be.setItem(1,item("repair_talisman"));var storage=ItemStorage.SIDED.find(h.getLevel(),be.getBlockPos(),null);
        try(var t=Transaction.openOuter()){h.assertTrue(storage.extract(ItemVariant.of(be.getItem(0)),1,t)==0&&storage.extract(ItemVariant.of(be.getItem(1)),1,t)==0,"Unsided storage cannot access either protected slot");t.commit();}
        h.assertTrue(be.getSlotsForFace(Direction.DOWN).length==0&&be.getSlotsForFace(Direction.UP).length==0,"Pedestal has no automated slots");h.succeed();
    }
    @GameTest(maxTicks=20)public void pedestalObserversReceiveOnlySanitizedDisplayItem(GameTestHelper h){
        var p=player(h);var be=place(h,p,"dark_matter_pedestal");be.setEssence(1234);be.setItem(0,star(5678));var artifact=item("life_stone");artifact.set(DataComponents.CUSTOM_NAME,Component.literal("Private family keepsake"));be.setItem(1,artifact);
        var tag=be.getUpdateTag(h.getLevel().registryAccess());var input=TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag);var visible=input.read("display_artifact",ItemStack.OPTIONAL_CODEC).orElseThrow();
        h.assertTrue(tag.keySet().equals(Set.of("display_only","display_artifact","effect_range"))&&visible.is(artifact.getItem())&&visible.getCount()==1&&!visible.has(DataComponents.CUSTOM_NAME),"Observer update contains only the plain public artifact, not ownership, private data or charge");
        var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new AlchemyMachinesBlockEntity(be.getBlockPos(),be.getBlockState());copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.essence()==1234&&copy.owner().equals(p.getUUID())&&KleinStars.stored(copy.getItem(0))==5678&&copy.getItem(1).getHoverName().getString().equals("Private family keepsake"),"Full server save still retains all private state");
        be.removeItemNoUpdate(1);var empty=TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),be.getUpdateTag(h.getLevel().registryAccess()));h.assertTrue(empty.read("display_artifact",ItemStack.OPTIONAL_CODEC).orElseThrow().isEmpty(),"Removal publishes empty display");h.succeed();
    }
    @GameTest(maxTicks=20)public void everyDefaultMachineHasSurvivalRecipeAndValue(GameTestHelper h){
        for(var entry:AlchemyMachines.BLOCKS.entrySet()){h.assertTrue(Assaying.eligibility(new ItemStack(entry.getValue())).isEmpty(),"Default uncharged machine valued "+entry.getKey());h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,Jugcraft.id(entry.getKey()))).isPresent(),"Survival recipe "+entry.getKey());}h.succeed();
    }
}
