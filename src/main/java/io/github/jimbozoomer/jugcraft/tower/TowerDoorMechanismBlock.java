package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static hangar tracks and drive housings; only the client renderer moves the door leaves. */
public final class TowerDoorMechanismBlock extends HorizontalDirectionalBlock {
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
	private static final double[][] BOUNDS = {
			{0, 0, 6.2, 16, 16, 16}, {0, 0, 2.7, 16, 16, 16}, {0, 0, 4.8, 16, 16, 16},
			{0, 4, 6.2, 16, 16, 16}, {0, 4, .5, 16, 16, 16}, {0, 0, 0, 16, 3, 16}};

	public TowerDoorMechanismBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return FurnitureBlock.turned(BOUNDS[state.getValue(PART)], state.getValue(FACING));
	}
}
