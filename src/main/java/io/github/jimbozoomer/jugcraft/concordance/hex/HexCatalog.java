package io.github.jimbozoomer.jugcraft.concordance.hex;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Every curse the data defines (roadmap step 22), by id and by the reagent that casts it. */
public final class HexCatalog {
	public static final HexCatalog EMPTY = new HexCatalog(Map.of());

	private final Map<String, CurseDefinition> curses;

	public HexCatalog(Map<String, CurseDefinition> curses) {
		this.curses = Collections.unmodifiableMap(new LinkedHashMap<>(curses));
	}

	public Map<String, CurseDefinition> curses() {
		return curses;
	}

	public @Nullable CurseDefinition curse(String id) {
		return curses.get(id);
	}

	public @Nullable CurseDefinition byReagent(String item) {
		for (CurseDefinition curse : curses.values()) {
			if (curse.reagent().equals(item)) {
				return curse;
			}
		}
		return null;
	}
}
