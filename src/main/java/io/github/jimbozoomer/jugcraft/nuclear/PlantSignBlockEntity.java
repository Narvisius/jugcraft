package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Only there so the client can draw the sign; everything it shows comes from the block state. */
public class PlantSignBlockEntity extends BlockEntity {
	public PlantSignBlockEntity(BlockPos pos, BlockState state) {
		super(PlantInterior.SIGN_ENTITY, pos, state);
	}
}
