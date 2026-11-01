package io.github.jimbozoomer.jugcraft.botany;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;

/** One bucket of water, with an actual bowl returned on draining or crafting. */
public final class BotanicalWaterBowl extends Item {
    BotanicalWaterBowl(Properties p){super(p.stacksTo(1).craftRemainder(Items.BOWL));}
    static void register(){
        UseItemCallback.EVENT.register((user,world,hand)->{
            var held=user.getItemInHand(hand);if(!held.is(Items.BOWL))return InteractionResult.PASS;
            var hit=getPlayerPOVHitResult(world,user,ClipContext.Fluid.SOURCE_ONLY);if(hit.getType()!=HitResult.Type.BLOCK||!world.getFluidState(hit.getBlockPos()).is(Fluids.WATER))return InteractionResult.PASS;
            if(!(user instanceof ServerPlayer p))return InteractionResult.SUCCESS;var at=hit.getBlockPos();if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
            var state=world.getBlockState(at);if(!(state.getBlock() instanceof BucketPickup pickup))return InteractionResult.PASS;
            var taken=pickup.pickupBlock(p,world,at,state);if(taken.isEmpty())return InteractionResult.FAIL;
            p.setItemInHand(hand,ItemUtils.createFilledResult(held,p,new ItemStack(Jugtania.item("water_bowl"))));return InteractionResult.SUCCESS;
        });
        FluidStorage.ITEM.registerForItems((stack,context)->new SingleSlotStorage<FluidVariant>(){
            private boolean full(){return context.getItemVariant().isOf(Jugtania.item("water_bowl"))&&context.getAmount()>0;}
            public boolean isResourceBlank(){return !full();}
            public FluidVariant getResource(){return full()?FluidVariant.of(Fluids.WATER):FluidVariant.blank();}
            public long getAmount(){return full()?FluidConstants.BUCKET:0;}
            public long getCapacity(){return FluidConstants.BUCKET;}
            public long insert(FluidVariant fluid,long amount,TransactionContext tx){return 0;}
            public long extract(FluidVariant fluid,long limit,TransactionContext tx){return full()&&fluid.equals(FluidVariant.of(Fluids.WATER))&&limit>=FluidConstants.BUCKET&&context.exchange(ItemVariant.of(Items.BOWL),1,tx)==1?FluidConstants.BUCKET:0;}
        },Jugtania.item("water_bowl"));
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        var at=c.getClickedPos();if(!Authority.mayChange(p.level(),p,at))return InteractionResult.FAIL;
        var tank=FluidStorage.SIDED.find(p.level(),at,c.getClickedFace());if(tank==null)return InteractionResult.PASS;
        try(var tx=Transaction.openOuter()){
            if(tank.insert(FluidVariant.of(Fluids.WATER),FluidConstants.BUCKET,tx)!=FluidConstants.BUCKET)return InteractionResult.FAIL;
            var context=ContainerItemContext.ofPlayerHand(p,c.getHand());
            if(!p.hasInfiniteMaterials()&&context.exchange(ItemVariant.of(Items.BOWL),1,tx)!=1)return InteractionResult.FAIL;
            tx.commit();return InteractionResult.SUCCESS;
        }
    }
}
