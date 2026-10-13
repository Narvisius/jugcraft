package io.github.jimbozoomer.jugcraft.concordance.sign;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Server to client: a {@link Sign} (by index) to show at {@code at}, travelling from {@code from} when it shows a
 * transfer. One event, never animation frames: the client draws it from its own settings.
 */
public record SignPayload(int sign, Vec3 at, Optional<Vec3> from) implements CustomPacketPayload {
	public static final Type<SignPayload> TYPE = new Type<>(Jugcraft.id("concordance_sign"));
	private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> POINT = StreamCodec.composite(
			ByteBufCodecs.DOUBLE, Vec3::x,
			ByteBufCodecs.DOUBLE, Vec3::y,
			ByteBufCodecs.DOUBLE, Vec3::z,
			Vec3::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, SignPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, SignPayload::sign,
			POINT, SignPayload::at,
			ByteBufCodecs.optional(POINT), SignPayload::from,
			SignPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
