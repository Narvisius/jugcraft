package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.*;

/** Physical job escrow. Items leave cells before reservation and return or drop on cancellation/removal. */
final class DigitalReservations extends SnapshotParticipant<Map<UUID,CellContents>> {
    private record Saved(UUID root,CellContents contents){
        static final Codec<Saved> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("root").forGetter(Saved::root),CellContents.CODEC.fieldOf("contents").forGetter(Saved::contents)).apply(i,Saved::new));
    }
    private final DigitalNode node;
    private final Map<UUID,CellContents> held=new LinkedHashMap<>();
    DigitalReservations(DigitalNode node){this.node=node;}
    @Override protected Map<UUID,CellContents> createSnapshot(){return new LinkedHashMap<>(held);}
    @Override protected void readSnapshot(Map<UUID,CellContents> snapshot){held.clear();held.putAll(snapshot);}
    @Override protected void onFinalCommit(){node.setChanged();}
    List<CellContents.Entry> contents(UUID root){return held.getOrDefault(root,CellContents.EMPTY).entries();}
    long extract(UUID root,ItemStack sample,long max,TransactionContext tx){
        var stored=held.getOrDefault(root,CellContents.EMPTY);int n=(int)Math.min(max,stored.count(sample));if(n<=0)return 0;
        updateSnapshots(tx);var left=stored.remove(sample,n);if(left.entries().isEmpty())held.remove(root);else held.put(root,left);return n;
    }
    /** Hold no more than one batch's direct ingredients (nine stacks), never theoretical future output. */
    static void reserve(DigitalNode node,UUID root,List<ItemStack> inputs,DigitalNetwork network){
        if(!network.online()||!node.reservations.held.containsKey(root)&&node.reservations.held.size()>=64)return;
        CellContents wanted=CellContents.EMPTY;
        for(var input:inputs)if(!input.isEmpty())wanted=wanted.add(input,input.getCount());
        try(var tx=Transaction.openOuter()){
            var stored=node.reservations.held.getOrDefault(root,CellContents.EMPTY);
            for(var entry:wanted.entries()){
                int heldElsewhere=network.nodes().stream().filter(n->n!=node).mapToInt(n->n.reservations.held.getOrDefault(root,CellContents.EMPTY).count(entry.sample())).sum();
                int missing=entry.count()-stored.count(entry.sample())-heldElsewhere;if(missing<=0)continue;
                int room=576-stored.amount();if(room<=0)break;
                long taken=network.forJob(null).extract(entry.sample(),Math.min(missing,room),tx);
                if(taken>0){node.reservations.updateSnapshots(tx);stored=stored.add(entry.sample(),(int)taken);node.reservations.held.put(root,stored);}
            }tx.commit();
        }
    }
    void release(UUID root,DigitalNetwork network){
        var stored=held.get(root);if(stored==null)return;
        try(var tx=Transaction.openOuter()){
            for(var entry:stored.entries()){
                long returned=network.forJob(null).insert(entry.sample(),entry.count(),tx);
                if(returned>0)extract(root,entry.sample(),returned,tx);
            }tx.commit();
        }
        // Storage can be full or unpowered. Return the actual remainder at this protected machine.
        drop(root);
    }
    private void drop(UUID root){
        var stored=held.remove(root);if(stored==null)return;
        if(node.getLevel()!=null&&!node.getLevel().isClientSide())for(var entry:stored.entries()){
            int remaining=entry.count();while(remaining>0){int count=Math.min(remaining,entry.sample().getMaxStackSize());
                Block.popResource(node.getLevel(),node.getBlockPos(),entry.sample().copyWithCount(count));remaining-=count;}
        }node.setChanged();
    }
    void dropAll(){for(UUID root:List.copyOf(held.keySet()))drop(root);}
    void clear(){held.clear();node.setChanged();}
    void releaseFinished(DigitalNetwork network){
        for(UUID root:List.copyOf(held.keySet()))if(network.nodes().stream().noneMatch(n->n.orders.stream().anyMatch(o->o.root().equals(root))||n.processing.stream().anyMatch(j->j.root().equals(root))))release(root,network);
    }
    void save(ValueOutput out){out.store("job_reservations",Saved.CODEC.listOf(0,64),held.entrySet().stream().map(e->new Saved(e.getKey(),e.getValue())).toList());}
    void load(ValueInput in){held.clear();for(var saved:in.read("job_reservations",Saved.CODEC.listOf(0,64)).orElse(List.of()))if(saved.contents.amount()<=576)held.put(saved.root,saved.contents);}
}
