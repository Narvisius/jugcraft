package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Fortifications (batch 55, docs/features/fortifications.md): bastion concrete (with a wall), the parapet, the steel
 * ladder, the blast door, the ammo hoist and the ready rack. Keep the list, kinds, strengths and numbers in sync with
 * tools/fortifications.py; tools/check_mod_data.py checks them.
 */
public final class Fortifications {
	/** The ammo hoist: ticks between lifts, items each lift carries, and how many items each hoist block holds. */
	public static final int HOIST_INTERVAL = 8;
	public static final int HOIST_BATCH = 4;
	public static final int HOIST_BUFFER = 16;
	/** The ready rack: its slots, and how far beyond a gun's own size (in blocks) a rack can stand and still feed it. */
	public static final int RACK_SLOTS = 9;
	public static final int RACK_REACH = 2;
	/** The shells a ready rack takes. */
	public static final TagKey<Item> SHELLS = TagKey.create(Registries.ITEM, Jugcraft.id("artillery_shells"));
	/** Every block, by id, in registration order (slab, stairs and wall included). */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static BlockEntityType<AmmoHoistBlock.Entity> HOIST_ENTITY;
	public static BlockEntityType<ReadyRackBlock.Entity> RACK_ENTITY;
	private static final List<String> TOOLTIPS = List.of("bastion_concrete", "bastion_parapet", "steel_ladder", "blast_door",
			"ammo_hoist", "ready_rack", "bunker_door", "sliding_gate", "bastion_parapet_corner", "bastion_embrasure");

	private Fortifications() {
	}

	public static void register() {
		entry("bastion_concrete", "bastion", 4.0F, 24.0F);
		entry("bastion_parapet", "parapet", 4.0F, 24.0F);
		entry("steel_ladder", "ladder", 1.5F, 6.0F);
		entry("blast_door", "door", 15.0F, 1200.0F);
		entry("ammo_hoist", "hoist", 3.0F, 6.0F);
		entry("ready_rack", "rack", 2.5F, 6.0F);
		entry("bunker_door", "door_wood", 4.0F, 12.0F);
		entry("sliding_gate", "gate", 6.0F, 24.0F);
		entry("bastion_parapet_corner", "parapet_corner", 4.0F, 24.0F);
		entry("bastion_embrasure", "embrasure", 4.0F, 24.0F);
		HOIST_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("ammo_hoist"),
				FabricBlockEntityTypeBuilder.create(AmmoHoistBlock.Entity::new, BLOCKS.get("ammo_hoist")).build());
		RACK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("ready_rack"),
				FabricBlockEntityTypeBuilder.create(ReadyRackBlock.Entity::new, BLOCKS.get("ready_rack")).build());
		// Pipes, hoppers and conveyors can load a hoist (never take from one, so a hopper under a shaft cannot rob it) and
		// can stock or empty a ready rack.
		ItemStorage.SIDED.registerForBlockEntity((hoist, side) -> FilteringStorage.insertOnlyOf(hoist.buffer), HOIST_ENTITY);
		ItemStorage.SIDED.registerForBlockEntity((rack, side) -> ContainerStorage.of(rack.shells, side), RACK_ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
	}

	private static void entry(String id, String kind, float hardness, float blast) {
		switch (kind) {
			case "bastion" -> {
				BlockBehaviour.Properties base = BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE)
						.requiresCorrectToolForDrops().strength(hardness, blast);
				Block full = block(id, new Block(properties(id, base)));
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
				block(id + "_stairs", new StairBlock(full.defaultBlockState(),
						properties(id + "_stairs", BlockBehaviour.Properties.ofFullCopy(full))));
				block(id + "_wall", new WallBlock(properties(id + "_wall", BlockBehaviour.Properties.ofFullCopy(full).forceSolidOn())));
			}
			case "parapet" -> block(id, new ParapetBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
					.sound(SoundType.STONE).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion())));
			case "ladder" -> block(id, new LadderBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER)
					.mapColor(MapColor.METAL).sound(SoundType.METAL).strength(hardness, blast))));
			// A door places its own upper half (DoorBlock.setPlacedBy), so a plain block item will do.
			case "door" -> block(id, new DoorBlock(BlockSetType.IRON, properties(id, BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()
					.strength(hardness, blast).noOcclusion().pushReaction(PushReaction.IMMOVEABLE))));
			case "hoist" -> block(id, new AmmoHoistBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion())));
			case "rack" -> block(id, new ReadyRackBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
					.sound(SoundType.METAL).strength(hardness, blast).noOcclusion())));
			// Fortification extras: a wooden door opened by hand, a redstone sliding gate, a parapet corner and an embrasure.
			case "door_wood" -> block(id, new DoorBlock(BlockSetType.SPRUCE, properties(id, BlockBehaviour.Properties.of()
					.mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(hardness, blast).noOcclusion().ignitedByLava())));
			case "gate" -> block(id, new SlidingGateBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion()
					.pushReaction(PushReaction.IMMOVEABLE))));
			case "parapet_corner" -> block(id, new ParapetCornerBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
					.sound(SoundType.STONE).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion())));
			case "embrasure" -> block(id, new EmbrasureBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
					.sound(SoundType.STONE).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion())));
			default -> throw new IllegalArgumentException(kind);
		}
	}

	private static BlockBehaviour.Properties properties(String id, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(id)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)
				.useBlockDescriptionPrefix()) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				if (TOOLTIPS.contains(path)) {
					tooltip.accept(Component.translatable("tooltip.jugcraft." + path).withStyle(ChatFormatting.GRAY));
				}
			}
		});
		BLOCKS.put(path, block);
		return block;
	}
}
