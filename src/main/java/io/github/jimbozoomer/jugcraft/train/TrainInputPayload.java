package io.github.jimbozoomer.jugcraft.train;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what the locomotive's driver is pressing. Throttle is 1 (forwards held: notch up), -1 (back held:
 * notch down) or 0; brake is jump held. The server clamps them and works the throttle ({@link Locomotive#steer}).
 */
public record TrainInputPayload(int throttle, boolean brake) implements CustomPacketPayload {
	public static final Type<TrainInputPayload> TYPE = new Type<>(Jugcraft.id("train_input"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TrainInputPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, TrainInputPayload::throttle,
			ByteBufCodecs.BOOL, TrainInputPayload::brake,
			TrainInputPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
