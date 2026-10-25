package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Finite item/fluid cell loading and evacuation. Cell changes and network JE share the transaction. */
final class DigitalCellIO {
    private DigitalCellIO(){}
    static boolean accepts(ItemStack cell){return !DigitalCreativeCell.creative(cell)&&(cell.getItem() instanceof DigitalStorage.CellItem||cell.getItem() instanceof FluidCells.Cell);}
    static void tick(DigitalNode node,DigitalNetwork network,ServerPlayer actor){
        ItemStack cell=node.getItem(0);if(!accepts(cell)||!node.getItem(1).isEmpty())return;
        boolean fill=node.emitBelow;
        if(cell.getItem() instanceof DigitalStorage.CellItem type){
            var contents=cell.getOrDefault(DigitalStorage.CONTENTS,CellContents.EMPTY);
            if(fill?contents.amount()>=type.capacity:contents.entries().isEmpty()){finish(node,cell);return;}
            if(fill){
                var available=network.contents();
                for(int examined=0;examined<Math.min(512,available.size());examined++){
                    node.ioCursor=Math.floorMod(node.ioCursor,available.size());var entry=available.get(node.ioCursor++);
                    var sample=entry.sample();if(DigitalWorkshops.filtered(cell,sample))continue;
                    int room=contents.accepted(sample.copyWithCount(Math.min(64,entry.count())),type.capacity);if(room==0)continue;
                    try(var tx=Transaction.openOuter()){
                        int moved=(int)network.extract(sample,room,tx);
                        if(moved>0){node.snapshotCells(tx);cell.set(DigitalStorage.CONTENTS,contents.add(sample,moved));tx.commit();return;}
                    }
                }
            }else{
                var entry=contents.entries().getFirst();
                try(var tx=Transaction.openOuter()){
                    int moved=(int)network.insert(entry.sample(),Math.min(64,entry.count()),tx);
                    if(moved>0){node.snapshotCells(tx);cell.set(DigitalStorage.CONTENTS,contents.remove(entry.sample(),moved));tx.commit();}
                }
            }
        }else if(cell.getItem() instanceof FluidCells.Cell type){
            var contents=cell.getOrDefault(FluidCells.CONTENTS,FluidCells.Contents.EMPTY);var port=new FluidCells.Port(node,actor);
            if(fill?contents.used()>=type.capacity:contents.entries().isEmpty()){finish(node,cell);return;}
            if(fill){
                var available=new java.util.ArrayList<>(port.amounts().entrySet());
                for(int examined=0;examined<Math.min(512,available.size());examined++){
                    node.ioCursor=Math.floorMod(node.ioCursor,available.size());var entry=available.get(node.ioCursor++);var fluid=entry.getKey();
                    if(FluidCells.filtered(cell,fluid)||contents.entries().size()>=63&&contents.amount(fluid)==0)continue;
                    long room=Math.min(FluidConstants.BUCKET,Math.min(type.capacity-contents.used(),entry.getValue()));if(room<=0)continue;
                    try(var tx=Transaction.openOuter()){
                        long moved=port.extract(fluid,room,tx);
                        if(moved>0){node.snapshotCells(tx);cell.set(FluidCells.CONTENTS,contents.change(fluid,moved));tx.commit();return;}
                    }
                }
            }else{
                var entry=contents.entries().getFirst();
                try(var tx=Transaction.openOuter()){
                    long moved=port.insert(entry.fluid(),Math.min(FluidConstants.BUCKET,entry.amount()),tx);
                    if(moved>0){node.snapshotCells(tx);cell.set(FluidCells.CONTENTS,contents.change(entry.fluid(),-moved));tx.commit();}
                }
            }
        }
    }
    private static void finish(DigitalNode node,ItemStack cell){node.setItem(1,cell);node.setItem(0,ItemStack.EMPTY);}
    static final class Port implements net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>{
        private final DigitalNode node;private final net.minecraft.core.Direction side;
        Port(DigitalNode node,net.minecraft.core.Direction side){this.node=node;this.side=side;}
        private boolean allowed(){var actor=node.actor();return actor!=null&&node.permitted(actor);}
        @Override public boolean supportsInsertion(){return side==null||side==net.minecraft.core.Direction.UP;}
        @Override public boolean supportsExtraction(){return side!=net.minecraft.core.Direction.UP;}
        @Override public long insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant resource,long max,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){
            if(!supportsInsertion()||!allowed()||max<=0||resource.isBlank()||!node.getItem(0).isEmpty()||!accepts(resource.toStack()))return 0;
            node.snapshotCells(tx);node.setItem(0,resource.toStack());return 1;
        }
        @Override public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant resource,long max,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){
            if(!supportsExtraction()||!allowed()||max<=0||resource.isBlank()||!resource.matches(node.getItem(1)))return 0;
            int n=(int)Math.min(max,node.getItem(1).getCount());if(n<=0)return 0;
            node.snapshotCells(tx);node.getItem(1).shrink(n);return n;
        }
        @Override public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>> iterator(){
            if(!supportsExtraction()||!allowed()||node.getItem(1).isEmpty())return java.util.Collections.emptyIterator();
            var resource=net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(node.getItem(1));
            net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> view=new net.fabricmc.fabric.api.transfer.v1.storage.StorageView<>(){
                public long extract(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant requested,long amount,net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext tx){return resource.equals(requested)?Port.this.extract(requested,amount,tx):0;}
                public net.fabricmc.fabric.api.transfer.v1.item.ItemVariant getResource(){return resource;}
                public boolean isResourceBlank(){return false;}
                public long getAmount(){return allowed()&&resource.matches(node.getItem(1))?node.getItem(1).getCount():0;}
                public long getCapacity(){return 1;}
            };
            return java.util.List.of(view).iterator();
        }
    }
}
