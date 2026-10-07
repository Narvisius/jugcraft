package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * What Knitting Needles knit, in the order sneaking cycles through them: a beanie, wool socks and five sweaters (plain,
 * striped, and with a pumpkin, a bat or an autumn leaf knitted in). Each takes {@link #rows} rows, a ball of yarn a row,
 * and is worn in its {@link #slot}; its look on the wearer is the equipment asset {@code jugcraft:<asset>} (the knit,
 * dyed the garment's colour, and the sweater's motif over it).
 */
public enum Knitwear {
	BEANIE("knit_beanie", EquipmentSlot.HEAD, 2, "knit"),
	SOCKS("wool_socks", EquipmentSlot.FEET, 2, "knit"),
	SWEATER("knit_sweater", EquipmentSlot.CHEST, 5, "knit"),
	STRIPED_SWEATER("striped_sweater", EquipmentSlot.CHEST, 5, "knit_striped"),
	PUMPKIN_SWEATER("pumpkin_sweater", EquipmentSlot.CHEST, 5, "knit_pumpkin"),
	BAT_SWEATER("bat_sweater", EquipmentSlot.CHEST, 5, "knit_bat"),
	LEAF_SWEATER("leaf_sweater", EquipmentSlot.CHEST, 5, "knit_leaf");

	public final String item;
	public final EquipmentSlot slot;
	public final int rows;
	public final String asset;

	Knitwear(String item, EquipmentSlot slot, int rows, String asset) {
		this.item = item;
		this.slot = slot;
		this.rows = rows;
		this.asset = asset;
	}

	/** The next project in the needles' cycle. */
	public Knitwear next() {
		return values()[(ordinal() + 1) % values().length];
	}

	/** The garment {@code item} is, or null. */
	public static @Nullable Knitwear of(Item item) {
		for (Knitwear knit : values()) {
			if (JugcraftAgriculture.item(knit.item) == item) {
				return knit;
			}
		}
		return null;
	}

	/** The project at {@code index}, the first for anything out of range. */
	public static Knitwear byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : BEANIE;
	}
}
