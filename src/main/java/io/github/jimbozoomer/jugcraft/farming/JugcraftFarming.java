package io.github.jimbozoomer.jugcraft.farming;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Farming (batch 9, docs/features/farming.md): the sprinkler and the cotton crop. (The crop harvester is a machine;
 * see MachineKind.)
 */
public final class JugcraftFarming {
	public static Block SPRINKLER;
	/** The cotton crop block; it has no item of its own: cotton seeds plant it. */
	public static Block COTTON_CROP;
	public static Item COTTON_SEEDS;
	public static Item COTTON;
	public static BlockEntityType<SprinklerBlockEntity> SPRINKLER_ENTITY;

	private JugcraftFarming() {
	}

	public static void register() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("sprinkler"));
		SPRINKLER = Registry.register(BuiltInRegistries.BLOCK, blockKey, new SprinklerBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).setId(blockKey).noOcclusion()));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("sprinkler"));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(SPRINKLER, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		SPRINKLER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("sprinkler"),
				FabricBlockEntityTypeBuilder.create(SprinklerBlockEntity::new, SPRINKLER).build());
		FluidStorage.SIDED.registerForBlockEntity((sprinkler, side) -> sprinkler.water, SPRINKLER_ENTITY);
		ItemStorage.SIDED.registerForBlockEntity((sprinkler, side) -> sprinkler.hopper, SPRINKLER_ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(SPRINKLER));

		ResourceKey<Block> cropKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("cotton_crop"));
		COTTON_CROP = Registry.register(BuiltInRegistries.BLOCK, cropKey,
				new CottonCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).setId(cropKey)));
		ResourceKey<Item> seedsKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("cotton_seeds"));
		COTTON_SEEDS = Registry.register(BuiltInRegistries.ITEM, seedsKey,
				new BlockItem(COTTON_CROP, new Item.Properties().setId(seedsKey)));
		ResourceKey<Item> cottonKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("cotton"));
		COTTON = Registry.register(BuiltInRegistries.ITEM, cottonKey, new Item(new Item.Properties().setId(cottonKey)));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> output.accept(COTTON_SEEDS));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> output.accept(COTTON));
	}
}
