package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Dragonblood needs firm End stone or moss, but no skylight to advance its normal sapling stages. */
public class EndTreeSaplingBlock extends SaplingBlock {
	public EndTreeSaplingBlock(TreeGrower grower, Properties properties) { super(grower, properties); }
	@Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(Blocks.END_STONE) || state.is(Blocks.MOSS_BLOCK);
	}
	@Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (random.nextInt(7) == 0) advanceTree(level, pos, state, random);
	}
}
