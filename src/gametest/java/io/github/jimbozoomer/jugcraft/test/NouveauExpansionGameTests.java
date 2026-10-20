package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.nouveau.*;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** End-of-port regression coverage for physical conservation, access and native acquisition. */
public final class NouveauExpansionGameTests {
    private static ServerPlayer player(GameTestHelper h){var p=h.makeMockServerPlayerInLevel();p.setGameMode(GameType.SURVIVAL);p.snapTo(h.absoluteVec(new Vec3(2.5,1,2.5)));RateGate.forget(p.getUUID());return p;}
    private static StorageLectern lectern(GameTestHelper h,ServerPlayer p,BlockPos at){h.setBlock(at,JugNouveau.LECTERN);var b=(StorageLectern)h.getLevel().getBlockEntity(h.absolutePos(at));b.placedBy(p);return b;}
    @GameTest(maxTicks=20)
    public void allContentFamiliesHaveNativeObjects(GameTestHelper h){
        for(var entry:NouveauCatalog.BLOCK_ENTRIES.values())h.assertTrue(NouveauCatalog.BLOCKS.get(entry.source())!=null,"Missing native block: "+entry.source());
        for(var entry:NouveauCatalog.ITEM_ENTRIES.values())h.assertTrue(NouveauCatalog.ITEMS.get(entry.source())!=null&&NouveauCatalog.ITEMS.get(entry.source())!=Items.AIR,"Missing native item: "+entry.source());
        for(String station:List.of("enchanting_apparatus","imbuement_chamber","scribes_table","alteration_table","crush"))h.assertTrue(!NouveauRecipes.forStation(station).isEmpty(),"No loaded recipes for "+station);
        h.assertTrue(NouveauRecipes.all().stream().anyMatch(r->r.operation().equals("summon_ritual")&&r.id().endsWith("/bats")&&r.ley()==0),"Bat entity-selection source loads independently of numeric Source costs");
        h.assertTrue(NouveauCatalog.block("archwood_chest") instanceof NouveauChest,"Archwood chest uses native inventory and lid behavior");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void terminalWithdrawsOnceAcrossTwoViewers(GameTestHelper h){
        var p=player(h);var home=lectern(h,p,new BlockPos(1,1,1));home.bind(p);h.setBlock(3,1,1,Blocks.CHEST);var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3,1,1)));chest.setItem(0,new ItemStack(Items.DIAMOND,16));home.link(p,chest.getBlockPos());
        var a=new NouveauStorageTerminal.Menu(1,p.getInventory(),home);var b=new NouveauStorageTerminal.Menu(2,p.getInventory(),home);a.quickMoveStack(p,0);b.quickMoveStack(p,0);
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==16&&chest.getItem(0).isEmpty(),"A stale catalog cannot withdraw a second copy");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void namedAndComponentDistinctStacksStayDistinct(GameTestHelper h){
        var p=player(h);var home=lectern(h,p,new BlockPos(1,1,1));home.bind(p);h.setBlock(3,1,1,Blocks.CHEST);var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3,1,1)));var named=new ItemStack(Items.DIAMOND,3);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named Gem"));chest.setItem(0,named);chest.setItem(1,new ItemStack(Items.DIAMOND,5));home.link(p,chest.getBlockPos());
        var menu=new NouveauStorageTerminal.Menu(1,p.getInventory(),home);h.assertTrue(menu.count(0)+menu.count(1)==8&&menu.count(2)==0,"Components create two distinct catalog rows");menu.quickMoveStack(p,0);menu.quickMoveStack(p,0);h.assertTrue(p.getInventory().countItem(Items.DIAMOND)==8,"All actual gems transfer without collapsing their identities");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void remoteLecternsRejectCyclesAndRespectLostLinks(GameTestHelper h){
        var p=player(h);var a=lectern(h,p,new BlockPos(1,1,1));var b=lectern(h,p,new BlockPos(3,1,1));h.assertTrue(a.proxyTo(p,b)&&a.root(p)==b,"Remote lectern resolves its loaded root");h.assertTrue(!b.proxyTo(p,a),"Cycle rejected before saving");h.setBlock(new BlockPos(3,1,1),Blocks.AIR);h.assertTrue(a.root(p)==null,"Removed root closes access without loading or creating an inventory");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void naturallyGeneratedStationIsAdoptedOnlyOnce(GameTestHelper h){
        var p=player(h);h.setBlock(1,1,1,NouveauCatalog.block("imbuement_chamber"));var station=(NouveauWorkshop)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));
        h.assertTrue(station.mayUse(p)&&p.getUUID().equals(station.owner()),"Nearby visitor can use a generated station");var other=player(h);h.assertTrue(!station.mayUse(other),"A second unrelated visitor cannot steal the station");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void failedRecipeMatchPreservesInputs(GameTestHelper h){
        var p=player(h);var data=com.google.gson.JsonParser.parseString("{\"type\":\"imbuement\",\"input\":{\"item\":\"minecraft:amethyst_shard\"},\"result\":{\"id\":\"jugcraft:nouveau_source_gem\"}}").getAsJsonObject();var input=new ItemStack(Items.DIAMOND,2);var recipe=new NouveauRecipes.Working("test:source","imbuement",List.of(data.get("input")),data,0,0);
        h.assertTrue(NouveauRecipes.match(recipe,List.of(input),p)==null&&input.getCount()==2,"Rejected inputs are unchanged");var gem=new ItemStack(Items.AMETHYST_SHARD,2);var match=NouveauRecipes.match(recipe,List.of(gem),p);h.assertTrue(match!=null&&match.output().is(NouveauCatalog.item("source_gem"))&&match.after().getFirst().getCount()==1&&gem.getCount()==2,"Accepted recipe reserves exactly one actual input before commit");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void fullAndFilteredRepositoriesRejectWithoutConsumption(GameTestHelper h){
        var p=player(h);h.setBlock(1,1,1,NouveauCatalog.block("repository"));var station=(NouveauWorkshop)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));station.placedBy(p);station.filter(new ItemStack(Items.IRON_INGOT));var diamond=new ItemStack(Items.DIAMOND,7);h.assertTrue(!NouveauCrafting.deposit(station,diamond,false)&&diamond.getCount()==7,"Filter prevents unrelated input");station.filter(ItemStack.EMPTY);for(int i=0;i<54;i++)station.setItem(i,new ItemStack(Items.STONE,64));h.assertTrue(!NouveauCrafting.deposit(station,diamond,false)&&diamond.getCount()==7,"Full storage cannot consume or partially apply a rejected transfer");h.succeed();
    }

    private static NouveauWorkshop station(GameTestHelper h,ServerPlayer p,String name,BlockPos at){h.setBlock(at,NouveauCatalog.block(name));var b=(NouveauWorkshop)h.getLevel().getBlockEntity(h.absolutePos(at));b.placedBy(p);return b;}
    @GameTest(maxTicks=20)
    public void sourcePaymentIsAtomicAndOwnerBound(GameTestHelper h){var p=player(h);var consumer=station(h,p,"imbuement_chamber",new BlockPos(1,1,1));var jar=station(h,p,"source_jar",new BlockPos(2,1,1));jar.fill(7);h.assertTrue(!NouveauSource.pay(h.getLevel(),p,consumer,8)&&jar.ley()==7,"Insufficient charge leaves every donor intact");h.assertTrue(NouveauSource.pay(h.getLevel(),p,consumer,5)&&jar.ley()==2,"Payment draws exactly the stated charge");consumer.fill(3);var stranger=player(h);h.assertTrue(!NouveauSource.pay(h.getLevel(),stranger,consumer,1)&&consumer.ley()==3&&jar.ley()==2,"Another owner's nearby jar cannot be drained");h.succeed();}
    @GameTest(maxTicks=20)
    public void potionJarRejectsMixingAndOverfill(GameTestHelper h){var p=player(h);var jar=station(h,p,"potion_jar",new BlockPos(1,1,1));var water=new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.WATER);var healing=new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.HEALING);h.assertTrue(jar.fillPotion(healing,99),"Jar accepts real doses");h.assertTrue(!jar.fillPotion(water,1)&&!jar.fillPotion(healing,2)&&jar.doses()==99,"No mixed or excess doses consumed");h.assertTrue(!jar.drainPotion(100)&&jar.drainPotion(2)&&jar.doses()==97,"Drain is bounded and exact");h.succeed();}
    @GameTest(maxTicks=20)
    public void allDirectRecipeOutputsDecodeWithComponents(GameTestHelper h){int checked=0;for(var recipe:NouveauRecipes.all()){var data=recipe.data();var value=data.has("result")?data.get("result"):data.get("output");if(value==null||!value.isJsonObject())continue;var obj=value.getAsJsonObject();if(!obj.has("id")&&!obj.has("item"))continue;h.assertTrue(!NouveauRecipes.stack(value,h.getLevel().registryAccess()).isEmpty(),"Output failed to decode: "+recipe.id());checked++;}h.assertTrue(checked>100,"All direct-output recipe families loaded");h.succeed();}

    @GameTest(maxTicks=20)
    public void workshopSaveRoundTripKeepsOwnerDosesAndItems(GameTestHelper h){var p=player(h);var jar=station(h,p,"potion_jar",new BlockPos(1,1,1));var potion=new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.HEALING);jar.fillPotion(potion,17);jar.fill(25);jar.setItem(0,new ItemStack(Items.DIAMOND,9));jar.customName("Saved Jar");jar.toggleLock();var saved=jar.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new NouveauWorkshop(jar.getBlockPos(),jar.getBlockState());copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));h.assertTrue(p.getUUID().equals(copy.owner())&&copy.doses()==17&&copy.potion().equals(potion)&&copy.ley()==25&&copy.getItem(0).getCount()==9&&copy.locked(),"World-save decoding retains owner, dose type/count, charge, items and lock");h.succeed();}

    @GameTest(maxTicks=20)
    public void everySuppliedGlyphLoadsAndEveryOperationCompiles(GameTestHelper h){
        var catalog=io.github.jimbozoomer.jugcraft.concordance.ConcordanceData.rules().catalog();
        var instrument=catalog.instrumentFor("jugcraft:nouveau_archmage_spell_book");
        h.assertTrue(instrument!=null,"Archmage instrument must load");
        int count=0;
        for(var entry:NouveauCatalog.ITEM_ENTRIES.values())if(entry.kind().equals("glyph")){
            String key="jugcraft:nouveau_"+entry.source().replaceFirst("^glyph_","");
            var component=catalog.component(key);h.assertTrue(component!=null,"Supplied glyph missing from composer: "+key);count++;
            if(component.slot()==io.github.jimbozoomer.jugcraft.concordance.compose.Slot.OPERATION){
                var result=io.github.jimbozoomer.jugcraft.concordance.compose.Compiler.compile("nouveau_touch struck "+key,catalog,r->io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState.MASTERED,instrument);
                h.assertTrue(result.ok(),"Operation must form a usable spell: "+key+" "+result.problems());
            }
        }
        h.assertTrue(count==85,"All 85 supplied glyphs are covered");h.succeed();
    }

    @GameTest(maxTicks=100)
    public void everyWorkshopIngredientHasARegisteredMatch(GameTestHelper h){
        var unique=new LinkedHashMap<String,com.google.gson.JsonElement>();
        for(var recipe:NouveauRecipes.all()){
            for(var ingredient:recipe.ingredients())unique.putIfAbsent(ingredient.toString(),ingredient);
            for(String field:List.of("reagent","input"))if(recipe.data().has(field))unique.putIfAbsent(recipe.data().get(field).toString(),recipe.data().get(field));
            if(recipe.data().has("pedestalItems"))for(var ingredient:recipe.data().getAsJsonArray("pedestalItems"))unique.putIfAbsent(ingredient.toString(),ingredient);
        }
        for(var entry:unique.entrySet()){boolean found=false;for(Item item:net.minecraft.core.registries.BuiltInRegistries.ITEM)if(item!=Items.AIR&&NouveauRecipes.ingredient(entry.getValue(),new ItemStack(item))){found=true;break;}
            h.assertTrue(found,"Recipe ingredient has no obtainable registered identity: "+entry.getKey());}
        h.succeed();
    }

    @GameTest(maxTicks=100)
    public void allFiftyPresetTomesCompileAndReserveActualCraftingInputs(GameTestHelper h){
        var p=player(h);int count=0;
        for(var recipe:NouveauRecipes.forStation("scribes_table"))if(recipe.operation().equals("caster_tome")){
            var plan=NouveauTomes.plan(recipe);h.assertTrue(plan!=null,"Supplied preset must compile: "+recipe.id());
            h.assertTrue(plan.limits().targets()==64&&plan.limits().work()==384&&plan.focus()>0,"Preset preserves bounded shared accounting");
            var match=NouveauRecipes.match(recipe,List.of(new ItemStack(Items.BOOK),new ItemStack(Items.PAPER),new ItemStack(NouveauCatalog.item("source_gem"))),p);
            h.assertTrue(match!=null&&recipe.id().equals(match.output().get(NouveauGear.TOME)),"Each preset is independently craftable and identifiable");
            h.assertTrue(match.after().stream().allMatch(ItemStack::isEmpty),"One real copy of each required input is reserved");count++;
        }h.assertTrue(count==50,"All fifty supplied tomes are available");h.succeed();
    }
    @GameTest(maxTicks=20)
    public void experienceGemStacksConserveEachGemsStoredPoints(GameTestHelper h){
        var p=player(h);var gems=new ItemStack(NouveauCatalog.item("experience_gem"),4);
        h.assertTrue(gems.getMaxStackSize()>=4,"The supplied four-gem recipe fits its result stack");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,gems);p.giveExperiencePoints(50);p.setShiftKeyDown(true);
        gems.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        int points=0,count=0;ItemStack filled=ItemStack.EMPTY;for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(NouveauCatalog.item("experience_gem"))){count+=item.getCount();points+=item.getCount()*item.getOrDefault(NouveauGear.EXPERIENCE,0);if(item.getOrDefault(NouveauGear.EXPERIENCE,0)>0)filled=item;}}
        h.assertTrue(p.totalExperience==0&&count==4&&points==50,"Filling one gem cannot copy XP onto four gems");
        p.setShiftKeyDown(false);filled.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        // The API uses the held stack; inspect the physical filled gem by moving it into that hand.
        h.assertTrue(p.totalExperience==0,"An empty held gem cannot withdraw another gem's XP");
        var empty=p.getMainHandItem();for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i)==filled){p.getInventory().setItem(i,empty);break;}
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,filled);filled.use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.totalExperience==50&&filled.getOrDefault(NouveauGear.EXPERIENCE,0)==0,"Withdrawing returns exactly the stored XP");h.succeed();
    }

}
