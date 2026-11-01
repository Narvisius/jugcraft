package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-approved original equipment gestures; no client-to-server ability authority. */
public record WildPlayerActionPayload(int player,boolean slam) implements CustomPacketPayload {
    public static final Type<WildPlayerActionPayload> TYPE=new Type<>(Jugcraft.id("wilds_player_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WildPlayerActionPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,WildPlayerActionPayload::player,ByteBufCodecs.BOOL,WildPlayerActionPayload::slam,WildPlayerActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
