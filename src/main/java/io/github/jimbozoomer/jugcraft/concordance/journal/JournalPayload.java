package io.github.jimbozoomer.jugcraft.concordance.journal;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The server's Concordance Journal for one player, sent when they ask for it (roadmap step 26). */
public record JournalPayload(List<JournalSection> sections) implements CustomPacketPayload {
	public static final Type<JournalPayload> TYPE = new Type<>(Jugcraft.id("concordance_journal"));
	public static final StreamCodec<RegistryFriendlyByteBuf, JournalPayload> CODEC = StreamCodec.composite(
			JournalSection.STREAM_CODEC.apply(ByteBufCodecs.list(Journal.MAX_SECTIONS)), JournalPayload::sections, JournalPayload::new);

	public JournalPayload {
		sections = List.copyOf(sections);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
