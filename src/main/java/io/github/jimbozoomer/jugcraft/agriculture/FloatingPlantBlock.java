package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plant floating on still water (tools/plants.py kind "surface"), like a lily pad: it rests on water (or ice) with
 * nothing but air above, and its item is placed on the water's surface. Duckweed is one.
 */
public class FloatingPlantBlock extends VegetationBlock {
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 1.5, 15.0);

	public FloatingPlantBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		FluidState fluid = level.getFluidState(pos);
		FluidState above = level.getFluidState(pos.above());
		return (fluid.getType() == Fluids.WATER || state.getBlock() instanceof IceBlock) && above.getType() == Fluids.EMPTY;
	}
}
