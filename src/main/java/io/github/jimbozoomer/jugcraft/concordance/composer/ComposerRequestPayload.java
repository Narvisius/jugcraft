package io.github.jimbozoomer.jugcraft.concordance.composer;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.compose.Grammar;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what a composer screen asks (Chunk A, A-02). Nothing in it is trusted as gameplay data: the action
 * is a number the server checks, the revision is only compared with the server's own ({@link Composer}), and the text
 * is compiled exactly as a typed command's is. The text decodes to at most {@value #MAX_TEXT} characters (a longer one
 * is a malformed packet, which ends the connection as any other does); the compiler refuses anything past
 * {@value Grammar#MAX_TEXT}.
 *
 * @param sequence the screen's own count of its requests, echoed in the answer so it can drop late ones
 * @param action {@link #OPEN}, {@link #CHECK}, {@link #INSCRIBE} or {@link #CLEAR}
 * @param revision the session revision the screen last heard ({@link ComposerStatePayload#revision}); used by inscribe
 *     and clear only
 * @param text the composition (check and inscribe only)
 */
public record ComposerRequestPayload(int sequence, int action, int revision, String text) implements CustomPacketPayload {
	/** Open the composer, or ask for its state again: the server answers with a {@link ComposerStatePayload}. */
	public static final int OPEN = 0;
	/** Check the text: the server answers with what it does and costs, or every problem. Nothing is written. */
	public static final int CHECK = 1;
	/** Inscribe the text on the instrument the screen was shown. */
	public static final int INSCRIBE = 2;
	/** Wipe the inscription on the instrument the screen was shown. */
	public static final int CLEAR = 3;
	public static final int MAX_TEXT = 4 * Grammar.MAX_TEXT;
	public static final Type<ComposerRequestPayload> TYPE = new Type<>(Jugcraft.id("concordance_composer_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ComposerRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ComposerRequestPayload::sequence,
			ByteBufCodecs.VAR_INT, ComposerRequestPayload::action,
			ByteBufCodecs.VAR_INT, ComposerRequestPayload::revision,
			ByteBufCodecs.stringUtf8(MAX_TEXT), ComposerRequestPayload::text,
			ComposerRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
