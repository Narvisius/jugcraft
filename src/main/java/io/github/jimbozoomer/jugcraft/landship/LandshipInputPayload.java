package io.github.jimbozoomer.jugcraft.landship;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what the Landship's driver is pressing. Forward (1 forward, -1 back) and turn (1 left, -1 right)
 * are -1, 0 or 1; cannon (attack held) and guns (use held) are 0 or 1. The server clamps them and drives the landship
 * ({@link Landship#steer}).
 */
public record LandshipInputPayload(int forward, int turn, int cannon, int guns) implements CustomPacketPayload {
	public static final Type<LandshipInputPayload> TYPE = new Type<>(Jugcraft.id("landship_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LandshipInputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, LandshipInputPayload::forward,
			ByteBufCodecs.VAR_INT, LandshipInputPayload::turn,
			ByteBufCodecs.VAR_INT, LandshipInputPayload::cannon,
			ByteBufCodecs.VAR_INT, LandshipInputPayload::guns,
			LandshipInputPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
