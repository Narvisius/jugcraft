package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Costume Trunk: a steamer trunk that keeps up to {@value CostumeTrunkBlockEntity#SLOTS} costumes
 * ({@link CostumeTrunkBlockEntity}).
 * <ul>
 * <li>Use it holding a costume to pack it away.</li>
 * <li>Use it with an empty hand to change: you put on the costume at the front, and the one you were wearing goes in at
 * the back (a helmet that isn't a costume has to come off first).</li>
 * <li>Sneak and use it with an empty hand to take out the costume packed last.</li>
 * </ul>
 * Its lid opens for {@value #OPEN_TICKS} ticks when it is used; comparators read how full it is.
 */
public class CostumeTrunkBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final int OPEN_TICKS = 20;
	private static final String MESSAGES = "message.jugcraft.costume_trunk.";
	private static final VoxelShape ALONG_X = Block.box(0.5, 0.0, 2.5, 15.5, 12.5, 13.5);
	private static final VoxelShape ALONG_Z = Block.box(2.5, 0.0, 0.5, 13.5, 12.5, 15.5);

	public CostumeTrunkBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_X : ALONG_Z;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CostumeTrunkBlockEntity(pos, state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A costume in hand is packed away. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(TrickOrTreat.COSTUMES)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CostumeTrunkBlockEntity trunk) {
			if (trunk.store(stack)) {
				stack.consume(1, player);
				player.sendOverlayMessage(Component.translatable(MESSAGES + "stored", trunk.count(), CostumeTrunkBlockEntity.SLOTS));
				open(level, pos, state, player);
			} else {
				player.sendOverlayMessage(Component.translatable(MESSAGES + "full"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** An empty hand changes into the next costume; sneaking, it takes out the last one. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof CostumeTrunkBlockEntity trunk)) {
			return InteractionResult.SUCCESS;
		}
		if (trunk.count() == 0) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "empty"));
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			ItemStack taken = trunk.takeLast();
			player.setItemInHand(InteractionHand.MAIN_HAND, taken);
			player.sendOverlayMessage(Component.translatable(MESSAGES + "taken", taken.getHoverName()));
			open(level, pos, state, player);
			return InteractionResult.SUCCESS;
		}
		ItemStack worn = player.getItemBySlot(EquipmentSlot.HEAD);
		if (!worn.isEmpty() && !worn.is(TrickOrTreat.COSTUMES)) {
			player.sendOverlayMessage(Component.translatable(MESSAGES + "helmet"));
			return InteractionResult.SUCCESS;
		}
		ItemStack next = trunk.takeNext();
		if (!worn.isEmpty()) {
			trunk.store(worn);
		}
		player.setItemSlot(EquipmentSlot.HEAD, next);
		player.sendOverlayMessage(Component.translatable(MESSAGES + "changed", next.getHoverName()));
		level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		open(level, pos, state, player);
		return InteractionResult.SUCCESS;
	}

	private void open(Level level, BlockPos pos, BlockState state, Player player) {
		if (!state.getValue(OPEN)) {
			level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.6F, 0.9F);
			level.gameEvent(player, GameEvent.CONTAINER_OPEN, pos);
		}
		level.scheduleTick(pos, this, OPEN_TICKS);
	}

	/** The lid falls shut. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(OPEN)) {
			level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.6F, 0.9F);
			level.gameEvent(null, GameEvent.CONTAINER_CLOSE, pos);
		}
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** How full it is: 0 when empty, 15 with all nine. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		int count = level.getBlockEntity(pos) instanceof CostumeTrunkBlockEntity trunk ? trunk.count() : 0;
		return count == 0 ? 0 : 1 + (count - 1) * 14 / (CostumeTrunkBlockEntity.SLOTS - 1);
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
		builder.add(FACING, OPEN);
	}
}
