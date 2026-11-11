package io.github.jimbozoomer.jugcraft.tv;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The surround speaker (Justin, 27 October 2026; approved from the mockup sheet tools/tv_speaker.py draws): a NARV-TECH
 * SR-6 on its stand, two blocks tall, turned any of sixteen ways (like a sign) to face the seats. Placed, it links
 * itself to the nearest screen within {@link TvSpeakerBlockEntity#AUTO_LINK} blocks; a Sync Key links it to another
 * ({@link SyncKeyItem}). Each player's game plays a linked screen's sound from every speaker on it too, as well as from
 * the screen (client/tv/TvPlayer), so speakers round a room make surround sound. Right-clicked with an empty hand it
 * says what it's linked to. Removing either half removes the other; the lower half (with the link) drops the speaker.
 */
public class TvSpeakerBlock extends Block implements EntityBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
	/** The stand (base plate and column), and the column with the cabinet over it (any way it's turned). */
	private static final VoxelShape LOWER = Shapes.or(Block.box(5.5, 0, 5.5, 10.5, 1.0, 10.5), Block.box(7.4, 0, 7.4, 8.6, 16, 8.6));
	private static final VoxelShape UPPER = Shapes.or(Block.box(7.4, 0, 7.4, 8.6, 9.2, 8.6), Block.box(5.2, 9.0, 5.2, 10.8, 15.6, 10.8));

	public TvSpeakerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, ROTATION);
	}

	/** Faces the player who places it; needs room for its upper half. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos above = context.getClickedPos().above();
		Level level = context.getLevel();
		if (above.getY() > level.getMaxY() || !level.getBlockState(above).canBeReplaced(context)) {
			return null;
		}
		return defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation() + 180.0F));
	}

	/** The upper half goes on; the speaker links itself to the nearest screen (and says so). */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
		if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof TvSpeakerBlockEntity speaker) {
			BlockPos screen = TvSpeakerBlockEntity.nearestScreen(server, pos, TvSpeakerBlockEntity.AUTO_LINK);
			speaker.link(screen);
			if (placer instanceof Player player) {
				player.sendOverlayMessage(screen == null ? Component.translatable("message.jugcraft.tv_speaker.none", TvSpeakerBlockEntity.AUTO_LINK)
						: Component.translatable("message.jugcraft.tv_speaker.auto", screen.getX(), screen.getY(), screen.getZ()));
			}
			if (screen != null) {
				server.playSound(null, pos.above(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.4F, 1.6F);
			}
		}
	}

	/** The lower half's position (where its link is kept). */
	public static BlockPos lower(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
	}

	/** A Sync Key is used on the speaker (tv/SyncKeyItem), not the speaker's status said. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		return stack.getItem() instanceof SyncKeyItem ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(lower(pos, state)) instanceof TvSpeakerBlockEntity speaker) {
			BlockPos s = speaker.screen();
			player.sendOverlayMessage(s == null ? Component.translatable("message.jugcraft.tv_speaker.status_none")
					: Component.translatable("message.jugcraft.tv_speaker.status_linked", s.getX(), s.getY(), s.getZ()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
	}

	/** A creative player breaking the upper half takes the lower half away first, without drops (as doors do). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
			BlockPos lower = pos.below();
			if (level.getBlockState(lower).is(this)) {
				level.setBlock(lower, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
			}
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Removing either half removes the other; the lower half drops the speaker (its loot table). */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		BlockPos other = lower ? pos.above() : pos.below();
		BlockState otherState = level.getBlockState(other);
		if (otherState.is(this) && otherState.getValue(HALF) != state.getValue(HALF)) {
			level.destroyBlock(other, !lower);
		}
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new TvSpeakerBlockEntity(pos, state) : null;
	}
}
