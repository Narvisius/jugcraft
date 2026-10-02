package io.github.jimbozoomer.jugcraft.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing: it exists so the client can draw the heliostat's mirror turning with the sun. */
public class HeliostatBlockEntity extends BlockEntity {
	public HeliostatBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftSolar.HELIOSTAT_ENTITY, pos, state);
	}
}
