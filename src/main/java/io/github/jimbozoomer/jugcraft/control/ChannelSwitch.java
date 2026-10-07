package io.github.jimbozoomer.jugcraft.control;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block a logic controller switches with its channel (batch 36-37): the relay and the alarm. */
public interface ChannelSwitch extends DataConnectable {
	/** Switches the block at {@code pos} on or off (server side); true if it changed. */
	boolean switchTo(Level level, BlockPos pos, BlockState state, boolean on);
}
