package io.github.jimbozoomer.jugcraft.deposit;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Surface resource deposits (docs/features/resource-deposits.md): patches of deposit blocks on stony hills that only a
 * deposit drill can work. Mirrors DEPOSITS in tools/deposits.py. The block items exist for creative building and
 * commands; survival players never get one, since a broken deposit drops nothing.
 */
public final class JugcraftDeposits {
	/** Deposit block id to the item each unit gives. */
	public static final Map<String, Identifier> DEPOSITS = new LinkedHashMap<>();
	public static final Map<String, DepositBlock> BLOCKS = new LinkedHashMap<>();

	static {
		DEPOSITS.put("coal_deposit", Identifier.fromNamespaceAndPath("minecraft", "coal"));
		DEPOSITS.put("iron_deposit", Identifier.fromNamespaceAndPath("minecraft", "raw_iron"));
		DEPOSITS.put("copper_deposit", Identifier.fromNamespaceAndPath("minecraft", "raw_copper"));
		DEPOSITS.put("tin_deposit", Jugcraft.id("raw_tin"));
	}

	private JugcraftDeposits() {
	}

	public static void register() {
		DEPOSITS.forEach((name, yield) -> {
			ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
			// As slow to break as obsidian and as blast-proof, so a patch is not lost by accident.
			DepositBlock block = Registry.register(BuiltInRegistries.BLOCK, blockKey, new DepositBlock(yield,
					BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).setId(blockKey).strength(50.0F, 1200.0F)));
			BLOCKS.put(name, block);
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
			Registry.register(BuiltInRegistries.ITEM, itemKey,
					new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> BLOCKS.values().forEach(output::accept));
	}
}
