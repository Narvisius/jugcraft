package io.github.jimbozoomer.jugcraft.drone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A drone control room screen panel: a thin panel mounted on a wall, facing the player who placed it.
 * Six panels in a wall, 3 wide and 2 tall, all facing the same way and touching no other panel,
 * combine into one big display ({@code part} 1 to 6, row by row from the top left as seen from the
 * front). The combined screen links to the nearest drone terminal within {@link #LINK_RANGE} blocks and
 * shows that depot live: fleet, flights, power and supply. Loose panels show a standby pattern.
 */
public class ControlScreenBlock extends BaseEntityBlock implements DepotDisplayBlockEntity.Display {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final int WIDTH = 3;
	public static final int HEIGHT = 2;
	/** 0 = loose panel; 1..6 = tile of a formed 3x2 screen, row by row from the top left. */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, WIDTH * HEIGHT);
	/** Panel thickness in pixels. */
	public static final int THICKNESS = 3;
	public static final int LINK_RANGE = 16;
	private static final int MAX_GROUP = 16;

	public ControlScreenBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double t = THICKNESS;
		return switch (state.getValue(FACING)) {
			case NORTH -> Block.box(0, 0, 16 - t, 16, 16, 16);
			case SOUTH -> Block.box(0, 0, 0, 16, 16, t);
			case WEST -> Block.box(16 - t, 0, 0, 16, 16, 16);
			default -> Block.box(0, 0, 0, t, 16, 16);
		};
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DepotDisplayBlockEntity(JugcraftDrones.SCREEN_ENTITY, pos, state);
	}

	@Override
	public boolean isAnchor(BlockState state) {
		return state.getValue(PART) == 1;
	}

	@Override
	public int linkRange() {
		return LINK_RANGE;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, JugcraftDrones.SCREEN_ENTITY, DepotDisplayBlockEntity::serverTick);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!level.isClientSide() && !oldState.is(this)) {
			reform(level, pos, state.getValue(FACING));
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		if (level.getBlockState(pos).is(this)) {
			return; // only a state change
		}
		Direction facing = state.getValue(FACING);
		Direction right = facing.getCounterClockWise();
		for (BlockPos next : new BlockPos[] {pos.above(), pos.below(), pos.relative(right), pos.relative(right.getOpposite())}) {
			BlockState other = level.getBlockState(next);
			if (other.is(this) && other.getValue(FACING) == facing) {
				reform(level, next, facing);
			}
		}
	}

	/** Direction to the viewer's right when looking at a screen that faces {@code facing}. */
	public static Direction right(Direction facing) {
		return facing.getCounterClockWise();
	}

	/**
	 * Works out the group of touching panels (same facing, in one wall) around {@code start}, and gives
	 * them their tile numbers if they are exactly one 3x2 screen, or sets them all loose otherwise.
	 */
	static void reform(Level level, BlockPos start, Direction facing) {
		Direction right = right(facing);
		Set<BlockPos> group = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start.immutable());
		group.add(start.immutable());
		boolean tooBig = false;
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (BlockPos next : new BlockPos[] {at.above(), at.below(), at.relative(right), at.relative(right.getOpposite())}) {
				if (group.contains(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof ControlScreenBlock && state.getValue(FACING) == facing) {
					if (group.size() >= MAX_GROUP) {
						tooBig = true;
						continue;
					}
					group.add(next.immutable());
					queue.add(next.immutable());
				}
			}
		}
		int minR = Integer.MAX_VALUE, maxR = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (BlockPos pos : group) {
			int r = along(pos, right);
			minR = Math.min(minR, r);
			maxR = Math.max(maxR, r);
			minY = Math.min(minY, pos.getY());
			maxY = Math.max(maxY, pos.getY());
		}
		boolean formed = !tooBig && group.size() == WIDTH * HEIGHT && maxR - minR + 1 == WIDTH && maxY - minY + 1 == HEIGHT;
		List<BlockPos> sorted = new ArrayList<>(group);
		for (BlockPos pos : sorted) {
			int part = formed ? 1 + (maxY - pos.getY()) * WIDTH + (along(pos, right) - minR) : 0;
			BlockState state = level.getBlockState(pos);
			if (state.getValue(PART) != part) {
				level.setBlock(pos, state.setValue(PART, part), Block.UPDATE_CLIENTS);
			}
			if (part == 1 && level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity anchor) {
				anchor.relink();
			}
		}
	}

	/** Position along the {@code right} direction. */
	private static int along(BlockPos pos, Direction right) {
		return pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ();
	}
}
