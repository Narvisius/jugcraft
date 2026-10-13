package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** A continuous two-metre material sheet; its eight phases require no ticking or block entity. */
public final class TowerPanelBlock extends Block {
	public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, 7);

	public TowerPanelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PHASE, 0));
	}

	public static BlockState at(BlockState state, BlockPos pos) {
		return state.getBlock() instanceof TowerPanelBlock
				? state.setValue(PHASE, (pos.getX() & 1) | ((pos.getY() & 1) << 1) | ((pos.getZ() & 1) << 2)) : state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PHASE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return at(defaultBlockState(), context.getClickedPos());
	}
}
