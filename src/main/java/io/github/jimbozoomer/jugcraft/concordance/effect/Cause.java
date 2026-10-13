package io.github.jimbozoomer.jugcraft.concordance.effect;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Where an effect came from: the acting creature or player (if any), what kind of source carried it, which one, and the
 * event it belongs to. Every effect carries one, and an effect triggered by another ({@link #triggered}) keeps the same
 * actor, source and event, one level deeper, so a kill made by a spell's second branch is still credited to its caster
 * and still counted against that cast's limits.
 *
 * @param actor the player or creature responsible, or null for a sourceless one (a shrine with no owner)
 * @param origin what kind of thing delivered it
 * @param source the id of that thing (a spell, item, ritual or creature type)
 * @param serial the event: one cast, one use, one ritual run; effects with the same serial share one {@link Ledger}
 * @param depth 0 for the effect itself, one more for each trigger in between
 */
public record Cause(@Nullable UUID actor, Origin origin, String source, long serial, int depth) {
	/** How deep triggered effects may go: an effect at this depth triggers nothing further. */
	public static final int MAX_DEPTH = 3;

	public enum Origin {
		SPELL("spell"),
		INVOCATION("invocation"),
		ITEM("item"),
		RITUAL("ritual"),
		POTION("potion"),
		WEAPON("weapon"),
		CREATURE("creature"),
		SHRINE("shrine");

		public final String id;

		Origin(String id) {
			this.id = id;
		}
	}

	public Cause {
		if (depth < 0 || depth > MAX_DEPTH) {
			throw new IllegalArgumentException("cause depth " + depth + " outside 0.." + MAX_DEPTH);
		}
	}

	public static Cause of(@Nullable UUID actor, Origin origin, String source, long serial) {
		return new Cause(actor, origin, source, serial, 0);
	}

	/** The cause of an effect this one triggers, or null if triggering would go deeper than {@link #MAX_DEPTH}. */
	public @Nullable Cause triggered() {
		return depth >= MAX_DEPTH ? null : new Cause(actor, origin, source, serial, depth + 1);
	}
}
