package io.github.jimbozoomer.jugcraft.concordance.rules;

/**
 * Something a player did that research can learn from. The server creates these from what it saw happen (an
 * examination, a finished study, a successful cast); a client can never submit one.
 */
public sealed interface Evidence {
	/** A luminous specimen examined by hand, at the light level where the player stood (0..15). */
	record Examined(String item, int light) implements Evidence {
	}

	/** A specimen read to the end at a study station. */
	record Studied(String item, String station) implements Evidence {
	}

	/** An invocation that took effect in a chunk ({@code chunk} is the packed chunk position). */
	record Invoked(String invocation, long chunk) implements Evidence {
	}

	/**
	 * Another player's written notes on one research entry, read: who wrote them ({@code author}, a player UUID) and
	 * the state they had reached when writing ({@code state}, never beyond understood). Notes are a shared record: they
	 * can count towards the states an entry allows them for, but they never stand in for a reader's own observation
	 * or practice.
	 */
	record ReadNotes(String research, String author, ResearchState state) implements Evidence {
	}

	/**
	 * A practice carried through to its end: a ritual completed ({@code activity} {@code jugcraft:ritual}, {@code key}
	 * the packed chunk it stood in) or a mixture bottled ({@code jugcraft:alchemy}, {@code key} the effects it carried).
	 * Distinct keys count once each, so mastery comes from varied practice, not repetition.
	 */
	record Practiced(String activity, String key) implements Evidence {
	}
}
