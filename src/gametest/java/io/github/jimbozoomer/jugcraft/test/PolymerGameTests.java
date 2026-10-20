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

/** End-to-end batch ledgers, overflow accounting, persistence and reachable construction. */
public class PolymerGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.MONOMER_PREPARATION_UNIT, IndustrialForms.POLYMERIZATION_KETTLE, IndustrialForms.POLYMER_EXTRUDER, IndustrialForms.POLYMER_RECLAIMER);}
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
        h.assertTrue(checked == 23, "All new process and molding recipes exercised, found " + checked);
        h.succeed();
    }


    @GameTest(structure = ARENA)
    public void portsStayAttachedToTheRightPartInAllOrientations(GameTestHelper h) {
        for (FormMachineBlock block : blocks()) {
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
                var state = block.defaultBlockState().setValue(MachineBlock.FACING, facing);
                h.setBlock(AT, state);
                block.setPlacedBy(h.getLevel(), h.absolutePos(AT), state, null, ItemStack.EMPTY);
                FormMachineBlockEntity m = h.getBlockEntity(AT, FormMachineBlockEntity.class);
                ticks(h, m, 1);
                h.assertTrue(m.status().state() != MachineLifecycle.UNFORMED, "Complete structure " + block.form().id() + " " + facing);
                for (int part = 0; part < m.form().footprint().size(); part++) {
                    BlockPos pos = m.form().footprint().partPos(m.getBlockPos(), facing, part);
                    for (Direction face : Direction.values()) {
                        FormPort port = m.form().port(part, FormSide.of(face, facing));
                        h.assertTrue((FluidStorage.SIDED.find(h.getLevel(), pos, face) != null) == (port != null && port.kind().fluid()), "Fluid face " + facing + "/" + part + "/" + face);
                        h.assertTrue((ItemStorage.SIDED.find(h.getLevel(), pos, face) != null) == (port != null && (port.kind() == FormPort.Kind.ITEM_IN || port.kind() == FormPort.Kind.ITEM_OUT)), "Item face excludes tool sockets");
                        h.assertTrue((EnergyStorage.SIDED.find(h.getLevel(), pos, face) != null) == (port != null && port.kind().energy()), "Power face");
                    }
                }
                clear(h, m);
            }
        }
        h.succeed();
    }

    @GameTest(structure=ARENA)
    public void diesOutputsAndPersistenceAreAuthoritative(GameTestHelper h) {
        var m=place(h,IndustrialForms.POLYMER_EXTRUDER);
        var r=recipe(m,h,"extrude_sheet");feed(m,r);
        m.container().setItem(m.form().firstSocketSlot(),new ItemStack(PetroItems.FILM_EXTRUSION_DIE));
        long energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.container().getItem(0).getCount()==4,"Wrong die cannot spend HDPE");
        m.container().setItem(m.form().firstSocketSlot(),new ItemStack(PetroItems.SHEET_EXTRUSION_DIE));
        m.container().setItem(m.form().firstOutputSlot(),new ItemStack(PetroItems.PLASTIC_SHEET,64));ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Full sheet drawer prevents spending");
        m.container().setItem(m.form().firstOutputSlot(),ItemStack.EMPTY);ticks(h,m,65);
        CompoundTag tag=m.saveWithFullMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));ticks(h,m,95);
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()).getCount()==8,"Eight sheets once after reload");
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()+1).isEmpty(),"Full-yield sheets cannot also create recyclable offcuts");
        h.assertTrue(m.container().getItem(m.form().firstSocketSlot()).is(PetroItems.SHEET_EXTRUSION_DIE),"Die retained");
        h.assertTrue(energy-m.energy().getAmount()==3840,"Fully paid across reload");
        clear(h,m);h.succeed();
    }
    @GameTest(structure=ARENA)
    public void gradesStayDistinctAndExistingPressAcceptsHdpe(GameTestHelper h) {
        var m=place(h,IndustrialForms.POLYMER_MOLDING_PRESS);
        m.container().setItem(0,new ItemStack(PetroItems.LDPE_PELLETS,4));
        m.container().setItem(m.form().firstSocketSlot(),new ItemStack(PetroItems.HOUSING_MOLD));
        long energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Flexible LDPE cannot become rigid housing");
        m.container().setItem(0,new ItemStack(PetroItems.HDPE_PELLETS,4));ticks(h,m,120);
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()).is(PetroItems.POLYMER_HOUSING),"HDPE works in original press");
        clear(h,m);m=place(h,IndustrialForms.POLYMER_RECLAIMER);
        m.container().setItem(0,new ItemStack(PetroItems.PLASTIC_SHEET,4));energy=m.energy().getAmount();ticks(h,m,125);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Unknown-grade legacy sheets cannot become clean HDPE");
        m.container().setItem(0,new ItemStack(PetroItems.PVC_OFFCUT,4));ticks(h,m,120);
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()).is(PetroItems.PVC_RESIN) && m.container().getItem(m.form().firstOutputSlot()).getCount()==1,"PVC stays PVC with 75 percent loss");
        clear(h,m);h.succeed();
    }
    @GameTest(structure=ARENA)
    public void constructionRecipesHaveIndependentEntryPaths(GameTestHelper h) {
        String[][] bills={
            {"minecraft:bricks", "jugcraft:steel_tank", "minecraft:bricks", "jugcraft:copper_cable", "jugcraft:basic_circuit", "jugcraft:copper_cable", "jugcraft:electric_motor", "jugcraft:machine_casing", "jugcraft:electric_motor"},
            {"jugcraft:steel_tank", "jugcraft:steel_gear", "jugcraft:steel_tank", "minecraft:bricks", "jugcraft:basic_circuit", "minecraft:bricks", "jugcraft:electric_motor", "jugcraft:machine_casing", "jugcraft:electric_motor"},
            {"jugcraft:steel_gear", "jugcraft:steel_plate", "jugcraft:steel_gear", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:electric_motor", "jugcraft:copper_cable", "jugcraft:machine_casing", "jugcraft:copper_cable"},
            {"jugcraft:steel_gear", "jugcraft:steel_plate", "jugcraft:steel_gear", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:electric_motor", "minecraft:bricks", "jugcraft:machine_casing", "minecraft:bricks"}
        };
        for(int i=0;i<bills.length;i++) {
            var stacks=java.util.Arrays.stream(bills[i]).map(id->new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)))).toList();
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
            var check=net.minecraft.world.item.crafting.RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            var result=check.getRecipeFor(input,h.getLevel());
            h.assertTrue(result.isPresent() && result.get().value().assemble(input).is(blocks().get(i).asItem()),"Unique reachable construction: "+blocks().get(i).form().id());
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
