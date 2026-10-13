package io.github.jimbozoomer.jugcraft.concordance.artifice;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Every substrate, gem, rune and affix the data defines (roadmap step 19), by id and by the item that is each one. */
public final class ArtificeCatalog {
	public static final ArtificeCatalog EMPTY = new ArtificeCatalog(Map.of(), Map.of(), Map.of(), Map.of());

	private final Map<String, Substrate> substrates;
	private final Map<String, Gem> gems;
	private final Map<String, Rune> runes;
	private final Map<String, Affix> affixes;

	public ArtificeCatalog(Map<String, Substrate> substrates, Map<String, Gem> gems, Map<String, Rune> runes, Map<String, Affix> affixes) {
		this.substrates = Collections.unmodifiableMap(new LinkedHashMap<>(substrates));
		this.gems = Collections.unmodifiableMap(new LinkedHashMap<>(gems));
		this.runes = Collections.unmodifiableMap(new LinkedHashMap<>(runes));
		this.affixes = Collections.unmodifiableMap(new LinkedHashMap<>(affixes));
	}

	public Map<String, Substrate> substrates() {
		return substrates;
	}

	public Map<String, Gem> gems() {
		return gems;
	}

	public Map<String, Rune> runes() {
		return runes;
	}

	public Map<String, Affix> affixes() {
		return affixes;
	}

	public @Nullable Substrate substrate(String id) {
		return substrates.get(id);
	}

	public @Nullable Gem gem(String id) {
		return gems.get(id);
	}

	public @Nullable Rune rune(String id) {
		return runes.get(id);
	}

	public @Nullable Affix affix(String id) {
		return affixes.get(id);
	}

	public @Nullable Substrate substrateOf(String item) {
		for (Substrate substrate : substrates.values()) {
			if (substrate.item().equals(item)) {
				return substrate;
			}
		}
		return null;
	}

	public @Nullable Gem gemOf(String item) {
		for (Gem gem : gems.values()) {
			if (gem.item().equals(item)) {
				return gem;
			}
		}
		return null;
	}

	public @Nullable Rune runeOf(String item) {
		for (Rune rune : runes.values()) {
			if (rune.item().equals(item)) {
				return rune;
			}
		}
		return null;
	}
}
