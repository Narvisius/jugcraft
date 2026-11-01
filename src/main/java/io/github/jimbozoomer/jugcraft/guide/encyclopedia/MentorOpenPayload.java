package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server-selected lesson after a validated interaction. The client still requests its own fresh pages. */
public record MentorOpenPayload(String page) implements CustomPacketPayload {
    public static final Type<MentorOpenPayload> TYPE = new Type<>(Jugcraft.id("mentor_lesson"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MentorOpenPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), MentorOpenPayload::page, MentorOpenPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
