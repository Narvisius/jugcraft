package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/**
 * The Hand in a Jar (Halloween decorations batch 17): a severed hand floating in yellowed fluid, drumming its fingers.
 * While it has a redstone signal ({@link #POWERED}) it points at the nearest player within
 * {@value OddityJarBlock#WATCH_RANGE} blocks (drawn by the client).
 */
public class HandJarBlock extends OddityJarBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public HandJarBlock(Properties properties) {
		super(properties, Kind.HAND);
		registerDefaultState(stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		boolean powered = level.hasNeighborSignal(pos);
		if (!level.isClientSide() && powered != state.getValue(POWERED)) {
			level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}
}
