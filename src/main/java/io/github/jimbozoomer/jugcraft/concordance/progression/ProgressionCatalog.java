package io.github.jimbozoomer.jugcraft.concordance.progression;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** The stages, the practice gates and the progression graph built from them with the rest of the rules (roadmap step 24). */
public final class ProgressionCatalog {
	public static final ProgressionCatalog EMPTY = new ProgressionCatalog(Map.of(), Map.of(), null);

	private final Map<String, StageDefinition> stages;
	private final Map<String, PracticeGate> practices;
	private final @Nullable ProgressionGraph graph;

	public ProgressionCatalog(Map<String, StageDefinition> stages, Map<String, PracticeGate> practices, @Nullable ProgressionGraph graph) {
		this.stages = Collections.unmodifiableMap(new LinkedHashMap<>(stages));
		this.practices = Collections.unmodifiableMap(new LinkedHashMap<>(practices));
		this.graph = graph;
	}

	public Map<String, StageDefinition> stages() {
		return stages;
	}

	public List<StageDefinition> ordered() {
		return Stages.ordered(stages.values());
	}

	public Map<String, PracticeGate> practices() {
		return practices;
	}

	public @Nullable ProgressionGraph graph() {
		return graph;
	}
}
