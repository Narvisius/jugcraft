package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A squash or decorative gourd: the fruit a {@link GourdStemBlock} grows beside itself, like a vanilla
 * pumpkin. It is an ordinary block for fall displays and a food ingredient; it faces the way it grew
 * (or towards the player who placed it), which only matters for the elongated butternut squash.
 */
public class GourdBlock extends HorizontalDirectionalBlock {
	private final VoxelShape northSouth;
	private final VoxelShape eastWest;

	/** {@code northSouth} is the shape facing north or south; {@code eastWest} the same shape turned a quarter. */
	public GourdBlock(Properties properties, VoxelShape northSouth, VoxelShape eastWest) {
		super(properties);
		this.northSouth = northSouth;
		this.eastWest = eastWest;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? northSouth : eastWest;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
