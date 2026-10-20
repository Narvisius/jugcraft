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
public class BiorefineryGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.MASHING_STATION, IndustrialForms.INDUSTRIAL_FERMENTER, IndustrialForms.CHEMICAL_DISTILLATION_STATION, IndustrialForms.BAUXITE_DIGESTER, IndustrialForms.ALUMINATE_FILTER_PRESS, IndustrialForms.ALUMINA_CALCINER, IndustrialForms.ALUMINUM_REDUCTION_CELL);}
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
                long supplied=0;
                for(int tick=0;tick<recipe.time();tick++) {
                    if(machine.energy().getAmount()<machine.form().profile().lanePerTick(recipe.energyPerTick() > 0 ? recipe.energyPerTick() : machine.form().family().usePerTick)) {
                        long refill=machine.energy().getCapacity()-machine.energy().getAmount();
                        ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());supplied+=refill;
                    }
                    ticks(h,machine,1);
                }
                h.assertTrue(machine.lane(0) == null, entry.id() + " did not finish: " + machine.status());
                h.assertTrue(starting + supplied - machine.energy().getAmount() == (long) recipe.time() * machine.form().profile().lanePerTick(recipe.energyPerTick() > 0 ? recipe.energyPerTick() : machine.form().family().usePerTick), entry.id() + " exact paid work");
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
        h.assertTrue(checked >= 14, "Refinery stages plus consolidated filtering and oven recipes exercised, found " + checked);
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
    public void constructionUsesReachableInputsAndMatchesRealRecipeManager(GameTestHelper h) {
        String[][] bills={
            {"jugcraft:steel_plate", "jugcraft:steel_gear", "jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:electric_motor", "jugcraft:steel_tank", "jugcraft:steel_plate", "jugcraft:basic_circuit", "jugcraft:steel_plate"},
            {"jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:basic_circuit", "jugcraft:steel_tank", "jugcraft:steel_plate", "jugcraft:electric_motor", "jugcraft:steel_plate"},
            {"jugcraft:steel_tank", "jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:machine_casing", "jugcraft:copper_cable", "minecraft:bricks", "jugcraft:copper_cable"},
            {"jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:steel_plate", "minecraft:bricks", "jugcraft:electric_motor", "minecraft:bricks", "jugcraft:copper_cable", "jugcraft:basic_circuit", "jugcraft:copper_cable"},
            {"jugcraft:steel_plate", "jugcraft:steel_gear", "jugcraft:steel_plate", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:electric_motor", "jugcraft:steel_tank", "jugcraft:machine_casing", "jugcraft:steel_tank"},
            {"minecraft:bricks", "jugcraft:steel_plate", "minecraft:bricks", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:electric_motor", "jugcraft:copper_cable", "jugcraft:machine_casing", "jugcraft:copper_cable"},
            {"jugcraft:copper_cable", "minecraft:bricks", "jugcraft:copper_cable", "minecraft:bricks", "jugcraft:basic_circuit", "minecraft:bricks", "jugcraft:steel_plate", "jugcraft:machine_casing", "jugcraft:steel_plate"},
        };
        for(int i=0;i<bills.length;i++) {
            var stacks=java.util.Arrays.stream(bills[i]).map(id->new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)))).toList();
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
            var check=net.minecraft.world.item.crafting.RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            var result=check.getRecipeFor(input,h.getLevel());
            h.assertTrue(result.isPresent() && result.get().value().assemble(input).is(blocks().get(i).asItem()),"Unique reachable bill: "+blocks().get(i).form().id());
        }
        h.succeed();
    }
    @GameTest(structure=ARENA)
    public void gasIsCapturedOrVentedExactlyOnceAcrossReload(GameTestHelper h){
        for(var block:List.of(IndustrialForms.INDUSTRIAL_FERMENTER,IndustrialForms.ALUMINUM_REDUCTION_CELL)) {
            var m=place(h,block);var r=recipe(m,h,block==IndustrialForms.INDUSTRIAL_FERMENTER ? "ferment_crop_mash" : "reduce_alumina");
            int out=m.form().ventsAny()?1:m.form().waterDrainOutput();var result=r.fluidResults().get(out);
            m.tanks().output(out).fill(result.fluid(),1900);feed(m,r);ticks(h,m,80);
            h.assertTrue(m.lane(0)!=null,"Running work");
            var save=m.saveWithoutMetadata(h.getLevel().registryAccess());
            m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),save));
            if (!m.form().ventsAny()) m.toggleWaterDrain(); // Preview overflow policy is saved per batch; the legacy vent control applies live.
            ((SimpleEnergyStorage)m.energy()).setAmount(m.energy().getCapacity());
            if(r.time()>300) ticks(h,m,160);
            ((SimpleEnergyStorage)m.energy()).setAmount(m.energy().getCapacity());
            ticks(h,m,r.time()-80-(r.time()>300?160:0));
            h.assertTrue(m.lane(0)==null && m.tanks().output(out).millibuckets()==2000 && m.drainedWater()==150 && m.processEmissions()==150,"100 captured + 150 vented, once across save");
            ticks(h,m,5);h.assertTrue(m.drainedWater()==150,"Idle never repeats venting");
            if (m.form().ventsAny()) m.toggleHoldVented();
            feed(m,r);long energy=m.energy().getAmount();ticks(h,m,2);
            h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Keep gas blocks full output before consumption");
            m.tanks().output(out).drain(2000);ticks(h,m,30);m.cancel();
            h.assertTrue(m.tanks().output(out).millibuckets()==0 && m.drainedWater()==150,"Cancel never emits a product");
            for(int i=0;i<r.items().size();i++) h.assertTrue(m.container().getItem(i).getCount()==r.items().get(i).count(),"Cancelled carbon/solid inputs refunded once");
            for(int i=0;i<r.fluids().size();i++) h.assertTrue(m.tanks().input(i).millibuckets()==r.fluids().get(i).amount(),"Cancelled mash refunded");
            clear(h,m);
        }
        h.succeed();
    }
    @GameTest(structure=ARENA)
    public void residueAndWaterCannotProduceFuelAndBlockedSolidStopsRefining(GameTestHelper h){
        var m=place(h,IndustrialForms.MASHING_STATION);
        m.tanks().input(0).fill(Fluids.WATER,1000);m.container().setItem(0,new ItemStack(PetroItems.DIGESTATE_FIBER,16));
        ticks(h,m,300);h.assertTrue(m.lane(0)==null && m.tanks().output(0).millibuckets()==0,"No residue-to-ethanol loop");clear(h,m);
        m=place(h,IndustrialForms.ALUMINATE_FILTER_PRESS);var r=m.recipes(h.getLevel().getServer()).stream().filter(e -> e.id().getPath().endsWith("recover_aluminate")).findFirst().orElseThrow().recipe();feed(m,r);
        m.container().setItem(m.form().firstOutputSlot()+1,new ItemStack(PetroItems.BAUXITE_RESIDUE,64));
        long energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.tanks().input(0).millibuckets()==1000,"Blocked mud drawer cannot lose slurry or make lye");h.succeed();
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
