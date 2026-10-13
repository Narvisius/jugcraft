package io.github.jimbozoomer.jugcraft.concordance.ecology;

/**
 * How many area samples ({@link Sampler}) one level may take in one game tick: {@value #PER_TICK}. A bed refused a
 * sample keeps its last one and asks again later, so a crowded garden grows on slightly older readings rather than
 * costing more time. One per level; it is not saved (a fresh tick starts a fresh count).
 */
public final class SampleBudget {
	public static final int PER_TICK = 16;

	private long tick = Long.MIN_VALUE;
	private int used;

	/** Takes one sample's allowance at game time {@code now}; false when this tick's are spent. */
	public boolean take(long now) {
		if (now != tick) {
			tick = now;
			used = 0;
		}
		if (used >= PER_TICK) {
			return false;
		}
		used++;
		return true;
	}

	/** Samples already taken at game time {@code now}. */
	public int used(long now) {
		return now == tick ? used : 0;
	}
}
