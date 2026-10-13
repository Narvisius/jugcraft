package io.github.jimbozoomer.jugcraft.concordance.hex;

import org.jspecify.annotations.Nullable;

/**
 * The categories of operation a ward stops (roadmap step 22). A ward guards its bearer against one category only:
 * linking (no new sympathetic link can be made to them), cursing (no curse pulse reaches them), scrying (no
 * investigation can name them as a curse's caster, and no taglock shows where they are) and moving (no Concordance
 * effect can push them).
 */
public enum WardCategory {
	LINKING("linking"),
	CURSING("cursing"),
	SCRYING("scrying"),
	MOVING("moving");

	public final String id;

	WardCategory(String id) {
		this.id = id;
	}

	public static @Nullable WardCategory fromId(String id) {
		for (WardCategory category : values()) {
			if (category.id.equals(id)) {
				return category;
			}
		}
		return null;
	}
}
