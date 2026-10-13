package io.github.jimbozoomer.jugcraft.concordance.vigil;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client (roadmap step 16): the player with entity id {@link #entity} has just made an offering, so clients
 * that see them play the offering gesture. Presentation only: the offering itself was the server's.
 */
public record VigilGesturePayload(int entity) implements CustomPacketPayload {
	public static final Type<VigilGesturePayload> TYPE = new Type<>(Jugcraft.id("vigil_gesture"));
	public static final StreamCodec<RegistryFriendlyByteBuf, VigilGesturePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, VigilGesturePayload::entity, VigilGesturePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
