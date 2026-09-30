package io.github.jimbozoomer.jugcraft.materials;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * A non-metal ore: stone and deepslate ores that drop the mineral item directly
 * (like vanilla redstone or lapis), plus a storage block of nine items.
 */
public final class MineralFamily {
	public final String name;
	public final Block ore;
	public final Block deepslateOre;
	public final Item item;
	public final Block storageBlock;

	private MineralFamily(String name) {
		this.name = name;
		this.ore = JugcraftRegistry.block(name + "_ore", Blocks.IRON_ORE);
		this.deepslateOre = JugcraftRegistry.block("deepslate_" + name + "_ore", Blocks.DEEPSLATE_IRON_ORE);
		this.item = JugcraftRegistry.item(name);
		this.storageBlock = JugcraftRegistry.block(name + "_block", Blocks.CALCITE);
	}

	public static MineralFamily register(String name) {
		return new MineralFamily(name);
	}
}
