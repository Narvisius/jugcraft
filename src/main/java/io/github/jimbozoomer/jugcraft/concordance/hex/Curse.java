package io.github.jimbozoomer.jugcraft.concordance.hex;

import java.util.UUID;

/**
 * A curse on a creature (roadmap step 22): which curse, who cast it (and their name, so tracing it never needs a lookup),
 * when it began and ends, its last pulse, and how much its bearer has learned of it ({@code insight}: 0 only its
 * symptoms, 1 its name and remedy, 2 its caster too).
 */
public record Curse(String curse, UUID caster, String casterName, long started, long until, long lastPulse, int insight) {
	public static final int NAMED = 1;
	public static final int TRACED = 2;

	public boolean active(long now) {
		return now < until;
	}

	public Curse pulsed(long now) {
		return new Curse(curse, caster, casterName, started, until, now, insight);
	}

	public Curse withInsight(int level) {
		return new Curse(curse, caster, casterName, started, until, lastPulse, Math.max(insight, Math.min(TRACED, level)));
	}
}
