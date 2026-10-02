package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The flywheel: a heavy steel wheel on a bearing frame that stores rotation. Shafts feeding any face but its front
 * spin it up; its front shaft (facing the way the player looked when placing it) drives what it faces from the
 * wheel's store, so it carries a line through gaps in a bursty source such as a hand crank or an engine short of fuel.
 * Right-click it to read how much it holds.
 */
public class FlywheelBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;

	public FlywheelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ShaftBlock.TURNING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ShaftBlock.TURNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FlywheelBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftKinetics.FLYWHEEL_ENTITY,
				(tickLevel, pos, tickState, flywheel) -> flywheel.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FlywheelBlockEntity flywheel) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.flywheel",
					String.format("%,d", flywheel.stored()), String.format("%,d", FlywheelBlockEntity.CAPACITY)));
		}
		return InteractionResult.SUCCESS;
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
}
