package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The Autumn Leaf Wall Garland (Thanksgiving decorations, tools/thanksgiving_decor.py PROPS, kind "garland"): a swag of
 * leaves hung on a wall like a {@link WallDecorationBlock}, which joins the same garland hung beside it on the same wall
 * facing the same way, so a row of them hangs as one garland, tied off with a bow and a tail of leaves only at its two
 * ends. {@link #LEFT} is joined to the neighbour counter-clockwise of its facing (the model's west side, facing north),
 * {@link #RIGHT} to the one clockwise of it. Only the two neighbours beside it are looked at, when one changes.
 */
public class LeafGarlandBlock extends WallDecorationBlock {
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	/** {@code depth}: how far it stands out from the wall; {@code low} and {@code high}: its extent, in pixels. */
	public LeafGarlandBlock(Properties properties, double depth, double low, double high) {
		super(properties, depth, low, high);
		registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
	}

	/** Whether {@code other} is this garland hung on the same wall, facing the way {@code state} faces. */
	private boolean joins(BlockState state, BlockState other) {
		return other.is(this) && other.getValue(FACING) == state.getValue(FACING);
	}

	private BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		return state.setValue(LEFT, joins(state, level.getBlockState(pos.relative(facing.getCounterClockWise()))))
				.setValue(RIGHT, joins(state, level.getBlockState(pos.relative(facing.getClockWise()))));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : joined(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		BlockState updated = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
		return updated.is(this) && direction.getAxis() == state.getValue(FACING).getClockWise().getAxis() ? joined(updated, level, pos) : updated;
	}

	/** A mirror swaps which side each neighbour is on. */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState mirrored = super.mirror(state, mirror);
		return mirror == Mirror.NONE ? mirrored : mirrored.setValue(LEFT, state.getValue(RIGHT)).setValue(RIGHT, state.getValue(LEFT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LEFT, RIGHT);
	}
}
