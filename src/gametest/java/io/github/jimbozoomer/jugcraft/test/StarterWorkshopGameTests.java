package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.machine.*;
import io.github.jimbozoomer.jugcraft.workshop.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

public class StarterWorkshopGameTests {
    @GameTest public void companionCraftingStateDoesNotAttachToSmithingTables(GameTestHelper h) {
        var crafting = new BlockPos(1, 1, 1);
        var smithing = new BlockPos(2, 1, 1);
        h.setBlock(crafting, Blocks.CRAFTING_TABLE);
        h.setBlock(smithing, Blocks.SMITHING_TABLE);
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(crafting)) instanceof local.peepo.CraftingTableCompanionBlockEntity,
            "Ordinary crafting tables retain companion work state");
        h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(smithing)) == null,
            "Smithing tables do not inherit an incompatible companion block entity");
        h.succeed();
    }

    @GameTest public void starterWorkshopExportsActualItemRegistry(GameTestHelper h) {
        new SurvivalRecipeGameTests().exportActualItemRegistryForTheAcquisitionAudit(h);
    }
    private static final String ARENA = "jugcraft-test:arms_arena";
    private static final BlockPos AT = new BlockPos(5, 2, 5);
    public static ItemStack stack(String id, int count) { return new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)), count); }
    private WorkshopBlockEntity place(GameTestHelper h, int station) {
        var block = JugcraftWorkshop.BLOCKS[station]; var state = block.defaultBlockState();
        h.setBlock(AT, state); block.setPlacedBy(h.getLevel(), h.absolutePos(AT), state, null, ItemStack.EMPTY);
        return h.getBlockEntity(AT, WorkshopBlockEntity.class);
    }
    private ServerPlayer worker(GameTestHelper h, WorkshopBlockEntity be) {
        var p = h.makeMockServerPlayerInLevel(); p.setGameMode(GameType.SURVIVAL);
        p.snapTo(be.getBlockPos().getX()+.5, be.getBlockPos().getY(), be.getBlockPos().getZ()-1.5, 0, 0);
        p.containerMenu = be.createMenu(41, p.getInventory(), p); return p;
    }
    private void tick(GameTestHelper h, WorkshopBlockEntity be, ServerPlayer worker, int n) {
        for (int i=0;i<n;i++) { if (worker != null) ((WorkshopMenu) worker.containerMenu).clickMenuButton(worker, WorkshopMenu.PULSE); be.tick(h.getLevel()); }
    }
    private void reload(GameTestHelper h, WorkshopBlockEntity be) {
        var saved = be.saveWithoutMetadata(h.getLevel().registryAccess());
        be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
    }
    private void ceramics(WorkshopBlockEntity be) {
        be.setItem(6,stack("jugcraft:basic_ceramic_lining",1)); be.setItem(7,stack("jugcraft:ceramic_crucible",1));
    }
    private void recipe(WorkshopBlockEntity be, String id) { if (!be.select(Jugcraft.id("starter_workshop/"+id))) throw new AssertionError("Recipe unavailable "+id); }

    @GameTest(structure=ARENA) public void starterWorkshopAutomationUsesEitherPartWithoutSupplyingLaborOrTakingTools(GameTestHelper h) {
        var be=place(h,0);recipe(be,"copper_wire");be.setItem(3,stack("jugcraft:wire_cutters",1));
        var input=net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(h.getLevel(),be.getBlockPos().west(),Direction.UP);
        var output=net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(h.getLevel(),be.getBlockPos(),Direction.DOWN);
        h.assertTrue(input!=null && output!=null,"Hoppers/pipes reach master through either part");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long inserted=input.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(Items.COPPER_INGOT),65,tx);
            h.assertTrue(inserted==64,"Copper stays in one slot, leaves other ingredient slots free");
            h.assertTrue(output.extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(JugcraftWorkshop.ITEMS.get("wire_cutters")),1,tx)==0,"Automation never extracts reusable tool");tx.commit();
        }
        tick(h,be,null,400);h.assertTrue(!be.busy() && be.getItem(5).isEmpty(),"Automation is not manual work");
        var p=worker(h,be);tick(h,be,p,100);be.release(p);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(output.extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(stack("jugcraft:copper_wire",1)),3,tx)==3,"Finished wire extractable");tx.commit();
        }
        h.assertTrue(be.getItem(0).getCount()==63 && be.getItem(5).isEmpty(),"Exactly one paid input and one output transfer");h.succeed();
    }

    @GameTest public void starterWorkshopCraftingPatternsSelectTheirIntendedProducts(GameTestHelper h) {
        var recipes=new ArrayList<String>();
        for(String name:List.of("workshop_bench","workshop_hammer","wire_cutters","soldering_tool","small_ceramic_kiln","small_crucible_furnace","unfired_ceramic_lining","unfired_ceramic_crucible","unfired_insulator","insulated_copper_cable")) recipes.add("starter_workshop/"+name);
        recipes.addAll(List.of("copper_cable","insulated_copper_cable"));
        for(String name:recipes) {
            try(var in=getClass().getResourceAsStream("/data/jugcraft/recipe/"+name+".json")) {
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                var slots=new ArrayList<ItemStack>();int width=3,height;
                var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());
                if(json.has("pattern")) {
                    var pattern=json.getAsJsonArray("pattern");width=pattern.get(0).getAsString().length();height=pattern.size();
                    for(var row:pattern) for(char symbol:row.getAsString().toCharArray()) slots.add(symbol==' '?ItemStack.EMPTY:
                        new ItemStack(net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops,json.getAsJsonObject("key").get(String.valueOf(symbol))).getOrThrow().items().findFirst().orElseThrow()));
                } else {
                    for(var ingredient:json.getAsJsonArray("ingredients")) slots.add(new ItemStack(net.minecraft.world.item.crafting.Ingredient.CODEC.parse(ops,ingredient).getOrThrow().items().findFirst().orElseThrow()));
                    height=(slots.size()+2)/3;while(slots.size()<width*height)slots.add(ItemStack.EMPTY);
                }
                var grid=net.minecraft.world.item.crafting.CraftingInput.of(width,height,slots);
                var selected=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,h.getLevel()).orElseThrow();
                var expected=json.getAsJsonObject("result");
                h.assertTrue(ItemStack.matches(selected.value().assemble(grid),stack(expected.get("id").getAsString(),expected.has("count")?expected.get("count").getAsInt():1)),"Crafting collision for "+name+": "+selected.id());
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
        var clayGrid=net.minecraft.world.item.crafting.CraftingInput.of(2,2,List.of(
            new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL),new ItemStack(Items.CLAY_BALL)));
        var clayRecipe=h.getLevel().recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,clayGrid,h.getLevel()).orElseThrow();
        h.assertTrue(clayRecipe.value().assemble(clayGrid).is(Items.CLAY),"Ceramic lining must preserve vanilla clay-block crafting");
        h.succeed();
    }

    @GameTest(structure=ARENA) public void starterWorkshopDisabledFeatureAndProtectedSecondCellDenyWork(GameTestHelper h) {
        var be=place(h,0);var p=worker(h,be);recipe(be,"iron_plate");
        be.setItem(0,new ItemStack(Items.IRON_INGOT));be.setItem(3,stack("jugcraft:workshop_hammer",1));
        io.github.jimbozoomer.jugcraft.config.JugcraftConfig.setFeatureEnabled("machines",false);
        try {tick(h,be,p,10);h.assertTrue(!be.busy() && be.getItem(0).getCount()==1,"Disabled feature spends nothing");}
        finally {io.github.jimbozoomer.jugcraft.config.JugcraftConfig.setFeatureEnabled("machines",true);}
        var protectedCell=new java.util.concurrent.atomic.AtomicReference<>(be.getBlockPos().west());
        PlayerBlockBreakEvents.BEFORE.register((level,actor,pos,state,entity)->!pos.equals(protectedCell.get()));
        try {
            h.assertTrue(!be.pulse(p),"Protection on second cell must deny manual work at master");
            h.assertTrue(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(h.getLevel(),p,be.getBlockPos(),be.getBlockState(),be),"Protected second cell must prevent tearing down master");
        } finally {protectedCell.set(null);}
        p.setGameMode(GameType.ADVENTURE);h.assertTrue(!be.pulse(p),"Adventure cannot work");h.succeed();
    }

    @GameTest(structure=ARENA) public void starterWorkshopEveryManualReceiptMatchesPoweredYieldAndApprovedWorkTime(GameTestHelper h) {
        var be=place(h,0); var player=worker(h,be);
        h.assertTrue(be.catalog().entries().size()==9,"Exactly the nine approved manual recipes");
        for (var entry:be.catalog().entries()) {
            var r=entry.recipe(); be.select(entry.id()); be.setItem(5,ItemStack.EMPTY);
            for(int i=0;i<3;i++) be.setItem(i,ItemStack.EMPTY);
            for(int i=0;i<r.parts().size();i++) { var p=r.parts().get(i); be.setItem(i,new ItemStack(p.ingredient().items().findFirst().orElseThrow(),p.count())); }
            be.setItem(3,r.toolStack()); be.setItem(4,new ItemStack(Items.COAL));
            tick(h,be,null,20); h.assertTrue(be.progress()==0 && be.getItem(5).isEmpty(),"Hoppers/idle cannot perform manual work");
            tick(h,be,player,r.time()-1); h.assertTrue(be.getItem(5).isEmpty() && be.progress()==r.time()-1,"Exact active work cost");
            tick(h,be,player,1); h.assertTrue(ItemStack.matches(be.getItem(5),r.result().create()),"Same approved yield "+entry.id());
            h.assertTrue(ItemStack.matches(be.getItem(3),r.toolStack()),"Tool must remain unchanged");
            var powered=h.getLevel().getServer().getRecipeManager().getRecipes().stream().filter(holder -> {
                if(holder.value() instanceof MachineRecipe m) return ItemStack.matches(m.output().create(),r.result().create()) && m.time()==r.time();
                if(holder.value() instanceof MultiMachineRecipe m) return ItemStack.matches(m.output().create(),r.result().create()) && m.time()==r.time();
                return false;
            }).findFirst();
            h.assertTrue(powered.isPresent(),"Must correspond to existing powered recipe");
            be.release(player);
        }
        h.succeed();
    }
    @GameTest(structure=ARENA) public void starterWorkshopReleaseReloadAndConcurrentPacketsNeverGrantFreeWork(GameTestHelper h) {
        var be=place(h,0); var p=worker(h,be); var other=worker(h,be);
        recipe(be,"copper_wire"); be.setItem(0,new ItemStack(Items.COPPER_INGOT)); be.setItem(3,stack("jugcraft:wire_cutters",1));
        for(int i=0;i<50;i++) ((WorkshopMenu)p.containerMenu).clickMenuButton(p,WorkshopMenu.PULSE);
        h.assertTrue(be.progress()==0,"Repeated packets cannot advance work"); be.tick(h.getLevel());
        h.assertTrue(be.progress()==1,"One server tick advances once");
        h.assertTrue(!be.pulse(other),"Second viewer cannot add a worker");
        tick(h,be,p,24); be.release(p); tick(h,be,null,80);
        h.assertTrue(be.progress()==25,"Release pauses exactly");
        reload(h,be); tick(h,be,null,40); h.assertTrue(be.progress()==25,"Reload cannot revive enabled work");
        h.assertTrue(!be.select(Jugcraft.id("starter_workshop/iron_plate")),"Active receipt cannot switch");
        h.assertTrue(!be.canTakeItemThroughFace(3,be.getItem(3),Direction.DOWN),"Automation cannot steal tool");
        h.assertTrue(!p.containerMenu.slots.get(3).mayPickup(p),"Player cannot remove active tool");
        tick(h,be,other,75); h.assertTrue(be.getItem(5).getCount()==3 && be.getItem(0).isEmpty(),"Another player can resume, no duplicate inputs/results");
        other.setGameMode(GameType.SPECTATOR); h.assertTrue(!be.pulse(other),"Spectator denied");
        p.snapTo(be.getBlockPos().getX()+30,be.getBlockPos().getY(),be.getBlockPos().getZ(),0,0); h.assertTrue(!be.pulse(p),"Remote work denied");
        h.succeed();
    }
    @GameTest(structure=ARENA) public void starterWorkshopKilnFiresAllCeramicsAndKeepsOrdinaryFuelCredit(GameTestHelper h) {
        var be=place(h,1); be.setItem(4,new ItemStack(Items.COAL));
        int paid=0;
        for(var entry:be.catalog().entries()) {
            var r=entry.recipe(); be.select(entry.id()); var part=r.parts().getFirst();
            be.setItem(0,new ItemStack(part.ingredient().items().findFirst().orElseThrow(),part.count())); be.setItem(5,ItemStack.EMPTY);
            tick(h,be,null,r.time()); paid+=r.time();
            h.assertTrue(ItemStack.matches(be.getItem(5),r.result().create()),"Kiln result "+entry.id());
        }
        h.assertTrue(paid==1200 && be.fuelCredit()==400 && be.getItem(4).isEmpty(),"Ordinary coal pays exact active time");
        tick(h,be,null,300); reload(h,be); h.assertTrue(be.fuelCredit()==400,"Idle/reload must retain fuel credit");
        h.assertTrue(be.fuelTicks(new ItemStack(Items.CHARCOAL))==1600,"Use vanilla charcoal, not generator discount");
        h.assertTrue(be.fuelTicks(new ItemStack(Items.OAK_PLANKS))==300,"Accept ordinary wood fuel");
        h.succeed();
    }
    @GameTest(structure=ARENA) public void starterWorkshopAlloysRequirePermanentCeramicsAndPreservePaidPartialBatches(GameTestHelper h) {
        var be=place(h,2); recipe(be,"bronze_ingot");
        be.setItem(0,new ItemStack(Items.COPPER_INGOT,24)); be.setItem(1,stack("jugcraft:tin_ingot",8)); be.setItem(4,new ItemStack(Items.COAL));
        tick(h,be,null,30); h.assertTrue(be.progress()==0 && be.getItem(4).getCount()==1,"Missing parts spend nothing");
        ceramics(be); tick(h,be,null,123); reload(h,be);
        h.assertTrue(be.progress()==123 && be.fuelCredit()==1477,"Save paid work and heat");
        be.setItem(5,stack("jugcraft:bronze_ingot",64)); tick(h,be,null,80);
        h.assertTrue(be.progress()==123 && be.fuelCredit()==1477,"Full output pauses active batch without burn");
        be.setItem(5,ItemStack.EMPTY); tick(h,be,null,1477);
        h.assertTrue(be.getItem(5).getCount()==32 && be.fuelCredit()==0,"One coal makes eight ten-second bronze batches");
        h.assertTrue(be.getItem(0).isEmpty() && be.getItem(1).isEmpty(),"Metals consumed exactly once per batch");
        h.assertTrue(be.getItem(6).getCount()==1 && be.getItem(7).getCount()==1,"Ceramics permanent");
        recipe(be,"solder_ingot"); be.setItem(5,ItemStack.EMPTY); be.setItem(0,stack("jugcraft:tin_ingot",1)); be.setItem(1,stack("jugcraft:lead_ingot",1));
        be.setItem(4,new ItemStack(Items.STICK)); tick(h,be,null,100);
        h.assertTrue(be.progress()==100 && be.fuelCredit()==0,"Short fuel stops without resetting");
        tick(h,be,null,20); be.setItem(4,new ItemStack(Items.COAL)); tick(h,be,null,20);
        h.assertTrue(be.getItem(5).is(BuiltInRegistries.ITEM.getValue(Jugcraft.id("solder_ingot"))) && be.getItem(5).getCount()==2,"Solder resumes to exact output");
        recipe(be,"brass_ingot"); be.setItem(5,ItemStack.EMPTY); be.setItem(0,new ItemStack(Items.COPPER_INGOT,3)); be.setItem(1,stack("jugcraft:zinc_ingot",1));
        tick(h,be,null,200); h.assertTrue(ItemStack.matches(be.getItem(5),stack("jugcraft:brass_ingot",4)),"Brass route"); h.succeed();
    }
    @GameTest(structure=ARENA) public void starterWorkshopFuelContainersCancelAndOutputBackpressureAreLossless(GameTestHelper h) {
        var be=place(h,1); recipe(be,"fire_crucible"); be.setItem(0,stack("jugcraft:unfired_ceramic_crucible",1)); be.setItem(4,new ItemStack(Items.LAVA_BUCKET));
        be.setItem(5,stack("jugcraft:ceramic_crucible",64)); tick(h,be,null,5);
        h.assertTrue(be.getItem(4).is(Items.LAVA_BUCKET) && !be.busy(),"Full output cannot consume first fuel/inputs");
        be.setItem(5,ItemStack.EMPTY); tick(h,be,null,40);
        h.assertTrue(be.getItem(4).is(Items.BUCKET) && be.fuelCredit()==19960,"Lava remainder and fuel credit");
        h.assertTrue(be.canTakeItemThroughFace(4,be.getItem(4),Direction.DOWN),"Empty fuel bucket extractable");
        be.setItem(0,new ItemStack(Items.STONE,64)); h.assertTrue(!be.cancel(),"Cancellation cannot void blocked reserved feed");
        be.setItem(0,ItemStack.EMPTY); h.assertTrue(be.cancel() && be.getItem(0).getCount()==1 && be.fuelCredit()==19960,"Cancel returns feed only, never used fuel");
        h.succeed();
    }
    @GameTest(structure=ARENA) public void starterWorkshopBothMultiblockPartsPlaceBreakAndDropContentsOnce(GameTestHelper h) {
        for(int kind:new int[]{0,2}) for(int broken:new int[]{0,1}) {
            var be=place(h,kind); var block=JugcraftWorkshop.BLOCKS[kind]; var pos=be.getBlockPos(); var state=be.getBlockState();
            be.setItem(4,new ItemStack(Items.COAL,3));
            if(kind==2) { ceramics(be); recipe(be,"bronze_ingot"); be.setItem(0,new ItemStack(Items.COPPER_INGOT,3)); be.setItem(1,stack("jugcraft:tin_ingot",1)); tick(h,be,null,17); }
            h.getLevel().destroyBlock(block.footprint().partPos(pos,Direction.NORTH,broken),true);
            for(int i=0;i<2;i++) h.assertTrue(h.getLevel().isEmptyBlock(block.footprint().partPos(pos,Direction.NORTH,i)),"Remove entire station");
            var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
            h.assertTrue(drops.stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum()==1,"Exactly one station item");
            h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.COAL)).mapToInt(e->e.getItem().getCount()).sum()==(kind==2?2:3),"Inventory drops once");
            if(kind==2) h.assertTrue(drops.stream().filter(e->e.getItem().is(Items.COPPER_INGOT)).mapToInt(e->e.getItem().getCount()).sum()==3,"Escrow returned, not completed alloy");
            drops.forEach(net.minecraft.world.entity.Entity::discard);
        }
        var block=JugcraftWorkshop.BLOCKS[2]; var player=h.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
        var pos=h.absolutePos(AT); h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3); h.getLevel().setBlock(pos.above(),Blocks.STONE.defaultBlockState(),3);
        var stack=new ItemStack(block.asItem(),2); player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false);
        stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(stack.getCount()==2 && h.getLevel().isEmptyBlock(pos),"Obstructed second cell rejects whole placement");
        h.getLevel().removeBlock(pos.above(),false); stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        h.assertTrue(stack.getCount()==1 && block.complete(h.getLevel(),pos,h.getLevel().getBlockState(pos)),"Normal item placement forms station");
        player.setGameMode(GameType.CREATIVE); player.gameMode.destroyBlock(pos.above());
        h.assertTrue(h.getLevel().isEmptyBlock(pos) && h.getLevel().isEmptyBlock(pos.above()),"Creative removes both halves");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3),e->e.getItem().is(block.asItem())).isEmpty(),"Creative does not duplicate station");
        h.succeed();
    }
}
