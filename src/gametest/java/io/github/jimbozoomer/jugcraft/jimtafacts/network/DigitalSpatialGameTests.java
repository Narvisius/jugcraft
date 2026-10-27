package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Authored for the final combined run, including real world exchange and stale-cell conservation. */
public final class DigitalSpatialGameTests {
    @GameTest public void reservationUsesIdentityBeforeNetworkIdAssignment(GameTestHelper h){
        var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,h.getLevel()){
            @Override public int hashCode(){throw new AssertionError("Reservation must not query an unassigned entity ID");}
        };
        SpatialReservation.rejected(cow,true);
        h.assertTrue(SpatialReservation.rejected(cow),"Rejected construction-time entity is tracked by identity");
        SpatialReservation.rejected(cow,false);
        h.assertTrue(!SpatialReservation.rejected(cow),"Allowed movement clears identity reservation");
        h.succeed();
    }
    @GameTest(maxTicks=300) public void residentKeepsUuidNameAndHealthAcrossStoredRevision(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside.below(),Blocks.STONE);
        var cow=net.minecraft.world.entity.EntityTypes.COW.create(h.getLevel(),net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        h.assertTrue(cow!=null,"Cow type creates a resident");cow.setNoAi(true);cow.setCustomName(net.minecraft.network.chat.Component.literal("Spatial resident"));cow.setHealth(7);
        var at=h.absolutePos(s.inside);cow.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);var identity=cow.getUUID();h.getLevel().addFreshEntity(cow);
        String captured=DigitalSpatialJournal.exchange(s.port,s.player);h.assertTrue(captured.startsWith("Exchanged")&&h.getLevel().getEntity(identity)==null,"Capture removes the real resident once: "+captured);
        var stale=s.port.getItem(1).copy();s.port.setItem(0,s.port.removeItem(1,1));
        h.runAfterDelay(101,()->{
            String restored=DigitalSpatialJournal.exchange(s.port,s.player);var animal=h.getLevel().getEntity(identity);
            h.assertTrue(restored.startsWith("Exchanged")&&animal instanceof net.minecraft.world.entity.LivingEntity living&&living.getHealth()==7&&animal.getName().getString().equals("Spatial resident"),"Restoration preserves original identity, name and health: "+restored);
            s.port.setItem(1,ItemStack.EMPTY);s.port.setItem(0,stale);
            h.runAfterDelay(101,()->{String rejected=DigitalSpatialJournal.exchange(s.port,s.player);h.assertTrue(rejected.contains("stale or copied")&&h.getLevel().getEntity(identity)==animal,"Old cell cannot duplicate a resident");animal.discard();h.succeed();});
        });
    }
    private record Setup(ServerPlayer player,DigitalSpatial.Port port,BlockPos inside) {}
    private static Setup setup(GameTestHelper h){
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
        player.snapTo(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1))));
        for(int x:new int[]{2,5})for(int y:new int[]{2,5})for(int z:new int[]{2,5})h.setBlock(new BlockPos(x,y,z),DigitalSpatial.PYLON);
        for(int x=3;x<5;x++)for(int y=3;y<5;y++)for(int z=3;z<5;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        var at=new BlockPos(1,2,2);h.setBlock(at,DigitalSpatial.PORT);
        var port=(DigitalSpatial.Port)h.getLevel().getBlockEntity(h.absolutePos(at));port.owner=player.getUUID();port.energy.setAmount(10000);
        var cell=new ItemStack(DigitalStorage.ITEMS.get("spatial_storage_cell_2"));cell.set(DigitalSpatial.CORNERS,List.of(GlobalPos.of(h.getLevel().dimension(),h.absolutePos(new BlockPos(2,2,2))),GlobalPos.of(h.getLevel().dimension(),h.absolutePos(new BlockPos(5,5,5)))));port.setItem(0,cell);
        return new Setup(player,port,new BlockPos(3,3,3));
    }
    @GameTest(maxTicks=300) public void exchangeRestoresBlocksAndRejectsCopiedOldCell(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.STONE);
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.startsWith("Exchanged"),"First exchange succeeds: "+result);
        h.assertTrue(h.getBlockState(s.inside).isAir()&&s.port.energy.getAmount()==8600,"Eight-block plot consumes exactly 1,400 JE and removes stone");
        ItemStack stale=s.port.getItem(1).copy();s.port.setItem(0,s.port.removeItem(1,1));
        h.runAfterDelay(101,()->{
            String restored=DigitalSpatialJournal.exchange(s.port,s.player);
            h.assertTrue(restored.startsWith("Exchanged")&&h.getBlockState(s.inside).is(Blocks.STONE),"Same plot restores original block: "+restored);
            h.assertTrue(s.port.energy.getAmount()==7200,"Restore consumes exactly the same fee");
            s.port.setItem(1,ItemStack.EMPTY);s.port.setItem(0,stale);
            h.runAfterDelay(101,()->{
                String rejected=DigitalSpatialJournal.exchange(s.port,s.player);
                h.assertTrue(rejected.contains("stale or copied")&&h.getBlockState(s.inside).is(Blocks.STONE)&&s.port.energy.getAmount()==7200,"Copied revision neither restores twice nor spends JE: "+rejected);h.succeed();
            });
        });
    }
    @GameTest public void unsupportedContainerDoesNotConsumeCellOrPower(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.CRAFTER);var before=s.port.getItem(0).copy();
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.startsWith("Unsupported")&&s.port.energy.getAmount()==10000&&s.port.getItem(1).isEmpty()&&ItemStack.isSameItemSameComponents(before,s.port.getItem(0)),"Container rejection keeps cell, inventory and JE: "+result);h.succeed();
    }
    @GameTest(maxTicks=300) public void barrelContentsMoveOnceAndOldInventoryHandlesAreEmpty(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.BARREL);
        var at=h.absolutePos(s.inside);var old=(net.minecraft.world.level.block.entity.BarrelBlockEntity)h.getLevel().getBlockEntity(at);
        var treasure=new ItemStack(net.minecraft.world.item.Items.DIAMOND,17);
        treasure.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Spatial cargo"));old.setItem(4,treasure.copy());
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.startsWith("Exchanged")&&h.getBlockState(s.inside).isAir()&&old.isEmpty(),"Cargo leaves world and stale inventory handles cannot duplicate it: "+result);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(1));
        h.assertTrue(drops.stream().noneMatch(e->e.getItem().is(net.minecraft.world.item.Items.DIAMOND)),"Removing the source barrel does not drop cargo");
        s.port.setItem(0,s.port.removeItem(1,1));h.runAfterDelay(101,()->{
            String restored=DigitalSpatialJournal.exchange(s.port,s.player);
            h.assertTrue(restored.startsWith("Exchanged"),"Stored barrel can return: "+restored);
            var barrel=(net.minecraft.world.level.block.entity.BarrelBlockEntity)h.getLevel().getBlockEntity(at);
            h.assertTrue(barrel!=old&&ItemStack.matches(treasure,barrel.getItem(4)),"Slot, count and custom components survive a real journal round trip");
            h.assertTrue(!h.getBlockState(s.inside).getValue(net.minecraft.world.level.block.BarrelBlock.OPEN),"Restored barrel has no stale open state");h.succeed();
        });
    }
    @GameTest public void spatialInventoriesRequireExactlyOnePayloadPerBarrel(GameTestHelper h){
        var states=List.of(Blocks.BARREL.defaultBlockState(),Blocks.STONE.defaultBlockState());boolean missing=false,duplicate=false,wrong=false;
        try{SpatialInventories.validate(states,List.of());}catch(IllegalArgumentException expected){missing=true;}
        var payload=new SpatialInventories.Entry(0,new net.minecraft.nbt.CompoundTag());
        try{SpatialInventories.validate(states,List.of(payload,payload));}catch(IllegalArgumentException expected){duplicate=true;}
        try{SpatialInventories.validate(states,List.of(new SpatialInventories.Entry(1,new net.minecraft.nbt.CompoundTag())));}catch(IllegalArgumentException expected){wrong=true;}
        h.assertTrue(missing&&duplicate&&wrong,"Missing, duplicate and misplaced inventory snapshots are rejected before writes");h.succeed();
    }
    @GameTest(maxTicks=300) public void idleTransportContainersRoundTripWithoutStaleCargo(GameTestHelper h){
        var s=setup(h);var blocks=List.of(Blocks.HOPPER,Blocks.DISPENSER,Blocks.DROPPER);
        var positions=List.of(s.inside,s.inside.east(),s.inside.south());
        var old=new java.util.ArrayList<net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity>();
        var cargo=new ItemStack(net.minecraft.world.item.Items.EMERALD,13);
        cargo.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Transport cargo"));
        for(int i=0;i<3;i++){
            h.setBlock(positions.get(i),blocks.get(i));
            var node=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(positions.get(i)));
            node.setItem(3,cargo.copy());old.add(node);
        }
        String captured=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(captured.startsWith("Exchanged")&&old.stream().allMatch(net.minecraft.world.Container::isEmpty),"All three cargo containers detach without a live duplicate: "+captured);
        s.port.setItem(0,s.port.removeItem(1,1));h.runAfterDelay(101,()->{
            String restored=DigitalSpatialJournal.exchange(s.port,s.player);
            h.assertTrue(restored.startsWith("Exchanged"),"Transport inventories restore: "+restored);
            for(int i=0;i<3;i++){
                var node=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(positions.get(i)));
                h.assertTrue(node!=old.get(i)&&node.getType()==old.get(i).getType()&&ItemStack.matches(cargo,node.getItem(3)),"Correct inventory type, slot and components return once");
            }
            h.succeed();
        });
    }
    @GameTest public void vanillaProcessorsDetachWithCargoAndPreserveTheirMetadata(GameTestHelper h){
        var blocks=List.of(Blocks.FURNACE,Blocks.SMOKER,Blocks.BLAST_FURNACE,Blocks.BREWING_STAND);
        for(int i=0;i<blocks.size();i++){
            var relative=new BlockPos(2+i,2,2);h.setBlock(relative,blocks.get(i));var at=h.absolutePos(relative);
            var old=(net.minecraft.world.level.block.entity.BaseContainerBlockEntity)h.getLevel().getBlockEntity(at);
            var named=new ItemStack(blocks.get(i));named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Stored processor "+i));old.applyComponentsFromItemStack(named);
            old.setItem(0,new ItemStack(net.minecraft.world.item.Items.IRON_ORE,7));
            h.assertTrue(old.getName().getString().equals("Stored processor "+i)&&old.getItem(0).getCount()==7,"Fixture starts with named cargo");
            var entries=SpatialInventories.capture(h.getLevel(),List.of(at));var state=h.getBlockState(relative);
            SpatialInventories.validate(List.of(state),entries);SpatialInventories.detach(h.getLevel(),List.of(at));
            h.assertTrue(old.isEmpty()&&old.isRemoved(),"Outgoing processor references lose their cargo");
            SpatialInventories.restore(h.getLevel(),List.of(at),entries);
            var restored=(net.minecraft.world.level.block.entity.BaseContainerBlockEntity)h.getLevel().getBlockEntity(at);
            h.assertTrue(restored!=old&&restored.getType()==old.getType()&&restored.getItem(0).getCount()==7&&restored.getName().getString().equals("Stored processor "+i),"Exact processor type, name and cargo restored once");
        }h.succeed();
    }
    @GameTest public void closedShulkerKeepsColorCargoAndCannotRestoreIntoAnotherColor(GameTestHelper h){
        var red=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_shulker_box"));
        var blue=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("blue_shulker_box"));
        var relative=new BlockPos(2,2,2);h.setBlock(relative,red);var at=h.absolutePos(relative);
        var old=(net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)h.getLevel().getBlockEntity(at);
        var cargo=new ItemStack(net.minecraft.world.item.Items.DIAMOND,19);old.setItem(26,cargo.copy());
        var entries=SpatialInventories.capture(h.getLevel(),List.of(at));boolean mismatch=false;
        try{SpatialInventories.validate(List.of(blue.defaultBlockState()),entries);}catch(IllegalArgumentException expected){mismatch=true;}
        h.assertTrue(mismatch,"Shared block entity type does not erase shulker color identity");
        SpatialInventories.detach(h.getLevel(),List.of(at));SpatialInventories.restore(h.getLevel(),List.of(at),entries);
        var restored=(net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity)h.getLevel().getBlockEntity(at);
        h.assertTrue(old.isEmpty()&&old.isRemoved()&&restored!=old&&restored.isClosed()&&ItemStack.matches(cargo,restored.getItem(26)),"Closed shulker restores cargo once without stale handles");h.succeed();
    }
    @GameTest public void pendingDispensersAndWrongPayloadTypesAreRejected(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.DISPENSER);var at=h.absolutePos(s.inside);
        h.getLevel().scheduleTick(at,Blocks.DISPENSER,20);boolean pending=false;
        try{SpatialInventories.capture(h.getLevel(),List.of(at));}catch(IllegalArgumentException expected){pending=true;}
        var payload=new net.minecraft.nbt.CompoundTag();payload.putString("jugcraft_spatial_container","minecraft:barrel");boolean wrong=false;
        try{SpatialInventories.validate(List.of(Blocks.HOPPER.defaultBlockState()),List.of(new SpatialInventories.Entry(0,payload)));}catch(IllegalArgumentException expected){wrong=true;}
        h.assertTrue(pending&&wrong&&s.port.energy.getAmount()==10000&&s.port.getItem(1).isEmpty(),"Pending pulses and cross-type inventories cannot begin migration");
        h.assertTrue(!SpatialInventories.supported(Blocks.DISPENSER.defaultBlockState().setValue(net.minecraft.world.level.block.DispenserBlock.TRIGGERED,true)),"Powered dispenser is not an idle container");h.succeed();
    }
    @GameTest public void missingPylonAndBlockedOutputNeverChangeWorld(GameTestHelper h){
        var s=setup(h);h.setBlock(s.inside,Blocks.STONE);s.port.setItem(1,new ItemStack(net.minecraft.world.item.Items.DIRT));
        String result=DigitalSpatialJournal.exchange(s.port,s.player);
        h.assertTrue(result.contains("clear the output")&&h.getBlockState(s.inside).is(Blocks.STONE)&&s.port.energy.getAmount()==10000,"Blocked output does not start exchange");
        s.port.setItem(1,ItemStack.EMPTY);h.setBlock(new BlockPos(5,5,5),Blocks.AIR);
        result=DigitalSpatialJournal.exchange(s.port,s.player);h.assertTrue(result.contains("Eight loaded")&&h.getBlockState(s.inside).is(Blocks.STONE)&&s.port.energy.getAmount()==10000,"Broken frame prevents exchange: "+result);h.succeed();
    }
    @GameTest public void ironFurnaceMigrationPreparesWithoutTouchingLiveCargo(GameTestHelper h){
        var block=io.github.jimbozoomer.jugcraft.jimtafacts.industry.IndustrialConstruction.BLOCKS.get("iron_furnace");
        var relative=new BlockPos(2,2,2);h.setBlock(relative,block);var at=h.absolutePos(relative);
        var old=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)h.getLevel().getBlockEntity(at);
        old.setItem(0,new ItemStack(net.minecraft.world.item.Items.IRON_ORE,7));
        old.setItem(1,new ItemStack(net.minecraft.world.item.Items.COAL,3));
        var positions=List.of(at);var states=List.of(h.getBlockState(relative));
        var entries=SpatialInventories.capture(h.getLevel(),positions);
        var prepared=SpatialInventories.prepare(h.getLevel(),positions,states,entries);
        h.assertTrue(h.getLevel().getBlockEntity(at)==old&&!old.isRemoved()&&old.getItem(0).getCount()==7,
                "Preparation must not remove or empty the live furnace");
        boolean wrong=false;
        try{SpatialInventories.prepare(h.getLevel(),positions,List.of(Blocks.FURNACE.defaultBlockState()),entries);}
        catch(IllegalArgumentException expected){wrong=true;}
        h.assertTrue(wrong&&old.getItem(1).getCount()==3,"Shared furnace type cannot erase iron housing identity or consume fuel");
        SpatialInventories.detach(h.getLevel(),positions);SpatialInventories.install(h.getLevel(),prepared);
        var restored=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)h.getLevel().getBlockEntity(at);
        h.assertTrue(old.isEmpty()&&old.isRemoved()&&restored!=old&&restored.getItem(0).getCount()==7
                &&restored.getItem(1).getCount()==3&&h.getBlockState(relative).is(block),
                "Iron furnace retains housing and cargo exactly once");h.succeed();
    }

    @GameTest(maxTicks=300) public void pairedChestsMoveAsOneBoundaryAndDoNotRetainStaleCargo(GameTestHelper h){
        var s=setup(h);var first=s.inside;var second=first.east();
        var left=Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,Direction.NORTH).setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT);
        var right=left.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT);
        // Derive the partner direction from the actual block API rather than assuming viewer-left.
        if(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(left)!=Direction.EAST){var swap=left;left=right;right=swap;}
        h.setBlock(first,left);h.setBlock(second,right);
        var a=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(first));
        var b=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(second));
        a.setItem(0,new ItemStack(net.minecraft.world.item.Items.EMERALD,3));b.setItem(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND,5));
        boolean refused=false;try{SpatialInventories.capture(h.getLevel(),List.of(h.absolutePos(first)));}catch(IllegalArgumentException expected){refused=true;}
        h.assertTrue(refused&&!a.isEmpty()&&!b.isEmpty(),"Capturing one half refuses before clearing either inventory");
        String result=DigitalSpatialJournal.exchange(s.port,s.player);h.assertTrue(result.startsWith("Exchanged")&&a.isEmpty()&&b.isEmpty(),"Both halves leave the world without stale contents: "+result);
        s.port.setItem(0,s.port.removeItem(1,1));h.runAfterDelay(101,()->{
            String restored=DigitalSpatialJournal.exchange(s.port,s.player);h.assertTrue(restored.startsWith("Exchanged"),"Paired chest restoration: "+restored);
            var restoredA=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(first));var restoredB=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(second));
            h.assertTrue(restoredA.getItem(0).getCount()==3&&restoredB.getItem(0).getCount()==5,"Each half keeps its own exact cargo");h.succeed();
        });
    }
}
