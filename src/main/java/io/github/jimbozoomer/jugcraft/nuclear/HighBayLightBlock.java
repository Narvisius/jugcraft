package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A high-bay light: an LED fitting hung close under a high roof. A real one lights the floor far below brightly, but
 * block light in the game fades out within 15 blocks; so when placed it also sets the game's invisible light block
 * about four blocks over the floor (or over whatever stands on it) straight below, past pipes and railings in the
 * way, and takes it away again with it.
 */
public class HighBayLightBlock extends FurnitureBlock {
	private static final int REACH = 32;

	public HighBayLightBlock(Properties properties, double[]... boxes) {
		super(properties, boxes);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide() && !oldState.is(this)) {
			// A moment later, so a whole building placed at once (a plant) has its floors in by then.
			level.scheduleTick(pos, this, 2);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		// Down to the floor: the first full block under the light (pipes, rails, railings and the like are passed by),
		// or the top of what stands on it; then the light four over it, or as near four as there is air.
		BlockPos floor = null;
		BlockPos at = pos.below();
		for (int n = 0; n < REACH && level.isInWorldBounds(at); n++, at = at.below()) {
			BlockState here = level.getBlockState(at);
			if (!here.isAir() && here.isCollisionShapeFullBlock(level, at)) {
				floor = at;
				break;
			}
		}
		if (floor == null) {
			return;
		}
		for (int up : new int[] {4, 3, 5, 2, 6}) {
			BlockPos glow = floor.above(up);
			if (glow.getY() < pos.getY() - 1 && level.getBlockState(glow).isAir()) {
				level.setBlock(glow, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), Block.UPDATE_ALL);
				return;
			}
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return;
		}
		// Its light: the first full-strength light block straight below.
		BlockPos at = pos.below();
		for (int n = 0; n < REACH && level.isInWorldBounds(at); n++, at = at.below()) {
			BlockState here = level.getBlockState(at);
			if (here.is(Blocks.LIGHT) && here.getValue(LightBlock.LEVEL) == 15) {
				level.removeBlock(at, false);
				return;
			}
			if (!here.isAir() && here.isCollisionShapeFullBlock(level, at)) {
				return;
			}
		}
	}
}
