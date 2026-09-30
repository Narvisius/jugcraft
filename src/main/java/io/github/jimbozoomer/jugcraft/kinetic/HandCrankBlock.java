package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The hand crank: the first kinetic source. Place it against a shaft, gearbox or machine; each
 * right-click keeps it turning for {@link HandCrankBlockEntity#TICKS_PER_CRANK} more ticks (up to
 * {@link HandCrankBlockEntity#MAX_TICKS}), pushing {@link HandCrankBlockEntity#OUTPUT} KE/t into the
 * block it faces. Cranking makes the player a little hungry.
 */
public class HandCrankBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);

	public HandCrankBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ShaftBlock.TURNING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ShaftBlock.TURNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// Faces (drives) the block it was placed against.
		return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HandCrankBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, JugcraftKinetics.HAND_CRANK_ENTITY,
				(tickLevel, pos, tickState, crank) -> crank.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank) {
			crank.crank(player);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.hand_crank", crank.secondsLeft()));
		}
		return InteractionResult.SUCCESS;
	}
}
