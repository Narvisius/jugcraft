package io.github.jimbozoomer.jugcraft.energy;

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
	/** JE per tick one push may send through a copper cable network. */
	public static final long COPPER_RATE = 256;

	public CableBlock(Properties properties) {
		super(4.0F, properties);
		BlockState state = this.stateDefinition.any();
		for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		this.registerDefaultState(state);
	}

	public long transferRate() {
		return COPPER_RATE;
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
			EnergyNetworks.invalidate(realLevel);
		}
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighborState));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
	}

	private static BlockState connections(BlockGetter level, BlockPos pos, BlockState state) {
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			BlockState neighbor = level.getBlockState(pos.relative(entry.getKey()));
			state = state.setValue(entry.getValue(), connectsTo(neighbor));
		}
		return state;
	}

	private static boolean connectsTo(BlockState neighbor) {
		return neighbor.getBlock() instanceof EnergyConnectable;
	}
}
