package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Bunker and trench interiors (batch 59, docs/features/bunker-interiors.md): the Trench Periscope, the Map Table, the Gas
 * Curtain, the Field Kitchen, corrugated iron, timber shoring, the bunker lamp and the bunk. Keep the list, kinds,
 * strengths and numbers in sync with tools/bunkerworks.py; tools/check_mod_data.py checks them.
 */
public final class Bunkerworks {
	/** The trench periscope: ticks between its scans, how far it sees (blocks) and its cone's half-angle (degrees). */
	public static final int PERISCOPE_INTERVAL = 20;
	public static final int PERISCOPE_RANGE = 64;
	public static final int PERISCOPE_CONE = 45;
	/** The map table: how far from it (blocks) the marks it lists may point, how near a fire control table must stand. */
	public static final int MAP_RANGE = 256;
	public static final int MAP_LINK = 4;
	/** How many marks the map table lists at most. */
	public static final int MAP_LINES = 5;
	/** The field kitchen's light while it burns, and the bunker lamp's. */
	public static final int KITCHEN_LIGHT = 13;
	public static final int LAMP_LIGHT = 14;
	/** Every block, by id, in registration order (slab and stairs included). */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static BlockEntityType<FieldKitchenBlock.Entity> KITCHEN_ENTITY;
	private static final List<String> TOOLTIPS = List.of("trench_periscope", "map_table", "gas_curtain", "field_kitchen",
			"corrugated_iron", "timber_shoring", "bunker_lamp", "bunker_bunk");

	private Bunkerworks() {
	}

	public static void register() {
		entry("trench_periscope", "periscope", 2.5F, 6.0F);
		entry("map_table", "map_table", 2.5F, 3.0F);
		entry("gas_curtain", "curtain", 0.8F, 0.8F);
		entry("field_kitchen", "kitchen", 3.5F, 6.0F);
		entry("corrugated_iron", "family", 3.0F, 6.0F);
		entry("timber_shoring", "pillar", 2.0F, 3.0F);
		entry("bunker_lamp", "lamp", 3.5F, 3.5F);
		entry("bunker_bunk", "bunk", 2.0F, 2.0F);
		KITCHEN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("field_kitchen"),
				FabricBlockEntityTypeBuilder.create(FieldKitchenBlock.Entity::new, BLOCKS.get("field_kitchen")).build());
		// Pipes, hoppers and conveyors can stock a field kitchen with fuel, never take it out.
		ItemStorage.SIDED.registerForBlockEntity((kitchen, side) -> FilteringStorage.insertOnlyOf(kitchen.fuel), KITCHEN_ENTITY);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
	}

	private static void entry(String id, String kind, float hardness, float blast) {
		switch (kind) {
			case "periscope" -> block(id, new TrenchPeriscopeBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion()
					.pushReaction(PushReaction.POPPED))));
			case "map_table" -> block(id, new MapTableBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
					.sound(SoundType.WOOD).strength(hardness, blast).noOcclusion().ignitedByLava())));
			case "curtain" -> block(id, new GasCurtainBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
					.sound(SoundType.WOOL).strength(hardness, blast).noCollision().noOcclusion().ignitedByLava()
					.pushReaction(PushReaction.POPPED))));
			case "kitchen" -> block(id, new FieldKitchenBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
					.sound(SoundType.METAL).requiresCorrectToolForDrops().strength(hardness, blast).noOcclusion()
					.lightLevel(state -> state.getValue(FieldKitchenBlock.LIT) ? KITCHEN_LIGHT : 0))));
			case "family" -> {
				Block full = block(id, new Block(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
						.sound(SoundType.METAL).requiresCorrectToolForDrops().strength(hardness, blast))));
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
				block(id + "_stairs", new StairBlock(full.defaultBlockState(),
						properties(id + "_stairs", BlockBehaviour.Properties.ofFullCopy(full))));
			}
			case "pillar" -> block(id, new RotatedPillarBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
					.sound(SoundType.WOOD).strength(hardness, blast).ignitedByLava())));
			case "lamp" -> block(id, new LanternBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
					.mapColor(MapColor.METAL).strength(hardness, blast).lightLevel(state -> LAMP_LIGHT))));
			case "bunk" -> block(id, new BunkerBunkBlock(properties(id, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
					.sound(SoundType.WOOD).strength(hardness, blast).noOcclusion().ignitedByLava()
					.pushReaction(PushReaction.POPPED))));
			default -> throw new IllegalArgumentException(kind);
		}
	}

	/** The compass point (N, NE, E ... NW) of a horizontal offset: north is -z, east is +x. */
	public static String bearing(double dx, double dz) {
		double degrees = Math.toDegrees(Math.atan2(dx, -dz));
		int point = Math.floorMod((int) Math.round(degrees / 45.0), 8);
		return COMPASS[point];
	}

	private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private static BlockBehaviour.Properties properties(String id, BlockBehaviour.Properties properties) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(id)));
	}

	private static Block block(String path, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		// The two-block-tall ones (periscope, curtain, bunk) place their upper half themselves (TallDecorationBlock).
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
