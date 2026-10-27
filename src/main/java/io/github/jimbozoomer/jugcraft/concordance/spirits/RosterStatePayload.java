package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: one reader's roster (Concordance Chunk D, D-07, D-S11). It is sent when the roster opens and as the
 * answer to each request, never otherwise; the client has no rules and shows only this. At most {@value #MAX_ROWS} rows,
 * cut before it is sent and refused past that when read: the reader's Bound Wills first (each pact and agreement, one
 * without its place or not yet claimed by it included, its presence in the same row), then the courier requests they
 * filed, then the workers on their roster not already shown; {@code more} counts the rows left out (the journal and the
 * workers and logistics commands list everything). Every word is the server's; a row's type never travels.
 *
 * @param sequence the request this answers (an open's own), so the screen can drop a late answer
 * @param enabled  false while the Concordance is switched off: every row is shown and none offers anything (D-S16)
 * @param rows     the rows, in that order
 * @param more     how many rows were left out
 * @param result   the answer to the action this state answers; empty for an open
 */
public record RosterStatePayload(int sequence, boolean enabled, List<Row> rows, int more, Optional<PactStatePayload.Result> result)
		implements CustomPacketPayload {
	public static final int MAX_ROWS = 64;
	/** The longest portrait id sent (an entity type's or an item's; a longer one is sent empty). */
	public static final int MAX_PORTRAIT = 256;
	public static final int ACTIVE = 0, WAITING = 1, SUSPENDED = 2, UPKEEP = 3, UNLOADED = 4, LOST = 5, WITHOUT_PLACE = 6, UNCLAIMED = 7,
			IN_TRANSIT = 8, STRANDED = 9, RETURNING = 10, DISABLED = 11;
	/** Why a row waits, by number, as a mark beside its words (tools/concordance_roster.py ICONS; the client draws each). Append only. */
	public static final List<String> ICONS = List.of("active", "waiting", "suspended", "upkeep", "unloaded", "lost", "without_place",
			"unclaimed", "in_transit", "stranded", "returning", "disabled");
	public static final Type<RosterStatePayload> TYPE = new Type<>(Jugcraft.id("concordance_roster_state"));

	/**
	 * One row. {@code id}: a Bound Will's id, a courier request's (UUID(0, its id); {@link #request()}) or a worker's; a
	 * request names it back. {@code portrait}: the entity type the client draws (a request's item id; empty when none, and the
	 * client draws the Spirit Anchor). {@code kind}, {@code name}, {@code service}, {@code place} (and whether what it needs
	 * there is {@code loaded}), its current {@code task}, its {@code next} obligation and why it is {@code waiting} are words;
	 * {@code icon} is one of ICONS' numbers (never colour alone); {@code revision} fingerprints what an action on it depends on;
	 * {@code actions} has a bit for each action it offers now; {@code confirm} is what an end asks first, or what a cancel does
	 * (empty when neither).
	 */
	public record Row(UUID id, String portrait, Component kind, Component name, Component service, Component place, boolean loaded,
			Component task, Component next, Component waiting, int icon, int revision, int actions, Component confirm) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Row> STREAM_CODEC = StreamCodec.composite(
				UUIDUtil.STREAM_CODEC, Row::id,
				ByteBufCodecs.stringUtf8(MAX_PORTRAIT), Row::portrait,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::kind,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::name,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::service,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::place,
				ByteBufCodecs.BOOL, Row::loaded,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::task,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::next,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::waiting,
				ByteBufCodecs.VAR_INT, Row::icon,
				ByteBufCodecs.VAR_INT, Row::revision,
				ByteBufCodecs.VAR_INT, Row::actions,
				ComponentSerialization.TRUSTED_STREAM_CODEC, Row::confirm,
				Row::new);

		/** Whether it offers {@code action} now (the server still decides when it is pressed). */
		public boolean offers(int action) {
			return action >= 0 && action < 31 && (actions & (1 << action)) != 0;
		}

		/** Whether it is a courier request's row (its id's high bits are zero; a record's and a worker's are random). */
		public boolean request() {
			return id.getMostSignificantBits() == 0L;
		}
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, RosterStatePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, RosterStatePayload::sequence,
			ByteBufCodecs.BOOL, RosterStatePayload::enabled,
			Row.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ROWS)), RosterStatePayload::rows,
			ByteBufCodecs.VAR_INT, RosterStatePayload::more,
			ByteBufCodecs.optional(PactStatePayload.Result.STREAM_CODEC), RosterStatePayload::result,
			RosterStatePayload::new);

	public RosterStatePayload {
		rows = List.copyOf(rows.subList(0, Math.min(rows.size(), MAX_ROWS)));
		more = Math.max(0, more);
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
