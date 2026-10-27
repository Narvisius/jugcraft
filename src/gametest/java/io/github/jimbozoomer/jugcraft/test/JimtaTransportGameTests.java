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
    @GameTest public void solidAndHollowFacadesPreserveSamplesAndGateCachedPorts(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_diamond_wood_item"));
        var node=h.getBlockEntity(pos,IndustrialTransportDevices.Node.class);node.setItem(9,new ItemStack(Items.GOLD_INGOT,3));
        var fittings=node.attachments();var side=Direction.WEST;int face=side.ordinal();
        var port=ItemStorage.SIDED.find(h.getLevel(),node.getBlockPos(),side);
        var view=java.util.stream.StreamSupport.stream(port.spliterator(),false).filter(v->v.getAmount()>0).findFirst().orElseThrow();
        fittings.faces.set(face,Blocks.OAK_PLANKS.defaultBlockState());fittings.facadeModes[face]=TransportFacades.SEALED;fittings.changed();
        h.assertTrue(fittings.blocked(side)&&!fittings.plugged(side)&&!TransportAttachments.openEnergyEdge(h.getLevel(),node.getBlockPos(),side),"A solid facade seals routes without pretending to be a separate plug");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(port.extract(ItemVariant.of(Items.GOLD_INGOT),3,tx)==0&&view.extract(ItemVariant.of(Items.GOLD_INGOT),3,tx)==0,"Sealed facade immediately revokes cached storage and views");
        }
        var snapshot=node.saveWithoutMetadata(h.getLevel().registryAccess());
        node.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),snapshot));
        h.assertTrue(fittings.facadeStack(side).is(TransportFacades.SOLID)&&fittings.faces.get(face).is(Blocks.OAK_PLANKS),"Saved facade retains its sample and actual variant");
        fittings.facadeModes[face]=TransportFacades.OPEN;fittings.changed();
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(port.extract(ItemVariant.of(Items.GOLD_INGOT),3,tx)==3,"Hollow facade permits the same connection without losing cargo");}
        snapshot.remove("facade_modes");node.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),snapshot));
        h.assertTrue(!fittings.blocked(side)&&fittings.facadeModes[face]==0,"Older saves retain connection-permitting cosmetic covers");h.succeed();
    }
    @GameTest public void facadeConversionPreservesSampleWithoutMultiplyingItems(GameTestHelper h){
        var original=io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalFacades.facade(Blocks.BRICKS.defaultBlockState());original.setCount(12);
        var input=net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(original));
        var solid=TransportFacades.CONVERSION.assemble(input);
        h.assertTrue(solid.is(TransportFacades.SOLID)&&solid.getCount()==1&&solid.get(io.github.jimbozoomer.jugcraft.jimtafacts.network.DigitalFacades.MATERIAL).is(Blocks.BRICKS),"Conversion consumes one sampled cover and retains its appearance");
        var hollow=TransportFacades.CONVERSION.assemble(net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(solid)));
        h.assertTrue(hollow.is(TransportFacades.HOLLOW),"Solid cover converts into hollow cover");
        h.assertTrue(!TransportFacades.CONVERSION.matches(net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(solid,hollow)),h.getLevel()),"Multiple input stacks cannot become one facade");
        h.assertTrue(!TransportFacades.CONVERSION.matches(net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(new ItemStack(TransportFacades.SOLID))),h.getLevel()),"Unsampled facade is not a valid recipe");h.succeed();
    }
    @GameTest public void poweredExtractionPaysOnlyForActualExtraCargo(GameTestHelper h){
        var pos=new BlockPos(3,2,3);var pipe=IndustrialTransport.BLOCKS.get("jimta_pipe_wood_item");
        h.setBlock(pos,pipe.defaultBlockState().setValue(net.minecraft.world.level.block.DirectionalBlock.FACING,Direction.EAST));h.setBlock(pos.east(),Blocks.BARREL);h.setBlock(pos.west(),Blocks.BARREL);
        var node=h.getBlockEntity(pos,TransportAttachments.Node.class);var fittings=node.attachments();fittings.powerFace=Direction.UP.ordinal();fittings.changed();
        var source=h.getBlockEntity(pos.east(),BarrelBlockEntity.class);var target=h.getBlockEntity(pos.west(),BarrelBlockEntity.class);source.setItem(0,new ItemStack(Items.IRON_INGOT,64));
        var power=io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(h.getLevel(),node.getBlockPos(),Direction.UP);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(power.insert(500,tx)==256,"Original adaptor uses the native bounded 256-JE inlet");}
        h.assertTrue(fittings.power.getAmount()==0,"Aborted charge insertion cannot create energy");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){power.insert(256,tx);tx.commit();}
        h.assertTrue(!TransportAttachments.openEdge(h.getLevel(),node.getBlockPos(),Direction.UP)&&TransportAttachments.openEnergyEdge(h.getLevel(),node.getBlockPos(),Direction.UP),"Adaptor face accepts power while sealing item/fluid traffic");
        h.getBlockState(pos).tick(h.getLevel(),node.getBlockPos(),h.getLevel().getRandom());
        h.assertTrue(source.getItem(0).getCount()==16&&target.getItem(0).getCount()==48&&fittings.power.getAmount()==0,"Sixteen base items plus thirty-two extras spend exactly 256 JE");
        fittings.power.setAmount(128);fittings.mask=1<<Direction.WEST.ordinal();fittings.changed();
        h.getBlockState(pos).tick(h.getLevel(),node.getBlockPos(),h.getLevel().getRandom());
        h.assertTrue(source.getItem(0).getCount()==16&&fittings.power.getAmount()==128,"Blocked output cannot consume charge or cargo");
        h.assertTrue(io.github.jimbozoomer.jugcraft.tools.Chargeable.energy(fittings.adaptorStack())==128,"Removed adaptor carries its actual remaining charge");
        fittings.powerFace=-1;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(power.insert(1,tx)==0,"Removing the adaptor revokes a cached power inlet");}h.succeed();
    }
    @GameTest public void activeFittingsPreserveCargoAndRevokeCachedPorts(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_diamond_wood_item"));
        var node=h.getBlockEntity(pos,IndustrialTransportDevices.Node.class);node.setItem(0,new ItemStack(Items.DIAMOND));node.setItem(9,new ItemStack(Items.IRON_INGOT,7));
        var port=ItemStorage.SIDED.find(h.getLevel(),node.getBlockPos(),Direction.WEST);
        var view=java.util.stream.StreamSupport.stream(port.spliterator(),false).filter(v->!v.isResourceBlank()&&v.getResource().isOf(Items.IRON_INGOT)).findFirst().orElseThrow();
        node.attachments().mask=1<<Direction.WEST.ordinal();node.attachments().faces.set(Direction.UP.ordinal(),Blocks.BRICKS.defaultBlockState());
        var snapshot=node.saveWithoutMetadata(h.getLevel().registryAccess());
        node.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),snapshot));
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(port.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==0&&view.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==0,"A plug revokes both cached storage and cached view extraction");
        }
        node.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),node.getUpdateTag(h.getLevel().registryAccess())));
        h.assertTrue(node.getItem(0).is(Items.DIAMOND)&&node.getItem(9).getCount()==7&&node.attachments().faces.get(Direction.UP.ordinal()).is(Blocks.BRICKS),"Saved fittings and visual-only packets preserve real samples and cargo");
        node.attachments().mask=1<<Direction.DOWN.ordinal();
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(port.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==7,"A bottom plug does not close the unsealed west cargo port");
        }
        h.setBlock(pos,Blocks.AIR);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(port.extract(ItemVariant.of(Items.IRON_INGOT),7,tx)==0,"A removed device cannot supply cargo through an old reference");
        }h.succeed();
    }
    @GameTest public void additionalIndustrialSortersExposeRealTransferPorts(GameTestHelper h){
        var pos=new BlockPos(2,2,2);
        for(String name:java.util.List.of("jimta_item_filter","jimta_sorting_machine")){
            h.setBlock(pos,IndustrialAutomation.BLOCKS.get(name).defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlock.FACING,Direction.EAST));
            h.setBlock(pos.east(),Blocks.BARREL);
            var sorter=h.getBlockEntity(pos,io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlockEntity.class);
            sorter.setItem(0,new ItemStack(Items.IRON_INGOT));
            var port=ItemStorage.SIDED.find(h.getLevel(),sorter.getBlockPos(),Direction.WEST);
            h.assertTrue(port!=null,"Variant has registered inlet: "+name);
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                h.assertTrue(port.insert(ItemVariant.of(Items.GOLD_INGOT),8,tx)==0,"Filter rejects unmatched cargo: "+name);
                h.assertTrue(port.insert(ItemVariant.of(Items.IRON_INGOT),8,tx)==8,"Variant forwards matching cargo: "+name);tx.commit();
            }
            h.assertTrue(h.getBlockEntity(pos.east(),BarrelBlockEntity.class).getItem(0).getCount()==8&&sorter.getItem(0).getCount()==1,"Cargo moves once, sample stays: "+name);
            h.getBlockEntity(pos.east(),BarrelBlockEntity.class).clearContent();h.setBlock(pos,Blocks.AIR);
        }h.succeed();
    }
    @GameTest public void sorterAndDisposalRecheckBothPluggedFaces(GameTestHelper h){
        var pos=new BlockPos(2,2,2);var block=IndustrialTransport.BLOCKS.get("jimta_pipe_diamond_item");
        h.setBlock(pos,block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlock.FACING,Direction.EAST));h.setBlock(pos.east(),Blocks.BARREL);
        var sorter=h.getBlockEntity(pos,io.github.jimbozoomer.jugcraft.logistics.ItemSorterBlockEntity.class);sorter.setItem(0,new ItemStack(Items.IRON_INGOT));
        var inlet=ItemStorage.SIDED.find(h.getLevel(),sorter.getBlockPos(),Direction.WEST);
        for(var side:java.util.List.of(Direction.WEST,Direction.EAST)){
            sorter.attachments().mask=1<<side.ordinal();
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(inlet.insert(ItemVariant.of(Items.IRON_INGOT),8,tx)==0,"Sorter's intake and direct output both honor plugs");}
        }
        sorter.attachments().mask=0;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(inlet.insert(ItemVariant.of(Items.IRON_INGOT),8,tx)==8,"Unsealed sorter still forwards matching cargo");tx.commit();}
        var disposal=pos.south(3);h.setBlock(disposal,IndustrialTransport.BLOCKS.get("jimta_pipe_void_item").defaultBlockState().setValue(IndustrialTransportSpecial.VoidBlock.ENABLED,true));
        var node=h.getBlockEntity(disposal,TransportAttachments.Node.class);var sink=ItemStorage.SIDED.find(h.getLevel(),node.getBlockPos(),Direction.UP);node.attachments().mask=1<<Direction.UP.ordinal();
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){h.assertTrue(sink.insert(ItemVariant.of(Items.DIAMOND),1,tx)==0,"Cached disposal port refuses contents after fitting a plug");}
        h.assertTrue(sorter.getItem(0).getCount()==1&&h.getBlockEntity(pos.east(),BarrelBlockEntity.class).getItem(0).getCount()==8,"Real cargo moves once; the filter sample remains");h.succeed();
    }
    @GameTest public void passivePlugsBlockFluidAndPowerWithoutConsumingEither(GameTestHelper h){
        var fluidPipe=new BlockPos(2,2,2);var tank=fluidPipe.east();h.setBlock(fluidPipe,IndustrialTransport.BLOCKS.get("jimta_pipe_gold_fluid"));h.setBlock(tank,io.github.jimbozoomer.jugcraft.fluid.JugcraftFluids.FLUID_TANK);
        var fluidNode=h.getBlockEntity(fluidPipe,TransportAttachments.Node.class);fluidNode.attachments().mask=1<<Direction.EAST.ordinal();fluidNode.changed();
        long bucket=net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
        var sourceFluid=net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage.withFixedCapacity(bucket,()->{});sourceFluid.variant=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER);sourceFluid.amount=bucket;
        var source=h.absolutePos(fluidPipe.west());
        h.assertTrue(io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.pushToNeighbors(h.getLevel(),source,sourceFluid,bucket,java.util.List.of(Direction.EAST))==0&&sourceFluid.amount==bucket,"Fluid remains at its source behind a plugged exit");
        fluidNode.attachments().mask=0;fluidNode.changed();
        h.assertTrue(io.github.jimbozoomer.jugcraft.fluid.FluidNetworks.pushToNeighbors(h.getLevel(),source,sourceFluid,bucket,java.util.List.of(Direction.EAST))==bucket&&sourceFluid.amount==0,"Removing the plug reopens the cached fluid route");
        h.assertTrue(h.getBlockEntity(tank,io.github.jimbozoomer.jugcraft.fluid.FluidTankBlockEntity.class).storage.amount==bucket,"Destination contains exactly one bucket");
        var cable=new BlockPos(2,2,5);var battery=cable.east();h.setBlock(cable,IndustrialTransport.BLOCKS.get("jimta_pipe_gold_power"));h.setBlock(battery,JimtaIndustry.MACHINES.get("jimta_mfe"));
        var powerNode=h.getBlockEntity(cable,TransportAttachments.Node.class);powerNode.attachments().mask=1<<Direction.EAST.ordinal();powerNode.changed();
        var power=new io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage(1000,1000,1000,()->{});power.setAmount(1000);var feed=h.absolutePos(cable.west());
        h.assertTrue(io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.pushToNeighbors(h.getLevel(),feed,power,1000,java.util.List.of(Direction.EAST))==0&&power.getAmount()==1000,"Power is conserved behind the plugged exit");
        powerNode.attachments().mask=0;powerNode.changed();
        h.assertTrue(io.github.jimbozoomer.jugcraft.energy.EnergyNetworks.pushToNeighbors(h.getLevel(),feed,power,1000,java.util.List.of(Direction.EAST))==1000&&power.getAmount()==0,"Removing the plug reopens power transport");h.succeed();
    }
    @GameTest public void passiveFittingsSealAndColorTheActualInjectionEdge(GameTestHelper h){
        var first=new BlockPos(2,2,2);var second=first.east();var output=second.east();
        h.setBlock(first,IndustrialTransport.BLOCKS.get("jimta_pipe_gold_item"));h.setBlock(second,IndustrialTransport.BLOCKS.get("jimta_pipe_gold_item"));h.setBlock(output,Blocks.BARREL);
        var a=h.getBlockEntity(first,TransportAttachments.Node.class);var b=h.getBlockEntity(second,TransportAttachments.Node.class);
        var source=h.absolutePos(first.west());var cargo=new SimpleContainer(new ItemStack(Items.DIAMOND,8));var storage=ContainerStorage.of(cargo,null);
        a.attachments().mask=1<<Direction.WEST.ordinal();a.changed();
        h.assertTrue(ItemNetworks.push(h.getLevel(),source,Direction.EAST,storage,8,Set.of(source))==0,"Plug closes the initial injection edge too");
        a.attachments().mask=0;a.attachments().lenses[Direction.WEST.ordinal()]=13; // blue lens: 11 + 2
        b.attachments().lenses[Direction.WEST.ordinal()]=33;b.changed();a.changed(); // red filter: 14 + 19
        h.assertTrue(ItemNetworks.push(h.getLevel(),source,Direction.EAST,storage,8,Set.of(source))==0&&cargo.getItem(0).getCount()==8,"Red face filter rejects the blue injection route without losses");
        b.attachments().lenses[Direction.WEST.ordinal()]=30;b.changed();
        h.assertTrue(ItemNetworks.push(h.getLevel(),source,Direction.EAST,storage,8,Set.of(source))==8&&cargo.isEmpty(),"Matching blue filter transfers all eight real items after cache invalidation");
        h.assertTrue(h.getBlockEntity(output,BarrelBlockEntity.class).getItem(0).getCount()==8,"Output holds exactly the input amount");h.succeed();
    }
    @GameTest public void passiveFittingsPersistAndClearLensesRemoveRouteColor(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_stone_item"));var node=h.getBlockEntity(pos,TransportAttachments.Node.class);
        node.attachments().mask=1<<Direction.NORTH.ordinal();node.attachments().lenses[Direction.EAST.ordinal()]=1;node.attachments().lenses[Direction.WEST.ordinal()]=18;node.attachments().faces.set(Direction.UP.ordinal(),Blocks.OBSIDIAN.defaultBlockState());
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new TransportAttachments.Node(node.getBlockPos(),node.getBlockState());
        copy.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.blocked(Direction.NORTH)&&copy.attachments().faces.get(Direction.UP.ordinal()).is(Blocks.OBSIDIAN),"Plug and sampled cover persist on separate faces");
        h.assertTrue(copy.route(Direction.EAST,11)==-1&&copy.route(Direction.WEST,-1)==-1&&copy.route(Direction.WEST,11)==-2,"Clear lens removes color; clear filter only accepts uncolored routes");h.succeed();
    }
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
