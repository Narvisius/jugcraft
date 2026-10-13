package io.github.jimbozoomer.jugcraft.concordance.crimson;

/**
 * The offering rules (roadmap step 16), pure: what an offering yields, and why one is refused. Vitae is never made from
 * health except here, and never more than the health taken; each offering's yield falls as exhaustion rises, which only
 * time clears ({@link Exhaustion}), so the Vitae a player can give in an hour is bounded whatever heals them.
 */
public final class Offerings {
	private Offerings() {
	}

	/** Percent of a rite's Vitae an offering yields at {@code exhaustion} points: full while fresh, then less and less. */
	public static int efficiency(int exhaustion) {
		if (exhaustion <= 3) {
			return 100;
		}
		if (exhaustion <= 7) {
			return 50;
		}
		return exhaustion < Exhaustion.MAX ? 25 : 0;
	}

	/** What an offering came to. */
	public enum Outcome {
		/** Given: the health was taken and the Vitae made. */
		OFFERED("offered"),
		/** It would leave too little health. */
		TOO_WEAK("too_weak"),
		/** Exhaustion is too high for this rite. */
		EXHAUSTED("exhausted"),
		/** Too soon after the last offering. */
		TOO_SOON("too_soon"),
		/** The vessel has no room for what it would yield (no health is taken for nothing). */
		FULL("full");

		public final String id;

		Outcome(String id) {
			this.id = id;
		}
	}

	/** An offering's result: the Vitae made, the health taken and the exhaustion after (unchanged when refused). */
	public record Attempt(Outcome outcome, int vitae, int health, Exhaustion exhaustion) {
	}

	/**
	 * One offering of {@code rite} by someone with {@code health} health points and {@code exhaustion}, at game time
	 * {@code now}, {@code lastOffering} being the game time of their previous one, into a vessel with {@code room} Vitae
	 * of room. The yield rounds down.
	 */
	public static Attempt offer(Rite rite, float health, Exhaustion exhaustion, long lastOffering, long now, int room) {
		if (now - lastOffering < rite.cooldown()) {
			return new Attempt(Outcome.TOO_SOON, 0, 0, exhaustion);
		}
		int tired = exhaustion.current(now);
		if (tired + rite.exhaustion() > Exhaustion.MAX) {
			return new Attempt(Outcome.EXHAUSTED, 0, 0, exhaustion);
		}
		if (health - rite.health() < rite.floor()) {
			return new Attempt(Outcome.TOO_WEAK, 0, 0, exhaustion);
		}
		int vitae = rite.vitae() * efficiency(tired) / 100;
		if (vitae < 1) {
			return new Attempt(Outcome.EXHAUSTED, 0, 0, exhaustion);
		}
		if (vitae > room) {
			return new Attempt(Outcome.FULL, 0, 0, exhaustion);
		}
		return new Attempt(Outcome.OFFERED, vitae, rite.health(), exhaustion.add(now, rite.exhaustion()));
	}
}
