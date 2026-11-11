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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cell the {@link CradleStrutsBlock}'s struts pass through, other than its master: it knows which cell of the pair it
 * is ({@link #INDEX} in {@link CradleStrutsBlock#CELLS}) and which way the pair faces, and collides with what of the
 * struts is in it. Without its master (while a tower places its blocks, or left over) it is a whole cell, easy to clear.
 * It has no item: breaking it breaks the struts, which drop one.
 */
public final class CradleStrutsPartBlock extends Block {
	/** How many cells a pair may have (the master's index, 0, included). */
	public static final int PARTS = 64;
	public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, PARTS - 1);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape STRAY = Shapes.block();

	public CradleStrutsPartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(INDEX, 1).setValue(FACING, Direction.SOUTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(INDEX, FACING);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Vec3i o = CradleStrutsBlock.offset(state.getValue(INDEX), state.getValue(FACING));
		return pos.offset(-o.getX(), -o.getY(), -o.getZ());
	}

	/** Whether this part's master is there, facing the same way (never loading its chunk to find out). */
	private static boolean hasMaster(BlockGetter level, BlockPos pos, BlockState state) {
		BlockPos at = masterPos(pos, state);
		if (level instanceof LevelReader reader && !reader.hasChunkAt(at)) {
			return false;
		}
		BlockState master = level.getBlockState(at);
		return master.getBlock() instanceof CradleStrutsBlock && master.getValue(CradleStrutsBlock.FACING) == state.getValue(FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!hasMaster(level, pos, state)) {
			return STRAY;
		}
		return CradleStrutsBlock.shape(state.getValue(INDEX), state.getValue(FACING));
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
		return new ItemStack(JugcraftTower.BLOCKS.get("cradle_struts"));
	}

	/** Broken by a player, the whole pair comes down, dropping its item (the master's loot). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && hasMaster(level, pos, state)) {
			level.destroyBlock(masterPos(pos, state), !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However else it goes, its master goes with it (the master then clears its other parts). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this) || !hasMaster(level, pos, state)) {
			return;
		}
		level.removeBlock(masterPos(pos, state), false);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
