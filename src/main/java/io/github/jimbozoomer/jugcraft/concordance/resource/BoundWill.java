package io.github.jimbozoomer.jugcraft.concordance.resource;

import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One Bound Will record: an agreement with a named counterpart (a spirit, a construct), held by one player. It has an
 * identity of its own and is never merged, split or counted as an amount; it moves only as itself, and only if its
 * terms allow it to change hands. Since Concordance Chunk D (D-02, D-S9) a record sealed at a place carries its
 * {@link Pact}: the one authority for where it is kept, whether it is held and why, who benefits, the presence serving it,
 * its upkeep, and the service terms and day's count it was sealed with (so a pact can be seated again at a new place, D-S6).
 * A record saved before carries none until the block its terms were sealed at claims it.
 */
public record BoundWill(UUID id, String agreement, String counterpart, UUID holder, long sealedAt, boolean transferable, @Nullable Pact pact) {
	/** A record without pact data: one saved before Concordance Chunk D, or a plain ledger entry. */
	public BoundWill(UUID id, String agreement, String counterpart, UUID holder, long sealedAt, boolean transferable) {
		this(id, agreement, counterpart, holder, sealedAt, transferable, null);
	}

	public BoundWill withHolder(UUID newHolder) {
		return new BoundWill(id, agreement, counterpart, newHolder, sealedAt, transferable, pact);
	}

	public BoundWill with(@Nullable Pact next) {
		return new BoundWill(id, agreement, counterpart, holder, sealedAt, transferable, next);
	}

	/** Why a pact's service is off (D-S9): not held, held by its holder, or held for its upkeep. Ids are saved: append only. */
	public enum Hold {
		NONE("none"),
		HOLDER("holder"),
		UPKEEP("upkeep");

		public final String id;

		Hold(String id) {
			this.id = id;
		}

		public static @Nullable Hold fromId(String id) {
			for (Hold hold : values()) {
				if (hold.id.equals(id)) {
					return hold;
				}
			}
			return null;
		}
	}

	/**
	 * A pact's state (D-02): where it is kept (null: without its place), whether it is held and since when, who benefits (a
	 * UseMode id, "personal" or "party"), the presence serving it (null: none called), its upkeep (null: none), the service
	 * terms copied at sealing (D-S23; null: none copied, as for a pact made in a test) and its day's count of tasks (the day
	 * of game time {@code window}, {@link #NO_WINDOW} before its first task; D-S1: once a game day per pact, wherever it is
	 * seated).
	 */
	public record Pact(@Nullable Place place, Hold hold, long holdSince, String beneficiaries, @Nullable UUID presence, @Nullable Upkeep upkeep,
			@Nullable Service service, long window, int doneToday) {
		public static final long NO_WINDOW = Long.MIN_VALUE;

		/** A pact with no service terms copied and no task counted. */
		public Pact(@Nullable Place place, Hold hold, long holdSince, String beneficiaries, @Nullable UUID presence, @Nullable Upkeep upkeep) {
			this(place, hold, holdSince, beneficiaries, presence, upkeep, null, NO_WINDOW, 0);
		}

		public Pact at(@Nullable Place where) {
			return new Pact(where, hold, holdSince, beneficiaries, presence, upkeep, service, window, doneToday);
		}

		public Pact held(Hold why, long since) {
			return new Pact(place, why, since, beneficiaries, presence, upkeep, service, window, doneToday);
		}

		public Pact withPresence(@Nullable UUID next) {
			return new Pact(place, hold, holdSince, beneficiaries, next, upkeep, service, window, doneToday);
		}

		public Pact with(@Nullable Upkeep next) {
			return new Pact(place, hold, holdSince, beneficiaries, presence, next, service, window, doneToday);
		}

		public Pact counted(long day, int done) {
			return new Pact(place, hold, holdSince, beneficiaries, presence, upkeep, service, day, done);
		}
	}

	/** The service terms a pact was sealed with (D-S23: a sealed pact keeps them): its one work, area, hours and daily quota. */
	public record Service(String work, int radius, int from, int to, int quota) {
	}

	/**
	 * Upkeep terms copied at sealing (offering: an item tag id; count a period; period and grace in game ticks; cap: the most
	 * periods its escrow holds) and its clock: when the next offering falls due, the grace's end once warned
	 * ({@link #NOT_WARNED} otherwise) and when it was last looked at.
	 */
	public record Upkeep(String offering, int count, long period, long grace, int cap, long due, long graceUntil, long lookedAt) {
		public static final long NOT_WARNED = Long.MIN_VALUE;

		public boolean warned() {
			return graceUntil != NOT_WARNED;
		}

		public Upkeep clock(long nextDue, long nextGraceUntil, long looked) {
			return new Upkeep(offering, count, period, grace, cap, nextDue, nextGraceUntil, looked);
		}
	}
}
