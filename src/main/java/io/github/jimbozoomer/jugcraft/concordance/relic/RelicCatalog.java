package io.github.jimbozoomer.jugcraft.concordance.relic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Every relic the data defines (roadmap step 20), by id and by its item. */
public final class RelicCatalog {
	public static final RelicCatalog EMPTY = new RelicCatalog(Map.of());

	private final Map<String, RelicDefinition> relics;

	public RelicCatalog(Map<String, RelicDefinition> relics) {
		this.relics = Collections.unmodifiableMap(new LinkedHashMap<>(relics));
	}

	public Map<String, RelicDefinition> all() {
		return relics;
	}

	public @Nullable RelicDefinition get(String id) {
		return relics.get(id);
	}

	public @Nullable RelicDefinition byItem(String item) {
		for (RelicDefinition relic : relics.values()) {
			if (relic.item().equals(item)) {
				return relic;
			}
		}
		return null;
	}
}
