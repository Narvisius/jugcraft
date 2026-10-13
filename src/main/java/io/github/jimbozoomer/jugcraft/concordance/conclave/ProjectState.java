package io.github.jimbozoomer.jugcraft.concordance.conclave;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * A project under way (roadmap step 23): which project, whose it is ({@code owner}: {@code player:<uuid>} for a
 * personal project, {@code party:<uuid>} for a party's communal one), who began it and when, the stage it is on, what
 * that stage has received by requirement, who contributed to it and on which days, everyone who has contributed to any
 * stage, and when it was finished (0 while it is not).
 */
public record ProjectState(String project, String owner, UUID founder, long started, int stage, Map<String, Integer> progress,
		Set<UUID> contributors, Set<Long> days, Set<UUID> everyone, long finished) {
	public ProjectState {
		progress = Collections.unmodifiableMap(new TreeMap<>(progress));
		contributors = Collections.unmodifiableSet(new TreeSet<>(contributors));
		days = Collections.unmodifiableSet(new TreeSet<>(days));
		everyone = Collections.unmodifiableSet(new TreeSet<>(everyone));
	}

	public boolean complete() {
		return finished > 0L;
	}

	public boolean communal() {
		return owner.startsWith(Projects.PARTY);
	}

	public int progress(String requirement) {
		return progress.getOrDefault(requirement, 0);
	}
}
