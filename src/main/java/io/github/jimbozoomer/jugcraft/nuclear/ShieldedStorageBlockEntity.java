package io.github.jimbozoomer.jugcraft.nuclear;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The store in a lead pig, lead-lined locker or shielded waste drum (ShieldedStorageBlock): a chest-like store of
 * the block's size that lets only its block's share of the contents' radiation out (RadiationField.held). The
 * locker's doors stand open while anyone has it open.
 */
public class ShieldedStorageBlockEntity extends BaseContainerBlockEntity implements ShieldedContainer {
	private NonNullList<ItemStack> items;
	private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
		@Override
		protected void onOpen(Level level, BlockPos pos, BlockState state) {
			sound(state, true);
			setOpen(state, true);
		}

		@Override
		protected void onClose(Level level, BlockPos pos, BlockState state) {
			sound(state, false);
			setOpen(state, false);
		}

		@Override
		protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int now) {
		}

		@Override
		public boolean isOwnContainer(Player player) {
			return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == ShieldedStorageBlockEntity.this;
		}
	};

	public ShieldedStorageBlockEntity(BlockPos pos, BlockState state) {
		super(StorageBlocks.STORE_ENTITY, pos, state);
		items = NonNullList.withSize(kind(state).slots, ItemStack.EMPTY);
	}

	private static ShieldedStorageBlock.Kind kind(BlockState state) {
		return state.getBlock() instanceof ShieldedStorageBlock block ? block.kind : ShieldedStorageBlock.Kind.DRUM;
	}

	public ShieldedStorageBlock.Kind kind() {
		return kind(getBlockState());
	}

	@Override
	public double leakage() {
		return kind().leakage;
	}

	private void setOpen(BlockState state, boolean open) {
		if (level != null && kind() == ShieldedStorageBlock.Kind.LOCKER && state.hasProperty(ShieldedStorageBlock.OPEN)) {
			level.setBlock(worldPosition, state.setValue(ShieldedStorageBlock.OPEN, open), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
	}

	private void sound(BlockState state, boolean open) {
		if (level == null) {
			return;
		}
		SoundEvent sound = switch (kind()) {
			case LOCKER -> open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE;
			case DRUM -> open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE;
			case PIG -> open ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK;
		};
		level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.5F, kind() == ShieldedStorageBlock.Kind.LOCKER ? 1.2F : 0.9F);
	}

	@Override
	public void startOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
			openers.incrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState(), user.getContainerInteractionRange());
		}
	}

	@Override
	public void stopOpen(ContainerUser user) {
		if (!remove && !user.getLivingEntity().isSpectator() && level != null) {
			openers.decrementOpeners(user.getLivingEntity(), level, worldPosition, getBlockState());
		}
	}

	@Override
	public List<ContainerUser> getEntitiesWithContainerOpen() {
		return level == null ? List.of() : openers.getEntitiesWithContainerOpen(level, worldPosition);
	}

	public void recheckOpen() {
		if (!remove && level != null) {
			openers.recheckOpeners(level, worldPosition, getBlockState());
		}
	}

	/** The lead pig keeps what is in it when broken (its loot table copies it to the item), so nothing spills. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (kind(state) != ShieldedStorageBlock.Kind.PIG) {
			super.preRemoveSideEffects(pos, state);
		}
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft." + kind().id());
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return kind().slots;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		int rows = kind().slots / 9;
		MenuType<ChestMenu> type = rows == 1 ? MenuType.GENERIC_9x1 : rows == 2 ? MenuType.GENERIC_9x2 : MenuType.GENERIC_9x3;
		return new ChestMenu(type, containerId, inventory, this, rows);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
