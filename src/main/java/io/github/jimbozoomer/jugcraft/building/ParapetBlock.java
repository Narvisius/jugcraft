package io.github.jimbozoomer.jugcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Bastion Parapet (batch 55): a crenellated top for a gun deck. A half-height footing carries two merlons with a
 * gap (the crenel) between them, running the way the player faced when placing it, so a gunner can stand behind a row of
 * them and see and shoot out through the gaps. Its shape matches the model.
 */
public class ParapetBlock extends HorizontalDirectionalBlock {
	/** Facing north or south, the merlons stand at the west and east; facing east or west, at the north and south. */
	private static final VoxelShape ACROSS_X = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 6, 16, 16),
			Block.box(10, 8, 0, 16, 16, 16));
	private static final VoxelShape ACROSS_Z = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 8, 0, 16, 16, 6),
			Block.box(0, 8, 10, 16, 16, 16));

	public ParapetBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ACROSS_X : ACROSS_Z;
	}
}
