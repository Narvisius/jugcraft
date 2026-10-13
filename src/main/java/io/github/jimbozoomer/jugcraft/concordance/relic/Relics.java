package io.github.jimbozoomer.jugcraft.concordance.relic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The rules every relic follows (roadmap step 20, docs/features/arcane-concordance-relics.md). A relic works only in a
 * context one of its modes names; anywhere else (carried loose, worn for show, in the wrong hand, in a chest) it does
 * nothing, and {@link #check} says exactly why. Pure: the server supplies where the relic is and what is around it.
 * <p>
 * Aggregation: one player's carried and worn relics pulse at most {@value #BUDGET} times in any {@value #BUDGET_TICKS}
 * game ticks together, and one owner's installed relics at most {@value #SHRINE_BUDGET} ({@link Budget}); a relic held
 * back by its budget is not spent and tries again at the next check. Their effects go through the shared effect
 * boundary, whose stacking keeps two relics giving the same status from adding up: the stronger holds.
 */
public final class Relics {
	/** The most pulses one player's carried and worn relics may have in {@link #BUDGET_TICKS}. */
	public static final int BUDGET = 2;
	/** The most pulses one owner's installed relics may have in {@link #BUDGET_TICKS}, however many shrines they keep. */
	public static final int SHRINE_BUDGET = 4;
	public static final int BUDGET_TICKS = 20;
	/** "Calm" means no harm taken in this many game ticks. */
	public static final int CALM_TICKS = 200;
	/** The holder of installed relics whose shrine has no owner. */
	public static final UUID NO_OWNER = new UUID(0L, 0L);

	/**
	 * What is around a relic when it would pulse: who uses it (the holder, or the shrine's owner), the dimension, whether
	 * the open sky is above it and it is night, when its holder was last hurt, whether its holder is hurt now, and the
	 * game time.
	 */
	public record Situation(UUID user, String dimension, boolean sky, boolean night, long lastHurt, boolean wounded, long now) {
	}

	/**
	 * Whether a relic may pulse: the mode it would use, or the reason it cannot. {@code supported} lists every context
	 * it works in (for the message). {@code resting} means it is allowed but waiting for its interval: not a fault.
	 */
	public record Verdict(@Nullable Mode mode, String reason, List<Context> supported) {
		public boolean allowed() {
			return mode != null && reason.isEmpty();
		}

		public boolean resting() {
			return "resting".equals(reason);
		}
	}

	private Relics() {
	}

	public static Verdict check(RelicDefinition relic, Context context, RelicState state, Situation situation) {
		List<Context> supported = relic.contexts();
		Mode mode = relic.mode(context);
		if (mode == null) {
			return new Verdict(null, context == Context.INSTALLED ? "cannot_install" : "wrong_context", supported);
		}
		if (relic.owned() && state.owner() != null && !state.owner().equals(situation.user())) {
			return new Verdict(null, "not_owner", supported);
		}
		Mode.Conditions conditions = mode.conditions();
		if (!conditions.dimensions().isEmpty() && !conditions.dimensions().contains(situation.dimension())) {
			return new Verdict(null, "wrong_dimension", supported);
		}
		if (conditions.sky() && !situation.sky()) {
			return new Verdict(null, "needs_sky", supported);
		}
		if (conditions.night() && !situation.night()) {
			return new Verdict(null, "needs_night", supported);
		}
		if (conditions.calm() && situation.now() - situation.lastHurt() < CALM_TICKS) {
			return new Verdict(null, "not_calm", supported);
		}
		if (conditions.wounded() && mode.target() == Mode.Target.SELF && !situation.wounded()) {
			return new Verdict(null, "not_wounded", supported);
		}
		if (state.charge() < mode.cost()) {
			return new Verdict(null, "no_charge", supported);
		}
		if (situation.now() - state.lastPulse() < mode.interval()) {
			return new Verdict(mode, "resting", supported);
		}
		return new Verdict(mode, "", supported);
	}

	/**
	 * Pulses holders' relics have had lately: at most {@code limit} for one holder in any {@link #BUDGET_TICKS}, however
	 * many relics it carries, wears or installs. Not saved: a restart only forgets a second of history.
	 */
	public static final class Budget {
		private final int limit;
		private final Map<UUID, Deque<Long>> pulses = new HashMap<>();

		public Budget(int limit) {
			this.limit = limit;
		}

		/** Takes one pulse for {@code holder} at {@code now}, if the budget allows; returns whether it did. */
		public boolean take(UUID holder, long now) {
			Deque<Long> recent = pulses.computeIfAbsent(holder, unused -> new ArrayDeque<>());
			while (!recent.isEmpty() && (now - recent.peekFirst() >= BUDGET_TICKS || now < recent.peekFirst())) {
				recent.removeFirst();
			}
			if (recent.size() >= limit) {
				return false;
			}
			recent.addLast(now);
			return true;
		}

		/** Whether {@code holder} could take a pulse at {@code now} (taking nothing). */
		public boolean allows(UUID holder, long now) {
			Deque<Long> recent = pulses.get(holder);
			if (recent == null) {
				return true;
			}
			int live = 0;
			for (long at : recent) {
				if (now - at < BUDGET_TICKS && now >= at) {
					live++;
				}
			}
			return live < limit;
		}

		public void forget(UUID holder) {
			pulses.remove(holder);
		}
	}
}
