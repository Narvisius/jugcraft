package io.github.jimbozoomer.jugcraft.agriculture;

/**
 * The feasts of the kitchen and cooking expansion's slices 2 and 3 (tools/feasts.py FEASTS), each a {@link FeastBlock} served
 * {@value FeastBlock#SERVINGS} servings at a time, in the owner's own textures. A serving gives {@code nutrition} hunger
 * and {@code saturation}; the servings add up to about what the ingredients give (docs/features/feasts-and-food-displays.md).
 * {@code light} is the feast's light while any is left; {@code height} and {@code inset} its outline whole (pixels), and
 * {@code leftovers} the height of what is left after the last serving.
 */
public enum FeastDish {
	ROAST_CHICKEN("roast_chicken", 5, 0.7F, 0, 1, 7, 3),
	HONEY_GLAZED_HAM("honey_glazed_ham", 7, 0.8F, 0, 1, 8, 3),
	SHEPHERDS_PIE("shepherds_pie", 5, 0.7F, 0, 1, 8, 1),
	STUFFED_PUMPKIN("stuffed_pumpkin", 5, 0.6F, 0, 2, 11, 8),
	GLEAMING_SALAD("gleaming_salad", 3, 0.6F, 6, 2, 9, 5),
	/** The menu's nachos (slice 3, tools/menu.py): a tray of tortilla chips eaten down to the last bitten chip. */
	NACHOS("nachos", 3, 0.6F, 0, 2, 7, 7);

	public final String id;
	public final int nutrition;
	public final float saturation;
	public final int light;
	public final int inset;
	public final int height;
	public final int leftovers;

	FeastDish(String id, int nutrition, float saturation, int light, int inset, int height, int leftovers) {
		this.id = id;
		this.nutrition = nutrition;
		this.saturation = saturation;
		this.light = light;
		this.inset = inset;
		this.height = height;
		this.leftovers = leftovers;
	}

	/** The serving a bowl takes from it: a bowl food that gives the bowl back. */
	public String serving() {
		return "bowl_of_" + id;
	}
}
