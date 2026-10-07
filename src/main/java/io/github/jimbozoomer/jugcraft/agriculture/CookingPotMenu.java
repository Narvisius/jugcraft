package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The Cooking Pot's screen: six ingredient slots (3x2) at the left, four result slots (2x2) at the right. */
public class CookingPotMenu extends AbstractContainerMenu {
	public static final int INPUT_X = 30;
	public static final int INPUT_Y = 26;
	public static final int RESULT_X = 116;
	public static final int RESULT_Y = 26;

	private final Container container;
	private final ContainerData data;

	/** Client-side constructor: contents arrive through slot and data syncing. */
	public CookingPotMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(CookingPotBlockEntity.SLOTS),
				new SimpleContainerData(CookingPotBlockEntity.DATA_COUNT));
	}

	public CookingPotMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(JugcraftAgriculture.COOKING_POT_MENU, containerId);
		checkContainerSize(container, CookingPotBlockEntity.SLOTS);
		checkContainerDataCount(data, CookingPotBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		for (int slot = 0; slot < CookingPotBlockEntity.INPUTS; slot++) {
			addSlot(new Slot(container, slot, INPUT_X + (slot % 3) * 18, INPUT_Y + (slot / 3) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return container.canPlaceItem(getContainerSlot(), stack);
				}
			});
		}
		for (int index = 0; index < CookingPotBlockEntity.OUTPUTS; index++) {
			addSlot(new Slot(container, CookingPotBlockEntity.RESULT + index, RESULT_X + (index % 2) * 18, RESULT_Y + (index / 2) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	/** Cooking progress from 0 to {@code width}. */
	public int progress(int width) {
		int time = data.get(CookingPotBlockEntity.DATA_TIME);
		return time <= 0 ? 0 : data.get(CookingPotBlockEntity.DATA_PROGRESS) * width / time;
	}

	/** Whether a heat source is under the pot. */
	public boolean heated() {
		return data.get(CookingPotBlockEntity.DATA_HEAT) != 0;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int potSlots = CookingPotBlockEntity.SLOTS;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < potSlots) {
			if (!moveItemStackTo(stack, potSlots, potSlots + 36, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!container.canPlaceItem(0, stack) || !moveItemStackTo(stack, 0, CookingPotBlockEntity.INPUTS, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == original.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return original;
	}
}
