package io.github.jimbozoomer.jugcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Ready Rack (batch 55): {@value Fortifications#RACK_SLOTS} slots of shells (the item tag jugcraft:artillery_shells)
 * standing beside a gun. A gunner with no shell of the kind their gun fires draws one from any ready rack within
 * {@value Fortifications#RACK_REACH} blocks of the gun ({@link #take}). Its model shows how full it is, a shelf at a time.
 *
 * <p>Use it with shells in hand to stock it, empty-handed to take a stack back. Pipes, hoppers, conveyors and the ammo
 * hoist can stock it and take from it. Breaking it drops what it holds.
 */
public class ReadyRackBlock extends HorizontalDirectionalBlock implements EntityBlock {
	/** How full the rack looks: 0 (empty) to 3 (every shelf stocked). */
	public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);

	public ReadyRackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FILL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FILL);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Use with shells: stock the rack with as many as fit. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(Fortifications.SHELLS) || !(level.getBlockEntity(pos) instanceof Entity rack)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack left = rack.shells.addItem(player.getAbilities().instabuild ? stack.copy() : stack);
		if (!player.getAbilities().instabuild) {
			player.setItemInHand(hand, left);
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.ready_rack.contents", rack.count()));
		return InteractionResult.SUCCESS;
	}

	/** Use empty-handed: take the last stocked stack back. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof Entity rack)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		for (int slot = rack.shells.getContainerSize() - 1; slot >= 0; slot--) {
			ItemStack stack = rack.shells.getItem(slot);
			if (!stack.isEmpty()) {
				rack.shells.setItem(slot, ItemStack.EMPTY);
				if (!player.getInventory().add(stack)) {
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
				}
				break;
			}
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.ready_rack.contents", rack.count()));
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	/**
	 * Takes one {@code ammo} from any ready rack in {@code area} (a gun's reach), for a gunner who has none. Returns
	 * whether it found one.
	 */
	public static boolean take(ServerLevel level, AABB area, Item ammo) {
		return find(level, area, ammo, true);
	}

	/** Whether any ready rack in {@code area} holds an {@code ammo}, without taking it. */
	public static boolean has(ServerLevel level, AABB area, Item ammo) {
		return find(level, area, ammo, false);
	}

	private static boolean find(ServerLevel level, AABB area, Item ammo, boolean take) {
		for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
				BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
			if (!(level.getBlockEntity(pos) instanceof Entity rack)) {
				continue;
			}
			for (int slot = 0; slot < rack.shells.getContainerSize(); slot++) {
				ItemStack stack = rack.shells.getItem(slot);
				if (stack.is(ammo)) {
					if (take) {
						rack.shells.removeItem(slot, 1);
					}
					return true;
				}
			}
		}
		return false;
	}

	/** The rack's shells, and the fill its model shows. */
	public static class Entity extends BlockEntity {
		public final SimpleContainer shells = new SimpleContainer(Fortifications.RACK_SLOTS) {
			@Override
			public boolean canPlaceItem(int slot, ItemStack stack) {
				return stack.is(Fortifications.SHELLS);
			}

			@Override
			public void setChanged() {
				super.setChanged();
				Entity.this.contentsChanged();
			}
		};

		/** Set while loading from disk, so filling the slots does not touch the world mid-load. */
		private boolean loading;

		public Entity(BlockPos pos, BlockState state) {
			super(Fortifications.RACK_ENTITY, pos, state);
		}

		/** How many shells it holds. */
		public int count() {
			int count = 0;
			for (int slot = 0; slot < shells.getContainerSize(); slot++) {
				count += shells.getItem(slot).getCount();
			}
			return count;
		}

		/** Saves the change and shows it: a shelf of shells for each third of the slots in use, rounding up. */
		private void contentsChanged() {
			setChanged();
			if (loading || level == null || level.isClientSide()) {
				return;
			}
			int used = 0;
			for (int slot = 0; slot < shells.getContainerSize(); slot++) {
				if (!shells.getItem(slot).isEmpty()) {
					used++;
				}
			}
			int fill = (used * 3 + shells.getContainerSize() - 1) / shells.getContainerSize();
			BlockState state = getBlockState();
			if (state.hasProperty(FILL) && state.getValue(FILL) != fill) {
				level.setBlock(worldPosition, state.setValue(FILL, fill), Block.UPDATE_CLIENTS);
			}
		}

		@Override
		public void preRemoveSideEffects(BlockPos pos, BlockState state) {
			if (level != null) {
				Containers.dropContents(level, pos, shells);
			}
		}

		@Override
		protected void loadAdditional(ValueInput input) {
			super.loadAdditional(input);
			NonNullList<ItemStack> items = NonNullList.withSize(shells.getContainerSize(), ItemStack.EMPTY);
			ContainerHelper.loadAllItems(input, items);
			loading = true;
			for (int slot = 0; slot < items.size(); slot++) {
				shells.setItem(slot, items.get(slot));
			}
			loading = false;
		}

		@Override
		protected void saveAdditional(ValueOutput output) {
			super.saveAdditional(output);
			NonNullList<ItemStack> items = NonNullList.withSize(shells.getContainerSize(), ItemStack.EMPTY);
			for (int slot = 0; slot < items.size(); slot++) {
				items.set(slot, shells.getItem(slot));
			}
			ContainerHelper.saveAllItems(output, items);
		}
	}
}
