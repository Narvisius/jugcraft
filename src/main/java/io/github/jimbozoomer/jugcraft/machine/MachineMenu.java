package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared menu for all machines. Slot layout: fuel at (56,35); input (56,35) and output (116,35) for processors. */
public class MachineMenu extends AbstractContainerMenu {
	public static final int INPUT_X = 56;
	public static final int OUTPUT_X = 116;
	public static final int SLOT_Y = 35;

	private final MachineKind kind;
	private final Container container;
	private final ContainerData data;

	/** Client-side constructor: contents arrive through slot and data syncing. */
	public MachineMenu(MenuType<?> type, MachineKind kind, int containerId, Inventory inventory) {
		this(type, kind, containerId, inventory, new SimpleContainer(kind.slots), new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
	}

	public MachineMenu(MenuType<?> type, MachineKind kind, int containerId, Inventory inventory, Container container, ContainerData data) {
		super(type, containerId);
		checkContainerSize(container, kind.slots);
		checkContainerDataCount(data, MachineBlockEntity.DATA_COUNT);
		this.kind = kind;
		this.container = container;
		this.data = data;

		if (kind == MachineKind.COAL_GENERATOR) {
			addSlot(new Slot(container, 0, INPUT_X, SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return GeneratorFuels.burnTicks(stack) > 0;
				}
			});
		} else if (kind.isProcessor()) {
			addSlot(new Slot(container, 0, INPUT_X, SLOT_Y));
			addSlot(new Slot(container, 1, OUTPUT_X, SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	public MachineKind kind() {
		return kind;
	}

	public int data(int index) {
		return data.get(index);
	}

	/** Energy stored, reassembled from the two synced 16-bit halves. */
	public long energy() {
		return combine(MachineBlockEntity.DATA_ENERGY_LOW, MachineBlockEntity.DATA_ENERGY_HIGH);
	}

	public long capacity() {
		return combine(MachineBlockEntity.DATA_CAPACITY_LOW, MachineBlockEntity.DATA_CAPACITY_HIGH);
	}

	private long combine(int low, int high) {
		return ((long) (data.get(high) & 0xFFFF) << 16) | (data.get(low) & 0xFFFF);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int machineSlots = kind.slots;
		int playerEnd = machineSlots + 36;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < machineSlots) {
			if (!moveItemStackTo(stack, machineSlots, playerEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (machineSlots == 0 || !container.canPlaceItem(0, stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
