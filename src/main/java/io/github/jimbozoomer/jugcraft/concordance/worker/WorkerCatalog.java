package io.github.jimbozoomer.jugcraft.concordance.worker;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** The loaded worker definitions (roadmap step 17), by id. */
public record WorkerCatalog(Map<String, WorkerDefinition> workers) {
	public static final WorkerCatalog EMPTY = new WorkerCatalog(Map.of());

	public WorkerCatalog {
		workers = Collections.unmodifiableMap(new TreeMap<>(workers));
	}

	public <T extends WorkerDefinition> @Nullable T get(String id, Class<T> kind) {
		WorkerDefinition definition = workers.get(id);
		return kind.isInstance(definition) ? kind.cast(definition) : null;
	}
}
