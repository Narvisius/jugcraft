package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A structural I-section: collision and neighbour occlusion agree with its recessed flange geometry. */
public final class TowerGirderBlock extends RotatedPillarBlock {
	private static final VoxelShape Y = Shapes.or(Block.box(0, 0, 0, 16, 16, 3),
			Block.box(0, 0, 13, 16, 16, 16), Block.box(6, 0, 3, 10, 16, 13));
	private static final VoxelShape X = Shapes.or(Block.box(0, 0, 0, 16, 16, 3),
			Block.box(0, 0, 13, 16, 16, 16), Block.box(0, 6, 3, 16, 10, 13));
	private static final VoxelShape Z = Shapes.or(Block.box(0, 0, 0, 16, 3, 16),
			Block.box(0, 13, 0, 16, 16, 16), Block.box(6, 3, 0, 10, 13, 16));

	public TowerGirderBlock(Properties properties) {
		super(properties.noOcclusion());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction.Axis axis = state.getValue(AXIS);
		return axis == Direction.Axis.X ? X : axis == Direction.Axis.Z ? Z : Y;
	}
}
