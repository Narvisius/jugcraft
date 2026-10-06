package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A high-bay light: an LED fitting hung close under a high roof. A real one lights the floor far below brightly, but
 * block light in the game fades out within 15 blocks; so when placed it also sets the game's invisible light block
 * four blocks over the floor (or over whatever stands under it) straight below, and takes it away again with it.
 */
public class HighBayLightBlock extends FurnitureBlock {
	private static final int REACH = 32;

	public HighBayLightBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (level.isClientSide() || oldState.is(this)) {
			return;
		}
		// Down through the air to the floor (or the top of what stands on it), then back up four.
		BlockPos at = pos.below();
		int n = 0;
		while (n < REACH && level.getBlockState(at.below()).isAir() && level.isInWorldBounds(at.below())) {
			at = at.below();
			n++;
		}
		BlockPos glow = at.above(4);
		if (glow.getY() < pos.getY() - 1 && level.getBlockState(glow).isAir()) {
			level.setBlock(glow, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		BlockPos at = pos.below();
		for (int n = 0; n < REACH; n++, at = at.below()) {
			BlockState here = level.getBlockState(at);
			if (here.is(Blocks.LIGHT) && here.getValue(LightBlock.LEVEL) == 15) {
				level.removeBlock(at, false);
				return;
			}
			if (!here.isAir()) {
				return;
			}
		}
	}
}
