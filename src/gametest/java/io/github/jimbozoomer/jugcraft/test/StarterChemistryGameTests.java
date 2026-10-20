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

/** Real controllers and loaded recipes: atomic gas/solid output, tool retention, saved policies and energy ledgers. */
public class StarterChemistryGameTests {
    private static final String ARENA = "jugcraft-test:arms_arena";
    private static final BlockPos AT = new BlockPos(7, 1, 7);
    private static List<FormMachineBlock> blocks() {
        return List.of(IndustrialForms.CHEMICAL_INFUSER, IndustrialForms.CHEMICAL_OXIDIZER,
                IndustrialForms.COAL_GASIFIER, IndustrialForms.GAS_CLEANING_RACK);
    }

    @GameTest(structure = ARENA)
    public void everyLoadedRecipePaysItsLedgerAndRetainsItsTool(GameTestHelper h) {
        int checked = 0;
        for (FormMachineBlock block : blocks()) {
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
                // Longer upgrading batches need a connected supply, not just one full internal buffer.
                long supplied = 0;
                for (int tick = 0; tick < recipe.time(); tick++) {
                    if (machine.energy().getAmount() < (recipe.energyPerTick() > 0 ? recipe.energyPerTick() : machine.form().family().usePerTick)) {
                        long refill = machine.energy().getCapacity() - machine.energy().getAmount();
                        supplied += refill;
                        ((SimpleEnergyStorage) machine.energy()).setAmount(machine.energy().getCapacity());
                    }
                    ticks(h, machine, 1);
                }
                h.assertTrue(machine.lane(0) == null, entry.id() + " did not finish: " + machine.status());
                h.assertTrue(starting + supplied - machine.energy().getAmount() == (long) recipe.time() * machine.form().profile().lanePerTick(recipe.energyPerTick() > 0 ? recipe.energyPerTick() : machine.form().family().usePerTick), entry.id() + " exact paid work including supplied power");
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
        h.assertTrue(checked >= 24, "Gas processes including all shared scrubber feeds exercised, found " + checked);
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void waterDrainIsExplicitSavedPerBatchAndNeverDiscardsOtherFluids(GameTestHelper h) {
        FormMachineBlockEntity m = place(h, IndustrialForms.CHEMICAL_INFUSER);
        FluidRecipe recipe = recipe(m, h, "infuser_co_methanation");
        feed(m, recipe);
        m.tanks().output(1).fill(Fluids.WATER, 1950);
        ticks(h, m, 1);
        h.assertTrue(m.lane(0) != null && m.lane(0).drainWater(), "Default batch drains excess water");
        m.toggleWaterDrain();
        ticks(h, m, 199);
        h.assertTrue(m.tanks().output(1).millibuckets() == 2000 && m.drainedWater() == 150, "Collects 50, drains exactly 150 despite toggle during batch");
        feed(m, recipe);
        long energy = m.energy().getAmount();
        ticks(h, m, 3);
        h.assertTrue(m.lane(0) == null && m.status().state() == MachineLifecycle.OUTPUT_BLOCKED && m.energy().getAmount() == energy,
                "Keep water blocks before consuming anything");
        m.tanks().output(1).drain(2000);
        m.tanks().output(1).fill(PetroFluids.LYE.source(), 1);
        m.toggleWaterDrain();
        ticks(h, m, 3);
        h.assertTrue(m.lane(0) == null && m.tanks().output(1).millibuckets() == 1
                && m.tanks().input(0).millibuckets() == recipe.fluids().get(0).amount()
                && m.tanks().input(1).millibuckets() == recipe.fluids().get(1).amount(),
                "Drain cannot discard lye or begin with wrong fluid");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void fullGasAndResidueOutputsBlockWithoutSpendingInputs(GameTestHelper h) {
        FormMachineBlockEntity m = place(h, IndustrialForms.GAS_CLEANING_RACK);
        var r = recipe(m, h, "rack_clean_synthesis_gas");
        feed(m, r);
        m.container().setItem(m.form().firstOutputSlot(), new ItemStack(PetroItems.MIXED_GAS_RESIDUE, 64));
        long start = m.energy().getAmount();
        ticks(h, m, 1);
        h.assertTrue(m.lane(0) == null && m.energy().getAmount() == start && m.tanks().input(0).millibuckets() == 500,
                "Full residue drawer blocks with inputs untouched");
        m.container().setItem(m.form().firstOutputSlot(), ItemStack.EMPTY);
        m.tanks().output(1).fill(PetroFluids.HYDROGEN.fluid(), 1800);
        ticks(h, m, 1);
        h.assertTrue(m.lane(0) == null && m.tanks().input(1).millibuckets() == 125, "Full gas outlet also blocks");
        m.tanks().output(1).drain(1800);
        ticks(h, m, 100);
        h.assertTrue(m.tanks().output(0).millibuckets() == 250 && m.tanks().output(1).millibuckets() == 250,
                "Both products appear together after unblocking");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void aRestartKeepsTheCostToolAndExcessWaterPolicy(GameTestHelper h) {
        FormMachineBlockEntity m = place(h, IndustrialForms.CHEMICAL_INFUSER);
        feed(m, recipe(m, h, "infuser_co2_methanation"));
        m.tanks().output(1).fill(Fluids.WATER, 2000);
        ticks(h, m, 73);
        h.assertTrue(m.lane(0) != null && m.lane(0).progress() == 73, "Partly paid batch");
        CompoundTag saved = m.saveWithoutMetadata(h.getLevel().registryAccess());
        // Reload the same world controller, as a chunk reload does; no synthetic unregistered block entity.
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
        h.assertTrue(m.lane(0).energyPerTick() == 128 && m.lane(0).drainWater() && m.lane(0).progress() == 73,
                "Cost, progress and water policy survive save");
        ticks(h, m, 127);
        h.assertTrue(m.lane(0) == null && m.tanks().output(0).millibuckets() == 100 && m.drainedWater() == 400,
                "Exact results after reload");
        h.assertTrue(m.energy().getAmount() == m.energy().getCapacity() - 25_600, "No refunded or repeated energy");
        h.assertTrue(m.container().getItem(m.form().firstSocketSlot()).is(PetroItems.NICKEL_CATALYST_BED), "Same bed retained");
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void cancellationReturnsCarbonAndWaterButNoGasOrEmissions(GameTestHelper h) {
        FormMachineBlockEntity m = place(h, IndustrialForms.COAL_GASIFIER);
        feed(m, recipe(m, h, "expanded_gasifier_coal"));
        ticks(h, m, 90);
        m.cancel();
        h.assertTrue(m.lane(0) == null && m.container().getItem(0).getCount() == 1 && m.tanks().input(0).millibuckets() == 1000,
                "Untransformed carbon and water returned once");
        h.assertTrue(m.tanks().output(0).millibuckets() == 0 && m.processEmissions() == 0, "No unearned result");
        h.assertTrue(m.energy().getAmount() == m.energy().getCapacity() - 90 * 58, "Spent heat is not refunded");
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
                        h.assertTrue((EnergyStorage.SIDED.find(h.getLevel(), pos, face) != null) == (port != null && port.kind() == FormPort.Kind.ENERGY_IN), "Power face");
                    }
                }
                clear(h, m);
            }
        }
        h.succeed();
    }

    @GameTest(structure = ARENA)
    public void methaneFuelRetainsFractionalCreditAndCannotLoopElectrolysis(GameTestHelper h) {
        FormMachineBlockEntity m = place(h, IndustrialForms.GAS_BURNING_GENERATOR);
        ((SimpleEnergyStorage) m.energy()).setAmount(0);
        m.tanks().input(0).fill(PetroFluids.METHANE.fluid(), 3);
        ticks(h, m, 1);
        h.assertTrue(m.energy().getAmount() == 256 && m.burn() == 192, "First mB: 256 output and 192 saved credit");
        CompoundTag saved = m.saveWithoutMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
        ticks(h, m, 6);
        h.assertTrue(m.energy().getAmount() == 1344 && m.burn() == 0 && m.tanks().input(0).millibuckets() == 0,
                "Three mB always make exactly 3 * 448 JE across reload");
        for (int speed = 0; speed <= 4; speed++) {
            for (int efficiency = 0; efficiency <= 4; efficiency++) {
                var effect = new MachineUpgrades.Effect(speed, efficiency);
                long hydrogenCost = new MachineUpgrades.Effect(0,4).use(MachineKind.ELECTROLYTIC_CELL.usePerTick) * 800 / 500;
                h.assertTrue(300 * hydrogenCost + effect.use(128) * effect.ticks(200) > 100 * 448,
                        "CO methanation cannot profit from electrolytic H2 with any upgrade combination");
            }
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
        m.selectRecipe(m.recipes(m.getLevel().getServer()).stream().filter(e -> e.recipe() == r).findFirst().orElseThrow().id());
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
