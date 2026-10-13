package io.github.jimbozoomer.jugcraft.concordance.rules;

import io.github.jimbozoomer.jugcraft.concordance.ritual.RitualDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/**
 * Applies evidence to a player's {@link Knowledge} under a set of {@link ConcordanceRules}. Pure and deterministic: the
 * same knowledge, rules and evidence always give the same result, and the server is the only caller.
 * <ul>
 * <li>Evidence is recorded only for entries whose prerequisites are met and which have a state left to reach, and only
 * where it matches one of the rules for such a state.</li>
 * <li>Each evidence key counts once. Repeating an observation adds nothing (an examination in less light than before
 * improves the stored light level, which is the only way a repeat changes anything).</li>
 * <li>States are reached in order; one piece of evidence can carry an entry through several states at once (examining a
 * specimen in darkness first time both encounters and observes it).</li>
 * <li>Evidence for a later state is kept even when an earlier one is missing, and counts once the earlier states are
 * reached: notes read before observing an entry complete it as soon as the reader observes it themselves.</li>
 * </ul>
 */
public final class ResearchEngine {
	private ResearchEngine() {
	}

	public record Transition(String research, ResearchState from, ResearchState to) {
	}

	/** {@code recorded} is false when the evidence taught nothing new (a duplicate, or nothing it applies to). */
	public record Result(Knowledge knowledge, List<Transition> transitions, boolean recorded) {
	}

	public static Result apply(Knowledge knowledge, ConcordanceRules rules, Evidence evidence, EvidenceRule.TagLookup tags) {
		Knowledge current = knowledge;
		List<Transition> transitions = new ArrayList<>();
		boolean recorded = false;
		for (Definitions.Research entry : rules.research().values()) {
			if (!prerequisitesMet(current, entry)) {
				continue;
			}
			Knowledge.Progress progress = current.progress(entry.id());
			if (!progress.state().atLeast(entry.highest()) || progress.state() == ResearchState.NONE) {
				Map<String, Long> evidenceKeys = new TreeMap<>(progress.evidence());
				boolean added = false;
				for (Map.Entry<ResearchState, List<EvidenceRule>> state : entry.states().entrySet()) {
					if (progress.state().atLeast(state.getKey())) {
						continue;
					}
					for (EvidenceRule rule : state.getValue()) {
						Map.Entry<String, Long> key = rule.keyFor(evidence, tags, entry.id(), state.getKey());
						if (key == null) {
							continue;
						}
						Long old = evidenceKeys.get(key.getKey());
						if (old == null && evidenceKeys.size() >= Knowledge.MAX_EVIDENCE) {
							continue;
						}
						if (old == null || key.getValue() < old) {
							evidenceKeys.put(key.getKey(), key.getValue());
							added = true;
						}
					}
				}
				if (!added) {
					continue;
				}
				recorded = true;
				ResearchState reached = advance(entry, progress.state(), evidenceKeys, tags, transitions);
				current = current.withProgress(entry.id(), new Knowledge.Progress(reached, evidenceKeys));
			}
		}
		if (!transitions.isEmpty()) {
			current = relearn(current, rules);
		}
		return new Result(current, List.copyOf(transitions), recorded);
	}

	/** Moves an entry on through every state whose evidence is complete, recording each step. */
	private static ResearchState advance(Definitions.Research entry, ResearchState from, Map<String, Long> evidence,
			EvidenceRule.TagLookup tags, List<Transition> transitions) {
		ResearchState state = from;
		ResearchState next = state.next();
		while (next != null && entry.states().containsKey(next) && anySatisfied(entry.states().get(next), evidence, tags)) {
			transitions.add(new Transition(entry.id(), state, next));
			state = next;
			next = state.next();
		}
		return state;
	}

	private static boolean anySatisfied(List<EvidenceRule> rules, Map<String, Long> evidence, EvidenceRule.TagLookup tags) {
		for (EvidenceRule rule : rules) {
			if (rule.satisfied(evidence, tags)) {
				return true;
			}
		}
		return false;
	}

