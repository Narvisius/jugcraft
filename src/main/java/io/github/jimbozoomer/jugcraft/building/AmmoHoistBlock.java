package io.github.jimbozoomer.jugcraft.building;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Ammo Hoist (batch 55). Stack hoist blocks into a shaft. Each holds up to {@value Fortifications#HOIST_BUFFER} items
 * and, every {@value Fortifications#HOIST_INTERVAL} ticks, lifts up to {@value Fortifications#HOIST_BATCH} of them into the
 * hoist above it. The top hoist (shown with a pulley head) hands its items to the container on top of it or, failing
 * that, to the containers beside it, such as ready racks on a gun deck. Anything can be lifted, not only shells.
 *
 * <p>Pipes, hoppers and conveyors can load any hoist but never take from one, so a hopper under the shaft cannot rob it;
 * a player loads one by using it with an item in hand. Breaking a hoist drops what it holds.
 */
public class AmmoHoistBlock extends Block implements EntityBlock {
	/** Whether this is the top of its shaft (no hoist above it). */
	public static final BooleanProperty TOP = BooleanProperty.create("top");

	public AmmoHoistBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(TOP, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(TOP);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(TOP, !(context.getLevel().getBlockState(context.getClickedPos().above()).getBlock()
				instanceof AmmoHoistBlock));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
			Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (direction == Direction.UP) {
			return state.setValue(TOP, !(neighborState.getBlock() instanceof AmmoHoistBlock));
		}
		return state;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (!oldState.is(this)) {
			level.scheduleTick(pos, this, Fortifications.HOIST_INTERVAL);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof Entity hoist && !hoist.buffer.isResourceBlank()) {
			hoist.lift(level, pos);
		}
		level.scheduleTick(pos, this, Fortifications.HOIST_INTERVAL);
	}

	/** Use with an item in hand: load as much of the held stack as fits. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof Entity hoist)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = hoist.buffer.insert(ItemVariant.of(stack), stack.getCount(), transaction);
			if (inserted == 0) {
				return InteractionResult.PASS;
			}
			transaction.commit();
			if (!player.getAbilities().instabuild) {
				stack.shrink((int) inserted);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	/** A hoist block's load: up to {@value Fortifications#HOIST_BUFFER} items of one kind. */
	public static class Entity extends BlockEntity {
		public final SingleItemStorage buffer = new SingleItemStorage() {
			@Override
			protected long getCapacity(ItemVariant variant) {
				return Math.min(Fortifications.HOIST_BUFFER, variant.toStack().getMaxStackSize());
			}

			@Override
			protected void onFinalCommit() {
				setChanged();
			}
		};

		public Entity(BlockPos pos, BlockState state) {
			super(Fortifications.HOIST_ENTITY, pos, state);
		}

		/** Lifts a batch: into the hoist above, or out of the top hoist into the container above or beside it. */
		void lift(ServerLevel level, BlockPos pos) {
			BlockPos above = pos.above();
			if (level.getBlockEntity(above) instanceof Entity next) {
				StorageUtil.move(buffer, next.buffer, v -> true, Fortifications.HOIST_BATCH, null);
				return;
			}
			long left = Fortifications.HOIST_BATCH;
			left -= deliver(level, above, Direction.DOWN, left);
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (left <= 0) {
					break;
				}
				BlockPos beside = pos.relative(side);
				if (!(level.getBlockState(beside).getBlock() instanceof AmmoHoistBlock)) {
					left -= deliver(level, beside, side.getOpposite(), left);
				}
			}
		}

		private long deliver(ServerLevel level, BlockPos target, Direction face, long max) {
			Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, target, face);
			return storage == null ? 0 : StorageUtil.move(buffer, storage, v -> true, max, null);
		}

		/** Breaking a hoist drops its load. */
		@Override
		public void preRemoveSideEffects(BlockPos pos, BlockState state) {
			if (level == null || buffer.isResourceBlank()) {
				return;
			}
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), buffer.variant.toStack((int) buffer.amount));
			buffer.amount = 0;
			buffer.variant = ItemVariant.blank();
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			buffer.readValue(input);
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			buffer.writeValue(output);
		}
	}
}
