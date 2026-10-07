package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: a spooky firework burst at ({@code x}, {@code y}, {@code z}) into the {@link FireworkShape} with
 * this ordinal, twinkling or not. Each client draws the picture in sparks, facing its own player.
 */
public record SpookyBurstPayload(double x, double y, double z, int shape, boolean twinkle) implements CustomPacketPayload {
	public static final Type<SpookyBurstPayload> TYPE = new Type<>(Jugcraft.id("spooky_burst"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SpookyBurstPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, SpookyBurstPayload::x,
			ByteBufCodecs.DOUBLE, SpookyBurstPayload::y,
			ByteBufCodecs.DOUBLE, SpookyBurstPayload::z,
			ByteBufCodecs.VAR_INT, SpookyBurstPayload::shape,
			ByteBufCodecs.BOOL, SpookyBurstPayload::twinkle,
			SpookyBurstPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
