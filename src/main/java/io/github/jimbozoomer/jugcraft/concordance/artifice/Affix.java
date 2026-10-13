package io.github.jimbozoomer.jugcraft.concordance.artifice;

/**
 * An affix (roadmap step 19, data: concordance/affix): a random property, rolled as one of {@code steps + 1} evenly
 * spaced values from {@code min} to {@code max}, with the capacity it uses. Two affixes of one {@code group} never sit
 * on one artifice.
 */
public record Affix(String id, String attribute, Stat.Operation operation, double min, double max, int steps, int cost, String group) {
	/** The value of step {@code step} (0 to {@code steps}). */
	public double value(int step) {
		return steps == 0 ? min : min + (max - min) * Math.clamp(step, 0, steps) / steps;
	}
}
