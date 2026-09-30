package io.github.jimbozoomer.jugcraft.logistics;

import io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A conveyor: carries items (and anyone standing on it) the way it faces, while rotation drives it.
 * Shafts, gearboxes and motors drive it through any side; one drive runs every conveyor joined to it,
 * for {@link ConveyorBlockEntity#KE_PER_CONVEYOR} KE per conveyor per tick. Items come on from pipes,
 * hoppers, machines, other conveyors, or by being dropped on it, and go off into the conveyor or
 * inventory in front, or onto the ground. A splitter sends items left, straight on and right in turn.
 */
public class ConveyorBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

	private final boolean splitter;

	public ConveyorBlock(Properties properties, boolean splitter) {
		super(properties);
		this.splitter = splitter;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ShaftBlock.TURNING, false));
	}

	public boolean isSplitter() {
		return splitter;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ShaftBlock.TURNING);
	}

	/** Faces away from the player, so items run the way they look. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ConveyorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return createTickerHelper(type, JugcraftLogistics.CONVEYOR_ENTITY, level.isClientSide()
				? (tickLevel, pos, tickState, conveyor) -> conveyor.clientTick(tickState)
				: (tickLevel, pos, tickState, conveyor) -> conveyor.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	/** A running conveyor carries mobs and players along, up to the items' speed. */
	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		if (state.getValue(ShaftBlock.TURNING) && !(entity instanceof ItemEntity) && !entity.isShiftKeyDown()) {
			Direction facing = state.getValue(FACING);
			Vec3 motion = entity.getDeltaMovement();
			double along = motion.x * facing.getStepX() + motion.z * facing.getStepZ();
			if (along < ConveyorBlockEntity.SPEED) {
				double push = (ConveyorBlockEntity.SPEED - along) * 0.5;
				entity.setDeltaMovement(motion.add(facing.getStepX() * push, 0, facing.getStepZ() * push));
			}
		}
		super.stepOn(level, pos, state, entity);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			KineticNetworks.invalidate(level);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
			Orientation orientation, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
	}
}
