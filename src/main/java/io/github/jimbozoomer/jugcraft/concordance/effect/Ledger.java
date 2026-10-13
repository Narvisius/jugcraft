package io.github.jimbozoomer.jugcraft.concordance.effect;

import java.util.HashSet;
import java.util.Set;

/**
 * The account of one event (one cast, one use, one ritual run: one {@link Cause#serial()}), shared by everything it
 * sets off, triggered branches and repeated pulses included. Every application of an effect is admitted here first,
 * and is refused if it would
 * <ul>
 * <li>apply the same step to the same target twice ({@link Admission#DUPLICATE}: one-time accounting),</li>
 * <li>reach more distinct targets than the event's limit ({@link Admission#OVER_TARGETS}), or</li>
 * <li>spend more work than its limit ({@link Admission#OVER_WORK}).</li>
 * </ul>
 * So however a spell branches or repeats, it cannot do more than its compiled limits allow. Not thread-safe: used on
 * the server thread only.
 */
public final class Ledger {
	/** The most an event may do: distinct targets, work units and triggered branches. */
	public record Limits(int targets, int work, int branches) {
		public Limits {
			if (targets < 0 || work < 0 || branches < 0) {
				throw new IllegalArgumentException("negative limit");
			}
		}
	}

	public enum Admission {
		ADMITTED,
		DUPLICATE,
		OVER_TARGETS,
		OVER_WORK;

		public boolean admitted() {
			return this == ADMITTED;
		}
	}

	private final Limits limits;
	private final Set<String> applications = new HashSet<>();
	private final Set<String> targets = new HashSet<>();
	private int work;
	private int branches;

	public Ledger(Limits limits) {
		this.limits = limits;
	}

	public Limits limits() {
		return limits;
	}

	/**
	 * Admits one application of {@code step} (unique within the event, such as {@code "0/1/2"} for depth, pulse and
	 * step) to {@code target} (a creature's UUID or a block position), costing {@code cost} work, and records it.
	 */
	public Admission admit(String step, String target, int cost) {
		String application = step + "@" + target;
		if (applications.contains(application)) {
			return Admission.DUPLICATE;
		}
		if (!targets.contains(target) && targets.size() >= limits.targets()) {
			return Admission.OVER_TARGETS;
		}
		if (cost < 0 || work + cost > limits.work()) {
			return Admission.OVER_WORK;
		}
		applications.add(application);
		targets.add(target);
		work += cost;
		return Admission.ADMITTED;
	}

	/** Takes one triggered branch from the allowance; false (and nothing taken) if none is left. */
	public boolean branch() {
		if (branches >= limits.branches()) {
			return false;
		}
		branches++;
		return true;
	}

	public int targets() {
		return targets.size();
	}

	public int work() {
		return work;
	}

	public int branches() {
		return branches;
	}
}
