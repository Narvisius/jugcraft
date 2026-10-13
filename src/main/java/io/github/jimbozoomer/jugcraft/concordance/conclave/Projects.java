package io.github.jimbozoomer.jugcraft.concordance.conclave;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The rules of Conclave projects (roadmap step 23). A project is personal ({@code player:<uuid>}: only its owner
 * contributes) or communal ({@code party:<uuid>}: any member of that party contributes); one project at a time each.
 * A stage is met when every requirement is full and either enough different contributors took part or, the solo
 * alternative, contributions came on enough different days. Pure.
 */
public final class Projects {
	public static final String PLAYER = "player:";
	public static final String PARTY = "party:";

	/** What a contribution did: how much was accepted, why not (else ""), and the project after. */
	public record Contribution(int accepted, String reason, ProjectState next) {
	}

	private Projects() {
	}

	public static String personal(UUID player) {
		return PLAYER + player;
	}

	public static String communal(UUID party) {
		return PARTY + party;
	}

	/**
	 * Why {@code standing} cannot begin a project now, or "": a member of a rank that may, in good standing, its party's
	 * leader for a communal project, with no unfinished project there already.
	 */
	public static String mayStart(Standing standing, long now, boolean communal, boolean leader, @Nullable ProjectState current) {
		if (!standing.member()) {
			return "not_member";
		}
		if (!Conclave.rank(standing).projects) {
			return "rank";
		}
		if (!Conclave.goodStanding(standing, now)) {
			return "lapsed";
		}
		if (communal && !leader) {
			return "not_leader";
		}
		return current != null && !current.complete() ? "busy" : "";
	}

	public static ProjectState start(ProjectDefinition project, String owner, UUID founder, long now) {
		return new ProjectState(project.id(), owner, founder, now, 0, Map.of(), Set.of(), Set.of(), Set.of(), 0L);
	}

	/**
	 * {@code who} offers {@code amount} toward a requirement of the current stage of type {@code type} and target
	 * {@code target}, on game day {@code day}: the stage takes what it still needs, never more.
	 */
	public static Contribution contribute(ProjectState state, ProjectDefinition project, String type, String target, int amount, UUID who, long day) {
		if (state.complete()) {
			return new Contribution(0, "complete", state);
		}
		if (amount <= 0) {
			return new Contribution(0, "nothing", state);
		}
		ProjectDefinition.Stage stage = project.stages().get(state.stage());
		for (Requirement requirement : stage.requirements()) {
			if (!requirement.type().equals(type) || !requirement.target().equals(target)) {
				continue;
			}
			int missing = requirement.count() - state.progress(requirement.id());
			if (missing <= 0) {
				return new Contribution(0, "full", state);
			}
			int accepted = Math.min(missing, amount);
			Map<String, Integer> progress = new HashMap<>(state.progress());
			progress.merge(requirement.id(), accepted, Integer::sum);
			Set<UUID> contributors = new HashSet<>(state.contributors());
			contributors.add(who);
			Set<Long> days = new HashSet<>(state.days());
			days.add(day);
			Set<UUID> everyone = new HashSet<>(state.everyone());
			everyone.add(who);
			return new Contribution(accepted, "", new ProjectState(state.project(), state.owner(), state.founder(), state.started(), state.stage(),
					progress, contributors, days, everyone, 0L));
		}
		return new Contribution(0, "not_needed", state);
	}

	/** What the current stage still lacks: a requirement's id, {@code "cooperation"}, or "" when it is met. */
	public static String missing(ProjectState state, ProjectDefinition project) {
		if (state.complete()) {
			return "";
		}
		ProjectDefinition.Stage stage = project.stages().get(state.stage());
		for (Requirement requirement : stage.requirements()) {
			if (state.progress(requirement.id()) < requirement.count()) {
				return requirement.id();
			}
		}
		return state.contributors().size() >= stage.contributors() || state.days().size() >= stage.days() ? "" : "cooperation";
	}

	/** The project after its current stage is met (the next stage, empty; or finished at {@code now}); unchanged if not met. */
	public static ProjectState advance(ProjectState state, ProjectDefinition project, long now) {
		if (state.complete() || !missing(state, project).isEmpty()) {
			return state;
		}
		boolean last = state.stage() + 1 >= project.stages().size();
		return new ProjectState(state.project(), state.owner(), state.founder(), state.started(), last ? state.stage() : state.stage() + 1,
				last ? state.progress() : Map.of(), last ? state.contributors() : Set.of(), last ? state.days() : Set.of(), state.everyone(),
				last ? Math.max(1L, now) : 0L);
	}
}
