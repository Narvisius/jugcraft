package io.github.jimbozoomer.jugcraft.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Advanced Crafting Table's screen (client/AdvancedCraftingScreen): the 6 x 6 grid (the table's own slots, so what
 * is on it stays when the screen closes), the result and the player's inventory. The result is the table's (it finds it
 * whenever the grid changes, for every screen open on it); taking it makes it once, shift-clicking it as many times as
 * the grid and the inventory allow.
 */
public class AdvancedCraftingMenu extends AbstractContainerMenu {
	/** Where the slots' items are drawn (the screen's texture, tools/adv_crafting_table.py gui(): its frames 1 out). */
	public static final int GRID_X = 11, GRID_Y = 25, RESULT_X = 173, RESULT_Y = 66, INVENTORY_X = 38, INVENTORY_Y = 153;
	public static final int RESULT = AdvancedCraftingTableBlockEntity.SLOTS;
	private static final int INVENTORY_START = RESULT + 1, INVENTORY_END = INVENTORY_START + 36;

	private final Container grid;
	private final SimpleContainer resultSlot = new SimpleContainer(1);
	private final @Nullable AdvancedCraftingTableBlockEntity table;
	private final BlockPos pos;

	/** The client's side (its slots filled by the server). */
	public AdvancedCraftingMenu(int id, Inventory inventory, BlockPos pos) {
		this(id, inventory, new SimpleContainer(AdvancedCraftingTableBlockEntity.SLOTS), null, pos);
	}

	public AdvancedCraftingMenu(int id, Inventory inventory, AdvancedCraftingTableBlockEntity table) {
		this(id, inventory, table, table, table.getBlockPos());
	}

	private AdvancedCraftingMenu(int id, Inventory inventory, Container grid, @Nullable AdvancedCraftingTableBlockEntity table, BlockPos pos) {
		super(JugcraftCrafting.MENU, id);
		checkContainerSize(grid, AdvancedCraftingTableBlockEntity.SLOTS);
		this.grid = grid;
		this.table = table;
		this.pos = pos;
		int side = AdvancedCraftingInput.SIDE;
		for (int slot = 0; slot < AdvancedCraftingTableBlockEntity.SLOTS; slot++) {
			addSlot(new Slot(grid, slot, GRID_X + (slot % side) * 18, GRID_Y + (slot / side) * 18));
		}
		addSlot(new ResultSlot());
		addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
		if (table != null) {
			table.listen(this);
			refreshResult();
		}
	}

	/** The table this screen is open on. */
	public BlockPos pos() {
		return pos;
	}

	/** The result slot shows the table's result (server; the clients get it with the screen's other slots). */
	void refreshResult() {
		if (table != null) {
			resultSlot.setItem(0, table.result().copy());
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return table == null || !table.isRemoved() && table.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (table != null) {
			table.unlisten(this);
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index == RESULT) {
			if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
				return ItemStack.EMPTY;
			}
			slot.onQuickCraft(stack, original);
		} else if (index < RESULT) {
			if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, 0, RESULT, false)) {
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
		if (index == RESULT && !stack.isEmpty() && !player.level().isClientSide()) {
			player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);   // no room: dropped
		}
		return original;
	}

	/** The result: nothing goes in; taking it makes it (the table takes one of every item on the grid). */
	private final class ResultSlot extends Slot {
		ResultSlot() {
			super(resultSlot, 0, RESULT_X, RESULT_Y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		protected void onQuickCraft(ItemStack stack, int amount) {
		}

		@Override
		public void onTake(Player player, ItemStack stack) {
			if (table != null) {
				stack.onCraftedBy(player, stack.getCount());
				table.craft(player);
				refreshResult();
			}
			super.onTake(player, stack);
		}
	}
}
