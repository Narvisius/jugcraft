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
public class FluorochemistryGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.ACID_CONCENTRATOR, IndustrialForms.CHEMICAL_DISSOLUTION_CHAMBER, IndustrialForms.FLUOROCARBON_REACTOR, IndustrialForms.FLUOROMONOMER_CRACKER, IndustrialForms.PTFE_SINTERING_OVEN, IndustrialForms.LINED_FLUOROCHEMICAL_REACTOR);}
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
        h.assertTrue(checked >= 10, "All new process and molding recipes exercised, found " + checked);
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
    public void dissolutionRequiresConcentrateAndRetainsEveryProduct(GameTestHelper h) {
        var m=place(h,IndustrialForms.CHEMICAL_DISSOLUTION_CHAMBER);
        var r=recipe(m,h,"dissolve_fluorite");feed(m,r);
        m.tanks().input(0).drain(750);m.tanks().input(0).fill(PetroFluids.SULFURIC_ACID.source(),750);
        long energy=m.energy().getAmount();ticks(h,m,3);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.container().getItem(0).getCount()==1,"Ordinary acid cannot bypass concentration");
        m.tanks().input(0).drain(750);m.tanks().input(0).fill(PetroFluids.CONCENTRATED_SULFURIC_ACID.source(),750);
        m.container().setItem(m.form().firstOutputSlot(),new ItemStack(PetroItems.CALCIUM_SULFATE,64));ticks(h,m,3);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Full gypsum drawer blocks before spending");
        m.container().setItem(m.form().firstOutputSlot(),ItemStack.EMPTY);
        m.tanks().output(0).fill(PetroFluids.HYDROGEN_FLUORIDE.fluid(),2000);ticks(h,m,3);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Full HF tank blocks; gas is not vented");
        m.tanks().output(0).drain(2000);ticks(h,m,80);
        var tag=m.saveWithFullMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));ticks(h,m,160);
        h.assertTrue(m.tanks().output(0).millibuckets()==500 && m.container().getItem(m.form().firstOutputSlot()).getCount()==1,"HF and residue exactly once after reload");
        h.assertTrue(energy-m.energy().getAmount()==19200 && m.container().getItem(0).isEmpty(),"Dissolution pays once");
        clear(h,m);h.succeed();
    }

    @GameTest(structure=ARENA)
    public void expandedReactorReservesTwoFullBatchesAcrossSave(GameTestHelper h) {
        var m=place(h,IndustrialForms.LINED_FLUOROCHEMICAL_REACTOR);
        var r=recipe(m,h,"prepare_chloroform");feed(m,r);feed(m,r);
        h.assertTrue(m.form().profile()==OperatingProfile.EXPANDED && m.tanks().input(0).capacityMb()==8000,"Expanded tanks and two-lane profile");
        long energy=m.energy().getAmount();ticks(h,m,75);
        h.assertTrue(m.lane(0)!=null && m.lane(1)!=null && m.tanks().input(0).millibuckets()==0 && m.tanks().input(1).millibuckets()==0,"Two complete receipts reserved");
        var tag=m.saveWithFullMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));ticks(h,m,165);
        h.assertTrue(m.lane(0)==null && m.lane(1)==null,"Both restored lanes finish");
        h.assertTrue(m.tanks().output(0).millibuckets()==1000 && m.tanks().output(1).millibuckets()==3000,"Both useful outputs doubled, not lost");
        h.assertTrue(energy-m.energy().getAmount()==27840,"Two lanes each pay ceil(64 * .9) for 240 ticks");
        clear(h,m);h.succeed();
    }

    @GameTest(structure=ARENA)
    public void ptfeCannotSkipPastePreparationOrSintering(GameTestHelper h) {
        var m=place(h,IndustrialForms.POLYMER_EXTRUDER);
        var r=recipe(m,h,"extrude_ptfe_liner");feed(m,r);
        m.container().setItem(0,new ItemStack(PetroItems.PTFE_POWDER,4));
        long energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"Dry powder is not ordinary melt-extrusion stock");
        m.container().setItem(0,new ItemStack(PetroItems.PTFE_PASTE_CHARGE,4));
        m.container().setItem(m.form().firstOutputSlot()+1,new ItemStack(PetroItems.PTFE_OFFCUT,64));ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy,"PTFE offcut storage reserved before work");
        m.container().setItem(m.form().firstOutputSlot()+1,ItemStack.EMPTY);ticks(h,m,200);
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()).is(PetroItems.PTFE_LINER_BLANK),"Extrusion yields a blank, not a finished liner");
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()+1).getCount()==1,"One clean allocated offcut");
        clear(h,m);m=place(h,IndustrialForms.POLYMER_RECLAIMER);
        for(var item:List.of(PetroItems.PTFE_SEAL,PetroItems.PTFE_FITTING,PetroItems.PTFE_LINER)) {
            m.container().setItem(0,new ItemStack(item,4));ticks(h,m,2);
            h.assertTrue(m.lane(0)==null,"Finished sintered parts are not clean unsintered scrap");
        }
        clear(h,m);m=place(h,IndustrialForms.POLYMER_MOLDING_PRESS);
        m.container().setItem(0,new ItemStack(PetroItems.PTFE_POWDER,4));
        m.container().setItem(m.form().firstSocketSlot(),new ItemStack(PetroItems.HOUSING_MOLD));ticks(h,m,2);
        h.assertTrue(m.lane(0)==null,"PTFE is not generic rigid plastic");
        clear(h,m);h.succeed();
    }

    @GameTest(structure=ARENA)
    public void fluoriteLootAndSnowBiomePlacementLoad(GameTestHelper h) {
        var level=h.getLevel();var material=io.github.jimbozoomer.jugcraft.materials.JugcraftMaterials.FLUORITE;
        ItemStack plain=new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        ItemStack silk=plain.copy();
        silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
        for(var ore:List.of(material.ore,material.deepslateOre)) {
            var mined=Block.getDrops(ore.defaultBlockState(),level,h.absolutePos(AT),null,null,plain);
            h.assertTrue(mined.size()==1 && mined.getFirst().is(material.item) && mined.getFirst().getCount()>=1 && mined.getFirst().getCount()<=2,"Fluorite ore supplies the chemistry route");
            var silked=Block.getDrops(ore.defaultBlockState(),level,h.absolutePos(AT),null,null,silk);
            h.assertTrue(silked.size()==1 && silked.getFirst().is(ore.asItem()),"Silk Touch preserves ore");
        }
        var biomes=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        for(String name:List.of("fluorite")) {
            var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,Jugcraft.id("has_ore/"+name));
            h.assertTrue(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS).is(tag),"Snow plains eligible");
            h.assertTrue(!biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT).is(tag),"Desert excluded");
            level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,Jugcraft.id("ore_"+name)));
        }
        h.succeed();
    }

    @GameTest(structure=ARENA)
    public void constructionRecipesResolveWithoutConflictingWithEarlierStations(GameTestHelper h) {
        String[] patterns={"TBTWCWBMB","BTBPMPWCK","PTBWCMBKT","BPBWGWMCT","BGBWCWPKP","SFLUCUMKM"};
        var ingredients=java.util.Map.ofEntries(java.util.Map.entry('T',"steel_tank"),java.util.Map.entry('B',"minecraft:bricks"),java.util.Map.entry('W',"copper_cable"),java.util.Map.entry('C',"basic_circuit"),java.util.Map.entry('M',"electric_motor"),java.util.Map.entry('P',"steel_plate"),java.util.Map.entry('K',"machine_casing"),java.util.Map.entry('G',"steel_gear"),java.util.Map.entry('S',"ptfe_seal"),java.util.Map.entry('F',"ptfe_fitting"),java.util.Map.entry('L',"ptfe_liner"),java.util.Map.entry('U',"titanium_ingot"));
        for(int i=0;i<patterns.length;i++) {
            var stacks=new java.util.ArrayList<ItemStack>();
            for(char symbol:patterns[i].toCharArray()) {
                String id=ingredients.get(symbol);if(!id.contains(":"))id="jugcraft:"+id;
                stacks.add(new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id))));
            }
            var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
            var check=net.minecraft.world.item.crafting.RecipeManager.createCheck(net.minecraft.world.item.crafting.RecipeType.CRAFTING);
            var result=check.getRecipeFor(input,h.getLevel());
            h.assertTrue(result.isPresent() && result.get().value().assemble(input).is(blocks().get(i).asItem()),"Unique construction for "+blocks().get(i).form().id());
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
