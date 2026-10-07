package io.github.jimbozoomer.jugcraft.building;

import java.util.Map;
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
 * The Bastion Parapet Corner (fortification extras): the parapet's half-height footing with one big merlon on its outer
 * corner, to turn a ring of parapets round a corner of a gun deck. Facing north (the way its placer looked), the merlon
 * stands at the front-left; it turns with its facing. Its shape matches the model.
 */
public class ParapetCornerBlock extends HorizontalDirectionalBlock {
	private static final VoxelShape FOOTING = Block.box(0, 0, 0, 16, 8, 16);
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Shapes.or(FOOTING, Block.box(0, 8, 0, 7, 16, 7)),
			Direction.EAST, Shapes.or(FOOTING, Block.box(9, 8, 0, 16, 16, 7)),
			Direction.SOUTH, Shapes.or(FOOTING, Block.box(9, 8, 9, 16, 16, 16)),
			Direction.WEST, Shapes.or(FOOTING, Block.box(0, 8, 9, 7, 16, 16)));

	public ParapetCornerBlock(Properties properties) {
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
		return SHAPES.get(state.getValue(FACING));
	}
}
