package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
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
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Trench works (batch 50, docs/features/trench-works.md): sandbags, timber revetment, duckboards, barbed wire, field
 * telephones and searchlights. Keep the list, kinds, strengths and numbers in sync with tools/trenchworks.py;
 * tools/check_mod_data.py checks them.
 */
public final class Trenchworks {
	/** Barbed wire: the fraction of normal speed kept while in it, and the damage each cut does. */
	public static final double WIRE_SLOW = 0.35;
	public static final float WIRE_DAMAGE = 1.0F;
	/** How far a field telephone's ring carries, in blocks. */
	public static final int PHONE_RANGE = 256;
	/** The searchlight: yaw steps in a full turn, the highest tilt step (15 degrees each) and its light. */
	public static final int SEARCHLIGHT_YAWS = 16;
	public static final int SEARCHLIGHT_TILTS = 5;
	public static final int SEARCHLIGHT_LIGHT = 15;
	/** Every block, by id, in registration order (slabs and stairs included). */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	public static BlockEntityType<SearchlightBlock.Entity> SEARCHLIGHT_ENTITY;
	private static final List<String> TOOLTIPS = List.of("sandbags", "duckboard", "barbed_wire", "field_telephone", "searchlight");

	private Trenchworks() {
	}

	public static void register() {
		entry("sandbags", "family", 2.0F, 12.0F);
		entry("timber_revetment", "family", 2.0F, 3.0F);
		entry("duckboard", "duckboard", 1.0F, 3.0F);
		entry("barbed_wire", "wire", 2.0F, 6.0F);
		entry("field_telephone", "telephone", 1.5F, 6.0F);
		entry("searchlight", "searchlight", 2.5F, 6.0F);
		SEARCHLIGHT_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("searchlight"),
				FabricBlockEntityTypeBuilder.create(SearchlightBlock.Entity::new, BLOCKS.get("searchlight")).build());
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
	}

	private static void entry(String id, String kind, float hardness, float blast) {
		switch (kind) {
			case "family" -> {
				boolean sand = id.equals("sandbags");
				BlockBehaviour.Properties base = sand
						? BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.WOOL)
						: BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.WOOD);
				Block full = block(id, new Block(properties(id, base.strength(hardness, blast))));
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
				block(id + "_stairs", new StairBlock(full.defaultBlockState(),
						properties(id + "_stairs", BlockBehaviour.Properties.ofFullCopy(full))));
			}
			case "duckboard" -> block(id, new DuckboardBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
					.strength(hardness, blast).noOcclusion())));
			case "wire" -> block(id, new BarbedWireBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)
					.strength(hardness, blast).noCollision().noOcclusion())));
			case "telephone" -> block(id, new FieldTelephoneBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.mapColor(MapColor.COLOR_GREEN).strength(hardness, blast).noOcclusion())));
			case "searchlight" -> block(id, new SearchlightBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
					.mapColor(MapColor.COLOR_BLACK).strength(hardness, blast).noOcclusion()
					.lightLevel(state -> state.getValue(SearchlightBlock.LIT) ? SEARCHLIGHT_LIGHT : 0))));
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
