package io.github.jimbozoomer.jugcraft.concordance.worker;

import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, unit 1 (D-01, D-02): the rules every kept agreement shares, its one record being its Bound Will
 * ({@link BoundWill.Pact}). Pure; it holds no state: the server says what it sees (who acts, what the ledger holds, whether
 * the holder is present near the place, how many periods the escrow holds, the time). A seal is refused with its first
 * reason, before anything is recorded, spent or called (D-S11). A record is kept at one place, with the service terms and
 * the day's count it was sealed with. Suspension is the record's: by its holder (the clock pauses) or for its upkeep.
 * Upkeep on the Spire's model (wonder/Spires.java:152-176): looked at only while the place is loaded, the Concordance is
 * on and the holder is present near it; at most one period falls due a look, and a due date that passed while nobody
 * looked becomes the moment they look again, so time away is never owed; none in escrow, a warning and a grace; the grace
 * over, held for its upkeep: nothing taken, nothing accrues, nothing lost, and an offering resumes it with a whole period
 * from then. After an absence (no look for more than ABSENT_TICKS) a pact due is warned on return: a first warning with
 * its whole grace (D-S3), one given before the absence again with the grace it had left, its end moved on by the time
 * away (D-P50); a shorter gap is not an absence, so a due date or grace end inside it counts. A pact
 * never ends by itself; only its holder ends it, once; the last HISTORY departures of each holder are kept. Keep the
 * numbers, Outcome, holds and DEPARTURES equal to tools/concordance_workers.py (SEAL_OUTCOMES, PACT, PACT_HOLDS,
 * PACT_DEPARTURES; check_pacts).
 */
public final class Pacts {
	public static final long DAY = Agreement.DAY;
	public static final long ABSENT_TICKS = 1200L;
	public static final long WRITE_TICKS = 200L;
	public static final int REACH = 64;
	public static final int HISTORY = 8;
	public static final int MAX_CAP = 4;
	public static final int MAX_COUNT = 64;
	public static final long MAX_PERIOD = 672000L;
	public static final long MAX_GRACE = 168000L;
	public static final String PERSONAL = "personal";
	public static final String PARTY = "party";
	/** Why a pact departed: its holder ended it, or its place was broken. */
	public static final String ENDED = "ended";
	public static final String BROKEN = "broken";
	/** Why a pact departed (append only; each worded as message.jugcraft.concordance.workers.departed.<id>). */
	public static final List<String> DEPARTURES = List.of(ENDED, BROKEN);

	private Pacts() {
	}

	/**
	 * How a seal went. Ids are stable (append only); each is worded as {@code message.jugcraft.concordance.workers.seal_<id>}
	 * (DISABLED as the switch's).
	 */
	public enum Outcome {
		DONE("done"),
		DISABLED("disabled"),
		NOT_PERMITTED("not_permitted"),
		TAKEN("taken"),
		ALREADY_HELD("already_held"),
		HOLDER_FULL("holder_full"),
		ROSTER_FULL("roster_full"),
		UNAVAILABLE("unavailable"),
		CANNOT_AFFORD("cannot_afford");

		public final String id;

		Outcome(String id) {
			this.id = id;
		}
	}

	/** What one look at a pact's upkeep came to. */
	public enum Event {
		NONE, PAID, WARNED, SUSPENDED, RESUMED
	}

	/** One look: the record's next state, whether to write it, what happened, whether to take one period's offerings. */
	public record Due(BoundWill next, boolean changed, Event event, boolean take) {
	}

	/** One departure kept in its holder's history. */
	public record Departure(UUID record, String agreement, String counterpart, String reason, long time, @Nullable Place place) {
	}

	/**
	 * The first reason a seal is refused now, in this order, or DONE: the actor may not seal at this place (D-S8); the place
	 * already keeps an agreement (TAKEN, D-S10); they keep one with this counterpart and it allows one (ALREADY_HELD); they
	 * hold BoundWillLedger.MAX_PER_HOLDER Bound Wills (HOLDER_FULL); their roster has no room (ROSTER_FULL, D-S11). DISABLED,
	 * UNAVAILABLE and CANNOT_AFFORD are the server's (the Concordance off; a presence that did not come; a cost not met).
	 */
	public static Outcome refusal(boolean permitted, boolean placeKept, boolean counterpartHeld, int held, boolean rosterFull) {
		if (!permitted) {
			return Outcome.NOT_PERMITTED;
		}
		if (placeKept) {
			return Outcome.TAKEN;
		}
		if (counterpartHeld) {
			return Outcome.ALREADY_HELD;
		}
		if (held >= BoundWillLedger.MAX_PER_HOLDER) {
			return Outcome.HOLDER_FULL;
		}
		return rosterFull ? Outcome.ROSTER_FULL : Outcome.DONE;
	}

