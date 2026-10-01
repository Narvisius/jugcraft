package io.github.jimbozoomer.jugcraft.electronics;

import io.github.jimbozoomer.jugcraft.Jugcraft;
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
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The electronics tier's blocks outside the machine framework (batch 7, docs/features/electronics.md): the network
 * terminal. The crystal grower and lithography station are machines (see MachineKind); wafers, chips and
 * processors are items (PetroItems, JugcraftComponents).
 */
public final class JugcraftElectronics {
	public static Block NETWORK_TERMINAL;

	private JugcraftElectronics() {
	}

	public static void register() {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id("network_terminal"));
		NETWORK_TERMINAL = Registry.register(BuiltInRegistries.BLOCK, blockKey, new NetworkTerminalBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(blockKey).noOcclusion()));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id("network_terminal"));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(NETWORK_TERMINAL, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(NETWORK_TERMINAL));
	}
}
