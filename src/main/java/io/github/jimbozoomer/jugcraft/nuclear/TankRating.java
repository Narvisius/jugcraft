package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.util.StringRepresentable;

/**
 * What a storage tank is built to hold (Justin, Oct 7: one tank MBS, a few ratings, the label filled in from what it
 * holds; docs/features/nuclear.md, "Storage tank ratings"). The rating is the outlet's (the tank's controller): it sets
 * the shell's paint and fittings and which liquids the tank takes (TankProducts). A flammable or corrosive tank also
 * takes general liquids; a cryogenic tank only cryogenic ones.
 */
public enum TankRating implements StringRepresentable {
	/** Plain carbon steel, white, the gooseneck vent: water, brine, nutrient solution, fracking fluid... */
	GENERAL("general"),
	/** White with a red product band, a pressure/vacuum vent on a flame arrester, foam chambers: fuels and crude. */
	FLAMMABLE("flammable"),
	/** Rubber-lined, a darker shell with a yellow band, a scrubber vent: acids, lye, electrolyte. */
	CORROSIVE("corrosive"),
	/** Vacuum-insulated (double-walled) and kept cold: liquid oxygen. Built as its own kit only. */
	CRYOGENIC("cryogenic");

	/** The block state property (the outlet's and the plates'). */
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<TankRating> PROPERTY =
			net.minecraft.world.level.block.state.properties.EnumProperty.create("rating", TankRating.class);

	private final String name;

	TankRating(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	/** Whether a tank of this rating takes a liquid of this kind. */
	public boolean takes(TankProducts.Kind kind) {
		return switch (kind) {
			case GENERAL -> this != CRYOGENIC;
			case FLAMMABLE -> this == FLAMMABLE;
			case CORROSIVE -> this == CORROSIVE;
			case CRYOGENIC -> this == CRYOGENIC;
			case REFUSED -> false;
		};
	}

	/** The rating a tank needs for a liquid of this kind (null: no storage tank takes it). */
	public static TankRating needed(TankProducts.Kind kind) {
		return switch (kind) {
			case GENERAL -> GENERAL;
			case FLAMMABLE -> FLAMMABLE;
			case CORROSIVE -> CORROSIVE;
			case CRYOGENIC -> CRYOGENIC;
			case REFUSED -> null;
		};
	}
}
