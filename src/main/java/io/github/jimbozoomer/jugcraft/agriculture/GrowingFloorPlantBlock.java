package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Renewable giant fungi/bushes, using the same bounded feature as natural generation. */
public class GrowingFloorPlantBlock extends FloorPlantBlock implements BonemealableBlock {
	private final String shape;
	public GrowingFloorPlantBlock(Properties properties, String shape) { super(properties); this.shape = shape; }
	@Override protected boolean mayPlaceOn(BlockState floor, BlockGetter level, BlockPos pos) {
		return FungalGrowthFeature.soil(shape, floor) || super.mayPlaceOn(floor, level, pos);
	}
	@Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
		return FungalGrowthFeature.soil(shape, level.getBlockState(pos.below()))
				&& (!shape.startsWith("glowcap") || level.getMaxLocalRawBrightness(pos.above()) < 13);
	}
	@Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		// Include collectible vanilla mushroom stems in the compost budget: four bone meal per success on average.
		return random.nextFloat() < .25F;
	}
	@Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
		if (!isValidBonemealTarget(level, pos, state, source)) return;
		String selected = shape.startsWith("glowcap") && random.nextBoolean() ? "glowcap_parasol" : shape;
		new FungalGrowthFeature(selected).place(level, level.getChunkSource().getGenerator(), random, pos);
	}
}
