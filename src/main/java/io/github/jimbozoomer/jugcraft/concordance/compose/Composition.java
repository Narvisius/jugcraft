package io.github.jimbozoomer.jugcraft.concordance.compose;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A spell as written: an ordered list of parts (a component and the modifiers joined to it), optionally followed by
 * {@code then} and the composition it triggers where it lands. This is only structure: whether it means anything,
 * and whether a player may cast it, is the {@link Compiler}'s job. Component ids are full ids
 * ({@code jugcraft:kindle}).
 */
public record Composition(List<Part> parts, @Nullable Composition then) {
	public Composition {
		parts = List.copyOf(parts);
	}

	public record Part(String component, List<String> modifiers) {
		public Part {
			modifiers = List.copyOf(modifiers);
		}
	}

	/** The canonical text: Jugcraft's own components by their short names, single spaces, lower case. */
	public String text() {
		List<String> words = new ArrayList<>();
		for (Part part : parts) {
			StringBuilder word = new StringBuilder(shortName(part.component()));
			for (String modifier : part.modifiers()) {
				word.append('+').append(shortName(modifier));
			}
			words.add(word.toString());
		}
		String text = String.join(" ", words);
		return then == null ? text : text + " then " + then.text();
	}

	/** How deep the {@code then} chain goes: 0 for a spell with no branch. */
	public int depth() {
		return then == null ? 0 : 1 + then.depth();
	}

	static String shortName(String id) {
		return id.startsWith("jugcraft:") ? id.substring("jugcraft:".length()) : id;
	}
}
