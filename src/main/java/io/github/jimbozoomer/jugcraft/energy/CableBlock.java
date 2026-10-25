package io.github.jimbozoomer.jugcraft.energy;

import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import java.util.Map;
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
 * A cable segment. It has no block entity and does no per-tick work: producers push
 * energy through it via {@link EnergyNetworks}, and it only tells the network cache to
 * refresh when its surroundings change.
 */
public class CableBlock extends PipeBlock implements EnergyConnectable {
	/** JE per tick one push may send through a network of each cable tier (CABLES in tools/machines.py). */
	public static final long COPPER_RATE = 256;
	public static final long SILVER_RATE = 1_024;
	public static final long ALUMINUM_RATE = 4_096;

	private final long rate;

	public CableBlock(Properties properties, long rate) {
		super(6.0F, properties);
		this.rate = rate;
		BlockState state = this.stateDefinition.any();
		for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		this.registerDefaultState(state);
	}

	/** JE per tick through this cable; a network carries as much as its slowest cable. */
	public long transferRate() {
		return rate;
	}
	public long transferRate(BlockState state){return transferRate();}

	/** A closed switch is a network boundary, not a zero-rate member that stalls its entire neighboring bus. */
	public boolean conducts(BlockState state) { return transferRate(state) > 0; }
	public boolean observesTransfers() { return false; }
	public void onEnergyTransferred(Level level, BlockPos pos, long amount) {}

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
			EnergyNetworks.invalidate(realLevel);
		}
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction, neighborState));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
		// Structures and commands place default states without getStateForPlacement. Resolve
		// their arms as well, after neighbours and their block entities have been installed.
		if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
		if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState connected = connections(level, pos, state);
		if (connected != state) level.setBlock(pos, connected, Block.UPDATE_ALL);
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
	 * an energy storage on the touching face (Jugcraft machines or other mods' blocks).
	 * World generation has no full level to query, so there only the marker interface counts.
	 */
	public static boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction, BlockState neighbor) {
		if (neighbor.getBlock() instanceof MachineBlock machine) {
			// Only faces that really take power: a machine's power socket, never an unpowered machine.
			return machine.acceptsPower(neighbor, direction.getOpposite());
		}
		if (neighbor.getBlock() instanceof EnergyConnectable) {
			return true;
		}
		return level instanceof Level realLevel && !neighbor.isAir()
				&& EnergyStorage.SIDED.find(realLevel, pos.relative(direction), direction.getOpposite()) != null;
	}
}
