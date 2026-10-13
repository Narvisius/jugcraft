package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Growth rules shared by every Jugcraft crop. They follow vanilla crops (soil under and around
 * the plant) plus one agricultural system: legumes feed their neighbours nitrogen.
 */
public final class CropGrowth {
	/** Crops that fix nitrogen. Data packs can add their own. */
	public static final TagKey<Block> NITROGEN_FIXING = TagKey.create(Registries.BLOCK, Jugcraft.id("nitrogen_fixing_crops"));
	/** Non-legume crops next to a legume grow this much faster. Keep in sync with tools/agriculture.py. */
	public static final float LEGUME_BONUS = 1.5F;
	/** Vanilla crops need this much light to grow. */
	public static final int MIN_GROWTH_LIGHT = 9;

	private CropGrowth() {
	}

	/**
	 * Growth speed at {@code pos}: 1, plus 1 for farmland under the plant (3 if moist), plus a quarter
	 * of that for each of the eight blocks around it; then times {@link #LEGUME_BONUS} next to a legume, and
	 * times {@link HarvestMoon#GROWTH_BONUS} under the Harvest Moon.
	 * Unlike vanilla, dense planting is not penalised, so fields and maze walls can be solid.
	 * Reads at most 17 block states, and only on a random tick.
	 */
	public static float speed(BlockGetter level, BlockPos pos, boolean isLegume) {
		float speed = 1.0F;
		BlockPos soil = pos.below();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockState state = level.getBlockState(soil.offset(dx, 0, dz));
				float value = 0.0F;
				if (state.is(BlockTags.GROWS_CROPS)) {
					value = state.hasProperty(BlockStateProperties.MOISTURE) && state.getValue(BlockStateProperties.MOISTURE) > 0 ? 3.0F : 1.0F;
				}
				speed += dx == 0 && dz == 0 ? value : value / 4.0F;
			}
		}
		if (!isLegume && nextToLegume(level, pos)) {
			speed *= LEGUME_BONUS;
		}
		if (HarvestMoon.active()) {
			speed *= HarvestMoon.GROWTH_BONUS;
		}
		return speed;
	}

	/**
	 * Growth speed of a paddy crop ({@link PaddyCropBlock}) at {@code pos}: as {@link #speed}, with flooded soil in place of
	 * farmland. Bog soil under still water counts as moist farmland (3), under the plant and a quarter for each of the eight
	 * around it, so a flooded paddy grows as fast as a watered field.
	 */
	public static float paddySpeed(BlockGetter level, BlockPos pos) {
		float speed = 1.0F;
		BlockPos soil = pos.below();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos at = soil.offset(dx, 0, dz);
				float value = level.getBlockState(at).is(CranberryBushBlock.BOG_SOIL) && level.getFluidState(at.above()).is(FluidTags.WATER) ? 3.0F : 0.0F;
				speed += dx == 0 && dz == 0 ? value : value / 4.0F;
			}
		}
		if (nextToLegume(level, pos)) {
			speed *= LEGUME_BONUS;
		}
		if (HarvestMoon.active()) {
			speed *= HarvestMoon.GROWTH_BONUS;
		}
		return speed;
	}

	/** Whether any of the eight blocks around {@code pos} (same height) is a legume crop. */
	public static boolean nextToLegume(BlockGetter level, BlockPos pos) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if ((dx != 0 || dz != 0) && level.getBlockState(pos.offset(dx, 0, dz)).is(NITROGEN_FIXING)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Vanilla's chance formula: one growth step in {@code 25 * growthTime / speed + 1} random ticks. */
	public static int chanceDivisor(float speed, float growthTime) {
		return (int) (25.0F * growthTime / speed) + 1;
	}
}
