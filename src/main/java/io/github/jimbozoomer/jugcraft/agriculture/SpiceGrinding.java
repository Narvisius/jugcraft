package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.item.ItemStack;

/**
 * Grinding spices with the Mortar and Pestle (garden crops, herbs and spices, part b; tools/spices.py GRIND): the
 * Concordance's mortar ({@code jugcraft:mortar}, concordance/MortarItem), used with a Dried Chili in the other hand, grinds
 * it into {@value #COUNT} Paprika. Kitchen grinding needs no alchemy: anyone may grind a chili.
 */
public final class SpiceGrinding {
	public static final String INPUT = "dried_chili";
	public static final String RESULT = "paprika";
	public static final int COUNT = 2;

	private SpiceGrinding() {
	}

	/** Whether the mortar grinds {@code stack} into a spice. */
	public static boolean grinds(ItemStack stack) {
		return !stack.isEmpty() && stack.is(JugcraftAgriculture.item(INPUT));
	}

	/** What one ground piece gives. */
	public static ItemStack result() {
		return new ItemStack(JugcraftAgriculture.item(RESULT), COUNT);
	}
}
