package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.fluid.FluidConnectable;
import io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A tank's roof vent: a gooseneck with its flame arrester and screen. With a pipe laid on top of it, it is the foot of
 * a vent line instead (a flanged standpipe with the arrester, the gooseneck at the line's far end): an indoor tank's
 * vent piped out through the building's roof. Pipes join it like any fluid fitting.
 */
public class TankVentBlock extends FurnitureBlock implements FluidConnectable {
	public static final BooleanProperty RISER = BooleanProperty.create("riser");

	public TankVentBlock(Properties properties) {
		super(properties, new double[] {4.0, 0, 0.8, 12.0, 15.6, 12.0});
		registerDefaultState(defaultBlockState().setValue(RISER, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(RISER);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		boolean riser = context.getLevel().getBlockState(context.getClickedPos().above()).getBlock() instanceof FluidPipeBlock;
		return defaultBlockState().setValue(FACING, facing).setValue(RISER, riser);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP) {
			return state.setValue(RISER, neighborState.getBlock() instanceof FluidPipeBlock);
		}
		return state;
	}
}
