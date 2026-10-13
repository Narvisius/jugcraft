package io.github.jimbozoomer.jugcraft.concordance.ecology;

import java.util.List;

/**
 * Where a unit of nutrients goes when a source gives one to several beds (a Mulch Maw, a nitrogen fixer): to the bed
 * with the least that still has room, the first in the given order on a tie, so the same garden always shares the
 * same way and nothing is created or lost (a source with nowhere to put a unit keeps it).
 */
public final class Nourishment {
	private Nourishment() {
	}

	/** The index of the poorest store with room under {@code capacity}, or -1 when every one is full. */
	public static int poorest(List<Integer> stores, int capacity) {
		int best = -1;
		for (int i = 0; i < stores.size(); i++) {
			int store = stores.get(i);
			if (store < capacity && (best < 0 || store < stores.get(best))) {
				best = i;
			}
		}
		return best;
	}
}
