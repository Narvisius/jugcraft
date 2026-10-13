package io.github.jimbozoomer.jugcraft.concordance.crimson;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** The loaded offering rites (roadmap step 16), by id. */
public record CrimsonCatalog(Map<String, Rite> rites) {
	public static final CrimsonCatalog EMPTY = new CrimsonCatalog(Map.of());

	public CrimsonCatalog {
		rites = Collections.unmodifiableMap(new TreeMap<>(rites));
	}

	public @Nullable Rite rite(String id) {
		return rites.get(id);
	}
}
