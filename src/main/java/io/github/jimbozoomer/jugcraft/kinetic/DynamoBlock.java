package io.github.jimbozoomer.jugcraft.kinetic;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The dynamo: the bridge from kinetic power to JE. Rotation reaching any of its faces is turned into
 * JE at {@link DynamoBlockEntity#EFFICIENCY_PERCENT}%, which it pushes into cables on every side.
 * (Machines can also run straight off a shaft, with no loss.) The magnet dynamo is the same block with
 * {@link DynamoBlockEntity#MAGNET} stats.
 */
public class DynamoBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	private final DynamoBlockEntity.Stats stats;

	public DynamoBlock(Properties properties) {
		this(properties, DynamoBlockEntity.COPPER);
	}

	public DynamoBlock(Properties properties, DynamoBlockEntity.Stats stats) {
		super(properties);
		this.stats = stats;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	public DynamoBlockEntity.Stats stats() {
		return stats;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DynamoBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftKinetics.DYNAMO_ENTITY,
				(tickLevel, pos, tickState, dynamo) -> dynamo.serverTick((ServerLevel) tickLevel, pos));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DynamoBlockEntity dynamo) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.dynamo",
					dynamo.energy.getAmount(), dynamo.energy.getCapacity()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			KineticNetworks.invalidate(level);
			EnergyNetworks.invalidate(level);
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		KineticNetworks.invalidate(level);
		EnergyNetworks.invalidate(level);
	}
}
