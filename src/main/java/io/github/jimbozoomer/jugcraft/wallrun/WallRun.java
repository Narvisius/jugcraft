package io.github.jimbozoomer.jugcraft.wallrun;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A pipe or cable run inside a made block (docs/features/wall_runs.md): the block keeps the wall's look (whatever block
 * it was, kept in its {@link WallRunBlockEntity}), and the pipe or cable runs through it, joined to its neighbours like
 * the plain one. Its six direction properties say where a wall sleeve shows: a side the run comes out of into the
 * open (joined to something that is not another run in a wall).
 */
public interface WallRun {
	/** The plain pipe or cable this is: what it drops and what the wrench pulls out. */
	Block carrier();

	/** Whether the plain pipe or cable here would join this neighbour (its own rule). */
	boolean joins(BlockGetter level, BlockPos pos, Direction direction, BlockState neighbor);
}
