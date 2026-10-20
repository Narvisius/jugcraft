package io.github.jimbozoomer.jugcraft.nouveau;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;
/** Only presentation is networked; trapping, duration and pop damage remain server-owned. */
public final class NouveauBubble {
    private NouveauBubble(){}
    public static void register(){PayloadTypeRegistry.clientboundPlay().register(Shell.TYPE,Shell.CODEC);}
    public static void show(LivingEntity target,int duration,boolean popping){if(target.level().isClientSide())return;var packet=new Shell(target.getId(),target.getUUID(),Math.clamp(duration,1,24000),popping);for(var player:PlayerLookup.tracking(target))ServerPlayNetworking.send(player,packet);if(target instanceof ServerPlayer player)ServerPlayNetworking.send(player,packet);}
    public record Shell(int entity,UUID uuid,int duration,boolean popping) implements CustomPacketPayload {
        public static final Type<Shell> TYPE=new Type<>(Jugcraft.id("nouveau_bubble"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Shell> CODEC=new StreamCodec<>(){
            public Shell decode(RegistryFriendlyByteBuf b){return new Shell(b.readVarInt(),b.readUUID(),Math.clamp(b.readVarInt(),1,24000),b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,Shell p){b.writeVarInt(p.entity);b.writeUUID(p.uuid);b.writeVarInt(p.duration);b.writeBoolean(p.popping);}
        };public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