	public static boolean prerequisitesMet(Knowledge knowledge, Definitions.Research entry) {
		for (Definitions.Requirement requirement : entry.requires()) {
			if (!knowledge.state(requirement.research()).atLeast(requirement.state())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Recomputes what a player may cast from their research: every invocation whose research has reached its state,
	 * keyed by the Spell Engine spell it casts (the client checks casts by spell, without the rules), at the cost for that
	 * player's state. Called after research changes and after the rules reload.
	 */
	public static Knowledge relearn(Knowledge knowledge, ConcordanceRules rules) {
		Map<String, Integer> invocations = new LinkedHashMap<>();
		for (Definitions.Invocation invocation : rules.invocations().values()) {
			ResearchState reached = knowledge.state(invocation.research());
			if (reached.atLeast(invocation.state())) {
				invocations.put(invocation.spell(), invocation.cost(reached));
			}
		}
		return invocations.equals(knowledge.invocations()) ? knowledge : knowledge.withInvocations(invocations);
	}

	/**
	 * Sets an entry's state directly (operators and tests), keeping its evidence. Moving it back also takes away what
	 * the higher states taught on the next {@link #relearn}.
	 */
	public static Knowledge grant(Knowledge knowledge, ConcordanceRules rules, String research, ResearchState state) {
		Knowledge.Progress progress = knowledge.progress(research);
		return relearn(knowledge.withProgress(research, new Knowledge.Progress(state, progress.evidence())), rules);
	}

	/** Whether a player's research lets them take part in a ritual. */
	public static boolean knowsRitual(Knowledge knowledge, RitualDefinition ritual) {
		return knowledge.state(ritual.research()).atLeast(ritual.state());
	}

	/** Whether a player's research lets them run a working. */
	public static boolean knowsWorking(Knowledge knowledge, Definitions.Working working) {
		return knowledge.state(working.research()).atLeast(working.state());
	}

	/** Why an entry stands where it does, for status commands and diagnostics. */
	public sealed interface Explanation {
		/** The rules do not define it (a removed data pack, or a typo). */
		record Unknown(String research) implements Explanation {
		}

		/** A prerequisite has not reached its state yet. */
		record NeedsPrerequisite(Definitions.Requirement requirement, ResearchState has) implements Explanation {
		}

		/** Complete: every state it defines is reached. */
		record Complete(ResearchState state) implements Explanation {
		}

		/** The next state and how far each of its alternatives has come. */
		record Next(ResearchState state, List<RuleProgress> alternatives) implements Explanation {
		}
	}

	public record RuleProgress(EvidenceRule rule, int have, int need) {
	}

	public static List<Explanation> explain(Knowledge knowledge, ConcordanceRules rules, String research,
			EvidenceRule.TagLookup tags) {
		Definitions.Research entry = rules.research(research);
		if (entry == null) {
			return List.of(new Explanation.Unknown(research));
		}
		List<Explanation> out = new ArrayList<>();
		for (Definitions.Requirement requirement : entry.requires()) {
			ResearchState has = knowledge.state(requirement.research());
			if (!has.atLeast(requirement.state())) {
				out.add(new Explanation.NeedsPrerequisite(requirement, has));
			}
		}
		if (!out.isEmpty()) {
			return out;
		}
		Knowledge.Progress progress = knowledge.progress(research);
		@Nullable ResearchState next = progress.state().next();
		if (next == null || !entry.states().containsKey(next)) {
			return List.of(new Explanation.Complete(progress.state()));
		}
		List<RuleProgress> alternatives = new ArrayList<>();
		for (EvidenceRule rule : entry.states().get(next)) {
			alternatives.add(new RuleProgress(rule, rule.progress(progress.evidence(), tags), rule.distinct()));
		}
		return List.of(new Explanation.Next(next, alternatives));
	}
}
