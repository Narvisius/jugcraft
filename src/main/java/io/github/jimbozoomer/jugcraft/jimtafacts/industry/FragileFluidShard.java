package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.context.UseOnContext;
import java.util.List;

/** Single-fill, extract-only once sealed. Final extraction consumes the shell in the same transaction. */
public final class FragileFluidShard extends Item {
    public static final long CAPACITY=500*FluidNetworks.DROPLETS_PER_MB;
    public FragileFluidShard(Properties properties){super(properties);}
    public static StoredFluid contents(ItemStack stack){
        var value=stack.get(JugcraftFluids.STORED_FLUID);
        return value==null||value.variant().isBlank()||value.amount()<=0?null:new StoredFluid(value.variant(),Math.min(CAPACITY,value.amount()));
    }
    private static ItemStack filled(ItemStack stack,FluidVariant fluid,long amount){
        var result=stack.copyWithCount(1);
        result.set(JugcraftFluids.STORED_FLUID,new StoredFluid(fluid,amount));
        result.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(1F),List.of(),List.of(),List.of()));
        return result;
    }
    public static SingleSlotStorage<FluidVariant> storage(ContainerItemContext context){return new SingleSlotStorage<>(){
        private boolean valid(){return !context.getItemVariant().isBlank()&&context.getAmount()>0&&context.getItemVariant().toStack().getItem() instanceof FragileFluidShard;}
        private StoredFluid held(){return valid()?contents(context.getItemVariant().toStack()):null;}
        @Override public long insert(FluidVariant fluid,long maximum,TransactionContext tx){
            if(!valid()||fluid.isBlank()||maximum<=0||held()!=null)return 0;
            long moved=Math.min(maximum,CAPACITY);
            return context.exchange(ItemVariant.of(filled(context.getItemVariant().toStack(),fluid,moved)),1,tx)==1?moved:0;
        }
        @Override public long extract(FluidVariant fluid,long maximum,TransactionContext tx){
            var value=held();if(value==null||maximum<=0||!value.variant().equals(fluid))return 0;
            long moved=Math.min(maximum,value.amount());
            // Never exchange an empty variant: remove the exact occupied slot atomically.
            boolean changed=moved==value.amount()?context.getMainSlot().extract(context.getItemVariant(),1,tx)==1:
                context.exchange(ItemVariant.of(filled(context.getItemVariant().toStack(),fluid,value.amount()-moved)),1,tx)==1;
            return changed?moved:0;
        }
        @Override public boolean isResourceBlank(){return held()==null;}
        @Override public FluidVariant getResource(){var v=held();return v==null?FluidVariant.blank():v.variant();}
        @Override public long getAmount(){var v=held();return v==null?0:v.amount();}
        @Override public long getCapacity(){return CAPACITY;}
    };}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        if(!(context.getLevel() instanceof ServerLevel level))return InteractionResult.SUCCESS;
        if(player.isSpectator()||!Authority.mayChange(level,player,context.getClickedPos()))return InteractionResult.FAIL;
        var tank=FluidStorage.SIDED.find(level,context.getClickedPos(),context.getClickedFace());if(tank==null)return InteractionResult.PASS;
        var shard=storage(ContainerItemContext.ofPlayerHand(player,context.getHand()));
        try(var tx=Transaction.openOuter()){
            long moved=contents(context.getItemInHand())==null?StorageUtil.move(tank,shard,v->true,CAPACITY,tx):StorageUtil.move(shard,tank,v->true,CAPACITY,tx);
            if(moved<=0)return InteractionResult.FAIL;
            tx.commit();
        }
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,display,lines,flag);
        var held=contents(stack);lines.accept(held==null?Component.literal("Empty — accepts one fill, up to 500 mB"):held.describe());
        lines.accept(Component.literal("Use on a tank. Sealed after filling; consumed when fully drained."));
    }
}
