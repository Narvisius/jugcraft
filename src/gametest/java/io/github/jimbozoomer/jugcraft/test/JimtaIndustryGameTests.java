package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.jimtafacts.industry.JimtaIndustry;
import io.github.jimbozoomer.jugcraft.machine.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;

/** Real native-machine batches and component persistence; no supplied binaries are executed. */
public class JimtaIndustryGameTests {
    private static MachineBlockEntity machine(GameTestHelper h, String id) {
        h.setBlock(new BlockPos(1,1,1),JimtaIndustry.MACHINES.get(id));
        return (MachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(1,1,1)));
    }
    private static void tick(GameTestHelper h, MachineBlockEntity m, int count) {
        var power=(SimpleEnergyStorage)m.energyFor(null);
        for(int i=0;i<count;i++) { power.setAmount(power.getCapacity());m.serverTick(h.getLevel(),m.getBlockPos(),m.getBlockState()); }
    }
    private static ItemStack item(String id,int count) { return new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(id)),count); }

    @GameTest public void compressorRequiresFullBatchAndConsumesExactlyThatBatch(GameTestHelper h) {
        var m=machine(h,"jimta_compressor");m.setItem(0,new ItemStack(Items.IRON_INGOT,8));tick(h,m,240);
        h.assertTrue(m.getItem(0).getCount()==8 && m.getItem(1).isEmpty(),"Partial batch cannot make an iron block");
        m.setItem(0,new ItemStack(Items.IRON_INGOT,10));tick(h,m,200);
        h.assertTrue(m.getItem(0).getCount()==1 && m.getItem(1).is(Items.IRON_BLOCK) && m.getItem(1).getCount()==1,"Exactly nine ingots consumed");
        tick(h,m,200);h.assertTrue(m.getItem(0).getCount()==1 && m.getItem(1).getCount()==1,"Remainder cannot duplicate another output");h.succeed();
    }
    @GameTest public void extractorReturnsBottlesAndBlocksWhenRemainderSlotsAreFull(GameTestHelper h) {
        var m=machine(h,"jimta_extractor");m.setItem(0,item("latex_bottle",1));m.setItem(2,new ItemStack(Items.STONE,64));m.setItem(3,new ItemStack(Items.STONE,64));
        tick(h,m,240);h.assertTrue(m.getItem(0).getCount()==1 && m.getItem(1).isEmpty(),"No bottle loss when both remainder slots are blocked");
        m.setItem(2,ItemStack.EMPTY);tick(h,m,200);
        h.assertTrue(m.getItem(0).isEmpty() && m.getItem(1).is(item("rubber",1).getItem()) && m.getItem(1).getCount()==3,"Three rubber per latex bottle");
        h.assertTrue(m.getItem(2).is(Items.GLASS_BOTTLE) && m.getItem(2).getCount()==1,"Bottle returned exactly once");h.succeed();
    }
    @GameTest public void centrifugeHeatsBeforeProcessingAndPersistsItsHeat(GameTestHelper h) {
        var m=machine(h,"jimta_thermal_centrifuge");m.setItem(0,item("washed_iron_ore",9));tick(h,m,100);
        h.assertTrue(m.getItem(0).getCount()==9 && m.getItem(1).isEmpty() && m.processingProgress()==0,"Warming does not consume input or fake process progress");
        var saved=m.saveWithFullMetadata(h.getLevel().registryAccess());
        int heat=saved.getInt("industrialHeat").orElse(0);h.assertTrue(heat>0 && heat<1500,"Source iron temperature not yet reached");
        m.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(m.saveWithFullMetadata(h.getLevel().registryAccess()).getInt("industrialHeat").orElse(-1)==heat,"Heat persists through reload");
        tick(h,m,1300);h.assertTrue(m.getItem(0).isEmpty() && m.getItem(1).getCount()==9,"One nine-ore batch completed");
        h.assertTrue(m.getItem(2).is(item("gold_dust",1).getItem()) && m.getItem(2).getCount()==1,"Source tiny-gold recovery consolidated to one shared dust");h.succeed();
    }
    @GameTest public void compactMaceratorUsesNativeRecipesMenusAndAutomation(GameTestHelper h) {
        var m=machine(h,"jimta_macerator");h.assertTrue(m.kind()==MachineKind.CRUSHER,"Shares crusher system");
        h.assertTrue(((MachineBlock)m.getBlockState().getBlock()).footprint(m.getBlockState()).size()==1,"Original compact geometry");
        h.assertTrue(ItemStorage.SIDED.find(h.getLevel(),m.getBlockPos(),Direction.UP)!=null,"Native sided automation");
        m.setItem(0,new ItemStack(Items.COAL));tick(h,m,160);
        h.assertTrue(m.getItem(0).isEmpty() && m.getItem(1).is(JimtaIndustry.MATERIALS.get("jimta_coal_dust")),"Native recipe manager processes imported carbon chain");h.succeed();
    }
}
