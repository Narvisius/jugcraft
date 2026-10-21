package io.github.jimbozoomer.jugcraft.finance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Market Chest, drawn as a clerk's counter: a company's shop. Only a player who controls a company can place one;
 * goods put in it are listed on the Commodities market for that company at the prices set on its screen, and the
 * money goes to the company's treasury ({@link MarketChestBlockEntity}). Only the company's controller (and operators)
 * may open or break it.
 */
public class MarketChestBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** The counter's body and its overhanging top. */
	private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 13, 15), Block.box(0, 13, 0, 16, 16, 16));

	public MarketChestBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MarketChestBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, ExchangeBlocks.MARKET_CHEST_ENTITY,
				(tickLevel, pos, tickState, chest) -> chest.serverTick((ServerLevel) tickLevel));
	}

	/** Refuses the placement (server side) for a player who controls no company. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Player player = context.getPlayer();
		if (player instanceof ServerPlayer server && !context.getLevel().isClientSide()) {
			boolean controls = !Equities.get(server.level().getServer()).controlledBy(server.getUUID()).isEmpty();
			if (!controls && !FinanceService.isAdmin(server)) {
				server.sendOverlayMessage(Component.literal("Only a player who controls a company can set up a Market Chest."));
				return null;
			}
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity player,
            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, player, stack);
        if (player instanceof ServerPlayer owner && level.getBlockEntity(pos) instanceof MarketChestBlockEntity chest) chest.setOwner(owner);
    }

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && player instanceof ServerPlayer server
				&& level.getBlockEntity(pos) instanceof MarketChestBlockEntity chest) {
			chest.open(server);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}
}
