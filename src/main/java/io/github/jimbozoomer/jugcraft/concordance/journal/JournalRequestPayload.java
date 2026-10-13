package io.github.jimbozoomer.jugcraft.concordance.journal;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A player asking the server for their Concordance Journal (it carries nothing: the server decides what they see). */
public record JournalRequestPayload() implements CustomPacketPayload {
	public static final JournalRequestPayload INSTANCE = new JournalRequestPayload();
	public static final Type<JournalRequestPayload> TYPE = new Type<>(Jugcraft.id("concordance_journal_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JournalRequestPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
