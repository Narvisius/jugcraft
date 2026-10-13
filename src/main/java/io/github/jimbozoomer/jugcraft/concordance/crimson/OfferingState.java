package io.github.jimbozoomer.jugcraft.concordance.crimson;

/**
 * A player's offering state (roadmap step 16): their {@link Exhaustion}, the game time of their last offering and of
 * their last Crimson Surge. Saved with the player and kept through death, so dying never resets the offering loop.
 */
public record OfferingState(Exhaustion exhaustion, long lastOffering, long lastSurge) {
	public static final OfferingState NONE = new OfferingState(Exhaustion.NONE, Long.MIN_VALUE / 2, Long.MIN_VALUE / 2);

	public OfferingState withOffering(Exhaustion after, long now) {
		return new OfferingState(after, now, lastSurge);
	}

	public OfferingState withSurge(long now) {
		return new OfferingState(exhaustion, lastOffering, now);
	}
}
