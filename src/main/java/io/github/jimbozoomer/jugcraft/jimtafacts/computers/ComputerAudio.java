package io.github.jimbozoomer.jugcraft.jimtafacts.computers;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.HexFormat;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Bounded owner-authored DFPWM clips; no URLs or filesystem reads. Only nearby listeners receive them. */
public final class ComputerAudio {
    public static final int MAX_BYTES=4096;
    private ComputerAudio(){}
    public static void register(){PayloadTypeRegistry.clientboundPlay().register(Clip.TYPE,Clip.CODEC);}
    public static byte[] parse(String hex){
        if(hex.isEmpty()||hex.length()>MAX_BYTES*2||(hex.length()&1)!=0)throw new IllegalArgumentException("Audio: 1..4096 bytes as hexadecimal.");
        try{return HexFormat.of().parseHex(hex);}catch(IllegalArgumentException e){throw new IllegalArgumentException("Audio needs hexadecimal byte pairs.");}
    }
    public static void play(ServerPlayer actor,BlockPos source,byte[] data){
        var packet=new Clip(source,data);
        for(var player:actor.level().players())if(player.distanceToSqr(Vec3.atCenterOf(source))<=48*48&&ServerPlayNetworking.canSend(player,Clip.TYPE))ServerPlayNetworking.send(player,packet);
    }
    public record Clip(BlockPos pos,byte[] data) implements CustomPacketPayload {
        public static final Type<Clip> TYPE=new Type<>(Jugcraft.id("jimta_computer_audio"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Clip> CODEC=new StreamCodec<>(){
            public Clip decode(RegistryFriendlyByteBuf b){return new Clip(b.readBlockPos(),b.readByteArray(MAX_BYTES));}
            public void encode(RegistryFriendlyByteBuf b,Clip p){b.writeBlockPos(p.pos);b.writeByteArray(p.data);}
        };
        public Clip {if(data.length>MAX_BYTES)throw new IllegalArgumentException("Audio clip too large");pos=pos.immutable();data=data.clone();}
        @Override public byte[] data(){return data.clone();}
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
}
