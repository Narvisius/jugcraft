package io.github.jimbozoomer.jugcraft.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A heliostat: a mirror on a post that turns with the sun to keep its light on a solar receiver above it. It does
 * nothing by itself; a receiver counts the heliostats under open sky in its field ({@link SolarReceiverBlockEntity}).
 */
public class HeliostatBlock extends SunTrackingBlock {
	public HeliostatBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HeliostatBlockEntity(pos, state);
	}
}
