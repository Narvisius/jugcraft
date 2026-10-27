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
 * Storage blocks outside the machine framework: bulk and mixed-item crates. (The capacitor bank and steel tank
 * are multi-block machines; see MachineKind.)
 */
public final class JugcraftStorage {
	public static Block ITEM_CRATE;
	public static BlockEntityType<CrateBlockEntity> CRATE_ENTITY;
	public static final java.util.Map<StorageCrateTier, StorageCrateBlock> STORAGE_CRATES = new java.util.EnumMap<>(StorageCrateTier.class);
	public static final java.util.Map<StorageCrateTier, Item> STORAGE_UPGRADES = new java.util.EnumMap<>(StorageCrateTier.class);
	public static BlockEntityType<StorageCrateBlockEntity> STORAGE_CRATE_ENTITY;
	public static net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<StorageCrateMenu, Integer> STORAGE_CRATE_MENU;

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
		registerStorageCrates();
	}

	private static void registerStorageCrates() {
		for (var tier : StorageCrateTier.values()) {
			var bk = ResourceKey.create(Registries.BLOCK, Jugcraft.id(tier.id()));
			var block = Registry.register(BuiltInRegistries.BLOCK, bk, new StorageCrateBlock(
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).setId(bk).strength(3.5F, 6.0F)
					.pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE), tier));
			STORAGE_CRATES.put(tier, block);
			var ik = ResourceKey.create(Registries.ITEM, Jugcraft.id(tier.id()));
			Registry.register(BuiltInRegistries.ITEM, ik, new BlockItem(block, new Item.Properties().setId(ik).useBlockDescriptionPrefix()) {
				@Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
					net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip,
					net.minecraft.world.item.TooltipFlag flag) {
					tooltip.accept(net.minecraft.network.chat.Component.translatable("tooltip.jugcraft.storage_crate.capacity", tier.slots, tier.slots / 27));
				}
			});
			if (tier != StorageCrateTier.BRONZE) {
				var kit = ResourceKey.create(Registries.ITEM, Jugcraft.id(tier.kitId()));
				STORAGE_UPGRADES.put(tier, Registry.register(BuiltInRegistries.ITEM, kit, new StorageCrateUpgradeItem(new Item.Properties().setId(kit), tier)));
			}
		}
		STORAGE_CRATE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("storage_crate"),
			FabricBlockEntityTypeBuilder.create(StorageCrateBlockEntity::new, STORAGE_CRATES.values().toArray(Block[]::new)).build());
		STORAGE_CRATE_MENU = Registry.register(BuiltInRegistries.MENU, Jugcraft.id("storage_crate"),
			new net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<>(StorageCrateMenu::new, net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
		// Fabric's existing sided-container fallback exposes WorldlyContainer to pipes and hoppers.
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
			for (var tier : StorageCrateTier.values()) { out.accept(STORAGE_CRATES.get(tier)); if (STORAGE_UPGRADES.containsKey(tier)) out.accept(STORAGE_UPGRADES.get(tier)); }
		});
	}
}
