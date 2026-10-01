package io.github.jimbozoomer.jugcraft.tools;

import io.github.jimbozoomer.jugcraft.energy.EnergyConnectable;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The charging station: two blocks tall, with a cradle at chest height that holds one powered tool
 * (or the rocket pack) and charges it from cables. Use it with a tool to hang the tool up; use it with
 * an empty hand to take the tool back. The lower half holds the block entity; either half connects to
 * cables and passes use to it.
 */
public class ChargingStationBlock extends BaseEntityBlock implements EnergyConnectable {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape LOWER = Shapes.or(Block.box(0, 0, 0, 16, 2, 16), Block.box(1, 2, 3, 15, 16, 16));
	private static final VoxelShape UPPER = Block.box(1, 0, 3, 15, 15.5, 16);

	public ChargingStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
				.setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, LIT);
	}

	/** Faces the player; needs room for the upper half. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos above = context.getClickedPos().above();
		Level level = context.getLevel();
		if (above.getY() > level.getMaxY() || !level.getBlockState(above).canBeReplaced(context)) {
			return null;
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
	}

	/** The block holding the station's block entity. */
	public static BlockPos lowerPos(BlockState state, BlockPos pos) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new ChargingStationBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel) || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
			return null;
		}
		return createTickerHelper(type, JugcraftTools.CHARGING_STATION_ENTITY,
				(tickLevel, pos, tickState, station) -> station.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof UpgradeModuleItem module) {
			return fitModule(stack, module, state, level, pos, player);
		}
		if (!(stack.getItem() instanceof Chargeable)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(level.getBlockEntity(lowerPos(state, pos)) instanceof ChargingStationBlockEntity station) || !station.tool().isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			station.setTool(stack.split(1));
		}
		return InteractionResult.SUCCESS;
	}

	/** Fits an upgrade module into the tool on the cradle (see {@link ToolUpgrades}). */
	private static InteractionResult fitModule(ItemStack stack, UpgradeModuleItem module, BlockState state, Level level, BlockPos pos,
			Player player) {
		if (!(level.getBlockEntity(lowerPos(state, pos)) instanceof ChargingStationBlockEntity station) || station.tool().isEmpty()) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			ItemStack tool = station.tool().copy();
			Component moduleName = stack.getHoverName();
			ToolUpgrades.Result result = ToolUpgrades.fit(level, tool, module.kind());
			if (result == ToolUpgrades.Result.FITTED) {
				station.setTool(tool);
				stack.consume(1, player);
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.module." + result.name().toLowerCase(Locale.ROOT),
					moduleName, tool.getHoverName()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(lowerPos(state, pos)) instanceof ChargingStationBlockEntity station)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			ItemStack tool = station.tool();
			if (!tool.isEmpty()) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.charging_station.tool", tool.getHoverName(),
						String.format("%,d", Chargeable.energy(tool)), String.format("%,d", Chargeable.capacity(tool))));
				station.setTool(ItemStack.EMPTY);
				if (!player.getInventory().add(tool)) {
					Block.popResource(level, pos, tool);
				}
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.charging_station",
						String.format("%,d", station.energy().getAmount()), String.format("%,d", station.energy().getCapacity())));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			EnergyNetworks.invalidate(level);
		}
	}

	/** Removing either half removes the other; the lower half drops the station (see its loot table). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		EnergyNetworks.invalidate(level);
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		BlockPos other = lower ? pos.above() : pos.below();
		BlockState otherState = level.getBlockState(other);
		if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
			level.destroyBlock(other, !lower);
		}
	}
}
