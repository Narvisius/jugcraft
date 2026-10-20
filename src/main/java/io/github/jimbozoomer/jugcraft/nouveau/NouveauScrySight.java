package io.github.jimbozoomer.jugcraft.nouveau;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;

/** Server-selected, bounded, loaded-block vision. Only permitted matches are sent to the viewer. */
public final class NouveauScrySight {
    private static final Map<UUID,Sight> SIGHTS=new HashMap<>();
    private static List<BlockPos> OFFSETS=List.of();
    private static final class Sight {Block block;net.minecraft.tags.TagKey<Block> tag;BlockPos origin;Identifier dimension;int cursor;long until;boolean explicit;final Set<BlockPos> hits=new LinkedHashSet<>();Sight(Block b,BlockPos at,long until,boolean explicit){block=b;origin=at;this.until=until;this.explicit=explicit;}}
    private NouveauScrySight(){}
    public static void register(){
        OFFSETS=BlockPos.betweenClosedStream(-24,-24,-24,24,24,24).map(BlockPos::immutable).filter(p->p.distSqr(BlockPos.ZERO)<=24*24).sorted(Comparator.comparingDouble(p->p.distSqr(BlockPos.ZERO))).toList();
        PayloadTypeRegistry.clientboundPlay().register(Marks.TYPE,Marks.CODEC);
        ServerTickEvents.END_SERVER_TICK.register(server->{if(server.getTickCount()%5!=0)return;for(var p:server.getPlayerList().getPlayers())tick(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->SIGHTS.remove(h.getPlayer().getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->SIGHTS.clear());
    }
    public static void begin(ServerPlayer p,Block block,int duration){var sight=new Sight(block,p.blockPosition(),p.level().getGameTime()+duration,true);
        for(var recipe:NouveauRecipes.all())if(recipe.operation().equals("scry_ritual")){var augment=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse(recipe.data().get("augment").getAsString()));if(block.defaultBlockState().is(augment)){sight.tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,Identifier.parse(recipe.data().get("highlight").getAsString()));break;}}
        SIGHTS.put(p.getUUID(),sight);NouveauStatuses.give(p,"scrying",duration,0,p);}
    private static void tick(ServerPlayer p){
        var l=p.level();var sight=SIGHTS.get(p.getUUID());
        if(!JugNouveau.enabled()||!p.isAlive()||p.isSpectator()||!NouveauStatuses.has(p,"scrying")){if(SIGHTS.remove(p.getUUID())!=null)send(p,List.of());return;}
        Block held=p.getOffhandItem().getItem() instanceof net.minecraft.world.item.BlockItem b?b.getBlock():Blocks.BUDDING_AMETHYST;
        if(sight==null||l.getGameTime()>sight.until){sight=new Sight(held,p.blockPosition(),l.getGameTime()+24000,false);SIGHTS.put(p.getUUID(),sight);}
        if(!sight.explicit&&sight.block!=held){sight.block=held;sight.cursor=0;sight.hits.clear();}
        if(!l.dimension().identifier().equals(sight.dimension)||p.blockPosition().distSqr(sight.origin)>64){sight.dimension=l.dimension().identifier();sight.origin=p.blockPosition();sight.cursor=0;sight.hits.clear();}
        for(int i=0;i<256;i++){
            var at=sight.origin.offset(OFFSETS.get(sight.cursor++%OFFSETS.size()));
            if(!l.hasChunkAt(at)||!(sight.tag==null?l.getBlockState(at).is(sight.block):l.getBlockState(at).is(sight.tag))||!Authority.mayChange(l,p,at))continue;
            if(sight.hits.size()<64)sight.hits.add(at.immutable());
        }
        if(p.tickCount%10==0){var current=sight;current.hits.removeIf(at->!l.hasChunkAt(at)||!(current.tag==null?l.getBlockState(at).is(current.block):l.getBlockState(at).is(current.tag))||p.blockPosition().distSqr(at)>32*32||!Authority.mayChange(l,p,at));send(p,current.hits.stream().sorted(Comparator.comparingDouble(p.blockPosition()::distSqr)).limit(32).toList());}
    }
    private static void send(ServerPlayer p,List<BlockPos> hits){ServerPlayNetworking.send(p,new Marks(p.level().dimension().identifier(),hits));}
    public record Marks(Identifier dimension,List<BlockPos> blocks) implements CustomPacketPayload {
        public static final Type<Marks> TYPE=new Type<>(Jugcraft.id("nouveau_scry_marks"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Marks> CODEC=new StreamCodec<>(){
            public Marks decode(RegistryFriendlyByteBuf b){var dimension=b.readIdentifier();int size=b.readVarInt();if(size<0||size>32)throw new IllegalArgumentException("Scry marker limit");var blocks=new ArrayList<BlockPos>();for(int i=0;i<size;i++)blocks.add(b.readBlockPos());return new Marks(dimension,List.copyOf(blocks));}
            public void encode(RegistryFriendlyByteBuf b,Marks m){b.writeIdentifier(m.dimension);b.writeVarInt(m.blocks.size());for(var at:m.blocks)b.writeBlockPos(at);}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
