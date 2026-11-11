package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The compiled sites' blocks and bookkeeping (towers and pump houses build, 19 October 2026: the medium plant's
 * cooling towers and pump houses, claude/jugcraft-towers-pumphouses-build.md): Plant Structure and its ladder, and the
 * anchors putting their sites in SiteRegistry as their chunks load on the server (the client's, SiteClient).
 */
public final class Sites {
	public static Block STRUCTURE;
	public static Block LADDER;
	/**
	 * The client's: an anchor now shows another site (the reactor building at power or refuelling): its placement
	 * swapped and the chunks it covers built again (client/SiteClient.java sets it).
	 */
	public static volatile java.util.function.Consumer<BlockEntity> clientRefresh = e -> {
	};

	private Sites() {
	}

	public static void register() {
		STRUCTURE = block("site_structure", SiteBlock::new);
		LADDER = block("site_ladder", SiteBlock::new);
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> load(entity, level));
		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> unload(entity, level));
	}

	private static Block block(String name, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Jugcraft.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
				.mapColor(MapColor.STONE).strength(3.0F, 12.0F).sound(SoundType.STONE).noOcclusion().dynamicShape().isSuffocating((st, l, p) -> false)
				.isViewBlocking((st, l, p, box) -> false).isRedstoneConductor((st, l, p) -> false).setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	/** An anchor's chunk loaded (either side): its site goes in. */
	public static SiteRegistry.Placement load(BlockEntity entity, Level level) {
		if (entity instanceof SiteAnchor anchor && anchor.siteKey() != null) {
			return SiteRegistry.add(level, entity.getBlockPos(), anchor.siteFacing(), anchor.siteKey());
		}
		return null;
	}

	/** An anchor's chunk unloaded (or the anchor broken): its site goes out. */
	public static SiteRegistry.Placement unload(BlockEntity entity, Level level) {
		if (entity instanceof SiteAnchor) {
			return SiteRegistry.remove(level, entity.getBlockPos());
		}
		return null;
	}
}
