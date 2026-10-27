package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;

/**
 * What a property does when a dose holds enough of it (data: {@code data/<ns>/concordance/property/<axis>.json}, or
 * {@code contaminant.json}): a status effect, at level 1 from {@code threshold} milli-units a part and one level more
 * for each further {@code perLevel} (at most {@code maxLevel}), lasting {@code ticksPerUnit} ticks for each unit (at
 * most {@code maxTicks}); and, if it declares {@code fouling} (Concordance Chunk C, C-02, C-P7), losing that many
 * milli-units for each unit of contaminant a dose carries, before its level and duration are worked out. Numbers within
 * the shared effect limits ({@link EffectSpec}).
 *
 * @param id      an {@link Axis} id, or {@code contaminant}
 * @param fouling milli-units of this property each unit of contaminant a part takes from a dose (0: none)
 */
public record Property(String id, String status, Intent intent, long threshold, long perLevel, int maxLevel, int ticksPerUnit,
		int maxTicks, long fouling) {
	public static final String CONTAMINANT = "contaminant";

	public Property {
		if (threshold < 1 || perLevel < 1 || maxLevel < 1 || maxLevel > EffectSpec.MAX_AMPLIFIER + 1 || ticksPerUnit < 1
				|| maxTicks < 1 || maxTicks > EffectSpec.MAX_DURATION || fouling < 0) {
			throw new IllegalArgumentException(id + ": numbers outside the effect limits");
		}
	}

	/** The level (1 up) a dose of {@code milli} a part gives, or 0 below the threshold. */
	public int level(long milli) {
		if (milli < threshold) {
			return 0;
		}
		return (int) Math.min(maxLevel, 1 + (milli - threshold) / perLevel);
	}

	/** How long a dose of {@code milli} a part lasts. */
	public int ticks(long milli) {
		return (int) Math.min(maxTicks, Math.multiplyExact(milli, ticksPerUnit) / 1000L);
	}

	/**
	 * How much of a dose's {@code amount} (milli-units a part) {@code contaminant} milli-units a part take away: {@code fouling}
	 * for each unit of contaminant, never more than there is (Concordance Chunk C, C-02, C-P7); 0 if it declares none.
	 */
	public long fouled(long amount, long contaminant) {
		if (fouling == 0L || amount <= 0L || contaminant <= 0L) {
			return 0L;
		}
		return Math.min(amount, Math.multiplyExact(contaminant, fouling) / 1000L);
	}
}
