package io.github.jimbozoomer.jugcraft.concordance.ecology;

/**
 * What a Mulch Maw makes of what it eats: each food is worth whole quarters of a nutrient (its item tag,
 * {@code #jugcraft:mulch/quarter} to {@code whole}), and every {@value #QUARTERS} quarters digested give one nutrient
 * to a bed. What it has digested stays with it ({@link #digest}) until a bed has room, so nothing is lost by rounding.
 * Nothing composted gives back the nutrients its growing cost, so a garden cannot feed itself on its own waste: its
 * declared sources (a nitrogen fixer, bone meal, fertilizer, plant matter grown elsewhere) make up the difference.
 */
public final class Mulch {
	public static final int QUARTERS = 4;
	/** The most quarters a Maw holds digested: one whole food more than a nutrient's worth, never more. */
	public static final int MAX_HELD = 2 * QUARTERS - 1;

	private Mulch() {
	}

	/** Whether a Maw holding {@code held} quarters may eat a food worth {@code value} more. */
	public static boolean mayEat(int held, int value) {
		return value > 0 && held + value <= MAX_HELD;
	}

	/** Quarters held after eating a food worth {@code value}. */
	public static int digest(int held, int value) {
		if (!mayEat(held, value)) {
			throw new IllegalArgumentException("cannot eat " + value + " holding " + held);
		}
		return held + value;
	}
}
