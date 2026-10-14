package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Furniture with a hot coffee on it (the glass coffee table): steam rises off the mug. */
public class SteamFurnitureBlock extends FurnitureBlock {
	private final double x, y, z;

	public SteamFurnitureBlock(Properties properties, double x, double y, double z, double[]... boxes) {
		super(properties, boxes);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		Steam.puff(level, pos, state.getValue(FACING), x, y, z, random);
	}
}
