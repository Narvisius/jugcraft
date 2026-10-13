package io.github.jimbozoomer.jugcraft.concordance.celestial;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** The loaded celestial patterns (roadmap step 15), by id. */
public record CelestialCatalog(Map<String, Pattern> patterns) {
	public static final CelestialCatalog EMPTY = new CelestialCatalog(Map.of());

	public CelestialCatalog {
		patterns = Collections.unmodifiableMap(new TreeMap<>(patterns));
	}

	public @Nullable Pattern pattern(String id) {
		return patterns.get(id);
	}
}
