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

/** Farm feed is the finite energy source; water and residue cannot be looped into biogas. */
public class AnaerobicGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.ANAEROBIC_DIGESTER,IndustrialForms.BIOGAS_CLEANER,IndustrialForms.DIGESTATE_SEPARATOR);}
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
                long supplied = 0;
                for (int tick = 0; tick < recipe.time(); tick++) {
                    long draw = machine.form().profile().lanePerTick(recipe.energyPerTick() > 0 ? recipe.energyPerTick() : machine.form().family().usePerTick);
                    if (machine.energy().getAmount() < draw) {
                        supplied += machine.energy().getCapacity() - machine.energy().getAmount();
                        ((SimpleEnergyStorage)machine.energy()).setAmount(machine.energy().getCapacity());
                    }
                    ticks(h, machine, 1);
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
        h.assertTrue(checked == 6, "All legacy and expanded digestion/cleanup processes exercised, found " + checked);
        h.succeed();
    }


    @GameTest(structure=ARENA)
    public void twoDigesterLanesSurviveReloadAndKeepBothProducts(GameTestHelper h){
        var m=place(h,IndustrialForms.ANAEROBIC_DIGESTER);
        m.selectRecipe(m.recipes(h.getLevel().getServer()).stream().filter(e -> e.id().getPath().endsWith("digest_farm_material")).findFirst().orElseThrow().id());
        m.container().setItem(0,new ItemStack(net.minecraft.world.item.Items.WHEAT,32));
        m.tanks().input(0).fill(Fluids.WATER,2000);
        long initial=m.energy().getAmount();ticks(h,m,173);
        h.assertTrue(m.lane(0)!=null && m.lane(1)!=null,"Two independently paid lanes");
        var saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        ticks(h,m,227);
        h.assertTrue(m.tanks().output(0).millibuckets()==2000 && m.tanks().output(1).millibuckets()==1500,"Products once across save");
        h.assertTrue(m.container().getItem(0).isEmpty() && m.tanks().input(0).millibuckets()==0,"Exactly 32 crops and 2 buckets consumed");
        h.assertTrue(initial-m.energy().getAmount()==20800,"Two lanes each pay 400 ticks at 26 JE/t in the retained bulk profile");
        h.succeed();
    }
    @GameTest(structure=ARENA)
    public void fullDigestateBlocksAndCancellationReturnsOnlyRawFeed(GameTestHelper h){
        var m=place(h,IndustrialForms.ANAEROBIC_DIGESTER);
        feed(m,recipe(m,h,"digest_farm_material"));m.tanks().output(1).fill(PetroFluids.DIGESTATE.source(),16000);
        long energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.container().getItem(0).getCount()==16,"No work or feed spent with blocked digestate");
        m.tanks().output(1).drain(16000);ticks(h,m,20);m.cancel();
        h.assertTrue(m.container().getItem(0).getCount()==16 && m.tanks().input(0).millibuckets()==1000,"Cancel restores feed");
        h.assertTrue(m.tanks().output(0).millibuckets()==0 && m.tanks().output(1).millibuckets()==0,"Cancel cannot award gas or fertilizer");
        m.container().setItem(0,new ItemStack(PetroItems.FERTILIZER,16));ticks(h,m,3);
        h.assertTrue(m.lane(0)==null,"Fertilizer is not digestible feed");h.succeed();
    }
    @GameTest(structure=ARENA)
    public void bioGeneratorAcceptsOnlyItsFuelsAndSavesFuelCredit(GameTestHelper h){
        var m=place(h,IndustrialForms.BIO_GENERATOR);((SimpleEnergyStorage)m.energy()).setAmount(0);
        m.tanks().input(0).fill(PetroFluids.RAW_BIOGAS.fluid(),3);ticks(h,m,2);
        h.assertTrue(m.energy().getAmount()==0 && m.tanks().input(0).millibuckets()==3,"Uncleaned biogas is rejected");
        m.tanks().input(0).drain(3);m.tanks().input(0).fill(PetroFluids.CLEAN_BIOGAS.fluid(),3);ticks(h,m,1);
        h.assertTrue(m.energy().getAmount()==64 && m.burn()==204,"Lower 64 JE/t rate with fractional credit");
        var save=m.saveWithoutMetadata(h.getLevel().registryAccess());m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),save));ticks(h,m,8);
        h.assertTrue(m.energy().getAmount()==576 && m.burn()==228 && m.tanks().input(0).millibuckets()==0,"Credit carries across restart");
        ticks(h,m,4);h.assertTrue(m.energy().getAmount()==804 && m.burn()==0,"Exactly 268 JE/mB, the same as ordinary biogas");
        m.tanks().input(0).fill(PetroFluids.BIOETHANOL.source(),1);((SimpleEnergyStorage)m.energy()).setAmount(m.energy().getCapacity());ticks(h,m,3);
        h.assertTrue(m.tanks().input(0).millibuckets()==1,"No fuel spent when full");
        ((SimpleEnergyStorage)m.energy()).setAmount(0);ticks(h,m,3);h.assertTrue(m.energy().getAmount()==192,"Existing bioethanol works");h.succeed();
    }
    @GameTest(structure=ARENA)
    public void wholePlantProductsAndEnergyHaveNoWaterFuelLoop(GameTestHelper h){
        long digestion=400*29, cleaning=100*24, separation=100*16;
        h.assertTrue(268000-digestion-cleaning-separation==252400,"Usable power from 16 consumed farm items");
        h.assertTrue(252400/16 > (192000-38400)/32,"Advanced plant improves over legacy fermentation per crop");
        long hydrogenCost=new MachineUpgrades.Effect(0,4).use(MachineKind.ELECTROLYTIC_CELL.usePerTick)*800/500;
        for(int speed=0;speed<=4;speed++)for(int efficiency=0;efficiency<=4;efficiency++){
            var e=new MachineUpgrades.Effect(speed,efficiency);
            h.assertTrue(268000+1600*hydrogenCost+e.use(128)*e.ticks(600)>448000,"Biogas plus electrolytic H2 cannot create a positive electrical upgrade loop");
        }
        var m=place(h,IndustrialForms.ANAEROBIC_DIGESTER);m.tanks().input(0).fill(Fluids.WATER,8000);ticks(h,m,10);
        h.assertTrue(m.lane(0)==null && m.lane(1)==null,"Water alone cannot start digestion");h.succeed();
    }
    @GameTest
    public void eachConstructionBillCraftsItsOwnMachine(GameTestHelper h){
        String[][] bills={
            {"steel_plate","steel_tank","steel_plate","steel_tank","electric_motor","steel_tank","steel_plate","basic_circuit","steel_plate"},
            {"steel_plate","fluid_tank","steel_plate","electric_motor","basic_circuit","machine_casing","steel_plate","steel_fluid_pipe","steel_plate"},
            {"steel_plate","fluid_tank","steel_plate","steel_gear","basic_circuit","electric_motor","steel_plate","machine_casing","steel_plate"},
            {"steel_plate","iron_gear","steel_plate","electric_motor","basic_circuit","fluid_tank","steel_plate","machine_casing","steel_plate"}};
        var expected=List.of(IndustrialForms.ANAEROBIC_DIGESTER,IndustrialForms.BIOGAS_CLEANER,IndustrialForms.DIGESTATE_SEPARATOR,IndustrialForms.BIO_GENERATOR);
        for(int i=0;i<bills.length;i++){
            var stacks=java.util.Arrays.stream(bills[i]).map(id->new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id)))).toList();
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
            var check=net.minecraft.world.item.crafting.RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            var result=check.getRecipeFor(input,h.getLevel());
            h.assertTrue(result.isPresent() && result.get().value().assemble(input).is(expected.get(i).asItem()),"Distinct reachable recipe for "+expected.get(i).form().id());
        }h.succeed();
    }
    @GameTest(structure = ARENA)
    public void portsStayAttachedToTheRightPartInAllOrientations(GameTestHelper h) {
        for (FormMachineBlock block : List.of(IndustrialForms.ANAEROBIC_DIGESTER, IndustrialForms.BIOGAS_CLEANER, IndustrialForms.DIGESTATE_SEPARATOR, IndustrialForms.BIO_GENERATOR)) {
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
    public void previewTankAliasesAndUpgradeSlotsMigrateOnce(GameTestHelper h) {
        var m=place(h,IndustrialForms.ANAEROBIC_DIGESTER);
        m.tanks().input(0).fill(Fluids.WATER,1234);
        m.tanks().output(0).fill(PetroFluids.RAW_BIOGAS.fluid(),567);
        m.container().setItem(1,new ItemStack(MachineUpgrades.SPEED));
        var saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.remove("inventory_layout");
        saved.put("tank_digestion_water",saved.get("tank_water")); saved.remove("tank_water");
        saved.put("tank_raw_biogas",saved.get("tank_biogas")); saved.remove("tank_biogas");
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(m.tanks().input(0).millibuckets()==1234 && m.tanks().output(0).millibuckets()==567,"Preview fluid amounts survive role conversion");
        h.assertTrue(m.container().getItem(1).isEmpty() && m.container().getItem(m.form().firstUpgradeSlot()).is(MachineUpgrades.SPEED),"Preview upgrade moves past the retained solid output");
        var migrated=m.saveWithoutMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),migrated));
        h.assertTrue(m.container().getItem(m.form().firstUpgradeSlot()).is(MachineUpgrades.SPEED) && m.container().getItem(m.form().firstUpgradeSlot()+1).isEmpty(),"Migration never shifts twice");
        h.assertTrue(m.form().profile().lanes()==4 && m.tanks().input(0).capacityMb()==16000,"Published bulk profile stays intact");
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
