package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;

/** Saved cancellation acknowledgements; never loads participant chunks or stores a second item inventory. */
final class DigitalJobLedger extends SavedData {
    private static final int LIMIT=4096;
    private record Participant(GlobalPos position,int face){
        private static final Codec<Participant> CURRENT=RecordCodecBuilder.create(i->i.group(GlobalPos.CODEC.fieldOf("position").forGetter(Participant::position),Codec.intRange(-1,5).optionalFieldOf("face",-1).forGetter(Participant::face)).apply(i,Participant::new));
        // Earlier saves used a block position only; retain them as standalone-node participants.
        static final Codec<Participant> CODEC=Codec.either(CURRENT,GlobalPos.CODEC).xmap(e->e.map(p->p,p->new Participant(p,-1)),com.mojang.datafixers.util.Either::left);
    }
    private record Root(UUID id,boolean cancelled,List<Participant> participants){
        static final Codec<Root> CODEC=RecordCodecBuilder.create(i->i.group(UUIDUtil.CODEC.fieldOf("id").forGetter(Root::id),Codec.BOOL.fieldOf("cancelled").forGetter(Root::cancelled),Participant.CODEC.listOf(1,256).fieldOf("participants").forGetter(Root::participants)).apply(i,Root::new));
        Root{participants=List.copyOf(participants);}
    }
    private static final Codec<DigitalJobLedger> CODEC=Root.CODEC.listOf(0,LIMIT).xmap(DigitalJobLedger::new,d->List.copyOf(d.roots.values()));
    private static final SavedDataType<DigitalJobLedger> TYPE=new SavedDataType<>(Jugcraft.id("jimta_crafting_jobs"),()->new DigitalJobLedger(List.of()),CODEC,null);
    private final Map<UUID,Root> roots=new LinkedHashMap<>();
    private DigitalJobLedger(List<Root> saved){for(var root:saved)roots.put(root.id,root);}
    static DigitalJobLedger get(DigitalNode node){return ((ServerLevel)node.getLevel()).getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    private static Participant position(DigitalNode node){return new Participant(GlobalPos.of(node.getLevel().dimension(),node.getBlockPos()),node.host==null?-1:node.getBlockState().getValue(DigitalNodeBlock.FACING).ordinal());}
    boolean track(DigitalNode node,UUID id){
        var root=roots.get(id);var at=position(node);
        if(root!=null&&root.cancelled)return false;
        if(root!=null&&root.participants.contains(at))return true;
        if(root==null&&roots.size()>=LIMIT||root!=null&&root.participants.size()>=256)return false;
        var participants=new ArrayList<>(root==null?List.<Participant>of():root.participants);participants.add(at);
        roots.put(id,new Root(id,false,participants));setDirty();return true;
    }
    void cancel(UUID id){var root=roots.get(id);if(root!=null&&!root.cancelled){roots.put(id,new Root(id,true,root.participants));setDirty();}}
    boolean cancelled(UUID id){var root=roots.get(id);return root!=null&&root.cancelled;}
    void finished(DigitalNode node,UUID id){
        if(node.orders.stream().anyMatch(o->o.root().equals(id))||node.processing.stream().anyMatch(j->j.root().equals(id)))return;
        var root=roots.get(id);if(root==null)return;var participants=new ArrayList<>(root.participants);
        if(!participants.remove(position(node)))return;
        if(participants.isEmpty())roots.remove(id);else roots.put(id,new Root(id,root.cancelled,participants));setDirty();
    }
    /** Called before work, including after save/reload or reconnecting a disconnected provider. */
    boolean observe(DigitalNode node){
        var ids=new LinkedHashSet<UUID>();node.orders.forEach(o->ids.add(o.root()));node.processing.forEach(j->ids.add(j.root()));
        boolean ready=true;
        for(var id:ids){
            if(cancelled(id)){DigitalProcessing.cancelNode(node,id);finished(node,id);}
            else if(!track(node,id)){node.processingStatus="Server job ledger is full; finish or cancel existing jobs";ready=false;}
        }
        return ready;
    }
    void removed(DigitalNode node){
        var ids=new LinkedHashSet<UUID>();node.orders.forEach(o->ids.add(o.root()));node.processing.forEach(j->ids.add(j.root()));
        ids.forEach(this::cancel);node.orders.clear();node.processing.clear();ids.forEach(id->finished(node,id));
    }
}
