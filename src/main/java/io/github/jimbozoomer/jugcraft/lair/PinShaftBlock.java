package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The steel shaft of one of the pins stuck in the Spindle Loft's pincushion: four pixels thick, upright, solid. */
public class PinShaftBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

	public PinShaftBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
