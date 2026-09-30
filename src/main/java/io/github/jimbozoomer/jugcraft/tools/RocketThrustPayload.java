package io.github.jimbozoomer.jugcraft.tools;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player fired their rocket pack this tick. */
public record RocketThrustPayload() implements CustomPacketPayload {
	public static final Type<RocketThrustPayload> TYPE = new Type<>(Jugcraft.id("rocket_thrust"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RocketThrustPayload> CODEC = StreamCodec.unit(new RocketThrustPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
