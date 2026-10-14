package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.chemistry.PetroItems;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineKind;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import io.github.jimbozoomer.jugcraft.machine.form.*;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;

/** Shared-process ledgers and old save compatibility. */
public class MachineConsolidationGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.BRINE_CONCENTRATOR,IndustrialForms.GAS_CLEANING_RACK,IndustrialForms.ALUMINATE_FILTER_PRESS,IndustrialForms.ALUMINA_CALCINER);}
    @GameTest(structure = ARENA)
    public void everyLoadedRecipePaysItsLedgerAndRetainsItsTool(GameTestHelper h) {
        int checked = 0;
        for (FormMachineBlock block : blocks()) {
            if (CoastalSeawaterIntake.matches(block.form())) continue;
            FormMachineBlockEntity first = place(h, block);
            var recipes = first.recipes(h.getLevel().getServer());
            h.assertTrue(!recipes.isEmpty(), "No loaded recipes for " + block.form().id());
            clear(h, first);
            for (var entry : recipes) {
                FormMachineBlockEntity machine = place(h, block);
                FluidRecipe recipe = entry.recipe();
                feed(machine, recipe);
                long starting = machine.energy().getAmount();
                if (recipe.tool().isPresent()) {
                    int slot = machine.form().firstSocketSlot();
                    ItemStack tool = machine.container().getItem(slot);
                    machine.container().setItem(slot, ItemStack.EMPTY);
                    ticks(h, machine, 1);
                    h.assertTrue(machine.lane(0) == null && machine.status().state() == MachineLifecycle.WAITING_TOOL
                            && machine.energy().getAmount() == starting, "Missing catalyst blocks before spending inputs or power");
                    machine.container().setItem(slot, tool);
                }
                long supplied=0;
                for(int tick=0;tick<recipe.time();tick++) {
                    if(machine.energy().getAmount()<machine.form().profile().lanePerTick(recipe.energyPerTick())) {
                        long refill=machine.energy().getCapacity()-machine.energy().getAmount();
                        ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());supplied+=refill;
                    }
                    ticks(h,machine,1);
                }
                h.assertTrue(machine.lane(0) == null, entry.id() + " did not finish: " + machine.status());
                h.assertTrue(starting + supplied - machine.energy().getAmount() == (long) recipe.time() * machine.form().profile().lanePerTick(recipe.energyPerTick()), entry.id() + " exact paid work");
                for (int i = 0; i < recipe.fluidResults().size(); i++) {
                    var result = recipe.fluidResults().get(i);
                    var tank = machine.tanks().output(recipe.resultTank(i));
                    h.assertTrue(tank.variant.isOf(result.fluid()) && tank.millibuckets() == result.amount(), entry.id() + " fluid " + i);
                }
                for (int i = 0; i < recipe.results().size(); i++) {
                    ItemStack result = recipe.results().get(i).create();
                    ItemStack held = machine.container().getItem(machine.form().firstOutputSlot() + i);
                    h.assertTrue(ItemStack.isSameItemSameComponents(result, held) && held.getCount() == result.getCount(), "Exact solid result");
                }
                if (recipe.tool().isPresent()) {
                    ItemStack held = machine.container().getItem(machine.form().firstSocketSlot());
                    h.assertTrue(held.getCount() == 1 && recipe.tool().get().test(held), "Reusable catalyst retained");
                }
                h.assertTrue(machine.processEmissions() == recipe.emissions(), "Emissions once on completion");
                ticks(h, machine, 3);
                h.assertTrue(machine.processEmissions() == recipe.emissions(), "Idle never repeats emissions");
                clear(h, machine);
                checked++;
            }
        }
        h.assertTrue(checked == 12, "All twelve consolidated process recipes exercised, found " + checked);
        h.succeed();
    }



    @GameTest(structure=ARENA)
    public void originalSavedBatchesAndSlotsSurviveTheSharedForms(GameTestHelper h) {
        var oldBlocks=List.of(IndustrialForms.BRINE_CONCENTRATOR,IndustrialForms.ACID_CONCENTRATOR,
            IndustrialForms.GAS_CLEANING_RACK,IndustrialForms.BIOGAS_CLEANER,
            IndustrialForms.ALUMINATE_FILTER_PRESS,IndustrialForms.DIGESTATE_SEPARATOR,
            IndustrialForms.ALUMINA_CALCINER,IndustrialForms.CHLORIDE_DRYER,IndustrialForms.PTFE_SINTERING_OVEN);
        String[] names={"concentrate_coastal_seawater","concentrate_sulfuric_acid","rack_clean_synthesis_gas","clean_biogas",
            "recover_aluminate","separate_digestate","calcine_hydroxide","dry_magnesium_chloride","sinter_ptfe_seal"};
        for(int i=0;i<oldBlocks.size();i++) {
            var m=place(h,oldBlocks.get(i));var r=recipe(m,h,names[i]);feed(m,r);
            long before=m.energy().getAmount();ticks(h,m,40);
            h.assertTrue(m.lane(0)!=null,"Original process started: "+names[i]);
            var save=m.saveWithFullMetadata(h.getLevel().registryAccess());
            save.putInt("form_version",1);save.remove("tank_oven_solution");
            // This is the old save schema: no oven input tank; old named output and item indices.
            m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),save));
            h.assertTrue(m.lane(0)!=null,"Escrow survives reload");
            ticks(h,m,r.time()-40);
            h.assertTrue(m.lane(0)==null,"Old batch completes: "+names[i]);
            h.assertTrue(before-m.energy().getAmount()==(long)r.time()*r.energyPerTick(),"No duplicate or unpaid work");
            for(int n=0;n<r.results().size();n++) {
                var expected=r.results().get(n).create();var actual=m.container().getItem(m.form().firstOutputSlot()+n);
                h.assertTrue(ItemStack.isSameItemSameComponents(expected,actual)&&actual.getCount()==expected.getCount(),"Old solid slot preserved");
            }
            for(int n=0;n<r.fluidResults().size();n++) {
                var expected=r.fluidResults().get(n);var tank=m.tanks().output(r.resultTank(n));
                h.assertTrue(tank.variant.isOf(expected.fluid())&&tank.millibuckets()==expected.amount(),"Old output role preserved");
            }
            ticks(h,m,5);
            var completed=m.saveWithFullMetadata(h.getLevel().registryAccess());
            completed.remove("tank_oven_solution");m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),completed));
            for(int n=0;n<r.fluidResults().size();n++) h.assertTrue(m.tanks().output(r.resultTank(n)).millibuckets()==r.fluidResults().get(n).amount(),"Stored product survives reload and cannot repeat");
            clear(h,m);
        }
        h.succeed();
    }

    @GameTest(structure=ARENA)
    public void changingJobsRequiresCompatibleOutputsAndAnEmptyUnusedFeed(GameTestHelper h) {
        var m=place(h,IndustrialForms.BRINE_CONCENTRATOR);var acid=recipe(m,h,"concentrate_sulfuric_acid");feed(m,acid);
        m.tanks().output(0).fill(PetroFluids.MAGNESIUM_BRINE.source(),250);
        long before=m.energy().getAmount();ticks(h,m,5);
        h.assertTrue(m.lane(0)==null&&m.energy().getAmount()==before&&m.tanks().input(0).millibuckets()==1000,"Old brine blocks acid without losing feed");
        m.tanks().output(0).drain(250);ticks(h,m,acid.time());
        h.assertTrue(m.tanks().output(0).variant.isOf(PetroFluids.CONCENTRATED_SULFURIC_ACID.source()),"Same concentrator runs acid after clearing previous output");clear(h,m);
        m=place(h,IndustrialForms.ALUMINA_CALCINER);var sinter=recipe(m,h,"sinter_ptfe_seal");feed(m,sinter);
        m.tanks().input(0).fill(PetroFluids.MAGNESIUM_CHLORIDE_SOLUTION.source(),500);before=m.energy().getAmount();ticks(h,m,5);
        h.assertTrue(m.lane(0)==null&&m.energy().getAmount()==before&&m.status().reason()==MachineStatus.Reason.MIXED_INPUTS,"Mixed solution and solid jobs cannot silently select a recipe");
        m.tanks().input(0).drain(500);ticks(h,m,sinter.time());
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()).is(PetroItems.PTFE_SEAL),"Shared oven sinters when incompatible feed is cleared");clear(h,m);h.succeed();
    }
    @GameTest(structure=ARENA)
    public void oldOvenStoresKeepOutputsFeedAndUpgradeSlots(GameTestHelper h) {
        var m=place(h,IndustrialForms.ALUMINA_CALCINER);
        m.container().setItem(0,new ItemStack(PetroItems.ALUMINUM_HYDROXIDE,7));
        var alumina=BuiltInRegistries.ITEM.getValue(Jugcraft.id("alumina"));
        m.container().setItem(1,new ItemStack(alumina,9));
        m.container().setItem(2,new ItemStack(MachineUpgrades.SPEED,2));
        m.container().setItem(3,new ItemStack(MachineUpgrades.EFFICIENCY,3));
        m.tanks().output(0).fill(Fluids.WATER,375);m.togglePause();
        var tag=m.saveWithFullMetadata(h.getLevel().registryAccess());
        tag.putInt("form_version",1);tag.remove("tank_oven_solution");
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
        h.assertTrue(m.container().getItem(0).is(PetroItems.ALUMINUM_HYDROXIDE)&&m.container().getItem(0).getCount()==7,"Old feed slot stays input");
        h.assertTrue(m.container().getItem(1).is(alumina)&&m.container().getItem(1).getCount()==9,"Old output slot stays output");
        h.assertTrue(m.form().firstUpgradeSlot()==2&&m.container().getItem(2).is(MachineUpgrades.SPEED)&&m.container().getItem(2).getCount()==2,"Old speed cards retained");
        h.assertTrue(m.container().getItem(3).is(MachineUpgrades.EFFICIENCY)&&m.container().getItem(3).getCount()==3,"Old efficiency cards retained");
        h.assertTrue(m.tanks().input(0).millibuckets()==0&&m.tanks().output(0).millibuckets()==375&&m.tanks().output(0).variant.isOf(Fluids.WATER),"Old condensate stays output, new solution tank empty");
        h.assertTrue(m.paused(),"Paused state retained");clear(h,m);h.succeed();
    }

    @GameTest(structure = ARENA)
    public void recipeLockPersistsAndNeverFallsBack(GameTestHelper h) {
        var m = place(h, IndustrialForms.ALUMINA_CALCINER);
        var all = m.recipes(h.getLevel().getServer());
        var locked = all.stream().filter(e -> e.id().getPath().endsWith("sinter_ptfe_seal")).findFirst().orElseThrow();
        var wrong = recipe(m,h,"calcine_hydroxide");
        h.assertTrue(m.selectRecipe(locked.id()),"Supported job can be locked before feeding");
        h.assertTrue(!m.selectRecipe(Jugcraft.id("not_a_recipe")) && locked.id().equals(m.recipeLock()),"Invalid job cannot clear the lock");
        feed(m,wrong); ticks(h,m,3);
        h.assertTrue(m.lane(0)==null && m.container().getItem(0).getCount()==wrong.items().getFirst().count(),"Lock must not spend wrong inputs");
        var saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(locked.id().equals(m.recipeLock()),"Saved lock survives reload");
        m.container().setItem(0,ItemStack.EMPTY);
        feed(m,locked.recipe()); ticks(h,m,1);
        h.assertTrue(m.lane(0)!=null && m.lane(0).recipe().equals(locked.id()),"Only selected job starts");
        clear(h,m);h.succeed();
    }

    @GameTest(structure = ARENA)
    public void changingRecipePreservesPaidBatchesAndAutomationFilters(GameTestHelper h) {
        var m=place(h,IndustrialForms.ALUMINA_CALCINER);
        var all=m.recipes(h.getLevel().getServer());
        var old=all.stream().filter(e->e.id().getPath().endsWith("calcine_hydroxide")).findFirst().orElseThrow();
        var next=all.stream().filter(e->e.id().getPath().endsWith("sinter_ptfe_seal")).findFirst().orElseThrow();
        m.selectRecipe(old.id());feed(m,old.recipe());ticks(h,m,3);
        var lane=m.lane(0);int paid=lane.progress();
        h.assertTrue(m.selectRecipe(next.id()) && m.lane(0)==lane && lane.progress()==paid,"Changing selection preserves paid batch");
        h.assertTrue(!m.container().canPlaceItem(0,new ItemStack(PetroItems.ALUMINUM_HYDROXIDE)),"Hoppers reject nonselected feed");
        var inputPort=m.form().ports().stream().filter(p->p.kind()==FormPort.Kind.FLUID_IN).findFirst().orElseThrow();
        var solution=recipe(m,h,"dry_magnesium_chloride").fluids().getFirst().fluid();
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            long inserted=m.fluidPort(inputPort).insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(solution),81000,tx);
            h.assertTrue(inserted==0,"Fluid automation rejects a different recipe's feed");
            tx.commit();
        }
        ticks(h,m,old.recipe().time()+1);
        h.assertTrue(m.lane(0)==null && m.container().getItem(m.form().firstOutputSlot()).getCount()==old.recipe().results().getFirst().create().getCount(),"Original batch finishes exactly once after selection changes");
        h.assertTrue(m.selectRecipe(null) && m.recipeLock()==null,"Automatic is explicit and reversible");
        h.assertTrue(m.container().canPlaceItem(0,new ItemStack(PetroItems.ALUMINUM_HYDROXIDE)),"Automatic restores broad feed acceptance");
        clear(h,m);h.succeed();
    }

    @GameTest(structure = ARENA)
    public void removedRecipeStaysLockedAndOldSavesStayAutomatic(GameTestHelper h) {
        var m=place(h,IndustrialForms.BRINE_CONCENTRATOR);
        var saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putString("recipe_lock","jugcraft:removed_by_datapack");
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        feed(m,recipe(m,h,"concentrate_coastal_seawater"));ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.status().reason()==MachineStatus.Reason.RECIPE_UNAVAILABLE,"Missing recipe cannot silently unlock or choose another job");
        saved.remove("recipe_lock");
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(m.recipeLock()==null,"Old saves default to Automatic");clear(h,m);h.succeed();
    }

    @GameTest(structure = ARENA)
    public void menuRejectsRemoteInvalidAndStaleSelection(GameTestHelper h) {
        var m=place(h,IndustrialForms.BRINE_CONCENTRATOR);
        var player=h.makeMockServerPlayerInLevel();
        player.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY(),m.getBlockPos().getZ()+.5);
        var menu=(FormMachineMenu)m.createMenu(7,player.getInventory(),player);player.containerMenu=menu;
        h.assertTrue(!menu.clickMenuButton(player,1000000) && m.recipeLock()==null,"Forged index is rejected");
        h.assertTrue(menu.clickMenuButton(player,FormMachineMenu.BUTTON_RECIPE),"Valid open nearby menu selects a job");
        h.assertTrue(!menu.clickMenuButton(player,FormMachineMenu.BUTTON_AUTO),"Same-tick repeated selection is bounded");
        var other=(FormMachineMenu)m.createMenu(8,player.getInventory(),player);
        h.assertTrue(other.selectedRecipe()==1,"Second viewer sees the shared selected recipe");
        h.assertTrue(!other.clickMenuButton(player,FormMachineMenu.BUTTON_AUTO),"Unopened menu cannot mutate machine");
        player.setPos(m.getBlockPos().getX()+100,m.getBlockPos().getY(),m.getBlockPos().getZ());
        h.assertTrue(!menu.clickMenuButton(player,FormMachineMenu.BUTTON_AUTO),"Remote player cannot change selection");
        // Exercise the exact recipe-cache invalidation performed by the datapack reload callback.
        try {
            var invalidate=io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes.class.getDeclaredMethod("clearCache");
            invalidate.setAccessible(true);invalidate.invoke(null);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
        h.assertTrue(menu.catalogStale(),"Open menu detects changed server recipe catalog");
        player.setPos(m.getBlockPos().getX()+.5,m.getBlockPos().getY(),m.getBlockPos().getZ()+.5);
        h.assertTrue(!menu.clickMenuButton(player,FormMachineMenu.BUTTON_AUTO),"Stale menu cannot change selection");
        player.containerMenu=player.inventoryMenu;clear(h,m);h.succeed();
    }

    private static FormMachineBlockEntity place(GameTestHelper h, FormMachineBlock block) {
        var state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
        h.setBlock(AT, state);
        block.setPlacedBy(h.getLevel(), h.absolutePos(AT), state, null, ItemStack.EMPTY);
        FormMachineBlockEntity machine = h.getBlockEntity(AT, FormMachineBlockEntity.class);
        ((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
        return machine;
    }
    private static FluidRecipe recipe(FormMachineBlockEntity m, GameTestHelper h, String name) {
        return m.recipes(h.getLevel().getServer()).stream().filter(e -> e.id().getPath().endsWith(name)).findFirst().orElseThrow().recipe();
    }
    private static void feed(FormMachineBlockEntity m, FluidRecipe r) {
        for (int i = 0; i < r.items().size(); i++) m.container().setItem(i, new ItemStack(r.items().get(i).ingredient().items().findFirst().orElseThrow(), r.items().get(i).count()));
        for (int i = 0; i < r.fluids().size(); i++) m.tanks().input(i).fill(r.fluids().get(i).fluid(), r.fluids().get(i).amount());
        r.tool().ifPresent(tool -> m.container().setItem(m.form().firstSocketSlot(), new ItemStack(tool.items().findFirst().orElseThrow())));
    }
    private static void ticks(GameTestHelper h, FormMachineBlockEntity m, int count) {
        for (int i = 0; i < count; i++) m.serverTick(h.getLevel(), m.getBlockPos(), h.getLevel().getBlockState(m.getBlockPos()));
    }
    private static void clear(GameTestHelper h, FormMachineBlockEntity m) {
        var form = m.form();
        var facing = m.getBlockState().getValue(MachineBlock.FACING);
        // No live world survives this disposable fixture. Clear peers first without neighbour callbacks.
        for (int i = form.footprint().size()-1; i >= 0; i--) h.getLevel().setBlock(form.footprint().partPos(m.getBlockPos(), facing, i), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}
