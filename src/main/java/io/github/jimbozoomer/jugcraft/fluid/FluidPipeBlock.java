package io.github.jimbozoomer.jugcraft.fluid;

import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * A fluid pipe segment (bronze or steel). Like a cable it has no block entity and does no per-tick
 * work: pumps push fluid through it via {@link FluidNetworks}, and it only tells the
 * network cache to refresh when its surroundings change.
 */
public class FluidPipeBlock extends PipeBlock implements FluidConnectable {
	/** Millibuckets per tick one push may send through a bronze pipe network. */
	public static final long BRONZE_RATE_MB = 250;
	/** Millibuckets per tick one push may send through a steel pipe network (refinery flows). */
	public static final long STEEL_RATE_MB = 1_000;

	private final long rateMb;

	public FluidPipeBlock(Properties properties, long rateMb) {
		super(4.0F, properties);
		this.rateMb = rateMb;
		BlockState state = this.stateDefinition.any();
		for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		this.registerDefaultState(state);
	}

	/** Droplets per tick one push may send through this pipe; a network carries as much as its slowest pipe. */
	public long transferRate() {
		return rateMb * FluidNetworks.DROPLETS_PER_MB;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return connections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (level instanceof Level realLevel) {
			FluidNetworks.invalidate(realLevel);
		}
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction, neighborState));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		FluidNetworks.invalidate(level);
	}

	private static BlockState connections(BlockGetter level, BlockPos pos, BlockState state) {
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			Direction direction = entry.getKey();
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			state = state.setValue(entry.getValue(), connectsTo(level, pos, direction, neighbor));
		}
		return state;
	}

	/**
	 * Connects like other tech mods' transmitters: to its own kind, and to any block that exposes
	 * a fluid storage on the touching face (Jugcraft machines or other mods' blocks).
	 * World generation has no full level to query, so there only the marker interface counts.
	 */
	private static boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction, BlockState neighbor) {
		if (neighbor.getBlock() instanceof FluidConnectable) {
			return true;
		}
		return level instanceof Level realLevel && !neighbor.isAir()
				&& FluidStorage.SIDED.find(realLevel, pos.relative(direction), direction.getOpposite()) != null;
	}
}
