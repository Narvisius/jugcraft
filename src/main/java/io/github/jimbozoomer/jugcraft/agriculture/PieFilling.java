package io.github.jimbozoomer.jugcraft.agriculture;

import org.jspecify.annotations.Nullable;

/**
 * The pies a Hearth Oven bakes, each from its filling (and pastry and sugar): apple, pumpkin cream, cranberry, sweet potato
 * and chestnut. Each comes as a raw pie ({@code raw_<id>_pie}), a baked pie placed like a cake ({@code <id>_pie}, eaten
 * or cut a slice at a time) and a slice ({@code <id>_pie_slice}). {@link #color} is the filling's, for the oven's renderer.
 */
public enum PieFilling {
	APPLE("apple", 4, 0.6F, 0xC89A48),
	PUMPKIN_CREAM("pumpkin_cream", 4, 0.6F, 0xE0822A),
	CRANBERRY("cranberry", 3, 0.6F, 0xA01C34),
	SWEET_POTATO("sweet_potato", 4, 0.7F, 0xD8682A),
	CHESTNUT("chestnut", 5, 0.7F, 0x6A3E1E);

	public final String id;
	/** What a slice gives: hunger and its saturation modifier. */
	public final int nutrition;
	public final float saturation;
	public final int color;

	PieFilling(String id, int nutrition, float saturation, int color) {
		this.id = id;
		this.nutrition = nutrition;
		this.saturation = saturation;
		this.color = color;
	}

	public String rawPie() {
		return "raw_" + id + "_pie";
	}

	public String pie() {
		return id + "_pie";
	}

	public String slice() {
		return id + "_pie_slice";
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
