package io.github.jimbozoomer.jugcraft.materials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
			this.ore = JugcraftRegistry.block(name + "_ore", Blocks.IRON_ORE);
			this.deepslateOre = JugcraftRegistry.block("deepslate_" + name + "_ore", Blocks.DEEPSLATE_IRON_ORE);
			this.rawBlock = JugcraftRegistry.block("raw_" + name + "_block", Blocks.RAW_IRON_BLOCK);
			this.raw = JugcraftRegistry.item("raw_" + name);
		} else {
			this.ore = null;
			this.deepslateOre = null;
			this.rawBlock = null;
			this.raw = null;
		}

		List<Item> extraList = new ArrayList<>();
		for (String extra : builder.extraItems) {
			extraList.add(JugcraftRegistry.item(extra));
		}
		this.extras = Collections.unmodifiableList(extraList);

		this.storageBlock = JugcraftRegistry.block(name + "_block", Blocks.IRON_BLOCK);
		this.ingot = builder.lore ? JugcraftRegistry.item(name + "_ingot", LoreItem::new) : JugcraftRegistry.item(name + "_ingot");
		this.nugget = JugcraftRegistry.item(name + "_nugget");
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	public static final class Builder {
		private final String name;
		private boolean mined;
		private boolean lore;
		private final List<String> extraItems = new ArrayList<>();

		private Builder(String name) {
			this.name = name;
		}

		/** Adds ore, deepslate ore, raw item and raw storage block. */
		public Builder mined() {
			this.mined = true;
			return this;
		}

		/** Gives the ingot a lore line under its name, {@code tooltip.jugcraft.<metal>_ingot} ({@link LoreItem}). */
		public Builder lore() {
			this.lore = true;
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
