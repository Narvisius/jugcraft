package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;

/**
 * The yellow safety railing. It joins like a fence, in any direction, to the railings and railing gates beside it, so
 * a run of it is continuous round corners and steps: a top rail, a knee rail and a kick plate. A post stands at
 * corners, ends and junctions, and every other block along a straight run. It joins nothing else, so a blueprint's
 * railing always comes out as drawn.
 */
public class RailingBlock extends CrossCollisionBlock {
	public static final BooleanProperty POST = BooleanProperty.create("post");

	public RailingBlock(Properties properties) {
		super(3.0F, 2.0F, 16.0F, 16.0F, 24.0F, properties);
		registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
				.setValue(WATERLOGGED, false).setValue(POST, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, WATERLOGGED, POST);
	}

	/** Whether a railing joins the block beside it. */
	public static boolean joins(BlockState neighbor, Direction towards) {
		if (neighbor.getBlock() instanceof RailingBlock) {
			return true;
		}
		// A gate joins along its line (across its swing).
		return neighbor.getBlock() instanceof RailingGateBlock && neighbor.getValue(RailingGateBlock.FACING).getAxis() != towards.getAxis();
	}

	/** The state for a railing at {@code pos} with these neighbours joined. */
	public static BlockState shaped(BlockState state, BlockPos pos, boolean n, boolean e, boolean s, boolean w) {
		boolean straight = n && s && !e && !w || e && w && !n && !s;
		boolean post = !straight || Math.floorMod(pos.getX() + pos.getZ(), 2) == 0;
		return state.setValue(NORTH, n).setValue(EAST, e).setValue(SOUTH, s).setValue(WEST, w).setValue(POST, post);
	}

	private BlockState connected(BlockGetter level, BlockPos pos, BlockState state) {
		return shaped(state, pos, joins(level.getBlockState(pos.north()), Direction.NORTH), joins(level.getBlockState(pos.east()), Direction.EAST),
				joins(level.getBlockState(pos.south()), Direction.SOUTH), joins(level.getBlockState(pos.west()), Direction.WEST));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
		return connected(context.getLevel(), context.getClickedPos(), state);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED)) {
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		return direction.getAxis().isHorizontal() ? connected(level, pos, state) : state;
	}
}
