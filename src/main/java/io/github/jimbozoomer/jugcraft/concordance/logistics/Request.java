package io.github.jimbozoomer.jugcraft.concordance.logistics;

import java.util.UUID;

/** One request: its fixed ticket and its progress (roadmap step 18). */
public record Request(Ticket ticket, Progress progress) {
	public long id() {
		return ticket.id();
	}

	public RequestState state() {
		if (progress.returning()) {
			return RequestState.RETURNING;
		}
		if (progress.carried() > 0) {
			return progress.aboard() ? RequestState.IN_TRANSIT : RequestState.STRANDED;
		}
		return progress.worker() != null ? RequestState.CLAIMED : RequestState.OPEN;
	}

	/** How many are still to be picked up: neither delivered nor in transit. */
	public int remaining() {
		return ticket.wanted() - progress.delivered() - progress.carried();
	}

	/** Whether {@code worker} holds this request's claim at {@code now}. */
	public boolean claimedBy(UUID worker, long now) {
		return worker.equals(progress.worker()) && progress.lease() >= now;
	}

	Request with(Progress next) {
		return new Request(ticket, next);
	}
}
