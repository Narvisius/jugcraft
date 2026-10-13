package io.github.jimbozoomer.jugcraft.concordance.ecology;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * What an organism needs: for each {@link Factor} a {@link Range}. A factor an organism does not care about has the
 * whole range, so it is always ideal.
 */
public record Niche(Map<Factor, Range> ranges) {
	public Niche {
		EnumMap<Factor, Range> all = new EnumMap<>(Factor.class);
		for (Factor factor : Factor.values()) {
			all.put(factor, ranges.getOrDefault(factor, Range.any(factor)));
		}
		ranges = Collections.unmodifiableMap(all);
	}

	public Range range(Factor factor) {
		return ranges.get(factor);
	}

	/**
	 * The values it can live with ({@code min} to {@code max}) and those it thrives on ({@code idealMin} to
	 * {@code idealMax}), with {@code min <= idealMin <= idealMax <= max}.
	 */
	public record Range(int min, int idealMin, int idealMax, int max) {
		public Range {
			if (min < 0 || min > idealMin || idealMin > idealMax || idealMax > max) {
				throw new IllegalArgumentException("a range runs least, ideal from, ideal to, most: " + min + ", " + idealMin
						+ ", " + idealMax + ", " + max);
			}
		}

		public static Range any(Factor factor) {
			return new Range(0, 0, factor.max, factor.max);
		}

		public Fit judge(int value) {
			if (value < min) {
				return Fit.SHORT;
			}
			if (value > max) {
				return Fit.EXCESS;
			}
			return value >= idealMin && value <= idealMax ? Fit.IDEAL : Fit.TOLERABLE;
		}
	}
}
