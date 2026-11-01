package io.github.jimbozoomer.jugcraft.backpack;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;

/** The same finite tank module travels between installed and held forms. */
final class OwnerBackpackFluids {
    static final long CAPACITY=4*FluidConstants.BUCKET;
    static void register(){
        var items=new java.util.ArrayList<Item>();for(String tier:OwnerBackpacks.TIERS)items.add(OwnerBackpacks.ITEMS.get(tier));items.add(OwnerBackpacks.ITEMS.get("tank_upgrade"));
        FluidStorage.ITEM.registerForItems((stack,context)->tank(stack).isEmpty()?null:storage(context),items.toArray(Item[]::new));
    }
    private static ItemStack tank(ItemStack stack){
        if(stack.is(OwnerBackpacks.ITEMS.get("tank_upgrade")))return stack;
        return stack.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).nonEmptyItemCopyStream().filter(s->s.is(OwnerBackpacks.ITEMS.get("tank_upgrade"))).findFirst().orElse(ItemStack.EMPTY);
    }
    static StoredFluid contents(ItemStack stack){var item=tank(stack);return item.isEmpty()?null:item.get(JugcraftFluids.STORED_FLUID);}
    static ItemStack filled(ItemStack original,FluidVariant fluid,long amount){
        var copy=original.copyWithCount(1);var upgrades=NonNullList.withSize(7,ItemStack.EMPTY);
        copy.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY).copyInto(upgrades);
        var target=copy.is(OwnerBackpacks.ITEMS.get("tank_upgrade"))?copy:upgrades.stream().filter(s->s.is(OwnerBackpacks.ITEMS.get("tank_upgrade"))).findFirst().orElse(ItemStack.EMPTY);
        if(target.isEmpty())return original;
        if(amount<=0)target.remove(JugcraftFluids.STORED_FLUID);else target.set(JugcraftFluids.STORED_FLUID,new StoredFluid(fluid,Math.min(CAPACITY,amount)));
        if(target!=copy)copy.set(OwnerBackpacks.UPGRADES,ItemContainerContents.fromItems(upgrades));return copy;
    }
    static SingleSlotStorage<FluidVariant> storage(ContainerItemContext context){return new SingleSlotStorage<>(){
        private ItemStack item(){return context.getItemVariant().toStack();}
        public boolean isResourceBlank(){return getAmount()<=0;}
        public FluidVariant getResource(){var value=contents(item());return value==null||value.amount()<=0?FluidVariant.blank():value.variant();}
        public long getAmount(){var value=contents(item());return value==null?0:Math.clamp(value.amount(),0,CAPACITY);}
        public long getCapacity(){return tank(item()).isEmpty()?0:CAPACITY;}
        private boolean change(FluidVariant variant,long amount,TransactionContext tx){var before=item();return !tank(before).isEmpty()&&context.exchange(ItemVariant.of(filled(before,variant,amount)),1,tx)==1;}
        public long insert(FluidVariant fluid,long limit,TransactionContext tx){
            if(fluid.isBlank()||limit<=0||!isResourceBlank()&&!getResource().equals(fluid))return 0;
            long old=getAmount(),moved=Math.min(limit,getCapacity()-old);return moved>0&&change(fluid,old+moved,tx)?moved:0;
        }
        public long extract(FluidVariant fluid,long limit,TransactionContext tx){
            if(limit<=0||!fluid.equals(getResource()))return 0;long old=getAmount(),moved=Math.min(limit,old);return moved>0&&change(fluid,old-moved,tx)?moved:0;
        }
    };}
    static InteractionResult useOn(UseOnContext c){
        if(tank(c.getItemInHand()).isEmpty())return InteractionResult.PASS;
        if(!(c.getPlayer() instanceof ServerPlayer player))return InteractionResult.SUCCESS;
        if(!Authority.mayChange(player.level(),player,c.getClickedPos()))return InteractionResult.FAIL;
        if(OwnerBackpacks.tier(c.getItemInHand())>=0&&!OwnerBackpackUpgrades.accessible(player,c.getItemInHand()))return InteractionResult.FAIL;
        var external=FluidStorage.SIDED.find(player.level(),c.getClickedPos(),c.getClickedFace());if(external==null)return world(c,player);
        var internal=storage(ContainerItemContext.ofPlayerHand(player,c.getHand()));long moved;
        try(var tx=Transaction.openOuter()){
            moved=player.isShiftKeyDown()?StorageUtil.move(internal,external,v->true,FluidConstants.BUCKET,tx):StorageUtil.move(external,internal,v->true,FluidConstants.BUCKET,tx);tx.commit();
        }
        player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(moved>0?"Transferred "+moved/FluidNetworks.DROPLETS_PER_MB+" mB.":"No compatible fluid could be transferred."));return InteractionResult.SUCCESS;
    }
    private static InteractionResult world(UseOnContext context,ServerPlayer player){
        var level=player.level();var at=context.getClickedPos();var state=level.getBlockState(at);
        var internal=storage(ContainerItemContext.ofPlayerHand(player,context.getHand()));
        if(!player.isShiftKeyDown()){
            if(!state.is(net.minecraft.world.level.block.Blocks.WATER)&&!state.is(net.minecraft.world.level.block.Blocks.LAVA)||!state.getFluidState().isSource())return InteractionResult.PASS;
            var fluid=FluidVariant.of(state.getFluidState().getType());
            try(var tx=Transaction.openOuter()){
                if(internal.insert(fluid,FluidConstants.BUCKET,tx)!=FluidConstants.BUCKET)return InteractionResult.FAIL;
                if(!level.setBlock(at,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),net.minecraft.world.level.block.Block.UPDATE_ALL))return InteractionResult.FAIL;
                tx.commit();
            }
            level.playSound(null,at,state.is(net.minecraft.world.level.block.Blocks.LAVA)?net.minecraft.sounds.SoundEvents.BUCKET_FILL_LAVA:net.minecraft.sounds.SoundEvents.BUCKET_FILL,net.minecraft.sounds.SoundSource.PLAYERS,1,1);return InteractionResult.SUCCESS;
        }
        at=at.relative(context.getClickedFace());
        if(!Authority.mayChange(level,player,at)||internal.getAmount()<FluidConstants.BUCKET)return InteractionResult.FAIL;
        var fluid=internal.getResource().getFluid();
        var bucket=fluid==net.minecraft.world.level.material.Fluids.WATER?Items.WATER_BUCKET:fluid==net.minecraft.world.level.material.Fluids.LAVA?Items.LAVA_BUCKET:null;
        if(!(bucket instanceof BucketItem item))return InteractionResult.PASS;
        try(var tx=Transaction.openOuter()){
            if(internal.extract(internal.getResource(),FluidConstants.BUCKET,tx)!=FluidConstants.BUCKET)return InteractionResult.FAIL;
            // Empty at this exact checked cell: a null hit prevents BucketItem trying an unchecked adjacent cell.
            if(!item.emptyContents(player,level,at,null))return InteractionResult.FAIL;
            tx.commit();return InteractionResult.SUCCESS;
        }
    }
    static boolean settings(ServerPlayer player,ItemStack pack,int action){
        if(!OwnerBackpacks.has(pack,"advanced_pump_upgrade")||!OwnerBackpacks.has(pack,"tank_upgrade"))return false;
        var tag=pack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(action==60)tag.putBoolean("PackPumpAuto",!tag.getBooleanOr("PackPumpAuto",false));
        else if(action==61)tag.putBoolean("PackPumpFillItems",!tag.getBooleanOr("PackPumpFillItems",false));else return false;
        pack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));OwnerBackpacks.changed(player,pack);return true;
    }
    static int flags(ItemStack pack){var tag=pack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();return (tag.getBooleanOr("PackPumpAuto",false)?1:0)|(tag.getBooleanOr("PackPumpFillItems",false)?2:0);}
    static void automatic(ServerPlayer player,ItemStack pack){
        if(player.level().getServer().getTickCount()%20!=0||!OwnerBackpacks.has(pack,"advanced_pump_upgrade")||(flags(pack)&1)==0||tank(pack).isEmpty())return;
        var original=new OwnerBackpackInventory(pack,()->{});
        int cursor=pack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getIntOr("PackPumpCursor",0);
        for(int scanned=0;scanned<8;scanned++){
            int slot=Math.floorMod(cursor+scanned,original.getContainerSize());var candidate=original.getItem(slot);if(candidate.isEmpty()||OwnerBackpacks.tier(candidate)>=0)continue;
            var simulation=new OwnerBackpackInventory(pack.copy(),()->{});simulation.removeItem(slot,1);
            var temp=new net.minecraft.world.SimpleContainer(simulation.carrier,candidate.copyWithCount(1));
            var slots=net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(temp,null);
            var packStorage=storage(ContainerItemContext.ofSingleSlot(slots.getSlot(0)));
            var itemStorage=ContainerItemContext.ofSingleSlot(slots.getSlot(1)).find(FluidStorage.ITEM);if(itemStorage==null)continue;
            try(var tx=Transaction.openOuter()){
                long moved=(flags(pack)&2)!=0?StorageUtil.move(packStorage,itemStorage,v->true,FluidConstants.BUCKET,tx):StorageUtil.move(itemStorage,packStorage,v->true,FluidConstants.BUCKET,tx);
                if(moved<=0)continue;tx.commit();
            }
            var result=new OwnerBackpackInventory(temp.getItem(0),()->{});if(!temp.getItem(1).isEmpty()&&!result.insert(temp.getItem(1)).isEmpty())continue;
            pack.set(OwnerBackpacks.CONTENTS,result.carrier.getOrDefault(OwnerBackpacks.CONTENTS,java.util.List.of()));
            pack.remove(net.minecraft.core.component.DataComponents.CONTAINER);
            pack.set(OwnerBackpacks.UPGRADES,result.carrier.getOrDefault(OwnerBackpacks.UPGRADES,ItemContainerContents.EMPTY));OwnerBackpacks.changed(player,pack);break;
        }
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,pack,t->t.putInt("PackPumpCursor",Math.floorMod(cursor+8,original.getContainerSize())));
    }

}
