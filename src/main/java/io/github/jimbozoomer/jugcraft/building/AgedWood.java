package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * Aged wood (docs/features/aged-wood.md): pine and maple planks darkened with nitric acid in the Chemical Reactor, each
 * with its stairs and slab. They are plain oak-like planks in every way but the look. Keep the woods in sync with
 * tools/aged_wood.py; tools/check_mod_data.py checks them.
 */
public final class AgedWood {
	/** The woods that age (tools/aged_wood.py WOODS). */
	public static final List<String> WOODS = List.of("pine", "maple");
	/** Every block, by id, in registration order. */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

	private AgedWood() {
	}

	public static void register() {
		woodSet("pine", MapColor.COLOR_BROWN);
		woodSet("maple", MapColor.TERRACOTTA_BROWN);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
	}

	private static void woodSet(String wood, MapColor color) {
		String id = "aged_" + wood;
		Block planks = block(id + "_planks", new Block(properties(id + "_planks",
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(color))));
		Block stairs = block(id + "_stairs", new StairBlock(planks.defaultBlockState(), properties(id + "_stairs",
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(color))));
		Block slab = block(id + "_slab", new SlabBlock(properties(id + "_slab",
				BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(color))));
		// Vanilla oak planks' fire behaviour.
		for (Block block : List.of(planks, stairs, slab)) {
			FlammableBlockRegistry.getDefaultInstance().add(block, 5, 20);
		}
	}

	private static BlockBehaviour.Properties properties(String id, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(id)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		// Furnace fuel like oak: a slab burns half as long as a block.
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)
				.useBlockDescriptionPrefix().cookingFuel(path.endsWith("_slab") ? ContextIntProviders.COOKING_TIME_WOOD_SLABS
						: ContextIntProviders.COOKING_TIME_WOOD_BLOCKS)));
		BLOCKS.put(path, block);
		return block;
	}
}
