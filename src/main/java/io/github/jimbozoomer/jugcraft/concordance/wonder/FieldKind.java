package io.github.jimbozoomer.jugcraft.concordance.wonder;

import org.jspecify.annotations.Nullable;

/**
 * What a raised Concord Spire does in its field, by its configuration (roadmap step 25,
 * docs/features/arcane-concordance-spire.md). Each pulse is bounded: at most a configuration's {@code count} things.
 */
public enum FieldKind {
	/** Kindled light in the field's dark open air (the shared boundary's illumination), so nothing hostile spawns there. */
	ILLUMINATION("illumination"),
	/** Crops in the field grow a step each pulse; nothing ripe is touched, and nothing is harvested or replaced. */
	GROWTH("growth"),
	/** The keeper's people in the field regain Focus, and hostile creatures there are revealed. */
	FOCUS("focus");

	public final String id;

	FieldKind(String id) {
		this.id = id;
	}

	public static @Nullable FieldKind fromId(String id) {
		for (FieldKind kind : values()) {
			if (kind.id.equals(id)) {
				return kind;
			}
		}
		return null;
	}
}
