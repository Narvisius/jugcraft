package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A corn maze's finish post: the gate's twin, with a chequered flag, planted by the gate at its maze's exit. Runners walk
 * through it; the gate watches it and stops their clocks there. It drops nothing when broken (the gate plants another
 * with its next maze).
 */
public class CornMazeFinishBlock extends Block {
	public CornMazeFinishBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CornMazeGateBlock.FACING, net.minecraft.core.Direction.NORTH));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CornMazeGateBlock.shape(state.getValue(CornMazeGateBlock.FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CornMazeGateBlock.posts(state.getValue(CornMazeGateBlock.FACING));
	}

	@Override
	protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CornMazeGateBlock.FACING);
	}
}
