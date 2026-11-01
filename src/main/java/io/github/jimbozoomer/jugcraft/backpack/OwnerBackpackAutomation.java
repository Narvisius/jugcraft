package io.github.jimbozoomer.jugcraft.backpack;

import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

/** Transactional placed ports. Menus, ownership checks and creature reservations are shared with held packs. */
final class OwnerBackpackAutomation {
    private OwnerBackpackAutomation(){}
    static void register(){
        ItemStorage.SIDED.registerForBlockEntity((bag,side)->bag.itemPort,OwnerBackpacks.ENTITY);
        FluidStorage.SIDED.registerForBlockEntity((bag,side)->bag.fluidPort,OwnerBackpacks.ENTITY);
    }
    static final class Items extends SnapshotParticipant<ItemStack> implements Storage<ItemVariant> {
        private final OwnerBackpackEntity bag;
        Items(OwnerBackpackEntity bag){this.bag=bag;}
        @Override protected ItemStack createSnapshot(){return bag.carrier().copy();}
        @Override protected void readSnapshot(ItemStack state){bag.restoreCarrier(state);}
        @Override protected void onFinalCommit(){bag.setChanged();}
        @Override public long insert(ItemVariant variant,long amount,TransactionContext tx){
            if(variant.isBlank()||amount<=0||!bag.mayAutomate()||OwnerBackpackInfinity.adminPack(bag.carrier())||!OwnerBackpackFilters.matches(bag.carrier(),variant.toStack(),"filter",1))return 0;
            updateSnapshots(tx);int offered=(int)Math.min(Integer.MAX_VALUE,amount);
            return offered-bag.inventory().insert(variant.toStack(offered)).getCount();
        }
        private long take(int slot,ItemVariant variant,long amount,TransactionContext tx){
            if(variant.isBlank()||amount<=0||!bag.mayAutomate()||!OwnerBackpackFilters.matches(bag.carrier(),variant.toStack(),"filter",2))return 0;
            var item=bag.inventory().getItem(slot);if(!variant.matches(item)||OwnerBackpackMobs.occupied(bag.carrier(),slot))return 0;
            int take=(int)Math.min(amount,OwnerBackpackInfinity.active(bag.carrier())?256:item.getCount());if(take<=0)return 0;updateSnapshots(tx);if(!OwnerBackpackInfinity.active(bag.carrier()))bag.inventory().removeItem(slot,take);return take;
        }
        @Override public long extract(ItemVariant variant,long amount,TransactionContext tx){
            long moved=0;for(int i=0;i<bag.inventory().getContainerSize()&&moved<amount;i++)moved+=take(i,variant,amount-moved,tx);return moved;
        }
        @Override public Iterator<StorageView<ItemVariant>> iterator(){
            if(!bag.mayAutomate())return Collections.emptyIterator();
            var views=new ArrayList<StorageView<ItemVariant>>();
            for(int i=0;i<bag.inventory().getContainerSize();i++){final int slot=i;views.add(new StorageView<>(){
                public boolean isResourceBlank(){return getResource().isBlank();}
                public ItemVariant getResource(){return ItemVariant.of(bag.inventory().getItem(slot));}
                public long getAmount(){return OwnerBackpackInfinity.retained(bag.carrier(),bag.inventory().getItem(slot))?Integer.MAX_VALUE:bag.inventory().getItem(slot).getCount();}
                public long getCapacity(){return OwnerBackpackStacks.limit(bag.carrier(),bag.inventory().getItem(slot));}
                public long extract(ItemVariant resource,long amount,TransactionContext tx){return take(slot,resource,amount,tx);}
            });}
            return views.iterator();
        }
    }
    static final class Fluids extends SnapshotParticipant<ItemStack> implements SingleSlotStorage<FluidVariant> {
        private final OwnerBackpackEntity bag;
        Fluids(OwnerBackpackEntity bag){this.bag=bag;}
        @Override protected ItemStack createSnapshot(){return bag.carrier().copy();}
        @Override protected void readSnapshot(ItemStack state){bag.restoreCarrier(state);}
        @Override protected void onFinalCommit(){bag.setChanged();}
        public boolean isResourceBlank(){return getAmount()==0;}
        public FluidVariant getResource(){var stored=OwnerBackpackFluids.contents(bag.carrier());return stored==null?FluidVariant.blank():stored.variant();}
        public long getAmount(){var stored=OwnerBackpackFluids.contents(bag.carrier());return stored==null?0:stored.amount();}
        public long getCapacity(){return OwnerBackpacks.has(bag.carrier(),"tank_upgrade")?OwnerBackpackFluids.CAPACITY:0;}
        public long insert(FluidVariant fluid,long amount,TransactionContext tx){
            if(fluid.isBlank()||amount<=0||!bag.mayAutomate()||!isResourceBlank()&&!fluid.equals(getResource()))return 0;
            long moved=Math.min(amount,Math.max(0,getCapacity()-getAmount()));if(moved<=0)return 0;
            updateSnapshots(tx);bag.restoreCarrier(OwnerBackpackFluids.filled(bag.carrier(),fluid,getAmount()+moved));return moved;
        }
        public long extract(FluidVariant fluid,long amount,TransactionContext tx){
            if(fluid.isBlank()||amount<=0||!bag.mayAutomate()||!fluid.equals(getResource()))return 0;
            long moved=Math.min(amount,getAmount());if(moved<=0)return 0;
            updateSnapshots(tx);bag.restoreCarrier(OwnerBackpackFluids.filled(bag.carrier(),fluid,getAmount()-moved));return moved;
        }
    }
}
