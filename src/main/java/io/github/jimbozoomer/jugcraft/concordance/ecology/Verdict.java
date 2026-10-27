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
 * factors hold it back and by how much. Since Concordance Chunk C (C-08), {@link #strained} says harvest strain decided the
 * disturbance fit.
 */
public record Verdict(Growth growth, Map<Factor, Fit> fits, boolean strained) {
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
		boolean strained = false;
		for (Factor factor : Factor.values()) {
			Niche.Range range = niche.range(factor);
			Fit fit = range.judge(habitat.get(factor));
			if (factor == Factor.DISTURBANCE && habitat.strain() > 0) {
				// Harvest strain (Concordance Chunk C, C-08, C-P59) counts against the plant's limit, never as disturbance it
				// needs: the worse of the area alone and the area with the strain.
				Fit withStrain = range.judge(habitat.strained());
				if (worse(withStrain, fit)) {
					fit = withStrain;
					strained = true;
				}
			}
			fits.put(factor, fit);
			outside |= fit.outside();
			tolerable |= fit == Fit.TOLERABLE;
		}
		return new Verdict(outside ? Growth.STALLED : tolerable ? Growth.TOLERATING : Growth.THRIVING, fits, strained);
	}

	/** Whether {@code a} holds a plant back more than {@code b}: outside its range, then tolerable, then ideal. */
	private static boolean worse(Fit a, Fit b) {
		return rank(a) > rank(b);
	}

	private static int rank(Fit fit) {
		return fit.outside() ? 2 : fit == Fit.TOLERABLE ? 1 : 0;
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
	 * ({@code compose.jugcraft.ecology.short}, {@code excess}, {@code tolerable}); where harvest strain decided the
	 * disturbance fit (Concordance Chunk C, C-08), the line says how much strain counted ({@code ecology.strained.*}).
	 */
	public List<Text> reasons(Niche niche, Habitat habitat) {
		List<Text> lines = new ArrayList<>();
		for (Factor factor : outside()) {
			Niche.Range range = niche.range(factor);
			Text.Ref name = new Text.Ref("factor", factor.id);
			if (strained && factor == Factor.DISTURBANCE) {
				lines.add(Text.of("ecology.strained.excess", name, habitat.get(factor), habitat.strain(), range.max()));
			} else {
				lines.add(fit(factor) == Fit.SHORT ? Text.of("ecology.short", name, habitat.get(factor), range.min())
						: Text.of("ecology.excess", name, habitat.get(factor), range.max()));
			}
		}
		for (Factor factor : tolerable()) {
			Niche.Range range = niche.range(factor);
			Text.Ref name = new Text.Ref("factor", factor.id);
			if (strained && factor == Factor.DISTURBANCE) {
				lines.add(Text.of("ecology.strained.tolerable", name, habitat.get(factor), habitat.strain(), range.idealMin(), range.idealMax()));
			} else {
				lines.add(Text.of("ecology.tolerable", name, habitat.get(factor), range.idealMin(), range.idealMax()));
			}
		}
		return lines;
	}
}
