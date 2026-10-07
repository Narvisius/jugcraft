package io.github.jimbozoomer.jugcraft.raiders;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Siege Ladder (raider extras): rough poles and rungs a raider grunt props against a wall that stops it, so walls
 * slow raiders rather than stop them. It crumbles away {@value JugcraftRaiders#LADDER_TTL} ticks after it is placed and
 * drops nothing; players break it in a moment. Raiders only put one up where mob griefing is on.
 */
public class SiegeLadderBlock extends LadderBlock {
	public SiegeLadderBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, JugcraftRaiders.LADDER_TTL);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		level.destroyBlock(pos, false);
	}

	/** Whether {@code state} is a siege ladder (for tests and the camps). */
	public static boolean is(BlockState state) {
		return state.getBlock() instanceof SiegeLadderBlock;
	}

	static Block create(Properties properties) {
		return new SiegeLadderBlock(properties);
	}
}
