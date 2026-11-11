package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cell of a {@link CrystalTreeBlock} other than its master: it knows its offset from the master ({@link #OX} and
 * {@link #OZ} less {@link CrystalTreeBlock#REACH}, {@link #OY}) and collides with what of the planter, the bench or the
 * trunk is in its cell. Without its master (while a tower places its blocks, or left over) it is a whole cell, easy to
 * clear. It has no item: breaking it breaks the tree, which drops one.
 */
public final class CrystalTreePartBlock extends Block {
	public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2 * CrystalTreeBlock.REACH);
	public static final IntegerProperty OY = IntegerProperty.create("oy", 0, CrystalTreeBlock.TRUNK_CELLS);
	public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2 * CrystalTreeBlock.REACH);
	private static final VoxelShape STRAY = Shapes.block();

	public CrystalTreePartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OX, CrystalTreeBlock.REACH).setValue(OY, 1).setValue(OZ, CrystalTreeBlock.REACH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OX, OY, OZ);
	}

	/** This part's offset from its master. */
	public static Vec3i offset(BlockState state) {
		return new Vec3i(state.getValue(OX) - CrystalTreeBlock.REACH, state.getValue(OY), state.getValue(OZ) - CrystalTreeBlock.REACH);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Vec3i o = offset(state);
		return pos.offset(-o.getX(), -o.getY(), -o.getZ());
	}

	/** Whether this part's master is there (never loading its chunk to find out). */
	private static boolean hasMaster(BlockGetter level, BlockPos pos, BlockState state) {
		BlockPos at = masterPos(pos, state);
		if (level instanceof LevelReader reader && !reader.hasChunkAt(at)) {
			return false;
		}
		return level.getBlockState(at).getBlock() instanceof CrystalTreeBlock;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!hasMaster(level, pos, state)) {
			return STRAY;
		}
		Vec3i o = offset(state);
		return CrystalTreeBlock.shape(o.getX(), o.getY(), o.getZ());
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
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftTower.BLOCKS.get("crystal_tree"));
	}

	/** Broken by a player, the whole tree comes down, dropping its item (the master's loot). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && hasMaster(level, pos, state)) {
			level.destroyBlock(masterPos(pos, state), !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However else it goes, its tree goes with it (the tree then clears its other parts). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this) || !hasMaster(level, pos, state)) {
			return;
		}
		level.removeBlock(masterPos(pos, state), false);
	}

	private static BlockState withOffset(BlockState state, int[] t) {
		int reach = CrystalTreeBlock.REACH;
		return state.setValue(OX, t[0] + reach).setValue(OZ, t[1] + reach);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		Vec3i o = offset(state);
		return withOffset(state, CurvedVideoWallPartBlock.turn(o.getX(), o.getZ(), rotation));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		Vec3i o = offset(state);
		return withOffset(state, CurvedVideoWallPartBlock.flip(o.getX(), o.getZ(), mirror));
	}
}
