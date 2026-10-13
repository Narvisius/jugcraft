package io.github.jimbozoomer.jugcraft.concordance.progression;

import io.github.jimbozoomer.jugcraft.concordance.conclave.Rank;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Which stage a player has reached, and what each route to the next still needs (roadmap step 24). Pure: the server
 * gives what the player knows and has done; these rules read the stage definitions against it.
 */
public final class Stages {
	/**
	 * What a player has: each research entry's state, each entry's tradition, their Conclave rank (null if they have not
	 * sworn), how many Conclave projects they helped finish, and the milestones features recorded for them.
	 */
	public record Situation(Map<String, ResearchState> research, Map<String, String> traditions, @Nullable Rank rank, int projects,
			Set<String> milestones) {
	}

	private Stages() {
	}

	/** The stages in order. */
	public static List<StageDefinition> ordered(Collection<StageDefinition> stages) {
		List<StageDefinition> ordered = new ArrayList<>(stages);
		ordered.sort(Comparator.comparingInt(StageDefinition::order));
		return ordered;
	}

	/**
	 * The furthest stage reached: each in order, while one of its routes holds. Null when not even the first stage is
	 * reached.
	 */
	public static @Nullable StageDefinition reached(Collection<StageDefinition> stages, Situation situation) {
		StageDefinition reached = null;
		for (StageDefinition stage : ordered(stages)) {
			if (stage.routes().stream().noneMatch(route -> missing(route, situation).isEmpty())) {
				break;
			}
			reached = stage;
		}
		return reached;
	}

	/** What {@code route} still needs, one line each (empty when it holds). */
	public static List<String> missing(Route route, Situation situation) {
		List<String> missing = new ArrayList<>();
		for (String entry : route.research()) {
			int at = entry.indexOf('@');
			ResearchState need = state(entry.substring(at + 1));
			ResearchState have = situation.research().getOrDefault(entry.substring(0, at), ResearchState.NONE);
			if (need == null || !have.atLeast(need)) {
				missing.add("research " + entry);
			}
		}
		if (route.mastered() > 0) {
			count(situation, ResearchState.MASTERED, route.mastered(), route.traditions(), missing);
		}
		if (route.understood() > 0) {
			count(situation, ResearchState.UNDERSTOOD, route.understood(), route.mastered() > 0 ? 0 : route.traditions(), missing);
		}
		if (!route.rank().isEmpty()) {
			Rank need = rank(route.rank());
			if (need == null || situation.rank() == null || situation.rank().ordinal() < need.ordinal()) {
				missing.add("rank " + route.rank());
			}
		}
		if (situation.projects() < route.projects()) {
			missing.add("projects " + route.projects() + " (" + situation.projects() + ")");
		}
		for (String milestone : route.milestones()) {
			if (!situation.milestones().contains(milestone)) {
				missing.add("milestone " + milestone);
			}
		}
		return missing;
	}

	private static void count(Situation situation, ResearchState least, int need, int traditions, List<String> missing) {
		int have = 0;
		Set<String> across = new HashSet<>();
		for (Map.Entry<String, ResearchState> entry : situation.research().entrySet()) {
			if (entry.getValue().atLeast(least)) {
				have++;
				across.add(situation.traditions().getOrDefault(entry.getKey(), ""));
			}
		}
		if (have < need) {
			missing.add(least.id() + " " + need + " (" + have + ")");
		}
		if (across.size() < traditions) {
			missing.add("traditions " + traditions + " (" + across.size() + ")");
		}
	}

	public static @Nullable ResearchState state(String id) {
		for (ResearchState state : ResearchState.values()) {
			if (state.id().equals(id)) {
				return state;
			}
		}
		return null;
	}

	public static @Nullable Rank rank(String id) {
		for (Rank rank : Rank.values()) {
			if (rank.id.equals(id)) {
				return rank;
			}
		}
		return null;
	}
}
