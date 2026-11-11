package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A drone's new leg (server to the players near its terminal), once when it starts: they animate it themselves. */
public record DronePayload(BlockPos terminal, Drone.View drone) implements CustomPacketPayload {
	public static final Type<DronePayload> TYPE = new Type<>(Jugcraft.id("warehouse_drone"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DronePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, DronePayload::terminal, Drone.View.STREAM_CODEC.cast(), DronePayload::drone, DronePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