	/** Whether {@code holder} keeps a record with {@code counterpart} (namespaces normalised: records before G9 wrote "gathering_shade"). */
	public static boolean holds(BoundWillLedger ledger, UUID holder, String counterpart) {
		String wanted = counterpart(counterpart);
		for (BoundWill record : ledger.heldBy(holder)) {
			if (counterpart(record.counterpart()).equals(wanted)) {
				return true;
			}
		}
		return false;
	}

	/** A counterpart id with its namespace ("jugcraft" when a record from before G9 wrote none). */
	public static String counterpart(String id) {
		return id.indexOf(':') >= 0 ? id : "jugcraft:" + id;
	}

	/** The record kept at {@code place}, or null. */
	public static @Nullable BoundWill keptAt(BoundWillLedger ledger, Place place) {
		for (BoundWill record : ledger.records().values()) {
			if (record.pact() != null && place.equals(record.pact().place())) {
				return record;
			}
		}
		return null;
	}

	/**
	 * Concordance Chunk D, D-04: {@code holder}'s record with {@code counterpart} that carries a pact (one a holder, D-S10),
	 * or null; namespaces normalised.
	 */
	public static @Nullable BoundWill pactWith(BoundWillLedger ledger, UUID holder, String counterpart) {
		String wanted = counterpart(counterpart);
		for (BoundWill record : ledger.heldBy(holder)) {
			if (record.pact() != null && counterpart(record.counterpart()).equals(wanted)) {
				return record;
			}
		}
		return null;
	}

	/** Upkeep terms only (clock unset): what a definition, or a test, seals with. */
	public static BoundWill.Upkeep terms(String offering, int count, long period, long grace, int cap) {
		return new BoundWill.Upkeep(offering, count, period, grace, cap, 0L, BoundWill.Upkeep.NOT_WARNED, 0L);
	}

	/** The service terms an agreement was sealed with. */
	public static BoundWill.Service service(Agreement terms) {
		return new BoundWill.Service(terms.work(), terms.radius(), terms.from(), terms.to(), terms.quota());
	}

	/** A pact sealed now: unheld, its service terms copied, no task done; its first offering falls due a period on (D-04's compact pays the first). */
	public static BoundWill.Pact sealed(Place place, String beneficiaries, @Nullable UUID presence, BoundWill.@Nullable Service service,
			BoundWill.@Nullable Upkeep terms, long now) {
		BoundWill.Upkeep upkeep = terms == null ? null : terms.clock(now + terms.period(), BoundWill.Upkeep.NOT_WARNED, now);
		return new BoundWill.Pact(place, BoundWill.Hold.NONE, now, beneficiaries, presence, upkeep, service, BoundWill.Pact.NO_WINDOW, 0);
	}

	/**
	 * A record from before pacts claimed at {@code place} by the block keeping {@code terms}: its service terms and day's count
	 * copied from them, and a suspension the block kept moved onto it. Once only.
	 */
	public static BoundWill claim(BoundWill record, Place place, @Nullable UUID presence, Agreement terms, long now) {
		if (record.pact() != null) {
			return record;
		}
		return record.with(new BoundWill.Pact(place, terms.suspended() ? BoundWill.Hold.HOLDER : BoundWill.Hold.NONE, now, PERSONAL, presence, null,
				service(terms), terms.window(), terms.doneToday()));
	}

	/** Whether a record carries no pact yet: one from before Chunk D that no block has claimed (not one without its place). */
	public static boolean unclaimed(BoundWill record) {
		return record.pact() == null;
	}

	/** Its holder suspends it (null unless unheld). */
	public static @Nullable BoundWill suspend(BoundWill record, long now) {
		BoundWill.Pact pact = record.pact();
		return pact == null || pact.hold() != BoundWill.Hold.NONE ? null : record.with(pact.held(BoundWill.Hold.HOLDER, now));
	}

