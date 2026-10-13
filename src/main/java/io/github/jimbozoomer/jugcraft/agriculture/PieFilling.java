package io.github.jimbozoomer.jugcraft.agriculture;

import org.jspecify.annotations.Nullable;

/**
 * What a Hearth Oven bakes. First the pies, each from its filling (and pastry and sugar): apple, pumpkin cream, cranberry,
 * sweet potato, chestnut, chocolate, the sweet berry cheesecake, and the orchards' peach and lemon meringue pies. Each comes
 * as a raw pie ({@code raw_<pie>}), a baked pie placed like a cake ({@code <pie>}, eaten or cut a slice at a time) and a
 * slice ({@code <pie>_slice}); {@code <pie>} is {@code <id>_pie} but for the cheesecake and the lemon meringue pie. Then the
 * cakes the owner drew (tools/cakes.py), made from Cake Batter the same way and set down as a {@link CakeBlock}: for a cake
 * {@link #pie} is the cake's own ID. Then the owner's square pies and tarts (tools/pies_and_tarts.py), made from pastry as
 * the pies are but set down as a {@link CakeBlock} of their own {@link #height}; their {@link #pie} is their own ID too.
 * {@link #color} is the filling's (a cake's sponge's), for the oven's renderer. The Hearth Oven saves a filling by its
 * place in this list, so new fillings go at the end.
 */
public enum PieFilling {
	APPLE("apple", "apple_pie", 4, 0.6F, 0xC89A48),
	PUMPKIN_CREAM("pumpkin_cream", "pumpkin_cream_pie", 4, 0.6F, 0xE0822A),
	CRANBERRY("cranberry", "cranberry_pie", 3, 0.6F, 0xA01C34),
	SWEET_POTATO("sweet_potato", "sweet_potato_pie", 4, 0.7F, 0xD8682A),
	CHESTNUT("chestnut", "chestnut_pie", 5, 0.7F, 0x6A3E1E),
	CHOCOLATE("chocolate", "chocolate_pie", 4, 0.6F, 0x5A3220),
	SWEET_BERRY("sweet_berry", "sweet_berry_cheesecake", 4, 0.6F, 0xB0283C),
	PEACH("peach", "peach_pie", 4, 0.6F, 0xE8904A),
	LEMON("lemon", "lemon_meringue_pie", 4, 0.6F, 0xF0D040),
	CARROT_CAKE("carrot_cake", 4, 0.6F, 0xA8501E),
	BIRTHDAY_CAKE("birthday_cake", 4, 0.6F, 0xF0D8A0),
	ICE_CREAM_CAKE("ice_cream_cake", 4, 0.6F, 0x9A5226),
	RED_VELVET_CAKE("red_velvet_cake", 4, 0.6F, 0xB0121E),
	CHEESECAKE("cheesecake", 4, 0.7F, 0xF2D896),
	COFFEE_CAKE("coffee_cake", 4, 0.6F, 0xC88A48),
	APPLE_CAKE("apple_cake", 4, 0.6F, 0x9E5A26),
	STRAWBERRY_PIE("strawberry_pie", 4, 0.6F, 0xB82838, 7),
	BLUEBERRY_TART("blueberry_tart", 3, 0.6F, 0x6A44B0, 4),
	PLUM_PIE("plum_pie", 4, 0.6F, 0x5A1E48, 7),
	BANOFFEE_PIE("banoffee_pie", 4, 0.6F, 0xEEE0A0, 7),
	SWEET_BERRY_TART("sweet_berry_tart", 3, 0.6F, 0xD84868, 4),
	LEMON_TART("lemon_tart", 3, 0.6F, 0xE8C040, 4),
	WHIPPED_PUMPKIN_PIE("whipped_pumpkin_pie", 4, 0.6F, 0xE07828, 7),
	PORK_PIE("pork_pie", 5, 0.8F, 0xE0A848, 7),
	STRAWBERRY_TART("strawberry_tart", 3, 0.6F, 0xE0607A, 4),
	COFFEE_TART("coffee_tart", 3, 0.6F, 0xD8B890, 4);

	public final String id;
	/** The baked pie's ID; the raw pie and the slice are named from it. */
	public final String pie;
	/** What a slice gives: hunger and its saturation modifier. */
	public final int nutrition;
	public final float saturation;
	public final int color;
	/** A cake (set down as a {@link CakeBlock}, burnt into a Burnt Cake), not a pie. */
	public final boolean cake;
	/** How many texels tall it is set down, as a {@link CakeBlock} (a cake, or one of the owner's pies and tarts); 0 for a pie in its round tin ({@link PieBlock}). */
	public final int height;

	PieFilling(String id, String pie, int nutrition, float saturation, int color) {
		this(id, pie, nutrition, saturation, color, false, 0);
	}

	/** A cake: its ID is its block's. */
	PieFilling(String cake, int nutrition, float saturation, int color) {
		this(cake, cake, nutrition, saturation, color, true, CakeBlock.HEIGHT);
	}

	/** One of the owner's square pies and tarts, {@code height} texels tall: its ID is its block's. */
	PieFilling(String bake, int nutrition, float saturation, int color, int height) {
		this(bake, bake, nutrition, saturation, color, false, height);
	}

	PieFilling(String id, String pie, int nutrition, float saturation, int color, boolean cake, int height) {
		this.id = id;
		this.pie = pie;
		this.nutrition = nutrition;
		this.saturation = saturation;
		this.color = color;
		this.cake = cake;
		this.height = height;
	}

	public String rawPie() {
		return "raw_" + pie;
	}

	public String pie() {
		return pie;
	}

	public String slice() {
		return pie + "_slice";
	}

	/** What it comes out as when left in the oven too long: a Burnt Pie, or a cake's Burnt Cake. */
	public String burnt() {
		return cake ? "burnt_cake" : "burnt_pie";
	}

	/** The filling whose raw pie {@code item} is, or null. */
	public static @Nullable PieFilling ofRaw(String item) {
		for (PieFilling filling : values()) {
			if (filling.rawPie().equals(item)) {
				return filling;
			}
		}
		return null;
	}
}
