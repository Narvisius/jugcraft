package io.github.jimbozoomer.jugcraft.guide.encyclopedia;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * A player asks for their encyclopedia (Concordance Chunk A, A-04): which request this is (the answer carries it back,
 * and the client drops answers to older ones) and, when the player is looking at a block, where it is (the Inspect page
 * reads a Circle Anchor or Ley Pylon there, if it is near enough). Nothing else: the client chooses no page and claims
 * nothing; the server decides everything the encyclopedia shows.
 */
public record EncyclopediaRequestPayload(int sequence, Optional<BlockPos> looking) implements CustomPacketPayload {
	public static final Type<EncyclopediaRequestPayload> TYPE = new Type<>(Jugcraft.id("encyclopedia_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, EncyclopediaRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, EncyclopediaRequestPayload::sequence,
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC), EncyclopediaRequestPayload::looking,
			EncyclopediaRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
