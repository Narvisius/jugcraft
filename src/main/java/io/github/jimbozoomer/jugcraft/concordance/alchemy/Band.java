package io.github.jimbozoomer.jugcraft.concordance.alchemy;

import org.jspecify.annotations.Nullable;

/**
 * How hot a mixture is, in four bands, and what a stir does at each: the share of the undissolved properties that
 * dissolves ({@code dissolve}, per thousand). Heat makes stirring work faster, but a searing stir also damages the
 * delicate properties ({@link Mixture#SEARING_KEEP}) and fouls the mixture ({@link Mixture#SEARING_CONTAMINANT}), and
 * any stir above cold thaws Rime ({@link Mixture#rimeKeep}).
 * Keep the numbers equal to tools/concordance_alchemy.py.
 */
public enum Band {
	COLD("cold", Integer.MIN_VALUE, 100),
	WARM("warm", 40, 300),
	HOT("hot", 90, 550),
	SEARING("searing", 160, 800);

	public final String id;
	/** The lowest temperature in the band. */
	public final int from;
	public final long dissolve;

	Band(String id, int from, long dissolve) {
		this.id = id;
		this.from = from;
		this.dissolve = dissolve;
	}

	public static Band of(int temperature) {
		Band band = COLD;
		for (Band each : values()) {
			if (temperature >= each.from) {
				band = each;
			}
		}
		return band;
	}

	/** {@code temperature} kept inside this band (Preserve's hold: the heat moves, but not out of the band). */
	public int keep(int temperature) {
		Band[] bands = values();
		int top = ordinal() + 1 < bands.length ? bands[ordinal() + 1].from - 1 : Integer.MAX_VALUE;
		return Math.clamp(temperature, from, top);
	}

	public static @Nullable Band fromId(String id) {
		for (Band band : values()) {
			if (band.id.equals(id)) {
				return band;
			}
		}
		return null;
	}
}
