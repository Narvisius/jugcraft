package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * A Monster Mash Dance Floor tile: black glass over a grid of coloured lamps. A tile next to a jukebox with a disc in it,
 * or with a redstone signal, lights up ({@link #DISTANCE} 0), and passes it on to the tiles beside it, one step further
 * each, up to {@value #REACH} tiles ({@link #DISTANCE} {@value #REACH} is dark): the same rule leaves use to know how far
 * they are from a log, worked out by scheduled ticks as neighbours change, so it settles by itself. Lit tiles (light
 * {@value #LIGHT}) pulse in rippling colours (drawn by the client, client/DanceFloorRenderer.java), and villagers on
 * them hop and spin now and then (a random tick).
 */
public class DanceFloorBlock extends BaseEntityBlock {
	public static final int REACH = 8;
	public static final int LIGHT = 8;
	public static final IntegerProperty DISTANCE = IntegerProperty.create("distance", 0, REACH);

	public DanceFloorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(DISTANCE, REACH));
	}

	public static boolean lit(BlockState state) {
		return state.getValue(DISTANCE) < REACH;
	}

	public static int light(BlockState state) {
		return lit(state) ? LIGHT : 0;
	}

	/** How far the tile at {@code pos} is from the music, by its neighbours. */
	public static int distance(LevelReader level, BlockPos pos, boolean powered) {
		if (powered) {
			return 0;
		}
		int best = REACH;
		for (Direction direction : Direction.values()) {
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			if (neighbor.getBlock() instanceof JukeboxBlock && neighbor.getValue(JukeboxBlock.HAS_RECORD)) {
				return 0;
			}
			if (neighbor.getBlock() instanceof DanceFloorBlock) {
				best = Math.min(best, neighbor.getValue(DISTANCE) + 1);
			}
		}
		return best;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.DANCE_FLOOR_ENTITY, pos, state);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		ticks.scheduleTick(pos, this, 1);
		return state;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
		super.neighborChanged(state, level, pos, neighbor, orientation, moved);
		level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int distance = distance(level, pos, level.hasNeighborSignal(pos));
		if (distance != state.getValue(DISTANCE)) {
			level.setBlock(pos, state.setValue(DISTANCE, distance), Block.UPDATE_ALL);
		}
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return lit(state);
	}

	/** Villagers on a lit tile hop and spin. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(pos.above()))) {
			villager.getJumpControl().jump();
			villager.setYRot(villager.getYRot() + 90.0F);
			villager.setYHeadRot(villager.getYRot());
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(DISTANCE);
	}
}
