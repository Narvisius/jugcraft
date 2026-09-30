package io.github.jimbozoomer.jugcraft;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Compile-time guard: Jugcraft recipes use these vanilla items by ID
 * (minecraft:sulfur from 26.2, minecraft:quartz). If Mojang renames or removes one,
 * compileTestJava fails instead of the recipe silently failing to load in game.
 */
final class VanillaReferences {
	static final Item[] USED_BY_RECIPES = {Items.SULFUR, Items.QUARTZ, Items.COPPER_INGOT};

	private VanillaReferences() {
	}
}
