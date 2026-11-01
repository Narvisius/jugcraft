package io.github.jimbozoomer.jugcraft.menagerie;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.material.Fluids;

/** A faucet moves real cargo, never a visual copy, across three authorized loaded cells. */
final class OwnerSupplementTransfers {
    private OwnerSupplementTransfers(){}
    static void faucet(OwnerSupplementData d,ServerLevel level){
        var pos=d.getBlockPos();var state=d.getBlockState();boolean powered=level.hasNeighborSignal(pos);
        boolean open=powered^HouseholdBlock.flag(state,"enabled");
        var next=HouseholdBlock.set(state,"powered",Boolean.toString(powered));
        boolean water=false;
        if(open){
            var actor=Authority.present(level,d.owner);var facing=HouseholdBlock.facing(state);
            var from=pos.relative(facing.getOpposite());var to=pos.below();
            if(actor!=null&&level.hasChunkAt(from)&&level.hasChunkAt(to)&&Authority.mayChange(level,actor,pos)&&Authority.mayChange(level,actor,from)&&Authority.mayChange(level,actor,to)&&opened(level,from)&&opened(level,to)){
                var input=FluidStorage.SIDED.find(level,from,facing);var output=FluidStorage.SIDED.find(level,to,Direction.UP);
                long amount=0;
                if(level.getBlockEntity(from) instanceof OwnerSupplementData source&&level.getBlockEntity(to) instanceof OwnerSupplementData destination&&OwnerSoftLiquids.move(source,destination))amount=FluidConstants.BOTTLE;
                if(amount==0&&input!=null&&output!=null){try(var tx=Transaction.openOuter()){
                    amount=StorageUtil.move(input,output,v->true,FluidConstants.BUCKET,tx);if(amount>0)tx.commit();
                }}
                if(amount>0)water=true;
                if(amount==0&&input!=null)amount=fillCauldron(level,to,input);
                if(amount==0&&output!=null){
                    amount=drainCauldron(level,from,output);
                    // World sources are consumed only after reserving a complete bucket.
                    if(amount==0){
                        var source=level.getBlockState(from);var fluid=source.getFluidState();
                        if(fluid.isSource()&&(source.is(Blocks.WATER)||source.is(Blocks.LAVA))&&source.getBlock() instanceof BucketPickup pickup){
                            try(var tx=Transaction.openOuter()){
                                if(output.insert(FluidVariant.of(fluid.getType()),FluidConstants.BUCKET,tx)==FluidConstants.BUCKET){
                                    var bucket=pickup.pickupBlock(actor,level,from,source);
                                    if(!bucket.isEmpty()){tx.commit();amount=FluidConstants.BUCKET;}
                                }
                            }
                        }
                    }
                }
                water=amount>0;
                if(amount==0){
                    var items=ItemStorage.SIDED.find(level,from,facing);var target=ItemStorage.SIDED.find(level,to,Direction.UP);
                    if(items!=null&&target!=null){try(var tx=Transaction.openOuter()){if(StorageUtil.move(items,target,v->true,1,tx)>0)tx.commit();}}
                    else if(items!=null&&level.getBlockState(to).isAir()){
                        int inspected=0;for(var view:items){if(++inspected>64)break;if(view.isResourceBlank()||view.getAmount()<=0)continue;
                            try(var tx=Transaction.openOuter()){var variant=view.getResource();if(view.extract(variant,1,tx)!=1)continue;var drop=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()-.1,pos.getZ()+.5,variant.toStack());drop.setDeltaMovement(0,0,0);drop.setDefaultPickUpDelay();if(level.addFreshEntity(drop))tx.commit();}break;
                        }
                    }
                }
                if(water)level.sendParticles(ParticleTypes.DRIPPING_WATER,pos.getX()+.5,pos.getY()+.25,pos.getZ()+.5,2,.03,.03,.03,0);
            }
        }
        next=HouseholdBlock.set(next,"has_water",Boolean.toString(water));
        next=HouseholdBlock.set(next,"connected",Boolean.toString(level.getBlockState(pos.below()).getBlock() instanceof OwnerSupplementBlock b&&b.function.equals("faucet")));
        if(next!=state)level.setBlockAndUpdate(pos,next);
    }
    private static long fillCauldron(ServerLevel level,BlockPos pos,net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> input){
        var state=level.getBlockState(pos);
        if(state.is(Blocks.CAULDRON)||state.is(Blocks.WATER_CAULDRON)){
            int old=state.is(Blocks.WATER_CAULDRON)?state.getValue(LayeredCauldronBlock.LEVEL):0;
            if(old<3)try(var tx=Transaction.openOuter()){
                // A level is exactly one bottle. Failed/partial extraction rolls back.
                long amount=FluidConstants.BOTTLE;
                if(input.extract(FluidVariant.of(Fluids.WATER),amount,tx)==amount&&level.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,old+1))){tx.commit();return amount;}
            }
            if(old==0)try(var tx=Transaction.openOuter()){
                if(input.extract(FluidVariant.of(Fluids.LAVA),FluidConstants.BUCKET,tx)==FluidConstants.BUCKET&&level.setBlockAndUpdate(pos,Blocks.LAVA_CAULDRON.defaultBlockState())){tx.commit();return FluidConstants.BUCKET;}
            }
        }
        return 0;
    }
    private static long drainCauldron(ServerLevel level,BlockPos pos,net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> output){
        var state=level.getBlockState(pos);boolean water=state.is(Blocks.WATER_CAULDRON);
        if(!water&&!state.is(Blocks.LAVA_CAULDRON))return 0;
        int old=water?state.getValue(LayeredCauldronBlock.LEVEL):1;
        long amount=water?FluidConstants.BOTTLE:FluidConstants.BUCKET;
        var after=water&&old>1?state.setValue(LayeredCauldronBlock.LEVEL,old-1):Blocks.CAULDRON.defaultBlockState();
        try(var tx=Transaction.openOuter()){
            if(output.insert(FluidVariant.of(water?Fluids.WATER:Fluids.LAVA),amount,tx)==amount&&level.setBlockAndUpdate(pos,after)){tx.commit();return amount;}
        }
        return 0;
    }
    private static boolean opened(ServerLevel level,BlockPos pos){return !(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container)||container.getLootTable()==null;}
}
