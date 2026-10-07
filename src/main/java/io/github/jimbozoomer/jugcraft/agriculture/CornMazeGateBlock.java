package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Corn Maze Gate: two hay-wrapped posts and a crossbar with a green pennant, the way into a corn maze
 * ({@link CornMazeGateBlockEntity}). It faces into the maze (away from whoever placed it), which is planted ahead of it.
 * <ul>
 * <li>Sneak and use it with an empty hand to choose the maze's size before planting: tiny, small, medium or large.</li>
 * <li>Use it holding corn kernels to plant the maze: one kernel for each stalk of wall it can plant, taken from your
 * inventory (it says how many it needs if you have too few).</li>
 * <li>Use it with an empty hand to see the maze's size and its board of best times.</li>
 * <li>Walk out through it to start the clock; reach the finish post at the far side to stop it.</li>
 * </ul>
 */
public class CornMazeGateBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final String MESSAGES = "message.jugcraft.corn_maze.";
	private static final VoxelShape POSTS_NS = Shapes.or(Block.box(0.0, 0.0, 6.0, 2.0, 16.0, 10.0), Block.box(14.0, 0.0, 6.0, 16.0, 16.0, 10.0));
	private static final VoxelShape POSTS_EW = Shapes.or(Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 2.0), Block.box(6.0, 0.0, 14.0, 10.0, 16.0, 16.0));
	private static final VoxelShape SHAPE_NS = Shapes.or(POSTS_NS, Block.box(0.0, 13.0, 7.0, 16.0, 15.0, 9.0));
	private static final VoxelShape SHAPE_EW = Shapes.or(POSTS_EW, Block.box(7.0, 13.0, 0.0, 9.0, 15.0, 16.0));

	public CornMazeGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** The posts and crossbar of a gate (or finish post) facing {@code facing}. */
	static VoxelShape shape(Direction facing) {
		return facing.getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
	}

	/** Only the posts stand in the way: runners walk through between them. */
	static VoxelShape posts(Direction facing) {
		return facing.getAxis() == Direction.Axis.Z ? POSTS_NS : POSTS_EW;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return posts(state.getValue(FACING));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CornMazeGateBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.CORN_MAZE_GATE_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((CornMazeGateBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		Item kernels = JugcraftAgriculture.item("corn_kernels");
		if (!stack.is(kernels)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof CornMazeGateBlockEntity gate)) {
			return InteractionResult.SUCCESS;
		}
		if (gate.planted()) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "already"));
			return InteractionResult.SUCCESS;
		}
		long seed = server.getRandom().nextLong();
		List<BlockPos> columns = gate.survey(server, seed);
		if (columns == null) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "blocked"));
			return InteractionResult.SUCCESS;
		}
		int needed = columns.size();
		if (!player.getAbilities().instabuild) {
			if (player.getInventory().countItem(kernels) < needed) {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "needs", needed));
				return InteractionResult.SUCCESS;
			}
			int left = needed;
			for (int slot = 0; slot < player.getInventory().getContainerSize() && left > 0; slot++) {
				ItemStack carried = player.getInventory().getItem(slot);
				if (carried.is(kernels)) {
					int taken = Math.min(left, carried.getCount());
					carried.shrink(taken);
					left -= taken;
				}
			}
			player.getInventory().setChanged();
		}
		gate.plant(server, seed, columns);
		player.sendOverlayMessage(Component.translatable(MESSAGES + "planting", Component.translatable(MESSAGES + "size." + CornMaze.SIZES[gate.size()]),
				needed));
		level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer viewer) || !(level.getBlockEntity(pos) instanceof CornMazeGateBlockEntity gate)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			if (gate.nextSize()) {
				CornMaze maze = gate.maze();
				player.sendOverlayMessage(Component.translatable(MESSAGES + "size_set", Component.translatable(MESSAGES + "size." + CornMaze.SIZES[gate.size()]),
						maze.side(), maze.side()));
				level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.2F);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "already"));
			}
		} else {
			gate.showBoard(viewer);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
