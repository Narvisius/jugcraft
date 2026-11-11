package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Part W5: the Signal Box's screen (client/warehouse/SignalBoxScreen.java): OUTPUT or INPUT, the rule, the item (set by
 * using the box with it, or cleared here) and the number. No slots; the screen reads the box itself (its block entity
 * is synced), its buttons come here.
 */
public class SignalBoxMenu extends AbstractContainerMenu {
	/** Buttons: OUTPUT / INPUT, RULE + k, CLEAR the item, NUMBER + n sets the number. */
	public static final int OUTPUT = 0, INPUT = 1, RULE = 10, CLEAR = 100, NUMBER = 1000;
	public final BlockPos box;

	public SignalBoxMenu(int id, Inventory inventory, BlockPos box) {
		super(JugcraftWarehouse.SIGNAL_BOX_MENU, id);
		this.box = box;
	}

	@Override
	public boolean stillValid(Player player) {
		return !player.isSpectator() && player.level().isLoaded(box) && player.level().getBlockEntity(box) instanceof SignalBoxBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(box)) <= 10 * 10;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (player.level().isClientSide() || !stillValid(player) || !(player.level().getBlockEntity(box) instanceof SignalBoxBlockEntity sb)) {
			return false;
		}
		TerminalBlockEntity t = sb.terminal();
		if (t != null && !t.mayUse(player)) {
			t.deny(player);
			return false;
		}
		sb.setOwner(player.getUUID());
		if (id == OUTPUT || id == INPUT) {
			sb.setMode(id);
		} else if (id >= RULE && id < RULE + 20) {
			sb.setRule(id - RULE);
		} else if (id == CLEAR) {
			sb.setItem(ItemStack.EMPTY);
		} else if (id >= NUMBER) {
			sb.setThreshold(id - (long) NUMBER);
		} else {
			return false;
		}
		return true;
	}
}
