package io.github.jimbozoomer.jugcraft.wilds;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** Approved cast announcement only; clients never supply another player's animation. */
public record WildSunPosePayload(int player,int move) implements CustomPacketPayload {
    public static final Type<WildSunPosePayload> TYPE=new Type<>(Jugcraft.id("wilds_sun_pose"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WildSunPosePayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,WildSunPosePayload::player,ByteBufCodecs.VAR_INT,WildSunPosePayload::move,WildSunPosePayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
