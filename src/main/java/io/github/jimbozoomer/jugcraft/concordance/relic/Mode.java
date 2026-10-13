package io.github.jimbozoomer.jugcraft.concordance.relic;

import java.util.List;
import java.util.Set;

/**
 * One way a relic works (roadmap step 20): the contexts it works in, whom it affects within what range, the status it
 * gives through the shared effect boundary (and for how long), how often it pulses, what each pulse costs in the relic's
 * charge, and the conditions it needs.
 */
public record Mode(String id, Set<Context> contexts, Target target, String status, int amplifier, int duration, int range, int interval,
		int cost, Conditions conditions) {
	public enum Target {
		/** The one carrying or wearing it. */
		SELF("self"),
		/** Players of the holder's party within range (for an installed relic, its shrine owner's party). */
		ALLIES("allies"),
		/** Hostile creatures within range. */
		HOSTILES("hostiles");

		public final String id;

		Target(String id) {
			this.id = id;
		}
	}

	/**
	 * What a pulse needs: the open sky above, night, no harm taken lately, someone hurt to mend (the holder for a self
	 * mode; for the others, only hurt creatures are its targets) and one of these dimensions (empty: any).
	 */
	public record Conditions(boolean sky, boolean night, boolean calm, boolean wounded, List<String> dimensions) {
		public static final Conditions NONE = new Conditions(false, false, false, false, List.of());
	}
}
