package io.github.jimbozoomer.jugcraft.artillery;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what a big gun's gunner is pressing. Forward (1 forward, -1 back) and turn (1 left, -1 right) drive
 * a self-propelled gun and are -1, 0 or 1; fire (attack held) is 0 or 1. The server clamps them and works the gun
 * ({@link CrewedGun#steer}).
 */
public record ArtilleryInputPayload(int forward, int turn, int fire) implements CustomPacketPayload {
	public static final Type<ArtilleryInputPayload> TYPE = new Type<>(Jugcraft.id("artillery_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ArtilleryInputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ArtilleryInputPayload::forward,
			ByteBufCodecs.VAR_INT, ArtilleryInputPayload::turn,
			ByteBufCodecs.VAR_INT, ArtilleryInputPayload::fire,
			ArtilleryInputPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
