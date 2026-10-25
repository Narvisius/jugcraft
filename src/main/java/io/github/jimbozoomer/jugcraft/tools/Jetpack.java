package io.github.jimbozoomer.jugcraft.tools;

import net.minecraft.world.item.ItemStack;

/**
 * A chest item that flies like the rocket pack when charged (RocketPackItem: holding jump in the air). The rocket pack
 * and the exosuit chestplate (batch 28) are jetpacks.
 */
public interface Jetpack {
	/** Whether this stack, worn on the chest, works as a jetpack. */
	default boolean isJetpack(ItemStack stack) {
		return true;
	}

	/** Resource checks also run on the client for prediction; only payThrust mutates a server stack. */
	default boolean hasThrustFuel(ItemStack stack) {
		return stack.getItem() instanceof Chargeable && Chargeable.energy(stack) >= RocketPackItem.ENERGY_PER_TICK;
	}

	default boolean payThrust(ItemStack stack) {
		return stack.getItem() instanceof Chargeable && Chargeable.drain(stack, RocketPackItem.ENERGY_PER_TICK);
	}
}
