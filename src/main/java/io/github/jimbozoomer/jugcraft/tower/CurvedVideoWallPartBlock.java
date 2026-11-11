package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cell of a {@link CurvedVideoWallBlock} other than its master: it knows its offset from the master ({@link #OX} and
 * {@link #OZ} less {@link CurvedVideoWallBlock#SPAN}, {@link #OY}) and collides with the wall's body in its cell (from the
 * master's aim; its shape depends on the master, so the block has a dynamic shape). It has no item: breaking it breaks
 * the wall, which drops one. Using it uses the wall.
 */
public final class CurvedVideoWallPartBlock extends Block {
	public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2 * CurvedVideoWallBlock.SPAN);
	public static final IntegerProperty OY = IntegerProperty.create("oy", 0, CurvedVideoWallBlock.HEIGHT - 1);
	public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2 * CurvedVideoWallBlock.SPAN);
	/** A part whose wall has gone (or is not built yet, while a tower places its blocks): a whole cell, easy to clear. */
	private static final VoxelShape STRAY = Shapes.block();

	public CurvedVideoWallPartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OX, CurvedVideoWallBlock.SPAN).setValue(OY, 1).setValue(OZ, CurvedVideoWallBlock.SPAN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OX, OY, OZ);
	}

	/** This part's offset from its master. */
	public static Vec3i offset(BlockState state) {
		return new Vec3i(state.getValue(OX) - CurvedVideoWallBlock.SPAN, state.getValue(OY), state.getValue(OZ) - CurvedVideoWallBlock.SPAN);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Vec3i o = offset(state);
		return pos.offset(-o.getX(), -o.getY(), -o.getZ());
	}

	/** The master's state, when the master is there and this part is one of its cells; else null. */
	private static @Nullable BlockState master(BlockGetter level, BlockPos pos, BlockState state) {
		BlockPos at = masterPos(pos, state);
		// (never loads the master's chunk just to work out a shape)
		if (level instanceof LevelReader reader && !reader.hasChunkAt(at)) {
			return null;
		}
		BlockState master = level.getBlockState(at);
		Vec3i o = offset(state);
		if (master.getBlock() instanceof CurvedVideoWallBlock
				&& !CurvedVideoWallBlock.shape(master.getValue(CurvedVideoWallBlock.AIM), o.getX(), o.getZ()).isEmpty()) {
			return master;
		}
		return null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState master = master(level, pos, state);
		if (master == null) {
			return STRAY;
		}
		Vec3i o = offset(state);
		return CurvedVideoWallBlock.shape(master.getValue(CurvedVideoWallBlock.AIM), o.getX(), o.getZ());
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShape(state, level, pos, context);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return master(level, pos, state) == null ? InteractionResult.PASS : CurvedVideoWallBlock.nextBoard(level, masterPos(pos, state), player);
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftTower.BLOCKS.get("curved_video_wall"));
	}

	/** Broken by a player, the whole wall comes down, dropping its item (the master's loot). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && master(level, pos, state) != null) {
			level.destroyBlock(masterPos(pos, state), !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However else it goes, its wall goes with it (the wall then clears its other parts). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this) || master(level, pos, state) == null) {
			return;
		}
		level.removeBlock(masterPos(pos, state), false);
	}

	/** (x, z) turned with a structure. */
	static int[] turn(int x, int z, Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> new int[] {-z, x};
			case CLOCKWISE_180 -> new int[] {-x, -z};
			case COUNTERCLOCKWISE_90 -> new int[] {z, -x};
			default -> new int[] {x, z};
		};
	}

	/** (x, z) mirrored with a structure. */
	static int[] flip(int x, int z, Mirror mirror) {
		return switch (mirror) {
			case LEFT_RIGHT -> new int[] {x, -z};
			case FRONT_BACK -> new int[] {-x, z};
			default -> new int[] {x, z};
		};
	}

	private static BlockState withOffset(BlockState state, int x, int z) {
		int span = CurvedVideoWallBlock.SPAN;
		if (Math.abs(x) > span || Math.abs(z) > span) {
			return state;
		}
		return state.setValue(OX, x + span).setValue(OZ, z + span);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		Vec3i o = offset(state);
		int[] t = turn(o.getX(), o.getZ(), rotation);
		return withOffset(state, t[0], t[1]);
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Vec3i o = offset(state);
		int[] t = flip(o.getX(), o.getZ(), mirror);
		return withOffset(state, t[0], t[1]);
	}
}
