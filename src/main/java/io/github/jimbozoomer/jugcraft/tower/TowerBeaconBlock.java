package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A compact sealed beacon; its empty space does not hide or obstruct neighbouring parts. */
public final class TowerBeaconBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(3.5, 0, 3.5, 12.5, 1.2, 12.5),
			Block.box(5, 1.2, 5, 11, 8, 11), Block.box(7.1, 1.1, 11, 8.9, 2.5, 12.1));
	public TowerBeaconBlock(Properties properties) { super(properties.noOcclusion()); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
}