	/**
	 * Its holder resumes what they suspended (null unless held by its holder): its clock moves on by the time held, and so does
	 * its last look (never past now), so an absence before the hold still counts as one (the unit review's fix 1). Looks
	 * return early while its holder holds it, so the last look is never after the hold began.
	 */
	public static @Nullable BoundWill resume(BoundWill record, long now) {
		BoundWill.Pact pact = record.pact();
		if (pact == null || pact.hold() != BoundWill.Hold.HOLDER) {
			return null;
		}
		BoundWill.Upkeep upkeep = pact.upkeep();
		long held = Math.max(0L, now - pact.holdSince());
		BoundWill.Upkeep moved = upkeep == null ? null
				: upkeep.clock(upkeep.due() + held, upkeep.warned() ? upkeep.graceUntil() + held : BoundWill.Upkeep.NOT_WARNED,
						Math.min(now, upkeep.lookedAt() + held));
		return record.with(pact.held(BoundWill.Hold.NONE, now).with(moved));
	}

	/**
	 * One look at {@code record}'s upkeep at {@code now} (the caller checks the Concordance is on, D-S16). After an absence a
	 * due date that has passed becomes now, so a holder back just before the next due date pays once, and the next falls a
	 * whole period later; resumed from an upkeep hold by an offering, the next falls a whole period from now: neither time
	 * away nor time held is ever charged (critique 3). A gap of at most ABSENT_TICKS is not an absence. A second look in the
	 * same tick goes on (the unit review's fix 7), so an offering paid in the tick of a ticker's look resumes at once. A
	 * holder warned before an absence is warned again on return with the grace they had left, its end moved on by the time
	 * away (unit 2's unit review, D-P50): time away is never charged, and leaving for a minute never renews it.
	 */
	public static Due due(BoundWill record, boolean present, int periodsHeld, long now) {
		BoundWill.Pact pact = record.pact();
		BoundWill.Upkeep upkeep = pact == null ? null : pact.upkeep();
		if (upkeep == null || !present || pact.hold() == BoundWill.Hold.HOLDER || now < upkeep.lookedAt()) {
			return new Due(record, false, Event.NONE, false);
		}
		boolean returned = now - upkeep.lookedAt() > ABSENT_TICKS;
		long dueAt = returned && upkeep.due() < now ? now : upkeep.due();
		if (pact.hold() == BoundWill.Hold.UPKEEP) {
			if (periodsHeld >= 1) {
				BoundWill.Upkeep paid = upkeep.clock(now + upkeep.period(), BoundWill.Upkeep.NOT_WARNED, now);
				return new Due(record.with(pact.held(BoundWill.Hold.NONE, now).with(paid)), true, Event.RESUMED, true);
			}
			return looked(record, pact, upkeep, now, returned);
		}
		if (now >= dueAt) {
			if (periodsHeld >= 1) {
				BoundWill.Upkeep paid = upkeep.clock(following(dueAt, upkeep.period(), now), BoundWill.Upkeep.NOT_WARNED, now);
				return new Due(record.with(pact.with(paid)), true, Event.PAID, true);
			}
			if (!upkeep.warned() || returned) {
				// The first warning, with the whole grace from this look; and after an absence (the holder away, the place
				// unloaded or the Concordance off) the warning again: the whole grace for a first warning (D-S3, critique 2a),
				// the grace left for one given before the absence, its end moved on by the time away (the unit review).
				long graceUntil = upkeep.warned() ? upkeep.graceUntil() + (now - upkeep.lookedAt()) : now + upkeep.grace();
				return new Due(record.with(pact.with(upkeep.clock(dueAt, graceUntil, now))), true, Event.WARNED, false);
			}
			if (now >= upkeep.graceUntil()) {
				return new Due(record.with(pact.held(BoundWill.Hold.UPKEEP, now).with(upkeep.clock(dueAt, upkeep.graceUntil(), now))),
						true, Event.SUSPENDED, false);
			}
		}
		return looked(record, pact, upkeep, now, returned);
	}

