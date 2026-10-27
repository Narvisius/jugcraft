package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Server to client: what a pact perch's screen shows and offers (Concordance Chunk D, D-04, D-05; D-S14, D-S15). It is sent
 * when the menu opens, as the answer to each request and after a reload. The client has no pact rules: this is all it
 * knows. Every list is cut to its bound before it is sent (and an empty stack dropped); the words are the server's.
 *
 * @param revision  the terms revision (it counts the offer); a request naming another is answered {@link PactMenu#CHANGED}
 * @param mode      NEGOTIATE (a visit: his beats, then his terms), KEEP (the reader's pact kept here), SEAT (the reader's pact
 *                  without its perch, offered a seat here) or CLOSED (nothing the reader can do here; the notes say why)
 * @param terms     every term of {@link #TERMS}, in that order: its key and its value in words (the record's, once sealed)
 * @param notes     what stands in the way of agreeing, how the pact stands, or why nothing can be done
 * @param patterns  the sky patterns the reader observed (KEEP): id, name, recall cost, whether this occurrence can still be recalled
 * @param offerings the offerings the reader carries (KEEP): item id, name, how many carried, whether the keeping has room
 * @param compact   the Open Hand Compact's materials, for their icons (its term says them in words); resonance its resonance
 * @param actions   a bit for each action the mode offers now ({@link #offers}); the server decides when one is pressed
 * @param result    the answer to the request this state answers; empty for the opening, CHOOSE and a reload
 */
public record PactStatePayload(int containerId, int revision, int mode, Component speaker, List<Component> beats, List<Term> terms,
		List<Component> notes, List<Choice> patterns, List<Choice> offerings, List<ItemStack> compact, int resonance, int actions,
		Optional<Result> result) implements CustomPacketPayload {
	public static final int NEGOTIATE = 0;
	public static final int KEEP = 1;
	public static final int SEAT = 2;
	public static final int CLOSED = 3;
	public static final int MAX_BEATS = 3;
	public static final int MAX_TERMS = 16;
	public static final int MAX_NOTES = 12;
	public static final int MAX_CHOICES = 8;
	public static final int MAX_COMPACT = 8;
	public static final int MAX_KEY = 64;
	/**
	 * A choice's id: a pattern's or an offering's, both from data (the item tag, the pattern files), so bounded only as any
	 * string is (vanilla's 32,767), never a length that could fail to encode and drop its player (the unit review).
	 */
	public static final int MAX_ID = 32767;
	/** Every term the terms page shows, in order (D-S15 and the perch's own; tools/concordance_pacts.py TERMS). */
	public static final List<String> TERMS = List.of("service", "hours", "area", "quota", "upkeep", "grace", "suspension", "duration",
			"breach", "ending", "returns", "perch", "beneficiaries", "compact");
	public static final Type<PactStatePayload> TYPE = new Type<>(Jugcraft.id("concordance_pact_state"));

	/** One term: its key (labelled by the client, screen.jugcraft.pact.term.<key>) and its value in words. */
	public record Term(String key, Component value) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Term> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(MAX_KEY), Term::key,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Term::value,
				Term::new);
	}

	/** One thing to choose: its id, its name, a number (a recall's cost, how many are carried) and whether it can be chosen now. */
	public record Choice(String id, Component name, int amount, boolean ready) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Choice> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.stringUtf8(MAX_ID), Choice::id,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Choice::name,
				ByteBufCodecs.VAR_INT, Choice::amount,
				ByteBufCodecs.BOOL, Choice::ready,
				Choice::new);
	}

	/** What a request came to: its own sequence, its reason (a stable id), whether it was done, and the words for it. */
	public record Result(int sequence, String reason, boolean done, Component message) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Result::sequence,
				ByteBufCodecs.stringUtf8(MAX_KEY), Result::reason,
				ByteBufCodecs.BOOL, Result::done,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Result::message,
				Result::new);
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, PactStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PactStatePayload::containerId,
			ByteBufCodecs.VAR_INT, PactStatePayload::revision,
			ByteBufCodecs.VAR_INT, PactStatePayload::mode,
			ComponentSerialization.TRUSTED_STREAM_CODEC, PactStatePayload::speaker,
			ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BEATS)), PactStatePayload::beats,
			Term.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TERMS)), PactStatePayload::terms,
			ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NOTES)), PactStatePayload::notes,
			Choice.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CHOICES)), PactStatePayload::patterns,
			Choice.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CHOICES)), PactStatePayload::offerings,
			ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_COMPACT)), PactStatePayload::compact,
			ByteBufCodecs.VAR_INT, PactStatePayload::resonance,
			ByteBufCodecs.VAR_INT, PactStatePayload::actions,
			ByteBufCodecs.optional(Result.STREAM_CODEC), PactStatePayload::result,
			PactStatePayload::new);

	public PactStatePayload {
		beats = bounded(beats, MAX_BEATS);
		terms = bounded(terms, MAX_TERMS);
		notes = bounded(notes, MAX_NOTES);
		patterns = bounded(patterns, MAX_CHOICES);
		offerings = bounded(offerings, MAX_CHOICES);
		compact = bounded(compact.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList(), MAX_COMPACT);
	}

	private static <T> List<T> bounded(List<T> list, int max) {
		return List.copyOf(list.subList(0, Math.min(list.size(), max)));
	}

	/** Whether the mode offers {@code action} now (the server still decides when it is pressed). */
	public boolean offers(int action) {
		return action >= 0 && action < 31 && (actions & (1 << action)) != 0;
	}

	/** The bits for {@code offered}. */
	public static int mask(int... offered) {
		int mask = 0;
		for (int action : offered) {
			mask |= 1 << action;
		}
		return mask;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
