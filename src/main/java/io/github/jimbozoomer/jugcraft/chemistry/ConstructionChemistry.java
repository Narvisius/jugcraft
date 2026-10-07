package io.github.jimbozoomer.jugcraft.chemistry;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Construction chemistry (batch 32, docs/features/construction-chemistry.md): the foam sprayer and its canisters,
 * construction foam, cement, concrete and blast-proof concrete. Keep the numbers in sync with tools/construction.py;
 * tools/check_mod_data.py checks them.
 */
public final class ConstructionChemistry {
	/** The foam sprayer's reach, the most blocks one spray fills, how far they spread, ticks between sprays. */
	public static final int SPRAY_RANGE = 16;
	public static final int SPRAY_BLOCKS = 12;
	public static final double SPRAY_RADIUS = 2.5;
	public static final int SPRAY_COOLDOWN = 8;
	/** Blocks of foam in one canister (its durability). */
	public static final int CANISTER_FOAM = 32;

	public static Block CONSTRUCTION_FOAM;
	public static Block CONCRETE;
	public static Block BLASTPROOF_CONCRETE;
	public static Item CEMENT_MIX;
	public static Item CEMENT;
	public static Item REBAR;
	public static Item FOAM_CANISTER;
	public static Item FOAM_SPRAYER;
	/** Every block, by id, in registration order. */
	public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();

	private ConstructionChemistry() {
	}

	public static void register() {
		CONSTRUCTION_FOAM = block("construction_foam", BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
				.strength(0.3F, 0.5F).sound(SoundType.WOOL).pushReaction(PushReaction.POPPED), Block::new);
		CONCRETE = family("concrete", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.STONE)
				.strength(2.5F, 9.0F).requiresCorrectToolForDrops());
		BLASTPROOF_CONCRETE = family("blastproof_concrete", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
				.mapColor(MapColor.COLOR_GRAY).strength(15.0F, 1200.0F).requiresCorrectToolForDrops());
		CEMENT_MIX = item("cement_mix", Item::new);
		CEMENT = item("cement", CementItem::new);
		REBAR = item("rebar", Item::new);
		FOAM_CANISTER = item("foam_canister", properties -> new Described(properties.durability(CANISTER_FOAM)));
		FOAM_SPRAYER = item("foam_sprayer", properties -> new FoamSprayerItem(properties.stacksTo(1)));

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output ->
				BLOCKS.values().forEach(output::accept));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.accept(CEMENT_MIX);
			output.accept(CEMENT);
			output.accept(REBAR);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
			output.accept(FOAM_SPRAYER);
			output.accept(FOAM_CANISTER);
		});
	}

	/** A full block with its slab and stairs. */
	private static Block family(String id, BlockBehaviour.Properties properties) {
		Block full = block(id, properties, Block::new);
		block(id + "_slab", BlockBehaviour.Properties.ofFullCopy(full), SlabBlock::new);
		block(id + "_stairs", BlockBehaviour.Properties.ofFullCopy(full), props -> new StairBlock(full.defaultBlockState(), props));
		return full;
	}

	private static Block block(String path, BlockBehaviour.Properties properties, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(path));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)
				.useBlockDescriptionPrefix()) {
			@Override
			public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
					Consumer<Component> tooltip, TooltipFlag flag) {
				describe(path, tooltip);
			}
		});
		BLOCKS.put(path, block);
		return block;
	}

	private static Item item(String path, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	/** The grey line from {@code tooltip.jugcraft.<path>}, for the items and blocks that have one. */
	static void describe(String path, Consumer<Component> tooltip) {
		if (java.util.List.of("construction_foam", "blastproof_concrete", "cement", "foam_canister", "foam_sprayer").contains(path)) {
			tooltip.accept(Component.translatable("tooltip.jugcraft." + path).withStyle(ChatFormatting.GRAY));
		}
	}

	/** An item with its tooltip line. */
	static class Described extends Item {
		Described(Properties properties) {
			super(properties);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
				TooltipFlag flag) {
			describe(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), tooltip);
		}
	}
}
