package io.github.jimbozoomer.jugcraft;

import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Compile-time guard: Jugcraft recipes use these vanilla items by ID
 * (minecraft:sulfur from 26.2, minecraft:quartz). If Mojang renames or removes one,
 * compileTestJava fails instead of the recipe silently failing to load in game.
 */
final class VanillaReferences {
	static final Item[] USED_BY_RECIPES = {Items.SULFUR, Items.QUARTZ, Items.COPPER_INGOT,
			// Ore processing: sieve, sawmill and byproducts (tools/machines.py).
			Items.GRAVEL, Items.FLINT, Items.SOUL_SAND, Items.SOUL_SOIL, Items.IRON_NUGGET, Items.GOLD_NUGGET,
			Items.STICK, Items.PAPER, Items.IRON_BARS, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS,
			Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS,
			Items.PALE_OAK_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS, Items.BAMBOO_PLANKS,
			// Steel tier.
			Items.COAL, Items.BRICKS, Items.FURNACE, Items.BLAST_FURNACE, Items.HOPPER, Items.IRON_INGOT};

	/** Vanilla item tags the sawmill recipes name (WOODS in tools/machines.py). */
	static final List<TagKey<Item>> TAGS_USED_BY_RECIPES = List.of(ItemTags.PLANKS, ItemTags.OAK_LOGS, ItemTags.SPRUCE_LOGS,
			ItemTags.BIRCH_LOGS, ItemTags.JUNGLE_LOGS, ItemTags.ACACIA_LOGS, ItemTags.DARK_OAK_LOGS, ItemTags.MANGROVE_LOGS,
			ItemTags.CHERRY_LOGS, ItemTags.PALE_OAK_LOGS, ItemTags.CRIMSON_STEMS, ItemTags.WARPED_STEMS, ItemTags.BAMBOO_BLOCKS);

	private VanillaReferences() {
	}
}
