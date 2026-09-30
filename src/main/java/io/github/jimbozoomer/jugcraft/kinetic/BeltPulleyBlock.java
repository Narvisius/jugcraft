package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A belt pulley: a shaft with a grooved wheel. It carries rotation along its axis like a shaft, and a
 * belt ({@link BeltItem}) links it to another pulley with the same axis up to
 * {@link BeltPulleyBlockEntity#MAX_LENGTH} blocks away, which then turns too (see {@link KineticNetworks}).
 */
public class BeltPulleyBlock extends ShaftBlock {
	public BeltPulleyBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BeltPulleyBlockEntity(pos, state);
	}
}
