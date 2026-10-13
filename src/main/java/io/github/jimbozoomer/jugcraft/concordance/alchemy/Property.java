package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;

/**
 * What a property does when a dose holds enough of it (data: {@code data/<ns>/concordance/property/<axis>.json}, or
 * {@code contaminant.json}): a status effect, at level 1 from {@code threshold} milli-units a part and one level more
 * for each further {@code perLevel} (at most {@code maxLevel}), lasting {@code ticksPerUnit} ticks for each unit (at
 * most {@code maxTicks}). Numbers within the shared effect limits ({@link EffectSpec}).
 *
 * @param id an {@link Axis} id, or {@code contaminant}
 */
public record Property(String id, String status, Intent intent, long threshold, long perLevel, int maxLevel, int ticksPerUnit,
		int maxTicks) {
	public static final String CONTAMINANT = "contaminant";

	public Property {
		if (threshold < 1 || perLevel < 1 || maxLevel < 1 || maxLevel > EffectSpec.MAX_AMPLIFIER + 1 || ticksPerUnit < 1
				|| maxTicks < 1 || maxTicks > EffectSpec.MAX_DURATION) {
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
}