	private static Due looked(BoundWill record, BoundWill.Pact pact, BoundWill.Upkeep upkeep, long now, boolean returned) {
		if (!returned && now - upkeep.lookedAt() < WRITE_TICKS) {
			return new Due(record, false, Event.NONE, false);
		}
		return new Due(record.with(pact.with(upkeep.clock(upkeep.due(), upkeep.graceUntil(), now))), true, Event.NONE, false);
	}

	/** Spires.java:163-166: the next due a period on, or a period from now if that has passed. */
	static long following(long due, long period, long now) {
		long next = due + period;
		return next <= now ? now + period : next;
	}

	/** Tasks done in the day of game time {@code now} (Agreement.done's rule, kept on the record: once a game day per pact, D-S1). */
	public static int done(BoundWill.Pact pact, long now) {
		return Math.floorDiv(now, DAY) == pact.window() ? pact.doneToday() : 0;
	}

	/** One task done at {@code now} (Agreement.didTask's rule). */
	public static BoundWill.Pact didTask(BoundWill.Pact pact, long now) {
		return pact.counted(Math.floorDiv(now, DAY), done(pact, now) + 1);
	}

	public static boolean held(BoundWill record) {
		return record.pact() != null && record.pact().hold() != BoundWill.Hold.NONE;
	}

	/** SUSPENDED (by its holder), UPKEEP_HELD, UPKEEP_DUE (warned, still serving) or null. */
	public static @Nullable Status status(BoundWill record) {
		BoundWill.Pact pact = record.pact();
		if (pact == null) {
			return null;
		}
		return switch (pact.hold()) {
			case HOLDER -> Status.SUSPENDED;
			case UPKEEP -> Status.UPKEEP_HELD;
			case NONE -> pact.upkeep() != null && pact.upkeep().warned() ? Status.UPKEEP_DUE : null;
		};
	}

	/** Whether it is kept at a place (false for one without its place, and for one not yet claimed: see {@link #unclaimed}). */
	public static boolean seated(BoundWill record) {
		return record.pact() != null && record.pact().place() != null;
	}

	/** Without its place (an orphan's place sealed again; D-04's perch broken): kept, its presence gone. */
	public static BoundWill unseat(BoundWill record) {
		return record.pact() == null ? record : record.with(record.pact().at(null).withPresence(null));
	}

	/** Whether {@code who} benefits: its holder always; their party only when the pact says party. */
	public static boolean benefits(BoundWill record, UUID who, boolean sameParty) {
		return record.holder().equals(who) || record.pact() != null && PARTY.equals(record.pact().beneficiaries()) && sameParty;
	}

	public static boolean beneficiaries(String id) {
		return PERSONAL.equals(id) || PARTY.equals(id);
	}

	/** "" within bounds; otherwise the first field that is not. */
	public static String valid(BoundWill.Upkeep upkeep) {
		if (!upkeep.offering().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
			return "offering";
		}
		if (upkeep.count() < 1 || upkeep.count() > MAX_COUNT) {
			return "count";
		}
		if (upkeep.period() < DAY || upkeep.period() > MAX_PERIOD) {
			return "period";
		}
		if (upkeep.grace() < 0 || upkeep.grace() > MAX_GRACE) {
			return "grace";
		}
		if (upkeep.cap() < 1 || upkeep.cap() > MAX_CAP) {
			return "cap";
		}
		return "";
	}

	/** The pact audit (none expected; tests run it): no place keeps two, holds and upkeeps in bounds, no holder over the cap. */
	public static List<String> problems(BoundWillLedger ledger) {
		List<String> problems = new ArrayList<>();
		Map<Place, UUID> places = new HashMap<>();
		Map<UUID, Integer> held = new HashMap<>();
		for (BoundWill record : ledger.records().values()) {
			held.merge(record.holder(), 1, Integer::sum);
			BoundWill.Pact pact = record.pact();
			if (pact == null) {
				continue;
			}
			if (pact.place() != null) {
				UUID other = places.putIfAbsent(pact.place(), record.id());
				if (other != null) {
					problems.add("place " + pact.place() + " keeps two agreements: " + other + " and " + record.id());
				}
			}
			if (!beneficiaries(pact.beneficiaries())) {
				problems.add("record " + record.id() + ": unknown beneficiaries " + pact.beneficiaries());
			}
			if (pact.hold() == BoundWill.Hold.UPKEEP && pact.upkeep() == null) {
				problems.add("record " + record.id() + ": held for an upkeep it does not have");
			}
			if (pact.upkeep() != null && !valid(pact.upkeep()).isEmpty()) {
				problems.add("record " + record.id() + ": upkeep " + valid(pact.upkeep()) + " out of bounds");
			}
			if (pact.doneToday() < 0) {
				problems.add("record " + record.id() + ": " + pact.doneToday() + " tasks done");
			}
		}
		held.forEach((holder, count) -> {
			if (count > BoundWillLedger.MAX_PER_HOLDER) {
				problems.add("holder " + holder + " holds " + count + " records");
			}
		});
		return problems;
	}

