package io.github.jimbozoomer.jugcraft.concordance.progression;

import java.util.List;

/**
 * One way to reach a stage (roadmap step 24): every condition it names must hold. {@code research} are exact entries and
 * states ({@code jugcraft:circle_lore@understood}); {@code mastered} and {@code understood} ask for that many entries at
 * least that far, across at least {@code traditions} traditions; {@code rank} a Starbound Conclave rank; {@code projects}
 * that many Conclave projects finished with the player contributing; {@code milestones} named deeds a feature records.
 * An empty or zero condition asks nothing.
 */
public record Route(String id, List<String> research, int mastered, int understood, int traditions, String rank, int projects,
		List<String> milestones) {
	public Route {
		research = List.copyOf(research);
		milestones = List.copyOf(milestones);
	}
}
