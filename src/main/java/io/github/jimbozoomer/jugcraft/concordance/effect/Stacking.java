package io.github.jimbozoomer.jugcraft.concordance.effect;

import org.jspecify.annotations.Nullable;

/**
 * What happens when a lasting effect (a status, protection or detection) reaches a creature that already has it. The
 * same rule decides for every source, so a ward from a spell and a ward from a shrine replace one another the same way.
 * Strength is the amplifier (0 for level I); time is the ticks left.
 */
public enum Stacking {
	/**
	 * The stronger stays: a stronger one replaces the existing one; one as strong but longer extends it; anything else
	 * changes nothing (a weaker effect never shortens or weakens what is there, and is not held in reserve).
	 */
	STRONGEST("strongest"),
	/** Each application adds a level, up to {@link #MAX_LEVELS}, and keeps the longer time. */
	ACCUMULATE("accumulate"),
	/** While it lasts it cannot be renewed: a second application changes nothing until the first has expired. */
	EXCLUSIVE("exclusive");

	/** The most levels {@link #ACCUMULATE} builds to (amplifier {@code MAX_LEVELS - 1}). */
	public static final int MAX_LEVELS = 3;

	public final String id;

	Stacking(String id) {
		this.id = id;
	}

	public static @Nullable Stacking fromId(String id) {
		for (Stacking stacking : values()) {
			if (stacking.id.equals(id)) {
				return stacking;
			}
		}
		return null;
	}

	public enum Outcome {
		/** Nothing was there: the incoming effect applies as it is. */
		APPLIED,
		/** It replaced a weaker one. */
		REPLACED,
		/** As strong as what was there, and it ran out later: the time was extended. */
		EXTENDED,
		/** It added a level ({@link #ACCUMULATE}). */
		STACKED,
		/** What was there stays, unchanged. */
		KEPT
	}

	/** The effect a creature ends up with: its amplifier and ticks left, and how that came about. */
	public record Result(Outcome outcome, int amplifier, int ticks) {
		public boolean changed() {
			return outcome != Outcome.KEPT;
		}
	}

	/**
	 * Combines what a creature has ({@code hasTicks <= 0} for nothing) with what reaches it. Pure: the caller applies
	 * the result only if {@link Result#changed()}.
	 */
	public Result combine(int hasAmplifier, int hasTicks, int amplifier, int ticks) {
		if (hasTicks <= 0) {
			return new Result(Outcome.APPLIED, amplifier, ticks);
		}
		return switch (this) {
			case STRONGEST -> {
				if (amplifier > hasAmplifier) {
					yield new Result(Outcome.REPLACED, amplifier, ticks);
				}
				if (amplifier == hasAmplifier && ticks > hasTicks) {
					yield new Result(Outcome.EXTENDED, amplifier, ticks);
				}
				yield new Result(Outcome.KEPT, hasAmplifier, hasTicks);
			}
			case ACCUMULATE -> {
				int stacked = Math.min(MAX_LEVELS - 1, Math.max(hasAmplifier, amplifier) + 1);
				int longer = Math.max(hasTicks, ticks);
				yield stacked == hasAmplifier && longer == hasTicks ? new Result(Outcome.KEPT, hasAmplifier, hasTicks)
						: new Result(stacked > hasAmplifier ? Outcome.STACKED : Outcome.EXTENDED, stacked, longer);
			}
			case EXCLUSIVE -> new Result(Outcome.KEPT, hasAmplifier, hasTicks);
		};
	}
}
