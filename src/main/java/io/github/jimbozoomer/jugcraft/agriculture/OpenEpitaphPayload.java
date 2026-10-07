package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: open the epitaph screen for inscription {@code slot} (0 for the epitaph) of the memorial whose part 0
 * is at {@code pos}, showing its lines now.
 */
public record OpenEpitaphPayload(BlockPos pos, int slot, List<String> lines) implements CustomPacketPayload {
	public static final Type<OpenEpitaphPayload> TYPE = new Type<>(Jugcraft.id("open_epitaph"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OpenEpitaphPayload> CODEC = StreamCodec.composite(
			BlockPos.STREAM_CODEC, OpenEpitaphPayload::pos,
			ByteBufCodecs.VAR_INT, OpenEpitaphPayload::slot,
			Epitaph.LINES_STREAM_CODEC, OpenEpitaphPayload::lines,
			OpenEpitaphPayload::new);

	/** The epitaph (inscription 0). */
	public OpenEpitaphPayload(BlockPos pos, List<String> lines) {
		this(pos, 0, lines);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
