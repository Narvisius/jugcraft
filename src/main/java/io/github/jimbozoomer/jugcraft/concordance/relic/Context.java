package io.github.jimbozoomer.jugcraft.concordance.relic;

import org.jspecify.annotations.Nullable;

/**
 * Where a relic is (roadmap step 20). A relic works only in the contexts its definition names; anywhere else, carried
 * loose in an inventory or kept in a chest included, it does nothing and can say why. {@link #INVENTORY} and
 * {@link #COSMETIC} are never working contexts: they name "carried loose" and "worn for show" so that a relic can report
 * them. A chest, a shulker box or a bundle is no context at all: nothing in one is ever looked at.
 */
public enum Context {
	MAIN_HAND("main_hand"),
	OFF_HAND("off_hand"),
	HEAD("head"),
	CHEST("chest"),
	LEGS("legs"),
	FEET("feet"),
	/** Worn in a Trinkets slot. */
	TRINKET("trinket"),
	/** Installed in a Reliquary Shrine. */
	INSTALLED("installed"),
	INVENTORY("inventory"),
	/** In a Trinkets cosmetic slot: shown, never worn. */
	COSMETIC("cosmetic");

	public final String id;

	Context(String id) {
		this.id = id;
	}

	/** Whether a definition may name it: everything but carried loose and worn for show. */
	public boolean working() {
		return this != INVENTORY && this != COSMETIC;
	}

	public static @Nullable Context fromId(String id) {
		for (Context context : values()) {
			if (context.id.equals(id)) {
				return context;
			}
		}
		return null;
	}
}
