package io.github.jimbozoomer.jugcraft.solar;

import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A block whose top part follows the sun: its model leaves that part out ({@link ShaftBlock#TURNING} is always true)
 * and the client draws it tilted from east to west with the time of day (client/KineticRotors, "sun" rotors).
 */
public abstract class SunTrackingBlock extends BaseEntityBlock {
	protected SunTrackingBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ShaftBlock.TURNING, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ShaftBlock.TURNING);
	}
}
