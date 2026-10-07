package io.github.jimbozoomer.jugcraft.building;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Sliding Gate (fortification extras): a panel of heavy steel bars, open only by redstone, like the blast door. Gates
 * placed side by side and stacked (facing the same way) form one gate: a signal into any of them slides them all aside
 * together, up to {@value #MAX_GATE} panels. Closed, it is a full-height barrier you can see and shoot through; open,
 * only the slim end post at its side remains and you walk through.
 */
public class SlidingGateBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	/** The most panels one gate can have. */
	public static final int MAX_GATE = 64;
	private static final VoxelShape CLOSED_NS = Block.box(0, 0, 6, 16, 16, 10);
	private static final VoxelShape CLOSED_EW = Block.box(6, 0, 0, 10, 16, 16);
	private static final VoxelShape OPEN_NS = Block.box(0, 0, 6, 2, 16, 10);
	private static final VoxelShape OPEN_EW = Block.box(6, 0, 0, 10, 16, 2);

	public SlidingGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, POWERED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(POWERED, powered)
				.setValue(OPEN, powered);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean ns = state.getValue(FACING).getAxis() == Direction.Axis.Z;
		return state.getValue(OPEN) ? ns ? OPEN_NS : OPEN_EW : ns ? CLOSED_NS : CLOSED_EW;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
			boolean movedByPiston) {
		if (level.isClientSide()) {
			return;
		}
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
		// The whole gate is open while any panel of it is powered.
		Set<BlockPos> gate = gate(level, pos, state.getValue(FACING).getAxis());
		boolean open = false;
		for (BlockPos panel : gate) {
			open |= level.getBlockState(panel).getValue(POWERED);
		}
		boolean changed = false;
		for (BlockPos panel : gate) {
			BlockState here = level.getBlockState(panel);
			if (here.getValue(OPEN) != open) {
				level.setBlock(panel, here.setValue(OPEN, open), Block.UPDATE_ALL);
				changed = true;
			}
		}
		if (changed) {
			level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.6F);
		}
	}

	/** Every panel of the gate {@code start} belongs to: gates facing along the same axis, side by side or stacked. */
	public static Set<BlockPos> gate(Level level, BlockPos start, Direction.Axis facingAxis) {
		Direction along = facingAxis == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start.immutable());
		seen.add(start.immutable());
		while (!queue.isEmpty() && seen.size() < MAX_GATE) {
			BlockPos here = queue.poll();
			for (Direction step : new Direction[] {along, along.getOpposite(), Direction.UP, Direction.DOWN}) {
				BlockPos next = here.relative(step);
				BlockState state = level.getBlockState(next);
				if (!seen.contains(next) && state.getBlock() instanceof SlidingGateBlock
						&& state.getValue(FACING).getAxis() == facingAxis && seen.size() < MAX_GATE) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		return seen;
	}

	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return state.getValue(OPEN);
	}
}
