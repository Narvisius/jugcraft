package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: what the roster screen asks (Concordance Chunk D, D-07, D-S11). Three bounded numbers and one row id,
 * nothing else: no text, no player, no item, no total. The server (Roster) drops one from a stand-in, out of bounds or not
 * newer than the last it took from this player, asks the gate its action shares, and acts only on a row it listed to this
 * player in its last state, at the revision it showed, if that row offers the action.
 *
 * @param sequence the screen's own count of its requests, rising
 * @param action   an index into {@link #ACTIONS}
 * @param row      a row id the server listed (a Bound Will's id, a worker's, or UUID(0, request id), Roster.rowOf); NO_ROW for OPEN
 * @param revision the row's revision as the screen last showed it; any other is answered "changed" with the roster again
 */
public record RosterRequestPayload(int sequence, int action, UUID row, int revision) implements CustomPacketPayload {
	public static final int OPEN = 0, SUSPEND = 1, RESUME = 2, END = 3, CANCEL = 4, RECOVER = 5;
	/** The actions, by index (tools/concordance_roster.py ACTIONS; check_roster). Append only. */
	public static final List<String> ACTIONS = List.of("open", "suspend", "resume", "end", "cancel", "recover");
	/** The row an OPEN names: none. */
	public static final UUID NO_ROW = new UUID(0L, 0L);
	public static final Type<RosterRequestPayload> TYPE = new Type<>(Jugcraft.id("concordance_roster_request"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RosterRequestPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RosterRequestPayload::sequence,
			ByteBufCodecs.VAR_INT, RosterRequestPayload::action,
			UUIDUtil.STREAM_CODEC, RosterRequestPayload::row,
			ByteBufCodecs.VAR_INT, RosterRequestPayload::revision,
			RosterRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
