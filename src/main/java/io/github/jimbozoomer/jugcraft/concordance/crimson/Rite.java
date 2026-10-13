package io.github.jimbozoomer.jugcraft.concordance.crimson;

/**
 * An offering rite (roadmap step 16; data: {@code data/<ns>/concordance/offering/}): giving {@link #health} health
 * points (half-hearts) for up to {@link #vitae} Vitae, adding {@link #exhaustion} points of offering exhaustion. It is
 * refused when it would leave fewer than {@link #floor} health points, within {@link #cooldown} game ticks of the last
 * offering, or past the exhaustion limit. Health is taken only by an explicit offering, never by anything else.
 */
public record Rite(String id, int health, int vitae, int exhaustion, int floor, int cooldown) {
	public Rite {
		if (health < 1 || vitae < 1 || exhaustion < 1 || exhaustion > Exhaustion.MAX || floor < 1 || cooldown < 0) {
			throw new IllegalArgumentException("a rite takes health, gives Vitae, adds exhaustion and keeps a health floor");
		}
		if (vitae > health) {
			throw new IllegalArgumentException("a rite never gives more Vitae than the health it takes");
		}
	}
}
