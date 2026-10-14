package io.github.jimbozoomer.jugcraft.concordance.composer;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Composing;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.Nullable;

/**
 * Server to client: what a composer screen's check, inscribe or clear came to (Chunk A, A-02), in the same words the
 * {@code /jugcraft concordance compose} command uses, because the same service decided it ({@link Composing}).
 *
 * @param sequence the request's own sequence, so a screen can drop an answer to an older request
 * @param revision the server's composer revision once this was done ({@link ComposerStatePayload#revision})
 * @param outcome the {@link Composing.Outcome} ordinal
 * @param text the canonical text of a valid spell, or else the text as checked (trimmed); empty for a refusal
 * @param heading what it came to, in a line ("touch struck light works:", "... cannot be cast:", "Inscribed on ...")
 * @param report the spell's numbers, the lines under the heading and the words the problems name
 */
public record ComposerResultPayload(int sequence, int revision, int outcome, String text, Component heading, Report report)
		implements CustomPacketPayload {
	public static final int MAX_LINES = 40;
	public static final int MAX_NAME = ComposerStatePayload.MAX_ID;
	public static final Type<ComposerResultPayload> TYPE = new Type<>(Jugcraft.id("concordance_composer_result"));

	/** A compiled spell's numbers: Focus, cooldown (ticks), capacity taken, and the targets and work it may reach. */
	public record Summary(int focus, int cooldown, int capacity, int targets, int work) {
		public static final StreamCodec<ByteBuf, Summary> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Summary::focus,
				ByteBufCodecs.VAR_INT, Summary::cooldown,
				ByteBufCodecs.VAR_INT, Summary::capacity,
				ByteBufCodecs.VAR_INT, Summary::targets,
				ByteBufCodecs.VAR_INT, Summary::work,
				Summary::new);
	}

	/**
	 * The compiled spell's numbers (when it compiled), the lines under the heading (what a valid spell does and costs, or
	 * each distinct problem), and the written names the problems blame, so a screen can mark those words without
	 * relying on colour.
	 */
	public record Report(Optional<Summary> summary, List<Component> lines, List<String> blamed) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Report> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.optional(Summary.STREAM_CODEC), Report::summary,
				ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)), Report::lines,
				ByteBufCodecs.stringUtf8(MAX_NAME).apply(ByteBufCodecs.list(Grammar.MAX_NAMES)), Report::blamed,
				Report::new);

		public Report {
			lines = List.copyOf(lines);
			blamed = List.copyOf(blamed);
		}
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, ComposerResultPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ComposerResultPayload::sequence,
			ByteBufCodecs.VAR_INT, ComposerResultPayload::revision,
			ByteBufCodecs.VAR_INT, ComposerResultPayload::outcome,
			ByteBufCodecs.stringUtf8(ComposerRequestPayload.MAX_TEXT), ComposerResultPayload::text,
			ComponentSerialization.TRUSTED_STREAM_CODEC, ComposerResultPayload::heading,
			Report.STREAM_CODEC, ComposerResultPayload::report,
			ComposerResultPayload::new);

	/** The outcome, or null for a number this game does not know. */
	public Composing.@Nullable Outcome what() {
		Composing.Outcome[] outcomes = Composing.Outcome.values();
		return outcome >= 0 && outcome < outcomes.length ? outcomes[outcome] : null;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
