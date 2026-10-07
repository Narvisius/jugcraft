package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A one-block crop with vanilla rules (farmland, light, bone meal, loot by age), growing with
 * {@link CropGrowth}: legumes speed up their neighbours, and dense planting is not penalised.
 */
public class JugcraftCropBlock extends CropBlock {
	private final String seedId;
	private final boolean legume;

	public JugcraftCropBlock(Properties properties, String seedId, boolean legume) {
		super(properties);
		this.seedId = seedId;
		this.legume = legume;
	}

	/** The item that plants this crop. */
	public Item seed() {
		return JugcraftAgriculture.item(seedId);
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return seed();
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = this.getAge(state);
		if (age < this.getMaxAge() && level.getRawBrightness(pos, 0) >= CropGrowth.MIN_GROWTH_LIGHT
				&& random.nextInt(CropGrowth.chanceDivisor(CropGrowth.speed(level, pos, legume), 1.0F)) == 0) {
			level.setBlock(pos, this.getStateForAge(age + 1), Block.UPDATE_CLIENTS);
		}
	}
}
