package io.github.jimbozoomer.jugcraft.concordance.ecology;

/**
 * One beat of a producer ({@link Organism.Role#PRODUCER}): the nutrients it spent from its bed and the Verdance it
 * made. It makes nothing outside its niche ({@link Outcome#STALLED}), on a bed too poor for its cost
 * ({@link Outcome#STARVED}), or with no room for what it would make ({@link Outcome#FULL}); then it spends nothing.
 */
public record Beat(int spent, int made, Outcome outcome) {
	public enum Outcome {
		BEAT("beat"),
		STALLED("stalled"),
		STARVED("starved"),
		FULL("full");

		public final String id;

		Outcome(String id) {
			this.id = id;
		}
	}

	public static Beat of(Organism producer, Verdict verdict, int nutrients, long space) {
		if (verdict.growth() == Verdict.Growth.STALLED) {
			return new Beat(0, 0, Outcome.STALLED);
		}
		if (nutrients < producer.cost()) {
			return new Beat(0, 0, Outcome.STARVED);
		}
		int made = verdict.growth() == Verdict.Growth.THRIVING ? producer.thriving() : producer.tolerating();
		if (space < made) {
			return new Beat(0, 0, Outcome.FULL);
		}
		return new Beat(producer.cost(), made, Outcome.BEAT);
	}
}
