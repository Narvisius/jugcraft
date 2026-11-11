package io.github.jimbozoomer.jugcraft.exterior;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.warehouse.HiddenBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The realistic building exterior (Justin, 2026-10-17: "realism max"; tools/exterior_data.py, exterior_kit.py):
 * insulated wall panels, the precast plinth, the parapet and its coping, the membrane roof deck, rooflights, smoke
 * vents, the roof hatch, door canopies, yard concrete, steel stairs at a real angle and their landings. IDs match
 * tools/exterior_data.py.
 */
public final class JugcraftExterior {
	public static final List<Item> ITEMS = new ArrayList<>();
	public static Block WALL_PANEL, WALL_PLINTH, PARAPET, ROOF_DECK, ROOFLIGHT, SMOKE_VENT, ROOF_HATCH, DOOR_CANOPY, YARD_CONCRETE;
	public static Block STEEL_STAIR, STAIR_LANDING, STAIR_GUARD, PART;
	public static BlockEntityType<StairBlockEntity> STAIR_ENTITY;
	/** The stair renderer's parts (the hidden exterior_part block's PART values; tools/exterior_data.py PART_NAMES). */
	public static final List<String> PARTS = List.of("stringer_l", "stringer_r", "rail_wall_l", "rail_wall_r", "rail_guard_l", "rail_guard_r",
			"knee_l", "knee_r", "post_l", "post_r", "bracket_l", "bracket_r");

	private JugcraftExterior() {
	}

	public static void register() {
		registerContent();
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> ITEMS.forEach(output::accept));
	}

	/** The blocks, items and block entity (all the startup check needs). */
	public static void registerContent() {
		BlockBehaviour.Properties cladding = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.METAL).strength(2.5F, 6.0F)
				.sound(SoundType.METAL);
		BlockBehaviour.Properties concrete = BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).mapColor(MapColor.STONE).strength(2.0F, 8.0F);
		WALL_PANEL = block("wall_panel", WallPanelBlock::new, cladding.lightLevel(s -> s.getValue(WallPanelBlock.LIT)
				&& s.getValue(WallPanelBlock.SHAPE) == WallPanelBlock.Shape.LIGHT ? 14 : 0), "wall_panel");
		WALL_PLINTH = block("wall_plinth", WallPlinthBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_STONE).strength(2.5F, 9.0F),
				"wall_plinth");
		PARAPET = block("parapet", CopingParapetBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.5F, 6.0F)
				.sound(SoundType.METAL), "parapet");
		ROOF_DECK = block("roof_deck", RoofDeckBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F)
				.sound(SoundType.METAL).noOcclusion(), null);
		BlockBehaviour.Properties opening = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).sound(SoundType.METAL)
				.noOcclusion();
		ROOFLIGHT = block("rooflight", p -> new RoofOpeningBlock(RoofOpeningBlock.Kind.ROOFLIGHT, p), opening, "rooflight");
		SMOKE_VENT = block("smoke_vent", p -> new RoofOpeningBlock(RoofOpeningBlock.Kind.SMOKE_VENT, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), "smoke_vent");
		ROOF_HATCH = block("roof_hatch", p -> new RoofOpeningBlock(RoofOpeningBlock.Kind.HATCH, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), null);
		DOOR_CANOPY = block("door_canopy", p -> new ShapedFacingBlock(ShapedFacingBlock.Kind.CANOPY, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion(), null);
		YARD_CONCRETE = block("yard_concrete", Block::new, concrete, null);
		STEEL_STAIR = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("steel_stair"), new SteelStairBlock(key(BlockBehaviour.Properties
				.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion().dynamicShape()
				.pushReaction(PushReaction.IMMOVEABLE), "steel_stair")));
		item("steel_stair", p -> new SteelStairItem(STEEL_STAIR, p.useBlockDescriptionPrefix()));
		STAIR_LANDING = block("stair_landing", p -> new ShapedFacingBlock(ShapedFacingBlock.Kind.LANDING, p),
				BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), null);
		STAIR_GUARD = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("stair_guard"), new StairGuardBlock(key(BlockBehaviour.Properties.of()
				.noOcclusion().strength(-1.0F, 3600000.0F).noLootTable().pushReaction(PushReaction.POPPED), "stair_guard")));
		PART = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("exterior_part"), new HiddenBlock(HiddenBlock.PART, key(BlockBehaviour.Properties.of()
				.noCollision().noOcclusion().strength(-1.0F, 3600000.0F).noLootTable(), "exterior_part")));
		// The wall panel's other tones: items placing it so (they are not the block's own item).
		for (Map.Entry<String, WallPanelBlock.Tone> e : Map.of("wall_panel_anthracite", WallPanelBlock.Tone.ANTHRACITE,
				"wall_panel_trapezoid", WallPanelBlock.Tone.TRAPEZOID).entrySet()) {
			item(e.getKey(), p -> new TonePanelItem(WALL_PANEL, p.component(DataComponents.BLOCK_STATE,
					BlockItemStateProperties.EMPTY.with(WallPanelBlock.TONE, e.getValue()))));
		}
		STAIR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("steel_stair"),
				FabricBlockEntityTypeBuilder.create(StairBlockEntity::new, STEEL_STAIR).build());
	}

	public static int part(String name) {
		return PARTS.indexOf(name);
	}

	private static BlockBehaviour.Properties key(BlockBehaviour.Properties properties, String name) {
		return properties.setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(name)));
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties, String tooltip) {
		Block block = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(name), factory.apply(key(properties, name)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Item.Properties ip = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		Item item = Registry.register(BuiltInRegistries.ITEM, itemKey, tooltip == null ? new BlockItem(block, ip) : new TipBlockItem(block, ip, tooltip));
		ITEMS.add(item);
		return block;
	}

	private static Item item(String name, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
		ITEMS.add(item);
		return item;
	}

	/** A block's item with its line of help (tooltip.jugcraft.exterior.*). */
	static final class TipBlockItem extends BlockItem {
		private final String tooltip;

		TipBlockItem(Block block, Properties properties, String tooltip) {
			super(block, properties);
			this.tooltip = tooltip;
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable("tooltip.jugcraft.exterior." + tooltip).withStyle(ChatFormatting.GRAY));
		}
	}

	/** A wall panel of another tone: places the block so, but is not the block's item (that stays the silver panel). */
	static final class TonePanelItem extends BlockItem {
		TonePanelItem(Block block, Properties properties) {
			super(block, properties);
		}

		@Override
		public void registerBlocks(Map<Block, Item> map, Item item) {
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
			out.accept(Component.translatable("tooltip.jugcraft.exterior.wall_panel").withStyle(ChatFormatting.GRAY));
		}
	}
}
