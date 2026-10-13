package io.github.jimbozoomer.jugcraft.concordance.rules;

import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * How far one player has come with one research entry. The states are ordered: each needs the one before it. A player
 * with no record of an entry is {@link #NONE}.
 */
public enum ResearchState {
	NONE,
	ENCOUNTERED,
	OBSERVED,
	UNDERSTOOD,
	MASTERED;

	/** The id used in data files and saves: the lower-case name. Stable once released. */
	public String id() {
		return name().toLowerCase(Locale.ROOT);
	}

	public boolean atLeast(ResearchState other) {
		return ordinal() >= other.ordinal();
	}

	public @Nullable ResearchState next() {
		return this == MASTERED ? null : values()[ordinal() + 1];
	}

	/** The state with this id, or null; {@code none} is not a state a rule can name. */
	public static @Nullable ResearchState fromId(String id) {
		for (ResearchState state : values()) {
			if (state != NONE && state.id().equals(id)) {
				return state;
			}
		}
		return null;
	}
}
