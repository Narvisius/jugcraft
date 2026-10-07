package io.github.jimbozoomer.jugcraft.machine;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A machine placed from one item that fills several blocks (see {@link Footprint}), in the style
 * of Immersive Engineering's pump and sample drill:
 * <ul>
 * <li>Placing it checks every block of the footprint is free, then fills the other blocks with
 * dummy parts ({@link #PART} 1, 2, ...). Only the master (part 0) has a block entity and ticks.</li>
 * <li>Breaking any part removes the whole machine and drops one item; the other parts are removed
 * without drops.</li>
 * <li>Right-clicks, comparators, cables, pipes and fluid containers on any part reach the master
 * ({@link MachineBlock#masterPos}).</li>
 * <li>Each part renders its own slice of one big model; the slices are cut by
 * {@code tools/generate_material_data.py} from the model in {@code tools/large_machines.py}.</li>
 * </ul>
 */
public class LargeMachineBlock extends MachineBlock implements WorldlyContainerHolder {
	/** Most blocks one machine may fill (PART_STATES in tools/model_writer.py). */
	public static final int MAX_PARTS = 64;
	/** Which block of the footprint this is; 0 is the master. */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, MAX_PARTS - 1);

	public LargeMachineBlock(Properties properties, MachineKind kind) {
		super(properties, kind);
		this.registerDefaultState(this.defaultBlockState().setValue(PART, 0));
	}

	/** The coke oven's chimney block: only the pipe, which stands at the corner shared by the four blocks below. */
	private static final int COKE_OVEN_CHIMNEY = 8;
	private static final Map<Direction, VoxelShape> CHIMNEY = Map.of(
			Direction.NORTH, Block.box(0, 0, 13, 3, 16, 16),
			Direction.EAST, Block.box(0, 0, 0, 3, 16, 3),
			Direction.SOUTH, Block.box(13, 0, 0, 16, 16, 3),
			Direction.WEST, Block.box(13, 0, 13, 16, 16, 16));

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (kind() == MachineKind.COKE_OVEN && state.getValue(PART) == COKE_OVEN_CHIMNEY) {
			return CHIMNEY.get(state.getValue(FACING));
		}
		return super.getShape(state, level, pos, context);
	}

	@Override
	public Footprint footprint(BlockState state) {
		return kind().footprint();
	}

	/** The state a newly placed machine gets, built to its full footprint (see {@link EnlargedMachineBlock}). */
	public BlockState formed(BlockState state) {
		return state;
	}

	@Override
	public BlockPos masterPos(BlockPos pos, BlockState state) {
		return footprint(state).masterPos(pos, state.getValue(FACING), state.getValue(PART));
	}

	@Override
	public int part(BlockState state) {
		return state.getValue(PART);
	}

	/** Hoppers (and Fabric item transfer) reach the master's slots through any part. */
	@Override
	public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
		return level.getBlockEntity(masterPos(pos, state)) instanceof MachineBlockEntity machine ? machine : null;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PART) == 0 ? super.newBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return state.getValue(PART) == 0 ? super.getTicker(level, state, type) : null;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = formed(super.getStateForPlacement(context));
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction facing = state.getValue(FACING);
		for (int part = 1; part < footprint(state).size(); part++) {
			BlockPos partPos = footprint(state).partPos(pos, facing, part);
			if (level.isOutsideBuildHeight(partPos) || !level.getWorldBorder().isWithinBounds(partPos)
					|| !level.getBlockState(partPos).canBeReplaced(BlockPlaceContext.at(context, partPos, Direction.UP))) {
				return null;
			}
		}
		return state.setValue(PART, 0);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide()) {
			return;
		}
		Direction facing = state.getValue(FACING);
		for (int part = 1; part < footprint(state).size(); part++) {
			level.setBlock(footprint(state).partPos(pos, facing, part), state.setValue(PART, part), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(this)) {
			return; // Only a state change (such as LIT), not a removal.
		}
		// Remove the other parts without drops: the part that was broken already dropped the item. A tank that keeps
		// its fluid drops from its master instead (its other parts drop nothing), so the master is broken with drops.
		Direction facing = state.getValue(FACING);
		BlockPos master = masterPos(pos, state);
		if (kind().keepsContents() && state.getValue(PART) != 0 && level.getBlockState(master).is(this)) {
			level.destroyBlock(master, true);
			return;
		}
		for (int part = 0; part < footprint(state).size(); part++) {
			BlockPos partPos = footprint(state).partPos(master, facing, part);
			BlockState other = level.getBlockState(partPos);
			if (!partPos.equals(pos) && other.is(this) && other.getValue(FACING) == facing && other.getValue(PART) == part) {
				level.removeBlock(partPos, false);
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return super.useWithoutItem(state, level, masterPos(pos, state), player, hit);
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return super.getAnalogOutputSignal(state, level, masterPos(pos, state), direction);
	}

	/** Rotating or mirroring one part alone would tear the machine apart, so structures leave it as placed. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(PART);
	}
}
