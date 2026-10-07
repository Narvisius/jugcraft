package io.github.jimbozoomer.jugcraft.control;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
 * A control monitor panel (batch 37): a thin screen hung on a wall. Six panels, {@link #WIDTH} wide and {@link #HEIGHT}
 * tall, facing the same way and touching no other panel, form one control-room display ({@code part} 1 to 6 from the
 * top left as seen from the front). A data cable from any of its panels to a logic controller links it; it then shows
 * every channel's reading, a bar, a two-minute graph and whether the channel is on. Loose panels show nothing.
 */
public class ControlMonitorBlock extends Block implements EntityBlock, DataConnectable {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final int WIDTH = 3;
	public static final int HEIGHT = 2;
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, WIDTH * HEIGHT);
	public static final int THICKNESS = 3;
	private static final int MAX_GROUP = 16;

	public ControlMonitorBlock(Properties properties) {
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
		return new ControlMonitorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || type != JugcraftControl.MONITOR_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((ControlMonitorBlockEntity) entity).serverTick((ServerLevel) tickLevel, pos, tickState);
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
			return;
		}
		Direction facing = state.getValue(FACING);
		Direction right = right(facing);
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

	/** The positions of a formed screen's six panels, from its anchor (part 1, top left). */
	public static BlockPos[] panels(BlockPos anchor, Direction facing) {
		Direction right = right(facing);
		BlockPos[] out = new BlockPos[WIDTH * HEIGHT];
		for (int row = 0; row < HEIGHT; row++) {
			for (int column = 0; column < WIDTH; column++) {
				out[row * WIDTH + column] = anchor.relative(right, column).below(row);
			}
		}
		return out;
	}

	/** Numbers the touching panels around {@code start} if they are exactly one 3x2 screen, or sets them loose. */
	static void reform(Level level, BlockPos start, Direction facing) {
		Direction right = right(facing);
		Set<BlockPos> group = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		group.add(start.immutable());
		queue.add(start.immutable());
		boolean tooBig = false;
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			for (BlockPos next : new BlockPos[] {at.above(), at.below(), at.relative(right), at.relative(right.getOpposite())}) {
				if (group.contains(next)) {
					continue;
				}
				BlockState state = level.getBlockState(next);
				if (state.getBlock() instanceof ControlMonitorBlock && state.getValue(FACING) == facing) {
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
		for (BlockPos pos : group) {
			int part = formed ? 1 + (maxY - pos.getY()) * WIDTH + (along(pos, right) - minR) : 0;
			BlockState state = level.getBlockState(pos);
			if (state.getValue(PART) != part) {
				level.setBlock(pos, state.setValue(PART, part), Block.UPDATE_CLIENTS);
			}
			if (part == 1 && level.getBlockEntity(pos) instanceof ControlMonitorBlockEntity anchor) {
				anchor.relink();
			}
		}
	}

	private static int along(BlockPos pos, Direction right) {
		return pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ();
	}
}
