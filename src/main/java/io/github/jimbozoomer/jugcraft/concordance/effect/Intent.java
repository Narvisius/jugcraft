package io.github.jimbozoomer.jugcraft.concordance.effect;

import org.jspecify.annotations.Nullable;

/**
 * Whether an effect helps or harms what it reaches. Harmful effects face the friendly-fire rules (never the effect's own
 * cause; another player only where the cause may hurt them and they are not in its party) and the creature's tolerance;
 * helpful ones may reach anyone. Spell Engine uses the same two intents for its impacts.
 */
public enum Intent {
	HELPFUL("helpful"),
	HARMFUL("harmful");

	public final String id;

	Intent(String id) {
		this.id = id;
	}

	public static @Nullable Intent fromId(String id) {
		for (Intent intent : values()) {
			if (intent.id.equals(id)) {
				return intent;
			}
		}
		return null;
	}
}
