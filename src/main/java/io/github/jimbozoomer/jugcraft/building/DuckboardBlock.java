package io.github.jimbozoomer.jugcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A duckboard (batch 50): a low slatted walkway laid on the trench floor. */
public class DuckboardBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2.5, 16);

	public DuckboardBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
