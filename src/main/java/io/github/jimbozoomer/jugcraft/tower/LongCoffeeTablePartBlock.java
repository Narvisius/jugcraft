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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A cell of a {@link LongCoffeeTableBlock} other than its master: it knows its offset from the master ({@link #OX} and
 * {@link #OZ} less {@link LongCoffeeTableBlock#REACH}) and collides with the part of the table's top in its cell. Without its
 * master (while a tower places its blocks, or left over) it is a whole cell, easy to clear. It has no item: breaking it
 * breaks the table, which drops one.
 */
public final class LongCoffeeTablePartBlock extends Block {
	public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2 * LongCoffeeTableBlock.REACH);
	public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2 * LongCoffeeTableBlock.REACH);
	private static final VoxelShape STRAY = Shapes.block();

	public LongCoffeeTablePartBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(OX, LongCoffeeTableBlock.REACH - 1).setValue(OZ, LongCoffeeTableBlock.REACH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OX, OZ);
	}

	/** This part's offset from its master. */
	public static Vec3i offset(BlockState state) {
		return new Vec3i(state.getValue(OX) - LongCoffeeTableBlock.REACH, 0, state.getValue(OZ) - LongCoffeeTableBlock.REACH);
	}

	public static BlockPos masterPos(BlockPos pos, BlockState state) {
		Vec3i o = offset(state);
		return pos.offset(-o.getX(), 0, -o.getZ());
	}

	/** The master's state, if it is there and this cell is one of its (never loading its chunk to find out). */
	private static @Nullable BlockState master(BlockGetter level, BlockPos pos, BlockState state) {
		BlockPos at = masterPos(pos, state);
		if (level instanceof LevelReader reader && !reader.hasChunkAt(at)) {
			return null;
		}
		BlockState m = level.getBlockState(at);
		return m.getBlock() instanceof LongCoffeeTableBlock && LongCoffeeTableBlock.footprint(m.getValue(LongCoffeeTableBlock.FACING)).contains(offset(state))
				? m : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState m = master(level, pos, state);
		if (m == null) {
			return STRAY;
		}
		Vec3i o = offset(state);
		Direction facing = m.getValue(LongCoffeeTableBlock.FACING);
		return LongCoffeeTableBlock.shape(o.getX(), o.getZ(), facing);
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
		return new ItemStack(JugcraftTower.BLOCKS.get("long_coffee_table"));
	}

	/** Broken by a player, the whole table goes, dropping its item (the master's loot). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && master(level, pos, state) != null) {
			level.destroyBlock(masterPos(pos, state), !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** However else it goes, its table goes with it (the table then clears its other parts). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this) || master(level, pos, state) == null) {
			return;
		}
		level.removeBlock(masterPos(pos, state), false);
	}

	private static BlockState withOffset(BlockState state, int[] t) {
		int reach = LongCoffeeTableBlock.REACH;
		if (Math.abs(t[0]) > reach || Math.abs(t[1]) > reach) {
			return state;
		}
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
