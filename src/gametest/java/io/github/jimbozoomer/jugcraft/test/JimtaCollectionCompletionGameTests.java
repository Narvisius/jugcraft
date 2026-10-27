package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.jimtafacts.industry.*;
import io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks.JimtaStatues;
import io.github.jimbozoomer.jugcraft.control.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;

/** Authored for the final combined batch; not executed during implementation. */
public final class JimtaCollectionCompletionGameTests {
    @GameTest public void ironFurnaceKeepsNativeInventoryState(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialConstruction.BLOCKS.get("iron_furnace"));
        var node=h.getBlockEntity(pos,net.minecraft.world.level.block.entity.FurnaceBlockEntity.class);
        node.setItem(0,new ItemStack(Items.RAW_IRON,3));node.setItem(1,new ItemStack(Items.COAL,2));
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());
        var copy=new net.minecraft.world.level.block.entity.FurnaceBlockEntity(h.absolutePos(pos),node.getBlockState());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.getItem(0).getCount()==3&&copy.getItem(1).getCount()==2&&copy.getItem(2).isEmpty(),"Iron housing uses native three-slot furnace state without duplicating output");
        h.assertTrue(copy.getType().isValid(node.getBlockState()),"Native furnace block entity recognizes the supplied iron housing");h.succeed();
    }

    @GameTest public void advancedMinerRetainsItsFilterAndSavedMode(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,CivilWorkshops.BLOCKS.get("advanced_miner"));
        var node=h.getBlockEntity(pos,CivilWorkshops.Node.class);
        h.assertTrue(!node.acceptsMiningDrops(List.of(new ItemStack(Items.DIAMOND))),"Empty include filter never mines everything");
        node.setItem(1,new ItemStack(Items.DIAMOND,3));
        h.assertTrue(node.acceptsMiningDrops(List.of(new ItemStack(Items.DIAMOND)))&&!node.acceptsMiningDrops(List.of(new ItemStack(Items.DIRT))),"Only selected drops match");
        node.excludeFilter=true;
        h.assertTrue(!node.acceptsMiningDrops(List.of(new ItemStack(Items.DIAMOND)))&&node.acceptsMiningDrops(List.of(new ItemStack(Items.DIRT))),"Exclude mode reverses the filter");
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new CivilWorkshops.Node(h.absolutePos(pos),node.getBlockState());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.excludeFilter&&copy.getItem(1).getCount()==3,"Save/load retains mode and real filter items without consuming them");h.succeed();
    }
    @GameTest public void industrialBufferRetainsCargoOwnershipAndRouting(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,WorkshopChute.ITEM_BUFFER);
        var node=h.getBlockEntity(pos,WorkshopChute.Node.class);node.owner=UUID.randomUUID();node.outputs=1<<Direction.EAST.ordinal();node.collect=false;node.energy.setAmount(92);node.setItem(3,new ItemStack(Items.DIAMOND,17));
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new WorkshopChute.Node(h.absolutePos(pos),node.getBlockState());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(copy.owner.equals(node.owner)&&copy.outputs==node.outputs&&!copy.collect&&copy.energy.getAmount()==92&&copy.getItem(3).getCount()==17,"Buffer housing retains shared routing, owner, charge and exact cargo");h.succeed();
    }

    @GameTest public void cropmatronPaysOnlyForPermittedCareAndPreservesWeedSupplies(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var machine=new BlockPos(2,2,6);var crop=machine.north();var soil=crop.below();
            h.setBlock(machine,CropMatron.BLOCK);h.setBlock(soil,Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE,0));h.setBlock(crop,Blocks.WHEAT);
            var n=h.getBlockEntity(machine,CropMatron.Node.class);n.owner=player.getUUID();n.cursor=4;n.energy.setAmount(160);n.water.fill(net.minecraft.world.level.material.Fluids.WATER,25);n.setItem(0,new ItemStack(Items.BONE_MEAL,2));
            try(var claim=TestClaims.open().block(h.absolutePos(crop),UUID.randomUUID())){n.step(h.getLevel());}
            h.assertTrue(n.energy.getAmount()==160&&n.water.millibuckets()==25&&n.getItem(0).getCount()==2,"Protected crop consumes no water, fertilizer or power");
            n.cursor=4;n.step(h.getLevel());
            h.assertTrue(h.getBlockState(soil).getValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE)==7&&n.energy.getAmount()==120&&n.water.millibuckets()==0&&n.getItem(0).getCount()==2,"Soil hydration uses 25 mB and 40 JE, without consuming fertilizer");
            h.setBlock(crop,IndustrialCrops.CROPS.get("weed"));n.setItem(1,new ItemStack(IndustrialCrops.ITEMS.get("jimta_weed_ex")));n.cursor=4;n.step(h.getLevel());
            h.assertTrue(h.getBlockState(crop).is(IndustrialCrops.CROPS.get("cross_sticks"))&&n.getItem(1).isEmpty()&&n.energy.getAmount()==80,"Weed treatment consumes one real dose and restores empty cross sticks");
            n.cursor=4;n.step(h.getLevel());h.assertTrue(n.energy.getAmount()==80&&n.getItem(0).getCount()==2,"Empty cross sticks do not spend fertilizer");h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}
    }
    @GameTest public void workshopFrameUpdatesOnlyItsRealNeighborConnections(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,FactoryDecor.FRAME);h.setBlock(pos.east(),FactoryDecor.FRAME);
        h.assertTrue(h.getBlockState(pos).getValue(FactoryDecor.Frame.CONNECTIONS.get(Direction.EAST)),"Frame joins the neighboring frame");
        h.setBlock(pos.east(),Blocks.STONE);
        h.assertTrue(!h.getBlockState(pos).getValue(FactoryDecor.Frame.CONNECTIONS.get(Direction.EAST)),"Solid decoration is not mistaken for another frame");h.succeed();
    }
    @GameTest public void fragileShardSealsAndConsumesOnlyOnCommittedFinalDrain(GameTestHelper h){
        var inventory=new net.minecraft.world.SimpleContainer(new ItemStack(IndustrialProduction.ITEMS.get("jimta_fragile_fluid_shard")));
        var slot=net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(inventory,null).getSlot(0);
        var storage=FragileFluidShard.storage(net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofSingleSlot(slot));
        var water=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER);
        var lava=net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA);
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(storage.insert(water,FragileFluidShard.CAPACITY*2,tx)==FragileFluidShard.CAPACITY,"Shell accepts at most 500 mB");tx.commit();
        }
        long half=FragileFluidShard.CAPACITY/2;
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(storage.extract(lava,half,tx)==0,"Wrong fluid cannot drain");
            h.assertTrue(storage.extract(water,half,tx)==half&&storage.insert(water,half,tx)==0,"Partially drained shard stays sealed");tx.commit();
        }
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(storage.extract(water,half,tx)==half&&inventory.getItem(0).isEmpty(),"Final drain removes the shell transactionally");
        }
        h.assertTrue(!inventory.getItem(0).isEmpty()&&storage.getAmount()==half,"Aborted drain restores both item and fluid");
        try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
            h.assertTrue(storage.extract(water,half,tx)==half,"Retry transfers the exact remainder");tx.commit();
        }
        h.assertTrue(inventory.getItem(0).isEmpty()&&storage.getAmount()==0,"Committed final drain consumes the shard");h.succeed();
    }
    @GameTest public void industrialTntUsesOwnerRemoteAndPreservesProtectedCargo(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);var other=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialDynamite.INDUSTRIAL_TNT);var at=h.absolutePos(pos);
            var charge=(IndustrialDynamite.Charge)h.getLevel().getBlockEntity(at);charge.owner=player.getUUID();
            h.assertTrue(!charge.prime(other,40)&&charge.fuse<0,"A different player cannot ignite the charge");
            var remote=new ItemStack(IndustrialDynamite.REMOTE);remote.set(IndustrialDynamite.LINKS,List.of(new IndustrialDynamite.Link(GlobalPos.of(h.getLevel().dimension(),at),charge.identity)));charge.linked=true;
            h.assertTrue(IndustrialDynamite.fire(player,remote)==1&&charge.fuse==40&&remote.get(IndustrialDynamite.LINKS).isEmpty(),"Industrial charge shares the owner-bound remote and finite fuse");
            h.setBlock(pos,Blocks.AIR);h.setBlock(pos.offset(3,0,0),Blocks.DIRT);h.setBlock(pos.offset(0,0,3),Blocks.BARREL);
            var barrel=(net.minecraft.world.level.block.entity.BarrelBlockEntity)h.getLevel().getBlockEntity(at.offset(0,0,3));barrel.setItem(0,new ItemStack(Items.DIAMOND,3));
            try(var claim=TestClaims.open().block(at.offset(3,0,0),other.getUUID())){
                IndustrialDynamite.blast(h.getLevel(),net.minecraft.world.phys.Vec3.atCenterOf(at),player.getUUID(),null,4);
                h.assertTrue(h.getBlockState(pos.offset(3,0,0)).is(Blocks.DIRT),"Wider burst still respects claims");
            }
            IndustrialDynamite.blast(h.getLevel(),net.minecraft.world.phys.Vec3.atCenterOf(at),player.getUUID(),null,4);
            h.assertTrue(h.getBlockState(pos.offset(3,0,0)).isAir()&&barrel.getItem(0).getCount()==3,"Four-block burst reaches permitted terrain but never destroys stored cargo");h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);h.getLevel().getServer().getPlayerList().remove(other);}
    }
    @GameTest public void hydratingCellUsesWaterOnlyWhenPermittedSoilChanges(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,1,2);h.setBlock(pos,Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE,0));
            var at=h.absolutePos(pos);var stack=IndustrialProduction.Cell.filled(new ItemStack(IndustrialCrops.ITEMS.get("jimta_hydrating_cell")),net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),IndustrialProduction.Cell.CAPACITY);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
            var use=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),Direction.UP,at,false));
            try(var claim=TestClaims.open().block(at,UUID.randomUUID())){stack.getItem().useOn(use);}
            h.assertTrue(IndustrialProduction.Cell.contents(player.getMainHandItem()).amount()==IndustrialProduction.Cell.CAPACITY&&h.getBlockState(pos).getValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE)==0,"Denied hydration preserves soil and water");
            stack.getItem().useOn(use);
            long remaining=IndustrialProduction.Cell.CAPACITY-HydratingCell.DOSE;
            h.assertTrue(IndustrialProduction.Cell.contents(player.getMainHandItem()).amount()==remaining&&h.getBlockState(pos).getValue(net.minecraft.world.level.block.FarmlandBlock.MOISTURE)==7,"One soil restoration spends exactly 25 mB");
            player.getMainHandItem().getItem().useOn(use);
            h.assertTrue(IndustrialProduction.Cell.contents(player.getMainHandItem()).amount()==remaining,"Already moist soil spends nothing");
            var storage=IndustrialProduction.Cell.storage(net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext.ofPlayerHand(player,net.minecraft.world.InteractionHand.MAIN_HAND));
            try(var tx=net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()){
                storage.extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER),remaining,tx);
                h.assertTrue(storage.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.LAVA),81,tx)==0,"Empty hydrating cell rejects lava");
            }
            h.assertTrue(IndustrialProduction.Cell.contents(player.getMainHandItem()).amount()==remaining,"Aborted transfer restores the held water");h.succeed();
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}
    }
    @GameTest public void phasedFacadePersistsAndNeverChangesItsPhysicalRouting(GameTestHelper h){
        var pos=new BlockPos(2,2,2);h.setBlock(pos,IndustrialTransport.BLOCKS.get("jimta_pipe_stone_item"));
        var node=h.getBlockEntity(pos,TransportAttachments.Node.class);var fittings=node.attachments();
        var base=TransportFacades.stack(Blocks.BRICKS.defaultBlockState(),TransportFacades.SEALED);base.setCount(12);
        var result=TransportFacades.PHASE_RECIPE.assemble(CraftingInput.of(3,1,List.of(base,new ItemStack(Items.DYE.red()),new ItemStack(Items.GLASS))));
        h.assertTrue(result.getCount()==1&&base.getCount()==12,"Recipe previews produce one facade without mutating inputs");
        fittings.faces.set(Direction.UP.ordinal(),Blocks.BRICKS.defaultBlockState());fittings.facadeModes[Direction.UP.ordinal()]=TransportFacades.SEALED;fittings.phases.set(Direction.UP.ordinal(),result.get(TransportFacades.PHASE));
        var relay=JugcraftControl.RELAY.defaultBlockState().setValue(Channels.CHANNEL,DyeColor.RED).setValue(RelayBlock.POWERED,true);h.setBlock(pos.east(),relay);
        h.assertTrue(TransportFacades.appearance(fittings,Direction.UP.ordinal()).isAir()&&fittings.blocked(Direction.UP),"Powered phase hides appearance but cannot bypass a sealed port");
        h.setBlock(pos.east(),relay.setValue(Channels.CHANNEL,DyeColor.BLUE));
        h.assertTrue(TransportFacades.appearance(fittings,Direction.UP.ordinal()).is(Blocks.BRICKS),"Other color cannot activate the phase");
        var saved=node.saveWithoutMetadata(h.getLevel().registryAccess());var copy=new TransportAttachments.Node(h.absolutePos(pos),node.getBlockState());copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        var dropped=copy.attachments().facadeStack(Direction.UP);h.assertTrue(dropped.get(TransportFacades.PHASE).equals(result.get(TransportFacades.PHASE)),"Reload/removal preserve the phase component");
        var converted=TransportFacades.CONVERSION.assemble(CraftingInput.of(1,1,List.of(dropped)));
        h.assertTrue(converted.is(TransportFacades.HOLLOW)&&converted.get(TransportFacades.PHASE).equals(result.get(TransportFacades.PHASE)),"Hollow conversion preserves channel and appearance");h.succeed();
    }
    @GameTest public void dispenserDynamiteRequiresBindingAndCurrentClaimAuthority(GameTestHelper h){
        var player=JimtaRobotGameTests.player(h);try{
            var pos=new BlockPos(2,2,2);h.setBlock(pos,Blocks.DISPENSER);h.setBlock(pos.east(),Blocks.AIR);var at=h.absolutePos(pos);var stack=new ItemStack(IndustrialDynamite.STICKY,3);
            h.assertTrue(!IndustrialDynamite.dispense(h.getLevel(),at,Direction.EAST,stack)&&stack.getCount()==3,"Unbound stacks are retained");
            stack.set(IndustrialDynamite.DISPENSER_OWNER,player.getUUID());
            try(var claim=TestClaims.open().block(at.east(),UUID.randomUUID())){h.assertTrue(!IndustrialDynamite.dispense(h.getLevel(),at,Direction.EAST,stack)&&stack.getCount()==3,"Protected muzzle rejects without consumption");}
            h.setBlock(pos.east(),Blocks.STONE);h.assertTrue(!IndustrialDynamite.dispense(h.getLevel(),at,Direction.EAST,stack),"Solid muzzle cannot fire through its wall");h.setBlock(pos.east(),Blocks.AIR);
            h.assertTrue(IndustrialDynamite.dispense(h.getLevel(),at,Direction.EAST,stack)&&stack.getCount()==2,"Permitted shot consumes exactly one bound charge");
        }finally{h.getLevel().getServer().getPlayerList().remove(player);}h.succeed();
    }
    @GameTest public void decorativeStatuesRetainTheirKindWithoutPlayerSkinState(GameTestHelper h){
        for(String kind:List.of("keeper","ice_spider")){
            var pos=new BlockPos(2,2,2);h.setBlock(pos.above(),Blocks.AIR);h.setBlock(pos,Blocks.AIR);var block=JimtaStatues.DECORATIVE.get(kind);h.setBlock(pos,block);h.setBlock(pos.above(),block.defaultBlockState().setValue(JimtaStatues.Y,1));
            var node=h.getBlockEntity(pos,JimtaStatues.Node.class);var snapshot=node.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new JimtaStatues.Node(h.absolutePos(pos),node.getBlockState());restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),snapshot));
            h.assertTrue(restored.kind().equals(kind)&&restored.profile==null,"Creature identity comes from its block, not a player profile");
            h.assertTrue(h.getLevel().getBlockEntity(h.absolutePos(pos.above()))==null,"Upper half has no duplicate inventory or renderer");
        }h.succeed();
    }
}
