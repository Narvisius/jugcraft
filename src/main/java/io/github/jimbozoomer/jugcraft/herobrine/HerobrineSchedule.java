package io.github.jimbozoomer.jugcraft.herobrine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Server-thread-only pacing core, independent of Minecraft. One step represents one second of live simulation;
 * neither wall-clock jumps, reconnects nor offline time earn active-play credit. Placement is NOT a successful visit.
 */
public final class HerobrineSchedule {
	public static final int TICKS_PER_SECOND = 20;
	public static final int RETRY_TICKS = 30 * TICKS_PER_SECOND;
	public static final int LEASE_TICKS = 180 * TICKS_PER_SECOND;
	public static final int MAX_LIVE = 32;
	public static final int MAX_REQUESTS_PER_STEP = 2;
	private static final int MINUTE = 60 * TICKS_PER_SECOND;
	public enum Kind { WATCHER, CLOSE }
	/** active must exclude menus, AFK, sleep, death, spectators and unsafe transitions. */
	public record Candidate(UUID player, boolean active, boolean calm) {}
	public record Request(UUID player, Kind kind, long token) {}
	/** Only progress is saved; entities and reservations deliberately do not survive a restart. */
	public record Progress(UUID player, long activeTicks, long dueAt, int interval, boolean eventMode,
			long sightings, long lastSeenEpochSecond, long strongDueAt, long retryAt) {
		public Progress {
			Objects.requireNonNull(player);
			if (activeTicks < 0 || dueAt < 0 || interval <= 0 || sightings < 0 || strongDueAt < 0 || retryAt < 0)
				throw new IllegalArgumentException("Invalid Herobrine progress");
		}
	}
	private static final class Lease {
		final Request request;
		final long expires;
		final Set<UUID> witnesses = new HashSet<>();
		UUID encounter;
		Lease(Request request, long expires) { this.request = request; this.expires = expires; }
	}
	private final Map<UUID, Progress> progress = new HashMap<>();
	private final Map<Long, Lease> leases = new HashMap<>();
	private final Map<UUID, Long> reserved = new HashMap<>();
	private final Set<UUID> activeNow = new HashSet<>();
	private final RandomGenerator random;
	private HerobrineSeason season;
	private long simulationTicks;
	private long nextToken = 1;
	private long revision;
	private boolean eventMode;

	public HerobrineSchedule(HerobrineSeason season, RandomGenerator random, Collection<Progress> saved) {
		this.season = Objects.requireNonNull(season);
		this.random = Objects.requireNonNull(random);
		for (Progress p : saved) {
			if (progress.putIfAbsent(p.player(), p) != null) throw new IllegalArgumentException("Duplicate Herobrine player");
		}
	}
	public void season(HerobrineSeason value) { season = Objects.requireNonNull(value); }
	public long revision() { return revision; }
	public List<Progress> snapshot() {
		return progress.values().stream().sorted(Comparator.comparing(p -> p.player().toString())).toList();
	}
	public Progress progress(UUID player) { return progress.get(player); }
	public int liveCount() { return leases.size(); }

