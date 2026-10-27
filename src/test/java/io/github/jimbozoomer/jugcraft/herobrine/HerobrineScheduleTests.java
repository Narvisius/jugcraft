package io.github.jimbozoomer.jugcraft.herobrine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** Deterministic executable tests; no game, network, personal profile or wall-clock waits. */
public final class HerobrineScheduleTests {
	private static int checks;
	private static final Instant EVENT = Instant.parse("2026-10-31T20:00:00Z");
	private static final Instant QUIET = Instant.parse("2026-11-04T05:00:00Z");
	private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);
	private static void check(boolean yes, String why) { checks++; if (!yes) throw new AssertionError(why); }
	private static HerobrineSchedule fresh() { return new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(19), List.of()); }
	private static HerobrineSchedule.Candidate candidate(UUID id) { return new HerobrineSchedule.Candidate(id, true, true); }
	private static HerobrineSchedule.Request due(HerobrineSchedule s, UUID id, Instant now, int seconds) {
		for (int i = 0; i < seconds; i++) {
			var requests = s.step(now, List.of(candidate(id)));
			if (!requests.isEmpty()) return requests.getFirst();
		}
		throw new AssertionError("No sighting by deadline");
	}
	private static HerobrineSchedule.Progress overdue(UUID id, long sightings, long overdueTicks) {
		return new HerobrineSchedule.Progress(id, 100000, 100000 - overdueTicks, 6000, true, sightings, 1, 200000, 0);
	}
	public static void main(String[] args) {
		calendar(); firstAndFailures(); visibilityAndReservations(); persistence(); fairnessAndLoad(); quietTransition(); strongScares(); everyoneGetsATurn();
		System.out.println("Herobrine schedule: " + checks + " assertions passed");
	}
	private static void calendar() {
		check(HerobrineSeason.DEFAULT.eventActive(QUIET.minusNanos(1)), "Every instant of November 3 remains active");
		check(!HerobrineSeason.DEFAULT.eventActive(QUIET), "Quiet starts at Nov 4 midnight New York");
		check(HerobrineSeason.DEFAULT.eventActive(Instant.parse("2026-11-04T03:30:00Z")), "UTC Nov 4 is still New York Nov 3");
		var utc = new HerobrineSeason(LocalDate.of(2026, 11, 3), ZoneId.of("UTC"));
		check(utc.quietStarts().equals(Instant.parse("2026-11-04T00:00:00Z")), "Zone is configurable");
		var dst = new HerobrineSeason(LocalDate.of(2026, 11, 1), ZoneId.of("America/New_York"));
		check(dst.quietStarts().equals(Instant.parse("2026-11-02T05:00:00Z")), "DST calendar day uses new offset");
	}
	private static void firstAndFailures() {
		var s = fresh();
		for (int i = 0; i < 299; i++) check(s.step(EVENT, List.of(candidate(A))).isEmpty(), "No first visit before five active minutes");
		var r = due(s, A, EVENT, 301);
		check(r.kind() == HerobrineSchedule.Kind.WATCHER, "First visit is recognizable distant watcher");
		check(s.progress(A).activeTicks() <= 12000, "First request within ten active minutes");
		check(s.failed(r), "Failed placement releases request");
		check(s.progress(A).sightings() == 0, "Failed placement is not a sighting");
		for (int i = 0; i < 29; i++) check(s.step(EVENT, List.of(candidate(A))).isEmpty(), "Bound retry rate");
		var retry = s.step(EVENT, List.of(candidate(A))).getFirst();
		check(retry.token() != r.token(), "Retry uses new token");
		check(!s.placed(r, UUID.randomUUID()), "Stale token cannot place an entity");
		check(!s.failed(r), "Stale failure cannot cancel newer request");
	}
	private static void visibilityAndReservations() {
		var s = fresh(); var r = due(s, A, EVENT, 600); UUID encounter = UUID.randomUUID();
		check(!s.seen(r, encounter, A, EVENT), "Unplaced entity cannot claim seen");
		check(s.placed(r, encounter), "Bind entity");
		check(!s.placed(r, UUID.randomUUID()), "Cannot rebind reservation");
		check(s.step(EVENT, List.of(candidate(A), candidate(B), new HerobrineSchedule.Candidate(C, false, false))).isEmpty(), "No duplicate target");
		check(!s.seen(r, UUID.randomUUID(), A, EVENT), "Wrong entity cannot claim credit");
		check(!s.seen(r, encounter, C, EVENT), "AFK/offline bystander cannot claim credit");
		check(s.seen(r, encounter, B, EVENT), "Actually seen shared witness credited");
		check(s.progress(A).sightings() == 0, "Target not credited because somebody else saw it");
		check(!s.seen(r, encounter, B, EVENT), "Witness callback idempotent");
		check(s.ended(r, encounter), "Unseen target's encounter ends");
		check(s.progress(A).retryAt() - s.progress(A).activeTicks() == 600, "Unseen target retries shortly");
		check(!s.seen(r, encounter, A, EVENT), "Ended encounter cannot claim late sighting");
		check(s.progress(B).dueAt() - s.progress(B).activeTicks() >= 12000, "Witness gets normal repeat cooldown");
		var next = due(s, A, EVENT, 30); UUID nextEntity = UUID.randomUUID(); s.placed(next, nextEntity);
		check(s.seen(next, nextEntity, A, EVENT), "Target confirmed seen"); s.ended(next, nextEntity);
		check(s.progress(A).sightings() == 1 && s.progress(A).retryAt() == 0, "Successful sighting consumes no failure penalty");
	}
	private static void persistence() {
		var s = fresh(); s.step(EVENT, List.of(candidate(A))); long active = s.progress(A).activeTicks();
		for (int i = 0; i < 100; i++) s.step(EVENT.plusSeconds(100000), List.of(new HerobrineSchedule.Candidate(A, false, false)));
		check(s.progress(A).activeTicks() == active, "Offline and AFK time do not accumulate");
		s.step(EVENT, List.of(candidate(A), candidate(A)));
		check(s.progress(A).activeTicks() == active + 20, "Duplicate candidate cannot double active time");
		var copy = new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(32), s.snapshot());
		check(copy.snapshot().equals(s.snapshot()), "Restart preserves exact player progress");
		var r = due(copy, A, EVENT, 600); copy.placed(r, UUID.randomUUID());
		var restart = new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(42), copy.snapshot());
		check(restart.liveCount() == 0, "Ephemeral entities/reservations discarded after restart");
		check(restart.step(EVENT, List.of(candidate(A))).size() == 1, "Unseen interruption retries, not long cooldown");
		var lease = fresh(); var request = due(lease, A, EVENT, 600);
		for (int i = 0; i < 180; i++) lease.step(EVENT, List.of());
		check(lease.liveCount() == 0 && !lease.failed(request), "Missing outcome cannot reserve forever");
	}
	private static void fairnessAndLoad() {
		var s = new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(19),
				List.of(overdue(A, 10, 9999), overdue(B, 0, 100), overdue(C, 0, 200)));
		var batch = s.step(EVENT, List.of(candidate(A), candidate(B), candidate(C)));
		check(batch.size() == 2 && batch.get(0).player().equals(C) && batch.get(1).player().equals(B), "Unseen first; oldest overdue wins independent of list order");
		batch.forEach(s::failed);
		check(s.step(EVENT, List.of(candidate(A), candidate(B), candidate(C))).getFirst().player().equals(A), "Retries do not block others");
		var rows = new ArrayList<HerobrineSchedule.Progress>(); var players = new ArrayList<HerobrineSchedule.Candidate>();
		for (int i = 0; i < 100; i++) { UUID id = new UUID(1, i); rows.add(overdue(id, 0, i)); players.add(candidate(id)); }
		var many = new HerobrineSchedule(HerobrineSeason.DEFAULT, new Random(19), rows);
		for (int i = 0; i < 20; i++) check(many.step(EVENT, players).size() <= 2, "Bounded attempts per second");
		check(many.liveCount() == HerobrineSchedule.MAX_LIVE, "Bounded concurrent encounters");
	}
	private static void quietTransition() {
		var quiet = fresh(); var r = due(quiet, A, QUIET, 90 * 60);
		check(quiet.progress(A).activeTicks() >= 45 * 60 * 20, "Quiet first encounter at least 45 active minutes");
		UUID entity = UUID.randomUUID(); quiet.placed(r, entity); quiet.seen(r, entity, A, QUIET);
		long repeat = quiet.progress(A).dueAt() - quiet.progress(A).activeTicks();
		check(repeat >= 60 * 60 * 20 && repeat <= 180 * 60 * 20, "Quiet repeats one to three active hours");
		var s = fresh(); s.step(EVENT, List.of(candidate(A))); var before = s.progress(A);
		s.step(QUIET, List.of(candidate(A))); var after = s.progress(A);
		check(after.dueAt() > before.dueAt() && !after.eventMode(), "Quiet transition scales pending wait");
		check(after.activeTicks() == before.activeTicks() + 20, "Calendar jump does not simulate offline play");
	}
	private static void strongScares() {
		Random minimum = new Random(0) {
			@Override public int nextInt(int bound) { return 0; }
			@Override public int nextInt(int origin, int bound) { return origin; }
		};
		var experienced = new HerobrineSchedule.Progress(A, 100000, 90000, 6000, true, 2, 1, 0, 0);
		var s = new HerobrineSchedule(HerobrineSeason.DEFAULT, minimum, List.of(experienced));
		var request = s.step(EVENT, List.of(candidate(A))).getFirst();
		check(request.kind() == HerobrineSchedule.Kind.CLOSE, "Experienced calm player can get a rare close encounter");
		UUID entity = UUID.randomUUID(); s.placed(request, entity); s.seen(request, entity, A, EVENT); s.ended(request, entity);
		check(s.progress(A).strongDueAt() - s.progress(A).activeTicks() == 45 * 60 * 20, "Close scare starts separate long cooldown");
		var next = due(s, A, EVENT, 10 * 60);
		check(next.kind() == HerobrineSchedule.Kind.WATCHER, "Ordinary sighting does not bypass strong-scare cooldown");
		var unsafe = new HerobrineSchedule(HerobrineSeason.DEFAULT, minimum, List.of(experienced));
		check(unsafe.step(EVENT, List.of(new HerobrineSchedule.Candidate(A, true, false))).getFirst().kind()
				== HerobrineSchedule.Kind.WATCHER, "Unsafe state never requests close scare");
	}
	private static void everyoneGetsATurn() {
		var s = fresh(); var players = new ArrayList<HerobrineSchedule.Candidate>();
		for (int i = 0; i < 200; i++) players.add(candidate(new UUID(2, i)));
		for (int second = 0; second < 650; second++) {
			for (var request : s.step(EVENT, players)) {
				UUID entity = UUID.randomUUID();
				s.placed(request, entity); s.seen(request, entity, request.player(), EVENT); s.ended(request, entity);
			}
		}
		check(s.snapshot().size() == 200 && s.snapshot().stream().allMatch(p -> p.sightings() == 1),
				"Two hundred eligible players each receive their own first turn when placement and witnessing succeed");
	}
}
