package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dieselworks (batch 45, docs/features/dieselworks.md): building blocks in the look of the dieselpunk giants. Keep the
 * list, kinds and strengths in sync with tools/dieselworks.py; tools/check_mod_data.py checks them.
 */
public final class Dieselworks {
	/** Light from the amber cage lamp. */
	public static final int LAMP_LIGHT = 14;
	/** Every block, by id, in registration order (slabs and stairs included). */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	private static final List<String> TOOLTIPS = List.of("rust_grating", "steel_i_beam", "amber_cage_lamp");

	private Dieselworks() {
	}

	public static void register() {
		entry("rust_plate", "family", 5.0F, 6.0F);
		entry("riveted_rust_plate", "family", 5.0F, 6.0F);
		entry("patina_plate", "family", 5.0F, 6.0F);
		entry("perforated_patina_plate", "full", 5.0F, 6.0F);
		entry("red_iron_plate", "family", 5.0F, 6.0F);
		entry("copper_dome_plate", "family", 5.0F, 6.0F);
		entry("riveted_band_block", "full", 5.0F, 6.0F);
		entry("skid_iron_block", "full", 5.0F, 6.0F);
		entry("ribbed_patina_pillar", "pillar", 5.0F, 6.0F);
		entry("ribbed_rust_pillar", "pillar", 5.0F, 6.0F);
		entry("rust_grating", "grating", 3.0F, 6.0F);
		entry("steel_i_beam", "beam", 5.0F, 6.0F);
		entry("porthole_window", "glass", 1.0F, 3.0F);
		entry("amber_cage_lamp", "lamp", 1.5F, 3.0F);
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
	}

	private static void entry(String id, String kind, float hardness, float blast) {
		// The steel set (both steel plates, the pillar, the band, the grating, the I-beam and the porthole's ring) is cool
		// blue-grey steel, so it maps as metal; skid iron keeps the giants' warm brown. The lamp sets its own orange below.
		MapColor color = id.contains("patina") || id.contains("dome") ? MapColor.WARPED_NYLIUM
				: id.contains("red") ? MapColor.COLOR_RED
				: id.equals("skid_iron_block") ? MapColor.TERRACOTTA_BROWN : MapColor.METAL;
		BlockBehaviour.Properties metal = BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(color)
				.strength(hardness, blast).requiresCorrectToolForDrops();
		switch (kind) {
			case "family" -> {
				Block full = block(id, new Block(properties(id, metal)));
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
				block(id + "_stairs", new StairBlock(full.defaultBlockState(),
						properties(id + "_stairs", BlockBehaviour.Properties.ofFullCopy(full))));
			}
			case "pillar" -> block(id, new RotatedPillarBlock(properties(id, metal)));
			case "beam" -> block(id, new IBeamBlock(properties(id, metal.noOcclusion())));
			case "grating" -> {
				// Glass's properties: see-through, never suffocating, never blocking the view.
				BlockBehaviour.Properties grate = BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(color)
						.strength(hardness, blast).requiresCorrectToolForDrops().sound(SoundType.CHAIN);
				Block full = block(id, new TransparentBlock(properties(id, grate)) {
				});
				block(id + "_slab", new SlabBlock(properties(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full))));
			}
			case "glass" -> block(id, new TransparentBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
					.mapColor(color).strength(hardness, blast))) {
			});
			case "lamp" -> block(id, new LampBlock(properties(id, BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
					.mapColor(MapColor.COLOR_ORANGE).strength(hardness, blast).lightLevel(state -> LAMP_LIGHT).noOcclusion())));
			default -> block(id, new Block(properties(id, metal)));
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

	/** A steel I-beam along its axis: two flanges joined by a web (the model in tools/dieselworks.py). */
	public static class IBeamBlock extends RotatedPillarBlock {
		private static final VoxelShape ALONG_Y = Shapes.or(Block.box(1, 0, 1, 4, 16, 15), Block.box(12, 0, 1, 15, 16, 15),
				Block.box(4, 0, 6.5, 12, 16, 9.5));
		private static final VoxelShape ALONG_Z = Shapes.or(Block.box(1, 1, 0, 4, 15, 16), Block.box(12, 1, 0, 15, 15, 16),
				Block.box(4, 6.5, 0, 12, 9.5, 16));
		private static final VoxelShape ALONG_X = Shapes.or(Block.box(0, 1, 1, 16, 15, 4), Block.box(0, 1, 12, 16, 15, 15),
				Block.box(0, 6.5, 4, 16, 9.5, 12));

		public IBeamBlock(Properties properties) {
			super(properties);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return switch (state.getValue(AXIS)) {
				case X -> ALONG_X;
				case Z -> ALONG_Z;
				default -> ALONG_Y;
			};
		}
	}

	/** The amber cage lamp: a small lamp standing on whatever it is placed on. */
	public static class LampBlock extends Block {
		private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

		public LampBlock(Properties properties) {
			super(properties);
		}

		@Override
		protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
			return SHAPE;
		}
	}
}
