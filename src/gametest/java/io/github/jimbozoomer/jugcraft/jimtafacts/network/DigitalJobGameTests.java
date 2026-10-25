package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;

/** Authored for the deferred combined batch; exercises actual escrow and queue transitions. */
public final class DigitalJobGameTests {
    private static ServerPlayer player(GameTestHelper h){return io.github.jimbozoomer.jugcraft.test.JimtafactsComputerGameTests.player(h);}
    private static DigitalFacades.Covers cable(GameTestHelper h,BlockPos pos){h.setBlock(pos,DigitalStorage.BLOCKS.get("fluix_glass_cable"));return h.getBlockEntity(pos,DigitalFacades.Covers.class);}
    private static DigitalNode attach(GameTestHelper h,DigitalFacades.Covers cable,ServerPlayer player,String kind,net.minecraft.core.Direction face){
        var block=DigitalStorage.BLOCKS.get(kind);var stack=new ItemStack(block);
        h.assertTrue(cable.parts.install(face,block.defaultBlockState(),stack,player)&&stack.isEmpty(),"Installing an attachment consumes exactly its own block item");return cable.parts.get(face);
    }
    @GameTest public void cablePartsKeepIndependentFiltersAndPrivateSavedData(GameTestHelper h){
        var p=player(h);var cable=cable(h,new BlockPos(2,2,2));var a=attach(h,cable,p,"import_bus",net.minecraft.core.Direction.NORTH);var b=attach(h,cable,p,"export_bus",net.minecraft.core.Direction.SOUTH);
        a.filter(new ItemStack(Items.IRON_INGOT));b.filter(new ItemStack(Items.GOLD_INGOT));a.setItem(0,new ItemStack(DigitalStorage.ITEMS.get("speed_card")));
        var saved=cable.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new DigitalFacades.Covers(cable.getBlockPos(),cable.getBlockState());copy.setLevel(h.getLevel());
        copy.loadAdditional(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.parts.get(net.minecraft.core.Direction.NORTH).filter().is(Items.IRON_INGOT)&&copy.parts.get(net.minecraft.core.Direction.SOUTH).filter().is(Items.GOLD_INGOT),"Face settings survive save/reload independently");
        h.assertTrue(copy.parts.get(net.minecraft.core.Direction.NORTH).getItem(0).is(DigitalStorage.ITEMS.get("speed_card")),"Real upgrade survives with the attachment");
        var render=cable.getUpdateTag(h.getLevel().registryAccess()).toString();h.assertTrue(!render.contains("owner")&&!render.contains("link_identity")&&!render.contains("speed_card")&&!render.contains("iron_ingot"),"World render updates do not reveal private filters, inventory or identity");
        h.assertTrue(cable.getBlockState().getValue(DigitalCableBlock.NORTH)&&cable.getBlockState().getValue(DigitalCableBlock.SOUTH),"Arms visibly join both attachments");
        h.assertTrue(!DigitalCableBlock.openFace(h.getLevel(),cable.getBlockPos(),net.minecraft.core.Direction.NORTH),"An attachment face does not become an unintended ME bridge");h.succeed();
    }
    @GameTest public void mountedToggleSeparatesRoutesAndPlaneRimsJoinTheirNeighbors(GameTestHelper h){
        var p=player(h);var a=cable(h,new BlockPos(2,2,2));var b=cable(h,new BlockPos(3,2,2));
        var first=attach(h,a,p,"formation_plane",net.minecraft.core.Direction.NORTH);var second=attach(h,b,p,"formation_plane",net.minecraft.core.Direction.NORTH);
        DigitalPlanes.appearance(first);DigitalPlanes.appearance(second);
        h.assertTrue(first.getBlockState().getValue(DigitalPlanes.HIGH_X)&&second.getBlockState().getValue(DigitalPlanes.LOW_X),"Source plane edges join adjacent matching faces");
        var toggle=attach(h,a,p,"toggle_bus",net.minecraft.core.Direction.SOUTH);
        h.assertTrue(!DigitalCableBlock.openFace(h.getLevel(),a.getBlockPos(),net.minecraft.core.Direction.SOUTH),"Unpowered normal toggle disconnects its outward ME route");
        h.setBlock(new BlockPos(2,1,2),net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        h.assertTrue(DigitalCableBlock.openFace(h.getLevel(),a.getBlockPos(),net.minecraft.core.Direction.SOUTH),"Powered toggle opens only its outward ME route");
        h.assertTrue(!DigitalCableBlock.openFace(h.getLevel(),a.getBlockPos(),net.minecraft.core.Direction.NORTH),"Neighbor plane remains an external work face");h.succeed();
    }
    @GameTest public void mountedEmittersAndLightTunnelsUpdateTheHostWithoutReplacingIt(GameTestHelper h){
        var p=player(h);var cable=cable(h,new BlockPos(2,2,2));var emitter=attach(h,cable,p,"energy_level_emitter",net.minecraft.core.Direction.NORTH);var source=attach(h,cable,p,"cable_energy_acceptor",net.minecraft.core.Direction.SOUTH);
        source.energy.setAmount(500);DigitalControls.update(emitter,new DigitalNetwork(List.of(emitter,source),false,p));
        h.assertTrue(cable.getBlockState().getSignal(h.getLevel(),cable.getBlockPos(),net.minecraft.core.Direction.SOUTH)==15&&cable.getBlockState().getSignal(h.getLevel(),cable.getBlockPos(),net.minecraft.core.Direction.NORTH)==0,"Emitter powers its own outward face only");
        var lamp=attach(h,cable,p,"light_p2p_tunnel",net.minecraft.core.Direction.UP);lamp.updateSignal(lamp.getBlockState().setValue(DigitalLinks.OUTPUT,4));
        h.assertTrue(cable.getBlockState().getValue(DigitalCableBlock.LAMP)&&h.getLevel().getBlockEntity(cable.getBlockPos())==cable,"Mounted light enables the host lamp without replacing the cable or other parts");
        DigitalLinks.reset(lamp);h.assertTrue(!cable.getBlockState().getValue(DigitalCableBlock.LAMP),"Reset clears the mounted light");h.succeed();
    }
    @GameTest public void attachmentChannelsCountSeparatelyOnTheRootSegment(GameTestHelper h){
        var p=player(h);var pos=new BlockPos(2,2,2);h.setBlock(pos,DigitalStorage.BLOCKS.get("fluix_smart_cable"));var cable=h.getBlockEntity(pos,DigitalFacades.Covers.class);
        var source=attach(h,cable,p,"cable_energy_acceptor",net.minecraft.core.Direction.DOWN);source.energy.setAmount(500);
        var terminal=attach(h,cable,p,"terminal",net.minecraft.core.Direction.NORTH);attach(h,cable,p,"fluid_terminal",net.minecraft.core.Direction.SOUTH);attach(h,cable,p,"import_bus",net.minecraft.core.Direction.EAST);
        var network=DigitalNetwork.findLocal(terminal,p);
        h.assertTrue(network.nodes().size()==4&&cable.getBlockState().getValue(DigitalCableBlock.CHANNELS)==3,"Three attachment consumers use three real root-segment channels, while the power acceptor is passive");h.succeed();
    }
    @GameTest public void sideOfAnAttachmentSelectsThatAttachmentRatherThanAnotherFace(GameTestHelper h){
        var p=player(h);var cable=cable(h,new BlockPos(2,2,2));attach(h,cable,p,"terminal",net.minecraft.core.Direction.NORTH);attach(h,cable,p,"import_bus",net.minecraft.core.Direction.UP);
        var point=new net.minecraft.world.phys.Vec3(cable.getBlockPos().getX()+.5,cable.getBlockPos().getY()+.875,cable.getBlockPos().getZ()+.05);
        var hit=new net.minecraft.world.phys.BlockHitResult(point,net.minecraft.core.Direction.UP,cable.getBlockPos(),false);
        h.assertTrue(cable.parts.selected(hit)==net.minecraft.core.Direction.NORTH,"Top edge of the north terminal remains the north terminal");h.succeed();
    }
    @GameTest public void replacingMountedTunnelDoesNotInheritTheOldPair(GameTestHelper h){
        var p=player(h);var a=cable(h,new BlockPos(2,2,2));var b=cable(h,new BlockPos(4,2,2));var input=attach(h,a,p,"item_p2p_tunnel",net.minecraft.core.Direction.NORTH);var output=attach(h,b,p,"item_p2p_tunnel",net.minecraft.core.Direction.NORTH);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(DigitalStorage.ITEMS.get("memory_card")));
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(b.getBlockPos()).add(0,0,-.5),net.minecraft.core.Direction.NORTH,b.getBlockPos(),false);
        var context=new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        p.setShiftKeyDown(true);DigitalLinks.memory(context,input,p);p.setShiftKeyDown(false);DigitalLinks.memory(context,output,p);
        h.assertTrue(DigitalLinks.peer(input,p)==output,"Saved tunnel identity resolves its exact mounted face");
        p.setShiftKeyDown(true);h.assertTrue(b.parts.detach(context),"Old output detaches");attach(h,b,p,"item_p2p_tunnel",net.minecraft.core.Direction.NORTH);
        h.assertTrue(DigitalLinks.peer(input,p)==null,"Replacement cannot inherit a route by sharing the old position and face");h.succeed();
    }
    @GameTest public void twoProvidersOnOneCableAcknowledgeCancellationSeparately(GameTestHelper h){
        var p=player(h);var cable=cable(h,new BlockPos(2,2,2));var a=attach(h,cable,p,"cable_pattern_provider",net.minecraft.core.Direction.NORTH);var b=attach(h,cable,p,"cable_pattern_provider",net.minecraft.core.Direction.SOUTH);
        var pattern=new DigitalProcessing.Pattern(List.of(new ItemStack(Items.RAW_IRON)),List.of(new ItemStack(Items.IRON_INGOT)));var root=UUID.randomUUID();var ledger=DigitalJobLedger.get(a);
        for(var node:List.of(a,b)){node.processing.add(new DigitalProcessing.Job(pattern,1,false,pattern.outputs(),root));h.assertTrue(ledger.track(node,root),"Each provider joins the real job tree");}
        ledger.cancel(root);ledger.observe(a);h.assertTrue(a.processing.isEmpty()&&ledger.cancelled(root)&&b.processing.size()==1,"First face cannot erase the second face's cancellation acknowledgement");
        ledger.observe(b);h.assertTrue(b.processing.isEmpty()&&!ledger.cancelled(root),"Only final acknowledgement clears the tree");h.succeed();
    }
    @GameTest public void removingCableAttachmentClosesStaleAccessWithoutRemovingOtherFaces(GameTestHelper h){
        var p=player(h);var cable=cable(h,new BlockPos(2,2,2));var source=attach(h,cable,p,"cable_energy_acceptor",net.minecraft.core.Direction.NORTH);source.energy.setAmount(321);
        var terminal=attach(h,cable,p,"terminal",net.minecraft.core.Direction.SOUTH);var retained=io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(h.getLevel(),cable.getBlockPos(),net.minecraft.core.Direction.NORTH);
        p.setShiftKeyDown(true);var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(cable.getBlockPos()).add(0,0,-.5),net.minecraft.core.Direction.NORTH,cable.getBlockPos(),false);
        h.assertTrue(cable.parts.detach(new net.minecraft.world.item.context.UseOnContext(p,net.minecraft.world.InteractionHand.MAIN_HAND,hit)),"Wrench removes selected attachment");
        try(var tx=Transaction.openOuter()){h.assertTrue(retained!=null&&retained.extract(1,tx)==0,"Cached energy access expires immediately");}
        h.assertTrue(cable.parts.get(net.minecraft.core.Direction.SOUTH)==terminal&&h.getLevel().getBlockEntity(cable.getBlockPos())==cable,"Other face and cable survive");
        h.assertTrue(!source.stillValid(p),"Any old menu closes after removal");
        long recovered=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(DigitalStorage.BLOCKS.get("cable_energy_acceptor").asItem()))recovered+=item.getOrDefault(DigitalWorkshops.ENERGY,0L);}
        h.assertTrue(recovered==321,"Exactly the stored charge is returned in the attachment item");h.succeed();
    }
    @GameTest public void cellPortFillsAndReturnsActualItems(GameTestHelper h){
        var p=player(h);var chest=node(h,p,"me_chest",1);var io=node(h,p,"io_port",2);chest.energy.setAmount(1000);
        var source=new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"));source.set(DigitalStorage.CONTENTS,CellContents.EMPTY.add(new ItemStack(Items.IRON_INGOT),8));chest.setItem(0,source);
        var target=new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"));io.setItem(0,target);io.emitBelow=true;var network=new DigitalNetwork(List.of(chest,io),false,p);
        DigitalCellIO.tick(io,network,p);h.assertTrue(source.get(DigitalStorage.CONTENTS).amount()==0&&target.get(DigitalStorage.CONTENTS).amount()==8&&chest.energy.getAmount()==992,"Filling transfers real stored items and charges ordinary JE");
        io.emitBelow=false;DigitalCellIO.tick(io,network,p);DigitalCellIO.tick(io,network,p);
        h.assertTrue(io.getItem(0).isEmpty()&&io.getItem(1)==target&&source.get(DigitalStorage.CONTENTS).amount()==8,"Emptying returns the same items then moves the empty cell to output");
        var accessor=new DigitalCellIO.Port(io,net.minecraft.core.Direction.DOWN);
        try(var tx=Transaction.openOuter()) {h.assertTrue(accessor.extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(target),1,tx)==1,"Only completed output is offered to automation");}
        h.assertTrue(io.getItem(1).getCount()==1,"Aborted automation restores the completed cell");
        h.assertTrue(!DigitalCellIO.accepts(new ItemStack(DigitalStorage.ITEMS.get("creative_storage_cell"))),"Infinite creative templates cannot be evacuated as finite cells");h.succeed();
    }
    @GameTest public void fluidCellFiltersRejectNewFluidsWithoutTrappingOldContents(GameTestHelper h){
        var p=player(h);var chest=node(h,p,"me_chest",2);chest.energy.setAmount(1000);
        var cell=new ItemStack(DigitalStorage.ITEMS.get("fluid_storage_cell_1k"));
        var water=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER);var lava=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA);
        cell.set(DigitalWorkshops.FILTERS,List.of(new ItemStack(Items.WATER_BUCKET)));cell.set(FluidCells.CONTENTS,FluidCells.Contents.EMPTY.change(lava,81000));chest.setItem(0,cell);
        var port=new FluidCells.Port(chest,p);
        try(var tx=Transaction.openOuter()){
            h.assertTrue(port.insert(lava,81000,tx)==0&&port.insert(water,81000,tx)==81000,"Filled-bucket filter admits only the selected new fluid");
            h.assertTrue(port.extract(lava,81000,tx)==81000,"Previously stored fluid remains extractable after filter changes");tx.commit();
        }
        h.assertTrue(cell.get(FluidCells.CONTENTS).amount(water)==81000&&cell.get(FluidCells.CONTENTS).amount(lava)==0,"Committed quantities are exact");h.succeed();
    }
    @GameTest public void spatialAnchorPaysAndStopsWhenPowerRunsOut(GameTestHelper h){
        var p=player(h);var source=node(h,p,"energy_acceptor",1);var anchor=node(h,p,"spatial_anchor",2);source.energy.setAmount(150);
        var network=new DigitalNetwork(List.of(source,anchor),false,p);
        try{
            h.assertTrue(DigitalSpatialAnchors.tick(anchor,network)&&source.energy.getAmount()==50,"Anchor acquires its bounded lease only after paying 100 JE");
            h.assertTrue(!DigitalSpatialAnchors.tick(anchor,network)&&source.energy.getAmount()==50,"An underpowered renewal releases the lease without partial debit");
        }finally{DigitalSpatialAnchors.release(anchor);}h.succeed();
    }
    private static DigitalNode node(GameTestHelper h,ServerPlayer p,String kind,int x){
        var at=new BlockPos(x,2,2);h.setBlock(at,DigitalStorage.BLOCKS.get(kind));var n=h.getBlockEntity(at,DigitalNode.class);n.owner(p.getUUID());return n;
    }
    private static DigitalPatterns.Pattern craft(){var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));grid.set(0,new ItemStack(Items.IRON_INGOT));return new DigitalPatterns.Pattern(grid);}
    @GameTest public void reservationIsPrivatePhysicalAndRollsBack(GameTestHelper h){
        var p=player(h);var chest=node(h,p,"me_chest",1);var cpu=node(h,p,"crafting_unit",2);var provider=node(h,p,"pattern_provider",3);
        var cell=new ItemStack(DigitalStorage.ITEMS.get("item_storage_cell_1k"));cell.set(DigitalStorage.CONTENTS,CellContents.EMPTY.add(new ItemStack(Items.IRON_INGOT),6));chest.setItem(0,cell);chest.energy.setAmount(1000);
        var root=UUID.randomUUID();cpu.orders.add(new DigitalPatterns.Order(craft(),1,root));DigitalJobLedger.get(cpu).track(cpu,root);
        var network=new DigitalNetwork(List.of(chest,cpu,provider),false,p);
        DigitalReservations.reserve(cpu,root,List.of(new ItemStack(Items.IRON_INGOT,4)),network);
        h.assertTrue(chest.contents().getFirst().count()==2&&cpu.reservations.contents(root).getFirst().count()==4,"Reservation removes four actual items from the public cell");
        DigitalReservations.reserve(provider,root,List.of(new ItemStack(Items.IRON_INGOT,4)),network);
        h.assertTrue(provider.reservations.contents(root).isEmpty(),"A second participant shares the same reservation rather than withdrawing it twice");
        try(var tx=Transaction.openOuter()){h.assertTrue(network.forJob(root).extract(new ItemStack(Items.IRON_INGOT),4,tx)==4,"Job can withdraw its escrow");}
        h.assertTrue(cpu.reservations.contents(root).getFirst().count()==4,"Aborted craft restores its exact escrow");
        try(var tx=Transaction.openOuter()){h.assertTrue(network.extract(new ItemStack(Items.IRON_INGOT),6,tx)==2,"Ordinary users cannot take reserved items");}
        DigitalProcessing.cancelTree(network,root);
        h.assertTrue(cpu.reservations.contents(root).isEmpty()&&chest.contents().getFirst().count()==6&&cpu.orders.isEmpty(),"Cancellation returns real reserved items exactly once");h.succeed();
    }
    @GameTest public void cancelledDependencyIsRememberedAcrossDisconnection(GameTestHelper h){
        var p=player(h);var cpu=node(h,p,"crafting_unit",1);var provider=node(h,p,"pattern_provider",5);var root=UUID.randomUUID();
        cpu.orders.add(new DigitalPatterns.Order(craft(),1,root));DigitalJobLedger.get(cpu).track(cpu,root);
        var pattern=new DigitalProcessing.Pattern(List.of(new ItemStack(Items.IRON_INGOT)),List.of(new ItemStack(Items.IRON_NUGGET,9)));
        h.assertTrue(DigitalProcessing.requestDependency(new DigitalProcessing.Binding(provider,pattern),root),"Dependency joins the saved root");
        DigitalProcessing.cancelTree(new DigitalNetwork(List.of(cpu),false,p),root);
        h.assertTrue(provider.processing.size()==1&&DigitalJobLedger.get(provider).cancelled(root),"Disconnected provider is not modified by a world scan; cancellation stays saved");
        DigitalJobLedger.get(provider).observe(provider);
        h.assertTrue(provider.processing.isEmpty()&&!DigitalJobLedger.get(provider).cancelled(root),"Provider acknowledges cancellation before doing more work; final acknowledgement frees the ledger entry");h.succeed();
    }
    @GameTest public void deliveredBatchDrainsButItsFutureBatchesAreCancelled(GameTestHelper h){
        var p=player(h);var provider=node(h,p,"pattern_provider",2);var root=UUID.randomUUID();
        var pattern=new DigitalProcessing.Pattern(List.of(new ItemStack(Items.IRON_INGOT)),List.of(new ItemStack(Items.IRON_NUGGET,9)));
        provider.processing.add(new DigitalProcessing.Job(pattern,8,true,pattern.outputs(),root));DigitalJobLedger.get(provider).track(provider,root);
        DigitalProcessing.cancelTree(new DigitalNetwork(List.of(provider),false,p),root);
        var retained=provider.processing.getFirst();
        h.assertTrue(retained.delivered()&&retained.remaining()==1&&!retained.root().equals(root)&&retained.pending().getFirst().getCount()==9,"Only the already-paid batch remains; no fake refund or repeated batches");h.succeed();
    }
    @GameTest public void prerequisiteCanPrecedeUndeliveredParentButCannotInterruptPaidBatch(GameTestHelper h){
        var p=player(h);var provider=node(h,p,"pattern_provider",2);var root=UUID.randomUUID();
        var parent=new DigitalProcessing.Pattern(List.of(new ItemStack(Items.IRON_INGOT)),List.of(new ItemStack(Items.IRON_BLOCK)));
        var prerequisite=new DigitalProcessing.Pattern(List.of(new ItemStack(Items.RAW_IRON)),List.of(new ItemStack(Items.IRON_INGOT)));
        provider.processing.add(new DigitalProcessing.Job(parent,1,false,parent.outputs(),root));DigitalJobLedger.get(provider).track(provider,root);
        h.assertTrue(DigitalProcessing.requestDependency(new DigitalProcessing.Binding(provider,prerequisite),root)&&provider.processing.getFirst().pattern().equals(prerequisite),"Prerequisite is inserted before its unsupplied parent");
        provider.processing.set(0,new DigitalProcessing.Job(prerequisite,1,true,prerequisite.outputs(),root));
        h.assertTrue(!DigitalProcessing.requestDependency(new DigitalProcessing.Binding(provider,parent),root),"A paid batch cannot be displaced while its physical machine is working");h.succeed();
    }
    @GameTest public void creativeSourceCannotBeConfiguredInSurvivalOrReceiveDeposits(GameTestHelper h){
        var p=player(h);p.setGameMode(GameType.SURVIVAL);var cell=new ItemStack(DigitalStorage.ITEMS.get("creative_storage_cell"));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,cell);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND));
        cell.getItem().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(!cell.has(DigitalStorage.CONTENTS),"Survival player cannot configure infinite items");
        p.setGameMode(GameType.CREATIVE);cell.getItem().use(h.getLevel(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        var chest=node(h,p,"me_chest",2);chest.setItem(0,cell);chest.energy.setAmount(1000);var network=new DigitalNetwork(List.of(chest),false,p);
        try(var tx=Transaction.openOuter()){
            h.assertTrue(network.insert(new ItemStack(Items.DIAMOND),3,tx)==0,"Creative source never silently destroys deposits");
            h.assertTrue(network.extract(new ItemStack(Items.DIAMOND),64,tx)==64,"Configured creative source supplies requested items");tx.commit();
        }
        h.assertTrue(cell.get(DigitalStorage.CONTENTS).count(new ItemStack(Items.DIAMOND))==DigitalCreativeCell.DISPLAY_AMOUNT&&chest.energy.getAmount()==936,"Template survives extraction while real JE pays for it");h.succeed();
    }
}
