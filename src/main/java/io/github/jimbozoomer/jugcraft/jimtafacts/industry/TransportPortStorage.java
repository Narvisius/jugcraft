package io.github.jimbozoomer.jugcraft.jimtafacts.industry;

import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.storage.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Rechecks the live face even through storage views cached before fitting a plug. */
public final class TransportPortStorage {
    private TransportPortStorage(){}
    public static boolean live(BlockEntity node,Direction side){
        if(!liveEnergy(node,side))return false;
        var fitting=TransportAttachments.at(node.getLevel(),node.getBlockPos());
        if(fitting==null)return true;
        return side==null?fitting.powerFace<0:fitting.powerFace!=side.ordinal();
    }
    public static boolean liveEnergy(BlockEntity node,Direction side){
        var level=node.getLevel();
        if(level==null||node.isRemoved()||!level.hasChunkAt(node.getBlockPos())||level.getBlockEntity(node.getBlockPos())!=node)return false;
        var fittings=TransportAttachments.at(level,node.getBlockPos());
        if(fittings!=null&&fittings.blocked(side))return false;
        if(side!=null&&!TransportAttachments.colorsOpen(level,node.getBlockPos(),side))return false;
        if(side!=null&&!io.github.jimbozoomer.jugcraft.finance.ClaimTransfers.allowed(level,node.getBlockPos(),node.getBlockPos().relative(side)))return false;
        return true;
    }
    public static <T extends TransferVariant<?>> Storage<T> guard(BlockEntity node,Direction side,Storage<T> storage){
        return new Storage<>(){
            public long insert(T resource,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(resource,amount);return live(node,side)?storage.insert(resource,amount,tx):0;}
            public long extract(T resource,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(resource,amount);return live(node,side)?storage.extract(resource,amount,tx):0;}
            public boolean supportsInsertion(){return live(node,side)&&storage.supportsInsertion();}
            public boolean supportsExtraction(){return live(node,side)&&storage.supportsExtraction();}
            public Iterator<StorageView<T>> iterator(){
                if(!live(node,side))return java.util.Collections.emptyIterator();
                var it=storage.iterator();
                return new Iterator<>(){
                    public boolean hasNext(){return live(node,side)&&it.hasNext();}
                    public StorageView<T> next(){var view=it.next();return new StorageView<>(){
                        public boolean isResourceBlank(){return view.isResourceBlank();}
                        public T getResource(){return view.getResource();}
                        public long getAmount(){return live(node,side)?view.getAmount():0;}
                        public long getCapacity(){return live(node,side)?view.getCapacity():0;}
                        public long extract(T resource,long amount,TransactionContext tx){StoragePreconditions.notBlankNotNegative(resource,amount);return live(node,side)?view.extract(resource,amount,tx):0;}
                    };}
                };
            }
        };
    }
}
