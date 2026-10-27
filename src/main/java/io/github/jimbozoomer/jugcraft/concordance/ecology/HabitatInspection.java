package io.github.jimbozoomer.jugcraft.concordance.ecology;

import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * What the encyclopedia's Inspect page says of a Verdant Bed and the plant on it (Concordance Chunk C, C-07, C-P14,
 * C-P54): each factor's value with how far its reading reaches (the bed's own moisture, with the water's reach; the light
 * at the plant as its two sources, sky and lamps, of which the habitat counts the greater; the bed's nutrients; the kinds
 * within {@link Sampler#RADIUS} blocks at the plant's height; the working magic in the sampled square and
 * {@link Sampler#HEIGHT} block above and below, and, since C-08, the strain recent harvests count against its limit),
 * judged, when an organism stands there, against that organism's range by the verdict growth reaches from the same
 * reading, with all four of its bounds (least, ideal from, ideal to, most).
 * Niches are unchanged: light is shown split, never judged split. Pure: InspectPage gathers the State on the server
 * (VerdantBedBlockEntity.inspect) and writes the lines; nothing changes.
 */
public final class HabitatInspection {
	private HabitatInspection() {
	}

	/** One reading: the habitat as growth reads it, the light at the plant from the sky and from lamps, and the water's reach. */
	public record State(Habitat habitat, int sky, int lamps, int waterReach) {
	}

	/** Each factor's line, in Factor's order, judged by nothing (a bed with no organism on it). */
	public static List<Text> unjudged(State state) {
		List<Text> out = new ArrayList<>();
		for (Factor factor : Factor.values()) {
			out.add(line(state, factor));
		}
		return out;
	}

	/**
	 * Each factor's line, in Factor's order, with how it suits {@code niche}: the fit is {@code verdict}'s own (Verdict.of on
	 * the same reading), so the page and growth never disagree; a factor the organism does not care about is "any".
	 */
	public static List<Text> judged(State state, Niche niche, Verdict verdict) {
		List<Text> out = new ArrayList<>();
		for (Factor factor : Factor.values()) {
			Niche.Range range = niche.range(factor);
			Text judgement = range.equals(Niche.Range.any(factor)) ? Text.of("ecology.inspect.any") : switch (verdict.fit(factor)) {
				case IDEAL -> Text.of("ecology.inspect.ideal", range.idealMin(), range.idealMax(), range.min(), range.max());
				case TOLERABLE -> Text.of("ecology.inspect.tolerable", range.idealMin(), range.idealMax(), range.min(), range.max());
				case SHORT -> Text.of("ecology.inspect.short", range.min(), range.idealMin(), range.idealMax(), range.max());
				case EXCESS -> Text.of("ecology.inspect.excess", range.max(), range.idealMin(), range.idealMax(), range.min());
			};
			out.add(Text.of("ecology.inspect.judged", line(state, factor), judgement));
		}
		return out;
	}

	/** What growth makes of this reading: thriving, slowly, or not at all. */
	public static Text verdict(Verdict verdict) {
		return Text.of("ecology.inspect.verdict", Text.of("ecology.growth." + verdict.growth().id));
	}

	/** A crop's pace on this reading, or ripe. */
	public static Text pace(Verdict verdict, boolean ripe) {
		return ripe ? Text.of("ecology.mature") : Text.of("ecology.growth." + verdict.growth().id);
	}

	/** Said before the readings of a bed with nothing on it that a niche judges. */
	public static Text unplanted() {
		return Text.of("ecology.inspect.unplanted");
	}

	/** A factor's value and how far its reading reaches. */
	static Text line(State state, Factor factor) {
		int value = state.habitat().get(factor);
		int square = 2 * Sampler.RADIUS + 1;
		return switch (factor) {
			case MOISTURE -> Text.of("ecology.inspect.moisture", value, Factor.MOISTURE.max, state.waterReach());
			case LIGHT -> Text.of("ecology.inspect.light", value, state.sky(), state.lamps());
			case NUTRIENTS -> Text.of("ecology.inspect.nutrients", value, Habitat.MAX_NUTRIENTS);
			case DIVERSITY -> Text.of("ecology.inspect.diversity", value, Sampler.RADIUS);
			// Concordance Chunk C (C-08, C-P59): harvest strain counts against the limit, so the row says how much.
			case DISTURBANCE -> state.habitat().strain() > 0
					? Text.of("ecology.inspect.disturbance_strained", value, state.habitat().strain(), square, square, Sampler.HEIGHT)
					: Text.of("ecology.inspect.disturbance", value, square, square, Sampler.HEIGHT);
		};
	}
}
