package io.github.jimbozoomer.jugcraft.concordance.rules;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * What one player has learned: for each research entry, the state reached and the evidence gathered, plus the
 * invocations that knowledge lets them cast (by Spell Engine spell id) with their current Focus cost. Immutable: every
 * change makes a new one, so a half-applied change can never be saved. The server owns it; the client receives a copy
 * for its codex, spell bar and screens, and never sends one back.
 * <p>
 * Entries for research that a data pack no longer defines are kept as they are, so removing and restoring a data pack
 * does not lose anyone's progress.
 */
public record Knowledge(int schema, Map<String, Progress> entries, Map<String, Integer> invocations) {
	/** The save format version. Version 1 is the first. */
	public static final int SCHEMA = 1;
	/** The most evidence keys kept for one entry: evidence that no longer advances it is not stored. */
	public static final int MAX_EVIDENCE = 128;
	public static final Knowledge EMPTY = new Knowledge(SCHEMA, Map.of(), Map.of());

	public Knowledge {
		entries = Collections.unmodifiableMap(new TreeMap<>(entries));
		invocations = Collections.unmodifiableMap(new TreeMap<>(invocations));
	}

	/** One entry's progress: the state reached and its evidence keys (see {@link EvidenceRule}). */
	public record Progress(ResearchState state, Map<String, Long> evidence) {
		public static final Progress NONE = new Progress(ResearchState.NONE, Map.of());

		public Progress {
			evidence = Collections.unmodifiableMap(new TreeMap<>(evidence));
		}
	}

	public Progress progress(String research) {
		return entries.getOrDefault(research, Progress.NONE);
	}

	public ResearchState state(String research) {
		return progress(research).state();
	}

	/** The Focus this player pays to cast a spell, or -1 if they have not learned the invocation that casts it. */
	public int invocationCost(String spell) {
		Integer cost = invocations.get(spell);
		return cost == null ? -1 : cost;
	}

	Knowledge withProgress(String research, Progress progress) {
		Map<String, Progress> copy = new LinkedHashMap<>(entries);
		copy.put(research, progress);
		return new Knowledge(SCHEMA, copy, invocations);
	}

	Knowledge withInvocations(Map<String, Integer> invocations) {
		return new Knowledge(SCHEMA, entries, invocations);
	}
}
