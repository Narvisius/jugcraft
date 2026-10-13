package io.github.jimbozoomer.jugcraft.concordance.conclave;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Every commission and project the data defines (roadmap step 23). */
public final class ConclaveCatalog {
	public static final ConclaveCatalog EMPTY = new ConclaveCatalog(Map.of(), Map.of());

	private final Map<String, CommissionDefinition> commissions;
	private final Map<String, ProjectDefinition> projects;

	public ConclaveCatalog(Map<String, CommissionDefinition> commissions, Map<String, ProjectDefinition> projects) {
		this.commissions = Collections.unmodifiableMap(new LinkedHashMap<>(commissions));
		this.projects = Collections.unmodifiableMap(new LinkedHashMap<>(projects));
	}

	public Map<String, CommissionDefinition> commissions() {
		return commissions;
	}

	public Map<String, ProjectDefinition> projects() {
		return projects;
	}

	public @Nullable CommissionDefinition commission(String id) {
		return commissions.get(id);
	}

	public @Nullable ProjectDefinition project(String id) {
		return projects.get(id);
	}
}
