package io.github.jimbozoomer.jugcraft.jimtafacts;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record JimtaInput(int action) implements CustomPacketPayload {
    public static final Type<JimtaInput> TYPE=new Type<>(Jugcraft.id("jimtafacts_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,JimtaInput> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,JimtaInput::action,JimtaInput::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
