package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A sapling that grows its tree alone, or a giant tree (a trunk two blocks wide) when four of them stand in a square,
 * as spruce and jungle saplings do (agriculture.TREES "giant" in tools). The giant grows from the square's north-west
 * corner, where vanilla's giant trunk shapes start; if it has no room, the four saplings stay.
 */
public class GiantSaplingBlock extends SaplingBlock {
	private final TreeGrower giant;

	public GiantSaplingBlock(TreeGrower grower, TreeGrower giant, Properties properties) {
		super(grower, properties);
		this.giant = giant;
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		if (state.getValue(STAGE) == 1) {
			// The four squares this sapling can be a corner of, from its own north-west.
			for (int dx = 0; dx >= -1; dx--) {
				for (int dz = 0; dz >= -1; dz--) {
					BlockPos corner = pos.offset(dx, 0, dz);
					if (isSquare(level, corner) && growGiant(level, corner, state, random)) {
						return;
					}
				}
			}
		}
		super.advanceTree(level, pos, state, random);
	}

	private boolean isSquare(BlockGetter level, BlockPos corner) {
		for (BlockPos at : square(corner)) {
			if (!level.getBlockState(at).is(this)) {
				return false;
			}
		}
		return true;
	}

	private boolean growGiant(ServerLevel level, BlockPos corner, BlockState state, RandomSource random) {
		List<BlockPos> square = square(corner);
		for (BlockPos at : square) {
			level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
		}
		if (giant.growTree(level, level.getChunkSource().getGenerator(), corner, state, random)) {
			return true;
		}
		for (BlockPos at : square) {
			level.setBlock(at, state, Block.UPDATE_NONE);
		}
		return false;
	}

	private static List<BlockPos> square(BlockPos corner) {
		return List.of(corner, corner.east(), corner.south(), corner.south().east());
	}
}
