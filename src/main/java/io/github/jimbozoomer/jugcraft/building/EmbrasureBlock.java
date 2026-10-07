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
 * The Bastion Embrasure (fortification extras): a full block of bastion concrete with a narrow gun slit through it, two
 * pixels high and six wide at eye height, running the way its placer looked. A defender behind it sees and shoots out
 * through the slit; arrows, bolts and shells from outside mostly meet concrete. Its shape matches the model.
 */
public class EmbrasureBlock extends HorizontalDirectionalBlock {
	/** The slit runs along z (facing north or south) or along x. */
	private static final VoxelShape ALONG_Z = Shapes.or(Block.box(0, 0, 0, 16, 9, 16), Block.box(0, 11, 0, 16, 16, 16),
			Block.box(0, 9, 0, 5, 11, 16), Block.box(11, 9, 0, 16, 11, 16));
	private static final VoxelShape ALONG_X = Shapes.or(Block.box(0, 0, 0, 16, 9, 16), Block.box(0, 11, 0, 16, 16, 16),
			Block.box(0, 9, 0, 16, 11, 5), Block.box(0, 9, 11, 16, 11, 16));

	public EmbrasureBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_Z : ALONG_X;
	}
}
