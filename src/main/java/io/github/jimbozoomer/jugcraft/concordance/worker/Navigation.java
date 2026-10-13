package io.github.jimbozoomer.jugcraft.concordance.worker;

/**
 * How a worker's paths are going (roadmap step 17): consecutive failures to find or follow a path. After
 * {@value #GIVE_UP} in a row it stops trying and says it cannot navigate, until {@value #RETRY_TICKS} game ticks have
 * passed or its goal changes, so a worker that cannot reach its goal never retries every tick.
 */
public record Navigation(int failures, long since) {
	public static final int GIVE_UP = 3;
	public static final int RETRY_TICKS = 600;
	public static final Navigation FINE = new Navigation(0, 0L);

	public Navigation failed(long now) {
		return new Navigation(failures + 1, failures == 0 ? now : since);
	}

	public Navigation succeeded() {
		return FINE;
	}

	/** Whether it has given up at game time {@code now} (it tries again after the retry period). */
	public boolean givenUp(long now) {
		return failures >= GIVE_UP && now - since < RETRY_TICKS;
	}
}
