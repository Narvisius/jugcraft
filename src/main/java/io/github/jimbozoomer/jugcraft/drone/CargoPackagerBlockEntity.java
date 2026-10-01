package io.github.jimbozoomer.jugcraft.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cargo packager's 27-slot store. Pipes, extractors and hoppers fill it from any side (it is a
 * plain item storage for the shared item API); drones take their loads from it and return anything
 * they could not deliver.
 */
public class CargoPackagerBlockEntity extends BaseContainerBlockEntity {
	public static final int SIZE = 27;
	private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
	/** Test supply (development command only): hands out any building block without using items. */
	private boolean unlimited;

	public CargoPackagerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftDrones.PACKAGER_ENTITY, pos, state);
	}

	/** Takes one matching item, or returns empty. */
	public ItemStack takeOne(java.util.function.Predicate<ItemStack> matches) {
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (!stack.isEmpty() && matches.test(stack)) {
				ItemStack one = stack.split(1);
				setChanged();
				return one;
			}
		}
		return ItemStack.EMPTY;
	}

	/** Counts matching items (for the missing-materials check). */
	public int count(java.util.function.Predicate<ItemStack> matches) {
		int count = 0;
		for (ItemStack stack : items) {
			if (!stack.isEmpty() && matches.test(stack)) {
				count += stack.getCount();
			}
		}
		return count;
	}

	public boolean unlimited() {
		return unlimited;
	}

	/** Turns the endless test supply on or off (the development-only {@code /dronetest supplies} command). */
	public void setUnlimited(boolean unlimited) {
		this.unlimited = unlimited;
		setChanged();
	}

	/** Puts items back; returns what did not fit. */
	public ItemStack giveBack(ItemStack stack) {
		if (unlimited) {
			return ItemStack.EMPTY;
		}
		ItemStack rest = stack.copy();
		for (int i = 0; i < items.size() && !rest.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (slot.isEmpty()) {
				items.set(i, rest);
				rest = ItemStack.EMPTY;
			} else if (ItemStack.isSameItemSameComponents(slot, rest) && slot.getCount() < slot.getMaxStackSize()) {
				int moved = Math.min(rest.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(moved);
				rest.shrink(moved);
			}
		}
		setChanged();
		return rest;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.cargo_packager");
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
		return SIZE;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return ChestMenu.threeRows(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		unlimited = input.getBooleanOr("unlimited", false);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		if (unlimited) {
			output.putBoolean("unlimited", true);
		}
	}
}
