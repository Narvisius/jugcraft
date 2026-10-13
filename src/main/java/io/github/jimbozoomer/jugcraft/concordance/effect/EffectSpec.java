package io.github.jimbozoomer.jugcraft.concordance.effect;

import org.jspecify.annotations.Nullable;

/**
 * One effect, independent of what delivers it: its operation, intent, strength and time. What the numbers mean depends
 * on the operation:
 * <ul>
 * <li>{@code magnitude}: health points for damage and restoration; tenths of a block per tick of push for movement;
 * the amplifier (0 is level I) for a status; points of absorption for protection (four per level); unused
 * otherwise.</li>
 * <li>{@code duration}: ticks a status, protection, detection or light lasts; unused otherwise.</li>
 * </ul>
 *
 * @param status the status effect's id, for {@link EffectKind#STATUS} only
 * @param school the Spell Power school whose damage type carries {@link EffectKind#DAMAGE} (null: generic magic)
 */
public record EffectSpec(EffectKind kind, Intent intent, int magnitude, int duration, @Nullable String status, Stacking stacking,
		@Nullable String school) {
	/** The strongest any effect may be, whatever its source: 20 hearts, a 4-blocks-a-tick push, amplifier 40. */
	public static final int MAX_MAGNITUDE = 40;
	/** The longest any effect may last: two minutes. */
	public static final int MAX_DURATION = 2400;
	/** The highest status amplifier (level V). */
	public static final int MAX_AMPLIFIER = 4;
	/** The fastest push, in tenths of a block per tick. */
	public static final int MAX_PUSH = 20;

	public EffectSpec {
		if (magnitude < 0 || magnitude > MAX_MAGNITUDE) {
			throw new IllegalArgumentException(kind.id + ": magnitude " + magnitude + " outside 0.." + MAX_MAGNITUDE);
		}
		if (duration < 0 || duration > MAX_DURATION) {
			throw new IllegalArgumentException(kind.id + ": duration " + duration + " outside 0.." + MAX_DURATION);
		}
		if ((kind == EffectKind.STATUS) != (status != null)) {
			throw new IllegalArgumentException(kind.id + ": a status id goes with a status effect, and only there");
		}
	}

	public static EffectSpec of(EffectKind kind, Intent intent, int magnitude, int duration) {
		return new EffectSpec(kind, intent, magnitude, duration, null, Stacking.STRONGEST, null);
	}

	public EffectSpec withMagnitude(int value) {
		return new EffectSpec(kind, intent, Math.clamp(value, 0, MAX_MAGNITUDE), duration, status, stacking, school);
	}

	public EffectSpec withDuration(int value) {
		return new EffectSpec(kind, intent, magnitude, Math.clamp(value, 0, MAX_DURATION), status, stacking, school);
	}

	/** Absorption amplifier for protection: four points a level, at least level I. */
	public int protectionAmplifier() {
		return Math.max(0, (magnitude + 3) / 4 - 1);
	}

	/** An effect as a particular creature takes it, and whether its tolerance reduced it. */
	public record Adjusted(EffectSpec spec, boolean reduced) {
	}
}
