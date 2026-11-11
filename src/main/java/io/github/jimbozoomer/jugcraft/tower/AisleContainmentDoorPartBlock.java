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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cell of an {@link AisleContainmentDoorBlock} other than its master: it knows its offset from the master ({@link #OX}
 * and {@link #OZ} less one, and {@link #OY}) and collides as the door does in its cell (its depth while shut, the header
 * open). Without its master it is a whole cell, easy to clear. It has no item: breaking it breaks the door, which drops one.
 */
public final class AisleContainmentDoorPartBlock extends Block {
	public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2);
	public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 1);
	public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2);
	private static final VoxelShape STRAY = Shapes.block();

	public AisleContainmentDoorPartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OX, 1).setValue(OY, 1).setValue(OZ, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OX, OY, OZ);
	}

	public static Vec3i offset(BlockState state) {
		return new Vec3i(state.getValue(OX) - 1, state.getValue(OY), state.getValue(OZ) - 1);
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
		return m.getBlock() instanceof AisleContainmentDoorBlock
				&& AisleContainmentDoorBlock.footprint(m.getValue(AisleContainmentDoorBlock.FACING)).contains(offset(state)) ? m : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState m = master(level, pos, state);
		return m == null ? STRAY : AisleContainmentDoorBlock.shape(state.getValue(OY), m.getValue(AisleContainmentDoorBlock.FACING), false);
	}

	/** Walkers plan their way through the doorway (it opens for them as they come, as the sliding doors do): shut, it was
	 * a wall to the path finder, and the technicians' way to the cold aisle went nowhere (10 November 2026). */
	@Override
	protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
		return type == net.minecraft.world.level.pathfinder.PathComputationType.LAND;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState m = master(level, pos, state);
		return m == null ? STRAY : AisleContainmentDoorBlock.shape(state.getValue(OY), m.getValue(AisleContainmentDoorBlock.FACING),
				m.getValue(AisleContainmentDoorBlock.OPEN));
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		return new ItemStack(JugcraftTower.BLOCKS.get("aisle_containment_door"));
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
		if (Math.abs(t[0]) > 1 || Math.abs(t[1]) > 1) {
			return state;
		}
		return state.setValue(OX, t[0] + 1).setValue(OZ, t[1] + 1);
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

	/** Which way the door this cell belongs to faces (north if it has none). */
	public static Direction facing(BlockGetter level, BlockPos pos, BlockState state) {
		BlockState m = master(level, pos, state);
		return m == null ? Direction.NORTH : m.getValue(AisleContainmentDoorBlock.FACING);
	}
}
