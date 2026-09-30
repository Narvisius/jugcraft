package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds no data: it only lets the client draw a turning shaft spinning (client/KineticRotorRenderer). */
public class ShaftBlockEntity extends BlockEntity {
	public ShaftBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.SHAFT_ENTITY, pos, state);
	}
}
