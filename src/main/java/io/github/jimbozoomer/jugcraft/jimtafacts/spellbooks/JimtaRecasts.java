package io.github.jimbozoomer.jugcraft.jimtafacts.spellbooks;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.server.level.ServerPlayer;

/** Short paid-once cast chains. Timeout starts the ordinary cooldown, never grants free fresh casts. */
public final class JimtaRecasts {
    private record Chain(int remaining,long expires){}
    private static final Map<ServerPlayer,Map<String,Chain>> ACTIVE=new WeakHashMap<>();
    private JimtaRecasts(){}
    public static void register(){ServerLifecycleEvents.SERVER_STOPPED.register(s->ACTIVE.clear());ServerTickEvents.END_SERVER_TICK.register(s->{for(var p:new ArrayList<>(ACTIVE.keySet())){var names=ACTIVE.get(p);for(var entry:new ArrayList<>(names.entrySet()))if(!p.isAlive()||p.isRemoved()||p.level().getGameTime()>=entry.getValue().expires())finish(p,entry.getKey());if(names.isEmpty())ACTIVE.remove(p);}});}
    public static boolean isPending(ServerPlayer p,String name){var chain=ACTIVE.getOrDefault(p,Map.of()).get(name);if(chain!=null&&p.level().getGameTime()>=chain.expires()){finish(p,name);return false;}return chain!=null&&chain.remaining()>0;}
    /** count includes the cast currently being performed. */
    public static void begin(ServerPlayer p,String name,int count,int windowTicks){ACTIVE.computeIfAbsent(p,k->new HashMap<>()).put(name,new Chain(Math.clamp(count-1,0,8),p.level().getGameTime()+Math.clamp(windowTicks,1,2400)));}
    public static void consume(ServerPlayer p,String name){var map=ACTIVE.get(p);if(map==null)return;var old=map.get(name);if(old==null)return;if(old.remaining()<=1)finish(p,name);else map.put(name,new Chain(old.remaining()-1,old.expires()));}
    public static void finish(ServerPlayer p,String name){var map=ACTIVE.get(p);if(map!=null)map.remove(name);var spell=JimtaSpellcasting.SPELLS.get(name);if(spell!=null)JimtaSpellcasting.cooldown(p,name,spell.cooldown());}
}
