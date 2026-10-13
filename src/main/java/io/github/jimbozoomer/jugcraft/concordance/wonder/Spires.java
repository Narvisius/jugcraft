package io.github.jimbozoomer.jugcraft.concordance.wonder;

import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The Concord Spire's rules (roadmap step 25, docs/features/arcane-concordance-spire.md). Pure: the server says what it
 * sees (the structure standing, the Ley and items at hand, the time) and applies what these rules decide.
 * <ul>
 * <li>A spire is founded by someone at the wonder's stage who knows its configuration's research.</li>
 * <li>Its phases are finished in order. A phase needs its structure standing with every earlier one, the
 * configuration's practices carried through by the keeper's side after it began, the rite completed near the heart,
 * and days of upkeep held in a row, as it asks; then the next phase begins, counted from nothing.</li>
 * <li>Upkeep runs from the first phase that holds days onwards: each day it takes the configuration's Ley and item if
 * the spire stands whole, is attended (the configuration's practice within the last attendance days) and has them; a
 * day missed takes nothing and starts the count of days again.</li>
 * <li>The field works while the spire is raised, whole, attended and its last day's upkeep was met. Nothing is ever
 * lost: a spire that lapses, is damaged or is left unattended keeps its phase, and works again once put right.</li>
 * </ul>
 */
public final class Spires {
	public static final long DAY = 24_000L;

	private Spires() {
	}

	/** Why {@code configuration} cannot be founded by someone at stage {@code order} who knows {@code research} ("" if it can). */
	public static String mayFound(SpireConfiguration configuration, int order, int needed, Map<String, ResearchState> research) {
		if (order < needed) {
			return "stage";
		}
		for (Definitions.Requirement requirement : configuration.requires()) {
			if (!research.getOrDefault(requirement.research(), ResearchState.NONE).atLeast(requirement.state())) {
				return "research";
			}
		}
		return "";
	}

	/** A spire founded now: its first phase begun, attended (the founding counts), its first upkeep a day away. */
	public static SpireState found(String id, SpireDefinition definition, SpireConfiguration configuration, UUID keeper, boolean communal,
			long now) {
		return new SpireState(id, definition.id(), configuration.id(), keeper, communal, 0, now, 0, false, 0, now + DAY, now, false, now,
				Set.of(keeper));
	}

	/** The phase being worked, or null once raised. */
	public static SpireDefinition.@Nullable Phase phase(SpireDefinition definition, SpireState state) {
		return state.raised(definition) ? null : definition.phases().get(state.phase());
	}

	/** The index of the first phase that holds days of upkeep (the phases' count if none does). */
	public static int firstUpkeep(SpireDefinition definition) {
		for (int i = 0; i < definition.phases().size(); i++) {
			if (definition.phases().get(i).sustain() > 0) {
				return i;
			}
		}
		return definition.phases().size();
	}

	/** The index of the phase whose structure is the crown (the phases' count if none is). */
	public static int crownPhase(SpireDefinition definition) {
		for (int i = 0; i < definition.phases().size(); i++) {
			if (definition.phases().get(i).structure().equals(SpireDefinition.CROWN)) {
				return i;
			}
		}
		return definition.phases().size();
	}

	/** Whether the daily upkeep runs: from the first phase that holds days onwards. */
	public static boolean upkeepRuns(SpireDefinition definition, SpireState state) {
		return state.phase() >= firstUpkeep(definition);
	}

	/**
	 * A practice the keeper's side carried through ({@code who}): when it is the configuration's, it attends the spire
	 * and counts towards the current phase (never beyond what the phase asks), its doer a contributor.
	 */
	public static SpireState practiced(SpireDefinition definition, SpireConfiguration configuration, SpireState state, String activity, UUID who,
			long now) {
		if (!activity.equals(configuration.practice())) {
			return state;
		}
		SpireDefinition.Phase phase = phase(definition, state);
		boolean counts = phase != null && state.practiced() < phase.practices();
		Set<UUID> contributors = new TreeSet<>(state.contributors());
		if (counts) {
			contributors.add(who);
		}
		return state.with(state.phase(), state.phaseBegan(), counts ? state.practiced() + 1 : state.practiced(), state.rite(), state.sustained(),
				state.nextDay(), Math.max(state.lastAttended(), now), state.supplied(), contributors);
	}

	/** The rite was completed near the heart by {@code participants}: counted when the current phase asks for it. */
	public static SpireState rite(SpireDefinition definition, SpireState state, Collection<UUID> participants) {
		SpireDefinition.Phase phase = phase(definition, state);
		if (phase == null || !phase.rite() || state.rite()) {
			return state;
		}
		Set<UUID> contributors = new TreeSet<>(state.contributors());
		contributors.addAll(participants);
		return state.with(state.phase(), state.phaseBegan(), state.practiced(), true, state.sustained(), state.nextDay(), state.lastAttended(),
				state.supplied(), contributors);
	}

	/**
	 * What the current phase still needs, the first thing only: "structure" (the structures through this phase do not
	 * all stand), "practices", "rite" or "sustain"; "" when it can be finished now, "raised" when no phase is left.
	 */
	public static String missing(SpireDefinition definition, SpireState state, boolean intact) {
		SpireDefinition.Phase phase = phase(definition, state);
		if (phase == null) {
			return "raised";
		}
		if (!intact) {
			return "structure";
		}
		if (state.practiced() < phase.practices()) {
			return "practices";
		}
		if (phase.rite() && !state.rite()) {
			return "rite";
		}
		if (state.sustained() < phase.sustain()) {
			return "sustain";
		}
		return "";
	}

	/**
	 * Finishes the current phase: the next begins, counted from nothing. Entering the first phase that holds upkeep, the
	 * first day falls due a day from now; raised, the spire keeps the day it was on.
	 */
	public static SpireState advance(SpireDefinition definition, SpireState state, long now) {
		int next = state.phase() + 1;
		long nextDay = next == firstUpkeep(definition) ? now + DAY : state.nextDay();
		return state.with(next, now, 0, false, 0, nextDay, state.lastAttended(), state.supplied(), state.contributors());
	}

	/** One day's upkeep: whether it fell due, whether it was met (or why not), and what it takes when met. */
	public record Day(SpireState next, boolean due, boolean met, long ley, int items, String reason) {
	}

	/**
	 * The day's upkeep, once due ({@code now} at or past the spire's next day): met when the spire stands whole, is
	 * attended and has the configuration's Ley and items at hand, which it then takes; otherwise nothing is taken and the
	 * days held in a row start again. A spire that was not loaded for days owes nothing for them: the next day falls a day
	 * after this one.
	 */
	public static Day day(SpireDefinition definition, SpireConfiguration configuration, SpireState state, long leyAvailable, int itemsAvailable,
			boolean intact, long now) {
		if (!upkeepRuns(definition, state) || now < state.nextDay()) {
			return new Day(state, false, state.supplied(), 0L, 0, "");
		}
		long nextDay = state.nextDay() + DAY;
		if (nextDay <= now) {
			nextDay = now + DAY;
		}
		String reason = !intact ? "damaged" : !attended(definition, state, now) ? "unattended"
				: leyAvailable < configuration.upkeepLey() || itemsAvailable < configuration.upkeepCount() ? "unsupplied" : "";
		boolean met = reason.isEmpty();
		SpireDefinition.Phase phase = phase(definition, state);
		int sustained = !met ? 0 : phase != null && phase.sustain() > 0 ? Math.min(state.sustained() + 1, phase.sustain()) : state.sustained();
		SpireState next = state.with(state.phase(), state.phaseBegan(), state.practiced(), state.rite(), sustained, nextDay, state.lastAttended(),
				met, state.contributors());
		return new Day(next, true, met, met ? configuration.upkeepLey() : 0L, met ? configuration.upkeepCount() : 0, reason);
	}

	/** Whether the keeper's side carried the configuration's practice through within the last attendance days. */
	public static boolean attended(SpireDefinition definition, SpireState state, long now) {
		return now - state.lastAttended() <= definition.attendanceDays() * DAY;
	}

	/** Why the field does not work now ("" when it does): still raising, damaged, unattended or unsupplied. */
	public static String dormant(SpireDefinition definition, SpireState state, boolean intact, long now) {
		if (!state.raised(definition)) {
			return "raising";
		}
		if (!intact) {
			return "damaged";
		}
		if (!attended(definition, state, now)) {
			return "unattended";
		}
		if (!state.supplied()) {
			return "unsupplied";
		}
		return "";
	}

	/** Whether the field works now. */
	public static boolean active(SpireDefinition definition, SpireState state, boolean intact, long now) {
		return dormant(definition, state, intact, now).isEmpty();
	}

	/**
	 * Changes the configuration. Before the crown, nothing is lost but the current phase's practices (they were the old
	 * configuration's); from the crown on, the spire goes back to its crown phase: the foundation and the phases before
	 * the crown stand, and the new crown, its rite and its days are done again.
	 */
	public static SpireState realign(SpireDefinition definition, SpireState state, SpireConfiguration next, long now) {
		if (next.id().equals(state.configuration())) {
			return state;
		}
		return state.withConfiguration(next.id(), Math.min(state.phase(), crownPhase(definition)), now);
	}
}
