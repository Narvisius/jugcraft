package io.github.jimbozoomer.jugcraft.agriculture;

/**
 * Crops that are picked when ripe and keep standing: tall crops (corn, ornamental corn, sunflower), climbing crops
 * on a trellis (tomato) and bushes (pepper, one block tall). Keep in sync with TALL_CROPS in
 * tools/agriculture.py; the checker compares them.
 *
 * <p>{@code heights[age]} is how many blocks tall the plant is at each age (0-7). At age 7 it is
 * ripe; picking sets it back to {@code pickReset}, which must be as tall as age 7, so the plant
 * keeps standing. {@code growthTime} scales the vanilla crop growth time. A {@code trellis} crop is
 * planted on a {@link TrellisBlock} and only grows up into more trellis, never into air.
 */
public enum TallCrop {
	CORN("corn_crop", "corn_kernels", new int[] {1, 1, 1, 2, 2, 3, 3, 3}, "corn", 2, 3, 5, 1.5F, false),
	SUNFLOWER("sunflower_crop", "sunflower_seeds", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "sunflower_seeds", 2, 4, 5, 1.25F, false),
	TOMATO("tomato_crop", "tomato_seeds", new int[] {1, 1, 1, 2, 2, 2, 2, 2}, "tomato", 2, 4, 5, 1.25F, true),
	PEPPER("pepper_crop", "pepper_seeds", new int[] {1, 1, 1, 1, 1, 1, 1, 1}, "pepper", 1, 3, 5, 1.25F, false),
	ORNAMENTAL_CORN("ornamental_corn_crop", "ornamental_corn_kernels", new int[] {1, 1, 1, 2, 2, 3, 3, 3}, "ornamental_corn", 1, 2, 5, 1.5F, false);

	public final String blockId;
	public final String seedId;
	private final int[] heights;
	public final String produceId;
	public final int pickMin;
	public final int pickMax;
	public final int pickReset;
	public final float growthTime;
	public final boolean trellis;

	TallCrop(String blockId, String seedId, int[] heights, String produceId, int pickMin, int pickMax, int pickReset,
			float growthTime, boolean trellis) {
		this.blockId = blockId;
		this.seedId = seedId;
		this.heights = heights;
		this.produceId = produceId;
		this.pickMin = pickMin;
		this.pickMax = pickMax;
		this.pickReset = pickReset;
		this.growthTime = growthTime;
		this.trellis = trellis;
		if (heights.length != TallCropBlock.MAX_AGE + 1 || heights[pickReset] != heights[TallCropBlock.MAX_AGE]) {
			throw new IllegalArgumentException(blockId + ": needs 8 heights, and picking must not shorten the plant");
		}
	}

	/** Blocks tall at {@code age}. */
	public int height(int age) {
		return heights[age];
	}
}
