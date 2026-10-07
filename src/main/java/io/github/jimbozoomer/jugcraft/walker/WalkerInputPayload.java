package io.github.jimbozoomer.jugcraft.walker;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what the Diesel Walker's pilot is pressing. Forward (1 forward, -1 back) and turn (1 left, -1
 * right) are -1, 0 or 1; jump, drill (use held) and punch (attack held) are 0 or 1. The server clamps them and
 * drives the walker ({@link DieselWalker#steer}).
 */
public record WalkerInputPayload(int forward, int turn, int jump, int drill, int punch) implements CustomPacketPayload {
	public static final Type<WalkerInputPayload> TYPE = new Type<>(Jugcraft.id("walker_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WalkerInputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, WalkerInputPayload::forward,
			ByteBufCodecs.VAR_INT, WalkerInputPayload::turn,
			ByteBufCodecs.VAR_INT, WalkerInputPayload::jump,
			ByteBufCodecs.VAR_INT, WalkerInputPayload::drill,
			ByteBufCodecs.VAR_INT, WalkerInputPayload::punch,
			WalkerInputPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
