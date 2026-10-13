package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * The five places a component can take in a composition, in the order they are written: how the spell leaves you
 * (delivery), what it chooses where it lands (selection), what it does (one or more operations), and how it ends
 * (termination, optional: a spell ends at once by default). Modifiers are written after the part they change, joined
 * with {@code +}.
 */
public enum Slot {
	DELIVERY,
	SELECTION,
	OPERATION,
	MODIFIER,
	TERMINATION;

	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static @Nullable Slot fromId(String id) {
		for (Slot slot : values()) {
			if (slot.id().equals(id)) {
				return slot;
			}
		}
		return null;
	}

	public Text.Ref ref() {
		return new Text.Ref("slot", id());
	}
}
