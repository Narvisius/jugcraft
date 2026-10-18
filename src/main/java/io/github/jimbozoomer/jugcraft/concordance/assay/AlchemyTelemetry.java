package io.github.jimbozoomer.jugcraft.concordance.assay;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/** Owner-only power diagnostics, at most twice a second; never a client-authoritative wallet. */
public final class AlchemyTelemetry {
    private record Usage(long amount,long tick,long lowUntil,long cooldown){}
    private static final Map<UUID,Usage> USAGE=new HashMap<>();
    private static final Map<UUID,State> SENT=new HashMap<>();
    private AlchemyTelemetry(){}
    public static void register(){
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers())if(p.tickCount%10==0){
            long now=p.level().getGameTime();var u=USAGE.getOrDefault(p.getUUID(),new Usage(0,0,0,0));
            var state=new State(Assaying.balance(p),AlchemyPower.available(p)-Assaying.balance(p),now-u.tick<40?u.amount:0,now<u.lowUntil,(int)Math.max(0,u.cooldown-now));
            if(!state.equals(SENT.get(p.getUUID()))){SENT.put(p.getUUID(),state);ServerPlayNetworking.send(p,state);}
        }});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{USAGE.remove(h.getPlayer().getUUID());SENT.remove(h.getPlayer().getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{USAGE.clear();SENT.clear();});
    }
    public static void paid(ServerPlayer p,long amount){var old=USAGE.getOrDefault(p.getUUID(),new Usage(0,0,0,0));USAGE.put(p.getUUID(),new Usage(amount,p.level().getGameTime(),0,old.cooldown));}
    public static void low(ServerPlayer p){var old=USAGE.getOrDefault(p.getUUID(),new Usage(0,0,0,0));USAGE.put(p.getUUID(),new Usage(old.amount,old.tick,p.level().getGameTime()+60,old.cooldown));}
    public static void cooldown(ServerPlayer p){var old=USAGE.getOrDefault(p.getUUID(),new Usage(0,0,0,0));USAGE.put(p.getUUID(),new Usage(old.amount,old.tick,old.lowUntil,p.level().getGameTime()+20));}
    public record State(long wallet,long stars,long spent,boolean low,int cooldown) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(Jugcraft.id("alchemy_power_status"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=new StreamCodec<>(){
            public State decode(RegistryFriendlyByteBuf b){return new State(b.readVarLong(),b.readVarLong(),b.readVarLong(),b.readBoolean(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,State s){b.writeVarLong(s.wallet);b.writeVarLong(s.stars);b.writeVarLong(s.spent);b.writeBoolean(s.low);b.writeVarInt(s.cooldown);}
        };
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
