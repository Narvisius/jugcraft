package local.peepo;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;

/** Server-wide admission budgets, shared across dimensions. Queues contain UUIDs, never entities/worlds. */
public final class CompanionBudget {
    private static final Map<MinecraftServer,CompanionBudget> SERVERS=new WeakHashMap<>();
    private final Lane searches=new Lane(),paths=new Lane(),socials=new Lane();
    private static final class Lane {
        final LinkedHashMap<UUID,Integer> waiting=new LinkedHashMap<>();
        int tick=-1,used;long granted,deferred;
        boolean take(UUID id,int now,int limit){
            if(tick!=now){tick=now;used=0;}
            // A goal that stopped requesting cannot hold up the queue indefinitely.
            for(int i=0;i<32 && !waiting.isEmpty();i++){
                var first=waiting.entrySet().iterator().next();
                if(now-first.getValue()<=20)break;waiting.remove(first.getKey());
            }
            if(!waiting.containsKey(id) && waiting.size()>=4096){deferred++;return false;}
            waiting.put(id,now);
            if(used>=limit || !waiting.keySet().iterator().next().equals(id)){deferred++;return false;}
            waiting.remove(id);used++;granted++;return true;
        }
    }
    private static int limit(String key,int fallback,int maximum){
        try{return Math.clamp(Integer.parseInt(JugcraftConfig.textOption(key)),1,maximum);}catch(RuntimeException ignored){return fallback;}
    }
    private static boolean take(PeepoEntity npc,boolean path){
        if(!(npc.level() instanceof ServerLevel level))return false;
        var server=level.getServer();var budget=SERVERS.computeIfAbsent(server,s->new CompanionBudget());
        return (path?budget.paths:budget.searches).take(npc.getUUID(),server.getTickCount(),path?
            limit("companions.paths_per_tick",8,64):limit("companions.searches_per_tick",4,32));
    }
    public static boolean search(PeepoEntity npc){return take(npc,false);}
    public static boolean path(PeepoEntity npc){return take(npc,true);}
    /** Cosmetic searches have their own small allowance; they cannot consume work/path slots. */
    public static boolean social(PeepoEntity npc){
        if(!(npc.level() instanceof ServerLevel level))return false;
        var server=level.getServer();var budget=SERVERS.computeIfAbsent(server,s->new CompanionBudget());
        return budget.socials.take(npc.getUUID(),server.getTickCount(),1);
    }
    public static String statistics(MinecraftServer server){
        var b=SERVERS.get(server);return b==null?"No companion requests":
            "Searches: "+b.searches.granted+" admitted, "+b.searches.deferred+" deferred; paths: "+b.paths.granted+" admitted, "+b.paths.deferred+" deferred; social: "+b.socials.granted+" admitted, "+b.socials.deferred+" deferred";
    }
    public static void initialize(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((level,chunk,generated)->io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks.invalidate(level));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_UNLOAD.register((level,chunk)->io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks.invalidate(level));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher,context,selection)->
            dispatcher.register(net.minecraft.commands.Commands.literal("peepobudget")
                .requires(source->net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(command->{command.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(statistics(command.getSource().getServer())),false);return 1;})));
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerEntityEvents.ENTITY_UNLOAD.register((entity,level)->{
            if(entity instanceof PeepoEntity npc){
                npc.unloadCompanionRoutine();
                var budget=SERVERS.get(level.getServer());
                if(budget!=null){budget.paths.waiting.remove(npc.getUUID());budget.searches.waiting.remove(npc.getUUID());budget.socials.waiting.remove(npc.getUUID());}
            }
        });
    }
}
