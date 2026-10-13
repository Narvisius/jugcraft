package io.github.jimbozoomer.jugcraft.agriculture;

/**
 * The food displays of the kitchen and cooking expansion's slice 2 (tools/feasts.py DISPLAYS), each a
 * {@link FoodDisplayBlock}: where the things set on one lie ({x, z} pixels in a display facing north, one place each), at
 * what height (pixels) and how big ({@code scale} of a whole item across), as the client's ShowcaseRenderer draws them.
 */
public enum FoodDisplay {
	PLATE("plate", new float[][] {{8, 8}}, 1.05F, 0.5F, 3, 1),
	PLATTER("platter", new float[][] {{5, 5}, {11, 5}, {5, 11}, {11, 11}}, 1.05F, 0.42F, 1, 1),
	SERVING_TRAY("serving_tray", new float[][] {{5, 5}, {11, 5}, {5, 11}, {11, 11}}, 1.05F, 0.42F, 1, 2);

	public final String id;
	public final float[][] layout;
	public final float height;
	public final float scale;
	/** Its outline: inset from the block's sides and how tall (pixels). */
	public final int inset;
	public final int tall;

	FoodDisplay(String id, float[][] layout, float height, float scale, int inset, int tall) {
		this.id = id;
		this.layout = layout;
		this.height = height;
		this.scale = scale;
		this.inset = inset;
		this.tall = tall;
	}

	public int places() {
		return layout.length;
	}
}
