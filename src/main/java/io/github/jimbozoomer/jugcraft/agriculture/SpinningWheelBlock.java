package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Spinning Wheel: a big spoked wheel on a slanted bench with a treadle, spinning wool into yarn
 * ({@link SpinningWheelBlockEntity}). Use it with wool to put a skein on the distaff; with an empty hand to work the
 * treadle (a redstone pulse works it too); with a piece of knitwear to unravel it back into yarn. It faces the player
 * who places it, the wheel to their right.
 */
public class SpinningWheelBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);
	private static final String MESSAGES = "message.jugcraft.spinning_wheel.";

	public SpinningWheelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
				.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpinningWheelBlockEntity(pos, state);
	}

	/** Wool goes on the distaff; knitwear is unravelled. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		DyeColor wool = Knitting.woolColor(stack);
		Knitwear knit = Knitwear.of(stack.getItem());
		if (wool == null && knit == null) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SpinningWheelBlockEntity wheel)) {
			return InteractionResult.SUCCESS;
		}
		if (knit != null) {
			wheel.unravel((ServerLevel) level, stack, player);
			return InteractionResult.SUCCESS;
		}
		if (!wheel.loadWool(wool)) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "busy"));
			return InteractionResult.SUCCESS;
		}
		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}

	/** An empty hand works the treadle. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof SpinningWheelBlockEntity wheel)) {
			return InteractionResult.SUCCESS;
		}
		if (!wheel.turn((ServerLevel) level)) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
		}
		return InteractionResult.SUCCESS;
	}

	/** A redstone pulse (its rising edge) works the treadle once. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation,
			boolean movedByPiston) {
		boolean powered = level.hasNeighborSignal(pos);
		if (powered == state.getValue(POWERED)) {
			return;
		}
		level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
		if (powered && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof SpinningWheelBlockEntity wheel) {
			wheel.turn(server);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** 0 with no wool on the distaff; then 1 and one more for each turn spun on it. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return level.getBlockEntity(pos) instanceof SpinningWheelBlockEntity wheel && wheel.wool() != null ? 1 + wheel.turns() : 0;
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
		builder.add(FACING, POWERED);
	}
}
