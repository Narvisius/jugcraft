package io.github.jimbozoomer.jugcraft.concordance.composer;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: what a composer screen may show and offer (Chunk A, A-02), sent when it opens and again whenever
 * the server's side of it changes (a spell written or wiped, another instrument in hand, new rules). The client has no
 * rules of its own, so this is all it knows: the instrument's limits, the words this player has learned (only those: the
 * rest are only counted) and what is inscribed now.
 *
 * @param revision the server's count of this player's composer states; a screen's inscribe or clear names the one it
 *     acted on, and is refused if it is not the latest
 * @param status {@link #READY}, {@link #DISABLED} or {@link #NO_INSTRUMENT}
 * @param instrument the instrument in the player's main hand, if any
 * @param words the composable words the player has learned, in the catalogue's order
 * @param locked how many composable words they have yet to learn
 * @param inscribed what is inscribed on the instrument in hand ("" for nothing)
 */
public record ComposerStatePayload(int revision, int status, Optional<InstrumentView> instrument, List<Word> words, int locked,
		String inscribed) implements CustomPacketPayload {
	/** A composing instrument is in hand. */
	public static final int READY = 0;
	/** The Concordance is switched off on this server. */
	public static final int DISABLED = 1;
	/** No composing instrument is in the main hand. */
	public static final int NO_INSTRUMENT = 2;
	public static final int MAX_WORDS = 256;
	public static final int MAX_ID = 256;
	public static final Type<ComposerStatePayload> TYPE = new Type<>(Jugcraft.id("concordance_composer_state"));

	/**
	 * A word the player may compose with.
	 *
	 * @param id its full id ({@code jugcraft:touch})
	 * @param slot its {@code compose.Slot} ordinal
	 * @param capacity the instrument capacity it takes
	 * @param focus the Focus it adds (before pulses repeat it)
	 * @param aspects for a modifier, the bit of the aspect it changes; for any other word, the bits of the aspects that
	 *     change it (bit = {@code compose.Component.Aspect} ordinal)
	 */
	public record Word(String id, int slot, int capacity, int focus, int aspects) {
		public static final StreamCodec<ByteBuf, Word> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(MAX_ID), Word::id,
				ByteBufCodecs.VAR_INT, Word::slot,
				ByteBufCodecs.VAR_INT, Word::capacity,
				ByteBufCodecs.VAR_INT, Word::focus,
				ByteBufCodecs.VAR_INT, Word::aspects,
				Word::new);
	}

	/** An instrument's limits, as {@link Instrument} has them (the client has no catalogue to look them up in). */
	public record InstrumentView(String item, int capacity, int targets, int work, int branches, int duration) {
		public static final StreamCodec<ByteBuf, InstrumentView> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(MAX_ID), InstrumentView::item,
				ByteBufCodecs.VAR_INT, InstrumentView::capacity,
				ByteBufCodecs.VAR_INT, InstrumentView::targets,
				ByteBufCodecs.VAR_INT, InstrumentView::work,
				ByteBufCodecs.VAR_INT, InstrumentView::branches,
				ByteBufCodecs.VAR_INT, InstrumentView::duration,
				InstrumentView::new);

		public static InstrumentView of(Instrument instrument) {
			return new InstrumentView(instrument.item(), instrument.capacity(), instrument.targets(), instrument.work(),
					instrument.branches(), instrument.duration());
		}
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, ComposerStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ComposerStatePayload::revision,
			ByteBufCodecs.VAR_INT, ComposerStatePayload::status,
			ByteBufCodecs.optional(InstrumentView.STREAM_CODEC), ComposerStatePayload::instrument,
			Word.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_WORDS)), ComposerStatePayload::words,
			ByteBufCodecs.VAR_INT, ComposerStatePayload::locked,
			ByteBufCodecs.stringUtf8(Grammar.MAX_TEXT), ComposerStatePayload::inscribed,
			ComposerStatePayload::new);

	public ComposerStatePayload {
		words = List.copyOf(words);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
