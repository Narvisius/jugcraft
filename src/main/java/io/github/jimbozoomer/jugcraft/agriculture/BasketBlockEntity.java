package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A basket's {@value #SLOTS} stacks (the kitchen and cooking expansion's slice 5; tools/soil.py BASKETS), shown in a 3 by
 * 3 screen. Every {@value #PICKUP_TICKS} ticks it takes in the item that has come to rest inside it, a stack at most;
 * hoppers and pipes reach it as any container. Breaking it spills what is inside.
 */
public class BasketBlockEntity extends BaseContainerBlockEntity {
	public static final int SLOTS = 9;
	/** Ticks between pickups (tools/soil.py PICKUP_TICKS). */
	public static final int PICKUP_TICKS = 4;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

	public BasketBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.BASKET_ENTITY, pos, state);
	}

	/** Takes in one item entity lying inside the basket, as much of it as fits. */
	public void serverTick(ServerLevel level) {
		if ((level.getGameTime() + worldPosition.asLong()) % PICKUP_TICKS != 0) {
			return;
		}
		AABB inside = new AABB(worldPosition.getX() + 1 / 16.0, worldPosition.getY() + 1 / 16.0, worldPosition.getZ() + 1 / 16.0,
				worldPosition.getX() + 15 / 16.0, worldPosition.getY() + 1.0, worldPosition.getZ() + 15 / 16.0);
		for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, inside, ItemEntity::isAlive)) {
			ItemStack stack = entity.getItem();
			int before = stack.getCount();
			ItemStack left = insert(stack.copy());
			if (left.getCount() != before) {
				if (left.isEmpty()) {
					entity.discard();
				} else {
					entity.setItem(left);
				}
				setChanged();
				return;
			}
		}
	}

	/** Puts {@code stack} into the basket: onto matching stacks first, then into empty slots. Returns what does not fit. */
	public ItemStack insert(ItemStack stack) {
		for (int slot = 0; slot < SLOTS && !stack.isEmpty(); slot++) {
			ItemStack held = items.get(slot);
			if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) {
				int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
				if (moved > 0) {
					held.grow(moved);
					stack.shrink(moved);
				}
			}
		}
		for (int slot = 0; slot < SLOTS && !stack.isEmpty(); slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, stack.copy());
				stack.setCount(0);
			}
		}
		return stack;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.basket");
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
		return SLOTS;
	}

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new DispenserMenu(containerId, inventory, this);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
