package io.github.jimbozoomer.jugcraft.aircraft;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AircraftInput(int forward,int turn,int vertical,boolean fire,boolean boost) implements CustomPacketPayload {
    public static final Type<AircraftInput> TYPE=new Type<>(Jugcraft.id("aircraft_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AircraftInput> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,AircraftInput::forward,ByteBufCodecs.VAR_INT,AircraftInput::turn,
        ByteBufCodecs.VAR_INT,AircraftInput::vertical,ByteBufCodecs.BOOL,AircraftInput::fire,ByteBufCodecs.BOOL,AircraftInput::boost,AircraftInput::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
