package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** The loaded grammar: every component by id and every instrument by the item it describes. Immutable. */
public final class Catalog {
	public static final Catalog EMPTY = new Catalog(Map.of(), Map.of());

	private final Map<String, Component> components;
	private final Map<String, Instrument> instruments;
	private final Map<String, Instrument> byItem = new TreeMap<>();

	public Catalog(Map<String, Component> components, Map<String, Instrument> instruments) {
		this.components = Collections.unmodifiableMap(new TreeMap<>(components));
		this.instruments = Collections.unmodifiableMap(new TreeMap<>(instruments));
		for (Instrument instrument : this.instruments.values()) {
			byItem.putIfAbsent(instrument.item(), instrument);
		}
	}

	public Map<String, Component> components() {
		return components;
	}

	public Map<String, Instrument> instruments() {
		return instruments;
	}

	public @Nullable Component component(String id) {
		return components.get(id);
	}

	/** The instrument definition for an item id, or null if that item is not a composing instrument. */
	public @Nullable Instrument instrumentFor(String item) {
		return byItem.get(item);
	}

	public List<Component> inSlot(Slot slot) {
		List<Component> out = new ArrayList<>();
		for (Component component : components.values()) {
			if (component.slot() == slot) {
				out.add(component);
			}
		}
		return out;
	}

	/** Every composable component's written name ({@link Composition#text()} form), by slot: for suggestions. */
	public Map<Slot, List<String>> names() {
		Map<Slot, List<String>> out = new LinkedHashMap<>();
		for (Slot slot : Slot.values()) {
			List<String> names = new ArrayList<>();
			for (Component component : inSlot(slot)) {
				if (!component.authored()) {
					names.add(Composition.shortName(component.id()));
				}
			}
			out.put(slot, names);
		}
		return out;
	}
}