	/** Called ONCE a second, regardless of player count. Return at most two placement attempts. */
	public List<Request> step(Instant now, Collection<Candidate> candidates) {
		eventMode = season.eventActive(now);
		simulationTicks += TICKS_PER_SECOND;
		for (Lease lease : new ArrayList<>(leases.values())) {
			if (lease.expires <= simulationTicks) finish(lease);
		}
		activeNow.clear();
		Map<UUID, Candidate> eligible = new HashMap<>();
		for (Candidate candidate : candidates) {
			if (!candidate.active() || eligible.putIfAbsent(candidate.player(), candidate) != null) continue;
			activeNow.add(candidate.player());
			Progress p = progress.get(candidate.player());
			if (p == null) {
				int interval = interval(true, eventMode);
				p = new Progress(candidate.player(), 0, interval, interval, eventMode, 0, 0,
						strongInterval(eventMode), 0);
			} else if (p.eventMode() != eventMode) {
				// Preserve the elapsed fraction when the calendar changes, rather than handing out one last busy burst.
				int interval = interval(p.sightings() == 0, eventMode);
				long remaining = Math.max(0, p.dueAt() - p.activeTicks());
				long scaled = Math.min(interval, (long) Math.ceil((double) remaining * interval / p.interval()));
				p = new Progress(p.player(), p.activeTicks(), p.activeTicks() + scaled, interval, eventMode,
						p.sightings(), p.lastSeenEpochSecond(), Math.max(p.strongDueAt(), p.activeTicks() + strongInterval(eventMode)), p.retryAt());
			}
			put(new Progress(p.player(), p.activeTicks() + TICKS_PER_SECOND, p.dueAt(), p.interval(), p.eventMode(),
					p.sightings(), p.lastSeenEpochSecond(), p.strongDueAt(), p.retryAt()));
		}
		List<Progress> due = eligible.keySet().stream().map(progress::get)
				.filter(p -> p.activeTicks() >= p.dueAt() && p.activeTicks() >= p.retryAt() && !reserved.containsKey(p.player()))
				.sorted(Comparator.<Progress, Boolean>comparing(p -> p.sightings() != 0)
						.thenComparing(Comparator.comparingLong((Progress p) -> p.activeTicks() - p.dueAt()).reversed())
						.thenComparingLong(Progress::lastSeenEpochSecond).thenComparing(p -> p.player().toString()))
				.limit(Math.min(MAX_REQUESTS_PER_STEP, MAX_LIVE - leases.size())).toList();
		List<Request> requests = new ArrayList<>();
		for (Progress p : due) {
			boolean close = p.sightings() >= 2 && p.activeTicks() >= p.strongDueAt()
					&& eligible.get(p.player()).calm() && random.nextInt(8) == 0;
			Request request = new Request(p.player(), close ? Kind.CLOSE : Kind.WATCHER, nextToken++);
			leases.put(request.token(), new Lease(request, simulationTicks + LEASE_TICKS));
			reserved.put(request.player(), request.token());
			requests.add(request);
		}
		return List.copyOf(requests);
	}

	/** Bind an actual entity once; a reused/stale token cannot acquire a different encounter. */
	public boolean placed(Request request, UUID encounter) {
		Lease lease = lease(request);
		if (lease == null || lease.encounter != null || encounter == null) return false;
		lease.encounter = encounter;
		return true;
	}
	public boolean failed(Request request) {
		Lease lease = lease(request);
		if (lease == null || lease.encounter != null) return false;
		finish(lease);
		return true;
	}
	/** Only call after server-side sustained gaze/line of sight and current safety validation. Not a client RPC. */
	public boolean seen(Request request, UUID encounter, UUID witness, Instant now) {
		Lease lease = lease(request);
		if (lease == null || !Objects.equals(encounter, lease.encounter) || encounter == null
				|| !activeNow.contains(witness) || !lease.witnesses.add(witness)) return false;
		Progress p = progress.get(witness);
		int interval = interval(false, season.eventActive(now));
		long strong = request.kind() == Kind.CLOSE ? p.activeTicks() + strongInterval(season.eventActive(now)) : p.strongDueAt();
		put(new Progress(p.player(), p.activeTicks(), p.activeTicks() + interval, interval, season.eventActive(now),
				p.sightings() + 1, now.getEpochSecond(), strong, 0));
		return true;
	}
	/** Witnesses must already have passed seen(); an outcome or reported witness list alone never awards credit. */
	public boolean ended(Request request, UUID encounter) {
		Lease lease = lease(request);
		if (lease == null || encounter == null || !encounter.equals(lease.encounter)) return false;
		finish(lease);
		return true;
	}
	private Lease lease(Request request) {
		Lease lease = leases.get(request.token());
		return lease != null && lease.request.equals(request) ? lease : null;
	}
	private void finish(Lease lease) {
		leases.remove(lease.request.token());
		reserved.remove(lease.request.player(), lease.request.token());
		if (!lease.witnesses.contains(lease.request.player())) {
			Progress p = progress.get(lease.request.player());
			put(new Progress(p.player(), p.activeTicks(), p.dueAt(), p.interval(), p.eventMode(), p.sightings(),
					p.lastSeenEpochSecond(), p.strongDueAt(), p.activeTicks() + RETRY_TICKS));
		}
	}
	private void put(Progress p) { progress.put(p.player(), p); revision++; }
	private int interval(boolean first, boolean event) {
		int min = event ? (first ? 5 : 10) : (first ? 45 : 60);
		int max = event ? (first ? 10 : 20) : (first ? 90 : 180);
		return random.nextInt(min * MINUTE, max * MINUTE + 1);
	}
	private static int strongInterval(boolean event) { return (event ? 45 : 180) * MINUTE; }
}
