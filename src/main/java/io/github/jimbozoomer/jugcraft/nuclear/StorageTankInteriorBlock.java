package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The inside of a formed storage tank: an invisible block in every cell the tank holds, so nothing spawns in there and
 * nothing gets in. The tank's outlet fills the space when it forms the tank and clears it when the tank comes apart;
 * if the shell is broken open, the interior empties itself from the hole, block by block, at once.
 */
public class StorageTankInteriorBlock extends Block {
	public StorageTankInteriorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (neighborState.isAir()) {
			ticks.scheduleTick(pos, this, 1);
		}
		return state;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		for (Direction direction : Direction.values()) {
			if (level.getBlockState(pos.relative(direction)).isAir()) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				return;
			}
		}
	}
}
