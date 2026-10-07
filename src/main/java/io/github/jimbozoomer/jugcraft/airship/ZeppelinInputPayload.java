package io.github.jimbozoomer.jugcraft.airship;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what the zeppelin's pilot is pressing, each -1, 0 or 1: forward (forward key 1, back -1), turn
 * (left 1, right -1) and vertical (jump 1, sprint -1). The server clamps them and flies the airship (see
 * {@link Zeppelin#steer}).
 */
public record ZeppelinInputPayload(int forward, int turn, int vertical) implements CustomPacketPayload {
	public static final Type<ZeppelinInputPayload> TYPE = new Type<>(Jugcraft.id("zeppelin_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ZeppelinInputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ZeppelinInputPayload::forward,
			ByteBufCodecs.VAR_INT, ZeppelinInputPayload::turn,
			ByteBufCodecs.VAR_INT, ZeppelinInputPayload::vertical,
			ZeppelinInputPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
