package io.github.jimbozoomer.jugcraft.guns;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, each tick the player holds the trigger of a gun whose barrels spin up (slice 8C, the Thresher): the
 * barrels are turning. The server counts the ticks itself and fires only once they have turned long enough
 * ({@link GunShots#spin}).
 */
public record GunSpinPayload() implements CustomPacketPayload {
	public static final GunSpinPayload INSTANCE = new GunSpinPayload();
	public static final Type<GunSpinPayload> TYPE = new Type<>(Jugcraft.id("gun_spin"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GunSpinPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
