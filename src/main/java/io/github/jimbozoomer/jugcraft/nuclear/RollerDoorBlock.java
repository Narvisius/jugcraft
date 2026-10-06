package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A roller shutter door: build it as a wall of these blocks in an opening and it works as one door. Using any part
 * rolls the whole door up into the coil at its top, or lets it down again: the curtain slides up (or down) over a few
 * seconds, drawn by client/RollerDoorRenderer.java while it moves (MOVING), and the coil over the opening grows as it
 * winds on. FACING is the way into the building: the coil hangs on that side.
 */
public class RollerDoorBlock extends HorizontalDirectionalBlock implements EntityBlock {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty MOVING = BooleanProperty.create("moving");
	/** The biggest door that opens as one. */
	public static final int MAX_BLOCKS = 400;
	/** Ticks the curtain takes to travel one block. */
	public static final int TICKS_PER_BLOCK = 6;
	private static final VoxelShape NS = Block.box(0, 0, 6.5, 16, 16, 9.5);
	private static final VoxelShape EW = Block.box(6.5, 0, 0, 9.5, 16, 16);

	public RollerDoorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false).setValue(MOVING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, OPEN, MOVING);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RollerDoorBlockEntity(pos, state);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NS : EW;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(OPEN) ? Shapes.empty() : getShape(state, level, pos, context);
	}

	/** The rows of door in this block's column: its bottom and top. */
	public static int[] column(BlockGetter level, BlockPos pos, BlockState state) {
		Direction.Axis axis = state.getValue(FACING).getAxis();
		int bottom = pos.getY(), top = pos.getY();
		while (bottom - pos.getY() > -64 && sameDoor(level.getBlockState(pos.atY(bottom - 1)), axis)) {
			bottom--;
		}
		while (top - pos.getY() < 64 && sameDoor(level.getBlockState(pos.atY(top + 1)), axis)) {
			top++;
		}
		return new int[] {bottom, top};
	}

	private static boolean sameDoor(BlockState state, Direction.Axis axis) {
		return state.getBlock() instanceof RollerDoorBlock && state.getValue(FACING).getAxis() == axis;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (state.getValue(MOVING)) {
			return InteractionResult.SUCCESS;
		}
		if (!level.isClientSide()) {
			toggle(level, pos);
		}
		return InteractionResult.SUCCESS;
	}

	/** Opens the door the block at pos belongs to if it is shut, or shuts it if it is open (server side). */
	public static void toggle(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof RollerDoorBlock self) || state.getValue(MOVING)) {
			return;
		}
		{
			boolean open = !state.getValue(OPEN);
			Direction facing = state.getValue(FACING);
			// The whole door: every connected roller door block in the same plane.
			Set<BlockPos> door = new HashSet<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			queue.add(pos);
			door.add(pos);
			Direction along = facing.getClockWise();
			while (!queue.isEmpty() && door.size() < MAX_BLOCKS) {
				BlockPos at = queue.poll();
				for (BlockPos next : new BlockPos[] {at.above(), at.below(), at.relative(along), at.relative(along.getOpposite())}) {
					if (!door.contains(next) && level.getBlockState(next).is(self) && level.getBlockState(next).getValue(FACING).getAxis() == facing.getAxis()) {
						door.add(next);
						queue.add(next);
					}
				}
			}
			long now = level.getGameTime();
			int longest = 1;
			for (BlockPos at : door) {
				BlockState here = level.getBlockState(at);
				int[] col = column(level, at, here);
				int ticks = (col[1] - col[0] + 1) * TICKS_PER_BLOCK;
				longest = Math.max(longest, ticks);
				// The timing first, so the client has it when the block turns to moving.
				if (level.getBlockEntity(at) instanceof RollerDoorBlockEntity entity) {
					entity.start(now, ticks, open);
				}
				level.setBlock(at, here.setValue(OPEN, open).setValue(MOVING, true), Block.UPDATE_CLIENTS);
				level.scheduleTick(at, self, ticks);
			}
			level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.5F);
			level.playSound(null, pos, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 0.35F, 0.6F);
		}
	}

	/** The curtain has finished travelling. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(MOVING)) {
			level.setBlock(pos, state.setValue(MOVING, false), Block.UPDATE_CLIENTS);
		}
	}
}
