package io.github.jimbozoomer.jugcraft.lair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Hollow Acre's blighted crops: Black Wheat standing in its field, and the Mown Circle's stubble. They never grow. */
public class LairPlantBlock extends VegetationBlock {
	private final VoxelShape shape;

	public LairPlantBlock(int height, Properties properties) {
		super(properties);
		this.shape = Block.box(1, 0, 1, 15, height, 15);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape;
	}
}
