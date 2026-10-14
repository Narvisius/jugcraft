package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A live panel hung on a wall (round 26, #9: the battery rooms' hydrogen monitor and exhaust fan): one block wide and
 * two high, labelled by train, its hitbox a slab out from the wall {@code depth} pixels deep (the piece facing north,
 * the wall behind it at z 0), the rest of its two blocks open.
 */
public class WallLivePanelBlock extends LivePanelBlock {
	private final double depth;

	public WallLivePanelBlock(Properties properties, int variants, double depth) {
		super(properties, 1, 2, variants);
		this.depth = depth;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return turned(new double[] {1, 1, 0, 15, 15, depth}, state.getValue(FACING));
	}
}
