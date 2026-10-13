package io.github.jimbozoomer.jugcraft.concordance.ecology;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * How a habitat suits an organism, factor by factor, and so how it grows: {@link Growth#THRIVING} when every factor is
 * ideal, {@link Growth#TOLERATING} (half the pace) when some are only tolerable, and {@link Growth#STALLED} when any
 * is outside what it can live with. The same reading always gives the same verdict, and {@link #reasons} says which
 * factors hold it back and by how much.
 */
public record Verdict(Growth growth, Map<Factor, Fit> fits) {
	public enum Growth {
		THRIVING("thriving", 2),
		TOLERATING("tolerating", 1),
		STALLED("stalled", 0);

		public final String id;
		/** Growth points a step at this pace earns: thriving is twice tolerating; stalled earns none. */
		public final int pace;

		Growth(String id, int pace) {
			this.id = id;
			this.pace = pace;
		}
	}

	public Verdict {
		fits = Collections.unmodifiableMap(new EnumMap<>(fits));
	}

	public static Verdict of(Niche niche, Habitat habitat) {
		Map<Factor, Fit> fits = new EnumMap<>(Factor.class);
		boolean outside = false;
		boolean tolerable = false;
		for (Factor factor : Factor.values()) {
			Fit fit = niche.range(factor).judge(habitat.get(factor));
			fits.put(factor, fit);
			outside |= fit.outside();
			tolerable |= fit == Fit.TOLERABLE;
		}
		return new Verdict(outside ? Growth.STALLED : tolerable ? Growth.TOLERATING : Growth.THRIVING, fits);
	}

	public Fit fit(Factor factor) {
		return fits.get(factor);
	}

	/** The factors outside the organism's range: what stops it. */
	public List<Factor> outside() {
		List<Factor> out = new ArrayList<>();
		for (Map.Entry<Factor, Fit> entry : fits.entrySet()) {
			if (entry.getValue().outside()) {
				out.add(entry.getKey());
			}
		}
		return out;
	}

	/** The factors only tolerable: what slows it. */
	public List<Factor> tolerable() {
		List<Factor> out = new ArrayList<>();
		for (Map.Entry<Factor, Fit> entry : fits.entrySet()) {
			if (entry.getValue() == Fit.TOLERABLE) {
				out.add(entry.getKey());
			}
		}
		return out;
	}

	/**
	 * One line for each factor that is not ideal, stopping factors first: its value and what the organism needs
	 * ({@code compose.jugcraft.ecology.short}, {@code excess}, {@code tolerable}).
	 */
	public List<Text> reasons(Niche niche, Habitat habitat) {
		List<Text> lines = new ArrayList<>();
		for (Factor factor : outside()) {
			Niche.Range range = niche.range(factor);
			Text.Ref name = new Text.Ref("factor", factor.id);
			lines.add(fit(factor) == Fit.SHORT ? Text.of("ecology.short", name, habitat.get(factor), range.min())
					: Text.of("ecology.excess", name, habitat.get(factor), range.max()));
		}
		for (Factor factor : tolerable()) {
			Niche.Range range = niche.range(factor);
			lines.add(Text.of("ecology.tolerable", new Text.Ref("factor", factor.id), habitat.get(factor), range.idealMin(), range.idealMax()));
		}
		return lines;
	}
}
