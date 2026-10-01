package io.github.jimbozoomer.jugcraft.tools;

import net.minecraft.world.item.ItemStack;

/**
 * An item that holds JE in its {@link JugcraftTools#ENERGY} component and is charged at the charging
 * station. The helpers read and write that component.
 */
public interface Chargeable {
	/** Most JE the item holds without capacity modules. */
	long baseCapacity();

	/** Most JE this stack holds: its base capacity once more for each capacity module fitted. */
	static long capacity(ItemStack stack) {
		if (!(stack.getItem() instanceof Chargeable chargeable)) {
			return 0;
		}
		return chargeable.baseCapacity() * (1 + ToolUpgrades.level(stack, ToolUpgrades.Kind.CAPACITY));
	}

	static long energy(ItemStack stack) {
		return stack.getOrDefault(JugcraftTools.ENERGY, 0L);
	}

	static void setEnergy(ItemStack stack, long amount) {
		long capacity = stack.getItem() instanceof Chargeable ? capacity(stack) : amount;
		stack.set(JugcraftTools.ENERGY, Math.max(0, Math.min(capacity, amount)));
	}

	/** Takes {@code amount} JE if the item has it all; returns whether it did. */
	static boolean drain(ItemStack stack, long amount) {
		long energy = energy(stack);
		if (energy < amount) {
			return false;
		}
		setEnergy(stack, energy - amount);
		return true;
	}

	/** Adds up to {@code amount} JE and returns how much went in. */
	static long charge(ItemStack stack, long amount) {
		if (!(stack.getItem() instanceof Chargeable)) {
			return 0;
		}
		long energy = energy(stack);
		long added = Math.max(0, Math.min(amount, capacity(stack) - energy));
		if (added > 0) {
			setEnergy(stack, energy + added);
		}
		return added;
	}
}
