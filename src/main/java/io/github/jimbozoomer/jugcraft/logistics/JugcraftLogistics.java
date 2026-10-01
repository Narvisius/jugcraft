package io.github.jimbozoomer.jugcraft.logistics;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Registers item logistics: brass item pipes, the pneumatic extractor, the item sorter, conveyors and
 * the brass wrench. Machines' own side configuration and ejecting live in the machine package.
 */
public final class JugcraftLogistics {
	public static Block BRASS_ITEM_PIPE;
	public static Block PNEUMATIC_EXTRACTOR;
	public static Block HIGH_PRESSURE_EXTRACTOR;
	public static Block ITEM_SORTER;
	public static Item BRASS_WRENCH;
	public static Block CONVEYOR;
	public static Block CONVEYOR_SPLITTER;
	public static Block CONVEYOR_SLOPE;
	public static BlockEntityType<ItemSorterBlockEntity> SORTER_ENTITY;
	public static BlockEntityType<ConveyorBlockEntity> CONVEYOR_ENTITY;

	private JugcraftLogistics() {
	}

	public static void register() {
		BRASS_ITEM_PIPE = block("brass_item_pipe", new ItemPipeBlock(properties("brass_item_pipe",
				BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.0F).sound(SoundType.COPPER).noOcclusion())));
		PNEUMATIC_EXTRACTOR = block("pneumatic_extractor", new PneumaticExtractorBlock(properties("pneumatic_extractor",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion()),
				PneumaticExtractorBlock.INTERVAL, PneumaticExtractorBlock.ITEMS_PER_PULL));
		HIGH_PRESSURE_EXTRACTOR = block("high_pressure_extractor", new PneumaticExtractorBlock(properties("high_pressure_extractor",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F).noOcclusion()),
				PneumaticExtractorBlock.HIGH_PRESSURE_INTERVAL, PneumaticExtractorBlock.HIGH_PRESSURE_ITEMS));
		ITEM_SORTER = block("item_sorter", new ItemSorterBlock(properties("item_sorter",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F).noOcclusion())));
		CONVEYOR = block("conveyor", new ConveyorBlock(properties("conveyor",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()), false));
		CONVEYOR_SPLITTER = block("conveyor_splitter", new ConveyorBlock(properties("conveyor_splitter",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion()), true));
		CONVEYOR_SLOPE = block("conveyor_slope", new ConveyorSlopeBlock(properties("conveyor_slope",
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F).noOcclusion())));

		ResourceKey<Item> wrenchKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("brass_wrench"));
		BRASS_WRENCH = Registry.register(BuiltInRegistries.ITEM, wrenchKey,
				new BrassWrenchItem(new Item.Properties().setId(wrenchKey).stacksTo(1)));

		SORTER_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("item_sorter"),
				FabricBlockEntityTypeBuilder.create(ItemSorterBlockEntity::new, ITEM_SORTER).build());
		ItemStorage.SIDED.registerForBlockEntity(ItemSorterBlockEntity::itemsFor, SORTER_ENTITY);
		CONVEYOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("conveyor"),
				FabricBlockEntityTypeBuilder.create(ConveyorBlockEntity::new, CONVEYOR, CONVEYOR_SPLITTER, CONVEYOR_SLOPE).build());
		ItemStorage.SIDED.registerForBlockEntity((conveyor, side) -> conveyor.storage(), CONVEYOR_ENTITY);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> {
			output.accept(BRASS_ITEM_PIPE);
			output.accept(PNEUMATIC_EXTRACTOR);
			output.accept(HIGH_PRESSURE_EXTRACTOR);
			output.accept(ITEM_SORTER);
			output.accept(CONVEYOR);
			output.accept(CONVEYOR_SPLITTER);
			output.accept(CONVEYOR_SLOPE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(BRASS_WRENCH));
	}

	private static BlockBehaviour.Properties properties(String path, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(path), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}
}
