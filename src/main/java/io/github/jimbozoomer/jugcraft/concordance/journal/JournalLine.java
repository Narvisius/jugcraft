package io.github.jimbozoomer.jugcraft.concordance.journal;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One line of the Concordance Journal (roadmap step 26): what it says in words, and, when there is one, the exact figures
 * behind it (counts, limits, ticks), which a player shows or hides in their settings.
 */
public record JournalLine(Component text, Optional<Component> exact) {
	public static final StreamCodec<RegistryFriendlyByteBuf, JournalLine> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.TRUSTED_STREAM_CODEC, JournalLine::text,
			ByteBufCodecs.optional(ComponentSerialization.TRUSTED_STREAM_CODEC), JournalLine::exact,
			JournalLine::new);
}
