package io.github.jimbozoomer.jugcraft.finance;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Market Chest's menu: its 27 slots in three rows (like a chest) over the player's inventory. The screen adds the
 * pricing panel to the right; prices and the rest go through the Exchange's own messages, not the menu.
 */
public class MarketChestMenu extends AbstractContainerMenu {
	public static final int SLOTS_X = 8;
	public static final int SLOTS_Y = 18;
	public static final int INVENTORY_Y = 84;

	private final Container container;
	private final BlockPos pos;

	/** The client's copy, opened with the chest's position. */
	public MarketChestMenu(int containerId, Inventory inventory, BlockPos pos) {
		this(containerId, inventory, new SimpleContainer(MarketChestBlockEntity.SIZE), pos);
	}

	public MarketChestMenu(int containerId, Inventory inventory, Container container, BlockPos pos) {
		super(ExchangeBlocks.MARKET_CHEST_MENU, containerId);
		checkContainerSize(container, MarketChestBlockEntity.SIZE);
		this.container = container;
		this.pos = pos;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(container, row * 9 + col, SLOTS_X + col * 18, SLOTS_Y + row * 18));
			}
		}
		addStandardInventorySlots(inventory, 8, INVENTORY_Y);
	}

	public BlockPos pos() {
		return pos;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int chestSlots = MarketChestBlockEntity.SIZE;
		int end = chestSlots + 36;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < chestSlots) {
			if (!moveItemStackTo(stack, chestSlots, end, true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, chestSlots, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}
}
