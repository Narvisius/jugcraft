package io.github.jimbozoomer.jugcraft.concordance.wonder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** The wonders and their configurations the rules loaded (roadmap step 25). */
public final class WonderCatalog {
	public static final WonderCatalog EMPTY = new WonderCatalog(Map.of(), Map.of());

	private final Map<String, SpireDefinition> wonders;
	private final Map<String, SpireConfiguration> configurations;

	public WonderCatalog(Map<String, SpireDefinition> wonders, Map<String, SpireConfiguration> configurations) {
		this.wonders = Collections.unmodifiableMap(new TreeMap<>(wonders));
		this.configurations = Collections.unmodifiableMap(new TreeMap<>(configurations));
	}

	public Map<String, SpireDefinition> wonders() {
		return wonders;
	}

	public @Nullable SpireDefinition wonder(String id) {
		return wonders.get(id);
	}

	public Map<String, SpireConfiguration> configurations() {
		return configurations;
	}

	public @Nullable SpireConfiguration configuration(String id) {
		return configurations.get(id);
	}

	/** The configurations of {@code wonder}, by id. */
	public List<SpireConfiguration> configurations(String wonder) {
		return configurations.values().stream().filter(configuration -> configuration.wonder().equals(wonder)).toList();
	}

	/** The wonder whose heart is {@code block}, if any. */
	public @Nullable SpireDefinition byHeart(String block) {
		for (SpireDefinition wonder : wonders.values()) {
			if (wonder.heart().equals(block)) {
				return wonder;
			}
		}
		return null;
	}
}
