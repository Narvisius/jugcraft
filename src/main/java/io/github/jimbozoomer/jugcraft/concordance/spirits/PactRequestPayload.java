package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what a pact perch's screen asks (Concordance Chunk D, D-04, D-05; D-S14). Five bounded numbers and
 * nothing else: no text, no player id, no item, no total. The server checks each again against the menu it opened for
 * this player (the container, stillValid, the actor, its rate gate, the authority).
 *
 * @param containerId the pact menu this is for; a request naming any other container is dropped
 * @param sequence    the screen's own count of its requests, rising; one not above the last the server took for this menu is dropped
 * @param action      an index into {@link #ACTIONS}
 * @param choice      an index into a list the server last sent this menu (a pattern for RECALL, an offering for DEPOSIT); 0 otherwise
 * @param revision    the terms revision the screen last heard; any other is answered "changed" with the terms again
 */
public record PactRequestPayload(int containerId, int sequence, int action, int choice, int revision) implements CustomPacketPayload {
	public static final int CHOOSE = 0, ACCEPT = 1, DECLINE = 2, RECALL = 3, DEPOSIT = 4, SUSPEND = 5, RESUME = 6, END = 7, SEAT = 8;
	/** The actions, by index (tools/concordance_pacts.py ACTIONS; check_pacts). Append only. */
	public static final List<String> ACTIONS = List.of("choose", "accept", "decline", "recall", "deposit", "suspend", "resume", "end", "seat");
	public static final Type<PactRequestPayload> TYPE = new Type<>(Jugcraft.id("concordance_pact_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PactRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, PactRequestPayload::containerId,
			ByteBufCodecs.VAR_INT, PactRequestPayload::sequence,
			ByteBufCodecs.VAR_INT, PactRequestPayload::action,
			ByteBufCodecs.VAR_INT, PactRequestPayload::choice,
			ByteBufCodecs.VAR_INT, PactRequestPayload::revision,
			PactRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
