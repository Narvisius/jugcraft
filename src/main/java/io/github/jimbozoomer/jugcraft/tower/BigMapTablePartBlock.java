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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A cell of the {@link BigMapTableBlock} other than its master: it knows which cell of the table it is ({@link #INDEX} in
 * {@link BigMapTableBlock#CELLS}) and collides with what of the table is in it. Without its master (while a tower places
 * its blocks, or left over) it is a whole cell, easy to clear. It has no item: breaking it breaks the table, which drops one.
 */
public final class BigMapTablePartBlock extends Block {
	/** How many cells a table may have (the master's index, 0, included): its 13 by 13 square. */
	public static final int PARTS = 169;
	public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, PARTS - 1);
	private static final VoxelShape STRAY = Shapes.block();

	public BigMapTablePartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(INDEX, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(INDEX);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Vec3i o = BigMapTableBlock.offset(state.getValue(INDEX));
		return pos.offset(-o.getX(), -o.getY(), -o.getZ());
	}

	/** Whether this part's master is there (never loading its chunk to find out). */
	private static boolean hasMaster(BlockGetter level, BlockPos pos, BlockState state) {
		BlockPos at = masterPos(pos, state);
		if (level instanceof LevelReader reader && !reader.hasChunkAt(at)) {
			return false;
		}
		return level.getBlockState(at).getBlock() instanceof BigMapTableBlock;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!hasMaster(level, pos, state)) {
			return STRAY;
		}
		return BigMapTableBlock.shape(state.getValue(INDEX));
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
		return new ItemStack(JugcraftTower.BLOCKS.get("big_map_table"));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return hasMaster(level, pos, state) ? BigMapTableBlock.use(level, masterPos(pos, state), player) : InteractionResult.PASS;
	}

	/** Broken by a player, the whole table comes down, dropping its item (the master's loot). */
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
}
