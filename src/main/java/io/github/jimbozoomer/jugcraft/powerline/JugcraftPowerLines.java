package io.github.jimbozoomer.jugcraft.powerline;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Power lines (docs/features/power-lines.md): the supports, the grid-connection boxes, the invisible frame they are
 * built of, the anchor block entity and the tick that moves energy along the wires. IDs match tools/power_lines.py.
 */
public final class JugcraftPowerLines {
	public static final Map<LineKind, Block> ANCHORS = new EnumMap<>(LineKind.class);
	public static final Map<LineKind, Item> ITEMS = new EnumMap<>(LineKind.class);
	public static Block FRAME;
	public static BlockEntityType<LineAnchorBlockEntity> ANCHOR_ENTITY;

	private JugcraftPowerLines() {
	}

	public static void register() {
		FRAME = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id("power_line_frame"), new LineFrameBlock(
				properties("power_line_frame", Blocks.IRON_BLOCK).strength(2.0F, 6.0F).noOcclusion()
						.pushReaction(PushReaction.IMMOVEABLE)));
		for (LineKind kind : LineKind.values()) {
			// Copy plain blocks only: a log's map colour reads its axis, which these blocks do not have.
			Block source = switch (kind.family) {
				case WOOD -> kind == LineKind.CONCRETE_POLE ? Blocks.STONE : Blocks.SPRUCE_PLANKS;
				default -> Blocks.IRON_BLOCK;
			};
			BlockBehaviour.Properties props = properties(kind.id, source).strength(2.0F, kind.isBox() ? 12.0F : 6.0F).noOcclusion()
					.pushReaction(PushReaction.IMMOVEABLE);
			if (kind.family == LineKind.Family.WOOD && kind != LineKind.CONCRETE_POLE) {
				props.sound(SoundType.WOOD);
			}
			Block block = Registry.register(BuiltInRegistries.BLOCK, Jugcraft.id(kind.id), new LineAnchorBlock(kind, props));
			ANCHORS.put(kind, block);
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(kind.id));
			Item item = Registry.register(BuiltInRegistries.ITEM, key,
					new LineItem(block, kind, new Item.Properties().setId(key).useBlockDescriptionPrefix().stacksTo(kind.isBox() ? 16 : 64)));
			ITEMS.put(kind, item);
		}
		ANCHOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Jugcraft.id("power_line_anchor"),
				FabricBlockEntityTypeBuilder.create(LineAnchorBlockEntity::new, ANCHORS.values().toArray(Block[]::new)).build());
		EnergyStorage.SIDED.registerForBlockEntity((anchor, side) -> anchor.energy(), ANCHOR_ENTITY);
		FluidStorage.SIDED.registerForBlockEntity((anchor, side) -> anchor.nitrogen, ANCHOR_ENTITY);
		ServerTickEvents.END_LEVEL_TICK.register(LineNetworks::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LinePlacement.forget(handler.getPlayer().getUUID()));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> ITEMS.values().forEach(output::accept));
		PowerLineShowcase.register();
	}

	private static BlockBehaviour.Properties properties(String path, Block copy) {
		return BlockBehaviour.Properties.ofFullCopy(copy).setId(ResourceKey.create(Registries.BLOCK, Jugcraft.id(path)));
	}
}
