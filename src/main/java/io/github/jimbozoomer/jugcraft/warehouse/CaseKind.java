package io.github.jimbozoomer.jugcraft.warehouse;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * What a case of an item looks like (tools/warehouse_data.py CASE_KINDS; a pallet of them, its load): drums for
 * liquids, sacks for powders, seeds and grit, crates for tools, armour and anything else that doesn't stack, banded
 * bundles for ingots, white cartons for food, kraft cartons for everything else.
 */
public enum CaseKind {
	CARTON, CARTON_W, SACK, CRATE, DRUM, INGOT;

	private static final java.util.Set<String> SACKED = java.util.Set.of("sugar", "gunpowder", "wheat", "sand", "red_sand", "gravel",
			"clay_ball", "nether_wart", "cocoa_beans", "flour", "salt", "bone_meal", "glowstone_dust", "redstone");

	public static CaseKind of(ItemStack stack) {
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if (path.endsWith("_bucket") || path.contains("potion") || path.equals("honey_bottle") || path.equals("experience_bottle")
				|| stack.getItem() instanceof DrumItem) {
			return DRUM;
		}
		if (stack.getMaxStackSize() == 1) {
			return CRATE;
		}
		if (path.endsWith("_ingot") || path.endsWith("_nugget") || stack.is(ConventionalItemTags.INGOTS) || stack.is(ConventionalItemTags.NUGGETS)) {
			return INGOT;
		}
		if (SACKED.contains(path) || path.endsWith("seeds") || path.endsWith("_dust") || path.endsWith("_powder") || path.endsWith("_meal")) {
			return SACK;
		}
		if (stack.has(DataComponents.FOOD)) {
			return CARTON_W;
		}
		return CARTON;
	}

	/** Its place in the layout's case kinds (the warehouse_part case models: case_carton, case_carton_w...). */
	public String model() {
		return "case_" + name().toLowerCase(java.util.Locale.ROOT);
	}
}
