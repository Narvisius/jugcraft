package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

/** Authored, not run: resource-specific routes must conserve actual items and retain filter samples. */
public final class JimtaTransportGameTests {
    @GameTest public void emzuliRowsApplyIndependentColorsAndSaveSamples(GameTestHelper h){
        var at=new BlockPos(2,2,2);var block=(IndustrialTransportDevices.ColorPipe)IndustrialTransport.BLOCKS.get("jimta_pipe_emzuli_item");h.setBlock(at,block);
        var node=h.getBlockEntity(at,IndustrialTransportDevices.Node.class);node.setItem(0,new ItemStack(Items.IRON_INGOT));node.setItem(9,new ItemStack(Items.GOLD_INGOT));node.setItem(18,new ItemStack(Items.EMERALD));node.setItem(27,new ItemStack(Items.DIAMOND));
        int[] colors={11,14,5,4};Item[] items={Items.IRON_INGOT,Items.GOLD_INGOT,Items.EMERALD,Items.DIAMOND};
        for(int i=0;i<4;i++)h.assertTrue(block.routeColor(h.getLevel(),node.getBlockPos(),node.getBlockState(),ItemVariant.of(items[i]),-1)==colors[i],"Each physical filter row paints its own route");
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());node.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(node.getContainerSize()==36&&node.getItem(27).is(Items.DIAMOND)&&node.getSlotsForFace(Direction.DOWN).length==0,"Four rows survive reload and remain unavailable to pipe cargo extraction");
        h.assertTrue(block.routeColor(h.getLevel(),node.getBlockPos(),node.getBlockState(),ItemVariant.of(Items.DIRT),-1)==-2,"No matching bank rejects the item");h.succeed();
    }
    @GameTest public void colorGateRoutesOnlyThePaintedColor(GameTestHelper h){
        var painter=IndustrialTransport.BLOCKS.get("jimta_pipe_lapis_item");var gate=IndustrialTransport.BLOCKS.get("jimta_pipe_daizuli_item");
        h.setBlock(new BlockPos(2,2,2),painter.defaultBlockState().setValue(IndustrialTransportDevices.ColorPipe.COLOR,11));
        h.setBlock(new BlockPos(3,2,2),gate.defaultBlockState().setValue(IndustrialTransportDevices.ColorPipe.COLOR,14));h.setBlock(new BlockPos(4,2,2),Blocks.BARREL);
        var cargo=new SimpleContainer(new ItemStack(Items.IRON_INGOT,8));var storage=ContainerStorage.of(cargo,null);var start=h.absolutePos(new BlockPos(1,2,2));
        ItemNetworks.invalidate(h.getLevel());
        h.assertTrue(ItemNetworks.push(h.getLevel(),start,Direction.EAST,storage,8,Set.of(start))==0&&cargo.getItem(0).getCount()==8,"Red gate rejects blue route without consuming cargo");
        h.setBlock(new BlockPos(3,2,2),gate.defaultBlockState().setValue(IndustrialTransportDevices.ColorPipe.COLOR,11));ItemNetworks.invalidate(h.getLevel());
        var destination=h.getBlockEntity(new BlockPos(4,2,2),BarrelBlockEntity.class);
        h.assertTrue(ItemNetworks.push(h.getLevel(),start,Direction.EAST,storage,8,Set.of(start))==8&&cargo.isEmpty()&&destination.getItem(0).getCount()==8,"Matching gate delivers the original eight items once");h.succeed();
    }
    @GameTest public void filteredPainterRechecksChangedSamplesAndNeverExportsThem(GameTestHelper h){
        h.setBlock(new BlockPos(2,2,2),IndustrialTransport.BLOCKS.get("jimta_pipe_emzuli_item"));h.setBlock(new BlockPos(3,2,2),Blocks.BARREL);
        var filter=h.getBlockEntity(new BlockPos(2,2,2),IndustrialTransportDevices.Node.class);filter.setItem(0,new ItemStack(Items.GOLD_INGOT));
        var cargo=new SimpleContainer(new ItemStack(Items.IRON_INGOT,5));var start=h.absolutePos(new BlockPos(1,2,2));var storage=ContainerStorage.of(cargo,null);
        ItemNetworks.invalidate(h.getLevel());h.assertTrue(ItemNetworks.push(h.getLevel(),start,Direction.EAST,storage,5,Set.of(start))==0,"Unlisted iron is rejected");
        filter.setItem(0,new ItemStack(Items.IRON_INGOT));
        h.assertTrue(ItemNetworks.push(h.getLevel(),start,Direction.EAST,storage,5,Set.of(start))==5,"Cached topology still rechecks the actual changed item filter");
        var exposed=ItemStorage.SIDED.find(h.getLevel(),filter.getBlockPos(),Direction.DOWN);
        if(exposed!=null)try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(exposed.extract(ItemVariant.of(Items.IRON_INGOT),64,tx)==0,"Fallback storage cannot extract filter samples");tx.commit();
        }
        h.assertTrue(filter.getItem(0).getCount()==1,"Filter sample remains after actual extraction attempt");h.succeed();
    }
    @GameTest public void deviceCargoExposureDoesNotIncludeSamplesEvenWithoutASide(GameTestHelper h){
        h.assertTrue(IndustrialTransport.BLOCKS.size()==39,"All 39 native transport variants are registered");
        var at=new BlockPos(2,2,2);h.setBlock(at,IndustrialTransport.BLOCKS.get("jimta_pipe_diamond_wood_item"));
        var device=h.getBlockEntity(at,IndustrialTransportDevices.Node.class);device.setItem(0,new ItemStack(Items.DIAMOND));device.setItem(9,new ItemStack(Items.IRON_INGOT,7));
        var storage=ItemStorage.SIDED.find(h.getLevel(),device.getBlockPos(),null);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(storage.extract(ItemVariant.of(Items.DIAMOND),1,tx)==0,"Unsided automation cannot steal the filter sample");
            h.assertTrue(storage.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==7,"Cargo remains available to ordinary extraction");
        }
        h.assertTrue(device.getItem(9).getCount()==7,"Aborted cargo extraction restores the inventory");h.succeed();
    }
}
