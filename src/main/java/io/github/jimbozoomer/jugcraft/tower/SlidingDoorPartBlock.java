package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cell of a {@link SlidingDoorBlock} other than its master, for every kind of sliding door: it knows its offset from the
 * master ({@link #OX} and {@link #OZ} less {@link #SPAN}, and {@link #OY}) and collides as the door does in its cell.
 * Without its master it is a whole cell, easy to clear. It has no item: breaking it breaks the door, which drops one.
 */
public final class SlidingDoorPartBlock extends Block {
	/** The farthest a cell can be from its master across (the entrance is 6 wide). */
	public static final int SPAN = 5;
	public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2 * SPAN);
	public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 2);
	public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2 * SPAN);
	/** Whether mobs may plan a way through this cell ({@link SlidingDoorStyle#walkable}). */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty WALK =
			net.minecraft.world.level.block.state.properties.BooleanProperty.create("walk");
	private static final VoxelShape STRAY = Shapes.block();

	public SlidingDoorPartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OX, SPAN + 1).setValue(OY, 0).setValue(OZ, SPAN).setValue(WALK, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OX, OY, OZ, WALK);
	}

	public static Vec3i offset(BlockState state) {
		return new Vec3i(state.getValue(OX) - SPAN, state.getValue(OY), state.getValue(OZ) - SPAN);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Vec3i o = offset(state);
		return pos.offset(-o.getX(), -o.getY(), -o.getZ());
	}

	/** The master's state, if it is there and this cell is one of its (never loading its chunk to find out). */
	static @Nullable BlockState master(BlockGetter level, BlockPos pos, BlockState state) {
		BlockPos at = masterPos(pos, state);
		if (level instanceof LevelReader reader && !reader.hasChunkAt(at)) {
			return null;
		}
		BlockState m = level.getBlockState(at);
		return m.getBlock() instanceof SlidingDoorBlock door
				&& SlidingDoorBlock.footprint(door.style, m.getValue(SlidingDoorBlock.FACING)).contains(offset(state)) ? m : null;
	}

	private static VoxelShape shape(BlockState state, BlockState m, boolean collision) {
		SlidingDoorBlock door = (SlidingDoorBlock) m.getBlock();
		Direction facing = m.getValue(SlidingDoorBlock.FACING);
		int[] cell = SlidingDoorBlock.frameCell(offset(state), facing);
		return SlidingDoorBlock.shape(door.style, m.getValue(SlidingDoorBlock.MIRRORED), facing, cell[0], cell[1],
				collision && m.getValue(SlidingDoorBlock.OPEN));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState m = master(level, pos, state);
		if (m == null) {
			return STRAY;
		}
		VoxelShape s = shape(state, m, false);
		return s.isEmpty() ? Block.box(0, 0, 0, 16, 2, 16) : s;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState m = master(level, pos, state);
		return m == null ? STRAY : shape(state, m, true);
	}

	/** Mobs plan their way through the doorway's cells (it opens for them as they come); a sidelight's or a header's they don't. */
	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return type == PathComputationType.LAND && state.getValue(WALK);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		BlockState m = master(level, pos, state);
		return m == null ? ItemStack.EMPTY : new ItemStack(m.getBlock());
	}

	/** Broken by a player, the whole door goes, dropping its item (the master's loot). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && master(level, pos, state) != null) {
			level.destroyBlock(masterPos(pos, state), !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However else it goes, its door goes with it (which then clears its other parts). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this) || master(level, pos, state) == null) {
			return;
		}
		level.removeBlock(masterPos(pos, state), false);
	}

	private static BlockState withOffset(BlockState state, int[] t) {
		if (Math.abs(t[0]) > SPAN || Math.abs(t[1]) > SPAN) {
			return state;
		}
		return state.setValue(OX, t[0] + SPAN).setValue(OZ, t[1] + SPAN);
	}

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		Vec3i o = offset(state);
		return withOffset(state, CurvedVideoWallPartBlock.turn(o.getX(), o.getZ(), rotation));
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		Vec3i o = offset(state);
		return withOffset(state, CurvedVideoWallPartBlock.flip(o.getX(), o.getZ(), mirror));
	}
}
