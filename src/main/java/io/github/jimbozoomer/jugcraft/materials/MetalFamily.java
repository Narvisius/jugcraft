package io.github.jimbozoomer.jugcraft.materials;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

/**
 * The canonical vanilla-style form set for one Jugcraft metal. Every metal uses the
 * same IDs as vanilla iron/copper ({@code <metal>_ingot}, {@code raw_<metal>}, ...)
 * and the matching {@code c:} convention tags, so future features and other tech
 * mods can share it. Add further forms (dust, plate, ...) only when a feature needs them.
 */
public final class MetalFamily {
	public final String name;
	public final Item ingot;
	public final Item nugget;
	public final Block storageBlock;
	public final @Nullable Item raw;
	public final @Nullable Block rawBlock;
	public final @Nullable Block ore;
	public final @Nullable Block deepslateOre;
	/** Plain items declared with {@link Builder#extraItem}, such as an alloy blend. */
	public final List<Item> extras;

	private MetalFamily(Builder builder) {
		this.name = builder.name;

		if (builder.mined) {
			this.ore = block(name + "_ore", Blocks.IRON_ORE);
			this.deepslateOre = block("deepslate_" + name + "_ore", Blocks.DEEPSLATE_IRON_ORE);
			this.rawBlock = block("raw_" + name + "_block", Blocks.RAW_IRON_BLOCK);
			this.raw = item("raw_" + name);
		} else {
			this.ore = null;
			this.deepslateOre = null;
			this.rawBlock = null;
			this.raw = null;
		}

		List<Item> extraList = new ArrayList<>();
		for (String extra : builder.extraItems) {
			extraList.add(item(extra));
		}
		this.extras = Collections.unmodifiableList(extraList);

		this.storageBlock = block(name + "_block", Blocks.IRON_BLOCK);
		this.ingot = item(name + "_ingot");
		this.nugget = item(name + "_nugget");
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	private static Item item(String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
		return item;
	}

	private static Block block(String path, Block copyFrom) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Jugcraft.id(path));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey,
				new Block(BlockBehaviour.Properties.ofFullCopy(copyFrom).setId(blockKey)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Jugcraft.id(path));
		Registry.register(BuiltInRegistries.ITEM, itemKey,
				new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
		return block;
	}

	public static final class Builder {
		private final String name;
		private boolean mined;
		private final List<String> extraItems = new ArrayList<>();

		private Builder(String name) {
			this.name = name;
		}

		/** Adds ore, deepslate ore, raw item and raw storage block. */
		public Builder mined() {
			this.mined = true;
			return this;
		}

		/** Adds a plain item, such as an alloy blend. */
		public Builder extraItem(String path) {
			this.extraItems.add(path);
			return this;
		}

		public MetalFamily build() {
			return new MetalFamily(this);
		}
	}
}
