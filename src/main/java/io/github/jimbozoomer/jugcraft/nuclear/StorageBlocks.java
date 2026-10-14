package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * The blocks for keeping radioactive material (round 14; the radwaste building in each plant is built of them,
 * tools/radwaste_store.py): the lead pig, lead-lined locker and shielded waste drum (ShieldedStorageBlock), the
 * lead-lined door (LeadDoorBlock), lead glass, and the warning beacon (WarningBeaconBlock).
 */
public final class StorageBlocks {
	/** The lead-lined door: opens by hand, heavy steel door sounds. */
	public static final BlockSetType LEAD = new BlockSetType("jugcraft_lead", true, true, false, BlockSetType.PressurePlateSensitivity.EVERYTHING,
			SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN,
			SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF,
			SoundEvents.STONE_BUTTON_CLICK_ON);
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static BlockEntityType<ShieldedStorageBlockEntity> STORE_ENTITY;
	public static Block LEAD_DOOR, LEAD_GLASS, WARNING_BEACON;

	private StorageBlocks() {
	}

	public static void register() {
		for (ShieldedStorageBlock.Kind kind : ShieldedStorageBlock.Kind.values()) {
			BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 12F).noOcclusion();
			block(kind.id(), p -> new ShieldedStorageBlock(kind, p), props, true);
		}
		LEAD_DOOR = block("lead_door", p -> new LeadDoorBlock(LEAD, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(5.0F, 12F).noOcclusion(), false);
		LEAD_GLASS = block("lead_glass", p -> new TransparentBlock(p) {
		}, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(3.0F, 12F), true);
		WARNING_BEACON = block("warning_beacon", WarningBeaconBlock::new,
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.0F).noOcclusion().lightLevel(state -> 10), true);
		ResourceKey<Item> doorItem = ResourceKey.create(Registries.ITEM, Jugcraft.id("lead_door"));
		Registry.register(BuiltInRegistries.ITEM, doorItem, new DoubleHighBlockItem(LEAD_DOOR, new Item.Properties().setId(doorItem).useBlockDescriptionPrefix()));
		STORE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("shielded_storage"),
				FabricBlockEntityTypeBuilder.create(ShieldedStorageBlockEntity::new, BLOCKS.get("lead_pig"), BLOCKS.get("lead_locker"),
						BLOCKS.get("waste_drum")).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> BLOCKS.values().forEach(output::accept));
	}

	private static Block block(String id, Function<BlockBehaviour.Properties, Block> make, BlockBehaviour.Properties props, boolean item) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(id));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, make.apply(props.setId(key)));
		if (item) {
			ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(id));
			Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		}
		BLOCKS.put(id, block);
		return block;
	}
}
