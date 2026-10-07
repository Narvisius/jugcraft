package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;

/**
 * Kaiserworks (batch 48, docs/features/kaiserworks.md): imperial building blocks to dress the dieselpunk set. Keep the
 * list, kinds and strengths in sync with tools/kaiserworks.py; tools/check_mod_data.py checks them.
 */
public final class Kaiserworks {
	/** Light from the imperial gas lamp. */
	public static final int LAMP_LIGHT = 15;
	/** Every block, by id, in registration order (slabs and stairs included). */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	private static final List<String> TOOLTIPS = List.of("imperial_crest", "wrought_iron_lattice", "imperial_gas_lamp");

	private Kaiserworks() {
	}

	public static void register() {
		entry("black_lacquer_plate", "family", 5.0F, 6.0F);
		entry("riveted_black_plate", "family", 5.0F, 6.0F);
		entry("gilt_trimmed_plate", "family", 5.0F, 6.0F);
		entry("polished_brass_plate", "family", 5.0F, 6.0F);
		entry("gilt_frieze", "full", 5.0F, 6.0F);
		entry("imperial_crest", "crest", 5.0F, 6.0F);
		entry("fluted_marble_column", "pillar", 2.0F, 6.0F);
		entry("black_iron_column", "pillar", 5.0F, 6.0F);
		entry("polished_marble", "family", 2.0F, 6.0F);
		entry("station_tiles", "family", 2.0F, 6.0F);
		entry("wrought_iron_lattice", "grating", 3.0F, 6.0F);
		entry("leaded_glass", "glass", 1.0F, 3.0F);
		entry("imperial_gas_lamp", "lamp", 1.5F, 3.0F);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
	}

	private static void entry(String id, String kind, float hardness, float blast) {
		boolean stone = id.contains("marble") || id.contains("tiles");
		MapColor color = id.contains("brass") ? MapColor.GOLD : stone ? MapColor.QUARTZ : MapColor.COLOR_BLACK;
		BlockBehaviour.Properties base = stone
				? BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(color).strength(hardness, blast)
						.requiresCorrectToolForDrops()
				: BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(color).strength(hardness, blast)
						.requiresCorrectToolForDrops();
		switch (kind) {
			case "family" -> {
				Block full = block(id, new Block(properties(id, base)));
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
				block(id + "_stairs", new StairBlock(full.defaultBlockState(),
						properties(id + "_stairs", BlockBehaviour.Properties.ofFullCopy(full))));
			}
			case "pillar" -> block(id, new RotatedPillarBlock(properties(id, base)));
			case "crest" -> block(id, new CrestBlock(properties(id, base)));
			case "grating" -> {
				// Glass's properties: see-through, never suffocating, never blocking the view.
				BlockBehaviour.Properties lattice = BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(color)
						.strength(hardness, blast).requiresCorrectToolForDrops().sound(SoundType.CHAIN);
				Block full = block(id, new TransparentBlock(properties(id, lattice)) {
				});
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
			}
			case "glass" -> block(id, new TransparentBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
					.mapColor(color).strength(hardness, blast))) {
			});
			case "lamp" -> block(id, new Dieselworks.LampBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
					.mapColor(MapColor.GOLD).strength(hardness, blast).lightLevel(state -> LAMP_LIGHT).noOcclusion())));
			default -> block(id, new Block(properties(id, base)));
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

	/** The imperial crest: a full block whose crest faces the player who places it. */
	public static class CrestBlock extends HorizontalDirectionalBlock {
		public CrestBlock(Properties properties) {
			super(properties);
			registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context) {
			return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
			builder.add(FACING);
		}
	}
}
