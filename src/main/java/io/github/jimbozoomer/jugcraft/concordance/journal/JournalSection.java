package io.github.jimbozoomer.jugcraft.concordance.journal;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One section of the Concordance Journal: its id (for the screens' tabs), its title and its lines. */
public record JournalSection(String id, Component title, List<JournalLine> lines) {
	public static final StreamCodec<RegistryFriendlyByteBuf, JournalSection> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, JournalSection::id,
			ComponentSerialization.TRUSTED_STREAM_CODEC, JournalSection::title,
			JournalLine.STREAM_CODEC.apply(ByteBufCodecs.list(Journal.MAX_LINES)), JournalSection::lines,
			JournalSection::new);

	public JournalSection {
		lines = List.copyOf(lines);
	}
}
