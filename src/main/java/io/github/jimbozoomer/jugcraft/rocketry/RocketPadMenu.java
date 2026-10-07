package io.github.jimbozoomer.jugcraft.rocketry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The rocket pad's screen: a 3x3 cargo grid at the left, the rocket and flight plan slots at the right, a launch button. */
public class RocketPadMenu extends AbstractContainerMenu {
	public static final int CARGO_X = 30;
	public static final int CARGO_Y = 17;
	public static final int ROCKET_X = 116;
	public static final int ROCKET_Y = 17;
	public static final int PLAN_X = 140;
	public static final int PLAN_Y = 17;
	public static final int LAUNCH = 0;

	private final Container container;
	private final ContainerData data;

	/** Client-side constructor: contents arrive through slot and data syncing. */
	public RocketPadMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(RocketPadBlockEntity.SLOTS),
				new SimpleContainerData(RocketPadBlockEntity.DATA_COUNT));
	}

	public RocketPadMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(JugcraftRocketry.ROCKET_PAD_MENU, containerId);
		checkContainerSize(container, RocketPadBlockEntity.SLOTS);
		checkContainerDataCount(data, RocketPadBlockEntity.DATA_COUNT);
		this.container = container;
		this.data = data;
		for (int slot = 0; slot < RocketPadBlockEntity.CARGO; slot++) {
			addSlot(new Slot(container, slot, CARGO_X + (slot % 3) * 18, CARGO_Y + (slot / 3) * 18));
		}
		addSlot(filtered(RocketPadBlockEntity.ROCKET, ROCKET_X, ROCKET_Y));
		addSlot(filtered(RocketPadBlockEntity.PLAN, PLAN_X, PLAN_Y));
		addStandardInventorySlots(inventory, 8, 84);
		addDataSlots(data);
	}

	private Slot filtered(int index, int x, int y) {
		return new Slot(container, index, x, y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return container.canPlaceItem(index, stack);
			}

			@Override
			public int getMaxStackSize() {
				return index == RocketPadBlockEntity.PLAN ? 1 : super.getMaxStackSize();
			}
		};
	}

	public RocketPadBlockEntity.Result result() {
		RocketPadBlockEntity.Result[] results = RocketPadBlockEntity.Result.values();
		int index = data.get(RocketPadBlockEntity.DATA_RESULT);
		return index >= 0 && index < results.length ? results[index] : RocketPadBlockEntity.Result.NONE;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id != LAUNCH || !(player instanceof ServerPlayer server) || !(container instanceof RocketPadBlockEntity pad)
				|| !stillValid(player) || !player.mayBuild()) {
			return false;
		}
		pad.launch(server.level());
		broadcastChanges();
		return true;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		int padSlots = RocketPadBlockEntity.SLOTS;
		Slot slot = slots.get(slotIndex);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (slotIndex < padSlots) {
			if (!moveItemStackTo(stack, padSlots, padSlots + 36, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(JugcraftRocketry.DELIVERY_ROCKET) && moveItemStackTo(stack, RocketPadBlockEntity.ROCKET, RocketPadBlockEntity.ROCKET + 1, false)) {
			// into the rocket slot
		} else if (stack.is(JugcraftRocketry.FLIGHT_PLAN) && !slots.get(RocketPadBlockEntity.PLAN).hasItem()
				&& stack.getCount() == 1 && moveItemStackTo(stack, RocketPadBlockEntity.PLAN, RocketPadBlockEntity.PLAN + 1, false)) {
			// into the plan slot
		} else if (!moveItemStackTo(stack, 0, RocketPadBlockEntity.CARGO, false)) {
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
