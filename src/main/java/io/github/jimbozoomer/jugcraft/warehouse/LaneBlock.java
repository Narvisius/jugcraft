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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/**
 * A drone lane floor tile (the floor block itself: drones drive on its top). MASK: which of its edges (north 1, east
 * 2, south 4, west 8) have no lane beyond them and so show the yellow line; CODE: which of four navigation code
 * stickers it shows (by where it lies, so a run of tiles doesn't repeat one).
 */
public class LaneBlock extends Block implements WarehouseBlock {
	public static final IntegerProperty MASK = IntegerProperty.create("mask", 0, 15);
	public static final IntegerProperty CODE = IntegerProperty.create("code", 0, 3);
	/** Part W3: what its slab shows underneath on an upper floor ({@link Soffit}). */
	public static final net.minecraft.world.level.block.state.properties.EnumProperty<Soffit> SOFFIT = Soffit.PROPERTY;
	private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public LaneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(MASK, 0).setValue(CODE, 0).setValue(SOFFIT, Soffit.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MASK, CODE, SOFFIT);
	}

	/** Lane tiles, drone crossings and the pallet build spot floor join up (no line between them). */
	public static boolean laneLike(BlockState state) {
		return state.getBlock() instanceof LaneBlock || state.getBlock() instanceof PackSpotBlock || state.getBlock() instanceof CrossingBlock;
	}

	/** A tile with a soffit under it is its topping only: it hides nothing beside or under it. */
	@Override
	protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
		return state.getValue(SOFFIT) == Soffit.NONE ? net.minecraft.world.phys.shapes.Shapes.block() : net.minecraft.world.phys.shapes.Shapes.empty();
	}

	/** A floor slab: no light through it (from one floor to the next). */
	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		return state.setValue(MASK, turnMask(state.getValue(MASK), rotation)).setValue(SOFFIT, state.getValue(SOFFIT).rotate(rotation));
	}

	/** An edge mask (north 1, east 2, south 4, west 8) turned with a blueprint. */
	static int turnMask(int mask, net.minecraft.world.level.block.Rotation rotation) {
		int turns = switch (rotation) {
			case CLOCKWISE_90 -> 1;
			case CLOCKWISE_180 -> 2;
			case COUNTERCLOCKWISE_90 -> 3;
			default -> 0;
		};
		return ((mask << turns) | (mask >> (4 - turns))) & 15;
	}

	static int mask(BlockGetter level, BlockPos pos) {
		int mask = 0;
		for (int i = 0; i < 4; i++) {
			if (!laneLike(level.getBlockState(pos.relative(EDGES[i])))) {
				mask |= 1 << i;
			}
		}
		return mask;
	}

	static int code(BlockPos pos) {
		return Math.floorMod(pos.getX() * 7 + pos.getZ() * 13, 4);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos pos = context.getClickedPos();
		return defaultBlockState().setValue(MASK, mask(context.getLevel(), pos)).setValue(CODE, code(pos));
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
			int mask = laneLike(neighborState) ? state.getValue(MASK) & ~bit : state.getValue(MASK) | bit;
			return state.setValue(MASK, mask);
		}
		return state;
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
