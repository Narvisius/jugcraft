package io.github.jimbozoomer.jugcraft.concordance.trinket;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: whether this player's worn trinkets (Wayfaring's belt and boots) are drawn on them, their "Show my
 * worn trinkets" setting. Sent when the client joins a world and whenever the setting changes; the server takes it for
 * that player only ({@link WornDisplay#choose}).
 */
public record WornDisplayPayload(boolean shown) implements CustomPacketPayload {
	public static final Type<WornDisplayPayload> TYPE = new Type<>(Jugcraft.id("worn_trinkets_shown"));
	public static final StreamCodec<RegistryFriendlyByteBuf, WornDisplayPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, WornDisplayPayload::shown,
			WornDisplayPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
