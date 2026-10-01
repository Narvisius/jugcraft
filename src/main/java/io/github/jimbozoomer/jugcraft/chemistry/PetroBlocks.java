package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Blocks of the oil line (BLOCKS in tools/petro.py): asphalt road, a slab of it and a road with a yellow centre line.
 * Walking on any of them is {@link #ASPHALT_SPEED} times as fast; the line turns to face the player who places it.
 */
public final class PetroBlocks {
	/** Movement speed on asphalt, as a multiple of normal (soul sand is 0.4). */
	public static final float ASPHALT_SPEED = 1.3F;

	public static Block ASPHALT;
	public static Block ASPHALT_SLAB;
	public static Block ASPHALT_ROAD_LINE;

	private PetroBlocks() {
	}

	public static void register() {
		ASPHALT = register("asphalt", Block::new);
		ASPHALT_SLAB = register("asphalt_slab", SlabBlock::new);
		ASPHALT_ROAD_LINE = register("asphalt_road_line", GlazedTerracottaBlock::new);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> {
			output.accept(ASPHALT);
			output.accept(ASPHALT_SLAB);
			output.accept(ASPHALT_ROAD_LINE);
		});
	}

	/** A stone-like block (pickaxe, needs a tool to drop) that speeds up walking. */
	private static Block register(String path, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(path));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				factory.apply(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).speedFactor(ASPHALT_SPEED).setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
