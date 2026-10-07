package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Candy Bowl: an orange terracotta bowl with a jack-o'-lantern grin, for trick-or-treating at players' homes.
 * Use a treat on it to fill it; use it with an empty hand to take one (one a night for visitors, any time for whoever
 * placed it); sneak-use it to see how many are left. It shows how full it is ({@link #FILL}). All year, all on the
 * server ({@link CandyBowlBlockEntity}).
 */
public class CandyBowlBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
	private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);

	public CandyBowlBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The start of the keys of the messages it shows. */
	protected String messages() {
		return "message.jugcraft.candy_bowl.";
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl) {
			bowl.setOwner(player.getUUID());
		}
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(CandyBagItem.TREATS)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl) {
			int added = bowl.add(stack);
			player.sendOverlayMessage(added > 0 ? Component.translatable(messages() + "filled", bowl.count(), CandyBowlBlockEntity.CAPACITY)
					: Component.translatable(messages() + "full"));
			if (added > 0) {
				level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof CandyBowlBlockEntity bowl)) {
			return InteractionResult.SUCCESS;
		}
		if (player.isSecondaryUseActive()) {
			player.sendOverlayMessage(Component.translatable(messages() + "count", bowl.count(), CandyBowlBlockEntity.CAPACITY));
			return InteractionResult.SUCCESS;
		}
		CandyBowlBlockEntity.Taken taken = bowl.take(player, TrickOrTreat.night(server.getOverworldClockTime()));
		player.sendOverlayMessage(Component.translatable(messages() + taken.name().toLowerCase(Locale.ROOT)));
		if (taken == CandyBowlBlockEntity.Taken.TAKEN) {
			level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CandyBowlBlockEntity(pos, state);
	}

	/** Whether this bowl invites trick-or-treaters on Halloween nights ({@link TrickOrTreaters}). */
	protected boolean invitesTrickOrTreaters() {
		return true;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || !invitesTrickOrTreaters() || type != JugcraftAgriculture.CANDY_BOWL_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((CandyBowlBlockEntity) entity).serverTick((ServerLevel) tickLevel);
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
		builder.add(FACING, FILL);
	}
}
