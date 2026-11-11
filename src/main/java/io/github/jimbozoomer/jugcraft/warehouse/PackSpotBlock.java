package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.world.level.block.Block;

/** The floor under a palletizing station's build spot: hatched, PALLET BUILD; lane tiles join up to it. */
public class PackSpotBlock extends Block implements WarehouseBlock {
	public PackSpotBlock(Properties properties) {
		super(properties);
	}
}
