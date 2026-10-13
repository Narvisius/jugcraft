package io.github.jimbozoomer.jugcraft.concordance.conclave;

import org.jspecify.annotations.Nullable;

/**
 * The kinds of contribution the Starbound Conclave recognises (roadmap step 23): a research entry advanced, a
 * commission fulfilled, another player taught through written notes, and a share of a project. Ranks ask for several
 * kinds, so no single repeated deed reaches them.
 */
public enum Kind {
	RESEARCH("research"),
	COMMISSION("commission"),
	TEACHING("teaching"),
	PROJECT("project");

	public final String id;

	Kind(String id) {
		this.id = id;
	}

	public static @Nullable Kind fromId(String id) {
		for (Kind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}
}
