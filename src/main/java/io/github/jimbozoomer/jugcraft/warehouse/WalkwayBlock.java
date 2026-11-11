package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Part W3's floor tiles for people (tools/warehouse_w3.py): the Pedestrian Walkway (green, a yellow line on each edge
 * with no walkway or crossing beyond it: MASK as a lane tile's; STENCIL the walking figure on some, by where it lies)
 * and the Drone Crossing (the zebra bars along AXIS, the drones' way over the walkway): drones keep off walkways and
 * drive over crossings like lane tiles (Network). Both have a SOFFIT on an upper floor, as lane tiles.
 */
public class WalkwayBlock extends Block implements WarehouseBlock {
	public static final IntegerProperty MASK = LaneBlock.MASK;
	public static final BooleanProperty STENCIL = BooleanProperty.create("stencil");
	public static final EnumProperty<Soffit> SOFFIT = Soffit.PROPERTY;
	private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public WalkwayBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MASK, 0).setValue(STENCIL, false).setValue(SOFFIT, Soffit.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MASK, STENCIL, SOFFIT);
	}

	static boolean walkLike(BlockState state) {
		return state.getBlock() instanceof WalkwayBlock || state.getBlock() instanceof CrossingBlock;
	}

	static int mask(BlockGetter level, BlockPos pos) {
		int mask = 0;
		for (int i = 0; i < 4; i++) {
			if (!walkLike(level.getBlockState(pos.relative(EDGES[i])))) {
				mask |= 1 << i;
			}
		}
		return mask;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		return defaultBlockState().setValue(MASK, mask(context.getLevel(), pos)).setValue(STENCIL, Math.floorMod(pos.getX() * 5 + pos.getZ() * 3, 7) == 0);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction.getAxis().isHorizontal()) {
			int bit = 1 << switch (direction) {
				case NORTH -> 0;
				case EAST -> 1;
				case SOUTH -> 2;
				default -> 3;
			};
			return state.setValue(MASK, walkLike(neighborState) ? state.getValue(MASK) & ~bit : state.getValue(MASK) | bit);
		}
		return state;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(SOFFIT) == Soffit.NONE ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(MASK, LaneBlock.turnMask(state.getValue(MASK), rotation)).setValue(SOFFIT, state.getValue(SOFFIT).rotate(rotation));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			Warehouses.changed(level, pos);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		Warehouses.changed(level, pos);
	}
}
