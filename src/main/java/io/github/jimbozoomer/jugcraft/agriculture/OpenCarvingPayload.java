package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: open the carving screen for one side of a pumpkin (or a giant pumpkin, whose master
 * block is {@code pos}), showing what is already carved there ({@code face}: its size and pixels), whether
 * this server allows free drawing or only starter faces, and the design of a Pumpkin Stencil held in the
 * other hand, if any (16 rows), which the screen offers to press in.
 */
public record OpenCarvingPayload(BlockPos pos, Direction side, CarvingFace.Sized face, boolean freeDraw, Optional<int[]> stencil)
		implements CustomPacketPayload {
	public static final Type<OpenCarvingPayload> TYPE = new Type<>(Jugcraft.id("open_carving"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenCarvingPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenCarvingPayload::pos,
			Direction.STREAM_CODEC, OpenCarvingPayload::side,
			CarvingFace.Sized.STREAM_CODEC, OpenCarvingPayload::face,
			ByteBufCodecs.BOOL, OpenCarvingPayload::freeDraw,
			ByteBufCodecs.optional(PumpkinCarving.FACE_STREAM_CODEC), OpenCarvingPayload::stencil,
			OpenCarvingPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
