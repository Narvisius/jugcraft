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
import net.minecraft.world.item.Items;

/**
 * Shared menu for all machines. Coal generator: fuel (56,35). Processors: input (56,35), output (116,35).
 * Steam generator: fuel (56,53), water bucket (56,17), empty buckets (116,35).
 * Multi-input processors: inputs laid out by {@link #inputX}, output (116,35).
 */
public class MachineMenu extends AbstractContainerMenu {
	public static final int INPUT_X = 56;
	public static final int OUTPUT_X = 116;
	public static final int SLOT_Y = 35;
	/** Byproduct slots sit in a row above the output slot. */
	public static final int BYPRODUCT_Y = 13;

	public static int byproductX(int index) {
		return 98 + index * 18;
	}

	/** Upgrade slots sit in a row below the output slot. */
	public static final int UPGRADE_Y = 57;

	public static int upgradeX(int index) {
		return 98 + index * 18;
	}
	/** X positions of processor input slots, by number of inputs; they end just left of the progress arrow. */
	private static final int[][] INPUT_LAYOUT = {{}, {56}, {38, 56}, {26, 44, 62}};

	public static int inputX(int inputs, int slot) {
		return INPUT_LAYOUT[inputs][slot];
	}

	private final MachineKind kind;
	private final Container container;
	private final ContainerData data;

	/** Client-side constructor: contents arrive through slot and data syncing. */
	public MachineMenu(MenuType<?> type, MachineKind kind, int containerId, Inventory inventory) {
		this(type, kind, containerId, inventory, new SimpleContainer(kind.containerSize()), new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
	}

	public MachineMenu(MenuType<?> type, MachineKind kind, int containerId, Inventory inventory, Container container, ContainerData data) {
		super(type, containerId);
		checkContainerSize(container, kind.containerSize());
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
		} else if (kind == MachineKind.STEAM_GENERATOR) {
			addSlot(new Slot(container, MachineBlockEntity.SLOT_FUEL, INPUT_X, 53) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return GeneratorFuels.steamBurnTicks(stack) > 0;
				}
			});
			addSlot(new Slot(container, MachineBlockEntity.SLOT_WATER_IN, INPUT_X, 17) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.is(Items.WATER_BUCKET);
				}
			});
			addSlot(new Slot(container, MachineBlockEntity.SLOT_BUCKET_OUT, OUTPUT_X, SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		} else if (kind.isProcessor()) {
			int inputs = kind.outputSlot();
			for (int slot = 0; slot < inputs; slot++) {
				addSlot(new Slot(container, slot, inputX(inputs, slot), SLOT_Y) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return container.canPlaceItem(getContainerSlot(), stack);
					}
				});
			}
			addSlot(new Slot(container, inputs, OUTPUT_X, SLOT_Y) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
			for (int index = 0; index < kind.byproductSlots(); index++) {
				addSlot(new Slot(container, inputs + 1 + index, byproductX(index), BYPRODUCT_Y) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return false;
					}
				});
			}
			for (int index = 0; index < kind.upgradeSlots(); index++) {
				addSlot(new Slot(container, kind.slots + index, upgradeX(index), UPGRADE_Y) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return MachineUpgrades.isUpgrade(stack);
					}
				});
			}
		}
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	/** Side-configuration buttons (ids 0..5 cycle a face, 6 toggles ejecting); only the server applies them. */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		return container instanceof MachineBlockEntity machine && machine.clickSideButton(id);
	}

	public MachineKind kind() {
		return kind;
	}

	public int data(int index) {
		return data.get(index);
	}

	/** Shift-click into the machine: upgrades only into upgrade slots, everything else only into the others. */
	private boolean moveIntoMachine(ItemStack stack, int machineSlots) {
		boolean upgrade = MachineUpgrades.isUpgrade(stack) && kind.upgradeSlots() > 0;
		int from = upgrade ? kind.slots : 0;
		int to = upgrade ? machineSlots : kind.slots;
		for (int slot = from; slot < to; slot++) {
			if (container.canPlaceItem(slot, stack) && moveItemStackTo(stack, slot, slot + 1, false)) {
				return true;
			}
		}
		return false;
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
		int machineSlots = kind.containerSize();
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
		} else if (!moveIntoMachine(stack, machineSlots)) {
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