	public static List<Departure> departed(List<Departure> history, Departure next) {
		List<Departure> all = new ArrayList<>(history);
		all.add(next);
		return bounded(all);
	}

	public static List<Departure> bounded(List<Departure> history) {
		return List.copyOf(history.subList(Math.max(0, history.size() - HISTORY), history.size()));
	}

	/**
	 * How a pact's service went (Concordance Chunk D, D-05, D-S1): its holder's Star-reckoning at the perch. After RECALLED,
	 * in the order checked: the server's (switched off; nothing kept here; a copy; not its holder in person), the pact's
	 * ({@link #serves}), then the pattern's, the astrolabe's and the claim's. Worded as
	 * message.jugcraft.concordance.pact.reckoning.<id> (DISABLED as the switch's, CONFLICTING as the diagnostic's); equal to
	 * tools/concordance_pacts.py RECKONING (check_pacts). Append only.
	 */
	public enum Reckoning {
		RECALLED("recalled"),
		DISABLED("disabled"),
		NOTHING("nothing"),
		CONFLICTING("conflicting"),
		NOT_HOLDER("not_holder"),
		SUSPENDED("suspended"),
		UPKEEP_HELD("upkeep_held"),
		OUTSIDE_AREA("outside_area"),
		OUTSIDE_HOURS("outside_hours"),
		QUOTA_SPENT("quota_spent"),
		NOT_OBSERVED("not_observed"),
		NO_ASTROLABE("no_astrolabe"),
		TOO_LITTLE("too_little"),
		RECALLED_ALREADY("recalled_already");

		public final String id;

		Reckoning(String id) {
			this.id = id;
		}
	}

	/**
	 * The first reason {@code record}'s pact serves nobody now, in this order, or null: no service or no place (NOTHING);
	 * held by its holder (SUSPENDED); held for upkeep (UPKEEP_HELD); {@code who} outside its area or in another dimension
	 * (OUTSIDE_AREA); outside its hours by the world's clock {@code time} (OUTSIDE_HOURS); the quota of the game day of
	 * {@code gameTime} spent (QUOTA_SPENT: game time only runs forward, so moving the clock cannot reset it). An offering
	 * due still serves through its grace.
	 */
	public static @Nullable Reckoning serves(BoundWill record, Place who, long time, long gameTime) {
		BoundWill.Pact pact = record.pact();
		BoundWill.Service service = pact == null ? null : pact.service();
		if (service == null || pact.place() == null) {
			return Reckoning.NOTHING;
		}
		if (pact.hold() == BoundWill.Hold.HOLDER) {
			return Reckoning.SUSPENDED;
		}
		if (pact.hold() == BoundWill.Hold.UPKEEP) {
			return Reckoning.UPKEEP_HELD;
		}
		if (!pact.place().within(who, service.radius())) {
			return Reckoning.OUTSIDE_AREA;
		}
		if (!inHours(service, time)) {
			return Reckoning.OUTSIDE_HOURS;
		}
		return done(pact, gameTime) >= service.quota() ? Reckoning.QUOTA_SPENT : null;
	}

	/** Whether the world clock's hour allows its service; the hours may run past midnight (as Agreement.inHours). */
	public static boolean inHours(BoundWill.Service service, long time) {
		int tick = (int) Math.floorMod(time, DAY);
		return service.from() <= service.to() ? tick >= service.from() && tick < service.to() : tick >= service.from() || tick < service.to();
	}

	/** Whether {@code record}'s pact is active (D-05): kept at its place and held by nobody (an offering due still serves). */
	public static boolean serving(BoundWill record) {
		BoundWill.Pact pact = record.pact();
		return pact != null && pact.place() != null && pact.hold() == BoundWill.Hold.NONE;
	}
}
