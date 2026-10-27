package io.github.jimbozoomer.jugcraft.concordance.ritual;

import io.github.jimbozoomer.jugcraft.concordance.rules.EvidenceRule;
import java.util.List;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * Which ritual an anchor's offerings answer for one player (Concordance Chunk C, C-03): the one function a leader's start
 * and the circle's readiness both use, so the rite the Inspect page reads is the rite an empty-hand use begins. Pure.
 *
 * @param chosen  the known ritual fully matched; the most items offered wins, ties to the first (rituals in id order)
 * @param plan    chosen's plan
 * @param unknown failing that, the first fully matched ritual the player has not learned: the start says so as before;
 *                readiness never names it
 * @param closest failing both, the known ritual fewest items short
 * @param missing closest's every shortfall
 */
public record Choice(@Nullable RitualDefinition chosen, Offerings.@Nullable Plan plan, @Nullable RitualDefinition unknown,
		@Nullable RitualDefinition closest, List<Offerings.Shortfall> missing) {
	/** Nothing offered: no ritual is chosen. */
	public static final Choice NONE = new Choice(null, null, null, null, List.of());

	public Choice {
		missing = List.copyOf(missing);
	}

	public static Choice of(List<RitualDefinition> rituals, List<Offerings.Slot> slots, Predicate<RitualDefinition> knows,
			EvidenceRule.TagLookup tags) {
		if (slots.stream().allMatch(slot -> slot.count() == 0)) {
			return NONE;
		}
		RitualDefinition chosen = null;
		Offerings.Plan plan = null;
		RitualDefinition unknown = null;
		RitualDefinition closest = null;
		List<Offerings.Shortfall> missing = List.of();
		int closestShort = Integer.MAX_VALUE;
		for (RitualDefinition ritual : rituals) {
			Offerings.Match match = Offerings.match(ritual, slots, tags);
			if (!knows.test(ritual)) {
				if (match.plan() != null && unknown == null) {
					unknown = ritual;
				}
			} else if (match.plan() != null) {
				// The most specific match wins (the most items offered); ties go to the first by id.
				if (chosen == null || ritual.offered() > chosen.offered()) {
					chosen = ritual;
					plan = match.plan();
				}
			} else {
				int lacking = 0;
				for (Offerings.Shortfall each : match.missing()) {
					lacking += each.offering().count() - each.have();
				}
				if (lacking < closestShort) {
					closestShort = lacking;
					closest = ritual;
					missing = match.missing();
				}
			}
		}
		return new Choice(chosen, plan, unknown, closest, missing);
	}

	/** The known ritual this is about: the one matched, else (when no unknown one matched) the closest; null otherwise. */
	public @Nullable RitualDefinition rite() {
		return chosen != null ? chosen : unknown != null ? null : closest;
	}
}
