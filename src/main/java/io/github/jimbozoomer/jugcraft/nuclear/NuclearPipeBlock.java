package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
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

/**
 * One of the three nuclear pipes (see {@link PipeKind}). The coolant pipe and the steam line only join equipment into
 * a unit (nothing flows block to block; the unit's model moves the heat). The circulating water conduit also feeds
 * the condenser it reaches: every water source block touching the conduit gives it {@link #INTAKE_PER_SOURCE} mB a
 * tick, up to {@link #CONDUIT_RATE} for the whole run. Ordinary fluid pipes can feed a condenser too.
 */
public class NuclearPipeBlock extends PipeBlock {
	/** mB a tick one conduit run can carry (a few conduits feed a whole plant). */
	public static final int CONDUIT_RATE = 16_000;
	/** mB a tick drawn from each water source block a conduit touches. */
	public static final int INTAKE_PER_SOURCE = 1_000;

	public final PipeKind kind;

	public NuclearPipeBlock(PipeKind kind, Properties properties) {
		super(kind.size / 2.0F, properties);
		this.kind = kind;
		BlockState state = this.stateDefinition.any();
		for (BooleanProperty property : PROPERTY_BY_DIRECTION.values()) {
			state = state.setValue(property, false);
		}
		this.registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState();
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			BlockPos next = context.getClickedPos().relative(entry.getKey());
			state = state.setValue(entry.getValue(), connectsTo(context.getLevel(), context.getClickedPos(), entry.getKey(),
					context.getLevel().getBlockState(next)));
		}
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(level, pos, direction, neighborState));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			EquipmentRegistry.of(level).touch();
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EquipmentRegistry.of(level).touch();
	}

	/** Re-checks the connections (equipment placed next to a pipe after it). */
	public BlockState refresh(BlockGetter level, BlockPos pos, BlockState state) {
		for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
			state = state.setValue(entry.getValue(), connectsTo(level, pos, entry.getKey(), level.getBlockState(pos.relative(entry.getKey()))));
		}
		return state;
	}

	private boolean connectsTo(BlockGetter level, BlockPos pos, Direction direction, BlockState neighbor) {
		if (neighbor.getBlock() instanceof NuclearPipeBlock other) {
			return other.kind == kind;
		}
		if (neighbor.getBlock() instanceof EquipmentFrameBlock || neighbor.getBlock() instanceof EquipmentAnchorBlock) {
			if (level instanceof Level real) {
				EquipmentRegistry registry = EquipmentRegistry.of(real);
				BlockPos anchor = registry.anchorOf(pos.relative(direction));
				EquipmentRegistry.Entry entry = anchor == null ? null : registry.at(anchor);
				return entry != null && entry.kind().takes(kind);
			}
			return false;
		}
		if (kind == PipeKind.WATER) {
			// The conduit's open intake end sits in water; it also joins ordinary pipes and tanks.
			if (neighbor.getFluidState().is(FluidTags.WATER) && neighbor.getFluidState().isSource()) {
				return true;
			}
			return level instanceof Level real && !neighbor.isAir()
					&& FluidStorage.SIDED.find(real, pos.relative(direction), direction.getOpposite()) != null;
		}
		return false;
	}
}
