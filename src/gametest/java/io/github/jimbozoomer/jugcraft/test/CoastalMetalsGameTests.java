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
public class CoastalMetalsGameTests {
    private static final String ARENA="jugcraft-test:arms_arena";
    private static final BlockPos AT=new BlockPos(7,1,7);
    private static List<FormMachineBlock> blocks(){return List.of(IndustrialForms.COASTAL_SEAWATER_INTAKE, IndustrialForms.BRINE_CONCENTRATOR, IndustrialForms.MINERAL_CONDITIONER, IndustrialForms.CHLORIDE_DRYER, IndustrialForms.MAGNESIUM_ELECTROLYSIS_CELL, IndustrialForms.TITANIUM_CHLORINATOR, IndustrialForms.TITANIUM_REDUCTION_RETORT, IndustrialForms.POLYMER_MOLDING_PRESS);}
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
        h.assertTrue(checked >= 22, "Coastal, molding and shared concentrator recipes exercised, found " + checked);
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
    public void coastalSourceIsPaidBoundedAndChecksTheActualWater(GameTestHelper h) {
        var level=h.getLevel();
        BlockPos at=h.absolutePos(AT).atY(CoastalSeawaterIntake.surfaceY(level));
        for(int x=-16;x<=16;x+=16) for(int z=-16;z<=16;z+=16) level.getChunkAt(at.offset(x,0,z));
        biome(h,at,"minecraft:ocean");
        var block=IndustrialForms.COASTAL_SEAWATER_INTAKE;
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            var state=block.defaultBlockState().setValue(MachineBlock.FACING,facing);
            level.setBlock(at,state,Block.UPDATE_ALL);block.setPlacedBy(level,at,state,null,ItemStack.EMPTY);
            var m=(FormMachineBlockEntity)level.getBlockEntity(at);
            ((SimpleEnergyStorage)m.energy()).setAmount(2000);
            BlockPos water=CoastalSeawaterIntake.source(m);
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
                for(int d=0;d<2;d++) level.setBlock(water.offset(x,-d,z),Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS);
                for(int y=1;y<8;y++) level.setBlock(water.offset(x,y,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
            }
            h.assertTrue(CoastalSeawaterIntake.validSource(level,water),"Open coastal source " + facing + " " + sourceDetails(level,water));
            var result=CoastalSeawaterIntake.pump(m,level);
            h.assertTrue(result.equals(MachineStatus.PROCESSING) && m.tanks().output(0).millibuckets()==200 && m.energy().getAmount()==1680,"Exactly one paid stroke");
            h.assertTrue(level.getFluidState(water).isSource(),"Shoreline retained");
            m.togglePause();m.serverTick(level,at,state);
            h.assertTrue(m.tanks().output(0).millibuckets()==200 && m.energy().getAmount()==1680,"Pause stops collection");
            m.tanks().output(0).fill(PetroFluids.SEAWATER.source(),1800);
            h.assertTrue(CoastalSeawaterIntake.pump(m,level).state()==MachineLifecycle.OUTPUT_BLOCKED && m.energy().getAmount()==1680,"Full storage costs nothing");
            m.tanks().output(0).drain(2000);((SimpleEnergyStorage)m.energy()).setAmount(0);
            h.assertTrue(CoastalSeawaterIntake.pump(m,level).state()==MachineLifecycle.WAITING_ENERGY && m.tanks().output(0).millibuckets()==0,"No unpaid seawater");
            h.assertTrue(!CoastalSeawaterIntake.validSource(level,water.above()),"Elevated pool rejected");
            level.setBlock(water.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
            h.assertTrue(!CoastalSeawaterIntake.validSource(level,water),"Shallow bucket pool rejected");
            level.setBlock(water.below(),Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS);
            level.setBlock(water.above(2),Blocks.STONE.defaultBlockState(),Block.UPDATE_CLIENTS);
            h.assertTrue(!CoastalSeawaterIntake.validSource(level,water),"Covered intake rejected");
            level.setBlock(water.above(2),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS);
            biome(h,at,"minecraft:plains");
            h.assertTrue(!CoastalSeawaterIntake.validSource(level,water),"Inland freshwater rejected");
            biome(h,at,"minecraft:ocean");
            clear(h,m);
        }
        h.succeed();
    }

    @GameTest(structure=ARENA)
    public void wrongMoldAndBlockedSaltDoNotSpendInputsAndReloadPaysOnce(GameTestHelper h) {
        var m=place(h,IndustrialForms.POLYMER_MOLDING_PRESS);
        var r=recipe(m,h,"mold_housing");feed(m,r);
        m.container().setItem(m.form().firstSocketSlot(),new ItemStack(PetroItems.HOSE_MOLD));
        long energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.container().getItem(0).getCount()==4,"Wrong reusable mold cannot spend feed");
        clear(h,m);
        m=place(h,IndustrialForms.TITANIUM_REDUCTION_RETORT);r=recipe(m,h,"reduce_and_clean_titanium_sponge");feed(m,r);
        m.container().setItem(m.form().firstOutputSlot()+1,new ItemStack(PetroItems.DRY_MAGNESIUM_CHLORIDE,64));
        energy=m.energy().getAmount();ticks(h,m,2);
        h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.container().getItem(0).getCount()==2,"Blocked recovered salt stops before magnesium is consumed");
        m.container().setItem(m.form().firstOutputSlot()+1,ItemStack.EMPTY);ticks(h,m,150);
        CompoundTag tag=m.saveWithFullMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),tag));
        ticks(h,m,250);
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()).is(PetroItems.TITANIUM_SPONGE) && m.container().getItem(m.form().firstOutputSlot()).getCount()==1,"One clean sponge after reload");
        h.assertTrue(m.container().getItem(m.form().firstOutputSlot()+1).getCount()==2 && m.container().getItem(0).isEmpty(),"Salt recovered once, no extra magnesium");
        h.assertTrue(energy-m.energy().getAmount()==38400,"Cleanup and reduction fully paid across reload");
        h.succeed();
    }

    @GameTest(structure=ARENA)
    public void freshwaterAndSaltBrineCannotStartMagnesium(GameTestHelper h) {
        for(var fluid:List.of(Fluids.WATER,PetroFluids.BRINE.source())) {
            var m=place(h,IndustrialForms.BRINE_CONCENTRATOR);m.tanks().input(0).fill(fluid,1000);
            long energy=m.energy().getAmount();ticks(h,m,210);
            h.assertTrue(m.lane(0)==null && m.energy().getAmount()==energy && m.tanks().output(0).millibuckets()==0,"No magnesium from generic water/brine");clear(h,m);
        }
        h.succeed();
    }

    private static String sourceDetails(net.minecraft.server.level.ServerLevel level,BlockPos water) {
        StringBuilder text=new StringBuilder("dimension="+level.dimension()+" sea="+level.getSeaLevel()+" source="+water);
        for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) {
            BlockPos p=water.offset(x,0,z);
            text.append(" [").append(x).append(',').append(z).append(" biome=").append(level.getBiome(p).unwrapKey())
                .append(" sky=").append(level.canSeeSky(p.above())).append(" fluid=").append(level.getFluidState(p))
                .append(" below=").append(level.getFluidState(p.below())).append(']');
        }
        return text.toString();
    }

    private static void biome(GameTestHelper h,BlockPos at,String biome) {
        h.getLevel().getServer().getCommands().performPrefixedCommand(h.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(),
                "fillbiome %d %d %d %d %d %d %s".formatted(at.getX()-16,at.getY()-4,at.getZ()-16,at.getX()+16,at.getY()+8,at.getZ()+16,biome));
    }

    @GameTest(structure=ARENA)
    public void constructionUsesReachableInputsAndMatchesRealRecipeManager(GameTestHelper h) {
        String[][] bills={
            {"jugcraft:steel_plate", "jugcraft:steel_gear", "jugcraft:steel_plate", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:steel_tank", "jugcraft:copper_cable", "jugcraft:machine_casing", "jugcraft:copper_cable"},
            {"minecraft:bricks", "jugcraft:steel_tank", "minecraft:bricks", "jugcraft:steel_tank", "jugcraft:basic_circuit", "jugcraft:steel_tank", "jugcraft:copper_cable", "jugcraft:electric_motor", "jugcraft:copper_cable"},
            {"jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:steel_plate", "jugcraft:basic_circuit", "jugcraft:electric_motor", "jugcraft:basic_circuit", "minecraft:bricks", "jugcraft:machine_casing", "minecraft:bricks"},
            {"minecraft:bricks", "jugcraft:steel_gear", "minecraft:bricks", "jugcraft:steel_tank", "jugcraft:basic_circuit", "jugcraft:steel_tank", "jugcraft:copper_cable", "jugcraft:machine_casing", "jugcraft:copper_cable"},
            {"jugcraft:copper_cable", "minecraft:bricks", "jugcraft:copper_cable", "minecraft:bricks", "jugcraft:machine_casing", "minecraft:bricks", "jugcraft:steel_plate", "jugcraft:basic_circuit", "jugcraft:steel_plate"},
            {"minecraft:bricks", "jugcraft:steel_tank", "jugcraft:steel_plate", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:electric_motor", "jugcraft:copper_cable", "jugcraft:machine_casing", "jugcraft:copper_cable"},
            {"jugcraft:steel_plate", "minecraft:bricks", "jugcraft:steel_plate", "jugcraft:steel_tank", "jugcraft:basic_circuit", "jugcraft:steel_tank", "jugcraft:electric_motor", "jugcraft:machine_casing", "jugcraft:electric_motor"},
            {"jugcraft:steel_plate", "jugcraft:housing_mold", "jugcraft:steel_plate", "jugcraft:electric_motor", "jugcraft:basic_circuit", "jugcraft:electric_motor", "minecraft:bricks", "jugcraft:machine_casing", "minecraft:bricks"},
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
