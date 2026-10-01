package io.github.jimbozoomer.jugcraft.farming;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Cotton: grows like wheat, from cotton seeds; when ripe it drops cotton and more seeds (loot table). */
public class CottonCropBlock extends CropBlock {
	public CottonCropBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return JugcraftFarming.COTTON_SEEDS;
	}
}
