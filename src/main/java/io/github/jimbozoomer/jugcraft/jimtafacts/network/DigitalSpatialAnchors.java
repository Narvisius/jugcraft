package io.github.jimbozoomer.jugcraft.jimtafacts.network;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;

/** Expiring, paid tickets for an already-loaded workshop chunk, not a remote chunk generator. */
final class DigitalSpatialAnchors {
    private static final int MAX_SERVER=32,MAX_OWNER=4;
    private static final TicketType TICKET=new TicketType(40,TicketType.FLAG_LOADING|TicketType.FLAG_SIMULATION|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private record Lease(UUID owner,long expires) {}
    private static final Map<MinecraftServer,Map<GlobalPos,Lease>> LEASES=new WeakHashMap<>();
    private DigitalSpatialAnchors(){}
    static void register(){
        Registry.register(BuiltInRegistries.TICKET_TYPE,Jugcraft.id("jimta_spatial_anchor"),TICKET);
        ServerLifecycleEvents.SERVER_STOPPED.register(LEASES::remove);
    }
    static boolean tick(DigitalNode node,DigitalNetwork network){
        if(!(node.getLevel() instanceof ServerLevel level)||!node.kind().equals("spatial_anchor"))return false;
        if(!network.online()||level.hasNeighborSignal(node.getBlockPos())){release(node);node.processingStatus="Paused: needs permitted power and no redstone";return false;}
        var server=level.getServer();long now=server.getTickCount();var owner=node.ownerId();
        var leases=LEASES.computeIfAbsent(server,s->new LinkedHashMap<>());leases.values().removeIf(lease->lease.expires<=now);
        var key=GlobalPos.of(level.dimension(),node.getBlockPos());
        if(owner==null||!leases.containsKey(key)&&(leases.size()>=MAX_SERVER||leases.values().stream().filter(l->l.owner.equals(owner)).count()>=MAX_OWNER)){
            release(node);node.processingStatus="Anchor limit: four per owner, 32 per server";return false;
        }
        try(var tx=Transaction.openOuter()){
            if(!network.pay(100,tx)){release(node);node.processingStatus="Needs 100 JE per second";return false;}
            level.getChunkSource().addTicketWithRadius(TICKET,ChunkPos.containing(node.getBlockPos()),2);
            leases.put(key,new Lease(owner,now+40));tx.commit();
        }
        node.processingStatus="Keeping this chunk active · 100 JE/s · redstone pauses";return true;
    }
    static void release(DigitalNode node){
        if(!(node.getLevel() instanceof ServerLevel level)||!node.kind().equals("spatial_anchor"))return;
        var leases=LEASES.get(level.getServer());if(leases==null)return;
        var key=GlobalPos.of(level.dimension(),node.getBlockPos());if(leases.remove(key)==null)return;
        ChunkPos chunk=ChunkPos.containing(node.getBlockPos());long now=level.getServer().getTickCount();
        boolean shared=leases.entrySet().stream().anyMatch(e->e.getValue().expires>now&&e.getKey().dimension().equals(level.dimension())&&ChunkPos.containing(e.getKey().pos()).equals(chunk));
        if(!shared)level.getChunkSource().removeTicketWithRadius(TICKET,chunk,2);
    }
}
