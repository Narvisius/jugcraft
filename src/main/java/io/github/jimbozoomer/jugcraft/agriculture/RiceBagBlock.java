package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * A Bag of Rice (the kitchen and cooking expansion's slice 4; tools/rice.py STORAGE): nine rice in one block, the owner's
 * sack, its tied side turned to the player who set it down. It packs and unpacks by crafting. The Bag of Corn Kernels
 * (slice 5, tools/soil.py SACKS) is the same sack with the owner's kernel bag top.
 */
public class RiceBagBlock extends HorizontalDirectionalBlock {
	public RiceBagBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
