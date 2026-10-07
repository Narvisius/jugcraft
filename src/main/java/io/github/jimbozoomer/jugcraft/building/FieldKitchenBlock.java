package io.github.jimbozoomer.jugcraft.building;

import io.github.jimbozoomer.jugcraft.agriculture.CookingPotBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.HearthOvenBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Field Kitchen (batch 59): a riveted iron stove with a firebox door at the front and its stove pipe up the back,
 * and a heat source for a Cooking Pot standing on it (block tag jugcraft:heat_sources; the pot counts it only while it is
 * {@link #LIT}).
 *
 * <p>It holds one stack of fuel: logs, coal, charcoal, coal blocks or coke, as the Hearth Oven burns them
 * ({@link HearthOvenBlockEntity#burnTicks}); nothing that leaves a container behind. Fuel burns down tick by tick while
 * it has burn time left, and the next piece is only put on the fire while a Cooking Pot stands on top, so an empty stove
 * never wastes fuel. Use it with fuel to load it, empty-handed to see its fuel and fire; pipes, hoppers and conveyors can
 * load it but never take from it. Breaking it drops its fuel.
 */
public class FieldKitchenBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final double[][] BOXES = {{0.5, 0, 0.5, 15.5, 16, 14}, {10.5, 4, 13.5, 13.5, 16, 16}};

	public FieldKitchenBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock.shape(BOXES, state.getValue(FACING));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!(level instanceof ServerLevel)) {
			return null;
		}
		return createTickerHelper(type, Bunkerworks.KITCHEN_ENTITY,
				(tickLevel, pos, tickState, kitchen) -> kitchen.serverTick((ServerLevel) tickLevel, pos, tickState));
	}

	/** Use with fuel: load as much of it as fits. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!Entity.isFuel(stack) || !(level.getBlockEntity(pos) instanceof Entity kitchen)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = kitchen.fuel.insert(ItemVariant.of(stack), stack.getCount(), transaction);
			if (inserted > 0) {
				transaction.commit();
				if (!player.getAbilities().instabuild) {
					stack.shrink((int) inserted);
				}
				level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.2F);
			}
		}
		kitchen.status(player);
		return InteractionResult.SUCCESS;
	}

	/** Empty-handed: how much fuel it holds and how long its fire has left. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Entity kitchen) {
			kitchen.status(player);
		}
		return InteractionResult.SUCCESS;
	}

	/** Smoke from the top of the stove pipe and now and then a flame at the firebox door while it burns (client only). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		Direction facing = state.getValue(FACING);
		Direction right = facing.getClockWise();
		// The pipe's top, 4 pixels right of the middle (seen from the front) and 6.5 behind it.
		double px = pos.getX() + 0.5 + right.getStepX() * 0.25 - facing.getStepX() * 0.4;
		double pz = pos.getZ() + 0.5 + right.getStepZ() * 0.25 - facing.getStepZ() * 0.4;
		level.addParticle(ParticleTypes.SMOKE, px, pos.getY() + 1.02, pz, 0.0, 0.04, 0.0);
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, px, pos.getY() + 1.05, pz, 0.0, 0.05, 0.0);
		}
		if (random.nextInt(6) == 0) {
			double dx = pos.getX() + 0.5 + facing.getStepX() * 0.52 + right.getStepX() * (random.nextDouble() - 0.5) * 0.3;
			double dz = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + right.getStepZ() * (random.nextDouble() - 0.5) * 0.3;
			level.addParticle(ParticleTypes.FLAME, dx, pos.getY() + 0.3, dz, 0.0, 0.0, 0.0);
		}
		if (random.nextInt(20) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
					0.6F, 1.0F, false);
		}
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
		builder.add(FACING, LIT);
	}

	/** The stove's fuel and fire. */
	public static class Entity extends BlockEntity {
		/** One stack of fuel. */
		public final SingleItemStorage fuel = new SingleItemStorage() {
			@Override
			protected long getCapacity(ItemVariant variant) {
				return variant.toStack().getMaxStackSize();
			}

			@Override
			protected boolean canInsert(ItemVariant variant) {
				return isFuel(variant.toStack());
			}

			@Override
			protected void onFinalCommit() {
				setChanged();
			}
		};
		/** Ticks of fire left, and how long the piece burning now lasts in all. */
		private int burn;
		private int burnTotal;

		public Entity(BlockPos pos, BlockState state) {
			super(Bunkerworks.KITCHEN_ENTITY, pos, state);
		}

		/** Whether the kitchen burns {@code stack} (the Hearth Oven's fuels, none of which leaves a container behind). */
		public static boolean isFuel(ItemStack stack) {
			return !stack.isEmpty() && HearthOvenBlockEntity.burnTicks(stack) > 0;
		}

		/** Ticks of fire left. */
		public int burn() {
			return burn;
		}

		/** How many fuel items it holds. */
		public int fuelCount() {
			return (int) fuel.amount;
		}

		/** Whether a Cooking Pot stands on top. */
		public static boolean potOnTop(Level level, BlockPos pos) {
			return level.getBlockEntity(pos.above()) instanceof CookingPotBlockEntity;
		}

		void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
			boolean changed = false;
			if (burn > 0) {
				burn--;
				changed = true;
			}
			if (burn <= 0 && !fuel.isResourceBlank() && fuel.amount > 0 && potOnTop(level, pos)) {
				int ticks = HearthOvenBlockEntity.burnTicks(fuel.variant.toStack());
				if (ticks > 0) {
					burn = ticks;
					burnTotal = ticks;
					fuel.amount--;
					if (fuel.amount <= 0) {
						fuel.variant = ItemVariant.blank();
						fuel.amount = 0;
					}
					changed = true;
				}
			}
			boolean lit = burn > 0;
			if (state.getValue(LIT) != lit) {
				level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
			}
			if (changed) {
				setChanged();
			}
		}

		void status(Player player) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.field_kitchen.status", fuelCount(), (burn + 19) / 20));
		}

		/** Breaking the kitchen drops its fuel. */
		@Override
		public void preRemoveSideEffects(BlockPos pos, BlockState state) {
			if (level == null || fuel.isResourceBlank() || fuel.amount <= 0) {
				return;
			}
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), fuel.variant.toStack((int) fuel.amount));
			fuel.amount = 0;
			fuel.variant = ItemVariant.blank();
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			fuel.readValue(input);
			burn = input.getInt("burn").orElse(0);
			burnTotal = input.getInt("burn_total").orElse(0);
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			fuel.writeValue(output);
			output.putInt("burn", burn);
			output.putInt("burn_total", burnTotal);
		}
	}
}
