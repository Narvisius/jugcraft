package io.github.jimbozoomer.jugcraft.agriculture;

/**
 * Crops that are picked when ripe and keep standing: tall crops (corn, ornamental corn, sunflower), climbing crops
 * on a trellis (tomato, cucumber, black pepper, vanilla), bushes (pepper, strawberry, blueberry, coffee, eggplant and
 * zucchini, one block tall), the kitchen herbs (a block tall) and paddy crops (rice).
 * Keep in sync with TALL_CROPS in tools/agriculture.py; the checker compares them.
 *
 * <p>{@code heights[age]} is how many blocks tall the plant is at each age (0-7). At age 7 it is
 * ripe; picking sets it back to {@code pickReset}, which must be as tall as age 7, so the plant
 * keeps standing. {@code growthTime} scales the vanilla crop growth time. A {@code trellis} crop is
 * planted on a {@link TrellisBlock} and only grows up into more trellis, never into air. A {@code paddy} crop
 * ({@link PaddyCropBlock}) stands in a still water source one block deep, over bog soil.
 */
public enum TallCrop {
	CORN("corn_crop", "corn_kernels", new int[] {1, 1, 1, 2, 2, 3, 3, 3}, "corn", 2, 3, 5, 1.5F, false, false),
	SUNFLOWER("sunflower_crop", "sunflower_seeds", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "sunflower_seeds", 2, 4, 5, 1.25F, false, false),
	TOMATO("tomato_crop", "tomato_seeds", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "tomato", 2, 4, 5, 1.25F, true, false),
	PEPPER("pepper_crop", "pepper_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "pepper", 1, 3, 5, 1.25F, false, false),
	ORNAMENTAL_CORN("ornamental_corn_crop", "ornamental_corn_kernels", new int[] {1, 1, 1, 2, 2, 3, 3, 3}, "ornamental_corn", 1, 2, 5, 1.5F, false, false),
	RICE("rice_crop", "rice", new int[] {1, 1, 1, 1, 2, 2, 2, 2}, "rice_panicle", 2, 3, 4, 1.25F, false, true),
	// The fruit crops (tools/fruit_crops.py): bushes a block tall, as the pepper.
	STRAWBERRY("strawberry_crop", "strawberry_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "strawberry", 1, 3, 5, 1.25F, false, false),
	BLUEBERRY("blueberry_crop", "blueberry_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "blueberries", 2, 4, 5, 1.25F, false, false),
	COFFEE("coffee_crop", "coffee_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "coffee_cherries", 1, 3, 5, 1.5F, false, false),
	// Garden crops, herbs and spices, part b (tools/vegetables.py, herbs.py, spices.py): the cucumber on its trellis, the
	// eggplant and zucchini bushes; the eight herbs, a block tall, cut back to age 4; the black pepper and vanilla vines.
	CUCUMBER("cucumber_crop", "cucumber_seeds", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "cucumber", 2, 4, 5, 1.25F, true, false),
	EGGPLANT("eggplant_crop", "eggplant_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "eggplant", 1, 3, 5, 1.25F, false, false),
	ZUCCHINI("zucchini_crop", "zucchini_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "zucchini", 1, 3, 5, 1.25F, false, false),
	BASIL("basil_crop", "basil", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "basil", 1, 3, 4, 1.0F, false, false),
	MINT("mint_crop", "mint", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "mint", 1, 3, 4, 1.0F, false, false),
	ROSEMARY("rosemary_crop", "rosemary", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "rosemary", 1, 3, 4, 1.0F, false, false),
	THYME("thyme_crop", "thyme", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "thyme", 1, 3, 4, 1.0F, false, false),
	PARSLEY("parsley_crop", "parsley", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "parsley", 1, 3, 4, 1.0F, false, false),
	SAGE("sage_crop", "sage", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "sage", 1, 3, 4, 1.0F, false, false),
	DILL("dill_crop", "dill", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "dill", 1, 3, 4, 1.0F, false, false),
	CHIVES("chives_crop", "chives", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "chives", 1, 3, 4, 1.0F, false, false),
	PEPPERCORN("peppercorn_crop", "peppercorns", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "peppercorns", 2, 4, 5, 1.5F, true, false),
	VANILLA("vanilla_crop", "vanilla_pods", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "vanilla_pods", 1, 2, 5, 1.5F, true, false);

	public final String blockId;
	public final String seedId;
	private final int[] heights;
	public final String produceId;
	public final int pickMin;
	public final int pickMax;
	public final int pickReset;
	public final float growthTime;
	public final boolean trellis;
	public final boolean paddy;

	TallCrop(String blockId, String seedId, int[] heights, String produceId, int pickMin, int pickMax, int pickReset,
			float growthTime, boolean trellis, boolean paddy) {
		this.blockId = blockId;
		this.seedId = seedId;
		this.heights = heights;
		this.produceId = produceId;
		this.pickMin = pickMin;
		this.pickMax = pickMax;
		this.pickReset = pickReset;
		this.growthTime = growthTime;
		this.trellis = trellis;
		this.paddy = paddy;
		if (heights.length != TallCropBlock.MAX_AGE + 1 || heights[pickReset] != heights[TallCropBlock.MAX_AGE]) {
			throw new IllegalArgumentException(blockId + ": needs 8 heights, and picking must not shorten the plant");
		}
	}

	/** Blocks tall at {@code age}. */
	public int height(int age) {
		return heights[age];
	}
}
