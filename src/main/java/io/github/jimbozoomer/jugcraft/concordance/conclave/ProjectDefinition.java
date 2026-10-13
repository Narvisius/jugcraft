package io.github.jimbozoomer.jugcraft.concordance.conclave;

import java.util.List;

/**
 * A Conclave project (roadmap step 23, data: concordance/project): stages done in order, each a set of requirements met
 * by any of its contributors, and a cooperation rule with a solo alternative: the stage needs {@code contributors}
 * different contributors, or contributions on {@code days} different days. A finished stage brings each of its
 * contributors its renown; the finished project brings everyone who contributed {@code renown} more and its reward.
 */
public record ProjectDefinition(String id, String tradition, int renown, String reward, int rewardCount, List<Stage> stages) {
	public record Stage(String id, int renown, int contributors, int days, List<Requirement> requirements) {
	}
}
