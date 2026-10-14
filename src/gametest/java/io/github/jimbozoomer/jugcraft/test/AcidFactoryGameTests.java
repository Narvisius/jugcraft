package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.chemistry.*;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids;
import io.github.jimbozoomer.jugcraft.machine.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;

public class AcidFactoryGameTests {
    private static final BlockPos AT = new BlockPos(4, 1, 4);
    private static final String ARENA = "jugcraft-test:arms_arena";
    private MachineBlockEntity place(GameTestHelper h) {
        var block = JugcraftMachines.MACHINES.get(MachineKind.CHEMICAL_REACTOR);
        var state = block.defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH);
        h.setBlock(AT, state);
        block.setPlacedBy(h.getLevel(),h.absolutePos(AT),state,null,ItemStack.EMPTY);
        var m = h.getBlockEntity(AT,MachineBlockEntity.class);
        ((SimpleEnergyStorage)m.energyFor(null)).setAmount(30000);
        return m;
    }
    private void tick(GameTestHelper h, MachineBlockEntity m, int n) {
        for(int i=0;i<n;i++) m.serverTick(h.getLevel(),m.getBlockPos(),m.getBlockState());
    }
    @GameTest(structure=ARENA)
    public void absorptionRequiresPaidCarrierAndRetainsItAcrossBatchesAndReload(GameTestHelper h) {
        var m=place(h); var t=m.tanks();
        t.input(0).fill(Fluids.WATER,500);t.input(1).fill(PetroFluids.SULFUR_TRIOXIDE.fluid(),500);
        tick(h,m,120);
        h.assertTrue(t.output(0).millibuckets()==0 && m.energyFor(null).getAmount()==30000,"Missing carrier spends nothing");
        try(var tx=Transaction.openOuter()) {
            h.assertTrue(m.fluidFor(Direction.UP).insert(FluidVariant.of(PetroFluids.SULFURIC_ACID.source()),81000,tx)==81000,"Charge via top");tx.commit();
        }
        tick(h,m,60);
        CompoundTag saved=m.saveWithoutMetadata(h.getLevel().registryAccess());
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        tick(h,m,140);
        h.assertTrue(t.output(0).has(PetroFluids.SULFURIC_ACID.source(),1000),"Exactly two batches across reload");
        h.assertTrue(t.output(0).millibuckets()==1000 && t.input(2).millibuckets()==1000,"Carrier not added to output or consumed");
        h.assertTrue(t.input(0).millibuckets()==0 && t.input(1).millibuckets()==0,"Paid both feeds");
        h.assertTrue(m.energyFor(null).getAmount()==10800,"Exactly 200 * 96 JE");
        try(var tx=Transaction.openOuter()) {
            h.assertTrue(t.input(2).extract(FluidVariant.of(PetroFluids.SULFURIC_ACID.source()),81000,tx)==0,"Automation cannot steal carrier");
        }
        var drops=Block.getDrops(m.getBlockState(),h.getLevel(),m.getBlockPos(),m);
        h.assertTrue(drops.size()==1 && drops.getFirst().has(JugcraftFluids.STORED_FLUID),"Reactor drop preserves carrier");
        t.input(2).drain(1000);m.applyComponentsFromItemStack(drops.getFirst());
        h.assertTrue(t.input(2).millibuckets()==1000,"Placement restores one carrier charge");h.succeed();
    }
    @GameTest(structure=ARENA)
    public void hydrochloricAbsorptionBlocksAtFullOutputThenFeedsCopperRecovery(GameTestHelper h) {
        var m=place(h);var t=m.tanks();
        t.input(0).fill(Fluids.WATER,500);t.input(1).fill(PetroFluids.HYDROGEN_CHLORIDE.fluid(),500);
        t.output(0).fill(PetroFluids.HYDROCHLORIC_ACID.source(),7500);tick(h,m,110);
        h.assertTrue(m.energyFor(null).getAmount()==30000 && t.input(0).millibuckets()==500,"Full output consumes nothing");
        t.output(0).drain(7500);tick(h,m,100);
        h.assertTrue(t.output(0).millibuckets()==1000 && t.input(2).millibuckets()==0,"HCl needs no carrier");
        t.output(0).drain(1000);t.input(0).fill(PetroFluids.HYDROCHLORIC_ACID.source(),250);
        m.setItem(0,new ItemStack(net.minecraft.world.item.Items.RAW_COPPER));tick(h,m,160);
        h.assertTrue(m.getItem(2).getCount()==1 && t.input(0).millibuckets()==0,"Copper recovery conserves raw-metal units");h.succeed();
    }
    @GameTest(structure=ARENA)
    public void legacyReactorTankLayoutMigratesOutputWithoutInventingCarrier(GameTestHelper h) {
        var m=place(h);var old=new FluidTanks(new FluidMachineSpec(java.util.List.of(8000),java.util.List.of(8000),2,1),(i,v)->true,()->{});
        old.input(0).fill(Fluids.WATER,725);old.output(0).fill(PetroFluids.SULFURIC_ACID.source(),1435);
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        old.save(out);m.tanks().load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(m.tanks().input(0).millibuckets()==725 && m.tanks().output(0).millibuckets()==1435,"Old inputs and outputs stay in correct roles");
        h.assertTrue(m.tanks().input(1).millibuckets()==0 && m.tanks().input(2).millibuckets()==0,"No free gas or acid carrier");h.succeed();
    }
}
