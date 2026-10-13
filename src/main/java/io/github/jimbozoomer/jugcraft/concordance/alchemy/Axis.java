package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import org.jspecify.annotations.Nullable;

/**
 * The six properties a mixture is measured in (roadmap step 13), each named for the Principle it carries. Alchemy
 * works with these six; the other Principles have no ingredients yet. The order is fixed: it is the order of a
 * {@link Vector}'s components and of every listing.
 */
public enum Axis {
	RADIANCE("radiance"),
	VERDANCE("verdance"),
	EMBER("ember"),
	RIME("rime"),
	TIDE("tide"),
	HOLLOW("hollow");

	public final String id;

	Axis(String id) {
		this.id = id;
	}

	public static @Nullable Axis fromId(String id) {
		for (Axis axis : values()) {
			if (axis.id.equals(id)) {
				return axis;
			}
		}
		return null;
	}
}
