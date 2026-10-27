package io.github.jimbozoomer.jugcraft.lair.king;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jspecify.annotations.Nullable;

/**
 * The King's Lantern (docs/features/pumpkin-king.md): an heirloom pumpkin carved with the Pumpkin King's grin round one of
 * his embers, which never goes out: a full block giving light {@value JugcraftPumpkinKing#LANTERN_LIGHT}, its face toward
 * whoever placed it. Made from a King's Ember and any heirloom pumpkin; it breaks back into itself.
 */
public class KingsLanternBlock extends HorizontalDirectionalBlock {
	public KingsLanternBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
}
