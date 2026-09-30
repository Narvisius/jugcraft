package io.github.jimbozoomer.jugcraft.storage;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Storage blocks outside the machine framework: the item crate. (The capacitor bank and steel tank
 * are multi-block machines; see MachineKind.)
 */
public final class JugcraftStorage {
	public static Block ITEM_CRATE;
	public static BlockEntityType<CrateBlockEntity> CRATE_ENTITY;

	private JugcraftStorage() {
	}

	public static void register() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("item_crate"));
		ITEM_CRATE = Registry.register(BuiltInRegistries.BLOCK, blockKey, new CrateBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).setId(blockKey).noOcclusion()));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("item_crate"));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(ITEM_CRATE, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		CRATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("item_crate"),
				FabricBlockEntityTypeBuilder.create(CrateBlockEntity::new, ITEM_CRATE).build());
		ItemStorage.SIDED.registerForBlockEntity((crate, side) -> crate.storage, CRATE_ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(ITEM_CRATE));
	}
}
