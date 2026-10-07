package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.control.Channels;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A field telephone (batch 50). A redstone signal into its back makes every other field telephone on the same channel
 * (a dye colour, set by using a dye on it) within {@value Trenchworks#PHONE_RANGE} blocks in the same dimension ring:
 * it gives a full signal from its front and its bell sounds. Telephones keep track of each other while their chunks are
 * loaded, checking every {@value #INTERVAL} ticks; a telephone in an unloaded chunk neither calls nor rings.
 */
public class FieldTelephoneBlock extends HorizontalDirectionalBlock {
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty RINGING = BooleanProperty.create("ringing");
	public static final int INTERVAL = 10;
	private static final VoxelShape SHAPE = Block.box(3, 0, 4, 13, 11, 13);
	/** Server side: the telephones each dimension knows of. Entries that are no longer telephones are dropped. */
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PHONES = new HashMap<>();

	public FieldTelephoneBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Channels.CHANNEL, DyeColor.WHITE)
				.setValue(POWERED, false).setValue(RINGING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, Channels.CHANNEL, POWERED, RINGING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (level instanceof ServerLevel server && !oldState.is(this)) {
			PHONES.computeIfAbsent(server.dimension(), k -> new HashSet<>()).add(pos.immutable());
			server.scheduleTick(pos, this, 1);
		}
	}

	/** Whether a signal comes into the telephone's back. */
	private static boolean calling(Level level, BlockPos pos, BlockState state) {
		Direction back = state.getValue(FACING).getOpposite();
		return level.getSignal(pos.relative(back), back) > 0;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation,
			boolean movedByPiston) {
		boolean calling = calling(level, pos, state);
		if (calling != state.getValue(POWERED)) {
			level.setBlock(pos, state.setValue(POWERED, calling), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		PHONES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
		boolean calling = calling(level, pos, state);
		boolean ringing = ringing(level, pos, state.getValue(Channels.CHANNEL));
		if (calling != state.getValue(POWERED) || ringing != state.getValue(RINGING)) {
			level.setBlock(pos, state.setValue(POWERED, calling).setValue(RINGING, ringing), Block.UPDATE_ALL);
		}
		if (ringing && (!state.getValue(RINGING) || level.getGameTime() % 40 == 0)) {
			level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 0.6F, 1.8F);
		}
		level.scheduleTick(pos, this, INTERVAL);
	}

	/** Whether another loaded telephone on this channel and in range is being called. */
	private static boolean ringing(ServerLevel level, BlockPos pos, DyeColor channel) {
		Set<BlockPos> phones = PHONES.get(level.dimension());
		if (phones == null) {
			return false;
		}
		long range = (long) Trenchworks.PHONE_RANGE * Trenchworks.PHONE_RANGE;
		for (Iterator<BlockPos> it = phones.iterator(); it.hasNext();) {
			BlockPos other = it.next();
			if (other.equals(pos) || !level.isLoaded(other)) {
				continue;
			}
			BlockState state = level.getBlockState(other);
			if (!(state.getBlock() instanceof FieldTelephoneBlock)) {
				it.remove();
				continue;
			}
			if (state.getValue(Channels.CHANNEL) == channel && state.getValue(POWERED) && other.distSqr(pos) <= range) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	/** A ringing telephone powers the block in front of it. */
	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		return state.getValue(RINGING) && direction == state.getValue(FACING).getOpposite() ? 15 : 0;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		return Channels.dye(stack, state, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.telephone.channel",
					Channels.name(state.getValue(Channels.CHANNEL))));
		}
		return InteractionResult.SUCCESS;
	}
}
