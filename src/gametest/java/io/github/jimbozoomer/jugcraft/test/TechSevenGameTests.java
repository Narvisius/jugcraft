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
public class TechSevenGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.PRECISION_FINISHER,IndustrialForms.WET_PROCESSING_BENCH,IndustrialForms.MINERAL_CONDITIONER,IndustrialForms.ALUMINA_CALCINER,IndustrialForms.CHEMICAL_DISSOLUTION_CHAMBER,IndustrialForms.ALUMINATE_FILTER_PRESS,IndustrialForms.CHEMICAL_INFUSER,IndustrialForms.CHEMICAL_DISTILLATION_STATION,IndustrialForms.BULK_CHEMICAL_REACTOR,IndustrialForms.ELECTROLYTIC_SEPARATOR,IndustrialForms.BULK_ELECTROLYTIC_SEPARATOR,IndustrialForms.BULK_CHEMICAL_INFUSER,IndustrialForms.BULK_CHEMICAL_OXIDIZER,IndustrialForms.BULK_FERMENTER);}
    @GameTest(structure = ARENA)
    public void sevenPackagesPayExactInputsEnergyOutputsAndTools(GameTestHelper h) {
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
                machine.selectRecipe(entry.id());
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
                long perTick=machine.form().profile().lanePerTick(recipe.energyPerTick()>0 ? recipe.energyPerTick() : machine.form().family().usePerTick);
                long supplied=0;
                for(int tick=0;tick<recipe.time();tick++) {
                    if(machine.energy().getAmount()<perTick) {
                        long refill=machine.energy().getCapacity()-machine.energy().getAmount();
                        ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());supplied+=refill;
                    }
                    ticks(h,machine,1);
                }
                h.assertTrue(machine.lane(0) == null, entry.id() + " did not finish: " + machine.status());
                h.assertTrue(starting + supplied - machine.energy().getAmount() == (long) recipe.time() * perTick, entry.id() + " exact paid work");
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
        h.assertTrue(checked >= 40, "Seven-package shared process recipes exercised, found " + checked);
        h.succeed();
    }
    @GameTest(structure=ARENA)
    public void batteryBudgetsRollbackAndRespectModesAndReserve(GameTestHelper h) {
        long[] tick={0}; int[] mode={0}, reserve={0}, limit={100}; boolean[] available={true};
        var b=new io.github.jimbozoomer.jugcraft.energy.BatteryEnergyStorage(10000,()->{},()->10000,
            ()->tick[0],()->1000,()->mode[0],()->reserve[0],()->1,()->limit[0],()->available[0]);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(b.insert(800,tx)==800,"Probe accepts charge");
        }
        h.assertTrue(b.getAmount()==0,"Aborted charge restores storage");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(b.insert(800,tx)==800 && b.insert(800,tx)==200,"All faces share the tick budget after rollback");tx.commit();
        }
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(b.insert(1,tx)==0,"Committed tick budget cannot be spent again");tx.commit();
        }
        tick[0]++;b.setAmount(6000);reserve[0]=50;limit[0]=50;b.tickPolicy();
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(b.extract(5000,tx)==500,"Output percentage limits discharge");
        }
        h.assertTrue(b.getAmount()==6000,"Aborted extraction restores charge");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(b.extract(5000,tx)==500,"Aborted extraction restores rate budget");tx.commit();
        }
        tick[0]++;b.setAmount(5100);b.tickPolicy();limit[0]=100;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            h.assertTrue(b.extract(1000,tx)==100,"Minimum charge is protected");tx.commit();
        }
        for(int m=0;m<4;m++) {mode[0]=m;
            h.assertTrue(b.supportsInsertion()==(m==0||m==1) && b.supportsExtraction()==(m==0||m==2),"Charge/discharge/stop modes");}
        available[0]=false;mode[0]=0;
        h.assertTrue(!b.supportsInsertion()&&!b.supportsExtraction(),"Detached installation freezes transfer");h.succeed();
    }

    @GameTest(structure=ARENA)
    public void automationTargetsPersistAndCardRemovalDoesNotEraseRules(GameTestHelper h) {
        var m=place(h,IndustrialForms.PRECISION_FINISHER);
        var e=m.recipes(h.getLevel().getServer()).stream().filter(r->r.id().getPath().endsWith("finish_alumina_plate")).findFirst().orElseThrow();
        h.assertTrue(!m.cycleAutomation(e.id(),false),"No card cannot edit");
        int slot=m.form().firstUpgradeSlot();m.container().setItem(slot,new ItemStack(MachineUpgrades.AUTOMATION));
        h.assertTrue(m.cycleAutomation(e.id(),false)&&m.cycleAutomation(e.id(),true),"Card enables rules");
        m.selectRecipe(e.id());feed(m,e.recipe());
        m.container().setItem(m.form().firstOutputSlot(),new ItemStack(PetroItems.ALUMINA_PLATE,16));
        long energy=m.energy().getAmount();ticks(h,m,3);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Stock target prevents spending another batch");
        var save=m.saveWithFullMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),save));
        h.assertTrue(m.stockTarget(e.id())==16 && m.recipePriority(e.id())==1 && e.id().equals(m.recipeLock()),"Reload retains priority target and lock");
        m.container().setItem(slot,ItemStack.EMPTY);ticks(h,m,1);
        h.assertTrue(m.lane(0)!=null && m.automationRuleCount()==1,"Removing card suspends saved target but permits production");
        clear(h,m);h.succeed();
    }

    @GameTest(structure=ARENA)
    public void cryogenicToolsGateWorkAndSurviveReload(GameTestHelper h) {
        for(boolean hydrogen:List.of(false,true)) {
            var kind=MachineKind.CRYOGENIC_LIQUEFIER;
            var block=io.github.jimbozoomer.jugcraft.machine.JugcraftMachines.MACHINES.get(kind);
            var state=block.defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH);
            h.setBlock(AT,state);block.setPlacedBy(h.getLevel(),h.absolutePos(AT),state,null,ItemStack.EMPTY);
            var m=h.getBlockEntity(AT,io.github.jimbozoomer.jugcraft.machine.MachineBlockEntity.class);
            var energy=(SimpleEnergyStorage)m.energyFor(null);energy.setAmount(kind.capacity);
            var gas=hydrogen?PetroFluids.HYDROGEN.fluid():PetroFluids.METHANE.fluid();
            var liquid=hydrogen?PetroFluids.LIQUID_HYDROGEN.source():PetroFluids.LIQUID_METHANE.source();
            var tool=hydrogen?PetroItems.HYDROGEN_COOLING_MODULE:PetroItems.METHANE_COOLING_MODULE;
            m.tanks().input(0).fill(gas,1000);
            for(int n=0;n<5;n++)m.serverTick(h.getLevel(),m.getBlockPos(),state);
            h.assertTrue(energy.getAmount()==kind.capacity && m.tanks().input(0).millibuckets()==1000,"Missing cooling module cannot spend inputs or power");
            if(hydrogen) {
                m.setItem(kind.slots,new ItemStack(PetroItems.METHANE_COOLING_MODULE));
                for(int n=0;n<5;n++)m.serverTick(h.getLevel(),m.getBlockPos(),state);
                h.assertTrue(energy.getAmount()==kind.capacity,"Methane tooling cannot process hydrogen");
            }
            m.setItem(kind.slots,new ItemStack(tool));
            for(int n=0;n<50;n++)m.serverTick(h.getLevel(),m.getBlockPos(),state);
            h.assertTrue(energy.getAmount()==kind.capacity-50L*kind.usePerTick,"Tool permits paid progress");
            m.setItem(kind.slots,ItemStack.EMPTY);
            for(int n=0;n<5;n++)m.serverTick(h.getLevel(),m.getBlockPos(),state);
            h.assertTrue(energy.getAmount()==kind.capacity-50L*kind.usePerTick && m.tanks().input(0).millibuckets()==1000,"Removing tool stops work without consuming feed");
            m.setItem(kind.slots,new ItemStack(tool));int duration=hydrogen?320:160;
            for(int n=0;n<duration-1;n++)m.serverTick(h.getLevel(),m.getBlockPos(),state);
            h.assertTrue(m.tanks().output(0).millibuckets()==0,"Removed tooling cannot retain prior progress");
            m.serverTick(h.getLevel(),m.getBlockPos(),state);
            h.assertTrue(m.tanks().input(0).millibuckets()==0 && m.tanks().output(0).millibuckets()==250 && m.tanks().output(0).variant.isOf(liquid),"Exactly 4 gas becomes 1 liquid");
            h.assertTrue(energy.getAmount()==kind.capacity-(50L+duration)*kind.usePerTick,"Every working tick was paid");
            var saved=m.saveWithFullMetadata(h.getLevel().registryAccess());
            m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            h.assertTrue(m.getItem(kind.slots).is(tool) && m.tanks().output(0).millibuckets()==250,"Reload preserves tool and product");
            h.setBlock(AT,Blocks.AIR);
        }
        h.succeed();
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
