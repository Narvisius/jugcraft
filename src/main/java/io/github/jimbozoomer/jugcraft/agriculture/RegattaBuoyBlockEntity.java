package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Marks a {@link RegattaBuoyBlock} so a Regatta Flag can find its course among the block entities of the
 * chunks around it, instead of reading every block. It keeps nothing: the buoy's number is in its block state.
 */
public class RegattaBuoyBlockEntity extends BlockEntity {
	public RegattaBuoyBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.REGATTA_BUOY_ENTITY, pos, state);
	}
}
