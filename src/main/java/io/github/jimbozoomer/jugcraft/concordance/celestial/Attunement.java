package io.github.jimbozoomer.jugcraft.concordance.celestial;

/**
 * A player's attunement to a pattern (roadmap step 15): until game time {@link #until}, its effect is theirs while the
 * pattern is up, or, when recalled, for {@link Calendar#RECALL_TICKS} ticks whatever the sky. Game time, not the
 * world's clock, ends it, so moving the clock neither stretches nor renews it.
 */
public record Attunement(String pattern, long until, boolean recalled) {
	public static final Attunement NONE = new Attunement("", Long.MIN_VALUE, false);

	public boolean active(long gameTime) {
		return !pattern.isEmpty() && gameTime < until;
	}
}
