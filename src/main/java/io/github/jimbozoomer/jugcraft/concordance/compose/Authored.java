package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * An authored invocation compiled for one instrument (roadmap step 10): its plan, and the plan of each of its tunings
 * that fits the same instrument. Built once per data load by {@code ConcordanceRules}, so a cast only looks its plan
 * up; it never compiles anything.
 *
 * @param tunings plans by modifier id, in the invocation's order
 */
public record Authored(String instrument, Plan plan, Map<String, Plan> tunings) {
	public Authored {
		tunings = Collections.unmodifiableMap(new LinkedHashMap<>(tunings));
	}

	/** The plan untuned ({@code tuning} null) or with that tuning, or null if the tuning does not fit here. */
	public @Nullable Plan plan(@Nullable String tuning) {
		return tuning == null ? plan : tunings.get(tuning);
	}

	/** The Focus a tuning adds to a cast: what its modifier adds to the composition. -1 if it does not fit here. */
	public int tuningFocus(String tuning) {
		Plan tuned = tunings.get(tuning);
		return tuned == null ? -1 : tuned.focus() - plan.focus();
	}
}
