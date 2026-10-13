package io.github.jimbozoomer.jugcraft.concordance.resource;

/**
 * How much may move through a container per window of game ticks: {@code limit} per {@code window} ticks, counted
 * from the start of the current window. Immutable; {@link #spend} returns the budget after a transfer.
 */
public record RateBudget(long limit, int window, long windowStart, long used) {
	public RateBudget {
		if (limit < 0 || window < 1 || used < 0) {
			throw new IllegalArgumentException("bad rate budget");
		}
	}

	public static RateBudget of(long limit, int window) {
		return new RateBudget(limit, window, Long.MIN_VALUE, 0L);
	}

	private boolean current(long now) {
		return windowStart != Long.MIN_VALUE && now >= windowStart && now - windowStart < window;
	}

	/** What may still move at game time {@code now}. */
	public long allowance(long now) {
		return current(now) ? Math.max(0L, limit - used) : limit;
	}

	public RateBudget spend(long now, long amount) {
		if (amount < 0 || amount > allowance(now)) {
			throw new IllegalArgumentException("over the rate budget");
		}
		return current(now) ? new RateBudget(limit, window, windowStart, used + amount)
				: new RateBudget(limit, window, now - Math.floorMod(now, (long) window), amount);
	}
}
