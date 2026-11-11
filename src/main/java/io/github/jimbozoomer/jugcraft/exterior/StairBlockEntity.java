package io.github.jimbozoomer.jugcraft.exterior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A stair section's first cell on each side (part 0): nothing to keep, it is there for its renderer
 * (client/exterior/StairRenderer.java), which draws that side's stringer and handrail up the section.
 */
public class StairBlockEntity extends BlockEntity {
	public StairBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftExterior.STAIR_ENTITY, pos, state);
	}
}
