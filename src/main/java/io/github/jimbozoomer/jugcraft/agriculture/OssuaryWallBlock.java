package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * An Ossuary Wall (the churchyard's ornaments): a full block of a catacomb's wall, rows of skulls stacked between courses
 * of long bones laid end-out, set facing whoever placed it so the skulls look out at them.
 */
public class OssuaryWallBlock extends HorizontalDirectionalBlock {
	public OssuaryWallBlock(Properties properties) {
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
