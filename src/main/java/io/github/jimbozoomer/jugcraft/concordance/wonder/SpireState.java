package io.github.jimbozoomer.jugcraft.concordance.wonder;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * One Concord Spire's progress, kept by the world (roadmap step 25), never by its heart block: which wonder and
 * configuration, its keeper and whether the keeper's party shares it, the phase being worked (the phases' count once
 * raised) and when it began, the practices and rite counted towards it, the days of upkeep held in a row, when the next
 * day's upkeep falls due, when the keeper's side last carried the configuration's practice through (attendance), whether
 * the last day's upkeep was met, when it was founded, and everyone who contributed (they share its milestone).
 */
public record SpireState(String id, String wonder, String configuration, UUID keeper, boolean communal, int phase, long phaseBegan,
		int practiced, boolean rite, int sustained, long nextDay, long lastAttended, boolean supplied, long founded, Set<UUID> contributors) {
	public SpireState {
		contributors = Collections.unmodifiableSet(new TreeSet<>(contributors));
	}

	public boolean raised(SpireDefinition definition) {
		return phase >= definition.phases().size();
	}

	SpireState with(int phase, long phaseBegan, int practiced, boolean rite, int sustained, long nextDay, long lastAttended, boolean supplied,
			Set<UUID> contributors) {
		return new SpireState(id, wonder, configuration, keeper, communal, phase, phaseBegan, practiced, rite, sustained, nextDay, lastAttended,
				supplied, founded, contributors);
	}

	/** This spire with its keeper's party sharing it, or not. */
	public SpireState withCommunal(boolean shared) {
		return new SpireState(id, wonder, configuration, keeper, shared, phase, phaseBegan, practiced, rite, sustained, nextDay, lastAttended,
				supplied, founded, contributors);
	}

	SpireState withConfiguration(String next, int phase, long now) {
		return new SpireState(id, wonder, next, keeper, communal, phase, now, 0, false, 0, nextDay, lastAttended, false, founded, contributors);
	}
}
