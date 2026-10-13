package io.github.jimbozoomer.jugcraft.concordance.rules;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * One way to reach a research state: a kind of evidence and how much of it. A state lists several alternatives and
 * any one of them is enough (a field observation or a laboratory study, for example).
 * <p>
 * Evidence is kept per entry as keys, and each key counts once however often it is repeated:
 * <ul>
 * <li>{@code examine:<item>} holds the lowest light the specimen was examined at;</li>
 * <li>{@code study:<station>:<item>} records a finished study;</li>
 * <li>{@code invoke:<invocation>:<chunk>} records an invocation that took effect in a chunk;</li>
 * <li>{@code notes:<author>} records another player's notes read (one key per author, whatever they wrote);</li>
 * <li>{@code practice:<activity>:<key>} records a practice completed (a ritual in a chunk, a distinct mixture).</li>
 * </ul>
 * Notes may stand for states up to understood only ({@link RulesParser} refuses them for mastered): reading can teach
 * what to do, but a player masters an entry only by their own practice.
 */
public record EvidenceRule(Kind kind, Specimens specimens, @Nullable Integer maxLight, int distinct, @Nullable String station,
		@Nullable String invocation, @Nullable String activity) {
	public enum Kind {
		EXAMINE("examine"),
		STUDY("study"),
		INVOKE("invoke"),
		NOTES("notes"),
		PRACTICE("practice");

		public final String id;

		Kind(String id) {
			this.id = id;
		}
	}

	/** Which items count: listed ids and item tags ({@code #ns:path}). */
	public record Specimens(List<String> items, List<String> tags) {
		public static final Specimens NONE = new Specimens(List.of(), List.of());

		public boolean matches(String item, TagLookup tags) {
			if (items.contains(item)) {
				return true;
			}
			for (String tag : this.tags) {
				if (tags.isIn(item, tag)) {
					return true;
				}
			}
			return false;
		}
	}

	/** Answers item tag membership from the running game (the rules themselves never load registries). */
	@FunctionalInterface
	public interface TagLookup {
		boolean isIn(String item, String tag);
	}

	/**
	 * The key this evidence adds under this rule and the value to keep (the lowest), or null if it does not apply.
	 * {@code research} is the entry the rule belongs to and {@code ruleState} the state it is a way into: notes count
	 * only for the entry they were written about, and only for states their author had reached.
	 */
	public Map.@Nullable Entry<String, Long> keyFor(Evidence evidence, TagLookup tags, String research, ResearchState ruleState) {
		return switch (evidence) {
			case Evidence.Examined examined when kind == Kind.EXAMINE && specimens.matches(examined.item(), tags) ->
					Map.entry("examine:" + examined.item(), (long) examined.light());
			case Evidence.Studied studied when kind == Kind.STUDY && studied.station().equals(station)
					&& specimens.matches(studied.item(), tags) -> Map.entry("study:" + studied.station() + ":" + studied.item(), 0L);
			case Evidence.Invoked invoked when kind == Kind.INVOKE && invoked.invocation().equals(invocation) ->
					Map.entry("invoke:" + invoked.invocation() + ":" + invoked.chunk(), 0L);
			case Evidence.ReadNotes notes when kind == Kind.NOTES && notes.research().equals(research)
					&& notes.state().atLeast(ruleState) && ruleState != ResearchState.MASTERED ->
					Map.entry("notes:" + notes.author(), 0L);
			case Evidence.Practiced practiced when kind == Kind.PRACTICE && practiced.activity().equals(activity) ->
					Map.entry("practice:" + practiced.activity() + ":" + practiced.key(), 0L);
			default -> null;
		};
	}

	/** How much of the needed evidence {@code evidence} holds for this rule (never more than {@link #distinct}). */
	public int progress(Map<String, Long> evidence, TagLookup tags) {
		int count = 0;
		for (Map.Entry<String, Long> entry : evidence.entrySet()) {
			if (counts(entry.getKey(), entry.getValue(), tags)) {
				count++;
				if (count >= distinct) {
					break;
				}
			}
		}
		return count;
	}

	public boolean satisfied(Map<String, Long> evidence, TagLookup tags) {
		return progress(evidence, tags) >= distinct;
	}

	private boolean counts(String key, long value, TagLookup tags) {
		return switch (kind) {
			case EXAMINE -> key.startsWith("examine:") && specimens.matches(key.substring("examine:".length()), tags)
					&& (maxLight == null || value <= maxLight);
			case STUDY -> {
				String prefix = "study:" + station + ":";
				yield key.startsWith(prefix) && specimens.matches(key.substring(prefix.length()), tags);
			}
			case INVOKE -> key.startsWith("invoke:" + invocation + ":");
			case NOTES -> key.startsWith("notes:");
			case PRACTICE -> key.startsWith("practice:" + activity + ":");
		};
	}
}
