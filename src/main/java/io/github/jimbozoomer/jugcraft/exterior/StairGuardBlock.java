package io.github.jimbozoomer.jugcraft.exterior;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper part of a stair's guard rail (what is drawn is the stair's own handrail): over the top cell of each column
 * of a {@link SteelStairBlock} section on an open side, a thin wall 1.5 blocks high along that side, so with the stair
 * cell's own the rail stands 1.5 blocks over every tread. Nothing to see or aim at; the section puts it there and
 * takes it away, and it goes by itself when the stair under it does.
 */
public class StairGuardBlock extends Block {
	public static final EnumProperty<Direction> FACING = SteelStairBlock.FACING;
	public static final EnumProperty<SteelStairBlock.Side> SIDE = SteelStairBlock.SIDE;
	private static final Map<Direction, VoxelShape[]> WALLS = new EnumMap<>(Direction.class);

	static {
		for (Direction f : Direction.Plane.HORIZONTAL) {
			WALLS.put(f, new VoxelShape[] {ExteriorShapes.turned(f, new double[] {0, 0, 0, SteelStairBlock.GUARD_T, SteelStairBlock.GUARD_H, 16}),
					ExteriorShapes.turned(f, new double[] {16 - SteelStairBlock.GUARD_T, 0, 0, 16, SteelStairBlock.GUARD_H, 16})});
		}
	}

	public StairGuardBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIDE, SteelStairBlock.Side.RIGHT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return WALLS.get(state.getValue(FACING))[state.getValue(SIDE).ordinal()];
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.DOWN && !(neighborState.getBlock() instanceof SteelStairBlock)) {
			return Blocks.AIR.defaultBlockState();
		}
		return state;
	}

	@Override
	protected boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
		return true;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return true;
	}
}
