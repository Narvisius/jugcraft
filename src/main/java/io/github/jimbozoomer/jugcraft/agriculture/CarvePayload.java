package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: the finished face for the side the player opened with the Carving Knife. The face is
 * one of the known sizes with exactly as many ints as that size takes ({@link CarvingFace.Sized}); the
 * server checks everything else ({@link PumpkinCarvings#carve}).
 */
public record CarvePayload(BlockPos pos, Direction side, CarvingFace.Sized face) implements CustomPacketPayload {
	public static final Type<CarvePayload> TYPE = new Type<>(Jugcraft.id("carve"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CarvePayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, CarvePayload::pos,
			Direction.STREAM_CODEC, CarvePayload::side,
			CarvingFace.Sized.STREAM_CODEC, CarvePayload::face,
			CarvePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
