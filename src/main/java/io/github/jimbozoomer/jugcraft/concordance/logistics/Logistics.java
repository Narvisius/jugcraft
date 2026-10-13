package io.github.jimbozoomer.jugcraft.concordance.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The logistics ledger (roadmap step 18): every open request, its claim, its reservation and the items it has in
 * transit, in one place. Couriers ask it for work and report each step; the caller moves the real items in the same
 * tick as the step it reports, so an item is always either in a container or counted here as carried, never both and
 * never neither. Pure Java: the server keeps one ledger for the world (with the item stacks the counts describe), and
 * the step 18 harness drives the same class with counted containers.
 * <p>
 * The rules that keep items from being duplicated or lost:
 * <ul>
 * <li>One courier a request: a claim is exclusive and lapses unless renewed ({@value #LEASE_TICKS} ticks), so a removed
 * or unloaded courier releases it by itself.</li>
 * <li>Reservations: a courier reserves no more at a source than the source holds less what other claims have
 * reserved there, and picks up no more than it reserved (re-reading the container when it gets there).</li>
 * <li>Cargo in transit belongs to the request, not the courier: a courier released while carrying leaves it here,
 * stranded, for another courier to take up at the post or for the requester to recover.</li>
 * <li>Nothing is closed with cargo still carried: a cancelled request, or one whose destination is gone, first takes
 * its cargo back.</li>
 * </ul>
 */
public final class Logistics {
	/** The most one request may ask for (four stacks of 64). */
	public static final int MAX_WANTED = 256;
	/** The most open requests one player may have. */
	public static final int MAX_OPEN_PER_PLAYER = 16;
	/** The most open requests in a world. */
	public static final int MAX_OPEN = 1024;
	/** How long a claim lasts unless its courier renews it (game ticks). */
	public static final long LEASE_TICKS = 200L;
	/** How long a request released for want of a source waits before a courier tries it again (game ticks). */
	public static final long RETRY_TICKS = 200L;
	/** How many events each post's history keeps. */
	public static final int HISTORY = 32;

	public enum Outcome {
		DONE, NOT_FOUND, NOT_YOURS, CLAIMED, WRONG_STATE, BAD_COUNT, TOO_MANY, NOTHING_FREE
	}

	public record Filed(Outcome outcome, long id) {
	}

	public record Recovered(Outcome outcome, int amount) {
	}

	private final Map<Long, Request> requests = new LinkedHashMap<>();
	private final Map<Place, Deque<Event>> history = new LinkedHashMap<>();
	private long nextId;

	public Logistics() {
		this(1L, List.of(), Map.of());
	}

	/** A ledger as it was saved: its next id, its open requests (oldest first) and each post's history. */
	public Logistics(long nextId, Collection<Request> open, Map<Place, List<Event>> saved) {
		this.nextId = Math.max(1L, nextId);
		for (Request request : open) {
			requests.put(request.id(), request);
			this.nextId = Math.max(this.nextId, request.id() + 1);
		}
		saved.forEach((post, events) -> {
			Deque<Event> kept = new ArrayDeque<>();
			for (Event event : events) {
				kept.addLast(event);
				if (kept.size() > HISTORY) {
					kept.removeFirst();
				}
			}
			history.put(post, kept);
		});
	}

	public long nextId() {
		return nextId;
	}

	/** Every open request, oldest first. */
	public List<Request> requests() {
		return List.copyOf(requests.values());
	}

	public @Nullable Request get(long id) {
		return requests.get(id);
	}

	/** A post's history, oldest first. */
	public List<Event> history(Place post) {
		Deque<Event> events = history.get(post);
		return events == null ? List.of() : List.copyOf(events);
	}

	public Map<Place, List<Event>> histories() {
		Map<Place, List<Event>> copy = new LinkedHashMap<>();
		history.forEach((post, events) -> copy.put(post, List.copyOf(events)));
		return Collections.unmodifiableMap(copy);
	}

	public int openFor(UUID requester) {
		int count = 0;
		for (Request request : requests.values()) {
			if (request.ticket().requester().equals(requester)) {
				count++;
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- filing and claiming

	public Filed file(UUID requester, Place post, Place destination, String item, int wanted, long now) {
		if (wanted < 1 || wanted > MAX_WANTED) {
			return new Filed(Outcome.BAD_COUNT, 0L);
		}
		if (openFor(requester) >= MAX_OPEN_PER_PLAYER || requests.size() >= MAX_OPEN) {
			return new Filed(Outcome.TOO_MANY, 0L);
		}
		long id = nextId++;
		requests.put(id, new Request(new Ticket(id, requester, post, destination, item, wanted, now), Progress.NEW));
		log(post, now, id, "filed", wanted, item);
		return new Filed(Outcome.DONE, id);
	}

	/**
	 * The next request a courier of {@code post} may claim: cargo waiting at the post first (stranded, or to be taken
	 * back), then the oldest open request not resting after a failed try.
	 */
	public @Nullable Request next(Place post, long now) {
		lapse(now);
		Request oldest = null;
		for (Request request : requests.values()) {
			Progress progress = request.progress();
			if (!request.ticket().post().equals(post) || progress.worker() != null) {
				continue;
			}
			if (progress.carried() > 0) {
				return request;
			}
			if (oldest == null && progress.notBefore() <= now) {
				oldest = request;
			}
		}
		return oldest;
	}

	/** Claims a request for {@code worker}; only one courier at a time ever holds a request. */
	public Outcome claim(long id, UUID worker, long now) {
		lapse(now);
		Request request = requests.get(id);
		if (request == null) {
			return Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		if (p.worker() != null) {
			return p.worker().equals(worker) ? renew(id, worker, now) : Outcome.CLAIMED;
		}
		if (p.carried() == 0 && p.notBefore() > now) {
			return Outcome.WRONG_STATE;
		}
		put(request.with(new Progress(p.delivered(), p.carried(), 0, p.source(), worker, now + LEASE_TICKS, false, p.returning(),
				p.note(), p.notBefore())));
		log(request.ticket().post(), now, id, "claimed", 0, "");
		return Outcome.DONE;
	}

	/** The courier is still at it: its claim lasts another lease. */
	public Outcome renew(long id, UUID worker, long now) {
		Request request = mine(id, worker, now);
		if (request == null) {
			return requests.containsKey(id) ? Outcome.NOT_YOURS : Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		put(request.with(new Progress(p.delivered(), p.carried(), p.reserved(), p.source(), worker, now + LEASE_TICKS, p.aboard(),
				p.returning(), p.note(), p.notBefore())));
		return Outcome.DONE;
	}

	/** How many of {@code item} at {@code source} live claims other than {@code except} have reserved. */
	public int reservedAt(Place source, String item, long except) {
		int reserved = 0;
		for (Request request : requests.values()) {
			Progress p = request.progress();
			if (request.id() != except && p.reserved() > 0 && p.worker() != null && source.equals(p.source())
					&& item.equals(request.ticket().item())) {
				reserved += p.reserved();
			}
		}
		return reserved;
	}

	/**
	 * The claiming courier reserves what it will take from {@code source}, which holds {@code available} of the item:
	 * no more than the request still needs, and no more than other claims have left there.
	 */
	public Outcome reserve(long id, UUID worker, Place source, int available, long now) {
		Request request = mine(id, worker, now);
		if (request == null) {
			return requests.containsKey(id) ? Outcome.NOT_YOURS : Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		if (p.carried() > 0 || p.returning()) {
			return Outcome.WRONG_STATE;
		}
		int amount = Math.min(request.remaining(), available - reservedAt(source, request.ticket().item(), id));
		if (amount <= 0) {
			return Outcome.NOTHING_FREE;
		}
		put(request.with(new Progress(p.delivered(), 0, amount, source, worker, p.lease(), false, false, "", p.notBefore())));
		log(request.ticket().post(), now, id, "reserved", amount, source.text());
		return Outcome.DONE;
	}

	// ---------------------------------------------------------------- moving items (the caller moves the stacks)

	/**
	 * The courier took {@code taken} from its source (at most what it reserved; it re-read the container): they are now
	 * carried. None taken (the source was emptied meanwhile) clears the reservation.
	 */
	public Outcome pickUp(long id, UUID worker, int taken, long now) {
		Request request = mine(id, worker, now);
		if (request == null) {
			return requests.containsKey(id) ? Outcome.NOT_YOURS : Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		if (p.reserved() == 0 || p.carried() > 0 || p.returning()) {
			return Outcome.WRONG_STATE;
		}
		if (taken < 0 || taken > p.reserved()) {
			return Outcome.BAD_COUNT;
		}
		if (taken == 0) {
			put(request.with(new Progress(p.delivered(), 0, 0, null, worker, p.lease(), false, false, "source_empty", p.notBefore())));
			log(request.ticket().post(), now, id, "source_empty", 0, p.source() == null ? "" : p.source().text());
			return Outcome.NOTHING_FREE;
		}
		put(request.with(new Progress(p.delivered(), taken, 0, p.source(), worker, p.lease(), true, false, "", p.notBefore())));
		log(request.ticket().post(), now, id, "picked_up", taken, p.source() == null ? "" : p.source().text());
		return Outcome.DONE;
	}

	/** The claiming courier, at the post, takes up stranded cargo. */
	public Outcome takeUp(long id, UUID worker, long now) {
		Request request = mine(id, worker, now);
		if (request == null) {
			return requests.containsKey(id) ? Outcome.NOT_YOURS : Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		if (p.carried() == 0 || p.aboard()) {
			return Outcome.WRONG_STATE;
		}
		put(request.with(new Progress(p.delivered(), p.carried(), 0, p.source(), worker, p.lease(), true, p.returning(), p.note(),
				p.notBefore())));
		log(request.ticket().post(), now, id, "taken_up", p.carried(), "");
		return Outcome.DONE;
	}

	/**
	 * The courier put {@code put} of its cargo into the destination (what fitted). The whole request delivered closes
	 * it; the cargo spent with more still wanted returns it to open for the rest.
	 */
	public Outcome deliver(long id, UUID worker, int put, long now) {
		Request request = mine(id, worker, now);
		if (request == null) {
			return requests.containsKey(id) ? Outcome.NOT_YOURS : Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		if (!p.aboard() || p.carried() == 0 || p.returning()) {
			return Outcome.WRONG_STATE;
		}
		if (put < 0 || put > p.carried()) {
			return Outcome.BAD_COUNT;
		}
		if (put == 0) {
			return Outcome.NOTHING_FREE;
		}
		int delivered = p.delivered() + put;
		int carried = p.carried() - put;
		Place post = request.ticket().post();
		if (delivered == request.ticket().wanted()) {
			close(request);
			log(post, now, id, "done", put, "");
		} else if (carried == 0) {
			put(request.with(new Progress(delivered, 0, 0, null, null, 0L, false, false, "", 0L)));
			log(post, now, id, "delivered", put, "");
		} else {
			put(request.with(new Progress(delivered, carried, 0, p.source(), worker, p.lease(), true, false, "full", p.notBefore())));
			log(post, now, id, "delivered", put, "full");
		}
		return Outcome.DONE;
	}

	/** The courier put {@code put} of the cargo it is taking back where it belongs; all of it back closes the request. */
	public Outcome returned(long id, UUID worker, int put, long now) {
		Request request = mine(id, worker, now);
		if (request == null) {
			return requests.containsKey(id) ? Outcome.NOT_YOURS : Outcome.NOT_FOUND;
		}
		Progress p = request.progress();
		if (!p.returning() || !p.aboard() || p.carried() == 0) {
			return Outcome.WRONG_STATE;
		}
		if (put < 0 || put > p.carried()) {
			return Outcome.BAD_COUNT;
		}
		if (put == 0) {
			return Outcome.NOTHING_FREE;
		}
		int carried = p.carried() - put;
		log(request.ticket().post(), now, id, "returned", put, "");
		if (carried == 0) {
			close(request);
			log(request.ticket().post(), now, id, "cancelled", 0, p.note());
		} else {
			put(request.with(new Progress(p.delivered(), carried, 0, p.source(), worker, p.lease(), true, true, p.note(), p.notBefore())));
		}
		return Outcome.DONE;
	}

	// ---------------------------------------------------------------- giving up, cancelling, recovering

	/**
	 * The courier gives up its claim (removed, unable to reach, nothing to take, or its body spent): any reservation
	 * lapses and any cargo stays here, stranded. {@code retry} rests the request before another try (no source had it).
	 */
	public Outcome release(long id, UUID worker, String why, boolean retry, long now) {
		Request request = requests.get(id);
		if (request == null) {
			return Outcome.NOT_FOUND;
		}
		if (!worker.equals(request.progress().worker())) {
			return Outcome.NOT_YOURS;
		}
		drop(request, why, retry ? now + RETRY_TICKS : 0L, now);
		return Outcome.DONE;
	}

	/** Claims whose courier stopped renewing them lapse (it was removed, unloaded or the world restarted without it). */
	public void lapse(long now) {
		for (Request request : List.copyOf(requests.values())) {
			Progress p = request.progress();
			if (p.worker() != null && p.lease() < now) {
				drop(request, "lapsed", 0L, now);
			}
		}
	}

	/**
	 * The requester cancels: with nothing carried the request closes at once (its reservation released); with cargo
	 * in transit it is taken back first.
	 */
	public Outcome cancel(long id, UUID by, long now) {
		Request request = requests.get(id);
		if (request == null) {
			return Outcome.NOT_FOUND;
		}
		if (!request.ticket().requester().equals(by)) {
			return Outcome.NOT_YOURS;
		}
		return giveBack(request, "cancelled", now);
	}

	/** A destination is gone (broken): every request bound for it is cancelled, taking its cargo back first. */
	public void destinationGone(Place destination, long now) {
		for (Request request : List.copyOf(requests.values())) {
			if (request.ticket().destination().equals(destination)) {
				giveBack(request, "destination_gone", now);
			}
		}
	}

	/**
	 * The requester takes stranded cargo themselves (no courier has it aboard): the request closes and the caller hands
	 * over exactly {@link Recovered#amount} in the same tick.
	 */
	public Recovered recover(long id, UUID by, long now) {
		Request request = requests.get(id);
		if (request == null) {
			return new Recovered(Outcome.NOT_FOUND, 0);
		}
		if (!request.ticket().requester().equals(by)) {
			return new Recovered(Outcome.NOT_YOURS, 0);
		}
		Progress p = request.progress();
		if (p.carried() == 0 || p.aboard()) {
			return new Recovered(Outcome.WRONG_STATE, 0);
		}
		close(request);
		log(request.ticket().post(), now, id, "recovered", p.carried(), "");
		return new Recovered(Outcome.DONE, p.carried());
	}

	// ---------------------------------------------------------------- internals

	private @Nullable Request mine(long id, UUID worker, long now) {
		lapse(now);
		Request request = requests.get(id);
		return request != null && request.claimedBy(worker, now) ? request : null;
	}

	private Outcome giveBack(Request request, String why, long now) {
		Progress p = request.progress();
		if (p.carried() == 0) {
			close(request);
			log(request.ticket().post(), now, request.id(), "cancelled", 0, why);
		} else if (!p.returning()) {
			put(request.with(new Progress(p.delivered(), p.carried(), 0, p.source(), p.worker(), p.lease(), p.aboard(), true, why,
					p.notBefore())));
			log(request.ticket().post(), now, request.id(), "returning", p.carried(), why);
		}
		return Outcome.DONE;
	}

	private void drop(Request request, String why, long notBefore, long now) {
		Progress p = request.progress();
		put(request.with(new Progress(p.delivered(), p.carried(), 0, p.carried() > 0 ? p.source() : null, null, 0L, false, p.returning(),
				why, notBefore)));
		log(request.ticket().post(), now, request.id(), p.carried() > 0 ? "stranded" : "released", p.carried(), why);
	}

	private void close(Request request) {
		requests.remove(request.id());
	}

	private void put(Request request) {
		requests.put(request.id(), request);
	}

	private void log(Place post, long now, long id, String kind, int amount, String note) {
		Deque<Event> events = history.computeIfAbsent(post, unused -> new ArrayDeque<>());
		events.addLast(new Event(now, id, kind, amount, note));
		while (events.size() > HISTORY) {
			events.removeFirst();
		}
	}

	/** Every item the ledger holds in transit, by request (for audits: each must equal the stacks kept with it). */
	public Map<Long, Integer> carried() {
		Map<Long, Integer> carried = new LinkedHashMap<>();
		for (Request request : requests.values()) {
			if (request.progress().carried() > 0) {
				carried.put(request.id(), request.progress().carried());
			}
		}
		return carried;
	}

	/** Problems with the ledger's own invariants (none expected; the harness and the server's audit check). */
	public List<String> problems() {
		List<String> problems = new ArrayList<>();
		for (Request request : requests.values()) {
			Progress p = request.progress();
			int wanted = request.ticket().wanted();
			if (p.delivered() < 0 || p.carried() < 0 || p.reserved() < 0 || p.delivered() + p.carried() > wanted) {
				problems.add("request " + request.id() + ": counts out of bounds " + p);
			}
			if (p.reserved() > 0 && (p.carried() > 0 || p.worker() == null || p.source() == null)) {
				problems.add("request " + request.id() + ": a reservation without a live claim at a source");
			}
			if (p.aboard() && (p.carried() == 0 || p.worker() == null)) {
				problems.add("request " + request.id() + ": aboard without cargo or a courier");
			}
		}
		return problems;
	}
}
